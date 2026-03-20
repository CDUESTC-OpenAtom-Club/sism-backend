-- refresh_tokens cleanup script
-- Scope:
-- - refresh_tokens is a runtime authentication/session table.
-- - It should not be preloaded by fixed seed data.
-- - Safe to clear before environment reset or test-data rebuild.

BEGIN;

TRUNCATE TABLE public.refresh_tokens RESTART IDENTITY;

COMMIT;
