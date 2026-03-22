-- ============================================================
-- V39__cleanup_test_data.sql
-- 清理测试数据，保留真实业务数据
--
-- 测试数据:
-- - 6个测试任务 (task_id: 92062-92064, 92092-92094)
-- - 385个测试指标
-- - 1616个测试里程碑
--
-- 真实数据:
-- - 8个真实任务
-- - 65个真实指标
-- ============================================================

-- 步骤1: 删除测试里程碑 (先删除子表)
DELETE FROM indicator_milestone
WHERE indicator_id IN (
    SELECT id FROM indicator
    WHERE task_id IN (92062, 92063, 92064, 92092, 92093, 92094)
);
SELECT '已删除测试里程碑' as status, COUNT(*) as deleted_rows FROM indicator_milestone WHERE 1=0;

-- 步骤2: 删除测试指标
DELETE FROM indicator
WHERE task_id IN (92062, 92063, 92064, 92092, 92093, 92094);

-- 步骤3: 删除测试任务
DELETE FROM sys_task
WHERE task_id IN (92062, 92063, 92064, 92092, 92093, 92094);

-- 步骤4: 验证清理结果
SELECT '=== 清理后数据统计 ===' as status;
SELECT '任务总数' as type, COUNT(*) as count FROM sys_task WHERE is_deleted = false;
SELECT '指标总数' as type, COUNT(*) as count FROM indicator WHERE is_deleted = false;
SELECT '里程碑总数' as type, COUNT(*) as count FROM indicator_milestone;
