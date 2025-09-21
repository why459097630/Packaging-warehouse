<# NDJC SelfCheck (dual-mode)
 - 新模式：检测 templates/<id>/ 下“五件套”并校验 JSON 语法；YAML 做关键字段存在性检查
 - 旧模式：跳过200+锚点全面扫描（仅兼容提示），不再阻断
 - 输出：
    anchors/anchors_status.csv
    anchors/missing_text.txt   （留空，用于兼容）
    anchors/missing_block.txt  （留空，用于兼容）
    anchors/scan-report.json
 - 退出码：有缺失/语法错误 -> 非0；否则0
#>

Param(
  [string]$TemplatesRoot = "templates",
  [string]$OutDir = "anchors"
)

$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

function Test-JsonFile($path) {
  if (-not (Test-Path $path)) { return @{ ok=$false; error="file not found" } }
  try {
    $raw = Get-Content -Raw -LiteralPath $path -Encoding UTF8
    $null = $raw | ConvertFrom-Json
    return @{ ok=$true; error=$null }
  } catch {
    return @{ ok=$false; error=$_.Exception.Message }
  }
}

function Test-YamlLite($path, $requiredKeys) {
  if (-not (Test-Path $path)) { return @{ ok=$false; error="file not found" } }
  $txt = Get-Content -Raw -LiteralPath $path -Encoding UTF8
  foreach ($k in $requiredKeys) {
    if ($txt -notmatch "(?m)^\s*$k\s*:" ) {
      return @{ ok=$false; error="missing key '$k'" }
    }
  }
  return @{ ok=$true; error=$null }
}

$report = @()
$templates = Get-ChildItem -Path $TemplatesRoot -Directory -ErrorAction SilentlyContinue

$foundNewMode = $false
foreach ($t in $templates) {
  $tmplId = $t.Name
  $tmplDir = $t.FullName
  $hasTemplateYaml = Test-Path (Join-Path $tmplDir "template.yaml")

  if ($hasTemplateYaml) {
    $foundNewMode = $true
    Write-Host ("[NEW] Checking template: {0}" -f $tmplId) -ForegroundColor Cyan

    $files = @{
      "template.yaml" = (Join-Path $tmplDir "template.yaml")
      "anchors.schema.json" = (Join-Path $tmplDir "anchors.schema.json")
      "slots.map.json" = (Join-Path $tmplDir "slots.map.json")
      "deps/manifest.json" = (Join-Path $tmplDir "deps/manifest.json")
      "presets/default.preset.json" = (Join-Path $tmplDir "presets\default.preset.json")
    }

    $missing = @()
    foreach ($kv in $files.GetEnumerator()) {
      if (-not (Test-Path $kv.Value)) { $missing += $kv.Key }
    }

    $yamlRes = Test-YamlLite $files."template.yaml" @("id","version","engine","entry")
    $schemaRes = Test-JsonFile $files."anchors.schema.json"
    $slotsRes  = Test-JsonFile $files."slots.map.json"
    $depsRes   = Test-JsonFile $files."deps/manifest.json"
    $presetRes = Test-JsonFile $files."presets/default.preset.json"

    $ok = ($missing.Count -eq 0) -and $yamlRes.ok -and $schemaRes.ok -and $slotsRes.ok -and $depsRes.ok -and $presetRes.ok

    $report += [pscustomobject]@{
      template = $tmplId
      mode     = "new"
      ok       = $ok
      missing  = ($missing -join ",")
      template_yaml = $yamlRes.error
      anchors_schema = $schemaRes.error
      slots_map  = $slotsRes.error
      deps_manifest = $depsRes.error
      preset_default = $presetRes.error
    }
  }
}

if (-not $foundNewMode) {
  # 旧仓库或未引入新模板时的兼容输出（不阻断）
  Write-Host "[LEGACY] No template.yaml found under 'templates/'. Skipping legacy 200+ anchors scan." -ForegroundColor Yellow
  $report += [pscustomobject]@{ template="__legacy__"; mode="legacy"; ok=$true; missing=""; template_yaml=$null; anchors_schema=$null; slots_map=$null; deps_manifest=$null; preset_default=$null }
}

# 写 CSV
$csvPath = Join-Path $OutDir "anchors_status.csv"
$report | Export-Csv -NoTypeInformation -Encoding UTF8 -Path $csvPath

# 兼容输出（留空文件）
Set-Content -LiteralPath (Join-Path $OutDir "missing_text.txt") -Value "" -Encoding UTF8
Set-Content -LiteralPath (Join-Path $OutDir "missing_block.txt") -Value "" -Encoding UTF8

# JSON 报告
$scanJson = $report | ConvertTo-Json -Depth 5
Set-Content -LiteralPath (Join-Path $OutDir "scan-report.json") -Value $scanJson -Encoding UTF8

# 退出码
if ($report.Where({ -not $_.ok }).Count -gt 0) {
  Write-Error "SelfCheck failed. See $csvPath and anchors/scan-report.json"
} else {
  Write-Host "SelfCheck passed." -ForegroundColor Green
}
