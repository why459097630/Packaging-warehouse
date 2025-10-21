#!/usr/bin/env bash
# NDJC: materialize anchors from plan → apply into template app
# Usage: bash scripts/ndjc-materialize.sh <APP_DIR> <RUN_ID>
# Inputs:
#   requests/<RUN_ID>/02_plan.json
# Outputs:
#   requests/<RUN_ID>/02_plan.sanitized.json
#   requests/<RUN_ID>/03_apply_result.json
#   requests/<RUN_ID>/applied-anchors.txt
#   requests/<RUN_ID>/missing-anchors.txt
#   build-logs/materialize.log
#   build-logs/anchors.before.txt
#   build-logs/modified-files.txt

set -euo pipefail

APP_DIR="${1:-app}"
RUN_ID="${2:-${RUN_ID:-}}"

if [ -z "${RUN_ID:-}" ]; then
  echo "::error::RUN_ID missing (pass as 2nd arg or set env RUN_ID)"
  exit 1
fi

REQ_DIR="requests/${RUN_ID}"
PLAN_JSON="${PLAN_JSON:-${REQ_DIR}/02_plan.json}"
PLAN_JSON_SAN="${PLAN_JSON/02_plan.json/02_plan.sanitized.json}"
APPLY_JSON="${APPLY_JSON:-${REQ_DIR}/03_apply_result.json}"
SUMMARY_TXT="${REQ_DIR}/actions-summary.txt"

mkdir -p "${REQ_DIR}" build-logs
LOG_FILE="build-logs/materialize.log"; : > "${LOG_FILE}"

# ✳️ 把两份日志放到 03_apply_result.json 同目录
APPLIED_LIST="${REQ_DIR}/applied-anchors.txt"; : > "${APPLIED_LIST}"
MISSING_LIST="${REQ_DIR}/missing-anchors.txt"; : > "${MISSING_LIST}"

log() { printf '%s %s\n' "[$(date +%H:%M:%S)]" "$*" | tee -a "${LOG_FILE}"; }
ts_utc() { date -u +%Y-%m-%dT%H:%M:%SZ; }
escape_sed() { printf '%s' "$1" | sed -e 's/[\/&]/\\&/g'; }

# -------- Resolve registry path (fail-fast if missing) --------
resolve_registry() {
  local cand=""
  if [ -n "${REGISTRY_FILE:-}" ]; then
    cand="$REGISTRY_FILE"
  elif [ -n "${NDJC_REGISTRY_FILE:-}" ]; then
    cand="$NDJC_REGISTRY_FILE"
  fi
  if [ -z "$cand" ] || [ ! -f "$cand" ]; then
    local ws="${GITHUB_WORKSPACE:-$PWD}"
    local gitroot
    gitroot="$(git rev-parse --show-toplevel 2>/dev/null || echo "$PWD")"
    for p in \
      "$ws/niandongjicheng/lib/ndjc/anchors/registry.circle-basic.json" \
      "$gitroot/niandongjicheng/lib/ndjc/anchors/registry.circle-basic.json" \
      "$ws/lib/ndjc/anchors/registry.circle-basic.json" \
      "$gitroot/lib/ndjc/anchors/registry.circle-basic.json"
    do
      if [ -f "$p" ]; then cand="$p"; break; fi
    done
  fi
  if [ -z "$cand" ] || [ ! -f "$cand" ]; then
    echo "::error::Registry file not found. Please set REGISTRY_FILE or NDJC_REGISTRY_FILE to niandongjicheng/lib/ndjc/anchors/registry.circle-basic.json"
    exit 1
  fi
  REGISTRY_FILE="$cand"
  export REGISTRY_FILE
}
resolve_registry

log "env: PLAN_JSON=${PLAN_JSON}"
log "env: PLAN_JSON_SAN=${PLAN_JSON_SAN}"
log "env: APPLY_JSON=${APPLY_JSON}"
log "env: APP_DIR=${APP_DIR}"
log "env: RUN_ID=${RUN_ID}"
log "env: REGISTRY_FILE=${REGISTRY_FILE}"
log "env: NDJC_SANITIZE_FAIL_ON_EMPTY=${NDJC_SANITIZE_FAIL_ON_EMPTY:-0}"

