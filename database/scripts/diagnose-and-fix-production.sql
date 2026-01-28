-- 生产环境诊断和修复脚本
-- 日期: 2026-01-28
-- 用途: 诊断并修复生产环境发现的问题

-- ============================================
-- 第一部分: 诊断检查
-- ============================================

\echo '========================================='
\echo '生产环境诊断检查'
\echo '========================================='
\echo ''

-- 1. 检查用户表
\echo '1. 用户表检查'
\echo '-----------------------------------------'
SELECT 
    user_id, 
    username, 
    real_name, 
    org_id, 
    is_active,
    LEFT(password_hash, 30) || '...' AS password_preview
FROM app_user
WHERE username IN ('zhanlue', 'jiaowu', 'keyan', 'admin', 'renshi', 'xuesheng')
ORDER BY username;
\echo ''

-- 2. 检查组织表
\echo '2. 组织表检查'
\echo '-----------------------------------------'
SELECT org_id, org_name, org_type, parent_org_id, sort_order
FROM org
WHERE org_id <= 10 OR org_name IN ('战略发展部', '教务处', '科研处', '人事处', '学生处')
ORDER BY org_id;
\echo ''

-- 3. 检查考核周期表
\echo '3. 考核周期表检查'
\echo '-----------------------------------------'
SELECT cycle_id, cycle_name, year, start_date, end_date
FROM assessment_cycle
ORDER BY year DESC;
\echo ''

-- 4. 检查 Flyway 迁移历史
\echo '4. Flyway 迁移历史'
\echo '-----------------------------------------'
SELECT 
    installed_rank, 
    version, 
    description, 
    type, 
    script, 
    installed_on, 
    execution_time, 
    success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 10;
\echo ''

-- 5. 检查表统计
\echo '5. 表统计信息'
\echo '-----------------------------------------'
SELECT 'app_user' AS table_name, COUNT(*) AS row_count FROM app_user
UNION ALL
SELECT 'org', COUNT(*) FROM org
UNION ALL
SELECT 'assessment_cycle', COUNT(*) FROM assessment_cycle
UNION ALL
SELECT 'indicators', COUNT(*) FROM indicators
UNION ALL
SELECT 'strategic_task', COUNT(*) FROM strategic_task
UNION ALL
SELECT 'milestones', COUNT(*) FROM milestones
UNION ALL
SELECT 'progress_report', COUNT(*) FROM progress_report;
\echo ''

-- ============================================
-- 第二部分: 问题修复
-- ============================================

\echo '========================================='
\echo '开始修复问题'
\echo '========================================='
\echo ''

-- 修复 1: 确保所有测试用户存在且密码正确
\echo '修复 1: 更新测试用户'
\echo '-----------------------------------------'

-- 确保战略发展部存在
INSERT INTO org (org_name, org_type, parent_org_id, sort_order)
VALUES ('战略发展部', 'STRATEGY_DEPT', NULL, 1)
ON CONFLICT (org_name) DO NOTHING;

-- 确保职能部门存在
INSERT INTO org (org_name, org_type, parent_org_id, sort_order)
VALUES 
    ('教务处', 'FUNCTION_DEPT', NULL, 2),
    ('科研处', 'FUNCTION_DEPT', NULL, 3),
    ('人事处', 'FUNCTION_DEPT', NULL, 4),
    ('学生处', 'FUNCTION_DEPT', NULL, 5)
ON CONFLICT (org_name) DO NOTHING;

-- 更新或插入测试用户
-- 密码统一为: 123456
-- 密码哈希: $2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi

INSERT INTO app_user (username, real_name, org_id, password_hash, is_active)
VALUES 
    ('zhanlue', '张战略', (SELECT org_id FROM org WHERE org_name = '战略发展部'), '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi', TRUE),
    ('jiaowu', '李教务', (SELECT org_id FROM org WHERE org_name = '教务处'), '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi', TRUE),
    ('keyan', '王科研', (SELECT org_id FROM org WHERE org_name = '科研处'), '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi', TRUE),
    ('renshi', '赵人事', (SELECT org_id FROM org WHERE org_name = '人事处'), '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi', TRUE),
    ('xuesheng', '钱学生', (SELECT org_id FROM org WHERE org_name = '学生处'), '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi', TRUE)
ON CONFLICT (username) DO UPDATE
SET 
    password_hash = EXCLUDED.password_hash,
    is_active = TRUE,
    real_name = EXCLUDED.real_name,
    org_id = EXCLUDED.org_id,
    updated_at = CURRENT_TIMESTAMP;

\echo '✓ 测试用户已更新'
\echo ''

-- 修复 2: 确保考核周期数据存在
\echo '修复 2: 确保考核周期数据'
\echo '-----------------------------------------'

INSERT INTO assessment_cycle (cycle_name, year, start_date, end_date, description)
VALUES 
    ('2024年度考核周期', 2024, '2024-01-01', '2024-12-31', '2024年度战略指标考核周期'),
    ('2025年度考核周期', 2025, '2025-01-01', '2025-12-31', '2025年度战略指标考核周期'),
    ('2026年度考核周期', 2026, '2026-01-01', '2026-12-31', '2026年度战略指标考核周期')
ON CONFLICT (year) DO UPDATE
SET 
    cycle_name = EXCLUDED.cycle_name,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP;

\echo '✓ 考核周期数据已更新'
\echo ''

-- ============================================
-- 第三部分: 验证修复结果
-- ============================================

\echo '========================================='
\echo '验证修复结果'
\echo '========================================='
\echo ''

-- 验证用户
\echo '验证用户数据:'
\echo '-----------------------------------------'
SELECT 
    u.username, 
    u.real_name, 
    o.org_name, 
    u.is_active,
    CASE 
        WHEN u.password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi' 
        THEN '✓ 正确' 
        ELSE '✗ 错误' 
    END AS password_status
FROM app_user u
LEFT JOIN org o ON u.org_id = o.org_id
WHERE u.username IN ('zhanlue', 'jiaowu', 'keyan', 'renshi', 'xuesheng')
ORDER BY u.username;
\echo ''

-- 验证考核周期
\echo '验证考核周期数据:'
\echo '-----------------------------------------'
SELECT cycle_id, cycle_name, year, start_date, end_date
FROM assessment_cycle
ORDER BY year DESC;
\echo ''

\echo '========================================='
\echo '修复完成！'
\echo '========================================='
\echo ''
\echo '下一步:'
\echo '1. 重启后端服务: sudo systemctl restart sism-backend'
\echo '2. 测试 keyan 用户登录'
\echo '3. 测试 /api/cycles 端点'
\echo ''
