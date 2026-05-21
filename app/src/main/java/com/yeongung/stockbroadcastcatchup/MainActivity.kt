package com.yeongung.stockbroadcastcatchup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StockBroadcastCatchupApp() }
    }
}

enum class Screen { Live, RecentSummary, Indices, History, Detail }

data class UiState(
    val screen: Screen = Screen.Live,
    val transcript: List<String> = listOf(
        "00:12:31  반도체 업종이 강세를 보이고 있어요.",
        "00:12:26  엔비디아 실적이 시장 기대를 상회했습니다.",
        "00:12:21  금리 부담으로 변동성이 커지고 있습니다."
    )
)

class MainViewModel : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state
    fun navigate(screen: Screen) = _state.update { it.copy(screen = screen) }
}

@Composable
fun StockBroadcastCatchupApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    MaterialTheme {
        Surface(color = Dark, modifier = Modifier.fillMaxSize()) {
            when (state.screen) {
                Screen.Live -> LiveScreen(state, viewModel::navigate)
                Screen.RecentSummary -> RecentSummaryScreen(viewModel::navigate)
                Screen.Indices -> IndicesScreen(viewModel::navigate)
                Screen.History -> HistoryScreen(viewModel::navigate)
                Screen.Detail -> DetailScreen(viewModel::navigate)
            }
        }
    }
}

@Composable
fun LiveScreen(state: UiState, navigate: (Screen) -> Unit) {
    Page {
        Text("주식방송 캐치업", color = TextMain, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Pill("● 듣는 중 00:12:34")
        Spacer(Modifier.height(24.dp))
        CardBox("지금 무슨 얘기?") {
            Text("반도체, 엔비디아 실적, 금리 부담을 이야기하는 중입니다.", color = TextMain, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(16.dp))
        CardBox("실시간 자막") {
            state.transcript.forEach { Text(it, color = TextMain, modifier = Modifier.padding(vertical = 5.dp)) }
        }
        TextButton(onClick = { navigate(Screen.Indices) }) { Text("현재 지수 보기", color = Accent) }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MainButton("방금 1분 요약", Modifier.weight(1f)) { navigate(Screen.RecentSummary) }
            MainButton("종료하고 정리", Modifier.weight(1f)) { navigate(Screen.Detail) }
        }
        Spacer(Modifier.height(10.dp))
        SubButton("기록 보기", Modifier.fillMaxWidth()) { navigate(Screen.History) }
    }
}

@Composable
fun RecentSummaryScreen(navigate: (Screen) -> Unit) {
    Page {
        BackTitle("방금 1분 요약") { navigate(Screen.Live) }
        CardBox("요약") {
            listOf(
                "반도체 업종이 시장 주도 흐름을 보였습니다.",
                "엔비디아 실적 기대가 투자 심리에 영향을 줬습니다.",
                "미국채 금리 부담으로 성장주 변동성이 언급됐습니다.",
                "일부 구간은 소음으로 누락될 수 있어요."
            ).forEach { Text("• $it", color = TextMain, modifier = Modifier.padding(vertical = 7.dp)) }
        }
        Spacer(Modifier.weight(1f))
        MainButton("라이브로 돌아가기", Modifier.fillMaxWidth()) { navigate(Screen.Live) }
    }
}

@Composable
fun IndicesScreen(navigate: (Screen) -> Unit) {
    Page {
        BackTitle("현재 지수") { navigate(Screen.Live) }
        Text("원할 때만 보기", color = TextSub)
        Spacer(Modifier.height(18.dp))
        listOf("코스피 2,655.42 ▲0.68%", "코스닥 856.21 ▲0.62%", "나스닥 17,689.36 ▲0.91%", "USD/KRW 1,382.10 ▼0.21%").forEach {
            CardBox("") { Text(it, color = TextMain, style = MaterialTheme.typography.titleLarge) }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.weight(1f))
        SubButton("닫기", Modifier.fillMaxWidth()) { navigate(Screen.Live) }
    }
}

@Composable
fun HistoryScreen(navigate: (Screen) -> Unit) {
    Page {
        BackTitle("기록") { navigate(Screen.Live) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(listOf("5월 21일 오전 장중 시황", "엔비디아 실적 점검 라이브", "마감 시황 정리")) { title ->
                Card(onClick = { navigate(Screen.Detail) }, colors = CardDefaults.cardColors(Card), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text(title, color = TextMain, style = MaterialTheme.typography.titleMedium)
                        Text("반도체 / 금리 / 외국인 수급", color = TextSub)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailScreen(navigate: (Screen) -> Unit) {
    Page {
        BackTitle("방송 상세") { navigate(Screen.Live) }
        CardBox("최종 요약") {
            Text("반도체 수급, 엔비디아 실적, 미국채 금리 부담이 주요 주제였습니다. 단기적으로는 금리와 실적 기대에 따라 변동성이 이어질 수 있다는 내용입니다.", color = TextMain)
        }
        Spacer(Modifier.height(14.dp))
        CardBox("타임라인") {
            listOf("09:03 반도체 업종 전망", "09:12 미국채 금리 영향", "09:25 코스피 단기 과열", "09:38 엔비디아 실적 코멘트").forEach {
                Text("• $it", color = TextMain, modifier = Modifier.padding(vertical = 5.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        CardBox("저신뢰 구간") { Text("09:21 ~ 09:23 인식 품질 낮음", color = Warn) }
    }
}

@Composable
fun Page(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Dark).padding(24.dp), content = content)
}

@Composable
fun BackTitle(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onBack) { Text("←", color = TextMain) }
        Text(title, color = TextMain, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(1.dp))
    }
    Spacer(Modifier.height(18.dp))
}

@Composable
fun Pill(text: String) {
    Text(text, color = TextMain, modifier = Modifier.background(Color(0x33E5484D), RoundedCornerShape(999.dp)).padding(horizontal = 16.dp, vertical = 8.dp))
}

@Composable
fun CardBox(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(Card), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            if (title.isNotBlank()) {
                Text(title, color = Accent, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

@Composable
fun MainButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(56.dp), colors = ButtonDefaults.buttonColors(Accent), shape = RoundedCornerShape(16.dp)) {
        Text(text, color = TextMain)
    }
}

@Composable
fun SubButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(56.dp), colors = ButtonDefaults.buttonColors(Color(0xFF1B2732)), shape = RoundedCornerShape(16.dp)) {
        Text(text, color = TextMain)
    }
}

private val Dark = Color(0xFF071016)
private val Card = Color(0xFF121D26)
private val TextMain = Color(0xFFF5F7FA)
private val TextSub = Color(0xFFAAB3BD)
private val Accent = Color(0xFF20C7BE)
private val Warn = Color(0xFFFFC857)
