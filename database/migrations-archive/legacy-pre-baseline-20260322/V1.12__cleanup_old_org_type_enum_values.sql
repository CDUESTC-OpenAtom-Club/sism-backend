-- V1.12: 清理旧的 org_type 枚举值
--
-- 说明: 由于系统尚未上线，我们严格遵守三层架构，清理不再使用的旧枚举值
-- 注意: PostgreSQL 不允许删除正在使用的枚举值，因此我们采用重建枚举类型的方式
--
-- 保留的枚举值: admin, functional, academic
-- 删除的枚举值: STRATEGY_DEPT, FUNCTION_DEPT, COLLEGE, DIVISION, SCHOOL, FUNCTIONAL_DEPT, OTHER
--
-- Author: System
-- Date: 2026-03-15
-- ============================================

-- ============================================
-- 步骤 1: 创建新的枚举类型（仅包含三层架构）
-- ============================================
CREATE TYPE org_type_new AS ENUM ('admin', 'functional', 'academic');

COMMENT ON TYPE org_type_new IS '组织类型枚举 - 三层架构: admin(系统管理层), functional(职能部门), academic(二级学院)';

RAISE NOTICE '✓ 已创建新的枚举类型 org_type_new';

-- ============================================
-- 步骤 2: 将 sys_org.type 列转换为新的枚举类型
-- ============================================
-- 2.1: 先将列临时改为 VARCHAR 类型
ALTER TABLE sys_org
    ALTER COLUMN type TYPE VARCHAR(20);

RAISE NOTICE '✓ 已将 sys_org.type 临时转换为 VARCHAR';

-- 2.2: 验证所有数据都在新枚举值范围内
DO $$
DECLARE
    invalid_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO invalid_count
    FROM sys_org
    WHERE type NOT IN ('admin', 'functional', 'academic');

    IF invalid_count > 0 THEN
        RAISE EXCEPTION '发现 % 条记录使用不在新枚举值范围内的类型！', invalid_count;
    ELSE
        RAISE NOTICE '✓ 验证通过: 所有数据都在新枚举值范围内';
    END IF;
END $$;

-- 2.3: 将列改为新的枚举类型
ALTER TABLE sys_org
    ALTER COLUMN type TYPE org_type_new
    USING type::org_type_new;

RAISE NOTICE '✓ 已将 sys_org.type 转换为新的枚举类型 org_type_new';

-- ============================================
-- 步骤 3: 删除旧的枚举类型
-- ============================================
-- 注意: 需要先删除旧的枚举类型
DROP TYPE org_type;

RAISE NOTICE '✓ 已删除旧的枚举类型 org_type';

-- ============================================
-- 步骤 4: 重命名新枚举类型为原名称
-- ============================================
ALTER TYPE org_type_new RENAME TO org_type;

COMMENT ON TYPE org_type IS '组织类型枚举 - 三层架构: admin(系统管理层), functional(职能部门), academic(二级学院)';

RAISE NOTICE '✓ 已重命名 org_type_new → org_type';

-- ============================================
-- 步骤 5: 添加检查约束以确保数据完整性
-- ============================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_sys_org_type'
        AND table_name = 'sys_org'
    ) THEN
        ALTER TABLE sys_org
        ADD CONSTRAINT chk_sys_org_type
        CHECK (type IN ('admin', 'functional', 'academic'));

        COMMENT ON CONSTRAINT chk_sys_org_type ON sys_org
        IS '确保组织类型符合三层架构: admin, functional, academic';

        RAISE NOTICE '✓ 已添加检查约束 chk_sys_org_type';
    ELSE
        RAISE NOTICE '- 检查约束已存在';
    END IF;
END $$;

-- ============================================
-- 步骤 6: 验证最终结果
-- ============================================
DO $$
BEGIN
    RAISE NOTICE '===========================================';
    RAISE NOTICE '枚举值清理完成！';
    RAISE NOTICE '===========================================';
    RAISE NOTICE '当前 org_type 枚举值:';

    PERFORM *
    FROM (
        SELECT enumlabel as 枚举值
        FROM pg_enum
        WHERE enumtypid = (SELECT oid FROM pg_type WHERE typname = 'org_type')
        ORDER BY enumsortorder
    ) enum_values;

    RAISE NOTICE '===========================================';

    -- 显示组织类型分布
    RAISE NOTICE '当前组织类型分布:';
    PERFORM *
    FROM (
        SELECT
            type as 类型,
            COUNT(*) as 数量
        FROM sys_org
        GROUP BY type
        ORDER BY type
    ) org_stats;

    RAISE NOTICE '===========================================';
    RAISE NOTICE '✓ 三层架构严格执行完成！';
    RAISE NOTICE '===========================================';

END $$;

-- ============================================
-- 完成标记
-- ============================================
-- 旧枚举值已完全清除:
--   ✓ STRATEGY_DEPT
--   ✓ FUNCTION_DEPT
--   ✓ COLLEGE
--   ✓ DIVISION
--   ✓ SCHOOL
--   ✓ FUNCTIONAL_DEPT
--   ✓ OTHER
--
-- 新枚举值（严格三层架构）:
--   ✓ admin        - 系统管理层
--   ✓ functional   - 职能部门
--   ✓ academic     - 二级学院
-- ============================================
