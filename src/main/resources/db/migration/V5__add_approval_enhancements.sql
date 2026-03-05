-- V5__add_approval_enhancements.sql
-- Multi-level Approval Workflow Enhancement
-- Created: 2026-02-25
-- Description: Add user-supervisor relationships, org hierarchy, and enhance audit tables for multi-level approval flow

-- =====================================================
-- 1. User Supervisor Relationship Table
-- =====================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'sys_user_supervisor') THEN
        CREATE TABLE sys_user_supervisor (
            id BIGSERIAL PRIMARY KEY,
            user_id BIGINT NOT NULL REFERENCES sys_user(id),
            supervisor_id BIGINT NOT NULL REFERENCES sys_user(id),
            level INT NOT NULL DEFAULT 1 CHECK (level IN (1, 2)),
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE(user_id, level)
        );
        
        COMMENT ON TABLE sys_user_supervisor IS '用户主管关系�?- 存储用户的主管关系，支持多级主管';
        COMMENT ON COLUMN sys_user_supervisor.user_id IS '下属用户ID';
        COMMENT ON COLUMN sys_user_supervisor.supervisor_id IS '主管用户ID';
        COMMENT ON COLUMN sys_user_supervisor.level IS '主管级别: 1=直接主管, 2=二级主管';
        
        CREATE INDEX idx_user_supervisor_user ON sys_user_supervisor(user_id);
        CREATE INDEX idx_user_supervisor_level ON sys_user_supervisor(user_id, level);
        CREATE INDEX idx_user_supervisor_supervisor ON sys_user_supervisor(supervisor_id);
        
        RAISE NOTICE 'Table sys_user_supervisor created successfully';
    ELSE
        RAISE NOTICE 'Table sys_user_supervisor already exists';
    END IF;
END $$;

-- =====================================================
-- 2. Organization Hierarchy Table
-- =====================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'sys_org_hierarchy') THEN
        CREATE TABLE sys_org_hierarchy (
            id BIGSERIAL PRIMARY KEY,
            org_id BIGINT NOT NULL REFERENCES sys_org(id),
            parent_org_id BIGINT REFERENCES sys_org(id),
            level INT NOT NULL DEFAULT 1,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE(org_id)
        );
        
        COMMENT ON TABLE sys_org_hierarchy IS '部门层级关系�?- 存储部门的层级关�?;
        COMMENT ON COLUMN sys_org_hierarchy.org_id IS '部门ID';
        COMMENT ON COLUMN sys_org_hierarchy.parent_org_id IS '上级部门ID';
        COMMENT ON COLUMN sys_org_hierarchy.level IS '部门级别: 1=一级部�? 2=二级部门, 以此类推';
        
        CREATE INDEX idx_org_hierarchy_org ON sys_org_hierarchy(org_id);
        CREATE INDEX idx_org_hierarchy_parent ON sys_org_hierarchy(parent_org_id);
        
        RAISE NOTICE 'Table sys_org_hierarchy created successfully';
    ELSE
        RAISE NOTICE 'Table sys_org_hierarchy already exists';
    END IF;
END $$;

-- =====================================================
-- 3. Enhance audit_step_def Table
-- =====================================================
DO $$
BEGIN
    -- Add approver_type column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_step_def' AND column_name = 'approver_type') THEN
        ALTER TABLE audit_step_def ADD COLUMN approver_type VARCHAR(20) DEFAULT 'ROLE';
        COMMENT ON COLUMN audit_step_def.approver_type IS '审批人类�? ROLE=角色, USER=指定用户, SUPERVISOR=主管, DYNAMIC_USER=动态解�?;
    END IF;
    
    -- Add approver_ids column (for specific users or dynamic resolution)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_step_def' AND column_name = 'approver_ids') THEN
        ALTER TABLE audit_step_def ADD COLUMN approver_ids TEXT[];
        COMMENT ON COLUMN audit_step_def.approver_ids IS '审批人ID列表，用于USER或DYNAMIC_USER类型';
    END IF;
    
    -- Add approval_mode column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_step_def' AND column_name = 'approval_mode') THEN
        ALTER TABLE audit_step_def ADD COLUMN approval_mode VARCHAR(20) DEFAULT 'SEQUENTIAL';
        COMMENT ON COLUMN audit_step_def.approval_mode IS '审批模式: SEQUENTIAL=串行, PARALLEL=并行(会签)';
    END IF;
    
    -- Add supervisor_level column (for SUPERVISOR type)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_step_def' AND column_name = 'supervisor_level') THEN
        ALTER TABLE audit_step_def ADD COLUMN supervisor_level INT;
        COMMENT ON COLUMN audit_step_def.supervisor_level IS '主管级别: 1=直接主管, 2=二级主管';
    END IF;
    
    RAISE NOTICE 'Table audit_step_def enhanced successfully';
