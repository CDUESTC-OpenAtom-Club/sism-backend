-- Migration V1.5: Migrate from org table to sys_org table
-- Date: 2026-02-11
-- Purpose: Consolidate organization management to sys_org table

-- ============================================
-- Step 1: Add unique constraint to sys_org.name
-- ============================================
ALTER TABLE sys_org ADD CONSTRAINT uk_sys_org_name UNIQUE (name);

COMMENT ON CONSTRAINT uk_sys_org_name ON sys_org IS '组织名称唯一约束';

-- ============================================
-- Step 2: Remove parent_org_id from sys_org (flat structure)
-- ============================================
ALTER TABLE sys_org DROP COLUMN IF EXISTS parent_org_id;

-- ============================================
-- Step 3: Migrate app_user foreign key from org to sys_org
-- ============================================

-- 3.1: Drop existing foreign key constraint on app_user.org_id FIRST
DO $$
DECLARE
    constraint_name_var TEXT;
BEGIN
    -- Find the constraint name dynamically
    SELECT constraint_name INTO constraint_name_var
    FROM information_schema.table_constraints
    WHERE table_name = 'app_user' 
      AND constraint_type = 'FOREIGN KEY'
      AND constraint_name LIKE '%org%';
    
    -- Drop the constraint if it exists
    IF constraint_name_var IS NOT NULL THEN
        EXECUTE 'ALTER TABLE app_user DROP CONSTRAINT ' || constraint_name_var;
        RAISE NOTICE 'Dropped constraint: %', constraint_name_var;
    ELSE
        RAISE NOTICE 'No foreign key constraint found on app_user.org_id';
    END IF;
END $$;

-- 3.2: Update app_user.org_id to reference sys_org.id
-- Current: app_user.org_id = 205 (党委保卫部 | 保卫处 in org table)
-- Target: sys_org.id = 42 (党委保卫部 | 保卫处)

-- Create a temporary mapping table
CREATE TEMP TABLE org_mapping AS
SELECT 
    o.org_id as old_org_id,
    s.id as new_org_id,
    o.org_name as org_name
FROM org o
INNER JOIN sys_org s ON o.org_name = s.name;

-- Show mapping for verification
DO $$
DECLARE
    mapping_record RECORD;
BEGIN
    FOR mapping_record IN SELECT * FROM org_mapping LOOP
        RAISE NOTICE 'Mapping: org_id % (%) -> sys_org.id %', 
            mapping_record.old_org_id, 
            mapping_record.org_name, 
            mapping_record.new_org_id;
    END LOOP;
END $$;

-- Update app_user to reference sys_org using the mapping
UPDATE app_user
SET org_id = (
    SELECT new_org_id 
    FROM org_mapping 
    WHERE old_org_id = app_user.org_id
    LIMIT 1
)
WHERE EXISTS (
    SELECT 1 
    FROM org_mapping 
    WHERE old_org_id = app_user.org_id
);

-- 3.3: Add new foreign key constraint
ALTER TABLE app_user 
    ADD CONSTRAINT fk_app_user_sys_org 
    FOREIGN KEY (org_id) 
    REFERENCES sys_org(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

COMMENT ON CONSTRAINT fk_app_user_sys_org ON app_user IS '用户所属组织外键约束';

-- ============================================
-- Step 4: Rename org table to org_deprecated
-- ============================================
ALTER TABLE org RENAME TO org_deprecated;

COMMENT ON TABLE org_deprecated IS '已废弃的组织表，已迁移到sys_org，保留用于数据恢复';

-- ============================================
-- Step 5: Add indexes to sys_org for performance
-- ============================================
CREATE INDEX IF NOT EXISTS idx_sys_org_type ON sys_org(type);
CREATE INDEX IF NOT EXISTS idx_sys_org_active ON sys_org(is_active);
CREATE INDEX IF NOT EXISTS idx_sys_org_sort ON sys_org(sort_order);

-- ============================================
-- Step 6: Verify migration
-- ============================================

-- Check sys_org has unique names
DO $$
DECLARE
    duplicate_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO duplicate_count
    FROM (
        SELECT name, COUNT(*) as cnt
        FROM sys_org
        GROUP BY name
        HAVING COUNT(*) > 1
    ) duplicates;
    
    IF duplicate_count > 0 THEN
        RAISE EXCEPTION 'Found % duplicate organization names in sys_org', duplicate_count;
    ELSE
        RAISE NOTICE 'Verification passed: All organization names are unique';
    END IF;
END $$;

-- Check app_user references valid sys_org
DO $$
DECLARE
    invalid_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO invalid_count
    FROM app_user u
    LEFT JOIN sys_org o ON u.org_id = o.id
    WHERE o.id IS NULL;
    
    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'Found % app_user records with invalid org_id', invalid_count;
    ELSE
        RAISE NOTICE 'Verification passed: All app_user records reference valid sys_org';
    END IF;
END $$;

-- ============================================
-- Migration Summary
-- ============================================
-- ✓ Added unique constraint to sys_org.name
-- ✓ Removed parent_org_id from sys_org (flat structure)
-- ✓ Migrated app_user foreign key to sys_org
-- ✓ Renamed org table to org_deprecated
-- ✓ Added performance indexes
-- ✓ Verified data integrity
