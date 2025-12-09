
Param(
  [Parameter(Mandatory=$false)][string]$Path = "../assembly/assembly.json"
)
# Simple machine checks for Contract-UI v1 (PowerShell)
$ErrorActionPreference = "Stop"
if (!(Test-Path $Path)) { throw "Assembly file not found: $Path" }
$raw = Get-Content -Raw -Path $Path
$data = $raw | ConvertFrom-Json

# Check required fields
$required = @("template","uiPack","modules","routingEntry")
$missing = @()
foreach ($k in $required) { if (-not $data.PSObject.Properties.Name.Contains($k)) { $missing += $k } }
if ($missing.Count -gt 0) { throw "Missing required fields: $($missing -join ', ')" }

# Check modules array non-empty
if ($data.modules.Count -lt 1) { throw "modules must not be empty" }

# Check each module has required fields
foreach ($m in $data.modules) {
  foreach ($k in @("moduleId","type","supportedSlots","routes")) {
    if (-not $m.PSObject.Properties.Name.Contains($k)) { throw "module missing field '$k' for $($m.moduleId)" }
  }
  # Slot whitelist (core 7 + optional 3)
  $slotWhitelist = @("header","hero","primary","secondary","detail","sheet","tabBar","fab","dialog","settings")
  foreach ($s in $m.supportedSlots) {
    if (-not ($slotWhitelist -contains $s)) { throw "unsupported slot '$s' in module $($m.moduleId)" }
  }
  # Route rules
  if ($m.routes.Count -lt 1) { throw "module $($m.moduleId) must declare at least 1 route" }
}

# Entry route must exist
$allRoutes = @()
foreach ($m in $data.modules) { foreach ($r in $m.routes) { $allRoutes += $r.routeId } }
if (-not ($allRoutes -contains $data.routingEntry)) { throw "routingEntry '$($data.routingEntry)' does not exist" }

Write-Host "[OK] Contract-UI v1 checks passed." -ForegroundColor Green
