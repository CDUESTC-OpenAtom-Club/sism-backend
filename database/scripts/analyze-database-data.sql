-- ============================================
-- SISM 数据库统计和诊断脚本
-- 用途: 统计所有表的数据量，发现数据质量问题
-- 版本: V1.0
-- ============================================

\echo '============================================'
\echo 'SISM 数据库统计报告'
\echo '============================================'
\echo ''

-- ============================================
-- 1. 表数量和记录数统计
-- ============================================
\echo '>>> 1. 表记录数量统计'
\echo ''

DO $$
DECLARE
    table_record RECORD;
    total_records BIGINT := 0;
    table_count INT := 0;
BEGIN
    -- 创建临时表存储结果
    DROP TABLE IF EXISTS temp_table_stats;
    CREATE TEMP TABLE temp_table_stats (
        table_name TEXT,
        record_count BIGINT,
        comment TEXT
    );

    -- 统计各个表的记录数
    FOR table_record IN
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_type = 'BASE TABLE'
        ORDER BY table_name
    LOOP
        EXECUTE format('
            INSERT INTO temp_table_stats (table_name, record_count, comment)
            SELECT ''%s'', COUNT(*), ''%s''
            FROM %s
        ', table_record.table_name, table_record.table_name, table_record.table_name);
    END LOOP;

    -- 输出统计结果
    FOR table_record IN SELECT * FROM temp_table_stats ORDER BY record_count DESC LOOP
        RAISE NOTICE '[%-30s] %,6d 条记录', table_record.table_name, table_record.record_count;
        total_records := total_records + table_record.record_count;
        table_count := table_count + 1;
    END LOOP;

    RAISE NOTICE '';
    RAISE NOTICE '总计: % 个表, %,d 条记录', table_count, total_records;

    -- 检查空表
    RAISE NOTICE '';
    FOR table_record IN SELECT * FROM temp_table_stats WHERE record_count = 0 LOOP
        RAISE NOTICE '[WARNING] 表 %s 为空', table_record.table_name;
    END LOOP;

    DROP TABLE temp_table_stats;
END $$;

\echo ''

-- ============================================
-- 2. 核心表详细统计
-- ============================================
\echo '>>> 2. 核心表详细统计'
\echo ''

-- 2.1 sys_org 表
DO $$
DECLARE
    v_count INT;
    v_root_count INT;
    v_dept_count INT;
BEGIN
    SELECT COUNT(*) INTO v_count FROM sys_org;
    SELECT COUNT(*) INTO v_root_count FROM sys_org WHERE parent_id IS NULL;

    RAISE NOTICE '[sys_org] 总计: % 条', v_count;
    RAISE NOTICE '  - 根节点: % 条', v_root_count;
    RAISE NOTICE '  - 子节点: % 条', v_count - v_root_count;

    IF v_count = 0 THEN
        RAISE NOTICE '[ERROR] sys_org 表为空，系统无法运行！';
    END IF;
END $$;

-- 2.2 sys_user 表
DO $$
DECLARE
    v_count INT;
    v_active INT;
BEGIN
    SELECT COUNT(*) INTO v_count FROM sys_user;
    SELECT COUNT(*) INTO v_active FROM sys_user WHERE is_active = true;

    RAISE NOTICE '[sys_user] 总计: % 条 (活跃: %)', v_count, v_active;

    IF v_count = 0 THEN
        RAISE NOTICE '[WARNING] sys_user 表为空';
    END IF;
END $$;

-- 2.3 assessment_cycle 表
DO $$
DECLARE
    v_count INT;
BEGIN
    SELECT COUNT(*) INTO v_count FROM assessment_cycle;
    RAISE NOTICE '[assessment_cycle] 总计: % 条', v_count;

    IF v_count = 0 THEN
        RAISE NOTICE '[WARNING] assessment_cycle 表为空，无法创建任务';
    END IF;
END $$;

-- 2.4 sys_task 表
DO $$
DECLARE
    v_count INT;
BEGIN
    SELECT COUNT(*) INTO v_count FROM sys_task;
    RAISE NOTICE '[sys_task] 总计: % 条', v_count;

    IF v_count = 0 THEN
        RAISE NOTICE '[WARNING] sys_task 表为空，无法创建指标';
    END IF;
END $$;

-- 2.5 indicator 表（核心表）
DO $$
DECLARE
    v_count INT;
    v_no_task INT;
    v_no_owner INT;
    v_no_target INT;
    v_deleted INT;
BEGIN
    SELECT COUNT(*) INTO v_count FROM indicator;
    SELECT COUNT(*) INTO v_no_task FROM indicator WHERE task_id IS NULL;
    SELECT COUNT(*) INTO v_no_owner FROM indicator WHERE owner_org_id IS NULL;
    SELECT COUNT(*) INTO v_no_target FROM indicator WHERE target_org_id IS NULL;
    SELECT COUNT(*) INTO v_deleted FROM indicator WHERE is_deleted = true;

    RAISE NOTICE '[indicator] 总计: % 条', v_count;
    RAISE NOTICE '  - 缺少 task_id: % 条 [问题]', v_no_task;
    RAISE NOTICE '  - 缺少 owner_org_id: % 条 [问题]', v_no_owner;
    RAISE NOTICE '  - 缺少 target_org_id: % 条 [问题]', v_no_target;
    RAISE NOTICE '  - 已删除: % 条', v_deleted;

    IF v_count = 0 THEN
        RAISE NOTICE '[CRITICAL] indicator 表为空，系统核心功能无法使用！';
    END IF;
END $$;

-- 2.6 indicator_milestone 表
DO $$
DECLARE
    v_count INT;
    v_no_indicator INT;
BEGIN
    SELECT COUNT(*) INTO v_count FROM indicator_milestone;
    SELECT COUNT(*) INTO v_no_indicator FROM indicator_milestone WHERE indicator_id IS NULL;

    RAISE NOTICE '[indicator_milestone] 总计: % 条', v_count;
    RAISE NOTICE '  - 缺少 indicator_id: % 条 [问题]', v_no_indicator;
END $$;

-- 2.7 progress_report 表
DO $$
DECLARE
    v_count INT;
BEGIN
    SELECT COUNT(*) INTO v_count FROM progress_report;
    RAISE NOTICE '[progress_report] 总计: % 条', v_count;
END $$;

-- 2.8 adhoc_task 表
DO $$
DECLARE
    v_count INT;
BEGIN
    SELECT COUNT(*) INTO v_count FROM adhoc_task;
    RAISE NOTICE '[adhoc_task] 总计: % 条', v_count;
END $$;

-- 2.9 plan 表
DO $$
DECLARE
    v_count INT;
BEGIN
    SELECT COUNT(*) INTO v_count FROM plan;
    RAISE NOTICE '[plan] 总计: % 条', v_count;
END $$;

\echo ''

-- ============================================
-- 3. 测试数据检测
-- ============================================
\echo '>>> 3. 测试数据检测'
\echo ''

-- 检测包含 "test" 的数据
DO $$
DECLARE
    v_test_users INT;
    v_test_orgs INT;
    v_test_tasks INT;
    v_test_indicators INT;
BEGIN
    -- 检测测试用户
    SELECT COUNT(*) INTO v_test_users FROM sys_user
    WHERE username ILIKE '%test%' OR real_name ILIKE '%test%';

    -- 检测测试组织
    SELECT COUNT(*) INTO v_test_orgs FROM sys_org
    WHERE name ILIKE '%test%' OR name ILIKE '%测试%';

    -- 检测测试任务
    SELECT COUNT(*) INTO v_test_tasks FROM sys_task
    WHERE name ILIKE '%test%' OR name ILIKE '%测试%';

    -- 检测测试指标
    SELECT COUNT(*) INTO v_test_indicators FROM indicator
    WHERE indicator_desc ILIKE '%test%' OR indicator_desc ILIKE '%测试%';

    RAISE NOTICE '[检测] 测试数据统计:';
    IF v_test_users > 0 THEN
        RAISE NOTICE '  - 测试用户: % 条', v_test_users;
    END IF;
    IF v_test_orgs > 0 THEN
        RAISE NOTICE '  - 测试组织: % 条', v_test_orgs;
    END IF;
    IF v_test_tasks > 0 THEN
        RAISE NOTICE '  - 测试任务: % 条', v_test_tasks;
    END IF;
    IF v_test_indicators > 0 THEN
        RAISE NOTICE '  - 测试指标: % 条', v_test_indicators;
    END IF;

    IF v_test_users + v_test_orgs + v_test_tasks + v_test_indicators = 0 THEN
        RAISE NOTICE '  ✓ 未发现明显的测试数据';
    ELSE
        RAISE NOTICE '  ⚠ 发现测试数据，建议清理';
    END IF;
END $$;

\echo ''

-- ============================================
-- 4. 数据质量问题汇总
-- ============================================
\echo '>>> 4. 数据质量问题汇总'
\echo ''

DO $$
DECLARE
    total_issues INT := 0;
    issue_count INT;
BEGIN
    RAISE NOTICE '数据完整性问题:';

    -- 统计孤儿记录
    SELECT COUNT(*) INTO issue_count FROM indicator WHERE task_id IS NULL;
    IF issue_count > 0 THEN
        RAISE NOTICE '  [问题1] indicator 表缺少 task_id: % 条', issue_count;
        total_issues := total_issues + 1;
    END IF;

    SELECT COUNT(*) INTO issue_count FROM indicator WHERE owner_org_id IS NULL;
    IF issue_count > 0 THEN
        RAISE NOTICE '  [问题2] indicator 表缺少 owner_org_id: % 条', issue_count;
        total_issues := total_issues + 1;
    END IF;

    SELECT COUNT(*) INTO issue_count FROM indicator WHERE target_org_id IS NULL;
    IF issue_count > 0 THEN
        RAISE NOTICE '  [问题3] indicator 表缺少 target_org_id: % 条', issue_count;
        total_issues := total_issues + 1;
    END IF;

    SELECT COUNT(*) INTO issue_count FROM indicator_milestone WHERE indicator_id IS NULL;
    IF issue_count > 0 THEN
        RAISE NOTICE '  [问题4] indicator_milestone 表缺少 indicator_id: % 条', issue_count;
        total_issues := total_issues + 1;
    END IF;

    IF total_issues = 0 THEN
        RAISE NOTICE '';
        RAISE NOTICE '✓ 未发现数据完整性问题';
    ELSE
        RAISE NOTICE '';
        RAISE NOTICE '总计: % 个数据完整性问题', total_issues;
    END IF;
END $$;

\echo ''

-- ============================================
-- 5. 数据样例
-- ============================================
\echo '>>> 5. 数据样例（前5条）'
\echo ''

\echo '--- sys_org 样例 ---'
SELECT id, name, type, parent_id FROM sys_org ORDER BY id LIMIT 5;

\echo ''
\echo '--- sys_user 样例 ---'
SELECT id, username, real_name, org_id FROM sys_user ORDER BY id LIMIT 5;

\echo ''
\echo '--- assessment_cycle 样例 ---'
SELECT id, name, year, status FROM assessment_cycle ORDER BY id LIMIT 5;

\echo ''
\echo '--- sys_task 样例 ---'
SELECT task_id, name, task_type, cycle_id, org_id FROM sys_task ORDER BY task_id LIMIT 5;

\echo ''
\echo '--- indicator 样例 ---'
SELECT id, indicator_desc, task_id, owner_org_id, target_org_id, progress
FROM indicator
WHERE is_deleted = false
ORDER BY id LIMIT 5;

\echo ''
\echo '============================================'
\echo '统计完成'
\echo '============================================'
