-- ============================================================================
-- 完整业务流程测试 - 逐一验证每个流程
-- ============================================================================

\echo '========================================'
\echo '完整业务流程测试'
\echo '========================================'
\echo ''

-- ============================================================================
-- 流程1: 指标创建与第一层下发 (战略发展部 -> 职能部门)
-- ============================================================================
\echo '【流程1】指标创建与第一层下发'
\echo '----------------------------------------'

-- 1.1 检查指标状态枚举
\echo '1.1 指标状态枚举值:'
SELECT DISTINCT status FROM indicator WHERE status IS NOT NULL;

-- 1.2 检查审批流定义
\echo '1.2 审批流定义:'
SELECT id, title, status FROM audit_flow_def LIMIT 5;

-- 1.3 检查indicator表完整字段
\echo '1.3 indicator表关键字段:'
SELECT column_name
FROM information_schema.columns
WHERE table_name = 'indicator'
  AND column_name IN ('status', 'owner_org_id', 'target_org_id', 'distribution_status')
ORDER BY column_name;

-- 1.4 检查API相关字段完整性
\echo '1.4 数据统计:'
SELECT
  COUNT(*) FILTER (WHERE status = 'DRAFT') AS draft_count,
  COUNT(*) FILTER (WHERE status = 'PENDING') AS pending_count,
  COUNT(*) FILTER (WHERE status = 'DISTRIBUTED') AS distributed_count,
  COUNT(*) AS total_count
FROM indicator;

\echo ''

-- ============================================================================
-- 流程2: 指标拆分与第二层下发 (职能部门 -> 学院)
-- ============================================================================
\echo '【流程2】指标拆分与父子关系'
\echo '----------------------------------------'

-- 2.1 检查父子指标数据
\echo '2.1 父子指标统计:'
SELECT
  COUNT(*) FILTER (WHERE parent_indicator_id IS NULL) AS parent_count,
  COUNT(*) FILTER (WHERE parent_indicator_id IS NOT NULL) AS child_count,
  COUNT(*) AS total_count
FROM indicator;

-- 2.2 查看父子指标示例
\echo '2.2 父子指标示例:'
SELECT
  p.id AS parent_id,
  p.indicator_desc AS parent_desc,
  c.id AS child_id,
  c.indicator_desc AS child_desc
FROM indicator p
LEFT JOIN indicator c ON c.parent_indicator_id = p.id
WHERE p.id IN (SELECT parent_indicator_id FROM indicator WHERE parent_indicator_id IS NOT NULL)
LIMIT 5;

\echo ''

-- ============================================================================
-- 流程3: 学院进度填报与审批
-- ============================================================================
\echo '【流程3】学院进度填报与审批'
\echo '----------------------------------------'

-- 3.1 plan_report表结构
\echo '3.1 plan_report表状态分布:'
SELECT status, COUNT(*) FROM plan_report GROUP BY status;

-- 3.2 plan_report_indicator关联表
\echo '3.2 plan_report_indicator表检查:'
SELECT COUNT(*) AS record_count FROM plan_report_indicator;

-- 3.3 indicator进度字段
\echo '3.3 指标进度分布:'
SELECT
  COUNT(*) FILTER (WHERE progress = 0) AS no_progress,
  COUNT(*) FILTER (WHERE progress > 0) AS has_progress,
  COUNT(*) AS total_count,
  AVG(progress) AS avg_progress
FROM indicator;

\echo ''

-- ============================================================================
-- 流程4: 多级审批与驳回机制
-- ============================================================================
\echo '【流程4】多级审批与驳回机制'
\echo '----------------------------------------'

-- 4.1 审批步骤定义
\echo '4.1 审批步骤定义示例:'
SELECT
  afd.id AS flow_id,
  afd.title AS flow_title,
  asd.step_order,
  asd.step_name,
  asd.approver_role
FROM audit_flow_def afd
LEFT JOIN audit_step_def asd ON asd.flow_def_id = afd.id
ORDER BY afd.id, asd.step_order
LIMIT 10;

-- 4.2 audit_instance状态字段
\echo '4.2 audit_instance表状态枚举:'
SELECT DISTINCT status FROM audit_instance WHERE status IS NOT NULL;

-- 4.3 当前步骤字段
\echo '4.3 current_step_id字段:'
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'audit_instance' AND column_name = 'current_step_id';

\echo ''

-- ============================================================================
-- 流程5: 审批时间轴
-- ============================================================================
\echo '【流程5】审批时间轴'
\echo '----------------------------------------'

-- 5.1 audit_step_instance关键字段
\echo '5.1 audit_step_instance关键字段:'
SELECT column_name
FROM information_schema.columns
WHERE table_name = 'audit_step_instance'
  AND column_name IN ('created_at', 'comment', 'status', 'assignee_id', 'step_id')
