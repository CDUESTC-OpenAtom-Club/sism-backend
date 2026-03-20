-- plan_report clean seed
-- Scope:
-- - plan_report is the execution-side monthly input record.
-- - It is distinct from progress_report, which belongs to the analytics output side.
-- Approval note:
-- - Current runtime approval seeds only model PLAN package approval.
-- - This file does not yet include a separate plan_report approval-instance chain.

BEGIN;

DELETE FROM public.plan_report
WHERE id IN (3003);

INSERT INTO public.plan_report (
    id,
    plan_id,
    report_month,
    report_org_type,
    report_org_id,
    status,
    submitted_at,
    remark,
    created_at,
    updated_at,
    is_deleted,
    created_by
)
VALUES
    (3001, 4036, '202603', 'FUNC_DEPT', 36, 'APPROVED', NOW(), '党委办公室 2026 年 3 月已提交并完成审批', NOW(), NOW(), false, 191),
    (3002, 4042, '202603', 'FUNC_DEPT', 42, 'DRAFT', NULL, '保卫处 2026 年 3 月草稿填报', NOW(), NOW(), false, 215)
ON CONFLICT (id) DO UPDATE
SET
    plan_id = EXCLUDED.plan_id,
    report_month = EXCLUDED.report_month,
    report_org_type = EXCLUDED.report_org_type,
    report_org_id = EXCLUDED.report_org_id,
    status = EXCLUDED.status,
    submitted_at = EXCLUDED.submitted_at,
    remark = EXCLUDED.remark,
    updated_at = EXCLUDED.updated_at,
    is_deleted = EXCLUDED.is_deleted,
    created_by = EXCLUDED.created_by;

COMMIT;
