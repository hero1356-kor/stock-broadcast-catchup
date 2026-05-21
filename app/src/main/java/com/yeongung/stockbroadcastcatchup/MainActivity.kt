package com.yeongung.stockbroadcastcatchup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StockBroadcastCatchupApp()
        }
    }
}

enum class AppScreen { Live, RecentSummary, Indices, History, Detail }

data class TranscriptLine(val time: String, val text: String)
data class MarketIndex(val name: String, val value: String, val change: String)
data class BroadcastRecord(val title: String, val subtitle: String, val duration: String)

data class AppUiState(
    val screen: AppScreen = AppScreen.Live,
    val elapsed: String = "00:12:34",
    val currentSummary: String = "반도체, 엔비디아 실적, 금리 부담을 이야기하는 중입니다.",
    val transcriptLines: List<TranscriptLine> = listOf(
        TranscriptLine("00:12:31", "반도체 업종이 강세를 보이고 있어요."),
        TranscriptLine("00:12:26", "엔비디아 실적이 시장 기대를 상회했습니다."),
        TranscriptLine("00:12:21", "금리 부담이 지속되며 변동성이 커지고 있습니다.")
    ),
    val recentSummary: List<String> = listOf(
        "반도체 업종이 강세를 보이며 시장 주도 흐름을 이끌고 있어요.",
        "엔비디아 실적이 시장 기대를 상회해 투자 심리가 개선되었습니다.",
        "금리 부담이 지속되며 변동성이 커지고 있습니다.",
        "단기적으로는 실적과 금리 방향에 따라 등락이 이어질 수 있어요."
    ),
    val indices: List<MarketIndex> = listOf(
        MarketIndex("코스피", "2,655.42", "▲ 0.68%"),
        MarketIndex("코스닥", "856.21", "▲ 0.62%"),
        MarketIndex("나스닥", "17,689.36", "▲ 0.91%"),
        MarketIndex("USD/KRW", "1,382.10", "▼ 0.21%")
    ),
    val records: List<BroadcastRecord> = listOf(
        BroadcastRecord("5월 21일 오전 장중 시황", "반도체 / 금리 / 외국인 수급", "52분"),
        BroadcastRecord("엔비디아 실적 점검 라이브", "반도체 / 미국증시", "38분"),
        BroadcastRecord("마감 시황 정리", "코스피 / 환율 / 2차전지", "44분")
    )
)

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState

    fun goTo(screen: AppScreen) {
        _uiState.update { it.copy(screen = screen) }
    }
}

@Composable
fun StockBroadcastCatchupApp(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    MaterialTheme {
        Surface(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            when (uiState.screen) {
                AppScreen.Live -> LiveScreen(uiState, viewModel::goTo)
                AppScreen.RecentSummary -> RecentSummaryScreen(uiState, viewModel::goTo)
                AppScreen.Indices -> IndicesScreen(uiState, viewModel::goTo)
                AppScreen.History -> HistoryScreen(uiState, viewModel::goTo)
                AppScreen.Detail -> DetailScreen(uiState, viewModel::goTo)
            }
        }
    }
}

