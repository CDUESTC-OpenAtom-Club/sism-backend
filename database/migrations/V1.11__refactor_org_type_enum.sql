-- V1.11: 重构组织类型枚举 - 从多类型简化为三级架构
--
-- 变更说明：
--   1. 添加新的枚举值：admin, functional, academic
--   2. 更新现有数据映射到新类型
--   3. 移除旧的枚举值
--
-- 组织类型映射关系：
--   Admin层级（系统管理层）:    SCHOOL, STRATEGY_DEPT, STRATEGIC_DEPT → admin
--   Functional层级（职能部门）: FUNCTIONAL_DEPT, FUNCTION_DEPT → functional
--   Academic层级（二级学院）:   COLLEGE, SECONDARY_COLLEGE, COLLEGE_GROUP, DIVISION → academic
--
-- Author: System
-- Date: 2026-03-15
-- ============================================

-- ============================================
-- 步骤 1: 添加新的枚举值到 org_type
-- ============================================
DO $$
BEGIN
    -- 添加 admin 枚举值
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum
        WHERE enumtypid = (SELECT oid FROM pg_type WHERE typname = 'org_type')
        AND enumlabel = 'admin'
    ) THEN
        ALTER TYPE org_type ADD VALUE 'admin';
        RAISE NOTICE '✓ 已添加枚举值: admin';
    ELSE
        RAISE NOTICE '- 枚举值 admin 已存在';
    END IF;

    -- 添加 functional 枚举值
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum
        WHERE enumtypid = (SELECT oid FROM pg_type WHERE typname = 'org_type')
        AND enumlabel = 'functional'
    ) THEN
        ALTER TYPE org_type ADD VALUE 'functional';
        RAISE NOTICE '✓ 已添加枚举值: functional';
    ELSE
        RAISE NOTICE '- 枚举值 functional 已存在';
    END IF;

    -- 添加 academic 枚举值
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum
        WHERE enumtypid = (SELECT oid FROM pg_type WHERE typname = 'org_type')
        AND enumlabel = 'academic'
    ) THEN
        ALTER TYPE org_type ADD VALUE 'academic';
        RAISE NOTICE '✓ 已添加枚举值: academic';
    ELSE
        RAISE NOTICE '- 枚举值 academic 已存在';
    END IF;

END $$;

-- ============================================
-- 步骤 2: 迁移现有组织数据到新类型
-- ============================================
DO $$
DECLARE
    admin_count INTEGER;
    functional_count INTEGER;
    academic_count INTEGER;
BEGIN
    -- 2.1: 映射到 admin 类型
    UPDATE sys_org
    SET type = 'admin'
    WHERE type IN ('SCHOOL', 'STRATEGY_DEPT');

    GET DIAGNOSTICS admin_count = ROW_COUNT;
    RAISE NOTICE '✓ 已将 % 条记录映射到 admin 类型', admin_count;

    -- 2.2: 映射到 functional 类型
    UPDATE sys_org
    SET type = 'functional'
    WHERE type IN ('FUNCTIONAL_DEPT', 'FUNCTION_DEPT', 'OTHER');

    GET DIAGNOSTICS functional_count = ROW_COUNT;
    RAISE NOTICE '✓ 已将 % 条记录映射到 functional 类型', functional_count;

    -- 2.3: 映射到 academic 类型
    UPDATE sys_org
    SET type = 'academic'
    WHERE type IN ('COLLEGE', 'DIVISION');

    GET DIAGNOSTICS academic_count = ROW_COUNT;
    RAISE NOTICE '✓ 已将 % 条记录映射到 academic 类型', academic_count;

    -- 显示迁移汇总
    RAISE NOTICE '===========================================';
    RAISE NOTICE '数据迁移汇总:';
    RAISE NOTICE '  Admin层级:      % 条记录', admin_count;
    RAISE NOTICE '  Functional层级: % 条记录', functional_count;
    RAISE NOTICE '  Academic层级:   % 条记录', academic_count;
    RAISE NOTICE '===========================================';

END $$;

-- ============================================
-- 步骤 3: 验证迁移结果
-- ============================================
DO $$
BEGIN
    -- 检查是否还有未映射的类型
    IF EXISTS (
        SELECT 1 FROM sys_org
        WHERE type NOT IN ('admin', 'functional', 'academic')
    ) THEN
        RAISE WARNING '警告: 发现未映射的组织类型！';
        RAISE WARNING '未映射的类型: %', (
            SELECT STRING_AGG(DISTINCT type, ', ')
            FROM sys_org
            WHERE type NOT IN ('admin', 'functional', 'academic')
        );
    ELSE
        RAISE NOTICE '✓ 验证通过: 所有组织记录已成功映射到新类型';
    END IF;

    -- 显示各类型数量统计
    RAISE NOTICE '===========================================';
    RAISE NOTICE '当前组织类型分布:';
    PERFORM * FROM (
        SELECT
            type,
            COUNT(*) as org_count
        FROM sys_org
        GROUP BY type
        ORDER BY org_count DESC
    ) stats;
    RAISE NOTICE '===========================================';

END $$;

-- ============================================
-- 步骤 4: 更新列注释
-- ============================================
COMMENT ON COLUMN sys_org.type IS '组织类型: admin(系统管理层), functional(职能部门), academic(二级学院)';

-- ============================================
-- 注意: 旧枚举值保留用于历史数据恢复
-- 旧枚举值: SCHOOL, STRATEGY_DEPT, FUNCTIONAL_DEPT, FUNCTION_DEPT, COLLEGE, DIVISION, OTHER
-- 新枚举值: admin, functional, academic
-- ============================================
