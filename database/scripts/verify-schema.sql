-- ============================================
-- SISM 数据库表结构验证脚本
-- 用于验证所有表、外键约束和索引是否正确创建
-- ============================================

-- 验证所有表是否存在
SELECT 
    'TABLE_CHECK' AS check_type,
    table_name,
    CASE 
        WHEN table_name IN (
            'org', 'app_user', 'assessment_cycle', 'strategic_task', 
            'indicator', 'milestone', 'progress_report', 'approval_record',
            'alert_window', 'alert_rule', 'alert_event',
            'adhoc_task', 'adhoc_task_target', 'adhoc_task_indicator_map',
            'audit_log'
        ) THEN 'EXISTS'
        ELSE 'UNEXPECTED'
    END AS status
FROM information_schema.tables
WHERE table_schema = 'public' 
    AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- 验证所有枚举类型是否存在
SELECT 
    'ENUM_CHECK' AS check_type,
    typname AS enum_name,
    'EXISTS' AS status
FROM pg_type
WHERE typtype = 'e'
    AND typname IN (
        'org_type', 'task_type', 'indicator_level', 'indicator_status',
        'milestone_status', 'report_status', 'approval_action',
        'alert_severity', 'alert_status', 'adhoc_scope_type',
        'adhoc_task_status', 'audit_action', 'audit_entity_type'
    )
ORDER BY typname;

-- 验证外键约束
SELECT 
    'FK_CHECK' AS check_type,
    tc.table_name,
    tc.constraint_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name,
    'EXISTS' AS status
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
    AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY'
    AND tc.table_schema = 'public'
ORDER BY tc.table_name, tc.constraint_name;

-- 验证索引
SELECT 
    'INDEX_CHECK' AS check_type,
    schemaname,
    tablename,
    indexname,
    'EXISTS' AS status
FROM pg_indexes
WHERE schemaname = 'public'
ORDER BY tablename, indexname;

-- 验证主键约束
SELECT 
    'PK_CHECK' AS check_type,
    tc.table_name,
    tc.constraint_name,
    kcu.column_name,
    'EXISTS' AS status
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
WHERE tc.constraint_type = 'PRIMARY KEY'
    AND tc.table_schema = 'public'
ORDER BY tc.table_name;

-- 验证检查约束
SELECT 
    'CHECK_CONSTRAINT' AS check_type,
    tc.table_name,
    tc.constraint_name,
    cc.check_clause,
    'EXISTS' AS status
FROM information_schema.table_constraints AS tc
JOIN information_schema.check_constraints AS cc
    ON tc.constraint_name = cc.constraint_name
WHERE tc.constraint_type = 'CHECK'
    AND tc.table_schema = 'public'
ORDER BY tc.table_name;

-- 验证触发器
SELECT 
    'TRIGGER_CHECK' AS check_type,
    trigger_name,
    event_object_table AS table_name,
    action_timing,
    event_manipulation,
    'EXISTS' AS status
FROM information_schema.triggers
WHERE trigger_schema = 'public'
ORDER BY event_object_table, trigger_name;

-- 统计各表记录数
SELECT 
    'ROW_COUNT' AS check_type,
    schemaname,
    relname AS table_name,
    n_live_tup AS row_count
FROM pg_stat_user_tables
WHERE schemaname = 'public'
ORDER BY relname;
