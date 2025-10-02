#!/usr/bin/env bash
# NDJC: materialize anchors from plan → apply into template app
# Usage: bash scripts/ndjc-materialize.sh <APP_DIR> <RUN_ID>
# Inputs:
#   requests/<RUN_ID>/02_plan.json
# Outputs:
#   requests/<RUN_ID>/03_apply_result.json
#   requests/<RUN_ID>/actions-summary.txt
set -euo pipefail

APP_DIR="${1:-app}"
RUN_ID="${2:-${RUN_ID:-}}"

if [ -z "${RUN_ID:-}" ]; then
  echo "::error::RUN_ID missing (pass as 2nd arg or set env RUN_ID)"
  exit 1
fi
REQ_DIR="requests/${RUN_ID}"
PLAN_JSON="${REQ_DIR}/02_plan.json"
APPLY_JSON="${REQ_DIR}/03_apply_result.json"
SUMMARY_TXT="${REQ_DIR}/actions-summary.txt"

if [ ! -f "${PLAN_JSON}" ]; then
  echo "::error::plan not found: ${PLAN_JSON}"
  exit 1
fi
if [ ! -d "${APP_DIR}" ]; then
  echo "::error::APP_DIR not found: ${APP_DIR}"
  exit 1
fi

# ---------- small utils ----------
ts_utc() { date -u +%Y-%m-%dT%H:%M:%SZ; }
escape_sed() { printf '%s' "$1" | sed -e 's/[\/&]/\\&/g'; }

# write a json line of change into temp changes file
CHANGES_FILE="$(mktemp)"
add_change() {
  local file="$1" marker="$2" count="$3" kind="$4"
  python - "$file" "$marker" "$count" "$kind" >> "$CHANGES_FILE" <<'PY'
import json,sys
print(json.dumps({"file":sys.argv[1],"marker":sys.argv[2],
                  "replacedCount":int(sys.argv[3]),"kind":sys.argv[4]}))
PY
}

# ---------- read plan json (python → bash) ----------
read_plan() {
  python - "$PLAN_JSON" <<'PY'
import json,sys
p = sys.argv[1]
with open(p,'r',encoding='utf-8') as f:
    plan = json.load(f)

meta = plan.get('meta',{})
text = plan.get('text',{}) or plan.get('TEXT',{}) or {}
block = plan.get('block',{}) or plan.get('BLOCK',{}) or {}
lists = plan.get('lists',{}) or plan.get('LISTS',{}) or {}
iff   = plan.get('if',{})   or plan.get('IF',{})   or {}

def sh_kv_map(name, d):
    # declare -A NAME=(["k"]="v" ...)
    out = [f'declare -gA {name}=(']
    for k,v in d.items():
        k = str(k); v = '' if v is None else str(v)
        v = v.replace('\n','\\n')
        out.append(f'["{k}"]="{v}"')
    out.append(')')
    return ' '.join(out)

print(f'META_TEMPLATE="{meta.get("template","")}"')
print(f'META_APPNAME="{meta.get("appName","")}"')
print(f'META_PKG="{meta.get("packageId","")}"')
print(f'META_MODE="{meta.get("mode","")}"')

print(sh_kv_map('TEXT_KV', text))
print(sh_kv_map('BLOCK_KV', block))
# lists: ensure list of strings; also coerce primitives to single item
lists_norm = {}
for k,v in lists.items():
    if isinstance(v,list):
        lists_norm[k] = [str(x) for x in v]
    elif v is None:
        lists_norm[k] = []
    else:
        lists_norm[k] = [str(v)]
# encode list map as declare -A with \x1f join
print('declare -gA LISTS_KV=(' +
      ' '.join([f'["{k}"]="' + "\x1f".join(v) + '"' for k,v in lists_norm.items()]) + ')')

print(sh_kv_map('IFCOND_KV', iff))
PY
}

# apply python → bash
eval "$(read_plan)"

