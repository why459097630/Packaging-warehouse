#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   PLAN_JSON, APPLY_JSON, APP_DIR, RUN_ID 从环境传入
#   可选 argv: APP_DIR RUN_ID （优先于 env）
APP_DIR_ARG="${1:-}"
RUN_ID_ARG="${2:-}"

APP_DIR="${APP_DIR_ARG:-${APP_DIR:-}}"
RUN_ID="${RUN_ID_ARG:-${RUN_ID:-ndjc-run}}"
PLAN_JSON="${PLAN_JSON:-requests/${RUN_ID}/02_plan.json}"
APPLY_JSON="${APPLY_JSON:-requests/${RUN_ID}/03_apply_result.json}"

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
LOG_DIR="${REPO_ROOT}/build-logs"
mkdir -p "${LOG_DIR}"

log() { echo "[$(date -u +%H:%M:%S)] $*" | tee -a "${LOG_DIR}/materialize.log" >/dev/null; }

# --- tools ---
need() { command -v "$1" >/dev/null 2>&1 || { echo "missing tool: $1"; exit 2; }; }
need sed; need grep; need awk
if command -v jq >/dev/null 2>&1; then HAVE_JQ=1; else HAVE_JQ=0; fi

# --- counters for diagnostics ---
pkg_fixed=0
kt_renamed=0
imports_moved=0
toplevel_warn=0

echo "{}" > "${LOG_DIR}/materialize.log" 2>/dev/null || true
truncate -s 0 "${LOG_DIR}/materialize.log" || true

log "{ \"renamed\": 0, \"pkgfixed\": 0 }"
log "env: PLAN_JSON=${PLAN_JSON}"
log "env: APPLY_JSON=${APPLY_JSON}"
log "env: APP_DIR=${APP_DIR}"
log "env: RUN_ID=${RUN_ID}"

if [[ -z "${APP_DIR}" || ! -d "${APP_DIR}" ]]; then
  echo "::error ::APP_DIR not found: ${APP_DIR}"
  exit 1
fi

# 读取 applicationId（若 jq 可用）
application_id=""
if [[ "${HAVE_JQ}" = "1" && -f "${PLAN_JSON}" ]]; then
  application_id="$(jq -r '.gradle.applicationId // .meta.packageId // empty' "${PLAN_JSON}" || true)"
fi

# ---------- 1) Java → Kotlin 的安全重命名 ----------
is_probably_kotlin() {
  # 只要出现典型 Kotlin 关键字/语法就判定
  grep -Eq '\b(data|sealed|object|companion)\b|^[[:space:]]*fun[[:space:]]|:[[:space:]]*AppCompatActivity\b' "$1"
}
rename_java_to_kt() {
  while IFS= read -r -d '' f; do
    if is_probably_kotlin "$f"; then
      local kt="${f%.java}.kt"
      git mv -f "$f" "$kt" 2>/dev/null || mv -f "$f" "$kt"
      echo "$kt"
      kt_renamed=$((kt_renamed+1))
    fi
  done < <(find "${APP_DIR}" -type f -name "*.java" -print0)
}
log "::group::Kotlin rename (.java → .kt)"
renamed_files="$(rename_java_to_kt || true)"
if [[ -n "${renamed_files}" ]]; then
  echo "${renamed_files}" | sed 's/^/  moved: /'
fi
log "::endgroup::"

# ---------- 2) Manifest 的 package 属性清除（AGP 8+） ----------
strip_manifest_package_attr() {
  local mf
  while IFS= read -r -d '' mf; do
    if grep -qE '<manifest[^>]*\spackage="' "$mf"; then
      # 移除 package="..."; 尽量不动其它属性/空白
      sed -E -i 's/(<manifest[^>]*)([[:space:]]+package="[^"]*")/\1/g' "$mf"
      pkg_fixed=$((pkg_fixed+1))
    fi
  done < <(find "${APP_DIR}" -type f -name "AndroidManifest.xml" -print0)
}
log "::group::Fix AndroidManifest (remove package attr)"
strip_manifest_package_attr
log "package_fixed=${pkg_fixed}"
log "::endgroup::"

