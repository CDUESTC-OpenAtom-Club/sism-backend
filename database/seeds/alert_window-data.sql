-- alert_window clean seed
-- Scope:
-- - Provide warning check windows for the current 2026 cycle.

BEGIN;

INSERT INTO public.alert_window (
    window_id,
    created_at,
    updated_at,
    cutoff_date,
    is_default,
    name,
    cycle_id
)
VALUES
    (7101, NOW(), NOW(), DATE '2026-03-31', true, '2026年3月预警窗口', 4),
    (7102, NOW(), NOW(), DATE '2026-06-30', false, '2026年6月预警窗口', 4),
    (7103, NOW(), NOW(), DATE '2026-09-30', false, '2026年9月预警窗口', 4),
    (7104, NOW(), NOW(), DATE '2026-12-31', false, '2026年12月预警窗口', 4)
ON CONFLICT (window_id) DO UPDATE
SET
    updated_at = EXCLUDED.updated_at,
    cutoff_date = EXCLUDED.cutoff_date,
    is_default = EXCLUDED.is_default,
    name = EXCLUDED.name,
    cycle_id = EXCLUDED.cycle_id;

COMMIT;
