#!/usr/bin/env bash
# NDJC: materialize anchors from plan → apply into template app
# Usage: bash scripts/ndjc-materialize.sh <APP_DIR> <RUN_ID>
# Inputs:
#   requests/<RUN_ID>/02_plan.json
# Outputs:
#   requests/<RUN_ID>/02_plan.sanitized.json
#   requests/<RUN_ID>/03_apply_result.json
#   build-logs/materialize.log, anchors.txt, modified-files.txt
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

log() { printf '%s %s\n' "[$(date +%H:%M:%S)]" "$*" | tee -a "${LOG_FILE}"; }
ts_utc() { date -u +%Y-%m-%dT%H:%M:%SZ; }
escape_sed() { printf '%s' "$1" | sed -e 's/[\/&]/\\&/g'; }

log "env: PLAN_JSON=${PLAN_JSON}"
log "env: APP_DIR=${APP_DIR}"
log "env: RUN_ID=${RUN_ID}"

# ---------- 0) sanitize plan（调用 TS 单文件） ----------
log "::group::Sanitize plan"
npx -y tsx lib/ndjc/sanitize/index.ts --plan="${PLAN_JSON}"
log "::endgroup::"

if [ ! -f "${PLAN_JSON_SAN}" ]; then
  echo "::error::Sanitized plan not found: ${PLAN_JSON_SAN}"
  exit 1
fi

# ---------- 1) 读取 sanitized plan 到 bash 变量 ----------
read_plan() {
  python - "$PLAN_JSON_SAN" <<'PY'
import json,sys
p = sys.argv[1]
with open(p,'r',encoding='utf-8') as f:
    plan = json.load(f)

meta = plan.get('meta',{})
text = plan.get('text',{}) or {}
block = plan.get('block',{}) or {}
lists = plan.get('lists',{}) or {}
iff   = plan.get('if',{})   or {}
hooks = plan.get('hooks',{}) or {}

def sh_kv_map(name, d):
    out = [f'declare -gA {name}=(']
    for k,v in d.items():
        k = str(k); v = '' if v is None else str(v)
        v = v.replace('\n','\\n')
        out.append(f'["{k}"]="{v}"')
    out.append(')')
    return ' '.join(out)

def to_list_map(name, d):
    # encode list map as declare -A with \x1f join
    out = [f'declare -gA {name}=(']
    for k,v in d.items():
        if isinstance(v,list):
            items = [str(x) for x in v]
        elif v is None:
            items = []
        else:
            items = [str(v)]
        joined = "\x1f".join(items).replace('\n','\\n')
        out.append(f'["{k}"]="{joined}"')
    out.append(')')
    return ' '.join(out)

print(sh_kv_map('TEXT_KV', text))
print(sh_kv_map('BLOCK_KV', block))
print(to_list_map('LISTS_KV', lists))
print(sh_kv_map('IFCOND_KV', iff))
print(to_list_map('HOOKS_KV', hooks))
PY
}

eval "$(read_plan)"

# --------- hardening: ensure assoc arrays ----------
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

# ---------- counters ----------
replaced_text=0
replaced_block=0
replaced_list=0
replaced_if=0
hooks_applied=0
missing_text=0
missing_block=0
missing_list=0
missing_if=0
missing_hook=0

# ---------- file selection ----------
mapfile -t WORK_FILES < <(find "$APP_DIR" -type f \( \
  -name "*.xml" -o -name "*.kt" -o -name "*.kts" -o -name "*.gradle" -o \
  -name "*.pro" -o -name "AndroidManifest.xml" -o -name "*.txt" \
  \) | LC_ALL=C sort)

# ---------- replacers ----------
replace_text_in_file() {
  local file="$1" key="$2" val="$3"
  local before count
  before=$(grep -o -n -F "$key" "$file" | wc -l | tr -d ' ')
  [ "$before" -eq 0 ] && return 1
  local vv; vv="$(escape_sed "$val")"
  sed -i "s/${key}/${vv}/g" "$file"
  local after
  after=$(grep -o -n -F "$key" "$file" | wc -l | tr -d ' ')
  count=$((before-after)); [ "$count" -lt 0 ] && count=0
  if [ "$count" -gt 0 ]; then
    replaced_text=$((replaced_text+count))
  fi
  return 0
}

replace_block_in_file() {
  local file="$1" name="$2" body="$3"
  local cnt=0
  # xml
  if perl -0777 -ne 'exit 1 unless /<!--\s*BLOCK:'"$name"'\s*-->.*?<!--\s*END_BLOCK\s*-->/s' "$file"; then
    perl -0777 -i -pe 's/<!--\s*BLOCK:'"$name"'\s*-->.*?<!--\s*END_BLOCK\s*-->/<!-- BLOCK:'"$name"' -->\n'"$body"'\n<!-- END_BLOCK -->/s' "$file"
    cnt=$((cnt+1))
  fi
  # //
  if perl -0777 -ne 'exit 1 unless /\/\/\s*BLOCK:'"$name"'.*?\/\/\s*END_BLOCK/s' "$file"; then
    perl -0777 -i -pe 's/\/\/\s*BLOCK:'"$name"'.*?\/\/\s*END_BLOCK/\/\/ BLOCK:'"$name"'\n'"$body"'\n\/\/ END_BLOCK/s' "$file"
    cnt=$((cnt+1))
  fi
  # /* */
  if perl -0777 -ne 'exit 1 unless /\/\*\s*BLOCK:'"$name"'\s*\*\/.*?\/\*\s*END_BLOCK\s*\*\//s' "$file"; then
    perl -0777 -i -pe 's/\/\*\s*BLOCK:'"$name"'\s*\*\/.*?\/\*\s*END_BLOCK\s*\*\//\/\* BLOCK:'"$name"' \*\/\n'"$body"'\n\/\* END_BLOCK \*\//s' "$file"
    cnt=$((cnt+1))
  fi
  if [ "$cnt" -gt 0 ]; then
    replaced_block=$((replaced_block+cnt)); return 0
  fi
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
    replaced_if=$((replaced_if+1)); return 0
  fi
  return 1
}

