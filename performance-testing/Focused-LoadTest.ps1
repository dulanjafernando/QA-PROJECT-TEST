# Focused Load Testing Script for QA Project Authentication
# Simplified approach with validated test data

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
    Write-Host "• Users to test: $Users" -ForegroundColor White
    Write-Host "• Login attempts per user: $LoginAttempts" -ForegroundColor White
    Write-Host "• Total operations: $($Users + ($Users * $LoginAttempts))" -ForegroundColor White
    Write-Host "• Target API: $BaseUrl/api/auth" -ForegroundColor White
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
            Email = $email
            ResponseTime = $stopwatch.ElapsedMilliseconds
            StatusCode = $response.StatusCode
            Success = $responseData.success
            Message = $responseData.message
            Timestamp = Get-Date
        }
        
        if ($responseData.success) {
            $global:registeredUsers += @{
                Username = $username
                Password = $password
                UserIndex = $UserIndex
            }
            Write-Host "✓ Registration successful: $username ($($stopwatch.ElapsedMilliseconds)ms)" -ForegroundColor Green
        } else {
            Write-Host "✗ Registration failed: $username - $($responseData.message)" -ForegroundColor Red
        }
        
    } catch {
        $stopwatch.Stop()
        
        $errorMessage = $_.Exception.Message
        $statusCode = 0
        
        if ($_.Exception.Response) {
            $statusCode = $_.Exception.Response.StatusCode.value__
            try {
                $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
                $errorResponse = $reader.ReadToEnd() | ConvertFrom-Json
                $errorMessage = $errorResponse.message
            } catch {
                # Keep original error message if JSON parsing fails
            }
        }
        
        $result = @{
            Operation = "Registration"
            UserIndex = $UserIndex
            Username = $username
            Email = $email
            ResponseTime = $stopwatch.ElapsedMilliseconds
            StatusCode = $statusCode
            Success = $false
            Message = $errorMessage
            Timestamp = Get-Date
        }
        
        Write-Host "✗ Registration error: $username - $errorMessage" -ForegroundColor Red
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
            HasToken = $responseData.token -ne $null -and $responseData.token -ne ""
            Message = $responseData.message
            Timestamp = Get-Date
        }
        
        if ($responseData.success) {
            Write-Host "✓ Login successful: $($UserCredentials.Username) #$AttemptNumber ($($stopwatch.ElapsedMilliseconds)ms)" -ForegroundColor Green
        } else {
            Write-Host "✗ Login failed: $($UserCredentials.Username) #$AttemptNumber - $($responseData.message)" -ForegroundColor Red
        }
        
    } catch {
        $stopwatch.Stop()
        
        $errorMessage = $_.Exception.Message
        $statusCode = 0
        
        if ($_.Exception.Response) {
            $statusCode = $_.Exception.Response.StatusCode.value__
            try {
                $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
                $errorResponse = $reader.ReadToEnd() | ConvertFrom-Json
                $errorMessage = $errorResponse.message
            } catch {
                # Keep original error message
            }
        }
        
        $result = @{
            Operation = "Login"
            UserIndex = $UserCredentials.UserIndex
            Username = $UserCredentials.Username
            AttemptNumber = $AttemptNumber
            ResponseTime = $stopwatch.ElapsedMilliseconds
            StatusCode = $statusCode
            Success = $false
            HasToken = $false
            Message = $errorMessage
            Timestamp = Get-Date
        }
        
        Write-Host "✗ Login error: $($UserCredentials.Username) #$AttemptNumber - $errorMessage" -ForegroundColor Red
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
        Start-Sleep -Milliseconds 300  # Small delay to prevent overwhelming
    }
    
    Write-Host ""
    Write-Host "Registration complete. Successfully registered: $($global:registeredUsers.Count) users" -ForegroundColor Yellow
    Write-Host ""
    
    # Phase 2: Load Test Logins
    if ($global:registeredUsers.Count -gt 0) {
        Write-Host "PHASE 2: Login Load Testing" -ForegroundColor Cyan
        Write-Host "Testing login performance with concurrent requests..." -ForegroundColor Yellow
        Write-Host ""
        
        # Test concurrent logins
        $jobs = @()
        
        foreach ($user in $global:registeredUsers) {
            for ($attempt = 1; $attempt -le $LoginAttempts; $attempt++) {
                $job = Start-Job -ScriptBlock {
                    param($UserCreds, $AttemptNum, $BaseUrl)
                    
                    $requestData = @{
                        username = $UserCreds.Username
                        password = $UserCreds.Password
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
                        
                        return @{
                            Operation = "Login"
                            UserIndex = $UserCreds.UserIndex
                            Username = $UserCreds.Username
                            AttemptNumber = $AttemptNum
                            ResponseTime = $stopwatch.ElapsedMilliseconds
                            StatusCode = $response.StatusCode
                            Success = $responseData.success
                            HasToken = $responseData.token -ne $null
                            Message = $responseData.message
                            Timestamp = Get-Date
                        }
                        
                    } catch {
                        $stopwatch.Stop()
                        
                        return @{
                            Operation = "Login"
                            UserIndex = $UserCreds.UserIndex
                            Username = $UserCreds.Username
                            AttemptNumber = $AttemptNum
                            ResponseTime = $stopwatch.ElapsedMilliseconds
                            StatusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { 0 }
                            Success = $false
                            HasToken = $false
                            Message = $_.Exception.Message
                            Timestamp = Get-Date
                        }
                    }
                } -ArgumentList $user, $attempt, $BaseUrl
                
                $jobs += $job
            }
        }
        
        # Wait for concurrent jobs and collect results
        Write-Host "Executing $($jobs.Count) concurrent login requests..." -ForegroundColor Yellow
        
        $concurrentResults = @()
        foreach ($job in $jobs) {
            $result = Wait-Job $job | Receive-Job
            $concurrentResults += $result
            Remove-Job $job
        }
        
        $global:results += $concurrentResults
        
        # Display concurrent results
        $successfulLogins = $concurrentResults | Where-Object { $_.Success }
        $failedLogins = $concurrentResults | Where-Object { -not $_.Success }
        
        Write-Host ""
        Write-Host "Concurrent login tests completed:" -ForegroundColor Yellow
        Write-Host "• Successful logins: $($successfulLogins.Count)" -ForegroundColor Green
        Write-Host "• Failed logins: $($failedLogins.Count)" -ForegroundColor Red
        
    } else {
        Write-Host "No users registered successfully. Skipping login tests." -ForegroundColor Red
    }
}

