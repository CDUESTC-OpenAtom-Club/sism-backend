-- Remove deprecated indicator.distribution_status after status-based rollout is complete.
-- The compatibility field has drifted from indicator.status and is no longer used by the main code path.

ALTER TABLE public.indicator
    DROP CONSTRAINT IF EXISTS indicator_distribution_status_check;

ALTER TABLE public.indicator
    DROP COLUMN IF EXISTS distribution_status;
