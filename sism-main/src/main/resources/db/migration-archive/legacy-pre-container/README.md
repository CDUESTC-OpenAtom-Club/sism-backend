# Legacy Flyway Archive

This directory contains pre-container Flyway migrations that are no longer part
of the active runtime migration chain.

Reason:

- The project now treats `V1__baseline_current_schema.sql` as the only active
  baseline for empty databases.
- Historical `V1.x` migrations assumed partially upgraded databases and cannot
  be replayed safely from an empty PostgreSQL instance.

Rules:

- Do not add this directory to Spring Flyway locations.
- Do not move archived migrations back into `db/migration/`.
- Existing databases must be aligned to the `V1` baseline before applying
  active `V2+` migrations.
