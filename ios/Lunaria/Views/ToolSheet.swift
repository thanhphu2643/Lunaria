import SwiftUI
import LunariaCore

enum Tool: String, CaseIterable, Identifiable {
    case dueDate, pregnancyTest, pregnancyWeek
    var id: String { rawValue }

    var title: String {
        switch self {
        case .dueDate: "Due Date Calculator"
        case .pregnancyTest: "Pregnancy Test Calculator"
        case .pregnancyWeek: "Pregnancy Calculator"
        }
    }

    var subtitle: String {
        switch self {
        case .dueDate: "Calculates estimated due date"
        case .pregnancyTest: "Calculates optimal date for pregnancy test."
        case .pregnancyWeek: "Estimates pregnancy week."
        }
    }

    var icon: String {
        switch self {
        case .dueDate: "calendar.badge.clock"
        case .pregnancyTest: "testtube.2"
        case .pregnancyWeek: "heart.text.square"
        }
    }
}

struct ToolSheet: View {
    let tool: Tool
    @Environment(CycleModel.self) private var model
    @Environment(\.dismiss) private var dismiss
    @State private var lmp = Date.now
    @State private var cycleLength = 28

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    DatePicker("First day of last period", selection: $lmp, in: ...Date.now, displayedComponents: .date)
                    Stepper("Cycle length: \(cycleLength) days", value: $cycleLength, in: 20...45)
                }
                Section("Result") { result }
                Section {
                    Text("Estimates only, based on the calendar method. Always confirm with a healthcare professional.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle(tool.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() } } }
            .onAppear {
                if let last = model.periods.last { lmp = last.start.date }
                cycleLength = Int(model.stats?.averageCycleLength ?? 28)
            }
        }
    }

    @ViewBuilder private var result: some View {
        let start = CalDate(lmp)
        let len = UInt32(cycleLength)
        switch tool {
        case .dueDate:
            if let r = try? dueDate(lastPeriodStart: start, cycleLength: len) {
                Row(title: "Estimated due date", value: r.dueDate.formatted(.dateTime.day().month(.wide).year()), highlight: true)
                Row(title: "Likely conception", value: r.conceptionDate.formatted())
            }
        case .pregnancyTest:
            if let r = try? pregnancyTestDate(lastPeriodStart: start, cycleLength: len) {
                Row(title: "Best day to test", value: r.recommendedTest.formatted(.dateTime.weekday().day().month(.wide)), highlight: true)
                Row(title: "Earliest (sensitive test)", value: r.earliestTest.formatted())
                Row(title: "Estimated ovulation", value: r.estimatedOvulation.formatted())
            }
        case .pregnancyWeek:
            if let r = try? pregnancyProgress(lastPeriodStart: start, cycleLength: len, today: CalDate(.now)) {
                Row(title: "You are", value: "\(r.weeks) weeks \(r.days) days", highlight: true)
                Row(title: "Trimester", value: "\(r.trimester)")
                Row(title: "Due date", value: r.dueDate.formatted(.dateTime.day().month(.wide).year()))
                ProgressView(value: r.progress).tint(Theme.accent)
            }
        }
    }
}

private struct Row: View {
    let title: String
    let value: String
    var highlight = false

    var body: some View {
        HStack {
            Text(title).foregroundStyle(Theme.secondaryText)
            Spacer()
            Text(value)
                .font(highlight ? .headline : .body)
                .foregroundStyle(highlight ? Theme.accent : Theme.text)
        }
    }
}
