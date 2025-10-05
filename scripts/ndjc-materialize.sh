#!/usr/bin/env bash
# NDJC: materialize anchors from plan → apply into template app
# Usage: bash scripts/ndjc-materialize.sh <APP_DIR> <RUN_ID>
set -euo pipefail

APP_DIR="${1:-app}"
RUN_ID="${2:-${RUN_ID:-}}"

if [ -z "${RUN_ID:-}" ]; then
  echo "::error::RUN_ID missing (pass as 2nd arg or set env RUN_ID)"
  exit 1
fi

REQ_DIR="requests/${RUN_ID}"
PLAN_JSON="${PLAN_JSON:-${REQ_DIR}/02_plan.json}"
PLAN_JSON_SAN="${REQ_DIR}/02_plan.sanitized.json"
APPLY_JSON="${APPLY_JSON:-${REQ_DIR}/03_apply_result.json}"
SUMMARY_TXT="${REQ_DIR}/actions-summary.txt"

mkdir -p "${REQ_DIR}" build-logs
LOG_FILE="build-logs/materialize.log"
: > "${LOG_FILE}"

log()       { printf '%s %s\n' "[$(date +%H:%M:%S)]" "$*" | tee -a "${LOG_FILE}"; }
ts_utc()    { date -u +%Y-%m-%dT%H:%M:%SZ; }
escape_sed(){ printf '%s' "$1" | sed -e 's/[\/&]/\\&/g'; }

log "env: PLAN_JSON=${PLAN_JSON}"
log "env: PLAN_JSON_SAN=${PLAN_JSON_SAN}"
log "env: APPLY_JSON=${APPLY_JSON}"
log "env: APP_DIR=${APP_DIR}"
log "env: RUN_ID=${RUN_ID}"

# 0) Sanitize（不报错也继续）
log "::group::Sanitize plan"
npx -y tsx lib/ndjc/sanitize/index.ts --plan="${PLAN_JSON}" --out="${PLAN_JSON_SAN}" || true
log "::endgroup::"

# fallback
if [ ! -f "${PLAN_JSON_SAN}" ]; then
  log "sanitized plan not found, fallback to original: ${PLAN_JSON}"
  PLAN_JSON_SAN="${PLAN_JSON}"
fi

# 采样（before）
ANCHORS_BEFORE="build-logs/anchors.before.txt"
{ grep -R --line-number -E 'NDJC:|BLOCK:|LIST:|HOOK:|IF:' "${APP_DIR}" 2>/dev/null || true; } > "${ANCHORS_BEFORE}" || true
log "anchors before: $(wc -l < "${ANCHORS_BEFORE}" | tr -d ' ')"

# 1) 读取 plan（兼容新旧 schema）—— ★ 这里做 Shell 安全转义
read_plan() {
  python - "$PLAN_JSON_SAN" <<'PY'
import json,sys

def shq(s: str) -> str:
    # 单引号包裹，内部单引号 → '"'"'
    return "'" + s.replace("'", "'\"'\"'") + "'"

p = sys.argv[1]
with open(p,'r',encoding='utf-8') as f:
    plan = json.load(f)

meta   = plan.get('meta') or plan.get('metadata') or {}
text   = plan.get('text') or {}
block  = plan.get('block') or {}
lists  = plan.get('lists') or {}
iff    = plan.get('if') or {}
hooks  = plan.get('hooks') or {}

# legacy 兼容
if not text and isinstance(plan.get('anchors'), dict):
    text = plan['anchors']
if not block and isinstance(plan.get('blocks'), dict):
    block = plan['blocks']
if not lists and isinstance(plan.get('lists'), dict):
    lists = plan['lists']
if not iff and isinstance(plan.get('conditions'), dict):
    iff = plan['conditions']
hooks = hooks or {}

def sh_kv_map(name: str, d: dict) -> str:
    out = [f'declare -gA {name}=(']
    for k,v in (d or {}).items():
        k = str(k)
        v = '' if v is None else str(v)
        v = v.replace("\n", "\\n")           # 真实换行 → 字面量 \n
        out.append(f'["{k}"]={shq(v)}')      # 值用单引号包裹并转义
    out.append(')')
    return ' '.join(out)

def to_list_map(name: str, d: dict) -> str:
    out = [f'declare -gA {name}=(']
    for k,v in (d or {}).items():
        if isinstance(v, list):
            items = [str(x) for x in v]
        elif v is None:
            items = []
        else:
            items = [str(v)]
        joined = "\x1f".join(items).replace("\n","\\n")
        out.append(f'["{k}"]={shq(joined)}')
    out.append(')')
    return ' '.join(out)

print(sh_kv_map('TEXT_KV', text))
print(sh_kv_map('BLOCK_KV', block))
print(to_list_map('LISTS_KV', lists))
print(sh_kv_map('IFCOND_KV', iff))
print(to_list_map('HOOKS_KV', hooks))
print(f'echo "loaded counts => text:{len(text)} block:{len(block)} lists:{len(lists)} if:{len(iff)} hooks:{len(hooks)}" >> "build-logs/materialize.log"')
PY
}

