# Simple Load Testing Script for QA Project
param(
    [int]$Users = 5,
    [int]$LoginAttempts = 3,
    [string]$BaseUrl = "http://localhost:8080"
)

$global:results = @()
$global:startTime = Get-Date
$global:registeredUsers = @()

function Write-Header {
    Write-Host "===========================================" -ForegroundColor Green
    Write-Host "  QA PROJECT PERFORMANCE LOAD TESTING    " -ForegroundColor Green
    Write-Host "===========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Configuration:" -ForegroundColor Yellow
    Write-Host "Users to test: $Users" -ForegroundColor White
    Write-Host "Login attempts per user: $LoginAttempts" -ForegroundColor White
    Write-Host "Total operations: $($Users + ($Users * $LoginAttempts))" -ForegroundColor White
    Write-Host "Target API: $BaseUrl/api/auth" -ForegroundColor White
    Write-Host ""
}

function Register-TestUser {
    param($UserIndex)
    
    $timestamp = Get-Date -Format "HHmmss"
    $randomId = Get-Random -Minimum 100 -Maximum 999
    
    $username = "perftest$UserIndex$timestamp"
    $email = "perftest$UserIndex$timestamp$randomId@loadtest.com"
    $password = "LoadTest123!"
    
    $requestData = @{
        username = $username
        email = $email
        password = $password
    } | ConvertTo-Json
    
    $headers = @{
        'Content-Type' = 'application/json'
        'Accept' = 'application/json'
    }
    
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    
    try {
        $response = Invoke-WebRequest -Uri "$BaseUrl/api/auth/register" -Method POST -Body $requestData -Headers $headers -ErrorAction Stop
        $stopwatch.Stop()
        
        $responseData = $response.Content | ConvertFrom-Json
        
        $result = @{
            Operation = "Registration"
            UserIndex = $UserIndex
            Username = $username
            ResponseTime = $stopwatch.ElapsedMilliseconds
            StatusCode = $response.StatusCode
            Success = $responseData.success
            Timestamp = Get-Date
        }
        
        if ($responseData.success) {
            $global:registeredUsers += @{
                Username = $username
                Password = $password
                UserIndex = $UserIndex
            }
            Write-Host "Registration successful: $username ($($stopwatch.ElapsedMilliseconds)ms)" -ForegroundColor Green
        } else {
            Write-Host "Registration failed: $username" -ForegroundColor Red
        }
        
    } catch {
        $stopwatch.Stop()
        
        $result = @{
            Operation = "Registration"
            UserIndex = $UserIndex
            Username = $username
            ResponseTime = $stopwatch.ElapsedMilliseconds
            StatusCode = 400
            Success = $false
            Timestamp = Get-Date
        }
        
        Write-Host "Registration error: $username" -ForegroundColor Red
    }
    
    $global:results += $result
    return $result
}

function Test-UserLogin {
    param($UserCredentials, $AttemptNumber)
    
    $requestData = @{
        username = $UserCredentials.Username
        password = $UserCredentials.Password
    } | ConvertTo-Json
    
    $headers = @{
        'Content-Type' = 'application/json'
        'Accept' = 'application/json'
    }
    
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    
    try {
        $response = Invoke-WebRequest -Uri "$BaseUrl/api/auth/login" -Method POST -Body $requestData -Headers $headers -ErrorAction Stop
        $stopwatch.Stop()
        
        $responseData = $response.Content | ConvertFrom-Json
        
        $result = @{
            Operation = "Login"
            UserIndex = $UserCredentials.UserIndex
            Username = $UserCredentials.Username
            AttemptNumber = $AttemptNumber
            ResponseTime = $stopwatch.ElapsedMilliseconds
            StatusCode = $response.StatusCode
            Success = $responseData.success
            HasToken = $responseData.token -ne $null
            Timestamp = Get-Date
        }
        
        if ($responseData.success) {
            Write-Host "Login successful: $($UserCredentials.Username) #$AttemptNumber ($($stopwatch.ElapsedMilliseconds)ms)" -ForegroundColor Green
        } else {
            Write-Host "Login failed: $($UserCredentials.Username) #$AttemptNumber" -ForegroundColor Red
        }
        
    } catch {
        $stopwatch.Stop()
        
        $result = @{
            Operation = "Login"
            UserIndex = $UserCredentials.UserIndex
            Username = $UserCredentials.Username
            AttemptNumber = $AttemptNumber
            ResponseTime = $stopwatch.ElapsedMilliseconds
            StatusCode = 401
            Success = $false
            HasToken = $false
            Timestamp = Get-Date
        }
        
        Write-Host "Login error: $($UserCredentials.Username) #$AttemptNumber" -ForegroundColor Red
    }
    
    $global:results += $result
    return $result
}

