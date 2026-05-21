package com.yeongung.stockbroadcastcatchup.viewmodel

import androidx.lifecycle.ViewModel
import com.yeongung.stockbroadcastcatchup.data.FakeBroadcastRepository
import com.yeongung.stockbroadcastcatchup.model.BroadcastSession
import com.yeongung.stockbroadcastcatchup.model.IndexQuote
import com.yeongung.stockbroadcastcatchup.model.TranscriptLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
    val history: List<BroadcastSession> = emptyList(),
    val selectedBroadcast: BroadcastSession? = null,
)

class MainViewModel : ViewModel() {
    private val repository = FakeBroadcastRepository()

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
                selectedBroadcast = state.history.firstOrNull { it.id == id } ?: state.selectedBroadcast,
            )
        }
    }

    private fun createInitialState(): MainUiState {
        val history = repository.broadcastHistory()
        return MainUiState(
            listeningStatus = repository.listeningStatus(),
            elapsedLabel = repository.elapsedLabel(),
            currentTopic = repository.currentTopic(),
            recentTranscript = repository.recentTranscript(),
            recentOneMinuteSummary = repository.recentOneMinuteSummary(),
            currentIndices = repository.currentIndices(),
            history = history,
            selectedBroadcast = history.firstOrNull(),
        )
    }
}
