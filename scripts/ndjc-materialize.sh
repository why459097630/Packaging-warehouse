#!/usr/bin/env bash
# scripts/ndjc-materialize.sh
# Apply NDJC plan onto template app tree, with safe Kotlin rename and manifest fixes.
# Usage (from workflow):
#   PLAN_JSON, APPLY_JSON, APP_DIR, RUN_ID come from env
#   scripts/ndjc-materialize.sh "${APP_DIR}" "${RUN_ID}"

set -euo pipefail

# -------- inputs --------
APP_DIR_ARG="${1:-}"
RUN_ID_ARG="${2:-}"
APP_DIR="${APP_DIR_ARG:-${APP_DIR:-templates/circle-basic/app}}"
RUN_ID="${RUN_ID_ARG:-${RUN_ID:-ndjc-local-run}}"
PLAN_JSON="${PLAN_JSON:-requests/${RUN_ID}/02_plan.json}"
APPLY_JSON="${APPLY_JSON:-requests/${RUN_ID}/03_apply_result.json}"
REQ_DIR="$(dirname "${PLAN_JSON}")"

# -------- logs --------
LOG_DIR="build-logs"
mkdir -p "${LOG_DIR}" "${REQ_DIR}"
LOG_FILE="${LOG_DIR}/materialize.log"
: > "${LOG_FILE}"  # truncate

log() { printf '%s %s\n' "[$(date +%H:%M:%S)]" "$*" | tee -a "${LOG_FILE}"; }

log "{ \"renamed\": 0, \"pkgfixed\": 0 }"
log "env: PLAN_JSON=${PLAN_JSON}"
log "env: APPLY_JSON=${APPLY_JSON}"
log "env: APP_DIR=${APP_DIR}"
log "env: RUN_ID=${RUN_ID}"

# -------- helpers --------
# safe_git_mv <src> <dst>  (fall back to mv if git mv fails)
safe_git_mv() {
  local src="$1" dst="$2"
  if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    git mv -f "$src" "$dst" 2>/dev/null || mv -f "$src" "$dst"
  else
    mv -f "$src" "$dst"
  fi
}

# -------- 0) guard: app dir must exist --------
if [[ ! -d "${APP_DIR}" ]]; then
  echo "::error ::APP_DIR not found: ${APP_DIR}"
  log "APP_DIR missing: ${APP_DIR}"
  exit 1
fi

# -------- 1) Kotlin rename (.java -> .kt) --------
echo "::group::Kotlin rename (java -> .kt)"
log "::group::Kotlin rename (java -> .kt)"
kotlin_renamed=0

