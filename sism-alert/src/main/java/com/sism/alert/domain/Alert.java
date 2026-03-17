package com.sism.alert.domain;

import com.sism.shared.domain.model.base.AggregateRoot;
import jakarta.persistence.*;
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

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_TRIGGERED = "TRIGGERED";
    public static final String STATUS_RESOLVED = "RESOLVED";

    @Id
    @SequenceGenerator(name = "alert_event_event_id_seq", sequenceName = "alert_event_event_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alert_event_event_id_seq")
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
    private String status = STATUS_PENDING;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Override
    public boolean canPublish() {
        return STATUS_RESOLVED.equals(status);
    }

    @Override
    public void validate() {
        if (indicatorId == null) {
            throw new IllegalArgumentException("Indicator ID is required");
        }
        if (severity == null || severity.trim().isEmpty()) {
            throw new IllegalArgumentException("Severity is required");
        }
    }

    public void trigger() {
        this.status = STATUS_TRIGGERED;
        this.updatedAt = LocalDateTime.now();
    }

    public void resolve(Long handledBy, String handledNote) {
        this.status = STATUS_RESOLVED;
        this.handledBy = handledBy;
        this.handledNote = handledNote;
        this.updatedAt = LocalDateTime.now();
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
