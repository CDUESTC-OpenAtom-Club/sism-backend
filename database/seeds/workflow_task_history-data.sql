-- workflow_task_history clean seed
-- Scope:
-- - Keep a readable execution trace for representative workflow_task records.

BEGIN;

DELETE FROM public.workflow_task_history
WHERE task_id IN (9101, 9102, 9103, 9104);

INSERT INTO public.workflow_task_history (
    task_id,
    history
)
VALUES
    (9101, '2026-03-10 09:00 发起党委办公室年度 Plan 下发审批'),
    (9101, '2026-03-10 11:00 战略发展部负责人审批通过'),
    (9101, '2026-03-11 16:00 分管校领导审批通过，流程完成'),

    (9102, '2026-03-18 09:00 发起保卫处年度 Plan 下发审批'),
    (9102, '2026-03-18 09:05 提交节点自动完成'),
    (9102, '2026-03-20 08:30 当前停留在战略发展部负责人审批'),

    (9103, '2026-03-19 10:00 发起教务处年度 Plan 审批'),
    (9103, '2026-03-19 10:02 提交节点自动完成'),
    (9103, '2026-03-20 09:30 当前停留在职能部门审批人审批');

COMMIT;
