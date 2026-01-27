-- ============================================
-- SISM 战略指标管理系统 数据库初始化脚本
-- 版本: V0.5
-- 数据库: PostgreSQL / OpenTenBase
-- ============================================

-- 创建枚举类型
-- 注意: 枚举值必须与 Java 后端 com.sism.enums 包中的定义保持一致
CREATE TYPE org_type AS ENUM ('SCHOOL', 'FUNCTIONAL_DEPT', 'FUNCTION_DEPT', 'COLLEGE', 'STRATEGY_DEPT', 'DIVISION', 'OTHER');
CREATE TYPE task_type AS ENUM ('BASIC', 'REGULAR', 'KEY', 'SPECIAL', 'QUANTITATIVE', 'DEVELOPMENT');
CREATE TYPE indicator_level AS ENUM ('STRAT_TO_FUNC', 'FUNC_TO_COLLEGE');
CREATE TYPE indicator_status AS ENUM ('ACTIVE', 'ARCHIVED');
CREATE TYPE milestone_status AS ENUM ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'DELAYED', 'CANCELED');
CREATE TYPE report_status AS ENUM ('DRAFT', 'SUBMITTED', 'RETURNED', 'APPROVED', 'REJECTED');
CREATE TYPE approval_action AS ENUM ('APPROVE', 'REJECT', 'RETURN');
CREATE TYPE alert_severity AS ENUM ('INFO', 'WARNING', 'CRITICAL');
CREATE TYPE alert_status AS ENUM ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED');
CREATE TYPE adhoc_scope_type AS ENUM ('ALL_ORGS', 'BY_DEPT_ISSUED_INDICATORS', 'CUSTOM');
CREATE TYPE adhoc_task_status AS ENUM ('DRAFT', 'OPEN', 'CLOSED', 'ARCHIVED');
CREATE TYPE audit_action AS ENUM ('CREATE', 'UPDATE', 'DELETE', 'APPROVE', 'ARCHIVE', 'RESTORE');
CREATE TYPE audit_entity_type AS ENUM ('ORG', 'USER', 'CYCLE', 'TASK', 'INDICATOR', 'MILESTONE', 'REPORT', 'ADHOC_TASK', 'ALERT');

