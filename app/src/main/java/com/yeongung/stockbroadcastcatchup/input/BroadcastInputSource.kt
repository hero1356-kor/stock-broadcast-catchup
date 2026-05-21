package com.yeongung.stockbroadcastcatchup.input

import com.yeongung.stockbroadcastcatchup.model.BroadcastSession
import com.yeongung.stockbroadcastcatchup.model.IndexQuote
import com.yeongung.stockbroadcastcatchup.model.TranscriptLine

enum class BroadcastInputSourceType {
    FAKE,
    MICROPHONE,
    ANDROID_PLAYBACK_CAPTURE,
    TEXT,
}

data class BroadcastInputSnapshot(
    val listeningStatus: String,
    val elapsedLabel: String,
    val currentTopic: String,
    val recentTranscript: List<TranscriptLine>,
    val recentOneMinuteSummary: List<String>,
    val currentIndices: List<IndexQuote>,
    val history: List<BroadcastSession>,
) {
    companion object {
        fun pending(sourceName: String): BroadcastInputSnapshot = BroadcastInputSnapshot(
            listeningStatus = "Pending",
            elapsedLabel = "00:00:00",
            currentTopic = "$sourceName input is planned for a later phase.",
            recentTranscript = emptyList(),
            recentOneMinuteSummary = emptyList(),
            currentIndices = emptyList(),
            history = emptyList(),
        )
    }
}

interface BroadcastInputSource {
    val type: BroadcastInputSourceType

    fun currentSnapshot(): BroadcastInputSnapshot
}
