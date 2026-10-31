#!/usr/bin/env bash
# Builds the Rust core for Android (.so per ABI) and generates Kotlin bindings.
# Requires: Android NDK (ANDROID_NDK_HOME, or the newest one under $ANDROID_HOME/ndk)
# and `cargo install cargo-ndk`. Gradle runs this automatically before each build.
# Output: android/jniLibs/<abi>/liblunaria_core.so and android/bindings/uniffi/lunaria_core/*.kt
# Use with JNA (net.java.dev.jna:jna:5.x@aar) in your Android app.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/core"
export PATH="$HOME/.cargo/bin:/opt/homebrew/bin:/usr/local/bin:$PATH"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
if [ -z "${ANDROID_NDK_HOME:-}" ] && [ -d "$SDK/ndk" ]; then
  ANDROID_NDK_HOME="$SDK/ndk/$(ls "$SDK/ndk" | sort -V | tail -1)"
  export ANDROID_NDK_HOME
fi
if command -v rustup >/dev/null; then
  export PATH="$(dirname "$(rustup which cargo)"):$PATH"
  rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android >/dev/null
fi
cargo build --release --lib
cargo ndk --platform 26 -t arm64-v8a -t armeabi-v7a -t x86_64 -o "$ROOT/android/jniLibs" build --release --lib
LIBEXT=$([ "$(uname)" = "Darwin" ] && echo dylib || echo so)
cargo run --quiet --bin uniffi-bindgen -- generate \
  --library "target/release/liblunaria_core.$LIBEXT" --language kotlin --no-format --out-dir "$ROOT/android/bindings"
echo "✓ Android libs in android/jniLibs, Kotlin bindings in android/bindings"
