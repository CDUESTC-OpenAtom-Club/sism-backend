-- idempotency_records clean seed
-- Scope:
-- - Idempotency records are request-runtime cache data.
-- - Clean baseline should keep this table empty.

BEGIN;

-- Intentionally no seed rows.

COMMIT;
