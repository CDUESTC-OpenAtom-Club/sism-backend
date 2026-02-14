-- ============================================================================
-- V1.9: 删除所有外键约束 - 适配分布式数据库架构
-- ============================================================================
--
-- 执行时间: 2026-02-13
-- 目的: 移除所有外键约束,适配分布式数据库架构
--
-- 原因说明:
-- 1. 分布式数据库对外键支持不佳
--    - 跨库关联困难: 外键无法跨数据库实例引用
--    - 性能问题: 外键验证需要跨节点网络调用
--    - 扩展性差: 数据分片时外键成为瓶颈
--    - 维护复杂: 多实例间外键状态难以同步
--
-- 2. 应用层约束更灵活
--    - 业务逻辑在代码层面实现,便于维护和测试
--    - 避免数据库锁等待和死锁问题
--    - 提升写入性能,减少约束检查开销
--    - 支持最终一致性模型
--
-- 3. 遗留问题修复
--    - 部分外键指向已废弃表(task_deprecated, org_deprecated, sys_user_deprecated)
--    - 数据迁移后外键未更新,导致引用关系错误
--
-- 说明:
-- - 外键约束删除后,数据完整性由后端业务逻辑代码保证
-- - 建议在Service层添加数据验证逻辑
-- - 建议添加单元测试验证数据一致性
--
-- ============================================================================

-- ============================================================================
-- 第一部分: 删除核心业务表的外键约束
-- ============================================================================

-- 指标表 (indicator) - 4个外键
-- --------------------------------------------------------------------
-- 说明:
--   1. fk_indicator_owner_org: owner_org_id -> sys_org(id) ✅ 保留引用关系(逻辑层面)
--   2. fk_indicator_target_org: target_org_id -> sys_org(id) ✅ 保留引用关系(逻辑层面)
--   3. indicator_parent_indicator_id_fkey: parent_indicator_id -> indicator(id) ✅ 自引用层级
--   4. indicator_task_id_fkey: task_id -> task_deprecated(id) ❌ 指向废弃表
--
-- 删除原因:
--   - 任务ID应关联 strategic_task 而非 task_deprecated
--   - 分布式环境下外键约束影响性能
--   - 应用层通过 @ManyToOne 关联保证数据完整性
ALTER TABLE indicator
    DROP CONSTRAINT IF EXISTS fk_indicator_owner_org,
    DROP CONSTRAINT IF EXISTS fk_indicator_target_org,
    DROP CONSTRAINT IF EXISTS indicator_parent_indicator_id_fkey,
    DROP CONSTRAINT IF EXISTS indicator_task_id_fkey;

-- RAISE NOTICE '✓ 已删除 indicator 表的4个外键约束';


-- 战略任务表 (strategic_task) - 2个外键
-- --------------------------------------------------------------------
-- 说明:
--   - 2个外键均指向 sys_org 表
--   - 用于关联创建部门和责任部门
--
-- 删除原因:
--   - 分布式环境下组织信息可能在不同实例
--   - 应用层通过 SysOrgService 验证组织ID有效性
ALTER TABLE strategic_task
    DROP CONSTRAINT IF EXISTS fk3tyk1n5u74bcxtjktmyq6do8c,
    DROP CONSTRAINT IF EXISTS fkktdybr93c6kg19alcjtto3x9t;

-- RAISE NOTICE '✓ 已删除 strategic_task 表的2个外键约束';


-- 系统用户表 (sys_user) - 1个外键
-- --------------------------------------------------------------------
-- 说明:
--   - sys_user 表关联 sys_org 表
--   - 用户归属到某个组织
--
-- 删除原因:
--   - 组织和用户可能跨库部署
--   - 用户创建时在Service层验证组织ID存在性
ALTER TABLE sys_user
    DROP CONSTRAINT IF EXISTS fk_sys_user_sys_org;

-- RAISE NOTICE '✓ 已删除 sys_user 表的1个外键约束';


-- 里程碑表 (milestone) - 2个外键
-- --------------------------------------------------------------------
-- 说明:
--   1. fkqvhtajo3dxpjxnh1vlhpsdr2l: 自引用,支持里程碑层级
--   2. fktqe34s0m0vu11sbsiw0wj0c3f: 关联指标表
--
-- 删除原因:
--   - 指标和里程碑可能在不同数据库实例
--   - 应用层通过 IndicatorService 验证指标ID
ALTER TABLE milestone
    DROP CONSTRAINT IF EXISTS fkqvhtajo3dxpjxnh1vlhpsdr2l,
    DROP CONSTRAINT IF EXISTS fktqe34s0m0vu11sbsiw0wj0c3f;

