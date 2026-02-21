package com.sism.vo;

import com.sism.enums.AuditAction;
import com.sism.enums.AuditEntityType;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Value Object for audit log data
 * Used for API responses when querying audit logs
 *
 * Requirements: 7.5 - Support viewing audit logs
 * Converted to record for immutability and simplicity
 * **Validates: Requirements 4.1**
 *
 * @param logId          Audit log ID
 * @param entityType     Type of entity being audited
 * @param entityId       ID of the entity being audited
 * @param action         Action performed (CREATE, UPDATE, DELETE, APPROVE, ARCHIVE, RESTORE)
 * @param beforeJson      JSON snapshot of data before the change
 * @param afterJson       JSON snapshot of data after the change
 * @param changedFields   Map of changed fields with before/after values
 * @param reason          Reason or comments for the action
 * @param actorUserId     ID of the user who performed the action
 * @param actorUserName   Name of the user who performed the action
 * @param actorOrgId      ID of the organization of the actor
 * @param actorOrgName    Name of the organization of the actor
 * @param createdAt       Timestamp when the audit log was created
 */
public record AuditLogVO(
    Long logId,
    AuditEntityType entityType,
    Long entityId,
    AuditAction action,
    Map<String, Object> beforeJson,
    Map<String, Object> afterJson,
    Map<String, Object> changedFields,
    String reason,
    Long actorUserId,
    String actorUserName,
    Long actorOrgId,
    String actorOrgName,
    LocalDateTime createdAt
) {
    /**
     * Compact constructor with validation and defensive copying
     */
    public AuditLogVO {
        if (entityType == null) {
            throw new IllegalArgumentException("Entity type cannot be null");
        }
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null");
        }
        // Create defensive copies for mutable maps
        if (beforeJson != null) {
            beforeJson = Map.copyOf(beforeJson);
        }
        if (afterJson != null) {
            afterJson = Map.copyOf(afterJson);
        }
        if (changedFields != null) {
            changedFields = Map.copyOf(changedFields);
        }
    }
}
