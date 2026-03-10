-- ============================================================================
-- V19: 清理无用的数据库表 (Cleanup Unused Tables)
-- ============================================================================
-- 执行时间: 2026-03-08
-- 对应问题: 数据库中有7张完全未使用的表需要清理
--
-- 清理内容:
--   1. 2_warn_event               - 旧版预警事件表（已被新系统替代）
--   2. 2_warn_summary_daily       - 旧版预警日汇总表（已被新系统替代）
--   3. approval_action_record     - 未实现的审批操作记录表
--   4. approval_steps             - 未实现的审批步骤表
--   5. audit_action_log           - 未使用的审计操作日志表
--   6. warn_rule                  - 旧的预警规则表（功能已废弃）
--   7. common_log                 - 未使用的通用日志表
--
-- 验证:
--   - 所有表在代码中均无对应的Entity类
--   - 所有表在代码中均无Repository
--   - 所有表在代码中均无Service使用
--   - 所有表数据量均为0
--
-- 保留:
--   - sys_org_hierarchy (28条数据) - 审批流程核心表，必须保留
--
-- 执行后表数量: 41 → 34 (减少7张，-17.1%)
-- ============================================================================

-- 1. 删除旧版预警系统表
DROP TABLE IF EXISTS "2_warn_event" CASCADE;
DROP TABLE IF EXISTS "2_warn_summary_daily" CASCADE;

-- 2. 删除未实现的审批功能表
DROP TABLE IF EXISTS approval_action_record CASCADE;
DROP TABLE IF EXISTS approval_steps CASCADE;
DROP TABLE IF EXISTS audit_action_log CASCADE;

-- 3. 删除旧的预警规则表
DROP TABLE IF EXISTS warn_rule CASCADE;

-- 4. 删除未使用的通用日志表
DROP TABLE IF EXISTS common_log CASCADE;