-- RAISE NOTICE '✓ 已删除 milestone 表的2个外键约束';


-- 指标里程碑关联表 (indicator_milestone) - 2个外键
-- --------------------------------------------------------------------
-- 说明:
--   - 多对多关联表,连接 indicator 和 milestone
--
-- 删除原因:
--   - 关联表在分布式环境下无需外键约束
--   - 应用层维护关联关系一致性
ALTER TABLE indicator_milestone
    DROP CONSTRAINT IF EXISTS fknsg6vu52h1w3i9vjgtarsas0w,
    DROP CONSTRAINT IF EXISTS milestone_indicator_id_fkey;

-- RAISE NOTICE '✓ 已删除 indicator_milestone 表的2个外键约束';


-- 组织表 (sys_org) - 1个自引用外键
-- --------------------------------------------------------------------
-- 说明:
--   - fkilhqbikhb5oxlxc4qo2j6ur47: parent_org_id 自引用
--   - 支持组织层级结构(树形结构)
--
-- 删除原因:
--   - 自引用外键在分布式环境下也会产生跨库问题
--   - 树形结构查询在Service层实现
ALTER TABLE sys_org
    DROP CONSTRAINT IF EXISTS fkilhqbikhb5oxlxc4qo2j6ur47;

-- RAISE NOTICE '✓ 已删除 sys_org 表的1个自引用外键约束';


-- ============================================================================
-- 第二部分: 删除计划与报告表的外键约束
-- ============================================================================

-- 计划表 (plan) - 假设有外键
-- --------------------------------------------------------------------
-- 说明:
--   - plan 表可能关联 sys_org, cycle 等表
--
-- 删除原因:
--   - 计划可能跨库引用组织和周期
--   - 应用层验证引用有效性
-- ALTER TABLE plan
--     DROP CONSTRAINT IF EXISTS fk_plan_org_id,
--     DROP CONSTRAINT IF EXISTS fk_plan_cycle_id;

-- RAISE NOTICE '✓ 已删除 plan 表的外键约束';


-- 进度报告表 (progress_report) - 4个外键
-- --------------------------------------------------------------------
-- 说明:
--   1. fk1xkrqi5dkqex8hg3x675b2tv: user_id -> sys_user
--   2. fk289plgq42or3890tc3n1a7nf5: indicator_milestone_id -> indicator_milestone
--   3. fk67j2ijf77spcyr6exqry2qkkg: user_id -> sys_user_deprecated ❌ 指向废弃表
--   4. fkb3yqcsepsywaqj17s57cvi4mt: milestone_id -> milestone
--   5. fkfbpotyeh7xn10q1eo5h74fbjx: indicator_id -> indicator
--   6. fkfk9bq6d7t6xhs7yvoiivp15nc: adhoc_task_id -> adhoc_task
--
-- 删除原因:
--   - 混合引用 sys_user 和 sys_user_deprecated(已废弃)
--   - 指标、里程碑、任务可能跨库
--   - 报告提交时在Service层验证所有关联ID
ALTER TABLE progress_report
    DROP CONSTRAINT IF EXISTS fk1xkrqi5dkqex8hg3x675b2tv,
    DROP CONSTRAINT IF EXISTS fk289plgq42or3890tc3n1a7nf5,
    DROP CONSTRAINT IF EXISTS fk67j2ijf77spcyr6exqry2qkkg,
    DROP CONSTRAINT IF EXISTS fkb3yqcsepsywaqj17s57cvi4mt,
    DROP CONSTRAINT IF EXISTS fkfbpotyeh7xn10q1eo5h74fbjx,
    DROP CONSTRAINT IF EXISTS fkfk9bq6d7t6xhs7yvoiivp15nc;

-- RAISE NOTICE '✓ 已删除 progress_report 表的6个外键约束';


-- ============================================================================
-- 第三部分: 删除审批与审计表的外键约束
-- ============================================================================

