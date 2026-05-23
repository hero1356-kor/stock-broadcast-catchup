import SwiftUI

struct ContentView: View {
    @EnvironmentObject private var vm: AppViewModel

    var body: some View {
        Group {
            switch vm.screen {
            case .live: LiveView()
            case .recentSummary: RecentSummaryView()
            case .currentIndex: CurrentIndexView()
            case .catchupAlerts: CatchupAlertsView()
            case .history: HistoryView()
            case .detail: DetailView()
            }
        }
        .background(AppColors.page.ignoresSafeArea())
        .preferredColorScheme(.dark)
    }
}

struct LiveView: View {
    @EnvironmentObject private var vm: AppViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text("주식방송 캐치업")
                    .font(.largeTitle.bold())
                    .foregroundStyle(AppColors.ink)
                StatusPill(status: vm.listeningStatus, elapsed: vm.elapsedLabel)

                SttCard()
                MicQualityCard()

                MainButton("방금 1분 요약", color: AppColors.primary, textColor: .black) {
                    vm.showRecentSummary()
                }

                AppCard(color: AppColors.raised) {
                    SectionTitle("지금 무슨 얘기?")
                    Text(vm.currentTopic)
                        .font(.title2.bold())
                        .foregroundStyle(AppColors.ink)
                }

                AppCard(color: AppColors.raised) {
                    HStack {
                        SectionTitle("요약카드")
                        Spacer()
                        Button("자세히") { vm.showRecentSummary() }
                            .foregroundStyle(AppColors.primary)
                    }
                    BulletList(items: Array(vm.recentOneMinuteSummary.prefix(2)))
                }

                AppCard(color: AppColors.primarySoft) {
                    HStack {
                        SectionTitle("캐치업")
                        Spacer()
                        Button("보기") { vm.showCatchupAlerts() }
                            .foregroundStyle(AppColors.primary)
                    }
                    Text(vm.unreadCatchupCount > 0 ? "\(vm.unreadCatchupCount)개 새 알림" : "새 알림 없음")
                        .foregroundStyle(AppColors.ink)
                    Text(vm.catchupAlerts.first?.title ?? "중요한 방송 흐름이 잡히면 여기에 쌓입니다.")
                        .foregroundStyle(AppColors.muted)
                }

                AppCard {
                    SectionTitle("실시간 자막")
                    ForEach(vm.recentTranscript) { line in
                        TranscriptRow(line: line)
                        Divider().background(AppColors.muted.opacity(0.3))
                    }
                }

                HStack {
                    Spacer()
                    Button("현재 지수 보기") { vm.showCurrentIndex() }
                        .foregroundStyle(AppColors.primary)
                        .font(.headline)
                }

                MainButton("캐치업 알림 보기", color: AppColors.surface) { vm.showCatchupAlerts() }
                MainButton("종료하고 정리", color: AppColors.secondary) { vm.finishAndShowDetail() }
                MainButton("기록 보기", color: AppColors.surface) { vm.showHistory() }
            }
            .padding(22)
        }
    }
}

struct SttCard: View {
    @EnvironmentObject private var vm: AppViewModel

    var body: some View {
        AppCard(color: AppColors.primarySoft) {
            SectionTitle("STT")
            Text(vm.sttStatusLabel).foregroundStyle(AppColors.ink)
            MainButton(vm.isSttListening ? "STT 중지" : "STT 시작", color: vm.isSttListening ? AppColors.secondary : AppColors.primary, textColor: vm.isSttListening ? .white : .black) {
                vm.isSttListening ? vm.stopStt() : vm.startStt()
            }
        }
    }
}

struct MicQualityCard: View {
    @EnvironmentObject private var vm: AppViewModel

