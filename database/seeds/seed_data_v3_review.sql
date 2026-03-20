-- =============================================================================
-- SISM v3 审核版种子数据
-- 说明:
-- 1. 本文件按当前真实库结构编写: cycle / plan / sys_task / indicator / indicator_milestone
-- 2. 所有数据均为“审核草案”，默认以 ROLLBACK 结束，不会真正写入数据库
-- 3. 若审核通过，请复制为正式执行脚本后再将最后一行改为 COMMIT;
-- 4. 参考真实库基线:
--    cycle_max=90, plan_max=7, task_max=92091, indicator_max=20417, milestone_max=36228
-- =============================================================================

\echo '============================================================'
\echo 'SISM v3 审核版种子数据预演开始'
\echo '本次执行会在最后 ROLLBACK，不会写入数据库'
\echo '============================================================'
\echo ''

BEGIN;

-- -----------------------------------------------------------------------------
-- 0. 预检查
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM cycle WHERE id = 91) THEN
        RAISE EXCEPTION '审核草案冲突: cycle.id=91 已存在，请先调整草案 ID';
    END IF;

    IF EXISTS (SELECT 1 FROM plan WHERE id = 8) THEN
        RAISE EXCEPTION '审核草案冲突: plan.id=8 已存在，请先调整草案 ID';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM sys_task
        WHERE task_id BETWEEN 92092 AND 92095
           OR task_name IN (
               '2027年度战略任务1-教学质量提升',
               '2027年度战略任务2-科研协同攻坚',
               '2027年度战略任务3-数字治理提效',
               '2027年度战略任务4-产教融合示范'
           )
    ) THEN
        RAISE EXCEPTION '审核草案冲突: 预留任务 ID 或任务名称已存在';
    END IF;
END $$;

-- -----------------------------------------------------------------------------
-- 1. 周期与计划
-- -----------------------------------------------------------------------------
INSERT INTO cycle (
    id, cycle_name, year, start_date, end_date, description, created_at, updated_at
) VALUES (
    91,
    '2027年度战略目标考核（审核草案）',
    2027,
    DATE '2027-01-01',
    DATE '2027-12-31',
    '用于审核当前结构下的战略任务/指标种子设计，不直接入库',
    NOW(),
    NOW()
);

INSERT INTO plan (
    id, cycle_id, created_at, updated_at, is_deleted,
    target_org_id, created_by_org_id, plan_level, status
) VALUES (
    8,
    91,
    NOW(),
    NOW(),
    FALSE,
    35,
    35,
    'STRAT_TO_FUNC',
    'DRAFT'
);

-- -----------------------------------------------------------------------------
-- 2. 战略任务
-- 当前结构注意事项:
-- - sys_task 仍保留兼容列 name / desc，因此这里与 task_name / task_desc 双写
-- - task_type 仅使用当前有效值 BASIC / DEVELOPMENT
-- -----------------------------------------------------------------------------
INSERT INTO sys_task (
    task_id, created_at, updated_at, remark, sort_order,
    task_desc, task_name, task_type, created_by_org_id, cycle_id, org_id,
    is_deleted, plan_id, name, "desc"
) VALUES
(
    92092, NOW(), NOW(), '审核草案-教学条线', 1,
    '围绕课程建设、教学成果和学院协同改进形成年度闭环提升任务',
    '2027年度战略任务1-教学质量提升', 'BASIC', 35, 91, 35,
    FALSE, 8, '2027年度战略任务1-教学质量提升', '围绕课程建设、教学成果和学院协同改进形成年度闭环提升任务'
),
(
    92093, NOW(), NOW(), '审核草案-科研条线', 2,
    '聚焦项目申报、成果转化与科研平台协同建设，形成科研攻坚牵引任务',
    '2027年度战略任务2-科研协同攻坚', 'DEVELOPMENT', 35, 91, 35,
    FALSE, 8, '2027年度战略任务2-科研协同攻坚', '聚焦项目申报、成果转化与科研平台协同建设，形成科研攻坚牵引任务'
),
(
    92094, NOW(), NOW(), '审核草案-数字校园条线', 3,
    '推动业务系统整合、数据治理和服务线上化，支撑学校治理提效',
    '2027年度战略任务3-数字治理提效', 'BASIC', 35, 91, 35,
    FALSE, 8, '2027年度战略任务3-数字治理提效', '推动业务系统整合、数据治理和服务线上化，支撑学校治理提效'
),
(
    92095, NOW(), NOW(), '审核草案-校企合作条线', 4,
    '围绕校企合作基地、实践课程和就业联动打造产教融合示范任务',
    '2027年度战略任务4-产教融合示范', 'DEVELOPMENT', 35, 91, 35,
    FALSE, 8, '2027年度战略任务4-产教融合示范', '围绕校企合作基地、实践课程和就业联动打造产教融合示范任务'
);

