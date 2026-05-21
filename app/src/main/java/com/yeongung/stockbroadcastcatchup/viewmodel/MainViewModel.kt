package com.yeongung.stockbroadcastcatchup.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yeongung.stockbroadcastcatchup.domain.BroadcastCatchupUseCase
import com.yeongung.stockbroadcastcatchup.input.MicInputSource
import com.yeongung.stockbroadcastcatchup.model.BroadcastSession
import com.yeongung.stockbroadcastcatchup.model.CatchupAlert
import com.yeongung.stockbroadcastcatchup.model.IndexQuote
import com.yeongung.stockbroadcastcatchup.model.TranscriptLine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
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
    val catchupAlerts: List<CatchupAlert> = emptyList(),
    val history: List<BroadcastSession> = emptyList(),
    val selectedBroadcast: BroadcastSession? = null,
) {
    val unreadCatchupCount: Int
        get() = catchupAlerts.count { !it.isRead }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val catchupUseCase = BroadcastCatchupUseCase(
        inputSource = MicInputSource(application),
    )
    private var sttJob: Job? = null

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

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

    fun startSttInput() {
        if (_uiState.value.isSttListening || sttJob?.isActive == true) return
        if (!_uiState.value.hasMicrophonePermission) {
            _uiState.update {
                it.copy(sttStatusLabel = "마이크 권한을 허용하면 STT를 시작합니다.")
            }
            return
        }

        sttJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSttListening = true,
                    listeningStatus = "STT 듣는 중",
                    sttStatusLabel = "말소리를 듣고 있습니다.",
                )
            }

            catchupUseCase.transcriptFlow()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isSttListening = false,
                            listeningStatus = "STT 오류",
                            sttStatusLabel = error.message ?: "음성 인식을 시작하지 못했습니다.",
                        )
                    }
                }
                .collect { line ->
                    _uiState.update { state ->
                        val nextTranscript = catchupUseCase.recentOneMinuteTranscript(
                            transcript = listOf(line) + state.recentTranscript,
                            maxLines = MAX_RECENT_TRANSCRIPT_LINES,
                        )
                        val alert = catchupUseCase.buildCatchupAlert(line)
                        val nextAlerts = if (alert == null) {
                            state.catchupAlerts
                        } else {
                            (listOf(alert) + state.catchupAlerts).distinctBy { it.id }.take(MAX_CATCHUP_ALERTS)
                        }

                        state.copy(
                            listeningStatus = "STT 듣는 중",
                            elapsedLabel = line.time,
                            currentTopic = catchupUseCase.inferCurrentTopic(nextTranscript),
                            recentTranscript = nextTranscript,
                            recentOneMinuteSummary = catchupUseCase.summarizeRecentTranscript(nextTranscript),
                            catchupAlerts = nextAlerts,
                            sttStatusLabel = "인식된 문장을 요약에 반영했습니다.",
                        )
                    }
                }
        }
    }

    fun stopSttInput() {
        sttJob?.cancel()
        sttJob = null
        _uiState.update {
            it.copy(
                isSttListening = false,
                listeningStatus = "STT 중지",
                sttStatusLabel = "STT가 중지됐습니다.",
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
        stopSttInput()
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

    private companion object {
        const val MAX_RECENT_TRANSCRIPT_LINES = 12
        const val MAX_CATCHUP_ALERTS = 5
    }
}
