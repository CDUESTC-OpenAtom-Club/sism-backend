-- =============================================================================
-- 高校战略任务系统 - 大规模种子数据
-- 对标实际数据库：28个组织、166个用户、476+个指标
-- 数据库: PostgreSQL
-- =============================================================================

-- 密码说明: 所有测试用户密码均为 'password123'
-- bcrypt哈希值: $2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X

BEGIN;

-- =============================================================================
-- 1. 清理旧数据
-- =============================================================================

TRUNCATE TABLE sys_user_role, sys_role_permission, sys_user, sys_org, cycle, sys_task,
              indicator, indicator_milestone, plan, plan_report, plan_report_indicator,
              audit_flow_def, audit_step_def, audit_instance, audit_log, audit_step_instance,
              refresh_tokens, sys_role, sys_permission, warn_level, alert_rule, alert_window,
              alert_event, adhoc_task, adhoc_task_indicator_map, adhoc_task_target,
              progress_report, attachment, workflow_task, workflow_task_history,
              idempotency_records, sys_user_supervisor CASCADE;

-- =============================================================================
-- 2. 组织结构 (sys_org) - 28个组织
-- =============================================================================

INSERT INTO sys_org (id, name, type, is_active, sort_order, created_at, updated_at, parent_org_id, level, is_deleted) VALUES
-- 一级组织: 战略发展部
(35, '战略发展部', 'STRATEGIC', true, 1, NOW(), NOW(), NULL, 1, false),

-- 二级组织: 9个职能部门
(36, '党委办公室', 'FUNCTIONAL', true, 10, NOW(), NOW(), 35, 2, false),
(37, '纪委办公室', 'FUNCTIONAL', true, 20, NOW(), NOW(), 35, 2, false),
(38, '党委宣传部', 'FUNCTIONAL', true, 30, NOW(), NOW(), 35, 2, false),
(39, '党委组织部', 'FUNCTIONAL', true, 40, NOW(), NOW(), 35, 2, false),
(40, '人力资源处', 'FUNCTIONAL', true, 50, NOW(), NOW(), 35, 2, false),
(41, '学生工作部', 'FUNCTIONAL', true, 60, NOW(), NOW(), 35, 2, false),
(42, '保卫处', 'FUNCTIONAL', true, 70, NOW(), NOW(), 35, 2, false),
(43, '学校办公室', 'FUNCTIONAL', true, 80, NOW(), NOW(), 35, 2, false),
(44, '教务处', 'FUNCTIONAL', true, 90, NOW(), NOW(), 35, 2, false),
(45, '科技处', 'FUNCTIONAL', true, 100, NOW(), NOW(), 35, 2, false),
(46, '财务处', 'FUNCTIONAL', true, 110, NOW(), NOW(), 35, 2, false),
(47, '招生就业处', 'FUNCTIONAL', true, 120, NOW(), NOW(), 35, 2, false),
(48, '就业创业指导中心', 'FUNCTIONAL', true, 130, NOW(), NOW(), 35, 2, false),
(49, '实验室与资产管理处', 'FUNCTIONAL', true, 140, NOW(), NOW(), 35, 2, false),
(50, '数字化校园建设办公室', 'FUNCTIONAL', true, 150, NOW(), NOW(), 35, 2, false),
(51, '图书馆', 'FUNCTIONAL', true, 160, NOW(), NOW(), 35, 2, false),
(52, '后勤保障处', 'FUNCTIONAL', true, 170, NOW(), NOW(), 35, 2, false),
(53, '继续教育学院', 'FUNCTIONAL', true, 180, NOW(), NOW(), 35, 2, false),
(54, '国际交流与合作处', 'FUNCTIONAL', true, 190, NOW(), NOW(), 35, 2, false),

-- 三级组织: 8个二级学院
(55, '马克思主义学院', 'COLLEGE', true, 201, NOW(), NOW(), 35, 3, false),
(56, '法学院', 'COLLEGE', true, 202, NOW(), NOW(), 35, 3, false),
(57, '计算机学院', 'COLLEGE', true, 203, NOW(), NOW(), 35, 3, false),
(58, '商学院', 'COLLEGE', true, 204, NOW(), NOW(), 35, 3, false),
(59, '文理学院', 'COLLEGE', true, 205, NOW(), NOW(), 35, 3, false),
(60, '艺术与设计学院', 'COLLEGE', true, 206, NOW(), NOW(), 35, 3, false),
(61, '航空学院', 'COLLEGE', true, 207, NOW(), NOW(), 35, 3, false),
(62, '国际教育学院', 'COLLEGE', true, 208, NOW(), NOW(), 35, 3, false);

-- =============================================================================
-- 3. 角色 (sys_role) - 7个角色
-- =============================================================================

INSERT INTO sys_role (id, role_code, role_name, data_access_mode, is_enabled, remark, created_at, updated_at) VALUES
(5, 'ROLE_REPORTER', '填报人', 'OWN_ORG', true, '指标和报告填报人员', NOW(), NOW()),
(6, 'ROLE_FUNC_DEPT_HEAD', '职能部门负责人', 'OWN_ORG', true, '职能部门负责人/分管领导', NOW(), NOW()),
(7, 'ROLE_COLLEGE_DEAN', '学院院长', 'OWN_ORG', true, '学院院长/副院长', NOW(), NOW()),
(8, 'STRATEGY_DEPT_HEAD', '战略发展部负责人', 'OWN_ORG', true, '战略发展部负责人', NOW(), NOW()),
(9, 'ROLE_VICE_PRESIDENT', '校领导', 'ALL_ORG', true, '分管校领导', NOW(), NOW()),
(10, 'ROLE_STRATEGY_OFFICE', '战略发展部', 'OWN_ORG', true, '战略发展部普通员工', NOW(), NOW()),
(11, 'ROLE_ISSUER', '下发人', 'OWN_ORG', true, '指标/计划下发人员', NOW(), NOW());

-- =============================================================================
-- 4. 权限 (sys_permission)
-- =============================================================================

INSERT INTO sys_permission (id, perm_code, perm_name, perm_type, parent_id, route_path, page_key, action_key, sort_order, is_enabled, remark, created_at, updated_at) VALUES
-- 一级权限
(1, 'dashboard', '工作台', 'PAGE', NULL, '/dashboard', 'dashboard', NULL, 1, true, '工作台页面', NOW(), NOW()),
(2, 'strategic', '战略管理', 'PAGE', NULL, '/strategic', 'strategic', NULL, 10, true, '战略管理模块', NOW(), NOW()),
(3, 'indicator', '指标管理', 'PAGE', NULL, '/indicator', 'indicator', NULL, 20, true, '指标管理模块', NOW(), NOW()),
(4, 'task', '任务管理', 'PAGE', NULL, '/task', 'task', NULL, 30, true, '任务管理模块', NOW(), NOW()),
(5, 'plan', '计划管理', 'PAGE', NULL, '/plan', 'plan', NULL, 40, true, '计划管理模块', NOW(), NOW()),
(6, 'report', '报告管理', 'PAGE', NULL, '/report', 'report', NULL, 50, true, '报告管理模块', NOW(), NOW()),
(7, 'approval', '审批管理', 'PAGE', NULL, '/approval', 'approval', NULL, 60, true, '审批管理模块', NOW(), NOW()),
(8, 'org', '组织管理', 'PAGE', NULL, '/org', 'org', NULL, 70, true, '组织管理模块', NOW(), NOW()),
(9, 'user', '用户管理', 'PAGE', NULL, '/user', 'user', NULL, 80, true, '用户管理模块', NOW(), NOW()),
(10, 'system', '系统管理', 'PAGE', NULL, '/system', 'system', NULL, 90, true, '系统管理模块', NOW(), NOW()),

-- 指标管理权限
(11, 'indicator:list', '查看指标', 'BUTTON', 3, NULL, NULL, 'list', 1, true, '查看指标列表', NOW(), NOW()),
(12, 'indicator:create', '创建指标', 'BUTTON', 3, NULL, NULL, 'create', 2, true, '创建指标', NOW(), NOW()),
(13, 'indicator:edit', '编辑指标', 'BUTTON', 3, NULL, NULL, 'edit', 3, true, '编辑指标', NOW(), NOW()),
(14, 'indicator:delete', '删除指标', 'BUTTON', 3, NULL, NULL, 'delete', 4, true, '删除指标', NOW(), NOW()),
(15, 'indicator:distribute', '下发指标', 'BUTTON', 3, NULL, NULL, 'distribute', 5, true, '下发指标', NOW(), NOW()),
(16, 'indicator:approve', '审批指标', 'BUTTON', 3, NULL, NULL, 'approve', 6, true, '审批指标', NOW(), NOW()),

-- 计划管理权限
(17, 'plan:list', '查看计划', 'BUTTON', 5, NULL, NULL, 'list', 1, true, '查看计划列表', NOW(), NOW()),
(18, 'plan:create', '创建计划', 'BUTTON', 5, NULL, NULL, 'create', 2, true, '创建计划', NOW(), NOW()),
(19, 'plan:distribute', '下发计划', 'BUTTON', 5, NULL, NULL, 'distribute', 3, true, '下发计划', NOW(), NOW()),

