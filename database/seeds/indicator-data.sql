-- indicator clean seed
-- Status rule:
-- - indicator.status is an indicator-level lifecycle/projection field.
-- - It is not the authoritative package status; plan.status is the main distribution state.
-- - Current sample rows are aligned so that representative indicators do not contradict
--   their parent plan container status.

BEGIN;

DELETE FROM public.indicator
WHERE id IN (2007, 2008);

INSERT INTO public.indicator (
    id,
    task_id,
    parent_indicator_id,
    indicator_desc,
    weight_percent,
    sort_order,
    remark,
    created_at,
    updated_at,
    type,
    progress,
    is_deleted,
    owner_org_id,
    target_org_id,
    status,
    responsible_user_id,
    is_enabled
)
VALUES
    (2001, 41001, NULL, '完成党委办公室年度重点工作分解与落实', 50.00, 1, '战略发展部下发至党委办公室', NOW(), NOW(), '定量', 100, false, 35, 36, 'DISTRIBUTED', 191, true),
    (2002, 41001, NULL, '形成党委统战领域专项推进台账', 50.00, 2, '战略发展部下发至党委办公室', NOW(), NOW(), '定性', 85, false, 35, 36, 'DISTRIBUTED', 191, true),

    (2003, 41002, NULL, '完成保卫处年度安全巡检计划编制', 60.00, 1, '战略发展部拟下发至保卫处，当前 plan 仍在审批中', NOW(), NOW(), '定量', 0, false, 35, 42, 'DRAFT', 215, true),
    (2004, 41002, NULL, '建立重点区域隐患整改闭环机制', 40.00, 2, '战略发展部拟下发至保卫处，当前 plan 仍在审批中', NOW(), NOW(), '定性', 0, false, 35, 42, 'DRAFT', 215, true),

    (2005, 41003, NULL, '推进教务处教学质量提升专项工作', 55.00, 1, '战略发展部拟下发至教务处', NOW(), NOW(), '定量', 0, false, 35, 44, 'DRAFT', 223, true),
    (2006, 41003, NULL, '完善课程建设与专业评估机制', 45.00, 2, '战略发展部拟下发至教务处', NOW(), NOW(), '定性', 0, false, 35, 44, 'DRAFT', 223, true)
ON CONFLICT (id) DO UPDATE
SET
    task_id = EXCLUDED.task_id,
    parent_indicator_id = EXCLUDED.parent_indicator_id,
    indicator_desc = EXCLUDED.indicator_desc,
    weight_percent = EXCLUDED.weight_percent,
    sort_order = EXCLUDED.sort_order,
    remark = EXCLUDED.remark,
    updated_at = EXCLUDED.updated_at,
    type = EXCLUDED.type,
    progress = EXCLUDED.progress,
    is_deleted = EXCLUDED.is_deleted,
    owner_org_id = EXCLUDED.owner_org_id,
    target_org_id = EXCLUDED.target_org_id,
    status = EXCLUDED.status,
    responsible_user_id = EXCLUDED.responsible_user_id,
    is_enabled = EXCLUDED.is_enabled;

COMMIT;
