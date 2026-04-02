package com.sism.shared.domain.model.workflow;

import com.sism.shared.domain.model.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AuditInstance - 审批实例聚合根
 * Represents an active approval workflow instance
 */
@Getter
@Setter
@Entity
@Table(name = "audit_instance")
@Where(clause = "is_deleted = false")
@Access(AccessType.FIELD)
public class AuditInstance extends AggregateRoot<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Column(name = "flow_def_id")
    private Long flowDefId;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(nullable = false)
    private String status = STATUS_PENDING;

    @Column(name = "title")
    private String title;

    @Column(name = "requester_id")
    private Long requesterId;

    @Column(name = "requester_org_id")
    private Long requesterOrgId;

    @Column(name = "current_step_index")
    private Integer currentStepIndex = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "result")
    private String result;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "instance", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepIndex ASC")
    private List<AuditStepInstance> stepInstances = new ArrayList<>();

    @Override
    public boolean canPublish() {
        return STATUS_APPROVED.equals(status) || STATUS_REJECTED.equals(status);
    }

    @Override
    public void validate() {
        if (entityType == null || entityType.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity type is required");
        }
        if (entityId == null) {
            throw new IllegalArgumentException("Entity ID is required");
        }
    }

    public void approve(Long userId, String comment) {
        if (!STATUS_PENDING.equals(status)) {
            throw new IllegalStateException("Cannot approve: workflow is not pending");
        }
        this.status = STATUS_APPROVED;
        this.result = "Approved by user " + userId + (comment != null ? ": " + comment : "");
        this.completedAt = LocalDateTime.now();
    }

    public void reject(Long userId, String comment) {
        if (!STATUS_PENDING.equals(status)) {
            throw new IllegalStateException("Cannot reject: workflow is not pending");
        }
        this.status = STATUS_REJECTED;
        this.result = "Rejected by user " + userId + (comment != null ? ": " + comment : "");
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (!STATUS_PENDING.equals(status)) {
            throw new IllegalStateException("Cannot cancel: workflow is not pending");
        }
        this.status = STATUS_CANCELLED;
        this.completedAt = LocalDateTime.now();
    }

    public void start(Long requesterId, Long requesterOrgId) {
        this.requesterId = requesterId;
        this.requesterOrgId = requesterOrgId;
        this.startedAt = LocalDateTime.now();
    }

    public void transfer(Long targetUserId) {
        // 转交审批逻辑 - 这里只是一个占位符
        // 实际应该更新当前审批人等信息
    }

    public void addApprover(Long approverId) {
        // 添加审批人逻辑 - 这里只是一个占位符
        // 实际应该添加到 stepInstances 或其他关联表
    }

    public void addStepInstance(AuditStepInstance stepInstance) {
        stepInstances.add(stepInstance);
        stepInstance.setInstance(this);
    }

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }
}
