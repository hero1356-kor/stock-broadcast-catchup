package com.yeongung.stockbroadcastcatchup.domain

import com.yeongung.stockbroadcastcatchup.data.MarketIndexRepository
import com.yeongung.stockbroadcastcatchup.data.MarketIndexResult
import com.yeongung.stockbroadcastcatchup.input.BroadcastInputSnapshot
import com.yeongung.stockbroadcastcatchup.input.BroadcastInputSource
import com.yeongung.stockbroadcastcatchup.input.TextInputSource
import com.yeongung.stockbroadcastcatchup.model.BroadcastSession
import com.yeongung.stockbroadcastcatchup.model.TranscriptLine
import kotlinx.coroutines.flow.Flow

class BroadcastCatchupUseCase(
    private val inputSource: BroadcastInputSource = TextInputSource(),
    private val marketIndexRepository: MarketIndexRepository = MarketIndexRepository(),
) {
    fun loadCurrentSnapshot(): BroadcastInputSnapshot = inputSource.currentSnapshot()

    fun transcriptFlow(): Flow<TranscriptLine> = inputSource.transcriptFlow()

    suspend fun loadLiveCurrentIndices(): MarketIndexResult = marketIndexRepository.currentIndices()

    fun inferCurrentTopic(recentTranscript: List<TranscriptLine>): String {
        val combinedText = recentTranscript.joinToString(separator = " ") { it.text }
        val topics = mutableListOf<String>()

        if (combinedText.contains("코스피") || combinedText.contains("코스닥") || combinedText.contains("지수")) {
            topics += "국내 지수"
        }
        if (combinedText.contains("환율") || combinedText.contains("원달러")) {
            topics += "환율"
        }
        if (combinedText.contains("나스닥") || combinedText.contains("미국") || combinedText.contains("금리")) {
            topics += "미국장"
        }
        if (combinedText.contains("반도체") || combinedText.contains("삼성전자") || combinedText.contains("SK하이닉스")) {
            topics += "반도체"
        }

        return if (topics.isEmpty()) {
            "방송 자막을 받아 핵심 흐름을 정리하는 중입니다."
        } else {
            "${topics.distinct().joinToString(", ")} 흐름을 이야기하는 중입니다."
        }
    }

    fun summarizeRecentTranscript(recentTranscript: List<TranscriptLine>): List<String> {
        if (recentTranscript.isEmpty()) {
            return listOf(
                "방송 자막이 들어오면 최근 흐름을 바로 정리합니다.",
                "시장 키워드가 쌓이면 지수 움직임과 함께 확인합니다.",
            )
        }

        val latestLine = recentTranscript.first()
        return listOf(
            "최근 자막 ${recentTranscript.size}개를 기준으로 흐름을 갱신했습니다.",
            "방금 나온 문장: ${latestLine.text}",
            inferCurrentTopic(recentTranscript),
        )
    }

    fun findBroadcast(
        history: List<BroadcastSession>,
        id: String,
    ): BroadcastSession? = history.firstOrNull { it.id == id }
}
