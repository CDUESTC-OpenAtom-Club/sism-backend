-- plan_report clean seed
-- Scope:
-- - plan_report is the execution-side monthly input record.
-- - It is distinct from progress_report, which belongs to the analytics output side.
-- Approval note:
-- - plan_report now also reserves an audit_instance_id slot for runtime workflow linkage.
-- - In clean seeds, that linkage stays NULL until a real monthly report submission starts approval.
-- Seed policy:
-- - plan_report is runtime business data and should start empty in the clean baseline.

BEGIN;

SELECT 1 WHERE FALSE;

COMMIT;
