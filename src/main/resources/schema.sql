-- ============================================
-- SISM Spring Boot Schema Definition (Optional)
-- 此文件为可选配置，仅在需要 Spring Boot 自动执行时使用
-- 推荐使用 strategic-task-management/database/init.sql
-- ============================================

-- 注意: 此文件仅包含表结构定义的引用
-- 实际的完整初始化脚本位于: ../../../strategic-task-management/database/init.sql

-- 如果需要 Spring Boot 自动执行数据库初始化，请配置:
-- spring.sql.init.mode=always
-- spring.sql.init.schema-locations=classpath:schema.sql
-- spring.sql.init.data-locations=classpath:data.sql

-- 但是，推荐的做法是:
-- 1. 手动执行 strategic-task-management/database/init.sql
-- 2. 配置 spring.jpa.hibernate.ddl-auto=validate 验证表结构
-- 3. 生产环境使用 spring.jpa.hibernate.ddl-auto=none

-- 此文件保留为空，作为占位符
