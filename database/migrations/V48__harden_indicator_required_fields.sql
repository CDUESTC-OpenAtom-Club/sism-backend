-- Harden indicator required fields and defaults.
-- Goal: keep core business fields in a valid state even if upstream code misses a value.

BEGIN;

-- 1. Backfill existing rows before tightening constraints.
UPDATE public.indicator
SET indicator_desc = CONCAT('指标-', id)
WHERE indicator_desc IS NULL OR btrim(indicator_desc) = '';

UPDATE public.indicator
SET weight_percent = 100
WHERE weight_percent IS NULL OR weight_percent <= 0;

UPDATE public.indicator
SET sort_order = 0
WHERE sort_order IS NULL OR sort_order < 0;

UPDATE public.indicator
SET owner_org_id = target_org_id
WHERE owner_org_id IS NULL
  AND target_org_id IS NOT NULL;

UPDATE public.indicator
SET target_org_id = owner_org_id
WHERE target_org_id IS NULL
  AND owner_org_id IS NOT NULL;

UPDATE public.indicator
SET status = 'DRAFT'
WHERE status IS NULL OR btrim(status) = '';

UPDATE public.indicator
SET progress = 0
WHERE progress IS NULL;

UPDATE public.indicator
SET is_enabled = TRUE
WHERE is_enabled IS NULL;

UPDATE public.indicator
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

UPDATE public.indicator
SET updated_at = CURRENT_TIMESTAMP
WHERE updated_at IS NULL;

-- 2. Align defaults with domain expectations.
ALTER TABLE public.indicator
  ALTER COLUMN weight_percent SET DEFAULT 100,
  ALTER COLUMN sort_order SET DEFAULT 0,
  ALTER COLUMN status SET DEFAULT 'DRAFT',
  ALTER COLUMN progress SET DEFAULT 0,
  ALTER COLUMN is_enabled SET DEFAULT TRUE,
  ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
  ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

-- 3. Tighten nullability for core fields that are mandatory in backend logic.
ALTER TABLE public.indicator
  ALTER COLUMN owner_org_id SET NOT NULL,
  ALTER COLUMN target_org_id SET NOT NULL,
  ALTER COLUMN status SET NOT NULL,
  ALTER COLUMN progress SET NOT NULL,
  ALTER COLUMN is_enabled SET NOT NULL;

-- 4. Add defensive checks that match service/domain rules.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'indicator_weight_percent_positive_check'
  ) THEN
    ALTER TABLE public.indicator
      ADD CONSTRAINT indicator_weight_percent_positive_check
      CHECK (weight_percent > 0);
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'indicator_sort_order_non_negative_check'
  ) THEN
    ALTER TABLE public.indicator
      ADD CONSTRAINT indicator_sort_order_non_negative_check
      CHECK (sort_order >= 0);
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'indicator_desc_not_blank_check'
  ) THEN
    ALTER TABLE public.indicator
      ADD CONSTRAINT indicator_desc_not_blank_check
      CHECK (btrim(indicator_desc) <> '');
  END IF;
END $$;

COMMIT;
