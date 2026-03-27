#!/usr/bin/env bash
set -euo pipefail

ASSEMBLY_JSON="lib/ndjc/assembly.local.json"
APP_DIR="templates/Core-Templates/app"
GOOGLE_SERVICES_JSON="${APP_DIR}/google-services.json"

if [ ! -f "$ASSEMBLY_JSON" ]; then
  echo "::error::Missing $ASSEMBLY_JSON"
  exit 1
fi

if [ -z "${FIREBASE_PROJECT_ID:-}" ]; then
  echo "::error::FIREBASE_PROJECT_ID is required"
  exit 1
fi

if ! command -v gcloud >/dev/null 2>&1; then
  echo "::error::gcloud is required but not found"
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "::error::python3 is required but not found"
  exit 1
fi

PACKAGE_NAME="$(node -e "const fs=require('fs'); const j=JSON.parse(fs.readFileSync('${ASSEMBLY_JSON}','utf8')); process.stdout.write(((j.packageName||j.applicationId||'')+'').trim());")"
APP_LABEL="$(node -e "const fs=require('fs'); const j=JSON.parse(fs.readFileSync('${ASSEMBLY_JSON}','utf8')); process.stdout.write(((j.appName||j.app_label||'NDJC App')+'').trim());")"

if [ -z "$PACKAGE_NAME" ]; then
  echo "::error::packageName/applicationId missing in ${ASSEMBLY_JSON}"
  exit 1
fi

ACCESS_TOKEN="$(gcloud auth print-access-token)"
if [ -z "$ACCESS_TOKEN" ]; then
  echo "::error::Failed to get Google access token"
  exit 1
fi

mkdir -p "$APP_DIR"

echo "[NDJC_FIREBASE] project=${FIREBASE_PROJECT_ID}"
echo "[NDJC_FIREBASE] packageName=${PACKAGE_NAME}"
echo "[NDJC_FIREBASE] appLabel=${APP_LABEL}"

echo "[NDJC_FIREBASE] active gcloud account"
gcloud auth list

echo "[NDJC_FIREBASE] active gcloud project"
gcloud config get-value project || true

echo "[NDJC_FIREBASE] service account from ADC"
gcloud auth application-default print-access-token >/dev/null 2>&1 && echo "[NDJC_FIREBASE] application-default credentials available" || echo "[NDJC_FIREBASE] application-default credentials unavailable"

echo "[NDJC_FIREBASE] token identity"
curl -sS -H "Authorization: Bearer ${ACCESS_TOKEN}" "https://oauth2.googleapis.com/tokeninfo?access_token=${ACCESS_TOKEN}" || true
echo

TMP_DIR="$(mktemp -d)"
LIST_JSON="${TMP_DIR}/android-apps-list.json"
CREATE_JSON="${TMP_DIR}/android-app-create.json"
OP_JSON="${TMP_DIR}/operation.json"
CFG_JSON="${TMP_DIR}/config.json"

cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

fetch_android_apps() {
  local page_token=""
  local merged='{"apps":[]}'
  local first="1"

  while true; do
    local url="https://firebase.googleapis.com/v1beta1/projects/${FIREBASE_PROJECT_ID}/androidApps?pageSize=100"
    if [ -n "$page_token" ]; then
      url="${url}&pageToken=${page_token}"
    fi

    local resp
    resp="$(curl -sS -X GET \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" \
      -H "Accept: application/json" \
      "$url")"

    local maybe_error
    maybe_error="$(printf '%s' "$resp" | python3 - <<'PY'
import sys, json
raw = sys.stdin.read().strip()
if not raw:
    print("")
    sys.exit(0)
try:
    data = json.loads(raw)
except Exception:
    print("INVALID_JSON")
    sys.exit(0)
err = data.get("error")
if err:
    print(json.dumps(err, ensure_ascii=False))
else:
    print("")
PY
)"
    if [ -n "$maybe_error" ]; then
      echo "::error::Firebase androidApps.list failed: $maybe_error"
      exit 1
    fi

    if [ "$first" = "1" ]; then
      merged="$resp"
      first="0"
    else
      merged="$(MERGED="$merged" RESP="$resp" python3 - <<'PY'
