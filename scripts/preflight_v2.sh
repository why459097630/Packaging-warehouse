#!/usr/bin/env bash
set -Eeuo pipefail

# === Init ===
mkdir -p build-logs
SUMMARY="build-logs/summary.txt"
: > "$SUMMARY"
echo "[Preflight] Start" | tee -a "$SUMMARY"

# === 1) 五件套/Schema 自检（优先 pwsh，其次 powershell）===
if command -v pwsh >/dev/null 2>&1; then
  pwsh -NoProfile -NonInteractive -File ./Tools/ndjc-selfcheck.v2.ps1
elif command -v powershell >/dev/null 2>&1; then
  powershell -NoProfile -NonInteractive -File ./Tools/ndjc-selfcheck.v2.ps1
else
  echo "[Preflight] No PowerShell (pwsh/powershell) found on runner." | tee -a "$SUMMARY"
  exit 127
fi
echo "[Preflight] SelfCheck OK" | tee -a "$SUMMARY"

# === 2) 轻量 Gradle 预检（资源 + Lint）===
# 覆盖 release + debug 资源，保证后续构建变体一致
./gradlew clean :app:processReleaseResources :app:processDebugResources :app:lintRelease -x test --stacktrace --info \
  | tee build-logs/gradle-preflight.log

# 拷贝 Lint 报告（可能不存在，忽略错误即可）
mkdir -p build-logs/lint
cp -f app/build/reports/lint-results-*.{html,xml,txt} build-logs/lint/ 2>/dev/null || true

# === 3) 关键错误摘要：总会产出 quick-errors.txt ===
: > build-logs/quick-errors.txt
grep -nEi \
  "Android resource linking failed|AAPT| error: |Manifest merger failed|Duplicate class|not found|> Task :app:lintRelease FAILED|Lint .*(Fatal|Error)|FAILURE: Build failed" \
  build-logs/gradle-preflight.log > build-logs/quick-errors.txt || true

if [ -s build-logs/quick-errors.txt ]; then
  echo "[Preflight] Gradle found issues" | tee -a "$SUMMARY"
  echo "---- quick-errors ----" >> "$SUMMARY"
  cat build-logs/quick-errors.txt >> "$SUMMARY"
  exit 1
fi

echo "[Preflight] OK" | tee -a "$SUMMARY"
exit 0
