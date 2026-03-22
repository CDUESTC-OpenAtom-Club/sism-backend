-- sys_role / sys_role_permission alignment proposal
-- Review file only. Do not execute directly before business confirmation.
--
-- Goal:
-- 1. Editing/filling capability is reserved for:
--    - ROLE_REPORTER (5)
-- 2. Approval/leadership roles only keep approve/reject style permissions:
--    - ROLE_FUNC_DEPT_HEAD (6)
--    - ROLE_COLLEGE_DEAN (7)
--    - STRATEGY_DEPT_HEAD (8)
--    - ROLE_VICE_PRESIDENT (9)
--    - ROLE_STRATEGY_OFFICE (10)
-- 3. ROLE_ISSUER (11) is deprecated and no longer used for edit/submit capability
-- 3. audit_step_def submit nodes remain step_type='SUBMIT' with role_id NULL.
--    Submit actor comes from requesterId in workflow code, not from role_id.
-- 4. If a user must both edit and approve, it should be modeled by sys_user_role multi-role binding,
--    not by granting edit permissions to approval-only roles.
--
-- Important operational note:
-- - If "战略发展部操作人" must create/edit/submit dispatch content, that account should also hold ROLE_REPORTER (5).
-- - Example: zlb_admin currently has role 8 only; business may need an extra sys_user_role binding to role 5.

BEGIN;

-- =========================================================
-- Part 1: Target sys_role baseline
-- =========================================================
-- Existing role IDs are preserved and only normalized if needed.

UPDATE public.sys_role
SET
    role_code = 'ROLE_REPORTER',
    role_name = '填报人',
    data_access_mode = 'OWN_ORG',
    is_enabled = true,
    remark = '负责数据填报、草稿修改、提交'
WHERE id = 5;

UPDATE public.sys_role
SET
    role_code = 'ROLE_FUNC_DEPT_HEAD',
    role_name = '职能部门负责人',
    data_access_mode = 'OWN_ORG',
    is_enabled = true,
    remark = '仅负责职能部门审批，不负责填报修改'
WHERE id = 6;

UPDATE public.sys_role
SET
    role_code = 'ROLE_COLLEGE_DEAN',
    role_name = '二级学院院长',
    data_access_mode = 'OWN_ORG',
    is_enabled = true,
    remark = '仅负责学院审批，不负责填报修改'
WHERE id = 7;

UPDATE public.sys_role
SET
    role_code = 'STRATEGY_DEPT_HEAD',
    role_name = '战略发展部负责人',
    data_access_mode = 'ALL',
    is_enabled = true,
    remark = '仅负责战略审批，不负责填报修改'
WHERE id = 8;

UPDATE public.sys_role
SET
    role_code = 'ROLE_VICE_PRESIDENT',
    role_name = '分管校领导',
    data_access_mode = 'ALL',
    is_enabled = true,
    remark = '仅负责领导审批，不负责填报修改'
WHERE id = 9;

UPDATE public.sys_role
SET
    role_code = 'ROLE_STRATEGY_OFFICE',
    role_name = '战略发展部',
    data_access_mode = 'ALL',
    is_enabled = true,
    remark = '仅负责终审，不负责填报修改'
WHERE id = 10;

UPDATE public.sys_role
SET
    role_code = 'ROLE_ISSUER',
    role_name = '下发人',
    data_access_mode = 'OWN_ORG',
    is_enabled = false,
    remark = '已停用：编辑/提交能力已合并到 ROLE_REPORTER'
WHERE id = 11;

-- =========================================================
-- Part 2: Target sys_role_permission baseline
-- =========================================================
-- Strategy:
-- - Clean existing bindings for role 5~11
-- - Rebuild according to the reviewed permission model

DELETE FROM public.sys_role_permission
WHERE role_id IN (5, 6, 7, 8, 9, 10, 11);

-- ROLE_REPORTER (5): the only edit/submit role
INSERT INTO public.sys_role_permission (role_id, perm_id) VALUES
(5, 1),   -- PAGE_DASHBOARD
(5, 3),   -- PAGE_DATA_REPORT
(5, 4),   -- PAGE_INDICATOR_DISPATCH
(5, 5),   -- BTN_STRATEGY_TASK_DISPATCH_SUBMIT
(5, 8),   -- BTN_DATA_REPORT_SUBMIT
(5, 10);  -- BTN_INDICATOR_DISPATCH_SUBMIT

