# PowerShell Load Testing Script for QA Project Authentication API
# Alternative approach when JMeter path is not configured

param(
    [int]$ConcurrentUsers = 10,
    [int]$RequestsPerUser = 5,
    [string]$BaseUrl = "http://localhost:8080",
    [string]$TestType = "login"
)

# Performance metrics collection
$global:results = @()
$global:errors = @()
$global:startTime = Get-Date
$global:totalRequests = 0
$global:successfulRequests = 0

function Write-TestHeader {
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "QA Project Load Testing with PowerShell" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Test Configuration:" -ForegroundColor Yellow
    Write-Host "- Concurrent Users: $ConcurrentUsers" -ForegroundColor White
    Write-Host "- Requests per User: $RequestsPerUser" -ForegroundColor White
    Write-Host "- Total Requests: $($ConcurrentUsers * $RequestsPerUser)" -ForegroundColor White
    Write-Host "- Target URL: $BaseUrl" -ForegroundColor White
    Write-Host "- Test Type: $TestType" -ForegroundColor White
    Write-Host ""
}

function Test-LoginEndpoint {
    param($UserId, $RequestNum)
    
    $testData = @{
        username = "loadtest$UserId$RequestNum"
        password = "LoadTest123!"
    } | ConvertTo-Json
    
    $headers = @{
        'Content-Type' = 'application/json'
        'Accept' = 'application/json'
    }
    
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    
    try {
        $response = Invoke-WebRequest -Uri "$BaseUrl/api/auth/login" -Method POST -Body $testData -Headers $headers -ErrorAction Stop
        $stopwatch.Stop()
        
        $result = @{
            UserId = $UserId
            RequestNum = $RequestNum
            ResponseTime = $stopwatch.ElapsedMilliseconds
            StatusCode = $response.StatusCode
            Success = $true
            Timestamp = Get-Date
            ResponseSize = $response.RawContentLength
        }
        
        $global:successfulRequests++
        
    } catch {
        $stopwatch.Stop()
        
        $result = @{
            UserId = $UserId
            RequestNum = $RequestNum
            ResponseTime = $stopwatch.ElapsedMilliseconds
            StatusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { 0 }
            Success = $false
            Error = $_.Exception.Message
            Timestamp = Get-Date
            ResponseSize = 0
        }
        
        $global:errors += $result
    }
    
    $global:results += $result
    $global:totalRequests++
    
    # Real-time progress
    if ($global:totalRequests % 10 -eq 0) {
        Write-Host "Completed $global:totalRequests requests..." -ForegroundColor Cyan
    }
    
    return $result
}

function Test-RegisterEndpoint {
    param($UserId, $RequestNum)
    
    $uniqueId = Get-Random -Minimum 1000 -Maximum 9999
    $testData = @{
        username = "loadtest$UserId$RequestNum$uniqueId"
        email = "loadtest$UserId$RequestNum$uniqueId@testdomain.com"
        password = "LoadTest123!"
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
            UserId = $UserId
            RequestNum = $RequestNum
            ResponseTime = $stopwatch.ElapsedMilliseconds
            StatusCode = $response.StatusCode
            Success = $true
            Timestamp = Get-Date
            ResponseSize = $response.RawContentLength
        }
        
        $global:successfulRequests++
        
    } catch {
        $stopwatch.Stop()
        
        $result = @{
            UserId = $UserId
            RequestNum = $RequestNum
            ResponseTime = $stopwatch.ElapsedMilliseconds
            StatusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { 0 }
            Success = $false
            Error = $_.Exception.Message
            Timestamp = Get-Date
            ResponseSize = 0
        }
        
        $global:errors += $result
    }
    
    $global:results += $result
    $global:totalRequests++
    
    return $result
}

