#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# @file    scripts/ndjc-materialize.sh
# @role    NDJC 物化脚本：按 NdjcPlanV1 把模板中的锚点替换为实际内容
#
# ✅ 支持锚点类型：
#   1) 文本： NDJC:KEY
#   2) 块：   BLOCK:KEY ... /BLOCK:KEY
#   3) 列表： LIST:KEY  ... /LIST:KEY                 （数组 items 渲染为多段）
#   4) 条件： IF:KEY    ... /IF:KEY                   （truthy 保留；falsey 清空）
#   5) 资源： RES:type/path                           （落盘到 src/main/res/type/path）
#   6) Hook： HOOK:KEY                                （片段替换 HOOK 占位）
#
# @plan 字段：
#   .text       { Key: "string" }
#   .block      { Key: "multiline-string" }
#   .lists      { Key: ["frag1","frag2",...] }
#   .if         { Key: true|false|0|1|"" }
#   .resources  { "RES:drawable/logo.png": "<text 或 base64:...>" }
#   .hooks      { Key: ["line1","line2",...] }
#
# @usage  bash scripts/ndjc-materialize.sh <APP_DIR> <PLAN_JSON> <OUT_JSON> [--strict]
#         --strict：严格模式（若完全没有任何替换 → 退出码 2）
#
# @output
#   build-logs/materialize.log
#   anchors/missing_{text,block,list,if}.txt
#   requests/<runId>/03_apply_result.json
# -----------------------------------------------------------------------------

set -euo pipefail
IFS=$'\n\t'

LOG_DIR="build-logs"
LOG_FILE="${LOG_DIR}/materialize.log"
MISS_DIR="anchors"
mkdir -p "${LOG_DIR}" "${MISS_DIR}"

log()  { printf '%s %s\n' "[$(date -u +%H:%M:%S)]" "$*" | tee -a "${LOG_FILE}"; }
fail() { echo "::error:: $*" | tee -a "${LOG_FILE}"; exit 1; }

APP_DIR="${1:-}"; PLAN_JSON="${2:-}"; OUT_JSON="${3:-}"
STRICT=0; [[ "${4:-}" == "--strict" ]] && STRICT=1

[[ -n "${APP_DIR}"  ]] || fail "缺少 APP_DIR"
[[ -n "${PLAN_JSON}" ]] || fail "缺少 PLAN_JSON"
[[ -n "${OUT_JSON}"  ]] || fail "缺少 OUT_JSON"

# ---------- 路径归一化、限定修改范围 ----------
realpath_cmd() { python3 - "$1" <<'PY'
import os,sys; print(os.path.realpath(sys.argv[1]))
PY
}
APP_DIR_ABS=$(realpath_cmd "${APP_DIR}")
PLAN_ABS=$(realpath_cmd "${PLAN_JSON}")
OUT_ABS=$(realpath_cmd "${OUT_JSON}")

[[ -d "${APP_DIR_ABS}" ]] || fail "APP_DIR 不存在：${APP_DIR_ABS}"
[[ -f "${PLAN_ABS}"    ]] || fail "PLAN_JSON 不存在：${PLAN_ABS}"
mkdir -p "$(dirname "${OUT_ABS}")"

