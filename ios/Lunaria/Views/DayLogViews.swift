import SwiftUI
import LunariaCore

/// Sheet shown when tapping a day in the calendar.
struct DayDetailSheet: View {
    let date: CalDate
    @Environment(CycleModel.self) private var model
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    PeriodActions(date: date)
                    DayLogEditor(date: date)
                }
                .padding(24)
            }
            .navigationTitle(date.formatted(.dateTime.weekday(.wide).day().month(.wide)))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() } } }
        }
    }
}

struct PeriodActions: View {
    let date: CalDate
    @Environment(CycleModel.self) private var model

    var body: some View {
        let isPeriod = model.isPeriodDay(date)
        let isFuture = date > model.today
        VStack(spacing: 10) {
            if !isPeriod {
                PillButton(title: "Period started", icon: "drop.fill", filled: true) {
                    model.perform { try model.store.startPeriod(start: date) }
                }
                .disabled(isFuture)
            } else {
                PillButton(title: "Period ended this day", icon: "checkmark", filled: true) {
                    model.perform { try model.store.endPeriod(date: date) }
                }
            }
            PillButton(title: isPeriod ? "Remove this day" : "Add just this day", icon: isPeriod ? "minus" : "plus", filled: false) {
                model.perform { try model.store.togglePeriodDay(date: date) }
            }
            .disabled(isFuture && !isPeriod)
            if isFuture {
                Text("You can log periods up to today.")
                    .font(.footnote).foregroundStyle(Theme.secondaryText)
            }
        }
    }
}

struct PillButton: View {
    let title: String
    let icon: String
    let filled: Bool
    let action: () -> Void
    @Environment(\.isEnabled) private var enabled

    var body: some View {
        Button(action: action) {
            Label(title, systemImage: icon)
                .font(.system(size: 17, weight: .semibold))
                .frame(maxWidth: .infinity, minHeight: 52)
                .foregroundStyle(filled ? .white : Theme.accent)
                .background(filled ? Theme.accent : Theme.ovulation, in: Capsule())
                .opacity(enabled ? 1 : 0.4)
        }
        .buttonStyle(.plain)
        .sensoryFeedback(.impact(weight: .light), trigger: title)
    }
}

/// Flow / mood / symptoms / note editor, saved on every change.
struct DayLogEditor: View {
    let date: CalDate
    @Environment(CycleModel.self) private var model
    @State private var log: DayLog?

    var body: some View {
        let current = log ?? model.log(for: date)
        VStack(alignment: .leading, spacing: 22) {
            section("Flow") {
                chips(Flow.allCases, selected: current.flow.map { [$0] } ?? [], title: \.title) { f in
                    update { $0.flow = $0.flow == f ? nil : f }
                }
            }
            section("Mood") {
                chips(Mood.allCases, selected: current.mood.map { [$0] } ?? [], title: \.title) { m in
                    update { $0.mood = $0.mood == m ? nil : m }
                }
            }
            section("Symptoms") {
                chips(Symptom.allCases, selected: current.symptoms, title: \.title) { s in
                    update { l in
                        if let i = l.symptoms.firstIndex(of: s) { l.symptoms.remove(at: i) } else { l.symptoms.append(s) }
                    }
                }
            }
            section("Intimacy") {
                IntimacyEditor(intimacy: current.intimacy) { new in update { $0.intimacy = new } }
            }
            section("Notes") {
                TextField("How are you feeling?", text: Binding(
                    get: { current.note },
                    set: { v in update { $0.note = v } }
                ), axis: .vertical)
                .lineLimit(3...6)
                .padding(14)
                .background(Theme.background.opacity(0.6), in: RoundedRectangle(cornerRadius: 16))
            }
        }
        .onChange(of: date) { log = nil }
    }

    private func update(_ change: (inout DayLog) -> Void) {
        var l = log ?? model.log(for: date)
        change(&l)
        log = l
        model.perform { try model.store.saveLog(log: l) }
    }

    private func section(_ title: String, @ViewBuilder content: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title).font(.system(size: 18, weight: .semibold)).foregroundStyle(Theme.text)
            content()
        }
    }

    private func chips<T: Hashable>(_ items: [T], selected: [T], title: KeyPath<T, String>, tap: @escaping (T) -> Void) -> some View {
        FlowLayout(spacing: 8) {
            ForEach(items, id: \.self) { item in
                let on = selected.contains(item)
                Button { tap(item) } label: {
                    Text(item[keyPath: title])
                        .font(.system(size: 15, weight: .medium))
                        .padding(.horizontal, 14).padding(.vertical, 9)
                        .foregroundStyle(on ? .white : Theme.text)
                        .background(on ? Theme.accent : Theme.background.opacity(0.7), in: Capsule())
                }
                .buttonStyle(.plain)
            }
        }
    }
}

