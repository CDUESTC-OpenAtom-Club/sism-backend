-- 清理已生成的不完整 2025 年数据

BEGIN;

-- 删除 2025 年的里程碑
DELETE FROM indicator_milestone
WHERE indicator_id IN (
    SELECT id FROM indicator WHERE year = 2025
);

-- 删除 2025 年的指标
DELETE FROM indicator
WHERE year = 2025;

-- 删除 2025 年的任务
DELETE FROM sys_task
WHERE cycle_id = 7;

COMMIT;

-- 验证清理结果
SELECT '清理后的统计' AS 说明;
SELECT 
    '2025年任务数' AS 统计项,
    COUNT(*) AS 数量
FROM sys_task
WHERE cycle_id = 7
UNION ALL
SELECT 
    '2025年指标数' AS 统计项,
    COUNT(*) AS 数量
FROM indicator
WHERE year = 2025
UNION ALL
SELECT 
    '2025年里程碑数' AS 统计项,
    COUNT(*) AS 数量
FROM indicator_milestone m
JOIN indicator i ON m.indicator_id = i.id
WHERE i.year = 2025;
