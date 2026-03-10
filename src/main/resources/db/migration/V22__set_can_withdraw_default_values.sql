-- V22: Set default values for can_withdraw field
-- Purpose: Fix existing indicators with NULL can_withdraw values
-- Date: 2026-03-09

DO $$
BEGIN
    -- Set can_withdraw = true for all DRAFT and ACTIVE indicators
    -- These are indicators that haven't been distributed yet, so they should be withdrawable
    UPDATE public.indicator
    SET can_withdraw = true
    WHERE can_withdraw IS NULL
      AND status IN ('DRAFT', 'ACTIVE');

    -- Set can_withdraw = false for DISTRIBUTED indicators
    -- These have been formally distributed and should not be withdrawable by default
    UPDATE public.indicator
    SET can_withdraw = false
    WHERE can_withdraw IS NULL
      AND status = 'DISTRIBUTED';

    -- Set can_withdraw = false for ARCHIVED indicators
    -- Archived indicators should not be withdrawable
    UPDATE public.indicator
    SET can_withdraw = false
    WHERE can_withdraw IS NULL
      AND status = 'ARCHIVED';

    -- Set can_withdraw = true for any remaining NULL values (fallback)
    UPDATE public.indicator
    SET can_withdraw = true
    WHERE can_withdraw IS NULL;

    -- Add a default value for future inserts
    ALTER TABLE public.indicator 
    ALTER COLUMN can_withdraw SET DEFAULT true;

    RAISE NOTICE 'Successfully set can_withdraw default values for all indicators';
END $$;
