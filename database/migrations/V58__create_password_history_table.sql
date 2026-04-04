-- V58: 创建密码历史表
-- 用于存储用户历史密码，防止重复使用最近的密码

CREATE TABLE IF NOT EXISTS password_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_history_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

CREATE INDEX idx_password_history_user_id ON password_history(user_id);
CREATE INDEX idx_password_history_created_at ON password_history(created_at DESC);

COMMENT ON TABLE password_history IS '密码历史记录表';
COMMENT ON COLUMN password_history.id IS '主键ID';
COMMENT ON COLUMN password_history.user_id IS '用户ID';
COMMENT ON COLUMN password_history.password_hash IS '密码哈希值';
COMMENT ON COLUMN password_history.created_at IS '创建时间';