@Composable
fun LiveScreen(uiState: AppUiState, goTo: (AppScreen) -> Unit) {
    AppScaffold {
        Header(title = "주식방송 캐치업", trailing = "≡")
        StatusPill("● 듣는 중 ${uiState.elapsed}")
        SpacerHeight(24)

        BigSummaryCard(
            title = "지금 무슨 얘기?",
            text = uiState.currentSummary
        )

        SpacerHeight(16)
        CardBlock(title = "실시간 자막") {
            uiState.transcriptLines.forEach { line ->
                Row(modifier = androidx.compose.ui.Modifier.padding(vertical = 6.dp)) {
                    Text(line.time, color = Accent, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = androidx.compose.ui.Modifier.width(12.dp))
                    Text(line.text, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        SpacerHeight(12)
        TextButton(onClick = { goTo(AppScreen.Indices) }) {
            Text("현재 지수 보기", color = TextPrimary)
        }

        Spacer(modifier = androidx.compose.ui.Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryActionButton("방금 1분 요약", modifier = androidx.compose.ui.Modifier.weight(1f)) {
                goTo(AppScreen.RecentSummary)
            }
            PrimaryActionButton("종료하고 정리", modifier = androidx.compose.ui.Modifier.weight(1f)) {
                goTo(AppScreen.Detail)
            }
        }
        SpacerHeight(10)
        SecondaryActionButton("기록 보기", modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
            goTo(AppScreen.History)
        }
    }
}

@Composable
fun RecentSummaryScreen(uiState: AppUiState, goTo: (AppScreen) -> Unit) {
    AppScaffold {
        BackHeader("방금 1분 요약") { goTo(AppScreen.Live) }
        SpacerHeight(24)
        CardBlock(title = "요약") {
            uiState.recentSummary.forEach { item ->
                Text("• $item", color = TextPrimary, style = MaterialTheme.typography.bodyLarge, modifier = androidx.compose.ui.Modifier.padding(vertical = 8.dp))
            }
        }
        SpacerHeight(12)
        Text("일부 구간은 소음으로 누락될 수 있어요", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = androidx.compose.ui.Modifier.weight(1f))
        PrimaryActionButton("라이브로 돌아가기", modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
            goTo(AppScreen.Live)
        }
    }
}

@Composable
fun IndicesScreen(uiState: AppUiState, goTo: (AppScreen) -> Unit) {
    AppScaffold {
        BackHeader("현재 지수") { goTo(AppScreen.Live) }
        Text("원할 때만 보기", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        SpacerHeight(24)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            uiState.indices.chunked(2).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { index ->
                        IndexCard(index, modifier = androidx.compose.ui.Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(modifier = androidx.compose.ui.Modifier.weight(1f))
        SecondaryActionButton("닫기", modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
            goTo(AppScreen.Live)
        }
    }
}

@Composable
fun HistoryScreen(uiState: AppUiState, goTo: (AppScreen) -> Unit) {
    AppScaffold {
        BackHeader("기록") { goTo(AppScreen.Live) }
        SpacerHeight(16)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(uiState.records) { record ->
                Card(
                    onClick = { goTo(AppScreen.Detail) },
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = androidx.compose.ui.Modifier.padding(18.dp)) {
                        Text(record.title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                        SpacerHeight(6)
                        Text(record.subtitle, color = TextMuted)
                        SpacerHeight(6)
                        Text(record.duration, color = Accent)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailScreen(uiState: AppUiState, goTo: (AppScreen) -> Unit) {
    AppScaffold {
        BackHeader("방송 상세") { goTo(AppScreen.Live) }
        SpacerHeight(16)
        CardBlock(title = "최종 요약") {
            Text("반도체 업종의 수급과 엔비디아 실적, 미국채 금리 부담이 주요 주제였습니다. 단기적으로는 실적 기대와 금리 방향에 따라 시장 변동성이 이어질 수 있다는 내용으로 정리됩니다.", color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
        }
        SpacerHeight(16)
        CardBlock(title = "타임라인") {
            listOf("09:03 반도체 업종 전망", "09:12 미국채 금리 영향", "09:25 코스피 단기 과열 논의", "09:38 엔비디아 실적 관련 코멘트").forEach {
                Text("• $it", color = TextPrimary, modifier = androidx.compose.ui.Modifier.padding(vertical = 6.dp))
            }
        }
        SpacerHeight(16)
        CardBlock(title = "저신뢰 구간") {
            Text("09:21 ~ 09:23 인식 품질이 낮아 내용이 일부 누락될 수 있습니다.", color = Warning)
        }
    }
}

@Composable
fun AppScaffold(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 22.dp, vertical = 28.dp),
        content = content
    )
}

@Composable
fun Header(title: String, trailing: String) {
    Row(modifier = androidx.compose.ui.Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(trailing, color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
        Text(title, color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
        Text("▥", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
fun BackHeader(title: String, onBack: () -> Unit) {
    Row(modifier = androidx.compose.ui.Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onBack) { Text("←", color = TextPrimary, style = MaterialTheme.typography.headlineMedium) }
        Text(title, color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = androidx.compose.ui.Modifier.width(48.dp))
    }
}

@Composable
fun StatusPill(text: String) {
    Box(modifier = androidx.compose.ui.Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text(
            text = text,
            color = TextPrimary,
            modifier = androidx.compose.ui.Modifier
                .background(RecordingRed.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
                .padding(horizontal = 18.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun BigSummaryCard(title: String, text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = androidx.compose.ui.Modifier.padding(22.dp)) {
            Text(title, color = Accent, style = MaterialTheme.typography.titleLarge)
            SpacerHeight(14)
            Text(text, color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
fun CardBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(22.dp), modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
        Column(modifier = androidx.compose.ui.Modifier.padding(18.dp)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            SpacerHeight(10)
            content()
        }
    }
}

@Composable
fun IndexCard(index: MarketIndex, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(20.dp), modifier = modifier) {
        Column(modifier = androidx.compose.ui.Modifier.padding(18.dp)) {
            Text(index.name, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            SpacerHeight(12)
            Text(index.value, color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
            SpacerHeight(8)
            Text(index.change, color = if (index.change.startsWith("▼")) LossRed else Accent)
        }
    }
}

@Composable
fun PrimaryActionButton(text: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(58.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent), shape = RoundedCornerShape(16.dp)) {
        Text(text, color = TextPrimary)
    }
}

@Composable
fun SecondaryActionButton(text: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(58.dp), colors = ButtonDefaults.buttonColors(containerColor = ButtonDark), shape = RoundedCornerShape(16.dp)) {
        Text(text, color = TextPrimary)
    }
}

@Composable
fun SpacerHeight(height: Int) {
    Spacer(modifier = androidx.compose.ui.Modifier.height(height.dp))
}

private val DarkBackground = androidx.compose.ui.graphics.Color(0xFF071016)
private val CardBackground = androidx.compose.ui.graphics.Color(0xFF121D26)
private val ButtonDark = androidx.compose.ui.graphics.Color(0xFF1B2732)
private val TextPrimary = androidx.compose.ui.graphics.Color(0xFFF5F7FA)
private val TextMuted = androidx.compose.ui.graphics.Color(0xFFAAB3BD)
private val Accent = androidx.compose.ui.graphics.Color(0xFF20C7BE)
private val Warning = androidx.compose.ui.graphics.Color(0xFFFFC857)
private val RecordingRed = androidx.compose.ui.graphics.Color(0xFFE5484D)
private val LossRed = androidx.compose.ui.graphics.Color(0xFFFF6B6B)

private val Int.dp: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.dp
