# Performance Testing Documentation
# QA Project Load Testing with Apache JMeter

## Overview
This comprehensive performance testing suite is designed to evaluate the authentication API endpoints under various load conditions using Apache JMeter.

## Test Strategy

### Critical API Endpoint Selected: `/api/auth/login`
**Rationale:**
- Most frequently accessed endpoint in production
- Involves complex operations (database queries, password verification, token generation)
- Critical for user experience and application security
- Representative of real-world usage patterns

## Load Testing Scenarios

### 1. Normal Load Test
- **Concurrent Users:** 50
- **Ramp-up Period:** 30 seconds
- **Test Duration:** 5 minutes
- **Loops per User:** 5
- **Purpose:** Baseline performance under normal conditions

### 2. Peak Load Test  
- **Concurrent Users:** 100
- **Ramp-up Period:** 10 seconds
- **Test Duration:** 5 minutes
- **Loops per User:** 3
- **Purpose:** Performance during peak business hours

### 3. Stress Test
- **Concurrent Users:** 200
- **Ramp-up Period:** 5 seconds
- **Test Duration:** 3 minutes
- **Loops per User:** 2
- **Purpose:** Identify breaking points and system limits

### 4. Spike Test
- **Concurrent Users:** 500
- **Ramp-up Period:** 1 second
- **Test Duration:** 2 minutes
- **Loops per User:** 1
- **Purpose:** Evaluate system behavior under sudden load spikes

## Key Performance Metrics

### Response Time Metrics
- **Average Response Time:** Target < 500ms
- **95th Percentile:** Target < 1000ms
- **99th Percentile:** Target < 2000ms
- **Maximum Response Time:** Target < 5000ms

### Throughput Metrics
- **Transactions per Second (TPS):** Target > 20 TPS
- **Requests per Minute:** Target > 1200 RPM
- **Data Throughput:** Monitor KB/sec

### Error Rate Metrics
- **Error Rate:** Target < 5%
- **Success Rate:** Target > 95%
- **HTTP Status Codes:** Monitor distribution

### Resource Utilization
- **CPU Usage:** Monitor server CPU during tests
- **Memory Usage:** Track heap and non-heap memory
- **Database Connections:** Monitor connection pool usage
- **Network I/O:** Track bandwidth utilization

## Test Data Strategy

### Dynamic User Generation
- Random usernames using patterns: `loadtest{random}{number}`
- Random emails: `loadtest{random}@testdomain.com`
- Consistent passwords: `LoadTest123!`
- Mix of valid and invalid credentials for realistic scenarios

### Parameterized Testing
- CSV file with predefined test data
- Mix of successful and failed authentication attempts
- Edge cases for validation testing

## JMeter Test Plan Components

### Thread Groups
1. **Login Load Test Group**
   - Primary authentication testing
   - Concurrent user simulation
   - Response validation

2. **Registration Load Test Group**
   - New user creation testing
   - Unique constraint validation
   - Database write performance

### HTTP Request Configuration
- **Protocol:** HTTP
- **Server:** localhost
- **Port:** 8080
- **Content-Type:** application/json
- **Timeout Settings:** Connect: 10s, Response: 30s

### Assertions and Validations
- HTTP response code validation (200, 400, 401)
- JSON response structure verification
- Token extraction for successful logins
- Error message validation

### Listeners and Reporting
- **View Results Tree:** Detailed request/response analysis
- **Summary Report:** Aggregate performance metrics
- **Graph Results:** Visual performance trends
- **Response Time Graph:** Time-based performance analysis

## Execution Instructions

### Prerequisites
1. Apache JMeter 5.6.3 or later
2. Java 8+ runtime environment
3. Spring Boot application running on localhost:8080

### Manual Execution
```bash
# Navigate to performance testing directory
cd C:\Users\User\Desktop\QAproject\performance-testing

# Run JMeter GUI mode for test development
jmeter -t QA-Project-Load-Test.jmx

# Run JMeter CLI mode for automated testing
jmeter -n -t QA-Project-Load-Test.jmx -l results.jtl -e -o report
```

### Automated Execution
```bash
# Execute comprehensive test suite
run-load-tests.bat
```

## Performance Analysis Framework

### Response Time Analysis
- Monitor average, median, 95th and 99th percentiles
- Identify response time degradation patterns
- Compare across different load levels

### Throughput Analysis
- Measure requests per second at various load levels
- Identify maximum sustainable throughput
- Monitor throughput degradation under stress

### Error Rate Analysis
- Track error patterns and rates
- Identify error types (validation vs. system errors)
- Monitor error rate correlation with load

### Resource Utilization Correlation
- Correlate performance metrics with system resources
- Identify resource bottlenecks
- Monitor database performance under load

## Expected Results and Benchmarks

### Baseline Performance Targets
- **Login Response Time:** < 200ms average
- **Registration Response Time:** < 500ms average
- **Error Rate:** < 2% under normal load
- **Throughput:** > 50 TPS for login operations

### Performance Degradation Thresholds
- **Warning Level:** Response time > 500ms
- **Critical Level:** Response time > 1000ms
- **Failure Level:** Error rate > 10%

## Bottleneck Identification Strategy

### Database Performance
- Monitor query execution times
- Analyze connection pool utilization
- Check for lock contention

### Application Performance  
- Monitor JVM memory usage
- Track garbage collection impact
- Analyze thread pool utilization

### Network Performance
- Monitor network latency
- Track bandwidth utilization
- Analyze connection establishment times

## Performance Optimization Recommendations

### Database Optimizations
- Index optimization for user lookups
- Connection pool tuning
- Query optimization for authentication

### Application Optimizations
- JVM heap size tuning
- Connection pooling optimization
- Caching strategy implementation

### Infrastructure Optimizations
- Load balancer configuration
- CDN implementation for static resources
- Database read replica implementation

## Continuous Performance Testing

### Integration with CI/CD
- Automated performance regression testing
- Performance thresholds as build gates
- Trend analysis over time

### Monitoring and Alerting
- Real-time performance monitoring
- Automated alert thresholds
- Performance dashboard implementation