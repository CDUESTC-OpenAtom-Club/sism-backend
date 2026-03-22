-- ============================================================
-- V38__fix_task_type_null_and_remove_redundant_column.sql
-- 修复任务类型为空的问题并清理冗余字段
--
-- 问题:
-- 1. 部分任务的 task_type 字段为空 (NULL)
-- 2. 存在冗余字段 'type' (与 task_type 重复)
--
-- 修复:
-- 1. 如果 type 字段有值，用它来填充 task_type (如果 task_type 为 NULL)
-- 2. 剩余的 NULL task_type 设置为 'BASIC' (基础性)
-- 3. 删除冗余的 type 字段
-- ============================================================

BEGIN;

-- 步骤1: 查看修复前的 task_type 分布
SELECT '修复前 task_type 分布:' AS status;
SELECT task_type, COUNT(*) AS cnt
FROM sys_task
WHERE COALESCE(is_deleted, false) = false
GROUP BY task_type
ORDER BY task_type;

-- 步骤2: 查看修复前的 type 分布
SELECT '修复前 type 分布:' AS status;
SELECT type, COUNT(*) AS cnt
FROM sys_task
WHERE COALESCE(is_deleted, false) = false
GROUP BY type
ORDER BY type;

-- 步骤3: 统计 NULL task_type 的数量
SELECT 'NULL task_type 数量:' AS status, COUNT(*) AS cnt
FROM sys_task
WHERE task_type IS NULL AND COALESCE(is_deleted, false) = false;

-- 步骤4: 删除旧的 CHECK 约束 (如果存在)
DO $$
BEGIN
    -- 删除旧的 CHECK 约束，只允许 BASIC 和 DEVELOPMENT
    ALTER TABLE sys_task DROP CONSTRAINT IF EXISTS strategic_task_task_type_check;
    RAISE NOTICE '已删除旧的 CHECK 约束';
END $$;

-- 步骤5: 用 type 字段的值填充 NULL 的 task_type
WITH updated AS (
  UPDATE sys_task
  SET task_type = type::text,
      updated_at = NOW()
  WHERE task_type IS NULL
    AND type IS NOT NULL
    AND COALESCE(is_deleted, false) = false
  RETURNING task_id
)
SELECT '步骤5: 从 type 填充 task_type' AS status, COUNT(*) AS affected_rows
FROM updated;

-- 步骤6: 剩余的 NULL task_type 设置为 'BASIC'
WITH updated AS (
  UPDATE sys_task
  SET task_type = 'BASIC',
      updated_at = NOW()
  WHERE task_type IS NULL
    AND COALESCE(is_deleted, false) = false
  RETURNING task_id
)
SELECT '步骤6: 设置剩余 NULL 为 BASIC' AS status, COUNT(*) AS affected_rows
FROM updated;

-- 步骤7: 删除冗余的 type 字段
-- 注意: 先将字段设置为允许 NULL，然后删除
DO $$
BEGIN
    -- 首先将 type 字段改为 nullable
    ALTER TABLE sys_task ALTER COLUMN type DROP NOT NULL;
    RAISE NOTICE '已将 sys_task.type 设为 nullable';

    -- 删除冗余的 type 字段
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'sys_task' AND column_name = 'type'
    ) THEN
        ALTER TABLE sys_task DROP COLUMN IF EXISTS type;
        RAISE NOTICE '已删除 sys_task.type 字段';
    ELSE
        RAISE NOTICE 'sys_task.type 字段不存在，跳过删除';
    END IF;
END $$;

-- 步骤8: 验证修复后的 task_type 分布
SELECT '修复后 task_type 分布:' AS status;
SELECT task_type, COUNT(*) AS cnt
FROM sys_task
WHERE COALESCE(is_deleted, false) = false
GROUP BY task_type
ORDER BY task_type;

-- 步骤9: 确认 type 字段已删除
SELECT '验证 type 字段已删除:' AS status;
SELECT COUNT(*) AS column_exists
FROM information_schema.columns
WHERE table_name = 'sys_task' AND column_name = 'type';

COMMIT;
