#!/bin/bash

# 1. 生成代码包
echo "Generating code..."
./ci/generate.sh --prompt "$PROMPT" --out generated
tar -czf generated.tar.gz generated

# 2. 注入模板
echo "Injecting into template..."
tar -xzf generated.tar.gz
./ci/inject.sh generated template workspace
tar -czf workspace.tar.gz -C workspace .

# 3. 打包 APK
echo "Building APK..."
./gradlew assembleRelease --no-daemon --build-cache

# 4. 签名
echo "Signing APK..."
jarsigner -keystore keystore.jks -storepass $KS_PASS -keypass $KEY_PASS app-release-unsigned.apk $KEY_ALIAS
mv app-release-unsigned.apk app-release-signed.apk

# 5. 显示签名后的 APK
echo "APK signed and ready: app-release-signed.apk"
