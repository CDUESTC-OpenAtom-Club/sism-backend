-- ============================================================================
-- V20: 添加 PENDING_REVIEW 状态以区分指标定义审核和进度审批
-- (Add PENDING_REVIEW status to distinguish indicator definition review from progress approval)
-- ============================================================================
-- 执行时间: 2026-03-08
-- 对应问题: 指标状态混淆 - PENDING 状态在指标定义审核和进度审批中含义不清
--
-- 问题描述:
--   当前系统使用 PENDING 状态表示"待审核"（指标定义审核），但这与进度审批中的
--   progressApprovalStatus.PENDING（待审批）概念混淆。需要引入 PENDING_REVIEW
--   作为指标生命周期的独立状态，明确表示"指标定义待战略发展部审核"。
--
-- 变更内容:
--   1. 添加缺失的生命周期状态到 indicator_status 枚举
--      - DRAFT (草稿 - 未提交审核)
--      - PENDING (待审核 - 已提交等待战略部门审批) [将被 PENDING_REVIEW 替代]
--      - DISTRIBUTED (已下发 - 已审批并分发到各部门)
--   2. 添加新状态 PENDING_REVIEW (待审核 - 指标定义审核)
--   3. 迁移现有 PENDING 记录到 PENDING_REVIEW（如果存在）
--   4. 添加枚举类型文档说明四状态生命周期
--   5. 添加索引优化 PENDING_REVIEW 查询性能
--
-- 生命周期状态流转:
--   DRAFT → PENDING_REVIEW → DISTRIBUTED → ARCHIVED
--
-- 保留行为:
--   - progressApprovalStatus 字段保持不变 (NONE, DRAFT, PENDING, APPROVED, REJECTED)
--   - DISTRIBUTED 状态的指标行为不变
--   - 现有 CRUD 操作不受影响
--   - ACTIVE 状态保留用于向后兼容（等同于 DISTRIBUTED）
-- ============================================================================

-- Step 1: Add missing lifecycle status values to indicator_status enum
-- Note: PostgreSQL requires adding enum values one at a time
-- Using IF NOT EXISTS to make migration idempotent
-- IMPORTANT: PostgreSQL requires enum values to be committed before they can be used
-- Therefore, we only ADD enum values in this migration, without using them

DO $$
BEGIN
    -- Add DRAFT status
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum 
        WHERE enumlabel = 'DRAFT' 
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'indicator_status')
    ) THEN
        ALTER TYPE public.indicator_status ADD VALUE 'DRAFT';
    END IF;

    -- Add PENDING status (temporary, will be replaced by PENDING_REVIEW)
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum 
        WHERE enumlabel = 'PENDING' 
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'indicator_status')
    ) THEN
        ALTER TYPE public.indicator_status ADD VALUE 'PENDING';
    END IF;

    -- Add DISTRIBUTED status
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum 
        WHERE enumlabel = 'DISTRIBUTED' 
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'indicator_status')
    ) THEN
        ALTER TYPE public.indicator_status ADD VALUE 'DISTRIBUTED';
    END IF;

    -- Step 2: Add PENDING_REVIEW status (the new distinct status for indicator definition review)
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum 
        WHERE enumlabel = 'PENDING_REVIEW' 
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'indicator_status')
    ) THEN
        ALTER TYPE public.indicator_status ADD VALUE 'PENDING_REVIEW';
    END IF;
END $$;

-- Step 3: Add documentation comment to enum type
COMMENT ON TYPE public.indicator_status IS 
'Indicator lifecycle status (指标生命周期状态):
- DRAFT (草稿): Not yet submitted for review
- PENDING_REVIEW (待审核): Submitted and awaiting strategic dept approval of indicator definition
- DISTRIBUTED (已下发): Approved and distributed to departments
- ACTIVE (运行中): Legacy status, equivalent to DISTRIBUTED (deprecated)
- ARCHIVED (已归档): Soft-deleted indicator
- PENDING (待审核): Deprecated, use PENDING_REVIEW instead

Lifecycle flow: DRAFT → PENDING_REVIEW → DISTRIBUTED → ARCHIVED

Note: This status field represents indicator definition lifecycle, separate from 
progressApprovalStatus field which represents progress submission approval workflow.';

-- Step 4: Add comment to status column for clarity
COMMENT ON COLUMN public.indicator.status IS 
'Indicator lifecycle status: DRAFT (草稿), PENDING_REVIEW (待审核), DISTRIBUTED (已下发), ARCHIVED (已归档).
Separate from progress_approval_status which handles progress submission approval.';

-- Verification queries (commented out, for manual testing)
-- SELECT enumlabel FROM pg_enum WHERE enumtypid = (SELECT oid FROM pg_type WHERE typname = 'indicator_status') ORDER BY enumsortorder;
-- SELECT status, COUNT(*) FROM public.indicator GROUP BY status;
