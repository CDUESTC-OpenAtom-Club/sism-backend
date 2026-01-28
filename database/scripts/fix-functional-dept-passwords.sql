-- 修复职能部门用户密码
-- 所有密码重置为: 123456
-- BCrypt哈希: $2a$10$UF.UUADlBmXZUqvuj/yuuOehOFkcLBfCYmWc7WROCiuGHMVVceWMS

-- 更新所有职能部门用户的密码
UPDATE app_user
SET password_hash = '$2a$10$UF.UUADlBmXZUqvuj/yuuOehOFkcLBfCYmWc7WROCiuGHMVVceWMS',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id IN (
    SELECT u.user_id
    FROM app_user u
    JOIN org o ON u.org_id = o.org_id
    WHERE o.org_type = 'FUNCTIONAL_DEPT' OR o.org_type = 'FUNCTION_DEPT'
);

-- 验证更新结果
SELECT 
    u.username,
    u.real_name,
    o.org_name,
    LENGTH(u.password_hash) as hash_length,
    SUBSTRING(u.password_hash, 1, 20) as hash_prefix
FROM app_user u
JOIN org o ON u.org_id = o.org_id
WHERE o.org_type = 'FUNCTIONAL_DEPT' OR o.org_type = 'FUNCTION_DEPT'
ORDER BY u.username;
