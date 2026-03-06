-- =============================================================================
-- V17: 添加多级审批跟踪字段到 audit_instance 表
-- =============================================================================
-- 目的：支持完整的多级审批流程跟踪
-- 创建时间：2026-03-06
-- =============================================================================

-- 添加多级审批跟踪字段
ALTER TABLE audit_instance ADD COLUMN IF NOT EXISTS current_step_order INTEGER DEFAULT 1;
COMMENT ON COLUMN audit_instance.current_step_order IS '当前审批步骤顺序（1=一级，2=二级，3=终审）';

ALTER TABLE audit_instance ADD COLUMN IF NOT EXISTS submitter_dept_id BIGINT;
COMMENT ON COLUMN audit_instance.submitter_dept_id IS '提交人所在部门ID（从indicator.target_org_id获取）';

ALTER TABLE audit_instance ADD COLUMN IF NOT EXISTS direct_supervisor_id BIGINT;
COMMENT ON COLUMN audit_instance.direct_supervisor_id IS '一级审批人ID（部门负责人）';

ALTER TABLE audit_instance ADD COLUMN IF NOT EXISTS level2_supervisor_id BIGINT;
COMMENT ON COLUMN audit_instance.level2_supervisor_id IS '二级审批人ID（分管校领导）';

ALTER TABLE audit_instance ADD COLUMN IF NOT EXISTS superior_dept_id BIGINT;
COMMENT ON COLUMN audit_instance.superior_dept_id IS '上级部门ID（从indicator.owner_org_id获取）';

-- 添加索引以提升查询性能
CREATE INDEX IF NOT EXISTS idx_audit_instance_current_step ON audit_instance(current_step_order);
CREATE INDEX IF NOT EXISTS idx_audit_instance_submitter_dept ON audit_instance(submitter_dept_id);
CREATE INDEX IF NOT EXISTS idx_audit_instance_direct_supervisor ON audit_instance(direct_supervisor_id);
CREATE INDEX IF NOT EXISTS idx_audit_instance_level2_supervisor ON audit_instance(level2_supervisor_id);
CREATE INDEX IF NOT EXISTS idx_audit_instance_superior_dept ON audit_instance(superior_dept_id);

-- 添加外键约束（可选，根据需要）
-- ALTER TABLE audit_instance ADD CONSTRAINT fk_audit_instance_submitter_dept 
--     FOREIGN KEY (submitter_dept_id) REFERENCES sys_org(id);
-- ALTER TABLE audit_instance ADD CONSTRAINT fk_audit_instance_superior_dept 
--     FOREIGN KEY (superior_dept_id) REFERENCES sys_org(id);
-- ALTER TABLE audit_instance ADD CONSTRAINT fk_audit_instance_direct_supervisor 
--     FOREIGN KEY (direct_supervisor_id) REFERENCES sys_user(id);
-- ALTER TABLE audit_instance ADD CONSTRAINT fk_audit_instance_level2_supervisor 
--     FOREIGN KEY (level2_supervisor_id) REFERENCES sys_user(id);
