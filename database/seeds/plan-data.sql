-- plan clean seed
-- Business rule:
-- - Every year, every target organization should own a plan container,
--   even when there is no concrete task yet.
-- - Functional departments use STRAT_TO_FUNC containers created by strategy office.
-- - Colleges use FUNC_TO_COLLEGE containers. Here we use org 44 (教务处) as the
--   default academic-plan creator for clean seed purposes.
-- Status rule:
-- - plan.status is the authoritative package-level distribution status.
-- - indicator.status may exist as a downstream projection/lifecycle field, but when the two
--   differ, plan.status is the source-of-truth for the overall package.
-- - Most auto-generated containers stay at DRAFT baseline; only the representative 2026 examples
--   below are lifted to concrete business states for review readability.

BEGIN;

INSERT INTO public.plan (
    id,
    cycle_id,
    created_at,
    updated_at,
    is_deleted,
    target_org_id,
    created_by_org_id,
    plan_level,
    status
)
SELECT
    c.id * 1000 + o.id AS id,
    c.id AS cycle_id,
    NOW(),
    NOW(),
    false,
    o.id AS target_org_id,
    CASE
        WHEN o.id BETWEEN 36 AND 54 THEN 35
        ELSE 44
    END AS created_by_org_id,
    CASE
        WHEN o.id BETWEEN 36 AND 54 THEN 'STRAT_TO_FUNC'
        ELSE 'FUNC_TO_COLLEGE'
    END::plan_level AS plan_level,
    CASE
        WHEN c.id = 4 AND o.id = 36 THEN 'DISTRIBUTED'
        WHEN c.id = 4 AND o.id = 42 THEN 'PENDING'
        WHEN c.id = 4 AND o.id = 55 THEN 'DISTRIBUTED'
        ELSE 'DRAFT'
    END AS status
FROM public.cycle c
JOIN public.sys_org o
    ON o.id BETWEEN 36 AND 62
WHERE o.is_deleted = false
ON CONFLICT (id) DO UPDATE
SET
    cycle_id = EXCLUDED.cycle_id,
    updated_at = EXCLUDED.updated_at,
    is_deleted = EXCLUDED.is_deleted,
    target_org_id = EXCLUDED.target_org_id,
    created_by_org_id = EXCLUDED.created_by_org_id,
    plan_level = EXCLUDED.plan_level,
    status = EXCLUDED.status;

COMMIT;
