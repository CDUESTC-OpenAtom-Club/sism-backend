-- 修复 keyan 用户登录问题
-- 问题: keyan 用户登录返回 401 未授权
-- 日期: 2026-01-28

-- 1. 检查 keyan 用户是否存在
SELECT 
    user_id, 
    username, 
    real_name, 
    org_id, 
    is_active,
    LEFT(password_hash, 30) || '...' AS password_preview
FROM app_user
WHERE username = 'keyan';

-- 2. 检查科研处组织是否存在
SELECT org_id, org_name, org_type
FROM org
WHERE org_name = '科研处' OR org_id = 3;

-- 3. 修复或插入 keyan 用户
-- 密码: 123456
-- 密码哈希: $2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi
INSERT INTO app_user (username, real_name, org_id, password_hash, is_active)
VALUES ('keyan', '王科研', 3, '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi', TRUE)
ON CONFLICT (username) DO UPDATE
SET 
    password_hash = EXCLUDED.password_hash,
    is_active = TRUE,
    real_name = EXCLUDED.real_name,
    org_id = EXCLUDED.org_id,
    updated_at = CURRENT_TIMESTAMP;

-- 4. 验证修复结果
SELECT 
    user_id, 
    username, 
    real_name, 
    org_id, 
    is_active,
    LEFT(password_hash, 30) || '...' AS password_preview,
    created_at,
    updated_at
FROM app_user
WHERE username = 'keyan';

-- 5. 验证所有测试账号
SELECT 
    u.username, 
    u.real_name, 
    o.org_name, 
    u.is_active,
    LEFT(u.password_hash, 30) || '...' AS password_preview
FROM app_user u
LEFT JOIN org o ON u.org_id = o.org_id
WHERE u.username IN ('zhanlue', 'jiaowu', 'keyan', 'admin')
ORDER BY u.username;
