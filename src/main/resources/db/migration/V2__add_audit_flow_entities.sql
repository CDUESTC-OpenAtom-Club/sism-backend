-- ============================================
-- Add Audit Flow Entities
-- Version: 2.0
-- Date: 2026-02-13
-- Description: Add audit workflow management tables
-- ============================================

-- Create audit_flow_def table
CREATE TABLE IF NOT EXISTS audit_flow_def (
    id BIGSERIAL PRIMARY KEY,
    flow_name VARCHAR(100) NOT NULL,
    flow_code VARCHAR(50) NOT NULL UNIQUE,
    entity_type VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_flow_code UNIQUE (flow_code)
);

-- Create index on entity_type for faster lookups
CREATE INDEX idx_audit_flow_entity_type ON audit_flow_def(entity_type);

-- Add comment
COMMENT ON TABLE audit_flow_def IS 'Audit workflow definitions';
COMMENT ON COLUMN audit_flow_def.flow_code IS 'Unique flow identifier code';
COMMENT ON COLUMN audit_flow_def.entity_type IS 'Entity type this flow applies to (INDICATOR, TASK, etc.)';

-- Create audit_step_def table
CREATE TABLE IF NOT EXISTS audit_step_def (
    id BIGSERIAL PRIMARY KEY,
    flow_id BIGINT NOT NULL,
    step_order INTEGER NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    approver_role VARCHAR(50) NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_step_flow FOREIGN KEY (flow_id) REFERENCES audit_flow_def(id) ON DELETE CASCADE
);

-- Create indexes for audit_step_def
CREATE INDEX idx_flow_id ON audit_step_def(flow_id);
CREATE INDEX idx_flow_step_order ON audit_step_def(flow_id, step_order);

-- Add comments
COMMENT ON TABLE audit_step_def IS 'Audit workflow step definitions';
COMMENT ON COLUMN audit_step_def.step_order IS 'Order of step in workflow (1, 2, 3, ...)';
COMMENT ON COLUMN audit_step_def.approver_role IS 'Role required to approve this step';
COMMENT ON COLUMN audit_step_def.is_required IS 'Whether this step is mandatory';

-- Create audit_instance table
CREATE TABLE IF NOT EXISTS audit_instance (
    id BIGSERIAL PRIMARY KEY,
    flow_id BIGINT NOT NULL,
    entity_id BIGINT NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    current_step_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    initiated_by BIGINT NOT NULL,
    initiated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_instance_flow FOREIGN KEY (flow_id) REFERENCES audit_flow_def(id),
    CONSTRAINT fk_audit_instance_step FOREIGN KEY (current_step_id) REFERENCES audit_step_def(id),
    CONSTRAINT fk_audit_instance_user FOREIGN KEY (initiated_by) REFERENCES sys_user(id)
);

-- Create indexes for audit_instance
CREATE INDEX idx_audit_instance_flow ON audit_instance(flow_id);
CREATE INDEX idx_audit_instance_entity ON audit_instance(entity_type, entity_id);
CREATE INDEX idx_audit_instance_status ON audit_instance(status);
CREATE INDEX idx_audit_instance_initiator ON audit_instance(initiated_by);

-- Add comments
COMMENT ON TABLE audit_instance IS 'Running instances of audit workflows';
COMMENT ON COLUMN audit_instance.entity_id IS 'ID of the entity being audited';
COMMENT ON COLUMN audit_instance.entity_type IS 'Type of entity being audited';
COMMENT ON COLUMN audit_instance.current_step_id IS 'Current step in the workflow';
COMMENT ON COLUMN audit_instance.status IS 'Status: PENDING, IN_PROGRESS, APPROVED, REJECTED, CANCELLED';
