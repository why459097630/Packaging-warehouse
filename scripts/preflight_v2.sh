#!/usr/bin/env bash
set -Eeuo pipefail

mkdir -p build-logs
SUMMARY="build-logs/summary.txt"
: > "$SUMMARY"
echo "[Preflight] Start" | tee -a "$SUMMARY"

# 1) 五件套/Schema 自检（优先 pwsh.exe，再退回 powershell.exe）
if command -v pwsh.exe >/dev/null 2>&1; then
  pwsh.exe -NoProfile -ExecutionPolicy Bypass -File ./Tools/ndjc-selfcheck.v2.ps1
else
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./Tools/ndjc-selfcheck.v2.ps1
fi
echo "[Preflight] SelfCheck OK" | tee -a "$SUMMARY"

# 2) 快速 Gradle 任务（资源 + Lint）
chmod +x ./gradlew || true
./gradlew clean :app:processReleaseResources :app:lintRelease -x test --stacktrace --info \
  | tee build-logs/gradle-preflight.log

# 3) 摘要错误
grep -nE "Android resource linking failed|error: |AAPT|Manifest merger|Duplicate class|not found" \
  build-logs/gradle-preflight.log || true > build-logs/quick-errors.txt

if [ -s build-logs/quick-errors.txt ]; then
  echo "[Preflight] Gradle found issues" | tee -a "$SUMMARY"
  echo "---- quick-errors ----" >> "$SUMMARY"
  cat build-logs/quick-errors.txt >> "$SUMMARY"
  exit 1
fi

echo "[Preflight] OK" | tee -a "$SUMMARY"
