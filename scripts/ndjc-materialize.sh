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
#   build-logs/anchors.after.txt
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

# ⇩ 将 applied/missing 日志输出到与 03_apply_result.json 相同目录
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

# ---------------- 1) read plan (filter “empty-value” anchors) ----------------
read_plan() {
python - "$PLAN_JSON_SAN" <<'PY'
import json, sys
p=sys.argv[1]
plan=json.load(open(p,'r',encoding='utf-8'))

# 兼容字段
text  = plan.get('anchors')   or plan.get('text')        or {}
block = plan.get('blocks')    or plan.get('block')       or {}
lists = plan.get('lists')     or plan.get('list')        or {}
iff   = plan.get('if')        or plan.get('conditions')  or {}
hooks = plan.get('hooks')     or plan.get('hook')        or {}
aliases = plan.get('aliases', {})  # optional

def esc(v:str)->str:
    return v.replace('"','\\"').replace('$','\\$').replace('\n','\\n')

def not_empty(v):
    if v is None: return False
    s = str(v)
    return len(s.strip()) > 0

# 仅保留“有值”的锚点：
# - TEXT/BLOCK/HOOK: 非空字符串
# - LIST: 至少一项，且每项去空后仍有内容
# - IF: 只要键存在就算“有值”（False 也算值，交由物化逻辑处理）
text  = {k: str(v) for k,v in (text  or {}).items() if not_empty(v)}
block = {k: str(v) for k,v in (block or {}).items() if not_empty(v)}
hooks = {k: str(v) for k,v in (hooks or {}).items() if not_empty(v)}

def norm_items(v):
    if v is None: return []
    arr = v if isinstance(v, list) else [v]
    out=[]
    for x in arr:
        s = "" if x is None else str(x)
        if s.strip():
            out.append(s)
    return out

lists = {k: norm_items(v) for k,v in (lists or {}).items()}
lists = {k: v for k,v in lists.items() if len(v) > 0}

iff = {k: v for k,v in (iff or {}).items()}

def sh_kv_map(name,d):
    out=[f'declare -gA {name}=(']
    for k,v in d.items():
        out.append(f'["{esc(str(k))}"]="{esc("" if v is None else str(v))}"')
    out.append(')')
    return ' '.join(out)

def to_list_map(name,d):
    SEP="\x1f"; out=[f'declare -gA {name}=(']
    for k,v in d.items():
        items=[str(x) for x in v]
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

# ensure assoc
ensure_assoc(){ local n="$1"; if ! declare -p "$n" >/dev/null 2>&1 || ! declare -p "$n" 2>/dev/null | grep -q 'declare \-A'; then eval "unset $n; declare -gA $n=()"; fi; }
ensure_assoc TEXT_KV; ensure_assoc BLOCK_KV; ensure_assoc LISTS_KV; ensure_assoc IFCOND_KV; ensure_assoc HOOKS_KV; ensure_assoc ALIASES_KV

# ---------------- Compat helpers ----------------
variants_for() {
  local key="$1"
  local alt1="${key//./_}"
  local alt2="${key//_/.}"
  printf "%s\n%s\n%s\n" "$key" "$alt1" "$alt2" | awk '!x[$0]++'
}

re_end_block='(END_BLOCK|ENDBLOCK)'
re_end_list='(END_LIST|ENDLIST)'
re_end_if='(END_IF|ENDIF)'
re_end_hook='(END_HOOK|ENDHOOK)'

# 纯 Shell 渲染（已移除旧的 heredoc 实现，避免 EOF_TMPL 错误）
render_items() {
  # $1: template, $2: items (LF joined)
  local tmpl="$1" items="$2" out="" token=""
  if   grep -q '\${ITEM}'   <<<"$tmpl"; then token='${ITEM}'
  elif grep -q '\$ITEM'     <<<"$tmpl"; then token='$ITEM'
  elif grep -q '{{ITEM}}'   <<<"$tmpl"; then token='{{ITEM}}'
  fi
  while IFS= read -r it; do
    [ -z "${it}" ] && continue
    if [ -n "$token" ]; then
      local safe_tmpl="${tmpl//"$token"/"$it"}"
      out+="${safe_tmpl}"
    else
      out+="${it}\n"
    fi
    [[ "$out" == *$'\n' ]] || out+=$'\n'
  done <<<"$items"
  printf "%b" "$out"
}

looks_like_json(){ [[ "$1" =~ ^[[:space:]]*[\{\[] ]]; }
looks_like_html(){ [[ "$1" == *"<!--"* ]]; }
is_code_for_kg(){
  [[ "$1" =~ (^|[[:space:]])(fun|val|var|import|class|object|when|if|dependencies|plugins)[[:space:]] ]]
}
comment_for_kg(){ sed 's/^/\/\/ /'; }
prepare_body_for_file(){
  local file="$1"; shift
  local body="$*"
  local ext="${file##*.}"
  if [[ "$ext" == "kt" || "$ext" == "kts" || "$file" == *".gradle" ]]; then
    if looks_like_json "$body" || looks_like_html "$body"; then
      log "compat.used: commented-out non-code block for $file"
      printf '%s\n' "$body" | comment_for_kg; return 0
    fi
    if ! is_code_for_kg "$body"; then
      log "compat.used: body not detected as kotlin/gradle code → commented for $file"
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

# ---------------- anchors snapshot (before) ----------------
ANCHORS_BEFORE="build-logs/anchors.before.txt"
{ grep -R --line-number -E 'NDJC:|BLOCK:|LIST:|HOOK:|IF:' "${APP_DIR}" 2>/dev/null || true; } > "${ANCHORS_BEFORE}" || true
before_total=$(wc -l <"${ANCHORS_BEFORE}" || echo 0)
log "anchors before: ${before_total}"

# ---------------- existence helpers ----------------
exists_text()  { grep -RFl -- "$1" "${APP_DIR}" >/dev/null 2>&1; }

exists_block_any() {
  local name="$1"
  local token="$name"; [[ "$token" == BLOCK:* ]] || token="BLOCK:$token"
  grep -RIl -E "(<!-- *$token *-->.*?<!-- *${re_end_block} *-->|// *$token.*?// *${re_end_block}|/\* *$token *\*/.*?/\\* *${re_end_block} *\*/)" -z "${APP_DIR}" 2>/dev/null | grep -q .
}
exists_list_any() {
  local name="$1"
  local token="$name"; [[ "$token" == LIST:* ]] || token="LIST:$token"
  grep -RIl -E "($token.*?${re_end_list})" -z "${APP_DIR}" 2>/dev/null | grep -q .
}
exists_if_any() {
  local name="$1"
  local token="$name"; [[ "$token" == IF:* ]] || token="IF:$token"
  grep -RIl -E "(<!-- *$token *-->.*?<!-- *${re_end_if} *-->|// *$token.*?// *${re_end_if}|/\* *$token *\*/.*?/\\* *${re_end_if} *\*/)" -z "${APP_DIR}" 2>/dev/null | grep -q .
}
exists_hook_any() {
  local name="$1"
  local token="$name"; [[ "$token" == HOOK:* ]] || token="HOOK:$token"
  grep -RIl -E "(<!-- *$token *-->.*?<!-- *${re_end_hook} *-->|// *$token.*?// *${re_end_hook}|/\* *$token *\*/.*?/\\* *${re_end_hook} *\*/)" -z "${APP_DIR}" 2>/dev/null | grep -q .
}

# ---------------- list & hook helpers ----------------
get_list_items() {
  local raw="${LISTS_KV[$1]-}"; [ -z "${raw}" ] && return 0
  python - "$raw" <<'PY'
import sys; print("\n".join(sys.argv[1].split("\x1f")))
PY
}
get_hook_body() {
  local raw="${HOOKS_KV[$1]-}"; [ -z "${raw}" ] && return 0
  python - "$raw" <<'PY'
import sys; print("\n".join(sys.argv[1].split("\x1f")))
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

# ---------------- 2) apply all ----------------
apply_all() {
  local file k v nm
  # TEXT
  for file in "${WORK_FILES[@]}"; do
    for k in "${!TEXT_KV[@]}"; do
      v="${TEXT_KV[$k]}"
      replace_text_in_file "$file" "$k" "$v" || true
    done
  done
  # BLOCKS
  for file in "${WORK_FILES[@]}"; do
    for k in "${!BLOCK_KV[@]}"; do
      v="${BLOCK_KV[$k]}"
      while IFS= read -r nm; do
        replace_block_in_file_any "$file" "$nm" "$v" && { log "compat.used: BLOCK name '${k}'→'${nm}' on $file"; break; }
      done < <(variants_for "$k")
    done
  done
  # LISTS
  for file in "${WORK_FILES[@]}"; do
    for k in "${!LISTS_KV[@]}"; do
      while IFS= read -r nm; do
        replace_list_in_file_any "$file" "$nm" && { log "compat.used: LIST name '${k}'→'${nm}' on $file"; break; }
      done < <(variants_for "$k")
    done
  done
  # IF
  for file in "${WORK_FILES[@]}"; do
    for k in "${!IFCOND_KV[@]}"; do
      v="${IFCOND_KV[$k]}"
      while IFS= read -r nm; do
        replace_if_in_file_any "$file" "$nm" "$v" && { log "compat.used: IF name '${k}'→'${nm}' on $file"; break; }
      done < <(variants_for "$k")
    done
  done
  # HOOKS
  for file in "${WORK_FILES[@]}"; do
    for k in "${!HOOKS_KV[@]}"; do
      v="$(get_hook_body "$k" || true)"; [ -z "$v" ] && continue
      while IFS= read -r nm; do
        replace_hook_in_file_any "$file" "$nm" "$v" && { log "compat.used: HOOK name '${k}'→'${nm}' on $file"; break; }
      done < <(variants_for "$k")
    done
  done
}
apply_all || true

# ---------------- 3) stats & output ----------------
ANCHORS_AFTER="build-logs/anchors.after.txt"
{ grep -R --line-number -E 'NDJC:|BLOCK:|LIST:|HOOK:|IF:' "${APP_DIR}" 2>/dev/null || true; } > "${ANCHORS_AFTER}" || true

note_missing() { echo -e "$1\t$2\t$3" >> "${MISSING_LIST}"; }

# 只统计“有值”的锚点（已经在 Python 读取时过滤）
for k in "${!TEXT_KV[@]}";   do exists_text "$k"  || { missing_text=$((missing_text+1));  note_missing "TEXT" "$k" "NOT_FOUND"; }; done
for k in "${!BLOCK_KV[@]}";  do found=0; while IFS= read -r nm; do exists_block_any "$nm"  && { found=1; break; }; done < <(variants_for "$k"); [ "$found" -eq 1 ] || { missing_block=$((missing_block+1)); note_missing "BLOCK" "$k" "NOT_FOUND"; }; done
for k in "${!LISTS_KV[@]}";  do found=0; while IFS= read -r nm; do exists_list_any "$nm"   && { found=1; break; }; done < <(variants_for "$k"); [ "$found" -eq 1 ] || { missing_list=$((missing_list+1));  note_missing "LIST" "$k" "NOT_FOUND"; }; done
for k in "${!IFCOND_KV[@]}"; do found=0; while IFS= read -r nm; do exists_if_any "$nm"     && { found=1; break; }; done < <(variants_for "$k"); [ "$found" -eq 1 ] || { missing_if=$((missing_if+1));    note_missing "IF" "$k" "NOT_FOUND"; }; done
missing_hook=0
for k in "${!HOOKS_KV[@]}";  do found=0; while IFS= read -r nm; do exists_hook_any "$nm"   && { found=1; break; }; done < <(variants_for "$k"); [ "$found" -eq 1 ] || { missing_hook=$((missing_hook+1)); note_missing "HOOK" "$k" "NOT_FOUND"; }; done

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
log "applied anchors → ${APPLIED_LIST}"
log "missing anchors → ${MISSING_LIST}"
log "done."
