package com.yeongung.stockbroadcastcatchup.input

import com.yeongung.stockbroadcastcatchup.data.FakeBroadcastRepository

class FakeInputSource(
    private val repository: FakeBroadcastRepository = FakeBroadcastRepository(),
) : BroadcastInputSource {
    override val type: BroadcastInputSourceType = BroadcastInputSourceType.FAKE

    override fun currentSnapshot(): BroadcastInputSnapshot {
        return BroadcastInputSnapshot(
            listeningStatus = repository.listeningStatus(),
            elapsedLabel = repository.elapsedLabel(),
            currentTopic = repository.currentTopic(),
            recentTranscript = repository.recentTranscript(),
            recentOneMinuteSummary = repository.recentOneMinuteSummary(),
            currentIndices = repository.currentIndices(),
            history = repository.broadcastHistory(),
        )
    }
}
