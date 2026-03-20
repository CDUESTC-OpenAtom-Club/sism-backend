-- sys_role clean seed
-- Source rule:
-- - Align to the current plan-centric workflow model.
-- - Keep the canonical role set used by audit_step_def / sys_user_role / sys_role_permission.
-- - Clarify each role's business responsibility so later seed files can be audited consistently.
-- Resolution rule:
-- - role_id alone is NOT enough to locate a concrete approver.
-- - Current seeds resolve people by the combination of:
--   1) role_id
--   2) current workflow step_name / flow_id
--   3) approver_org_id or requester_org_id context
-- Relation rule:
-- - sys_role is defined by the workflow routing model in audit_step_def.
-- - Only the 4 core workflow roles used by role_id in audit_step_def are kept active here:
--   reporter / approver / leader / final approver.
-- - Business labels such as "战略发展部负责人" / "分管校领导" / "职能部门终审人"
--   are expressed by step_name + org context, not by separate role rows.
-- - Special case: "分管校领导" is treated as a global leader seat in the current clean seed,
--   so it may not be derived from the target organization itself.

BEGIN;

INSERT INTO public.sys_role (
    id,
    role_code,
    role_name,
    data_access_mode,
    is_enabled,
    remark,
    created_at,
    updated_at
)
VALUES
    (
        5,
        'ROLE_REPORTER',
        '填报人',
        'OWN_ORG',
        true,
        '计划/任务/指标内容的实际填报人。负责草稿编辑、填报提交、月报填报，不负责审批节点决策。',
        NOW(),
        NOW()
    ),
    (
        6,
        'ROLE_APPROVER',
        '审批人',
        'OWN_ORG',
        true,
        '通用审批角色。用于职能部门审批人与二级学院审批人节点，具体审批对象由当前 org_id 决定。',
        NOW(),
        NOW()
    ),
    (
        7,
        'ROLE_LEADER',
        '领导',
        'ALL',
        true,
        '通用领导角色。用于战略发展部负责人、学院院长、分管校领导等领导审批节点。落人时必须结合 step_name 与组织上下文；其中“分管校领导”在当前种子中按全局领导席位处理。',
        NOW(),
        NOW()
    ),
    (
        8,
        'ROLE_FINAL_APPROVER',
        '终审人',
        'ALL',
        true,
        '通用终审角色。用于战略发展部终审与职能部门终审节点。落人时必须结合 step_name 与 approver_org_id，不能只靠角色本身查人。',
        NOW(),
        NOW()
    )
ON CONFLICT (id) DO UPDATE
SET
    role_code = EXCLUDED.role_code,
    role_name = EXCLUDED.role_name,
    data_access_mode = EXCLUDED.data_access_mode,
    is_enabled = EXCLUDED.is_enabled,
    remark = EXCLUDED.remark,
    updated_at = EXCLUDED.updated_at;

DELETE FROM public.sys_role
WHERE id IN (9, 10, 11, 12, 13);

COMMIT;
