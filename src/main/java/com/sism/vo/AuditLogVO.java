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
 * **Validates: Requirements 4.1**
 *
 * @author SISM Team
 */
public class AuditLogVO {

    private Long logId;
    private AuditEntityType entityType;
    private Long entityId;
    private AuditAction action;
    private Map<String, Object> beforeJson;
    private Map<String, Object> afterJson;
    private Map<String, Object> changedFields;
    private String reason;
    private Long actorUserId;
    private String actorUserName;
    private Long actorOrgId;
    private String actorOrgName;
    private LocalDateTime createdAt;

    /**
     * Default constructor
     */
    public AuditLogVO() {
    }

    /**
     * Full constructor with validation
     */
    public AuditLogVO(Long logId, AuditEntityType entityType, Long entityId, AuditAction action,
                      Map<String, Object> beforeJson, Map<String, Object> afterJson,
                      Map<String, Object> changedFields, String reason,
                      Long actorUserId, String actorUserName, Long actorOrgId, 
                      String actorOrgName, LocalDateTime createdAt) {
        if (entityType == null) {
            throw new IllegalArgumentException("Entity type cannot be null");
        }
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null");
        }
        this.logId = logId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        // Create defensive copies for mutable maps
        this.beforeJson = beforeJson != null ? Map.copyOf(beforeJson) : null;
        this.afterJson = afterJson != null ? Map.copyOf(afterJson) : null;
        this.changedFields = changedFields != null ? Map.copyOf(changedFields) : null;
        this.reason = reason;
        this.actorUserId = actorUserId;
        this.actorUserName = actorUserName;
        this.actorOrgId = actorOrgId;
        this.actorOrgName = actorOrgName;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public AuditEntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(AuditEntityType entityType) {
        if (entityType == null) {
            throw new IllegalArgumentException("Entity type cannot be null");
        }
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null");
        }
        this.action = action;
    }

    public Map<String, Object> getBeforeJson() {
        return beforeJson;
    }

    public void setBeforeJson(Map<String, Object> beforeJson) {
        this.beforeJson = beforeJson != null ? Map.copyOf(beforeJson) : null;
    }

    public Map<String, Object> getAfterJson() {
        return afterJson;
    }

    public void setAfterJson(Map<String, Object> afterJson) {
        this.afterJson = afterJson != null ? Map.copyOf(afterJson) : null;
    }

    public Map<String, Object> getChangedFields() {
        return changedFields;
    }

    public void setChangedFields(Map<String, Object> changedFields) {
        this.changedFields = changedFields != null ? Map.copyOf(changedFields) : null;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorUserName() {
        return actorUserName;
    }

    public void setActorUserName(String actorUserName) {
        this.actorUserName = actorUserName;
    }

    public Long getActorOrgId() {
        return actorOrgId;
    }

    public void setActorOrgId(Long actorOrgId) {
        this.actorOrgId = actorOrgId;
    }

    public String getActorOrgName() {
        return actorOrgName;
    }

    public void setActorOrgName(String actorOrgName) {
        this.actorOrgName = actorOrgName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}