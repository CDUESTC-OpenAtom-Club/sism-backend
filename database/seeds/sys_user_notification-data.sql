-- sys_user_notification clean seed
-- Scope:
-- - User notifications are runtime inbox data.
-- - Clean baseline should keep this table empty.
--
-- Table structure reference
-- Source: sism-main/src/main/resources/db/migration/V60__create_user_notification_and_indicator_reminder_tables.sql
--
-- Columns:
-- - id BIGSERIAL PRIMARY KEY
-- - recipient_user_id BIGINT NOT NULL
-- - sender_user_id BIGINT
-- - sender_org_id BIGINT
-- - notification_type VARCHAR(64) NOT NULL
-- - title VARCHAR(255) NOT NULL
-- - content TEXT NOT NULL
-- - status VARCHAR(32) NOT NULL DEFAULT 'UNREAD'
-- - action_url VARCHAR(500)
-- - related_entity_type VARCHAR(64)
-- - related_entity_id BIGINT
-- - batch_key VARCHAR(64)
-- - metadata_json JSONB
-- - read_at TIMESTAMP
-- - created_at TIMESTAMP NOT NULL DEFAULT NOW()
-- - updated_at TIMESTAMP NOT NULL DEFAULT NOW()
--
-- Indexes:
-- - idx_sys_user_notification_recipient_created(recipient_user_id, created_at DESC)
-- - idx_sys_user_notification_related_entity(related_entity_type, related_entity_id)
-- - idx_sys_user_notification_batch_key(batch_key)
--
-- Notes:
-- - This seed file intentionally keeps sys_user_notification empty.
-- - Actual inbox data is generated at runtime by business operations.

BEGIN;

-- Intentionally no seed rows.

COMMIT;
