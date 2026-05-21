package com.yeongung.stockbroadcastcatchup.domain

import com.yeongung.stockbroadcastcatchup.data.MarketIndexRepository
import com.yeongung.stockbroadcastcatchup.data.MarketIndexResult
import com.yeongung.stockbroadcastcatchup.input.BroadcastInputSnapshot
import com.yeongung.stockbroadcastcatchup.input.BroadcastInputSource
import com.yeongung.stockbroadcastcatchup.input.TextInputSource
import com.yeongung.stockbroadcastcatchup.model.BroadcastSession
import com.yeongung.stockbroadcastcatchup.model.CatchupAlert
import com.yeongung.stockbroadcastcatchup.model.TranscriptLine
import kotlinx.coroutines.flow.Flow

class BroadcastCatchupUseCase(
    private val inputSource: BroadcastInputSource = TextInputSource(),
    private val marketIndexRepository: MarketIndexRepository = MarketIndexRepository(),
) {
    fun loadCurrentSnapshot(): BroadcastInputSnapshot = inputSource.currentSnapshot()

    fun transcriptFlow(): Flow<TranscriptLine> = inputSource.transcriptFlow()

    suspend fun loadLiveCurrentIndices(): MarketIndexResult = marketIndexRepository.currentIndices()

    fun recentOneMinuteTranscript(
        transcript: List<TranscriptLine>,
        maxLines: Int,
    ): List<TranscriptLine> {
        val latestSeconds = transcript.firstOrNull()?.time?.toElapsedSeconds()
            ?: return transcript.take(maxLines)

        return transcript
            .filter { line ->
                val seconds = line.time.toElapsedSeconds()
                seconds == null || latestSeconds - seconds <= ONE_MINUTE_SECONDS
            }
            .take(maxLines)
    }

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
            "방송 음성을 받아 핵심 흐름을 정리하는 중입니다."
        } else {
            "${topics.distinct().joinToString(", ")} 흐름을 이야기하는 중입니다."
        }
    }

    fun summarizeRecentTranscript(recentTranscript: List<TranscriptLine>): List<String> {
        if (recentTranscript.isEmpty()) {
            return listOf(
                "STT가 시작되면 최근 1분 방송 흐름을 바로 정리합니다.",
                "시장 키워드가 쌓이면 캐치업 알림으로 따로 모아둡니다.",
            )
        }

        val latestLine = recentTranscript.first()
        return listOf(
            "최근 1분 자막 ${recentTranscript.size}개를 기준으로 흐름을 갱신했습니다.",
            "방금 인식한 문장: ${latestLine.text}",
            inferCurrentTopic(recentTranscript),
        )
    }

    fun buildCatchupAlert(line: TranscriptLine): CatchupAlert? {
        val text = line.text
        val alert = when {
            text.contains("환율") || text.contains("원달러") -> CatchupAlertDraft(
                title = "환율 언급",
                message = "방송에서 환율 부담이 언급됐습니다. 나중에 다시 볼 항목으로 남겼어요.",
            )
            text.contains("외국인") || text.contains("수급") || text.contains("프로그램") -> CatchupAlertDraft(
                title = "수급 체크",
                message = "외국인 또는 프로그램 수급 이야기가 나왔습니다. 흐름 확인용으로 보관했습니다.",
            )
            text.contains("금리") || text.contains("미국") || text.contains("나스닥") -> CatchupAlertDraft(
                title = "미국장 변수",
                message = "미국장, 금리, 나스닥 관련 변수가 언급됐습니다.",
            )
            text.contains("실적") || text.contains("엔비디아") || text.contains("반도체") -> CatchupAlertDraft(
                title = "실적/반도체 이슈",
                message = "실적 또는 반도체 관련 내용이 나왔습니다. 주요 방송 흐름으로 저장했습니다.",
            )
            text.contains("코스닥") || text.contains("2차전지") -> CatchupAlertDraft(
                title = "코스닥 종목 흐름",
                message = "코스닥 또는 2차전지 흐름이 언급됐습니다.",
            )
            else -> null
        } ?: return null

        return CatchupAlert(
            id = "${line.time}-${text.hashCode()}",
            time = line.time,
            title = alert.title,
            message = alert.message,
        )
    }

    fun findBroadcast(
        history: List<BroadcastSession>,
        id: String,
    ): BroadcastSession? = history.firstOrNull { it.id == id }

    private fun String.toElapsedSeconds(): Long? {
        val parts = split(":").mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            2 -> parts[0] * 60L + parts[1]
            3 -> parts[0] * 3_600L + parts[1] * 60L + parts[2]
            else -> null
        }
    }

    private companion object {
        const val ONE_MINUTE_SECONDS = 60L
    }
}

private data class CatchupAlertDraft(
    val title: String,
    val message: String,
)
