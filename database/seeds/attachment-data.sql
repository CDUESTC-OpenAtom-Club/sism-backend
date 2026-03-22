-- attachment clean seed
-- Scope:
-- - attachment stores uploaded physical file metadata.
-- - Runtime-generated file rows should not be part of the clean seed baseline.

BEGIN;

INSERT INTO public.attachment (
    id,
    storage_driver,
    bucket,
    object_key,
    public_url,
    original_name,
    content_type,
    file_ext,
    size_bytes,
    sha256,
    etag,
    uploaded_by,
    uploaded_at,
    remark,
    is_deleted,
    deleted_at
)
SELECT
    NULL::bigint,
    NULL::varchar,
    NULL::varchar,
    NULL::varchar,
    NULL::varchar,
    NULL::varchar,
    NULL::varchar,
    NULL::varchar,
    NULL::bigint,
    NULL::varchar,
    NULL::varchar,
    NULL::bigint,
    NULL::timestamp,
    NULL::varchar,
    NULL::boolean,
    NULL::timestamp
WHERE FALSE;

COMMIT;
