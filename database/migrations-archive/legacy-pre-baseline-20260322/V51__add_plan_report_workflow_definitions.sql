-- V51__add_plan_report_workflow_definitions.sql
-- Description: Add PlanReport workflow definitions to complete the monthly report approval chain.
-- This is the #1 priority from the core business module planning document:
--   "优先把 plan_report 补进正式审批主链路"
-- Date: 2026-03-20

-- 1. Add PlanReport workflow flow definitions
INSERT INTO public.audit_flow_def (
    id, flow_code, flow_name, is_enabled, created_at, updated_at, description, version, entity_type
)
VALUES
    (5, 'PLAN_REPORT_FUNC', '月报审批流程（职能部门）', true, NOW(), NOW(), '职能部门月度填报审批流程：填报人提交 → 职能部门审批 → 战略发展部审批', 1, 'PlanReport'),
    (6, 'PLAN_REPORT_COLLEGE', '月报审批流程（二级学院）', true, NOW(), NOW(), '二级学院月度填报审批流程：填报人提交 → 学院审批 → 院长审批 → 职能部门终审', 1, 'PlanReport')
ON CONFLICT (id) DO UPDATE
SET
    flow_code = EXCLUDED.flow_code,
    flow_name = EXCLUDED.flow_name,
    is_enabled = EXCLUDED.is_enabled,
    updated_at = EXCLUDED.updated_at,
    description = EXCLUDED.description,
    version = EXCLUDED.version,
    entity_type = EXCLUDED.entity_type;

-- 2. Add PlanReport workflow step definitions
INSERT INTO public.audit_step_def (
    id, flow_id, step_name, step_type, role_id, is_terminal, created_at, updated_at, step_no
)
VALUES
    -- PLAN_REPORT_FUNC (flow_id=5): 填报人提交 → 职能部门审批 → 战略发展部审批
    (31, 5, '填报人提交',           'SUBMIT',   NULL, false, NOW(), NOW(), 1),
    (32, 5, '职能部门审批人审批',     'APPROVAL', 6,    false, NOW(), NOW(), 2),
    (33, 5, '战略发展部负责人审批',   'APPROVAL', 7,    true,  NOW(), NOW(), 3),

    -- PLAN_REPORT_COLLEGE (flow_id=6): 填报人提交 → 学院审批 → 院长审批 → 职能部门终审
    (41, 6, '填报人提交',           'SUBMIT',   NULL, false, NOW(), NOW(), 1),
    (42, 6, '二级学院审批人审批',     'APPROVAL', 6,    false, NOW(), NOW(), 2),
    (43, 6, '学院院长审批人审批',     'APPROVAL', 7,    false, NOW(), NOW(), 3),
    (44, 6, '职能部门终审人审批',     'APPROVAL', 8,    true,  NOW(), NOW(), 4)
ON CONFLICT (id) DO UPDATE
SET
    flow_id = EXCLUDED.flow_id,
    step_name = EXCLUDED.step_name,
    step_type = EXCLUDED.step_type,
    role_id = EXCLUDED.role_id,
    is_terminal = EXCLUDED.is_terminal,
    updated_at = EXCLUDED.updated_at,
    step_no = EXCLUDED.step_no;
