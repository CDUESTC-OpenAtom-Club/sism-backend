-- workflow_task clean seed
-- Scope:
-- - Provide lightweight workflow inbox/outbox tasks mapped from the current plan approval chain.

BEGIN;

DELETE FROM public.workflow_task
WHERE task_id IN (9104);

INSERT INTO public.workflow_task (
    task_id,
    assignee_id,
    assignee_org_id,
    completed_at,
    current_step,
    due_date,
    error_message,
    initiator_id,
    initiator_org_id,
    next_step,
    result,
    started_at,
    status,
    task_name,
    task_type,
    workflow_id,
    workflow_type
)
VALUES
    (
        9101,
        124,
        35,
        NOW(),
        '分管校领导审批',
        TIMESTAMP '2026-03-12 18:00:00',
        NULL,
        188,
        35,
        NULL,
        'APPROVED',
        TIMESTAMP '2026-03-10 09:00:00',
        'COMPLETED',
        '党委办公室年度 Plan 下发审批',
        'APPROVAL',
        '4001',
        'PLAN_AUDIT'
    ),
    (
        9102,
        188,
        35,
        NULL,
        '战略发展部负责人审批',
        TIMESTAMP '2026-03-20 18:00:00',
        NULL,
        188,
        35,
        '分管校领导审批',
        NULL,
        TIMESTAMP '2026-03-18 09:00:00',
        'PENDING',
        '保卫处年度 Plan 下发审批',
        'APPROVAL',
        '4002',
        'PLAN_AUDIT'
    ),
    (
        9103,
        224,
        44,
        NULL,
        '职能部门审批人审批',
        TIMESTAMP '2026-03-21 18:00:00',
        NULL,
        223,
        44,
        '分管校领导审批',
        NULL,
        TIMESTAMP '2026-03-19 10:00:00',
        'PENDING',
        '教务处年度 Plan 审批',
        'APPROVAL',
        '4003',
        'PLAN_AUDIT'
    )
ON CONFLICT (task_id) DO UPDATE
SET
    assignee_id = EXCLUDED.assignee_id,
    assignee_org_id = EXCLUDED.assignee_org_id,
    completed_at = EXCLUDED.completed_at,
    current_step = EXCLUDED.current_step,
    due_date = EXCLUDED.due_date,
    error_message = EXCLUDED.error_message,
    initiator_id = EXCLUDED.initiator_id,
    initiator_org_id = EXCLUDED.initiator_org_id,
    next_step = EXCLUDED.next_step,
    result = EXCLUDED.result,
    started_at = EXCLUDED.started_at,
    status = EXCLUDED.status,
    task_name = EXCLUDED.task_name,
    task_type = EXCLUDED.task_type,
    workflow_id = EXCLUDED.workflow_id,
    workflow_type = EXCLUDED.workflow_type;

COMMIT;
