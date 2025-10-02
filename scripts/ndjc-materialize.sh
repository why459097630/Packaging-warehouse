#!/usr/bin/env bash
# NDJC materialize (plan -> template) with APPLY RESULT persisted.
# 作用：
#  - 读取 02_plan.json
#  - 在模板 APP_DIR 内执行锚点替换（文本/块/列表）
#  - 生成完整的 03_apply_result.json（含 changes、统计计数）
# 用法（CI 已按此调用）：
#   scripts/ndjc-materialize.sh "<APP_DIR>" "<PLAN_JSON>" "<OUT_JSON>"
#
# 备注：
#  - 文本锚点：直接把 "NDJC:XXXX" 全局替换为 value
#  - 块锚点：
#      XML:  <!-- BLOCK:NAME --> ... <!-- END_BLOCK -->
#      代码: // BLOCK:NAME ... // END_BLOCK  或  /* BLOCK:NAME */ ... /* END_BLOCK */
#  - 列表锚点：将 "LIST:NAME" 替换为渲染后的列表字符串
#  - 统计会写入 03_apply_result.json，后续 Guard 会据此判断是否“已替换”

set -euo pipefail

APP_DIR="${1:-templates/circle-basic/app}"
PLAN_JSON="${2:-requests/unknown/02_plan.json}"
OUT_JSON="${3:-requests/unknown/03_apply_result.json}"

mkdir -p "$(dirname "$OUT_JSON")"

log() { echo "[$(date -u +%H:%M:%S)] $*"; }

# ----------- 读取 plan（交给 Python 做解析） -----------
# 输出为 shell 可 source 的变量/数组定义
read_plan() {
python - "$PLAN_JSON" <<'PY'
import json, sys, shlex

p = sys.argv[1]
with open(p, 'r', encoding='utf-8') as f:
    plan = json.load(f)

meta = plan.get('meta', {})
text = plan.get('text', {}) or plan.get('anchors',{}).get('text',{})
block = plan.get('block', {}) or plan.get('anchors',{}).get('block',{})
lists = plan.get('lists', {}) or plan.get('anchors',{}).get('list',{})
iff   = plan.get('if', {}) or plan.get('anchors',{}).get('if',{})
# 这里先不展开 resources / hooks / gradle；若有需要可继续补齐

def sh_var(name, val):
    print(f'{name}={shlex.quote(val)}')

def sh_arr(name, mapping):
    # 输出 k=v 的一维“伪数组”（key|value 换行），bash 再解析
    print(f'{name}_KV<<__KV__')
    for k, v in (mapping or {}).items():
        if isinstance(v, (list,tuple)):
            # 列表以 JSON 串输出，后面 bash 再用 python 解析
            vv = json.dumps(v, ensure_ascii=False)
            print(f'{k}|{vv}')
        else:
            vv = '' if v is None else str(v)
            print(f'{k}|{vv}')
    print('__KV__')

sh_var("META_TEMPLATE", meta.get("template",""))
sh_var("META_APPNAME",  meta.get("appName",""))
sh_var("META_PKG",      meta.get("packageId",""))
sh_var("META_MODE",     meta.get("mode",""))

sh_arr("TEXT",  text)
sh_arr("BLOCK", block)
sh_arr("LISTS", lists)
sh_arr("IFCOND", iff)
PY
}

# 接收 Python 产生的变量
eval "$(read_plan)"

# 将 *_KV 解析为 bash 关联“字典”的文本形式（用 grep 搜索时再逐项处理）
TEXT_KV="${TEXT_KV-}"
BLOCK_KV="${BLOCK_KV-}"
LISTS_KV="${LISTS_KV-}"
IFCOND_KV="${IFCOND_KV-}"

# 统计
replaced_text=0
replaced_block=0
replaced_list=0
replaced_if=0
resources_written=0
hooks_applied=0

# 记录 changes（后面写 JSON）
CHANGES_FILE="$(mktemp)"
: > "$CHANGES_FILE"

