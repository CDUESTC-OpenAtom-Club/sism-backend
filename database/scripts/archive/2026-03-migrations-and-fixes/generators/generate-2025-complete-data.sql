-- ============================================================================
-- 生成完整的 2025 年测试数据
-- 基于 2026 年数据结构，复制任务、指标（包括子指标）和里程碑
-- ============================================================================

BEGIN;

-- 步骤 1: 创建 2025 年的任务（基于 2026 年任务）
INSERT INTO sys_task (
    plan_id,
    cycle_id,
    task_name,
    task_desc,
    task_type,
    org_id,
    created_by_org_id,
    sort_order,
    remark,
    created_at,
    updated_at,
    is_deleted
)
SELECT 
    plan_id,
    7 AS cycle_id,  -- 使用 2025 年的 cycle_id
    REPLACE(task_name, '2026', '2025') AS task_name,
    REPLACE(COALESCE(task_desc, ''), '2026', '2025') AS task_desc,
    task_type,
    org_id,
    created_by_org_id,
    sort_order,
    REPLACE(COALESCE(remark, ''), '2026', '2025') AS remark,
    NOW() AS created_at,
    NOW() AS updated_at,
    false AS is_deleted
FROM sys_task
WHERE cycle_id = 1  -- 复制所有 cycle_id = 1 的任务（实际上是 2026 年的任务）
AND is_deleted = false
ON CONFLICT DO NOTHING;

-- 步骤 2: 创建任务映射表
CREATE TEMP TABLE task_mapping AS
SELECT 
    old_task.task_id AS old_task_id,
    new_task.task_id AS new_task_id
FROM sys_task old_task
JOIN sys_task new_task ON 
    REPLACE(old_task.task_name, '2026', '2025') = new_task.task_name
    AND old_task.cycle_id = 1
    AND new_task.cycle_id = 7;

-- 步骤 3: 插入父指标（parent_indicator_id IS NULL）
INSERT INTO indicator (
    task_id,
    parent_indicator_id,
    level,
    owner_org_id,
    target_org_id,
    indicator_desc,
    weight_percent,
    sort_order,
    remark,
    created_at,
    updated_at,
    type,
    progress,
    is_deleted,
    status,
    actual_value,
    target_value,
    unit,
    responsible_person,
    can_withdraw,
    is_qualitative,
    pending_progress,
    pending_remark,
    pending_attachments,
    progress_approval_status,
    status_audit,
    type1,
    type2,
    owner_dept,
    responsible_dept,
    "year"
)
SELECT 
    tm.new_task_id AS task_id,
    NULL AS parent_indicator_id,
    i.level,
    i.owner_org_id,
    i.target_org_id,
    REPLACE(i.indicator_desc, '2026', '2025') AS indicator_desc,
    i.weight_percent,
    i.sort_order,
    REPLACE(COALESCE(i.remark, ''), '2026', '2025') AS remark,
    NOW() AS created_at,
    NOW() AS updated_at,
    i.type,
    FLOOR(RANDOM() * 61 + 20)::INTEGER AS progress,
    false AS is_deleted,
    i.status,
    i.actual_value,
    i.target_value,
    i.unit,
    i.responsible_person,
    i.can_withdraw,
    i.is_qualitative,
    i.pending_progress,
    REPLACE(COALESCE(i.pending_remark, ''), '2026', '2025') AS pending_remark,
    i.pending_attachments,
    i.progress_approval_status,
    i.status_audit,
    i.type1,
    i.type2,
    i.owner_dept,
    i.responsible_dept,
    2025 AS "year"
FROM indicator i
JOIN task_mapping tm ON i.task_id = tm.old_task_id
WHERE i.year = 2026
AND i.is_deleted = false
AND i.parent_indicator_id IS NULL;

-- 步骤 4: 创建父指标映射表
CREATE TEMP TABLE parent_indicator_mapping AS
SELECT 
    old_ind.id AS old_indicator_id,
    new_ind.id AS new_indicator_id
FROM indicator old_ind
JOIN indicator new_ind ON 
    REPLACE(old_ind.indicator_desc, '2026', '2025') = new_ind.indicator_desc
    AND old_ind.year = 2026
    AND new_ind.year = 2025
    AND old_ind.parent_indicator_id IS NULL
    AND new_ind.parent_indicator_id IS NULL;

