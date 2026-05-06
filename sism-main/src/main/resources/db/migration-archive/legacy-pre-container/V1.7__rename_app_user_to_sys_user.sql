-- Migration V1.7: Rename app_user to sys_user
-- Date: 2026-02-10
-- Purpose: Consolidate user tables by renaming app_user to sys_user and removing old sys_user

-- Step 1: Backup old sys_user table (rename to deprecated)
ALTER TABLE IF EXISTS sys_user RENAME TO sys_user_deprecated;

-- Step 2: Rename app_user to sys_user (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'app_user') THEN
        ALTER TABLE app_user RENAME TO sys_user;
        RAISE NOTICE 'Renamed app_user to sys_user';
    ELSE
        RAISE NOTICE 'app_user table not found, skipping rename';
    END IF;
END $$;

-- Step 3: Rename sequence
ALTER SEQUENCE IF EXISTS app_user_user_id_seq RENAME TO sys_user_user_id_seq;

-- Step 4: Rename constraints (using actual constraint names) - only if sys_user table exists and constraints exist
DO $$
BEGIN
    -- Check if sys_user table exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'sys_user') THEN

        -- Primary key
        IF EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_schema = 'public' AND table_name = 'sys_user' AND constraint_name = 'app_user_pkey1') THEN
            ALTER TABLE sys_user RENAME CONSTRAINT app_user_pkey1 TO sys_user_pkey;
            RAISE NOTICE 'Renamed primary key constraint';
        END IF;

        -- Unique constraint
        IF EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_schema = 'public' AND table_name = 'sys_user' AND constraint_name = 'uk_3k4cplvh82srueuttfkwnylq0') THEN
            ALTER TABLE sys_user RENAME CONSTRAINT uk_3k4cplvh82srueuttfkwnylq0 TO sys_user_username_key;
            RAISE NOTICE 'Renamed unique constraint';
        END IF;

        -- Foreign key
        IF EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_schema = 'public' AND table_name = 'sys_user' AND constraint_name = 'fk_app_user_sys_org') THEN
            ALTER TABLE sys_user RENAME CONSTRAINT fk_app_user_sys_org TO fk_sys_user_sys_org;
            RAISE NOTICE 'Renamed foreign key constraint';
        END IF;

        -- Check constraints
        IF EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_schema = 'public' AND table_name = 'sys_user' AND constraint_name = 'app_user_created_at_not_null1') THEN
            ALTER TABLE sys_user RENAME CONSTRAINT app_user_created_at_not_null1 TO sys_user_created_at_not_null;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_schema = 'public' AND table_name = 'sys_user' AND constraint_name = 'app_user_is_active_not_null1') THEN
            ALTER TABLE sys_user RENAME CONSTRAINT app_user_is_active_not_null1 TO sys_user_is_active_not_null;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_schema = 'public' AND table_name = 'sys_user' AND constraint_name = 'app_user_org_id_not_null1') THEN
            ALTER TABLE sys_user RENAME CONSTRAINT app_user_org_id_not_null1 TO sys_user_org_id_not_null;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_schema = 'public' AND table_name = 'sys_user' AND constraint_name = 'app_user_password_hash_not_null1') THEN
            ALTER TABLE sys_user RENAME CONSTRAINT app_user_password_hash_not_null1 TO sys_user_password_hash_not_null;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_schema = 'public' AND table_name = 'sys_user' AND constraint_name = 'app_user_real_name_not_null1') THEN
            ALTER TABLE sys_user RENAME CONSTRAINT app_user_real_name_not_null1 TO sys_user_real_name_not_null;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_schema = 'public' AND table_name = 'sys_user' AND constraint_name = 'app_user_updated_at_not_null1') THEN
            ALTER TABLE sys_user RENAME CONSTRAINT app_user_updated_at_not_null1 TO sys_user_updated_at_not_null;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_schema = 'public' AND table_name = 'sys_user' AND constraint_name = 'app_user_user_id_not_null1') THEN
            ALTER TABLE sys_user RENAME CONSTRAINT app_user_user_id_not_null1 TO sys_user_user_id_not_null;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_schema = 'public' AND table_name = 'sys_user' AND constraint_name = 'app_user_username_not_null1') THEN
            ALTER TABLE sys_user RENAME CONSTRAINT app_user_username_not_null1 TO sys_user_username_not_null;
        END IF;

    END IF;
END $$;

-- Step 5: Update column name from user_id to id for consistency (if sys_user exists and column exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'sys_user') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'sys_user' AND column_name = 'user_id') THEN
            ALTER TABLE sys_user RENAME COLUMN user_id TO id;
            RAISE NOTICE 'Renamed user_id to id';
        END IF;
    END IF;
END $$;

-- Step 6: Add comment (if sys_user exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'sys_user') THEN
        COMMENT ON TABLE sys_user IS 'System user table (renamed from app_user on 2026-02-10)';
    END IF;
END $$;

-- Verification queries (only if tables exist)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'sys_user') THEN
        PERFORM 'sys_user table created' as status, COUNT(*) as record_count FROM sys_user;
        RAISE NOTICE 'sys_user table created: % records', (SELECT COUNT(*) FROM sys_user);
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'sys_user_deprecated') THEN
        PERFORM 'sys_user_deprecated table exists' as status, COUNT(*) as record_count FROM sys_user_deprecated;
        RAISE NOTICE 'sys_user_deprecated table exists: % records', (SELECT COUNT(*) FROM sys_user_deprecated);
    END IF;
END $$;