-- -----------------------------------------------------------------------------
-- 3. 指标
-- 口径:
-- - 与当前真实库保持一致，使用 indicator.id 主键
-- - type 使用现网已有值“定量”
-- - status 使用 DRAFT / DISTRIBUTED
-- - 指标状态统一使用 status 字段
-- -----------------------------------------------------------------------------
INSERT INTO indicator (
    id, task_id, parent_indicator_id, indicator_desc, weight_percent, sort_order,
    remark, created_at, updated_at, type, progress, is_deleted,
    owner_org_id, target_org_id, status,
    responsible_user_id, is_enabled
) VALUES
(
    20418, 92092, NULL, '完成校级一流课程建设项目不少于12项', 35.00, 1,
    '战略发展部统筹，教务处牵头推进', NOW(), NOW(), '定量', 0, FALSE,
    35, 44, 'DISTRIBUTED', 223, TRUE
),
(
    20419, 92092, NULL, '计算机学院新增省级教学成果培育项目不少于3项', 30.00, 2,
    '由教务处协同学院推进成果培育', NOW(), NOW(), '定量', 0, FALSE,
    44, 57, 'DRAFT', 273, TRUE
),
(
    20420, 92092, NULL, '商学院核心课程过程性考核覆盖率达到95%', 35.00, 3,
    '聚焦课堂评价和过程考核质量提升', NOW(), NOW(), '定量', 0, FALSE,
    44, 58, 'DRAFT', 276, TRUE
),
(
    20421, 92093, NULL, '年度新增省部级及以上科研项目不少于18项', 40.00, 1,
    '科技处统筹项目申报组织', NOW(), NOW(), '定量', 0, FALSE,
    35, 45, 'DISTRIBUTED', 227, TRUE
),
(
    20422, 92093, NULL, '计算机学院高水平论文与横向项目协同转化金额达到120万元', 30.00, 2,
    '突出科研成果转化与产学协同', NOW(), NOW(), '定量', 0, FALSE,
    45, 57, 'DRAFT', 273, TRUE
),
(
    20423, 92093, NULL, '商学院智库咨询报告被校外单位采纳不少于6篇', 30.00, 3,
    '突出应用研究和社会服务导向', NOW(), NOW(), '定量', 0, FALSE,
    45, 58, 'DRAFT', 276, TRUE
),
(
    20424, 92094, NULL, '完成3个核心业务系统的数据标准统一与主数据贯通', 45.00, 1,
    '数字校园办统筹数据治理专项', NOW(), NOW(), '定量', 0, FALSE,
    35, 50, 'DISTRIBUTED', 247, TRUE
),
(
    20425, 92094, NULL, '财务线上审批事项平均流转时长压缩至2个工作日以内', 25.00, 2,
    '以服务提效为目标推进流程优化', NOW(), NOW(), '定量', 0, FALSE,
    50, 46, 'DRAFT', 231, TRUE
),
(
    20426, 92095, NULL, '新增稳定运行的校企联合实践基地不少于8个', 50.00, 1,
    '强化实践基地建设与资源协同', NOW(), NOW(), '定量', 0, FALSE,
    35, 44, 'DISTRIBUTED', 223, TRUE
),
(
    20427, 92095, NULL, '依托校企项目形成可复用实践课程模块不少于10个', 50.00, 2,
    '同步支撑教学改革和就业能力提升', NOW(), NOW(), '定量', 0, FALSE,
    44, 57, 'DRAFT', 273, TRUE
);

