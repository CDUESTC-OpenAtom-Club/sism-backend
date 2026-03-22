package com.sism.strategy.domain.plan;

import com.sism.shared.domain.model.base.AggregateRoot;
import com.sism.strategy.domain.event.PlanCreatedEvent;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "plan", schema = "public")
@Access(AccessType.FIELD)
public class Plan extends AggregateRoot<Long> {

    @Id
    @SequenceGenerator(name = "Plan_IdSeq", sequenceName = "public.plan_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Plan_IdSeq")
    @Column(name = "id")
    private Long id;

    @Column(name = "cycle_id", nullable = false)
    private Long cycleId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @Column(name = "target_org_id", nullable = false)
    private Long targetOrgId;

    @Column(name = "created_by_org_id", nullable = false)
    private Long createdByOrgId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_level", columnDefinition = "plan_level", nullable = false)
    private PlanLevel planLevel;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "audit_instance_id")
    private Long auditInstanceId;

    public static Plan create(Long cycleId, Long targetOrgId, Long createdByOrgId, PlanLevel planLevel) {
        if (cycleId == null) {
            throw new IllegalArgumentException("Cycle ID cannot be null");
        }
        if (targetOrgId == null) {
            throw new IllegalArgumentException("Target org ID cannot be null");
        }
        if (createdByOrgId == null) {
            throw new IllegalArgumentException("Created by org ID cannot be null");
        }
        if (planLevel == null) {
            throw new IllegalArgumentException("Plan level cannot be null");
        }

        Plan plan = new Plan();
        plan.cycleId = cycleId;
        plan.targetOrgId = targetOrgId;
        plan.createdByOrgId = createdByOrgId;
        plan.planLevel = planLevel;
        plan.createdAt = LocalDateTime.now();
        plan.updatedAt = LocalDateTime.now();
        plan.isDeleted = false;
        plan.status = PlanStatus.DRAFT.value();
        plan.addEvent(new PlanCreatedEvent(plan.id, planLevel.name(), targetOrgId));
        return plan;
    }

    public void activate() {
        if (PlanStatus.DISTRIBUTED.value().equals(this.status)) {
            throw new IllegalStateException("Plan is already distributed");
        }
        this.status = PlanStatus.DISTRIBUTED.value();
        this.updatedAt = LocalDateTime.now();
    }

    public void ensureCanSubmitForApproval() {
        if (!PlanStatus.DRAFT.value().equals(this.status) && !PlanStatus.RETURNED.value().equals(this.status)) {
            throw new IllegalStateException("Plan must be in DRAFT or RETURNED state to submit for approval");
        }
    }

    public void submitForApproval() {
        ensureCanSubmitForApproval();
        this.status = PlanStatus.PENDING.value();
        this.updatedAt = LocalDateTime.now();
    }

    public void approve() {
        if (PlanStatus.DISTRIBUTED.value().equals(this.status)) {
            throw new IllegalStateException("Plan is already distributed");
        }
        this.status = PlanStatus.DISTRIBUTED.value();
        this.updatedAt = LocalDateTime.now();
    }

    public void returnForRevision() {
        this.status = PlanStatus.RETURNED.value();
        this.updatedAt = LocalDateTime.now();
    }

    public void withdraw() {
        this.status = PlanStatus.DRAFT.value();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isEditable() {
        return PlanStatus.DRAFT.value().equals(this.status) || PlanStatus.RETURNED.value().equals(this.status);
    }

    public boolean isDistributed() {
        return PlanStatus.DISTRIBUTED.value().equals(this.status);
    }

    public boolean isReturned() {
        return PlanStatus.RETURNED.value().equals(this.status);
    }

    @Override
    public void validate() {
        if (cycleId == null) {
            throw new IllegalArgumentException("Cycle ID is required");
        }
        if (targetOrgId == null) {
            throw new IllegalArgumentException("Target org ID is required");
        }
        if (createdByOrgId == null) {
            throw new IllegalArgumentException("Created by org ID is required");
        }
        if (planLevel == null) {
            throw new IllegalArgumentException("Plan level is required");
        }
    }
}
