# SISM Backend Performance Benchmarks

## Overview

This document defines the performance requirements and benchmarking strategy for the SISM (Strategic Indicator Management System) backend API.

## Performance Requirements

### Response Time Targets

| Operation Type | Target | Rationale |
|---------------|--------|-----------|
| Authentication | < 500ms | User login should feel instant |
| Simple Queries | < 200ms | Single entity retrieval (by ID) |
| List Queries | < 300ms | Paginated list endpoints |
| Write Operations | < 300ms | Create/Update single entity |
| Complex Queries | < 1000ms | Queries with multiple joins |
| Bulk Operations | < 2000ms | Batch processing operations |
| Health Check | < 100ms | System health monitoring |

### Throughput Targets

- **Concurrent Users**: Support 50+ concurrent users
- **Requests per Second**: Handle 100+ requests/second
- **Database Connections**: Pool size 20-50 connections

### Resource Utilization

- **CPU Usage**: < 70% under normal load
- **Memory Usage**: < 1GB heap for typical workload
- **Database Connections**: < 80% pool utilization

## Benchmark Test Suite

### Location

Performance benchmark tests are located at:
```
sism-backend/src/test/java/com/sism/performance/PerformanceBenchmarkTest.java
```

### Running Benchmarks

**Run all performance tests:**
```bash
cd sism-backend
mvn test -Dtest=PerformanceBenchmarkTest
```

**Run specific benchmark:**
```bash
mvn test -Dtest=PerformanceBenchmarkTest#testAuthenticationPerformance
```

**Run with detailed output:**
```bash
mvn test -Dtest=PerformanceBenchmarkTest -X
```

### Benchmark Tests

#### 1. Authentication Performance
- **Test**: `testAuthenticationPerformance()`
- **Target**: < 500ms
- **Validates**: JWT token generation and user authentication

#### 2. Simple Query Performance
- **Test**: `testSimpleQueryPerformance()`
- **Target**: < 200ms
- **Validates**: Single entity retrieval by ID

#### 3. List Query Performance
- **Test**: `testListQueryPerformance()`
- **Target**: < 300ms
- **Validates**: Paginated list queries with 20 items

#### 4. Write Operation Performance
- **Test**: `testWriteOperationPerformance()`
- **Target**: < 300ms
- **Validates**: Entity creation with validation

#### 5. Complex Query Performance
- **Test**: `testComplexQueryPerformance()`
- **Target**: < 1000ms
- **Validates**: Queries with multiple table joins

#### 6. Concurrent Request Performance
- **Test**: `testConcurrentRequestPerformance()`
- **Target**: < 2000ms for 10 requests
- **Validates**: System behavior under concurrent load

#### 7. Update Operation Performance
- **Test**: `testUpdateOperationPerformance()`
- **Target**: < 300ms
- **Validates**: Entity updates with validation

#### 8. Health Check Performance
- **Test**: `testHealthCheckPerformance()`
- **Target**: < 100ms
- **Validates**: System health endpoint responsiveness

## Performance Monitoring

### Spring Boot Actuator

The application includes Spring Boot Actuator for runtime monitoring:

**Endpoints:**
- `/actuator/health` - Application health status
- `/actuator/metrics` - Application metrics
- `/actuator/metrics/http.server.requests` - HTTP request metrics
- `/actuator/metrics/jvm.memory.used` - JVM memory usage
- `/actuator/metrics/jdbc.connections.active` - Database connections

**Enable in application.yml:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  endpoint:
    health:
      show-details: always
```

### Database Query Performance

**Monitor slow queries:**
```yaml
spring:
  jpa:
    properties:
      hibernate:
        show_sql: true
        format_sql: true
        use_sql_comments: true
        generate_statistics: true
```

**PostgreSQL slow query log:**
```sql
-- Enable slow query logging
ALTER SYSTEM SET log_min_duration_statement = 1000; -- Log queries > 1s
SELECT pg_reload_conf();