# --------- hardening: ensure 4 kvs are assoc arrays ----------
ensure_assoc() {
  local name="$1"
  if ! declare -p "$name" >/dev/null 2>&1; then
    eval "declare -gA $name=()"
  elif ! declare -p "$name" 2>/dev/null | grep -q 'declare \-A'; then
    eval "unset $name; declare -gA $name=()"
  fi
}
ensure_assoc TEXT_KV
ensure_assoc BLOCK_KV
ensure_assoc LISTS_KV
ensure_assoc IFCOND_KV

# decode LISTS_KV values (split by \x1f) to bash arrays when需要
get_list_items() {
  # $1 key  -> print items one per line
  local raw="${LISTS_KV[$1]-}"
  [ -z "$raw" ] && return 0
  python - "$raw" <<'PY'
import sys
print("\n".join(sys.argv[1].split("\x1f")))
PY
}

# ---------- counters ----------
replaced_text=0
replaced_block=0
replaced_list=0
replaced_if=0
resources_written=0
hooks_applied=0
missing_text=0
missing_block=0
missing_list=0
missing_if=0

# ---------- file selection ----------
# 普遍地在模板中常见的后缀；可按需补
mapfile -t WORK_FILES < <(find "$APP_DIR" -type f \( \
  -name "*.xml" -o -name "*.kt" -o -name "*.kts" -o -name "*.gradle" -o \
  -name "*.pro" -o -name "AndroidManifest.xml" -o -name "*.txt" \
  \) | LC_ALL=C sort)

# ---------- text replacement ----------
replace_text_in_file() {
  local file="$1" key="$2" val="$3"
  local before count
  before=$(grep -o -n -F "$key" "$file" | wc -l | tr -d ' ')
  [ "$before" -eq 0 ] && return 1
  local vv; vv="$(escape_sed "$val")"
  sed -i "s/${key}/${vv}/g" "$file"
  local after
  after=$(grep -o -n -F "$key" "$file" | wc -l | tr -d ' ')
  count=$((before-after))
  [ "$count" -lt 0 ] && count=0
  if [ "$count" -gt 0 ]; then
    replaced_text=$((replaced_text+count))
    add_change "$file" "$key" "$count" "text"
  fi
  return 0
}

# ---------- block replacement (3 styles) ----------
# XML:   <!-- BLOCK:NAME --> ... <!-- END_BLOCK -->
# Line:  // BLOCK:NAME       ... // END_BLOCK
# C:     /* BLOCK:NAME */    ... /* END_BLOCK */
replace_block_in_file() {
  local file="$1" name="$2" body="$3"
  local cnt=0 new_body
  new_body="$body"

  # xml comment region
  if perl -0777 -ne 'exit 1 unless /<!--\s*BLOCK:'"$name"'\s*-->.*?<!--\s*END_BLOCK\s*-->/s' "$file"; then
    perl -0777 -i -pe 's/<!--\s*BLOCK:'"$name"'\s*-->.*?<!--\s*END_BLOCK\s*-->/<!-- BLOCK:'"$name"' -->\n'"$new_body"'\n<!-- END_BLOCK -->/s' "$file"
    cnt=$((cnt+1))
  fi
  # // line comment region
  if perl -0777 -ne 'exit 1 unless /\/\/\s*BLOCK:'"$name"'.*?\/\/\s*END_BLOCK/s' "$file"; then
    perl -0777 -i -pe 's/\/\/\s*BLOCK:'"$name"'.*?\/\/\s*END_BLOCK/\/\/ BLOCK:'"$name"'\n'"$new_body"'\n\/\/ END_BLOCK/s' "$file"
    cnt=$((cnt+1))
  fi
  # /* */ c-style region
  if perl -0777 -ne 'exit 1 unless /\/\*\s*BLOCK:'"$name"'\s*\*\/.*?\/\*\s*END_BLOCK\s*\*\//s' "$file"; then
    perl -0777 -i -pe 's/\/\*\s*BLOCK:'"$name"'\s*\*\/.*?\/\*\s*END_BLOCK\s*\*\//\/\* BLOCK:'"$name"' \*\/\n'"$new_body"'\n\/\* END_BLOCK \*\//s' "$file"
    cnt=$((cnt+1))
  fi

  if [ "$cnt" -gt 0 ]; then
    replaced_block=$((replaced_block+cnt))
    add_change "$file" "BLOCK:${name}" "$cnt" "block"
    return 0
  fi
  return 1
}

