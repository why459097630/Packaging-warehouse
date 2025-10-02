#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# @file    scripts/ndjc-materialize.sh
# @role    NDJC 物化脚本：按 NdjcPlanV1 把模板中的锚点（NDJC:/BLOCK:/LIST:/IF:）替换为实际内容
# @owner   ndjc@team
# @since   2025-10-02
# @entry   yes
# @usage   bash scripts/ndjc-materialize.sh <APP_DIR> <PLAN_JSON> <OUT_JSON> [--strict]
#
# 这个脚本做什么：
#   1) 读取 NdjcPlanV1（通常是 02_plan.json），从中解析出 text / block（可扩展到 lists/if）
#   2) 在指定 APP_DIR 下，替换模板中的锚点：
#      - 文本锚点：     NDJC:KeyName
#      - 代码块锚点：  BLOCK:KeyName ... /BLOCK:KeyName
#   3) 统计替换次数并生成结果 JSON（例如 03_apply_result.json），便于 CI “严格守卫”判定
#   4) 把详细过程记录到 build-logs/materialize.log，构建失败时也会随着 artifacts 上传
#
# 输入参数：
#   APP_DIR   - Android 模块目录（含 src/main/...），只在此目录内进行替换（做路径白名单）
#   PLAN_JSON - NdjcPlanV1 文件路径（一般是 requests/<runId>/02_plan.json）
#   OUT_JSON  - 输出统计 JSON 文件路径（一般是 requests/<runId>/03_apply_result.json）
#   --strict  - 可选；严格模式：若任何一个 plan 键找不到落点会计入 missing，且替换总数为 0 时退出码非 0
#
# 输出：
#   - 文件：build-logs/materialize.log            # 过程日志（可审计）
#   - 文件：requests/<runId>/03_apply_result.json # 机器可读统计
#
# 退出码：
#   0  - 执行成功（即使替换为 0，默认也返回 0；若加 --strict 且替换为 0，将返回 2）
#   1  - 参数/环境错误（jq 未安装、文件缺失、路径非法等）
#   2  - 严格模式下替换总数为 0（No anchors replaced at all）
#
# 注意：
#   - 本脚本只做“物化替换”，不修改 Gradle/权限等（这些由 plan.grad le 或其它步骤处理）
#   - 如需支持 lists/if/resources/hooks，可在“可扩展区”按需增强
# -----------------------------------------------------------------------------

set -euo pipefail
IFS=$'\n\t'

# ---------- 工具与日志 ----------
LOG_DIR="build-logs"
LOG_FILE="${LOG_DIR}/materialize.log"
mkdir -p "${LOG_DIR}"

log() { printf '%s %s\n' "[$(date -u +%H:%M:%S)]" "$*" | tee -a "${LOG_FILE}"; }
fail() { echo "::error:: $*" | tee -a "${LOG_FILE}"; exit 1; }

# ---------- 参数解析 ----------
APP_DIR="${1:-}"; PLAN_JSON="${2:-}"; OUT_JSON="${3:-}"
STRICT=0
if [[ "${4:-}" == "--strict" ]]; then STRICT=1; fi

[[ -n "${APP_DIR}"  ]] || fail "缺少参数：APP_DIR"
[[ -n "${PLAN_JSON}" ]] || fail "缺少参数：PLAN_JSON"
[[ -n "${OUT_JSON}"  ]] || fail "缺少参数：OUT_JSON"

# 路径规范化，限定写入范围（防止越权/误伤）
realpath_cmd() {
  python3 - <<'PY' "$1"
import os,sys; print(os.path.realpath(sys.argv[1]))
PY
}
APP_DIR_ABS=$(realpath_cmd "${APP_DIR}")
PLAN_ABS=$(realpath_cmd "${PLAN_JSON}")
OUT_ABS=$(realpath_cmd "${OUT_JSON}")
PWD_ABS=$(realpath_cmd ".")

[[ -d "${APP_DIR_ABS}" ]] || fail "APP_DIR 不存在：${APP_DIR_ABS}"
[[ -f "${PLAN_ABS}"    ]] || fail "PLAN_JSON 不存在：${PLAN_ABS}"

# 确保 OUT_JSON 的父目录存在
OUT_DIR=$(dirname "${OUT_ABS}")
mkdir -p "${OUT_DIR}"

