package com.sism.shared.domain.model.workflow;

import com.sism.shared.domain.model.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AuditInstance - 审批实例聚合根
 * Represents an active approval workflow instance
 */
@Getter
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
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        assertIdUnchanged(this.id, id);
        this.id = id;
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
        AuditStepInstance currentStep = getCurrentStepInstance();
        if (currentStep != null) {
            currentStep.setStatus(STATUS_APPROVED);
            currentStep.setApproverId(userId);
            currentStep.setComment(comment);
            currentStep.setApprovedAt(LocalDateTime.now());
        }

        if (hasNextStep()) {
            this.currentStepIndex = this.currentStepIndex + 1;
            this.result = "Step approved by user " + userId + (comment != null ? ": " + comment : "");
            return;
        }

        this.status = STATUS_APPROVED;
        this.result = "Approved by user " + userId + (comment != null ? ": " + comment : "");
        this.completedAt = LocalDateTime.now();
    }

    public void reject(Long userId, String comment) {
        if (!STATUS_PENDING.equals(status)) {
            throw new IllegalStateException("Cannot reject: workflow is not pending");
        }
        AuditStepInstance currentStep = getCurrentStepInstance();
        if (currentStep != null) {
            currentStep.setStatus(STATUS_REJECTED);
            currentStep.setApproverId(userId);
            currentStep.setComment(comment);
            currentStep.setApprovedAt(LocalDateTime.now());
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

    public void setFlowDefId(Long flowDefId) {
        this.flowDefId = flowDefId;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    @Deprecated
    public void setStatus(String status) {
        validateStatus(status);
        this.status = status;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public void setRequesterOrgId(Long requesterOrgId) {
        this.requesterOrgId = requesterOrgId;
    }

    public void setCurrentStepIndex(Integer currentStepIndex) {
        if (currentStepIndex != null && currentStepIndex < 0) {
            throw new IllegalArgumentException("Current step index cannot be negative");
        }
        this.currentStepIndex = currentStepIndex;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void setIsDeleted(Boolean deleted) {
        isDeleted = deleted;
    }

    public void setStepInstances(List<AuditStepInstance> stepInstances) {
        this.stepInstances.clear();
        if (stepInstances != null) {
            stepInstances.forEach(this::addStepInstance);
        }
    }

    public void transfer(Long targetUserId) {
        throw new UnsupportedOperationException("Transfer operation is not implemented");
    }

    public void addApprover(Long approverId) {
        throw new UnsupportedOperationException("Add approver operation is not implemented");
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

    private AuditStepInstance getCurrentStepInstance() {
        if (stepInstances == null || stepInstances.isEmpty()) {
            return null;
        }
        return stepInstances.stream()
                .filter(step -> step.getStepIndex() != null && step.getStepIndex().equals(currentStepIndex))
                .findFirst()
                .orElse(null);
    }

    private boolean hasNextStep() {
        if (stepInstances == null || stepInstances.isEmpty()) {
            return false;
        }
        return stepInstances.stream()
                .anyMatch(step -> step.getStepIndex() != null && step.getStepIndex() > currentStepIndex);
    }

    private void validateStatus(String nextStatus) {
        if (nextStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        if (!STATUS_PENDING.equals(nextStatus)
                && !STATUS_APPROVED.equals(nextStatus)
                && !STATUS_REJECTED.equals(nextStatus)
                && !STATUS_CANCELLED.equals(nextStatus)) {
            throw new IllegalArgumentException("Unsupported audit instance status: " + nextStatus);
        }
    }
}
