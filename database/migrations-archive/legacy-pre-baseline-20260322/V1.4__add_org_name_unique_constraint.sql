-- V1.4: 为组织名称添加唯一约束
-- 确保组织名称不可重复

-- 添加唯一约束
ALTER TABLE org ADD CONSTRAINT uk_org_name UNIQUE (org_name);

-- 添加注释
COMMENT ON CONSTRAINT uk_org_name ON org IS '组织名称唯一约束';
