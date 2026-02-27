-- ============================================
-- SISM Spring Boot Schema Definition
-- 基本表结构定义
-- ============================================

-- 创建序列
CREATE SEQUENCE IF NOT EXISTS sys_org_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS sys_user_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS assessment_cycle_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS sys_task_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS indicator_seq START WITH 1 INCREMENT BY 1;

-- ============================================
-- 组织表 (sys_org)
-- ============================================
CREATE TABLE IF NOT EXISTS sys_org (
    id BIGINT PRIMARY KEY DEFAULT nextval('sys_org_seq'),
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    parent_id BIGINT,
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 用户表 (sys_user)
-- ============================================
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY DEFAULT nextval('sys_user_seq'),
    username VARCHAR(50) NOT NULL UNIQUE,
    real_name VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    org_id BIGINT NOT NULL,
    sso_id VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (org_id) REFERENCES sys_org(id)
);

-- ============================================
-- 评估周期表 (assessment_cycle)
-- ============================================
CREATE TABLE IF NOT EXISTS assessment_cycle (
    id BIGINT PRIMARY KEY DEFAULT nextval('assessment_cycle_seq'),
    name VARCHAR(100) NOT NULL,
    year INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 战略任务表 (sys_task)
-- ============================================
CREATE TABLE IF NOT EXISTS sys_task (
    id BIGINT PRIMARY KEY DEFAULT nextval('sys_task_seq'),
    task_name VARCHAR(200) NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    cycle_id BIGINT NOT NULL,
    org_id BIGINT NOT NULL,
    created_by_org_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cycle_id) REFERENCES assessment_cycle(id),
    FOREIGN KEY (org_id) REFERENCES sys_org(id),
    FOREIGN KEY (created_by_org_id) REFERENCES sys_org(id)
);

-- ============================================
-- 指标表 (indicator)
-- ============================================
CREATE TABLE IF NOT EXISTS indicator (
    id BIGINT PRIMARY KEY DEFAULT nextval('indicator_seq'),
    indicator_desc TEXT NOT NULL,
    task_id BIGINT NOT NULL,
    owner_org_id BIGINT NOT NULL,
    target_org_id BIGINT NOT NULL,
    level VARCHAR(20) DEFAULT 'PRIMARY',
    weight_percent DECIMAL(5,2) DEFAULT 0.0,
    sort_order INTEGER DEFAULT 0,
    type VARCHAR(20) DEFAULT '基础性',
    type1 VARCHAR(20) DEFAULT '定量',
    type2 VARCHAR(20) DEFAULT '基础性',
    progress INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    year INTEGER NOT NULL,
    is_deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES sys_task(id),
    FOREIGN KEY (owner_org_id) REFERENCES sys_org(id),
    FOREIGN KEY (target_org_id) REFERENCES sys_org(id)
);

-- ============================================
-- 指标里程碑表 (indicator_milestone)
-- ============================================
CREATE SEQUENCE IF NOT EXISTS indicator_milestone_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS indicator_milestone (
    id BIGINT PRIMARY KEY DEFAULT nextval('indicator_milestone_seq'),
    indicator_id BIGINT NOT NULL,
    milestone_name VARCHAR(200) NOT NULL,
    milestone_desc TEXT,
    due_date DATE NOT NULL,
    target_progress INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING',
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (indicator_id) REFERENCES indicator(id)
);