import os, json
merged = json.loads(os.environ["MERGED"])
resp = json.loads(os.environ["RESP"])
apps = merged.get("apps", [])
apps.extend(resp.get("apps", []))
merged["apps"] = apps
merged["nextPageToken"] = resp.get("nextPageToken", "")
print(json.dumps(merged, ensure_ascii=False))
PY
)"
    fi

    page_token="$(printf '%s' "$resp" | python3 - <<'PY'
import sys, json
raw = sys.stdin.read().strip()
if not raw:
    print("")
    sys.exit(0)
data = json.loads(raw)
print(data.get("nextPageToken", ""))
PY
)"
    if [ -z "$page_token" ]; then
      break
    fi
  done

  printf '%s' "$merged"
}

find_existing_app_id() {
  local json_file="$1"
  python3 - "$json_file" "$PACKAGE_NAME" <<'PY'
import sys, json
path = sys.argv[1]
target = sys.argv[2]
with open(path, "r", encoding="utf-8") as f:
    data = json.load(f)

for app in data.get("apps", []):
    if app.get("packageName") == target:
        app_id = app.get("appId", "").strip()
        if not app_id:
            name = app.get("name", "").strip()
            if name:
                app_id = name.split("/")[-1]
        print(app_id)
        break
else:
    print("")
PY
}

printf '%s' "$(fetch_android_apps)" > "$LIST_JSON"

EXISTING_APP_ID="$(find_existing_app_id "$LIST_JSON")"

if [ -n "$EXISTING_APP_ID" ]; then
  APP_ID="$EXISTING_APP_ID"
  echo "[NDJC_FIREBASE] existing Android app found: ${APP_ID}"
else
  echo "[NDJC_FIREBASE] Android app not found, creating..."

  CREATE_BODY="$(PACKAGE_NAME="$PACKAGE_NAME" APP_LABEL="$APP_LABEL" python3 - <<'PY'
import json, os
body = {
    "packageName": os.environ["PACKAGE_NAME"],
    "displayName": os.environ["APP_LABEL"]
}
print(json.dumps(body, ensure_ascii=False))
PY
)"

  printf '%s' "$CREATE_BODY" > "$CREATE_JSON"

  CREATE_RESP_FILE="${TMP_DIR}/android-app-create-response.txt"
  CREATE_HTTP_CODE="$(curl -sS -o "$CREATE_RESP_FILE" -w "%{http_code}" -X POST \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -d @"$CREATE_JSON" \
    "https://firebase.googleapis.com/v1beta1/projects/${FIREBASE_PROJECT_ID}/androidApps")"

  CREATE_RESP="$(cat "$CREATE_RESP_FILE")"

  echo "[NDJC_FIREBASE] androidApps.create http=${CREATE_HTTP_CODE}"
  echo "[NDJC_FIREBASE] androidApps.create raw response begin"
  cat "$CREATE_RESP_FILE"
  echo
  echo "[NDJC_FIREBASE] androidApps.create raw response end"

  if [ "$CREATE_HTTP_CODE" -lt 200 ] || [ "$CREATE_HTTP_CODE" -ge 300 ]; then
    echo "::error::Firebase androidApps.create http=${CREATE_HTTP_CODE}"
    exit 1
  fi

  CREATE_ERROR="$(printf '%s' "$CREATE_RESP" | python3 - <<'PY'
import sys, json
raw = sys.stdin.read().strip()
if not raw:
    print("EMPTY_RESPONSE")
    sys.exit(0)
try:
    data = json.loads(raw)
except Exception:
    print("INVALID_JSON")
    sys.exit(0)
err = data.get("error")
if err:
    print(json.dumps(err, ensure_ascii=False))
else:
    print("")
PY
)"
  if [ -n "$CREATE_ERROR" ]; then
    echo "::error::Firebase androidApps.create failed: $CREATE_ERROR"
    exit 1
  fi

  OP_NAME="$(printf '%s' "$CREATE_RESP" | python3 - <<'PY'
