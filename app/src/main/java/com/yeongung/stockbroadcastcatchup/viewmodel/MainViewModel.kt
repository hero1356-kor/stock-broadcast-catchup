package com.yeongung.stockbroadcastcatchup.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yeongung.stockbroadcastcatchup.domain.BroadcastCatchupUseCase
import com.yeongung.stockbroadcastcatchup.input.MicInputSource
import com.yeongung.stockbroadcastcatchup.input.TextInputSource
import com.yeongung.stockbroadcastcatchup.model.BroadcastSession
import com.yeongung.stockbroadcastcatchup.model.CatchupAlert
import com.yeongung.stockbroadcastcatchup.model.IndexQuote
import com.yeongung.stockbroadcastcatchup.model.TranscriptLine
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppScreen {
    Live,
    RecentSummary,
    CurrentIndex,
    History,
    BroadcastDetail,
    CatchupAlerts,
}

data class MainUiState(
    val screen: AppScreen = AppScreen.Live,
    val listeningStatus: String = "",
    val elapsedLabel: String = "",
    val currentTopic: String = "",
    val recentTranscript: List<TranscriptLine> = emptyList(),
    val recentOneMinuteSummary: List<String> = emptyList(),
    val currentIndices: List<IndexQuote> = emptyList(),
    val currentIndexStatusLabel: String = "가상 지수입니다. 화면을 열면 최신 값을 가져옵니다.",
    val isRefreshingCurrentIndices: Boolean = false,
    val hasMicrophonePermission: Boolean = false,
    val isSttListening: Boolean = false,
    val sttStatusLabel: String = "마이크 권한을 확인한 뒤 STT를 시작할 수 있습니다.",
    val isDemoRunning: Boolean = false,
    val demoStatusLabel: String = "앱 화면을 먼저 볼 수 있게 샘플 방송 자막을 재생할 수 있습니다.",
    val catchupAlerts: List<CatchupAlert> = emptyList(),
    val history: List<BroadcastSession> = emptyList(),
    val selectedBroadcast: BroadcastSession? = null,
) {
    val unreadCatchupCount: Int
        get() = catchupAlerts.count { !it.isRead }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val micUseCase = BroadcastCatchupUseCase(
        inputSource = MicInputSource(application),
    )
    private val demoUseCase = BroadcastCatchupUseCase(
        inputSource = TextInputSource(millisecondsPerScriptSecond = DEMO_MILLISECONDS_PER_SCRIPT_SECOND),
    )
    private val catchupUseCase = micUseCase
    private var sttJob: Job? = null
    private var sttElapsedJob: Job? = null
    private var demoJob: Job? = null

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        startDemoInput()
    }

    fun setMicrophonePermission(granted: Boolean) {
        _uiState.update { state ->
            state.copy(
                hasMicrophonePermission = granted,
                sttStatusLabel = if (granted) {
                    "STT를 시작할 준비가 됐습니다."
                } else {
                    "마이크 권한이 필요합니다."
                },
            )
        }
    }

    fun startDemoInput() {
        if (_uiState.value.isDemoRunning || demoJob?.isActive == true) return
        stopSttInput(updateStatus = false)

        demoJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDemoRunning = true,
                    listeningStatus = "데모 재생 중",
                    elapsedLabel = "00:00:00",
                    currentTopic = "샘플 방송 자막을 재생하고 있습니다.",
                    recentTranscript = emptyList(),
                    recentOneMinuteSummary = listOf(
                        "샘플 자막이 들어오면 최근 1분 요약이 갱신됩니다.",
                        "STT 성공 여부와 상관없이 UI 흐름을 먼저 확인할 수 있습니다.",
                    ),
                    catchupAlerts = emptyList(),
                    demoStatusLabel = "샘플 방송 자막을 재생 중입니다.",
                    sttStatusLabel = "데모 재생 중에는 STT를 잠시 멈춥니다.",
                )
            }

            demoUseCase.transcriptFlow()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isDemoRunning = false,
                            listeningStatus = "데모 오류",
                            demoStatusLabel = error.message ?: "데모 자막을 재생하지 못했습니다.",
                        )
                    }
                }
                .collect { line ->
                    applyTranscriptLine(line = line, useCase = demoUseCase) { state ->
                        state.copy(
                            listeningStatus = "데모 재생 중",
                            demoStatusLabel = "샘플 자막이 요약에 반영됐습니다.",
                        )
                    }
                }

            _uiState.update {
                it.copy(
                    isDemoRunning = false,
                    listeningStatus = "데모 완료",
                    demoStatusLabel = "샘플 방송 자막 재생이 끝났습니다.",
                )
            }
        }
    }

    fun stopDemoInput() {
        demoJob?.cancel()
        demoJob = null
        _uiState.update {
            it.copy(
                isDemoRunning = false,
                listeningStatus = "데모 중지",
                demoStatusLabel = "데모 자막 재생이 중지됐습니다.",
            )
        }
    }

    fun startSttInput() {
        if (_uiState.value.isSttListening || sttJob?.isActive == true) return
        stopDemoInputForStt()
        if (!_uiState.value.hasMicrophonePermission) {
            _uiState.update {
                it.copy(sttStatusLabel = "마이크 권한을 허용하면 STT를 시작합니다.")
            }
            return
        }

        val startedAtMillis = SystemClock.elapsedRealtime()
        sttJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSttListening = true,
                    listeningStatus = "STT 듣는 중",
                    elapsedLabel = formatElapsed(0L),
                    sttStatusLabel = "말소리를 듣고 있습니다.",
                    demoStatusLabel = "STT 테스트 중에는 데모 자막이 멈춥니다.",
                )
            }
            startSttElapsedCounter(startedAtMillis)

            try {
                micUseCase.transcriptFlow()
                    .catch { error ->
                        stopSttElapsedCounter()
                        _uiState.update {
                            it.copy(
                                isSttListening = false,
                                listeningStatus = "STT 오류",
                                sttStatusLabel = error.message ?: "음성 인식을 시작하지 못했습니다.",
                            )
                        }
                    }
                    .collect { line ->
                        applyTranscriptLine(line = line, useCase = micUseCase) { state ->
                            state.copy(
                                listeningStatus = "STT 듣는 중",
                                sttStatusLabel = "인식된 문장을 요약에 반영했습니다.",
                            )
                        }
                    }
            } finally {
                stopSttElapsedCounter()
            }
        }
    }

    fun stopSttInput() {
        stopSttInput(updateStatus = true)
    }

    private fun stopSttInput(updateStatus: Boolean) {
        sttJob?.cancel()
        sttJob = null
        stopSttElapsedCounter()
        _uiState.update {
            if (updateStatus) {
                it.copy(
                    isSttListening = false,
                    listeningStatus = "STT 중지",
                    sttStatusLabel = "STT가 중지됐습니다.",
                )
            } else {
                it.copy(isSttListening = false)
            }
        }
    }

    private fun startSttElapsedCounter(startedAtMillis: Long) {
        stopSttElapsedCounter()
        sttElapsedJob = viewModelScope.launch {
            while (isActive) {
                val elapsedMillis = SystemClock.elapsedRealtime() - startedAtMillis
                _uiState.update { state ->
                    if (state.isSttListening) {
                        state.copy(elapsedLabel = formatElapsed(elapsedMillis))
                    } else {
                        state
                    }
                }
                delay(1_000L)
            }
        }
    }

    private fun stopSttElapsedCounter() {
        sttElapsedJob?.cancel()
        sttElapsedJob = null
    }

    private fun stopDemoInputForStt() {
        demoJob?.cancel()
        demoJob = null
        _uiState.update {
            it.copy(isDemoRunning = false)
        }
    }

    private fun applyTranscriptLine(
        line: TranscriptLine,
        useCase: BroadcastCatchupUseCase,
        extraState: (MainUiState) -> MainUiState,
    ) {
        _uiState.update { state ->
            val nextTranscript = useCase.recentOneMinuteTranscript(
                transcript = listOf(line) + state.recentTranscript,
                maxLines = MAX_RECENT_TRANSCRIPT_LINES,
            )
            val alert = useCase.buildCatchupAlert(line)
            val nextAlerts = if (alert == null) {
                state.catchupAlerts
            } else {
                (listOf(alert) + state.catchupAlerts).distinctBy { it.id }.take(MAX_CATCHUP_ALERTS)
            }

            extraState(
                state.copy(
                    elapsedLabel = line.time,
                    currentTopic = useCase.inferCurrentTopic(nextTranscript),
                    recentTranscript = nextTranscript,
                    recentOneMinuteSummary = useCase.summarizeRecentTranscript(nextTranscript),
                    catchupAlerts = nextAlerts,
                ),
            )
        }
    }

    fun showLive() {
        _uiState.update { it.copy(screen = AppScreen.Live) }
    }

    fun showRecentSummary() {
        _uiState.update { it.copy(screen = AppScreen.RecentSummary) }
    }

    fun showCatchupAlerts() {
        _uiState.update { it.copy(screen = AppScreen.CatchupAlerts) }
    }

    fun markAllCatchupAlertsRead() {
        _uiState.update { state ->
            state.copy(
                catchupAlerts = state.catchupAlerts.map { it.copy(isRead = true) },
            )
        }
    }

    fun showCurrentIndex() {
        _uiState.update { it.copy(screen = AppScreen.CurrentIndex) }
        refreshCurrentIndices()
    }

    fun refreshCurrentIndices() {
        if (_uiState.value.isRefreshingCurrentIndices) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshingCurrentIndices = true,
                    currentIndexStatusLabel = "지수 정보를 가져오는 중입니다...",
                )
            }

            try {
                val result = catchupUseCase.loadLiveCurrentIndices()
                _uiState.update {
                    it.copy(
                        currentIndices = result.quotes,
                        currentIndexStatusLabel = "온라인 지수 · ${result.updatedAtLabel} 기준",
                        isRefreshingCurrentIndices = false,
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        currentIndexStatusLabel = "지수 정보를 가져오지 못해 마지막 값을 보여드려요.",
                        isRefreshingCurrentIndices = false,
                    )
                }
            }
        }
    }

    fun showHistory() {
        _uiState.update { it.copy(screen = AppScreen.History) }
    }

    fun finishAndShowDetail() {
        _uiState.update { state ->
            state.copy(
                screen = AppScreen.BroadcastDetail,
                selectedBroadcast = state.history.firstOrNull(),
            )
        }
    }

    fun openBroadcast(id: String) {
        _uiState.update { state ->
            state.copy(
                screen = AppScreen.BroadcastDetail,
                selectedBroadcast = catchupUseCase.findBroadcast(state.history, id)
                    ?: state.selectedBroadcast,
            )
        }
    }

    override fun onCleared() {
        stopDemoInputForStt()
        stopSttInput(updateStatus = false)
        super.onCleared()
    }

    private fun createInitialState(): MainUiState {
        val snapshot = catchupUseCase.loadCurrentSnapshot()
        return MainUiState(
            listeningStatus = snapshot.listeningStatus,
            elapsedLabel = snapshot.elapsedLabel,
            currentTopic = snapshot.currentTopic,
            recentTranscript = snapshot.recentTranscript,
            recentOneMinuteSummary = snapshot.recentOneMinuteSummary,
            currentIndices = snapshot.currentIndices,
            history = snapshot.history,
            selectedBroadcast = snapshot.history.firstOrNull(),
        )
    }

    private fun formatElapsed(elapsedMillis: Long): String {
        val totalSeconds = (elapsedMillis / 1_000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private companion object {
        const val MAX_RECENT_TRANSCRIPT_LINES = 12
        const val MAX_CATCHUP_ALERTS = 5
        const val DEMO_MILLISECONDS_PER_SCRIPT_SECOND = 250L
    }
}
