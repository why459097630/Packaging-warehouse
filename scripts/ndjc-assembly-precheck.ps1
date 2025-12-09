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
