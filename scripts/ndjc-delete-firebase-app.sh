#!/usr/bin/env bash
set -euo pipefail

if [ -z "${FIREBASE_PROJECT_ID:-}" ]; then
  echo "::error::FIREBASE_PROJECT_ID is required"
  exit 1
fi

if [ -z "${PACKAGE_NAME:-}" ]; then
  echo "::error::PACKAGE_NAME is required"
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

ACCESS_TOKEN="$(gcloud auth print-access-token)"
if [ -z "$ACCESS_TOKEN" ]; then
  echo "::error::Failed to get Google access token"
  exit 1
fi

TMP_DIR="$(mktemp -d)"
LIST_JSON="${TMP_DIR}/android-apps-list.json"

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

APP_ID="$(find_existing_app_id "$LIST_JSON")"

if [ -z "$APP_ID" ]; then
  echo "[NDJC_FIREBASE_DELETE] app not found, treat as already deleted"
  exit 0
fi

DELETE_URL="https://firebase.googleapis.com/v1beta1/projects/${FIREBASE_PROJECT_ID}/androidApps/${APP_ID}"
HTTP_CODE="$(curl -sS -o /tmp/ndjc-firebase-delete-response.txt -w "%{http_code}" -X DELETE \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Accept: application/json" \
  "$DELETE_URL")"

echo "[NDJC_FIREBASE_DELETE] delete http=${HTTP_CODE}"
cat /tmp/ndjc-firebase-delete-response.txt || true
echo

if [ "$HTTP_CODE" = "404" ]; then
  echo "[NDJC_FIREBASE_DELETE] app already deleted"
  exit 0
fi

if [ "$HTTP_CODE" -lt 200 ] || [ "$HTTP_CODE" -ge 300 ]; then
  echo "::error::Firebase androidApps.delete http=${HTTP_CODE}"
  exit 1
fi

echo "[NDJC_FIREBASE_DELETE] deleted package=${PACKAGE_NAME} appId=${APP_ID}"