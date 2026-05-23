import Foundation

@MainActor
final class AppViewModel: ObservableObject {
    @Published var screen: AppScreen = .live
    @Published var listeningStatus = "데모 재생 중"
    @Published var elapsedLabel = "00:00:00"
    @Published var currentTopic = "샘플 방송 자막을 재생하고 있습니다."
    @Published var recentTranscript: [TranscriptLine] = []
    @Published var recentOneMinuteSummary: [String] = ["샘플 자막이 들어오면 최근 1분 요약이 갱신됩니다."]
    @Published var currentIndices: [IndexQuote] = SampleData.indices
    @Published var catchupAlerts: [CatchupAlert] = []
    @Published var history: [BroadcastSession] = SampleData.history
    @Published var selectedBroadcast: BroadcastSession? = SampleData.history.first
    @Published var sttStatusLabel = "마이크 권한을 확인한 뒤 STT를 시작할 수 있습니다."
    @Published var isSttListening = false
    @Published var isDemoRunning = false

    private var demoTask: Task<Void, Never>?
    private let speechService = SpeechRecognizerService()

    init() {
        startDemo()
    }

    var unreadCatchupCount: Int {
        catchupAlerts.filter { !$0.isRead }.count
    }

    func showLive() { screen = .live }
    func showRecentSummary() { screen = .recentSummary }
    func showCurrentIndex() { screen = .currentIndex }
    func showCatchupAlerts() { screen = .catchupAlerts }
    func showHistory() { screen = .history }

    func finishAndShowDetail() {
        selectedBroadcast = history.first
        screen = .detail
    }

    func openBroadcast(_ item: BroadcastSession) {
        selectedBroadcast = item
        screen = .detail
    }

    func markAllCatchupAlertsRead() {
        catchupAlerts = catchupAlerts.map { item in
            var next = item
            next.isRead = true
            return next
        }
    }

    func startDemo() {
        stopStt()
        demoTask?.cancel()
        isDemoRunning = true
        listeningStatus = "데모 재생 중"
        elapsedLabel = "00:00:00"
        currentTopic = "샘플 방송 자막을 재생하고 있습니다."
        recentTranscript = []
        catchupAlerts = []
        recentOneMinuteSummary = ["샘플 자막이 들어오면 최근 1분 요약이 갱신됩니다."]

        demoTask = Task { [weak self] in
            for line in SampleData.demoTranscript {
                if Task.isCancelled { return }
                try? await Task.sleep(nanoseconds: 500_000_000)
                await MainActor.run { self?.applyTranscript(line) }
            }
            await MainActor.run {
                self?.isDemoRunning = false
                self?.listeningStatus = "데모 완료"
            }
        }
    }

    func stopDemo() {
        demoTask?.cancel()
        demoTask = nil
        isDemoRunning = false
        listeningStatus = "데모 중지"
    }

    func startStt() {
        stopDemo()
        isSttListening = true
        listeningStatus = "STT 듣는 중"
        sttStatusLabel = "iPhone 또는 iPad 마이크로 소리를 듣고 있습니다."
        speechService.start { [weak self] text in
            Task { @MainActor in
                self?.applyTranscript(TranscriptLine(time: Self.nowLabel(), text: text))
            }
        } onError: { [weak self] message in
            Task { @MainActor in
                self?.isSttListening = false
                self?.listeningStatus = "STT 오류"
                self?.sttStatusLabel = message
            }
        }
    }

    func stopStt() {
        speechService.stop()
        isSttListening = false
        if listeningStatus == "STT 듣는 중" {
            listeningStatus = "STT 중지"
        }
    }

    private func applyTranscript(_ line: TranscriptLine) {
        elapsedLabel = line.time
        recentTranscript.insert(line, at: 0)
        recentTranscript = Array(recentTranscript.prefix(12))
        currentTopic = inferCurrentTopic()
        recentOneMinuteSummary = summarize()
        if let alert = buildAlert(line) {
            catchupAlerts.insert(alert, at: 0)
            catchupAlerts = Array(catchupAlerts.prefix(5))
        }
    }

    private func inferCurrentTopic() -> String {
        let joined = recentTranscript.map { $0.text }.joined(separator: " ")
        if joined.contains("엔비디아") || joined.contains("반도체") {
            return "반도체와 엔비디아 실적 흐름을 이야기하는 중입니다."
        }
        if joined.contains("금리") || joined.contains("채권") {
            return "금리와 시장 변동성을 이야기하는 중입니다."
        }
        return recentTranscript.first?.text ?? "방송 자막을 기다리는 중입니다."
    }

    private func summarize() -> [String] {
        let latest = recentTranscript.prefix(3).map { $0.text }
        if latest.isEmpty { return ["아직 요약할 자막이 없습니다."] }
        return ["최근 자막 \(latest.count)개 기준으로 흐름을 갱신했습니다.", "방금 인식한 문장: \(latest.first ?? "-")"]
    }

    private func buildAlert(_ line: TranscriptLine) -> CatchupAlert? {
        if line.text.contains("엔비디아") || line.text.contains("실적") {
            return CatchupAlert(time: line.time, title: "실적/반도체 이슈", message: line.text)
        }
        if line.text.contains("금리") || line.text.contains("채권") {
            return CatchupAlert(time: line.time, title: "금리 이슈", message: line.text)
        }
        return nil
    }

    private static func nowLabel() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss"
        return formatter.string(from: Date())
    }
}
