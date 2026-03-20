-- alert_rule clean seed
-- Scope:
-- - Keep a minimal rule set for 2026 plan/indicator progress monitoring.

BEGIN;

INSERT INTO public.alert_rule (
    rule_id,
    created_at,
    updated_at,
    gap_threshold,
    is_enabled,
    name,
    severity,
    cycle_id
)
VALUES
    (7201, NOW(), NOW(), 10.00, true, '2026进度轻度偏差提醒', 'WARNING', 4),
    (7202, NOW(), NOW(), 20.00, true, '2026进度严重偏差提醒', 'CRITICAL', 4),
    (7203, NOW(), NOW(), 35.00, true, '2026进度危急偏差提醒', 'CRITICAL', 4)
ON CONFLICT (rule_id) DO UPDATE
SET
    updated_at = EXCLUDED.updated_at,
    gap_threshold = EXCLUDED.gap_threshold,
    is_enabled = EXCLUDED.is_enabled,
    name = EXCLUDED.name,
    severity = EXCLUDED.severity,
    cycle_id = EXCLUDED.cycle_id;

COMMIT;
