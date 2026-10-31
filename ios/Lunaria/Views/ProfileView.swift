import SwiftUI
import UniformTypeIdentifiers
import LunariaCore

struct ProfileView: View {
    @Environment(CycleModel.self) private var model
    @State private var confirmErase = false
    @State private var importing = false

    var body: some View {
        let settings = model.settings
        NavigationStack {
            Form {
                Section {
                    Toggle("Learn from my history", isOn: binding(\.autoAdjust, settings))
                    Stepper("Cycle length: \(settings.cycleLength) days", value: binding(\.cycleLength, settings), in: 15...60)
                        .disabled(settings.autoAdjust && (model.stats?.loggedCycles ?? 0) > 0)
                    Stepper("Period length: \(settings.periodLength) days", value: binding(\.periodLength, settings), in: 1...14)
                        .disabled(settings.autoAdjust && !model.periods.isEmpty)
                    Stepper("Luteal phase: \(settings.lutealLength) days", value: binding(\.lutealLength, settings), in: 7...20)
                } header: {
                    Text("Cycle")
                } footer: {
                    Text("With “Learn from my history” on, averages of your last 6 cycles are used once you have logged data.")
                }

                if let stats = model.stats, stats.loggedCycles > 0 {
                    Section("History") {
                        LabeledContent("Cycles logged", value: "\(stats.loggedCycles)")
                        if let s = stats.shortestCycle { LabeledContent("Shortest cycle", value: "\(s) days") }
                        if let l = stats.longestCycle { LabeledContent("Longest cycle", value: "\(l) days") }
                    }
                }

                Section("Periods") {
                    ForEach(model.periods.reversed(), id: \.start.id) { p in
                        LabeledContent(p.start.formatted(.dateTime.day().month(.wide).year()),
                                       value: "\(p.end.id == p.start.id ? 1 : daysBetween(p.start, p.end) + 1) days")
                    }
                    .onDelete { idx in
                        let list = Array(model.periods.reversed())
                        model.perform { for i in idx { try model.store.deletePeriod(start: list[i].start) } }
                    }
                    if model.periods.isEmpty { Text("No periods logged yet").foregroundStyle(.secondary) }
                }

                Section {
                    if let json = try? model.store.exportJson() {
                        ShareLink(item: json, preview: SharePreview("Lunaria backup")) {
                            Label("Export backup", systemImage: "square.and.arrow.up")
                        }
                    }
                    Button { importing = true } label: { Label("Import backup", systemImage: "square.and.arrow.down") }
                    Button(role: .destructive) { confirmErase = true } label: { Label("Erase all data", systemImage: "trash") }
                } header: {
                    Text("Your data")
                } footer: {
                    Text("Lunaria has no account, no server and no analytics. Your data never leaves this device unless you export it.\n\nLunaria core v\(coreVersion()) · Open source (MIT)")
                }
            }
            .navigationTitle("Profile")
            .confirmationDialog("Erase all data?", isPresented: $confirmErase, titleVisibility: .visible) {
                Button("Erase", role: .destructive) { model.perform { try model.store.eraseAll() } }
            } message: {
                Text("This permanently removes every period and log from this device.")
            }
            .fileImporter(isPresented: $importing, allowedContentTypes: [.json, .plainText]) { result in
                model.perform {
                    let url = try result.get()
                    let ok = url.startAccessingSecurityScopedResource()
                    defer { if ok { url.stopAccessingSecurityScopedResource() } }
                    try model.store.importJson(json: String(contentsOf: url, encoding: .utf8))
                }
            }
        }
    }

    private func binding<V>(_ key: WritableKeyPath<CycleSettings, V>, _ current: CycleSettings) -> Binding<V> {
        Binding(
            get: { current[keyPath: key] },
            set: { v in
                var s = model.settings
                s[keyPath: key] = v
                model.perform { try model.store.updateSettings(settings: s) }
            }
        )
    }

    private func daysBetween(_ a: CalDate, _ b: CalDate) -> Int {
        Calendar.current.dateComponents([.day], from: a.date, to: b.date).day ?? 0
    }
}

extension Stepper where Label == Text {
    init(_ title: String, value: Binding<UInt32>, in range: ClosedRange<Int>) {
        self.init(title, value: Binding(get: { Int(value.wrappedValue) }, set: { value.wrappedValue = UInt32($0) }), in: range)
    }
}
