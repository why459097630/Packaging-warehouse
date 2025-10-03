#!/usr/bin/env bash
# NDJC: materialize anchors from plan → apply into template app
# Usage:
#   bash scripts/ndjc-materialize.sh <APP_DIR> <RUN_ID>
# or:
#   APP_DIR=templates/circle-basic/app RUN_ID=xxx bash scripts/ndjc-materialize.sh
#
# Inputs:
#   requests/<RUN_ID>/02_plan.json
# Outputs:
#   requests/<RUN_ID>/03_apply_result.json
#   requests/<RUN_ID>/actions-summary.txt

set -euo pipefail

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

# ---------- utils ----------
ts_utc() { date -u +%Y-%m-%dT%H:%M:%SZ; }
escape_sed() { printf '%s' "$1" | sed -e 's/[\/&]/\\&/g'; }

# record changes into a temp file as JSON lines
CHANGES_FILE="$(mktemp)"
add_change() {
  local file="$1" marker="$2" count="$3" kind="$4"
  python - "$file" "$marker" "$count" "$kind" "$CHANGES_FILE" <<'PY'
import json,sys
file, marker, count, kind, outp = sys.argv[1], sys.argv[2], int(sys.argv[3]), sys.argv[4], sys.argv[5]
with open(outp, 'a', encoding='utf-8') as f:
    f.write(json.dumps({"file":file,"marker":marker,"replacedCount":count,"kind":kind})+"\n")
PY
}

# ---------- parse plan (python → bash) ----------
read_plan() {
  python - "$PLAN_JSON" <<'PY'
import json,sys
p = sys.argv[1]
with open(p,'r',encoding='utf-8') as f:
    plan = json.load(f)

meta = plan.get('meta',{}) or {}
text = plan.get('text',{}) or plan.get('TEXT',{}) or {}
block = plan.get('block',{}) or plan.get('BLOCK',{}) or {}
lists = plan.get('lists',{}) or plan.get('LISTS',{}) or {}
iff   = plan.get('if',{})   or plan.get('IF',{})   or {}
# 兼容扩展：resources/hooks
res   = plan.get('resources',{}) or {}
hooks = plan.get('hooks',{}) or {}

def sh_kv_map(name, d):
    out = [f'declare -gA {name}=(']
    for k,v in (d or {}).items():
        k = str(k)
        v = '' if v is None else str(v).replace('\n','\\n')
        out.append('["{}"]="{}"'.format(k, v))
    out.append(')')
    return ' '.join(out)

# lists 需要序列化为 ["a\x1fb\x1fc"] 的一条值，bash 再 split
lists_norm = {}
for k,v in (lists or {}).items():
    if isinstance(v,list):
        lists_norm[k] = [str(x) for x in v]
    elif v is None:
        lists_norm[k] = []
    else:
        lists_norm[k] = [str(v)]

print('META_TEMPLATE="{}"'.format(meta.get("template","")))
print('META_APPNAME="{}"'.format(meta.get("appName","")))
print('META_PKG="{}"'.format(meta.get("packageId","")))
print('META_MODE="{}"'.format(meta.get("mode","")))

print(sh_kv_map('TEXT_KV', text))
print(sh_kv_map('BLOCK_KV', block))
print('declare -gA LISTS_KV=(' + ' '.join(['["{}"]="{}"'.format(k, "\x1f".join(v)) for k,v in lists_norm.items()]) + ')')
print(sh_kv_map('IFCOND_KV', iff))
print(sh_kv_map('RES_KV', res))
print(sh_kv_map('HOOK_KV', hooks))
PY
}
eval "$(read_plan)"

# ensure assoc arrays exist
ensure_assoc() {
  local name="$1"
  if ! declare -p "$name" >/dev/null 2>&1; then
    eval "declare -gA $name=()"
  elif ! declare -p "$name" 2>/dev/null | grep -q 'declare \-A'; then
    eval "unset $name; declare -gA $name=()"
  fi
}
ensure_assoc TEXT_KV; ensure_assoc BLOCK_KV; ensure_assoc LISTS_KV; ensure_assoc IFCOND_KV; ensure_assoc RES_KV; ensure_assoc HOOK_KV

# decode LISTS_KV values
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
autofix_kotlin_renamed=0
autofix_package_fixed=0

# ---------- file selection ----------
mapfile -t WORK_FILES < <(find "$APP_DIR" -type f \( \
  -name "*.xml" -o -name "*.kt" -o -name "*.kts" -o -name "*.gradle" -o \
  -name "*.pro" -o -name "AndroidManifest.xml" -o -name "*.txt" \
  \) | LC_ALL=C sort)

