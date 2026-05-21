package com.yeongung.stockbroadcastcatchup.input

import com.yeongung.stockbroadcastcatchup.model.TranscriptLine
import java.util.Locale

data class TranscriptCue(
    val offsetSeconds: Int,
    val text: String,
) {
    fun toTranscriptLine(): TranscriptLine = TranscriptLine(
        time = formatOffset(offsetSeconds),
        text = text,
    )

    private fun formatOffset(seconds: Int): String {
        val safeSeconds = seconds.coerceAtLeast(0)
        val hours = safeSeconds / 3_600
        val minutes = (safeSeconds % 3_600) / 60
        val remainingSeconds = safeSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }
}

object SampleBroadcastScripts {
    val openingMarket: List<TranscriptCue> = listOf(
        TranscriptCue(
            offsetSeconds = 0,
            text = "코스피가 장 초반 보합권에서 등락하면서 외국인 수급을 확인하는 흐름입니다.",
        ),
        TranscriptCue(
            offsetSeconds = 4,
            text = "삼성전자와 SK하이닉스는 나스닥 선물 약세에도 반도체 실적 기대가 버티고 있습니다.",
        ),
        TranscriptCue(
            offsetSeconds = 8,
            text = "원달러 환율은 1,380원대 후반에서 움직이며 성장주 부담으로 언급되고 있습니다.",
        ),
        TranscriptCue(
            offsetSeconds = 12,
            text = "코스닥은 2차전지 일부 종목이 반등하지만 전체 거래대금은 아직 크지 않습니다.",
        ),
        TranscriptCue(
            offsetSeconds = 16,
            text = "방송에서는 미국 금리와 엔비디아 실적 발표 전후 변동성을 계속 확인하자고 정리했습니다.",
        ),
        TranscriptCue(
            offsetSeconds = 20,
            text = "잠시 뒤에는 외국인 선물 매매와 프로그램 수급이 지수 방향을 바꾸는지 보겠습니다.",
        ),
    )
}
