-- audit_log cleanup script
-- Scope:
-- - audit_log is treated as historical audit trace data, not a required part of the clean seed chain.
-- - Safe to clear before loading the current clean seeds when no legacy trace retention is required.

BEGIN;

TRUNCATE TABLE public.audit_log RESTART IDENTITY;

COMMIT;
