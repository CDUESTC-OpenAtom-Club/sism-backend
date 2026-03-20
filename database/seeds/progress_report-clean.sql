-- progress_report cleanup script
-- Scope:
-- - progress_report is the analytics Report entity used by the sism-analytics module.
-- - This cleanup is only for resetting analytics report output data before reloading
--   progress_report-data.sql.
-- - It must not be interpreted as removal of execution-side plan_report data.

BEGIN;

TRUNCATE TABLE public.progress_report RESTART IDENTITY;

COMMIT;
