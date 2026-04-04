-- =============================================================================
-- Migration: Add avatar_url column to sys_user table
-- Description: 支持用户头像功能
-- Author: Claude
-- Date: 2026-04-03
-- =============================================================================

-- 添加 avatar_url 字段
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
        AND table_name = 'sys_user'
        AND column_name = 'avatar_url'
    ) THEN
        ALTER TABLE sys_user
        ADD COLUMN avatar_url VARCHAR(500) NULL;

        COMMENT ON COLUMN sys_user.avatar_url IS '用户头像URL';

        RAISE NOTICE 'Added avatar_url column to sys_user table';
    ELSE
        RAISE NOTICE 'Column avatar_url already exists in sys_user table';
    END IF;
END $$;