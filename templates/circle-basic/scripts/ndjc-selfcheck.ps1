Param(
  [string]$Root = "."
)

$errors = @()

# 必要文件
$required = @(
  "settings.gradle",
  "gradle/wrapper/gradle-wrapper.properties",
  "app/build.gradle",
  "app/src/main/AndroidManifest.xml",
  "anchors.schema.json",
  "slots.map.json"
)
$required | ForEach-Object {
  if (-not (Test-Path (Join-Path $Root $_))) { $errors += "缺文件: $_" }
}

# 读取 schema，统计锚点
$schemaPath = Join-Path $Root "anchors.schema.json"
if (Test-Path $schemaPath) {
  try {
    $schema = Get-Content $schemaPath -Raw | ConvertFrom-Json
    $count = ($schema.anchors | Get-Member -MemberType NoteProperty).Count
    Write-Host "anchors.schema.json 锚点数: $count"
    if ($count -lt 80) { $errors += "锚点数过少(<80)，当前: $count" }
  } catch { $errors += "anchors.schema.json 解析失败: $_" }
}

# 版本检查（可选）
$gradleProps = Join-Path $Root "gradle/wrapper/gradle-wrapper.properties"
if (Test-Path $gradleProps) {
  $content = Get-Content $gradleProps -Raw
  if ($content -notmatch "gradle-8\.2\.1") { $errors += "Gradle wrapper 版本不是 8.2.1" }
}

# 结果
if ($errors.Count -eq 0) {
  Write-Host "NDJC Selfcheck ✅ 通过"
  exit 0
} else {
  Write-Host "NDJC Selfcheck ❌ 未通过："
  $errors | ForEach-Object { Write-Host " - $_" }
  exit 1
}
