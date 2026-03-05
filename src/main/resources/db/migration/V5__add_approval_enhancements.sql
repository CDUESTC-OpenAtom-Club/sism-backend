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
        
        COMMENT ON TABLE sys_user_supervisor IS '鐢ㄦ埛涓荤鍏崇郴琛?- 瀛樺偍鐢ㄦ埛鐨勪富绠″叧绯伙紝鏀寔澶氱骇涓荤';
        COMMENT ON COLUMN sys_user_supervisor.user_id IS '涓嬪睘鐢ㄦ埛ID';
        COMMENT ON COLUMN sys_user_supervisor.supervisor_id IS '涓荤鐢ㄦ埛ID';
        COMMENT ON COLUMN sys_user_supervisor.level IS '涓荤绾у埆: 1=鐩存帴涓荤, 2=浜岀骇涓荤';
        
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
        
        COMMENT ON TABLE sys_org_hierarchy IS '閮ㄩ棬灞傜骇鍏崇郴琛?- 瀛樺偍閮ㄩ棬鐨勫眰绾у叧绯?;
        COMMENT ON COLUMN sys_org_hierarchy.org_id IS '閮ㄩ棬ID';
        COMMENT ON COLUMN sys_org_hierarchy.parent_org_id IS '涓婄骇閮ㄩ棬ID';
        COMMENT ON COLUMN sys_org_hierarchy.level IS '閮ㄩ棬绾у埆: 1=涓€绾ч儴闂? 2=浜岀骇閮ㄩ棬, 浠ユ绫绘帹';
        
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
        COMMENT ON COLUMN audit_step_def.approver_type IS '瀹℃壒浜虹被鍨? ROLE=瑙掕壊, USER=鎸囧畾鐢ㄦ埛, SUPERVISOR=涓荤, DYNAMIC_USER=鍔ㄦ€佽В鏋?;
    END IF;
    
    -- Add approver_ids column (for specific users or dynamic resolution)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_step_def' AND column_name = 'approver_ids') THEN
        ALTER TABLE audit_step_def ADD COLUMN approver_ids TEXT[];
        COMMENT ON COLUMN audit_step_def.approver_ids IS '瀹℃壒浜篒D鍒楄〃锛岀敤浜嶶SER鎴朌YNAMIC_USER绫诲瀷';
    END IF;
    
    -- Add approval_mode column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_step_def' AND column_name = 'approval_mode') THEN
        ALTER TABLE audit_step_def ADD COLUMN approval_mode VARCHAR(20) DEFAULT 'SEQUENTIAL';
        COMMENT ON COLUMN audit_step_def.approval_mode IS '瀹℃壒妯″紡: SEQUENTIAL=涓茶, PARALLEL=骞惰(浼氱)';
    END IF;
    
    -- Add supervisor_level column (for SUPERVISOR type)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_step_def' AND column_name = 'supervisor_level') THEN
        ALTER TABLE audit_step_def ADD COLUMN supervisor_level INT;
        COMMENT ON COLUMN audit_step_def.supervisor_level IS '涓荤绾у埆: 1=鐩存帴涓荤, 2=浜岀骇涓荤';
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
        COMMENT ON COLUMN audit_instance.current_step_order IS '褰撳墠瀹℃壒姝ラ搴忓彿';
    END IF;
    
    -- Add submitter_dept_id column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_instance' AND column_name = 'submitter_dept_id') THEN
        ALTER TABLE audit_instance ADD COLUMN submitter_dept_id BIGINT;
        COMMENT ON COLUMN audit_instance.submitter_dept_id IS '鎻愪氦浜洪儴闂↖D';
    END IF;
    
    -- Add direct_supervisor_id column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_instance' AND column_name = 'direct_supervisor_id') THEN
        ALTER TABLE audit_instance ADD COLUMN direct_supervisor_id BIGINT;
        COMMENT ON COLUMN audit_instance.direct_supervisor_id IS '鐩存帴涓荤ID';
    END IF;
    
    -- Add level2_supervisor_id column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_instance' AND column_name = 'level2_supervisor_id') THEN
        ALTER TABLE audit_instance ADD COLUMN level2_supervisor_id BIGINT;
        COMMENT ON COLUMN audit_instance.level2_supervisor_id IS '浜岀骇涓荤ID';
    END IF;
    
    -- Add superior_dept_id column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_instance' AND column_name = 'superior_dept_id') THEN
        ALTER TABLE audit_instance ADD COLUMN superior_dept_id BIGINT;
        COMMENT ON COLUMN audit_instance.superior_dept_id IS '涓婄骇閮ㄩ棬ID';
    END IF;
    
    -- Add pending_approvers column (for parallel approval tracking)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_instance' AND column_name = 'pending_approvers') THEN
        ALTER TABLE audit_instance ADD COLUMN pending_approvers BIGINT[];
        COMMENT ON COLUMN audit_instance.pending_approvers IS '寰呭鎵逛汉ID鍒楄〃(浼氱鏃朵娇鐢?';
    END IF;
    
    -- Add approved_approvers column (for parallel approval tracking)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_instance' AND column_name = 'approved_approvers') THEN
        ALTER TABLE audit_instance ADD COLUMN approved_approvers BIGINT[];
        COMMENT ON COLUMN audit_instance.approved_approvers IS '宸查€氳繃瀹℃壒浜篒D鍒楄〃(浼氱鏃朵娇鐢?';
    END IF;
    
    -- Add rejected_approvers column (for parallel approval tracking)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_instance' AND column_name = 'rejected_approvers') THEN
        ALTER TABLE audit_instance ADD COLUMN rejected_approvers BIGINT[];
        COMMENT ON COLUMN audit_instance.rejected_approvers IS '宸叉嫆缁濆鎵逛汉ID鍒楄〃(浼氱鏃朵娇鐢?';
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
