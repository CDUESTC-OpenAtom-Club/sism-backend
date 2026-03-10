-- ============================================================================
-- V21: 迁移 PENDING 数据到 PENDING_REVIEW 并添加索引
-- (Migrate PENDING data to PENDING_REVIEW and add index)
-- ============================================================================
-- 执行时间: 2026-03-09
-- 依赖: V20 (必须先添加枚举值)
--
-- 说明:
--   V20 添加了枚举值，本迁移在枚举值提交后使用它们。
--   PostgreSQL 要求枚举值在独立事务中提交后才能使用。
--
-- 变更内容:
--   1. 迁移现有 PENDING 记录到 PENDING_REVIEW
--   2. 添加 PENDING_REVIEW 状态的索引以优化查询性能
-- ============================================================================

-- Step 1: Migrate existing PENDING records to PENDING_REVIEW
-- This ensures existing indicators awaiting definition review are correctly labeled
UPDATE public.indicator 
SET status = 'PENDING_REVIEW'::public.indicator_status 
WHERE status = 'PENDING'::public.indicator_status;

-- Step 2: Add index for PENDING_REVIEW queries to optimize performance
-- This index helps when filtering indicators awaiting review
CREATE INDEX IF NOT EXISTS idx_indicator_status_pending_review 
ON public.indicator(status) 
WHERE status = 'PENDING_REVIEW'::public.indicator_status;

-- Verification queries (commented out, for manual testing)
-- SELECT enumlabel FROM pg_enum WHERE enumtypid = (SELECT oid FROM pg_type WHERE typname = 'indicator_status') ORDER BY enumsortorder;
-- SELECT status, COUNT(*) FROM public.indicator GROUP BY status;
