package com.yeongung.stockbroadcastcatchup.domain

import com.yeongung.stockbroadcastcatchup.input.BroadcastInputSnapshot
import com.yeongung.stockbroadcastcatchup.input.BroadcastInputSource
import com.yeongung.stockbroadcastcatchup.input.FakeInputSource
import com.yeongung.stockbroadcastcatchup.model.BroadcastSession

class BroadcastCatchupUseCase(
    private val inputSource: BroadcastInputSource = FakeInputSource(),
) {
    fun loadCurrentSnapshot(): BroadcastInputSnapshot = inputSource.currentSnapshot()

    fun findBroadcast(
        history: List<BroadcastSession>,
        id: String,
    ): BroadcastSession? = history.firstOrNull { it.id == id }
}