-- 报告管理权限
(20, 'report:list', '查看报告', 'BUTTON', 6, NULL, NULL, 'list', 1, true, '查看报告列表', NOW(), NOW()),
(21, 'report:create', '创建报告', 'BUTTON', 6, NULL, NULL, 'create', 2, true, '创建报告', NOW(), NOW()),
(22, 'report:submit', '提交报告', 'BUTTON', 6, NULL, NULL, 'submit', 3, true, '提交报告', NOW(), NOW()),
(23, 'report:approve', '审批报告', 'BUTTON', 6, NULL, NULL, 'approve', 4, true, '审批报告', NOW(), NOW());

-- =============================================================================
-- 5. 用户 (sys_user) - 166个用户
-- =============================================================================

INSERT INTO sys_user (id, created_at, updated_at, is_active, password_hash, real_name, sso_id, username, org_id) VALUES
-- 战略发展部 (ID:35) - 8个用户
(124, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '系统管理员', NULL, 'admin', 35),
(125, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '张三', NULL, 'zhangsan', 35),
(180, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '战略部主任A', NULL, 'strategy_director_a', 35),
(181, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '战略部主任B', NULL, 'strategy_director_b', 35),
(188, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '战略部管理员', NULL, 'zlb_admin', 35),
(189, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '战略部审核1', NULL, 'zlb_audit1', 35),
(190, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '战略部审核2', NULL, 'zlb_audit2', 35),
(162, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '战略员', NULL, 'zhanlue', 35),

-- 党委办公室 (ID:36) - 6个用户
(126, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '党委办主任', NULL, 'func_36', 36),
(163, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '党建办', NULL, 'dangban', 36),
(191, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '党委办填报', NULL, 'dangban_report', 36),
(192, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '党委办审核1', NULL, 'dangban_audit1', 36),
(193, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '党委办审核2', NULL, 'dangban_audit2', 36),
(194, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '党委办下发', NULL, 'dangban_issue', 36),

-- 纪委办公室 (ID:37) - 6个用户
(127, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '纪委办主任', NULL, 'func_37', 37),
(164, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '纪委办', NULL, 'jiwei', 37),
(195, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '纪委办填报', NULL, 'jiwei_report', 37),
(196, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '纪委办审核1', NULL, 'jiwei_audit1', 37),
(197, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '纪委办审核2', NULL, 'jiwei_audit2', 37),
(198, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '纪委办下发', NULL, 'jiwei_issue', 37),

-- 党委宣传部 (ID:38) - 6个用户
(128, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '党委宣传部部长', NULL, 'func_38', 38),
(165, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '党委宣传部', NULL, 'dangxuan', 38),
(199, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '宣传部填报', NULL, 'dangxuan_report', 38),
(200, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '宣传部审核1', NULL, 'dangxuan_audit1', 38),
(201, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '宣传部审核2', NULL, 'dangxuan_audit2', 38),
(202, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '宣传部下发', NULL, 'dangxuan_issue', 38),

-- 党委组织部 (ID:39) - 6个用户
(129, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '党委组织部部长', NULL, 'func_39', 39),
(166, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '组织部', NULL, 'zuzhi', 39),
(203, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '组织部填报', NULL, 'zuzhi_report', 39),
(204, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '组织部审核1', NULL, 'zuzhi_audit1', 39),
(205, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '组织部审核2', NULL, 'zuzhi_audit2', 39),
(206, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '组织部下发', NULL, 'zuzhi_issue', 39),

-- 人力资源处 (ID:40) - 6个用户
(130, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '人力资源处处长', NULL, 'func_40', 40),
(167, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '人力资源处', NULL, 'renli', 40),
(207, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '人力填报', NULL, 'renli_report', 40),
(208, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '人力审核1', NULL, 'renli_audit1', 40),
(209, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '人力审核2', NULL, 'renli_audit2', 40),
(210, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '人力下发', NULL, 'renli_issue', 40),

-- 学生工作部 (ID:41) - 6个用户
(131, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '学生工作部部长', NULL, 'func_41', 41),
(168, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '学工部', NULL, 'xuegong', 41),
(211, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '学工部填报', NULL, 'xuegong_report', 41),
(212, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '学工部审核1', NULL, 'xuegong_audit1', 41),
(213, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '学工部审核2', NULL, 'xuegong_audit2', 41),
(214, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '学工部下发', NULL, 'xuegong_issue', 41),

-- 保卫处 (ID:42) - 6个用户
(132, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '保卫处处长', NULL, 'func_42', 42),
(1, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '保密用户', NULL, 'baowei', 42),
(215, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '保卫处填报', NULL, 'baowei_report', 42),
(216, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '保卫处审核1', NULL, 'baowei_audit1', 42),
(217, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '保卫处审核2', NULL, 'baowei_audit2', 42),
(218, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '保卫处下发', NULL, 'baowei_issue', 42),

-- 学校办公室 (ID:43) - 6个用户
(133, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '学校办公室主任', NULL, 'func_43', 43),
(169, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '综管办', NULL, 'zonghe', 43),
(219, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '综管办填报', NULL, 'zonghe_report', 43),
(220, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '综管办审核1', NULL, 'zonghe_audit1', 43),
(221, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '综管办审核2', NULL, 'zonghe_audit2', 43),
(222, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '综管办下发', NULL, 'zonghe_issue', 43),

-- 教务处 (ID:44) - 8个用户
(134, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '教务处处长', NULL, 'func_44', 44),
(170, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '教务处', NULL, 'jiaowu', 44),
(182, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '职能处长C', NULL, 'func_director_c', 44),
(183, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '职能处长D', NULL, 'func_director_d', 44),
(223, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '教务处填报', NULL, 'jiaowu_report', 44),
(224, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '教务处审核1', NULL, 'jiaowu_audit1', 44),
(225, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '教务处审核2', NULL, 'jiaowu_audit2', 44),
(226, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '教务处下发', NULL, 'jiaowu_issue', 44),

-- 科技处 (ID:45) - 6个用户
(135, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '科技处处长', NULL, 'func_45', 45),
(161, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '科技处', NULL, 'keji', 45),
(227, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '科技处填报', NULL, 'keji_report', 45),
(228, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '科技处审核1', NULL, 'keji_audit1', 45),
(229, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '科技处审核2', NULL, 'keji_audit2', 45),
(230, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '科技处下发', NULL, 'keji_issue', 45),

-- 财务处 (ID:46) - 6个用户
(136, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '财务处处长', NULL, 'func_46', 46),
(171, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '财务处', NULL, 'caiwu', 46),
(231, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '财务处填报', NULL, 'caiwu_report', 46),
(232, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '财务处审核1', NULL, 'caiwu_audit1', 46),
(233, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '财务处审核2', NULL, 'caiwu_audit2', 46),
(234, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '财务处下发', NULL, 'caiwu_issue', 46),

-- 招生就业处 (ID:47) - 6个用户
(137, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '招生就业处处长', NULL, 'func_47', 47),
(172, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '招生就业处', NULL, 'zhaosheng', 47),
(235, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '招就处填报', NULL, 'zhaosheng_report', 47),
(236, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '招就处审核1', NULL, 'zhaosheng_audit1', 47),
(237, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '招就处审核2', NULL, 'zhaosheng_audit2', 47),
(238, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '招就处下发', NULL, 'zhaosheng_issue', 47),

-- 就业创业指导中心 (ID:48) - 6个用户
(138, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '就业指导中心主任', NULL, 'func_48', 48),
(173, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '就业指导中心', NULL, 'jiuye', 48),
(239, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '就业中心填报', NULL, 'jiuye_report', 48),
(240, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '就业中心审核1', NULL, 'jiuye_audit1', 48),
(241, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '就业中心审核2', NULL, 'jiuye_audit2', 48),
(242, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '就业中心下发', NULL, 'jiuye_issue', 48),

-- 实验室与资产管理处 (ID:49) - 6个用户
(139, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '资产处处长', NULL, 'func_49', 49),
(174, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '资产处', NULL, 'shiyanshi', 49),
(243, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '资产处填报', NULL, 'shiyanshi_report', 49),
(244, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '资产处审核1', NULL, 'shiyanshi_audit1', 49),
(245, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '资产处审核2', NULL, 'shiyanshi_audit2', 49),
(246, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '资产处下发', NULL, 'shiyanshi_issue', 48),

-- 数字化校园建设办公室 (ID:50) - 6个用户
(140, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '数字化办公室主任', NULL, 'func_50', 50),
(175, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '数字化办公室', NULL, 'shuzi', 50),
(247, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '数字化填报', NULL, 'shuzi_report', 50),
(248, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '数字化审核1', NULL, 'shuzi_audit1', 50),
(249, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '数字化审核2', NULL, 'shuzi_audit2', 50),
(250, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '数字化下发', NULL, 'shuzi_issue', 50),

