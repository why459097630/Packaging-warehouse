#!/usr/bin/env bash
set -euo pipefail

MARKER="app/src/main/assets/build_marker.txt"
TEMPLATES_DIR="templates"
DEST="app/src/main"

echo "==> Apply template step"

# 读取模板名（优先 JSON，其次正则提取），默认 form-template
TEMPLATE=""
if [[ -f "$MARKER" ]]; then
  if command -v jq >/dev/null 2>&1; then
    TEMPLATE="$(jq -r '.template // empty' "$MARKER" 2>/dev/null || true)"
  fi
  if [[ -z "${TEMPLATE:-}" ]]; then
    TEMPLATE="$(grep -oE '(core-template|form-template|simple-template)' "$MARKER" | head -n1 || true)"
  fi
fi
[[ -z "${TEMPLATE:-}" ]] && TEMPLATE="form-template"

SRC="$TEMPLATES_DIR/$TEMPLATE"
if [[ ! -d "$SRC" ]]; then
  echo "Template '$TEMPLATE' not found under '$TEMPLATES_DIR'. Falling back to form-template."
  TEMPLATE="form-template"
  SRC="$TEMPLATES_DIR/$TEMPLATE"
fi
echo "Using template: $TEMPLATE"

# 清理并拷贝模板内容
echo "Sync $SRC -> $DEST"
rm -rf "$DEST/java" "$DEST/kotlin" "$DEST/res" "$DEST/assets" "$DEST/AndroidManifest.xml" || true
mkdir -p "$DEST"
rsync -a "$SRC/" "$DEST/"

# 记录一次构建信息，便于排查
mkdir -p "$DEST/assets"
date -u +%Y-%m-%dT%H:%M:%SZ > "$DEST/assets/build_time_utc.txt"
echo "{\"template\":\"$TEMPLATE\"}" > "$DEST/assets/build_info.json"

echo "Template applied."
