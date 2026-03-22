-- sys_user_role alignment proposal
-- Review file only. Do not execute directly before business confirmation.
--
-- Purpose:
-- 1. Align operator / reporter / approver / final approver accounts with the reviewed workflow model
-- 2. Make "can edit/submit" modeled by explicit role binding in sys_user_role
-- 3. Keep approval-only accounts free from edit-capable roles
--
-- Reviewed role baseline:
--   5  = ROLE_REPORTER         (填报人，唯一编辑/提交角色)
--   6  = ROLE_FUNC_DEPT_HEAD   (职能部门负责人，审批)
--   7  = ROLE_COLLEGE_DEAN     (二级学院院长，审批)
--   8  = STRATEGY_DEPT_HEAD    (战略发展部负责人，审批)
--   10 = ROLE_STRATEGY_OFFICE  (战略发展部，终审)
--   11 = ROLE_ISSUER           (已停用，不再分配)
--
-- Key business rule:
-- - "操作人" should have edit capability through ROLE_REPORTER
-- - "审批人/终审人" should not receive edit-capable roles unless business explicitly wants mixed duties

BEGIN;

-- =========================================================
-- Part 1: Strategy department accounts
-- =========================================================

-- zlb_admin:
-- Current state: role 8 only
-- Target state:
--   - retain 8 (战略发展部负责人/审批视角)
--   - add 5 (填报人/制单发起视角)
INSERT INTO public.sys_user_role (user_id, role_id)
SELECT u.id, 5
FROM public.sys_user u
WHERE u.username = 'zlb_admin'
  AND NOT EXISTS (
      SELECT 1
      FROM public.sys_user_role ur
      WHERE ur.user_id = u.id
        AND ur.role_id = 5
  );

-- zlb_final1:
-- Target state: final/strategy approval role only
DELETE FROM public.sys_user_role
WHERE user_id = (SELECT id FROM public.sys_user WHERE username = 'zlb_final1')
  AND role_id IN (5, 11);

INSERT INTO public.sys_user_role (user_id, role_id)
SELECT u.id, 10
FROM public.sys_user u
WHERE u.username = 'zlb_final1'
  AND NOT EXISTS (
      SELECT 1
      FROM public.sys_user_role ur
      WHERE ur.user_id = u.id
        AND ur.role_id = 10
  );

-- zlb_final2:
-- Target state: final/strategy approval role only
DELETE FROM public.sys_user_role
WHERE user_id = (SELECT id FROM public.sys_user WHERE username = 'zlb_final2')
  AND role_id IN (5, 11);

INSERT INTO public.sys_user_role (user_id, role_id)
SELECT u.id, 10
FROM public.sys_user u
WHERE u.username = 'zlb_final2'
  AND NOT EXISTS (
      SELECT 1
      FROM public.sys_user_role ur
      WHERE ur.user_id = u.id
        AND ur.role_id = 10
  );

-- =========================================================
-- Part 2: Functional department accounts
-- =========================================================

-- Reporter accounts should keep reporter role only
DELETE FROM public.sys_user_role
WHERE user_id IN (
    SELECT id
    FROM public.sys_user
    WHERE username IN ('baowei_report', 'jiaowu_report', 'dangban_report')
)
AND role_id IN (6, 11);

INSERT INTO public.sys_user_role (user_id, role_id)
SELECT u.id, 5
FROM public.sys_user u
WHERE u.username IN ('baowei_report', 'jiaowu_report', 'dangban_report')
  AND NOT EXISTS (
      SELECT 1
      FROM public.sys_user_role ur
      WHERE ur.user_id = u.id
        AND ur.role_id = 5
  );

-- Functional approver accounts should keep approval role only
DELETE FROM public.sys_user_role
WHERE user_id IN (
    SELECT id
    FROM public.sys_user
    WHERE username IN ('baowei_audit1', 'baowei_audit2', 'jiaowu_audit1', 'dangban_audit1', 'dangban_audit2')
)
AND role_id IN (5, 11);

INSERT INTO public.sys_user_role (user_id, role_id)
SELECT u.id, 6
FROM public.sys_user u
WHERE u.username IN ('baowei_audit1', 'baowei_audit2', 'jiaowu_audit1', 'dangban_audit1', 'dangban_audit2')
  AND NOT EXISTS (
      SELECT 1
      FROM public.sys_user_role ur
      WHERE ur.user_id = u.id
        AND ur.role_id = 6
  );

-- =========================================================
-- Part 3: College accounts
-- =========================================================

-- College reporter keeps reporter role
DELETE FROM public.sys_user_role
WHERE user_id = (SELECT id FROM public.sys_user WHERE username = 'makesi_report')
  AND role_id IN (7, 11);

INSERT INTO public.sys_user_role (user_id, role_id)
SELECT u.id, 5
FROM public.sys_user u
WHERE u.username = 'makesi_report'
  AND NOT EXISTS (
      SELECT 1
      FROM public.sys_user_role ur
      WHERE ur.user_id = u.id
        AND ur.role_id = 5
  );

-- College approver accounts keep college approval role only
DELETE FROM public.sys_user_role
WHERE user_id IN (
    SELECT id
    FROM public.sys_user
    WHERE username IN ('makesi_audit1', 'makesi_audit2')
)
AND role_id IN (5, 11);

INSERT INTO public.sys_user_role (user_id, role_id)
SELECT u.id, 7
FROM public.sys_user u
WHERE u.username IN ('makesi_audit1', 'makesi_audit2')
  AND NOT EXISTS (
      SELECT 1
      FROM public.sys_user_role ur
      WHERE ur.user_id = u.id
        AND ur.role_id = 7
  );

COMMIT;

-- =========================================================
-- Suggested review query
-- =========================================================
-- SELECT u.username, u.real_name, ur.role_id, r.role_code, r.role_name
-- FROM public.sys_user u
-- JOIN public.sys_user_role ur ON ur.user_id = u.id
-- JOIN public.sys_role r ON r.id = ur.role_id
-- WHERE u.username IN (
--   'zlb_admin','zlb_final1','zlb_final2',
--   'baowei_report','baowei_audit1','baowei_audit2',
--   'jiaowu_report','jiaowu_audit1','jiaowu_reserve',
-- Note: jiaowu_reserve is a reserve account and should remain unbound in sys_user_role.
--   'dangban_report','dangban_audit1','dangban_audit2',
--   'makesi_report','makesi_audit1','makesi_audit2'
-- )
-- ORDER BY u.username, ur.role_id;
