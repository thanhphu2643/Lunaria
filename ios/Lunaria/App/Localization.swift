import Foundation

/// In-app language choice. Stored in UserDefaults so it survives launches.
enum AppLanguage: String, CaseIterable, Identifiable {
    case system, en, vi

    static let storageKey = "appLanguage"
    var id: String { rawValue }

    static var current: AppLanguage {
        AppLanguage(rawValue: UserDefaults.standard.string(forKey: storageKey) ?? "") ?? .system
    }

    /// Language actually used for strings: "en" or "vi".
    var resolved: String {
        switch self {
        case .en: "en"
        case .vi: "vi"
        case .system: Locale.preferredLanguages.first?.hasPrefix("vi") == true ? "vi" : "en"
        }
    }

    /// Locale for dates and numbers.
    var locale: Locale {
        if resolved == "vi" { return Locale(identifier: "vi_VN") }
        return self == .system ? .current : Locale(identifier: "en_\(Locale.current.region?.identifier ?? "US")")
    }

    var label: String {
        switch self {
        case .system: L("System")
        case .en: "English"
        case .vi: "Tiếng Việt"
        }
    }
}

private let viBundle = Bundle.main.path(forResource: "vi", ofType: "lproj").flatMap(Bundle.init(path:))

/// Localized string for the selected app language. English text is the key.
func L(_ key: String, _ args: CVarArg...) -> String {
    let lang = AppLanguage.current
    let format = lang.resolved == "vi" ? (viBundle?.localizedString(forKey: key, value: key, table: nil) ?? key) : key
    return args.isEmpty ? format : String(format: format, locale: lang.locale, arguments: args)
}

/// "1 day" / "3 days" in English, "3 ngày" in Vietnamese.
func Ldays(_ n: Int) -> String {
    n == 1 ? L("%d day", n) : L("%d days", n)
}

extension Date {
    func localized(_ style: Date.FormatStyle) -> String {
        formatted(style.locale(AppLanguage.current.locale))
    }
}