# Basic presence check for plan
if [ ! -f "${PLAN_JSON}" ]; then
  echo "::error::Plan file not found: ${PLAN_JSON}"
  exit 1
fi

# ---------------- 0) sanitize plan ----------------
log "::group::Sanitize plan"
env PLAN_JSON="${PLAN_JSON}" RUN_ID="${RUN_ID}" NDJC_SANITIZE_OVERWRITE=0 \
  REGISTRY_FILE="${REGISTRY_FILE}" \
  NDJC_SANITIZE_FAIL_ON_EMPTY="${NDJC_SANITIZE_FAIL_ON_EMPTY:-0}" \
  npx -y tsx lib/ndjc/sanitize/index.ts
log "::endgroup::"

# Fallback: if derived name not present, try *.sanitized.json next to PLAN_JSON
if [ ! -f "${PLAN_JSON_SAN}" ]; then
  alt="${PLAN_JSON%.json}.sanitized.json"
  if [ -f "${alt}" ]; then
    PLAN_JSON_SAN="${alt}"
    log "compat.used: fallback PLAN_JSON_SAN → ${PLAN_JSON_SAN}"
  fi
fi
if [ ! -f "${PLAN_JSON_SAN}" ]; then
  echo "::error::Sanitized plan not found: ${PLAN_JSON_SAN}"
  exit 1
fi

# ---------------- 1) read plan (with aliases) ----------------
read_plan() {
python - "$PLAN_JSON_SAN" <<'PY'
import json,sys
p=sys.argv[1]
plan=json.load(open(p,'r',encoding='utf-8'))

text  = plan.get('anchors')   or plan.get('text')        or {}
block = plan.get('blocks')    or plan.get('block')       or {}
lists = plan.get('lists')     or plan.get('list')        or {}
iff   = plan.get('if')        or plan.get('conditions')  or {}
hooks = plan.get('hooks')     or plan.get('hook')        or {}
aliases = plan.get('aliases', {})

def esc(v:str)->str:
    return v.replace('"','\\"').replace('$','\\$').replace('\n','\\n')

def sh_kv_map(name,d):
    out=[f'declare -gA {name}=(']
    for k,v in d.items():
        out.append(f'["{esc(str(k))}"]="{esc("" if v is None else str(v))}"')
    out.append(')')
    return ' '.join(out)

def to_list_map(name,d):
    SEP="\x1f"; out=[f'declare -gA {name}=(']
    for k,v in d.items():
        if isinstance(v,list): items=[str(x) for x in v]
        elif v is None: items=[]
        else: items=[str(v)]
        out.append(f'["{esc(str(k))}"]="{esc(SEP.join(items))}"')
    out.append(')')
    return ' '.join(out)

print(sh_kv_map('TEXT_KV', text))
print(sh_kv_map('BLOCK_KV', block))
print(to_list_map('LISTS_KV', lists))
print(sh_kv_map('IFCOND_KV', iff))
print(to_list_map('HOOKS_KV', hooks))
print(sh_kv_map('ALIASES_KV', aliases))
PY
}
eval "$(read_plan)"

# ensure assoc maps
ensure_assoc(){ local n="$1"; if ! declare -p "$n" >/dev/null 2>&1 || ! declare -p "$n" 2>/dev/null | grep -q 'declare \-A'; then eval "unset $n; declare -gA $n=()"; fi; }
ensure_assoc TEXT_KV; ensure_assoc BLOCK_KV; ensure_assoc LISTS_KV; ensure_assoc IFCOND_KV; ensure_assoc HOOKS_KV; ensure_assoc ALIASES_KV

# ---------------- helpers ----------------
trim(){ awk '{$1=$1;print}' <<<"${1:-}"; }

re_end_block='(END_BLOCK|ENDBLOCK)'
re_end_list='(END_LIST|ENDLIST)'
re_end_if='(END_IF|ENDIF)'
re_end_hook='(END_HOOK|ENDHOOK)'

