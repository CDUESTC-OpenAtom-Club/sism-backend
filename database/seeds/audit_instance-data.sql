-- audit_instance clean seed
-- Scope:
-- - Current runtime approval seeds model PLAN-level approval only.
-- - plan_report approval is intentionally not represented here yet.
-- Snapshot note:
-- - These rows are intended to be a coherent representative snapshot of plan package approval.

BEGIN;

DELETE FROM public.audit_instance
WHERE id IN (4004);

INSERT INTO public.audit_instance (
    id,
    status,
    started_at,
    created_at,
    updated_at,
    completed_at,
    entity_id,
    entity_type,
    flow_def_id,
    is_deleted,
    requester_id,
    requester_org_id
)
VALUES
    (4001, 'APPROVED', NOW(), NOW(), NOW(), NOW(), 4036, 'PLAN', 1, false, 188, 35),
    (4002, 'IN_REVIEW', NOW(), NOW(), NOW(), NULL, 4042, 'PLAN', 1, false, 188, 35),
    (4003, 'IN_REVIEW', NOW(), NOW(), NOW(), NULL, 4044, 'PLAN', 3, false, 223, 44)
ON CONFLICT (id) DO UPDATE
SET
    status = EXCLUDED.status,
    started_at = EXCLUDED.started_at,
    updated_at = EXCLUDED.updated_at,
    completed_at = EXCLUDED.completed_at,
    entity_id = EXCLUDED.entity_id,
    entity_type = EXCLUDED.entity_type,
    flow_def_id = EXCLUDED.flow_def_id,
    is_deleted = EXCLUDED.is_deleted,
    requester_id = EXCLUDED.requester_id,
    requester_org_id = EXCLUDED.requester_org_id;

COMMIT;
