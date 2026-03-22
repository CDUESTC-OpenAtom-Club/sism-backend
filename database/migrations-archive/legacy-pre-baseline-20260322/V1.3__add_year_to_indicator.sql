-- Migration: Add year field to indicator table
-- Date: 2026-02-08
-- Purpose: Fix dashboard display issue - indicators need year field for filtering

-- Add year column
ALTER TABLE indicator 
ADD COLUMN IF NOT EXISTS year INTEGER;

-- Set default year to 2026 for all existing indicators
UPDATE indicator
SET year = 2026
WHERE year IS NULL;

-- Add index for better query performance
CREATE INDEX IF NOT EXISTS idx_indicator_year ON indicator(year);

-- Add comment
COMMENT ON COLUMN indicator.year IS '指标所属年份';
