# Comprehensive Load Testing Script for QA Project
# Tests both registration and login with proper user creation

param(
    [int]$ConcurrentUsers = 5,
    [int]$RequestsPerUser = 2,
    [string]$BaseUrl = "http://localhost:8080"
)

$global:results = @()
$global:errors = @()
$global:startTime = Get-Date
$global:totalRequests = 0
$global:successfulRequests = 0
$global:registeredUsers = @()

function Write-TestHeader {
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "QA Project Comprehensive Load Testing" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Test Configuration:" -ForegroundColor Yellow
    Write-Host "- Concurrent Users: $ConcurrentUsers" -ForegroundColor White
    Write-Host "- Requests per User: $RequestsPerUser" -ForegroundColor White
    Write-Host "- Total Operations: Registration + Login tests" -ForegroundColor White
    Write-Host "- Target URL: $BaseUrl" -ForegroundColor White
    Write-Host ""
}

function Test-UserRegistration {
    param($UserId)
    
    $uniqueId = Get-Random -Minimum 1000 -Maximum 9999
    $timestamp = Get-Date -Format "yyyyMMddHHmmss"
    
    $username = "loadtest$UserId$timestamp"
    $email = "loadtest$UserId$timestamp@testdomain.com"
    $password = "LoadTest123!"
    
    $testData = @{
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
        $response = Invoke-WebRequest -Uri "$BaseUrl/api/auth/register" -Method POST -Body $testData -Headers $headers -ErrorAction Stop
        $stopwatch.Stop()
        
        $result = @{
            Operation = "Registration"
            UserId = $UserId
            Username = $username
            ResponseTime = $stopwatch.ElapsedMilliseconds
            StatusCode = $response.StatusCode
            Success = $true
            Timestamp = Get-Date
            ResponseSize = $response.RawContentLength
        }
        
        # Store user credentials for login testing
        $global:registeredUsers += @{
            Username = $username
            Password = $password
            UserId = $UserId
        }
        
        $global:successfulRequests++
        Write-Host "✓ User $username registered successfully ($($stopwatch.ElapsedMilliseconds)ms)" -ForegroundColor Green
        
    } catch {
        $stopwatch.Stop()
        
        $result = @{
            Operation = "Registration"
            UserId = $UserId
            Username = $username
            ResponseTime = $stopwatch.ElapsedMilliseconds
            StatusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { 0 }
            Success = $false
            Error = $_.Exception.Message
            Timestamp = Get-Date
            ResponseSize = 0
        }
        
        $global:errors += $result
        Write-Host "✗ User $username registration failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    $global:results += $result
    $global:totalRequests++
    
    return $result
}

function Test-UserLogin {
    param($UserCredentials, $RequestNum)
    
    $testData = @{
        username = $UserCredentials.Username
        password = $UserCredentials.Password
    } | ConvertTo-Json
    
    $headers = @{
        'Content-Type' = 'application/json'
        'Accept' = 'application/json'
    }
    
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    
    try {
        $response = Invoke-WebRequest -Uri "$BaseUrl/api/auth/login" -Method POST -Body $testData -Headers $headers -ErrorAction Stop
        $stopwatch.Stop()
        
        $responseContent = $response.Content | ConvertFrom-Json
        
        $result = @{
            Operation = "Login"
            UserId = $UserCredentials.UserId
            Username = $UserCredentials.Username
            RequestNum = $RequestNum
            ResponseTime = $stopwatch.ElapsedMilliseconds
            StatusCode = $response.StatusCode
            Success = $true
            Timestamp = Get-Date
            ResponseSize = $response.RawContentLength
            HasToken = $responseContent.token -ne $null
        }
        
        $global:successfulRequests++
        Write-Host "✓ User $($UserCredentials.Username) login successful ($($stopwatch.ElapsedMilliseconds)ms)" -ForegroundColor Green
        
    } catch {
        $stopwatch.Stop()
        
        $result = @{
            Operation = "Login"
            UserId = $UserCredentials.UserId
            Username = $UserCredentials.Username
            RequestNum = $RequestNum
            ResponseTime = $stopwatch.ElapsedMilliseconds
            StatusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { 0 }
            Success = $false
            Error = $_.Exception.Message
            Timestamp = Get-Date
            ResponseSize = 0
            HasToken = $false
        }
        
        $global:errors += $result
        Write-Host "✗ User $($UserCredentials.Username) login failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    $global:results += $result
    $global:totalRequests++
    
    return $result
}

function Start-ComprehensiveLoadTest {
    Write-TestHeader
    
    Write-Host "Phase 1: User Registration" -ForegroundColor Cyan
    Write-Host "Creating $ConcurrentUsers test users..." -ForegroundColor Yellow
    
    # Phase 1: Register users
    for ($userId = 1; $userId -le $ConcurrentUsers; $userId++) {
        Test-UserRegistration -UserId $userId
        Start-Sleep -Milliseconds 500  # Small delay to avoid overwhelming the server
    }
    
    Write-Host ""
    Write-Host "Phase 2: Login Load Testing" -ForegroundColor Cyan
    Write-Host "Testing login with $($global:registeredUsers.Count) registered users..." -ForegroundColor Yellow
    
    # Phase 2: Test login with registered users
    $jobs = @()
    
    $scriptBlock = {
        param($UserCredentials, $RequestsPerUser, $BaseUrl)
        
        function Test-UserLogin {
            param($UserCredentials, $RequestNum, $BaseUrl)
            
            $testData = @{
                username = $UserCredentials.Username
                password = $UserCredentials.Password
            } | ConvertTo-Json
            
            $headers = @{
                'Content-Type' = 'application/json'
                'Accept' = 'application/json'
            }
            
            $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
            
            try {
                $response = Invoke-WebRequest -Uri "$BaseUrl/api/auth/login" -Method POST -Body $testData -Headers $headers -ErrorAction Stop
                $stopwatch.Stop()
                
                return @{
                    Operation = "Login"
                    UserId = $UserCredentials.UserId
                    Username = $UserCredentials.Username
                    RequestNum = $RequestNum
                    ResponseTime = $stopwatch.ElapsedMilliseconds
                    StatusCode = $response.StatusCode
                    Success = $true
                    Timestamp = Get-Date
                    ResponseSize = $response.RawContentLength
                }
            } catch {
                $stopwatch.Stop()
                
                return @{
                    Operation = "Login"
                    UserId = $UserCredentials.UserId
                    Username = $UserCredentials.Username
                    RequestNum = $RequestNum
                    ResponseTime = $stopwatch.ElapsedMilliseconds
                    StatusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { 0 }
                    Success = $false
                    Error = $_.Exception.Message
                    Timestamp = Get-Date
                    ResponseSize = 0
                }
            }
        }
        
        $results = @()
        for ($i = 1; $i -le $RequestsPerUser; $i++) {
            $result = Test-UserLogin -UserCredentials $UserCredentials -RequestNum $i -BaseUrl $BaseUrl
            $results += $result
            Start-Sleep -Milliseconds 200  # Small delay between requests
        }
        
        return $results
    }
    
    # Start concurrent login tests
    foreach ($user in $global:registeredUsers) {
        $job = Start-Job -ScriptBlock $scriptBlock -ArgumentList $user, $RequestsPerUser, $BaseUrl
        $jobs += $job
    }
    
    # Wait for all jobs to complete
    Write-Host "Executing concurrent login tests..." -ForegroundColor Yellow
    
    $jobResults = @()
    foreach ($job in $jobs) {
        $result = Wait-Job $job | Receive-Job
        $jobResults += $result
        Remove-Job $job
    }
    
    # Add job results to global results
    $global:results += $jobResults
    $global:totalRequests += $jobResults.Count
    $global:successfulRequests += ($jobResults | Where-Object { $_.Success }).Count
    $global:errors += $jobResults | Where-Object { -not $_.Success }
}

function Show-DetailedAnalysis {
    $endTime = Get-Date
    $totalDuration = ($endTime - $global:startTime).TotalSeconds
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "COMPREHENSIVE PERFORMANCE ANALYSIS" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    
    # Overall Statistics
    Write-Host "Overall Test Summary:" -ForegroundColor Yellow
    Write-Host "- Total Duration: $([math]::Round($totalDuration, 2)) seconds" -ForegroundColor White
    Write-Host "- Total Operations: $global:totalRequests" -ForegroundColor White
    Write-Host "- Successful Operations: $global:successfulRequests" -ForegroundColor White
    Write-Host "- Failed Operations: $($global:errors.Count)" -ForegroundColor White
    Write-Host "- Overall Success Rate: $([math]::Round(($global:successfulRequests / $global:totalRequests) * 100, 2))%" -ForegroundColor White
    Write-Host ""
    
    # Registration vs Login Analysis
    $registrationResults = $global:results | Where-Object { $_.Operation -eq "Registration" }
    $loginResults = $global:results | Where-Object { $_.Operation -eq "Login" }
    
    Write-Host "Operation Breakdown:" -ForegroundColor Yellow
    Write-Host "- Registration Operations: $($registrationResults.Count)" -ForegroundColor White
    Write-Host "- Login Operations: $($loginResults.Count)" -ForegroundColor White
    
    if ($registrationResults.Count -gt 0) {
        $regSuccessRate = (($registrationResults | Where-Object { $_.Success }).Count / $registrationResults.Count) * 100
        $regAvgTime = ($registrationResults | Where-Object { $_.Success } | Measure-Object -Property ResponseTime -Average).Average
        Write-Host "- Registration Success Rate: $([math]::Round($regSuccessRate, 2))%" -ForegroundColor White
        Write-Host "- Registration Avg Response Time: $([math]::Round($regAvgTime, 2))ms" -ForegroundColor White
    }
    
    if ($loginResults.Count -gt 0) {
        $loginSuccessRate = (($loginResults | Where-Object { $_.Success }).Count / $loginResults.Count) * 100
        $loginAvgTime = ($loginResults | Where-Object { $_.Success } | Measure-Object -Property ResponseTime -Average).Average
        Write-Host "- Login Success Rate: $([math]::Round($loginSuccessRate, 2))%" -ForegroundColor White
        Write-Host "- Login Avg Response Time: $([math]::Round($loginAvgTime, 2))ms" -ForegroundColor White
    }
    Write-Host ""
    
    # Throughput Analysis
    $throughput = $global:totalRequests / $totalDuration
    Write-Host "Throughput Analysis:" -ForegroundColor Yellow
    Write-Host "- Operations per Second: $([math]::Round($throughput, 2))" -ForegroundColor White
    Write-Host "- Operations per Minute: $([math]::Round($throughput * 60, 2))" -ForegroundColor White
    Write-Host ""
    
    # Response Time Analysis (successful requests only)
    $successfulResults = $global:results | Where-Object { $_.Success }
    if ($successfulResults.Count -gt 0) {
        $responseTimes = $successfulResults | ForEach-Object { $_.ResponseTime }
        $avgResponseTime = ($responseTimes | Measure-Object -Average).Average
        $minResponseTime = ($responseTimes | Measure-Object -Minimum).Minimum
        $maxResponseTime = ($responseTimes | Measure-Object -Maximum).Maximum
        
        Write-Host "Response Time Analysis (ms):" -ForegroundColor Yellow
        Write-Host "- Average: $([math]::Round($avgResponseTime, 2))" -ForegroundColor White
        Write-Host "- Minimum: $minResponseTime" -ForegroundColor White
        Write-Host "- Maximum: $maxResponseTime" -ForegroundColor White
        
        if ($responseTimes.Count -gt 2) {
            $sortedTimes = $responseTimes | Sort-Object
            $p95Index = [math]::Floor($sortedTimes.Count * 0.95)
            $p99Index = [math]::Floor($sortedTimes.Count * 0.99)
            Write-Host "- 95th Percentile: $($sortedTimes[$p95Index])" -ForegroundColor White
            Write-Host "- 99th Percentile: $($sortedTimes[$p99Index])" -ForegroundColor White
        }
        Write-Host ""
    }
    
    # Error Analysis
    if ($global:errors.Count -gt 0) {
        Write-Host "Error Analysis:" -ForegroundColor Red
        $errorsByOperation = $global:errors | Group-Object Operation
        foreach ($group in $errorsByOperation) {
            Write-Host "- $($group.Name) Errors: $($group.Count)" -ForegroundColor White
        }
        
        $errorsByStatus = $global:errors | Group-Object StatusCode
        foreach ($group in $errorsByStatus) {
            Write-Host "- HTTP $($group.Name): $($group.Count) errors" -ForegroundColor White
        }
        Write-Host ""
    }
    
    # Performance Benchmarks
    Write-Host "Performance Assessment:" -ForegroundColor Yellow
    
    # Response Time Assessment
    if ($successfulResults.Count -gt 0) {
        $avgTime = ($successfulResults | Measure-Object -Property ResponseTime -Average).Average
        if ($avgTime -lt 300) {
            Write-Host "- Response Time: EXCELLENT (< 300ms avg)" -ForegroundColor Green
        } elseif ($avgTime -lt 500) {
            Write-Host "- Response Time: GOOD (< 500ms avg)" -ForegroundColor Yellow
        } elseif ($avgTime -lt 1000) {
            Write-Host "- Response Time: ACCEPTABLE (< 1000ms avg)" -ForegroundColor Yellow
        } else {
            Write-Host "- Response Time: NEEDS IMPROVEMENT (> 1000ms avg)" -ForegroundColor Red
        }
    }
    
    # Throughput Assessment
    if ($throughput -gt 15) {
        Write-Host "- Throughput: EXCELLENT (> 15 ops/s)" -ForegroundColor Green
    } elseif ($throughput -gt 10) {
        Write-Host "- Throughput: GOOD (> 10 ops/s)" -ForegroundColor Yellow
    } elseif ($throughput -gt 5) {
        Write-Host "- Throughput: ACCEPTABLE (> 5 ops/s)" -ForegroundColor Yellow
    } else {
        Write-Host "- Throughput: NEEDS IMPROVEMENT (< 5 ops/s)" -ForegroundColor Red
    }
    
    # Error Rate Assessment
    $errorRate = ($global:errors.Count / $global:totalRequests) * 100
    if ($errorRate -eq 0) {
        Write-Host "- Reliability: EXCELLENT (0% errors)" -ForegroundColor Green
    } elseif ($errorRate -lt 2) {
        Write-Host "- Reliability: GOOD (< 2% errors)" -ForegroundColor Green
    } elseif ($errorRate -lt 5) {
        Write-Host "- Reliability: ACCEPTABLE (< 5% errors)" -ForegroundColor Yellow
    } else {
        Write-Host "- Reliability: CRITICAL (> 5% errors)" -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "Bottleneck Analysis:" -ForegroundColor Yellow
    
    # Identify potential bottlenecks
    if ($loginResults.Count -gt 0) {
        $loginTimes = $loginResults | Where-Object { $_.Success } | ForEach-Object { $_.ResponseTime }
        if ($loginTimes.Count -gt 0) {
            $avgLoginTime = ($loginTimes | Measure-Object -Average).Average
            if ($avgLoginTime -gt 500) {
                Write-Host "- LOGIN BOTTLENECK: Authentication taking >500ms on average" -ForegroundColor Red
                Write-Host "  Recommendations: Optimize password hashing, database indexing" -ForegroundColor Gray
            }
        }
    }
    
    if ($registrationResults.Count -gt 0) {
        $regTimes = $registrationResults | Where-Object { $_.Success } | ForEach-Object { $_.ResponseTime }
        if ($regTimes.Count -gt 0) {
            $avgRegTime = ($regTimes | Measure-Object -Average).Average
            if ($avgRegTime -gt 800) {
                Write-Host "- REGISTRATION BOTTLENECK: User creation taking >800ms on average" -ForegroundColor Red
                Write-Host "  Recommendations: Database write optimization, async processing" -ForegroundColor Gray
            }
        }
    }
    
    if ($errorRate -gt 5) {
        Write-Host "- HIGH ERROR RATE: >5% of requests failing" -ForegroundColor Red
        Write-Host "  Recommendations: Review error logs, check system resources" -ForegroundColor Gray
    }
}

# Main execution
try {
    Start-ComprehensiveLoadTest
    Show-DetailedAnalysis
    
    # Export results
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $resultsPath = "C:\Users\User\Desktop\QAproject\performance-testing\comprehensive_results_$timestamp.csv"
    $global:results | Export-Csv -Path $resultsPath -NoTypeInformation
    Write-Host "Detailed results exported to: $resultsPath" -ForegroundColor Green
    
} catch {
    Write-Host "Error during load testing: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host $_.ScriptStackTrace -ForegroundColor Red
}