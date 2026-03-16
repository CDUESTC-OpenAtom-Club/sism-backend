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
@Table(name = "alert_event")
@Where(clause = "is_deleted = false")
@Access(AccessType.FIELD)
public class Alert extends AggregateRoot<Long> {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_TRIGGERED = "TRIGGERED";
    public static final String STATUS_RESOLVED = "RESOLVED";

    @Id
    @SequenceGenerator(name = "alert_event_id_seq", sequenceName = "alert_event_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alert_event_id_seq")
    @Column(name = "id")
    private Long id;

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
        setUpdatedAt(LocalDateTime.now());
    }

    public void resolve(Long resolvedBy, String resolution) {
        this.status = STATUS_RESOLVED;
        this.resolvedBy = resolvedBy;
        this.resolution = resolution;
        this.resolvedAt = LocalDateTime.now();
        setUpdatedAt(LocalDateTime.now());
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return super.getCreatedAt();
    }

    @Override
    public void setCreatedAt(LocalDateTime createdAt) {
        super.setCreatedAt(createdAt);
    }

    @Override
    public LocalDateTime getUpdatedAt() {
        return super.getUpdatedAt();
    }

    @Override
    public void setUpdatedAt(LocalDateTime updatedAt) {
        super.setUpdatedAt(updatedAt);
    }

    @PrePersist
    protected void onCreate() {
        if (getCreatedAt() == null) {
            setCreatedAt(LocalDateTime.now());
        }
        if (getUpdatedAt() == null) {
            setUpdatedAt(LocalDateTime.now());
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        setUpdatedAt(LocalDateTime.now());
    }
}