END $$;

-- =====================================================
-- 4. Enhance audit_instance Table
-- =====================================================
DO $$
BEGIN
    -- Add current_step_order column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_instance' AND column_name = 'current_step_order') THEN
        ALTER TABLE audit_instance ADD COLUMN current_step_order INT DEFAULT 1;
        COMMENT ON COLUMN audit_instance.current_step_order IS '当前审批步骤序号';
    END IF;
    
    -- Add submitter_dept_id column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_instance' AND column_name = 'submitter_dept_id') THEN
        ALTER TABLE audit_instance ADD COLUMN submitter_dept_id BIGINT;
        COMMENT ON COLUMN audit_instance.submitter_dept_id IS '提交人部门ID';
    END IF;
    
    -- Add direct_supervisor_id column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_instance' AND column_name = 'direct_supervisor_id') THEN
        ALTER TABLE audit_instance ADD COLUMN direct_supervisor_id BIGINT;
        COMMENT ON COLUMN audit_instance.direct_supervisor_id IS '直接主管ID';
    END IF;
    
    -- Add level2_supervisor_id column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_instance' AND column_name = 'level2_supervisor_id') THEN
        ALTER TABLE audit_instance ADD COLUMN level2_supervisor_id BIGINT;
        COMMENT ON COLUMN audit_instance.level2_supervisor_id IS '二级主管ID';
    END IF;
    
    -- Add superior_dept_id column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_instance' AND column_name = 'superior_dept_id') THEN
        ALTER TABLE audit_instance ADD COLUMN superior_dept_id BIGINT;
        COMMENT ON COLUMN audit_instance.superior_dept_id IS '上级部门ID';
    END IF;
    
    -- Add pending_approvers column (for parallel approval tracking)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_instance' AND column_name = 'pending_approvers') THEN
        ALTER TABLE audit_instance ADD COLUMN pending_approvers BIGINT[];
        COMMENT ON COLUMN audit_instance.pending_approvers IS '待审批人ID列表(会签时使�?';
    END IF;
    
    -- Add approved_approvers column (for parallel approval tracking)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_instance' AND column_name = 'approved_approvers') THEN
        ALTER TABLE audit_instance ADD COLUMN approved_approvers BIGINT[];
        COMMENT ON COLUMN audit_instance.approved_approvers IS '已通过审批人ID列表(会签时使�?';
    END IF;
    
    -- Add rejected_approvers column (for parallel approval tracking)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_instance' AND column_name = 'rejected_approvers') THEN
        ALTER TABLE audit_instance ADD COLUMN rejected_approvers BIGINT[];
        COMMENT ON COLUMN audit_instance.rejected_approvers IS '已拒绝审批人ID列表(会签时使�?';
    END IF;
    
    RAISE NOTICE 'Table audit_instance enhanced successfully';
END $$;

-- =====================================================
-- 5. Add Foreign Keys for audit_instance (if not exists)
-- =====================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE constraint_name = 'fk_audit_instance_submitter_dept') THEN
        ALTER TABLE audit_instance 
        ADD CONSTRAINT fk_audit_instance_submitter_dept 
        FOREIGN KEY (submitter_dept_id) REFERENCES sys_org(id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE constraint_name = 'fk_audit_instance_direct_supervisor') THEN
        ALTER TABLE audit_instance 
        ADD CONSTRAINT fk_audit_instance_direct_supervisor 
        FOREIGN KEY (direct_supervisor_id) REFERENCES sys_user(id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE constraint_name = 'fk_audit_instance_level2_supervisor') THEN
        ALTER TABLE audit_instance 
        ADD CONSTRAINT fk_audit_instance_level2_supervisor 
        FOREIGN KEY (level2_supervisor_id) REFERENCES sys_user(id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE constraint_name = 'fk_audit_instance_superior_dept') THEN
        ALTER TABLE audit_instance 
        ADD CONSTRAINT fk_audit_instance_superior_dept 
        FOREIGN KEY (superior_dept_id) REFERENCES sys_org(id);
    END IF;
    
    RAISE NOTICE 'Foreign keys added successfully';
END $$;

-- =====================================================
-- 6. Create Indexes for Performance
-- =====================================================
DO $$
BEGIN
    CREATE INDEX IF NOT EXISTS idx_audit_instance_step_order ON audit_instance(current_step_order);
    CREATE INDEX IF NOT EXISTS idx_audit_instance_submitter ON audit_instance(submitter_dept_id);
    CREATE INDEX IF NOT EXISTS idx_audit_instance_status_order ON audit_instance(status, current_step_order);
    
    RAISE NOTICE 'Migration V5__add_approval_enhancements completed successfully';
END $$;
