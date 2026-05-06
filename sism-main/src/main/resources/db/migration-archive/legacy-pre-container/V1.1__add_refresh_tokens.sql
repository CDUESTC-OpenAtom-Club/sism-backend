-- ============================================
-- SISM 企业级优化 - Refresh Token 表
-- 版本: V1.1
-- 需求: 1.2.2 实现 Refresh Token 机制
-- ============================================

-- Refresh Token 存储表
-- 用于安全存储用户的刷新令牌，支持 Token 轮换和撤销
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(user_id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP,
    device_info VARCHAR(255),
    ip_address VARCHAR(45)
);

-- 索引优化
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);

-- 注释
COMMENT ON TABLE refresh_tokens IS 'Refresh Token 存储表 - 用于安全的会话管理';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'Token 的 SHA-256 哈希值，不存储原始 Token';
COMMENT ON COLUMN refresh_tokens.expires_at IS 'Token 过期时间';
COMMENT ON COLUMN refresh_tokens.revoked_at IS 'Token 撤销时间，非空表示已撤销';
COMMENT ON COLUMN refresh_tokens.device_info IS '设备信息（User-Agent）';
COMMENT ON COLUMN refresh_tokens.ip_address IS '客户端 IP 地址';

-- 触发器: 自动更新 updated_at（如果需要）
-- 注意: refresh_tokens 表不需要 updated_at，因为 token 一旦创建就不会修改

-- ============================================
-- 清理过期 Token 的函数（可选，用于定期清理）
-- ============================================
CREATE OR REPLACE FUNCTION cleanup_expired_refresh_tokens()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM refresh_tokens 
    WHERE expires_at < CURRENT_TIMESTAMP 
       OR revoked_at IS NOT NULL;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_expired_refresh_tokens() IS '清理过期或已撤销的 Refresh Token';
