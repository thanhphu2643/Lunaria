#!/usr/bin/env bash
# Builds the Rust core for iOS and packages it as a Swift package at ios/LunariaCore.
#   ./scripts/build-ios.sh            # release
#   PROFILE=debug ./scripts/build-ios.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CORE="$ROOT/core"
PKG="$ROOT/ios/LunariaCore"
PROFILE="${PROFILE:-release}"
CARGO_FLAG=$([ "$PROFILE" = "release" ] && echo "--release" || echo "")
LIB=liblunaria_core.a
GEN="$CORE/target/uniffi-swift"

# Use rustup's toolchain (a Homebrew `rust` on PATH has no iOS std libraries).
if command -v rustup >/dev/null; then
  export PATH="$(dirname "$(rustup which cargo)"):$PATH"
  rustup target add aarch64-apple-ios aarch64-apple-ios-sim x86_64-apple-ios >/dev/null
fi

cd "$CORE"
# Host build: used only to read UniFFI metadata for bindgen.
cargo build $CARGO_FLAG --lib
for t in aarch64-apple-ios aarch64-apple-ios-sim x86_64-apple-ios; do
  IPHONEOS_DEPLOYMENT_TARGET=17.0 cargo build $CARGO_FLAG --lib --target "$t"
done

rm -rf "$GEN" && mkdir -p "$GEN"
cargo run --quiet --bin uniffi-bindgen -- generate \
  --library "target/$PROFILE/liblunaria_core.dylib" \
  --language swift --out-dir "$GEN"

# Headers dir for the xcframework: FFI header + module map named module.modulemap.
HEADERS="$GEN/headers"
mkdir -p "$HEADERS"
cp "$GEN/lunaria_coreFFI.h" "$HEADERS/"
cp "$GEN/lunaria_coreFFI.modulemap" "$HEADERS/module.modulemap"

# Universal simulator lib (arm64 + x86_64).
SIM="$CORE/target/ios-sim-universal/$PROFILE"
mkdir -p "$SIM"
lipo -create \
  "target/aarch64-apple-ios-sim/$PROFILE/$LIB" \
  "target/x86_64-apple-ios/$PROFILE/$LIB" \
  -output "$SIM/$LIB"

rm -rf "$PKG/LunariaCoreFFI.xcframework"
xcodebuild -create-xcframework \
  -library "target/aarch64-apple-ios/$PROFILE/$LIB" -headers "$HEADERS" \
  -library "$SIM/$LIB" -headers "$HEADERS" \
  -output "$PKG/LunariaCoreFFI.xcframework" >/dev/null

mkdir -p "$PKG/Sources/LunariaCore"
cp "$GEN/lunaria_core.swift" "$PKG/Sources/LunariaCore/LunariaCore.swift"

echo "✓ Swift package ready at $PKG"