# 追加 change 记录
add_change() {
  local file="$1" marker="$2" count="$3" kind="$4"
  python - <<PY >> "$CHANGES_FILE"
import json,sys
print(json.dumps({"file":sys.argv[1],"marker":sys.argv[2],"replacedCount":int(sys.argv[3]),"kind":sys.argv[4]}, ensure_ascii=False))
PY
  "$file" "$marker" "$count" "$kind" >/dev/null 2>&1 || true
}

# ---- 工具函数 ----
escape_sed() {
  printf '%s' "$1" | sed -e 's/[&/\]/\\&/g'
}

# 渲染列表为字符串：简单判断上下文（行尾是否在 XML 或代码里）
render_list_payload() {
  # $1 = 原始列表的 JSON 字符串
  python - <<'PY' "$1"
import json,sys
arr = json.loads(sys.argv[1] or '[]')
# 默认用逗号加空格连接
print(', '.join([str(x) for x in arr]))
PY
}

# 块替换：同时支持 XML 注释块、//注释块、/* 块注释 */
replace_block_in_file() {
  local file="$1" name="$2" new_body="$3"
  local cnt=0

  # XML 注释：<!-- BLOCK:NAME --> ... <!-- END_BLOCK -->
  if grep -q "<!--[[:space:]]*BLOCK:${name}[[:space:]]*-->" "$file"; then
    perl -0777 -i -pe \
      "s/<!--\\s*BLOCK:${name}\\s*-->(.*?)<!--\\s*END_BLOCK\\s*-->/<!-- BLOCK:${name} -->\\n${new_body}\\n<!-- END_BLOCK -->/gs" \
      "$file" && cnt=1
  fi

  # 行注释：// BLOCK:NAME ... // END_BLOCK
  if grep -q "//[[:space:]]*BLOCK:${name}" "$file"; then
    perl -0777 -i -pe \
      "s/\\/\\/\\s*BLOCK:${name}.*?\\/\\/\\s*END_BLOCK/\\/\\/ BLOCK:${name}\\n${new_body}\\n\\/\\/ END_BLOCK/gs" \
      "$file" && cnt=$((cnt+1))
  fi

  # 块注释：/* BLOCK:NAME */ ... /* END_BLOCK */
  if grep -q "/\\*[[:space:]]*BLOCK:${name}" "$file"; then
    perl -0777 -i -pe \
      "s/\\/\\*\\s*BLOCK:${name}.*?\\*\\/\\s*.*?\\/\\*\\s*END_BLOCK\\s*\\*\\//\\/\\* BLOCK:${name} \\*\\/\\n${new_body}\\n/* END_BLOCK *\\//gs" \
      "$file" && cnt=$((cnt+1))
  fi

  echo "$cnt"
}

# ---- 1) 文本锚点 NDJC:XXXX ----
apply_text() {
  local k v k_pat files c file val_sed
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    k="${line%%|*}"; v="${line#*|}"
    k_pat="NDJC:${k}"
    # 找到包含此锚点的文件
    while IFS= read -r file; do
      [ -z "$file" ] && continue
      c="$(grep -o "$k_pat" "$file" | wc -l | tr -d ' ')"
      if [ "$c" -gt 0 ]; then
        val_sed="$(escape_sed "$v")"
        sed -i "s/${k_pat}/${val_sed}/g" "$file"
        add_change "$file" "$k_pat" "$c" "text"
        replaced_text=$((replaced_text + c))
      fi
    done < <(grep -RIl --exclude-dir=".git" --exclude="*.png" --exclude="*.jpg" "$k_pat" "$APP_DIR" || true)
  done <<< "$TEXT_KV"
}

