-- ============================================================
-- 核心业务数据一致性检查与清理脚本
-- 范围：sys_task / plan / indicator
--
-- 修复策略（可重复执行）：
-- 1) 修复 task.cycle_id 无效（按 task_name 中年份映射到 cycle.year）
-- 2) 规范“一个计划一个周期”：将 cycle=7 的任务迁移到 plan_id=7
-- 3) 自动补齐缺失的 plan（按已存在 task 引用生成占位 plan）
-- 4) 修复 active 子指标引用 deleted 父指标（解除挂载，parent_indicator_id=NULL）
-- ============================================================

BEGIN;

-- ------------------------------------------------------------
-- A. 修复前快照
-- ------------------------------------------------------------
SELECT 'before_task_plan_orphan' AS metric, COUNT(*) AS cnt
FROM sys_task t
LEFT JOIN plan p ON p.id = t.plan_id AND COALESCE(p.is_deleted, false) = false
WHERE COALESCE(t.is_deleted, false) = false
  AND (t.plan_id IS NULL OR p.id IS NULL)
UNION ALL
SELECT 'before_task_cycle_orphan' AS metric, COUNT(*) AS cnt
FROM sys_task t
LEFT JOIN cycle c ON c.id = t.cycle_id
WHERE COALESCE(t.is_deleted, false) = false
  AND (t.cycle_id IS NULL OR c.id IS NULL)
UNION ALL
SELECT 'before_indicator_parent_deleted' AS metric, COUNT(*) AS cnt
FROM indicator c
JOIN indicator p ON p.id = c.parent_indicator_id
WHERE COALESCE(c.is_deleted, false) = false
  AND COALESCE(p.is_deleted, false) = true;

-- ------------------------------------------------------------
-- B. 修复 task.cycle_id 无效（按任务名年份映射）
-- ------------------------------------------------------------
WITH task_year AS (
  SELECT t.task_id,
         NULLIF(SUBSTRING(t.task_name FROM '(20[0-9]{2})'), '')::INT AS year_from_name
  FROM sys_task t
  LEFT JOIN cycle c ON c.id = t.cycle_id
  WHERE COALESCE(t.is_deleted, false) = false
    AND (t.cycle_id IS NULL OR c.id IS NULL)
),
matched AS (
  SELECT ty.task_id, c.id AS cycle_id
  FROM task_year ty
  JOIN cycle c ON c.year = ty.year_from_name
  WHERE ty.year_from_name IS NOT NULL
),
updated AS (
  UPDATE sys_task t
  SET cycle_id = m.cycle_id,
      updated_at = NOW()
  FROM matched m
  WHERE t.task_id = m.task_id
  RETURNING t.task_id
)
SELECT 'fixed_task_cycle_by_name_year' AS action, COUNT(*) AS affected_rows
FROM updated;

-- B.1) 兜底：任务名无年份时，按 created_at 年份映射 cycle.year
WITH task_year_fallback AS (
  SELECT t.task_id,
         EXTRACT(YEAR FROM t.created_at)::INT AS year_from_created_at
  FROM sys_task t
  LEFT JOIN cycle c ON c.id = t.cycle_id
  WHERE COALESCE(t.is_deleted, false) = false
    AND (t.cycle_id IS NULL OR c.id IS NULL)
),
matched AS (
  SELECT ty.task_id, c.id AS cycle_id
  FROM task_year_fallback ty
  JOIN cycle c ON c.year = ty.year_from_created_at
),
updated AS (
  UPDATE sys_task t
  SET cycle_id = m.cycle_id,
      updated_at = NOW()
  FROM matched m
  WHERE t.task_id = m.task_id
  RETURNING t.task_id
)
SELECT 'fixed_task_cycle_by_created_at_year' AS action, COUNT(*) AS affected_rows
FROM updated;

-- ------------------------------------------------------------
-- C. 规范计划拆分：cycle=7 的任务迁移到 plan_id=7
-- 说明：历史数据中 plan_id=1 混挂多个周期，此处按周期拆分。
-- ------------------------------------------------------------
WITH moved AS (
  UPDATE sys_task t
  SET plan_id = 7,
      updated_at = NOW()
  WHERE COALESCE(t.is_deleted, false) = false
    AND t.plan_id = 1
    AND t.cycle_id = 7
  RETURNING t.task_id
)
SELECT 'moved_tasks_plan1_to_plan7_for_cycle7' AS action, COUNT(*) AS affected_rows
FROM moved;

