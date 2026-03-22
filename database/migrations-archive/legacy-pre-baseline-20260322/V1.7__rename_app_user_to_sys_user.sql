-- Migration V1.7: Rename app_user to sys_user
-- Date: 2026-02-10
-- Purpose: Consolidate user tables by renaming app_user to sys_user and removing old sys_user

-- Step 1: Backup old sys_user table (rename to deprecated)
ALTER TABLE IF EXISTS sys_user RENAME TO sys_user_deprecated;

-- Step 2: Rename app_user to sys_user
ALTER TABLE app_user RENAME TO sys_user;

-- Step 3: Rename sequence
ALTER SEQUENCE IF EXISTS app_user_user_id_seq RENAME TO sys_user_user_id_seq;

-- Step 4: Rename constraints (using actual constraint names)
-- Primary key
ALTER TABLE sys_user RENAME CONSTRAINT app_user_pkey1 TO sys_user_pkey;

-- Unique constraint
ALTER TABLE sys_user RENAME CONSTRAINT uk_3k4cplvh82srueuttfkwnylq0 TO sys_user_username_key;

-- Foreign key
ALTER TABLE sys_user RENAME CONSTRAINT fk_app_user_sys_org TO fk_sys_user_sys_org;

-- Check constraints
ALTER TABLE sys_user RENAME CONSTRAINT app_user_created_at_not_null1 TO sys_user_created_at_not_null;
ALTER TABLE sys_user RENAME CONSTRAINT app_user_is_active_not_null1 TO sys_user_is_active_not_null;
ALTER TABLE sys_user RENAME CONSTRAINT app_user_org_id_not_null1 TO sys_user_org_id_not_null;
ALTER TABLE sys_user RENAME CONSTRAINT app_user_password_hash_not_null1 TO sys_user_password_hash_not_null;
ALTER TABLE sys_user RENAME CONSTRAINT app_user_real_name_not_null1 TO sys_user_real_name_not_null;
ALTER TABLE sys_user RENAME CONSTRAINT app_user_updated_at_not_null1 TO sys_user_updated_at_not_null;
ALTER TABLE sys_user RENAME CONSTRAINT app_user_user_id_not_null1 TO sys_user_user_id_not_null;
ALTER TABLE sys_user RENAME CONSTRAINT app_user_username_not_null1 TO sys_user_username_not_null;

-- Step 5: Update column name from user_id to id for consistency
ALTER TABLE sys_user RENAME COLUMN user_id TO id;

-- Step 6: Add comment
COMMENT ON TABLE sys_user IS 'System user table (renamed from app_user on 2026-02-10)';

-- Verification queries
SELECT 'sys_user table created' as status, COUNT(*) as record_count FROM sys_user;
SELECT 'sys_user_deprecated table exists' as status, COUNT(*) as record_count FROM sys_user_deprecated;
