package com.yeongung.stockbroadcastcatchup.input

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.yeongung.stockbroadcastcatchup.data.FakeBroadcastRepository
import com.yeongung.stockbroadcastcatchup.model.TranscriptLine
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidSpeechRecognizerInputSource(
    context: Context,
    private val repository: FakeBroadcastRepository = FakeBroadcastRepository(),
) : BroadcastInputSource {
    private val appContext = context.applicationContext

    override val type: BroadcastInputSourceType = BroadcastInputSourceType.MICROPHONE

    override fun currentSnapshot(): BroadcastInputSnapshot = BroadcastInputSnapshot(
        listeningStatus = "STT 준비",
        elapsedLabel = "00:00:00",
        currentTopic = "녹음 시작을 누르면 TV 소리를 텍스트화합니다.",
        recentTranscript = emptyList(),
        recentOneMinuteSummary = listOf(
            "텍스트화가 시작되면 최근 1분 방송 흐름을 바로 정리합니다.",
            "클라우드 STT 설정이 없는 빌드에서도 버튼을 누르면 기기 내장 STT로 바로 시도합니다.",
        ),
        currentIndices = repository.currentIndices(),
        history = repository.broadcastHistory(),
    )

    override fun transcriptFlow(): Flow<TranscriptLine> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            close(IllegalStateException("이 기기에서 Android 음성 인식을 사용할 수 없습니다."))
            return@callbackFlow
        }

        val mainHandler = Handler(Looper.getMainLooper())
        val closed = AtomicBoolean(false)
        val startMillis = SystemClock.elapsedRealtime()
        val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
        var lastEmittedText = ""

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREA.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, SPEECH_MINIMUM_LENGTH_MILLIS)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, COMPLETE_SILENCE_MILLIS)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, POSSIBLY_COMPLETE_SILENCE_MILLIS)
        }

        fun emitCandidate(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.normalizeTranscript()
                .orEmpty()

            if (text.length >= MIN_TRANSCRIPT_TEXT_LENGTH && text != lastEmittedText) {
                lastEmittedText = text
                trySend(
                    TranscriptLine(
                        time = formatElapsed(SystemClock.elapsedRealtime() - startMillis),
                        text = text,
                    ),
                )
            }
        }

        fun startListening(delayMillis: Long = 0L) {
            if (closed.get()) return
            mainHandler.postDelayed(
                {
                    if (!closed.get()) {
                        try {
                            recognizer.startListening(recognizerIntent)
                        } catch (error: SecurityException) {
                            close(error)
                        } catch (_: RuntimeException) {
                            startListening(delayMillis = ERROR_RESTART_DELAY_MILLIS)
                        }
                    }
                },
                delayMillis,
            )
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onPartialResults(partialResults: Bundle?) {
                emitCandidate(partialResults)
            }

            override fun onResults(results: Bundle?) {
                emitCandidate(results)
                startListening(delayMillis = RESTART_DELAY_MILLIS)
            }

            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> close(
                        SecurityException("마이크 권한이 필요합니다."),
                    )

                    else -> startListening(delayMillis = ERROR_RESTART_DELAY_MILLIS)
                }
            }
        })

        startListening()

        awaitClose {
            closed.set(true)
            mainHandler.removeCallbacksAndMessages(null)
            mainHandler.post {
                recognizer.cancel()
                recognizer.destroy()
            }
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

    private companion object {
        const val RESTART_DELAY_MILLIS = 250L
        const val ERROR_RESTART_DELAY_MILLIS = 700L
        const val SPEECH_MINIMUM_LENGTH_MILLIS = 8_000
        const val COMPLETE_SILENCE_MILLIS = 1_500
        const val POSSIBLY_COMPLETE_SILENCE_MILLIS = 900
        const val MIN_TRANSCRIPT_TEXT_LENGTH = 2
    }
}