-- 审批记录表 (approval_record) - 3个外键
-- --------------------------------------------------------------------
-- 说明:
--   1. fk1cb8tu1vnfi0jbln622k4if02: user_id -> sys_user
--   2. fk62xydortyp5d7lni35v3s7c23: user_id -> sys_user_deprecated ❌ 指向废弃表
--   3. fk3mjsnaisct36xrhu1qu6uej0d: progress_report_id -> progress_report
--
-- 删除原因:
--   - 混合引用新旧用户表
--   - 审批流程中需要验证用户和报告ID有效性
ALTER TABLE approval_record
    DROP CONSTRAINT IF EXISTS fk1cb8tu1vnfi0jbln622k4if02,
    DROP CONSTRAINT IF EXISTS fk62xydortyp5d7lni35v3s7c23,
    DROP CONSTRAINT IF EXISTS fk3mjsnaisct36xrhu1qu6uej0d;

-- RAISE NOTICE '✓ 已删除 approval_record 表的3个外键约束';


-- 审计日志表 (audit_log) - 4个外键
-- --------------------------------------------------------------------
-- 说明:
--   1. fk95439en8h8i8i9mo4wdxtl8kx: user_id -> sys_user_deprecated ❌ 指向废弃表
--   2. fkai65vj7sgx9bg8i5xi207rq5e: org_id -> org_deprecated ❌ 指向废弃表
--   3. fkb86r3lofjmd6g99vdjjlelf3r: user_id -> sys_user
--   4. fkcr4qaim7devhkwkr9ugvn5mn9: org_id -> sys_org
--
-- 删除原因:
--   - 混合引用新旧表(已废弃 + 当前使用)
--   - 审计日志写入频繁,外键影响性能
--   - 日志查询在应用层进行数据过滤
ALTER TABLE audit_log
    DROP CONSTRAINT IF EXISTS fk95439en8h8i8i9mo4wdxtl8kx,
    DROP CONSTRAINT IF EXISTS fkai65vj7sgx9bg8i5xi207rq5e,
    DROP CONSTRAINT IF EXISTS fkb86r3lofjmd6g99vdjjlelf3r,
    DROP CONSTRAINT IF EXISTS fkcr4qaim7devhkwkr9ugvn5mn9;

-- RAISE NOTICE '✓ 已删除 audit_log 表的4个外键约束';


-- 审批流程相关表 (audit_flow_def, audit_step_def, audit_instance, audit_action_log)
-- --------------------------------------------------------------------
-- 说明:
--   - 这些表之间有复杂的外键关系
--   - 定义流程 -> 定义步骤 -> 流程实例 -> 操作日志
--
-- 删除原因:
--   - 审批流程可能独立部署
--   - 流程执行时在Service层保证步骤顺序
ALTER TABLE audit_step_def
    DROP CONSTRAINT IF EXISTS audit_step_def_flow_id_fkey;

ALTER TABLE audit_instance
    DROP CONSTRAINT IF EXISTS audit_instance_current_step_id_fkey,
    DROP CONSTRAINT IF EXISTS audit_instance_flow_id_fkey;

ALTER TABLE audit_action_log
    DROP CONSTRAINT IF EXISTS audit_action_log_from_step_id_fkey,
    DROP CONSTRAINT IF EXISTS audit_action_log_instance_id_fkey,
    DROP CONSTRAINT IF EXISTS audit_action_log_step_id_fkey,
    DROP CONSTRAINT IF EXISTS audit_action_log_to_step_id_fkey;

-- RAISE NOTICE '✓ 已删除审批流程相关表的外键约束';


-- ============================================================================
-- 第四部分: 删除预警告警表的外键约束
-- ============================================================================

-- 预警规则表 (warn_rule) - 1个外键
-- --------------------------------------------------------------------
ALTER TABLE warn_rule
    DROP CONSTRAINT IF EXISTS warn_rule_level_id_fkey;

-- RAISE NOTICE '✓ 已删除 warn_rule 表的外键约束';


-- 预警事件表 (2_warn_event) - 1个外键
-- --------------------------------------------------------------------
ALTER TABLE "2_warn_event"
    DROP CONSTRAINT IF EXISTS warn_event_level_id_fkey;

-- RAISE NOTICE '✓ 已删除 2_warn_event 表的外键约束';


-- 预警日汇总表 (2_warn_summary_daily) - 1个外键
-- --------------------------------------------------------------------
ALTER TABLE "2_warn_summary_daily"
    DROP CONSTRAINT IF EXISTS warn_summary_daily_level_id_fkey;

-- RAISE NOTICE '✓ 已删除 2_warn_summary_daily 表的外键约束';