replace_hook_in_file() {
  local file="$1" name="$2" body="$3"
  local cnt=0
  # xml
  if perl -0777 -ne 'exit 1 unless /<!--\s*HOOK:'"$name"'\s*-->.*?<!--\s*END_HOOK\s*-->/s' "$file"; then
    perl -0777 -i -pe 's/<!--\s*HOOK:'"$name"'\s*-->.*?<!--\s*END_HOOK\s*-->/<!-- HOOK:'"$name"' -->\n'"$body"'\n<!-- END_HOOK -->/s' "$file"
    cnt=$((cnt+1))
  fi
  # //
  if perl -0777 -ne 'exit 1 unless /\/\/\s*HOOK:'"$name"'.*?\/\/\s*END_HOOK/s' "$file"; then
    perl -0777 -i -pe 's/\/\/\s*HOOK:'"$name"'.*?\/\/\s*END_HOOK/\/\/ HOOK:'"$name"'\n'"$body"'\n\/\/ END_HOOK/s' "$file"
    cnt=$((cnt+1))
  fi
  # /* */
  if perl -0777 -ne 'exit 1 unless /\/\*\s*HOOK:'"$name"'\s*\*\/.*?\/\*\s*END_HOOK\s*\*\//s' "$file"; then
    perl -0777 -i -pe 's/\/\*\s*HOOK:'"$name"'\s*\*\/.*?\/\*\s*END_HOOK\s*\*\//\/\* HOOK:'"$name"' \*\/\n'"$body"'\n\/\* END_HOOK \*\//s' "$file"
    cnt=$((cnt+1))
  fi
  if [ "$cnt" -gt 0 ]; then
    hooks_applied=$((hooks_applied+cnt)); return 0
  fi
  return 1
}

# ---------- 2) 应用全部 ----------
apply_all() {
  local file k v

  # text
  for file in "${WORK_FILES[@]}"; do
    for k in "${!TEXT_KV[@]}"; do
      v="${TEXT_KV[$k]}"
      replace_text_in_file "$file" "$k" "$v" || true
    done
  done

  # blocks
  for file in "${WORK_FILES[@]}"; do
    for k in "${!BLOCK_KV[@]}"; do
      v="${BLOCK_KV[$k]}"
      replace_block_in_file "$file" "$k" "$v" || true
    done
  done

  # lists
  for file in "${WORK_FILES[@]}"; do
    for k in "${!LISTS_KV[@]}"; do
      replace_list_in_file "$file" "$k" || true
    done
  done

  # if
  for file in "${WORK_FILES[@]}"; do
    for k in "${!IFCOND_KV[@]}"; do
      v="${IFCOND_KV[$k]}"
      replace_if_in_file "$file" "$k" "$v" || true
    done
  done

  # hooks（包括 KOTLIN_IMPORTS / KOTLIN_TOPLEVEL 等）
  for file in "${WORK_FILES[@]}"; do
    for k in "${!HOOKS_KV[@]}"; do
      v="$(get_hook_body "$k" || true)"
      [ -z "$v" ] && continue
      replace_hook_in_file "$file" "$k" "$v" || true
    done
  done
}

apply_all || true

# ---------- 3) 统计与产出 ----------
for k in "${!TEXT_KV[@]}";   do grep -RFl -- "${k}" "${APP_DIR}" >/dev/null 2>&1 || true; done
for k in "${!BLOCK_KV[@]}";  do
  if ! grep -RIl -E "(<!-- *BLOCK:${k} *-->|// *BLOCK:${k}|/\* *BLOCK:${k} *\*/)" "${APP_DIR}" >/dev/null 2>&1; then
    missing_block=$((missing_block+1))
  fi
done
for k in "${!LISTS_KV[@]}";  do grep -RIl "LIST:${k}" "${APP_DIR}" >/dev/null 2>&1 || true; done
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
  echo "missing_text=${missing_text}"
  echo "missing_block=${missing_block}"
  echo "ts=$(ts_utc)"
} > "${SUMMARY_TXT}"

# 诊断产物
ANCHORS_TXT="build-logs/anchors.txt"
{
  grep -R --line-number -E 'NDJC:|BLOCK:|LIST:|HOOK:|IF:' "${APP_DIR}" 2>/dev/null || true
} > "${ANCHORS_TXT}" || true

MODIFIED_TXT="build-logs/modified-files.txt"
git diff --name-only | sed 's/^/git: /' > "${MODIFIED_TXT}" || true

echo "NDJC materialize: total=${replaced_total} text=${replaced_text} block=${replaced_block} list=${replaced_list} if=${replaced_if} hooks=${hooks_applied}" | tee -a "${LOG_FILE}"
echo "done."
