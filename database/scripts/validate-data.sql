-- ============================================
-- SISM 数据校验脚本
-- 用途: 验证数据同步后的数据完整性
-- 版本: V1.0
-- ============================================

-- 设置输出格式
\echo '============================================'
\echo 'SISM 数据完整性校验'
\echo '============================================'
\echo ''

-- ============================================
-- 1. 表记录数量检查
-- ============================================
\echo '>>> 1. 表记录数量检查'
\echo ''

DO $$
DECLARE
    org_count INT;
    user_count INT;
    cycle_count INT;
    task_count INT;
    indicator_count INT;
    milestone_count INT;
    report_count INT;
    adhoc_count INT;
BEGIN
    SELECT COUNT(*) INTO org_count FROM org;
    SELECT COUNT(*) INTO user_count FROM app_user;
    SELECT COUNT(*) INTO cycle_count FROM assessment_cycle;
    SELECT COUNT(*) INTO task_count FROM strategic_task;
    SELECT COUNT(*) INTO indicator_count FROM indicator;
    SELECT COUNT(*) INTO milestone_count FROM milestone;
    SELECT COUNT(*) INTO report_count FROM progress_report;
    SELECT COUNT(*) INTO adhoc_count FROM adhoc_task;
    
    RAISE NOTICE 'org: % 条记录', org_count;
    RAISE NOTICE 'app_user: % 条记录', user_count;
    RAISE NOTICE 'assessment_cycle: % 条记录', cycle_count;
    RAISE NOTICE 'strategic_task: % 条记录', task_count;
    RAISE NOTICE 'indicator: % 条记录', indicator_count;
    RAISE NOTICE 'milestone: % 条记录', milestone_count;
    RAISE NOTICE 'progress_report: % 条记录', report_count;
    RAISE NOTICE 'adhoc_task: % 条记录', adhoc_count;
    
    -- 检查核心表是否有数据
    IF org_count = 0 THEN
        RAISE WARNING '[FAIL] org 表为空，请先执行数据同步';
    END IF;
    IF cycle_count = 0 THEN
        RAISE WARNING '[FAIL] assessment_cycle 表为空，请先执行数据同步';
    END IF;
END $$;

\echo ''

-- ============================================
-- 2. 外键完整性检查 - org 自引用
-- ============================================
\echo '>>> 2. 外键完整性检查 - org 自引用 (parent_org_id)'
\echo ''

SELECT 
    o.org_id,
    o.org_name,
    o.parent_org_id AS invalid_parent_id,
    '[FAIL] parent_org_id 指向不存在的 org' AS issue
FROM org o
LEFT JOIN org p ON o.parent_org_id = p.org_id
WHERE o.parent_org_id IS NOT NULL 
  AND p.org_id IS NULL;

DO $$
DECLARE
    orphan_count INT;
BEGIN
    SELECT COUNT(*) INTO orphan_count
    FROM org o
    LEFT JOIN org p ON o.parent_org_id = p.org_id
    WHERE o.parent_org_id IS NOT NULL AND p.org_id IS NULL;
    
    IF orphan_count = 0 THEN
        RAISE NOTICE '[PASS] org.parent_org_id 外键完整性检查通过';
    ELSE
        RAISE WARNING '[FAIL] 发现 % 条 org 记录的 parent_org_id 无效', orphan_count;
    END IF;
END $$;

\echo ''

-- ============================================
-- 3. 外键完整性检查 - app_user.org_id
-- ============================================
\echo '>>> 3. 外键完整性检查 - app_user.org_id'
\echo ''

SELECT 
    u.user_id,
    u.username,
    u.org_id AS invalid_org_id,
    '[FAIL] org_id 指向不存在的 org' AS issue
FROM app_user u
LEFT JOIN org o ON u.org_id = o.org_id
WHERE o.org_id IS NULL;

DO $$
DECLARE
    orphan_count INT;
BEGIN
    SELECT COUNT(*) INTO orphan_count
    FROM app_user u
    LEFT JOIN org o ON u.org_id = o.org_id
    WHERE o.org_id IS NULL;
    
    IF orphan_count = 0 THEN
        RAISE NOTICE '[PASS] app_user.org_id 外键完整性检查通过';
    ELSE
        RAISE WARNING '[FAIL] 发现 % 条 app_user 记录的 org_id 无效', orphan_count;
    END IF;
END $$;

\echo ''

