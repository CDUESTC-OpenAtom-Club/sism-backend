-- Repair PLAN_DISPATCH_STRATEGY workflow definition data without changing table structure.
-- This aligns step ordering, step type, and role assignment for the current approval chain.

WITH target_flow AS (
    SELECT id
    FROM audit_flow_def
    WHERE flow_code = 'PLAN_DISPATCH_STRATEGY'
    LIMIT 1
)
UPDATE audit_step_def step
SET
    step_no = CASE
        WHEN step.step_name ILIKE '%提交%' THEN 1
        WHEN step.step_name ILIKE '%战略%' THEN 2
        WHEN step.step_name ILIKE '%校领导%' OR step.step_name ILIKE '%分管%' THEN 3
        ELSE COALESCE(step.step_no, 999)
    END,
    step_type = CASE
        WHEN step.step_name ILIKE '%提交%' THEN 'SUBMIT'
        ELSE COALESCE(NULLIF(step.step_type, ''), 'APPROVAL')
    END,
    role_id = CASE
        WHEN step.step_name ILIKE '%提交%' THEN NULL
        WHEN step.step_name ILIKE '%战略%' THEN COALESCE(step.role_id, 7)
        WHEN step.step_name ILIKE '%校领导%' OR step.step_name ILIKE '%分管%' THEN COALESCE(step.role_id, 7)
        ELSE step.role_id
    END,
    updated_at = NOW()
FROM target_flow
WHERE step.flow_id = target_flow.id;

INSERT INTO sys_user_role (user_id, role_id)
SELECT 124, 7
WHERE EXISTS (SELECT 1 FROM sys_user WHERE id = 124)
  AND EXISTS (SELECT 1 FROM sys_role WHERE id = 7)
  AND NOT EXISTS (
      SELECT 1
      FROM sys_user_role
      WHERE user_id = 124
        AND role_id = 7
  );
