-- =====================================================
-- Migration: V1.2 - 添加部门字段到 indicator 表
-- Date: 2026-02-08
-- Description: 添加 owner_dept 和 responsible_dept 字段以支持数据看板功能
-- =====================================================

-- 1. 添加部门字段
ALTER TABLE indicator 
ADD COLUMN IF NOT EXISTS owner_dept VARCHAR(100),
ADD COLUMN IF NOT EXISTS responsible_dept VARCHAR(100);

-- 2. 添加索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_indicator_owner_dept ON indicator(owner_dept);
CREATE INDEX IF NOT EXISTS idx_indicator_responsible_dept ON indicator(responsible_dept);

-- 3. 从 indicator_desc 中提取部门信息并填充 responsible_dept
-- 格式："{指标描述} - {部门名称}"
UPDATE indicator
SET responsible_dept = TRIM(SPLIT_PART(indicator_desc, '-', 2))
WHERE indicator_desc LIKE '%-%'
  AND TRIM(SPLIT_PART(indicator_desc, '-', 2)) != ''
  AND responsible_dept IS NULL;

-- 4. 清理无效的部门名称（只保留在 sys_org 中存在的部门）
UPDATE indicator
SET responsible_dept = NULL
WHERE responsible_dept IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_org WHERE name = indicator.responsible_dept
  );

-- 5. 添加字段注释
COMMENT ON COLUMN indicator.owner_dept IS '来源部门/下发部门（职能部门）';
COMMENT ON COLUMN indicator.responsible_dept IS '责任部门/承接部门（可以是职能部门或二级学院）';

-- 6. 验证数据
DO $$
DECLARE
  total_count INTEGER;
  has_responsible INTEGER;
  has_owner INTEGER;
BEGIN
  SELECT COUNT(*) INTO total_count FROM indicator WHERE is_deleted = false;
  SELECT COUNT(*) INTO has_responsible FROM indicator WHERE responsible_dept IS NOT NULL AND is_deleted = false;
  SELECT COUNT(*) INTO has_owner FROM indicator WHERE owner_dept IS NOT NULL AND is_deleted = false;
  
  RAISE NOTICE '=== 迁移完成统计 ===';
  RAISE NOTICE '总指标数: %', total_count;
  RAISE NOTICE '已设置 responsible_dept: % (%.1f%%)', has_responsible, (has_responsible::FLOAT / total_count * 100);
  RAISE NOTICE '已设置 owner_dept: % (%.1f%%)', has_owner, (has_owner::FLOAT / total_count * 100);
END $$;
