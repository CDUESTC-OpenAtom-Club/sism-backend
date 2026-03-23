-- Migration V1.8: Migrate task to strategic_task
-- Date: 2026-02-10
-- Purpose: Migrate data from task table to strategic_task and deprecate old task table

-- Step 1: Drop old foreign key constraints pointing to org_deprecated
ALTER TABLE strategic_task DROP CONSTRAINT IF EXISTS fkau0e6evpx28sq1ex0j4ivqv0m;
ALTER TABLE strategic_task DROP CONSTRAINT IF EXISTS fk3sb68smrgnjoste981m3eokxl;

-- Step 2: Ensure foreign keys to sys_org exist (should already exist from previous migrations)
-- These constraints should already exist: fk3tyk1n5u74bcxtjktmyq6do8c and fkktdybr93c6kg19alcjtto3x9t

-- Step 3: Migrate data from task to strategic_task
INSERT INTO strategic_task (
    task_id,
    cycle_id,
    task_name,
    task_desc,
    task_type,
    org_id,
    created_by_org_id,
    plan_id,
    sort_order,
    remark,
    created_at,
    updated_at,
    is_deleted,
    name,
    type
)
SELECT 
    t.id as task_id,
    p.cycle_id,
    t.name as task_name,
    t.desc as task_desc,
    t.type::varchar as task_type,
    p.target_org_id as org_id,
    p.created_by_org_id,
    t.plan_id,
    t.sort_order,
    t.remark,
    t.created_at,
    t.updated_at,
    t.is_deleted,
    t.name as name,
    t.type as type
FROM task t
INNER JOIN plan p ON t.plan_id = p.id
WHERE NOT EXISTS (
    SELECT 1 FROM strategic_task st WHERE st.task_id = t.id
);

-- Step 4: Update sequence to continue from max task_id
SELECT setval('strategic_task_task_id_seq', (SELECT COALESCE(MAX(task_id), 0) + 1 FROM strategic_task), false);

-- Step 5: Rename old task table to task_deprecated
ALTER TABLE task RENAME TO task_deprecated;

-- Step 6: Add comments
COMMENT ON TABLE strategic_task IS 'Strategic task table (migrated from task on 2026-02-10)';
COMMENT ON TABLE task_deprecated IS 'Deprecated task table (replaced by strategic_task on 2026-02-10)';

-- Verification queries
SELECT 'strategic_task records' as status, COUNT(*) as count FROM strategic_task;
SELECT 'task_deprecated records' as status, COUNT(*) as count FROM task_deprecated;
