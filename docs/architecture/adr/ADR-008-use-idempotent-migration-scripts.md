# ADR-008: Use Idempotent Migration Scripts

## Status

Accepted

## Date

2026-02-13

## Context

When implementing Flyway migrations, we needed to handle the scenario where:
- Production database already has tables and data
- Development databases may be in various states
- Migrations need to be safely re-runnable during development
- Risk of migration failures due to existing objects

Standard Flyway migrations fail if objects already exist:
```sql
CREATE TABLE attachment (...);  -- Fails if table exists
ALTER TABLE task ADD COLUMN ...;  -- Fails if column exists
```

This creates problems:
- Can't baseline existing production databases cleanly
- Development databases in inconsistent states cause failures
- Manual intervention required to fix failed migrations
- Risk of data loss if migrations are forced

PostgreSQL provides DO blocks and conditional logic to make migrations idempotent:
```sql
DO $$
BEGIN
    IF NOT EXISTS (...) THEN
        CREATE TABLE ...;
    END IF;
END $$;
```

## Decision

We will write **idempotent migration scripts** using PostgreSQL DO blocks:
1. Wrap all DDL statements in conditional checks
2. Check for existence before creating tables, columns, indexes
3. Use `IF NOT EXISTS` clauses where supported
4. Use information_schema queries for complex checks
5. Make migrations safe to run multiple times

Pattern for tables:
```sql
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables 
                   WHERE table_name = 'attachment') THEN
        CREATE TABLE attachment (...);
    END IF;
END $$;
```

Pattern for columns:
```sql
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'task' AND column_name = 'new_column') THEN
        ALTER TABLE task ADD COLUMN new_column VARCHAR(255);
    END IF;
END $$;
```

## Consequences

### Positive

- Safe to run migrations multiple times (idempotent)
- Can baseline existing production databases without errors
- Development databases can be in any state
- Reduces risk of migration failures
- Easier to recover from failed migrations
- Supports iterative development (re-run migrations during testing)
- Clear intent in migration scripts (explicit existence checks)

### Negative

- More verbose migration scripts
- Slightly more complex to write
- Requires understanding of PostgreSQL DO blocks
- Can't rely on Flyway's checksum validation alone
- May hide actual migration errors if not careful

### Neutral

- No performance impact (checks are fast)
- Works only with PostgreSQL (not database-agnostic)
- Flyway still tracks migration history normally

## Alternatives Considered

### Alternative 1: Standard non-idempotent migrations

Write standard SQL without existence checks.

**Why not chosen**:
- Fails on existing production databases
- Requires manual intervention for failures
- Can't re-run migrations during development
- Higher risk of migration errors
- Difficult to baseline existing databases

### Alternative 2: Use Flyway's baseline feature only

Rely on Flyway's baseline-on-migrate without idempotent scripts.

**Why not chosen**:
- Doesn't help with development database inconsistencies
- Still fails if objects exist in non-baselined databases
- Doesn't support iterative development workflow
- Less flexible than idempotent scripts

### Alternative 3: Drop and recreate everything

Drop all objects and recreate from scratch in each migration.

**Why not chosen**:
- Loses all data (unacceptable for production)
- Extremely risky
- Doesn't support incremental migrations
- Not a real migration strategy

## Implementation Notes

**V1__baseline_schema.sql** (Idempotent):
```sql
-- Create attachment table (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables 
                   WHERE table_name = 'attachment') THEN
        CREATE TABLE attachment (
            id BIGSERIAL PRIMARY KEY,
            storage_driver VARCHAR(50),
            bucket VARCHAR(255),
            object_key VARCHAR(500) NOT NULL,
            public_url TEXT,
            original_name VARCHAR(255) NOT NULL,
            content_type VARCHAR(100),
            file_ext VARCHAR(50),
            size_bytes BIGINT,
            sha256 VARCHAR(64),
            etag VARCHAR(255),
            uploaded_by BIGINT,
            uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
            remark TEXT,
            is_deleted BOOLEAN DEFAULT FALSE,
            deleted_at TIMESTAMP WITH TIME ZONE
        );
    END IF;
END $$;

-- Create index (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes 
                   WHERE indexname = 'idx_attachment_uploaded_by') THEN
        CREATE INDEX idx_attachment_uploaded_by ON attachment(uploaded_by);
    END IF;
END $$;
```

**V2__add_new_entities.sql** (Idempotent):
```sql
-- Add audit_flow_def table
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables 
                   WHERE table_name = 'audit_flow_def') THEN
        CREATE TABLE audit_flow_def (
            id BIGSERIAL PRIMARY KEY,
            flow_name VARCHAR(100) NOT NULL,
            flow_code VARCHAR(50) NOT NULL UNIQUE,
            entity_type VARCHAR(50) NOT NULL,
            description TEXT,
            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
        );
    END IF;
END $$;
```

**Testing Idempotency**:
```bash
# Run migration twice - should succeed both times
mvn flyway:migrate
mvn flyway:migrate  # Should be no-op, no errors
```

**Execution Results**:
- All migrations are idempotent
- Successfully applied to production database
- Can be re-run without errors
- Development databases can be in any state

## References

- Task 3.1: Flyway Integration
- ADR-007: Adopt Flyway for Schema Management
- PostgreSQL DO Blocks: https://www.postgresql.org/docs/current/sql-do.html
- Migration Files: `sism-backend/src/main/resources/db/migration/`