eval "$(read_plan)"

# 1.5) 保障：若不是关联数组强制重建
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
ensure_assoc HOOKS_KV

get_list_items() {
  local raw="${LISTS_KV[$1]-}"
  [ -z "$raw" ] && return 0
  python - "$raw" <<'PY'
import sys
print("\n".join(sys.argv[1].split("\x1f")))
PY
}
get_hook_body() {
  local raw="${HOOKS_KV[$1]-}"
  [ -z "$raw" ] && return 0
  python - "$raw" <<'PY'
import sys
print("\n".join(sys.argv[1].split("\x1f")))
PY
}

# 2) 待处理文件
mapfile -t WORK_FILES < <(find "$APP_DIR" -type f \( \
  -name "*.xml" -o -name "*.kt" -o -name "*.kts" -o -name "*.gradle" -o \
  -name "*.pro" -o -name "AndroidManifest.xml" -o -name "*.txt" \
  \) | LC_ALL=C sort)

# 3) 替换实现
replaced_text=0
replaced_block=0
replaced_list=0
replaced_if=0
hooks_applied=0
missing_text=0
missing_block=0
missing_list=0
missing_if=0

replace_text_in_file() {
  local file="$1" key="$2" val="$3"
  local before after count vv
  before=$(grep -o -n -F "$key" "$file" | wc -l | tr -d ' ')
  [ "$before" -eq 0 ] && return 1
  vv="$(escape_sed "$val")"
  sed -i "s/${key}/${vv}/g" "$file"
  after=$(grep -o -n -F "$key" "$file" | wc -l | tr -d ' ')
  count=$((before-after)); [ "$count" -lt 0 ] && count=0
  [ "$count" -gt 0 ] && replaced_text=$((replaced_text+count))
  return 0
}

replace_block_in_file() {
  local file="$1" name="$2" body="$3"
  local cnt=0
  if perl -0777 -ne 'exit 1 unless /<!--\s*BLOCK:'"$name"'\s*-->.*?<!--\s*END_BLOCK\s*-->/s' "$file"; then
    perl -0777 -i -pe 's/<!--\s*BLOCK:'"$name"'\s*-->.*?<!--\s*END_BLOCK\s*-->/<!-- BLOCK:'"$name"' -->\n'"$body"'\n<!-- END_BLOCK -->/s' "$file"; cnt=$((cnt+1))
  fi
  if perl -0777 -ne 'exit 1 unless /\/\/\s*BLOCK:'"$name"'.*?\/\/\s*END_BLOCK/s' "$file"; then
    perl -0777 -i -pe 's/\/\/\s*BLOCK:'"$name"'.*?\/\/\s*END_BLOCK/\/\/ BLOCK:'"$name"'\n'"$body"'\n\/\/ END_BLOCK/s' "$file"; cnt=$((cnt+1))
  fi
  if perl -0777 -ne 'exit 1 unless /\/\*\s*BLOCK:'"$name"'\s*\*\/.*?\/\*\s*END_BLOCK\s*\*\//s' "$file"; then
    perl -0777 -i -pe 's/\/\*\s*BLOCK:'"$name"'\s*\*\/.*?\/\*\s*END_BLOCK\s*\*\//\/\* BLOCK:'"$name"' \*\/\n'"$body"'\n\/\* END_BLOCK \*\//s' "$file"; cnt=$((cnt+1))
  fi
  [ "$cnt" -gt 0 ] && { replaced_block=$((replaced_block+cnt)); return 0; }
  return 1
}

