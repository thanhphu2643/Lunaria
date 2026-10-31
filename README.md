# Lunaria 🌸

An open-source, privacy-first period tracker for iOS and Android. The cycle logic lives in a shared **Rust** core, exposed through [UniFFI](https://github.com/mozilla/uniffi-rs). The iOS app is written in **SwiftUI** and the Android app in **Jetpack Compose**. Both call the same Rust code, so they always produce identical predictions.

- No account, no server, no analytics. Data is a single JSON file on the device.
- Predicts periods, ovulation and the fertile window, and learns from your history (average of your last 6 cycles).
- Daily log: flow, mood, symptoms, notes.
- Tools: due date, best day for a pregnancy test, pregnancy week.
- JSON backup export/import.

UI inspired by the [Periods calendar UI](https://dribbble.com/shots/25069846-Periods-calendar-UI) shot on Dribbble.

> ⚠️ Predictions use the calendar method and are estimates. They are not contraception and not medical advice.

## Architecture

```
core/                 Rust crate `lunaria_core` (all logic + storage, unit-tested)
  src/cycle.rs        prediction engine: day classification, month grid, status, stats
  src/calculators.rs  due date / pregnancy test / pregnancy week
  src/store.rs        CycleStore: periods, day logs, settings, JSON persistence
ios/
  LunariaCore/        Swift package: generated bindings + LunariaCoreFFI.xcframework
  Lunaria/            SwiftUI app (Home, Calendar, Log, Learn, Profile)
  project.yml         XcodeGen spec
android/
  app/                Jetpack Compose app (same 5 screens), JNA + generated Kotlin bindings
scripts/
  build-ios.sh        builds the xcframework and Swift bindings
  build-android.sh    builds .so libraries and Kotlin bindings
```

Neither Swift nor Kotlin computes cycle data. Both call `CycleStore` (Rust) and render the result.

### Prediction model

| Day | Rule |
|---|---|
| Period | logged by the user |
| Predicted period | projected from the last start + average cycle length; if the period is late, it moves to today |
| Ovulation | next period start − luteal phase (14 days by default) |
| Fertile days | 5 days before ovulation through 1 day after |
| Safe days | everything else inside a known cycle |

## Build

Requirements: Xcode 15+, [rustup](https://rustup.rs), and [XcodeGen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`).

```bash
cd core && cargo test          # run the Rust tests
./scripts/build-ios.sh         # build the Rust core into ios/LunariaCore
cd ios && xcodegen generate    # generate Lunaria.xcodeproj
open Lunaria.xcodeproj         # run on a simulator or device
```

> If `cargo` on your PATH comes from Homebrew's `rust`, the script switches to rustup's toolchain, because only rustup can install the iOS targets.

### Android

Requirements: JDK 17+, the Android SDK with an NDK installed, rustup, and `cargo install cargo-ndk`.

```bash
cd android
./gradlew installDebug       # builds the Rust core for each ABI, generates bindings, installs the app
```

Gradle runs `scripts/build-android.sh` before every build. That script picks the newest NDK under `$ANDROID_HOME/ndk` unless `ANDROID_NDK_HOME` is set. To skip the Rust step: `./gradlew assembleDebug -PskipRust`.

The app declares no `INTERNET` permission and turns off cloud backup.

## Using the core from Swift

```swift
let store = try CycleStore.open(path: url.path)
try store.startPeriod(start: CalDate(year: 2026, month: 10, day: 30))
let month = try store.month(year: 2026, month: 11, today: today, firstWeekday: 0)
for day in month.days { print(day.date.day, day.kind) }   // .period / .ovulation / .fertile / .safe …
let status = try store.status(today: today)              // cycle day, next period, fertile window
```

## Contributing

Issues and PRs are welcome. Good first areas: localization (Vietnamese and others), widgets, reminders, HealthKit import.

## License

MIT
