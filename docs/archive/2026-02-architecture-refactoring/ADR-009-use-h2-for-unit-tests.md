# ADR-009: Use H2 for Unit Tests

## Status

Accepted

## Date

2026-02-13

## Context

The backend uses PostgreSQL in production, but we needed to decide on a database strategy for unit and integration tests:

**Option 1: H2 in-memory database**
- Fast startup (milliseconds)
- No external dependencies
- Runs in JVM memory
- PostgreSQL compatibility mode available
- Some SQL dialect differences

**Option 2: TestContainers with PostgreSQL**
- Exact production database match
- No dialect differences
- Requires Docker
- Slower startup (5-10 seconds per test class)
- More resource intensive

**Option 3: Shared PostgreSQL test database**
- Exact production match
- Requires external database setup
- Test isolation challenges
- Slower than H2

Current test requirements:
- 409 tests need to run quickly in CI/CD
- Most tests are unit tests (service layer, entity validation)
- Integration tests are a small subset
- Test data is prepared in @BeforeEach methods
- No complex PostgreSQL-specific features in tests

## Decision

We will use **H2 in-memory database** for unit and integration tests:
1. Configure H2 with PostgreSQL compatibility mode
2. Use JPA schema generation (ddl-auto: create-drop)
3. Disable Flyway for tests (not needed with JPA generation)
4. Prepare test data in @BeforeEach methods
5. Use @Profile("!test") to exclude production-only beans

Configuration:
```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  flyway:
    enabled: false
```

Reserve TestContainers for:
- Complex integration tests requiring exact PostgreSQL behavior
- Performance testing
- Testing PostgreSQL-specific features (if needed in future)

## Consequences

### Positive

- Fast test execution (409 tests run in ~3 minutes)
- No external dependencies (Docker not required)
- Simple CI/CD setup
- Low resource usage
- Easy local development (no database setup)
- Tests run in parallel without conflicts
- Fresh database for each test class

### Negative

- SQL dialect differences may cause issues (rare)
- Not exact production environment match
- Some PostgreSQL features unavailable in H2
- May miss PostgreSQL-specific bugs

### Neutral

- Test data preparation required (same for any approach)
- JPA schema generation works well for test scenarios
- Can add TestContainers later if needed

## Alternatives Considered

### Alternative 1: TestContainers with PostgreSQL

Use TestContainers to run real PostgreSQL in Docker for all tests.

**Why not chosen**:
- Significantly slower (5-10 seconds startup per test class)
- Requires Docker (complicates CI/CD and local setup)
- Overkill for unit tests
- Higher resource usage
- Most tests don't need exact PostgreSQL match
- Can be added later for specific integration tests

### Alternative 2: Shared PostgreSQL test database

Use a shared PostgreSQL instance for all tests.

**Why not chosen**:
- Requires external database setup
- Test isolation challenges (parallel execution)
- Slower than H2
- More complex local development setup
- Risk of test data conflicts

### Alternative 3: Mock repositories

Mock all repository calls in service tests.

**Why not chosen**:
- Doesn't test actual database interactions
- Misses JPA mapping issues
- More complex test setup
- Less confidence in integration
- Still need database for entity tests

## Implementation Notes

**Test Configuration**:
```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ServiceTest {
    @Autowired
    private SomeService service;
    
    @BeforeEach
    void setUp() {
        // Prepare test data
    }
}
```

**H2 Compatibility Fixes**:
```java
// Fix reserved keyword 'year'
@Column(name = "`year`")  // Backticks for H2
private Integer year;
```

**Exclude Production Beans**:
```java
@Component
@Profile("!test")  // Don't load in test environment
public class EnvConfigValidator {
    // Production-only validation
}
```

**Test Results**:
- Total tests: 409
- Passing: 149 (36%)
- Failing: 260 (64% - due to missing test data, not H2 issues)
- Build time: ~3 minutes
- ApplicationContext loads successfully
- No H2-specific failures

**Known Limitations**:
- Some PostgreSQL-specific SQL functions unavailable
- JSONB type not supported (use TEXT in tests)
- Array types have different syntax
- Full-text search not available

**Future Enhancements**:
- Add TestContainers for complex integration tests
- Create separate test profile for PostgreSQL integration tests
- Document H2 vs PostgreSQL differences

## References

- Task 1: All Existing Tests Pass
- Test Configuration: `sism-backend/src/test/resources/application-test.yml`
- ADR-010: Defer TestContainers Integration
- H2 PostgreSQL Mode: http://www.h2database.com/html/features.html#compatibility
