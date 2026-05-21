package com.yeongung.stockbroadcastcatchup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yeongung.stockbroadcastcatchup.model.BroadcastSession
import com.yeongung.stockbroadcastcatchup.model.IndexQuote
import com.yeongung.stockbroadcastcatchup.model.TranscriptLine
import com.yeongung.stockbroadcastcatchup.ui.theme.CatchupColors
import com.yeongung.stockbroadcastcatchup.ui.theme.StockBroadcastCatchupTheme
import com.yeongung.stockbroadcastcatchup.viewmodel.AppScreen
import com.yeongung.stockbroadcastcatchup.viewmodel.MainUiState
import com.yeongung.stockbroadcastcatchup.viewmodel.MainViewModel

@Composable
fun StockBroadcastCatchupApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    StockBroadcastCatchupTheme {
        when (state.screen) {
            AppScreen.Live -> LiveScreen(
                state = state,
                onRecentSummary = viewModel::showRecentSummary,
                onFinish = viewModel::finishAndShowDetail,
                onHistory = viewModel::showHistory,
                onCurrentIndex = viewModel::showCurrentIndex,
            )

            AppScreen.RecentSummary -> RecentSummaryScreen(
                state = state,
                onBack = viewModel::showLive,
            )

            AppScreen.CurrentIndex -> CurrentIndexScreen(
                indices = state.currentIndices,
                statusLabel = state.currentIndexStatusLabel,
                isRefreshing = state.isRefreshingCurrentIndices,
                onRefresh = viewModel::refreshCurrentIndices,
                onBack = viewModel::showLive,
            )

            AppScreen.History -> HistoryScreen(
                history = state.history,
                onBack = viewModel::showLive,
                onOpenBroadcast = viewModel::openBroadcast,
            )

            AppScreen.BroadcastDetail -> BroadcastDetailScreen(
                broadcast = state.selectedBroadcast,
                onBack = viewModel::showLive,
            )
        }
    }
}

