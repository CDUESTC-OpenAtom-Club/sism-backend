-- V1.10: 更新组织类型枚举值以匹配新的代码定义
-- 将旧的类型值映射到新的 admin/functional/academic 三种类型

DO $$
BEGIN
    -- 步骤 1: 将旧的类型值映射到新的 'functional' 类型
    -- 包括 FUNCTIONAL_DEPT 和其别名 FUNCTION_DEPT
    UPDATE sys_org
    SET type = 'functional'
    WHERE type IN ('FUNCTIONAL_DEPT', 'FUNCTION_DEPT');

    -- 步骤 2: 将旧的学院和学部类型映射到新的 'academic' 类型
    -- 包括 COLLEGE 和其下的 DIVISION
    UPDATE sys_org
    SET type = 'academic'
    WHERE type IN ('COLLEGE', 'DIVISION');

    -- 步骤 3: 将校级和战略部门映射到新的 'admin' 类型
    -- 假设 SCHOOL 和 STRATEGY_DEPT 都属于最高管理层级
    UPDATE sys_org
    SET type = 'admin'
    WHERE type IN ('SCHOOL', 'STRATEGY_DEPT');

    -- 步骤 4: 处理 'OTHER' 类型
    -- 将其归类到默认类型 'functional'
    UPDATE sys_org
    SET type = 'functional'
    WHERE type = 'OTHER';

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