# ---------- list replacement ----------
# Region form:
#   ... LIST:NAME ... (template chunk) ... END_LIST ...
# The template chunk will be duplicated per item, replacing ${ITEM}
replace_list_in_file() {
  local file="$1" name="$2"
  local items; items="$(get_list_items "$name" || true)"
  [ -z "$items" ] && return 1

  # capture region and inner template
  if ! perl -0777 -ne 'exit 1 unless /LIST:'"$name"'.*?END_LIST/s' "$file"; then
    return 1
  fi

  local tmpl
  tmpl="$(perl -0777 -ne '
    if (/LIST:'"$name"'\s*(.*)\s*END_LIST/s) { print($1) }
  ' "$file")"

  [ -z "$tmpl" ] && return 1

  # render with ${ITEM}
  local rendered=""
  while IFS= read -r it; do
    rendered+=$(python - <<PY
import os,sys
tmpl = """$tmpl"""
print(tmpl.replace("\${ITEM}", """$it"""))
PY
)
    rendered+=$'\n'
  done <<<"$items"

  perl -0777 -i -pe 's/LIST:'"$name"'.*?END_LIST/LIST:'"$name"'\n'"$rendered"'END_LIST/s' "$file"
  replaced_list=$((replaced_list+1))
  add_change "$file" "LIST:${name}" 1 "list"
  return 0
}

# ---------- IF replacement (very simple on/off) ----------
# Region form (任一风格均可):
#   IF:NAME ... END_IF
# 若 IFCOND_KV["NAME"] 为真(非空且不为 "false"/"0") → 保留区域；否则清空区域
replace_if_in_file() {
  local file="$1" name="$2" cond="$3"
  local truthy=0
  case "${cond,,}" in
    ""|"false"|"0"|"no"|"off") truthy=0;;
    *) truthy=1;;
  esac
  local hit=0
  # xml
  if perl -0777 -ne 'exit 1 unless /<!--\s*IF:'"$name"'\s*-->.*?<!--\s*END_IF\s*-->/s' "$file"; then
    hit=1
    if [ "$truthy" -eq 1 ]; then
      perl -0777 -i -pe 's/<!--\s*IF:'"$name"'\s*-->(.*?)<!--\s*END_IF\s*-->/\1/s' "$file"
    else
      perl -0777 -i -pe 's/<!--\s*IF:'"$name"'\s*-->.*?<!--\s*END_IF\s*-->/<!-- IF:'"$name"' -->\n<!-- END_IF -->/s' "$file"
    fi
  fi
  # // line
  if perl -0777 -ne 'exit 1 unless /\/\/\s*IF:'"$name"'.*?\/\/\s*END_IF/s' "$file"; then
    hit=1
    if [ "$truthy" -eq 1 ]; then
      perl -0777 -i -pe 's/\/\/\s*IF:'"$name"'(.*?)\/\/\s*END_IF/\1/s' "$file"
    else
      perl -0777 -i -pe 's/\/\/\s*IF:'"$name"'.*?\/\/\s*END_IF/\/\/ IF:'"$name"'\n\/\/ END_IF/s' "$file"
    fi
  fi
  # /* */ c-style
  if perl -0777 -ne 'exit 1 unless /\/\*\s*IF:'"$name"'\s*\*\/.*?\/\*\s*END_IF\s*\*\//s' "$file"; then
    hit=1
    if [ "$truthy" -eq 1 ]; then
      perl -0777 -i -pe 's/\/\*\s*IF:'"$name"'\s*\*\/(.*?)\/\*\s*END_IF\s*\*\//\1/s' "$file"
    else
      perl -0777 -i -pe 's/\/\*\s*IF:'"$name"'\s*\*\/.*?\/\*\s*END_IF\s*\*\//\/\* IF:'"$name"' \*\/\n\/\* END_IF \*\//s' "$file"
    fi
  fi
  if [ "$hit" -eq 1 ]; then
    replaced_if=$((replaced_if+1))
    add_change "$file" "IF:${name}" 1 "if"
    return 0
  fi
  return 1
}

