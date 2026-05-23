import Foundation

enum SampleData {
    static let demoTranscript: [TranscriptLine] = [
        TranscriptLine(time: "00:00:03", text: "반도체 업종이 오늘 시장을 주도하고 있습니다."),
        TranscriptLine(time: "00:00:07", text: "엔비디아 실적이 시장 기대를 상회했다는 평가가 나옵니다."),
        TranscriptLine(time: "00:00:12", text: "다만 미국채 금리 부담은 여전히 성장주 변동성을 키우고 있습니다."),
        TranscriptLine(time: "00:00:18", text: "환율은 외국인 수급에 중요한 변수로 보고 있습니다."),
        TranscriptLine(time: "00:00:25", text: "단기적으로는 실적과 금리 방향을 같이 봐야 합니다.")
    ]

    static let indices: [IndexQuote] = [
        IndexQuote(name: "코스피", value: "2,655.42", change: "0.68%", isUp: true),
        IndexQuote(name: "코스닥", value: "856.21", change: "0.62%", isUp: true),
        IndexQuote(name: "나스닥", value: "17,689.36", change: "0.91%", isUp: true),
        IndexQuote(name: "USD/KRW", value: "1,382.10", change: "0.21%", isUp: false)
    ]

    static let history: [BroadcastSession] = [
        BroadcastSession(
            title: "오전 장중 시황",
            dateLabel: "오늘",
            durationLabel: "52분",
            topicLine: "반도체 / 금리 / 외국인 수급",
            finalSummary: "반도체 수급, 엔비디아 실적, 미국채 금리 부담이 주요 주제였습니다.",
            timeline: [
                TimelineItem(time: "09:03", title: "반도체 업종 전망"),
                TimelineItem(time: "09:12", title: "미국채 금리 영향"),
                TimelineItem(time: "09:25", title: "코스피 단기 과열"),
                TimelineItem(time: "09:38", title: "엔비디아 실적 코멘트")
            ],
            lowConfidenceRanges: ["09:21 ~ 09:23 인식 품질 낮음"]
        )
    ]
}
