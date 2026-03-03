-- V7__create_approval_tables.sql
-- Multi-level Approval Workflow Tables
-- Creates tables for three-level approval workflow:
-- 1. Direct supervisor approval
-- 2. Level-2 supervisor approval
-- 3. Superior department joint approval

-- Approval Instances Table
CREATE TABLE IF NOT EXISTS approval_instances (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    current_step_order INT DEFAULT 1,
    submitter_dept_id BIGINT,
    initiated_by BIGINT NOT NULL,
    initiated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Approval Steps Table
CREATE TABLE IF NOT EXISTS approval_steps (
    id BIGSERIAL PRIMARY KEY,
    instance_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    step_name VARCHAR(50) NOT NULL,
    step_description VARCHAR(200),
    approver_org_id BIGINT,
    approver_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    comment TEXT,
    acted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_approval_steps_instance
        FOREIGN KEY (instance_id)
        REFERENCES approval_instances(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_approval_steps_approver
        FOREIGN KEY (approver_id)
        REFERENCES sys_user(id)
        ON DELETE SET NULL
);

-- Create indexes for approval_instances
CREATE INDEX IF NOT EXISTS idx_approval_instances_entity ON approval_instances(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_approval_instances_status ON approval_instances(status);
CREATE INDEX IF NOT EXISTS idx_approval_instances_initiated_by ON approval_instances(initiated_by);
CREATE INDEX IF NOT EXISTS idx_approval_instances_submitter_dept ON approval_instances(submitter_dept_id);

-- Create indexes for approval_steps
CREATE INDEX IF NOT EXISTS idx_approval_steps_instance ON approval_steps(instance_id);
CREATE INDEX IF NOT EXISTS idx_approval_steps_step_order ON approval_steps(instance_id, step_order);
CREATE INDEX IF NOT EXISTS idx_approval_steps_approver_org ON approval_steps(approver_org_id);
CREATE INDEX IF NOT EXISTS idx_approval_steps_status ON approval_steps(status);

-- PostgreSQL Comments for approval_instances
COMMENT ON TABLE approval_instances IS '审批实例表';
COMMENT ON COLUMN approval_instances.entity_type IS 'Entity type (INDICATOR_DISTRIBUTION, PROGRESS_REPORT)';
COMMENT ON COLUMN approval_instances.entity_id IS 'ID of the entity being approved';
COMMENT ON COLUMN approval_instances.status IS 'PENDING, IN_PROGRESS, APPROVED, REJECTED';
COMMENT ON COLUMN approval_instances.current_step_order IS 'Current step (1, 2, 3)';
COMMENT ON COLUMN approval_instances.submitter_dept_id IS 'Submitter department ID';
COMMENT ON COLUMN approval_instances.initiated_by IS 'User who initiated the approval';
COMMENT ON COLUMN approval_instances.initiated_at IS 'When the approval was initiated';
COMMENT ON COLUMN approval_instances.completed_at IS 'When the approval was completed';

-- PostgreSQL Comments for approval_steps
COMMENT ON TABLE approval_steps IS '审批步骤表';
COMMENT ON COLUMN approval_steps.instance_id IS 'Parent approval instance';
COMMENT ON COLUMN approval_steps.step_order IS 'Step order (1, 2, 3)';
COMMENT ON COLUMN approval_steps.step_name IS 'Step name (直接主管审批, 二级主管审批, 上级部门审批)';
COMMENT ON COLUMN approval_steps.step_description IS 'Step description';
COMMENT ON COLUMN approval_steps.approver_org_id IS 'Organization that should approve this step';
COMMENT ON COLUMN approval_steps.approver_id IS 'User who approved/rejected this step';
COMMENT ON COLUMN approval_steps.status IS 'PENDING, APPROVED, REJECTED';
COMMENT ON COLUMN approval_steps.comment IS 'Approval/rejection comment';
COMMENT ON COLUMN approval_steps.acted_at IS 'When the action was taken';
