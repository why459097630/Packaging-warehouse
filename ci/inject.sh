#!/bin/bash
# inject.sh

TEMPLATE=$1
APP_NAME=$2
API_BASE=$3
API_SECRET=$4

# 假设我们将数据注入到 strings.xml 中
echo "Injecting API data into template..."

VALUES_DIR="templates/$TEMPLATE/app/src/main/res/values"
mkdir -p $VALUES_DIR

# 更新 strings.xml 文件
cat <<EOF > $VALUES_DIR/strings.xml
<resources>
    <string name="app_name">$APP_NAME</string>
    <string name="api_base">$API_BASE</string>
    <string name="api_secret">$API_SECRET</string>
</resources>
EOF

echo "Injection complete!"
