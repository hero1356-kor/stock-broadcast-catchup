package com.yeongung.stockbroadcastcatchup.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeongung.stockbroadcastcatchup.domain.BroadcastCatchupUseCase
import com.yeongung.stockbroadcastcatchup.model.BroadcastSession
import com.yeongung.stockbroadcastcatchup.model.IndexQuote
import com.yeongung.stockbroadcastcatchup.model.TranscriptLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    Live,
    RecentSummary,
    CurrentIndex,
    History,
    BroadcastDetail,
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
    val history: List<BroadcastSession> = emptyList(),
    val selectedBroadcast: BroadcastSession? = null,
)

class MainViewModel : ViewModel() {
    private val catchupUseCase = BroadcastCatchupUseCase()

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun showLive() {
        _uiState.update { it.copy(screen = AppScreen.Live) }
    }

    fun showRecentSummary() {
        _uiState.update { it.copy(screen = AppScreen.RecentSummary) }
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
}