    var body: some View {
        let status = vm.isSttListening && vm.recentTranscript.isEmpty
            ? "듣는 중인데 자막이 안 뜨면 TV 소리가 멀거나 작게 들어오는 상태일 수 있어요."
            : "TV 소리 테스트 전에 STT 시작을 누르고 아래 조건을 확인해보세요."

        AppCard(color: AppColors.raised) {
            SectionTitle("마이크 품질 체크")
            Text(status).foregroundStyle(AppColors.ink)
            BulletList(items: [
                "iPhone/iPad를 TV 스피커 30cm~1m 근처에 둬보세요.",
                "TV 볼륨은 중간 이상으로, 너무 크면 왜곡될 수 있어요.",
                "배경음악이 적고 말소리가 또렷한 장면으로 먼저 테스트하세요.",
                "Android와 같은 방송, 같은 거리, 같은 볼륨으로 비교하세요."
            ])
        }
    }
}

struct RecentSummaryView: View {
    @EnvironmentObject private var vm: AppViewModel

    var body: some View {
        ScreenPage(title: "방금 1분 요약") {
            AppCard(color: AppColors.raised) { BulletList(items: vm.recentOneMinuteSummary) }
            AppCard(color: AppColors.primarySoft) { Text("일부 구간은 소음으로 누락될 수 있어요").foregroundStyle(AppColors.ink) }
            SectionTitle("방금 지나간 자막")
            ForEach(vm.recentTranscript) { TranscriptRow(line: $0) }
            MainButton("라이브로 돌아가기", color: AppColors.primary, textColor: .black) { vm.showLive() }
        }
    }
}

struct CurrentIndexView: View {
    @EnvironmentObject private var vm: AppViewModel

    var body: some View {
        ScreenPage(title: "현재 지수", subtitle: "원할 때만 보기") {
            ForEach(vm.currentIndices) { quote in
                AppCard {
                    HStack {
                        VStack(alignment: .leading) {
                            Text(quote.name).foregroundStyle(AppColors.muted)
                            Text(quote.value).font(.title.bold()).foregroundStyle(AppColors.ink)
                        }
                        Spacer()
                        Text((quote.isUp ? "▲ " : "▼ ") + quote.change)
                            .font(.title3.bold())
                            .foregroundStyle(quote.isUp ? AppColors.positive : AppColors.danger)
                    }
                }
            }
            MainButton("닫기", color: AppColors.primary, textColor: .black) { vm.showLive() }
        }
    }
}

struct CatchupAlertsView: View {
    @EnvironmentObject private var vm: AppViewModel

    var body: some View {
        ScreenPage(title: "캐치업 알림", subtitle: "놓친 방송 흐름") {
            AppCard(color: AppColors.primarySoft) {
                Text(vm.unreadCatchupCount > 0 ? "새로 잡힌 항목 \(vm.unreadCatchupCount)개가 있습니다." : "확인하지 않은 항목은 없습니다.")
                    .foregroundStyle(AppColors.ink)
            }
            ForEach(vm.catchupAlerts) { alert in
                AppCard(color: alert.isRead ? AppColors.surface : AppColors.raised) {
                    Text(alert.time).foregroundStyle(AppColors.primary)
                    Text(alert.title).font(.title3.bold()).foregroundStyle(AppColors.ink)
                    Text(alert.message).foregroundStyle(AppColors.muted)
                }
            }
            if vm.unreadCatchupCount > 0 {
                MainButton("모두 확인", color: AppColors.surface) { vm.markAllCatchupAlertsRead() }
            }
            MainButton("라이브로 돌아가기", color: AppColors.primary, textColor: .black) { vm.showLive() }
        }
    }
}

struct HistoryView: View {
    @EnvironmentObject private var vm: AppViewModel

    var body: some View {
        ScreenPage(title: "기록") {
            ForEach(vm.history) { item in
                Button { vm.openBroadcast(item) } label: {
                    AppCard {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(item.title).font(.title3.bold()).foregroundStyle(AppColors.ink)
                            Text("\(item.dateLabel) · \(item.durationLabel)").foregroundStyle(AppColors.muted)
                            Text(item.topicLine).foregroundStyle(AppColors.primary)
                        }
                    }
                }
            }
        }
    }
}

struct DetailView: View {
    @EnvironmentObject private var vm: AppViewModel

