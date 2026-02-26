# Property-Based Tests for Redis Integration

This directory contains property-based tests using jqwik to verify the correctness of Redis integration components.

## Prerequisites

### Docker Required

All property-based tests in this directory use **Testcontainers** to spin up a Redis instance for testing. You must have Docker installed and running on your machine.

**Install Docker:**
- Windows/Mac: [Docker Desktop](https://www.docker.com/products/docker-desktop)
- Linux: Follow your distribution's Docker installation guide

**Verify Docker is running:**
```bash
docker ps
```

If Docker is not running, the tests will fail with:
```
IllegalStateException: Previous attempts to find a Docker environment failed
```

## Running the Tests

### Run all property tests
```bash
./mvnw test -Dtest="com.sism.property.*"
```

### Run specific property test class
```bash
./mvnw test -Dtest=RedisRateLimiterPropertyTest
./mvnw test -Dtest=RedisStorageConsistencyPropertyTest
```

### Run a specific property test method
```bash
./mvnw test -Dtest=RedisRateLimiterPropertyTest#rateLimiting_shouldWorkCorrectly
```

## Test Files

### RedisStorageConsistencyPropertyTest.java
Tests basic Redis storage operations:
- Connection establishment
- String and integer data storage/retrieval
- TTL (Time To Live) configuration
- Key format validation for rate limiting, token blacklist, and idempotency
- Increment operations
- Key deletion
- SetIfAbsent for idempotency

**Validates: Requirements 1.1, 1.2, 1.3, 1.4**

### RedisRateLimiterPropertyTest.java
Tests RedisRateLimiter implementation:
- Rate limiting works correctly with Redis
- Counter increments atomically
- TTL is set correctly on first request
- Remaining quota calculation
- Reset functionality
- Concurrent request handling
- Multiple keys independence
- Invalid parameter handling
- Window expiration behavior
- Distributed rate limiting across multiple instances

**Validates: Requirements 1.2, 1.7**

## Property Test Characteristics

All property tests follow these guidelines:
- **Minimum 100 iterations** per property (some tests use 50 or 20 for slower operations)
- **@Label annotation** format: `Feature: architecture-refactoring, Property X.Y: Description`
- **Validates** comments link properties to requirements
- **Random input generation** using jqwik's `@ForAll` and `@Provide` annotations
- **Testcontainers** for Redis integration (requires Docker)

## Test Execution Time

Property-based tests take longer than unit tests due to:
- Multiple iterations (100+ per property)
- Docker container startup/teardown
- Redis operations over network

Expected execution time:
- RedisStorageConsistencyPropertyTest: ~15-20 seconds
- RedisRateLimiterPropertyTest: ~30-40 seconds

## Troubleshooting

### Docker not found
**Error:** `IllegalStateException: Previous attempts to find a Docker environment failed`

**Solution:** 
1. Install Docker Desktop
2. Start Docker
3. Verify with `docker ps`

### Port conflicts
**Error:** `Port 6379 is already in use`

**Solution:**
1. Stop any running Redis instances: `docker stop $(docker ps -q --filter ancestor=redis:7-alpine)`
2. Or change the Redis port in test configuration

### Slow test execution
**Issue:** Tests take too long

**Solution:**
1. Reduce `tries` parameter in `@Property` annotations (not recommended for CI)
2. Use `.withReuse(true)` on Testcontainers (already configured)
3. Run specific test methods instead of entire test classes

### Test failures
**Issue:** Property test fails with counterexample

**Action:**
1. Review the counterexample in test output
2. Determine if it's a bug in the code or test
3. Fix the code or adjust the test assumptions
4. Re-run to verify fix

## CI/CD Integration

For CI/CD pipelines, ensure:
1. Docker is available in the CI environment
2. Docker-in-Docker (DinD) is configured if using containerized CI
3. Sufficient resources allocated for Docker containers
4. Network access for pulling Redis image

Example GitHub Actions configuration:
```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    services:
      docker:
        image: docker:dind
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run property tests
        run: ./mvnw test -Dtest="com.sism.property.*"
```

## Design Document Reference

These tests implement **Property 1: Redis Storage Consistency** from the architecture-refactoring design document.

See: `.kiro/specs/architecture-refactoring/design.md`
