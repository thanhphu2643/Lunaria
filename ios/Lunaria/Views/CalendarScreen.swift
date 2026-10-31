import SwiftUI
import LunariaCore

struct CalendarScreen: View {
    @Environment(CycleModel.self) private var model
    @State private var anchor = Calendar.current.date(from: Calendar.current.dateComponents([.year, .month], from: .now))!
    @State private var selectedDay: CalDate?
    @State private var tool: Tool?

    private var comps: DateComponents { Calendar.current.dateComponents([.year, .month], from: anchor) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Text(L("Calendar"))
                    .font(.system(size: 34, weight: .bold))
                    .foregroundStyle(Theme.text)
                    .padding(.top, 8)

                monthHeader
                if let view = model.month(year: comps.year!, month: comps.month!) {
                    MonthGrid(view: view) { selectedDay = $0 }
                        .gesture(DragGesture(minimumDistance: 30).onEnded { v in
                            if v.translation.width < -40 { shift(1) } else if v.translation.width > 40 { shift(-1) }
                        })
                }
                Legend()

                if model.periods.isEmpty {
                    Text(L("Tap a day to log when your last period started — predictions appear instantly."))
                        .font(.callout)
                        .foregroundStyle(Theme.secondaryText)
                }

                GeneralTools(tool: $tool)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
        .scrollIndicators(.hidden)
        .sheet(item: $selectedDay) { day in
            DayDetailSheet(date: day)
                .presentationDetents([.medium, .large])
                .presentationCornerRadius(32)
        }
        .sheet(item: $tool) { tool in
            ToolSheet(tool: tool)
                .presentationCornerRadius(32)
        }
    }

    private var monthHeader: some View {
        HStack(spacing: 14) {
            Image(systemName: "calendar")
                .font(.system(size: 22))
                .foregroundStyle(Theme.text)
            Text({ let t = anchor.localized(.dateTime.month(.wide).year()); return t.prefix(1).uppercased() + t.dropFirst() }())
                .font(.system(size: 20, weight: .medium))
                .foregroundStyle(Theme.text)
                .contentTransition(.numericText())
            Spacer()
            Button { shift(-1) } label: { Image(systemName: "chevron.left") }
                .accessibilityLabel(L("Previous month"))
            Button { shift(1) } label: { Image(systemName: "chevron.right") }
                .accessibilityLabel(L("Next month"))
                .padding(.leading, 8)
        }
        .font(.system(size: 17, weight: .semibold))
        .foregroundStyle(Theme.accent)
    }

    private func shift(_ months: Int) {
        withAnimation(.snappy) {
            anchor = Calendar.current.date(byAdding: .month, value: months, to: anchor)!
        }
    }
}

// MARK: - Grid

struct MonthGrid: View {
    let view: MonthView
    @Environment(CycleModel.self) private var model
    var onSelect: (CalDate) -> Void

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 8), count: 7)

    private var weekdaySymbols: [String] {
        let cal = Calendar.current
        var cal2 = cal; cal2.locale = AppLanguage.current.locale
        // Vietnamese short names ("Thứ 2") are too wide for a 7-column grid; use "T2".
        let symbols = AppLanguage.current.resolved == "vi" ? cal2.veryShortWeekdaySymbols : cal2.shortWeekdaySymbols
        let first = cal.firstWeekday - 1
        return Array(symbols[first...] + symbols[..<first])
    }

    var body: some View {
        LazyVGrid(columns: columns, spacing: 14) {
            ForEach(weekdaySymbols, id: \.self) { s in
                Text(s)
                    .font(.system(size: 17))
                    .foregroundStyle(Theme.secondaryText)
                    .padding(.bottom, 10)
            }
            ForEach(view.days, id: \.date.id) { day in
                DayCell(day: day, intimate: day.inMonth && model.log(for: day.date).intimacy != nil)
                    .onTapGesture { onSelect(day.date) }
            }
        }
    }
}

struct DayCell: View {
    let day: CalendarDay
    var intimate = false

