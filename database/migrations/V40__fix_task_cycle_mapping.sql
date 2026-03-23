-- ============================================================
-- V40__fix_task_cycle_mapping.sql
-- 修复任务周期映射问题
--
-- 问题：任务被错误地关联到示例数据周期(id=90)，而前端查询的是正式周期(id=4)
-- 导致前端无法获取任务类型，显示为"其他"
--
-- 修复：将 cycle_id = 90 的任务更新为 cycle_id = 4 (2026年度正式周期)
-- ============================================================

-- 步骤1: 查看当前状态（仅查询不更新）
SELECT '=== 修复前 ===' as status;
SELECT 'cycle_id=90的任务' as info, COUNT(*) as count FROM sys_task WHERE cycle_id = 90 AND is_deleted = false;
SELECT 'cycle_id=4的任务' as info, COUNT(*) as count FROM sys_task WHERE cycle_id = 4 AND is_deleted = false;

-- 步骤2: 更新任务的周期ID
UPDATE sys_task
SET cycle_id = 4
WHERE cycle_id = 90 AND is_deleted = false;

-- 步骤3: 验证修复结果
SELECT '=== 修复后 ===' as status;
SELECT 'cycle_id=90的任务' as info, COUNT(*) as count FROM sys_task WHERE cycle_id = 90 AND is_deleted = false;
SELECT 'cycle_id=4的任务' as info, COUNT(*) as count FROM sys_task WHERE cycle_id = 4 AND is_deleted = false;

-- 显示修复后的任务列表
SELECT task_id, task_name, task_type, cycle_id
FROM sys_task
WHERE is_deleted = false
ORDER BY cycle_id, task_id;