-- ============================================
-- 4. 外键完整性检查 - strategic_task
-- ============================================
\echo '>>> 4. 外键完整性检查 - strategic_task'
\echo ''

-- 检查 cycle_id
SELECT 
    t.task_id,
    t.task_name,
    t.cycle_id AS invalid_cycle_id,
    '[FAIL] cycle_id 指向不存在的 assessment_cycle' AS issue
FROM strategic_task t
LEFT JOIN assessment_cycle c ON t.cycle_id = c.cycle_id
WHERE c.cycle_id IS NULL;

-- 检查 org_id
SELECT 
    t.task_id,
    t.task_name,
    t.org_id AS invalid_org_id,
    '[FAIL] org_id 指向不存在的 org' AS issue
FROM strategic_task t
LEFT JOIN org o ON t.org_id = o.org_id
WHERE o.org_id IS NULL;

-- 检查 created_by_org_id
SELECT 
    t.task_id,
    t.task_name,
    t.created_by_org_id AS invalid_created_by_org_id,
    '[FAIL] created_by_org_id 指向不存在的 org' AS issue
FROM strategic_task t
LEFT JOIN org o ON t.created_by_org_id = o.org_id
WHERE o.org_id IS NULL;

DO $$
DECLARE
    cycle_orphan INT;
    org_orphan INT;
    created_by_orphan INT;
BEGIN
    SELECT COUNT(*) INTO cycle_orphan
    FROM strategic_task t
    LEFT JOIN assessment_cycle c ON t.cycle_id = c.cycle_id
    WHERE c.cycle_id IS NULL;
    
    SELECT COUNT(*) INTO org_orphan
    FROM strategic_task t
    LEFT JOIN org o ON t.org_id = o.org_id
    WHERE o.org_id IS NULL;
    
    SELECT COUNT(*) INTO created_by_orphan
    FROM strategic_task t
    LEFT JOIN org o ON t.created_by_org_id = o.org_id
    WHERE o.org_id IS NULL;
    
    IF cycle_orphan = 0 AND org_orphan = 0 AND created_by_orphan = 0 THEN
        RAISE NOTICE '[PASS] strategic_task 外键完整性检查通过';
    ELSE
        IF cycle_orphan > 0 THEN
            RAISE WARNING '[FAIL] 发现 % 条 strategic_task 记录的 cycle_id 无效', cycle_orphan;
        END IF;
        IF org_orphan > 0 THEN
            RAISE WARNING '[FAIL] 发现 % 条 strategic_task 记录的 org_id 无效', org_orphan;
        END IF;
        IF created_by_orphan > 0 THEN
            RAISE WARNING '[FAIL] 发现 % 条 strategic_task 记录的 created_by_org_id 无效', created_by_orphan;
        END IF;
    END IF;
END $$;

\echo ''

-- ============================================
-- 5. 外键完整性检查 - indicator (核心检查)
-- ============================================
\echo '>>> 5. 外键完整性检查 - indicator'
\echo ''

-- 检查 task_id
SELECT 
    i.indicator_id,
    i.indicator_desc,
    i.task_id AS invalid_task_id,
    '[FAIL] task_id 指向不存在的 strategic_task' AS issue
FROM indicator i
LEFT JOIN strategic_task t ON i.task_id = t.task_id
WHERE t.task_id IS NULL;

-- 检查 parent_indicator_id (自引用)
SELECT 
    i.indicator_id,
    i.indicator_desc,
    i.parent_indicator_id AS invalid_parent_id,
    '[FAIL] parent_indicator_id 指向不存在的 indicator' AS issue
FROM indicator i
LEFT JOIN indicator p ON i.parent_indicator_id = p.indicator_id
WHERE i.parent_indicator_id IS NOT NULL 
  AND p.indicator_id IS NULL;

-- 检查 owner_org_id (Requirements 7.3)
SELECT 
    i.indicator_id,
    i.indicator_desc,
    i.owner_org_id AS invalid_owner_org_id,
    '[FAIL] owner_org_id 指向不存在的 org' AS issue
FROM indicator i
LEFT JOIN org o ON i.owner_org_id = o.org_id
WHERE o.org_id IS NULL;

-- 检查 target_org_id (Requirements 7.3)
SELECT 
    i.indicator_id,
    i.indicator_desc,
    i.target_org_id AS invalid_target_org_id,
    '[FAIL] target_org_id 指向不存在的 org' AS issue
