import Foundation
import Observation
import LunariaCore

/// Thin Swift facade over the Rust `CycleStore`. All cycle logic lives in Rust.
@Observable
final class CycleModel {
    let store: CycleStore
    /// Bumped after every mutation so views re-query the store.
    private(set) var revision = 0
    var errorMessage: String?

    init(store: CycleStore? = nil) {
        if let store {
            self.store = store
        } else {
            let dir = URL.applicationSupportDirectory.appending(path: "Lunaria", directoryHint: .isDirectory)
            let path = dir.appending(path: "data.json").path(percentEncoded: false)
            do {
                self.store = try CycleStore.open(path: path)
            } catch {
                self.store = CycleStore.inMemory()
                errorMessage = "Could not open saved data: \(error.localizedDescription)"
            }
        }
    }

    static var preview: CycleModel {
        let m = CycleModel(store: .inMemory())
        let today = CalDate(Date())
        try? m.store.startPeriod(start: today.adding(days: -40))
        try? m.store.startPeriod(start: today.adding(days: -12))
        return m
    }

    var today: CalDate { _ = revision; return CalDate(Date()) }

    private var firstWeekday: UInt8 { UInt8(Calendar.current.firstWeekday - 1) }

    func perform(_ action: () throws -> Void) {
        do {
            try action()
            revision += 1
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    // MARK: Queries (read `revision` so @Observable tracks them)

    func month(year: Int, month: Int) -> MonthView? {
        _ = revision
        return try? store.month(year: Int32(year), month: UInt32(month), today: today, firstWeekday: firstWeekday)
    }

    var status: CycleStatus? { _ = revision; return (try? store.status(today: today)) ?? nil }
    var stats: CycleStats? { _ = revision; return try? store.stats() }
    var settings: CycleSettings { _ = revision; return store.settings() }
    var periods: [PeriodEntry] { _ = revision; return store.periods() }

    func log(for date: CalDate) -> DayLog {
        _ = revision
        return store.logFor(date: date) ?? DayLog(date: date, flow: nil, mood: nil, symptoms: [], note: "", intimacy: nil)
    }

    func isPeriodDay(_ date: CalDate) -> Bool {
        periods.contains { $0.start <= date && date <= $0.end }
    }
}

// MARK: - Date bridging

extension CalDate {
    init(_ date: Date, calendar: Calendar = .current) {
        let c = calendar.dateComponents([.year, .month, .day], from: date)
        self.init(year: Int32(c.year!), month: UInt32(c.month!), day: UInt32(c.day!))
    }

    var date: Date {
        Calendar.current.date(from: DateComponents(year: Int(year), month: Int(month), day: Int(day)))!
    }

    func adding(days: Int) -> CalDate {
        CalDate(Calendar.current.date(byAdding: .day, value: days, to: date)!)
    }

    func formatted(_ style: Date.FormatStyle = .dateTime.day().month(.abbreviated)) -> String {
        date.formatted(style)
    }
}

extension CalDate: Comparable {
    public static func < (a: CalDate, b: CalDate) -> Bool {
        (a.year, a.month, a.day) < (b.year, b.month, b.day)
    }
}

extension CalDate: Identifiable {
    public var id: Int { Int(year) * 10_000 + Int(month) * 100 + Int(day) }
}
