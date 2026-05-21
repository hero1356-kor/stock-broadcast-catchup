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

class MicInputSource(
    context: Context,
    private val repository: FakeBroadcastRepository = FakeBroadcastRepository(),
) : BroadcastInputSource {
    private val appContext = context.applicationContext

    override val type: BroadcastInputSourceType = BroadcastInputSourceType.MICROPHONE

    override fun currentSnapshot(): BroadcastInputSnapshot = BroadcastInputSnapshot(
        listeningStatus = "STT 준비",
        elapsedLabel = "00:00:00",
        currentTopic = "마이크 버튼을 눌러 방송 음성을 받아보세요.",
        recentTranscript = emptyList(),
        recentOneMinuteSummary = listOf(
            "STT가 시작되면 최근 1분 흐름을 요약합니다.",
            "외부 STT SDK 없이 Android 기본 음성 인식으로 먼저 검증합니다.",
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
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREA.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        fun startListening(delayMillis: Long = 0L) {
            if (closed.get()) return
            mainHandler.postDelayed(
                {
                    if (!closed.get()) {
                        recognizer.startListening(recognizerIntent)
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
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()

                if (!text.isNullOrBlank()) {
                    trySend(
                        TranscriptLine(
                            time = formatElapsed(SystemClock.elapsedRealtime() - startMillis),
                            text = text,
                        ),
                    )
                }
                startListening(delayMillis = RESTART_DELAY_MILLIS)
            }

            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    -> startListening(delayMillis = RESTART_DELAY_MILLIS)

                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> close(
                        SecurityException("마이크 권한이 필요합니다."),
                    )

                    else -> close(IllegalStateException(errorMessage(error)))
                }
            }
        })

        startListening()

        awaitClose {
            closed.set(true)
            mainHandler.removeCallbacksAndMessages(null)
            recognizer.cancel()
            recognizer.destroy()
        }
    }

    private fun formatElapsed(elapsedMillis: Long): String {
        val totalSeconds = (elapsedMillis / 1_000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "마이크 입력을 처리하지 못했습니다."
        SpeechRecognizer.ERROR_CLIENT -> "음성 인식이 중지됐습니다."
        SpeechRecognizer.ERROR_NETWORK -> "음성 인식 네트워크 오류가 발생했습니다."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "음성 인식 연결 시간이 초과됐습니다."
        SpeechRecognizer.ERROR_SERVER -> "음성 인식 서비스 오류가 발생했습니다."
        else -> "음성 인식 오류가 발생했습니다. code=$error"
    }

    private companion object {
        const val RESTART_DELAY_MILLIS = 350L
    }
}