# ---------- text replacement ----------
replace_text_in_file() {
  local file="$1" key="$2" val="$3"
  local before after count vv
  before=$(grep -o -n -F "$key" "$file" | wc -l | tr -d ' ')
  [ "$before" -eq 0 ] && return 1
  vv="$(escape_sed "$val")"
  sed -i "s/${key}/${vv}/g" "$file"
  after=$(grep -o -n -F "$key" "$file" | wc -l | tr -d ' ')
  count=$((before-after)); [ "$count" -lt 0 ] && count=0
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
  local items tmpl rendered
  items="$(get_list_items "$name" || true)"
  [ -z "$items" ] && return 1
  if ! perl -0777 -ne 'exit 1 unless /LIST:'"$name"'.*?END_LIST/s' "$file"; then
    return 1
  fi
  tmpl="$(perl -0777 -ne 'if (/LIST:'"$name"'\s*(.*)\s*END_LIST/s) { print($1) }' "$file")"
  [ -z "$tmpl" ] && return 1

  rendered=""
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

# ---------- Kotlin/Java 兜底修复 ----------
# 目标：若 Kotlin 代码误存为 .java，改名为 .kt，并修正/插入 package
python - "$APP_DIR" "$CHANGES_FILE" <<'PY'
import os, re, json, sys, shutil, io
app_dir, changes_path = sys.argv[1], sys.argv[2]
java_root = os.path.join(app_dir, 'src', 'main', 'java')
if not os.path.isdir(java_root):
    sys.exit(0)

KOTLIN_SIG = re.compile(r'\b(fun|val|var|data\s+class|object|@Composable)\b')
PKG_RE = re.compile(r'^\s*package\s+([a-zA-Z0-9_.]+)\s*$', re.M)

def desired_pkg(path):
    # path: .../src/main/java/com/xxx/yyy/Foo.java
    rel = os.path.relpath(path, java_root)
    parts = rel.replace('\\','/').split('/')
    if len(parts) < 2:  # file directly under java/
        return None
    return '.'.join(parts[:-1])

def add_change(file, marker, count, kind):
    with open(changes_path,'a',encoding='utf-8') as f:
        f.write(json.dumps({"file":file,"marker":marker,"replacedCount":count,"kind":kind})+"\n")

renamed = 0
pkgfixed = 0

for root,_,files in os.walk(java_root):
    for fn in files:
        full = os.path.join(root, fn)
        try:
            with open(full,'r',encoding='utf-8',errors='ignore') as f:
                content = f.read()
        except Exception:
            continue

        looks_kotlin = bool(KOTLIN_SIG.search(content))
        base, ext = os.path.splitext(full)

        # 1) .java 但内容像 Kotlin → 重命名为 .kt
        if ext == '.java' and looks_kotlin:
            new = base + '.kt'
            shutil.move(full, new)
            add_change(new, "autofix:kotlin_rename", 1, "autofix")
            renamed += 1
            full = new
            content = content  # unchanged text

        # 2) 修正/注入 package
        want_pkg = desired_pkg(full)
        if not want_pkg:
            continue
        m = PKG_RE.search(content)
        if m:
            cur = m.group(1).strip()
            if cur != want_pkg:
                content = PKG_RE.sub('package '+want_pkg, content, count=1)
                with open(full,'w',encoding='utf-8') as f:
                    f.write(content)
                add_change(full, "autofix:package_fix", 1, "autofix")
                pkgfixed += 1
        else:
            # insert at top
            content = 'package '+want_pkg+'\n\n'+content
            with open(full,'w',encoding='utf-8') as f:
                f.write(content)
            add_change(full, "autofix:package_insert", 1, "autofix")
            pkgfixed += 1

print(json.dumps({"renamed":renamed,"pkgfixed":pkgfixed}))
PY
# 把 Python 汇总读回到 bash 计数
auto_json="$(tail -n 1 "$CHANGES_FILE" 2>/dev/null | grep -q '"kind":"autofix"' && echo || true)"
# 上面一行仅为了不报错；真正的计数从直接调用 python 的 stdout 获取：
auto_summary="$(python - "$APP_DIR" "$CHANGES_FILE" <<'PY'
# dummy pass to print what previous python already printed; we don't re-run
PY
)"
# 为了保证计数，我们直接重新读取 CHANGES_FILE 统计：
autofix_kotlin_renamed="$(grep -c '"autofix:kotlin_rename"' "$CHANGES_FILE" || true)"
autofix_package_fixed="$(grep -E -c '"autofix:package_(fix|insert)"' "$CHANGES_FILE" || true)"

# ---------- resources/hooks（目前仅计数，真正落盘由模板/生成器负责；可按需扩展） ----------
resources_written=$((resources_written+0))
hooks_applied=$((hooks_applied+0))

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

# ---------- write 03_apply_result.json ----------
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
  "autofix_kotlin_renamed": ${autofix_kotlin_renamed},
  "autofix_package_fixed": ${autofix_package_fixed},
  "generated_at": "$(ts_utc)"
}
JSON

# ---------- write actions-summary.txt ----------
{
  echo "branch=${GITHUB_REF_NAME:-main}"
  echo "variant=unknown"
  echo "apk="
  echo "classes="
  echo "missing_text=${missing_text}"
  echo "missing_block=${missing_block}"
  echo "autofix_kotlin_renamed=${autofix_kotlin_renamed}"
  echo "autofix_package_fixed=${autofix_package_fixed}"
  echo "ts=$(ts_utc)"
} > "$SUMMARY_TXT"

echo "NDJC materialize: total=${replaced_total} text=${replaced_text} block=${replaced_block} list=${replaced_list} if=${replaced_if}"
echo "missing: text=${missing_text} block=${missing_block} list=${missing_list} if=${missing_if}"
echo "autofix: kotlin_renamed=${autofix_kotlin_renamed} package_fixed=${autofix_package_fixed}"
