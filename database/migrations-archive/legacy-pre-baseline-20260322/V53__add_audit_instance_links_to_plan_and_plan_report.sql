-- V53__add_audit_instance_links_to_plan_and_plan_report.sql
-- Purpose:
-- - Keep plan / plan_report aligned with workflow runtime by reserving direct audit_instance linkage columns.
-- - Allow clean seeds to start with NULL linkage before any approval instance is created.

ALTER TABLE public.plan
    ADD COLUMN IF NOT EXISTS audit_instance_id BIGINT;

ALTER TABLE public.plan
    ALTER COLUMN audit_instance_id DROP NOT NULL;

ALTER TABLE public.plan_report
    ADD COLUMN IF NOT EXISTS audit_instance_id BIGINT;

COMMENT ON COLUMN public.plan.audit_instance_id IS
    'Optional runtime link to audit_instance.id. NULL means the plan has not started approval yet.';

COMMENT ON COLUMN public.plan_report.audit_instance_id IS
    'Optional runtime link to audit_instance.id. NULL means the monthly report has not started approval yet.';
