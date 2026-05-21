package com.yeongung.stockbroadcastcatchup.model

data class TranscriptLine(
    val time: String,
    val text: String,
)

data class IndexQuote(
    val name: String,
    val value: String,
    val change: String,
    val isUp: Boolean,
)

data class TimelineItem(
    val time: String,
    val title: String,
)

data class BroadcastSession(
    val id: String,
    val title: String,
    val dateLabel: String,
    val durationLabel: String,
    val topicLine: String,
    val finalSummary: String,
    val oneMinuteSummary: List<String>,
    val transcript: List<TranscriptLine>,
    val timeline: List<TimelineItem>,
    val lowConfidenceRanges: List<String>,
)
