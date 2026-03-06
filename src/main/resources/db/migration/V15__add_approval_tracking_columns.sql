-- =============================================================================
-- V15: 添加审批跟踪字段到 audit_instance 表
-- =============================================================================
-- 目的：修复 "column approved_approvers does not exist" 错误
-- 问题：实体类中定义了这些字段，但数据库表中缺失
-- 创建时间：2026-03-06
-- =============================================================================

-- 添加审批跟踪字段
DO $$
BEGIN
    -- Add pending_approvers column (待审批人列表)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'audit_instance' AND column_name = 'pending_approvers'
    ) THEN
        ALTER TABLE audit_instance ADD COLUMN pending_approvers BIGINT[];
        COMMENT ON COLUMN audit_instance.pending_approvers IS '待审批人ID列表';
    END IF;
    
    -- Add approved_approvers column (已审批人列表)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'audit_instance' AND column_name = 'approved_approvers'
    ) THEN
        ALTER TABLE audit_instance ADD COLUMN approved_approvers BIGINT[];
        COMMENT ON COLUMN audit_instance.approved_approvers IS '已审批人ID列表';
    END IF;
    
    -- Add rejected_approvers column (已拒绝人列表)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'audit_instance' AND column_name = 'rejected_approvers'
    ) THEN
        ALTER TABLE audit_instance ADD COLUMN rejected_approvers BIGINT[];
        COMMENT ON COLUMN audit_instance.rejected_approvers IS '已拒绝人ID列表';
    END IF;
END $$;

-- 初始化现有记录的值为空数组
UPDATE audit_instance 
SET 
    pending_approvers = COALESCE(pending_approvers, '{}'),
    approved_approvers = COALESCE(approved_approvers, '{}'),
    rejected_approvers = COALESCE(rejected_approvers, '{}')
WHERE 
    pending_approvers IS NULL 
    OR approved_approvers IS NULL 
    OR rejected_approvers IS NULL;
