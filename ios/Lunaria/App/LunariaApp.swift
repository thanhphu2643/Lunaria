import SwiftUI

@main
struct LunariaApp: App {
    @State private var model = CycleModel()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(model)
                .fontDesign(.rounded)
                .tint(Theme.accent)
                // Theme colours are designed for light mode only.
                .preferredColorScheme(.light)
        }
    }
}