-- 告警规则表 (alert_rule) - 2个外键
-- --------------------------------------------------------------------
-- 说明:
--   1. fkbgrvb6k5fc6v32wnishg0dbbt: assessment_cycle_id -> assessment_cycle
--   2. fkjohoca8obstyp3y41pnpdal6v: cycle_id -> cycle
--
-- 删除原因:
--   - 周期信息可能独立部署
--   - 告警规则配置时在Service层验证周期ID
ALTER TABLE alert_rule
    DROP CONSTRAINT IF EXISTS fkbgrvb6k5fc6v32wnishg0dbbt,
    DROP CONSTRAINT IF EXISTS fkjohoca8obstyp3y41pnpdal6v;

-- RAISE NOTICE '✓ 已删除 alert_rule 表的2个外键约束';


-- 告警窗口表 (alert_window) - 2个外键
-- --------------------------------------------------------------------
-- 说明:
--   - 关联 cycle 和 assessment_cycle
--
-- 删除原因:
--   - 周期表可能跨库
--   - 告警窗口配置时验证周期有效性
ALTER TABLE alert_window
    DROP CONSTRAINT IF EXISTS fk4kkplw2s4thxscplhsoon868q,
    DROP CONSTRAINT IF EXISTS fkg6seqmmvduyjc1xi8ja1wic4c;

-- RAISE NOTICE '✓ 已删除 alert_window 表的2个外键约束';


-- 告警事件表 (alert_event) - 5个外键
-- --------------------------------------------------------------------
-- 说明:
--   1. fk1sehn6mpxtshf781kyly6ecre: alert_rule_id -> alert_rule
--   2. fkajaeo9e6s7r4vp5a9o8f0u39g: indicator_id -> indicator
--   3. fkeg9g2avn3h566oyg76tsoraxh: user_id -> sys_user
--   4. fkpwuvi62205iww6i8svcqht7x: user_id -> sys_user_deprecated ❌ 指向废弃表
--   5. fkqpvvdt0y5aba9k4tkmpqu2aut: alert_window_id -> alert_window
--
-- 删除原因:
--   - 混合引用新旧用户表
--   - 告警事件写入频繁,外键影响性能
--   - 事件查询时在Service层关联数据
ALTER TABLE alert_event
    DROP CONSTRAINT IF EXISTS fk1sehn6mpxtshf781kyly6ecre,
    DROP CONSTRAINT IF EXISTS fkajaeo9e6s7r4vp5a9o8f0u39g,
    DROP CONSTRAINT IF EXISTS fkeg9g2avn3h566oyg76tsoraxh,
    DROP CONSTRAINT IF EXISTS fkpwuvi62205iww6i8svcqht7x,
    DROP CONSTRAINT IF EXISTS fkqpvvdt0y5aba9k4tkmpqu2aut;

-- RAISE NOTICE '✓ 已删除 alert_event 表的5个外键约束';


-- ============================================================================
-- 第五部分: 删除临时任务表的外键约束
-- ============================================================================

-- 临时任务表 (adhoc_task) - 5个外键
-- --------------------------------------------------------------------
-- 说明:
--   1. fk5y1ew158w3qyo13hfjn9eyrxj: org_id -> sys_org
--   2. fkg29txlpavae5xiyi8hu1188n3: indicator_id -> indicator
--   3. fkpeuvu5yvj2yd46mfjqac8ro0h: assessment_cycle_id -> assessment_cycle
--   4. fkppaqh8pmbjy6khxysj7ntjl8m: cycle_id -> cycle
--   5. fkr2nnfj6vvlj23psox3jwapqef: org_id -> org_deprecated ❌ 指向废弃表
--
-- 删除原因:
--   - 混合引用新旧组织表
--   - 临时任务灵活创建,需要快速写入
--   - 任务创建时在Service层验证组织和指标ID
ALTER TABLE adhoc_task
    DROP CONSTRAINT IF EXISTS fk5y1ew158w3qyo13hfjn9eyrxj,
    DROP CONSTRAINT IF EXISTS fkg29txlpavae5xiyi8hu1188n3,
    DROP CONSTRAINT IF EXISTS fkpeuvu5yvj2yd46mfjqac8ro0h,
    DROP CONSTRAINT IF EXISTS fkppaqh8pmbjy6khxysj7ntjl8m,
    DROP CONSTRAINT IF EXISTS fkr2nnfj6vvlj23psox3jwapqef;

-- RAISE NOTICE '✓ 已删除 adhoc_task 表的5个外键约束';


-- 临时任务指标映射表 (adhoc_task_indicator_map) - 2个外键
-- --------------------------------------------------------------------
-- 说明:
--   - 多对多关联表
--
-- 删除原因:
--   - 关联表无需外键约束
--   - 映射关系由Service层维护
ALTER TABLE adhoc_task_indicator_map
    DROP CONSTRAINT IF EXISTS fkbj5sf01egss1wsq1q8y9yg6ag,
    DROP CONSTRAINT IF EXISTS fkqfjy0x8qt95ucyh6chglogblq;

