<#
NDJC 自查脚本（PowerShell 版）
- 以 anchors/expected_text.txt & anchors/expected_block.txt 为准
- 扫描 templates/ 下所有模板文件，统计锚点是否已落位
- 生成报告：anchors/scan-report.json、anchors/anchors_status.csv、
  anchors/missing_text.txt、anchors/missing_block.txt
- 缺失则返回非零退出码（用于 CI）

修复/增强：
- 修正排除目录匹配：不会再把 “build.gradle” 误判为 build 目录而排除
- 兼容 CRLF/LF/CR 三种换行读取 expected_* 文件
- 精确大小写匹配（-CaseSensitive）
#>

param(
  [string]$RepoRoot = (Resolve-Path ".").Path,
  [string]$TemplatesDir = "templates",
  [string]$ExpectedDir = "anchors",
  [switch]$FailOnMissing = $true
)

$ErrorActionPreference = "Stop"

function Read-Expected([string]$path) {
  if (!(Test-Path $path)) { throw "Expected file not found: $path" }
  Get-Content -Raw $path |
    ForEach-Object { $_ -split "(`r`n|`n|`r)+" } |
    ForEach-Object { $_.Trim() } |
    Where-Object  { $_ -ne "" -and -not ($_.StartsWith("#")) } |
    Sort-Object -Unique
}

# ---- 路径准备（规范化） ----
$repoPath          = (Resolve-Path $RepoRoot).Path
$expectedTextPath  = Join-Path $repoPath (Join-Path $ExpectedDir "expected_text.txt")
$expectedBlockPath = Join-Path $repoPath (Join-Path $ExpectedDir "expected_block.txt")
$outDir            = Join-Path $repoPath $ExpectedDir
$scanRoot          = Join-Path $repoPath $TemplatesDir

if (!(Test-Path $scanRoot)) { throw "Templates dir not found: $scanRoot" }
if (!(Test-Path $outDir))   { New-Item -ItemType Directory -Path $outDir | Out-Null }

# ---- 读取期望清单 ----
$expectedText  = Read-Expected $expectedTextPath
$expectedBlock = Read-Expected $expectedBlockPath

# 需扫描的文件后缀（可按需增减）
$exts = @(
  ".kt", ".kts", ".java", ".xml", ".gradle", ".pro", ".properties",
  ".txt", ".md", ".json", ".yml", ".yaml"
)

# 排除目录（务必写成“目录边界”正则，避免误杀文件名）
# 说明：[\\/] 同时兼容 Windows 与 *nix 分隔符
$excludeDirs = @(
  "[\\/]\.git[\\/]",            # .git/
  "[\\/]build[\\/]",            # build/
  "[\\/]\.gradle[\\/]",         # .gradle/
  "[\\/]\.idea[\\/]",           # .idea/
  "[\\/]node_modules[\\/]",     # node_modules/
  "[\\/]dist[\\/]",             # dist/
  "[\\/]outputs[\\/]"           # outputs/
)

function Test-Excluded([string]$fullPath, [string[]]$patterns) {
  foreach ($p in $patterns) { if ($fullPath -match $p) { return $true } }
  return $false
}

# ---- 收集文件（正确的排除逻辑） ----
$files = Get-ChildItem -Path $scanRoot -Recurse -File | Where-Object {
  ($exts -contains $_.Extension.ToLower()) -and -not (Test-Excluded $_.FullName $excludeDirs)
}

# ---- 扫描函数 ----
function Scan-Anchors {
  param(
    [string[]]$Anchors,
    [string]$Kind  # "TEXT" | "BLOCK"
  )
  $results = @()
  foreach ($a in $Anchors) {
    # 精确字符串匹配（大小写敏感）
    $pattern = [Regex]::Escape($a)
    $matches = @()
    foreach ($f in $files) {
      $hit = Select-String -Path $f.FullName -Pattern $pattern -SimpleMatch -CaseSensitive
      if ($hit) {
        $matches += [PSCustomObject]@{
          file  = $f.FullName.Replace($repoPath, "").TrimStart("\","/")
          lines = ($hit | Select-Object -ExpandProperty LineNumber)
        }
      }
    }
    $results += [PSCustomObject]@{
      anchor  = $a
      kind    = $Kind
      found   = ($matches.Count -gt 0)
      count   = $matches.Count            # 命中文件的数量（每文件多行已汇总到 lines）
      matches = $matches
    }
  }
  return $results
}

Write-Host "Scanning $($files.Count) files under $scanRoot ..." -ForegroundColor Cyan

$textRes  = Scan-Anchors -Anchors $expectedText  -Kind "TEXT"
$blockRes = Scan-Anchors -Anchors $expectedBlock -Kind "BLOCK"

# ---- 汇总 ----
$all = $textRes + $blockRes
$missingText  = $textRes  | Where-Object { -not $_.found } | Select-Object -ExpandProperty anchor
$missingBlock = $blockRes | Where-Object { -not $_.found } | Select-Object -ExpandProperty anchor

# 输出 CSV
$csvPath = Join-Path $outDir "anchors_status.csv"
$all | Select-Object kind,anchor,found,count |
  Export-Csv -NoTypeInformation -Encoding UTF8 -Path $csvPath

# 输出缺失清单
$missTextPath  = Join-Path $outDir "missing_text.txt"
$missBlockPath = Join-Path $outDir "missing_block.txt"
$missingText  | Out-File -Encoding UTF8 $missTextPath
$missingBlock | Out-File -Encoding UTF8 $missBlockPath

# 输出 JSON（详细命中）
$report = [PSCustomObject]@{
  scanned_files   = $files.Count
  expected_text   = $expectedText.Count
  expected_block  = $expectedBlock.Count
  present_text    = ($textRes  | Where-Object {$_.found}).Count
  present_block   = ($blockRes | Where-Object {$_.found}).Count
  missing_text    = $missingText
  missing_block   = $missingBlock
  details         = $all
  generated_at    = (Get-Date).ToString("yyyy-MM-dd HH:mm:ssK")
}
$reportPath = Join-Path $outDir "scan-report.json"
$report | ConvertTo-Json -Depth 6 | Out-File -Encoding UTF8 $reportPath

# 控制台摘要
Write-Host ""
Write-Host "==== NDJC Anchor Self-Check ====" -ForegroundColor Green
Write-Host ("Text:  {0}/{1} present"  -f $report.present_text,  $report.expected_text)
Write-Host ("Block: {0}/{1} present"  -f $report.present_block, $report.expected_block)
Write-Host ("Missing text:  {0}" -f $missingText.Count)
Write-Host ("Missing block: {0}" -f $missingBlock.Count)
Write-Host "Reports:"
Write-Host (" - " + $csvPath)
Write-Host (" - " + $missTextPath)
Write-Host (" - " + $missBlockPath)
Write-Host (" - " + $reportPath)
Write-Host "===============================" -ForegroundColor Green

if ($FailOnMissing -and (($missingText.Count + $missingBlock.Count) -gt 0)) {
  Write-Error "Missing anchors detected. See anchors/missing_*.txt"
  exit 2
}
