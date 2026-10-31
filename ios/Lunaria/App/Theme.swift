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
        case .period: L("Period")
        case .predictedPeriod: L("Predicted")
        case .ovulation: L("Ovulation")
        case .fertile: L("Fertile Days")
        case .safe: L("Safe Days")
        case .unknown: ""
        }
    }
}

extension CyclePhase {
    var title: String {
        switch self {
        case .menstrual: L("Menstrual phase")
        case .follicular: L("Follicular phase")
        case .fertile: L("Fertile window")
        case .luteal: L("Luteal phase")
        }
    }

    var blurb: String {
        switch self {
        case .menstrual: L("Rest, hydrate and keep warm. Energy is usually lower.")
        case .follicular: L("Estrogen is rising — energy and mood often improve.")
        case .fertile: L("Highest chance of pregnancy in this window.")
        case .luteal: L("Progesterone peaks. PMS symptoms may appear near the end.")
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
