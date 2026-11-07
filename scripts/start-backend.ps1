# ==============================================================================
# AI-Based College Resource Finder - Windows PowerShell Startup Script
# ==============================================================================

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path "$ScriptDir\.."
$BackendDir = "$ProjectRoot\backend"

# Ensure Maven is on PATH
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    $LocalMvn = "$env:USERPROFILE\tools\maven\apache-maven-3.9.9\bin"
    if (Test-Path "$LocalMvn\mvn.cmd") {
        $env:PATH = "$LocalMvn;" + $env:PATH
        Write-Host "[INFO] Appended Maven to PATH: $LocalMvn" -ForegroundColor Cyan
    } else {
        Write-Error "[ERROR] Maven is not installed or not in PATH."
        exit 1
    }
}

# Verify Java
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error "[ERROR] Java 21 is not installed or not in PATH."
    exit 1
}

# Prompt for DB_PASSWORD if not already provided
if (-not $env:DB_PASSWORD) {
    Write-Host "[NOTE] DB_PASSWORD is not set in environment." -ForegroundColor Yellow
    $promptPass = Read-Host "Enter MySQL root password (leave empty if none)" -AsSecureString
    $env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $promptPass).Password
}

Set-Location $BackendDir
Write-Host "[INFO] Starting Spring Boot application from $BackendDir..." -ForegroundColor Green
mvn spring-boot:run
