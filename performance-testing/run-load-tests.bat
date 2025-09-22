@echo off
REM Performance Testing Script for QA Project
REM Executes JMeter load tests and generates comprehensive reports

echo ========================================
echo QA Project Performance Testing Suite
echo ========================================

REM Set environment variables
set JMETER_HOME=C:\apache-jmeter-5.6.3
set TEST_PLAN=QA-Project-Load-Test.jmx
set RESULTS_DIR=results
set TIMESTAMP=%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

REM Create results directory
if not exist %RESULTS_DIR% mkdir %RESULTS_DIR%

REM Start Spring Boot application (if not already running)
echo Checking if Spring Boot application is running...
curl -s http://localhost:8080/api/auth/test > nul
if %errorlevel% neq 0 (
    echo Starting Spring Boot application...
    cd ..\backend
    start "Spring Boot App" cmd /c "mvn spring-boot:run"
    timeout /t 30 /nobreak > nul
    cd ..\performance-testing
)

echo ========================================
echo Running Load Tests...
echo ========================================

REM Execute JMeter test plan
echo Test execution starting at %date% %time%

REM Run Normal Load Test (50 users)
echo Running Normal Load Test - 50 concurrent users...
%JMETER_HOME%\bin\jmeter -n -t %TEST_PLAN% ^
    -l %RESULTS_DIR%\normal-load-%TIMESTAMP%.jtl ^
    -e -o %RESULTS_DIR%\normal-load-report-%TIMESTAMP% ^
    -Jusers=50 -Jrampup=30 -Jduration=300

REM Run Peak Load Test (100 users)
echo Running Peak Load Test - 100 concurrent users...
%JMETER_HOME%\bin\jmeter -n -t %TEST_PLAN% ^
    -l %RESULTS_DIR%\peak-load-%TIMESTAMP%.jtl ^
    -e -o %RESULTS_DIR%\peak-load-report-%TIMESTAMP% ^
    -Jusers=100 -Jrampup=10 -Jduration=300

REM Run Stress Test (200 users)
echo Running Stress Test - 200 concurrent users...
%JMETER_HOME%\bin\jmeter -n -t %TEST_PLAN% ^
    -l %RESULTS_DIR%\stress-test-%TIMESTAMP%.jtl ^
    -e -o %RESULTS_DIR%\stress-test-report-%TIMESTAMP% ^
    -Jusers=200 -Jrampup=5 -Jduration=180

echo ========================================
echo Load Testing Complete!
echo ========================================

echo Results saved in: %RESULTS_DIR%
echo HTML reports generated for each test scenario
echo.
echo Key files:
echo - Normal Load: %RESULTS_DIR%\normal-load-report-%TIMESTAMP%\index.html
echo - Peak Load: %RESULTS_DIR%\peak-load-report-%TIMESTAMP%\index.html  
echo - Stress Test: %RESULTS_DIR%\stress-test-report-%TIMESTAMP%\index.html
echo.

pause