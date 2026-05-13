$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$envFile = Join-Path $repoRoot "mail.env"
if (-not (Test-Path $envFile)) {
    Write-Host "未找到 mail.env。请复制 mail.env.example 为 mail.env 并填写 MAIL_* 后再运行。" -ForegroundColor Yellow
    exit 1
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) {
        return
    }
    $idx = $line.IndexOf("=")
    if ($idx -lt 1) {
        return
    }
    $name = $line.Substring(0, $idx).Trim()
    $value = $line.Substring($idx + 1).Trim()
    [Environment]::SetEnvironmentVariable($name, $value, "Process")
}

if ([string]::IsNullOrWhiteSpace($env:SPRING_PROFILES_ACTIVE)) {
    $env:SPRING_PROFILES_ACTIVE = "mail"
}
elseif ($env:SPRING_PROFILES_ACTIVE -notmatch "(^|,)\s*mail\s*(,|$)") {
    $env:SPRING_PROFILES_ACTIVE = "$($env:SPRING_PROFILES_ACTIVE),mail"
}

Write-Host "SPRING_PROFILES_ACTIVE=$($env:SPRING_PROFILES_ACTIVE)" -ForegroundColor Cyan
mvn spring-boot:run