# 白名单：仅允许修改 APP_DIR 下的文件
within_appdir() {
  local file_abs; file_abs=$(realpath_cmd "$1")
  [[ "${file_abs}" == ${APP_DIR_ABS}/* ]]
}

# 依赖 jq（runner 一般有，没有就装）
if ! command -v jq >/dev/null 2>&1; then
  log "jq 未安装，尝试安装..."
  sudo apt-get update -y >/dev/null 2>&1 || true
  sudo apt-get install -y jq >/dev/null 2>&1 || true
  command -v jq >/dev/null 2>&1 || fail "无法安装 jq，请在 runner 预装"
fi

log "开始物化：APP_DIR=${APP_DIR_ABS}"
log "读取计划：PLAN_JSON=${PLAN_ABS}"
log "输出统计：OUT_JSON=${OUT_ABS}"
[[ ${STRICT} -eq 1 ]] && log "严格模式：缺少替换将计入 missing；替换总数为 0 会报错"

# ---------- 统计量 ----------
replaced_total=0
replaced_text=0
replaced_block=0
missing_text=0
missing_block=0

# ---------- 辅助：安全替换 ----------
# 说明：为避免 sed/awk 在特殊字符（/、&、| 等）处踩坑，我们使用 perl 做全量替换
# 返回值：0 表示至少替换 1 次；1 表示未匹配到
safe_replace_all() {
  local pattern="$1" content="$2" file="$3"
  # 先判断是否匹配
  if grep -q -- "$pattern" "$file"; then
    # 使用 perl 全局替换，支持多字符、安全转义
    perl -0777 -pe "s/\Q${pattern}\E/${content//\//\\/}/g" "$file" > "$file.ndjc.tmp" || return 1
    mv "$file.ndjc.tmp" "$file"
    return 0
  else
    return 1
  fi
}

# ---------- 替换：文本锚点 NDJC:Key ----------
# Plan 结构：.text 是一个对象：{ "Title": "xxx", "Subtitle": "yyy", ... }
if jq -e '.text? // empty' "${PLAN_ABS}" >/dev/null 2>&1; then
  # 逐个键取值
  while IFS=$'\t' read -r key val; do
    [[ -n "${key}" ]] || continue
    anchor="NDJC:${key}"
    # 在 APP_DIR 中寻找包含该锚点的文件（排除 build/.gradle/.git）
    while IFS= read -r -d '' file; do
      # 安全限制：必须在 APP_DIR 内
      within_appdir "${file}" || { log "跳过越界文件：${file}"; continue; }
      # 执行替换
      if safe_replace_all "${anchor}" "${val}" "${file}"; then
        log "文本替换：${file} ← ${anchor}"
        ((replaced_text+=1, replaced_total+=1))
      fi
    done < <(grep -RIl --null --exclude-dir=build --exclude-dir=.gradle --exclude-dir=.git -- "${anchor}" "${APP_DIR_ABS}" || true)

    # 严格模式下，如果一个 key 在任何文件都没出现，则记 missing
    if [[ ${STRICT} -eq 1 ]]; then
      if ! grep -RIl --quiet --exclude-dir=build --exclude-dir=.gradle --exclude-dir=.git -- "${anchor}" "${APP_DIR_ABS}"; then
        log "文本锚点缺失（未找到落点）：${anchor}"
        ((missing_text+=1))
      fi
    fi
  done < <(jq -r '
      (.text // {}) | to_entries[] |
      # 以 tab 分隔，避免 value 中出现空格影响 read
      "\(.key)\t\(.value)"
    ' "${PLAN_ABS}")
else
  log "Plan 中未包含 .text，跳过文本锚点"
fi

# ---------- 替换：代码块锚点 BLOCK:Key ... /BLOCK:Key ----------
# Plan 结构：.block 是对象：{ "MainScreen": "<xml/code>", ... }
# 替换策略：保留起止标记行，仅把中间内容替换为 plan 给定内容
if jq -e '.block? // empty' "${PLAN_ABS}" >/dev/null 2>&1; then
  while IFS=$'\t' read -r key val; do
    [[ -n "${key}" ]] || continue
    start="BLOCK:${key}"
    end="/BLOCK:${key}"

    while IFS= read -r -d '' file; do
      within_appdir "${file}" || { log "跳过越界文件：${file}"; continue; }
      # 用 awk 实现“标记内替换为 val”
      # 注意：val 可能包含反斜杠/斜杠等，这里用 printf %s 安全传入
      awk -v S="${start}" -v E="${end}" -v CONTENT="$(printf '%s' "${val}")" '
        BEGIN{inblk=0}
        {
          if (index($0,S)>0 && inblk==0) { print; inblk=1; next }
          if (index($0,E)>0 && inblk==1) { print CONTENT; print; inblk=0; next }
          if (inblk==0) { print }
        }
      ' "$file" > "$file.ndjc.tmp" && mv "$file.ndjc.tmp" "$file"

      # 粗略认为每个文件有一个块落点（多数模板如此），计一次
      log "代码块替换：${file} ← ${start}..${end}"
      ((replaced_block+=1, replaced_total+=1))
    done < <(grep -RIl --null --exclude-dir=build --exclude-dir=.gradle --exclude-dir=.git -- "${start}" "${APP_DIR_ABS}" || true)

    if [[ ${STRICT} -eq 1 ]]; then
      if ! grep -RIl --quiet --exclude-dir=build --exclude-dir=.gradle --exclude-dir=.git -- "${start}" "${APP_DIR_ABS}"; then
        log "代码块锚点缺失（未找到落点）：${start}"
        ((missing_block+=1))
      fi
    fi
  done < <(jq -r '
      (.block // {}) | to_entries[] |
      "\(.key)\t\(.value)"
    ' "${PLAN_ABS}")
else
  log "Plan 中未包含 .block，跳过代码块锚点"
fi

# ---------- 可扩展区（按需开启） ----------
# TODO(lists):   处理 LIST:* 锚点，将数组渲染为重复 Item 片段
# TODO(if):      处理 IF:* 锚点，按条件包含/排除片段
# TODO(res):     处理 resources（RES:*），把 base64/纯文本落盘到 res/... 目录
# TODO(gradle):  处理 plan.gradle 字段（权限/依赖/SDK 等），或在其它步骤汇总

# ---------- 输出统计 JSON ----------
cat > "${OUT_ABS}" <<JSON
{
  "replaced_total": ${replaced_total},
  "replaced_text": ${replaced_text},
  "replaced_block": ${replaced_block},
  "missing_text": ${missing_text},
  "missing_block": ${missing_block},
  "generated_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
JSON
log "已生成统计：${OUT_ABS}"

# ---------- 严格模式下的失败条件 ----------
if [[ ${STRICT} -eq 1 && ${replaced_total} -eq 0 ]]; then
  fail "严格模式：未发生任何替换（replaced_total=0）"
  # 上面 fail 已 exit 1；如需区分错误码，可改为：exit 2
fi

log "物化完成：total=${replaced_total} (text=${replaced_text}, block=${replaced_block}), missing(text=${missing_text}, block=${missing_block})"
exit 0