FROM indicator i
LEFT JOIN org o ON i.target_org_id = o.org_id
WHERE o.org_id IS NULL;

DO $$
DECLARE
    task_orphan INT;
    parent_orphan INT;
    owner_orphan INT;
    target_orphan INT;
BEGIN
    SELECT COUNT(*) INTO task_orphan
    FROM indicator i
    LEFT JOIN strategic_task t ON i.task_id = t.task_id
    WHERE t.task_id IS NULL;
    
    SELECT COUNT(*) INTO parent_orphan
    FROM indicator i
    LEFT JOIN indicator p ON i.parent_indicator_id = p.indicator_id
    WHERE i.parent_indicator_id IS NOT NULL AND p.indicator_id IS NULL;
    
    SELECT COUNT(*) INTO owner_orphan
    FROM indicator i
    LEFT JOIN org o ON i.owner_org_id = o.org_id
    WHERE o.org_id IS NULL;
    
    SELECT COUNT(*) INTO target_orphan
    FROM indicator i
    LEFT JOIN org o ON i.target_org_id = o.org_id
    WHERE o.org_id IS NULL;
    
    IF task_orphan = 0 AND parent_orphan = 0 AND owner_orphan = 0 AND target_orphan = 0 THEN
        RAISE NOTICE '[PASS] indicator 外键完整性检查通过';
    ELSE
        IF task_orphan > 0 THEN
            RAISE WARNING '[FAIL] 发现 % 条 indicator 记录的 task_id 无效', task_orphan;
        END IF;
        IF parent_orphan > 0 THEN
            RAISE WARNING '[FAIL] 发现 % 条 indicator 记录的 parent_indicator_id 无效', parent_orphan;
        END IF;
        IF owner_orphan > 0 THEN
            RAISE WARNING '[FAIL] 发现 % 条 indicator 记录的 owner_org_id 无效', owner_orphan;
        END IF;
        IF target_orphan > 0 THEN
            RAISE WARNING '[FAIL] 发现 % 条 indicator 记录的 target_org_id 无效', target_orphan;
        END IF;
    END IF;
END $$;

\echo ''

-- ============================================
-- 6. 外键完整性检查 - milestone (Requirements 7.4)
-- ============================================
\echo '>>> 6. 外键完整性检查 - milestone'
\echo ''

-- 检查 indicator_id
SELECT 
    m.milestone_id,
    m.milestone_name,
    m.indicator_id AS invalid_indicator_id,
    '[FAIL] indicator_id 指向不存在的 indicator' AS issue
FROM milestone m
LEFT JOIN indicator i ON m.indicator_id = i.indicator_id
WHERE i.indicator_id IS NULL;

-- 检查 inherited_from (自引用)
SELECT 
    m.milestone_id,
    m.milestone_name,
    m.inherited_from AS invalid_inherited_from,
    '[FAIL] inherited_from 指向不存在的 milestone' AS issue
FROM milestone m
LEFT JOIN milestone p ON m.inherited_from = p.milestone_id
WHERE m.inherited_from IS NOT NULL 
  AND p.milestone_id IS NULL;

DO $$
DECLARE
    indicator_orphan INT;
    inherited_orphan INT;
BEGIN
    SELECT COUNT(*) INTO indicator_orphan
    FROM milestone m
    LEFT JOIN indicator i ON m.indicator_id = i.indicator_id
    WHERE i.indicator_id IS NULL;
    
    SELECT COUNT(*) INTO inherited_orphan
    FROM milestone m
    LEFT JOIN milestone p ON m.inherited_from = p.milestone_id
    WHERE m.inherited_from IS NOT NULL AND p.milestone_id IS NULL;
    
    IF indicator_orphan = 0 AND inherited_orphan = 0 THEN
        RAISE NOTICE '[PASS] milestone 外键完整性检查通过';
    ELSE
        IF indicator_orphan > 0 THEN
            RAISE WARNING '[FAIL] 发现 % 条 milestone 记录的 indicator_id 无效', indicator_orphan;
        END IF;
        IF inherited_orphan > 0 THEN
            RAISE WARNING '[FAIL] 发现 % 条 milestone 记录的 inherited_from 无效', inherited_orphan;
        END IF;
    END IF;
END $$;

\echo ''

-- ============================================
-- 7. 外键完整性检查 - progress_report
-- ============================================
\echo '>>> 7. 外键完整性检查 - progress_report'
\echo ''

