#!/usr/bin/env bash
set -euo pipefail
APP_DIR="${1:-app}"
RUN_ID="${2:-${RUN_ID:-}}"
[ -z "${RUN_ID}" ] && { echo "::error::RUN_ID missing"; exit 1; }

REQ_DIR="requests/${RUN_ID}"
APPLY="${REQ_DIR}/03_apply_result.json"

[ -f "${APPLY}" ] || { echo "::error::${APPLY} not found (no materialize result)"; exit 1; }

# 统计 replacedCount
COUNT=$(python3 - <<'PY'
import json,sys
p=sys.argv[1]
d=json.load(open(p))
print(sum(c.get("replacedCount",0) for c in d.get("changes",[])))
PY
"${APPLY}")
if [ "${COUNT}" -le 0 ]; then
  echo "::error::No anchors replaced (COUNT=${COUNT})."
  exit 1
fi

# 禁止残留 NDJC: 标记（最常见：strings.xml 未替换）
if grep -R --include="*.xml" -n "NDJC:" "${APP_DIR}/src/main/res" >/dev/null 2>&1; then
  echo "::error::NDJC markers found in res/ (not fully injected)."
  exit 1
fi

# 如模板启用了 JSON 数据，检查是否存在且非空
RAW_JSON="${APP_DIR}/src/main/res/raw/seed_posts.json"
if [ -f "${RAW_JSON}" ]; then
  sz=$(wc -c < "${RAW_JSON}" || echo 0)
  [ "${sz}" -gt 5 ] || { echo "::error::seed_posts.json empty"; exit 1; }
fi

echo "NDJC guards passed: replacedCount=${COUNT}"