@Composable
private fun LiveScreen(
    state: MainUiState,
    onRecentSummary: () -> Unit,
    onFinish: () -> Unit,
    onHistory: () -> Unit,
    onCurrentIndex: () -> Unit,
) {
    ScrollPage {
        LiveHeader(
            status = state.listeningStatus,
            elapsed = state.elapsedLabel,
        )

        Spacer(Modifier.height(24.dp))

        SimpleCard(containerColor = CatchupColors.SurfaceRaised) {
            SectionTitle("지금 무슨 얘기?")
            Spacer(Modifier.height(10.dp))
            Text(
                text = state.currentTopic,
                color = CatchupColors.Ink,
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        Spacer(Modifier.height(16.dp))
        TranscriptPreview(lines = state.recentTranscript)

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCurrentIndex) {
                Text(
                    text = "현재 지수 보기",
                    color = CatchupColors.Primary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        ActionButton(
            text = "방금 1분 요약",
            containerColor = CatchupColors.Primary,
            contentColor = Color(0xFF041313),
            onClick = onRecentSummary,
        )
        Spacer(Modifier.height(10.dp))
        ActionButton(
            text = "종료하고 정리",
            containerColor = CatchupColors.Secondary,
            onClick = onFinish,
        )
        Spacer(Modifier.height(10.dp))
        ActionButton(
            text = "기록 보기",
            containerColor = CatchupColors.SurfaceMuted,
            contentColor = CatchupColors.Ink,
            onClick = onHistory,
        )
    }
}

@Composable
private fun RecentSummaryScreen(
    state: MainUiState,
    onBack: () -> Unit,
) {
    ScrollPage(
        title = "방금 1분 요약",
        onBack = onBack,
    ) {
        SimpleCard(containerColor = CatchupColors.SurfaceRaised) {
            BulletList(items = state.recentOneMinuteSummary)
        }

        Spacer(Modifier.height(14.dp))
        SimpleCard(containerColor = CatchupColors.PrimarySoft) {
            Text(
                text = "일부 구간은 소음으로 누락될 수 있어요",
                color = CatchupColors.Ink,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("방금 지나간 자막")
        Spacer(Modifier.height(8.dp))
        state.recentTranscript.forEach { line ->
            TranscriptLineRow(line = line)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(20.dp))
        ActionButton(
            text = "라이브로 돌아가기",
            containerColor = CatchupColors.Primary,
            contentColor = Color(0xFF041313),
            onClick = onBack,
        )
    }
}

@Composable
private fun CurrentIndexScreen(
    indices: List<IndexQuote>,
    statusLabel: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
) {
    ScrollPage(
        title = "현재 지수",
        subtitle = "원할 때만 보기",
        onBack = onBack,
    ) {
        SimpleCard(containerColor = CatchupColors.PrimarySoft) {
            Text(
                text = statusLabel,
                color = CatchupColors.Ink,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(Modifier.height(12.dp))
        indices.forEach { quote ->
            IndexQuoteCard(quote = quote)
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(12.dp))
        ActionButton(
            text = if (isRefreshing) "가져오는 중..." else "새로고침",
            containerColor = CatchupColors.SurfaceMuted,
            contentColor = CatchupColors.Ink,
            onClick = onRefresh,
        )
        Spacer(Modifier.height(10.dp))
        ActionButton(
            text = "닫기",
            containerColor = CatchupColors.Primary,
            contentColor = Color(0xFF041313),
            onClick = onBack,
        )
    }
}

@Composable
private fun HistoryScreen(
    history: List<BroadcastSession>,
    onBack: () -> Unit,
    onOpenBroadcast: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CatchupColors.Page,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
        ) {
            TopBar(title = "기록", onBack = onBack)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = history,
                    key = { it.id },
                ) { broadcast ->
                    HistoryCard(
                        broadcast = broadcast,
                        onClick = { onOpenBroadcast(broadcast.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BroadcastDetailScreen(
    broadcast: BroadcastSession?,
    onBack: () -> Unit,
) {
    ScrollPage(
        title = "방송 상세",
        onBack = onBack,
    ) {
        if (broadcast == null) {
            Text(
                text = "볼 수 있는 기록이 아직 없습니다.",
                color = CatchupColors.Ink,
                style = MaterialTheme.typography.bodyLarge,
            )
            return@ScrollPage
        }

        Text(
            text = broadcast.title,
            color = CatchupColors.Ink,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${broadcast.dateLabel} · ${broadcast.durationLabel}",
            color = CatchupColors.InkMuted,
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(18.dp))
        SectionTitle("최종 요약")
        Spacer(Modifier.height(8.dp))
        SimpleCard {
            Text(
                text = broadcast.finalSummary,
                color = CatchupColors.Ink,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("주요 흐름")
        Spacer(Modifier.height(8.dp))
        SimpleCard {
            broadcast.timeline.forEach { item ->
                TimelineRow(time = item.time, title = item.title)
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("저신뢰 구간")
        Spacer(Modifier.height(8.dp))
        SimpleCard {
            if (broadcast.lowConfidenceRanges.isEmpty()) {
                Text(
                    text = "특별히 낮게 인식된 구간이 없습니다.",
                    color = CatchupColors.Ink,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                BulletList(items = broadcast.lowConfidenceRanges)
            }
        }
    }
}

@Composable
private fun ScrollPage(
    title: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CatchupColors.Page,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
        ) {
            if (title != null && onBack != null) {
                TopBar(title = title, subtitle = subtitle, onBack = onBack)
            }
            content()
        }
    }
}

@Composable
private fun LiveHeader(
    status: String,
    elapsed: String,
) {
    Column {
        Text(
            text = "주식방송 캐치업",
            color = CatchupColors.Ink,
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(12.dp))
        StatusPill(status = status, elapsed = elapsed)
    }
}

@Composable
private fun TopBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(
                text = "뒤로",
                color = CatchupColors.Primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Text(
            text = title,
            color = CatchupColors.Ink,
            style = MaterialTheme.typography.headlineLarge,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = CatchupColors.InkMuted,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun StatusPill(
    status: String,
    elapsed: String,
) {
    Row(
        modifier = Modifier
            .background(CatchupColors.PrimarySoft, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(CatchupColors.Positive),
        )
        Text(
            text = "$status $elapsed",
            color = CatchupColors.Ink,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun TranscriptPreview(lines: List<TranscriptLine>) {
    SimpleCard {
        SectionTitle("실시간 자막")
        Spacer(Modifier.height(10.dp))
        lines.forEachIndexed { index, line ->
            TranscriptLineRow(line = line)
            if (index != lines.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = CatchupColors.SurfaceMuted,
                )
            }
        }
    }
}

@Composable
private fun TranscriptLineRow(line: TranscriptLine) {
    Column {
        Text(
            text = line.time,
            color = CatchupColors.Primary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = line.text,
            color = CatchupColors.Ink,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = CatchupColors.Primary,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun SimpleCard(
    containerColor: Color = CatchupColors.Surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        content = {
            Column(
                modifier = Modifier.padding(18.dp),
                content = content,
            )
        },
    )
}

@Composable
private fun ActionButton(
    text: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun BulletList(items: List<String>) {
    items.forEachIndexed { index, item ->
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "•",
                color = CatchupColors.Primary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = item,
                color = CatchupColors.Ink,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
        }
        if (index != items.lastIndex) {
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun IndexQuoteCard(quote: IndexQuote) {
    SimpleCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quote.name,
                    color = CatchupColors.InkMuted,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = quote.value,
                    color = CatchupColors.Ink,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            Text(
                text = if (quote.isUp) "▲ ${quote.change}" else "▼ ${quote.change}",
                color = if (quote.isUp) CatchupColors.Positive else CatchupColors.Danger,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun HistoryCard(
    broadcast: BroadcastSession,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CatchupColors.Surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = broadcast.title,
                color = CatchupColors.Ink,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${broadcast.dateLabel} · ${broadcast.durationLabel}",
                color = CatchupColors.InkMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = broadcast.topicLine,
                color = CatchupColors.Primary,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun TimelineRow(
    time: String,
    title: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = time,
            color = CatchupColors.Primary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 14.dp),
        )
        Text(
            text = title,
            color = CatchupColors.Ink,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
    }
}
