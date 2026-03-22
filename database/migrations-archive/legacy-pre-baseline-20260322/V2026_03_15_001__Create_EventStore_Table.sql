-- database/migrations/V2026_03_15_001__Create_EventStore_Table.sql

-- 创建事件存储表
CREATE TABLE IF NOT EXISTS event_store (
    id BIGINT PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255),
    aggregate_type VARCHAR(255),
    event_data TEXT NOT NULL,
    occurred_on TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP,
    error_message TEXT,
    version BIGINT DEFAULT 0,
    CONSTRAINT pk_event_store PRIMARY KEY (id)
);

-- 创建索引以提高查询性能
CREATE INDEX idx_event_type ON event_store(event_type);
CREATE INDEX idx_aggregate_id ON event_store(aggregate_id);
CREATE INDEX idx_occurred_on ON event_store(occurred_on);
CREATE INDEX idx_is_processed ON event_store(is_processed);
CREATE INDEX idx_event_id ON event_store(event_id);
CREATE INDEX idx_aggregate_id_created_at ON event_store(aggregate_id, created_at);

-- 创建序列器
CREATE SEQUENCE event_store_id_seq START WITH 1 INCREMENT BY 1;

-- 添加注释
COMMENT ON TABLE event_store IS '领域事件存储表，用于事件溯源和审计';
COMMENT ON COLUMN event_store.event_id IS '事件唯一标识';
COMMENT ON COLUMN event_store.event_type IS '事件类型';
COMMENT ON COLUMN event_store.aggregate_id IS '聚合根ID';
COMMENT ON COLUMN event_store.event_data IS '事件数据（JSON格式）';
COMMENT ON COLUMN event_store.is_processed IS '事件是否已被消费处理';

-- 验证表创建成功
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'event_store') THEN
        RAISE NOTICE 'Event store table created successfully';
    ELSE
        RAISE EXCEPTION 'Failed to create event_store table';
    END IF;
END
$$;
