-- indicator_milestone clean seed

BEGIN;

DELETE FROM public.indicator_milestone
WHERE id IN (6007, 6008, 6009);

INSERT INTO public.indicator_milestone (
    id,
    indicator_id,
    milestone_name,
    milestone_desc,
    due_date,
    status,
    sort_order,
    created_at,
    updated_at,
    target_progress,
    is_paired
)
VALUES
    (6001, 2001, '一季度完成任务分解', '完成党委办公室年度重点工作分解', DATE '2026-03-31', 'COMPLETED', 1, NOW(), NOW(), 30, false),
    (6002, 2001, '二季度完成中期检查', '完成中期执行检查与纠偏', DATE '2026-06-30', 'COMPLETED', 2, NOW(), NOW(), 60, false),
    (6003, 2001, '年底完成总结验收', '完成年度总结与验收归档', DATE '2026-12-31', 'IN_PROGRESS', 3, NOW(), NOW(), 100, false),

    (6007, 2002, '一季度建立专项推进台账', '完成党委统战领域专项推进台账的建立与首次归集', DATE '2026-03-31', 'COMPLETED', 1, NOW(), NOW(), 30, false),
    (6008, 2002, '二季度完成动态更新', '完成专项推进台账的阶段更新与责任事项核对', DATE '2026-06-30', 'COMPLETED', 2, NOW(), NOW(), 70, false),
    (6009, 2002, '年底完成总结归档', '完成专项推进台账年度总结材料补充与归档', DATE '2026-12-31', 'IN_PROGRESS', 3, NOW(), NOW(), 100, false),

    (6004, 2003, '提交巡检计划初稿', '形成年度巡检计划初稿', DATE '2026-03-31', 'IN_PROGRESS', 1, NOW(), NOW(), 30, false),
    (6005, 2003, '完成重点区域排查', '完成重点区域安全排查与整改计划', DATE '2026-06-30', 'NOT_STARTED', 2, NOW(), NOW(), 70, false),
    (6006, 2003, '形成年度闭环报告', '形成全年安全治理闭环报告', DATE '2026-12-31', 'NOT_STARTED', 3, NOW(), NOW(), 100, false)
ON CONFLICT (id) DO UPDATE
SET
    indicator_id = EXCLUDED.indicator_id,
    milestone_name = EXCLUDED.milestone_name,
    milestone_desc = EXCLUDED.milestone_desc,
    due_date = EXCLUDED.due_date,
    status = EXCLUDED.status,
    sort_order = EXCLUDED.sort_order,
    updated_at = EXCLUDED.updated_at,
    target_progress = EXCLUDED.target_progress,
    is_paired = EXCLUDED.is_paired;

COMMIT;
