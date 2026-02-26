# SISM DATABASE KNOWLEDGE BASE

**Generated:** 2026-02-24

## OVERVIEW

PostgreSQL database for SISM. Flyway migrations, seed data, maintenance scripts.

## STRUCTURE

```
database/
├── migrations/          # Flyway V1.0, V2, V3
├── seeds/               # Seed data
└── scripts/             # DB maintenance
    ├── db-setup.js      # PostgreSQL init
    ├── add-college-users.sql
    ├── fix-weight-to-integer.sql
    └── archive/         # One-time fixes
```

## WHERE TO LOOK

| Task | Location |
|------|----------|
| Schema changes | migrations/ |
| Test data | seeds/ |
| Setup | scripts/db-setup.js |

## CONVENTIONS

- Flyway naming: V{version}__{description}.sql
- Idempotent migrations (check if exists before create)
- DO blocks in migrations
- Migrations reversible or have rollback
- Seed data for dev only (NOT production)

## ANTI-PATTERNS

- NO direct SQL in application code (use Flyway)
- scripts/archive contains legacy fixes (DO NOT copy)
- drop-all.sql: DANGER

## NOTES

- 3 migration versions
- Scripts for setup, maintenance, fixes
