-- V1.6: 为 indicator 表添加 owner_org_id 和 target_org_id 外键列
-- 迁移策略：
-- 1. 添加可空列
-- 2. 从现有的 owner_dept 和 responsible_dept 字符串映射到 sys_org.id
-- 3. 设置默认值（如果无法映射）
-- 4. 添加 NOT NULL 约束
-- 5. 添加外键约束

-- Step 1: 添加新列（允许 NULL）
ALTER TABLE indicator
ADD COLUMN IF NOT EXISTS owner_org_id BIGINT,
ADD COLUMN IF NOT EXISTS target_org_id BIGINT;

-- Step 2: 创建临时映射表（组织名称 -> sys_org.id）
-- 注意：这里假设 owner_dept 和 responsible_dept 存储的是组织名称

-- Step 3: 更新 owner_org_id（从 owner_dept 映射）
UPDATE indicator i
SET owner_org_id = so.id
FROM sys_org so
WHERE i.owner_dept IS NOT NULL
  AND i.owner_dept = so.name
  AND i.owner_org_id IS NULL;

-- Step 4: 更新 target_org_id（从 responsible_dept 映射）
UPDATE indicator i
SET target_org_id = so.id
FROM sys_org so
WHERE i.responsible_dept IS NOT NULL
  AND i.responsible_dept = so.name
  AND i.target_org_id IS NULL;

-- Step 5: 对于无法映射的记录，设置为第一个 functional 组织
-- （假设这是一个合理的默认值）
DO $$
DECLARE
    default_org_id BIGINT;
BEGIN
    -- 获取第一个 functional 组织的 ID
    SELECT id INTO default_org_id
    FROM sys_org
    WHERE type = 'functional'
    AND is_active = true
    ORDER BY sort_order
    LIMIT 1;

    -- 如果找到默认组织，更新 NULL 值
    IF default_org_id IS NOT NULL THEN
        UPDATE indicator
        SET owner_org_id = default_org_id
        WHERE owner_org_id IS NULL;

        UPDATE indicator
        SET target_org_id = default_org_id
        WHERE target_org_id IS NULL;

        RAISE NOTICE '已将 NULL 值设置为默认组织 ID: %', default_org_id;
    ELSE
        RAISE EXCEPTION '无法找到默认组织（functional）';
    END IF;
END $$;

-- Step 6: 验证没有 NULL 值
DO $$
DECLARE
    null_owner_count INTEGER;
    null_target_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO null_owner_count FROM indicator WHERE owner_org_id IS NULL;
    SELECT COUNT(*) INTO null_target_count FROM indicator WHERE target_org_id IS NULL;
    
    IF null_owner_count > 0 OR null_target_count > 0 THEN
        RAISE EXCEPTION '仍有 NULL 值: owner_org_id=%, target_org_id=%', null_owner_count, null_target_count;
    END IF;
    
    RAISE NOTICE '验证通过：所有记录都有有效的组织 ID';
END $$;

-- Step 7: 添加 NOT NULL 约束
ALTER TABLE indicator
ALTER COLUMN owner_org_id SET NOT NULL,
ALTER COLUMN target_org_id SET NOT NULL;

-- Step 8: 添加外键约束
ALTER TABLE indicator
ADD CONSTRAINT fk_indicator_owner_org
    FOREIGN KEY (owner_org_id)
    REFERENCES sys_org(id)
    ON DELETE RESTRICT,
ADD CONSTRAINT fk_indicator_target_org
    FOREIGN KEY (target_org_id)
    REFERENCES sys_org(id)
    ON DELETE RESTRICT;

-- Step 9: 添加索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_indicator_owner_org ON indicator(owner_org_id);
CREATE INDEX IF NOT EXISTS idx_indicator_target_org ON indicator(target_org_id);

-- Step 10: 添加 level 列（如果不存在）
ALTER TABLE indicator
ADD COLUMN IF NOT EXISTS level VARCHAR(20);

-- 为现有记录设置默认 level 值
UPDATE indicator
SET level = CASE
    WHEN parent_indicator_id IS NULL THEN 'PRIMARY'
    ELSE 'SECONDARY'
END
WHERE level IS NULL;

-- 设置 level 为 NOT NULL
ALTER TABLE indicator
ALTER COLUMN level SET NOT NULL;

-- 完成
RAISE NOTICE '✓ indicator 表迁移完成';
RAISE NOTICE '  - 添加了 owner_org_id 和 target_org_id 列';
RAISE NOTICE '  - 添加了外键约束';
RAISE NOTICE '  - 添加了索引';
RAISE NOTICE '  - 添加了 level 列';
