-- =====================================================
-- 修复职能部门用户密码
-- =====================================================
-- 问题: 职能部门账号全部无法登录
-- 原因: 密码哈希与 admin 不一致
-- 解决: 统一使用与 admin 相同的密码哈希 (密码: 123456)
-- =====================================================

-- 正确的密码哈希 (123456)
-- 与 admin 用户使用相同的哈希值
UPDATE app_user 
SET password_hash = '$2a$10$UF.UUADlBmXZU1tU3iec3OK5lfK4TOvVxErggE0HGPguRTiOO/dmi',
    updated_at = CURRENT_TIMESTAMP
WHERE username IN (
    'jiaowu',      -- 教务处
    'xuegong',     -- 学工处  
    'keyan',       -- 科研处
    'renshichu',   -- 人事处
    'caiwuchu',    -- 财务处
    'zichan',      -- 资产处
    'houqin',      -- 后勤处
    'guoji',       -- 国际处
    'xuanchuan',   -- 宣传部
    'zuzhi'        -- 组织部
);

-- 验证更新结果
SELECT 
    username,
    real_name,
    LEFT(password_hash, 30) as pwd_prefix,
    updated_at
FROM app_user 
WHERE username IN (
    'jiaowu', 'xuegong', 'keyan', 'renshichu', 'caiwuchu',
    'zichan', 'houqin', 'guoji', 'xuanchuan', 'zuzhi'
)
ORDER BY username;

-- 预期结果: 所有职能部门用户的密码哈希前缀应为 $2a$10$UF.UUADlBmXZU1tU3iec
