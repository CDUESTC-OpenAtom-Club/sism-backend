-- sys_task clean seed
-- Scope:
-- - Provide representative tasks under yearly plan containers.
-- - For the 2026 review set, every target organization plan container should own at least one task.
-- Field rule:
-- - name / desc are the only business-source columns for task title and description.
-- - Clean seeds follow name / desc as the final business-standard fields.

BEGIN;

INSERT INTO public.sys_task (
    task_id,
    created_at,
    updated_at,
    remark,
    sort_order,
    name,
    "desc",
    task_type,
    created_by_org_id,
    cycle_id,
    org_id,
    is_deleted,
    plan_id
)
VALUES
    (41001, NOW(), NOW(), '2026 年党委办公室任务', 1, '党委办公室年度重点工作', '党委办公室年度重点工作任务容器', 'BASIC', 35, 4, 36, false, 4036),
    (41005, NOW(), NOW(), '2026 年纪委办公室任务', 1, '纪委办公室监督执纪任务', '纪委办公室年度监督执纪任务容器', 'BASIC', 35, 4, 37, false, 4037),
    (41006, NOW(), NOW(), '2026 年党委宣传部任务', 1, '党委宣传部品牌建设任务', '党委宣传部年度品牌建设任务容器', 'BASIC', 35, 4, 38, false, 4038),
    (41007, NOW(), NOW(), '2026 年党委组织部任务', 1, '党委组织部组织与教师发展任务', '党委组织部年度组织与教师发展任务容器', 'BASIC', 35, 4, 39, false, 4039),
    (41008, NOW(), NOW(), '2026 年人力资源部任务', 1, '人力资源部师资队伍优化任务', '人力资源部年度师资队伍优化任务容器', 'BASIC', 35, 4, 40, false, 4040),
    (41009, NOW(), NOW(), '2026 年学工部任务', 1, '学工部学生发展支持任务', '学工部年度学生发展支持任务容器', 'BASIC', 35, 4, 41, false, 4041),
    (41002, NOW(), NOW(), '2026 年保卫处任务', 1, '保卫处安全治理任务', '保卫处安全治理任务容器', 'BASIC', 35, 4, 42, false, 4042),
    (41010, NOW(), NOW(), '2026 年综合办公室任务', 1, '综合办公室治理效能任务', '综合办公室年度治理效能任务容器', 'BASIC', 35, 4, 43, false, 4043),
    (41003, NOW(), NOW(), '2026 年教务处任务', 1, '教务处教学质量提升任务', '教务处教学质量提升任务容器', 'BASIC', 35, 4, 44, false, 4044),
    (41011, NOW(), NOW(), '2026 年科技处任务', 1, '科技处科研创新促进任务', '科技处年度科研创新促进任务容器', 'BASIC', 35, 4, 45, false, 4045),
    (41012, NOW(), NOW(), '2026 年财务部任务', 1, '财务部预算绩效管理任务', '财务部年度预算绩效管理任务容器', 'BASIC', 35, 4, 46, false, 4046),
    (41013, NOW(), NOW(), '2026 年招生工作处任务', 1, '招生工作处招生质量提升任务', '招生工作处年度招生质量提升任务容器', 'BASIC', 35, 4, 47, false, 4047),
    (41014, NOW(), NOW(), '2026 年就业创业指导中心任务', 1, '就业创业指导中心就业服务提升任务', '就业创业指导中心年度就业服务提升任务容器', 'BASIC', 35, 4, 48, false, 4048),
    (41015, NOW(), NOW(), '2026 年实验室建设管理处任务', 1, '实验室建设管理处实验室安全建设任务', '实验室建设管理处年度实验室安全建设任务容器', 'BASIC', 35, 4, 49, false, 4049),
    (41016, NOW(), NOW(), '2026 年数字校园建设办公室任务', 1, '数字校园建设办公室数字校园推进任务', '数字校园建设办公室年度数字校园推进任务容器', 'BASIC', 35, 4, 50, false, 4050),
    (41017, NOW(), NOW(), '2026 年图书馆任务', 1, '图书馆文献档案服务提升任务', '图书馆年度文献档案服务提升任务容器', 'BASIC', 35, 4, 51, false, 4051),
    (41018, NOW(), NOW(), '2026 年后勤资产处任务', 1, '后勤资产处后勤保障任务', '后勤资产处年度后勤保障任务容器', 'BASIC', 35, 4, 52, false, 4052),
    (41019, NOW(), NOW(), '2026 年继续教育部任务', 1, '继续教育部项目优化任务', '继续教育部年度项目优化任务容器', 'BASIC', 35, 4, 53, false, 4053),
    (41020, NOW(), NOW(), '2026 年国际合作与交流处任务', 1, '国际合作与交流处国际交流拓展任务', '国际合作与交流处年度国际交流拓展任务容器', 'BASIC', 35, 4, 54, false, 4054)
ON CONFLICT (task_id) DO UPDATE
SET
    updated_at = EXCLUDED.updated_at,
    remark = EXCLUDED.remark,
    sort_order = EXCLUDED.sort_order,
    task_type = EXCLUDED.task_type,
    created_by_org_id = EXCLUDED.created_by_org_id,
    cycle_id = EXCLUDED.cycle_id,
    org_id = EXCLUDED.org_id,
    is_deleted = EXCLUDED.is_deleted,
    plan_id = EXCLUDED.plan_id,
    name = EXCLUDED.name,
    "desc" = EXCLUDED."desc";

COMMIT;
