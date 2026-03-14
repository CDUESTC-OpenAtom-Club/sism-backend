package com.sism.alert.domain;

import com.sism.shared.domain.model.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

/**
 * Alert - 预警实体
 * Represents an alert event in the system
 */
@Getter
@Setter
@Entity
@Table(name = "alert")
@Where(clause = "is_deleted = false")
public class Alert extends AggregateRoot<Long> {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_TRIGGERED = "TRIGGERED";
    public static final String STATUS_RESOLVED = "RESOLVED";

    @Column(name = "alert_type", nullable = false)
    private String alertType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "severity", nullable = false)
    private String severity;

    @Column(name = "status", nullable = false)
    private String status = STATUS_PENDING;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolution")
    private String resolution;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public boolean canPublish() {
        return STATUS_RESOLVED.equals(status);
    }

    @Override
    public void validate() {
        if (alertType == null || alertType.trim().isEmpty()) {
            throw new IllegalArgumentException("Alert type is required");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (severity == null || severity.trim().isEmpty()) {
            throw new IllegalArgumentException("Severity is required");
        }
    }

    public void trigger() {
        this.status = STATUS_TRIGGERED;
        this.triggeredAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void resolve(Long resolvedBy, String resolution) {
        this.status = STATUS_RESOLVED;
        this.resolvedBy = resolvedBy;
        this.resolution = resolution;
        this.resolvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
