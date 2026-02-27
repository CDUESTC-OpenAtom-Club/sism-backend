-- ============================================================================
-- V2.0: 清理冗余废弃表
-- ============================================================================
--
-- 执行时间: 2026-02-27
-- 目的: 删除历次迁移遗留的废弃表，统一表命名规范
--
-- 清理内容:
-- 1. strategic_task  - 数据已全部迁移至 sys_task（0条数据），废弃删除
-- 2. strategic_task_backup - 迁移完成后的备份表（0条数据），废弃删除
-- 3. milestone       - 功能已由 indicator_milestone 替代（0条数据），废弃删除
-- 4. assessment_cycle - 功能已由 cycle 表统一管理（数据冗余），废弃删除
--
-- 执行后:
-- 活跃表总数从 40 减少至 36（含已删除的 org_deprecated/sys_user_deprecated/task_deprecated）
-- 规范表总数: 34（核心业务表命名统一为 sys_ 前缀）
-- ============================================================================

-- 1. 删除 strategic_task（数据已在 sys_task，0条数据）
DROP TABLE IF EXISTS strategic_task;

-- 2. 删除 strategic_task_backup（迁移备份，0条数据）
DROP TABLE IF EXISTS strategic_task_backup;

-- 3. 删除 milestone（功能已由 indicator_milestone 替代，0条数据）
DROP TABLE IF EXISTS milestone;

-- 4. 删除 assessment_cycle（数据与 cycle 表完全冗余）
DROP TABLE IF EXISTS assessment_cycle;
