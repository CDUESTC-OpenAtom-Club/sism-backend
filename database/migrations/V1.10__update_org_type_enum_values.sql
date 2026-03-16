-- V1.10: 更新组织类型枚举值以匹配新的代码定义
-- 将旧的类型值映射到新的 admin/functional/academic 三种类型
--
-- 组织类型映射关系：
--   Admin层级（系统管理层）:    SCHOOL, STRATEGY_DEPT, STRATEGIC_DEPT
--   Functional层级（职能部门）: FUNCTIONAL_DEPT, FUNCTION_DEPT
--   Academic层级（二级学院）:   COLLEGE, SECONDARY_COLLEGE, COLLEGE_GROUP, DIVISION
--
-- Author: System
-- Date: 2026-03-15
-- ============================================

DO $$
BEGIN
    -- 步骤 1: 将旧的类型值映射到新的 'admin' 类型
    -- 包括校级、战略部门
    UPDATE sys_org
    SET type = 'admin'
    WHERE type IN ('SCHOOL', 'STRATEGY_DEPT', 'STRATEGIC_DEPT');

    RAISE NOTICE '步骤1完成: 已将 SCHOOL, STRATEGY_DEPT, STRATEGIC_DEPT 映射到 admin';

    -- 步骤 2: 将旧的类型值映射到新的 'functional' 类型
    -- 包括各职能部门
    UPDATE sys_org
    SET type = 'functional'
    WHERE type IN ('FUNCTIONAL_DEPT', 'FUNCTION_DEPT');

    RAISE NOTICE '步骤2完成: 已将 FUNCTIONAL_DEPT, FUNCTION_DEPT 映射到 functional';

    -- 步骤 3: 将旧的学院和学部类型映射到新的 'academic' 类型
    -- 包括所有学院相关类型
    UPDATE sys_org
    SET type = 'academic'
    WHERE type IN ('COLLEGE', 'SECONDARY_COLLEGE', 'COLLEGE_GROUP', 'DIVISION');

    RAISE NOTICE '步骤3完成: 已将 COLLEGE, SECONDARY_COLLEGE, COLLEGE_GROUP, DIVISION 映射到 academic';

    -- 步骤 4: 处理 'OTHER' 类型
    -- 将其归类到默认类型 'functional'
    UPDATE sys_org
    SET type = 'functional'
    WHERE type = 'OTHER';

    RAISE NOTICE '步骤4完成: 已将 OTHER 映射到 functional';

END $$;

-- 步骤 5: 添加数据库约束以保证数据完整性
-- 防止未来有任何不符合枚举定义的值被插入数据库
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_sys_org_type'
        AND table_name = 'sys_org'
    ) THEN
        ALTER TABLE sys_org
        ADD CONSTRAINT chk_sys_org_type CHECK (type IN ('admin', 'functional', 'academic'));

        COMMENT ON CONSTRAINT chk_sys_org_type ON sys_org
        IS '确保组织类型只能是 admin, functional, 或 academic';
    END IF;
END $$;
