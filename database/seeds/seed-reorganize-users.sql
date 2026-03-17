-- ============================================================
-- 用户账号重组脚本
-- ============================================================
-- 战略发展部 (org_id=35): 1 管理员 + 2 审核人
-- 职能部门 (org_id=36-54): 1 填报人 + 2 审核人 + 1 下发人
-- 二级学院 (org_id=55-62): 1 填报人 + 2 审核人
-- 统一密码: admin123
-- ============================================================

BEGIN;

-- ============================================================
-- Step 1: 创建缺失的"下发人"角色
-- ============================================================
INSERT INTO sys_role (id, role_code, role_name, data_access_mode, is_enabled, remark, created_at, updated_at)
SELECT nextval('sys_role_id_seq'), 'ROLE_ISSUER', '下发人', 'OWN_ORG', true,
       '职能部门下发指标的角色', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'ROLE_ISSUER');

-- ============================================================
-- Step 2: 禁用所有现有用户 & 清除角色绑定
-- ============================================================
UPDATE sys_user SET is_active = false WHERE is_active = true;
DELETE FROM sys_user_role;

-- ============================================================
-- Step 3: 创建新用户
-- 密码 admin123 的 BCrypt hash
-- ============================================================

-- -------------------- 战略发展部 (org_id=35) --------------------
-- 1 管理员 + 2 审核人
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'zlb_admin',  '战略部管理员',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 35, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'zlb_audit1', '战略部审核人1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 35, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'zlb_audit2', '战略部审核人2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 35, true, NOW(), NOW());

-- -------------------- 职能部门 (org_id=36-54) --------------------
-- 每个部门: 1 填报人 + 2 审核人 + 1 下发人

-- org_id=36 党委办公室 | 党委统战部
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'dangban_report', '党委办填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 36, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'dangban_audit1', '党委办审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 36, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'dangban_audit2', '党委办审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 36, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'dangban_issue',  '党委办下发人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 36, true, NOW(), NOW());

-- org_id=37 纪委办公室 | 监察处
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'jiwei_report', '纪委办填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 37, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'jiwei_audit1', '纪委办审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 37, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'jiwei_audit2', '纪委办审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 37, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'jiwei_issue',  '纪委办下发人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 37, true, NOW(), NOW());

-- org_id=38 党委宣传部 | 宣传策划部
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'dangxuan_report', '宣传部填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 38, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'dangxuan_audit1', '宣传部审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 38, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'dangxuan_audit2', '宣传部审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 38, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'dangxuan_issue',  '宣传部下发人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 38, true, NOW(), NOW());

-- org_id=39 党委组织部 | 党委教师工作部
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'zuzhi_report', '组织部填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 39, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'zuzhi_audit1', '组织部审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 39, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'zuzhi_audit2', '组织部审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 39, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'zuzhi_issue',  '组织部下发人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 39, true, NOW(), NOW());

-- org_id=40 人力资源部
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'renli_report', '人力部填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 40, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'renli_audit1', '人力部审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 40, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'renli_audit2', '人力部审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 40, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'renli_issue',  '人力部下发人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 40, true, NOW(), NOW());

-- org_id=41 党委学工部 | 学生工作处
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'xuegong_report', '学工部填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 41, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'xuegong_audit1', '学工部审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 41, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'xuegong_audit2', '学工部审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 41, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'xuegong_issue',  '学工部下发人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 41, true, NOW(), NOW());

-- org_id=42 党委保卫部 | 保卫处
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'baowei_report', '保卫处填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 42, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'baowei_audit1', '保卫处审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 42, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'baowei_audit2', '保卫处审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 42, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'baowei_issue',  '保卫处下发人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 42, true, NOW(), NOW());

-- org_id=43 学校综合办公室
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'zonghe_report', '综合办填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 43, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'zonghe_audit1', '综合办审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 43, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'zonghe_audit2', '综合办审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 43, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'zonghe_issue',  '综合办下发人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 43, true, NOW(), NOW());

