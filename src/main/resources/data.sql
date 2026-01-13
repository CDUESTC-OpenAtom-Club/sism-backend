-- ============================================
-- SISM Spring Boot Initial Data (Optional)
-- 此文件为可选配置，仅在需要 Spring Boot 自动执行时使用
-- 推荐使用 strategic-task-management/database/seed-data.sql
-- ============================================

-- 注意: 此文件仅包含基础数据的简化版本
-- 完整的示例数据位于: ../../../strategic-task-management/database/seed-data.sql

-- 基础组织数据（最小化版本）
-- 仅在数据库为空时插入
INSERT INTO org (org_name, org_type, parent_org_id, sort_order) 
SELECT '战略发展部', 'STRATEGY_DEPT', NULL, 1
WHERE NOT EXISTS (SELECT 1 FROM org WHERE org_name = '战略发展部');

INSERT INTO org (org_name, org_type, parent_org_id, sort_order) 
SELECT '教务处', 'FUNCTION_DEPT', NULL, 2
WHERE NOT EXISTS (SELECT 1 FROM org WHERE org_name = '教务处');

INSERT INTO org (org_name, org_type, parent_org_id, sort_order) 
SELECT '计算机学院', 'COLLEGE', NULL, 10
WHERE NOT EXISTS (SELECT 1 FROM org WHERE org_name = '计算机学院');

-- 基础用户数据（密码: 123456）
INSERT INTO app_user (username, real_name, org_id, password_hash) 
SELECT 'admin', '系统管理员', 1, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi'
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'admin');

-- 基础考核周期
INSERT INTO assessment_cycle (cycle_name, year, start_date, end_date, description) 
SELECT '2025年度考核周期', 2025, '2025-01-01', '2025-12-31', '2025年度战略指标考核周期'
WHERE NOT EXISTS (SELECT 1 FROM assessment_cycle WHERE year = 2025);

-- 基础预警规则
INSERT INTO alert_rule (cycle_id, name, severity, gap_threshold, is_enabled) 
SELECT 1, '轻度预警', 'INFO', 10.00, TRUE
WHERE EXISTS (SELECT 1 FROM assessment_cycle WHERE cycle_id = 1)
  AND NOT EXISTS (SELECT 1 FROM alert_rule WHERE name = '轻度预警' AND cycle_id = 1);

INSERT INTO alert_rule (cycle_id, name, severity, gap_threshold, is_enabled) 
SELECT 1, '中度预警', 'WARNING', 20.00, TRUE
WHERE EXISTS (SELECT 1 FROM assessment_cycle WHERE cycle_id = 1)
  AND NOT EXISTS (SELECT 1 FROM alert_rule WHERE name = '中度预警' AND cycle_id = 1);

INSERT INTO alert_rule (cycle_id, name, severity, gap_threshold, is_enabled) 
SELECT 1, '严重预警', 'CRITICAL', 30.00, TRUE
WHERE EXISTS (SELECT 1 FROM assessment_cycle WHERE cycle_id = 1)
  AND NOT EXISTS (SELECT 1 FROM alert_rule WHERE name = '严重预警' AND cycle_id = 1);

-- 基础预警窗口
INSERT INTO alert_window (cycle_id, name, cutoff_date, is_default) 
SELECT 1, 'Q1季度检查', '2025-03-31', FALSE
WHERE EXISTS (SELECT 1 FROM assessment_cycle WHERE cycle_id = 1)
  AND NOT EXISTS (SELECT 1 FROM alert_window WHERE name = 'Q1季度检查' AND cycle_id = 1);

INSERT INTO alert_window (cycle_id, name, cutoff_date, is_default) 
SELECT 1, '年度终检', '2025-12-31', TRUE
WHERE EXISTS (SELECT 1 FROM assessment_cycle WHERE cycle_id = 1)
  AND NOT EXISTS (SELECT 1 FROM alert_window WHERE name = '年度终检' AND cycle_id = 1);

-- 注意: 
-- 1. 所有 INSERT 语句都使用 WHERE NOT EXISTS 避免重复插入
-- 2. 这只是最小化的基础数据
-- 3. 完整的示例数据请使用 strategic-task-management/database/seed-data.sql
