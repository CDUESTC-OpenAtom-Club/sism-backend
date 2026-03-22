-- workflow_task_history clean seed
-- Scope:
-- - Runtime workflow history tables should start empty in clean local environments.
-- - History rows are generated together with workflow task state transitions at runtime.

BEGIN;

-- Intentionally no seed rows.

COMMIT;