/// Logs sex on a day: protected or not, plus a private note.
struct IntimacyEditor: View {
    let intimacy: Intimacy?
    let onChange: (Intimacy?) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                option("No", icon: nil, on: intimacy == nil) { onChange(nil) }
                option("Protected", icon: "heart.fill", on: intimacy?.protected == true) {
                    onChange(Intimacy(protected: true, note: intimacy?.note ?? ""))
                }
                option("Unprotected", icon: "heart.fill", on: intimacy?.protected == false) {
                    onChange(Intimacy(protected: false, note: intimacy?.note ?? ""))
                }
            }
            if let intimacy {
                TextField("Note (only stored on this phone)", text: Binding(
                    get: { intimacy.note },
                    set: { onChange(Intimacy(protected: intimacy.protected, note: $0)) }
                ), axis: .vertical)
                .lineLimit(2...5)
                .padding(14)
                .background(Theme.background.opacity(0.6), in: RoundedRectangle(cornerRadius: 16))
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .animation(.snappy, value: intimacy == nil)
    }

    private func option(_ title: String, icon: String?, on: Bool, tap: @escaping () -> Void) -> some View {
        Button(action: tap) {
            HStack(spacing: 5) {
                if let icon { Image(systemName: icon).font(.system(size: 12)) }
                Text(title)
            }
            .font(.system(size: 15, weight: .medium))
            .padding(.horizontal, 14).padding(.vertical, 9)
            .foregroundStyle(on ? .white : Theme.text)
            .background(on ? Theme.accent : Theme.background.opacity(0.7), in: Capsule())
        }
        .buttonStyle(.plain)
        .sensoryFeedback(.selection, trigger: on)
    }
}

/// Wrapping horizontal layout for chips.
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let rows = arrange(width: proposal.width ?? .infinity, subviews: subviews)
        return CGSize(width: proposal.width ?? rows.map(\.width).max() ?? 0, height: rows.last.map { $0.y + $0.height } ?? 0)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        for row in arrange(width: bounds.width, subviews: subviews) {
            var x = bounds.minX
            for i in row.indices {
                let size = subviews[i].sizeThatFits(.unspecified)
                subviews[i].place(at: CGPoint(x: x, y: bounds.minY + row.y), proposal: .unspecified)
                x += size.width + spacing
            }
        }
    }

    private struct Row { var indices: [Int] = []; var y: CGFloat = 0; var width: CGFloat = 0; var height: CGFloat = 0 }

    private func arrange(width: CGFloat, subviews: Subviews) -> [Row] {
        var rows = [Row()]
        for (i, sv) in subviews.enumerated() {
            let s = sv.sizeThatFits(.unspecified)
            if rows[rows.count - 1].width + s.width > width, !rows[rows.count - 1].indices.isEmpty {
                let prev = rows[rows.count - 1]
                rows.append(Row(y: prev.y + prev.height + spacing))
            }
            rows[rows.count - 1].indices.append(i)
            rows[rows.count - 1].width += s.width + spacing
            rows[rows.count - 1].height = max(rows[rows.count - 1].height, s.height)
        }
        return rows
    }
}

/// "Log" tab: today's log with a date switcher.
struct DailyLogScreen: View {
    @Environment(CycleModel.self) private var model
    @State private var day = Date.now

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Text("Daily Log")
                    .font(.system(size: 34, weight: .bold))
                    .foregroundStyle(Theme.text)
                    .padding(.top, 8)
                DatePicker("Day", selection: $day, in: ...Date.now, displayedComponents: .date)
                    .font(.system(size: 18, weight: .medium))
                PeriodActions(date: CalDate(day))
                DayLogEditor(date: CalDate(day))
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
        .scrollDismissesKeyboard(.interactively)
    }
}

// MARK: - Labels

extension Flow: CaseIterable {
    public static var allCases: [Flow] { [.spotting, .light, .medium, .heavy] }
    var title: String {
        switch self { case .spotting: "Spotting"; case .light: "Light"; case .medium: "Medium"; case .heavy: "Heavy" }
    }
}

extension Mood: CaseIterable {
    public static var allCases: [Mood] { [.happy, .calm, .sensitive, .sad, .irritated, .anxious] }
    var title: String {
        switch self {
        case .happy: "😊 Happy"; case .calm: "😌 Calm"; case .sensitive: "🥺 Sensitive"
        case .sad: "😢 Sad"; case .irritated: "😤 Irritated"; case .anxious: "😰 Anxious"
        }
    }
}

extension Symptom: CaseIterable {
    public static var allCases: [Symptom] {
        [.cramps, .headache, .bloating, .acne, .tenderBreasts, .fatigue, .backPain, .nausea, .cravings, .insomnia]
    }
    var title: String {
        switch self {
        case .cramps: "Cramps"; case .headache: "Headache"; case .bloating: "Bloating"; case .acne: "Acne"
        case .tenderBreasts: "Tender breasts"; case .fatigue: "Fatigue"; case .backPain: "Back pain"
        case .nausea: "Nausea"; case .cravings: "Cravings"; case .insomnia: "Insomnia"
        }
    }
}
