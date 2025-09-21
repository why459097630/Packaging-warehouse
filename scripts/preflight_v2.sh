#!/usr/bin/env bash
set -Eeuo pipefail

mkdir -p build-logs
SUMMARY="build-logs/summary.txt"
: > "$SUMMARY"
echo "[Preflight] Start" | tee -a "$SUMMARY"

# 1) 五件套/Schema 自检（优先 pwsh，其次 powershell）
if command -v pwsh >/dev/null 2>&1; then
  pwsh -NoProfile -NonInteractive -File ./Tools/ndjc-selfcheck.v2.ps1
elif command -v powershell >/dev/null 2>&1; then
  powershell -NoProfile -NonInteractive -File ./Tools/ndjc-selfcheck.v2.ps1
else
  echo "[Preflight] No PowerShell (pwsh/powershell) found on runner." | tee -a "$SUMMARY"
  exit 127
fi
echo "[Preflight] SelfCheck OK" | tee -a "$SUMMARY"

# 2) 快速 Gradle 任务（资源 + Lint）
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
