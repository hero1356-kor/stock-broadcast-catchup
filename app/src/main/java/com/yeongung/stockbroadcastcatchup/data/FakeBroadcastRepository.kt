package com.yeongung.stockbroadcastcatchup.data

import com.yeongung.stockbroadcastcatchup.model.BroadcastSession
import com.yeongung.stockbroadcastcatchup.model.IndexQuote
import com.yeongung.stockbroadcastcatchup.model.TimelineItem
import com.yeongung.stockbroadcastcatchup.model.TranscriptLine

class FakeBroadcastRepository {
    fun listeningStatus(): String = "듣는 중"

    fun elapsedLabel(): String = "00:12:34"

    fun currentTopic(): String =
        "반도체, 엔비디아 실적, 금리 부담을 이야기하는 중입니다."

    fun recentTranscript(): List<TranscriptLine> = listOf(
        TranscriptLine("00:12:31", "반도체 업종이 강세를 보이고 있어요."),
        TranscriptLine("00:12:26", "엔비디아 실적이 시장 기대를 상회했습니다."),
        TranscriptLine("00:12:21", "금리 부담이 지속되며 변동성이 커지고 있습니다."),
    )

    fun recentOneMinuteSummary(): List<String> = listOf(
        "반도체 업종이 강세를 보이며 시장 주도 흐름을 이끌고 있어요.",
        "엔비디아 실적이 시장 기대를 상회해 투자 심리가 개선되었습니다.",
        "금리 부담이 지속되며 변동성이 커지고 있습니다.",
        "단기적으로는 실적과 금리 방향에 따라 등락이 이어질 수 있어요.",
    )

    fun currentIndices(): List<IndexQuote> = listOf(
        IndexQuote("코스피", "2,655.42", "0.68%", true),
        IndexQuote("코스닥", "856.21", "0.62%", true),
        IndexQuote("나스닥", "17,689.36", "0.91%", true),
        IndexQuote("USD/KRW", "1,382.10", "0.21%", false),
    )

    fun broadcastHistory(): List<BroadcastSession> = listOf(
        BroadcastSession(
            id = "morning-market-0521",
            title = "5월 21일 오전 장중 시황",
            dateLabel = "오늘 09:00",
            durationLabel = "42분",
            topicLine = "반도체, 금리, 외국인 수급",
            finalSummary = "반도체 업종 수급과 엔비디아 실적 기대가 시장의 핵심 재료로 다뤄졌습니다. 금리 부담은 성장주 변동성을 키울 수 있는 요인으로 언급됐고, 외국인 수급은 장중 방향성을 확인할 지표로 정리됐습니다.",
            oneMinuteSummary = recentOneMinuteSummary(),
            transcript = listOf(
                TranscriptLine("09:03", "오늘 장은 반도체 업종이 먼저 움직이고 있습니다."),
                TranscriptLine("09:12", "미국채 금리 움직임을 같이 봐야 합니다."),
                TranscriptLine("09:25", "코스피는 단기 과열 부담도 일부 있습니다."),
                TranscriptLine("09:38", "엔비디아 실적 발표 전 기대감이 반영되고 있습니다."),
            ),
            timeline = listOf(
                TimelineItem("09:03", "반도체 업종 전망"),
                TimelineItem("09:12", "미국채 금리 영향"),
                TimelineItem("09:25", "코스피 단기 과열"),
                TimelineItem("09:38", "엔비디아 실적 코멘트"),
            ),
            lowConfidenceRanges = listOf("09:21 ~ 09:23 인식 품질 낮음"),
        ),
        BroadcastSession(
            id = "nvidia-earnings-check",
            title = "엔비디아 실적 점검 라이브",
            dateLabel = "어제 21:30",
            durationLabel = "31분",
            topicLine = "AI 반도체, 실적 기대, 밸류에이션",
            finalSummary = "엔비디아 실적 기대가 AI 반도체 전반의 투자 심리를 끌어올렸다는 내용이 중심이었습니다. 다만 이미 기대가 높아진 만큼 발표 직후에는 차익 실현 가능성도 함께 언급됐습니다.",
            oneMinuteSummary = listOf(
                "AI 반도체 수요가 여전히 강하다는 해석이 나왔습니다.",
                "실적 발표 전 기대감이 관련주에 먼저 반영됐습니다.",
                "발표 직후 변동성 확대 가능성도 함께 언급됐습니다.",
            ),
            transcript = listOf(
                TranscriptLine("21:34", "이번 실적에서 데이터센터 매출이 가장 중요합니다."),
                TranscriptLine("21:40", "기대가 높은 만큼 가이던스도 같이 봐야 합니다."),
                TranscriptLine("21:51", "국내 반도체 장비주로 관심이 이어질 수 있습니다."),
            ),
            timeline = listOf(
                TimelineItem("21:34", "데이터센터 매출"),
                TimelineItem("21:40", "가이던스 확인"),
                TimelineItem("21:51", "국내 관련주 영향"),
            ),
            lowConfidenceRanges = emptyList(),
        ),
        BroadcastSession(
            id = "closing-wrap",
            title = "마감 시황 정리",
            dateLabel = "5월 20일 15:40",
            durationLabel = "27분",
            topicLine = "마감 지수, 기관 수급, 환율",
            finalSummary = "마감 구간에서는 기관 수급과 환율 흐름이 주요 변수로 정리됐습니다. 지수는 상승 마감했지만 업종별 온도 차가 커서 다음 거래일에는 수급 지속 여부를 확인해야 한다는 코멘트가 있었습니다.",
            oneMinuteSummary = listOf(
                "지수는 상승 마감했지만 업종별 차이가 컸습니다.",
                "기관 수급이 일부 대형주를 지지했습니다.",
                "환율 흐름은 다음 거래일에도 확인할 변수로 언급됐습니다.",
            ),
            transcript = listOf(
                TranscriptLine("15:42", "마감까지는 대형주 쪽 수급이 유지됐습니다."),
                TranscriptLine("15:48", "환율은 아직 시장이 부담스러워하는 구간입니다."),
                TranscriptLine("15:59", "내일은 외국인 선물 수급을 먼저 보겠습니다."),
            ),
            timeline = listOf(
                TimelineItem("15:42", "대형주 수급"),
                TimelineItem("15:48", "환율 부담"),
                TimelineItem("15:59", "다음 장 확인 포인트"),
            ),
            lowConfidenceRanges = listOf("15:50 ~ 15:51 주변 소음"),
        ),
    )
}
