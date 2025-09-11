# tools\reserve-anchors.ps1  (ASCII only, fixed -replace usage)

param([string[]]$Templates=@('simple-template','core-template','form-template'))

$ErrorActionPreference = 'Stop'

$repo    = Split-Path -Parent $PSScriptRoot
$tplRoot = Join-Path $repo 'templates'

function Ensure-FileDir($p) {
  $d = Split-Path -Parent $p
  if (-not (Test-Path $d)) { New-Item -ItemType Directory -Force -Path $d | Out-Null }
}
function ReadText($p)  { if (Test-Path $p) { Get-Content -Raw -Encoding UTF8 $p } else { '' } }
function WriteText($p,$t) { Ensure-FileDir $p; Set-Content -Encoding UTF8 -Path $p -Value $t }

foreach($t in $Templates) {
  Write-Host ("Processing template: {0}" -f $t) -ForegroundColor Cyan

  $app = Join-Path $tplRoot "$t\app"
  if (-not (Test-Path $app)) { Write-Host ("Skip {0} (no app/)" -f $t) -ForegroundColor Yellow; continue }

  $man        = Join-Path $app 'src\main\AndroidManifest.xml'
  $strings    = Join-Path $app 'src\main\res\values\strings.xml'
  $colors     = Join-Path $app 'src\main\res\values\colors.xml'
  $themes     = Join-Path $app 'src\main\res\values\themes.xml'
  $gradle     = Join-Path $app 'build.gradle'
  $gradleKts  = Join-Path $app 'build.gradle.kts'

  # -------- AndroidManifest.xml --------
  $m = ReadText $man
  if ([string]::IsNullOrWhiteSpace($m)) {
    $m = @'
<?xml version="1.0" encoding="utf-8"?>
<manifest package="//NDJC:PACKAGE_NAME" xmlns:android="http://schemas.android.com/apk/res/android">
  <!-- //NDJC:PERMISSIONS -->
  <application android:label="//NDJC:APP_LABEL">
    <activity android:name=".MainActivity">
      <!-- //NDJC:INTENT_FILTERS -->
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>
  </application>
</manifest>
'@
  } else {
    # package
    if ($m -notmatch 'NDJC:PACKAGE_NAME') {
      $m = [regex]::Replace($m,'(<manifest[^>]*\spackage\s*=\s*")[^"]*(")','$1//NDJC:PACKAGE_NAME$2')
    }
    # app label
    if ($m -notmatch 'NDJC:APP_LABEL') {
      $m = [regex]::Replace($m,'(<application[^>]*)(>)',{
        param($m0)
        if ($m0.Value -match 'android:label=') { $m0.Value }
        else { $m0.Groups[1].Value + ' android:label="//NDJC:APP_LABEL"' + $m0.Groups[2].Value }
      })
    }
    # insert PERMISSIONS before </manifest>  (FIXED: one replacement string)
    if ($m -notmatch 'NDJC:PERMISSIONS') {
      $replacement = '  <!-- //NDJC:PERMISSIONS -->' + "`r`n" + '</manifest>'
      $m = $m -replace '</manifest>\s*$', $replacement
    }
    # insert INTENT_FILTERS right after opening <activity ...>
    if ($m -notmatch 'NDJC:INTENT_FILTERS') {
      $m = $m -replace '(<activity[^>]*>)', ('$1' + "`r`n" + '      <!-- //NDJC:INTENT_FILTERS -->')
    }
  }
  WriteText $man $m

  # -------- strings.xml --------
  $s = ReadText $strings
  if ([string]::IsNullOrWhiteSpace($s)) {
    $s = @'
<resources>
  <string name="app_name">//NDJC:APP_LABEL</string>
  <string name="ndjc_home_title">//NDJC:HOME_TITLE</string>
  <string name="ndjc_action_primary_text">//NDJC:HOME_BUTTON</string>
  <!-- //NDJC:MORE_STRINGS -->
</resources>
'@
  } else {
    foreach($ins in @(
      '  <string name="app_name">//NDJC:APP_LABEL</string>',
      '  <string name="ndjc_home_title">//NDJC:HOME_TITLE</string>',
      '  <string name="ndjc_action_primary_text">//NDJC:HOME_BUTTON</string>',
      '  <!-- //NDJC:MORE_STRINGS -->'
    )) {
      $marker = ($ins -replace '.*//(NDJC:[A-Z0-9_]+).*','$1')
      if ($s -notmatch [regex]::Escape($marker)) {
        $s = $s -replace '</resources>', ($ins + "`r`n</resources>")
      }
    }
  }
  WriteText $strings $s

  # -------- colors.xml --------
  $c = ReadText $colors
  if ([string]::IsNullOrWhiteSpace($c)) {
    $c = @'
<resources>
  <color name="ndjc_color_primary">#6750A4</color> <!-- //NDJC:COLOR_PRIMARY -->
  <color name="ndjc_color_on_primary">#FFFFFF</color> <!-- //NDJC:COLOR_ON_PRIMARY -->
  <color name="ndjc_color_secondary">#625B71</color> <!-- //NDJC:COLOR_SECONDARY -->
</resources>
'@
  } else {
    foreach($ins in @(
      '  <color name="ndjc_color_primary">#6750A4</color> <!-- //NDJC:COLOR_PRIMARY -->',
      '  <color name="ndjc_color_on_primary">#FFFFFF</color> <!-- //NDJC:COLOR_ON_PRIMARY -->',
      '  <color name="ndjc_color_secondary">#625B71</color> <!-- //NDJC:COLOR_SECONDARY -->'
    )) {
      $marker = ($ins -replace '.*//(NDJC:[A-Z0-9_]+).*','$1')
      if ($c -notmatch [regex]::Escape($marker)) {
        $c = $c -replace '</resources>', ($ins + "`r`n</resources>")
      }
    }
  }
  WriteText $colors $c

  # -------- themes.xml --------
  $th = ReadText $themes
  if ([string]::IsNullOrWhiteSpace($th)) {
    $th = @'
<resources>
  <style name="Theme.NDJC" parent="Theme.Material3.DayNight.NoActionBar">
    <!-- //NDJC:THEME_OVERRIDES -->
  </style>
</resources>
'@
  } else {
    if ($th -notmatch 'NDJC:THEME_OVERRIDES') {
      $th = $th -replace '(</style>)', ('    <!-- //NDJC:THEME_OVERRIDES -->' + "`r`n" + '$1')
    }
  }
  WriteText $themes $th

  # -------- MainActivity.kt anchors --------
  $main = Get-ChildItem (Join-Path $app 'src\main\java') -Recurse -Include MainActivity.kt -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($main) {
    $k = ReadText $main.FullName
    if ($k -notmatch 'NDJC:IMPORTS') {
      $k = $k -replace '^(package[^\r\n]+)', ('$1' + "`r`n" + '//NDJC:IMPORTS')
    }
    if ($k -notmatch 'NDJC:FEATURE_SWITCH') {
      if ($k -match 'setContent\s*\{') {
        $k = $k -replace '(setContent\s*\{)', ('$1' + "`r`n" + '            //NDJC:FEATURE_SWITCH')
      } else {
        $k = $k -replace 'class\s+MainActivity[^{]*\{', { param($m0) $m0.Value + "`r`n    //NDJC:FEATURE_SWITCH`r`n" }
      }
    }
    if ($k -notmatch 'NDJC:HOME_TITLE_BIND') {
      $k = $k -replace '(setContent\s*\{)', ('$1' + "`r`n" + '            //NDJC:HOME_TITLE_BIND')
    }
    if ($k -notmatch 'NDJC:HOME_BUTTON_ACTION') {
      $k = $k -replace '(onClick\s*=\s*\{?)', ('$1 //NDJC:HOME_BUTTON_ACTION ')
    }
    WriteText $main.FullName $k
  }

  # -------- build.gradle / .kts anchors --------
  $gFile = if (Test-Path $gradle) { $gradle } elseif (Test-Path $gradleKts) { $gradleKts } else { $null }
  if ($gFile) {
    $g = ReadText $gFile
    if ($g -match 'android\s*\{') {
      foreach($mk in 'COMPILE_SDK','MIN_SDK','TARGET_SDK','SIGNING_CONFIG') {
        if ($g -notmatch "NDJC:$mk") { $g = $g -replace '(android\s*\{)', ('$1' + "`r`n    //NDJC:$mk") }
      }
      foreach($mk in 'VERSION_CODE','VERSION_NAME') {
        if ($g -notmatch "NDJC:$mk") { $g = $g -replace '(defaultConfig\s*\{)', ('$1' + "`r`n        //NDJC:$mk") }
      }
    }
    if ($g -match 'dependencies\s*\{' -and $g -notmatch 'NDJC:DEPENDENCIES_EXTRA') {
      $g = $g -replace '(dependencies\s*\{)', ('$1' + "`r`n    //NDJC:DEPENDENCIES_EXTRA")
    }
    if ($g -match 'plugins\s*\{' -and $g -notmatch 'NDJC:PLUGINS_EXTRA') {
      $g = $g -replace '(plugins\s*\{)', ('$1' + "`r`n    //NDJC:PLUGINS_EXTRA")
    }
    WriteText $gFile $g
  }

  Write-Host ("Anchors reserved for {0}" -f $t) -ForegroundColor Green
}

Write-Host 'All done.' -ForegroundColor Cyan