render_items() {
  local tmpl="$1"; local items="$2"; local rendered=""
  local token=""
  if grep -q '\${ITEM}' <<<"$tmpl"; then token='\${ITEM}'
  elif grep -q '\$ITEM' <<<"$tmpl"; then token='\$ITEM'
  elif grep -q '{{ITEM}}' <<<"$tmpl"; then token='{{ITEM}}'
  else
    token='\${ITEM}'
  fi
  while IFS= read -r it; do
    rendered+=$(python - <<PY
import sys
tmpl = """$tmpl"""
tok  = """$token"""
it   = """$it"""
print(tmpl.replace(tok, it))
PY
)
    rendered+=$'\n'
  done <<<"$items"
  printf "%s" "$rendered"
}

looks_like_json(){ [[ "$1" =~ ^[[:space:]]*[\{\[] ]]; }
looks_like_html(){ [[ "$1" == *"<!--"* ]]; }
is_code_for_kg(){ [[ "$1" =~ (^|[[:space:]])(fun|val|var|import|class|object|when|if|dependencies|plugins)[[:space:]] ]]; }
comment_for_kg(){ sed 's/^/\/\/ /'; }
prepare_body_for_file(){
  local file="$1"; shift
  local body="$*"
  local ext="${file##*.}"
  if [[ "$ext" == "kt" || "$ext" == "kts" || "$file" == *".gradle" ]]; then
    if looks_like_json "$body" || looks_like_html "$body" || ! is_code_for_kg "$body"; then
      printf '%s\n' "$body" | comment_for_kg; return 0
    fi
  fi
  printf '%s' "$body"
}

# ---------------- counters ----------------
replaced_text=0 replaced_block=0 replaced_list=0 replaced_if=0 hooks_applied=0
missing_text=0 missing_block=0 missing_list=0 missing_if=0 missing_hook=0

# ---------------- file selection ----------------
mapfile -t WORK_FILES < <(find "$APP_DIR" -type f \( -name "*.xml" -o -name "*.kt" -o -name "*.kts" -o -name "*.gradle" -o -name "*.pro" -o -name "AndroidManifest.xml" -o -name "*.txt" \) | LC_ALL=C sort)

# ---------------- existence helpers ----------------
exists_text()  { grep -RFl -- "$1" "${APP_DIR}" >/dev/null 2>&1; }

exists_block_any() {
  local name="$1"; local token="$name"; [[ "$token" == BLOCK:* ]] || token="BLOCK:$token"
  grep -RIl -E "(<!-- *$token *-->.*?<!-- *${re_end_block} *-->|// *$token.*?// *${re_end_block}|/\* *$token *\*/.*?/\\* *${re_end_block} *\*/)" -z "${APP_DIR}" 2>/dev/null | grep -q .
}
exists_list_any() {
  local name="$1"; local token="$name"; [[ "$token" == LIST:* ]] || token="LIST:$token"
  grep -RIl -E "($token.*?${re_end_list})" -z "${APP_DIR}" 2>/dev/null | grep -q .
}
exists_if_any() {
  local name="$1"; local token="$name"; [[ "$token" == IF:* ]] || token="IF:$token"
  grep -RIl -E "(<!-- *$token *-->.*?<!-- *${re_end_if} *-->|// *$token.*?// *${re_end_if}|/\* *$token *\*/.*?/\\* *${re_end_if} *\*/)" -z "${APP_DIR}" 2>/dev/null | grep -q .
}
exists_hook_any() {
  local name="$1"; local token="$name"; [[ "$token" == HOOK:* ]] || token="HOOK:$token"
  grep -RIl -E "(<!-- *$token *-->.*?<!-- *${re_end_hook} *-->|// *$token.*?// *${re_end_hook}|/\* *$token *\*/.*?/\\* *${re_end_hook} *\*/)" -z "${APP_DIR}" 2>/dev/null | grep -q .
}

# ---------------- list & hook helpers ----------------
get_list_items() {
  local raw="${LISTS_KV[$1]-}"; [ -z "${raw}" ] && return 0
  python - "$raw" <<'PY'
import sys; print("\n".join([x for x in sys.argv[1].split("\x1f") if len(x)>0]))
PY
}
get_hook_body() {
  local raw="${HOOKS_KV[$1]-}"; [ -z "${raw}" ] && return 0
  python - "$raw" <<'PY'
import sys; print("\n".join([x for x in sys.argv[1].split("\x1f") if len(x)>0]))
PY
}

# ---------------- replacers ----------------
replace_text_in_file() {
  local file="$1" key="$2" val="$3"
  local hits; hits="$(grep -n -F "$key" "$file" || true)"
  [ -z "$hits" ] && return 1
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    echo -e "TEXT\t${key}\t${file}:${line%%:*}" >> "${APPLIED_LIST}"
  done <<< "${hits}"
  local vv; vv="$(escape_sed "$val")"
  sed -i "s/${key}/${vv}/g" "$file"
  local before after count
  before=$(printf '%s\n' "${hits}" | wc -l | tr -d ' ')
  after=$(grep -o -n -F "$key" "$file" | wc -l | tr -d ' ' || true)
  count=$((before-after)); [ "$count" -lt 0 ] && count=0
  if [ "$count" -gt 0 ]; then replaced_text=$((replaced_text+count)); return 0; fi
  return 1
}

replace_block_in_file_any() {
  local file="$1" name="$2" body="$3" cnt=0
  local token="$name"; [[ "$token" == BLOCK:* ]] || token="BLOCK:$token"
  body="$(prepare_body_for_file "$file" "$body")"
  if perl -0777 -ne 'exit 1 unless /<!--\s*'"$token"'\s*-->.*?<!--\s*'"$re_end_block"'\s*-->/s' "$file"; then
    perl -0777 -i -pe 's/<!--\s*'"$token"'\s*-->.*?<!--\s*'"$re_end_block"'\s*-->/<!-- '"$token"' -->\n'"$body"'\n<!-- END_BLOCK -->/s' "$file"; cnt=$((cnt+1))
  fi
  if perl -0777 -ne 'exit 1 unless /\/\/\s*'"$token"'.*?\/\/\s*'"$re_end_block"'/s' "$file"; then
    perl -0777 -i -pe 's/\/\/\s*'"$token"'.*?\/\/\s*'"$re_end_block"'/\/\/ '"$token"'\n'"$body"'\n\/\/ END_BLOCK/s' "$file"; cnt=$((cnt+1))
  fi
  if perl -0777 -ne 'exit 1 unless /\/\*\s*'"$token"'\s*\*\/.*?\/\*\s*'"$re_end_block"'\s*\*\//s' "$file"; then
    perl -0777 -i -pe 's/\/\*\s*'"$token"'\s*\*\/.*?\/\*\s*'"$re_end_block"'\s*\*\//\/\* '"$token"' \*\/\n'"$body"'\n\/\* END_BLOCK \*\//s' "$file"; cnt=$((cnt+1))
  fi
  if [ "$cnt" -gt 0 ]; then echo -e "BLOCK\t${name}\t${file}" >> "${APPLIED_LIST}"; replaced_block=$((replaced_block+cnt)); return 0; fi
  return 1
}

replace_list_in_file_any() {
  local file="$1" name="$2"
  local token="$name"; [[ "$token" == LIST:* ]] || token="LIST:$token"
  local items; items="$(get_list_items "$name" || true)"; [ -z "$items" ] && return 1
  if ! perl -0777 -ne 'exit 1 unless /'"$token"'.*?'"$re_end_list"'/s' "$file"; then return 1; fi
  local tmpl; tmpl="$(perl -0777 -ne 'if (/'"$token"'\s*(.*)\s*'"$re_end_list"'/s) { print($1) }' "$file")"
  [ -z "$tmpl" ] && tmpl=$'\n'
  local rendered; rendered="$(render_items "$tmpl" "$items")"
  perl -0777 -i -pe 's/'"$token"'.*?'"$re_end_list"'/'"$token"'\n'"$rendered"'END_LIST/s' "$file"
  echo -e "LIST\t${name}\t${file}" >> "${APPLIED_LIST}"
  replaced_list=$((replaced_list+1)); return 0
}

replace_if_in_file_any() {
  local file="$1" name="$2" cond="$3"
  local token="$name"; [[ "$token" == IF:* ]] || token="IF:$token"
  local truthy=0; case "${cond,,}" in ""|"false"|"0"|"no"|"off") truthy=0 ;; *) truthy=1 ;; esac
  local hit=0
  if perl -0777 -ne 'exit 1 unless /<!--\s*'"$token"'\s*-->.*?<!--\s*'"$re_end_if"'\s*-->/s' "$file"; then
    hit=1
    if [ "$truthy" -eq 1 ]; then
      perl -0777 -i -pe 's/<!--\s*'"$token"'\s*-->(.*?)<!--\s*'"$re_end_if"'\s*-->/\1/s' "$file"
    else
      perl -0777 -i -pe 's/<!--\s*'"$token"'\s*-->.*?<!--\s*'"$re_end_if"'\s*-->/<!-- '"$token"' -->\n<!-- END_IF -->/s' "$file"
    fi
  fi
  if perl -0777 -ne 'exit 1 unless /\/\/\s*'"$token"'.*?\/\/\s*'"$re_end_if"'/s' "$file"; then
    hit=1
    if [ "$truthy" -eq 1 ]; then
      perl -0777 -i -pe 's/\/\/\s*'"$token"'(.*?)\/\/\s*'"$re_end_if"'/\1/s' "$file"
    else
      perl -0777 -i -pe 's/\/\/\s*'"$token"'.*?\/\/\s*'"$re_end_if"'/\/\/ '"$token"'\n\/\/ END_IF/s' "$file"
    fi
  fi
  if perl -0777 -ne 'exit 1 unless /\/\*\s*'"$token"'\s*\*\/.*?\/\*\s*'"$re_end_if"'\s*\*\//s' "$file"; then
    hit=1
    if [ "$truthy" -eq 1 ]; then
      perl -0777 -i -pe 's/\/\*\s*'"$token"'\s*\*\/(.*?)\/\*\s*'"$re_end_if"'\s*\*\//\1/s' "$file"
    else
      perl -0777 -i -pe 's/\/\*\s*'"$token"'\s*\*\/.*?\/\*\s*'"$re_end_if"'\s*\*\//\/\* '"$token"' \*\/\n\/\* END_IF \*\//s' "$file"
    fi
  fi
  if [ "$hit" -eq 1 ]; then echo -e "IF\t${name}\t${file}" >> "${APPLIED_LIST}"; replaced_if=$((replaced_if+1)); return 0; fi
  return 1
}

