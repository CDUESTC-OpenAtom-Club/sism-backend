-- Allow multiple plan report rounds within the same month for the same plan/org.
-- Business rule:
-- - DRAFT reports can be reused within the same round.
-- - Once a monthly report round leaves DRAFT and reaches a completed state,
--   subsequent filling in the same month should create a brand new plan_report row.

ALTER TABLE public.plan_report
    DROP CONSTRAINT IF EXISTS uq_plan_report;

CREATE INDEX IF NOT EXISTS idx_plan_report_scope_created
    ON public.plan_report (plan_id, report_month, report_org_type, report_org_id, created_at DESC, id DESC);
