-- alert_event clean seed
-- Scope:
-- - Provide representative warning events derived from indicator progress gaps.

BEGIN;

DELETE FROM public.alert_event
WHERE event_id IN (7303);

INSERT INTO public.alert_event (
    event_id,
    created_at,
    updated_at,
    actual_percent,
    detail_json,
    expected_percent,
    gap_percent,
    handled_note,
    severity,
    status,
    handled_by,
    indicator_id,
    rule_id,
    window_id
)
VALUES
    (
        7301,
        NOW(),
        NOW(),
        20.00,
        '{"plan_id":4042,"target_org_id":42,"report_id":3002,"source":"plan_report_indicator"}'::jsonb,
        30.00,
        10.00,
        NULL,
        'WARNING',
        'OPEN',
        NULL,
        2003,
        7201,
        7101
    ),
    (
        7302,
        NOW(),
        NOW(),
        10.00,
        '{"plan_id":4042,"target_org_id":42,"report_id":3002,"source":"plan_report_indicator"}'::jsonb,
        30.00,
        20.00,
        '教务处已提醒保卫处补充整改清单',
        'CRITICAL',
        'RESOLVED',
        224,
        2004,
        7202,
        7101
    )
ON CONFLICT (event_id) DO UPDATE
SET
    updated_at = EXCLUDED.updated_at,
    actual_percent = EXCLUDED.actual_percent,
    detail_json = EXCLUDED.detail_json,
    expected_percent = EXCLUDED.expected_percent,
    gap_percent = EXCLUDED.gap_percent,
    handled_note = EXCLUDED.handled_note,
    severity = EXCLUDED.severity,
    status = EXCLUDED.status,
    handled_by = EXCLUDED.handled_by,
    indicator_id = EXCLUDED.indicator_id,
    rule_id = EXCLUDED.rule_id,
    window_id = EXCLUDED.window_id;

COMMIT;
