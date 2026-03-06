-- =============================================================================
-- V14: 添加性能优化索引
-- =============================================================================
-- 目的：优化查询性能，特别是按年份和状态查询指标的场景
-- 创建时间：2026-03-06
-- =============================================================================

-- 为 indicator 表添加复合索引（year + status）
-- 这将显著提升 findByYearAndStatus 查询的性能
CREATE INDEX IF NOT EXISTS idx_indicator_year_status 
ON indicator(year, status);

-- 为 indicator 表添加单独的 year 索引
-- 用于只按年份查询的场景
CREATE INDEX IF NOT EXISTS idx_indicator_year 
ON indicator(year);

-- 为 indicator 表添加 task_id 索引
-- 优化按任务查询指标的性能
CREATE INDEX IF NOT EXISTS idx_indicator_task_id 
ON indicator(task_id);

-- 为 indicator_milestone 表添加 indicator_id 索引
-- 优化批量加载里程碑的性能
CREATE INDEX IF NOT EXISTS idx_milestone_indicator_id 
ON indicator_milestone(indicator_id);

-- 添加注释说明索引用途
COMMENT ON INDEX idx_indicator_year_status IS '优化按年份和状态查询指标的性能';
COMMENT ON INDEX idx_indicator_year IS '优化按年份查询指标的性能';
COMMENT ON INDEX idx_indicator_task_id IS '优化按任务查询指标的性能';
COMMENT ON INDEX idx_milestone_indicator_id IS '优化批量加载里程碑的性能';
