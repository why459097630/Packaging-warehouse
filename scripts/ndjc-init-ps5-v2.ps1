# Generate a PS5.1-safe init script that builds JSON via hashtables (no here-strings)
path = "/mnt/data/ndjc-init-ps5-v2.ps1"
script = r'''
<#
NDJC One-Click Init (PS5.1-safe, JSON via ConvertTo-Json)
Usage:
  cd E:\NDJC\Packaging-warehouse
  powershell -File .\scripts\ndjc-init-ps5-v2.ps1 -OutDir ".\contracts" -PresetId "preset.A"
#>
param(
  [string]$OutDir = "contracts",
  [string]$PresetId = "preset.A"
)

$ErrorActionPreference = "Stop"
Write-Host "=== NDJC · One-Click Init (PS5.1 · v2) ===" -ForegroundColor Cyan

# --- Create directories
$dirs = @(
  $OutDir,
  (Join-Path $OutDir "modules"),
  (Join-Path $OutDir "modules\feature"),
  (Join-Path $OutDir "modules\cap"),
  (Join-Path $OutDir "presets"),
  (Join-Path $OutDir "UIPack"),
  "tools"
)
foreach ($d in $dirs) { New-Item -ItemType Directory -Force -Path $d | Out-Null }

function Write-Json($obj, $path) {
  $json = $obj | ConvertTo-Json -Depth 10
  $dir = Split-Path -Parent $path
  if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
  $json | Set-Content -Encoding UTF8 $path
  Write-Host "Wrote: $path"
}

# --- Template.manifest.json
$template = @{
  id = "Core-Template"
  version = "1.0.0"
  slots = @(
    @{ slotId = "home_main"; type = "screen"; multiplicity = "single" },
    @{ slotId = "tab_1";     type = "screen"; multiplicity = "single" },
    @{ slotId = "tab_2";     type = "screen"; multiplicity = "single" }
  )
  navigation = @{ style = "mixed"; entryPoints = @("tab_1","tab_2") }
  requiredUI = @("IAppTheme","ITopBar","IButtonPrimary","ICardListItem","IListScreenScaffold")
  capabilityHooks = @(
    @{ hookId = "onAppStart"; phase = "init";     orderable = $true },
    @{ hookId = "registerActivityResult"; phase = "postInit"; orderable = $true }
  )
  androidBase = @{ minSdk = 24; targetSdk = 34; manifestPlaceholders = @() }
}
Write-Json $template (Join-Path $OutDir "Template.manifest.json")

# --- UIPack.manifest.json
$uiPack = @{
  id = "ui.baseline.m3"
  version = "1.0.0"
  providesUI = @("IAppTheme","ITopBar","IButtonPrimary","ICardListItem","IListScreenScaffold")
  theme = @{ modes = @("light","dark","auto"); brandColor = "indigo" }
  resources = @{
    colors=@("ndjc_colors"); typography=@("ndjc_type"); shapes=@("ndjc_shapes")
    elevations=@(); motionSpecs=@()
  }
  constraints = @{ supportsNotch=$true; minContrastRatio=4.5; rtlSupport=$true }
}
Write-Json $uiPack (Join-Path $OutDir "UIPack\UIPack.manifest.json")

# --- Capability modules
$capAuth = @{
  type="capability"; id="cap.auth"; version="1.0.0"
  exposedAPIs=@("AuthService.signIn()","AuthService.currentUser()")
  permissions=@()
  gradleDeps=@("com.example:auth-core:1.0.0")
  initHooks=@(@{ hookId="onAppStart"; order=10; guard="always" })
  manifestAdditions=@()
  configKeys=@("AUTH_CLIENT_ID","AUTH_ENDPOINT")
}
Write-Json $capAuth (Join-Path $OutDir "modules\cap\cap.auth.manifest.json")

$capUpload = @{
  type="capability"; id="cap.upload"; version="1.0.0"
  exposedAPIs=@("UploadService.pickAndUpload(uri)")
  permissions=@("android.permission.CAMERA","android.permission.READ_MEDIA_IMAGES")
  gradleDeps=@("com.example:upload-kit:1.2.3")
  initHooks=@(@{ hookId="registerActivityResult"; order=20; guard="always" })
  manifestAdditions=@("<uses-feature android:name=""android.hardware.camera.any"" />")
  configKeys=@("UPLOAD_ENDPOINT")
}
Write-Json $capUpload (Join-Path $OutDir "modules\cap\cap.upload.manifest.json")

# --- Feature modules
$featureFeed = @{
  type="feature"; id="feature.feed"; version="1.0.0"
  screens=@(@{
    screenId="FeedScreen"; slotTarget="home_main"
    entrySpec=@{ entryType="tab"; label="Feed"; icon="ic_feed"; order=1 }
    routing="/feed"
  })
  uiNeeds=@("IListScreenScaffold","ICardListItem","IButtonPrimary")
  capabilityNeeds=@("cap.auth")
}
Write-Json $featureFeed (Join-Path $OutDir "modules\feature\feature.feed.manifest.json")

$featureProfile = @{
  type="feature"; id="feature.profile"; version="1.0.0"
  screens=@(@{
    screenId="ProfileScreen"; slotTarget="tab_2"
    entrySpec=@{ entryType="tab"; label="Profile"; icon="ic_profile"; order=2 }
    routing="/profile"
  })
  uiNeeds=@("IListScreenScaffold","ICardListItem","IButtonPrimary")
  capabilityNeeds=@("cap.auth")
}
Write-Json $featureProfile (Join-Path $OutDir "modules\feature\feature.profile.manifest.json")

# --- Preset & AssemblyPlan.sample.json
$preset = @{
  id = $PresetId
  template = "Core-Template"
  uiPack = "ui.baseline.m3"
  modules = @("feature.feed","feature.profile","cap.auth")
}
Write-Json $preset (Join-Path $OutDir ("presets\{0}.json" -f $PresetId))

$plan = @{
  planId = "local-sample"
  template = @{ id="Core-Template"; version="1.0.0" }
  uiPack = @{ id="ui.baseline.m3"; version="1.0.0" }
  features = @(@{ id="feature.feed"; version="1.0.0" }, @{ id="feature.profile"; version="1.0.0" })
  capabilities = @(@{ id="cap.auth"; version="1.0.0" }, @{ id="cap.upload"; version="1.0.0" })
  policy = @{ failOnMissingUI=$true; failOnSlotMismatch=$true; depConflictStrategy="highestWins" }
}
Write-Json $plan (Join-Path $OutDir "AssemblyPlan.sample.json")

# --- Precheck (same checks, PS5.1-safe)
$pre = @'
param(
  [string]$TemplatePath = "contracts\Template.manifest.json",
  [string]$UIPackPath   = "contracts\UIPack\UIPack.manifest.json",
  [string]$PlanPath     = "contracts\AssemblyPlan.sample.json",
  [string]$FeatureDir   = "contracts\modules\feature",
  [string]$CapDir       = "contracts\modules\cap"
)
$ErrorActionPreference = "Stop"
$tmpl = Get-Content $TemplatePath -Raw | ConvertFrom-Json
$ui   = Get-Content $UIPackPath   -Raw | ConvertFrom-Json
$plan = Get-Content $PlanPath     -Raw | ConvertFrom-Json

# 1) UI coverage
$need = New-Object 'System.Collections.Generic.HashSet[string]'
foreach($x in $tmpl.requiredUI){ [void]$need.Add($x) }
$have = New-Object 'System.Collections.Generic.HashSet[string]'
foreach($x in $ui.providesUI){ [void]$have.Add($x) }
$miss = @(); foreach($x in $need){ if(-not $have.Contains($x)){ $miss += $x } }
if($miss.Count -gt 0){ throw ("UI coverage failed → missing: " + ($miss -join ', ')) }

# 2) Slots
$slotIds = @(); foreach($s in $tmpl.slots){ $slotIds += $s.slotId }
foreach($feat in $plan.features){
  $fp = Join-Path $FeatureDir ("{0}.manifest.json" -f $feat.id)
  $f = Get-Content $fp -Raw | ConvertFrom-Json
  foreach($scr in $f.screens){
    if(-not ($slotIds -contains $scr.slotTarget)){
      throw ("Slot mismatch → feature=" + $feat.id + " target=" + $scr.slotTarget + " not in template.slots")
    }
  }
}

# 3) Capability needs
$capSet = New-Object 'System.Collections.Generic.HashSet[string]'
foreach($c in $plan.capabilities){ [void]$capSet.Add($c.id) }
foreach($feat in $plan.features){
  $fp = Join-Path $FeatureDir ("{0}.manifest.json" -f $feat.id)
  $f = Get-Content $fp -Raw | ConvertFrom-Json
  foreach($c in $f.capabilityNeeds){
    if(-not $capSet.Contains($c)){
      throw ("Capability missing → feature=" + $feat.id + " needs=" + $c)
    }
  }
}

Write-Host "Precheck OK: UI=100%, slots valid, capability needs covered." -ForegroundColor Green
'@
$pre | Set-Content -Encoding UTF8 "tools\ndjc-assembly-precheck.ps1"

Write-Host "Files created under '$OutDir' and 'tools'." -ForegroundColor Green
Write-Host "Next: .\tools\ndjc-assembly-precheck.ps1" -ForegroundColor Yellow
Write-Host "=== Done ===" -ForegroundColor Cyan
'''
with open(path, "w", encoding="utf-8") as f:
    f.write(script)
path
