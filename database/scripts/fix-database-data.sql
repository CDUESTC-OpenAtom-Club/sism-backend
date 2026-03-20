-- ============================================
-- SISM 数据修复脚本
-- 用途: 修复数据库中的测试数据、孤儿记录等问题
-- 版本: V1.0
-- ============================================

\echo '============================================'
\echo 'SISM 数据修复脚本'
\echo '============================================'
\echo ''
\echo '警告: 此脚本将修改数据库数据，请先备份！'
\echo '按 Ctrl+C 取消，按回车继续...'
\echo ''

-- ============================================
-- 1. 清理测试数据
-- ============================================
\echo '>>> 1. 清理测试数据'
\echo ''

BEGIN;

-- 1.1 清理测试用户（保留系统管理员）
DELETE FROM sys_user
WHERE (username ILIKE '%test%' OR real_name ILIKE '%test%')
  AND username NOT IN ('admin', 'system');

GET DIAGNOSTICS v_row_count = ROW_COUNT;
RAISE NOTICE '已删除 % 条测试用户记录', v_row_count;

-- 1.2 清理测试组织（保留基本组织结构）
DELETE FROM sys_org
WHERE (name ILIKE '%test%' OR name ILIKE '%测试%')
  AND id NOT IN (SELECT DISTINCT owner_org_id FROM indicator WHERE owner_org_id IS NOT NULL)
  AND id NOT IN (SELECT DISTINCT target_org_id FROM indicator WHERE target_org_id IS NOT NULL)
  AND id NOT IN (SELECT DISTINCT org_id FROM sys_user WHERE org_id IS NOT NULL);

GET DIAGNOSTICS v_row_count = ROW_COUNT;
RAISE NOTICE '已删除 % 条测试组织记录', v_row_count;

-- 1.3 清理测试任务
DELETE FROM sys_task
WHERE (name ILIKE '%test%' OR name ILIKE '%测试%')
  AND task_id NOT IN (SELECT DISTINCT task_id FROM indicator WHERE task_id IS NOT NULL);

GET DIAGNOSTICS v_row_count = ROW_COUNT;
RAISE NOTICE '已删除 % 条测试任务记录', v_row_count;

-- 1.4 清理测试指标
DELETE FROM indicator
WHERE (indicator_desc ILIKE '%test%' OR indicator_desc ILIKE '%测试%')
  AND id NOT IN (SELECT DISTINCT indicator_id FROM indicator_milestone);

GET DIAGNOSTICS v_row_count = ROW_COUNT;
RAISE NOTICE '已删除 % 条测试指标记录', v_row_count;

COMMIT;
RAISE NOTICE '✓ 测试数据清理完成';

\echo ''

-- ============================================
-- 2. 修复孤儿记录
-- ============================================
\echo '>>> 2. 修复孤儿记录'
\echo ''

BEGIN;

-- 2.1 删除没有 task_id 的指标（这些指标无法关联任务）
-- 先记录数量
SELECT COUNT(*) INTO v_orphan_count FROM indicator WHERE task_id IS NULL;
RAISE NOTICE '发现 % 条孤儿指标（task_id 为空）', v_orphan_count;

-- 删除孤儿指标
DELETE FROM indicator_milestone
WHERE indicator_id IN (SELECT id FROM indicator WHERE task_id IS NULL);

GET DIAGNOSTICS v_row_count = ROW_COUNT;
RAISE NOTICE '已删除 % 条孤儿指标的里程碑记录', v_row_count;

DELETE FROM indicator
WHERE task_id IS NULL;

GET DIAGNOSTICS v_row_count = ROW_COUNT;
RAISE NOTICE '已删除 % 条孤儿指标记录', v_row_count;

-- 2.2 删除没有关联的里程碑
DELETE FROM indicator_milestone
WHERE indicator_id NOT IN (SELECT id FROM indicator);

GET DIAGNOSTICS v_row_count = ROW_COUNT;
RAISE NOTICE '已删除 % 条孤儿里程碑记录', v_row_count;

COMMIT;
RAISE NOTICE '✓ 孤儿记录修复完成';

\echo ''

-- ============================================
-- 3. 数据规范化
-- ============================================
\echo '>>> 3. 数据规范化'
\echo ''

BEGIN;

-- 3.1 确保所有指标都有有效的组织关联
-- 如果指标缺少 owner_org_id，使用 target_org_id 作为后备
UPDATE indicator
SET owner_org_id = target_org_id
WHERE owner_org_id IS NULL AND target_org_id IS NOT NULL;

GET DIAGNOSTICS v_row_count = ROW_COUNT;
RAISE NOTICE '已修复 % 条指标的 owner_org_id', v_row_count;

-- 3.2 如果指标缺少 target_org_id，使用 owner_org_id 作为后备
UPDATE indicator
SET target_org_id = owner_org_id
WHERE target_org_id IS NULL AND owner_org_id IS NOT NULL;

GET DIAGNOSTICS v_row_count = ROW_COUNT;
RAISE NOTICE '已修复 % 条指标的 target_org_id', v_row_count;

-- 3.3 设置默认状态
UPDATE indicator
SET status = 'ACTIVE'
WHERE status IS NULL OR status = '';

GET DIAGNOSTICS v_row_count = ROW_COUNT;
RAISE NOTICE '已设置 % 条指标的默认状态', v_row_count;

-- 3.4 设置默认进度
UPDATE indicator
SET progress = 0
WHERE progress IS NULL;

GET DIAGNOSTICS v_row_count = ROW_COUNT;
RAISE NOTICE '已设置 % 条指标的默认进度', v_row_count;

-- 3.5 清理已删除的数据（is_deleted = true）
-- 可选：根据需要决定是否真的删除
-- DELETE FROM indicator WHERE is_deleted = true;

COMMIT;
RAISE NOTICE '✓ 数据规范化完成';

\echo ''

-- ============================================
-- 4. 数据验证
-- ============================================
\echo '>>> 4. 修复后数据验证'
\echo ''

DO $$
DECLARE
    v_issue_count INT;
BEGIN
    -- 验证孤儿记录
    SELECT COUNT(*) INTO v_issue_count FROM indicator WHERE task_id IS NULL;
    IF v_issue_count > 0 THEN
        RAISE WARNING '[WARNING] 仍有 % 条孤儿指标', v_issue_count;
    ELSE
        RAISE NOTICE '✓ 无孤儿指标';
    END IF;

    SELECT COUNT(*) INTO v_issue_count FROM indicator WHERE owner_org_id IS NULL;
    IF v_issue_count > 0 THEN
        RAISE WARNING '[WARNING] 仍有 % 条指标缺少 owner_org_id', v_issue_count;
    ELSE
        RAISE NOTICE '✓ 所有指标都有 owner_org_id';
    END IF;

    SELECT COUNT(*) INTO v_issue_count FROM indicator WHERE target_org_id IS NULL;
    IF v_issue_count > 0 THEN
        RAISE WARNING '[WARNING] 仍有 % 条指标缺少 target_org_id', v_issue_count;
    ELSE
        RAISE NOTICE '✓ 所有指标都有 target_org_id';
    END IF;

    SELECT COUNT(*) INTO v_issue_count FROM indicator_milestone WHERE indicator_id IS NULL;
    IF v_issue_count > 0 THEN
        RAISE WARNING '[WARNING] 仍有 % 条孤儿里程碑', v_issue_count;
    ELSE
        RAISE NOTICE '✓ 无孤儿里程碑';
    END IF;
END $$;

\echo ''
\echo '============================================'
\echo '数据修复完成'
\echo '============================================'
