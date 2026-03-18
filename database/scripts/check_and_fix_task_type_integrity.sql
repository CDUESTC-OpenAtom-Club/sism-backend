-- ============================================================
-- sys_task.task_type 一致性检查与安全清理脚本
--
-- 目标：
-- 1. 输出当前 task_type 分布与异常数量
-- 2. 自动将历史遗留枚举归一到双类别口径
--    - KEY / REGULAR / QUANTITATIVE -> BASIC
--    - SPECIAL -> DEVELOPMENT
-- 3. 若仍存在 NULL / 空白 / 未知值，则直接报错并回滚
--
-- 用法：
--   psql "$DB_URL" -f database/scripts/check_and_fix_task_type_integrity.sql
-- ============================================================

BEGIN;

-- ------------------------------------------------------------
-- A. 修复前快照
-- ------------------------------------------------------------
SELECT 'before_distribution' AS section, COALESCE(task_type, '<NULL>') AS task_type, COUNT(*) AS cnt
FROM sys_task
WHERE COALESCE(is_deleted, false) = false
GROUP BY COALESCE(task_type, '<NULL>')
ORDER BY task_type;

SELECT 'before_invalid_count' AS metric, COUNT(*) AS cnt
FROM sys_task
WHERE COALESCE(is_deleted, false) = false
  AND (
    task_type IS NULL
    OR BTRIM(task_type) = ''
    OR UPPER(BTRIM(task_type)) NOT IN (
      'BASIC', 'DEVELOPMENT', 'KEY', 'REGULAR', 'SPECIAL', 'QUANTITATIVE'
    )
  );

-- ------------------------------------------------------------
-- B. 先做格式归一，再收敛历史枚举
-- ------------------------------------------------------------
WITH normalized AS (
  UPDATE sys_task
  SET task_type = UPPER(BTRIM(task_type)),
      updated_at = NOW()
  WHERE COALESCE(is_deleted, false) = false
    AND task_type IS NOT NULL
    AND task_type <> UPPER(BTRIM(task_type))
  RETURNING task_id
)
SELECT 'normalized_case_and_whitespace' AS action, COUNT(*) AS affected_rows
FROM normalized;

WITH remapped AS (
  UPDATE sys_task
  SET task_type = CASE
                    WHEN task_type IN ('KEY', 'REGULAR', 'QUANTITATIVE') THEN 'BASIC'
                    WHEN task_type = 'SPECIAL' THEN 'DEVELOPMENT'
                    ELSE task_type
                  END,
      updated_at = NOW()
  WHERE COALESCE(is_deleted, false) = false
    AND task_type IN ('KEY', 'REGULAR', 'QUANTITATIVE', 'SPECIAL')
  RETURNING task_id
)
SELECT 'remapped_legacy_task_types' AS action, COUNT(*) AS affected_rows
FROM remapped;

-- ------------------------------------------------------------
-- C. 自动修复后，若仍有脏数据则终止并回滚
-- ------------------------------------------------------------
DO $$
DECLARE
  invalid_count INTEGER;
  invalid_examples TEXT;
BEGIN
  SELECT COUNT(*)
  INTO invalid_count
  FROM sys_task
  WHERE COALESCE(is_deleted, false) = false
    AND (
      task_type IS NULL
      OR BTRIM(task_type) = ''
      OR task_type NOT IN ('BASIC', 'DEVELOPMENT')
    );

  IF invalid_count > 0 THEN
    SELECT STRING_AGG(
             FORMAT('[%s] %s => %s', task_id, task_name, COALESCE(task_type, '<NULL>')),
             E'\n'
           )
    INTO invalid_examples
    FROM (
      SELECT task_id, task_name, task_type
      FROM sys_task
      WHERE COALESCE(is_deleted, false) = false
        AND (
          task_type IS NULL
          OR BTRIM(task_type) = ''
          OR task_type NOT IN ('BASIC', 'DEVELOPMENT')
        )
      ORDER BY task_id
      LIMIT 20
    ) sample_rows;

    RAISE EXCEPTION
      '仍存在无法自动清理的 task_type 脏数据，共 % 条。样例:%',
      invalid_count,
      E'\n' || COALESCE(invalid_examples, '<none>');
  END IF;
END $$;

-- ------------------------------------------------------------
-- D. 修复后快照
-- ------------------------------------------------------------
SELECT 'after_distribution' AS section, task_type, COUNT(*) AS cnt
FROM sys_task
WHERE COALESCE(is_deleted, false) = false
GROUP BY task_type
ORDER BY task_type;

SELECT 'after_invalid_count' AS metric, COUNT(*) AS cnt
FROM sys_task
WHERE COALESCE(is_deleted, false) = false
  AND (
    task_type IS NULL
    OR BTRIM(task_type) = ''
    OR task_type NOT IN ('BASIC', 'DEVELOPMENT')
  );

COMMIT;
