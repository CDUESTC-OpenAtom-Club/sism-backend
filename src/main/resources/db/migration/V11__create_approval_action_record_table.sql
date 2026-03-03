-- V8__create_approval_action_record_table.sql
-- Approval Action Record Table
-- Created: 2026-03-02
-- Description: Track all approval workflow actions including approve/reject/delegate/transfer

-- =====================================================
-- Create approval_action_record table
-- =====================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'approval_action_record') THEN
        CREATE TABLE approval_action_record (
            id BIGSERIAL PRIMARY KEY,
            instance_id BIGINT NOT NULL REFERENCES approval_instance(id) ON DELETE CASCADE,
            step_id BIGINT REFERENCES approval_step(id) ON DELETE SET NULL,
            actor_id BIGINT NOT NULL REFERENCES sys_user(id),
            actor_name VARCHAR(100),
            action VARCHAR(50) NOT NULL,
            comment TEXT,
            acted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            target_user_id BIGINT REFERENCES sys_user(id),
            target_user_name VARCHAR(100),
            ip_address VARCHAR(50),
            before_status VARCHAR(50),
            after_status VARCHAR(50),
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        COMMENT ON TABLE approval_action_record IS '审批操作记录表 - 记录审批流程中的所有操作';
        COMMENT ON COLUMN approval_action_record.instance_id IS '审批实例ID';
        COMMENT ON COLUMN approval_action_record.step_id IS '审批步骤ID';
        COMMENT ON COLUMN approval_action_record.actor_id IS '操作人ID';
        COMMENT ON COLUMN approval_action_record.actor_name IS '操作人姓名';
        COMMENT ON COLUMN approval_action_record.action IS '操作类型: APPROVE, REJECT, DELEGATE, TRANSFER, WITHDRAW, REVOKE, COMMENT';
        COMMENT ON COLUMN approval_action_record.comment IS '操作意见/备注';
        COMMENT ON COLUMN approval_action_record.acted_at IS '操作时间';
        COMMENT ON COLUMN approval_action_record.target_user_id IS '目标用户ID（加签/转审时使用）';
        COMMENT ON COLUMN approval_action_record.target_user_name IS '目标用户姓名';
        COMMENT ON COLUMN approval_action_record.ip_address IS '操作IP地址';
        COMMENT ON COLUMN approval_action_record.before_status IS '操作前状态';
        COMMENT ON COLUMN approval_action_record.after_status IS '操作后状态';

        CREATE INDEX idx_approval_action_instance ON approval_action_record(instance_id);
        CREATE INDEX idx_approval_action_step ON approval_action_record(step_id);
        CREATE INDEX idx_approval_action_actor ON approval_action_record(actor_id);
        CREATE INDEX idx_approval_action_action ON approval_action_record(action);
        CREATE INDEX idx_approval_action_acted_at ON approval_action_record(acted_at);
        CREATE INDEX idx_approval_action_target ON approval_action_record(target_user_id);

        RAISE NOTICE 'Table approval_action_record created successfully';
    ELSE
        RAISE NOTICE 'Table approval_action_record already exists';
    END IF;
END $$;

-- =====================================================
-- Add approval_mode column to approval_instance if not exists
-- =====================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'approval_instance' AND column_name = 'approval_mode') THEN
        ALTER TABLE approval_instance ADD COLUMN approval_mode VARCHAR(20) DEFAULT 'SEQUENTIAL';
        COMMENT ON COLUMN approval_instance.approval_mode IS '审批模式: SEQUENTIAL=串行, PARALLEL_ANY=任一通过, PARALLEL_ALL=全部通过, MAJORITY=多数决';
    END IF;
END $$;

-- =====================================================
-- Add data to approval_instance: populate approval_mode from existing records
-- =====================================================
DO $$
BEGIN
    -- For existing records with multiple pending approvers, set to PARALLEL_ALL
    UPDATE approval_instance
    SET approval_mode = 'PARALLEL_ALL'
    WHERE approval_mode IS NULL
      AND cardinality(pending_approvers) > 1;

    -- For remaining records, set to SEQUENTIAL
    UPDATE approval_instance
    SET approval_mode = 'SEQUENTIAL'
    WHERE approval_mode IS NULL;
END $$;

-- =====================================================
-- Create trigger to update approval_mode based on pending_approvers
-- =====================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'update_approval_mode_trigger') THEN
        CREATE TRIGGER update_approval_mode_trigger
        BEFORE INSERT OR UPDATE OF pending_approvers ON approval_instance
        FOR EACH ROW
        WHEN (NEW.pending_approvers IS NOT NULL AND array_length(NEW.pending_approvers, 1) > 1)
        EXECUTE FUNCTION update_approval_mode_to_parallel();

        RAISE NOTICE 'Trigger update_approval_mode_trigger created';
    END IF;
END $$;

-- =====================================================
-- Create function to update approval_mode
-- =====================================================
DO $$
BEGIN
    CREATE OR REPLACE FUNCTION update_approval_mode_to_parallel()
    RETURNS TRIGGER AS $$
    BEGIN
        IF array_length(NEW.pending_approvers, 1) > 1 THEN
            NEW.approval_mode = 'PARALLEL_ALL';
        ELSE
            NEW.approval_mode = 'SEQUENTIAL';
        END IF;
        RETURN NEW;
    END;
    $$ LANGUAGE plpgsql;
END $$;

RAISE NOTICE 'Migration V8__create_approval_action_record_table completed successfully';
