import Foundation

enum AppScreen {
    case live
    case recentSummary
    case currentIndex
    case catchupAlerts
    case history
    case detail
}

struct TranscriptLine: Identifiable, Equatable {
    let id = UUID()
    let time: String
    let text: String
}

struct IndexQuote: Identifiable, Equatable {
    let id = UUID()
    let name: String
    let value: String
    let change: String
    let isUp: Bool
}

struct CatchupAlert: Identifiable, Equatable {
    let id = UUID()
    let time: String
    let title: String
    let message: String
    var isRead: Bool = false
}

struct TimelineItem: Identifiable, Equatable {
    let id = UUID()
    let time: String
    let title: String
}

struct BroadcastSession: Identifiable, Equatable {
    let id = UUID()
    let title: String
    let dateLabel: String
    let durationLabel: String
    let topicLine: String
    let finalSummary: String
    let timeline: [TimelineItem]
    let lowConfidenceRanges: [String]
}
