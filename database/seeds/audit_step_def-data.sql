-- audit_step_def clean seed
-- Scope:
-- - Only keep the 4 approved workflow templates and their canonical 14 steps.
-- - Single-table seed only. Upstream dependencies are seeded separately by:
--   - audit_flow_def-data.sql
--   - sys_role-data.sql
-- Approver resolution note:
-- - role 6/7/8 are merged roles, so step_name + org context is part of the actual routing rule.
-- - For example:
--   - "战略发展部负责人审批" => role 7 + org 35
--   - "分管校领导审批" => role 7 + global leader seat
--   - "学院院长审批人审批" => role 7 + current college org
--   - "战略发展部终审人审批" => role 8 + org 35
--   - "职能部门终审人审批" => role 8 + current functional org

BEGIN;

DELETE FROM public.audit_step_def
WHERE flow_id IN (1, 2, 3, 4, 5, 6);

INSERT INTO public.audit_step_def (
    id,
    flow_id,
    step_name,
    step_type,
    role_id,
    is_terminal,
    created_at,
    updated_at,
    step_no
)
VALUES
    (1, 1, '填报人提交', 'SUBMIT', NULL, false, NOW(), NOW(), 1),
    (2, 1, '战略发展部负责人审批', 'APPROVAL', 7, false, NOW(), NOW(), 2),
    (3, 1, '分管校领导审批', 'APPROVAL', 7, true, NOW(), NOW(), 3),

    (4, 2, '填报人提交', 'SUBMIT', NULL, false, NOW(), NOW(), 1),
    (5, 2, '职能部门审批人审批', 'APPROVAL', 6, false, NOW(), NOW(), 2),
    (6, 2, '分管校领导审批', 'APPROVAL', 7, true, NOW(), NOW(), 3),

    (7, 3, '填报人提交', 'SUBMIT', NULL, false, NOW(), NOW(), 1),
    (8, 3, '职能部门审批人审批', 'APPROVAL', 6, false, NOW(), NOW(), 2),
    (9, 3, '分管校领导审批', 'APPROVAL', 7, false, NOW(), NOW(), 3),
    (10, 3, '战略发展部终审人审批', 'APPROVAL', 8, true, NOW(), NOW(), 4),

    (21, 4, '填报人提交', 'SUBMIT', NULL, false, NOW(), NOW(), 1),
    (22, 4, '二级学院审批人审批', 'APPROVAL', 6, false, NOW(), NOW(), 2),
    (23, 4, '学院院长审批人审批', 'APPROVAL', 7, false, NOW(), NOW(), 3),
    (24, 4, '职能部门终审人审批', 'APPROVAL', 8, true, NOW(), NOW(), 4),

    (31, 5, '填报人提交', 'SUBMIT', NULL, false, NOW(), NOW(), 1),
    (32, 5, '职能部门审批人审批', 'APPROVAL', 6, false, NOW(), NOW(), 2),
    (33, 5, '战略发展部负责人审批', 'APPROVAL', 7, true, NOW(), NOW(), 3),

    (41, 6, '填报人提交', 'SUBMIT', NULL, false, NOW(), NOW(), 1),
    (42, 6, '二级学院审批人审批', 'APPROVAL', 6, false, NOW(), NOW(), 2),
    (43, 6, '学院院长审批人审批', 'APPROVAL', 7, false, NOW(), NOW(), 3),
    (44, 6, '职能部门终审人审批', 'APPROVAL', 8, true, NOW(), NOW(), 4)
ON CONFLICT (id) DO UPDATE
SET
    flow_id = EXCLUDED.flow_id,
    step_name = EXCLUDED.step_name,
    step_type = EXCLUDED.step_type,
    role_id = EXCLUDED.role_id,
    is_terminal = EXCLUDED.is_terminal,
    updated_at = EXCLUDED.updated_at,
    step_no = EXCLUDED.step_no;

COMMIT;
