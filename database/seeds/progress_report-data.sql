-- progress_report clean seed
-- Scope:
-- - progress_report is an output/reporting table owned by analytics runtime flows.
-- - Clean seed baseline keeps this table empty.

BEGIN;

INSERT INTO public.progress_report (
    id,
    report_name,
    report_type,
    report_format,
    status,
    file_path,
    file_size,
    creator_id,
    generation_time,
    report_params,
    description,
    is_deleted,
    created_at,
    updated_at
)
SELECT
    NULL::bigint,
    NULL::varchar,
    NULL::varchar,
    NULL::varchar,
    NULL::varchar,
    NULL::varchar,
    NULL::bigint,
    NULL::bigint,
    NULL::timestamp,
    NULL::jsonb,
    NULL::text,
    NULL::boolean,
    NULL::timestamp,
    NULL::timestamp
WHERE FALSE;

COMMIT;
