-- ============================================
-- 性能优化脚本
-- 用于解决登录慢的问题
-- ============================================

-- 1. 检查现有索引
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
    AND tablename IN ('app_user', 'org')
ORDER BY tablename, indexname;

-- 2. 确保关键索引存在
CREATE INDEX IF NOT EXISTS idx_user_username ON app_user(username);
CREATE INDEX IF NOT EXISTS idx_user_org ON app_user(org_id);
CREATE INDEX IF NOT EXISTS idx_org_parent ON org(parent_org_id);
CREATE INDEX IF NOT EXISTS idx_org_type ON org(org_type);

-- 3. 更新表统计信息（非常重要！）
ANALYZE app_user;
ANALYZE org;

-- 4. 重建索引（如果索引损坏）
REINDEX TABLE app_user;
REINDEX TABLE org;

-- 5. 检查表大小和索引大小
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) AS index_size
FROM pg_tables
WHERE schemaname = 'public'
    AND tablename IN ('app_user', 'org')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- 6. 检查慢查询（需要启用 pg_stat_statements 扩展）
-- CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
-- SELECT 
--     query,
--     calls,
--     total_time,
--     mean_time,
--     max_time
-- FROM pg_stat_statements
-- WHERE query LIKE '%app_user%' OR query LIKE '%org%'
-- ORDER BY mean_time DESC
-- LIMIT 10;

-- 7. 检查表膨胀（bloat）
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size,
    n_live_tup AS live_tuples,
    n_dead_tup AS dead_tuples,
    CASE 
        WHEN n_live_tup > 0 
        THEN round(100.0 * n_dead_tup / n_live_tup, 2)
        ELSE 0
    END AS dead_tuple_percent
FROM pg_stat_user_tables
WHERE schemaname = 'public'
    AND tablename IN ('app_user', 'org')
ORDER BY n_dead_tup DESC;

-- 8. 如果死元组过多，执行VACUUM
-- VACUUM ANALYZE app_user;
-- VACUUM ANALYZE org;

-- 9. 检查连接数
SELECT 
    count(*) AS total_connections,
    count(*) FILTER (WHERE state = 'active') AS active_connections,
    count(*) FILTER (WHERE state = 'idle') AS idle_connections
FROM pg_stat_activity;

-- 10. 检查锁等待
SELECT 
    pid,
    usename,
    application_name,
    client_addr,
    state,
    wait_event_type,
    wait_event,
    query
FROM pg_stat_activity
WHERE wait_event IS NOT NULL
    AND state = 'active';

-- ============================================
-- 执行说明
-- ============================================
-- 1. 连接到数据库:
--    psql -U postgres -d sism_db
--
-- 2. 执行此脚本:
--    \i optimize-performance.sql
--
-- 3. 查看结果，重点关注:
--    - 索引是否存在
--    - 表大小是否合理
--    - 死元组百分比（应该 < 10%）
--
-- 4. 如果死元组过多，取消注释第8步的VACUUM命令并执行
--
-- ============================================