-- 步骤 5: 插入子指标（parent_indicator_id IS NOT NULL）
INSERT INTO indicator (
    task_id,
    parent_indicator_id,
    level,
    owner_org_id,
    target_org_id,
    indicator_desc,
    weight_percent,
    sort_order,
    remark,
    created_at,
    updated_at,
    type,
    progress,
    is_deleted,
    status,
    actual_value,
    target_value,
    unit,
    responsible_person,
    can_withdraw,
    is_qualitative,
    pending_progress,
    pending_remark,
    pending_attachments,
    progress_approval_status,
    status_audit,
    type1,
    type2,
    owner_dept,
    responsible_dept,
    "year"
)
SELECT 
    tm.new_task_id AS task_id,
    pim.new_indicator_id AS parent_indicator_id,
    i.level,
    i.owner_org_id,
    i.target_org_id,
    REPLACE(i.indicator_desc, '2026', '2025') AS indicator_desc,
    i.weight_percent,
    i.sort_order,
    REPLACE(COALESCE(i.remark, ''), '2026', '2025') AS remark,
    NOW() AS created_at,
    NOW() AS updated_at,
    i.type,
    FLOOR(RANDOM() * 61 + 20)::INTEGER AS progress,
    false AS is_deleted,
    i.status,
    i.actual_value,
    i.target_value,
    i.unit,
    i.responsible_person,
    i.can_withdraw,
    i.is_qualitative,
    i.pending_progress,
    REPLACE(COALESCE(i.pending_remark, ''), '2026', '2025') AS pending_remark,
    i.pending_attachments,
    i.progress_approval_status,
    i.status_audit,
    i.type1,
    i.type2,
    i.owner_dept,
    i.responsible_dept,
    2025 AS "year"
FROM indicator i
JOIN task_mapping tm ON i.task_id = tm.old_task_id
JOIN parent_indicator_mapping pim ON i.parent_indicator_id = pim.old_indicator_id
WHERE i.year = 2026
AND i.is_deleted = false
AND i.parent_indicator_id IS NOT NULL;

-- 步骤 6: 创建完整的指标映射表（包括父指标和子指标）
CREATE TEMP TABLE all_indicator_mapping AS
SELECT old_indicator_id, new_indicator_id FROM parent_indicator_mapping
UNION ALL
SELECT 
    old_ind.id AS old_indicator_id,
    new_ind.id AS new_indicator_id
FROM indicator old_ind
JOIN indicator new_ind ON 
    REPLACE(old_ind.indicator_desc, '2026', '2025') = new_ind.indicator_desc
    AND old_ind.year = 2026
    AND new_ind.year = 2025
    AND old_ind.parent_indicator_id IS NOT NULL
    AND new_ind.parent_indicator_id IS NOT NULL;

-- 步骤 7: 创建里程碑
INSERT INTO indicator_milestone (
    indicator_id,
    milestone_name,
    milestone_desc,
    due_date,
    status,
    sort_order,
    created_at,
    updated_at,
    target_progress,
    is_paired,
    inherited_from
)
SELECT 
    aim.new_indicator_id AS indicator_id,
    REPLACE(m.milestone_name, '2026', '2025') AS milestone_name,
    REPLACE(COALESCE(m.milestone_desc, ''), '2026', '2025') AS milestone_desc,
    m.due_date - INTERVAL '1 year' AS due_date,
    m.status,
    m.sort_order,
    NOW() AS created_at,
    NOW() AS updated_at,
    m.target_progress,
    m.is_paired,
    NULL AS inherited_from
FROM indicator_milestone m
JOIN all_indicator_mapping aim ON m.indicator_id = aim.old_indicator_id;

-- 清理临时表
DROP TABLE IF EXISTS task_mapping;
DROP TABLE IF EXISTS parent_indicator_mapping;
DROP TABLE IF EXISTS all_indicator_mapping;

COMMIT;

-- 查询结果统计
SELECT 
    '2025年任务数' AS 统计项,
    COUNT(*) AS 数量
FROM sys_task
WHERE cycle_id = 7
UNION ALL
SELECT 
    '2025年父指标数' AS 统计项,
    COUNT(*) AS 数量
FROM indicator
WHERE year = 2025 AND parent_indicator_id IS NULL
UNION ALL
SELECT 
    '2025年子指标数' AS 统计项,
    COUNT(*) AS 数量
FROM indicator
WHERE year = 2025 AND parent_indicator_id IS NOT NULL
UNION ALL
SELECT 
    '2025年指标总数' AS 统计项,
    COUNT(*) AS 数量
FROM indicator
WHERE year = 2025
UNION ALL
SELECT 
    '2025年里程碑数' AS 统计项,
    COUNT(*) AS 数量
FROM indicator_milestone m
JOIN indicator i ON m.indicator_id = i.id
WHERE i.year = 2025;
