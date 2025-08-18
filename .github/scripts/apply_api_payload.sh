#!/usr/bin/env bash
set -euo pipefail

PKG="com.example.meditationtimer"
JAVA_DIR="app/src/main/java/com/example/meditationtimer"
LAYOUT_DIR="app/src/main/res/layout"
VALUES_DIR="app/src/main/res/values"

mkdir -p "$JAVA_DIR" "$LAYOUT_DIR" "$VALUES_DIR"

# 你把 API 返回的三段内容喂进来：MAIN_JAVA / LAYOUT_XML / STRINGS_XML
: "${MAIN_JAVA:?MAIN_JAVA missing}"
: "${LAYOUT_XML:?LAYOUT_XML missing}"
: "${STRINGS_XML:?STRINGS_XML missing}"

printf '%s' "$MAIN_JAVA"   > "$JAVA_DIR/MainActivity.java"
printf '%s' "$LAYOUT_XML"  > "$LAYOUT_DIR/activity_main.xml"
printf '%s' "$STRINGS_XML" > "$VALUES_DIR/strings.xml"

# 兜底：确保包名一致
if ! grep -q "package $PKG" "$JAVA_DIR/MainActivity.java"; then
  echo "Fixing package name to $PKG"
  sed -i "1s|^package .*;|package $PKG;|" "$JAVA_DIR/MainActivity.java" || true
fi

# 最小 Manifest（若仓库已有就不改）
MANIFEST="app/src/main/AndroidManifest.xml"
if [ ! -s "$MANIFEST" ]; then
  cat > "$MANIFEST" <<EOF
<manifest package="$PKG" xmlns:android="http://schemas.android.com/apk/res/android">
  <application android:label="@string/app_name">
    <activity android:name=".MainActivity">
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>
  </application>
</manifest>
EOF
fi
