#!/usr/bin/env bash
# NDJC materialize: apply NdjcPlanV1 to template anchors.
# 兼容锚点写法：
#   文本：  NDJC:KEY
#   块：    BLOCK:KEY ... (/BLOCK:KEY | END_BLOCK)
#   列表：  LIST:KEY  ... (/LIST:KEY  | END_LIST)
#   条件：  IF:KEY    ... (/IF:KEY    | END_IF)
# 资源：   RES:type/path -> src/main/res/type/path
# Hook：   HOOK:KEY
# 严格模式(--strict)：若替换总数为 0，则失败退出码 2

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

# jq
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

# 统计
replaced_total=0
replaced_text=0;   missing_text=0
replaced_block=0;  missing_block=0
replaced_list=0;   missing_list=0
replaced_if=0;     missing_if=0
resources_written=0
hooks_applied=0

# ---------- 关键：规范化键名 ----------
normalize_key() {
  local k="$1"
  # 连续剥离，兼容重复/混用
  k="${k#NDJC:}"; k="${k#BLOCK:}"; k="${k#LIST:}"; k="${k#IF:}"
  echo "$k"
}

# 文本替换（全量）
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

# 通用：替换起止标记之间内容；end2 为备用结束标记（可为空）
replace_between() {
  local file="$1" start="$2" end1="$3" end2="${4:-}" content="$5"

  grep -q -- "$start" "$file" || return 1

  awk -v S="$start" -v E1="$end1" -v E2="$end2" -v CONTENT="$(printf '%s' "$content")" '
    BEGIN{inblk=0}
    {
      if (index($0,S)>0 && inblk==0) { print; inblk=1; next }
      if (inblk==1 && (index($0,E1)>0 || (length(E2)>0 && index($0,E2)>0))) {
        print CONTENT; print; inblk=0; next
      }
      if (inblk==0) { print }
    }
  ' "$file" > "$file.ndjc.tmp" && mv "$file.ndjc.tmp" "$file" || return 1

  return 0
}

# 搜索范围（排除构建/缓存目录）
find_files_with() {
  local needle="$1"
  grep -RIl --null --exclude-dir=build --exclude-dir=.gradle --exclude-dir=.git -- "${needle}" "${APP_DIR_ABS}" || true
}

# ========== 1) 文本 NDJC:KEY ==========
if jq -e '.text? // empty' "${PLAN_ABS}" >/dev/null 2>&1; then
  while IFS=$'\t' read -r key val; do
    [[ -n "${key}" ]] || continue
    key="$(normalize_key "${key}")"
    anchor="NDJC:${key}"
    hit=0
    while IFS= read -r -d '' file; do
      within_appdir "${file}" || { log "跳过越界文件：${file}"; continue; }
      if safe_replace_all "${anchor}" "${val}" "${file}"; then
        log "文本替换：${file} ← ${anchor}"
        ((replaced_text+=1, replaced_total+=1, hit=1))
      fi
    done < <(find_files_with "${anchor}")
    if [[ ${hit} -eq 0 ]]; then
      echo "${anchor}" >> "${MISS_DIR}/missing_text.txt"
      ((missing_text+=1))
    fi
  done < <(jq -r '(.text // {}) | to_entries[] | "\(.key)\t\(.value)"' "${PLAN_ABS}")
else
  log "Plan 无 .text，跳过文本"
fi

# ========== 2) 块 BLOCK:KEY ... (/BLOCK:KEY | END_BLOCK) ==========
if jq -e '.block? // empty' "${PLAN_ABS}" >/dev/null 2>&1; then
  while IFS=$'\t' read -r key val; do
    [[ -n "${key}" ]] || continue
    key="$(normalize_key "${key}")"
    start="BLOCK:${key}"
    end_primary="/BLOCK:${key}"
    end_compat="END_BLOCK"

    hit=0
    while IFS= read -r -d '' file; do
      within_appdir "${file}" || { log "跳过越界文件：${file}"; continue; }
      if replace_between "$file" "$start" "$end_primary" "$end_compat" "${val}"; then
        log "块替换：${file} ← ${start}..(${end_primary}|${end_compat})"
        ((replaced_block+=1, replaced_total+=1, hit=1))
      fi
    done < <(find_files_with "${start}")
    if [[ ${hit} -eq 0 ]]; then
      echo "${start}" >> "${MISS_DIR}/missing_block.txt"
      ((missing_block+=1))
    fi
  done < <(jq -r '(.block // {}) | to_entries[] | "\(.key)\t\(.value)"' "${PLAN_ABS}")
else
  log "Plan 无 .block，跳过块"
fi