replace_list_in_file() {
  local file="$1" name="$2"
  local items; items="$(get_list_items "$name" || true)"
  [ -z "$items" ] && return 1
  if ! perl -0777 -ne 'exit 1 unless /LIST:'"$name"'.*?END_LIST/s' "$file"; then
    return 1
  fi
  local tmpl
  tmpl="$(perl -0777 -ne 'if (/LIST:'"$name"'\s*(.*)\s*END_LIST/s) { print($1) }' "$file")"
  [ -z "$tmpl" ] && return 1

  local rendered=""
  while IFS= read -r it; do
    rendered+=$(python - <<PY
import sys
tmpl = """$tmpl"""
print(tmpl.replace("\${ITEM}", """$it"""))
PY
)
    rendered+=$'\n'
  done <<<"$items"

  perl -0777 -i -pe 's/LIST:'"$name"'.*?END_LIST/LIST:'"$name"'\n'"$rendered"'END_LIST/s' "$file"
  replaced_list=$((replaced_list+1))
  return 0
}

replace_if_in_file() {
  local file="$1" name="$2" cond="$3"
  local truthy=0; case "${cond,,}" in ""|"false"|"0"|"no"|"off") truthy=0;; *) truthy=1;; esac
  local hit=0
  if perl -0777 -ne 'exit 1 unless /<!--\s*IF:'"$name"'\s*-->.*?<!--\s*END_IF\s*-->/s' "$file"; then
    hit=1; if [ "$truthy" -eq 1 ]; then
      perl -0777 -i -pe 's/<!--\s*IF:'"$name"'\s*-->(.*?)<!--\s*END_IF\s*-->/\1/s' "$file"
    else
      perl -0777 -i -pe 's/<!--\s*IF:'"$name"'\s*-->.*?<!--\s*END_IF\s*-->/<!-- IF:'"$name"' -->\n<!-- END_IF -->/s' "$file"
    fi
  fi
  if perl -0777 -ne 'exit 1 unless /\/\/\s*IF:'"$name"'.*?\/\/\s*END_IF/s' "$file"; then
    hit=1; if [ "$truthy" -eq 1 ]; then
      perl -0777 -i -pe 's/\/\/\s*IF:'"$name"'(.*?)\/\/\s*END_IF/\1/s' "$file"
    else
      perl -0777 -i -pe 's/\/\/\s*IF:'"$name"'.*?\/\/\s*END_IF/\/\/ IF:'"$name"'\n\/\/ END_IF/s' "$file"
    fi
  fi
  if perl -0777 -ne 'exit 1 unless /\/\*\s*IF:'"$name"'\s*\*\/.*?\/\*\s*END_IF\s*\*\//s' "$file"; then
    hit=1; if [ "$truthy" -eq 1 ]; then
      perl -0777 -i -pe 's/\/\*\s*IF:'"$name"'\s*\*\/(.*?)\/\*\s*END_IF\s*\*\//\1/s' "$file"
    else
      perl -0777 -i -pe 's/\/\*\s*IF:'"$name"'\s*\*\/.*?\/\*\s*END_IF\s*\*\//\/\* IF:'"$name"' \*\/\n\/\* END_IF \*\//s' "$file"
    fi
  fi
  [ "$hit" -eq 1 ] && { replaced_if=$((replaced_if+1)); return 0; }
  return 1
}

