#!/usr/bin/env bash
# NDJC: materialize anchors from plan → apply into template app
# Inputs (env or argv):
#   APP_DIR (default app) / RUN_ID / PLAN_JSON / APPLY_JSON
# Outputs:
#   03_apply_result.json, actions-summary.txt
set -euo pipefail

APP_DIR="${1:-${APP_DIR:-app}}"
RUN_ID="${2:-${RUN_ID:-}}"

if [ -z "${RUN_ID:-}" ]; then
  echo "::error::RUN_ID missing (pass as 2nd arg or set env RUN_ID)"
  exit 1
fi

REQ_DIR="requests/${RUN_ID}"
PLAN_JSON="${PLAN_JSON:-${REQ_DIR}/02_plan.json}"
APPLY_JSON="${APPLY_JSON:-${REQ_DIR}/03_apply_result.json}"
SUMMARY_TXT="${SUMMARY_TXT:-${REQ_DIR}/actions-summary.txt}"

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
import json,sys,os,re
p = sys.argv[1]
with open(p,'r',encoding='utf-8') as f:
    plan = json.load(f)

meta = plan.get('meta',{}) or plan.get('META',{})
text = plan.get('text',{}) or plan.get('TEXT',{}) or {}
block = plan.get('block',{}) or plan.get('BLOCK',{}) or {}
lists = plan.get('lists',{}) or plan.get('LISTS',{}) or {}
iff   = plan.get('if',{})   or plan.get('IF',{})   or {}
gradle = plan.get('gradle',{}) or plan.get('GRADLE',{}) or {}

def sh_kv_map(name, d):
    out = [f'declare -gA {name}=(']
    for k,v in (d or {}).items():
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
# lists -> \x1f join
lists_norm={}
for k,v in (lists or {}).items():
    if isinstance(v,list): lists_norm[k]=[str(x) for x in v]
    elif v is None: lists_norm[k]=[]
    else: lists_norm[k]=[str(v)]
print('declare -gA LISTS_KV=(' +
      ' '.join([f'["{k}"]="' + "\x1f".join(v) + '"' for k,v in lists_norm.items()]) + ')')

print(sh_kv_map('IFCOND_KV', iff))
# also expose gradle.applicationId for package fallback
print(f'GRADLE_APPID="{gradle.get("applicationId","")}"')
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

# decode LISTS_KV values (split by \x1f) to bash arrays
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
  -name "*.xml" -o -name "*.kt" -o -name "*.java" -o -name "*.kts" -o -name "*.gradle" -o \
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
  local cnt=0 new_body; new_body="$body"

  # xml
  if perl -0777 -ne 'exit 1 unless /<!--\s*BLOCK:'"$name"'\s*-->.*?<!--\s*END_BLOCK\s*-->/s' "$file"; then
    perl -0777 -i -pe 's/<!--\s*BLOCK:'"$name"'\s*-->.*?<!--\s*END_BLOCK\s*-->/<!-- BLOCK:'"$name"' -->\n'"$new_body"'\n<!-- END_BLOCK -->/s' "$file"
    cnt=$((cnt+1))
  fi
  # //
  if perl -0777 -ne 'exit 1 unless /\/\/\s*BLOCK:'"$name"'.*?\/\/\s*END_BLOCK/s' "$file"; then
    perl -0777 -i -pe 's/\/\/\s*BLOCK:'"$name"'.*?\/\/\s*END_BLOCK/\/\/ BLOCK:'"$name"'\n'"$new_body"'\n\/\/ END_BLOCK/s' "$file"
    cnt=$((cnt+1))
  fi
  # /* */
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

# ---------- apply all ----------
apply_all() {
  local file k v
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

# ---------- Post-fix: Kotlin rename & package line ----------
autofix_kotlin_and_package() {
  local renamed=0 pkgfixed=0
  local want_pkg="${META_PKG:-}"
  [ -z "$want_pkg" ] && want_pkg="${GRADLE_APPID:-}"

  # 1) rename .java -> .kt if the file actually contains Kotlin
  if [ -d "$APP_DIR/src/main/java" ]; then
    while IFS= read -r -d '' f; do
      if python - "$f" <<'PY'
import sys,re,io
p=sys.argv[1]
try: s=open(p,'r',encoding='utf-8',errors='ignore').read()
except: sys.exit(1)
# Kotlin hints (PCRE-like but Python re):
pat = re.compile(r'''(?x)
    \b(data|sealed|enum)\s+class\b
  | \bobject\s+\w+
  | \bcompanion\s+object\b
  | \bsuspend\s+fun\b
  | \bfun\s+\w+\s*\(
  | \b(val|var)\s+\w+\s*:
  | \bclass\s+\w+\s*:\s+[A-Z]\w+
''')
sys.exit(0 if pat.search(s) else 1)
PY
      then
        new="${f%.java}.kt"
        if [ -e "$new" ] && [ "$new" != "$f" ]; then
          new="${f%.java}.ndjc.kt"
        fi
        git mv -f "$f" "$new" 2>/dev/null || mv -f "$f" "$new"
        renamed=$((renamed+1))
      fi
    done < <(find "$APP_DIR/src/main/java" -type f -name "*.java" -print0 2>/dev/null || true)
  fi

  # 2) unify package line to expected package (if available)
  if [ -n "$want_pkg" ]; then
    while IFS= read -r -d '' g; do
      if python - "$g" "$want_pkg" <<'PY'
import sys,re
p, pkg = sys.argv[1], sys.argv[2]
try:
    s = open(p,'r',encoding='utf-8',errors='ignore').read()
except:
    sys.exit(1)
changed = False
m = re.search(r'^\s*package\s+([A-Za-z0-9_.]+)\s*;?', s, flags=re.M)
if m:
    if m.group(1) != pkg:
        s = re.sub(r'^\s*package\s+[A-Za-z0-9_.]+\s*;?', 'package '+pkg, s, count=1, flags=re.M)
        changed = True
else:
    # insert at beginning
    s = 'package '+pkg+'\n'+s
    changed = True
if changed:
    open(p,'w',encoding='utf-8').write(s)
    sys.exit(0)
else:
    sys.exit(2)
PY
      then
        pkgfixed=$((pkgfixed+1))
      fi
    done < <(find "$APP_DIR/src/main" -type f \( -name "*.kt" -o -name "*.java" \) -print0)
  fi

  echo "{\"renamed\": ${renamed}, \"pkgfixed\": ${pkgfixed}}"
  echo "autofix: kotlin_renamed=${renamed}  package_fixed=${pkgfixed}"
}

autofix_kotlin_and_package || true

# 补充 missing 统计（只统计“完全未命中”的锚点个数）
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

# 03_apply_result.json
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

echo "NDJC materialize: total=${replaced_total} text=${replaced_text} block=${replaced_block} list=${replaced_list} if=${replaced_if}"
echo "missing: text=${missing_text} block=${missing_block} list=${missing_list} if=${missing_if}"
