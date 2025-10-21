#!/usr/bin/env bash
# NDJC: materialize anchors from plan → apply into template app

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

# 将 applied/missing 日志写到 03 同目录
APPLIED_LIST="${APPLY_JSON%/*}/applied-anchors.txt"; : > "${APPLIED_LIST}"
MISSING_LIST="${APPLY_JSON%/*}/missing-anchors.txt"; : > "${MISSING_LIST}"

log() { printf '%s %s\n' "[$(date +%H:%M:%S)]" "$*" | tee -a "${LOG_FILE}"; }
ts_utc() { date -u +%Y-%m-%dT%H:%M:%SZ; }

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

# ---------------- helpers ----------------
perl_escape_repl() {
  # 供 perl s/// 的替换部分使用：转义 / \ $ &
  # shellcheck disable=SC2001
  printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/\//\\\//g' -e 's/\$/\\\$/g' -e 's/&/\\\&/g'
}

# 读取 plan（保持你原有结构）
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
aliases = plan.get('aliases', {})  # 兼容字段，未使用重命名

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

ensure_assoc(){ local n="$1"; if ! declare -p "$n" >/dev/null 2>&1 || ! declare -p "$n" 2>/dev/null | grep -q 'declare \-A'; then eval "unset $n; declare -gA $n=()"; fi; }
ensure_assoc TEXT_KV; ensure_assoc BLOCK_KV; ensure_assoc LISTS_KV; ensure_assoc IFCOND_KV; ensure_assoc HOOKS_KV; ensure_assoc ALIASES_KV

# 只有“有值”的锚点才参与
has_non_empty() { [ -n "${1//[$'\t\r\n ']/}" ]; }

get_list_items() {
  local raw="${LISTS_KV[$1]-}"; [ -z "${raw}" ] && return 0
  python - "$raw" <<'PY'
import sys
print("\n".join([x for x in sys.argv[1].split("\x1f") if x!=""]))
PY
}

get_hook_body() {
  local raw="${HOOKS_KV[$1]-}"; [ -z "${raw}" ] && return 0
  python - "$raw" <<'PY'
import sys
print("\n".join(sys.argv[1].split("\x1f")))
PY
}

# 结束标记
re_end_block='(END_BLOCK|ENDBLOCK)'
re_end_list='(END_LIST|ENDLIST)'
re_end_if='(END_IF|ENDIF)'
re_end_hook='(END_HOOK|ENDHOOK)'

# 文件集合
mapfile -t WORK_FILES < <(find "$APP_DIR" -type f \( -name "*.xml" -o -name "*.kt" -o -name "*.kts" -o -name "*.gradle" -o -name "*.pro" -o -name "AndroidManifest.xml" -o -name "*.txt" \) | LC_ALL=C sort)

# 匹配是否存在
exists_text()  { grep -RFl -- "$1" "${APP_DIR}" >/dev/null 2>&1; }
exists_block_any() { grep -RIl -E "(<!-- *BLOCK:$1 *-->.*?<!-- *${re_end_block} *-->|// *BLOCK:$1.*?// *${re_end_block}|/\* *BLOCK:$1 *\*/.*?/\\* *${re_end_block} *\*/)" -z "${APP_DIR}" >/dev/null 2>&1; }
exists_list_any()  { grep -RIl -E "(LIST:$1.*?${re_end_list})" -z "${APP_DIR}" >/dev/null 2>&1; }
exists_if_any()    { grep -RIl -E "(<!-- *IF:$1 *-->.*?<!-- *${re_end_if} *-->|// *IF:$1.*?// *${re_end_if}|/\* *IF:$1 *\*/.*?/\\* *${re_end_if} *\*/)" -z "${APP_DIR}" >/dev/null 2>&1; }
exists_hook_any()  { grep -RIl -E "(<!-- *HOOK:$1 *-->.*?<!-- *${re_end_hook} *-->|// *HOOK:$1.*?// *${re_end_hook}|/\* *HOOK:$1 *\*/.*?/\\* *${re_end_hook} *\*/)" -z "${APP_DIR}" >/dev/null 2>&1; }

# 计数器
replaced_text=0 replaced_block=0 replaced_list=0 replaced_if=0 hooks_applied=0
missing_text=0 missing_block=0 missing_list=0 missing_if=0 missing_hook=0