import sys, json
data = json.loads(sys.stdin.read())
print(data.get("name", ""))
PY
)"
  if [ -z "$OP_NAME" ]; then
    echo "::error::Firebase androidApps.create did not return operation name"
    exit 1
  fi

  echo "[NDJC_FIREBASE] create operation: ${OP_NAME}"

  for i in $(seq 1 30); do
    OP_RESP="$(curl -sS -X GET \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" \
      -H "Accept: application/json" \
      "https://firebase.googleapis.com/v1beta1/${OP_NAME}")"

    printf '%s' "$OP_RESP" > "$OP_JSON"

    OP_ERROR="$(python3 - "$OP_JSON" <<'PY'
import sys, json
with open(sys.argv[1], "r", encoding="utf-8") as f:
    data = json.load(f)
err = data.get("error")
if err:
    print(json.dumps(err, ensure_ascii=False))
else:
    print("")
PY
)"
    if [ -n "$OP_ERROR" ]; then
      echo "::error::Firebase operation failed: $OP_ERROR"
      exit 1
    fi

    OP_DONE="$(python3 - "$OP_JSON" <<'PY'
import sys, json
with open(sys.argv[1], "r", encoding="utf-8") as f:
    data = json.load(f)
print("true" if data.get("done") else "false")
PY
)"
    if [ "$OP_DONE" = "true" ]; then
      echo "[NDJC_FIREBASE] create operation done"
      break
    fi

    if [ "$i" = "30" ]; then
      echo "::error::Timed out waiting for Firebase android app creation"
      exit 1
    fi

    sleep 2
  done

  printf '%s' "$(fetch_android_apps)" > "$LIST_JSON"
  APP_ID="$(find_existing_app_id "$LIST_JSON")"

  if [ -z "$APP_ID" ]; then
    echo "::error::Android app still not found after creation: ${PACKAGE_NAME}"
    exit 1
  fi

  echo "[NDJC_FIREBASE] created Android app: ${APP_ID}"
fi

CONFIG_RESP="$(curl -sS -X GET \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Accept: application/json" \
  "https://firebase.googleapis.com/v1beta1/projects/-/androidApps/${APP_ID}/config")"

printf '%s' "$CONFIG_RESP" > "$CFG_JSON"

CONFIG_ERROR="$(python3 - "$CFG_JSON" <<'PY'
import sys, json
with open(sys.argv[1], "r", encoding="utf-8") as f:
    data = json.load(f)
err = data.get("error")
if err:
    print(json.dumps(err, ensure_ascii=False))
else:
    print("")
PY
)"
if [ -n "$CONFIG_ERROR" ]; then
  echo "::error::Firebase androidApps.getConfig failed: $CONFIG_ERROR"
  exit 1
fi

python3 - "$CFG_JSON" "$GOOGLE_SERVICES_JSON" <<'PY'
import sys, json, base64
src = sys.argv[1]
dst = sys.argv[2]
with open(src, "r", encoding="utf-8") as f:
    data = json.load(f)
payload = data.get("configFileContents", "")
if not payload:
    raise SystemExit("configFileContents is empty")
decoded = base64.b64decode(payload)
with open(dst, "wb") as out:
    out.write(decoded)
PY

echo "[NDJC_FIREBASE] wrote ${GOOGLE_SERVICES_JSON}"
ls -la "$GOOGLE_SERVICES_JSON"

echo "[NDJC_FIREBASE] verify package name in google-services.json"
python3 - "$GOOGLE_SERVICES_JSON" "$PACKAGE_NAME" <<'PY'
import sys, json
path = sys.argv[1]
target = sys.argv[2]
with open(path, "r", encoding="utf-8") as f:
    data = json.load(f)

found = False
for client in data.get("client", []):
    info = client.get("client_info", {})
    android_info = info.get("android_client_info", {})
    if android_info.get("package_name") == target:
        found = True
        break

if not found:
    raise SystemExit(f"google-services.json does not contain package_name={target}")

print(f"[NDJC_FIREBASE] package_name matched: {target}")
PY
