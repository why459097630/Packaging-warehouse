#!/usr/bin/env bash
set -euo pipefail
APP_DIR="${1:-app}"
MODE="${2:-warn}"  # warn | strict

mkdir -p build-logs anchors

echo "[preflight] scanning $APP_DIR"

# 必要文件
if [[ ! -f "$APP_DIR/build.gradle" && ! -f "$APP_DIR/build.gradle.kts" ]]; then
  echo "::error::Gradle file missing"; exit 1
fi
if [[ ! -f "$APP_DIR/src/main/AndroidManifest.xml" ]]; then
  echo "::warning::Manifest missing"
fi

# 禁止残留锚点
LEFT=$(grep -REn "(NDJC:|IF:|LIST:|BLOCK:)" "$APP_DIR" || true)
if [[ -n "$LEFT" ]]; then
  echo "::${MODE}::Anchors remain after apply:"
  echo "$LEFT" | tee anchors/leftover.txt
  [[ "$MODE" == "strict" ]] && exit 2
fi

echo "[preflight] ok" | tee build-logs/preflight.log
