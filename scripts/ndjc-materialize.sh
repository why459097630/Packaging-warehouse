#!/usr/bin/env bash
# NDJC: materialize anchors from plan → apply into template app
# Usage: bash scripts/ndjc-materialize.sh <APP_DIR> <RUN_ID>
# Inputs:
#   requests/<RUN_ID>/02_plan.json
# Outputs:
#   requests/<RUN_ID>/03_apply_result.json
#   requests/<RUN_ID>/actions-summary.txt
set -euo pipefail

# 允许参数 > 环境变量 > 默认值 的优先级
APP_DIR="${1:-${APP_DIR:-app}}"
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
# lists to assoc with \x1f-joined values
lists_norm = {}
for k,v in lists.items():
    if isinstance(v,list):
        lists_norm[k] = [str(x) for x in v]
    elif v is None:
        lists_norm[k] = []
    else:
        lists_norm[k] = [str(v)]
print('declare -gA LISTS_KV=(' +
      ' '.join([f'["{k}"]="' + "\x1f".join(v) + '"' for k,v in lists_norm.items()]) + ')')

print(sh_kv_map('IFCOND_KV', iff))
PY
}

# apply python → bash
eval "$(read_plan)"

# --------- hardening ----------
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

# decode LISTS_KV value to lines
get_list_items() {
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

# ---------- block replacement ----------
replace_block_in_file() {
  local file="$1" name="$2" body="$3"
  local cnt=0 new_body
  new_body="$body"

  # xml comment region
  if perl -0777 -ne 'exit 1 unless /<!--\s*BLOCK:'"$name"'\s*-->.*?<!--\s*END_BLOCK\s*-->/s' "$file"; then
    perl -0777 -i -pe 's/<!--\s*BLOCK:'"$name"'\s*-->.*?<!--\s*END_BLOCK\s*-->/<!-- BLOCK:'"$name"' -->\n'"$new_body"'\n<!-- END_BLOCK -->/s' "$file"
    cnt=$((cnt+1))
  fi
  # // region
  if perl -0777 -ne 'exit 1 unless /\/\/\s*BLOCK:'"$name"'.*?\/\/\s*END_BLOCK/s' "$file"; then
    perl -0777 -i -pe 's/\/\/\s*BLOCK:'"$name"'.*?\/\/\s*END_BLOCK/\/\/ BLOCK:'"$name"'\n'"$new_body"'\n\/\/ END_BLOCK/s' "$file"
    cnt=$((cnt+1))
  fi
  # /* */ region
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
replace_list_in_file() {
  local file="$1" name="$2"
  local items; items="$(get_list_items "$name" || true)"
  [ -z "$items" ] && return 1

  if ! perl -0777 -ne 'exit 1 unless /LIST:'"$name"'.*?END_LIST/s' "$file"; then
    return 1
  fi

  local tmpl
  tmpl="$(perl -0777 -ne '
    if (/LIST:'"$name"'\s*(.*)\s*END_LIST/s) { print($1) }
  ' "$file")"
  [ -z "$tmpl" ] && return 1

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

# ---------- IF replacement ----------
replace_if_in_file() {
  local file="$1" name="$2" cond="$3"
  local truthy=0
  case "${cond,,}" in ""|"false"|"0"|"no"|"off") truthy=0;; *) truthy=1;; esac
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
  # //
  if perl -0777 -ne 'exit 1 unless /\/\/\s*IF:'"$name"'.*?\/\/\s*END_IF/s' "$file"; then
    hit=1
    if [ "$truthy" -eq 1 ]; then
      perl -0777 -i -pe 's/\/\/\s*IF:'"$name"'(.*?)\/\/\s*END_IF/\1/s' "$file"
    else
      perl -0777 -i -pe 's/\/\/\s*IF:'"$name"'.*?\/\/\s*END_IF/\/\/ IF:'"$name"'\n\/\/ END_IF/s' "$file"
    fi
  fi
  # /* */
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

# ---------- well-known semantic replacements ----------
apply_well_known() {
  local gradle="${APP_DIR}/build.gradle"
  local manifest="${APP_DIR}/src/main/AndroidManifest.xml"
  local strings="${APP_DIR}/src/main/res/values/strings.xml"

  local pkg="${TEXT_KV['NDJC:PACKAGE_NAME']-}"
  local label="${TEXT_KV['NDJC:APP_LABEL']-}"

  if [[ -n "${pkg:-}" ]]; then
    sed -i "s#applicationId 'com.ndjc.app'#applicationId '${pkg}'#g" "$gradle" 2>/dev/null || true
    sed -i "s#namespace 'com.ndjc.app'#namespace '${pkg}'#g" "$gradle" 2>/dev/null || true
    sed -i "s#package=\"com.ndjc.app\"#package=\"${pkg}\"#g" "$manifest" 2>/dev/null || true
    add_change "$gradle"  "NDJC:PACKAGE_NAME" 1 "semantic"
    add_change "$manifest" "NDJC:PACKAGE_NAME" 1 "semantic"
  fi

  if [[ -n "${label:-}" && -f "$strings" ]]; then
    # 兜底把模板默认文案替换为 label（如需可改为替换具体锚点名）
    sed -i "s#NDJC circle#${label}#g" "$strings" || true
    sed -i "s#Welcome to NDJC circle!#${label}#g" "$strings" || true
    add_change "$strings" "NDJC:APP_LABEL" 1 "semantic"
  fi
}

# ---------- companions writer (path 相对 APP_DIR；去掉开头的 'app/' 前缀) ----------
apply_companions() {
  jq -c '.companions[]? // empty' "$PLAN_JSON" | while read -r item; do
    rel=$(echo "$item" | jq -r '.path // empty')
    enc=$(echo "$item" | jq -r '.encoding // "utf8"')
    content=$(echo "$item" | jq -r '.content // ""')
    [ -z "$rel" ] && continue

    rel="${rel#app/}"
    abs="${APP_DIR}/${rel}"
    mkdir -p "$(dirname "$abs")"
    if [ "$enc" = "base64" ]; then
      echo "$content" | base64 -d > "$abs"
    else
      printf "%s" "$content" > "$abs"
    fi
    resources_written=$((resources_written+1))
    add_change "$abs" "companion" 1 "resource"
  done
}

# ---------- apply all ----------
apply_all() {
  local file k v
  # 0) 先做“语义替换”和 companions 落地
  apply_well_known
  apply_companions

  # 1) text
  for file in "${WORK_FILES[@]}"; do
    for k in "${!TEXT_KV[@]}"; do
      v="${TEXT_KV[$k]}"
      replace_text_in_file "$file" "$k" "$v" || true
    done
  done

  # 2) blocks
  for file in "${WORK_FILES[@]}"; do
    for k in "${!BLOCK_KV[@]}"; do
      v="${BLOCK_KV[$k]}"
      replace_block_in_file "$file" "$k" "$v" || true
    done
  done

  # 3) lists
  for file in "${WORK_FILES[@]}"; do
    for k in "${!LISTS_KV[@]}"; do
      replace_list_in_file "$file" "$k" || true
    done
  done

  # 4) if
  for file in "${WORK_FILES[@]}"; do
    for k in "${!IFCOND_KV[@]}"; do
      v="${IFCOND_KV[$k]}"
      replace_if_in_file "$file" "$k" "$v" || true
    done
  done
}

apply_all || true

# ---------- missing 统计 ----------
for k in "${!TEXT_KV[@]}";   do grep -RFl -- "${k}" "${APP_DIR}" >/dev/null 2>&1 || missing_text=$((missing_text+1)); done
for k in "${!BLOCK_KV[@]}";  do
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

# 03_apply_result.json：统计结果
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
