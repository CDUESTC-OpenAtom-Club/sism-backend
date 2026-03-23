ALTER TABLE plan
    ADD COLUMN IF NOT EXISTS workflow_instance_id BIGINT,
    ADD COLUMN IF NOT EXISTS submitted_by BIGINT,
    ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_reject_reason TEXT;