-- org_id=44 教务处
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'jiaowu_report', '教务处填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 44, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'jiaowu_audit1', '教务处审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 44, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'jiaowu_audit2', '教务处审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 44, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'jiaowu_issue',  '教务处下发人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 44, true, NOW(), NOW());

-- org_id=45 科技处
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'keji_report', '科技处填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 45, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'keji_audit1', '科技处审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 45, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'keji_audit2', '科技处审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 45, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'keji_issue',  '科技处下发人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 45, true, NOW(), NOW());

-- org_id=46 财务部
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'caiwu_report', '财务部填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 46, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'caiwu_audit1', '财务部审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 46, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'caiwu_audit2', '财务部审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 46, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'caiwu_issue',  '财务部下发人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 46, true, NOW(), NOW());

-- org_id=47 招生工作处
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'zhaosheng_report', '招生处填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 47, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'zhaosheng_audit1', '招生处审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 47, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'zhaosheng_audit2', '招生处审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 47, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'zhaosheng_issue',  '招生处下发人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 47, true, NOW(), NOW());

-- org_id=48 就业创业指导中心
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'jiuye_report', '就业中心填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 48, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'jiuye_audit1', '就业中心审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 48, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'jiuye_audit2', '就业中心审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 48, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'jiuye_issue',  '就业中心下发人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 48, true, NOW(), NOW());

-- org_id=49 实验室建设管理处
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'shiyanshi_report', '实验室填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 49, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'shiyanshi_audit1', '实验室审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 49, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'shiyanshi_audit2', '实验室审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 49, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'shiyanshi_issue',  '实验室下发人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 49, true, NOW(), NOW());

-- org_id=50 数字校园建设办公室
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'shuzi_report', '数字校园填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 50, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'shuzi_audit1', '数字校园审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 50, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'shuzi_audit2', '数字校园审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 50, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'shuzi_issue',  '数字校园下发人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 50, true, NOW(), NOW());

-- org_id=51 图书馆 | 档案馆
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'tushu_report', '图书馆填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 51, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'tushu_audit1', '图书馆审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 51, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'tushu_audit2', '图书馆审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 51, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'tushu_issue',  '图书馆下发人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 51, true, NOW(), NOW());

-- org_id=52 后勤资产处
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'houqin_report', '后勤处填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 52, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'houqin_audit1', '后勤处审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 52, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'houqin_audit2', '后勤处审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 52, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'houqin_issue',  '后勤处下发人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 52, true, NOW(), NOW());

-- org_id=53 继续教育部
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'jixu_report', '继续教育填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 53, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'jixu_audit1', '继续教育审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 53, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'jixu_audit2', '继续教育审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 53, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'jixu_issue',  '继续教育下发人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 53, true, NOW(), NOW());

-- org_id=54 国际合作与交流处
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'guoji_report', '国际处填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 54, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'guoji_audit1', '国际处审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 54, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'guoji_audit2', '国际处审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 54, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'guoji_issue',  '国际处下发人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 54, true, NOW(), NOW());

-- -------------------- 二级学院 (org_id=55-62) --------------------
-- 每个学院: 1 填报人 + 2 审核人

-- org_id=55 马克思主义学院
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'makesi_report', '马院填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 55, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'makesi_audit1', '马院审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 55, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'makesi_audit2', '马院审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 55, true, NOW(), NOW());

-- org_id=56 工学院
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'gongxue_report', '工学院填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 56, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'gongxue_audit1', '工学院审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 56, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'gongxue_audit2', '工学院审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 56, true, NOW(), NOW());

-- org_id=57 计算机学院
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'jisuanji_report', '计算机学院填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 57, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'jisuanji_audit1', '计算机学院审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 57, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'jisuanji_audit2', '计算机学院审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 57, true, NOW(), NOW());

-- org_id=58 商学院
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'shangxue_report', '商学院填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 58, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'shangxue_audit1', '商学院审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 58, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'shangxue_audit2', '商学院审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 58, true, NOW(), NOW());

-- org_id=59 文理学院
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'wenli_report', '文理学院填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 59, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'wenli_audit1', '文理学院审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 59, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'wenli_audit2', '文理学院审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 59, true, NOW(), NOW());