replace_hook_in_file() {
  local file="$1" name="$2" body="$3"
  local cnt=0
  if perl -0777 -ne 'exit 1 unless /<!--\s*HOOK:'"$name"'\s*-->.*?<!--\s*END_HOOK\s*-->/s' "$file"; then
    perl -0777 -i -pe 's/<!--\s*HOOK:'"$name"'\s*-->.*?<!--\s*END_HOOK\s*-->/<!-- HOOK:'"$name"' -->\n'"$body"'\n<!-- END_HOOK -->/s' "$file"; cnt=$((cnt+1))
  fi
  if perl -0777 -ne 'exit 1 unless /\/\/\s*HOOK:'"$name"'.*?\/\/\s*END_HOOK/s' "$file"; then
    perl -0777 -i -pe 's/\/\/\s*HOOK:'"$name"'.*?\/\/\s*END_HOOK/\/\/ HOOK:'"$name"'\n'"$body"'\n\/\/ END_HOOK/s' "$file"; cnt=$((cnt+1))
  fi
  if perl -0777 -ne 'exit 1 unless /\/\*\s*HOOK:'"$name"'\s*\*\/.*?\/\*\s*END_HOOK\s*\*\//s' "$file"; then
    perl -0777 -i -pe 's/\/\*\s*HOOK:'"$name"'\s*\*\/.*?\/\*\s*END_HOOK\s*\*\//\/\* HOOK:'"$name"' \*\/\n'"$body"'\n\/\* END_HOOK \*\//s' "$file"; cnt=$((cnt+1))
  fi
  [ "$cnt" -gt 0 ] && { hooks_applied=$((hooks_applied+cnt)); return 0; }
  return 1
}

# 4) 应用
apply_all() {
  local file k v
  for file in "${WORK_FILES[@]}"; do
    for k in "${!TEXT_KV[@]}"; do v="${TEXT_KV[$k]}"; replace_text_in_file "$file" "$k" "$v" || true; done
  done
  for file in "${WORK_FILES[@]}"; do
    for k in "${!BLOCK_KV[@]}"; do v="${BLOCK_KV[$k]}"; replace_block_in_file "$file" "$k" "$v" || true; done
  done
  for file in "${WORK_FILES[@]}"; do
    for k in "${!LISTS_KV[@]}"; do replace_list_in_file "$file" "$k" || true; done
  done
  for file in "${WORK_FILES[@]}"; do
    for k in "${!IFCOND_KV[@]}"; do v="${IFCOND_KV[$k]}"; replace_if_in_file "$file" "$k" "$v" || true; done
  done
  for file in "${WORK_FILES[@]}"; do
    for k in "${!HOOKS_KV[@]}"; do v="$(get_hook_body "$k" || true)"; [ -z "$v" ] || replace_hook_in_file "$file" "$k" "$v" || true; done
  done
}
apply_all || true

# 5) 统计 & 产物
for k in "${!BLOCK_KV[@]}";  do
  if ! grep -RIl -E "(<!-- *BLOCK:${k} *-->|// *BLOCK:${k}|/\* *BLOCK:${k} *\*/)" "${APP_DIR}" >/dev/null 2>&1; then
    missing_block=$((missing_block+1))
  fi
done
for k in "${!IFCOND_KV[@]}"; do
  if ! grep -RIl -E "(<!-- *IF:${k} *-->|// *IF:${k}|/\* *IF:${k} *\*/)" "${APP_DIR}" >/dev/null 2>&1; then
    missing_if=$((missing_if+1))
  fi
done

replaced_total=$((replaced_text+replaced_block+replaced_list+replaced_if+hooks_applied))
cat > "${APPLY_JSON}" <<JSON
{
  "replaced_total": ${replaced_total},
  "replaced_text": ${replaced_text},
  "replaced_block": ${replaced_block},
  "replaced_list": ${replaced_list},
  "replaced_if": ${replaced_if},
  "hooks_applied": ${hooks_applied},
  "missing_text": ${missing_text},
  "missing_block": ${missing_block},
  "missing_list": ${missing_list},
  "missing_if": ${missing_if},
  "generated_at": "$(ts_utc)"
}
JSON

{
  echo "branch=${GITHUB_REF_NAME:-$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo main)}"
  echo "variant=unknown"
  echo "apk="
  echo "classes="
  echo "ts=$(ts_utc)"
} > "${SUMMARY_TXT}"

ANCHORS_AFTER="build-logs/anchors.after.txt"
{ grep -R --line-number -E 'NDJC:|BLOCK:|LIST:|HOOK:|IF:' "${APP_DIR}" 2>/dev/null || true; } > "${ANCHORS_AFTER}" || true

MODIFIED_TXT="build-logs/modified-files.txt"
git diff --name-only | sed 's/^/git: /' > "${MODIFIED_TXT}" || true

log "NDJC materialize: total=${replaced_total} text=${replaced_text} block=${replaced_block} list=${replaced_list} if=${replaced_if} hooks=${hooks_applied}"
log "done."
