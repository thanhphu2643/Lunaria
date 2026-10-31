import SwiftUI
import LunariaCore

struct HomeView: View {
    var selectTab: (AppTab) -> Void
    @Environment(CycleModel.self) private var model

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(Date.now.formatted(.dateTime.weekday(.wide).day().month(.wide)))
                        .font(.system(size: 16)).foregroundStyle(Theme.secondaryText)
                    Text("Hello 🌸").font(.system(size: 34, weight: .bold)).foregroundStyle(Theme.text)
                }
                .padding(.top, 8)

                if let status = model.status {
                    CycleRing(status: status)
                    quickActions
                    PhaseCard(status: status)
                    statsRow
                } else {
                    EmptyState { selectTab(.calendar) }
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
        .scrollIndicators(.hidden)
        .background(alignment: .top) {
            LinearGradient(colors: [Theme.background, .white], startPoint: .top, endPoint: .center)
                .ignoresSafeArea()
        }
    }

    private var quickActions: some View {
        let today = model.today
        return HStack(spacing: 12) {
            if model.isPeriodDay(today) {
                PillButton(title: "Period ended", icon: "checkmark", filled: true) {
                    model.perform { try model.store.endPeriod(date: today) }
                }
            } else {
                PillButton(title: "Period started", icon: "drop.fill", filled: true) {
                    model.perform { try model.store.startPeriod(start: today) }
                }
            }
            PillButton(title: "Log today", icon: "square.and.pencil", filled: false) { selectTab(.log) }
        }
    }

    @ViewBuilder private var statsRow: some View {
        if let stats = model.stats {
            HStack(spacing: 12) {
                StatTile(value: "\(stats.averageCycleLength)", unit: "days", title: "Avg. cycle")
                StatTile(value: "\(stats.averagePeriodLength)", unit: "days", title: "Avg. period")
                StatTile(value: "\(stats.loggedCycles)", unit: "", title: "Cycles logged")
            }
        }
    }
}

struct CycleRing: View {
    let status: CycleStatus

    private var progress: Double {
        min(1, Double(status.cycleDay) / Double(max(status.cycleLength, 1)))
    }

    private var headline: String {
        if status.daysLate > 0 { return "\(status.daysLate) day\(status.daysLate == 1 ? "" : "s") late" }
        if status.daysUntilNextPeriod == 0 { return "Period expected today" }
        return "\(status.daysUntilNextPeriod) day\(status.daysUntilNextPeriod == 1 ? "" : "s")"
    }

    var body: some View {
        ZStack {
            Circle().stroke(Theme.ovulation, lineWidth: 22)
            Circle()
                .trim(from: 0, to: progress)
                .stroke(AngularGradient(colors: [Theme.period.opacity(0.6), Theme.accent], center: .center),
                        style: StrokeStyle(lineWidth: 22, lineCap: .round))
                .rotationEffect(.degrees(-90))
            VStack(spacing: 6) {
                Text(status.daysLate > 0 || status.daysUntilNextPeriod == 0 ? "" : "Period in")
                    .font(.system(size: 17)).foregroundStyle(Theme.secondaryText)
                Text(headline)
                    .font(.system(size: 36, weight: .bold))
                    .foregroundStyle(Theme.accent)
                    .multilineTextAlignment(.center)
                Text("Cycle day \(status.cycleDay)")
                    .font(.system(size: 16, weight: .medium))
                    .padding(.horizontal, 14).padding(.vertical, 6)
                    .background(Theme.ovulation, in: Capsule())
                    .foregroundStyle(Theme.text)
            }
            .padding(30)
        }
        .frame(width: 260, height: 260)
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .accessibilityElement(children: .combine)
    }
}

struct PhaseCard: View {
    let status: CycleStatus

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(status.phase.title).font(.system(size: 20, weight: .semibold)).foregroundStyle(Theme.text)
            Text(status.phase.blurb).font(.system(size: 15)).foregroundStyle(Theme.secondaryText)
            Divider()
            InfoLine(color: Theme.period, title: "Next period", value: status.nextPeriodStart.formatted())
            InfoLine(color: Theme.ovulation, title: "Ovulation", value: status.nextOvulation.formatted())
            InfoLine(color: Theme.fertile, title: "Fertile window",
                     value: "\(status.fertileStart.formatted()) – \(status.fertileEnd.formatted())")
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .card()
    }
}

private struct InfoLine: View {
    let color: Color
    let title: String
    let value: String

    var body: some View {
        HStack {
            Circle().fill(color).frame(width: 12, height: 12)
            Text(title).foregroundStyle(Theme.secondaryText)
            Spacer()
            Text(value).fontWeight(.semibold).foregroundStyle(Theme.text)
        }
        .font(.system(size: 16))
    }
}

struct StatTile: View {
    let value: String
    let unit: String
    let title: String

    var body: some View {
        VStack(spacing: 4) {
            HStack(alignment: .firstTextBaseline, spacing: 3) {
                Text(value).font(.system(size: 26, weight: .bold)).foregroundStyle(Theme.accent)
                Text(unit).font(.system(size: 13)).foregroundStyle(Theme.secondaryText)
            }
            Text(title).font(.system(size: 13)).foregroundStyle(Theme.secondaryText)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(Theme.background.opacity(0.6), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

private struct EmptyState: View {
    var openCalendar: () -> Void
    @Environment(CycleModel.self) private var model

    var body: some View {
        VStack(spacing: 18) {
            Image(systemName: "drop.circle.fill")
                .font(.system(size: 64))
                .foregroundStyle(Theme.period)
            Text("Welcome to Lunaria").font(.system(size: 22, weight: .semibold))
            Text("Log the first day of your last period and Lunaria will predict your next period, ovulation and fertile window. Everything stays on this device.")
                .multilineTextAlignment(.center)
                .foregroundStyle(Theme.secondaryText)
            PillButton(title: "My period started today", icon: "drop.fill", filled: true) {
                model.perform { try model.store.startPeriod(start: model.today) }
            }
            PillButton(title: "Pick a past date", icon: "calendar", filled: false, action: openCalendar)
        }
        .card()
        .padding(.top, 20)
    }
}

#Preview {
    HomeView(selectTab: { _ in }).environment(CycleModel.preview)
}
