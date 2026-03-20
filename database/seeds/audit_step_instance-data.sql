-- audit_step_instance clean seed

BEGIN;

DELETE FROM public.audit_step_instance
WHERE id IN (5011, 5012, 5013, 5014);

INSERT INTO public.audit_step_instance (
    id,
    instance_id,
    step_name,
    approved_at,
    approver_id,
    comment,
    status,
    step_index,
    step_def_id,
    approver_org_id,
    step_no,
    step_code,
    created_at
)
VALUES
    (5001, 4001, '填报人提交', NOW(), 188, '系统自动完成提交流程节点', 'APPROVED', 1, 1, 35, 1, NULL, NOW()),
    (5002, 4001, '战略发展部负责人审批', NOW(), 188, '战略发展部负责人审批通过', 'APPROVED', 2, 2, 35, 2, NULL, NOW()),
    (5003, 4001, '分管校领导审批', NOW(), 124, '分管校领导审批通过', 'APPROVED', 3, 3, 35, 3, NULL, NOW()),

    (5004, 4002, '填报人提交', NOW(), 188, '系统自动完成提交流程节点', 'APPROVED', 1, 1, 35, 1, NULL, NOW()),
    (5005, 4002, '战略发展部负责人审批', NULL, 188, NULL, 'PENDING', 2, 2, 35, 2, NULL, NOW()),
    (5006, 4002, '分管校领导审批', NULL, 124, NULL, 'WAITING', 3, 3, 35, 3, NULL, NOW()),

    (5007, 4003, '填报人提交', NOW(), 223, '系统自动完成提交流程节点', 'APPROVED', 1, 7, 44, 1, NULL, NOW()),
    (5008, 4003, '职能部门审批人审批', NULL, 224, NULL, 'PENDING', 2, 8, 44, 2, NULL, NOW()),
    (5009, 4003, '分管校领导审批', NULL, 124, NULL, 'WAITING', 3, 9, 35, 3, NULL, NOW()),
    (5010, 4003, '战略发展部终审人审批', NULL, 189, NULL, 'WAITING', 4, 10, 35, 4, NULL, NOW())
ON CONFLICT (id) DO UPDATE
SET
    instance_id = EXCLUDED.instance_id,
    step_name = EXCLUDED.step_name,
    approved_at = EXCLUDED.approved_at,
    approver_id = EXCLUDED.approver_id,
    comment = EXCLUDED.comment,
    status = EXCLUDED.status,
    step_index = EXCLUDED.step_index,
    step_def_id = EXCLUDED.step_def_id,
    approver_org_id = EXCLUDED.approver_org_id,
    step_no = EXCLUDED.step_no,
    step_code = EXCLUDED.step_code,
    created_at = EXCLUDED.created_at;

COMMIT;