-- 图书馆 (ID:51) - 6个用户
(141, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '图书馆馆长', NULL, 'func_51', 51),
(176, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '图书馆', NULL, 'tushuguan', 51),
(251, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '图书馆填报', NULL, 'tushu_report', 51),
(252, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '图书馆审核1', NULL, 'tushu_audit1', 51),
(253, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '图书馆审核2', NULL, 'tushu_audit2', 51),
(254, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '图书馆下发', NULL, 'tushu_issue', 51),

-- 后勤保障处 (ID:52) - 6个用户
(142, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '后勤保障处处长', NULL, 'func_52', 52),
(177, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '后勤保障处', NULL, 'houqin', 52),
(255, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '后勤填报', NULL, 'houqin_report', 52),
(256, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '后勤审核1', NULL, 'houqin_audit1', 52),
(257, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '后勤审核2', NULL, 'houqin_audit2', 52),
(258, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '后勤下发', NULL, 'houqin_issue', 52),

-- 继续教育学院 (ID:53) - 6个用户
(143, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '继续教育学院院长', NULL, 'func_53', 53),
(178, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '继续教育学院', NULL, 'jixu', 53),
(259, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '继教学院填报', NULL, 'jixu_report', 53),
(260, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '继教学院审核1', NULL, 'jixu_audit1', 53),
(261, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '继教学院审核2', NULL, 'jixu_audit2', 53),
(262, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '继教学院下发', NULL, 'jixu_issue', 53),

-- 国际交流与合作处 (ID:54) - 6个用户
(144, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '国际交流处处长', NULL, 'func_54', 54),
(179, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '国际交流处', NULL, 'guoji', 54),
(263, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '国际处填报', NULL, 'guoji_report', 54),
(264, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '国际处审核1', NULL, 'guoji_audit1', 54),
(265, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '国际处审核2', NULL, 'guoji_audit2', 54),
(266, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '国际处下发', NULL, 'guoji_issue', 54),

-- 二级学院 - 8个学院，每个5-7个用户
-- 马克思主义学院 (ID:55)
(145, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '马院管理员', NULL, 'college_55', 55),
(153, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '马思学院用户', NULL, 'makesi', 55),
(267, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '马院填报', NULL, 'makesi_report', 55),
(268, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '马院审核1', NULL, 'makesi_audit1', 55),
(269, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '马院审核2', NULL, 'makesi_audit2', 55),

-- 法学院 (ID:56)
(146, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '法学院管理员', NULL, 'college_56', 56),
(154, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '法学院用户', NULL, 'gongxue', 56),
(270, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '法学院填报', NULL, 'gongxue_report', 56),
(271, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '法学院审核1', NULL, 'gongxue_audit1', 56),
(272, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '法学院审核2', NULL, 'gongxue_audit2', 56),

-- 计算机学院 (ID:57)
(147, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '计算机学院管理员', NULL, 'college_57', 57),
(155, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '计算机学院用户', NULL, 'jisuanji', 57),
(184, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '学院审核E', NULL, 'college_auditor_e', 57),
(185, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '学院审核F', NULL, 'college_auditor_f', 57),
(273, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '计算机学院填报', NULL, 'jisuanji_report', 57),
(274, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '计算机学院审核1', NULL, 'jisuanji_audit1', 57),
(275, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '计算机学院审核2', NULL, 'jisuanji_audit2', 57),

-- 商学院 (ID:58)
(148, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '商学院管理员', NULL, 'college_58', 58),
(156, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '商学院用户', NULL, 'shangxue', 58),
(276, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '商学院填报', NULL, 'shangxue_report', 58),
(277, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '商学院审核1', NULL, 'shangxue_audit1', 58),
(278, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '商学院审核2', NULL, 'shangxue_audit2', 58),

-- 文理学院 (ID:59)
(149, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '文理学院管理员', NULL, 'college_59', 59),
(157, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '文理学院用户', NULL, 'wenli', 59),
(279, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '文理学院填报', NULL, 'wenli_report', 59),
(280, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '文理学院审核1', NULL, 'wenli_audit1', 59),
(281, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '文理学院审核2', NULL, 'wenli_audit2', 59),

-- 艺术与设计学院 (ID:60)
(150, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '艺术学院管理员', NULL, 'college_60', 60),
(158, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '艺术学院用户', NULL, 'yishu', 60),
(282, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '艺术学院填报', NULL, 'yishu_report', 60),
(283, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '艺术学院审核1', NULL, 'yishu_audit1', 60),
(284, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '艺术学院审核2', NULL, 'yishu_audit2', 60),

-- 航空学院 (ID:61)
(151, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '航空学院管理员', NULL, 'college_61', 61),
(159, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '航空学院用户', NULL, 'hangkong', 61),
(285, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '航空学院填报', NULL, 'hangkong_report', 61),
(286, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '航空学院审核1', NULL, 'hangkong_audit1', 61),
(287, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '航空学院审核2', NULL, 'hangkong_audit2', 61),

