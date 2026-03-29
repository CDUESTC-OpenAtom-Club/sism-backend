-- V1.4: 为组织名称添加唯一约束
-- 兼容旧表名 org 与当前表名 sys_org

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'sys_org'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE table_schema = 'public'
              AND table_name = 'sys_org'
              AND constraint_name = 'uk_org_name'
        ) THEN
            ALTER TABLE public.sys_org ADD CONSTRAINT uk_org_name UNIQUE (name);
        END IF;

        EXECUTE 'COMMENT ON CONSTRAINT uk_org_name ON public.sys_org IS ''组织名称唯一约束''';
    ELSIF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'org'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE table_schema = 'public'
              AND table_name = 'org'
              AND constraint_name = 'uk_org_name'
        ) THEN
            ALTER TABLE public.org ADD CONSTRAINT uk_org_name UNIQUE (org_name);
        END IF;

        EXECUTE 'COMMENT ON CONSTRAINT uk_org_name ON public.org IS ''组织名称唯一约束''';
    END IF;
END $$;