# ---- 2) 块锚点 BLOCK:XXXX / END_BLOCK ----
apply_block() {
  local k v files file cnt
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    k="${line%%|*}"; v="${line#*|}"

    # 可在此对不同块名做专门渲染（示例：ROUTE_HOME 生成特定 Kotlin 片段）
    new_body="$v"

    while IFS= read -r file; do
      [ -z "$file" ] && continue
      cnt="$(replace_block_in_file "$file" "$k" "$new_body")"
      if [ "$cnt" -gt 0 ]; then
        add_change "$file" "BLOCK:${k}" "$cnt" "block"
        replaced_block=$((replaced_block + cnt))
      fi
    done < <(grep -RIl "BLOCK:${k}" "$APP_DIR" || true)

  done <<< "$BLOCK_KV"
}

# ---- 3) 列表锚点 LIST:XXXX ----
apply_lists() {
  local k v files file payload cnt
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    k="${line%%|*}"; v="${line#*|}"

    payload="$(render_list_payload "$v")"
    pat="LIST:${k}"

    while IFS= read -r file; do
      [ -z "$file" ] && continue
      # 简单策略：把所在行的标记整体替换为渲染结果
      if grep -q "$pat" "$file"; then
        # 尝试保留行前缩进
        awk -v P="$pat" -v REP="$payload" '
          index($0,P){
            pre=substr($0,1,index($0,P)-1);
            print pre REP; next
          } {print}
        ' "$file" > "$file.__ndjc_tmp__" && mv "$file.__ndjc_tmp__" "$file"
        add_change "$file" "$pat" 1 "list"
        replaced_list=$((replaced_list + 1))
      fi
    done < <(grep -RIl "$pat" "$APP_DIR" || true)

  done <<< "$LISTS_KV"
}

# ---- 4) 条件锚点 IF:XXXX（占位：当前仅统计，不做内容裁剪）----
apply_if() {
  # 如果后续需要根据 IF 条件包裹/删除代码，在这里扩展
  if [ -n "${IFCOND_KV:-}" ]; then
    # 统计存在的 IF 标记数量
    local c
    c="$(grep -RI "\bIF:" "$APP_DIR" | wc -l | tr -d ' ' || true)"
    if [ "$c" -gt 0 ]; then
      replaced_if=$((replaced_if + 0))
    fi
  fi
}

# ----------------- 执行替换 -----------------
log "开始物化：APP_DIR=${APP_DIR}"
log "读取 plan：${PLAN_JSON}"
apply_text
apply_block
apply_lists
apply_if

total=$((replaced_text + replaced_block + replaced_list + replaced_if + resources_written + hooks_applied))
log "替换完成：total=${total} | text=${replaced_text} block=${replaced_block} list=${replaced_list} if=${replaced_if}"

# ----------------- 写 03_apply_result.json -----------------
python - "$OUT_JSON" "$CHANGES_FILE" <<'PY'
import json, sys, time, pathlib
out = sys.argv[1]
changes_file = sys.argv[2]

changes = []
with open(changes_file, "r", encoding="utf-8") as f:
    for ln in f:
        ln = ln.strip()
        if ln:
            changes.append(json.loads(ln))

# 从环境中拿一些上下文
run_id = ""
try:
    # requests/<runId>/03_apply_result.json 的父目录名就是 runId
    p = pathlib.Path(out).resolve()
    run_id = p.parent.name
except Exception:
    pass

summary = {
    "runId": run_id or "",
    "status": "applied",
    "template": "",
    "appTitle": "",
    "packageName": "",
    "note": "Apply result generated by ndjc-materialize.sh",
    "replaced_total": sum(int(c.get("replacedCount",0)) for c in changes),
    "replaced_text":  sum(int(c.get("replacedCount",0)) for c in changes if c.get("kind")=="text"),
    "replaced_block": sum(int(c.get("replacedCount",0)) for c in changes if c.get("kind")=="block"),
    "replaced_list":  sum(int(c.get("replacedCount",0)) for c in changes if c.get("kind")=="list"),
    "replaced_if":    0,
    "resources_written": 0,
    "hooks_applied": 0,
    "changes": changes,
    "generated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
}

with open(out, "w", encoding="utf-8") as f:
    json.dump(summary, f, ensure_ascii=False, indent=2)
PY

log "已写入 ${OUT_JSON}"