-- ------------------------------------------------------------
-- D. 自动补齐缺失 plan（基于活跃任务引用）
-- ------------------------------------------------------------
WITH plan_seed AS (
  SELECT
    t.plan_id AS id,
    MIN(t.cycle_id) AS cycle_id,
    COALESCE(MIN(t.org_id), MIN(t.created_by_org_id), 35) AS target_org_id,
    COALESCE(MIN(t.created_by_org_id), MIN(t.org_id), 35) AS created_by_org_id
  FROM sys_task t
  LEFT JOIN plan p ON p.id = t.plan_id
  WHERE COALESCE(t.is_deleted, false) = false
    AND t.plan_id IS NOT NULL
    AND p.id IS NULL
  GROUP BY t.plan_id
),
inserted AS (
  INSERT INTO plan (
    id, cycle_id, target_org_id, created_by_org_id,
    plan_level, status, is_deleted, created_at, updated_at
  )
  SELECT
    s.id, s.cycle_id, s.target_org_id, s.created_by_org_id,
    'STRAT_TO_FUNC', 'DRAFT', false, NOW(), NOW()
  FROM plan_seed s
  RETURNING id
)
SELECT 'inserted_missing_plans' AS action, COUNT(*) AS affected_rows
FROM inserted;

-- ------------------------------------------------------------
-- D.1) 修复 plan.cycle_id 无效（按该 plan 下活跃任务主周期回填）
-- ------------------------------------------------------------
WITH invalid_plan AS (
  SELECT p.id
  FROM plan p
  LEFT JOIN cycle c ON c.id = p.cycle_id
  WHERE COALESCE(p.is_deleted, false) = false
    AND (p.cycle_id IS NULL OR c.id IS NULL)
),
task_cycle_rank AS (
  SELECT t.plan_id AS id,
         t.cycle_id,
         COUNT(*) AS cnt,
         ROW_NUMBER() OVER (PARTITION BY t.plan_id ORDER BY COUNT(*) DESC, t.cycle_id DESC) AS rn
  FROM sys_task t
  JOIN invalid_plan p ON p.id = t.plan_id
  JOIN cycle c ON c.id = t.cycle_id
  WHERE COALESCE(t.is_deleted, false) = false
  GROUP BY t.plan_id, t.cycle_id
),
chosen AS (
  SELECT id, cycle_id
  FROM task_cycle_rank
  WHERE rn = 1
),
updated_plan AS (
  UPDATE plan p
  SET cycle_id = c.cycle_id,
      updated_at = NOW()
  FROM chosen c
  WHERE p.id = c.id
  RETURNING p.id
)
SELECT 'fixed_plan_cycle_by_task_majority' AS action, COUNT(*) AS affected_rows
FROM updated_plan;

-- ------------------------------------------------------------
-- E. 清理 active 子指标 -> deleted 父指标 的脏挂载
-- ------------------------------------------------------------
WITH detached AS (
  UPDATE indicator c
  SET parent_indicator_id = NULL,
      updated_at = NOW()
  FROM indicator p
  WHERE c.parent_indicator_id = p.id
    AND COALESCE(c.is_deleted, false) = false
    AND COALESCE(p.is_deleted, false) = true
  RETURNING c.id
)
SELECT 'detached_children_from_deleted_parent' AS action, COUNT(*) AS affected_rows
FROM detached;

-- ------------------------------------------------------------
-- F. 修复后快照
-- ------------------------------------------------------------
SELECT 'after_task_plan_orphan' AS metric, COUNT(*) AS cnt
FROM sys_task t
LEFT JOIN plan p ON p.id = t.plan_id AND COALESCE(p.is_deleted, false) = false
WHERE COALESCE(t.is_deleted, false) = false
  AND (t.plan_id IS NULL OR p.id IS NULL)
UNION ALL
SELECT 'after_task_cycle_orphan' AS metric, COUNT(*) AS cnt
FROM sys_task t
LEFT JOIN cycle c ON c.id = t.cycle_id
WHERE COALESCE(t.is_deleted, false) = false
  AND (t.cycle_id IS NULL OR c.id IS NULL)
UNION ALL
SELECT 'after_indicator_parent_deleted' AS metric, COUNT(*) AS cnt
FROM indicator c
JOIN indicator p ON p.id = c.parent_indicator_id
WHERE COALESCE(c.is_deleted, false) = false
  AND COALESCE(p.is_deleted, false) = true;

COMMIT;
