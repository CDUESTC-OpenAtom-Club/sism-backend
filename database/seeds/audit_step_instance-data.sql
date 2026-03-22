-- audit_step_instance clean seed
-- Scope:
-- - Workflow runtime step tables should start empty in clean environments.
-- - Step instances are created together with audit_instance records at runtime.

BEGIN;

-- Intentionally no seed rows.

COMMIT;
