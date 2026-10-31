// swift-tools-version:5.9
// Generated bindings + prebuilt Rust core. Run ../../scripts/build-ios.sh to (re)build.
import PackageDescription

let package = Package(
    name: "LunariaCore",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "LunariaCore", targets: ["LunariaCore"]),
    ],
    targets: [
        .binaryTarget(name: "lunaria_coreFFI", path: "LunariaCoreFFI.xcframework"),
        .target(name: "LunariaCore", dependencies: ["lunaria_coreFFI"], path: "Sources/LunariaCore"),
    ]
)
