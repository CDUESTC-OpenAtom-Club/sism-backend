-- =============================================================================
-- 审阅版: 补齐上报审批流程模板（不直接提交）
-- 说明:
-- 1. 当前业务代码对“报告上报审批”依赖两个流程编码:
--    - PLAN_REPORT_FUNC
--    - PLAN_REPORT_COLLEGE
-- 2. 本脚本只补 audit_flow_def / audit_step_def 的模板定义
-- 3. 默认以 ROLLBACK 结束，便于先审核流程名、步骤名、角色ID
-- 4. 审核通过后，可复制为正式脚本并将最后一行改为 COMMIT;
-- =============================================================================

\echo '============================================================'
\echo '报告上报审批流程模板 - 审阅版预演开始'
\echo '本次执行会在最后 ROLLBACK，不会写入数据库'
\echo '============================================================'
\echo ''

BEGIN;

-- -----------------------------------------------------------------------------
-- 0. 角色口径
-- -----------------------------------------------------------------------------
-- role_id = 5  : ROLE_REPORTER        填报人
-- role_id = 6  : ROLE_FUNC_DEPT_HEAD  职能部门负责人
-- role_id = 7  : ROLE_COLLEGE_DEAN    二级学院院长
--
-- 当前模板采用最小可跑通链路:
--   1. 填报人提交
--   2. 对应组织负责人审批（终审）
-- -----------------------------------------------------------------------------

-- -----------------------------------------------------------------------------
-- 1. 补齐 audit_flow_def
-- -----------------------------------------------------------------------------
INSERT INTO audit_flow_def (
    flow_code,
    flow_name,
    is_enabled,
    created_at,
    updated_at,
    description,
    version,
    remark,
    entity_type
)
SELECT
    'PLAN_REPORT_FUNC',
    '职能部门月度填报审批',
    TRUE,
    NOW(),
    NOW(),
    '审阅版模板: 职能部门报告提交后进入负责人审批',
    1,
    'review-only seed',
    'PlanReport'
WHERE NOT EXISTS (
    SELECT 1
    FROM audit_flow_def
    WHERE flow_code = 'PLAN_REPORT_FUNC'
);

INSERT INTO audit_flow_def (
    flow_code,
    flow_name,
    is_enabled,
    created_at,
    updated_at,
    description,
    version,
    remark,
    entity_type
)
SELECT
    'PLAN_REPORT_COLLEGE',
    '二级学院月度填报审批',
    TRUE,
    NOW(),
    NOW(),
    '审阅版模板: 二级学院报告提交后进入院长审批',
    1,
    'review-only seed',
    'PlanReport'
WHERE NOT EXISTS (
    SELECT 1
    FROM audit_flow_def
    WHERE flow_code = 'PLAN_REPORT_COLLEGE'
);

-- -----------------------------------------------------------------------------
-- 2. 补齐 audit_step_def
-- -----------------------------------------------------------------------------
-- PLAN_REPORT_FUNC: 提交 -> 职能部门负责人审批
INSERT INTO audit_step_def (
    flow_id,
    step_name,
    step_type,
    role_id,
    is_terminal,
    created_at,
    updated_at,
    step_no
)
SELECT
    f.id,
    v.step_name,
    v.step_type,
    v.role_id,
    v.is_terminal,
    NOW(),
    NOW(),
    v.step_no
FROM audit_flow_def f
JOIN (
    VALUES
        (1, '填报人提交', 'SUBMIT', NULL::bigint, FALSE),
        (2, '职能部门负责人审批', 'APPROVAL', 6::bigint, TRUE)
) AS v(step_no, step_name, step_type, role_id, is_terminal)
    ON TRUE
WHERE f.flow_code = 'PLAN_REPORT_FUNC'
  AND NOT EXISTS (
      SELECT 1
      FROM audit_step_def s
      WHERE s.flow_id = f.id
  );

-- PLAN_REPORT_COLLEGE: 提交 -> 学院院长审批
INSERT INTO audit_step_def (
    flow_id,
    step_name,
    step_type,
    role_id,
    is_terminal,
    created_at,
    updated_at,
    step_no
)
SELECT
    f.id,
    v.step_name,
    v.step_type,
    v.role_id,
    v.is_terminal,
    NOW(),
    NOW(),
    v.step_no
FROM audit_flow_def f
JOIN (
    VALUES
        (1, '填报人提交', 'SUBMIT', NULL::bigint, FALSE),
        (2, '学院院长审批', 'APPROVAL', 7::bigint, TRUE)
) AS v(step_no, step_name, step_type, role_id, is_terminal)
    ON TRUE
WHERE f.flow_code = 'PLAN_REPORT_COLLEGE'
  AND NOT EXISTS (
      SELECT 1
      FROM audit_step_def s
      WHERE s.flow_id = f.id
  );

-- -----------------------------------------------------------------------------
-- 3. 审核查看
-- -----------------------------------------------------------------------------
\echo '--- flow_def 审阅结果 ---'
SELECT
    id,
    flow_code,
    flow_name,
    entity_type,
    is_enabled,
    version,
    description
FROM audit_flow_def
WHERE flow_code IN ('PLAN_REPORT_FUNC', 'PLAN_REPORT_COLLEGE')
ORDER BY flow_code;

\echo '--- step_def 审阅结果 ---'
SELECT
    f.flow_code,
    s.id,
    s.step_no,
    s.step_name,
    s.step_type,
    s.role_id,
    s.is_terminal
FROM audit_step_def s
JOIN audit_flow_def f
    ON f.id = s.flow_id
WHERE f.flow_code IN ('PLAN_REPORT_FUNC', 'PLAN_REPORT_COLLEGE')
ORDER BY f.flow_code, s.step_no;

\echo ''
\echo '审阅完成，默认回滚。若确认无误，请改为正式执行脚本后使用 COMMIT。'

ROLLBACK;