# ---------- apply all ----------
apply_all() {
  local hit any file k v

  # 1) text
  for file in "${WORK_FILES[@]}"; do
    for k in "${!TEXT_KV[@]}"; do
      v="${TEXT_KV[$k]}"
      if replace_text_in_file "$file" "$k" "$v"; then
        : # counted inside
      else
        :
      fi
    done
  done

  # 2) blocks
  for file in "${WORK_FILES[@]}"; do
    for k in "${!BLOCK_KV[@]}"; do
      v="${BLOCK_KV[$k]}"
      if replace_block_in_file "$file" "$k" "$v"; then
        : 
      else
        :
      fi
    done
  done

  # 3) lists
  for file in "${WORK_FILES[@]}"; do
    for k in "${!LISTS_KV[@]}"; do
      if replace_list_in_file "$file" "$k"; then
        : 
      else
        :
      fi
    done
  done

  # 4) if
  for file in "${WORK_FILES[@]}"; do
    for k in "${!IFCOND_KV[@]}"; do
      v="${IFCOND_KV[$k]}"
      if replace_if_in_file "$file" "$k" "$v"; then
        : 
      else
        :
      fi
    done
  done
}

apply_all || true

# 补充 missing 统计（只统计“完全未命中”的锚点个数）
for k in "${!TEXT_KV[@]}";   do grep -RFl -- "${k}" "${APP_DIR}" >/dev/null 2>&1 || missing_text=$((missing_text+1)); done
for k in "${!BLOCK_KV[@]}";  do
  # 三类包围式任一命中则认为存在
  if ! grep -RIl -E "(<!-- *BLOCK:${k} *-->|// *BLOCK:${k}|/\* *BLOCK:${k} *\*/)" "${APP_DIR}" >/dev/null 2>&1; then
    missing_block=$((missing_block+1))
  fi
done
for k in "${!LISTS_KV[@]}";  do grep -RIl "LIST:${k}" "${APP_DIR}" >/dev/null 2>&1 || missing_list=$((missing_list+1)); done
for k in "${!IFCOND_KV[@]}"; do
  if ! grep -RIl -E "(<!-- *IF:${k} *-->|// *IF:${k}|/\* *IF:${k} *\*/)" "${APP_DIR}" >/dev/null 2>&1; then
    missing_if=$((missing_if+1))
  fi
done

replaced_total=$((replaced_text+replaced_block+replaced_list+replaced_if))

mkdir -p "$REQ_DIR"

# 03_apply_result.json （与之前结构兼容：只汇总计数）
cat > "$APPLY_JSON" <<JSON
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
  "generated_at": "$(ts_utc)"
}
JSON

# actions-summary.txt
{
  echo "branch=${GITHUB_REF_NAME:-main}"
  echo "variant=unknown"
  echo "apk="
  echo "classes="
  echo "missing_text=${missing_text}"
  echo "missing_block=${missing_block}"
  echo "ts=$(ts_utc)"
} > "$SUMMARY_TXT"

# 输出统计到日志
echo "NDJC materialize: total=${replaced_total} text=${replaced_text} block=${replaced_block} list=${replaced_list} if=${replaced_if}"
echo "missing: text=${missing_text} block=${missing_block} list=${missing_list} if=${missing_if}"
