-- ============================================================
-- 指标 task_id 一致性检查与修复脚本
-- 目标：
-- 1) 检查 indicator 表中 task_id 缺失情况（根指标/子指标）
-- 2) 按业务规则修复：子指标 task_id 为空时继承父指标 task_id
-- 3) 修复已确认的根指标缺失（人工映射）
-- 说明：
-- - 本脚本仅处理 is_deleted = false 的有效数据
-- - 可重复执行（幂等）
-- ============================================================

BEGIN;

-- 0) 修复前快照统计
SELECT 'before_all_active' AS scope,
       COUNT(*) AS total,
       COUNT(*) FILTER (WHERE task_id IS NULL) AS no_task_id,
       COUNT(*) FILTER (WHERE task_id IS NOT NULL) AS with_task_id
FROM indicator
WHERE COALESCE(is_deleted, false) = false
UNION ALL
SELECT 'before_root_active' AS scope,
       COUNT(*) AS total,
       COUNT(*) FILTER (WHERE task_id IS NULL) AS no_task_id,
       COUNT(*) FILTER (WHERE task_id IS NOT NULL) AS with_task_id
FROM indicator
WHERE COALESCE(is_deleted, false) = false
  AND parent_indicator_id IS NULL
UNION ALL
SELECT 'before_child_active' AS scope,
       COUNT(*) AS total,
       COUNT(*) FILTER (WHERE task_id IS NULL) AS no_task_id,
       COUNT(*) FILTER (WHERE task_id IS NOT NULL) AS with_task_id
FROM indicator
WHERE COALESCE(is_deleted, false) = false
  AND parent_indicator_id IS NOT NULL;

-- 1) 子指标继承父指标 task_id（核心修复）
WITH updated AS (
  UPDATE indicator child
  SET task_id = parent.task_id,
      updated_at = NOW()
  FROM indicator parent
  WHERE child.parent_indicator_id = parent.id
    AND COALESCE(child.is_deleted, false) = false
    AND COALESCE(parent.is_deleted, false) = false
    AND child.task_id IS NULL
    AND parent.task_id IS NOT NULL
  RETURNING child.id
)
SELECT 'fixed_child_inherit_parent_task_id' AS action, COUNT(*) AS affected_rows
FROM updated;

-- 1.1) 子指标继承“已软删除父指标”的 task_id（历史脏数据兜底）
-- 场景：child 活跃但 parent 已软删除，导致常规继承漏掉
WITH updated_deleted_parent AS (
  UPDATE indicator child
  SET task_id = parent.task_id,
      updated_at = NOW()
  FROM indicator parent
  WHERE child.parent_indicator_id = parent.id
    AND COALESCE(child.is_deleted, false) = false
    AND COALESCE(parent.is_deleted, false) = true
    AND child.task_id IS NULL
    AND parent.task_id IS NOT NULL
  RETURNING child.id
)
SELECT 'fixed_child_inherit_deleted_parent_task_id' AS action, COUNT(*) AS affected_rows
FROM updated_deleted_parent;

-- 2) 根指标人工映射修复（已确认脏数据）
-- 备注：
-- - indicator_id=20413 为“这是一个测试指标”，历史测试数据遗留，task_id 为空
-- - 映射到测试任务 task_id=92062（KEY）
WITH manual_map(indicator_id, task_id) AS (
  VALUES
    (20413::BIGINT, 92062::BIGINT)
),
updated_root AS (
  UPDATE indicator i
  SET task_id = m.task_id,
      updated_at = NOW()
  FROM manual_map m
  WHERE i.id = m.indicator_id
    AND COALESCE(i.is_deleted, false) = false
    AND i.parent_indicator_id IS NULL
    AND i.task_id IS NULL
    AND EXISTS (
      SELECT 1
      FROM sys_task t
      WHERE t.task_id = m.task_id
        AND COALESCE(t.is_deleted, false) = false
    )
  RETURNING i.id, m.task_id
)
SELECT 'fixed_root_manual_mapping' AS action,
       COUNT(*) AS affected_rows
FROM updated_root;

-- 3) 修复后统计
SELECT 'after_all_active' AS scope,
       COUNT(*) AS total,
       COUNT(*) FILTER (WHERE task_id IS NULL) AS no_task_id,
       COUNT(*) FILTER (WHERE task_id IS NOT NULL) AS with_task_id
FROM indicator
WHERE COALESCE(is_deleted, false) = false
UNION ALL
SELECT 'after_root_active' AS scope,
       COUNT(*) AS total,
       COUNT(*) FILTER (WHERE task_id IS NULL) AS no_task_id,
       COUNT(*) FILTER (WHERE task_id IS NOT NULL) AS with_task_id
FROM indicator
WHERE COALESCE(is_deleted, false) = false
  AND parent_indicator_id IS NULL
UNION ALL
SELECT 'after_child_active' AS scope,
       COUNT(*) AS total,
       COUNT(*) FILTER (WHERE task_id IS NULL) AS no_task_id,
       COUNT(*) FILTER (WHERE task_id IS NOT NULL) AS with_task_id
FROM indicator
WHERE COALESCE(is_deleted, false) = false
  AND parent_indicator_id IS NOT NULL;

COMMIT;

-- 4) 修复后检查：是否还有“可通过父指标补齐但未补齐”的数据
SELECT c.id AS child_id,
       c.parent_indicator_id AS parent_id,
       c.task_id AS child_task_id,
       p.task_id AS parent_task_id
FROM indicator c
JOIN indicator p ON p.id = c.parent_indicator_id
WHERE COALESCE(c.is_deleted, false) = false
  AND COALESCE(p.is_deleted, false) = false
  AND c.task_id IS NULL
  AND p.task_id IS NOT NULL
LIMIT 20;