-- org_id=60 艺术与科技学院
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'yishu_report', '艺术学院填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 60, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'yishu_audit1', '艺术学院审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 60, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'yishu_audit2', '艺术学院审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 60, true, NOW(), NOW());

-- org_id=61 航空学院
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'hangkong_report', '航空学院填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 61, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'hangkong_audit1', '航空学院审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 61, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'hangkong_audit2', '航空学院审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 61, true, NOW(), NOW());

-- org_id=62 国际教育学院
INSERT INTO sys_user (id, username, real_name, password_hash, org_id, is_active, created_at, updated_at) VALUES
(nextval('sys_user_user_id_seq'), 'guojiaoyu_report', '国际教育学院填报人',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 62, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'guojiaoyu_audit1', '国际教育学院审核人1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 62, true, NOW(), NOW()),
(nextval('sys_user_user_id_seq'), 'guojiaoyu_audit2', '国际教育学院审核人2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 62, true, NOW(), NOW());


-- ============================================================
-- Step 4: 分配角色
-- ============================================================
-- 角色ID对照:
--   5  = ROLE_REPORTER (填报人)
--   6  = ROLE_FUNC_DEPT_HEAD (职能部门负责人 → 用作职能部门审核人)
--   7  = ROLE_COLLEGE_DEAN (二级学院院长 → 用作学院审核人)
--   8  = STRATEGY_DEPT_HEAD (战略发展部负责人 → 管理员)
--   10 = ROLE_STRATEGY_OFFICE (战略发展部 → 战略部审核人)
--   11 = ROLE_ISSUER (下发人 → 新建的角色)

-- 战略发展部: 管理员(role=8) + 审核人(role=10)
INSERT INTO sys_user_role (id, user_id, role_id, created_at)
SELECT nextval('sys_user_role_id_seq'), u.id,
       CASE
           WHEN u.username = 'zlb_admin' THEN 8   -- STRATEGY_DEPT_HEAD
           ELSE 10                                  -- ROLE_STRATEGY_OFFICE
       END,
       NOW()
FROM sys_user u
WHERE u.username IN ('zlb_admin', 'zlb_audit1', 'zlb_audit2')
  AND u.is_active = true;

-- 职能部门: 填报人(role=5) + 审核人(role=6) + 下发人(role=11)
INSERT INTO sys_user_role (id, user_id, role_id, created_at)
SELECT nextval('sys_user_role_id_seq'), u.id,
       CASE
           WHEN u.username LIKE '%_report' THEN 5   -- ROLE_REPORTER
           WHEN u.username LIKE '%_issue'  THEN (SELECT id FROM sys_role WHERE role_code = 'ROLE_ISSUER')
           ELSE 6                                     -- ROLE_FUNC_DEPT_HEAD (审核人)
       END,
       NOW()
FROM sys_user u
WHERE u.org_id BETWEEN 36 AND 54
  AND u.is_active = true
  AND u.username LIKE '%\_%';

-- 二级学院: 填报人(role=5) + 审核人(role=7)
INSERT INTO sys_user_role (id, user_id, role_id, created_at)
SELECT nextval('sys_user_role_id_seq'), u.id,
       CASE
           WHEN u.username LIKE '%_report' THEN 5   -- ROLE_REPORTER
           ELSE 7                                     -- ROLE_COLLEGE_DEAN (审核人)
       END,
       NOW()
FROM sys_user u
WHERE u.org_id BETWEEN 55 AND 62
  AND u.is_active = true
  AND u.username LIKE '%\_%';

COMMIT;

-- ============================================================
-- 验证查询
-- ============================================================
-- SELECT u.username, u.real_name, o.name AS dept, r.role_name, r.role_code
-- FROM sys_user u
-- JOIN sys_org o ON o.id = u.org_id
-- LEFT JOIN sys_user_role ur ON ur.user_id = u.id
-- LEFT JOIN sys_role r ON r.id = ur.role_id
-- WHERE u.is_active = true
-- ORDER BY u.org_id, u.username;
