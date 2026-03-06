-- =============================================================================
-- V18: 添加指标审批流程定义
-- =============================================================================
-- 目的：为指标下发创建审批流程
-- 创建时间：2026-03-06
-- =============================================================================

-- 插入指标默认审批流程（职能部门）
INSERT INTO audit_flow_def (flow_code, flow_name, biz_type, entity_type, is_enabled, remark)
VALUES (
    'INDICATOR_DEFAULT_APPROVAL',
    '指标审批流程（职能部门）',
    'PLAN',  -- 保持与现有数据一致
    'INDICATOR',
    true,
    '职能部门接收指标后的审批流程：部门负责人 → 分管校领导'
)
ON CONFLICT (flow_code) DO UPDATE SET
    flow_name = EXCLUDED.flow_name,
    entity_type = EXCLUDED.entity_type,
    remark = EXCLUDED.remark,
    updated_at = CURRENT_TIMESTAMP;

-- 插入二级学院审批流程
INSERT INTO audit_flow_def (flow_code, flow_name, biz_type, entity_type, is_enabled, remark)
VALUES (
    'INDICATOR_COLLEGE_APPROVAL',
    '指标审批流程（二级学院）',
    'PLAN',  -- 保持与现有数据一致
    'INDICATOR',
    true,
    '二级学院接收指标后的审批流程：学院院长 → 分管校领导'
)
ON CONFLICT (flow_code) DO UPDATE SET
    flow_name = EXCLUDED.flow_name,
    entity_type = EXCLUDED.entity_type,
    remark = EXCLUDED.remark,
    updated_at = CURRENT_TIMESTAMP;

-- 注意：audit_step_def 表的步骤定义需要根据实际业务逻辑配置
-- 这里只创建流程定义，具体步骤由应用层代码管理
