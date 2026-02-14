-- ============================================
-- Add Warning Level Entity
-- Version: 3.0
-- Date: 2026-02-13
-- Description: Add warning level configuration table
-- ============================================

-- Create warn_level table
CREATE TABLE IF NOT EXISTS warn_level (
    id BIGSERIAL PRIMARY KEY,
    level_name VARCHAR(100) NOT NULL,
    level_code VARCHAR(50) NOT NULL UNIQUE,
    threshold_value INTEGER NOT NULL CHECK (threshold_value >= 0),
    severity VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_level_code UNIQUE (level_code)
);

-- Create indexes
CREATE INDEX idx_warn_level_severity ON warn_level(severity);
CREATE INDEX idx_warn_level_active ON warn_level(is_active);
CREATE INDEX idx_warn_level_severity_active ON warn_level(severity, is_active);

-- Add comments
COMMENT ON TABLE warn_level IS 'Warning level definitions with configurable thresholds';
COMMENT ON COLUMN warn_level.level_code IS 'Unique level identifier code';
COMMENT ON COLUMN warn_level.threshold_value IS 'Threshold value for triggering this warning level';
COMMENT ON COLUMN warn_level.severity IS 'Severity: LOW, MEDIUM, HIGH, CRITICAL';
COMMENT ON COLUMN warn_level.is_active IS 'Whether this warning level is active';

-- Insert default warning levels
INSERT INTO warn_level (level_name, level_code, threshold_value, severity, description, is_active) VALUES
('Low Warning', 'WARN_LOW', 70, 'LOW', 'Progress below 70% of target', true),
('Medium Warning', 'WARN_MEDIUM', 50, 'MEDIUM', 'Progress below 50% of target', true),
('High Warning', 'WARN_HIGH', 30, 'HIGH', 'Progress below 30% of target', true),
('Critical Warning', 'WARN_CRITICAL', 10, 'CRITICAL', 'Progress below 10% of target', true)
ON CONFLICT (level_code) DO NOTHING;