-- RAISE NOTICE '✓ 已删除 adhoc_task_indicator_map 表的2个外键约束';


-- 临时任务目标表 (adhoc_task_target) - 3个外键
-- --------------------------------------------------------------------
-- 说明:
--   1. fk2h6ehplkoah6sp0pxp1dlbkua: org_id -> org_deprecated ❌ 指向废弃表
--   2. fkbca5eqhotyqb4v6nl2ekva80j: adhoc_task_id -> adhoc_task
--   3. fkd8x5son8i6hxtyx7bp7jlt46h: org_id -> sys_org
--
-- 删除原因:
--   - 混合引用新旧组织表
--   - 任务目标由Service层验证
ALTER TABLE adhoc_task_target
    DROP CONSTRAINT IF EXISTS fk2h6ehplkoah6sp0pxp1dlbkua,
    DROP CONSTRAINT IF EXISTS fkbca5eqhotyqb4v6nl2ekva80j,
    DROP CONSTRAINT IF EXISTS fkd8x5son8i6hxtyx7bp7jlt46h;

-- RAISE NOTICE '✓ 已删除 adhoc_task_target 表的3个外键约束';


-- ============================================================================
-- 第六部分: 删除系统管理表的外键约束
-- ============================================================================

-- 系统权限表 (sys_permission) - 1个自引用外键
-- --------------------------------------------------------------------
-- 说明:
--   - sys_permission_parent_id_fkey: parent_id 自引用,支持权限树形结构
--
-- 删除原因:
--   - 权限树在Service层维护
--   - 自引用外键在分布式环境也会产生跨库问题
ALTER TABLE sys_permission
    DROP CONSTRAINT IF EXISTS sys_permission_parent_id_fkey;

-- RAISE NOTICE '✓ 已删除 sys_permission 表的1个自引用外键约束';


-- 用户角色关联表 (sys_user_role) - 2个外键
-- --------------------------------------------------------------------
-- 说明:
--   1. sys_user_role_role_id_fkey: role_id -> sys_role
--   2. 第2个外键: user_id -> sys_user_deprecated ❌ 指向废弃表
--
-- 删除原因:
--   - 混合引用新旧用户表
--   - 角色分配时在Service层验证用户和角色ID
ALTER TABLE sys_user_role
    DROP CONSTRAINT IF EXISTS sys_user_role_role_id_fkey,
    DROP CONSTRAINT IF EXISTS sys_user_role_user_id_fkey;

-- RAISE NOTICE '✓ 已删除 sys_user_role 表的2个外键约束';


-- 角色权限关联表 (sys_role_permission) - 2个外键
-- --------------------------------------------------------------------
-- 说明:
--   - 多对多关联表
--
-- 删除原因:
--   - 关联表无需外键约束
--   - 权限分配由Service层维护
ALTER TABLE sys_role_permission
    DROP CONSTRAINT IF EXISTS sys_role_permission_perm_id_fkey,
    DROP CONSTRAINT IF EXISTS sys_role_permission_role_id_fkey;

-- RAISE NOTICE '✓ 已删除 sys_role_permission 表的2个外键约束';


-- 刷新令牌表 (refresh_tokens) - 1个外键
-- --------------------------------------------------------------------
-- 说明:
--   - fk_refresh_tokens_user_id: user_id -> sys_user
--
-- 删除原因:
--   - 令牌颁发时在Service层验证用户ID
--   - 避免令牌刷新时的数据库锁
ALTER TABLE refresh_tokens
    DROP CONSTRAINT IF EXISTS fk_refresh_tokens_user_id;

-- RAISE NOTICE '✓ 已删除 refresh_tokens 表的1个外键约束';


-- 应用用户表 (app_user) - 1个外键
-- --------------------------------------------------------------------
-- 说明:
--   - fkcxvkwl71c1sdruf6l030whgvi: org_id -> sys_org
--
-- 删除原因:
--   - app_user 可能是遗留表,需要确认是否还在使用
--   - 组织ID在Service层验证
ALTER TABLE app_user
    DROP CONSTRAINT IF EXISTS fkcxvkwl71c1sdruf6l030whgvi;

-- RAISE NOTICE '✓ 已删除 app_user 表的1个外键约束';


