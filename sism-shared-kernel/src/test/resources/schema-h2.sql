-- Test schema for event infrastructure testing
-- This is ONLY for H2 in-memory database, does NOT affect any business tables

-- Event Store table (infrastructure-only, for testing)
CREATE TABLE IF NOT EXISTS event_store (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    version BIGINT DEFAULT 0
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_event_type ON event_store(event_type);
CREATE INDEX IF NOT EXISTS idx_aggregate_id ON event_store(aggregate_id);
CREATE INDEX IF NOT EXISTS idx_occurred_on ON event_store(occurred_on);
CREATE INDEX IF NOT EXISTS idx_is_processed ON event_store(is_processed);
