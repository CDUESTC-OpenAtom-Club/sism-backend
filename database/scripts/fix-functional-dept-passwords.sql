-- ============================================
-- 修复职能部门用户密码
-- 将所有职能部门用户的密码重置为 123456
-- 使用 Spring Security 兼容的 BCrypt hash ($2a$ 前缀)
-- ============================================

-- 密码: 123456
-- BCrypt Hash ($2a$ 前缀,与 Spring Security 兼容): $2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi

BEGIN;

-- 更新所有职能部门用户的密码
UPDATE app_user
SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi',
    updated_at = CURRENT_TIMESTAMP
WHERE org_id IN (
    SELECT org_id 
    FROM org 
    WHERE org_type = 'FUNCTIONAL_DEPT'
);

-- 验证更新结果
SELECT 
    u.username,
    u.real_name,
    o.org_name,
    o.org_type,
    SUBSTRING(u.password_hash, 1, 10) as hash_prefix,
    u.updated_at
FROM app_user u
JOIN org o ON u.org_id = o.org_id
WHERE o.org_type = 'FUNCTIONAL_DEPT'
ORDER BY u.username;

COMMIT;

-- 输出统计信息
SELECT 
    '职能部门用户密码已重置' as message,
    COUNT(*) as affected_users
FROM app_user u
JOIN org o ON u.org_id = o.org_id
WHERE o.org_type = 'FUNCTIONAL_DEPT';