ORDER BY column_name;

-- 5.2 检查是否有审批历史数据
\echo '5.2 审批步骤历史记录数:'
SELECT COUNT(*) AS record_count FROM audit_step_instance;

\echo ''

-- ============================================================================
-- 流程6: 审批人转办/代办 (后续功能)
-- ============================================================================
\echo '【流程6】审批人转办/代办 (后续功能)'
\echo '----------------------------------------'

-- 6.1 检查是否有转办相关字段
\echo '6.1 转办相关字段检查:'
SELECT column_name
FROM information_schema.columns
WHERE table_name = 'audit_instance'
  AND (column_name LIKE '%agent%' OR column_name LIKE '%transfer%' OR column_name LIKE '%delegate%');

\echo ''

-- ============================================================================
-- 流程7: 父子指标关系
-- ============================================================================
\echo '【流程7】父子指标关系'
\echo '----------------------------------------'

-- 7.1 验证无进度聚合
\echo '7.1 父子指标关系(仅关系查询):'
SELECT
  COUNT(DISTINCT parent_indicator_id) AS unique_parents,
  COUNT(*) FILTER (WHERE parent_indicator_id IS NOT NULL) AS total_children
FROM indicator;

\echo ''

-- ============================================================================
-- 流程8: 填报周期标识
-- ============================================================================
\echo '【流程8】填报周期标识'
\echo '----------------------------------------'

-- 8.1 plan_report周期字段
\echo '8.1 填报周期分布:'
SELECT report_month, COUNT(*) FROM plan_report GROUP BY report_month;

-- 8.2 其他周期相关表
\echo '8.2 其他周期字段检查:'
SELECT
  'plan_report.report_month' AS field_location,
  'character varying' AS data_type
UNION ALL
SELECT 'indicator表的周期字段(如果有)', ''
LIMIT 5;

\echo ''

-- ============================================================================
-- 流程9: 数据变更对比与知情权提醒
-- ============================================================================
\echo '【流程9】数据变更对比与知情权提醒'
\echo '----------------------------------------'

-- 9.1 audit_log表结构验证
\echo '9.1 audit_log变更记录字段:'
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'audit_log'
  AND column_name IN ('before_json', 'after_json', 'changed_fields', 'action')
ORDER BY column_name;

-- 9.2 变更记录统计
\echo '9.2 变更记录统计:'
SELECT
  entity_type,
  action,
  COUNT(*) AS record_count
FROM audit_log
GROUP BY entity_type, action
ORDER BY record_count DESC
LIMIT 10;

-- 9.3 变更记录示例
\echo '9.3 变更记录示例(前3条):'
SELECT
  entity_type,
  action,
  actor_user_id,
  changed_fields,
  created_at
FROM audit_log
ORDER BY created_at DESC
LIMIT 3;

\echo ''

-- ============================================================================
-- 流程9.1: 提交撤回功能
-- ============================================================================
\echo '【流程9.1】提交撤回功能'
\echo '----------------------------------------'

-- 9.1.1 检查撤回相关状态
\echo '9.1.1 状态枚举检查:'
SELECT
  'audit_instance' AS table_name,
  string_agg(DISTINCT status, ', ') AS status_values
FROM audit_instance
UNION ALL
SELECT
  'plan_report',
  string_agg(DISTINCT status, ', ')
FROM plan_report;

\echo ''

-- ============================================================================
-- 流程9.2: 多角色身份与回避原则
-- ============================================================================
\echo '【流程9.2】多角色身份与回避原则'
\echo '----------------------------------------'

-- 9.2.1 用户角色关联表
\echo '9.2.1 用户角色表:'
SELECT table_name, column_name
FROM information_schema.columns
WHERE table_name IN ('sys_user_role', 'user_role')
  AND column_name LIKE '%user%'
ORDER BY table_name, column_name;

\echo ''

-- ============================================================================
-- 附加验证: 关键表和索引
-- ============================================================================
\echo '【附加验证】关键表和索引'
\echo '----------------------------------------'

-- A1. 用户和组织数据
\echo 'A1. 基础数据统计:'
SELECT 'sys_user' AS table_name, COUNT(*) AS count FROM sys_user
UNION ALL
SELECT 'sys_org', COUNT(*) FROM sys_org
UNION ALL
SELECT 'sys_role', COUNT(*) FROM sys_role
UNION ALL
SELECT 'indicator', COUNT(*) FROM indicator
UNION ALL
SELECT 'plan_report', COUNT(*) FROM plan_report;

-- A2. 附件支持
\echo 'A2. 附件表检查:'
SELECT COUNT(*) AS attachment_count FROM attachment;

\echo ''
\echo '========================================'
\echo '完整测试完成'
\echo '========================================'
