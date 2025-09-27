#!/usr/bin/env bash
set -euo pipefail

# args: 1=app dir, 2=run id (可选，缺省用环境变量 RUN_ID)
APP_DIR="${1:-app}"
RUN_ID="${2:-${RUN_ID:-}}"

# --- 兼容 runner 只有 python3 的情况 ---
if ! command -v python >/dev/null 2>&1; then
  if command -v python3 >/dev/null 2>&1; then
    # 某些 Ubuntu 镜像无 /usr/bin/python；尽量创建一个临时别名
    sudo ln -sf "$(command -v python3)" /usr/local/bin/python || true
  fi
fi

if [ -z "${RUN_ID:-}" ]; then
  echo "::error::RUN_ID missing (pass as 2nd arg or set env RUN_ID)"
  exit 1
fi

REQ_DIR="requests/${RUN_ID}"
PLAN_JSON="${REQ_DIR}/02_plan.json"
APPLY_JSON="${REQ_DIR}/03_apply_result.json"

if [ ! -f "${APPLY_JSON}" ]; then
  echo "::error::${APPLY_JSON} not found (no materialize/apply result)"
  exit 1
fi
if [ ! -d "${APP_DIR}" ]; then
  echo "::error::APP_DIR '${APP_DIR}' not found"
  exit 1
fi

MANIFEST_XML="${APP_DIR}/src/main/AndroidManifest.xml"
if [ ! -f "${MANIFEST_XML}" ]; then
  echo "::error::AndroidManifest.xml missing at ${MANIFEST_XML}"
  exit 1
fi

# -------- 统计 replacedCount（总数 + 关键锚点数） ----------
read -r TOTAL_COUNT CRIT_COUNT <<<"$(
python - <<'PY' "${APPLY_JSON}"
import json, sys
p = sys.argv[1]
with open(p, "r", encoding="utf-8") as f:
    data = json.load(f)

def iter_changes(d):
    if isinstance(d, list):
        for it in d:
            for c in (it.get("changes") or []):
                yield c
    elif isinstance(d, dict):
        for c in (d.get("changes") or []):
            yield c

CRITICAL = {
    "NDJC:PACKAGE_NAME",
    "NDJC:APP_LABEL",
    "NDJC:HOME_TITLE",
    "NDJC:MAIN_BUTTON",
    "NDJC:PRIMARY_BUTTON_TEXT",
    "NDJC:BLOCK:PERMISSIONS",
    "NDJC:BLOCK:INTENT_FILTERS",
}

total = 0
crit  = 0
for c in iter_changes(data):
    rc = int(c.get("replacedCount", 0) or 0)
    total += rc
    mk = str(c.get("marker",""))
    if mk in CRITICAL:
        crit += rc

print(total, crit)
PY
)"

if [ "${TOTAL_COUNT}" -le 0 ]; then
  echo "::error::No anchors replaced at all (TOTAL=${TOTAL_COUNT})."
  exit 1
fi
if [ "${CRIT_COUNT}" -le 0 ]; then
  echo "::error::No *critical* anchors replaced (CRITICAL=${CRIT_COUNT})."
  exit 1
fi

# ---------- Gradle 文件错误转义守卫 ----------
GR_KTS="${APP_DIR}/build.gradle.kts"
GR_GROOVY="${APP_DIR}/build.gradle"
if [ -f "${GR_KTS}" ]; then
  if grep -q "id \\\'com\.android\.application\\\'" "${GR_KTS}"; then
    echo "::error::Escaped quotes found in build.gradle.kts (id \\'com.android.application\\'). Check uploader."
    exit 1
  fi
fi
if [ -f "${GR_GROOVY}" ]; then
  if grep -q "id \\\'com\.android\.application\\\'" "${GR_GROOVY}"; then
    echo "::error::Escaped quotes found in build.gradle (id \\'com.android.application\\'). Check uploader."
    exit 1
  fi
fi

# ---------- 扫描残留 NDJC 标记 ----------
LEFT_FILES=""
# res/ 下不允许残留 NDJC:
if grep -RIn --include="*.xml" "NDJC:" "${APP_DIR}/src/main/res" >/dev/null 2>&1; then
  LEFT_FILES+=" res_xml"
fi
# Manifest 中不允许残留（通常关键锚点都应清理）
if grep -In "NDJC:" "${MANIFEST_XML}" >/dev/null 2>&1; then
  LEFT_FILES+=" manifest"
fi
# Gradle 文件中不允许残留（如 NDJC:RES_CONFIGS 未被替换）
if [ -f "${GR_KTS}" ] && grep -In "NDJC:" "${GR_KTS}" >/dev/null 2>&1; then
  LEFT_FILES+=" gradle_kts"
fi
if [ -f "${GR_GROOVY}" ] && grep -In "NDJC:" "${GR_GROOVY}" >/dev/null 2>&1; then
  LEFT_FILES+=" gradle"
fi

if [ -n "${LEFT_FILES}" ]; then
  echo "::error::NDJC markers still present in:${LEFT_FILES}. Not fully injected."
  # 给一些提示定位
  echo "---- Residual markers (top 5 per file) ----"
  { [ -d "${APP_DIR}/src/main/res" ] && grep -RIn --max-count=5 --include="*.xml" "NDJC:" "${APP_DIR}/src/main/res" || true; } | head -n 50 || true
  grep -In --max-count=20 "NDJC:" "${MANIFEST_XML}" | head -n 20 || true
  { [ -f "${GR_KTS}" ] && grep -In --max-count=20 "NDJC:" "${GR_KTS}" || true; } | head -n 20 || true
  { [ -f "${GR_GROOVY}" ] && grep -In --max-count=20 "NDJC:" "${GR_GROOVY}" || true; } | head -n 20 || true
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

echo "NDJC guards passed: totalReplaced=${TOTAL_COUNT}, criticalReplaced=${CRIT_COUNT}, appDir=${APP_DIR}, runId=${RUN_ID}"
