-- Align workflow tables with the refactor design docs.
-- Remove redundant fields that are no longer part of the target model.

ALTER TABLE audit_step_instance
    DROP COLUMN IF EXISTS approver_name,
    DROP COLUMN IF EXISTS started_at,
    DROP COLUMN IF EXISTS ended_at,
    DROP COLUMN IF EXISTS skipped_at,
    DROP COLUMN IF EXISTS skipped_reason,
    DROP COLUMN IF EXISTS step_no,
    DROP COLUMN IF EXISTS step_code,
    DROP COLUMN IF EXISTS step_status,
    DROP COLUMN IF EXISTS handled_by,
    DROP COLUMN IF EXISTS handled_comment,
    DROP COLUMN IF EXISTS handled_at;

ALTER TABLE audit_step_def
    DROP COLUMN IF EXISTS approver_id,
    DROP COLUMN IF EXISTS approver_type,
    DROP COLUMN IF EXISTS can_skip,
    DROP COLUMN IF EXISTS timeout_hours,
    DROP COLUMN IF EXISTS step_code;

ALTER TABLE audit_instance
    DROP COLUMN IF EXISTS current_step_index,
    DROP COLUMN IF EXISTS result,
    DROP COLUMN IF EXISTS title,
    DROP COLUMN IF EXISTS biz_id,
    DROP COLUMN IF EXISTS current_step_id,
    DROP COLUMN IF EXISTS created_by;

ALTER TABLE audit_flow_def
    DROP COLUMN IF EXISTS remark;
