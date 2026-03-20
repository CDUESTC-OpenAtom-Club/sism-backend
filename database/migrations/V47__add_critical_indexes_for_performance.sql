-- V47__add_critical_indexes_for_performance.sql
-- 性能优化：为 indicator 表及其关联表添加关键索引
--
-- 问题分析：
-- 1. findByYear 使用原生 SQL JOIN 查询 (indicator -> sys_task -> plan -> cycle)
-- 2. indicator 表缺少 is_deleted + status 复合索引
-- 3. sys_task 和 plan 表缺少支撑 JOIN 查询的索引

-- ==================== indicator 表索引 ====================

-- 1. 为软删除+状态过滤添加复合索引（最常用的查询模式）
CREATE INDEX IF NOT EXISTS idx_indicator_deleted_status
ON indicator(is_deleted, status);

-- 2. 为状态过滤添加复合索引
CREATE INDEX IF NOT EXISTS idx_indicator_status_deleted
ON indicator(status, is_deleted);

-- 3. 为所有者+状态过滤添加复合索引
CREATE INDEX IF NOT EXISTS idx_indicator_owner_status
ON indicator(owner_org_id, status);

-- 4. 为目标组织+状态过滤添加复合索引
CREATE INDEX IF NOT EXISTS idx_indicator_target_status
ON indicator(target_org_id, status);

-- 5. 为创建时间排序添加索引
CREATE INDEX IF NOT EXISTS idx_indicator_created_at
ON indicator(created_at);

-- ==================== sys_task 表索引 ====================

-- 6. 为 plan_id 添加索引（支撑 indicator 的 JOIN 查询）
CREATE INDEX IF NOT EXISTS idx_task_plan_id
ON sys_task(plan_id);

-- ==================== plan 表索引 ====================

-- 7. 为 cycle_id 添加索引（支撑 JOIN 查询）
CREATE INDEX IF NOT EXISTS idx_plan_cycle_id
ON plan(cycle_id);

-- ==================== 收集统计信息 ====================

-- 收集表统计信息（帮助查询优化器选择正确的执行计划）
ANALYZE indicator;
ANALYZE sys_task;
ANALYZE plan;
ANALYZE cycle;

-- 验证索引创建成功
DO $$
DECLARE
    ind_idx_count INTEGER;
    task_idx_count INTEGER;
    plan_idx_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO ind_idx_count FROM pg_indexes WHERE tablename = 'indicator';
    SELECT COUNT(*) INTO task_idx_count FROM pg_indexes WHERE tablename = 'sys_task';
    SELECT COUNT(*) INTO plan_idx_count FROM pg_indexes WHERE tablename = 'plan';

    RAISE NOTICE '索引创建完成:';
    RAISE NOTICE '  - indicator: % 个索引', ind_idx_count;
    RAISE NOTICE '  - sys_task: % 个索引', task_idx_count;
    RAISE NOTICE '  - plan: % 个索引', plan_idx_count;
END $$;

RAISE NOTICE '性能优化索引迁移 V47 完成';
