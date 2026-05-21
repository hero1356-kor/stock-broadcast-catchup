package com.yeongung.stockbroadcastcatchup.input

import com.yeongung.stockbroadcastcatchup.data.FakeBroadcastRepository
import com.yeongung.stockbroadcastcatchup.model.TranscriptLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TextInputSource(
    private val script: List<TranscriptCue> = SampleBroadcastScripts.openingMarket,
    private val repository: FakeBroadcastRepository = FakeBroadcastRepository(),
    private val millisecondsPerScriptSecond: Long = 1_000L,
) : BroadcastInputSource {
    override val type: BroadcastInputSourceType = BroadcastInputSourceType.TEXT

    override fun currentSnapshot(): BroadcastInputSnapshot = BroadcastInputSnapshot(
        listeningStatus = "자막 대기 중",
        elapsedLabel = "00:00:00",
        currentTopic = "방송 자막을 기다리는 중입니다.",
        recentTranscript = emptyList(),
        recentOneMinuteSummary = listOf(
            "방송 자막이 들어오면 최근 흐름을 바로 정리합니다.",
            "지수, 환율, 반도체 같은 시장 키워드를 중심으로 묶어 보여줍니다.",
        ),
        currentIndices = repository.currentIndices(),
        history = repository.broadcastHistory(),
    )

    override fun transcriptFlow(): Flow<TranscriptLine> = flow {
        val delayUnitMillis = millisecondsPerScriptSecond.coerceAtLeast(1L)
        var previousOffsetSeconds = 0

        script.forEachIndexed { index, cue ->
            val waitSeconds = if (index == 0) {
                cue.offsetSeconds
            } else {
                cue.offsetSeconds - previousOffsetSeconds
            }.coerceAtLeast(0)

            delay(waitSeconds.toLong() * delayUnitMillis)
            emit(cue.toTranscriptLine())
            previousOffsetSeconds = cue.offsetSeconds
        }
    }
}
