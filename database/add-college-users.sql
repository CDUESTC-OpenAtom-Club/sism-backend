-- ============================================
-- 添加二级学院测试用户
-- 密码: 123456 (bcrypt hash)
-- 执行日期: 2026-01-27
-- 
-- 生产环境组织ID映射:
-- 55 - 马克思主义学院
-- 56 - 工学院
-- 57 - 计算机学院
-- 58 - 商学院
-- 59 - 文理学院
-- 60 - 艺术与科技学院
-- 61 - 航空学院
-- 62 - 国际教育学院
-- ============================================

-- 密码: 123456 的 bcrypt 哈希值
-- $2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi

-- 马克思主义学院用户 (org_id = 55)
INSERT INTO app_user (username, real_name, org_id, password_hash, is_active)
SELECT 'makesi', '马克思主义学院测试用户', 55, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi', TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'makesi');

-- 工学院用户 (org_id = 56)
INSERT INTO app_user (username, real_name, org_id, password_hash, is_active)
SELECT 'gongxue', '工学院测试用户', 56, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi', TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'gongxue');

-- 计算机学院用户 (org_id = 57)
INSERT INTO app_user (username, real_name, org_id, password_hash, is_active)
SELECT 'jisuanji', '计算机学院测试用户', 57, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi', TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'jisuanji');

-- 商学院用户 (org_id = 58)
INSERT INTO app_user (username, real_name, org_id, password_hash, is_active)
SELECT 'shangxue', '商学院测试用户', 58, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi', TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'shangxue');

-- 文理学院用户 (org_id = 59)
INSERT INTO app_user (username, real_name, org_id, password_hash, is_active)
SELECT 'wenli', '文理学院测试用户', 59, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi', TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'wenli');

-- 艺术与科技学院用户 (org_id = 60)
INSERT INTO app_user (username, real_name, org_id, password_hash, is_active)
SELECT 'yishu', '艺术与科技学院测试用户', 60, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi', TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'yishu');

-- 航空学院用户 (org_id = 61)
INSERT INTO app_user (username, real_name, org_id, password_hash, is_active)
SELECT 'hangkong', '航空学院测试用户', 61, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi', TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'hangkong');

-- 国际教育学院用户 (org_id = 62)
INSERT INTO app_user (username, real_name, org_id, password_hash, is_active)
SELECT 'guojijiaoyu', '国际教育学院测试用户', 62, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi', TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'guojijiaoyu');

-- 验证插入结果
SELECT u.user_id, u.username, u.real_name, o.org_name, o.org_type, u.is_active
FROM app_user u
JOIN org o ON u.org_id = o.org_id
WHERE o.org_type = 'COLLEGE'
ORDER BY u.user_id;