# TEXT 替换
replace_text_in_file() {
  local file="$1" key="$2" val="$3"
  local hits; hits="$(grep -n -F "$key" "$file" || true)"
  [ -z "$hits" ] && return 1
  # 记应用位置
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    echo -e "TEXT\t${key}\t${file}:${line%%:*}" >> "${APPLIED_LIST}"
  done <<< "${hits}"
  # 直接按字面替换
  local vv; vv="$(perl_escape_repl "$val")"
  perl -0777 -i -pe "s/\Q${key}\E/${vv}/g" "$file"
  # 估算替换次数
  local before after count
  before=$(printf '%s\n' "${hits}" | wc -l | tr -d ' ')
  after=$(grep -o -n -F "$key" "$file" | wc -l | tr -d ' ' || true)
  count=$((before-after)); [ "$count" -lt 0 ] && count=0
  if [ "$count" -gt 0 ]; then replaced_text=$((replaced_text+count)); return 0; fi
  return 1
}

# BLOCK/IF/HOOK/LIST 替换
replace_block_in_file_any() {
  local file="$1" name="$2" body="$3" cnt=0
  local repl; repl="$(perl_escape_repl "$body")"
  # XML
  if perl -0777 -ne 'exit 1 unless /<!--\s*BLOCK:'"$name"'\s*-->.*?<!--\s*'"$re_end_block"'\s*-->/s' "$file"; then
    perl -0777 -i -pe 's/<!--\s*BLOCK:'"$name"'\s*-->.*?<!--\s*'"$re_end_block"'\s*-->/<!-- BLOCK:'"$name"' -->\n'"$repl"'\n<!-- END_BLOCK -->/s' "$file"; cnt=$((cnt+1))
  fi
  # // 注释
  if perl -0777 -ne 'exit 1 unless /\/\/\s*BLOCK:'"$name"'.*?\/\/\s*'"$re_end_block"'/s' "$file"; then
    perl -0777 -i -pe 's/\/\/\s*BLOCK:'"$name"'.*?\/\/\s*'"$re_end_block"'/\/\/ BLOCK:'"$name"'\n'"$repl"'\n\/\/ END_BLOCK/s' "$file"; cnt=$((cnt+1))
  fi
  # /* */ 注释
  if perl -0777 -ne 'exit 1 unless /\/\*\s*BLOCK:'"$name"'\s*\*\/.*?\/\*\s*'"$re_end_block"'\s*\*\//s' "$file"; then
    perl -0777 -i -pe 's/\/\*\s*BLOCK:'"$name"'\s*\*\/.*?\/\*\s*'"$re_end_block"'\s*\*\//\/\* BLOCK:'"$name"' \*\/\n'"$repl"'\n\/\* END_BLOCK \*\//s' "$file"; cnt=$((cnt+1))
  fi
  if [ "$cnt" -gt 0 ]; then echo -e "BLOCK\t${name}\t${file}" >> "${APPLIED_LIST}"; replaced_block=$((replaced_block+cnt)); return 0; fi
  return 1
}

render_items() {
  # $1: 模板（已提取），$2: items（多行）
  python - "$1" <<'PY'
import sys
tmpl = sys.argv[1]
items = sys.stdin.read().splitlines()
for it in items:
  print(tmpl.replace("${ITEM}", it).replace("$ITEM", it).replace("{{ITEM}}", it))
PY
}

replace_list_in_file_any() {
  local file="$1" name="$2"
  local items; items="$(get_list_items "$name" || true)"
  [ -z "$items" ] && return 1
  if ! perl -0777 -ne 'exit 1 unless /LIST:'"$name"'.*?'"$re_end_list"'/s' "$file"; then return 1; fi
  local tmpl; tmpl="$(perl -0777 -ne 'if (/LIST:'"$name"'\s*(.*)\s*'"$re_end_list"'/s) { print($1) }' "$file")"
  [ -z "$tmpl" ] && tmpl=$'\n'
  local rendered; rendered="$(printf '%s\n' "$items" | render_items "$tmpl")"
  local repl; repl="$(perl_escape_repl "$rendered")"
  perl -0777 -i -pe 's/LIST:'"$name"'.*?'"$re_end_list"'/LIST:'"$name"'\n'"$repl"'END_LIST/s' "$file"
  echo -e "LIST\t${name}\t${file}" >> "${APPLIED_LIST}"
  replaced_list=$((replaced_list+1)); return 0
}