# ---------- 3) Kotlin 文件版式整理：把 import 统一上移 ----------
uniq_sorted() {
  awk '!a[$0]++' | sort
}
move_imports_to_head_for_file() {
  local file="$1"
  # 读取整个文件
  local pkg_line imports body
  pkg_line=""
  imports=""
  body=""

  # 用 awk 分段抽取
  # state: 0 before pkg, 1 after pkg, 2 reading imports anywhere, collect and strip from body
  awk '
    BEGIN{pkg=""; }
    {
      line=$0;
      if (NR==1 || match(line, /^[[:space:]]*package[[:space:]]+[A-Za-z0-9_.]+/)) {
        if (match(line, /^[[:space:]]*package[[:space:]]+[A-Za-z0-9_.]+/)) { pkg=line; next; }
      }
      if (match(line, /^[[:space:]]*package[[:space:]]+[A-Za-z0-9_.]+/)) { pkg=line; next; }

      if (match(line, /^[[:space:]]*import[[:space:]]+[A-Za-z0-9_.]+/)) {
        print "__NDJC_IMPORT__ " line;
        next;
      }
      print "__NDJC_BODY__ " line;
    }
    END{
      if (pkg!="") print "__NDJC_PKG__ " pkg;
    }
  ' "$file" > "${file}.ndjc.tmp"

  pkg_line="$(grep -m1 '^__NDJC_PKG__ ' "${file}.ndjc.tmp" | sed 's/^__NDJC_PKG__ //')"
  imports="$(grep '^__NDJC_IMPORT__ ' "${file}.ndjc.tmp" | sed 's/^__NDJC_IMPORT__ //')"
  body="$(sed -n 's/^__NDJC_BODY__ //p' "${file}.ndjc.tmp")"

  # 若文件开头未显式 package，则保持原样（只删中部 import 并统一上移）
  if [[ -z "${pkg_line}" ]]; then
    pkg_line="$(head -n1 "${file}" || true)"
    if [[ "${pkg_line}" =~ ^[[:space:]]*package[[:space:]]+ ]]; then
      : # ok
    else
      pkg_line=""
    fi
  fi

  # 去重+排序 import
  if [[ -n "${imports}" ]]; then
    imports="$(printf "%s\n" "${imports}" | uniq_sorted)"
  fi

  # 构造新内容
  {
    if [[ -n "${pkg_line}" ]]; then
      echo "${pkg_line}"
      echo
    fi
    if [[ -n "${imports}" ]]; then
      echo "${imports}"
      echo
    fi
    # 原 body：把可能残存的包头/空行去掉
    printf "%s\n" "${body}"
  } > "${file}.ndjc.out"

  # 若中部的 import 被上移，算一次
  if grep -q '^__NDJC_IMPORT__ ' "${file}.ndjc.tmp"; then
    imports_moved=$((imports_moved+1))
  fi

  mv -f "${file}.ndjc.out" "${file}"
  rm -f "${file}.ndjc.tmp"
}

log "::group::Normalize Kotlin imports"
while IFS= read -r -d '' kf; do
  move_imports_to_head_for_file "$kf"
done < <(find "${APP_DIR}" -type f -name "*.kt" -print0)
log "imports_moved=${imports_moved}"
log "::endgroup::"

# ---------- 4) （可选）顶层声明的快速体检 ----------
# 这里只做告警统计，不做侵入式改写（避免误修）。
check_toplevel_layout() {
  local file="$1"
  # 典型“错误落位”的几个信号：
  # - 文件中段出现 'package '（异常）或 import（上一阶段已搬移）
  # - 文件中出现多处 '@Composable' 开头、但紧邻前后是顶层以外的文本
  if grep -nE '^[[:space:]]*package[[:space:]]' "$file" | sed -n '2p' >/dev/null 2>&1; then
    toplevel_warn=$((toplevel_warn+1))
    echo "warn: multiple 'package' lines in ${file}" >> "${LOG_DIR}/materialize.log"
  fi
}
log "::group::Check Kotlin toplevel layout (soft warnings)"
while IFS= read -r -d '' kf; do
  check_toplevel_layout "$kf" || true
done < <(find "${APP_DIR}" -type f -name "*.kt" -print0)
log "toplevel_warn=${toplevel_warn}"
log "::endgroup::"

# ---------- 5) 统计与改动清单 ----------
# anchors.txt：保留给上游/下游查看（这里不解析 plan，只占位）
{
  echo "${APP_DIR}/build.gradle#: // HOOK:BEFORE_BUILD"
  echo "${APP_DIR}/src/main/AndroidManifest.xml#: // IF:* / BLOCK:* / LIST:*"
} > "${LOG_DIR}/anchors.txt"

git status --porcelain | awk '{print $2}' > "${LOG_DIR}/modified-files.txt" || true

# 末尾写一行摘要，便于在 Actions Summary 快速查看
echo "autofix: kotlin_renamed=${kt_renamed}  package_fixed=${pkg_fixed}" | tee -a "${LOG_DIR}/materialize.log" >/dev/null
echo "NDJC materialize: completed (imports_moved=${imports_moved}, toplevel_warn=${toplevel_warn})" | tee -a "${LOG_DIR}/materialize.log" >/dev/null
