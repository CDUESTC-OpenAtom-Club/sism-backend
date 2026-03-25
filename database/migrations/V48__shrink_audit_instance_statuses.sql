-- Shrink audit_instance.status to the active approval lifecycle only.
-- Historical DRAFT/WITHDRAWN instances are normalized back into IN_REVIEW.

UPDATE public.audit_instance
SET status = 'IN_REVIEW',
    completed_at = NULL,
    updated_at = now()
WHERE status IN ('DRAFT', 'WITHDRAWN');

ALTER TABLE public.audit_instance
    DROP CONSTRAINT IF EXISTS audit_instance_status_check;

ALTER TABLE public.audit_instance
    ADD CONSTRAINT audit_instance_status_check
    CHECK (status IN ('IN_REVIEW', 'APPROVED', 'REJECTED'));

COMMENT ON COLUMN public.audit_instance.status IS '审批实例状态：IN_REVIEW / APPROVED / REJECTED';