-- 检查 indicator_id
SELECT 
    r.report_id,
    r.indicator_id AS invalid_indicator_id,
    '[FAIL] indicator_id 指向不存在的 indicator' AS issue
FROM progress_report r
LEFT JOIN indicator i ON r.indicator_id = i.indicator_id
WHERE i.indicator_id IS NULL;

-- 检查 milestone_id
SELECT 
    r.report_id,
    r.milestone_id AS invalid_milestone_id,
    '[FAIL] milestone_id 指向不存在的 milestone' AS issue
FROM progress_report r
LEFT JOIN milestone m ON r.milestone_id = m.milestone_id
WHERE r.milestone_id IS NOT NULL 
  AND m.milestone_id IS NULL;

-- 检查 reporter_id
SELECT 
    r.report_id,
    r.reporter_id AS invalid_reporter_id,
    '[FAIL] reporter_id 指向不存在的 app_user' AS issue
FROM progress_report r
LEFT JOIN app_user u ON r.reporter_id = u.user_id
WHERE u.user_id IS NULL;

DO $$
DECLARE
    indicator_orphan INT;
    milestone_orphan INT;
    reporter_orphan INT;
BEGIN
    SELECT COUNT(*) INTO indicator_orphan
    FROM progress_report r
    LEFT JOIN indicator i ON r.indicator_id = i.indicator_id
    WHERE i.indicator_id IS NULL;
    
    SELECT COUNT(*) INTO milestone_orphan
    FROM progress_report r
    LEFT JOIN milestone m ON r.milestone_id = m.milestone_id
    WHERE r.milestone_id IS NOT NULL AND m.milestone_id IS NULL;
    
    SELECT COUNT(*) INTO reporter_orphan
    FROM progress_report r
    LEFT JOIN app_user u ON r.reporter_id = u.user_id
    WHERE u.user_id IS NULL;
    
    IF indicator_orphan = 0 AND milestone_orphan = 0 AND reporter_orphan = 0 THEN
        RAISE NOTICE '[PASS] progress_report 外键完整性检查通过';
    ELSE
        IF indicator_orphan > 0 THEN
            RAISE WARNING '[FAIL] 发现 % 条 progress_report 记录的 indicator_id 无效', indicator_orphan;
        END IF;
        IF milestone_orphan > 0 THEN
            RAISE WARNING '[FAIL] 发现 % 条 progress_report 记录的 milestone_id 无效', milestone_orphan;
        END IF;
        IF reporter_orphan > 0 THEN
            RAISE WARNING '[FAIL] 发现 % 条 progress_report 记录的 reporter_id 无效', reporter_orphan;
        END IF;
    END IF;
END $$;

\echo ''

-- ============================================
-- 8. 外键完整性检查 - adhoc_task
-- ============================================
\echo '>>> 8. 外键完整性检查 - adhoc_task'
\echo ''

-- 检查 cycle_id
SELECT 
    a.adhoc_task_id,
    a.task_title,
    a.cycle_id AS invalid_cycle_id,
    '[FAIL] cycle_id 指向不存在的 assessment_cycle' AS issue
FROM adhoc_task a
LEFT JOIN assessment_cycle c ON a.cycle_id = c.cycle_id
WHERE c.cycle_id IS NULL;

-- 检查 creator_org_id
SELECT 
    a.adhoc_task_id,
    a.task_title,
    a.creator_org_id AS invalid_creator_org_id,
    '[FAIL] creator_org_id 指向不存在的 org' AS issue
FROM adhoc_task a
LEFT JOIN org o ON a.creator_org_id = o.org_id
WHERE o.org_id IS NULL;

DO $$
DECLARE
    cycle_orphan INT;
    creator_orphan INT;
BEGIN
    SELECT COUNT(*) INTO cycle_orphan
    FROM adhoc_task a
    LEFT JOIN assessment_cycle c ON a.cycle_id = c.cycle_id
    WHERE c.cycle_id IS NULL;
    
    SELECT COUNT(*) INTO creator_orphan
    FROM adhoc_task a
    LEFT JOIN org o ON a.creator_org_id = o.org_id
    WHERE o.org_id IS NULL;
    
    IF cycle_orphan = 0 AND creator_orphan = 0 THEN
        RAISE NOTICE '[PASS] adhoc_task 外键完整性检查通过';
    ELSE
        IF cycle_orphan > 0 THEN
            RAISE WARNING '[FAIL] 发现 % 条 adhoc_task 记录的 cycle_id 无效', cycle_orphan;
        END IF;
        IF creator_orphan > 0 THEN
            RAISE WARNING '[FAIL] 发现 % 条 adhoc_task 记录的 creator_org_id 无效', creator_orphan;
        END IF;
    END IF;