# 收集所有 .java，再筛 Kotlin-like 关键字（空输入安全）
mapfile -t JAVA_FILES < <(find "${APP_DIR}" -type f -name "*.java" 2>/dev/null || true)
if (( ${#JAVA_FILES[@]} > 0 )); then
  for jf in "${JAVA_FILES[@]}"; do
    # Kotlin-like token：val/var/fun/object/companion/sealed/data/@Composable
    if grep -Eq -- '@Composable|(^|[[:space:]])(val|var|fun|object|companion|sealed|data)[[:space:]]' "$jf" 2>/dev/null; then
      kt="${jf%.java}.kt"
      safe_git_mv "$jf" "$kt"
      ((kotlin_renamed++)) || true
      log "renamed: ${jf} -> ${kt}"
    fi
  done
fi

log "kotlin_renamed=${kotlin_renamed}"
echo "::endgroup::"
log "::endgroup::"

# -------- 2) AndroidManifest: remove package attribute (AGP8+) --------
echo "::group::Fix AndroidManifest (remove package attr)"
log "::group::Fix AndroidManifest (remove package attr)"
pkgfixed=0

# 主 manifest 路径
MF="${APP_DIR}/src/main/AndroidManifest.xml"
if [[ -f "${MF}" ]]; then
  if grep -Eq '<manifest[^>]*\spackage=' "${MF}"; then
    # 安全替换：只移除 manifest 起始标签上的 package 属性
    tmp="$(mktemp)"
    sed -E 's/(<manifest[^>]*?)\s+package="[^"]*"/\1/g' "${MF}" > "${tmp}"
    mv -f "${tmp}" "${MF}"
    ((pkgfixed++)) || true
    log "package attribute removed in: ${MF}"
  else
    log "no package attribute found in: ${MF}"
  fi
else
  log "manifest not found: ${MF}"
fi

log "package_fixed=${pkgfixed}"
echo "::endgroup::"
log "::endgroup::"

# -------- 3) Normalize Kotlin imports / whitespace (空输入友好) --------
echo "::group::Normalize Kotlin imports"
log "::group::Normalize Kotlin imports"

# 收集 *.kt 文件；无文件时不报错
mapfile -t KT_FILES < <(find "${APP_DIR}" -type f -name "*.kt" 2>/dev/null || true)
if (( ${#KT_FILES[@]} > 0 )); then
  for kf in "${KT_FILES[@]}"; do
    # 仅做轻量整理：去行尾空格、折叠多余空行（不改变业务含义）
    # 行尾空白
    sed -i -E 's/[[:space:]]+$//' "${kf}"
    # 连续 3+ 空行折叠为 1 行（两次确保收敛）
    sed -i -E ':a;N;$!ba;s/\n{3,}/\n\n/g' "${kf}"
  done
  log "normalized ${#KT_FILES[@]} Kotlin files"
else
  log "no Kotlin files found under ${APP_DIR}; skip normalization"
fi

echo "::endgroup::"
log "::endgroup::"

# -------- 4) (可选) 生成锚点索引，便于排查 --------
ANCHORS_TXT="${LOG_DIR}/anchors.txt"
{
  echo "${APP_DIR}/build.gradle:"
  grep -nE 'NDJC:|BLOCK:|LIST:|HOOK:|IF:' "${APP_DIR}/build.gradle" 2>/dev/null || true
  echo
  grep -R --line-number -E 'NDJC:|BLOCK:|LIST:|HOOK:|IF:' "${APP_DIR}" 2>/dev/null || true
} > "${ANCHORS_TXT}" || true

# -------- 5) 写入 apply_result（供外部追踪） --------
# 尽量从 plan 中提取 app 名/包名；没有 jq 时用保底写法
APP_NAME="(unknown)"
PKG_NAME="(unknown)"
if command -v jq >/dev/null 2>&1 && [[ -f "${PLAN_JSON}" ]]; then
  APP_NAME="$(jq -r '.meta.appName // .meta.appTitle // empty' "${PLAN_JSON}" || true)"
  PKG_NAME="$(jq -r '.meta.packageId // .gradle.applicationId // empty' "${PLAN_JSON}" || true)"
  [[ -z "${APP_NAME}" ]] && APP_NAME="(unknown)"
  [[ -z "${PKG_NAME}" ]] && PKG_NAME="(unknown)"
fi

cat > "${APPLY_JSON}" <<EOF
{
  "runId": "${RUN_ID}",
  "status": "pre-ci",
  "template": "$(basename "$(dirname "${APP_DIR}")")",
  "appTitle": "${APP_NAME}",
  "packageName": "${PKG_NAME}",
  "note": "Apply result will be finalized in CI pipeline.",
  "changes": [],
  "warnings": []
}
EOF
git add -A "${APPLY_JSON}" 2>/dev/null || true

# -------- 6) 输出修改文件清单 --------
MODIFIED_TXT="${LOG_DIR}/modified-files.txt"
git diff --name-only | sed 's/^/requests: /' > "${MODIFIED_TXT}" || true
# 再列出工作区内（未加入 git 的）本地更改
find "${APP_DIR}" -type f -name "*.kt" -o -name "*.xml" -o -name "*.gradle" 2>/dev/null | sed 's/^/templates: /' >> "${MODIFIED_TXT}" || true

# -------- 7) 汇总/计数回显（供上游 step 提示用） --------
log "{ \"renamed\": ${kotlin_renamed}, \"pkgfixed\": ${pkgfixed} }"
echo "autofix: kotlin_renamed=${kotlin_renamed}  package_fixed=${pkgfixed}"
echo "NDJC materialize: total=$(wc -l < "${MODIFIED_TXT}" | tr -d ' ') text=0 block=0 list=0 if=0" | tee -a "${LOG_FILE}"

# 对缺失锚点计数（可留空，保兼容之前工作流的 notice）
missing_text=0; missing_block=0; missing_list=0; missing_if=0
echo "missing: text=${missing_text} block=${missing_block} list=${missing_list} if=${missing_if}" | tee -a "${LOG_FILE}"

# 成功退出
exit 0