function Start-LoadTest {
    Write-TestHeader
    
    Write-Host "Starting load test at $(Get-Date)..." -ForegroundColor Green
    Write-Host ""
    
    # Create script blocks for concurrent execution
    $scriptBlock = {
        param($UserId, $RequestsPerUser, $BaseUrl, $TestType)
        
        # Import functions into the job context
        function Test-LoginEndpoint {
            param($UserId, $RequestNum, $BaseUrl)
            
            $testData = @{
                username = "loadtest$UserId$RequestNum"
                password = "LoadTest123!"
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
                    UserId = $UserId
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
                    UserId = $UserId
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
            $result = Test-LoginEndpoint -UserId $UserId -RequestNum $i -BaseUrl $BaseUrl
            $results += $result
            Start-Sleep -Milliseconds 100  # Small delay between requests
        }
        
        return $results
    }
    
    # Start concurrent jobs
    $jobs = @()
    for ($userId = 1; $userId -le $ConcurrentUsers; $userId++) {
        $job = Start-Job -ScriptBlock $scriptBlock -ArgumentList $userId, $RequestsPerUser, $BaseUrl, $TestType
        $jobs += $job
    }
    
    # Wait for all jobs to complete and collect results
    Write-Host "Waiting for all concurrent users to complete..." -ForegroundColor Yellow
    
    $jobResults = @()
    foreach ($job in $jobs) {
        $result = Wait-Job $job | Receive-Job
        $jobResults += $result
        Remove-Job $job
    }
    
    # Process all results
    $global:results = $jobResults
    $global:totalRequests = $global:results.Count
    $global:successfulRequests = ($global:results | Where-Object { $_.Success }).Count
    $global:errors = $global:results | Where-Object { -not $_.Success }
}

function Show-PerformanceAnalysis {
    $endTime = Get-Date
    $totalDuration = ($endTime - $global:startTime).TotalSeconds
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "PERFORMANCE TEST RESULTS" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    
    # Basic Statistics
    Write-Host "Test Summary:" -ForegroundColor Yellow
    Write-Host "- Total Duration: $([math]::Round($totalDuration, 2)) seconds" -ForegroundColor White
    Write-Host "- Total Requests: $global:totalRequests" -ForegroundColor White
    Write-Host "- Successful Requests: $global:successfulRequests" -ForegroundColor White
    Write-Host "- Failed Requests: $($global:errors.Count)" -ForegroundColor White
    Write-Host "- Success Rate: $([math]::Round(($global:successfulRequests / $global:totalRequests) * 100, 2))%" -ForegroundColor White
    Write-Host "- Error Rate: $([math]::Round(($global:errors.Count / $global:totalRequests) * 100, 2))%" -ForegroundColor White
    Write-Host ""
    
    # Throughput Analysis
    $throughput = $global:totalRequests / $totalDuration
    Write-Host "Throughput Analysis:" -ForegroundColor Yellow
    Write-Host "- Requests per Second: $([math]::Round($throughput, 2))" -ForegroundColor White
    Write-Host "- Requests per Minute: $([math]::Round($throughput * 60, 2))" -ForegroundColor White
    Write-Host ""
    
    # Response Time Analysis
    $responseTimes = $global:results | Where-Object { $_.Success } | ForEach-Object { $_.ResponseTime }
    
    if ($responseTimes.Count -gt 0) {
        $avgResponseTime = ($responseTimes | Measure-Object -Average).Average
        $minResponseTime = ($responseTimes | Measure-Object -Minimum).Minimum
        $maxResponseTime = ($responseTimes | Measure-Object -Maximum).Maximum
        $p95ResponseTime = $responseTimes | Sort-Object | Select-Object -Index ([math]::Floor($responseTimes.Count * 0.95))
        $p99ResponseTime = $responseTimes | Sort-Object | Select-Object -Index ([math]::Floor($responseTimes.Count * 0.99))
        
        Write-Host "Response Time Analysis (ms):" -ForegroundColor Yellow
        Write-Host "- Average: $([math]::Round($avgResponseTime, 2))" -ForegroundColor White
        Write-Host "- Minimum: $minResponseTime" -ForegroundColor White
        Write-Host "- Maximum: $maxResponseTime" -ForegroundColor White
        Write-Host "- 95th Percentile: $p95ResponseTime" -ForegroundColor White
        Write-Host "- 99th Percentile: $p99ResponseTime" -ForegroundColor White
        Write-Host ""
    }
    
    # Error Analysis
    if ($global:errors.Count -gt 0) {
        Write-Host "Error Analysis:" -ForegroundColor Red
        $errorGroups = $global:errors | Group-Object StatusCode
        foreach ($group in $errorGroups) {
            Write-Host "- HTTP $($group.Name): $($group.Count) errors" -ForegroundColor White
        }
        Write-Host ""
    }
    
    # Performance Benchmarks
    Write-Host "Performance Benchmarks:" -ForegroundColor Yellow
    if ($avgResponseTime -lt 500) {
        Write-Host "- Response Time: EXCELLENT (< 500ms)" -ForegroundColor Green
    } elseif ($avgResponseTime -lt 1000) {
        Write-Host "- Response Time: GOOD (< 1000ms)" -ForegroundColor Yellow
    } else {
        Write-Host "- Response Time: NEEDS IMPROVEMENT (> 1000ms)" -ForegroundColor Red
    }
    
    if ($throughput -gt 20) {
        Write-Host "- Throughput: EXCELLENT (> 20 req/s)" -ForegroundColor Green
    } elseif ($throughput -gt 10) {
        Write-Host "- Throughput: GOOD (> 10 req/s)" -ForegroundColor Yellow
    } else {
        Write-Host "- Throughput: NEEDS IMPROVEMENT (< 10 req/s)" -ForegroundColor Red
    }
    
    $errorRate = ($global:errors.Count / $global:totalRequests) * 100
    if ($errorRate -lt 5) {
        Write-Host "- Error Rate: EXCELLENT (< 5%)" -ForegroundColor Green
    } elseif ($errorRate -lt 10) {
        Write-Host "- Error Rate: ACCEPTABLE (< 10%)" -ForegroundColor Yellow
    } else {
        Write-Host "- Error Rate: CRITICAL (> 10%)" -ForegroundColor Red
    }
}

function Export-Results {
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $resultsPath = "C:\Users\User\Desktop\QAproject\performance-testing\results_$timestamp.csv"
    
    $global:results | Export-Csv -Path $resultsPath -NoTypeInformation
    Write-Host ""
    Write-Host "Results exported to: $resultsPath" -ForegroundColor Green
}

# Main execution
try {
    Start-LoadTest
    Show-PerformanceAnalysis
    Export-Results
} catch {
    Write-Host "Error during load testing: $($_.Exception.Message)" -ForegroundColor Red
}