within_appdir() { [[ "$(realpath_cmd "$1")" == ${APP_DIR_ABS}/* ]]; }

# ---------- 依赖 jq ----------
if ! command -v jq >/dev/null 2>&1; then
  log "jq 未安装，尝试安装..."
  sudo apt-get update -y >/dev/null 2>&1 || true
  sudo apt-get install -y jq >/dev/null 2>&1 || true
  command -v jq >/dev/null 2>&1 || fail "无法安装 jq"
fi

log "物化开始：APP_DIR=${APP_DIR_ABS}"
log "读取计划：PLAN_JSON=${PLAN_ABS}"
log "输出统计：OUT_JSON=${OUT_ABS}"
[[ ${STRICT} -eq 1 ]] && log "严格模式：完全无替换将失败"

# ---------- 统计 ----------
replaced_total=0
replaced_text=0;   missing_text=0
replaced_block=0;  missing_block=0
replaced_list=0;   missing_list=0
replaced_if=0;     missing_if=0
resources_written=0
hooks_applied=0

# ---------- 文本安全替换 ----------
safe_replace_all() {
  local pattern="$1" content="$2" file="$3"
  if grep -q -- "$pattern" "$file"; then
    perl -0777 -pe "s/\Q${pattern}\E/${content//\//\\/}/g" "$file" > "$file.ndjc.tmp" || return 1
    mv "$file.ndjc.tmp" "$file"
    return 0
  else
    return 1
  fi
}

# ========== 1) 文本 NDJC:KEY ==========
if jq -e '.text? // empty' "${PLAN_ABS}" >/dev/null 2>&1; then
  while IFS=$'\t' read -r key val; do
    [[ -n "${key}" ]] || continue
    anchor="NDJC:${key}"
    hit=0
    while IFS= read -r -d '' file; do
      within_appdir "${file}" || { log "跳过越界文件：${file}"; continue; }
      if safe_replace_all "${anchor}" "${val}" "${file}"; then
        log "文本替换：${file} ← ${anchor}"
        ((replaced_text+=1, replaced_total+=1, hit=1))
      fi
    done < <(grep -RIl --null --exclude-dir=build --exclude-dir=.gradle --exclude-dir=.git -- "${anchor}" "${APP_DIR_ABS}" || true)
    if [[ ${hit} -eq 0 ]]; then
      echo "${anchor}" >> "${MISS_DIR}/missing_text.txt"
      ((missing_text+=1))
    fi
  done < <(jq -r '(.text // {}) | to_entries[] | "\(.key)\t\(.value)"' "${PLAN_ABS}")
else
  log "Plan 无 .text，跳过文本"
fi

# ========== 2) 块 BLOCK:KEY ... /BLOCK:KEY ==========
if jq -e '.block? // empty' "${PLAN_ABS}" >/dev/null 2>&1; then
  while IFS=$'\t' read -r key val; do
    [[ -n "${key}" ]] || continue
    start="BLOCK:${key}"; end="/BLOCK:${key}"
    hit=0
    while IFS= read -r -d '' file; do
      within_appdir "${file}" || { log "跳过越界文件：${file}"; continue; }
      # 把开始/结束标记之间替换为 val；保留标记行
      awk -v S="${start}" -v E="${end}" -v CONTENT="$(printf '%s' "${val}")" '
        BEGIN{inblk=0}
        {
          if (index($0,S)>0 && inblk==0) { print; inblk=1; next }
          if (index($0,E)>0 && inblk==1) { print CONTENT; print; inblk=0; next }
          if (inblk==0) { print }
        }
      ' "$file" > "$file.ndjc.tmp" && mv "$file.ndjc.tmp" "$file" || true
      if grep -q -- "$start" "$file"; then
        log "块替换：${file} ← ${start}..${end}"
        ((replaced_block+=1, replaced_total+=1, hit=1))
      fi
    done < <(grep -RIl --null --exclude-dir=build --exclude-dir=.gradle --exclude-dir=.git -- "${start}" "${APP_DIR_ABS}" || true)
    if [[ ${hit} -eq 0 ]]; then
      echo "${start}" >> "${MISS_DIR}/missing_block.txt"
      ((missing_block+=1))
    fi
  done < <(jq -r '(.block // {}) | to_entries[] | "\(.key)\t\(.value)"' "${PLAN_ABS}")
else
  log "Plan 无 .block，跳过块"
fi

# ========== 3) 列表 LIST:KEY ... /LIST:KEY ==========
if jq -e '.lists? // empty' "${PLAN_ABS}" >/dev/null 2>&1; then
  while IFS=$'\t' read -r key json; do
    [[ -n "${key}" ]] || continue
    start="LIST:${key}"; end="/LIST:${key}"
    content="$(jq -r '[.[]] | join("\n")' <<<"${json}")"
    hit=0
    while IFS= read -r -d '' file; do
      within_appdir "${file}" || { log "跳过越界文件：${file}"; continue; }
      awk -v S="${start}" -v E="${end}" -v CONTENT="$(printf '%s' "${content}")" '
        BEGIN{inblk=0}
        {
          if (index($0,S)>0 && inblk==0) { print; inblk=1; next }
          if (index($0,E)>0 && inblk==1) { print CONTENT; print; inblk=0; next }
          if (inblk==0) { print }
        }
      ' "$file" > "$file.ndjc.tmp" && mv "$file.ndjc.tmp" "$file" || true
      if grep -q -- "$start" "$file"; then
        log "列表替换：${file} ← ${start}..${end} (items=$(jq -r 'length' <<<"${json}"))"
        ((replaced_list+=1, replaced_total+=1, hit=1))
      fi
    done < <(grep -RIl --null --exclude-dir=build --exclude-dir=.gradle --exclude-dir=.git -- "${start}" "${APP_DIR_ABS}" || true)
    if [[ ${hit} -eq 0 ]]; then
      echo "${start}" >> "${MISS_DIR}/missing_list.txt"
      ((missing_list+=1))
    fi
  done < <(jq -r '(.lists // {}) | to_entries[] | "\(.key)\t\(.value|tostring)"' "${PLAN_ABS}")
else
  log "Plan 无 .lists，跳过列表"
fi

# ========== 4) 条件 IF:KEY ... /IF:KEY ==========
if jq -e '.if? // empty' "${PLAN_ABS}" >/dev/null 2>&1; then
  while IFS=$'\t' read -r key raw; do
    [[ -n "${key}" ]] || continue
    start="IF:${key}"; end="/IF:${key}"
    truthy=0
    if [[ "${raw}" != "false" && "${raw}" != "null" && "${raw}" != "0" && -n "${raw}" ]]; then truthy=1; fi
    hit=0
    while IFS= read -r -d '' file; do
      within_appdir "${file}" || { log "跳过越界文件：${file}"; continue; }
      if [[ ${truthy} -eq 1 ]]; then
        if grep -q -- "$start" "$file"; then
          log "条件保留：${file} ← ${start}..${end}"
          hit=1
        fi
      else
        awk -v S="${start}" -v E="${end}" '
          BEGIN{inblk=0}
          {
            if (index($0,S)>0 && inblk==0) { print; inblk=1; next }
            if (index($0,E)>0 && inblk==1) { print ""; print; inblk=0; next }
            if (inblk==0) { print }
          }
        ' "$file" > "$file.ndjc.tmp" && mv "$file.ndjc.tmp" "$file" || true
        if grep -q -- "$start" "$file"; then
          log "条件清空：${file} ← ${start}..${end}"
          ((replaced_if+=1, replaced_total+=1))
          hit=1
        fi
      fi
    done < <(grep -RIl --null --exclude-dir=build --exclude-dir=.gradle --exclude-dir=.git -- "${start}" "${APP_DIR_ABS}" || true)
    if [[ ${hit} -eq 0 ]]; then
      echo "${start}" >> "${MISS_DIR}/missing_if.txt"
      ((missing_if+=1))
    fi
  done < <(jq -r '(.if // {}) | to_entries[] | "\(.key)\t\(.value|tostring)"' "${PLAN_ABS}")
else
  log "Plan 无 .if，跳过条件"
fi

# ========== 5) 资源 RES:type/path ==========
# 支持：纯文本；或以 base64: 前缀标识的二进制
if jq -e '.resources? // empty' "${PLAN_ABS}" >/dev/null 2>&1; then
  while IFS=$'\t' read -r rkey content; do
    [[ "${rkey}" == RES:*/* ]] || { log "资源键不规范：${rkey}"; continue; }
    rel="${rkey#RES:}"                                # drawable/logo.png
    target="${APP_DIR_ABS}/src/main/res/${rel}"
    within_appdir "${target}" || { log "越界资源，跳过：${target}"; continue; }
    mkdir -p "$(dirname "${target}")"
    if [[ "${content}" == base64:* ]]; then
      printf '%s' "${content#base64:}" | base64 -d > "${target}"
    else
      printf '%s' "${content}" > "${target}"
    fi
    log "资源写入：${target}"
    ((resources_written+=1))
  done < <(jq -r '(.resources // {}) | to_entries[] | "\(.key)\t\(.value)"' "${PLAN_ABS}")
else
  log "Plan 无 .resources，跳过资源"
fi

# ========== 6) HOOK:KEY ==========
# 策略：将 "HOOK:KEY" 直接替换为拼接后的片段
if jq -e '.hooks? // empty' "${PLAN_ABS}" >/dev/null 2>&1; then
  while IFS=$'\t' read -r key json; do
    anchor="HOOK:${key}"
    payload="$(jq -r '[.[]] | join("\n")' <<<"${json}")"
    while IFS= read -r -d '' file; do
      within_appdir "${file}" || { log "跳过越界文件：${file}"; continue; }
      if safe_replace_all "${anchor}" "${payload}" "${file}"; then
        log "HOOK 应用：${file} ← ${anchor} (lines=$(jq -r 'length' <<<"${json}"))"
        ((hooks_applied+=1, replaced_total+=1))
      fi
    done < <(grep -RIl --null --exclude-dir=build --exclude-dir=.gradle --exclude-dir=.git -- "${anchor}" "${APP_DIR_ABS}" || true)
  done < <(jq -r '(.hooks // {}) | to_entries[] | "\(.key)\t\(.value|tostring)"' "${PLAN_ABS}")
else
  log "Plan 无 .hooks，跳过 Hook"
fi

# ---------- 输出统计 ----------
cat > "${OUT_ABS}" <<JSON
{
  "replaced_total": ${replaced_total},
  "replaced_text": ${replaced_text},
  "replaced_block": ${replaced_block},
  "replaced_list": ${replaced_list},
  "replaced_if": ${replaced_if},
  "resources_written": ${resources_written},
  "hooks_applied": ${hooks_applied},
  "missing_text": ${missing_text},
  "missing_block": ${missing_block},
  "missing_list": ${missing_list},
  "missing_if": ${missing_if},
  "generated_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
JSON
log "统计完成：${OUT_ABS}"

# ---------- 严格模式：完全无替换时失败 ----------
if [[ ${STRICT} -eq 1 && ${replaced_total} -eq 0 ]]; then
  log "严格模式失败：replaced_total=0"
  exit 2
fi

log "物化结束：total=${replaced_total} | text=${replaced_text} block=${replaced_block} list=${replaced_list} if=${replaced_if} res=${resources_written} hook=${hooks_applied} | missing t=${missing_text} b=${missing_block} l=${missing_list} i=${missing_if}"
exit 0
