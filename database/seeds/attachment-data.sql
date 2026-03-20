-- attachment clean seed
-- Scope:
-- - Store the physical file objects themselves.
-- - Business attachment usage is bound separately by plan_report_indicator_attachment.

BEGIN;

DELETE FROM public.attachment
WHERE id IN (8103, 8104);

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
VALUES
    (
        8101,
        'FILE',
        NULL,
        '2026/03/strategy/4036/dangban-ledger.pdf',
        NULL,
        '党委办公室重点工作台账.pdf',
        'application/pdf',
        'pdf',
        245761,
        '1111111111111111111111111111111111111111111111111111111111111111',
        NULL,
        191,
        NOW(),
        '党委办公室 2026 年 3 月填报附件',
        false,
        NULL
    ),
    (
        8102,
        'FILE',
        NULL,
        '2026/03/security/4042/inspection-plan.docx',
        NULL,
        '保卫处巡检计划初稿.docx',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'docx',
        182944,
        '2222222222222222222222222222222222222222222222222222222222222222',
        NULL,
        215,
        NOW(),
        '保卫处 2026 年 3 月草稿附件',
        false,
        NULL
    )
ON CONFLICT (id) DO UPDATE
SET
    storage_driver = EXCLUDED.storage_driver,
    bucket = EXCLUDED.bucket,
    object_key = EXCLUDED.object_key,
    public_url = EXCLUDED.public_url,
    original_name = EXCLUDED.original_name,
    content_type = EXCLUDED.content_type,
    file_ext = EXCLUDED.file_ext,
    size_bytes = EXCLUDED.size_bytes,
    sha256 = EXCLUDED.sha256,
    etag = EXCLUDED.etag,
    uploaded_by = EXCLUDED.uploaded_by,
    uploaded_at = EXCLUDED.uploaded_at,
    remark = EXCLUDED.remark,
    is_deleted = EXCLUDED.is_deleted,
    deleted_at = EXCLUDED.deleted_at;

COMMIT;
