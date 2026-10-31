import SwiftUI

enum AppTab: CaseIterable, Hashable {
    case home, calendar, log, learn, profile

    var icon: String {
        switch self {
        case .home: "house"
        case .calendar: "calendar"
        case .log: "square.and.pencil"
        case .learn: "book"
        case .profile: "person.crop.circle"
        }
    }

    var title: String {
        switch self {
        case .home: "Home"
        case .calendar: "Calendar"
        case .log: "Log"
        case .learn: "Learn"
        case .profile: "Profile"
        }
    }
}

struct RootView: View {
    @Environment(CycleModel.self) private var model
    @State private var tab: AppTab = .calendar

    var body: some View {
        // Native TabView: on iOS 26+ this renders as the system Liquid Glass tab bar.
        TabView(selection: $tab) {
            ForEach(AppTab.allCases, id: \.self) { t in
                screen(t)
                    .tabItem { Label(t.title, systemImage: t.icon) }
                    .tag(t)
            }
        }
        .minimizeTabBarOnScroll()
        .alert("Something went wrong", isPresented: Binding(
            get: { model.errorMessage != nil },
            set: { if !$0 { model.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(model.errorMessage ?? "")
        }
    }

    @ViewBuilder private func screen(_ t: AppTab) -> some View {
        switch t {
        case .home: HomeView(selectTab: { tab = $0 })
        case .calendar: CalendarScreen()
        case .log: DailyLogScreen()
        case .learn: LearnView()
        case .profile: ProfileView()
        }
    }
}

private extension View {
    @ViewBuilder func minimizeTabBarOnScroll() -> some View {
        if #available(iOS 26.0, *) {
            tabBarMinimizeBehavior(.onScrollDown)
        } else {
            self
        }
    }
}
