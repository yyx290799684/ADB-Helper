#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SDK_ROOT="${ANDROID_SDK_ROOT:-/opt/android/sdk}"
ANDROID_JAR="$(ls -1 "$SDK_ROOT"/platforms/android-*/android.jar | sort -V | tail -n 1)"
D8_BIN="$(ls -1 "$SDK_ROOT"/build-tools/*/d8 | sort -V | tail -n 1)"

BUILD_DIR="$ROOT_DIR/remote_server/build"
CLASSES_DIR="$BUILD_DIR/classes"
OUT_JAR="$ROOT_DIR/app/src/main/assets/adbhelper_app_server.jar"

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR" "$(dirname "$OUT_JAR")"

javac \
  -source 1.8 \
  -target 1.8 \
  -encoding UTF-8 \
  -bootclasspath "$ANDROID_JAR" \
  -d "$CLASSES_DIR" \
  "$ROOT_DIR/remote_server/src/com/yangyx/adbhelper/server/AppInfoServer.java"

"$D8_BIN" \
  --min-api 21 \
  --lib "$ANDROID_JAR" \
  --output "$OUT_JAR" \
  "$CLASSES_DIR"/com/yangyx/adbhelper/server/*.class

ls -lh "$OUT_JAR"
