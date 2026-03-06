-- V6__seed_approval_data.sql
-- Seed Data for Multi-Level Approval Workflow
-- Created: 2026-02-25
-- Description: Populate user-supervisor relationships and org hierarchy for approval workflow

-- =====================================================
-- 1. Seed Organization Hierarchy
-- =====================================================
-- Primary organizations (level 1)
INSERT INTO sys_org_hierarchy (org_id, parent_org_id, level)
SELECT id, NULL, 1 
FROM sys_org 
WHERE name LIKE '%æç¥åå±é?' OR name LIKE '%Strategic%'
ON CONFLICT (org_id) DO NOTHING;

-- Secondary organizations (level 2) - under Strategic Development Dept
INSERT INTO sys_org_hierarchy (org_id, parent_org_id, level)
SELECT 
    child.id,
    parent.id,
    2
FROM sys_org child
JOIN sys_org parent ON parent.name LIKE '%æç¥åå±é?' OR parent.name LIKE '%Strategic%'
WHERE child.id != parent.id
  AND child.id NOT IN (SELECT org_id FROM sys_org_hierarchy WHERE level = 1)
ON CONFLICT (org_id) DO NOTHING;

-- =====================================================
-- 2. Seed User-Supervisor Relationships
-- =====================================================
-- Level 1: Direct supervisors (e.g., admin is direct supervisor for func_user and college_user)
INSERT INTO sys_user_supervisor (user_id, supervisor_id, level)
SELECT u.id, s.id, 1
FROM sys_user u
CROSS JOIN (
    SELECT id FROM sys_user WHERE username = 'admin' LIMIT 1
) s
WHERE u.username IN ('func_user', 'college_user', 'testuser')
  AND u.id != s.id
ON CONFLICT (user_id, level) DO NOTHING;

-- Level 2: Second-level supervisors (e.g., func_user is level-2 supervisor for college_user)
INSERT INTO sys_user_supervisor (user_id, supervisor_id, level)
SELECT u.id, s.id, 2
FROM sys_user u
CROSS JOIN (
    SELECT id FROM sys_user WHERE username = 'func_user' LIMIT 1
) s
WHERE u.username IN ('college_user')
  AND u.id != s.id
ON CONFLICT (user_id, level) DO NOTHING;

-- =====================================================
-- 3. Configure Audit Flow Steps for Multi-Level Approval
-- =====================================================
-- Configure PLAN_APPROVAL flow with multi-level approval
-- Step 1: Direct Supervisor Approval
INSERT INTO audit_step_def (
    flow_id,
    step_order,
    step_name,
    approver_type,
    supervisor_level,
    approval_mode,
    is_required,
    created_at
)
SELECT 
    af.id,
    1,
    'ç´æ¥ä¸»ç®¡å®¡æ¹',
    'SUPERVISOR',
    1,
    'SEQUENTIAL',
    true,
    CURRENT_TIMESTAMP
FROM audit_flow_def af
WHERE af.flow_code = 'PLAN_APPROVAL'
  AND NOT EXISTS (
      SELECT 1 FROM audit_step_def 
      WHERE flow_id = af.id AND step_order = 1
  );

-- Step 2: Level-2 Supervisor Approval
INSERT INTO audit_step_def (
    flow_id,
    step_order,
    step_name,
    approver_type,
    supervisor_level,
    approval_mode,
    is_required,
    created_at
)
SELECT 
    af.id,
    2,
    'äºçº§ä¸»ç®¡å®¡æ¹',
    'SUPERVISOR',
    2,
    'SEQUENTIAL',
    true,
    CURRENT_TIMESTAMP
FROM audit_flow_def af
WHERE af.flow_code = 'PLAN_APPROVAL'
  AND NOT EXISTS (
      SELECT 1 FROM audit_step_def 
      WHERE flow_id = af.id AND step_order = 2
  );

-- Step 3: Superior Department Approval (Joint/Parallel)
INSERT INTO audit_step_def (
    flow_id,
    step_order,
    step_name,
    approver_type,
    approval_mode,
    is_required,
    created_at
)
SELECT 
    af.id,
    3,
    'ä¸çº§é¨é¨å®¡æ¹',
    'ROLE',
    'PARALLEL',
    true,
    CURRENT_TIMESTAMP
FROM audit_flow_def af
WHERE af.flow_code = 'PLAN_APPROVAL'
  AND NOT EXISTS (
      SELECT 1 FROM audit_step_def 
      WHERE flow_id = af.id AND step_order = 3
  );

-- =====================================================
-- 4. Verify Data
-- =====================================================
DO $$
DECLARE
    v_supervisor_count INT;
    v_org_hierarchy_count INT;
    v_audit_steps_count INT;
BEGIN
    SELECT COUNT(*) INTO v_supervisor_count FROM sys_user_supervisor;
    SELECT COUNT(*) INTO v_org_hierarchy_count FROM sys_org_hierarchy;
    SELECT COUNT(*) INTO v_audit_steps_count FROM audit_step_def WHERE flow_id IN (
        SELECT id FROM audit_flow_def WHERE flow_code = 'PLAN_APPROVAL'
    );
    
    RAISE NOTICE '=== Seed Data Summary ===';
    RAISE NOTICE 'User-Supervisor relationships: %', v_supervisor_count;
    RAISE NOTICE 'Organization hierarchy entries: %', v_org_hierarchy_count;
    RAISE NOTICE 'Audit steps configured: %', v_audit_steps_count;
    
    IF v_supervisor_count > 0 AND v_audit_steps_count >= 3 THEN
        RAISE NOTICE 'Seed data populated successfully!';
    ELSE
        RAISE WARNING 'Some seed data may be missing. Please verify.';
    END IF;
END $$;
