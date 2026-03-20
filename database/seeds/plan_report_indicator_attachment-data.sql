-- plan_report_indicator_attachment clean seed
-- Scope:
-- - Bind report-indicator records to physical attachments in concrete business scenes.

BEGIN;

DELETE FROM public.plan_report_indicator_attachment
WHERE id IN (8203, 8204);

INSERT INTO public.plan_report_indicator_attachment (
    id,
    plan_report_indicator_id,
    attachment_id,
    sort_order,
    created_by,
    created_at
)
VALUES
    (8201, 3501, 8101, 1, 191, NOW()),
    (8202, 3503, 8102, 1, 215, NOW())
ON CONFLICT (id) DO UPDATE
SET
    plan_report_indicator_id = EXCLUDED.plan_report_indicator_id,
    attachment_id = EXCLUDED.attachment_id,
    sort_order = EXCLUDED.sort_order,
    created_by = EXCLUDED.created_by,
    created_at = EXCLUDED.created_at;

COMMIT;
