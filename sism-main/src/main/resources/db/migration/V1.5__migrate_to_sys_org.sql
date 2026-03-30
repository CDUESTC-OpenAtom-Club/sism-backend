-- Migration V1.5: Migrate from org table to sys_org table
-- Date: 2026-02-11
-- Purpose: Consolidate organization management to sys_org table

-- ============================================
-- Step 1: Add unique constraint to sys_org.name
-- ============================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'sys_org'
          AND constraint_name = 'uk_sys_org_name'
    ) THEN
        ALTER TABLE sys_org ADD CONSTRAINT uk_sys_org_name UNIQUE (name);
    END IF;

    EXECUTE 'COMMENT ON CONSTRAINT uk_sys_org_name ON public.sys_org IS ''组织名称唯一约束''';
END $$;

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

-- Create a temporary mapping table only if org table exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'org'
    ) THEN
        CREATE TEMP TABLE org_mapping AS
        SELECT
            o.org_id as old_org_id,
            s.id as new_org_id,
            o.org_name as org_name
        FROM org o
        INNER JOIN sys_org s ON o.org_name = s.name;
    ELSE
        -- Create an empty temp table to avoid errors in subsequent steps
        CREATE TEMP TABLE org_mapping (
            old_org_id bigint,
            new_org_id bigint,
            org_name text
        );
    END IF;
END $$;

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
-- Only execute if org_mapping has data
DO $$
DECLARE
    mapping_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO mapping_count FROM org_mapping;

    IF mapping_count > 0 THEN
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

        RAISE NOTICE 'Updated % app_user records to reference sys_org', mapping_count;
    ELSE
        RAISE NOTICE 'No org mapping data found - skipping app_user update';
    END IF;
END $$;

-- 3.3: Add new foreign key constraint (only if not exists)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'app_user'
          AND constraint_name = 'fk_app_user_sys_org'
    ) THEN
        ALTER TABLE app_user
            ADD CONSTRAINT fk_app_user_sys_org
            FOREIGN KEY (org_id)
            REFERENCES sys_org(id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE;
    END IF;

    EXECUTE 'COMMENT ON CONSTRAINT fk_app_user_sys_org ON public.app_user IS ''用户所属组织外键约束''';
END $$;

-- ============================================
-- Step 4: Rename org table to org_deprecated
-- ============================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'org'
    ) THEN
        ALTER TABLE org RENAME TO org_deprecated;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'org_deprecated'
    ) THEN
        EXECUTE 'COMMENT ON TABLE public.org_deprecated IS ''已废弃的组织表，已迁移到sys_org，保留用于数据恢复''';
    END IF;
END $$;

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
