-- sys_permission clean seed
-- Scope:
-- - Keep only the 12 permissions currently used by the reviewed role-permission matrix.
-- - Parent-child relation is preserved by explicit parent_id.

BEGIN;

INSERT INTO public.sys_permission (
    id,
    perm_code,
    perm_name,
    perm_type,
    parent_id,
    route_path,
    page_key,
    action_key,
    sort_order,
    is_enabled,
    remark,
    created_at,
    updated_at
)
VALUES
    (1, 'PAGE_DASHBOARD', '数据看板', 'PAGE', NULL, '/dashboard', 'dashboard', NULL, 10, true, '各级组织数据看板入口', NOW(), NOW()),
    (2, 'PAGE_STRATEGY_TASK', '战略任务管理', 'PAGE', NULL, '/strategy/task', 'strategy_task', NULL, 20, true, '战略任务下发、审批、填报审核入口', NOW(), NOW()),
    (3, 'PAGE_DATA_REPORT', '数据填报', 'PAGE', NULL, '/data/report', 'data_report', NULL, 30, true, '职能部门/二级学院数据填报入口', NOW(), NOW()),
    (4, 'PAGE_INDICATOR_DISPATCH', '指标下发与审批', 'PAGE', NULL, '/indicator/dispatch', 'indicator_dispatch', NULL, 40, true, '指标下发、审批、填报审核入口', NOW(), NOW()),
    (5, 'BTN_STRATEGY_TASK_DISPATCH_SUBMIT', '下发（提交）', 'BUTTON', 2, NULL, NULL, 'dispatch_submit', 10, true, '战略任务下发提交', NOW(), NOW()),
    (6, 'BTN_STRATEGY_TASK_DISPATCH_APPROVE', '下发审核', 'BUTTON', 2, NULL, NULL, 'dispatch_approve', 20, true, '战略任务下发审批', NOW(), NOW()),
    (7, 'BTN_STRATEGY_TASK_REPORT_APPROVE', '填报审核', 'BUTTON', 2, NULL, NULL, 'report_approve', 30, true, '战略任务填报审核', NOW(), NOW()),
    (8, 'BTN_DATA_REPORT_SUBMIT', '提交', 'BUTTON', 3, NULL, NULL, 'submit', 10, true, '数据填报提交', NOW(), NOW()),
    (9, 'BTN_DATA_REPORT_APPROVE', '审核', 'BUTTON', 3, NULL, NULL, 'approve', 20, true, '数据填报审核', NOW(), NOW()),
    (10, 'BTN_INDICATOR_DISPATCH_SUBMIT', '下发（提交）', 'BUTTON', 4, NULL, NULL, 'dispatch_submit', 10, true, '指标下发提交', NOW(), NOW()),
    (11, 'BTN_INDICATOR_DISPATCH_APPROVE', '下发审核', 'BUTTON', 4, NULL, NULL, 'dispatch_approve', 20, true, '指标下发审核', NOW(), NOW()),
    (12, 'BTN_INDICATOR_REPORT_APPROVE', '填报审核', 'BUTTON', 4, NULL, NULL, 'report_approve', 30, true, '指标填报审核', NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET
    perm_code = EXCLUDED.perm_code,
    perm_name = EXCLUDED.perm_name,
    perm_type = EXCLUDED.perm_type,
    parent_id = EXCLUDED.parent_id,
    route_path = EXCLUDED.route_path,
    page_key = EXCLUDED.page_key,
    action_key = EXCLUDED.action_key,
    sort_order = EXCLUDED.sort_order,
    is_enabled = EXCLUDED.is_enabled,
    remark = EXCLUDED.remark,
    updated_at = EXCLUDED.updated_at;

COMMIT;
