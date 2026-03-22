-- plan_report_indicator_attachment clean seed
-- Scope:
-- - Bind report-indicator records to physical attachments in concrete business scenes.
-- Seed policy:
-- - Attachment links are runtime monthly-report data and should start empty in the clean baseline.

BEGIN;

SELECT 1 WHERE FALSE;

COMMIT;
