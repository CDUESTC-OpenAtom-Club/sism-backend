-- plan clean seed
-- Business rule:
-- - plan is a distribution container, not a self-owned annual container.
-- - Only strategy -> functional department containers are pre-seeded.
-- - functional -> college containers must be created by actual upstream distribution flow
--   and must not be pre-generated for colleges.
-- Status rule:
-- - plan.status is the authoritative package-level distribution status.
-- - indicator.status may exist as a downstream projection/lifecycle field, but when the two
--   differ, plan.status is the source-of-truth for the overall package.
-- - All clean-seed plan containers stay at the DRAFT baseline.

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
    status,
    audit_instance_id
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
    'DRAFT' AS status,
    NULL::BIGINT AS audit_instance_id
FROM public.cycle c
JOIN public.sys_org o
    ON o.id BETWEEN 36 AND 54
WHERE o.is_deleted = false
ON CONFLICT (id) DO UPDATE
SET
    cycle_id = EXCLUDED.cycle_id,
    updated_at = EXCLUDED.updated_at,
    is_deleted = EXCLUDED.is_deleted,
    target_org_id = EXCLUDED.target_org_id,
    created_by_org_id = EXCLUDED.created_by_org_id,
    plan_level = EXCLUDED.plan_level,
    status = EXCLUDED.status,
    audit_instance_id = EXCLUDED.audit_instance_id;

COMMIT;