-- View slow queries
SELECT query, calls, total_time, mean_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
```

## Performance Optimization Strategies

### 1. Database Optimization

**Indexing:**
- Add indexes on frequently queried columns
- Use composite indexes for multi-column queries
- Monitor index usage with `pg_stat_user_indexes`

**Query Optimization:**
- Use pagination for large result sets
- Avoid N+1 query problems with `@EntityGraph`
- Use native queries for complex operations
- Implement database-level caching

**Connection Pooling:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 2. Application Optimization

**Caching:**
```java
@Cacheable(value = "indicators", key = "#id")
public IndicatorVO getIndicatorById(Long id) {
    // ...
}
```

**Async Processing:**
```java
@Async
public CompletableFuture<List<IndicatorVO>> getIndicatorsAsync() {
    // ...
}
```

**Lazy Loading:**
- Use `@Lazy` for optional dependencies
- Implement pagination for large datasets
- Use DTOs to avoid loading unnecessary data

### 3. JVM Tuning

**Recommended JVM Options:**
```bash
java -Xms512m -Xmx1024m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -jar sism-backend.jar
```

## Performance Testing in CI/CD

### GitHub Actions Integration

Add performance tests to CI pipeline:

```yaml
name: Performance Tests

on:
  push:
    branches: [ main, develop ]
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM

jobs:
  performance:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run Performance Benchmarks
        run: |
          cd sism-backend
          mvn test -Dtest=PerformanceBenchmarkTest
      
      - name: Upload Performance Results
        uses: actions/upload-artifact@v3
        with:
          name: performance-results
          path: sism-backend/target/surefire-reports/
```

## Performance Regression Detection

### Baseline Metrics

Establish baseline performance metrics:

```bash
# Run benchmarks and save results
mvn test -Dtest=PerformanceBenchmarkTest > performance-baseline.txt

# Compare against baseline in CI
mvn test -Dtest=PerformanceBenchmarkTest > performance-current.txt
diff performance-baseline.txt performance-current.txt
```

### Alerting Thresholds

Set up alerts for performance degradation:

- **Response Time**: Alert if > 20% slower than baseline
- **Error Rate**: Alert if > 1% of requests fail
- **Resource Usage**: Alert if CPU > 80% or Memory > 90%

## Load Testing

### Apache JMeter

For comprehensive load testing, use Apache JMeter:

**Test Plan:**
1. Ramp up to 50 concurrent users over 5 minutes
2. Maintain 50 users for 10 minutes
3. Ramp down over 2 minutes

**Key Metrics:**
- Average response time
- 95th percentile response time
- Throughput (requests/second)
- Error rate

### Example JMeter Test

```bash
# Install JMeter
brew install jmeter  # macOS
# or download from https://jmeter.apache.org/

# Run load test
jmeter -n -t sism-load-test.jmx -l results.jtl -e -o report/
```

## Troubleshooting Performance Issues

### Common Issues

**1. Slow Database Queries**
- Check query execution plans: `EXPLAIN ANALYZE`
- Add missing indexes
- Optimize JOIN operations
- Use database connection pooling

**2. High Memory Usage**
- Check for memory leaks with heap dumps
- Optimize entity loading strategies
- Implement pagination
- Use DTOs instead of entities in responses

**3. High CPU Usage**
- Profile application with JProfiler or VisualVM
- Optimize algorithms and loops
- Use caching for expensive operations
- Implement async processing

**4. Slow Response Times**
- Enable HTTP compression
- Implement CDN for static assets
- Use database query caching
- Optimize JSON serialization

### Profiling Tools

**JProfiler:**
```bash
java -agentpath:/path/to/jprofiler/bin/linux-x64/libjprofilerti.so=port=8849 \
     -jar sism-backend.jar
```

**VisualVM:**
```bash
jvisualvm
# Connect to running application
```

**Spring Boot Actuator:**
```bash
# Get metrics
curl http://localhost:8080/actuator/metrics

# Get specific metric
curl http://localhost:8080/actuator/metrics/http.server.requests
```

## Performance Checklist

Before deploying to production:

- [ ] All performance benchmarks pass
- [ ] No slow queries (> 1s) identified
- [ ] Database indexes optimized
- [ ] Connection pool configured
- [ ] JVM heap size appropriate
- [ ] Caching strategy implemented
- [ ] Monitoring and alerting configured
- [ ] Load testing completed successfully
- [ ] Performance regression tests in CI/CD

## References

- [Spring Boot Performance Tuning](https://spring.io/guides/gs/spring-boot/)
- [PostgreSQL Performance Tips](https://wiki.postgresql.org/wiki/Performance_Optimization)
- [JVM Performance Tuning](https://docs.oracle.com/en/java/javase/17/gctuning/)
- [Apache JMeter User Manual](https://jmeter.apache.org/usermanual/index.html)

---

*Last Updated: 2026-02-14*
*Version: 1.0*