replace_hook_in_file_any() {
  local file="$1" name="$2" body="$3" cnt=0
  local token="$name"; [[ "$token" == HOOK:* ]] || token="HOOK:$token"
  body="$(prepare_body_for_file "$file" "$body")"
  if perl -0777 -ne 'exit 1 unless /<!--\s*'"$token"'\s*-->.*?<!--\s*'"$re_end_hook"'\s*-->/s' "$file"; then
    perl -0777 -i -pe 's/<!--\s*'"$token"'\s*-->.*?<!--\s*'"$re_end_hook"'\s*-->/<!-- '"$token"' -->\n'"$body"'\n<!-- END_HOOK -->/s' "$file"; cnt=$((cnt+1))
  fi
  if perl -0777 -ne 'exit 1 unless /\/\/\s*'"$token"'.*?\/\/\s*'"$re_end_hook"'/s' "$file"; then
    perl -0777 -i -pe 's/\/\/\s*'"$token"'.*?\/\/\s*'"$re_end_hook"'/\/\/ '"$token"'\n'"$body"'\n\/\/ END_HOOK/s' "$file"; cnt=$((cnt+1))
  fi
  if perl -0777 -ne 'exit 1 unless /\/\*\s*'"$token"'\s*\*\/.*?\/\*\s*'"$re_end_hook"'\s*\*\//s' "$file"; then
    perl -0777 -i -pe 's/\/\*\s*'"$token"'\s*\*\/.*?\/\*\s*'"$re_end_hook"'\s*\*\//\/\* '"$token"' \*\/\n'"$body"'\n\/\* END_HOOK \*\//s' "$file"; cnt=$((cnt+1))
  fi
  if [ "$cnt" -gt 0 ]; then echo -e "HOOK\t${name}\t${file}" >> "${APPLIED_LIST}"; hooks_applied=$((hooks_applied+cnt)); return 0; fi
  return 1
}

