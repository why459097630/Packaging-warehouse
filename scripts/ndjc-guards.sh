#!/usr/bin/env bash
set -euo pipefail

# args: 1=app dir, 2=run id (可选，缺省用环境变量 RUN_ID)
APP_DIR="${1:-app}"
RUN_ID="${2:-${RUN_ID:-}}"

if [ -z "${RUN_ID:-}" ]; then
  echo "::error::RUN_ID missing (pass as 2nd arg or set env RUN_ID)"
  exit 1
fi

REQ_DIR="requests/${RUN_ID}"
APPLY="${REQ_DIR}/03_apply_result.json"

if [ ! -f "${APPLY}" ]; then
  echo "::error::${APPLY} not found (no materialize result)"
  exit 1
fi

# ---------- 统计 replacedCount（兼容数组/对象两种结构） ----------
COUNT=$(
python3 - "$APPLY" <<'PY'
import json, sys
p = sys.argv[1] if len(sys.argv) > 1 else None
if not p:
    print(0); raise SystemExit(0)
with open(p, "r", encoding="utf-8") as f:
    data = json.load(f)
total = 0
if isinstance(data, list):
    for it in data:
        for c in (it.get("changes") or []):
            total += int(c.get("replacedCount", 0) or 0)
elif isinstance(data, dict):
    for c in (data.get("changes") or []):
        total += int(c.get("replacedCount", 0) or 0)
print(total)
PY
)

if [ "${COUNT}" -le 0 ]; then
  echo "::error::No critical anchors replaced (COUNT=${COUNT})."
  exit 1
fi

# ---------- 确认 res/ 内不再残留 NDJC 锚点 ----------
if grep -RIn --include="*.xml" "NDJC:" "${APP_DIR}/src/main/res" >/dev/null 2>&1; then
  echo "::error::NDJC markers still present in res/ (not fully injected)."
  exit 1
fi

# ---------- 保障原始种子数据存在且非空（两种命名择一） ----------
RAW_JSON="${APP_DIR}/src/main/res/raw/seed_data.json"
[ -f "${RAW_JSON}" ] || RAW_JSON="${APP_DIR}/src/main/res/raw/seed_posts.json"

if [ -f "${RAW_JSON}" ]; then
  BYTES="$(wc -c < "${RAW_JSON}" | tr -d ' ')"
  if [ "${BYTES}" -le 2 ]; then
    echo "::error::${RAW_JSON} is empty."
    exit 1
  fi
else
  echo "::warning::raw seed json not found under ${APP_DIR}/src/main/res/raw/"
fi

echo "NDJC guards passed: replacedCount=${COUNT}"
