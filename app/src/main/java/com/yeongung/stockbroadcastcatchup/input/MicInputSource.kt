package com.yeongung.stockbroadcastcatchup.input

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import com.yeongung.stockbroadcastcatchup.data.FakeBroadcastRepository
import com.yeongung.stockbroadcastcatchup.model.TranscriptLine
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

class MicInputSource(
    context: Context,
    private val repository: FakeBroadcastRepository = FakeBroadcastRepository(),
    private val sttClient: SttClient = ClovaCsrSttClient(context),
) : BroadcastInputSource {
    private val fallbackInputSource = AndroidSpeechRecognizerInputSource(context, repository)

    override val type: BroadcastInputSourceType = BroadcastInputSourceType.MICROPHONE

    override fun currentSnapshot(): BroadcastInputSnapshot = BroadcastInputSnapshot(
        listeningStatus = "STT 준비",
        elapsedLabel = "00:00:00",
        currentTopic = "녹음 시작을 누르면 TV 소리를 텍스트화합니다.",
        recentTranscript = emptyList(),
        recentOneMinuteSummary = listOf(
            "텍스트화가 시작되면 최근 1분 방송 흐름을 바로 정리합니다.",
            "TV 소리를 우선 클라우드 STT로 시도하고, 설정이 없거나 실패하면 기기 내장 STT로 바로 전환합니다.",
        ),
        currentIndices = repository.currentIndices(),
        history = repository.broadcastHistory(),
    )

    override fun transcriptFlow(): Flow<TranscriptLine> = if (sttClient.isConfigured) {
        clovaTranscriptFlow().catch {
            fallbackInputSource.transcriptFlow().collect { line -> emit(line) }
        }
    } else {
        fallbackInputSource.transcriptFlow()
    }

    private fun clovaTranscriptFlow(): Flow<TranscriptLine> = callbackFlow {
        val startMillis = SystemClock.elapsedRealtime()
        val chunkChannel = Channel<RecordedAudioChunk>(capacity = CHUNK_QUEUE_CAPACITY)
        var audioRecord: AudioRecord? = null
        var lastEmittedText = ""

        val transcriptionJob = launch(Dispatchers.IO) {
            try {
                for (chunk in chunkChannel) {
                    val normalizedText = sttClient.transcribe(chunk.wavAudio).normalizeTranscript()
                    if (normalizedText.length >= MIN_TRANSCRIPT_TEXT_LENGTH && normalizedText != lastEmittedText) {
                        lastEmittedText = normalizedText
                        trySend(
                            TranscriptLine(
                                time = chunk.elapsedLabel,
                                text = normalizedText,
                            ),
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                close(IllegalStateException("클라우드 STT 요청에 실패했습니다. 기기 내장 STT로 전환합니다.", error))
            }
        }

        val recordingJob = launch(Dispatchers.IO) {
            val pcmBuffer = ByteArrayOutputStream(CHUNK_PCM_BUFFER_BYTES)
            val readBuffer = ShortArray(READ_BUFFER_SAMPLES)
            var chunkStartedAtMillis = SystemClock.elapsedRealtime()
            var maxAbsAmplitude = 0

            fun flushChunk() {
                val pcmBytes = pcmBuffer.toByteArray()
                val shouldSend = pcmBytes.isNotEmpty() && maxAbsAmplitude >= MIN_CHUNK_PEAK_AMPLITUDE
                val elapsedLabel = formatElapsed(SystemClock.elapsedRealtime() - startMillis)

                pcmBuffer.reset()
                chunkStartedAtMillis = SystemClock.elapsedRealtime()
                maxAbsAmplitude = 0

                if (shouldSend) {
                    chunkChannel.trySend(
                        RecordedAudioChunk(
                            elapsedLabel = elapsedLabel,
                            wavAudio = encodeWav(pcmBytes = pcmBytes),
                        ),
                    )
                }
            }

            try {
                val recorder = createAudioRecord()
                audioRecord = recorder
                recorder.startRecording()

                while (isActive) {
                    val readCount = recorder.read(readBuffer, 0, readBuffer.size)
                    when {
                        readCount > 0 -> {
                            maxAbsAmplitude = max(
                                maxAbsAmplitude,
                                pcmBuffer.writeLittleEndianPcm(readBuffer, readCount),
                            )

                            if (SystemClock.elapsedRealtime() - chunkStartedAtMillis >= CHUNK_DURATION_MILLIS) {
                                flushChunk()
                            }
                        }

                        readCount < 0 -> throw IOException("AudioRecord read failed. code=$readCount")
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: SecurityException) {
                close(SecurityException("마이크 권한이 필요합니다.", error))
            } catch (error: Exception) {
                close(IllegalStateException("마이크 원본 오디오를 녹음하지 못했습니다: ${error.message}", error))
            } finally {
                chunkChannel.close()
                audioRecord?.stopSafely()
                audioRecord?.release()
                audioRecord = null
            }
        }

        awaitClose {
            recordingJob.cancel()
            transcriptionJob.cancel()
            chunkChannel.close()
            audioRecord?.stopSafely()
            audioRecord?.release()
            audioRecord = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun createAudioRecord(): AudioRecord {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
        )
        if (minBufferSize <= 0) {
            throw IOException("AudioRecord 최소 버퍼를 계산하지 못했습니다. code=$minBufferSize")
        }

        val bufferSize = max(minBufferSize * 4, READ_BUFFER_SAMPLES * BYTES_PER_SAMPLE * 4)
        val audioSources = listOf(
            MediaRecorder.AudioSource.UNPROCESSED,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
        )

        var lastError: Throwable? = null
        for (audioSource in audioSources) {
            try {
                val recorder = AudioRecord(
                    audioSource,
                    SAMPLE_RATE_HZ,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize,
                )
                if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                    return recorder
                }
                recorder.release()
            } catch (error: IllegalArgumentException) {
                lastError = error
            } catch (error: UnsupportedOperationException) {
                lastError = error
            }
        }

        throw IOException("사용 가능한 마이크 입력 소스를 초기화하지 못했습니다.", lastError)
    }

    private fun ByteArrayOutputStream.writeLittleEndianPcm(
        samples: ShortArray,
        count: Int,
    ): Int {
        var maxAbsAmplitude = 0
        for (index in 0 until count) {
            val sample = samples[index].toInt()
            write(sample and 0xFF)
            write((sample shr 8) and 0xFF)
            maxAbsAmplitude = max(maxAbsAmplitude, abs(sample))
        }
        return maxAbsAmplitude
    }

    private fun encodeWav(pcmBytes: ByteArray): ByteArray {
        val dataSize = pcmBytes.size
        val byteRate = SAMPLE_RATE_HZ * CHANNEL_COUNT * BITS_PER_SAMPLE / 8
        val blockAlign = CHANNEL_COUNT * BITS_PER_SAMPLE / 8

        return ByteArrayOutputStream(WAV_HEADER_BYTES + dataSize).apply {
            writeAscii("RIFF")
            writeIntLittleEndian(36 + dataSize)
            writeAscii("WAVE")
            writeAscii("fmt ")
            writeIntLittleEndian(16)
            writeShortLittleEndian(PCM_FORMAT)
            writeShortLittleEndian(CHANNEL_COUNT)
            writeIntLittleEndian(SAMPLE_RATE_HZ)
            writeIntLittleEndian(byteRate)
            writeShortLittleEndian(blockAlign)
            writeShortLittleEndian(BITS_PER_SAMPLE)
            writeAscii("data")
            writeIntLittleEndian(dataSize)
            write(pcmBytes)
        }.toByteArray()
    }

    private fun ByteArrayOutputStream.writeAscii(value: String) {
        write(value.toByteArray(Charsets.US_ASCII))
    }

    private fun ByteArrayOutputStream.writeIntLittleEndian(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
        write((value shr 16) and 0xFF)
        write((value shr 24) and 0xFF)
    }

    private fun ByteArrayOutputStream.writeShortLittleEndian(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
    }

    private fun AudioRecord.stopSafely() {
        try {
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                stop()
            }
        } catch (_: IllegalStateException) {
            // The recorder may already be released when the flow is closing.
        }
    }

    private fun String.normalizeTranscript(): String = trim().replace(Regex("\\s+"), " ")

    private fun formatElapsed(elapsedMillis: Long): String {
        val totalSeconds = (elapsedMillis / 1_000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private data class RecordedAudioChunk(
        val elapsedLabel: String,
        val wavAudio: ByteArray,
    )

    private companion object {
        const val SAMPLE_RATE_HZ = 16_000
        const val CHANNEL_COUNT = 1
        val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val BITS_PER_SAMPLE = 16
        const val BYTES_PER_SAMPLE = 2
        const val PCM_FORMAT = 1
        const val WAV_HEADER_BYTES = 44
        const val CHUNK_DURATION_MILLIS = 8_000L
        const val READ_INTERVAL_MILLIS = 100
        const val READ_BUFFER_SAMPLES = SAMPLE_RATE_HZ * READ_INTERVAL_MILLIS / 1_000
        const val CHUNK_PCM_BUFFER_BYTES = SAMPLE_RATE_HZ * BYTES_PER_SAMPLE * 9
        const val CHUNK_QUEUE_CAPACITY = 3
        const val MIN_TRANSCRIPT_TEXT_LENGTH = 2
        const val MIN_CHUNK_PEAK_AMPLITUDE = 150
    }
}
