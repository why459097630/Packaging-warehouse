#!/usr/bin/env bash
set -euo pipefail

PKG="com.example.meditationtimer"
PKG_PATH=$(echo "$PKG" | tr '.' '/')

echo "[check] ensure generated marker exists"
test -f generated/.ok || (echo "missing generated/.ok" && exit 1)

echo "[check] ensure MainActivity exists"
test -f app/src/main/java/${PKG_PATH}/MainActivity.java || (echo "missing MainActivity.java" && exit 1)

echo "[check] ensure package name in MainActivity is correct"
grep -q "package ${PKG};" app/src/main/java/${PKG_PATH}/MainActivity.java || (echo "wrong package in MainActivity.java" && exit 1)

echo "[check] ensure layout exists"
test -f app/src/main/res/layout/activity_main.xml || (echo "missing activity_main.xml" && exit 1)

echo "[check] ensure strings exists"
test -f app/src/main/res/values/strings.xml || (echo "missing strings.xml" && exit 1)

echo "[check] OK"
