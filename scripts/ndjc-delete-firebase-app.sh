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

find_existing_app_resource_name() {
  local json_file="$1"
  python3 - "$json_file" "$PACKAGE_NAME" <<'PY'
import sys, json
path = sys.argv[1]
target = sys.argv[2]

with open(path, "r", encoding="utf-8") as f:
    data = json.load(f)

for app in data.get("apps", []):
    if (app.get("packageName") or "").strip() == target:
        resource_name = (app.get("name") or "").strip()
        app_id = (app.get("appId") or "").strip()
        package_name = (app.get("packageName") or "").strip()
        print(resource_name)
        print(app_id)
        print(package_name)
        sys.exit(0)

sys.exit(0)
PY
}

printf '%s' "$(fetch_android_apps)" > "$LIST_JSON"

MATCH_LINES="$(find_existing_app_resource_name "$LIST_JSON")"

if [ -z "$MATCH_LINES" ]; then
  echo "[NDJC_FIREBASE_DELETE] app not found, treat as already deleted"
  echo "[NDJC_FIREBASE_DELETE] package=${PACKAGE_NAME}"
  echo "[NDJC_FIREBASE_DELETE] list_json follows"
  cat "$LIST_JSON" || true
  exit 0
fi

RESOURCE_NAME="$(printf '%s\n' "$MATCH_LINES" | sed -n '1p')"
APP_ID="$(printf '%s\n' "$MATCH_LINES" | sed -n '2p')"
MATCHED_PACKAGE_NAME="$(printf '%s\n' "$MATCH_LINES" | sed -n '3p')"

if [ -z "$RESOURCE_NAME" ]; then
  echo "::error::Matched Firebase app is missing resource name"
  echo "[NDJC_FIREBASE_DELETE] package=${PACKAGE_NAME}"
  echo "[NDJC_FIREBASE_DELETE] match_lines=${MATCH_LINES}"
  echo "[NDJC_FIREBASE_DELETE] list_json follows"
  cat "$LIST_JSON" || true
  exit 1
fi

DELETE_URL="https://firebase.googleapis.com/v1beta1/${RESOURCE_NAME}"

echo "[NDJC_FIREBASE_DELETE] package=${PACKAGE_NAME}"
echo "[NDJC_FIREBASE_DELETE] matched_package_name=${MATCHED_PACKAGE_NAME}"
echo "[NDJC_FIREBASE_DELETE] resource_name=${RESOURCE_NAME}"
echo "[NDJC_FIREBASE_DELETE] app_id=${APP_ID}"
echo "[NDJC_FIREBASE_DELETE] delete_url=${DELETE_URL}"

HTTP_CODE="$(curl -sS -o /tmp/ndjc-firebase-delete-response.txt -w "%{http_code}" -X DELETE \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Accept: application/json" \
  "$DELETE_URL")"

echo "[NDJC_FIREBASE_DELETE] delete http=${HTTP_CODE}"
cat /tmp/ndjc-firebase-delete-response.txt || true
echo

if [ "$HTTP_CODE" = "404" ]; then
  echo "::error::Firebase androidApps.delete returned 404; resource not found"
  echo "[NDJC_FIREBASE_DELETE] list_json follows"
  cat "$LIST_JSON" || true
  exit 1
fi

if [ "$HTTP_CODE" -lt 200 ] || [ "$HTTP_CODE" -ge 300 ]; then
  echo "::error::Firebase androidApps.delete http=${HTTP_CODE}"
  echo "[NDJC_FIREBASE_DELETE] list_json follows"
  cat "$LIST_JSON" || true
  exit 1
fi

echo "[NDJC_FIREBASE_DELETE] deleted package=${PACKAGE_NAME} resource_name=${RESOURCE_NAME} appId=${APP_ID}"