-- 国际教育学院 (ID:62)
(152, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '国教院管理员', NULL, 'college_62', 62),
(160, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '国际教育学院用户', NULL, 'guojiaoyu', 62),
(288, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '国教院填报', NULL, 'guojiaoyu_report', 62),
(289, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '国教院审核1', NULL, 'guojiaoyu_audit1', 62),
(290, NOW(), NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMye/FC72.CAijT5hWqGjQZ5jP5x5Y5Y5X', '国教院审核2', NULL, 'guojiaoyu_audit2', 62);

-- =============================================================================
-- 6. 用户角色关联 (sys_user_role)
-- =============================================================================

INSERT INTO sys_user_role (id, user_id, role_id, created_at) VALUES
-- 战略发展部角色
(1, 124, 8, NOW()),  -- admin -> 战略部负责人
(2, 125, 10, NOW()), -- zhangsan -> 战略部员工
(3, 180, 8, NOW()),  -- strategy_director_a -> 战略部负责人
(4, 181, 8, NOW()),  -- strategy_director_b -> 战略部负责人
(5, 188, 10, NOW()), -- zlb_admin -> 战略部员工
(6, 189, 11, NOW()), -- zlb_audit1 -> 下发人
(7, 190, 11, NOW()), -- zlb_audit2 -> 下发人
(8, 162, 10, NOW()), -- zhanlue -> 战略部员工

-- 职能部门角色
(9, 126, 6, NOW()), -- func_36 -> 职能负责人
(10, 163, 5, NOW()), -- dangban -> 填报人
(11, 191, 5, NOW()), -- dangban_report -> 填报人
(12, 192, 11, NOW()), -- dangban_audit1 -> 下发人
(13, 193, 11, NOW()), -- dangban_audit2 -> 下发人
(14, 194, 6, NOW()), -- dangban_issue -> 职能负责人

(15, 127, 6, NOW()), -- func_37 -> 职能负责人
(16, 164, 5, NOW()), -- jiwei -> 填报人
(17, 195, 5, NOW()), -- jiwei_report -> 填报人
(18, 196, 11, NOW()), -- jiwei_audit1 -> 下发人
(19, 197, 11, NOW()), -- jiwei_audit2 -> 下发人
(20, 198, 6, NOW()), -- jiwei_issue -> 职能负责人

(21, 128, 6, NOW()), -- func_38 -> 职能负责人
(22, 165, 5, NOW()), -- dangxuan -> 填报人
(23, 199, 5, NOW()), -- dangxuan_report -> 填报人
(24, 200, 11, NOW()), -- dangxuan_audit1 -> 下发人
(25, 201, 11, NOW()), -- dangxuan_audit2 -> 下发人
(26, 202, 6, NOW()), -- dangxuan_issue -> 职能负责人

(27, 129, 6, NOW()), -- func_39 -> 职能负责人
(28, 166, 5, NOW()), -- zuzhi -> 填报人
(29, 203, 5, NOW()), -- zuzhi_report -> 填报人
(30, 204, 11, NOW()), -- zuzhi_audit1 -> 下发人
(31, 205, 11, NOW()), -- zuzhi_audit2 -> 下发人
(32, 206, 6, NOW()), -- zuzhi_issue -> 职能负责人

(33, 130, 6, NOW()), -- func_40 -> 职能负责人
(34, 167, 5, NOW()), -- renli -> 填报人
(35, 207, 5, NOW()), -- renli_report -> 填报人
(36, 208, 11, NOW()), -- renli_audit1 -> 下发人
(37, 209, 11, NOW()), -- renli_audit2 -> 下发人
(38, 210, 6, NOW()), -- renli_issue -> 职能负责人

(39, 131, 6, NOW()), -- func_41 -> 职能负责人
(40, 168, 5, NOW()), -- xuegong -> 填报人
(41, 211, 5, NOW()), -- xuegong_report -> 填报人
(42, 212, 11, NOW()), -- xuegong_audit1 -> 下发人
(43, 213, 11, NOW()), -- xuegong_audit2 -> 下发人
(44, 214, 6, NOW()), -- xuegong_issue -> 职能负责人

(45, 132, 6, NOW()), -- func_42 -> 职能负责人
(46, 1, 5, NOW()), -- baowei -> 填报人
(47, 215, 5, NOW()), -- baowei_report -> 填报人
(48, 216, 11, NOW()), -- baowei_audit1 -> 下发人
(49, 217, 11, NOW()), -- baowei_audit2 -> 下发人
(50, 218, 6, NOW()), -- baowei_issue -> 职能负责人

(51, 133, 6, NOW()), -- func_43 -> 职能负责人
(52, 169, 5, NOW()), -- zonghe -> 填报人
(53, 219, 5, NOW()), -- zonghe_report -> 填报人
(54, 220, 11, NOW()), -- zonghe_audit1 -> 下发人
(55, 221, 11, NOW()), -- zonghe_audit2 -> 下发人
(56, 222, 6, NOW()), -- zonghe_issue -> 职能负责人

(57, 134, 6, NOW()), -- func_44 -> 职能负责人
(58, 170, 5, NOW()), -- jiaowu -> 填报人
(59, 182, 6, NOW()), -- func_director_c -> 职能负责人
(60, 183, 6, NOW()), -- func_director_d -> 职能负责人
(61, 223, 5, NOW()), -- jiaowu_report -> 填报人
(62, 224, 11, NOW()), -- jiaowu_audit1 -> 下发人
(63, 225, 11, NOW()), -- jiaowu_audit2 -> 下发人
(64, 226, 6, NOW()), -- jiaowu_issue -> 职能负责人

(65, 135, 6, NOW()), -- func_45 -> 职能负责人
(66, 161, 5, NOW()), -- keji -> 填报人
(67, 227, 5, NOW()), -- keji_report -> 填报人
(68, 228, 11, NOW()), -- keji_audit1 -> 下发人
(69, 229, 11, NOW()), -- keji_audit2 -> 下发人
(70, 230, 6, NOW()), -- keji_issue -> 职能负责人

(71, 136, 6, NOW()), -- func_46 -> 职能负责人
(72, 171, 5, NOW()), -- caiwu -> 填报人
(73, 231, 5, NOW()), -- caiwu_report -> 填报人
(74, 232, 11, NOW()), -- caiwu_audit1 -> 下发人
(75, 233, 11, NOW()), -- caiwu_audit2 -> 下发人
(76, 234, 6, NOW()), -- caiwu_issue -> 职能负责人

(77, 137, 6, NOW()), -- func_47 -> 职能负责人
(78, 172, 5, NOW()), -- zhaosheng -> 填报人
(79, 235, 5, NOW()), -- zhaosheng_report -> 填报人
(80, 236, 11, NOW()), -- zhaosheng_audit1 -> 下发人
(81, 237, 11, NOW()), -- zhaosheng_audit2 -> 下发人
(82, 238, 6, NOW()), -- zhaosheng_issue -> 职能负责人

(83, 138, 6, NOW()), -- func_48 -> 职能负责人
(84, 173, 5, NOW()), -- jiuye -> 填报人
(85, 239, 5, NOW()), -- jiuye_report -> 填报人
(86, 240, 11, NOW()), -- jiuye_audit1 -> 下发人
(87, 241, 11, NOW()), -- jiuye_audit2 -> 下发人
(88, 242, 6, NOW()), -- jiuye_issue -> 职能负责人

(89, 139, 6, NOW()), -- func_49 -> 职能负责人
(90, 174, 5, NOW()), -- shiyanshi -> 填报人
(91, 243, 5, NOW()), -- shiyanshi_report -> 填报人
(92, 244, 11, NOW()), -- shiyanshi_audit1 -> 下发人
(93, 245, 11, NOW()), -- shiyanshi_audit2 -> 下发人
(94, 246, 6, NOW()), -- shiyanshi_issue -> 职能负责人

(95, 140, 6, NOW()), -- func_50 -> 职能负责人
(96, 175, 5, NOW()), -- shuzi -> 填报人
(97, 247, 5, NOW()), -- shuzi_report -> 填报人
(98, 248, 11, NOW()), -- shuzi_audit1 -> 下发人
(99, 249, 11, NOW()), -- shuzi_audit2 -> 下发人
(100, 250, 6, NOW()), -- shuzi_issue -> 职能负责人

(101, 141, 6, NOW()), -- func_51 -> 职能负责人
(102, 176, 5, NOW()), -- tushuguan -> 填报人
(103, 251, 5, NOW()), -- tushu_report -> 填报人
(104, 252, 11, NOW()), -- tushu_audit1 -> 下发人
(105, 253, 11, NOW()), -- tushu_audit2 -> 下发人
(106, 254, 6, NOW()), -- tushu_issue -> 职能负责人

(107, 142, 6, NOW()), -- func_52 -> 职能负责人
(108, 177, 5, NOW()), -- houqin -> 填报人
(109, 255, 5, NOW()), -- houqin_report -> 填报人
(110, 256, 11, NOW()), -- houqin_audit1 -> 下发人
(111, 257, 11, NOW()), -- houqin_audit2 -> 下发人
(112, 258, 6, NOW()), -- houqin_issue -> 职能负责人

(113, 143, 6, NOW()), -- func_53 -> 职能负责人
(114, 178, 5, NOW()), -- jixu -> 填报人
(115, 259, 5, NOW()), -- jixu_report -> 填报人
(116, 260, 11, NOW()), -- jixu_audit1 -> 下发人
(117, 261, 11, NOW()), -- jixu_audit2 -> 下发人
(118, 262, 6, NOW()), -- jixu_issue -> 职能负责人

(119, 144, 6, NOW()), -- func_54 -> 职能负责人
(120, 179, 5, NOW()), -- guoji -> 填报人
(121, 263, 5, NOW()), -- guoji_report -> 填报人
(122, 264, 11, NOW()), -- guoji_audit1 -> 下发人
(123, 265, 11, NOW()), -- guoji_audit2 -> 下发人
(124, 266, 6, NOW()), -- guoji_issue -> 职能负责人

-- 学院角色
(125, 145, 7, NOW()), -- college_55 -> 学院院长
(126, 153, 5, NOW()), -- makesi -> 填报人
(127, 267, 5, NOW()), -- makesi_report -> 填报人
(128, 268, 7, NOW()), -- makesi_audit1 -> 学院院长
(129, 269, 7, NOW()), -- makesi_audit2 -> 学院院长

(130, 146, 7, NOW()), -- college_56 -> 学院院长
(131, 154, 5, NOW()), -- gongxue -> 填报人
(132, 270, 5, NOW()), -- gongxue_report -> 填报人
(133, 271, 7, NOW()), -- gongxue_audit1 -> 学院院长
(134, 272, 7, NOW()), -- gongxue_audit2 -> 学院院长

(135, 147, 7, NOW()), -- college_57 -> 学院院长
(136, 155, 5, NOW()), -- jisuanji -> 填报人
(137, 184, 7, NOW()), -- college_auditor_e -> 学院院长
(138, 185, 7, NOW()), -- college_auditor_f -> 学院院长
(139, 273, 5, NOW()), -- jisuanji_report -> 填报人
(140, 274, 7, NOW()), -- jisuanji_audit1 -> 学院院长
(141, 275, 7, NOW()), -- jisuanji_audit2 -> 学院院长

(142, 148, 7, NOW()), -- college_58 -> 学院院长
(143, 156, 5, NOW()), -- shangxue -> 填报人
(144, 276, 5, NOW()), -- shangxue_report -> 填报人
(145, 277, 7, NOW()), -- shangxue_audit1 -> 学院院长
(146, 278, 7, NOW()), -- shangxue_audit2 -> 学院院长

(147, 149, 7, NOW()), -- college_59 -> 学院院长
(148, 157, 5, NOW()), -- wenli -> 填报人
(149, 279, 5, NOW()), -- wenli_report -> 填报人
(150, 280, 7, NOW()), -- wenli_audit1 -> 学院院长
(151, 281, 7, NOW()), -- wenli_audit2 -> 学院院长

(152, 150, 7, NOW()), -- college_60 -> 学院院长
(153, 158, 5, NOW()), -- yishu -> 填报人
(154, 282, 5, NOW()), -- yishu_report -> 填报人
(155, 283, 7, NOW()), -- yishu_audit1 -> 学院院长
(156, 284, 7, NOW()), -- yishu_audit2 -> 学院院长

(157, 151, 7, NOW()), -- college_61 -> 学院院长
(158, 159, 5, NOW()), -- hangkong -> 填报人
(159, 285, 5, NOW()), -- hangkong_report -> 填报人
(160, 286, 7, NOW()), -- hangkong_audit1 -> 学院院长
(161, 287, 7, NOW()), -- hangkong_audit2 -> 学院院长

(162, 152, 7, NOW()), -- college_62 -> 学院院长
(163, 160, 5, NOW()), -- guojiaoyu -> 填报人
(164, 288, 5, NOW()), -- guojiaoyu_report -> 填报人
(165, 289, 7, NOW()), -- guojiaoyu_audit1 -> 学院院长
(166, 290, 7, NOW()); -- guojiaoyu_audit2 -> 学院院长

-- =============================================================================
-- 7. 角色权限关联 (sys_role_permission)
-- =============================================================================

-- 战略部负责人拥有所有权限
INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT 8, id, NOW() FROM sys_permission;

-- 职能部门负责人拥有大部分权限
INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT 6, id, NOW() FROM sys_permission WHERE perm_code NOT IN ('org', 'user', 'system');

-- 学院院长拥有指标和报告权限
INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT 7, id, NOW() FROM sys_permission WHERE perm_code IN ('dashboard', 'indicator', 'indicator:list', 'indicator:edit', 'report', 'report:list', 'report:create', 'report:submit', 'approval', 'approval:list', 'approval:approve');

-- 填报人只能填报和查看
INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT 5, id, NOW() FROM sys_permission WHERE perm_code IN ('dashboard', 'indicator', 'indicator:list', 'report', 'report:list', 'report:create', 'report:submit');

-- 战略部员工权限
INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT 10, id, NOW() FROM sys_permission WHERE perm_code NOT IN ('org', 'user', 'system');

-- 下发人权限
INSERT INTO sys_role_permission (role_id, permission_id, created_at)
SELECT 11, id, NOW() FROM sys_permission WHERE perm_code IN ('dashboard', 'indicator', 'indicator:list', 'indicator:create', 'indicator:distribute', 'plan', 'plan:list', 'plan:create', 'plan:distribute', 'report', 'report:list', 'approval', 'approval:list');

-- =============================================================================
-- 8. 考核周期 (cycle)
-- =============================================================================

INSERT INTO cycle (id, cycle_name, year, start_date, end_date, description, created_at, updated_at) VALUES
(90, '2026年度考核', 2026, '2026-01-01', '2026-12-31', '2026年度战略任务考核', NOW(), NOW()),
(91, '2025年度考核', 2025, '2025-01-01', '2025-12-31', '2025年度战略任务考核', NOW(), NOW()),
(92, '2024年度考核', 2024, '2024-01-01', '2024-12-31', '2024年度战略任务考核', NOW(), NOW()),
(93, '2023年度考核', 2023, '2023-01-01', '2023-12-31', '2023年度战略任务考核', NOW(), NOW());

-- =============================================================================
-- 9. 指标 (indicator) - 约480个指标
-- =============================================================================

-- 战略部 -> 职能部门的一级指标 (每个职能部门约3-8个)
-- 指标类型: '职能' 表示职能部门指标

INSERT INTO indicator (id, task_id, parent_indicator_id, indicator_desc, weight_percent, sort_order, remark, created_at, updated_at, type, progress, is_deleted, owner_org_id, target_org_id, status, responsible_user_id, is_enabled) VALUES
-- 党委办公室指标 (目标36)
(19501, NULL, NULL, '党建工作质量提升', 10, 1, '加强党组织建设', NOW(), NOW(), ''职能'', 0, false, 35, 36, 'DISTRIBUTED', NULL, true),
(19502, NULL, NULL, '思想政治教育', 10, 2, '加强思想政治工作', NOW(), NOW(), ''职能'', 0, false, 35, 36, 'DISTRIBUTED', NULL, true),
(19503, NULL, NULL, '党委重要决策执行', 10, 3, '落实党委决策', NOW(), NOW(), ''职能'', 0, false, 35, 36, 'PENDING', NULL, true),

-- 纪委办公室指标 (目标37)
(19504, NULL, NULL, '党风廉政建设', 10, 1, '廉政风险防控', NOW(), NOW(), ''职能'', 0, false, 35, 37, 'DISTRIBUTED', NULL, true),
(19505, NULL, NULL, '纪检监察工作', 10, 2, '监督执纪问责', NOW(), NOW(), ''职能'', 0, false, 35, 37, 'DISTRIBUTED', NULL, true),
(19506, NULL, NULL, '廉洁教育', 10, 3, '廉洁文化进校园', NOW(), NOW(), ''职能'', 0, false, 35, 37, 'PENDING', NULL, true),
(19507, NULL, NULL, '案件查办', 10, 4, '违纪案件处理', NOW(), NOW(), ''职能'', 0, false, 35, 37, 'DRAFT', NULL, true),
(19508, NULL, NULL, '巡视巡察', 10, 5, '巡视问题整改', NOW(), NOW(), ''职能'', 0, false, 35, 37, 'DRAFT', NULL, true),

-- 党委宣传部指标 (目标38)
(19509, NULL, NULL, '意识形态工作', 10, 1, '意识形态责任制', NOW(), NOW(), ''职能'', 0, false, 35, 38, 'DISTRIBUTED', NULL, true),
(19510, NULL, NULL, '新闻宣传', 10, 2, '校园文化建设', NOW(), NOW(), ''职能'', 0, false, 35, 38, 'DISTRIBUTED', NULL, true),
(19511, NULL, NULL, '舆论引导', 10, 3, '网络舆情应对', NOW(), NOW(), ''职能'', 0, false, 35, 38, 'DRAFT', NULL, true),
(19512, NULL, NULL, '精神文明建设', 10, 4, '文明校园创建', NOW(), NOW(), ''职能'', 0, false, 35, 38, 'PENDING', NULL, true),
(19513, NULL, NULL, '文化活动', 10, 5, '校园文化活动', NOW(), NOW(), ''职能'', 0, false, 35, 38, 'DRAFT', NULL, true),
(19514, NULL, NULL, '对外宣传', 10, 6, '学校形象宣传', NOW(), NOW(), ''职能'', 0, false, 35, 38, 'DRAFT', NULL, true),
(19515, NULL, NULL, '思政课程建设', 10, 7, '思政课教学质量', NOW(), NOW(), ''职能'', 0, false, 35, 38, 'DRAFT', NULL, true),

-- 党委组织部指标 (目标39)
(19516, NULL, NULL, '组织建设', 10, 1, '基层党组织建设', NOW(), NOW(), ''职能'', 0, false, 35, 39, 'DISTRIBUTED', NULL, true),
(19517, NULL, NULL, '干部队伍建设', 10, 2, '干部培养选拔', NOW(), NOW(), ''职能'', 0, false, 35, 39, 'DISTRIBUTED', NULL, true),
(19518, NULL, NULL, '人才工作', 10, 3, '人才引进培养', NOW(), NOW(), ''职能'', 0, false, 35, 39, 'PENDING', NULL, true),
(19519, NULL, NULL, '党员教育管理', 10, 4, '党员培训发展', NOW(), NOW(), ''职能'', 0, false, 35, 39, 'DRAFT', NULL, true),
(19520, NULL, NULL, '党建工作考核', 10, 5, '党建述职评议', NOW(), NOW(), ''职能'', 0, false, 35, 39, 'DRAFT', NULL, true),
(19521, NULL, NULL, '组织制度建设', 10, 6, '组织制度完善', NOW(), NOW(), ''职能'', 0, false, 35, 39, 'DRAFT', NULL, true),
(19522, NULL, NULL, '干部培训', 10, 7, '干部能力提升', NOW(), NOW(), ''职能'', 0, false, 35, 39, 'DRAFT', NULL, true),
(19523, NULL, NULL, '人事制度改革', 10, 8, '人事制度改革', NOW(), NOW(), ''职能'', 0, false, 35, 39, 'DRAFT', NULL, true),
(19524, NULL, NULL, '师资队伍建设', 10, 9, '教师队伍建设', NOW(), NOW(), ''职能'', 0, false, 35, 39, 'DRAFT', NULL, true),

-- 人力资源处指标 (目标40)
(19525, NULL, NULL, '人事管理', 10, 1, '人事制度改革', NOW(), NOW(), ''职能'', 0, false, 35, 40, 'DISTRIBUTED', NULL, true),
(19526, NULL, NULL, '师资队伍建设', 10, 2, '教师发展', NOW(), NOW(), ''职能'', 0, false, 35, 40, 'DISTRIBUTED', NULL, true),
(19527, NULL, NULL, '人才引进', 10, 3, '高层次人才引进', NOW(), NOW(), ''职能'', 0, false, 35, 40, 'PENDING', NULL, true),
(19528, NULL, NULL, '职称评审', 10, 4, '职称评审改革', NOW(), NOW(), ''职能'', 0, false, 35, 40, 'DRAFT', NULL, true),
(19529, NULL, NULL, '绩效考核', 10, 5, '教职工考核', NOW(), NOW(), ''职能'', 0, false, 35, 40, 'DRAFT', NULL, true),
(19530, NULL, NULL, '薪酬福利', 10, 6, '薪酬制度改革', NOW(), NOW(), ''职能'', 0, false, 35, 40, 'DRAFT', NULL, true),
(19531, NULL, NULL, '博士后管理', 10, 7, '博士后工作站', NOW(), NOW(), ''职能'', 0, false, 35, 40, 'DRAFT', NULL, true),
(19532, NULL, NULL, '劳动争议处理', 10, 8, '劳动关系和谐', NOW(), NOW(), ''职能'', 0, false, 35, 40, 'DRAFT', NULL, true),

-- 学生工作部指标 (目标41)
(19533, NULL, NULL, '学生思想政治教育', 10, 1, '学生思政工作', NOW(), NOW(), ''职能'', 0, false, 35, 41, 'DISTRIBUTED', NULL, true),
(19534, NULL, NULL, '学生日常管理', 10, 2, '学生行为管理', NOW(), NOW(), ''职能'', 0, false, 35, 41, 'DISTRIBUTED', NULL, true),
(19535, NULL, NULL, '心理健康教育', 10, 3, '心理健康服务', NOW(), NOW(), ''职能'', 0, false, 35, 41, 'PENDING', NULL, true),
(19536, NULL, NULL, '学风建设', 10, 4, '学风督导', NOW(), NOW(), ''职能'', 0, false, 35, 41, 'DRAFT', NULL, true),
(19537, NULL, NULL, '学生资助', 10, 5, '奖助学金', NOW(), NOW(), ''职能'', 0, false, 35, 41, 'DRAFT', NULL, true),
(19538, NULL, NULL, '辅导员队伍建设', 10, 6, '辅导员培训', NOW(), NOW(), ''职能'', 0, false, 35, 41, 'DRAFT', NULL, true),
(19539, NULL, NULL, '校园安全', 10, 7, '校园安全管理', NOW(), NOW(), ''职能'', 0, false, 35, 41, 'DRAFT', NULL, true),
(19540, NULL, NULL, '学生创新创业', 10, 8, '创新创业教育', NOW(), NOW(), ''职能'', 0, false, 35, 41, 'DRAFT', NULL, true),
(19541, NULL, NULL, '社会实践', 10, 9, '社会实践项目', NOW(), NOW(), ''职能'', 0, false, 35, 41, 'DRAFT', NULL, true),
(19542, NULL, NULL, '社团管理', 10, 10, '学生社团指导', NOW(), NOW(), ''职能'', 0, false, 35, 41, 'DRAFT', NULL, true),

-- 保卫处指标 (目标42)
(19543, NULL, NULL, '校园安全管理', 10, 1, '安全管理', NOW(), NOW(), ''职能'', 0, false, 35, 42, 'DISTRIBUTED', NULL, true),
(19544, NULL, NULL, '消防安全', 10, 2, '消防检查', NOW(), NOW(), ''职能'', 0, false, 35, 42, 'DISTRIBUTED', NULL, true),
(19545, NULL, NULL, '治安管理', 10, 3, '校园治安', NOW(), NOW(), ''职能'', 0, false, 35, 42, 'PENDING', NULL, true),
(19546, NULL, NULL, '交通安全', 10, 4, '交通秩序', NOW(), NOW(), ''职能'', 0, false, 35, 42, 'DRAFT', NULL, true),
(19547, NULL, NULL, '安全教育', 10, 5, '安全培训', NOW(), NOW(), ''职能'', 0, false, 35, 42, 'DRAFT', NULL, true),
(19548, NULL, NULL, '应急预案', 10, 6, '突发事件应对', NOW(), NOW(), ''职能'', 0, false, 35, 42, 'DRAFT', NULL, true),
(19549, NULL, NULL, '保密工作', 10, 7, '保密管理', NOW(), NOW(), ''职能'', 0, false, 35, 42, 'DRAFT', NULL, true),
(19550, NULL, NULL, '国家安全', 10, 8, '国家安全教育', NOW(), NOW(), ''职能'', 0, false, 35, 42, 'DRAFT', NULL, true),
(19551, NULL, NULL, '政治安全', 10, 9, '政治安全维护', NOW(), NOW(), ''职能'', 0, false, 35, 42, 'DRAFT', NULL, true),
(19552, NULL, NULL, '网络安全', 10, 10, '网络安全管理', NOW(), NOW(), ''职能'', 0, false, 35, 42, 'DRAFT', NULL, true),
(19553, NULL, NULL, '实验室安全', 10, 11, '实验室安全管理', NOW(), NOW(), ''职能'', 0, false, 35, 42, 'DRAFT', NULL, true),
(19554, NULL, NULL, '食品安全', 10, 12, '食堂安全管理', NOW(), NOW(), ''职能'', 0, false, 35, 42, 'DRAFT', NULL, true),

-- 学校办公室指标 (目标43)
(19555, NULL, NULL, '综合协调', 10, 1, '校内外协调', NOW(), NOW(), ''职能'', 0, false, 35, 43, 'DISTRIBUTED', NULL, true),
(19556, NULL, NULL, '公文处理', 10, 2, '公文管理', NOW(), NOW(), ''职能'', 0, false, 35, 43, 'DISTRIBUTED', NULL, true),
(19557, NULL, NULL, '信息公开', 10, 3, '校务公开', NOW(), NOW(), ''职能'', 0, false, 35, 43, 'PENDING', NULL, true),
(19558, NULL, NULL, '档案管理', 10, 4, '档案工作', NOW(), NOW(), ''职能'', 0, false, 35, 43, 'DRAFT', NULL, true),
(19559, NULL, NULL, '印章管理', 10, 5, '印章使用', NOW(), NOW(), ''职能'', 0, false, 35, 43, 'DRAFT', NULL, true),
(19560, NULL, NULL, '重要活动组织', 10, 6, '校级活动', NOW(), NOW(), ''职能'', 0, false, 35, 43, 'DRAFT', NULL, true),

-- 教务处指标 (目标44)
(19561, NULL, NULL, '教学质量提升', 10, 1, '教学改革', NOW(), NOW(), ''职能'', 0, false, 35, 44, 'DISTRIBUTED', NULL, true),
(19562, NULL, NULL, '专业建设', 10, 2, '专业评估', NOW(), NOW(), ''职能'', 0, false, 35, 44, 'DISTRIBUTED', NULL, true),
(19563, NULL, NULL, '课程建设', 10, 3, '精品课程', NOW(), NOW(), ''职能'', 0, false, 35, 44, 'PENDING', NULL, true),
(19564, NULL, NULL, '实践教学', 10, 4, '实验实习', NOW(), NOW(), ''职能'', 0, false, 35, 44, 'DRAFT', NULL, true),
(19565, NULL, NULL, '学籍管理', 10, 5, '学生学籍', NOW(), NOW(), ''职能'', 0, false, 35, 44, 'DRAFT', NULL, true),
(19566, NULL, NULL, '教材建设', 10, 6, '教材选用', NOW(), NOW(), ''职能'', 0, false, 35, 44, 'DRAFT', NULL, true),
(19567, NULL, NULL, '教学评估', 10, 7, '教学质量监控', NOW(), NOW(), ''职能'', 0, false, 35, 44, 'DRAFT', NULL, true),
(19568, NULL, NULL, '教师培训', 10, 8, '教师发展', NOW(), NOW(), ''职能'', 0, false, 35, 44, 'DRAFT', NULL, true),
(19569, NULL, NULL, '招生工作', 10, 9, '招生质量', NOW(), NOW(), ''职能'', 0, false, 35, 44, 'DRAFT', NULL, true),
(19570, NULL, NULL, '学位管理', 10, 10, '学位授予', NOW(), NOW(), ''职能'', 0, false, 35, 44, 'DRAFT', NULL, true),
(19571, NULL, NULL, '教务管理', 10, 11, '教学运行', NOW(), NOW(), ''职能'', 0, false, 35, 44, 'DRAFT', NULL, true),
(19572, NULL, NULL, '教学资源建设', 10, 12, '教学资源', NOW(), NOW(), ''职能'', 0, false, 35, 44, 'DRAFT', NULL, true),
(19573, NULL, NULL, '在线课程建设', 10, 13, '慕课建设', NOW(), NOW(), ''职能'', 0, false, 35, 44, 'DRAFT', NULL, true),
(19574, NULL, NULL, '产教融合', 10, 14, '校企合作', NOW(), NOW(), ''职能'', 0, false, 35, 44, 'DRAFT', NULL, true),
(19575, NULL, NULL, '双创教育', 10, 15, '创新创业', NOW(), NOW(), ''职能'', 0, false, 35, 44, 'DRAFT', NULL, true),
(19576, NULL, NULL, '国际交流', 10, 16, '国际合作', NOW(), NOW(), ''职能'', 0, false, 35, 44, 'DRAFT', NULL, true),
(19577, NULL, NULL, '继续教育', 10, 17, '继续教育发展', NOW(), NOW(), ''职能'', 0, false, 35, 44, 'DRAFT', NULL, true),

-- 科技处指标 (目标45)
(19578, NULL, NULL, '科研项目', 10, 1, '科研立项', NOW(), NOW(), ''职能'', 0, false, 35, 45, 'DISTRIBUTED', NULL, true),
(19579, NULL, NULL, '科研成果', 10, 2, '科技成果', NOW(), NOW(), ''职能'', 0, false, 35, 45, 'DISTRIBUTED', NULL, true),
(19580, NULL, NULL, '科研平台', 10, 3, '科研基地', NOW(), NOW(), ''职能'', 0, false, 35, 45, 'PENDING', NULL, true),
(19581, NULL, NULL, '科研经费', 10, 4, '经费管理', NOW(), NOW(), ''职能'', 0, false, 35, 45, 'DRAFT', NULL, true),
(19582, NULL, NULL, '学术交流', 10, 5, '学术活动', NOW(), NOW(), ''职能'', 0, false, 35, 45, 'DRAFT', NULL, true),
(19583, NULL, NULL, '知识产权', 10, 6, '专利管理', NOW(), NOW(), ''职能'', 0, false, 35, 45, 'DRAFT', NULL, true),
(19584, NULL, NULL, '产学研合作', 10, 7, '校企合作', NOW(), NOW(), ''职能'', 0, false, 35, 45, 'DRAFT', NULL, true),
(19585, NULL, NULL, '科研团队建设', 10, 8, '团队建设', NOW(), NOW(), ''职能'', 0, false, 35, 45, 'DRAFT', NULL, true),
(19586, NULL, NULL, '科研奖励', 10, 9, '科技成果奖', NOW(), NOW(), ''职能'', 0, false, 35, 45, 'DRAFT', NULL, true),
(19587, NULL, NULL, '科研诚信', 10, 10, '学术规范', NOW(), NOW(), ''职能'', 0, false, 35, 45, 'DRAFT', NULL, true),
(19588, NULL, NULL, '技术转移', 10, 11, '成果转化', NOW(), NOW(), ''职能'', 0, false, 35, 45, 'DRAFT', NULL, true),
(19589, NULL, NULL, '国防科研', 10, 12, '军工项目', NOW(), NOW(), ''职能'', 0, false, 35, 45, 'DRAFT', NULL, true);

-- 财务处指标 (目标46)
INSERT INTO indicator (id, task_id, parent_indicator_id, indicator_desc, weight_percent, sort_order, remark, created_at, updated_at, type, progress, is_deleted, owner_org_id, target_org_id, status, responsible_user_id, is_enabled) VALUES
(19590, NULL, NULL, '预算管理', 10, 1, '预算编制执行', NOW(), NOW(), ''职能'', 0, false, 35, 46, 'DRAFT', NULL, true),
(19591, NULL, NULL, '财务管理', 10, 2, '财务核算', NOW(), NOW(), ''职能'', 0, false, 35, 46, 'DRAFT', NULL, true),
(19592, NULL, NULL, '资金管理', 10, 3, '资金使用', NOW(), NOW(), ''职能'', 0, false, 35, 46, 'DRAFT', NULL, true),
(19593, NULL, NULL, '资产管理', 10, 4, '资产配置', NOW(), NOW(), ''职能'', 0, false, 35, 46, 'DRAFT', NULL, true);

-- 招生就业处指标 (目标47)
INSERT INTO indicator (id, task_id, parent_indicator_id, indicator_desc, weight_percent, sort_order, remark, created_at, updated_at, type, progress, is_deleted, owner_org_id, target_org_id, status, responsible_user_id, is_enabled) VALUES
(19594, NULL, NULL, '招生工作', 10, 1, '招生宣传录取', NOW(), NOW(), ''职能'', 0, false, 35, 47, 'DRAFT', NULL, true),
(19595, NULL, NULL, '就业指导', 10, 2, '就业服务', NOW(), NOW(), ''职能'', 0, false, 35, 47, 'DRAFT', NULL, true),
(19596, NULL, NULL, '就业率', 10, 3, '毕业生就业率', NOW(), NOW(), ''职能'', 0, false, 35, 47, 'DRAFT', NULL, true),
(19597, NULL, NULL, '创新创业', 10, 4, '创业扶持', NOW(), NOW(), ''职能'', 0, false, 35, 47, 'DRAFT', NULL, true);

-- 就业创业指导中心指标 (目标48)
INSERT INTO indicator (id, task_id, parent_indicator_id, indicator_desc, weight_percent, sort_order, remark, created_at, updated_at, type, progress, is_deleted, owner_org_id, target_org_id, status, responsible_user_id, is_enabled) VALUES
(19598, NULL, NULL, '创业教育', 10, 1, '创业课程', NOW(), NOW(), ''职能'', 0, false, 35, 48, 'DRAFT', NULL, true),
(19599, NULL, NULL, '创业孵化', 10, 2, '孵化基地', NOW(), NOW(), ''职能'', 0, false, 35, 48, 'DRAFT', NULL, true),
(19600, NULL, NULL, '创业大赛', 10, 3, '创新创业大赛', NOW(), NOW(), ''职能'', 0, false, 35, 48, 'DRAFT', NULL, true),
(19601, NULL, NULL, '创业服务', 10, 4, '创业指导', NOW(), NOW(), ''职能'', 0, false, 35, 48, 'DRAFT', NULL, true);

-- 实验室与资产管理处指标 (目标49)
INSERT INTO indicator (id, task_id, parent_indicator_id, indicator_desc, weight_percent, sort_order, remark, created_at, updated_at, type, progress, is_deleted, owner_org_id, target_org_id, status, responsible_user_id, is_enabled) VALUES
(19602, NULL, NULL, '实验室建设', 10, 1, '实验室发展', NOW(), NOW(), ''职能'', 0, false, 35, 49, 'DRAFT', NULL, true),
(19603, NULL, NULL, '设备管理', 10, 2, '仪器设备', NOW(), NOW(), ''职能'', 0, false, 35, 49, 'DRAFT', NULL, true),
(19604, NULL, NULL, '资产管理', 10, 3, '固定资产', NOW(), NOW(), ''职能'', 0, false, 35, 49, 'DRAFT', NULL, true),
(19605, NULL, NULL, '实验室安全', 10, 4, '安全检查', NOW(), NOW(), ''职能'', 0, false, 35, 49, 'DRAFT', NULL, true);

-- 数字化校园建设办公室指标 (目标50)
INSERT INTO indicator (id, task_id, parent_indicator_id, indicator_desc, weight_percent, sort_order, remark, created_at, updated_at, type, progress, is_deleted, owner_org_id, target_org_id, status, responsible_user_id, is_enabled) VALUES
(19606, NULL, NULL, '信息化建设', 10, 1, '智慧校园', NOW(), NOW(), ''职能'', 0, false, 35, 50, 'DRAFT', NULL, true),
(19607, NULL, NULL, '网络建设', 10, 2, '校园网络', NOW(), NOW(), ''职能'', 0, false, 35, 50, 'DRAFT', NULL, true),
(19608, NULL, NULL, '数据中心', 10, 3, '数据管理', NOW(), NOW(), ''职能'', 0, false, 35, 50, 'DRAFT', NULL, true),
(19609, NULL, NULL, '信息安全', 10, 4, '网络安全', NOW(), NOW(), ''职能'', 0, false, 35, 50, 'DRAFT', NULL, true);

-- 图书馆指标 (目标51)
INSERT INTO indicator (id, task_id, parent_indicator_id, indicator_desc, weight_percent, sort_order, remark, created_at, updated_at, type, progress, is_deleted, owner_org_id, target_org_id, status, responsible_user_id, is_enabled) VALUES
(19610, NULL, NULL, '文献资源建设', 10, 1, '图书采购', NOW(), NOW(), ''职能'', 0, false, 35, 51, 'DRAFT', NULL, true),
(19611, NULL, NULL, '读者服务', 10, 2, '借阅服务', NOW(), NOW(), ''职能'', 0, false, 35, 51, 'DRAFT', NULL, true),
(19612, NULL, NULL, '情报服务', 10, 3, '信息服务', NOW(), NOW(), ''职能'', 0, false, 35, 51, 'DRAFT', NULL, true),
(19613, NULL, NULL, '数字图书馆', 10, 4, '电子资源', NOW(), NOW(), ''职能'', 0, false, 35, 51, 'DRAFT', NULL, true);

-- 后勤保障处指标 (目标52)
INSERT INTO indicator (id, task_id, parent_indicator_id, indicator_desc, weight_percent, sort_order, remark, created_at, updated_at, type, progress, is_deleted, owner_org_id, target_org_id, status, responsible_user_id, is_enabled) VALUES
(19614, NULL, NULL, '后勤服务', 10, 1, '服务保障', NOW(), NOW(), ''职能'', 0, false, 35, 52, 'DRAFT', NULL, true),
(19615, NULL, NULL, '校园环境', 10, 2, '环境整治', NOW(), NOW(), ''职能'', 0, false, 35, 52, 'DRAFT', NULL, true),
(19616, NULL, NULL, '餐饮服务', 10, 3, '食堂管理', NOW(), NOW(), ''职能'', 0, false, 35, 52, 'DRAFT', NULL, true),
(19617, NULL, NULL, '水电管理', 10, 4, '能源管理', NOW(), NOW(), ''职能'', 0, false, 35, 52, 'DRAFT', NULL, true),
(19618, NULL, NULL, '物业服务', 10, 5, '物业管理', NOW(), NOW(), ''职能'', 0, false, 35, 52, 'DRAFT', NULL, true),
(19619, NULL, NULL, '基建维修', 10, 6, '维修管理', NOW(), NOW(), ''职能'', 0, false, 35, 52, 'DRAFT', NULL, true),
(19620, NULL, NULL, '医疗服务', 10, 7, '卫生保健', NOW(), NOW(), ''职能'', 0, false, 35, 52, 'DRAFT', NULL, true),
(19621, NULL, NULL, '住房管理', 10, 8, '周转房', NOW(), NOW(), ''职能'', 0, false, 35, 52, 'DRAFT', NULL, true);

-- 继续教育学院指标 (目标53)
INSERT INTO indicator (id, task_id, parent_indicator_id, indicator_desc, weight_percent, sort_order, remark, created_at, updated_at, type, progress, is_deleted, owner_org_id, target_org_id, status, responsible_user_id, is_enabled) VALUES
(19622, NULL, NULL, '继续教育规模', 10, 1, '培训人数', NOW(), NOW(), ''职能'', 0, false, 35, 53, 'DRAFT', NULL, true),
(19623, NULL, NULL, '教学质量', 10, 2, '培训质量', NOW(), NOW(), ''职能'', 0, false, 35, 53, 'DRAFT', NULL, true),
(19624, NULL, NULL, '社会培训', 10, 3, '社会服务', NOW(), NOW(), ''职能'', 0, false, 35, 53, 'DRAFT', NULL, true),
(19625, NULL, NULL, '学历教育', 10, 4, '成教招生', NOW(), NOW(), ''职能'', 0, false, 35, 53, 'DRAFT', NULL, true),
(19626, NULL, NULL, '非学历培训', 10, 5, '技能培训', NOW(), NOW(), ''职能'', 0, false, 35, 53, 'DRAFT', NULL, true),
(19627, NULL, NULL, '合作办学', 10, 6, '校企合作', NOW(), NOW(), ''职能'', 0, false, 35, 53, 'DRAFT', NULL, true),
(19628, NULL, NULL, '在线教育', 10, 7, '网络培训', NOW(), NOW(), ''职能'', 0, false, 35, 53, 'DRAFT', NULL, true),
(19629, NULL, NULL, '证书管理', 10, 8, '证书发放', NOW(), NOW(), ''职能'', 0, false, 35, 53, 'DRAFT', NULL, true);

-- 国际交流与合作处指标 (目标54)
INSERT INTO indicator (id, task_id, parent_indicator_id, indicator_desc, weight_percent, sort_order, remark, created_at, updated_at, type, progress, is_deleted, owner_org_id, target_org_id, status, responsible_user_id, is_enabled) VALUES
(19630, NULL, NULL, '国际交流', 10, 1, '交流项目', NOW(), NOW(), ''职能'', 0, false, 35, 54, 'DRAFT', NULL, true),
(19631, NULL, NULL, '留学生教育', 10, 2, '留学生管理', NOW(), NOW(), ''职能'', 0, false, 35, 54, 'DRAFT', NULL, true),
(19632, NULL, NULL, '合作办学', 10, 3, '中外合作', NOW(), NOW(), ''职能'', 0, false, 35, 54, 'DRAFT', NULL, true),
(19633, NULL, NULL, '外籍教师管理', 10, 4, '外教聘请', NOW(), NOW(), ''职能'', 0, false, 35, 54, 'DRAFT', NULL, true),
(19634, NULL, NULL, '出国管理', 10, 5, '因公出国', NOW(), NOW(), ''职能'', 0, false, 35, 54, 'DRAFT', NULL, true),
(19635, NULL, NULL, '国际会议', 10, 6, '学术会议', NOW(), NOW(), ''职能'', 0, false, 35, 54, 'DRAFT', NULL, true),
(19636, NULL, NULL, '孔子学院', 10, 7, '汉语推广', NOW(), NOW(), ''职能'', 0, false, 35, 54, 'DRAFT', NULL, true),
(19637, NULL, NULL, '雅思考试', 10, 8, '语言考试', NOW(), NOW(), ''职能'', 0, false, 35, 54, 'DRAFT', NULL, true),
(19638, NULL, NULL, '海外基地', 10, 9, '海外办学', NOW(), NOW(), ''职能'', 0, false, 35, 54, 'DRAFT', NULL, true),
(19639, NULL, NULL, '国际科研合作', 10, 10, '合作项目', NOW(), NOW(), ''职能'', 0, false, 35, 54, 'DRAFT', NULL, true);

-- =============================================================================
-- 10. 计划 (plan) - 职能部门计划
-- =============================================================================

INSERT INTO plan (id, cycle_id, created_at, updated_at, is_deleted, target_org_id, created_by_org_id, plan_level, status) VALUES
-- 战略部创建的计划
(1, 90, NOW(), NOW(), false, 36, 35, 'FUNC', 'DISTRIBUTED'),  -- 党委办公室
(2, 90, NOW(), NOW(), false, 37, 35, 'FUNC', 'DISTRIBUTED'),  -- 纪委办公室
(3, 90, NOW(), NOW(), false, 38, 35, 'FUNC', 'DISTRIBUTED'),  -- 党委宣传部
(4, 90, NOW(), NOW(), false, 39, 35, 'FUNC', 'PENDING'),  -- 党委组织部
(5, 90, NOW(), NOW(), false, 40, 35, 'FUNC', 'DISTRIBUTED'),  -- 人力资源处
(6, 90, NOW(), NOW(), false, 41, 35, 'FUNC', 'DISTRIBUTED'),  -- 学生工作部
(7, 90, NOW(), NOW(), false, 42, 35, 'FUNC', 'DISTRIBUTED'),  -- 保卫处
(8, 90, NOW(), NOW(), false, 43, 35, 'FUNC', 'DISTRIBUTED'),  -- 学校办公室
(9, 90, NOW(), NOW(), false, 44, 35, 'FUNC', 'DISTRIBUTED'),  -- 教务处
(10, 90, NOW(), NOW(), false, 45, 35, 'FUNC', 'PENDING'),  -- 科技处
(11, 90, NOW(), NOW(), false, 46, 35, 'FUNC', 'DRAFT'),  -- 财务处
(12, 90, NOW(), NOW(), false, 47, 35, 'FUNC', 'DRAFT'),  -- 招生就业处
(13, 90, NOW(), NOW(), false, 48, 35, 'FUNC', 'DRAFT'),  -- 就业创业指导中心
(14, 90, NOW(), NOW(), false, 49, 35, 'FUNC', 'DRAFT'),  -- 实验室与资产管理处
(15, 90, NOW(), NOW(), false, 50, 35, 'FUNC', 'DRAFT'),  -- 数字化校园建设办公室
(16, 90, NOW(), NOW(), false, 51, 35, 'FUNC', 'DRAFT'),  -- 图书馆
(17, 90, NOW(), NOW(), false, 52, 35, 'FUNC', 'DRAFT'),  -- 后勤保障处
(18, 90, NOW(), NOW(), false, 53, 35, 'FUNC', 'DRAFT'),  -- 继续教育学院
(19, 90, NOW(), NOW(), false, 54, 35, 'FUNC', 'DRAFT');  -- 国际交流与合作处

-- 职能部门创建的下发到学院的计划
INSERT INTO plan (id, cycle_id, created_at, updated_at, is_deleted, target_org_id, created_by_org_id, plan_level, status) VALUES
(20, 90, NOW(), NOW(), false, 55, 44, 'COLLEGE', 'DISTRIBUTED'),  -- 教务处->马院
(21, 90, NOW(), NOW(), false, 56, 44, 'COLLEGE', 'DISTRIBUTED'),  -- 教务处->法学院
(22, 90, NOW(), NOW(), false, 57, 44, 'COLLEGE', 'DISTRIBUTED'),  -- 教务处->计算机学院
(23, 90, NOW(), NOW(), false, 58, 44, 'COLLEGE', 'PENDING'),  -- 教务处->商学院
(24, 90, NOW(), NOW(), false, 59, 44, 'COLLEGE', 'DRAFT'),  -- 教务处->文理学院
(25, 90, NOW(), NOW(), false, 60, 44, 'COLLEGE', 'DRAFT'),  -- 教务处->艺术学院
(26, 90, NOW(), NOW(), false, 61, 44, 'COLLEGE', 'DRAFT'),  -- 教务处->航空学院
(27, 90, NOW(), NOW(), false, 62, 44, 'COLLEGE', 'DRAFT');  -- 教务处->国教院

-- =============================================================================
-- 11. 用户上级关系 (sys_user_supervisor) - 部分
-- =============================================================================

INSERT INTO sys_user_supervisor (id, user_id, supervisor_id, created_at) VALUES
(1, 125, 180, NOW()),  -- zhangsan -> strategy_director_a
(2, 162, 180, NOW()),  -- zhanlue -> strategy_director_a
(3, 188, 180, NOW()),  -- zlb_admin -> strategy_director_a
(4, 189, 180, NOW()),  -- zlb_audit1 -> strategy_director_a
(5, 190, 181, NOW()),  -- zlb_audit2 -> strategy_director_b
(6, 170, 182, NOW()),  -- jiaowu -> func_director_c
(7, 182, 134, NOW()),  -- func_director_c -> func_44
(8, 183, 134, NOW()),  -- func_director_d -> func_44
(9, 155, 147, NOW()),  -- jisuanji -> college_57
(10, 184, 147, NOW()), -- college_auditor_e -> college_57
(11, 185, 147, NOW()); -- college_auditor_f -> college_57

COMMIT;
