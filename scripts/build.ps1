<#
.SYNOPSIS
  NDJC ??????????????/ CI ?????

.DESCRIPTION
  - ??????????????????requests/<timestamp>/ ??????
  - ?????????????????????????????
  - ??? APK??ebug/Release??
  - ????????dist/<timestamp> ????????

.NOTES
  ??????????????????
    //NDJC:PACKAGE_NAME
    //NDJC:APP_LABEL
    //NDJC:HOME_TITLE
    //NDJC:HOME_BUTTON
    //NDJC:FEATURE_SWITCH

  2025-09-02 ?????
  - ??? ANDROID SDK ??????????????? SDK????????? local.properties????????/opt/android-sdk
  - ?????? Build-Tools?????34.0.0????? 33.0.2?????? aapt/aapt2
  - ??? here-string??????????????????????PowerShell ??????
#>

[CmdletBinding()]
param(
  [ValidateSet("simple-template","core-template","form-template")]
  [string]$Template = "simple-template",

  [Parameter(Mandatory=$false)]
  [string]$Replay = "",

  [ValidateSet("dice","timer","todo")]
  [string]$Feature = "dice",
  [string]$AppTitle = "NDJC Demo",
  [string]$MainButton = "Get Started",
  [string]$PackageName = "com.ndjc.demo",

  [ValidateSet("Debug","Release")]
  [string]$BuildType = "Release",

  [string]$RepoRoot     = (Resolve-Path "$PSScriptRoot/..").Path,
  [string]$RequestsRoot = (Resolve-Path "$PSScriptRoot/../requests").Path,
  [string]$DistRoot     = (Resolve-Path "$PSScriptRoot/../dist").Path,

  [string]$KeystorePath,
  [string]$KeystorePass,
  [string]$KeyAlias = "ndjc",

  [switch]$CheckAnchors = $true
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Section($title) { Write-Host "`n=== $title ===" -ForegroundColor Cyan }
function Ensure-Dir($path)    { if (-not (Test-Path $path)) { New-Item -ItemType Directory -Force -Path $path | Out-Null } }

function Load-DotEnv($envFile) {
  if (Test-Path $envFile) {
    (Get-Content $envFile) | ForEach-Object {
      if ($_ -match "^\s*#") { return }
      if ($_ -match "^\s*$") { return }
      $kv = $_ -split "=",2
      if ($kv.Length -eq 2) { [Environment]::SetEnvironmentVariable($kv[0].Trim(), $kv[1].Trim(), "Process") }
    }
    Write-Host "Loaded .env from $envFile"
  }
}

function Replace-InFileIfMarkerExists([string]$file, [string]$marker, [string]$value, [string]$pattern = $null) {
  if (-not (Test-Path $file)) { return }
  $text = Get-Content -Raw -Path $file
  if ($text -match [regex]::Escape($marker)) {
    $newText = if ($pattern) { [regex]::Replace($text, $pattern, $value) } else { $text -replace [regex]::Escape($marker), $value }
    if ($newText -ne $text) {
      Set-Content -Path $file -Value $newText -NoNewline
      Write-Host "  ??$([IO.Path]::GetFileName($file)) ??$marker"
    }
  }
}

function Save-Json($obj, $path) { $obj | ConvertTo-Json -Depth 10 | Set-Content -Path $path -Encoding UTF8 }

# 1) ??????
Write-Section "???????????
$AppDir         = Join-Path $RepoRoot "app"
$AndroidProject = $AppDir
$TemplateDir    = Join-Path (Join-Path $RepoRoot "templates") $Template
if (-not (Test-Path $AndroidProject)) { throw "?????Android ????????AndroidProject" }
if (-not (Test-Path $TemplateDir))   { throw "????????????$TemplateDir" }
Ensure-Dir $RequestsRoot
Ensure-Dir $DistRoot

# ??? .env??????
$dotenv = Join-Path $RepoRoot ".env"
Load-DotEnv $dotenv

# 2) ???/??????
Write-Section "???????????eplay ??????"
$ts = (Get-Date).ToString("yyyy-MM-dd_HHmmss")
if (-not [string]::IsNullOrWhiteSpace($Replay)) {
  $RequestDir = (Resolve-Path $Replay).Path
  if (-not (Test-Path $RequestDir)) { throw "-Replay ????????????$RequestDir" }
  Write-Host "???????????RequestDir"
} else {
  $RequestDir = Join-Path $RequestsRoot $ts
  Ensure-Dir $RequestDir

  $orchestrator = [ordered]@{
    promptOriginal   = "title=$AppTitle; button=$MainButton; feature=$Feature"
    selectedTemplate = $Template
    anchorsSelected  = @("PACKAGE_NAME","APP_LABEL","HOME_TITLE","HOME_BUTTON","FEATURE_SWITCH")
    variables        = @{ packageName = $PackageName; appTitle = $AppTitle; mainButton = $MainButton; feature = $Feature }
    createdAt        = (Get-Date).ToString("s")
  }
  $apiResp = [ordered]@{ title = $AppTitle; mainButton = $MainButton; feature = $Feature; package = $PackageName }
  $anchors = [ordered]@{ required = @("//NDJC:PACKAGE_NAME","//NDJC:APP_LABEL","//NDJC:HOME_TITLE","//NDJC:HOME_BUTTON","//NDJC:FEATURE_SWITCH") }

  Save-Json $orchestrator (Join-Path $RequestDir "orchestrator.json")
  Save-Json $apiResp      (Join-Path $RequestDir "api_response.json")
  Save-Json $anchors      (Join-Path $RequestDir "anchors.json")

  # ??index.md??? here-string??
  $mdLines = @(
    "# NDJC Request ($ts)",
    "- Template: $Template",
    "- Feature : $Feature",
    "- Title   : $AppTitle",
    "- Button  : $MainButton",
    "- Package : $PackageName"
  )
  $md = $mdLines -join "`r`n"
  Set-Content -Path (Join-Path $RequestDir "index.md") -Value $md -Encoding UTF8

  Write-Host "???????????RequestDir"
}

# 3) ????????????
if ($CheckAnchors) {
  Write-Section "?????????????????????"
  $anchorsCfg = Get-Content -Raw (Join-Path $RequestDir "anchors.json") | ConvertFrom-Json
  $markers    = $anchorsCfg.required
  $candidateFiles = @(
    (Join-Path $TemplateDir "app/src/main/AndroidManifest.xml"),
    (Join-Path $TemplateDir "app/src/main/res/values/strings.xml"),
    (Join-Path $TemplateDir "app/src/main/java/MainActivity.java"),
    (Join-Path $TemplateDir "app/src/main/java/com/example/MainActivity.java"),
    (Join-Path $TemplateDir "app/src/main/kotlin/MainActivity.kt")
  ) | Where-Object { Test-Path $_ }

  foreach ($m in $markers) {
    $hit = $false
    foreach ($f in $candidateFiles) {
      $txt = Get-Content -Raw -Path $f
      if ($txt -match [regex]::Escape($m)) { $hit = $true; break }
    }
    if (-not $hit) { Write-Warning "??????????????????$m?????????????????????" }
    else          { Write-Host "  ??$m" }
  }
}

# 4) ????????app/
Write-Section "?????????????????
Copy-Item -Path (Join-Path $TemplateDir "app/*") -Destination $AppDir -Recurse -Force

# 5) ??????
Write-Section "??????"
$api     = Get-Content -Raw (Join-Path $RequestDir "api_response.json") | ConvertFrom-Json
$pkg     = $api.package
$title   = $api.title
$button  = $api.mainButton
$feature = $api.feature

# 5.1 assets/ndjc_info.json
$assetsDir = Join-Path $AppDir "src/main/assets"
Ensure-Dir $assetsDir
Save-Json @{ package = $pkg; title = $title; button = $button; feature = $feature; builtAt = (Get-Date).ToString("s") } (Join-Path $assetsDir "ndjc_info.json")

# 5.2 ????????
$manifest = Join-Path $AppDir "src/main/AndroidManifest.xml"
$strings  = Join-Path $AppDir "src/main/res/values/strings.xml"
$mainJava = Join-Path $AppDir "src/main/java/MainActivity.java"
$mainAlt1 = Join-Path $AppDir "src/main/java/com/example/MainActivity.java"
$mainKt   = Join-Path $AppDir "src/main/kotlin/MainActivity.kt"

Replace-InFileIfMarkerExists $manifest "//NDJC:PACKAGE_NAME" $pkg
Replace-InFileIfMarkerExists $manifest "//NDJC:APP_LABEL"    $title
Replace-InFileIfMarkerExists $strings  "//NDJC:HOME_TITLE"   $title
Replace-InFileIfMarkerExists $strings  "//NDJC:HOME_BUTTON"  $button

# FEATURE_SWITCH ????????here-string?????????
$featureCodeJava = (
  "    // injected by NDJC" + "`r`n" +
  "    String feature = `"$feature`";" + "`r`n" +
  "    // TODO: ??? feature ?????????????ice/timer/todo??
)
$featureCodeKt = (
  "    // injected by NDJC" + "`r`n" +
  "    val feature = `"$feature`"" + "`r`n" +
  "    // TODO: ??? feature ?????????????ice/timer/todo??
)
Replace-InFileIfMarkerExists $mainJava "//NDJC:FEATURE_SWITCH" $featureCodeJava
Replace-InFileIfMarkerExists $mainAlt1 "//NDJC:FEATURE_SWITCH" $featureCodeJava
Replace-InFileIfMarkerExists $mainKt   "//NDJC:FEATURE_SWITCH" $featureCodeKt

# ======================== SDK ???????????? /opt/android-sdk??========================
function Resolve-AndroidSdkRoot {
  param([string]$Requested)
  $candidates = @()
  if ($Requested) { $candidates += $Requested }
  $candidates += @(
    $env:ANDROID_SDK_ROOT,
    $env:ANDROID_HOME,
    "E:\NDJC\android-sdk",
    "$env:LOCALAPPDATA\Android\Sdk",
    "C:\Android\sdk",
    "/opt/android-sdk"   # ??Linux CI ???
  ) | Where-Object { $_ -and (Test-Path $_) }
  if ($candidates.Count -gt 0) { return $candidates[0] }
  throw "?????ANDROID SDK???????????? ANDROID_SDK_ROOT??
}

Write-Section "??? ANDROID SDK ????????local.properties"
$SDK = Resolve-AndroidSdkRoot
$env:ANDROID_SDK_ROOT = $SDK
$env:ANDROID_HOME     = $SDK

# ?????? Build-Tools?????34.0.0?????33.0.2??
$btCandidates = @("34.0.0","33.0.2")
$BT = $null
foreach ($v in $btCandidates) {
  if (Test-Path (Join-Path $SDK "build-tools\$v\aapt.exe")) { $BT = $v; break }
  if (Test-Path (Join-Path $SDK "build-tools\$v\aapt"))     { $BT = $v; break }
}
if (-not $BT) { throw "???????????Build-Tools??4.0.0/33.0.2???????sdkmanager ?????????? }

# PATH ?????? aapt/zipalign
$env:PATH = (Join-Path $SDK "build-tools\$BT") + ";" + (Join-Path $SDK "platform-tools") + ";$env:PATH"
Write-Host "Using ANDROID_SDK_ROOT = $SDK"
Write-Host "Using Build-Tools     = $BT"

# ????????settings.gradle ??settings.gradle.kts??
$settings = Get-ChildItem -Path $RepoRoot -Recurse -File -Include "settings.gradle","settings.gradle.kts" | Select-Object -First 1
if (-not $settings) { throw "?????settings.gradle??????????????RepoRoot" }
$ProjectRoot = $settings.Directory.FullName

# ??? local.properties????????/opt/android-sdk??
$sdkFile = Join-Path $ProjectRoot "local.properties"
$sdkLine = "sdk.dir=" + ($SDK -replace '\\','/')
Set-Content -Path $sdkFile -Value $sdkLine -Encoding ASCII
Write-Host  "local.properties ??? -> $sdkFile"

# ??? aapt/aapt2
$aapt  = @((Join-Path $SDK "build-tools\$BT\aapt.exe"),  (Join-Path $SDK "build-tools\$BT\aapt"))  | Where-Object { Test-Path $_ } | Select-Object -First 1
$aapt2 = @((Join-Path $SDK "build-tools\$BT\aapt2.exe"), (Join-Path $SDK "build-tools\$BT\aapt2")) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $aapt -or -not $aapt2) { throw "Build-Tools $BT ????????? aapt/aapt2??SDK\build-tools\$BT?? }

# ??? Daemon??????????????
$wrapper = if (Test-Path (Join-Path $ProjectRoot "gradlew.bat")) {
  (Join-Path $ProjectRoot "gradlew.bat")
} elseif (Test-Path (Join-Path $ProjectRoot "gradlew")) {
  (Join-Path $ProjectRoot "gradlew")
} else {
  throw "?????gradlew/gradlew.bat ??$ProjectRoot"
}
& $wrapper --stop | Out-Null
# ====================== / SDK ?????======================

# 6) ???
Write-Section "Gradle ?????BuildType??
try { & icacls $wrapper /grant "*S-1-1-0:(RX)" | Out-Null } catch {}

Push-Location $RepoRoot
try {
  if ($BuildType -eq "Release") {
    & $wrapper clean assembleRelease -PndjcBuildTs="$ts" --no-daemon
  } else {
    & $wrapper clean assembleDebug   -PndjcBuildTs="$ts" --no-daemon
  }
} finally { Pop-Location }

# 7) ??????
Write-Section "???????????
$apkPattern = if ($BuildType -eq "Release") { "app/build/outputs/apk/release/*.apk" } else { "app/build/outputs/apk/debug/*.apk" }
$apkFiles   = Get-ChildItem -Path (Join-Path $RepoRoot $apkPattern) -ErrorAction SilentlyContinue
if (-not $apkFiles) { throw "?????APK ???????????????????? Gradle ????? }

$distDir = Join-Path $DistRoot $ts
Ensure-Dir $distDir

# ??? APK
$apkOut = Join-Path $distDir "apk"
Ensure-Dir $apkOut
$apkFiles | ForEach-Object { Copy-Item $_.FullName -Destination $apkOut -Force }

# ???????????
Copy-Item -Recurse -Force $RequestDir $distDir
$summary = Join-Path $distDir "ndjc_summary.md"
$summaryLines = @(
  "# NDJC Build Summary",
  "- Time     : $ts",
  "- Template : $Template",
  "- Feature  : $feature",
  "- Title    : $title",
  "- Button   : $button",
  "- Package  : $pkg",
  "- Build    : $BuildType",
  "",
  "## Files",
  "- APK(s)   : " + (($apkFiles | ForEach-Object { $_.Name }) -join ", "),
  "- Request  : " + (Split-Path -Leaf $RequestDir)
)
Set-Content -Path $summary -Encoding UTF8 -Value ($summaryLines -join "`r`n")

Write-Host "`n?????? ??
Write-Host "????????distDir"
$apkFiles | ForEach-Object { Write-Host "  ??$($_.FullName)" }

