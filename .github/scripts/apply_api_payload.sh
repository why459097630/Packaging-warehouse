#!/usr/bin/env bash
set -euo pipefail

# 该脚本把 API 返回的源码写进标准 Android 目录
# 支持用环境变量传入三段源码：
#   MAIN_JAVA / LAYOUT_XML / STRINGS_XML
# 也会兜底生成最小 Manifest，并校正包名

PKG="com.example.meditationtimer"
JAVA_DIR="app/src/main/java/com/example/meditationtimer"
LAYOUT_DIR="app/src/main/res/layout"
VALUES_DIR="app/src/main/res/values"
MANIFEST="app/src/main/AndroidManifest.xml"

mkdir -p "$JAVA_DIR" "$LAYOUT_DIR" "$VALUES_DIR"

: "${MAIN_JAVA:?MAIN_JAVA missing}"
: "${LAYOUT_XML:?LAYOUT_XML missing}"
: "${STRINGS_XML:?STRINGS_XML missing}"

printf '%s' "$MAIN_JAVA"   > "$JAVA_DIR/MainActivity.java"
printf '%s' "$LAYOUT_XML"  > "$LAYOUT_DIR/activity_main.xml"
printf '%s' "$STRINGS_XML" > "$VALUES_DIR/strings.xml"

# 确保包名一致
if ! grep -q "package $PKG" "$JAVA_DIR/MainActivity.java"; then
  sed -i "1s|^package .*;|package $PKG;|" "$JAVA_DIR/MainActivity.java" || true
fi

# 兜底 Manifest（仓库已有就不覆盖）
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

echo "✅ API payload applied into app/src"
