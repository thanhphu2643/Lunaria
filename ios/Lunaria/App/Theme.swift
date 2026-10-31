import SwiftUI
import LunariaCore

enum Theme {
    static let accent = Color(hex: 0xE0607E)
    static let period = Color(hex: 0xE8687C)
    static let ovulation = Color(hex: 0xFBE3E8)
    static let fertile = Color(hex: 0xE4EEFB)
    static let safeStroke = Color(hex: 0xEDB3BF)
    static let background = Color(hex: 0xFBE6EB)
    static let card = Color.white
    static let text = Color(hex: 0x2B2B2E)
    static let secondaryText = Color(hex: 0x6E6E73)
    static let iconBubble = Color(hex: 0xF4C9D3)
}

extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}

extension DayKind {
    var label: String {
        switch self {
        case .period: "Period"
        case .predictedPeriod: "Predicted"
        case .ovulation: "Ovulation"
        case .fertile: "Fertile Days"
        case .safe: "Safe Days"
        case .unknown: ""
        }
    }
}

extension CyclePhase {
    var title: String {
        switch self {
        case .menstrual: "Menstrual phase"
        case .follicular: "Follicular phase"
        case .fertile: "Fertile window"
        case .luteal: "Luteal phase"
        }
    }

    var blurb: String {
        switch self {
        case .menstrual: "Rest, hydrate and keep warm. Energy is usually lower."
        case .follicular: "Estrogen is rising — energy and mood often improve."
        case .fertile: "Highest chance of pregnancy in this window."
        case .luteal: "Progesterone peaks. PMS symptoms may appear near the end."
        }
    }
}

/// Card container used across screens.
struct CardBackground: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding(20)
            .background(Theme.card, in: RoundedRectangle(cornerRadius: 28, style: .continuous))
            .shadow(color: Theme.accent.opacity(0.06), radius: 16, y: 6)
    }
}

extension View {
    func card() -> some View { modifier(CardBackground()) }
}