replace_if_in_file_any() {
  local file="$1" name="$2" cond="$3"
  local truthy=0; case "${cond,,}" in ""|"false"|"0"|"no"|"off") truthy=0 ;; *) truthy=1 ;; esac
  local hit=0
  if perl -0777 -ne 'exit 1 unless /<!--\s*IF:'"$name"'\s*-->.*?<!--\s*'"$re_end_if"'\s*-->/s' "$file"; then
    hit=1
    if [ "$truthy" -eq 1 ]; then
      perl -0777 -i -pe 's/<!--\s*IF:'"$name"'\s*-->(.*?)<!--\s*'"$re_end_if"'\s*-->/\1/s' "$file"
    else
      perl -0777 -i -pe 's/<!--\s*IF:'"$name"'\s*-->.*?<!--\s*'"$re_end_if"'\s*-->/<!-- IF:'"$name"' -->\n<!-- END_IF -->/s' "$file"
    fi
  fi
  if perl -0777 -ne 'exit 1 unless /\/\/\s*IF:'"$name"'.*?\/\/\s*'"$re_end_if"'/s' "$file"; then
    hit=1
    if [ "$truthy" -eq 1 ]; then
      perl -0777 -i -pe 's/\/\/\s*IF:'"$name"'(.*?)\/\/\s*'"$re_end_if"'/\1/s' "$file"
    else
      perl -0777 -i -pe 's/\/\/\s*IF:'"$name"'.*?\/\/\s*'"$re_end_if"'/\/\/ IF:'"$name"'\n\/\/ END_IF/s' "$file"
    fi
  fi
  if perl -0777 -ne 'exit 1 unless /\/\*\s*IF:'"$name"'\s*\*\/.*?\/\*\s*'"$re_end_if"'\s*\*\//s' "$file"; then
    hit=1
    if [ "$truthy" -eq 1 ]; then
      perl -0777 -i -pe 's/\/\*\s*IF:'"$name"'\s*\*\/(.*?)\/\*\s*'"$re_end_if"'\s*\*\//\1/s' "$file"
    else
      perl -0777 -i -pe 's/\/\*\s*IF:'"$name"'\s*\*\/.*?\/\*\s*'"$re_end_if"'\s*\*\//\/\* IF:'"$name"' \*\/\n\/\* END_IF \*\//s' "$file"
    fi
  fi
  if [ "$hit" -eq 1 ]; then echo -e "IF\t${name}\t${file}" >> "${APPLIED_LIST}"; replaced_if=$((replaced_if+1)); return 0; fi
  return 1
}

replace_hook_in_file_any() {
  local file="$1" name="$2" body="$3" cnt=0
  local repl; repl="$(perl_escape_repl "$body")"
  if perl -0777 -ne 'exit 1 unless /<!--\s*HOOK:'"$name"'\s*-->.*?<!--\s*'"$re_end_hook"'\s*-->/s' "$file"; then
    perl -0777 -i -pe 's/<!--\s*HOOK:'"$name"'\s*-->.*?<!--\s*'"$re_end_hook"'\s*-->/<!-- HOOK:'"$name"' -->\n'"$repl"'\n<!-- END_HOOK -->/s' "$file"; cnt=$((cnt+1))
  fi
  if perl -0777 -ne 'exit 1 unless /\/\/\s*HOOK:'"$name"'.*?\/\/\s*'"$re_end_hook"'/s' "$file"; then
    perl -0777 -i -pe 's/\/\/\s*HOOK:'"$name"'.*?\/\/\s*'"$re_end_hook"'/\/\/ HOOK:'"$name"'\n'"$repl"'\n\/\/ END_HOOK/s' "$file"; cnt=$((cnt+1))
  fi
  if perl -0777 -ne 'exit 1 unless /\/\*\s*HOOK:'"$name"'\s*\*\/.*?\/\*\s*'"$re_end_hook"'\s*\*\//s' "$file"; then
    perl -0777 -i -pe 's/\/\*\s*HOOK:'"$name"'\s*\*\/.*?\/\*\s*'"$re_end_hook"'\s*\*\//\/\* HOOK:'"$name"' \*\/\n'"$repl"'\n\/\* END_HOOK \*\//s' "$file"; cnt=$((cnt+1))
  fi
  if [ "$cnt" -gt 0 ]; then echo -e "HOOK\t${name}\t${file}" >> "${APPLIED_LIST}"; hooks_applied=$((hooks_applied+cnt)); return 0; fi
  return 1
}

