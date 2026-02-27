-- ============================================================================
-- V8: indicator 表新增 distribution_status 列
-- ============================================================================
--
-- 执行时间: 2026-02-27
-- 对应文档: API接口文档.md 第 5.A 节（P0 必须完成）
--
-- 背景:
--   前端已完成 distributionStatus 字段的完整实现（DRAFT/DISTRIBUTED/PENDING/APPROVED/REJECTED）
--   后端需要在 indicator 表添加对应存储列
--
-- 迁移策略:
--   已有数据默认设为 DISTRIBUTED（符合历史数据语义：已下发状态）
--   新建草稿指标由后端应用层在 POST /indicators 时显式设为 DRAFT
-- ============================================================================

ALTER TABLE indicator
    ADD COLUMN IF NOT EXISTS distribution_status VARCHAR(20) NOT NULL DEFAULT 'DISTRIBUTED';

COMMENT ON COLUMN indicator.distribution_status IS
    '指标下发状态: DRAFT=草稿未下发, DISTRIBUTED=已下发, PENDING=待审批, APPROVED=已通过, REJECTED=已驳回';
