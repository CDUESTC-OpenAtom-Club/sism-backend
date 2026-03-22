ALTER TABLE public.plan
    ADD COLUMN IF NOT EXISTS workflow_instance_id BIGINT,
    ADD COLUMN IF NOT EXISTS submitted_by BIGINT,
    ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_reject_reason TEXT;

ALTER TABLE public.plan
    DROP CONSTRAINT IF EXISTS ck_plan_status;

ALTER TABLE public.plan
    ADD CONSTRAINT ck_plan_status
    CHECK (
        status IN (
            'DRAFT',
            'PENDING',
            'DISTRIBUTED',
            'IN_REVIEW',
            'APPROVED',
            'REJECTED',
            'RETURNED',
            'WITHDRAWN'
        )
    );
