-- ============================================================================
-- V52__remove_indicator_is_enabled_column
-- ============================================================================
-- Purpose:
--   删除 indicator 表中冗余的 is_enabled 字段
--
-- Rationale:
--   1. is_deleted 和 is_enabled 功能重复
--   2. is_deleted 已在代码中使用（逻辑删除）
--   3. is_enabled 未在 Java 实体中映射，属于无用字段
--
-- Impact:
--   - 删除后 indicator 表只保留 is_deleted 字段用于逻辑删除
--   - 如需"禁用"功能，应使用 status 字段
-- ============================================================================

BEGIN;

-- 删除 indicator 表的 is_enabled 列
ALTER TABLE public.indicator
    DROP COLUMN IF EXISTS is_enabled;

COMMIT;

-- 验证：确保字段已删除
-- DO $$
-- BEGIN
--     IF EXISTS (
--         SELECT 1 FROM information_schema.columns
--         WHERE table_schema = 'public'
--         AND table_name = 'indicator'
--         AND column_name = 'is_enabled'
--     ) THEN
--         RAISE EXCEPTION 'is_enabled column still exists';
--     END IF;
--     RAISE NOTICE '✓ is_enabled column successfully dropped from indicator table';
-- END $$;
