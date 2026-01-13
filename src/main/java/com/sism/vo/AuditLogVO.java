package com.sism.vo;

import com.sism.enums.AuditAction;
import com.sism.enums.AuditEntityType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Value Object for audit log data
 * Used for API responses when querying audit logs
 * 
 * Requirements: 7.5 - Support viewing audit logs
 */
@Data
public class AuditLogVO {

    /**
     * Audit log ID
     */
    private Long logId;

    /**
     * Type of entity being audited
     */
    private AuditEntityType entityType;

    /**
     * ID of the entity being audited
     */
    private Long entityId;

    /**
     * Action performed (CREATE, UPDATE, DELETE, APPROVE, ARCHIVE, RESTORE)
     */
    private AuditAction action;

    /**
     * JSON snapshot of data before the change
     */
    private Map<String, Object> beforeJson;

    /**
     * JSON snapshot of data after the change
     */
    private Map<String, Object> afterJson;

    /**
     * Map of changed fields with before/after values
     */
    private Map<String, Object> changedFields;

    /**
     * Reason or comments for the action
     */
    private String reason;

    /**
     * ID of the user who performed the action
     */
    private Long actorUserId;

    /**
     * Name of the user who performed the action
     */
    private String actorUserName;

    /**
     * ID of the organization of the actor
     */
    private Long actorOrgId;

    /**
     * Name of the organization of the actor
     */
    private String actorOrgName;

    /**
     * Timestamp when the audit log was created
     */
    private LocalDateTime createdAt;
}