END $$;

\echo ''

-- ============================================
-- 9. 外键完整性检查 - adhoc_task_target
-- ============================================
\echo '>>> 9. 外键完整性检查 - adhoc_task_target'
\echo ''

SELECT 
    t.adhoc_task_id,
    t.target_org_id AS invalid_target_org_id,
    '[FAIL] target_org_id 指向不存在的 org' AS issue
FROM adhoc_task_target t
LEFT JOIN org o ON t.target_org_id = o.org_id
WHERE o.org_id IS NULL;

DO $$
DECLARE
    orphan_count INT;
BEGIN
    SELECT COUNT(*) INTO orphan_count
    FROM adhoc_task_target t
    LEFT JOIN org o ON t.target_org_id = o.org_id
    WHERE o.org_id IS NULL;
    
    IF orphan_count = 0 THEN
        RAISE NOTICE '[PASS] adhoc_task_target 外键完整性检查通过';
    ELSE
        RAISE WARNING '[FAIL] 发现 % 条 adhoc_task_target 记录的 target_org_id 无效', orphan_count;
    END IF;
END $$;

\echo ''

-- ============================================
-- 10. 外键完整性检查 - approval_record
-- ============================================
\echo '>>> 10. 外键完整性检查 - approval_record'
\echo ''

-- 检查 report_id
SELECT 
    a.approval_id,
    a.report_id AS invalid_report_id,
    '[FAIL] report_id 指向不存在的 progress_report' AS issue
FROM approval_record a
LEFT JOIN progress_report r ON a.report_id = r.report_id
WHERE r.report_id IS NULL;

-- 检查 approver_id
SELECT 
    a.approval_id,
    a.approver_id AS invalid_approver_id,
    '[FAIL] approver_id 指向不存在的 app_user' AS issue
FROM approval_record a
LEFT JOIN app_user u ON a.approver_id = u.user_id
WHERE u.user_id IS NULL;

DO $$
DECLARE
    report_orphan INT;
    approver_orphan INT;
BEGIN
    SELECT COUNT(*) INTO report_orphan
    FROM approval_record a
    LEFT JOIN progress_report r ON a.report_id = r.report_id
    WHERE r.report_id IS NULL;
    
    SELECT COUNT(*) INTO approver_orphan
    FROM approval_record a
    LEFT JOIN app_user u ON a.approver_id = u.user_id
    WHERE u.user_id IS NULL;
    
    IF report_orphan = 0 AND approver_orphan = 0 THEN
        RAISE NOTICE '[PASS] approval_record 外键完整性检查通过';
    ELSE
        IF report_orphan > 0 THEN
            RAISE WARNING '[FAIL] 发现 % 条 approval_record 记录的 report_id 无效', report_orphan;
        END IF;
        IF approver_orphan > 0 THEN
            RAISE WARNING '[FAIL] 发现 % 条 approval_record 记录的 approver_id 无效', approver_orphan;
        END IF;
    END IF;
END $$;

\echo ''

-- ============================================
-- 11. 汇总校验结果
-- ============================================
\echo '============================================'
\echo '>>> 校验汇总'
\echo '============================================'
\echo ''

DO $$
DECLARE
    total_issues INT := 0;
    issue_count INT;
