# Run JMeter load/stress plans against a running API (default http://localhost:8080).
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$BaseUrl = if ($env:PERF_BASE_URL) { $env:PERF_BASE_URL } else { "http://localhost:8080" }
$DataDir = Join-Path $Root "performance\jmeter\data"

if (-not (Get-Command jmeter -ErrorAction SilentlyContinue)) {
    Write-Error "JMeter not found on PATH. Install Apache JMeter 5.6+ and add bin to PATH."
}

Write-Host "BASE_URL=$BaseUrl"
Write-Host "DATA_DIR=$DataDir"

New-Item -ItemType Directory -Force -Path "target\jmeter-device", "target\jmeter-auth" | Out-Null

Write-Host "Device load & stress..."
jmeter -n `
  -t "performance\jmeter\device-load-stress.jmx" `
  -l "target\jmeter-device\results.jtl" `
  -e -o "target\jmeter-device\report" `
  -JBASE_URL=$BaseUrl `
  -JDATA_DIR=$DataDir
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Auth load & stress..."
jmeter -n `
  -t "performance\jmeter\auth-load-stress.jmx" `
  -l "target\jmeter-auth\results.jtl" `
  -e -o "target\jmeter-auth\report" `
  -JBASE_URL=$BaseUrl
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$AuthReport = Join-Path $Root "target\jmeter-auth\report\index.html"
$DeviceReport = Join-Path $Root "target\jmeter-device\report\index.html"
Write-Host "Done:"
Write-Host "  Auth:   $AuthReport"
Write-Host "  Device: $DeviceReport"
Start-Process $AuthReport