# ---------------- 2) apply all（按“有值才统计”，先判是否有落位点） ----------------
apply_all() {
  local file k v

  # TEXT
  for k in "${!TEXT_KV[@]}"; do
    v="$(trim "${TEXT_KV[$k]}")"
    [ -z "$v" ] && continue            # 无值不统计
    if ! exists_text "$k"; then        # 模板里根本没有该占位符
      echo -e "TEXT\t${k}\tNOT_FOUND" >> "${MISSING_LIST}"
      missing_text=$((missing_text+1))
      continue
    fi
    for file in "${WORK_FILES[@]}"; do
      replace_text_in_file "$file" "$k" "$v" || true
    done
  done

  # BLOCKS
  for k in "${!BLOCK_KV[@]}"; do
    v="$(trim "${BLOCK_KV[$k]}")"
    [ -z "$v" ] && continue
    if ! exists_block_any "$k"; then
      echo -e "BLOCK\t${k}\tNOT_FOUND" >> "${MISSING_LIST}"
      missing_block=$((missing_block+1))
      continue
    fi
    for file in "${WORK_FILES[@]}"; do
      replace_block_in_file_any "$file" "$k" "$v" || true
    done
  done

  # LISTS
  for k in "${!LISTS_KV[@]}"; do
    local items
    items="$(get_list_items "$k" || true)"
    [ -z "$items" ] && continue
    if ! exists_list_any "$k"; then
      echo -e "LIST\t${k}\tNOT_FOUND" >> "${MISSING_LIST}"
      missing_list=$((missing_list+1))
      continue
    fi
    for file in "${WORK_FILES[@]}"; do
      replace_list_in_file_any "$file" "$k" || true
    done
  done

  # IF
  for k in "${!IFCOND_KV[@]}"; do
    v="${IFCOND_KV[$k]}"               # 只要键存在就算“有值”
    if ! exists_if_any "$k"; then
      echo -e "IF\t${k}\tNOT_FOUND" >> "${MISSING_LIST}"
      missing_if=$((missing_if+1))
      continue
    fi
    for file in "${WORK_FILES[@]}"; do
      replace_if_in_file_any "$file" "$k" "$v" || true
    done
  done

  # HOOKS
  for k in "${!HOOKS_KV[@]}"; do
    local body
    body="$(get_hook_body "$k" || true)"
    [ -z "$body" ] && continue
    if ! exists_hook_any "$k"; then
      echo -e "HOOK\t${k}\tNOT_FOUND" >> "${MISSING_LIST}"
      missing_hook=$((missing_hook+1))
      continue
    fi
    for file in "${WORK_FILES[@]}"; do
      replace_hook_in_file_any "$file" "$k" "$body" || true
    done
  done
}
apply_all || true

# ---------------- 3) stats & output ----------------

# （保留一下 anchors.before 快照，方便排查）
ANCHORS_BEFORE="build-logs/anchors.before.txt"
{ grep -R --line-number -E 'NDJC:|BLOCK:|LIST:|HOOK:|IF:' "${APP_DIR}" 2>/dev/null || true; } > "${ANCHORS_BEFORE}" || true

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

MODIFIED_TXT="build-logs/modified-files.txt"
git diff --name-only | sed 's/^/git: /' > "${MODIFIED_TXT}" || true

log "NDJC materialize: total=${replaced_total} text=${replaced_text} block=${replaced_block} list=${replaced_list} if=${replaced_if} hooks=${hooks_applied}"
log "applied anchors  → ${APPLIED_LIST}"
log "missing anchors  → ${MISSING_LIST}"
log "done."