BEGIN
    -- org.parent_org_id
    SELECT COUNT(*) INTO issue_count FROM org o LEFT JOIN org p ON o.parent_org_id = p.org_id WHERE o.parent_org_id IS NOT NULL AND p.org_id IS NULL;
    total_issues := total_issues + issue_count;
    
    -- app_user.org_id
    SELECT COUNT(*) INTO issue_count FROM app_user u LEFT JOIN org o ON u.org_id = o.org_id WHERE o.org_id IS NULL;
    total_issues := total_issues + issue_count;
    
    -- strategic_task
    SELECT COUNT(*) INTO issue_count FROM strategic_task t LEFT JOIN assessment_cycle c ON t.cycle_id = c.cycle_id WHERE c.cycle_id IS NULL;
    total_issues := total_issues + issue_count;
    SELECT COUNT(*) INTO issue_count FROM strategic_task t LEFT JOIN org o ON t.org_id = o.org_id WHERE o.org_id IS NULL;
    total_issues := total_issues + issue_count;
    SELECT COUNT(*) INTO issue_count FROM strategic_task t LEFT JOIN org o ON t.created_by_org_id = o.org_id WHERE o.org_id IS NULL;
    total_issues := total_issues + issue_count;
    
    -- indicator
    SELECT COUNT(*) INTO issue_count FROM indicator i LEFT JOIN strategic_task t ON i.task_id = t.task_id WHERE t.task_id IS NULL;
    total_issues := total_issues + issue_count;
    SELECT COUNT(*) INTO issue_count FROM indicator i LEFT JOIN indicator p ON i.parent_indicator_id = p.indicator_id WHERE i.parent_indicator_id IS NOT NULL AND p.indicator_id IS NULL;
    total_issues := total_issues + issue_count;
    SELECT COUNT(*) INTO issue_count FROM indicator i LEFT JOIN org o ON i.owner_org_id = o.org_id WHERE o.org_id IS NULL;
    total_issues := total_issues + issue_count;
    SELECT COUNT(*) INTO issue_count FROM indicator i LEFT JOIN org o ON i.target_org_id = o.org_id WHERE o.org_id IS NULL;
    total_issues := total_issues + issue_count;
    
    -- milestone
    SELECT COUNT(*) INTO issue_count FROM milestone m LEFT JOIN indicator i ON m.indicator_id = i.indicator_id WHERE i.indicator_id IS NULL;
    total_issues := total_issues + issue_count;
    SELECT COUNT(*) INTO issue_count FROM milestone m LEFT JOIN milestone p ON m.inherited_from = p.milestone_id WHERE m.inherited_from IS NOT NULL AND p.milestone_id IS NULL;
    total_issues := total_issues + issue_count;
    
    -- progress_report
    SELECT COUNT(*) INTO issue_count FROM progress_report r LEFT JOIN indicator i ON r.indicator_id = i.indicator_id WHERE i.indicator_id IS NULL;
    total_issues := total_issues + issue_count;
    SELECT COUNT(*) INTO issue_count FROM progress_report r LEFT JOIN milestone m ON r.milestone_id = m.milestone_id WHERE r.milestone_id IS NOT NULL AND m.milestone_id IS NULL;
    total_issues := total_issues + issue_count;
    SELECT COUNT(*) INTO issue_count FROM progress_report r LEFT JOIN app_user u ON r.reporter_id = u.user_id WHERE u.user_id IS NULL;
    total_issues := total_issues + issue_count;
    
    -- adhoc_task
    SELECT COUNT(*) INTO issue_count FROM adhoc_task a LEFT JOIN assessment_cycle c ON a.cycle_id = c.cycle_id WHERE c.cycle_id IS NULL;
    total_issues := total_issues + issue_count;
    SELECT COUNT(*) INTO issue_count FROM adhoc_task a LEFT JOIN org o ON a.creator_org_id = o.org_id WHERE o.org_id IS NULL;
    total_issues := total_issues + issue_count;
    
    -- adhoc_task_target
    SELECT COUNT(*) INTO issue_count FROM adhoc_task_target t LEFT JOIN org o ON t.target_org_id = o.org_id WHERE o.org_id IS NULL;
    total_issues := total_issues + issue_count;
    
    -- approval_record
    SELECT COUNT(*) INTO issue_count FROM approval_record a LEFT JOIN progress_report r ON a.report_id = r.report_id WHERE r.report_id IS NULL;
    total_issues := total_issues + issue_count;
    SELECT COUNT(*) INTO issue_count FROM approval_record a LEFT JOIN app_user u ON a.approver_id = u.user_id WHERE u.user_id IS NULL;
    total_issues := total_issues + issue_count;
    
    IF total_issues = 0 THEN
        RAISE NOTICE '';
        RAISE NOTICE '============================================';
        RAISE NOTICE 'All validations passed';
        RAISE NOTICE '============================================';
    ELSE
        RAISE WARNING '';
        RAISE WARNING '============================================';
        RAISE WARNING '发现 % 个数据完整性问题，请检查上述详细信息', total_issues;
        RAISE WARNING '============================================';
    END IF;
END $$;

\echo ''
\echo '校验完成'
