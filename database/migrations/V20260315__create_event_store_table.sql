-- Flyway migration: create event_store table
-- This table stores serialized domain events for EventStoreDatabase
CREATE TABLE IF NOT EXISTS event_store (
  event_id VARCHAR(128) PRIMARY KEY,
  event_type VARCHAR(255) NOT NULL,
  aggregate_id VARCHAR(128),
  occurred_on TIMESTAMP WITH TIME ZONE NOT NULL,
  payload TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_event_store_event_type ON event_store(event_type);
CREATE INDEX IF NOT EXISTS idx_event_store_aggregate_id ON event_store(aggregate_id);
CREATE INDEX IF NOT EXISTS idx_event_store_occurred_on ON event_store(occurred_on);