    var body: some View {
        ZStack {
            background
            Text("\(day.date.day)")
                .font(.system(size: 18, weight: day.isToday ? .bold : .regular))
                .foregroundStyle(foreground)
            if day.isToday {
                Circle().fill(day.kind == .period ? .white : Theme.accent)
                    .frame(width: 5, height: 5)
                    .offset(y: 14)
            }
        }
        .overlay(alignment: .topTrailing) {
            if intimate {
                Image(systemName: "heart.fill")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(day.kind == .period ? .white : Theme.accent)
                    .padding(3)
                    .background(Circle().fill(day.kind == .period ? Theme.accent : .white))
                    .offset(x: 2, y: -2)
            }
        }
        .frame(maxWidth: 56)
        .aspectRatio(1, contentMode: .fit)
        .contentShape(Circle())
        .accessibilityElement()
        .accessibilityLabel("\(day.date.formatted(.dateTime.day().month(.wide))), \(day.kind.label)\(intimate ? ", " + L("intimacy logged") : "")")
        .accessibilityAddTraits(.isButton)
    }

    @ViewBuilder private var background: some View {
        if !day.inMonth {
            Color.clear
        } else {
            switch day.kind {
            case .period:
                Circle().fill(Theme.period)
            case .predictedPeriod:
                Circle().fill(Theme.period.opacity(0.18))
                    .overlay(Circle().strokeBorder(Theme.period, style: StrokeStyle(lineWidth: 1.5, dash: [4, 3])))
            case .ovulation:
                Circle().fill(Theme.ovulation)
                    .overlay(Circle().strokeBorder(Theme.period.opacity(0.6), lineWidth: 1.5))
            case .fertile:
                Circle().fill(Theme.fertile)
            case .safe:
                Circle().strokeBorder(Theme.safeStroke, style: StrokeStyle(lineWidth: 1.2, dash: [3, 3]))
            case .unknown:
                Color.clear
            }
        }
    }

    private var foreground: Color {
        if !day.inMonth { return Theme.secondaryText.opacity(0.4) }
        return day.kind == .period ? .white : Theme.text
    }
}

struct Legend: View {
    private let items: [DayKind] = [.period, .ovulation, .fertile, .safe, .predictedPeriod]

    var body: some View {
        ViewThatFits {
            HStack(spacing: 14) { content }
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 110), alignment: .leading)], alignment: .leading, spacing: 10) {
                content
            }
        }
    }

    @ViewBuilder private var content: some View {
        ForEach(items, id: \.self) { kind in
            HStack(spacing: 6) {
                swatch(kind).frame(width: 14, height: 14)
                Text(kind.label).font(.system(size: 15)).foregroundStyle(Theme.secondaryText)
                    .fixedSize()
            }
        }
        HStack(spacing: 6) {
            Image(systemName: "heart.fill").font(.system(size: 11)).foregroundStyle(Theme.accent)
                .frame(width: 14, height: 14)
            Text(L("Intimacy")).font(.system(size: 15)).foregroundStyle(Theme.secondaryText).fixedSize()
        }
    }

    @ViewBuilder private func swatch(_ kind: DayKind) -> some View {
        switch kind {
        case .period: Circle().fill(Theme.period)
        case .ovulation: Circle().fill(Theme.ovulation).overlay(Circle().strokeBorder(Theme.period.opacity(0.6)))
        case .fertile: Circle().fill(Theme.fertile)
        case .safe: Circle().strokeBorder(Theme.safeStroke, style: StrokeStyle(lineWidth: 1, dash: [2, 2]))
        default: Circle().fill(Theme.period.opacity(0.18)).overlay(Circle().strokeBorder(Theme.period, style: StrokeStyle(lineWidth: 1, dash: [2, 2])))
        }
    }
}

// MARK: - General tools list

struct GeneralTools: View {
    @Binding var tool: Tool?

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text(L("General Tools:"))
                .font(.system(size: 26, weight: .semibold))
                .foregroundStyle(Theme.text)
            ForEach(Tool.allCases) { t in
                Button { tool = t } label: {
                    HStack(spacing: 20) {
                        Image(systemName: t.icon)
                            .font(.system(size: 28))
                            .symbolRenderingMode(.multicolor)
                            .foregroundStyle(Theme.accent)
                            .frame(width: 80, height: 80)
                            .background(Theme.iconBubble, in: Circle())
                        VStack(alignment: .leading, spacing: 4) {
                            Text(t.title).font(.system(size: 19, weight: .semibold)).foregroundStyle(Theme.text)
                            Text(t.subtitle).font(.system(size: 16)).foregroundStyle(Theme.secondaryText)
                        }
                        Spacer(minLength: 0)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
    }
}
