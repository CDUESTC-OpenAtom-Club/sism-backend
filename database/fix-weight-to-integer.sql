-- ============================================
-- 修复权重字段为正整数
-- 日期: 2026-01-19
-- 问题: weight_percent 字段包含小数值，需要转换为正整数
-- ============================================

-- 1. 将 indicator 表中的 weight_percent 四舍五入为正整数
UPDATE indicator
SET weight_percent = ROUND(weight_percent)
WHERE year = 2026 AND weight_percent != ROUND(weight_percent);

-- 2. 将 milestone 表中的 weight_percent 四舍五入为正整数
UPDATE milestone
SET weight_percent = ROUND(weight_percent)
WHERE weight_percent != ROUND(weight_percent);

-- 3. 验证修复结果
SELECT 
    '修复后权重非整数的指标数' as metric,
    COUNT(*) as count
FROM indicator
WHERE year = 2026 AND weight_percent != FLOOR(weight_percent);

SELECT 
    '修复后权重非整数的里程碑数' as metric,
    COUNT(*) as count
FROM milestone
WHERE weight_percent != FLOOR(weight_percent);

-- 4. 显示修复后的权重分布
SELECT 
    weight_percent as weight,
    COUNT(*) as indicator_count
FROM indicator
WHERE year = 2026
GROUP BY weight_percent
ORDER BY weight_percent;

-- ============================================
-- 完成
-- ============================================