-- ============================================
-- 1. org 组织表
-- ============================================
CREATE TABLE org (
    org_id BIGSERIAL PRIMARY KEY,
    org_name VARCHAR(100) NOT NULL,
    org_type org_type NOT NULL,
    parent_org_id BIGINT REFERENCES org(org_id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_org_parent ON org(parent_org_id);
CREATE INDEX idx_org_type ON org(org_type);

COMMENT ON TABLE org IS '组织表 - 战略发展部/职能部门/二级学院/系部';
COMMENT ON COLUMN org.org_type IS '组织类型: STRATEGY_DEPT-战略发展部, FUNCTION_DEPT-职能部门, COLLEGE-二级学院, DIVISION-系部';

-- ============================================
-- 2. app_user 用户表
-- ============================================
CREATE TABLE app_user (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    real_name VARCHAR(50) NOT NULL,
    org_id BIGINT NOT NULL REFERENCES org(org_id),
    password_hash VARCHAR(255) NOT NULL,
    sso_id VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_org ON app_user(org_id);
CREATE INDEX idx_user_username ON app_user(username);

COMMENT ON TABLE app_user IS '用户表';
COMMENT ON COLUMN app_user.sso_id IS '单点登录ID（预留）';


-- ============================================
-- 3. assessment_cycle 考核周期表
-- ============================================
CREATE TABLE assessment_cycle (
    cycle_id BIGSERIAL PRIMARY KEY,
    cycle_name VARCHAR(100) NOT NULL,
    year INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cycle_year ON assessment_cycle(year);

COMMENT ON TABLE assessment_cycle IS '考核周期表';

-- ============================================
-- 4. strategic_task 战略任务表
-- ============================================
CREATE TABLE strategic_task (
    task_id BIGSERIAL PRIMARY KEY,
    cycle_id BIGINT NOT NULL REFERENCES assessment_cycle(cycle_id),
    task_name VARCHAR(200) NOT NULL,
    task_desc TEXT,
    task_type task_type NOT NULL DEFAULT 'BASIC',
    org_id BIGINT NOT NULL REFERENCES org(org_id),
    created_by_org_id BIGINT NOT NULL REFERENCES org(org_id),
    sort_order INT NOT NULL DEFAULT 0,
    remark TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_task_cycle ON strategic_task(cycle_id);
CREATE INDEX idx_task_org ON strategic_task(org_id);

COMMENT ON TABLE strategic_task IS '战略任务表';
COMMENT ON COLUMN strategic_task.task_type IS '任务类型: BASIC-基础任务, DEVELOPMENT-发展任务';
COMMENT ON COLUMN strategic_task.created_by_org_id IS '创建方（通常为战略发展部）';

-- ============================================
-- 5. indicator 指标表
-- ============================================
CREATE TABLE indicator (
    indicator_id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES strategic_task(task_id),
    parent_indicator_id BIGINT REFERENCES indicator(indicator_id),
    level indicator_level NOT NULL,
    owner_org_id BIGINT NOT NULL REFERENCES org(org_id),
    target_org_id BIGINT NOT NULL REFERENCES org(org_id),
    indicator_desc TEXT NOT NULL,
    weight_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    year INT NOT NULL,
    status indicator_status NOT NULL DEFAULT 'ACTIVE',
    remark TEXT,
    -- 新增字段 (前端数据对齐 2026-01-19)
    is_qualitative BOOLEAN DEFAULT FALSE,
    type1 VARCHAR(20) DEFAULT '定量',
    type2 VARCHAR(20) DEFAULT '基础性',
    target_value DECIMAL(10,2),
    actual_value DECIMAL(10,2),
    unit VARCHAR(50),
    responsible_person VARCHAR(100),
    can_withdraw BOOLEAN DEFAULT FALSE,
    progress INTEGER DEFAULT 0,
    status_audit JSONB DEFAULT '[]'::jsonb,
    progress_approval_status VARCHAR(20) DEFAULT 'NONE',
    pending_progress INTEGER,
    pending_remark TEXT,
    pending_attachments JSONB DEFAULT '[]'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_indicator_task ON indicator(task_id);
CREATE INDEX idx_indicator_parent ON indicator(parent_indicator_id);
CREATE INDEX idx_indicator_owner ON indicator(owner_org_id);
CREATE INDEX idx_indicator_target ON indicator(target_org_id);
CREATE INDEX idx_indicator_status ON indicator(status);
CREATE INDEX idx_indicator_year ON indicator(year);
CREATE INDEX idx_indicator_type1 ON indicator(type1);
CREATE INDEX idx_indicator_type2 ON indicator(type2);
CREATE INDEX idx_indicator_is_qualitative ON indicator(is_qualitative);
CREATE INDEX idx_indicator_progress_approval ON indicator(progress_approval_status);

COMMENT ON TABLE indicator IS '指标表 - 支持自引用分层';
COMMENT ON COLUMN indicator.level IS '指标层级: STRAT_TO_FUNC-战略到职能, FUNC_TO_COLLEGE-职能到学院';
COMMENT ON COLUMN indicator.owner_org_id IS '发布方组织';
COMMENT ON COLUMN indicator.target_org_id IS '承接方组织';

-- ============================================
-- 6. milestone 里程碑表
-- ============================================
CREATE TABLE milestone (
    milestone_id BIGSERIAL PRIMARY KEY,
    indicator_id BIGINT NOT NULL REFERENCES indicator(indicator_id),
    milestone_name VARCHAR(200) NOT NULL,
    milestone_desc TEXT,
    due_date DATE NOT NULL,
    weight_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
    status milestone_status NOT NULL DEFAULT 'NOT_STARTED',
    sort_order INT NOT NULL DEFAULT 0,
    inherited_from BIGINT REFERENCES milestone(milestone_id),
    -- 新增字段 (前端数据对齐 2026-01-19)
    target_progress INTEGER DEFAULT 0,
    is_paired BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_milestone_indicator ON milestone(indicator_id);
CREATE INDEX idx_milestone_status ON milestone(status);
CREATE INDEX idx_milestone_due ON milestone(due_date);

COMMENT ON TABLE milestone IS '里程碑表';
COMMENT ON COLUMN milestone.inherited_from IS '继承来源（用于跨年度复刻）';


-- ============================================
-- 7. progress_report 进度汇报表
-- ============================================
CREATE TABLE progress_report (
    report_id BIGSERIAL PRIMARY KEY,
    indicator_id BIGINT NOT NULL REFERENCES indicator(indicator_id),
    milestone_id BIGINT REFERENCES milestone(milestone_id),
    adhoc_task_id BIGINT, -- FK 稍后添加
    percent_complete NUMERIC(5,2) NOT NULL DEFAULT 0,
    achieved_milestone BOOLEAN NOT NULL DEFAULT FALSE,
    narrative TEXT,
    reporter_id BIGINT NOT NULL REFERENCES app_user(user_id),
    status report_status NOT NULL DEFAULT 'DRAFT',
    is_final BOOLEAN NOT NULL DEFAULT FALSE,
    version_no INT NOT NULL DEFAULT 1,
    reported_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- R3: milestone_id 与 adhoc_task_id 不可同时存在
    CONSTRAINT chk_report_target CHECK (
        NOT (milestone_id IS NOT NULL AND adhoc_task_id IS NOT NULL)
    )
);

CREATE INDEX idx_report_indicator ON progress_report(indicator_id);
CREATE INDEX idx_report_milestone ON progress_report(milestone_id);
CREATE INDEX idx_report_adhoc ON progress_report(adhoc_task_id);
CREATE INDEX idx_report_reporter ON progress_report(reporter_id);
CREATE INDEX idx_report_status ON progress_report(status);

COMMENT ON TABLE progress_report IS '进度汇报表';
COMMENT ON COLUMN progress_report.is_final IS '是否最终版（同一里程碑仅允许1条APPROVED且is_final=true）';

-- ============================================
-- 8. approval_record 审批记录表
-- ============================================
CREATE TABLE approval_record (
    approval_id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL REFERENCES progress_report(report_id),
    approver_id BIGINT NOT NULL REFERENCES app_user(user_id),
    action approval_action NOT NULL,
    comment TEXT,
    acted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_approval_report ON approval_record(report_id);
CREATE INDEX idx_approval_approver ON approval_record(approver_id);

COMMENT ON TABLE approval_record IS '审批记录表';

-- ============================================
-- 9. alert_window 预警窗口表
-- ============================================
CREATE TABLE alert_window (
    window_id BIGSERIAL PRIMARY KEY,
    cycle_id BIGINT NOT NULL REFERENCES assessment_cycle(cycle_id),
    name VARCHAR(100) NOT NULL,
    cutoff_date DATE NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alert_window_cycle ON alert_window(cycle_id);

COMMENT ON TABLE alert_window IS '预警窗口表';

-- ============================================
-- 10. alert_rule 预警规则表
-- ============================================
CREATE TABLE alert_rule (
    rule_id BIGSERIAL PRIMARY KEY,
    cycle_id BIGINT NOT NULL REFERENCES assessment_cycle(cycle_id),
    name VARCHAR(100) NOT NULL,
    severity alert_severity NOT NULL DEFAULT 'WARNING',
    gap_threshold NUMERIC(5,2) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alert_rule_cycle ON alert_rule(cycle_id);

COMMENT ON TABLE alert_rule IS '预警规则表';
COMMENT ON COLUMN alert_rule.gap_threshold IS '预警阈值（差值百分比）';

-- ============================================
-- 11. alert_event 预警事件表
-- ============================================
CREATE TABLE alert_event (
    event_id BIGSERIAL PRIMARY KEY,
    indicator_id BIGINT NOT NULL REFERENCES indicator(indicator_id),
    window_id BIGINT NOT NULL REFERENCES alert_window(window_id),
    rule_id BIGINT NOT NULL REFERENCES alert_rule(rule_id),
    expected_percent NUMERIC(5,2) NOT NULL,
    actual_percent NUMERIC(5,2) NOT NULL,
    gap_percent NUMERIC(5,2) NOT NULL,
    severity alert_severity NOT NULL,
    status alert_status NOT NULL DEFAULT 'OPEN',
    handled_by BIGINT REFERENCES app_user(user_id),
    handled_note TEXT,
    detail_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alert_event_indicator ON alert_event(indicator_id);
CREATE INDEX idx_alert_event_window ON alert_event(window_id);
CREATE INDEX idx_alert_event_status ON alert_event(status);

COMMENT ON TABLE alert_event IS '预警事件表';
COMMENT ON COLUMN alert_event.detail_json IS '数据快照';


-- ============================================
-- 12. adhoc_task 临时任务表
-- ============================================
CREATE TABLE adhoc_task (
    adhoc_task_id BIGSERIAL PRIMARY KEY,
    cycle_id BIGINT NOT NULL REFERENCES assessment_cycle(cycle_id),
    creator_org_id BIGINT NOT NULL REFERENCES org(org_id),
    scope_type adhoc_scope_type NOT NULL DEFAULT 'ALL_ORGS',
    indicator_id BIGINT REFERENCES indicator(indicator_id),
    task_title VARCHAR(200) NOT NULL,
    task_desc TEXT,
    open_at DATE,
    due_at DATE,
    include_in_alert BOOLEAN NOT NULL DEFAULT FALSE,
    require_indicator_report BOOLEAN NOT NULL DEFAULT FALSE,
    status adhoc_task_status NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_adhoc_cycle ON adhoc_task(cycle_id);
CREATE INDEX idx_adhoc_creator ON adhoc_task(creator_org_id);
CREATE INDEX idx_adhoc_status ON adhoc_task(status);

COMMENT ON TABLE adhoc_task IS '临时任务表';
COMMENT ON COLUMN adhoc_task.scope_type IS '范围类型: ALL_ORGS-所有组织, BY_DEPT_ISSUED_INDICATORS-按部门发出指标, CUSTOM-自定义';

-- 添加 progress_report 的外键约束
ALTER TABLE progress_report 
ADD CONSTRAINT fk_report_adhoc FOREIGN KEY (adhoc_task_id) REFERENCES adhoc_task(adhoc_task_id);

-- ============================================
-- 13. adhoc_task_target 临时任务-目标组织表
-- ============================================
CREATE TABLE adhoc_task_target (
    adhoc_task_id BIGINT NOT NULL REFERENCES adhoc_task(adhoc_task_id),
    target_org_id BIGINT NOT NULL REFERENCES org(org_id),
    PRIMARY KEY (adhoc_task_id, target_org_id)
);

CREATE INDEX idx_adhoc_target_org ON adhoc_task_target(target_org_id);

COMMENT ON TABLE adhoc_task_target IS '临时任务-目标组织关联表';

-- ============================================
-- 14. adhoc_task_indicator_map 临时任务-指标映射表
-- ============================================
CREATE TABLE adhoc_task_indicator_map (
    adhoc_task_id BIGINT NOT NULL REFERENCES adhoc_task(adhoc_task_id),
    indicator_id BIGINT NOT NULL REFERENCES indicator(indicator_id),
    PRIMARY KEY (adhoc_task_id, indicator_id)
);

CREATE INDEX idx_adhoc_indicator_map ON adhoc_task_indicator_map(indicator_id);

COMMENT ON TABLE adhoc_task_indicator_map IS '临时任务-指标映射表';

-- ============================================
-- 15. audit_log 操作日志表
-- ============================================
CREATE TABLE audit_log (
    log_id BIGSERIAL PRIMARY KEY,
    entity_type audit_entity_type NOT NULL,
    entity_id BIGINT NOT NULL,
    action audit_action NOT NULL,
    before_json JSONB,
    after_json JSONB,
    changed_fields JSONB,
    reason TEXT,
    actor_user_id BIGINT REFERENCES app_user(user_id),
    actor_org_id BIGINT REFERENCES org(org_id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_actor ON audit_log(actor_user_id);
CREATE INDEX idx_audit_action ON audit_log(action);
CREATE INDEX idx_audit_created ON audit_log(created_at);

COMMENT ON TABLE audit_log IS '操作日志表 - 记录所有关键对象的变更';


-- ============================================
-- 校验视图
-- ============================================

-- v_milestone_weight_sum: 校验每指标下里程碑权重合计=100%
CREATE VIEW v_milestone_weight_sum AS
SELECT 
    i.indicator_id,
    i.indicator_desc,
    COALESCE(SUM(m.weight_percent), 0) AS total_weight,
    CASE 
        WHEN COALESCE(SUM(m.weight_percent), 0) = 100 THEN 'OK'
        ELSE 'INVALID'
    END AS validation_status
FROM indicator i
LEFT JOIN milestone m ON i.indicator_id = m.indicator_id
WHERE i.status = 'ACTIVE'
GROUP BY i.indicator_id, i.indicator_desc;

COMMENT ON VIEW v_milestone_weight_sum IS '校验每指标下里程碑权重合计是否为100%';

-- v_indicator_latest_report: 获取各指标最新最终报告完成度
CREATE VIEW v_indicator_latest_report AS
SELECT DISTINCT ON (i.indicator_id)
    i.indicator_id,
    i.indicator_desc,
    i.target_org_id,
    pr.report_id,
    pr.percent_complete,
    pr.reported_at,
    pr.status AS report_status
FROM indicator i
LEFT JOIN progress_report pr ON i.indicator_id = pr.indicator_id 
    AND pr.is_final = TRUE 
    AND pr.status = 'APPROVED'
WHERE i.status = 'ACTIVE'
ORDER BY i.indicator_id, pr.reported_at DESC NULLS LAST;

COMMENT ON VIEW v_indicator_latest_report IS '获取各指标最新最终报告完成度';

-- v_overdue_milestones: 逾期里程碑视图
CREATE VIEW v_overdue_milestones AS
SELECT 
    m.milestone_id,
    m.milestone_name,
    m.due_date,
    m.status,
    i.indicator_id,
    i.indicator_desc,
    i.target_org_id,
    o.org_name AS target_org_name
FROM milestone m
JOIN indicator i ON m.indicator_id = i.indicator_id
JOIN org o ON i.target_org_id = o.org_id
WHERE m.due_date < CURRENT_DATE 
    AND m.status NOT IN ('COMPLETED', 'CANCELED')
    AND i.status = 'ACTIVE';

COMMENT ON VIEW v_overdue_milestones IS '逾期里程碑视图';

-- ============================================
-- 触发器函数: 更新 updated_at
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为所有需要的表添加触发器
CREATE TRIGGER trg_org_updated_at BEFORE UPDATE ON org FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_user_updated_at BEFORE UPDATE ON app_user FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_cycle_updated_at BEFORE UPDATE ON assessment_cycle FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_task_updated_at BEFORE UPDATE ON strategic_task FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_indicator_updated_at BEFORE UPDATE ON indicator FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_milestone_updated_at BEFORE UPDATE ON milestone FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_report_updated_at BEFORE UPDATE ON progress_report FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_alert_window_updated_at BEFORE UPDATE ON alert_window FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_alert_rule_updated_at BEFORE UPDATE ON alert_rule FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_alert_event_updated_at BEFORE UPDATE ON alert_event FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_adhoc_updated_at BEFORE UPDATE ON adhoc_task FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- ============================================
-- 初始数据
-- ============================================

-- 插入组织数据
INSERT INTO org (org_name, org_type, parent_org_id, sort_order) VALUES
('战略发展部', 'STRATEGY_DEPT', NULL, 1),
('教务处', 'FUNCTION_DEPT', NULL, 2),
('科研处', 'FUNCTION_DEPT', NULL, 3),
('人事处', 'FUNCTION_DEPT', NULL, 4),
('学生处', 'FUNCTION_DEPT', NULL, 5),
('计算机学院', 'COLLEGE', NULL, 10),
('商学院', 'COLLEGE', NULL, 11),
('外国语学院', 'COLLEGE', NULL, 12),
('艺术学院', 'COLLEGE', NULL, 13),
('机械工程学院', 'COLLEGE', NULL, 14);

-- 插入用户数据（密码为 123456 的 bcrypt 哈希）
-- 注意：实际生产环境应使用真实的密码哈希
INSERT INTO app_user (username, real_name, org_id, password_hash) VALUES
('zhanlue', '张战略', 1, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi'),
('jiaowu', '李教务', 2, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi'),
('keyan', '王科研', 3, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi'),
('renshi', '赵人事', 4, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi'),
('xuesheng', '钱学生', 5, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi'),
('computer', '孙计算机', 6, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi'),
('business', '周商学', 7, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi'),
('foreign', '吴外语', 8, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi'),
('art', '郑艺术', 9, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi'),
('mechanical', '冯机械', 10, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi');

-- 插入考核周期
INSERT INTO assessment_cycle (cycle_name, year, start_date, end_date, description) VALUES
('2025年度考核周期', 2025, '2025-01-01', '2025-12-31', '2025年度战略指标考核周期');

-- 插入预警规则
INSERT INTO alert_rule (cycle_id, name, severity, gap_threshold, is_enabled) VALUES
(1, '轻度预警', 'INFO', 10.00, TRUE),
(1, '中度预警', 'WARNING', 20.00, TRUE),
(1, '严重预警', 'CRITICAL', 30.00, TRUE);

-- 插入预警窗口
INSERT INTO alert_window (cycle_id, name, cutoff_date, is_default) VALUES
(1, 'Q1季度检查', '2025-03-31', FALSE),
(1, 'Q2季度检查', '2025-06-30', FALSE),
(1, 'Q3季度检查', '2025-09-30', FALSE),
(1, '年度终检', '2025-12-31', TRUE);

-- ============================================
-- 完成
-- ============================================
-- 数据库初始化完成
-- 版本: V0.5
-- 日期: 2025-11-08