function Show-PerformanceResults {
    $endTime = Get-Date
    $totalDuration = ($endTime - $global:startTime).TotalSeconds
    
    Write-Host ""
    Write-Host "===========================================" -ForegroundColor Green
    Write-Host "        PERFORMANCE TEST RESULTS          " -ForegroundColor Green
    Write-Host "===========================================" -ForegroundColor Green
    
    # Separate results by operation
    $registrations = $global:results | Where-Object { $_.Operation -eq "Registration" }
    $logins = $global:results | Where-Object { $_.Operation -eq "Login" }
    
    $totalOperations = $global:results.Count
    $successfulOperations = ($global:results | Where-Object { $_.Success }).Count
    $failedOperations = $totalOperations - $successfulOperations
    
    # Overall Statistics
    Write-Host ""
    Write-Host "📊 OVERALL STATISTICS" -ForegroundColor Yellow
    Write-Host "• Test Duration: $([math]::Round($totalDuration, 2)) seconds" -ForegroundColor White
    Write-Host "• Total Operations: $totalOperations" -ForegroundColor White
    Write-Host "• Successful Operations: $successfulOperations" -ForegroundColor White
    Write-Host "• Failed Operations: $failedOperations" -ForegroundColor White
    Write-Host "• Success Rate: $([math]::Round(($successfulOperations / $totalOperations) * 100, 2))%" -ForegroundColor White
    Write-Host "• Operations per Second: $([math]::Round($totalOperations / $totalDuration, 2))" -ForegroundColor White
    
    # Registration Analysis
    if ($registrations.Count -gt 0) {
        Write-Host ""
        Write-Host "🔐 REGISTRATION PERFORMANCE" -ForegroundColor Yellow
        
        $regSuccessful = $registrations | Where-Object { $_.Success }
        $regFailed = $registrations | Where-Object { -not $_.Success }
        
        Write-Host "• Registration Attempts: $($registrations.Count)" -ForegroundColor White
        Write-Host "• Successful Registrations: $($regSuccessful.Count)" -ForegroundColor White
        Write-Host "• Failed Registrations: $($regFailed.Count)" -ForegroundColor White
        Write-Host "• Registration Success Rate: $([math]::Round(($regSuccessful.Count / $registrations.Count) * 100, 2))%" -ForegroundColor White
        
        if ($regSuccessful.Count -gt 0) {
            $regTimes = $regSuccessful | ForEach-Object { $_.ResponseTime }
            $avgRegTime = ($regTimes | Measure-Object -Average).Average
            $minRegTime = ($regTimes | Measure-Object -Minimum).Minimum
            $maxRegTime = ($regTimes | Measure-Object -Maximum).Maximum
            
            Write-Host "• Avg Registration Time: $([math]::Round($avgRegTime, 2))ms" -ForegroundColor White
            Write-Host "• Min Registration Time: $minRegTime ms" -ForegroundColor White
            Write-Host "• Max Registration Time: $maxRegTime ms" -ForegroundColor White
        }
    }
    
    # Login Analysis
    if ($logins.Count -gt 0) {
        Write-Host ""
        Write-Host "🔓 LOGIN PERFORMANCE" -ForegroundColor Yellow
        
        $loginSuccessful = $logins | Where-Object { $_.Success }
        $loginFailed = $logins | Where-Object { -not $_.Success }
        
        Write-Host "• Login Attempts: $($logins.Count)" -ForegroundColor White
        Write-Host "• Successful Logins: $($loginSuccessful.Count)" -ForegroundColor White
        Write-Host "• Failed Logins: $($loginFailed.Count)" -ForegroundColor White
        Write-Host "• Login Success Rate: $([math]::Round(($loginSuccessful.Count / $logins.Count) * 100, 2))%" -ForegroundColor White
        
        if ($loginSuccessful.Count -gt 0) {
            $loginTimes = $loginSuccessful | ForEach-Object { $_.ResponseTime }
            $avgLoginTime = ($loginTimes | Measure-Object -Average).Average
            $minLoginTime = ($loginTimes | Measure-Object -Minimum).Minimum
            $maxLoginTime = ($loginTimes | Measure-Object -Maximum).Maximum
            
            Write-Host "• Avg Login Time: $([math]::Round($avgLoginTime, 2))ms" -ForegroundColor White
            Write-Host "• Min Login Time: $minLoginTime ms" -ForegroundColor White
            Write-Host "• Max Login Time: $maxLoginTime ms" -ForegroundColor White
            
            # Token verification
            $tokensReceived = ($loginSuccessful | Where-Object { $_.HasToken }).Count
            Write-Host "• Tokens Generated: $tokensReceived / $($loginSuccessful.Count)" -ForegroundColor White
        }
    }
    
    # Performance Assessment
    Write-Host ""
    Write-Host "📈 PERFORMANCE ASSESSMENT" -ForegroundColor Yellow
    
    $allSuccessfulTimes = ($global:results | Where-Object { $_.Success } | ForEach-Object { $_.ResponseTime })
    if ($allSuccessfulTimes.Count -gt 0) {
        $overallAvgTime = ($allSuccessfulTimes | Measure-Object -Average).Average
        
        if ($overallAvgTime -lt 200) {
            Write-Host "• Response Time: EXCELLENT (< 200ms)" -ForegroundColor Green
        } elseif ($overallAvgTime -lt 500) {
            Write-Host "• Response Time: GOOD (< 500ms)" -ForegroundColor Green
        } elseif ($overallAvgTime -lt 1000) {
            Write-Host "• Response Time: ACCEPTABLE (< 1000ms)" -ForegroundColor Yellow
        } else {
            Write-Host "• Response Time: NEEDS IMPROVEMENT (> 1000ms)" -ForegroundColor Red
        }
    }
    
    $throughput = $totalOperations / $totalDuration
    if ($throughput -gt 10) {
        Write-Host "• Throughput: EXCELLENT (> 10 ops/sec)" -ForegroundColor Green
    } elseif ($throughput -gt 5) {
        Write-Host "• Throughput: GOOD (> 5 ops/sec)" -ForegroundColor Green
    } elseif ($throughput -gt 2) {
        Write-Host "• Throughput: ACCEPTABLE (> 2 ops/sec)" -ForegroundColor Yellow
    } else {
        Write-Host "• Throughput: NEEDS IMPROVEMENT (< 2 ops/sec)" -ForegroundColor Red
    }
    
    $errorRate = ($failedOperations / $totalOperations) * 100
    if ($errorRate -eq 0) {
        Write-Host "• Reliability: PERFECT (0% errors)" -ForegroundColor Green
    } elseif ($errorRate -lt 5) {
        Write-Host "• Reliability: EXCELLENT (< 5% errors)" -ForegroundColor Green
    } elseif ($errorRate -lt 10) {
        Write-Host "• Reliability: GOOD (< 10% errors)" -ForegroundColor Yellow
    } else {
        Write-Host "• Reliability: CRITICAL (> 10% errors)" -ForegroundColor Red
    }
    
    # Bottleneck Identification
    Write-Host ""
    Write-Host "🔍 BOTTLENECK ANALYSIS" -ForegroundColor Yellow
    
    $bottlenecksFound = $false
    
    if ($registrations.Count -gt 0) {
        $regSuccessful = $registrations | Where-Object { $_.Success }
        if ($regSuccessful.Count -gt 0) {
            $avgRegTime = ($regSuccessful | ForEach-Object { $_.ResponseTime } | Measure-Object -Average).Average
            if ($avgRegTime -gt 500) {
                Write-Host "• REGISTRATION BOTTLENECK: User creation > 500ms" -ForegroundColor Red
                Write-Host "  → Consider: Database indexing, connection pooling" -ForegroundColor Gray
                $bottlenecksFound = $true
            }
        }
    }
    
    if ($logins.Count -gt 0) {
        $loginSuccessful = $logins | Where-Object { $_.Success }
        if ($loginSuccessful.Count -gt 0) {
            $avgLoginTime = ($loginSuccessful | ForEach-Object { $_.ResponseTime } | Measure-Object -Average).Average
            if ($avgLoginTime -gt 300) {
                Write-Host "• LOGIN BOTTLENECK: Authentication > 300ms" -ForegroundColor Red
                Write-Host "  → Consider: Password hashing optimization, caching" -ForegroundColor Gray
                $bottlenecksFound = $true
            }
        }
    }
    
    if ($errorRate -gt 5) {
        Write-Host "• HIGH ERROR RATE: > 5% failures" -ForegroundColor Red
        Write-Host "  → Consider: Resource limits, validation logic" -ForegroundColor Gray
        $bottlenecksFound = $true
    }
    
    if (-not $bottlenecksFound) {
        Write-Host "• No significant bottlenecks identified ✓" -ForegroundColor Green
    }
    
    # Export results
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $csvPath = "C:\Users\User\Desktop\QAproject\performance-testing\load_test_results_$timestamp.csv"
    $global:results | Export-Csv -Path $csvPath -NoTypeInformation
    
    Write-Host ""
    Write-Host "Results exported to: $csvPath" -ForegroundColor Cyan
    Write-Host ""
}

# Execute the load test
Execute-LoadTest
Show-PerformanceResults