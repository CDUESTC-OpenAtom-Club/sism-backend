# Baseline Migration Notice

`database/migrations` no longer stores the active Flyway history chain.

The project now treats the current approved database schema as the new baseline:

- Active Flyway baseline: [V1__baseline_current_schema.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-main/src/main/resources/db/migration/V1__baseline_current_schema.sql)
- Legacy migrations archive: `database/migrations-archive/legacy-pre-baseline-20260322/`

Operational rule:

- New schema changes must be added after `V1` in `sism-main/src/main/resources/db/migration/`
- Do not restore archived migration files back into the active Flyway path