-- -----------------------------------------------------------------------------
-- 4. 里程碑
-- -----------------------------------------------------------------------------
INSERT INTO indicator_milestone (
    id, indicator_id, milestone_name, milestone_desc, due_date, status,
    sort_order, created_at, updated_at, target_progress, is_paired
) VALUES
(
    36229, 20418, '一流课程立项清单确认',
    '完成年度课程建设计划和责任分解', DATE '2027-03-31', 'NOT_STARTED',
    1, NOW(), NOW(), 25, FALSE
),
(
    36230, 20418, '一流课程中期检查',
    '完成课程建设中期检查与问题整改', DATE '2027-06-30', 'NOT_STARTED',
    2, NOW(), NOW(), 60, FALSE
),
(
    36231, 20419, '教学成果培育选题确定',
    '完成计算机学院教学成果选题与团队组建', DATE '2027-04-15', 'NOT_STARTED',
    1, NOW(), NOW(), 30, FALSE
),
(
    36232, 20420, '过程性考核方案备案',
    '核心课程过程性考核方案全部备案完成', DATE '2027-03-15', 'NOT_STARTED',
    1, NOW(), NOW(), 40, FALSE
),
(
    36233, 20421, '省部级项目申报批次一',
    '完成第一轮项目组织、评审与上报', DATE '2027-05-31', 'NOT_STARTED',
    1, NOW(), NOW(), 35, FALSE
),
(
    36234, 20421, '省部级项目申报批次二',
    '完成第二轮重点项目跟踪与补充申报', DATE '2027-09-30', 'NOT_STARTED',
    2, NOW(), NOW(), 75, FALSE
),
(
    36235, 20422, '横向合作项目签约',
    '完成重点合作企业项目签约与预算锁定', DATE '2027-06-15', 'NOT_STARTED',
    1, NOW(), NOW(), 50, FALSE
),
(
    36236, 20424, '主数据标准发布',
    '完成人员、组织、业务编码三类主数据标准发布', DATE '2027-04-30', 'NOT_STARTED',
    1, NOW(), NOW(), 35, FALSE
),
(
    36237, 20424, '核心系统接口联调',
    '完成教务、财务、统一门户接口联调', DATE '2027-08-31', 'NOT_STARTED',
    2, NOW(), NOW(), 75, FALSE
),
(
    36238, 20425, '财务流程梳理完成',
    '完成线上审批流程梳理与审批环节压缩方案', DATE '2027-03-31', 'NOT_STARTED',
    1, NOW(), NOW(), 30, FALSE
),
(
    36239, 20426, '实践基地首批签约',
    '完成首批校企实践基地签约与导师对接', DATE '2027-05-20', 'NOT_STARTED',
    1, NOW(), NOW(), 40, FALSE
),
(
    36240, 20427, '实践课程模块初版上线',
    '首批课程模块进入试运行与反馈阶段', DATE '2027-07-15', 'NOT_STARTED',
    1, NOW(), NOW(), 50, FALSE
);

-- -----------------------------------------------------------------------------
-- 5. 审核视图
-- -----------------------------------------------------------------------------
\echo '--- 周期草案 ---'
SELECT id, cycle_name, year, start_date, end_date
FROM cycle
WHERE id = 91;

\echo '--- 计划草案 ---'
SELECT id, cycle_id, target_org_id, created_by_org_id, plan_level, status
FROM plan
WHERE id = 8;

\echo '--- 任务草案 ---'
SELECT task_id, task_name, task_type, cycle_id, org_id, plan_id
FROM sys_task
WHERE task_id BETWEEN 92092 AND 92095
ORDER BY task_id;

\echo '--- 指标草案 ---'
SELECT id, task_id, indicator_desc, owner_org_id, target_org_id, status
FROM indicator
WHERE id BETWEEN 20418 AND 20427
ORDER BY id;

\echo '--- 里程碑草案 ---'
SELECT id, indicator_id, milestone_name, due_date, target_progress
FROM indicator_milestone
WHERE id BETWEEN 36229 AND 36240
ORDER BY id;

\echo ''
\echo '审核完成后请查看上述结果；当前脚本不会写入数据库。'

ROLLBACK;

\echo ''
\echo '============================================================'
\echo 'SISM v3 审核版种子数据预演结束（已 ROLLBACK）'
\echo '============================================================'
