package com.yeongung.stockbroadcastcatchup.domain

import com.yeongung.stockbroadcastcatchup.data.MarketIndexRepository
import com.yeongung.stockbroadcastcatchup.data.MarketIndexResult
import com.yeongung.stockbroadcastcatchup.input.BroadcastInputSnapshot
import com.yeongung.stockbroadcastcatchup.input.BroadcastInputSource
import com.yeongung.stockbroadcastcatchup.input.FakeInputSource
import com.yeongung.stockbroadcastcatchup.model.BroadcastSession

class BroadcastCatchupUseCase(
    private val inputSource: BroadcastInputSource = FakeInputSource(),
    private val marketIndexRepository: MarketIndexRepository = MarketIndexRepository(),
) {
    fun loadCurrentSnapshot(): BroadcastInputSnapshot = inputSource.currentSnapshot()

    suspend fun loadLiveCurrentIndices(): MarketIndexResult = marketIndexRepository.currentIndices()

    fun findBroadcast(
        history: List<BroadcastSession>,
        id: String,
    ): BroadcastSession? = history.firstOrNull { it.id == id }
}