-- ROLE_FUNC_DEPT_HEAD (6): approve only
INSERT INTO public.sys_role_permission (role_id, perm_id) VALUES
(6, 1),   -- PAGE_DASHBOARD
(6, 3),   -- PAGE_DATA_REPORT (read/approval entry)
(6, 9),   -- BTN_DATA_REPORT_APPROVE
(6, 12);  -- BTN_INDICATOR_REPORT_APPROVE

-- ROLE_COLLEGE_DEAN (7): approve only
INSERT INTO public.sys_role_permission (role_id, perm_id) VALUES
(7, 1),   -- PAGE_DASHBOARD
(7, 3),   -- PAGE_DATA_REPORT (read/approval entry)
(7, 9),   -- BTN_DATA_REPORT_APPROVE
(7, 12);  -- BTN_INDICATOR_REPORT_APPROVE

-- STRATEGY_DEPT_HEAD (8): strategic approval only
INSERT INTO public.sys_role_permission (role_id, perm_id) VALUES
(8, 1),   -- PAGE_DASHBOARD
(8, 4),   -- PAGE_INDICATOR_DISPATCH (approval entry)
(8, 11),  -- BTN_INDICATOR_DISPATCH_APPROVE
(8, 12);  -- BTN_INDICATOR_REPORT_APPROVE

-- ROLE_VICE_PRESIDENT (9): approve only
INSERT INTO public.sys_role_permission (role_id, perm_id) VALUES
(9, 1),   -- PAGE_DASHBOARD
(9, 2),   -- PAGE_STRATEGY_TASK (approval entry)
(9, 3),   -- PAGE_DATA_REPORT (approval entry)
(9, 4),   -- PAGE_INDICATOR_DISPATCH (approval entry)
(9, 6),   -- BTN_STRATEGY_TASK_DISPATCH_APPROVE
(9, 7),   -- BTN_STRATEGY_TASK_REPORT_APPROVE
(9, 9),   -- BTN_DATA_REPORT_APPROVE
(9, 11),  -- BTN_INDICATOR_DISPATCH_APPROVE
(9, 12);  -- BTN_INDICATOR_REPORT_APPROVE

-- ROLE_STRATEGY_OFFICE (10): final approval only
INSERT INTO public.sys_role_permission (role_id, perm_id) VALUES
(10, 1),  -- PAGE_DASHBOARD
(10, 2),  -- PAGE_STRATEGY_TASK (approval entry)
(10, 3),  -- PAGE_DATA_REPORT (approval entry)
(10, 4),  -- PAGE_INDICATOR_DISPATCH (approval entry)
(10, 6),  -- BTN_STRATEGY_TASK_DISPATCH_APPROVE
(10, 7),  -- BTN_STRATEGY_TASK_REPORT_APPROVE
(10, 9),  -- BTN_DATA_REPORT_APPROVE
(10, 11), -- BTN_INDICATOR_DISPATCH_APPROVE
(10, 12); -- BTN_INDICATOR_REPORT_APPROVE

-- ROLE_ISSUER (11): deprecated, keep no permissions

COMMIT;

-- =========================================================
-- Suggested follow-up checks (review only)
-- =========================================================
-- 1. Check current user-role binding for operator accounts:
--    SELECT u.username, ur.role_id, r.role_code
--    FROM sys_user u
--    JOIN sys_user_role ur ON ur.user_id = u.id
--    JOIN sys_role r ON r.id = ur.role_id
--    WHERE u.username IN ('zlb_admin', 'zlb_final1', 'zlb_final2');
--
-- 2. If zlb_admin must edit/submit dispatch content, bind ROLE_REPORTER:
--    INSERT INTO public.sys_user_role (user_id, role_id)
--    SELECT u.id, 5
--    FROM public.sys_user u
--    WHERE u.username = 'zlb_admin'
--      AND NOT EXISTS (
--          SELECT 1 FROM public.sys_user_role ur
--          WHERE ur.user_id = u.id AND ur.role_id = 5
--      );
--
-- 3. Recheck role-permission matrix:
--    SELECT rp.role_id, r.role_code, p.perm_code
--    FROM public.sys_role_permission rp
--    JOIN public.sys_role r ON r.id = rp.role_id
--    JOIN public.sys_permission p ON p.id = rp.perm_id
--    WHERE rp.role_id IN (5,6,7,8,9,10,11)
--    ORDER BY rp.role_id, p.id;
