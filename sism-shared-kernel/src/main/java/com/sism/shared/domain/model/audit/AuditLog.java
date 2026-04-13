package com.sism.shared.domain.model.audit;

import com.sism.shared.domain.model.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * AuditLog aggregate root - DDD model for audit logging
 */
@Getter
@Entity
@Table(name = "audit_log")
@Access(AccessType.FIELD)
public class AuditLog extends AggregateRoot<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(nullable = false)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_json", columnDefinition = "jsonb")
    private Map<String, Object> beforeJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_json", columnDefinition = "jsonb")
    private Map<String, Object> afterJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changed_fields", columnDefinition = "jsonb")
    private Map<String, Object> changedFields;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_org_id")
    private Long actorOrgId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime persistedCreatedAt;

    @Column(name = "request_id")
    private String requestId;

    @Override
    public Long getId() {
        return logId;
    }

    @Override
    public void setId(Long id) {
        assertIdUnchanged(this.logId, id);
        this.logId = id;
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return persistedCreatedAt;
    }

    @Override
    public void setCreatedAt(LocalDateTime createdAt) {
        super.setCreatedAt(createdAt);
        this.persistedCreatedAt = createdAt;
    }

    @Override
    public boolean canPublish() {
        return true;
    }

    @Override
    public void validate() {
        if (entityType == null || entityType.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity type is required");
        }
        if (entityId == null) {
            throw new IllegalArgumentException("Entity ID is required");
        }
        if (action == null || action.trim().isEmpty()) {
            throw new IllegalArgumentException("Action is required");
        }
    }

    public static AuditLog create(String entityType, Long entityId, String action,
                                   Map<String, Object> beforeJson, Map<String, Object> afterJson,
                                   Long actorUserId, Long actorOrgId) {
        AuditLog log = new AuditLog();
        log.entityType = entityType;
        log.entityId = entityId;
        log.action = action;
        log.beforeJson = beforeJson;
        log.afterJson = afterJson;
        log.actorUserId = actorUserId;
        log.actorOrgId = actorOrgId;
        log.setCreatedAt(LocalDateTime.now());
        log.validate();
        return log;
    }

    public void recordRequestContext(String requestId, String ipAddress, String userAgent) {
        this.requestId = requestId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public void recordChangeSet(Map<String, Object> changedFields, String reason) {
        this.changedFields = changedFields;
        this.reason = reason;
    }

    @PrePersist
    protected void onCreate() {
        if (persistedCreatedAt == null) {
            setCreatedAt(LocalDateTime.now());
        }
    }
}
