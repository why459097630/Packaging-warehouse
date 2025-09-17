git ls-files | xargs grep -RIn "NDJC:" | wc -l
git ls-files \
| xargs grep -RIn "NDJC:BLOCK(" \
| sed -E 's/.*NDJC:BLOCK\(([^)]+)\).*/\1/' \
| sort -u
git ls-files \
| xargs grep -RIn "NDJC:APP_LABEL|NDJC:HOME_TITLE|NDJC:MAIN_BUTTON|NDJC:COLOR_PRIMARY|NDJC:COLOR_ON_PRIMARY|NDJC:COLOR_SECONDARY|NDJC:THEME_OVERRIDES"
BT="$ANDROID_SDK_ROOT/build-tools/34.0.0"
APK="app-release.apk"      # ← 替换为你的 APK 路径

"$BT/aapt.exe" dump resources "$APK" \
| grep -E "ndjc_home_title|ndjc_action_primary_text|app_name" -n
APK="app-release.apk"  # ← 替换为你的 APK 路径
AN="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/apkanalyzer"

"$AN" manifest application-id "$APK"
"$AN" manifest version-code   "$APK"
"$AN" manifest version-name   "$APK"
BT="$ANDROID_SDK_ROOT/build-tools/34.0.0"
APK="app-release.apk"

"$BT/aapt.exe" list "$APK" | grep -E '^res/layout'
#!/usr/bin/env bash
set -euo pipefail

APK="${1:-app-release.apk}"
BT="${ANDROID_SDK_ROOT}/build-tools/34.0.0"
AN="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/apkanalyzer"

echo "== 统计 NDJC 锚点总数 =="
git ls-files | xargs grep -RIn "NDJC:" | wc -l || true

echo "== BLOCK 锚点清单（去重） =="
git ls-files | xargs grep -RIn "NDJC:BLOCK(" \
| sed -E 's/.*NDJC:BLOCK\(([^)]+)\).*/\1/' | sort -u || true

echo "== 文本锚点探测（常见） =="
git ls-files \
| xargs grep -RIn "NDJC:APP_LABEL|NDJC:HOME_TITLE|NDJC:MAIN_BUTTON|NDJC:COLOR_PRIMARY|NDJC:COLOR_ON_PRIMARY|NDJC:COLOR_SECONDARY|NDJC:THEME_OVERRIDES" || true

echo "== APK 资源关键 key 校验 =="
if [ -x "${BT}/aapt" ]; then AAPT="${BT}/aapt"; else AAPT="${BT}/aapt.exe"; fi
"${AAPT}" dump resources "${APK}" | grep -E "ndjc_home_title|ndjc_action_primary_text|app_name" -n || true

echo "== Manifest 核心信息 =="
"${AN}" manifest application-id "${APK}"
"${AN}" manifest version-code   "${APK}"
"${AN}" manifest version-name   "${APK}"

echo "Done."