# ========== 3) 列表 LIST:KEY ... (/LIST:KEY | END_LIST) ==========
if jq -e '.lists? // empty' "${PLAN_ABS}" >/dev/null 2>&1; then
  while IFS=$'\t' read -r key json; do
    [[ -n "${key}" ]] || continue
    key="$(normalize_key "${key}")"
    start="LIST:${key}"
    end_primary="/LIST:${key}"
    end_compat="END_LIST"
    content="$(jq -r '[.[]] | join("\n")' <<<"${json}")"

    hit=0
    while IFS= read -r -d '' file; do
      within_appdir "${file}" || { log "跳过越界文件：${file}"; continue; }
      if replace_between "$file" "$start" "$end_primary" "$end_compat" "${content}"; then
        log "列表替换：${file} ← ${start}..(${end_primary}|${end_compat}) (items=$(jq -r 'length' <<<"${json}"))"
        ((replaced_list+=1, replaced_total+=1, hit=1))
      fi
    done < <(find_files_with "${start}")
    if [[ ${hit} -eq 0 ]]; then
      echo "${start}" >> "${MISS_DIR}/missing_list.txt"
      ((missing_list+=1))
    fi
  done < <(jq -r '(.lists // {}) | to_entries[] | "\(.key)\t\(.value|tostring)"' "${PLAN_ABS}")
else
  log "Plan 无 .lists，跳过列表"
fi

# ========== 4) 条件 IF:KEY ... (/IF:KEY | END_IF) ==========
if jq -e '.if? // empty' "${PLAN_ABS}" >/dev/null 2>&1; then
  while IFS=$'\t' read -r key raw; do
    [[ -n "${key}" ]] || continue
    key="$(normalize_key "${key}")"
    start="IF:${key}"
    end_primary="/IF:${key}"
    end_compat="END_IF"

    truthy=0
    if [[ "${raw}" != "false" && "${raw}" != "null" && "${raw}" != "0" && -n "${raw}" ]]; then truthy=1; fi

    hit=0
    while IFS= read -r -d '' file; do
      within_appdir "${file}" || { log "跳过越界文件：${file}"; continue; }
      if [[ ${truthy} -eq 1 ]]; then
        if grep -q -- "$start" "$file"; then
          log "条件保留：${file} ← ${start}"
          hit=1
        fi
      else
        if replace_between "$file" "$start" "$end_primary" "$end_compat" ""; then
          log "条件清空：${file} ← ${start}..(${end_primary}|${end_compat})"
          ((replaced_if+=1, replaced_total+=1))
          hit=1
        fi
      fi
    done < <(find_files_with "${start}")
    if [[ ${hit} -eq 0 ]]; then
      echo "${start}" >> "${MISS_DIR}/missing_if.txt"
      ((missing_if+=1))
    fi
  done < <(jq -r '(.if // {}) | to_entries[] | "\(.key)\t\(.value|tostring)"' "${PLAN_ABS}")
else
  log "Plan 无 .if，跳过条件"
fi

# ========== 5) 资源 RES:type/path ==========
if jq -e '.resources? // empty' "${PLAN_ABS}" >/dev/null 2>&1; then
  while IFS=$'\t' read -r rkey content; do
    [[ "${rkey}" == RES:*/* ]] || { log "资源键不规范：${rkey}"; continue; }
    rel="${rkey#RES:}"
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
if jq -e '.hooks? // empty' "${PLAN_ABS}" >/dev/null 2>&1; then
  while IFS=$'\t' read -r key json; do
    anchor="HOOK:$(normalize_key "${key}")"
    payload="$(jq -r '[.[]] | join("\n")' <<<"${json}")"
    while IFS= read -r -d '' file; do
      within_appdir "${file}" || { log "跳过越界文件：${file}"; continue; }
      if safe_replace_all "${anchor}" "${payload}" "${file}"; then
        log "HOOK 应用：${file} ← ${anchor} (lines=$(jq -r 'length' <<<"${json}"))"
        ((hooks_applied+=1, replaced_total+=1))
      fi
    done < <(find_files_with "${anchor}")
  done < <(jq -r '(.hooks // {}) | to_entries[] | "\(.key)\t\(.value|tostring)"' "${PLAN_ABS}")
else
  log "Plan 无 .hooks，跳过 Hook"
fi

# 输出统计
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

# 严格模式：完全无替换则失败
if [[ ${STRICT} -eq 1 && ${replaced_total} -eq 0 ]]; then
  log "严格模式失败：replaced_total=0"
  exit 2
fi

log "物化结束：total=${replaced_total} | text=${replaced_text} block=${replaced_block} list=${replaced_list} if=${replaced_if} res=${resources_written} hook=${hooks_applied} | missing t=${missing_text} b=${missing_block} l=${missing_list} i=${missing_if}"
exit 0
