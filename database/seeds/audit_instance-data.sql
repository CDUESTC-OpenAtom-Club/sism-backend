-- audit_instance clean seed
-- Scope:
-- - Workflow runtime tables should start empty in clean environments.
-- - Approval instances are created by the application after users actually submit data.

BEGIN;

-- Intentionally no seed rows.

COMMIT;