# ---------------- 2) apply all ----------------
apply_all() {
  local file k v

  # TEXT（只处理有值）
  for file in "${WORK_FILES[@]}"; do
    for k in "${!TEXT_KV[@]}"; do
      v="${TEXT_KV[$k]}"; has_non_empty "$v" || continue
      replace_text_in_file "$file" "$k" "$v" || true
    done
  done

  # BLOCK（只处理有值）
  for file in "${WORK_FILES[@]}"; do
    for k in "${!BLOCK_KV[@]}"; do
      v="${BLOCK_KV[$k]}"; has_non_empty "$v" || continue
      replace_block_in_file_any "$file" "$k" "$v" || true
    done
  done

  # LIST（items>0 才处理）
  for file in "${WORK_FILES[@]}"; do
    for k in "${!LISTS_KV[@]}"; do
      local items_cnt
      items_cnt="$(get_list_items "$k" | wc -l | tr -d ' ' || true)"
      [ "${items_cnt:-0}" -gt 0 ] || continue
      replace_list_in_file_any "$file" "$k" || true
    done
  done

  # IF（只要出现在 plan 就参与）
  for file in "${WORK_FILES[@]}"; do
    for k in "${!IFCOND_KV[@]}"; do
      v="${IFCOND_KV[$k]}"
      replace_if_in_file_any "$file" "$k" "$v" || true
    done
  done

  # HOOK（只处理有值）
  for file in "${WORK_FILES[@]}"; do
    for k in "${!HOOKS_KV[@]}"; do
      v="$(get_hook_body "$k" || true)"; has_non_empty "$v" || continue
      replace_hook_in_file_any "$file" "$k" "$v" || true
    done
  done
}
apply_all || true

# ---------------- 3) stats & output ----------------
note_missing() { echo -e "$1\t$2\t$3" >> "${MISSING_LIST}"; }

# 仅对“有值”的锚点做 missing 统计
for k in "${!TEXT_KV[@]}"; do
  v="${TEXT_KV[$k]}"; has_non_empty "$v" || continue
  exists_text "$k" || { missing_text=$((missing_text+1)); note_missing "TEXT" "$k" "NOT_FOUND"; }
done
for k in "${!BLOCK_KV[@]}"; do
  v="${BLOCK_KV[$k]}"; has_non_empty "$v" || continue
  exists_block_any "$k" || { missing_block=$((missing_block+1)); note_missing "BLOCK" "$k" "NOT_FOUND"; }
done
for k in "${!LISTS_KV[@]}"; do
  cnt="$(get_list_items "$k" | wc -l | tr -d ' ' || true)"
  [ "${cnt:-0}" -gt 0 ] || continue
  exists_list_any "$k" || { missing_list=$((missing_list+1)); note_missing "LIST" "$k" "NOT_FOUND"; }
done
for k in "${!IFCOND_KV[@]}"; do
  exists_if_any "$k" || { missing_if=$((missing_if+1)); note_missing "IF" "$k" "NOT_FOUND"; }
done
# HOOK（有值才统计）
for k in "${!HOOKS_KV[@]}"; do
  v="$(get_hook_body "$k" || true)"; has_non_empty "$v" || continue
  exists_hook_any "$k" || { missing_hook=$((missing_hook+1)); note_missing "HOOK" "$k" "NOT_FOUND"; }
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

MODIFIED_TXT="build-logs/modified-files.txt"
git diff --name-only | sed 's/^/git: /' > "${MODIFIED_TXT}" || true

log "NDJC materialize: total=${replaced_total} text=${replaced_text} block=${replaced_block} list=${replaced_list} if=${replaced_if} hooks=${hooks_applied}"
log "applied anchors → ${APPLIED_LIST}"
log "missing anchors → ${MISSING_LIST}"
log "done."
