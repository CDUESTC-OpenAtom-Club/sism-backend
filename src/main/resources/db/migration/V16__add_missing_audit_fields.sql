-- =============================================================================
-- V16: 添加 audit_instance 表缺失的字段
-- =============================================================================
-- 目的：修复 "column completed_at does not exist" 等错误
-- 创建时间：2026-03-06
-- =============================================================================

DO $$
BEGIN
    -- Add completed_at column
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'audit_instance' AND column_name = 'completed_at'
    ) THEN
        ALTER TABLE audit_instance ADD COLUMN completed_at TIMESTAMP;
        COMMENT ON COLUMN audit_instance.completed_at IS '审批完成时间';
    END IF;
END $$;
