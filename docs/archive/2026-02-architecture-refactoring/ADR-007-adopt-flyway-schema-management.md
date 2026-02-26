# ADR-007: Adopt Flyway for Schema Management

## Status

Accepted

## Date

2026-02-13

## Context

The project previously used manual SQL scripts in `database/migrations/` directory for schema management:
- V1.0__init.sql, V1.1__add_refresh_tokens.sql, etc.
- Scripts were executed manually or via Node.js scripts
- No automated tracking of which migrations were applied
- Risk of applying migrations out of order or multiple times
- No integration with Spring Boot application lifecycle

This approach had several problems:
- Manual execution prone to human error
- No rollback capability
- Difficult to coordinate migrations across environments
- No validation that database schema matches application expectations
- JPA `ddl-auto: update` could conflict with manual migrations

Industry best practices recommend using a database migration tool like Flyway or Liquibase for:
- Versioned, repeatable migrations
- Automatic tracking of applied migrations
- Integration with application startup
- Validation of schema consistency

## Decision

We will adopt **Flyway** as the database migration tool:
1. Add Flyway dependency to pom.xml
2. Move migration scripts to `src/main/resources/db/migration/`
3. Configure Flyway in application.yml
4. Set JPA `ddl-auto: validate` (Flyway manages schema, JPA validates)
5. Enable `baseline-on-migrate` for existing production databases
6. Create idempotent migration scripts using PostgreSQL DO blocks

Configuration:
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    baseline-version: 1.0
  jpa:
    hibernate:
      ddl-auto: validate  # Changed from 'update'
```

## Consequences

### Positive

- Automated migration execution on application startup
- Complete migration history tracked in `flyway_schema_history` table
- Prevents duplicate migration execution
- Validates database schema matches application expectations
- Supports rollback through versioned scripts
- Industry-standard tool with excellent documentation
- CI/CD integration for automated testing
- Clear separation: Flyway manages schema, JPA validates

### Negative

- Additional dependency (flyway-core)
- Learning curve for team unfamiliar with Flyway
- Migration scripts must follow Flyway naming conventions
- Requires careful migration script design for production
- Baseline migration needed for existing databases

### Neutral

- Migration files moved to new location (one-time effort)
- Existing migrations work with minimal changes
- No impact on application runtime performance

## Alternatives Considered

### Alternative 1: Continue manual migrations

Keep existing manual SQL script approach.

**Why not chosen**:
- Prone to human error
- No automated tracking
- Difficult to coordinate across environments
- No validation capability
- Doesn't scale as team grows

### Alternative 2: Use Liquibase

Adopt Liquibase instead of Flyway.

**Why not chosen**:
- More complex (XML/YAML/JSON formats)
- Steeper learning curve
- Flyway's SQL-first approach simpler for team
- Flyway has better Spring Boot integration
- Team already familiar with SQL

### Alternative 3: Use JPA ddl-auto exclusively

Rely on Hibernate's `ddl-auto: update` for schema management.

**Why not chosen**:
- Not recommended for production
- No version control of schema changes
- Can't handle complex migrations (data transformations)
- No rollback capability
- Risk of unintended schema changes

## Implementation Notes

**Migration File Naming**:
```
V1__baseline_schema.sql          # Initial schema
V2__add_new_entities.sql         # New entities
V3__add_audit_columns.sql        # Schema changes
```

**Idempotent Migration Pattern**:
```sql
-- Use DO blocks for idempotent operations
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables 
                   WHERE table_name = 'attachment') THEN
        CREATE TABLE attachment (
            id BIGSERIAL PRIMARY KEY,
            -- columns...
        );
    END IF;
END $$;
```

**Baseline for Existing Databases**:
```sql
-- For production databases with existing schema
-- Flyway will baseline at version 1.0 and apply only newer migrations
```

**Execution Results**:
- Flyway integrated successfully
- 3 migrations created (V1, V2, V3)
- Production database migrated to schema version 3
- All migrations executed successfully
- JPA validation passes

## References

- Task 3.1: Flyway Integration
- Flyway Migration Guide: `sism-backend/docs/flyway-migration-guide.md`
- CI/CD Integration: Task 4.1 (Flyway validation in CI)
- Official Flyway Documentation: https://flywaydb.org/documentation/
