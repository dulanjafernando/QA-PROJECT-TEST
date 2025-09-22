# Check if Java is installed
try {
    $javaVersion = java -version 2>&1
    Write-Host "Java is installed:" -ForegroundColor Green
    Write-Host $javaVersion[0] -ForegroundColor Green
} catch {
    Write-Host "Java is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install Java from https://adoptium.net/" -ForegroundColor Yellow
    exit
}

# Find Java installation path
$javaPath = (Get-Command java).Path
$jdkPath = $javaPath | Split-Path | Split-Path

Write-Host "Detected JDK path: $jdkPath" -ForegroundColor Yellow

# Set JAVA_HOME
[Environment]::SetEnvironmentVariable("JAVA_HOME", $jdkPath, "Machine")
Write-Host "JAVA_HOME set to: $jdkPath" -ForegroundColor Green

# Verify
Write-Host "Verification:" -ForegroundColor Cyan
Write-Host "JAVA_HOME: $([Environment]::GetEnvironmentVariable('JAVA_HOME', 'Machine'))" -ForegroundColor Cyan

# Test Maven
try {
    $mavenVersion = mvn --version 2>&1
    Write-Host "Maven is working:" -ForegroundColor Green
    Write-Host $mavenVersion[0] -ForegroundColor Green
} catch {
    Write-Host "Maven is not working. Please check your Maven installation." -ForegroundColor Red
}

Write-Host "Please restart your terminal for changes to take effect." -ForegroundColor Yellow