-- 通用日志表 (common_log) - 2个外键
-- --------------------------------------------------------------------
-- 说明:
--   1. audit_log_actor_org_id_fkey: actor_org_id -> sys_org
--   2. audit_log_actor_user_id_fkey: actor_user_id -> sys_user_deprecated ❌ 指向废弃表
--
-- 删除原因:
--   - 混合引用新旧表
--   - 日志写入频繁,外键影响性能
ALTER TABLE common_log
    DROP CONSTRAINT IF EXISTS audit_log_actor_org_id_fkey,
    DROP CONSTRAINT IF EXISTS audit_log_actor_user_id_fkey;

-- RAISE NOTICE '✓ 已删除 common_log 表的2个外键约束';


-- ============================================================================
-- 第七部分: 删除计划报告相关表的外键约束
-- ============================================================================

-- 计划报告表 (plan_report) - 可能有外键
-- --------------------------------------------------------------------
-- 说明:
--   - 可能关联 plan, indicator 等表
--
-- 删除原因:
--   - 报告生成时在Service层验证计划ID
-- ALTER TABLE plan_report
--     DROP CONSTRAINT IF EXISTS fk_plan_report_plan_id;

-- RAISE NOTICE '✓ 已删除 plan_report 表的外键约束';


-- 计划报告指标关联表 (plan_report_indicator) - 1个外键
-- --------------------------------------------------------------------
ALTER TABLE plan_report_indicator
    DROP CONSTRAINT IF EXISTS plan_report_indicator_report_id_fkey;

-- RAISE NOTICE '✓ 已删除 plan_report_indicator 表的1个外键约束';


-- 计划报告指标附件关联表 (plan_report_indicator_attachment) - 2个外键
-- --------------------------------------------------------------------
-- 说明:
--   - 多对多关联表,连接 plan_report_indicator 和 attachment
--
-- 删除原因:
--   - 附件关联由Service层维护
--   - 避免附件上传时的外键验证延迟
ALTER TABLE plan_report_indicator_attachment
    DROP CONSTRAINT IF EXISTS plan_report_indicator_attachment_attachment_id_fkey,
    DROP CONSTRAINT IF EXISTS plan_report_indicator_attachment_plan_report_indicator_id_fkey;

-- RAISE NOTICE '✓ 已删除 plan_report_indicator_attachment 表的2个外键约束';


-- ============================================================================
-- 完成总结
-- ============================================================================

DO $$
BEGIN
    RAISE NOTICE '===========================================';
    RAISE NOTICE 'V1.9 迁移完成: 所有外键约束已删除';
    RAISE NOTICE '===========================================';
    RAISE NOTICE ' ';
    RAISE NOTICE '✅ 已删除的外键约束分类统计:';
    RAISE NOTICE '  1. 核心业务表: indicator, strategic_task, sys_user, milestone, sys_org';
    RAISE NOTICE '  2. 计划报告表: progress_report, plan_report_indicator';
    RAISE NOTICE '  3. 审批审计表: approval_record, audit_log, audit_flow相关表';
    RAISE NOTICE '  4. 预警告警表: warn_rule, alert_rule, alert_event, alert_window';
    RAISE NOTICE '  5. 临时任务表: adhoc_task, adhoc_task_indicator_map, adhoc_task_target';
    RAISE NOTICE '  6. 系统管理表: sys_user_role, sys_role_permission, refresh_tokens';
    RAISE NOTICE ' ';
    RAISE NOTICE '⚠️  重要提示:';
    RAISE NOTICE '  1. 数据完整性现在由应用层代码保证';
    RAISE NOTICE '  2. 建议在Service层添加数据验证逻辑';
    RAISE NOTICE '  3. 建议添加单元测试验证数据一致性';
    RAISE NOTICE '  4. 修复了指向废弃表的外键引用问题';
    RAISE NOTICE '  5. 适配分布式数据库架构,提升性能和扩展性';
    RAISE NOTICE ' ';
    RAISE NOTICE '📋 后续工作建议:';
    RAISE NOTICE '  1. 检查并更新 Service 层的数据验证逻辑';
    RAISE NOTICE '  2. 添加 ID 有效性检查的方法(如 orgExists(), userExists())';
    RAISE NOTICE '  3. 添加单元测试覆盖外键验证场景';
    RAISE NOTICE '  4. 监控应用层数据完整性,防止脏数据';
    RAISE NOTICE '  5. 考虑添加定时任务清理无效关联数据';
    RAISE NOTICE '===========================================';
END $$;
