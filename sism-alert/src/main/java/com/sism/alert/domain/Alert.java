package com.sism.alert.domain;

import com.sism.alert.domain.event.AlertCreatedEvent;
import com.sism.alert.domain.event.AlertResolvedEvent;
import com.sism.alert.domain.event.AlertTriggeredEvent;
import com.sism.alert.domain.enums.AlertSeverity;
import com.sism.alert.domain.enums.AlertStatus;
import com.sism.shared.domain.model.base.AggregateRoot;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Alert - 预警实体
 * Maps to alert_event table in database
 */
@Getter
@Setter
@Entity
@Table(name = "alert_event")
@Access(AccessType.FIELD)
public class Alert extends AggregateRoot<Long> {

    public static final String STATUS_OPEN = AlertStatus.OPEN.name();
    public static final String STATUS_IN_PROGRESS = AlertStatus.IN_PROGRESS.name();
    public static final String STATUS_RESOLVED = AlertStatus.RESOLVED.name();
    public static final String STATUS_CLOSED = AlertStatus.CLOSED.name();
    @Deprecated
    public static final String STATUS_PENDING = STATUS_OPEN;
    @Deprecated
    public static final String STATUS_TRIGGERED = STATUS_IN_PROGRESS;

    @Id
    @SequenceGenerator(name = "alert_event_event_id_seq", sequenceName = "alert_event_event_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.SEQUENCE, generator = "alert_event_event_id_seq")
    @Column(name = "event_id")
    private Long id;

    @Column(name = "indicator_id")
    private Long indicatorId;

    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "window_id")
    private Long windowId;

    @Column(name = "actual_percent", precision = 10, scale = 2)
    private BigDecimal actualPercent;

    @Column(name = "expected_percent", precision = 10, scale = 2)
    private BigDecimal expectedPercent;

    @Column(name = "gap_percent", precision = 10, scale = 2)
    private BigDecimal gapPercent;

    @Column(name = "detail_json", columnDefinition = "TEXT")
    private String detailJson;

    @Column(name = "handled_by")
    private Long handledBy;

    @Column(name = "handled_note")
    private String handledNote;

    @Column(name = "severity", nullable = false)
    private String severity;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertStatus status = AlertStatus.OPEN;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Override
    public boolean canPublish() {
        return AlertStatus.RESOLVED.equals(status);
    }

    @Override
    public void validate() {
        if (indicatorId == null) {
            throw new IllegalArgumentException("Indicator ID is required");
        }
        if (ruleId == null) {
            throw new IllegalArgumentException("Rule ID is required");
        }
        if (windowId == null) {
            throw new IllegalArgumentException("Window ID is required");
        }
        if (actualPercent == null) {
            throw new IllegalArgumentException("Actual percent is required");
        }
        if (expectedPercent == null) {
            throw new IllegalArgumentException("Expected percent is required");
        }
        if (gapPercent == null) {
            throw new IllegalArgumentException("Gap percent is required");
        }
        if (severity == null || severity.trim().isEmpty()) {
            throw new IllegalArgumentException("Severity is required");
        }
        if (AlertSeverity.normalize(severity) == null) {
            throw new IllegalArgumentException("Severity must be INFO, WARNING, or CRITICAL");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status must be OPEN, IN_PROGRESS, RESOLVED, or CLOSED");
        }
    }

    public void trigger() {
        if (AlertStatus.RESOLVED.equals(status) || AlertStatus.CLOSED.equals(status)) {
            throw new IllegalStateException("Resolved or closed alerts cannot be triggered again");
        }
        this.status = AlertStatus.IN_PROGRESS;
        this.updatedAt = LocalDateTime.now();
        this.addEvent(new AlertTriggeredEvent(this.id, this.indicatorId, this.severity, this.status.name()));
    }

    public void resolve(Long handledBy, String handledNote) {
        if (AlertStatus.CLOSED.equals(status)) {
            throw new IllegalStateException("Closed alerts cannot be resolved");
        }
        this.status = AlertStatus.RESOLVED;
        this.handledBy = handledBy;
        this.handledNote = handledNote;
        this.updatedAt = LocalDateTime.now();
        this.addEvent(new AlertResolvedEvent(this.id, this.indicatorId, handledBy, this.status.name()));
    }

    public void recordCreated() {
        this.addEvent(new AlertCreatedEvent(this.id, this.indicatorId, this.severity, this.status.name()));
    }

    public static AlertStatus normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        return AlertStatus.from(status);
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @Override
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    @Override
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
