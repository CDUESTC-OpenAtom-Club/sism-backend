-- =====================================================
-- 指标表状态字段优化脚本
-- 版本: V34
-- 目的: 合并状态字段，删除冗余字段
-- =====================================================

-- 1. 合并 distribution_status 到 status 字段
-- 删除 distribution_status 字段
ALTER TABLE public.indicator DROP COLUMN distribution_status;

-- 为 status 字段添加检查约束，只允许有效状态值
ALTER TABLE public.indicator
DROP CONSTRAINT IF EXISTS indicator_status_check;

ALTER TABLE public.indicator
ADD CONSTRAINT indicator_status_check
CHECK (status IN ('DRAFT', 'PENDING', 'DISTRIBUTED', 'APPROVED', 'ARCHIVED'));

COMMENT ON COLUMN public.indicator.status IS '指标状态: DRAFT=草稿, PENDING=待审批, DISTRIBUTED=已下发, APPROVED=已通过, ARCHIVED=已归档';

-- 2. 删除 year 字段（冗余，来源于任务/计划）
ALTER TABLE public.indicator DROP COLUMN year;

-- 3. 为 level 字段添加更清晰的注释和约束
COMMENT ON COLUMN public.indicator.level IS '指标层级关系: PRIMARY=主指标, STRAT_TO_FUNC=战略到职能, FUNC_TO_COLLEGE=职能到学院';

-- 确保 level 字段只有有效值
UPDATE public.indicator
SET level = CASE
    WHEN level IN ('PRIMARY', 'STRAT_TO_FUNC', 'FUNC_TO_COLLEGE') THEN level
    ELSE 'PRIMARY'
END;

-- 为 level 字段添加检查约束
ALTER TABLE public.indicator
DROP CONSTRAINT IF EXISTS indicator_level_check;

ALTER TABLE public.indicator
ADD CONSTRAINT indicator_level_check
CHECK (level IN ('PRIMARY', 'STRAT_TO_FUNC', 'FUNC_TO_COLLEGE'));
