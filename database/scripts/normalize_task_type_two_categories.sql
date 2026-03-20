-- ============================================================
-- 任务类型规范化脚本（双类别）
-- 业务口径：仅允许 BASIC（基础性） / DEVELOPMENT（发展性）
--
-- 规则：
-- - PLAN / KEY / REGULAR / QUANTITATIVE -> BASIC
-- - SPECIAL -> DEVELOPMENT
-- - BASIC / DEVELOPMENT 保持不变
-- ============================================================

BEGIN;

-- 修复前分布
SELECT task_type, COUNT(*) AS cnt
FROM sys_task
WHERE COALESCE(is_deleted, false) = false
GROUP BY task_type
ORDER BY task_type;

WITH updated AS (
  UPDATE sys_task
  SET task_type = CASE
                    WHEN task_type IN ('PLAN', 'KEY', 'REGULAR', 'QUANTITATIVE') THEN 'BASIC'
                    WHEN task_type = 'SPECIAL' THEN 'DEVELOPMENT'
                    ELSE task_type
                  END,
      updated_at = NOW()
  WHERE COALESCE(is_deleted, false) = false
    AND task_type IN ('PLAN', 'KEY', 'REGULAR', 'QUANTITATIVE', 'SPECIAL')
  RETURNING task_id
)
SELECT COUNT(*) AS affected_rows
FROM updated;

-- 修复后分布
SELECT task_type, COUNT(*) AS cnt
FROM sys_task
WHERE COALESCE(is_deleted, false) = false
GROUP BY task_type
ORDER BY task_type;

COMMIT;
