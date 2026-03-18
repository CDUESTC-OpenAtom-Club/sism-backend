package com.sism.execution.domain.model.plan;

import com.sism.execution.domain.event.PlanCreatedEvent;
import com.sism.shared.domain.model.base.AggregateRoot;
import jakarta.persistence.*;
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
    @SequenceGenerator(name="Plan_IdSeq", sequenceName="public.plan_id_seq", allocationSize=1)
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="Plan_IdSeq")
    @Column(name="id")
    private Long id;

    @Column(name="cycle_id", nullable=false)
    private Long cycleId;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt;

    @Column(name="is_deleted", nullable=false)
    private Boolean isDeleted;

    @Column(name="target_org_id", nullable=false)
    private Long targetOrgId;

    @Column(name="created_by_org_id", nullable=false)
    private Long createdByOrgId;

    @Enumerated(EnumType.STRING)
    @Column(name="plan_level", columnDefinition="plan_level", nullable=false)
    private PlanLevel planLevel;

    @Column(name="status", nullable=false)
    private String status;

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
        plan.status = "DRAFT";
        plan.addEvent(new PlanCreatedEvent(plan.id, planLevel.name(), targetOrgId));
        return plan;
    }

    /**
     * 激活计划（下发）
     * DRAFT -> ACTIVE
     */
    public void activate() {
        if ("ACTIVE".equals(this.status)) {
            throw new IllegalStateException("Plan is already active");
        }
        this.status = "ACTIVE";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 提交计划审批
     * DRAFT -> PENDING
     */
    public void submitForApproval() {
        if (!"DRAFT".equals(this.status)) {
            throw new IllegalStateException("Plan must be in DRAFT state to submit for approval");
        }
        this.status = "PENDING";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 审批通过计划
     * PENDING -> ACTIVE
     */
    public void approve() {
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException("Plan must be in PENDING state to approve");
        }
        this.status = "ACTIVE";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 驳回计划
     * PENDING -> REJECTED
     */
    public void reject() {
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException("Plan must be in PENDING state to reject");
        }
        this.status = "REJECTED";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 撤回计划到草稿
     * PENDING/REJECTED -> DRAFT
     */
    public void withdraw() {
        if ("DRAFT".equals(this.status) || "ACTIVE".equals(this.status) || "COMPLETED".equals(this.status)) {
            throw new IllegalStateException("Cannot withdraw plan in current state: " + this.status);
        }
        this.status = "DRAFT";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 完成计划
     * ACTIVE -> COMPLETED
     */
    public void complete() {
        if (!"ACTIVE".equals(this.status)) {
            throw new IllegalStateException("Plan must be active to complete");
        }
        this.status = "COMPLETED";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 取消计划
     */
    public void cancel() {
        if ("CANCELLED".equals(this.status) || "COMPLETED".equals(this.status)) {
            throw new IllegalStateException("Plan cannot be cancelled");
        }
        this.status = "CANCELLED";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查计划是否可编辑（草稿或被驳回状态）
     */
    public boolean isEditable() {
        return "DRAFT".equals(this.status) || "REJECTED".equals(this.status);
    }

    /**
     * 检查计划是否已下发（激活状态）
     */
    public boolean isDistributed() {
        return "ACTIVE".equals(this.status);
    }

    /**
     * 检查计划是否处于审批中
     */
    public boolean isPending() {
        return "PENDING".equals(this.status);
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
