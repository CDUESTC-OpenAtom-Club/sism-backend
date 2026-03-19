package com.sism.workflow.domain.runtime.model;

import com.sism.shared.domain.model.base.AggregateRoot;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * AuditInstance - 审批实例聚合根
 * Represents an active approval workflow instance.
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

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PENDING = "IN_REVIEW";
    public static final String STATUS_IN_PROGRESS = "IN_REVIEW";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_WITHDRAWN = "WITHDRAWN";
    public static final String STATUS_CANCELLED = "WITHDRAWN";
    public static final String STEP_STATUS_PENDING = "PENDING";
    public static final String STEP_STATUS_WAITING = "WAITING";
    public static final String STEP_STATUS_APPROVED = "APPROVED";
    public static final String STEP_STATUS_REJECTED = "REJECTED";

    @Column(name = "flow_def_id")
    private Long flowDefId;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(nullable = false)
    private String status = STATUS_PENDING;

    @Column(name = "requester_id")
    private Long requesterId;

    @Column(name = "requester_org_id")
    private Long requesterOrgId;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "instance", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepIndex ASC")
    private List<AuditStepInstance> stepInstances = new ArrayList<>();

    public static AuditInstance create(Long entityId, String entityType, AuditFlowDef flowDef) {
        if (entityId == null || entityId <= 0) {
            throw new IllegalArgumentException("Entity ID must be positive");
        }
        if (entityType == null || entityType.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity type is required");
        }
        if (flowDef == null) {
            throw new IllegalArgumentException("Flow definition is required");
        }

        AuditInstance instance = new AuditInstance();
        instance.setEntityId(entityId);
        instance.setEntityType(entityType);
        instance.setFlowDefId(flowDef.getId());
        instance.setStatus(STATUS_PENDING);
        instance.setStartedAt(LocalDateTime.now());
        return instance;
    }

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
            throw new IllegalStateException("Cannot approve: workflow is not in review");
        }
        if (stepInstances == null || stepInstances.isEmpty()) {
            this.status = STATUS_APPROVED;
            this.completedAt = LocalDateTime.now();
            return;
        }

        AuditStepInstance current = resolveCurrentPendingStep()
                .orElseThrow(() -> new IllegalStateException("No pending step available for approval"));

        if (current.getApproverId() != null && !Objects.equals(current.getApproverId(), userId)) {
            throw new IllegalStateException("Cannot approve: current step is assigned to another approver");
        }

        current.setStatus(STEP_STATUS_APPROVED);
        current.setComment(comment);
        current.setApprovedAt(LocalDateTime.now());

        Optional<AuditStepInstance> next = stepInstances.stream()
                .filter(step -> STEP_STATUS_WAITING.equals(step.getStatus()))
                .sorted(Comparator.comparing(step -> step.getStepIndex() == null ? Integer.MAX_VALUE : step.getStepIndex()))
                .findFirst();

        if (next.isPresent()) {
            AuditStepInstance nextStep = next.get();
            nextStep.setStatus(STEP_STATUS_PENDING);
        } else {
            this.status = STATUS_APPROVED;
            this.completedAt = LocalDateTime.now();
        }
    }

    public void reject(Long userId, String comment) {
        if (!STATUS_PENDING.equals(status)) {
            throw new IllegalStateException("Cannot reject: workflow is not in review");
        }
        if (stepInstances != null && !stepInstances.isEmpty()) {
            AuditStepInstance current = resolveCurrentPendingStep()
                    .orElseThrow(() -> new IllegalStateException("No pending step available for rejection"));
            if (current.getApproverId() != null && !Objects.equals(current.getApproverId(), userId)) {
                throw new IllegalStateException("Cannot reject: current step is assigned to another approver");
            }
            current.setStatus(STEP_STATUS_REJECTED);
            current.setComment(comment);
            current.setApprovedAt(LocalDateTime.now());
        }
        this.status = STATUS_REJECTED;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (!STATUS_PENDING.equals(status)) {
            throw new IllegalStateException("Cannot cancel: workflow is not in review");
        }
        this.status = STATUS_WITHDRAWN;
        this.completedAt = LocalDateTime.now();
    }

    public void start(Long requesterId, Long requesterOrgId) {
        this.requesterId = requesterId;
        this.requesterOrgId = requesterOrgId;
        this.status = STATUS_PENDING;
        this.startedAt = LocalDateTime.now();
    }

    public void transfer(Long targetUserId) {
        // Placeholder - retained for behavior compatibility in phase one.
    }

    public void addApprover(Long approverId) {
        // Placeholder - retained for behavior compatibility in phase one.
    }

    public void addStepInstance(AuditStepInstance stepInstance) {
        stepInstances.add(stepInstance);
        stepInstance.setInstance(this);
        if (stepInstance.getCreatedAt() == null) {
            stepInstance.setCreatedAt(LocalDateTime.now());
        }
    }

    public Optional<AuditStepInstance> resolveCurrentPendingStep() {
        return stepInstances.stream()
                .filter(step -> STEP_STATUS_PENDING.equals(step.getStatus()))
                .sorted(Comparator.comparing(step -> step.getStepIndex() == null ? Integer.MAX_VALUE : step.getStepIndex()))
                .findFirst();
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
