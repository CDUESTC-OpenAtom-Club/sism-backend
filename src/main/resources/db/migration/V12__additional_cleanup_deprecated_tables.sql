-- ============================================================================
-- V7: 清理冗余废弃表 (Cleanup Deprecated Tables)
-- ============================================================================
--
-- 执行时间: 2026-02-27
-- 对应文档: database-tables-index.md V2.0 节
--
-- 清理内容:
--   1. strategic_task        - 数据已全部迁移至 sys_task（0条），废弃删除
--   2. strategic_task_backup - 迁移完成后的备份表（0条），废弃删除
--   3. milestone             - 功能已由 indicator_milestone 替代（0条），废弃删除
--   4. assessment_cycle      - 数据与 cycle 表完全冗余，废弃删除
--
-- 执行后活跃表总数: 34（与 database-tables-index.md 规范一致）
-- ============================================================================

-- 1. 删除 strategic_task（数据已在 sys_task，0条数据）
DROP TABLE IF EXISTS strategic_task;

-- 2. 删除 strategic_task_backup（迁移备份，0条数据，使命完成）
DROP TABLE IF EXISTS strategic_task_backup;

-- 3. 删除 milestone（功能已由 indicator_milestone 替代，0条数据）
DROP TABLE IF EXISTS milestone;

-- 4. 删除 assessment_cycle（数据与 cycle 表完全冗余）
-- CASCADE 用于删除 adhoc_task/alert_rule/alert_window 上残留的外键约束引用
DROP TABLE IF EXISTS assessment_cycle CASCADE;