    var body: some View {
        ScreenPage(title: "방송 상세") {
            if let item = vm.selectedBroadcast {
                Text(item.title).font(.title.bold()).foregroundStyle(AppColors.ink)
                Text("\(item.dateLabel) · \(item.durationLabel)").foregroundStyle(AppColors.muted)
                SectionTitle("최종 요약")
                AppCard { Text(item.finalSummary).foregroundStyle(AppColors.ink) }
                SectionTitle("주요 흐름")
                AppCard {
                    ForEach(item.timeline) { row in
                        HStack(alignment: .top) {
                            Text(row.time).foregroundStyle(AppColors.primary)
                            Text(row.title).foregroundStyle(AppColors.ink)
                        }
                    }
                }
                SectionTitle("저신뢰 구간")
                AppCard { BulletList(items: item.lowConfidenceRanges) }
            }
        }
    }
}

struct ScreenPage<Content: View>: View {
    let title: String
    var subtitle: String? = nil
    @ViewBuilder let content: Content
    @EnvironmentObject private var vm: AppViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Button("뒤로") { vm.showLive() }.foregroundStyle(AppColors.primary)
                Text(title).font(.largeTitle.bold()).foregroundStyle(AppColors.ink)
                if let subtitle { Text(subtitle).foregroundStyle(AppColors.muted) }
                content
            }
            .padding(22)
        }
    }
}

struct AppCard<Content: View>: View {
    var color: Color = AppColors.surface
    @ViewBuilder let content: Content
    var body: some View { VStack(alignment: .leading, spacing: 10) { content }.padding(18).frame(maxWidth: .infinity, alignment: .leading).background(color).clipShape(RoundedRectangle(cornerRadius: 10)) }
}

struct MainButton: View {
    let text: String
    var color: Color
    var textColor: Color = .white
    let action: () -> Void
    init(_ text: String, color: Color, textColor: Color = .white, action: @escaping () -> Void) { self.text = text; self.color = color; self.textColor = textColor; self.action = action }
    var body: some View { Button(action: action) { Text(text).font(.headline).frame(maxWidth: .infinity).frame(height: 56).background(color).foregroundStyle(textColor).clipShape(RoundedRectangle(cornerRadius: 10)) } }
}

struct SectionTitle: View { let text: String; init(_ text: String) { self.text = text }; var body: some View { Text(text).font(.headline).foregroundStyle(AppColors.primary) } }
struct StatusPill: View { let status: String; let elapsed: String; var body: some View { Text("● \(status) \(elapsed)").font(.headline).padding(.horizontal, 14).padding(.vertical, 9).background(AppColors.primarySoft).clipShape(RoundedRectangle(cornerRadius: 8)).foregroundStyle(AppColors.ink) } }
struct TranscriptRow: View { let line: TranscriptLine; var body: some View { VStack(alignment: .leading, spacing: 4) { Text(line.time).foregroundStyle(AppColors.primary).font(.caption.bold()); Text(line.text).foregroundStyle(AppColors.ink) } } }
struct BulletList: View { let items: [String]; var body: some View { VStack(alignment: .leading, spacing: 8) { ForEach(items, id: \.self) { Text("• \($0)").foregroundStyle(AppColors.ink) } } } }

enum AppColors {
    static let page = Color(red: 0.03, green: 0.06, blue: 0.08)
    static let surface = Color(red: 0.07, green: 0.11, blue: 0.15)
    static let raised = Color(red: 0.10, green: 0.15, blue: 0.19)
    static let primarySoft = Color(red: 0.05, green: 0.20, blue: 0.20)
    static let primary = Color(red: 0.13, green: 0.78, blue: 0.74)
    static let secondary = Color(red: 0.34, green: 0.20, blue: 0.66)
    static let ink = Color(red: 0.96, green: 0.98, blue: 0.99)
    static let muted = Color(red: 0.65, green: 0.70, blue: 0.75)
    static let positive = Color(red: 0.21, green: 0.82, blue: 0.56)
    static let danger = Color(red: 0.95, green: 0.35, blue: 0.35)
}