function Execute-LoadTest {
    Write-Header
    
    # Phase 1: Register Users
    Write-Host "PHASE 1: User Registration" -ForegroundColor Cyan
    Write-Host "Creating $Users test users..." -ForegroundColor Yellow
    Write-Host ""
    
    for ($i = 1; $i -le $Users; $i++) {
        Register-TestUser -UserIndex $i
        Start-Sleep -Milliseconds 300
    }
    
    Write-Host ""
    Write-Host "Registration complete. Successfully registered: $($global:registeredUsers.Count) users" -ForegroundColor Yellow
    Write-Host ""
    
    # Phase 2: Load Test Logins
    if ($global:registeredUsers.Count -gt 0) {
        Write-Host "PHASE 2: Login Load Testing" -ForegroundColor Cyan
        Write-Host "Testing login performance..." -ForegroundColor Yellow
        Write-Host ""
        
        foreach ($user in $global:registeredUsers) {
            for ($attempt = 1; $attempt -le $LoginAttempts; $attempt++) {
                Test-UserLogin -UserCredentials $user -AttemptNumber $attempt
                Start-Sleep -Milliseconds 100
            }
        }
    } else {
        Write-Host "No users registered successfully. Skipping login tests." -ForegroundColor Red
    }
}

function Show-Results {
    $endTime = Get-Date
    $totalDuration = ($endTime - $global:startTime).TotalSeconds
    
    Write-Host ""
    Write-Host "===========================================" -ForegroundColor Green
    Write-Host "        PERFORMANCE TEST RESULTS          " -ForegroundColor Green
    Write-Host "===========================================" -ForegroundColor Green
    
    $registrations = $global:results | Where-Object { $_.Operation -eq "Registration" }
    $logins = $global:results | Where-Object { $_.Operation -eq "Login" }
    
    $totalOperations = $global:results.Count
    $successfulOperations = ($global:results | Where-Object { $_.Success }).Count
    $failedOperations = $totalOperations - $successfulOperations
    
    Write-Host ""
    Write-Host "OVERALL STATISTICS" -ForegroundColor Yellow
    Write-Host "Test Duration: $([math]::Round($totalDuration, 2)) seconds" -ForegroundColor White
    Write-Host "Total Operations: $totalOperations" -ForegroundColor White
    Write-Host "Successful Operations: $successfulOperations" -ForegroundColor White
    Write-Host "Failed Operations: $failedOperations" -ForegroundColor White
    Write-Host "Success Rate: $([math]::Round(($successfulOperations / $totalOperations) * 100, 2))%" -ForegroundColor White
    Write-Host "Operations per Second: $([math]::Round($totalOperations / $totalDuration, 2))" -ForegroundColor White
    
    if ($registrations.Count -gt 0) {
        Write-Host ""
        Write-Host "REGISTRATION PERFORMANCE" -ForegroundColor Yellow
        
        $regSuccessful = $registrations | Where-Object { $_.Success }
        
        Write-Host "Registration Attempts: $($registrations.Count)" -ForegroundColor White
        Write-Host "Successful Registrations: $($regSuccessful.Count)" -ForegroundColor White
        Write-Host "Registration Success Rate: $([math]::Round(($regSuccessful.Count / $registrations.Count) * 100, 2))%" -ForegroundColor White
        
        if ($regSuccessful.Count -gt 0) {
            $regTimes = $regSuccessful | ForEach-Object { $_.ResponseTime }
            $avgRegTime = ($regTimes | Measure-Object -Average).Average
            Write-Host "Avg Registration Time: $([math]::Round($avgRegTime, 2))ms" -ForegroundColor White
        }
    }
    
    if ($logins.Count -gt 0) {
        Write-Host ""
        Write-Host "LOGIN PERFORMANCE" -ForegroundColor Yellow
        
        $loginSuccessful = $logins | Where-Object { $_.Success }
        
        Write-Host "Login Attempts: $($logins.Count)" -ForegroundColor White
        Write-Host "Successful Logins: $($loginSuccessful.Count)" -ForegroundColor White
        Write-Host "Login Success Rate: $([math]::Round(($loginSuccessful.Count / $logins.Count) * 100, 2))%" -ForegroundColor White
        
        if ($loginSuccessful.Count -gt 0) {
            $loginTimes = $loginSuccessful | ForEach-Object { $_.ResponseTime }
            $avgLoginTime = ($loginTimes | Measure-Object -Average).Average
            Write-Host "Avg Login Time: $([math]::Round($avgLoginTime, 2))ms" -ForegroundColor White
            
            $tokensReceived = ($loginSuccessful | Where-Object { $_.HasToken }).Count
            Write-Host "Tokens Generated: $tokensReceived / $($loginSuccessful.Count)" -ForegroundColor White
        }
    }
    
    # Export results
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $csvPath = "load_test_results_$timestamp.csv"
    $global:results | Export-Csv -Path $csvPath -NoTypeInformation
    
    Write-Host ""
    Write-Host "Results exported to: $csvPath" -ForegroundColor Cyan
    Write-Host ""
}

# Execute the load test
Execute-LoadTest
Show-Results