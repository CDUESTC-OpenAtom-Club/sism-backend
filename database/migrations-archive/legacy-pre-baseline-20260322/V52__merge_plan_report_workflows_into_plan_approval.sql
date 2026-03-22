-- V52__merge_plan_report_workflows_into_plan_approval.sql
-- Description:
--   Merge PlanReport approval templates into the existing PLAN approval templates.
--   Future PlanReport submissions reuse:
--     - PLAN_APPROVAL_FUNCDEPT (id=3)
--     - PLAN_APPROVAL_COLLEGE (id=4)
--   Historical runtime rows are remapped to the closest surviving template/step ids
--   so stale references to flow/step definitions 5/6 and 31-44 are removed.

BEGIN;

-- 1) Remap existing runtime instances to surviving flow definitions.
UPDATE public.audit_instance
SET flow_def_id = CASE flow_def_id
    WHEN 5 THEN 3
    WHEN 6 THEN 4
    ELSE flow_def_id
END
WHERE flow_def_id IN (5, 6);

-- 2) Remap historical step instances away from the removed step definitions.
--    For former flow 5, step 33 is compacted to the surviving final review node.
UPDATE public.audit_step_instance
SET step_def_id = CASE step_def_id
        WHEN 31 THEN 7
        WHEN 32 THEN 8
        WHEN 33 THEN 10
        WHEN 41 THEN 11
        WHEN 42 THEN 12
        WHEN 43 THEN 13
        WHEN 44 THEN 14
        ELSE step_def_id
    END,
    step_name = CASE step_def_id
        WHEN 31 THEN '填报人提交'
        WHEN 32 THEN '职能部门审批人审批'
        WHEN 33 THEN '战略发展部终审人审批'
        WHEN 41 THEN '填报人提交'
        WHEN 42 THEN '二级学院审批人审批'
        WHEN 43 THEN '学院院长审批人审批'
        WHEN 44 THEN '职能部门终审人审批'
        ELSE step_name
    END,
    step_index = CASE step_def_id
        WHEN 31 THEN 1
        WHEN 32 THEN 2
        WHEN 33 THEN 4
        WHEN 41 THEN 1
        WHEN 42 THEN 2
        WHEN 43 THEN 3
        WHEN 44 THEN 4
        ELSE step_index
    END
WHERE step_def_id IN (31, 32, 33, 41, 42, 43, 44);

-- 3) Remove obsolete step definitions and flow definitions.
DELETE FROM public.audit_step_def
WHERE id IN (31, 32, 33, 41, 42, 43, 44)
   OR flow_id IN (5, 6);

DELETE FROM public.audit_flow_def
WHERE id IN (5, 6)
   OR flow_code IN ('PLAN_REPORT_FUNC', 'PLAN_REPORT_COLLEGE');

COMMIT;
