-- ============================================================================
-- 业务流程验证测试 SQL
-- 验证流程.md中描述的9个核心业务流程是否已实现
-- ============================================================================

\echo '========================================='
\echo '业务流程数据库结构验证测试'
\echo '========================================='
\echo ''

-- 流程1: 指标创建与第一层下发
\echo '【流程1】指标创建与第一层下发'
SELECT
  CASE
    WHEN EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_name='indicator' AND column_name='status'
    ) THEN '✓ indicator.status字段存在'
    ELSE '✗ indicator.status字段缺失'
  END AS test_result;

SELECT
  CASE
    WHEN COUNT(*) > 0 THEN '✓ audit_flow_def表有数据'
    ELSE '✗ audit_flow_def表无数据'
  END AS test_result
FROM audit_flow_def;

-- 流程2: 指标拆分与父子关系
\echo ''
\echo '【流程2】指标拆分与父子关系'
SELECT
  CASE
    WHEN EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_name='indicator' AND column_name='parent_indicator_id'
    ) THEN '✓ indicator.parent_indicator_id字段存在'
    ELSE '✗ parent_indicator_id字段缺失'
  END AS test_result;

-- 流程3: 学院进度填报
\echo ''
\echo '【流程3】学院进度填报与审批'
SELECT
  CASE
    WHEN EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_name='plan_report' AND column_name='status'
    ) THEN '✓ plan_report.status字段存在'
    ELSE '✗ plan_report.status字段缺失'
  END AS test_result;

SELECT
  CASE
    WHEN EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_name='indicator' AND column_name='progress'
    ) THEN '✓ indicator.progress字段存在'
    ELSE '✗ indicator.progress字段缺失'
  END AS test_result;

-- 流程4: 多级审批机制
\echo ''
\echo '【流程4】多级审批与驳回机制'
SELECT
  CASE
    WHEN COUNT(*) > 0 THEN '✓ audit_step_def表有数据'
    ELSE '✗ audit_step_def表无数据'
  END AS test_result
FROM audit_step_def;

SELECT
  CASE
    WHEN EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_name='audit_instance' AND column_name='current_step_id'
    ) THEN '✓ audit_instance.current_step_id字段存在'
    ELSE '✗ current_step_id字段缺失'
  END AS test_result;

-- 流程5: 审批时间轴
\echo ''
\echo '【流程5】审批时间轴'
SELECT
  CASE
    WHEN COUNT(*) = 3 THEN '✓ audit_step_instance包含时间轴必要字段'
    ELSE '✗ audit_step_instance缺少字段'
  END AS test_result
FROM information_schema.columns
WHERE table_name='audit_step_instance'
  AND column_name IN ('created_at', 'comment', 'status');

-- 流程8: 填报周期标识
\echo ''
\echo '【流程8】填报周期标识'
SELECT
  CASE
    WHEN EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_name='plan_report' AND column_name='report_month'
    ) THEN '✓ plan_report.report_month字段存在'
    ELSE '✗ report_month字段缺失'
  END AS test_result;

-- 关键表统计
\echo ''
\echo '【数据统计】关键表记录数'
SELECT 'indicator' AS table_name, COUNT(*) AS record_count FROM indicator
UNION ALL
SELECT 'audit_flow_def', COUNT(*) FROM audit_flow_def
UNION ALL
SELECT 'audit_instance', COUNT(*) FROM audit_instance
UNION ALL
SELECT 'plan_report', COUNT(*) FROM plan_report
UNION ALL
SELECT 'sys_user', COUNT(*) FROM sys_user
UNION ALL
SELECT 'sys_org', COUNT(*) FROM sys_org;

\echo ''
\echo '========================================='
\echo '测试完成'
\echo '========================================='
