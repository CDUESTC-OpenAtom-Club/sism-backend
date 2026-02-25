package com.sism.entity;

import com.sism.enums.AuditEntityType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Audit Instance entity
 * Represents a running instance of an audit workflow for a specific entity
 */
@Getter
@Setter
@Entity
@Table(name = "audit_instance", indexes = {
    @Index(name = "idx_flow_id", columnList = "flow_id"),
    @Index(name = "idx_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_status", columnList = "status")
})
public class AuditInstance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull(message = "Flow ID is required")
    @Column(name = "flow_id", nullable = false)
    private Long flowId;

    @NotNull(message = "Entity ID is required")
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @NotNull(message = "Entity type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private AuditEntityType entityType;

    @Column(name = "current_step_id")
    private Long currentStepId;

    @NotBlank(message = "Status is required")
    @Size(max = 50, message = "Status must not exceed 50 characters")
    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING";

    @NotNull(message = "Initiated by is required")
    @Column(name = "initiated_by", nullable = false)
    private Long initiatedBy;

    @NotNull(message = "Initiated at is required")
    @Column(name = "initiated_at", nullable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flow_id", referencedColumnName = "id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_audit_instance_flow"))
    private AuditFlowDef auditFlowDef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_step_id", referencedColumnName = "id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_audit_instance_step"))
    private AuditStepDef currentStep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by", referencedColumnName = "id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_audit_instance_user"))
    private SysUser initiator;

    // ==================== New Fields for Multi-Level Approval ====================

    /**
     * Current step order in the approval flow
     */
    @Column(name = "current_step_order")
    private Integer currentStepOrder = 1;

    /**
     * Submitter's department ID
     */
    @Column(name = "submitter_dept_id")
    private Long submitterDeptId;

    /**
     * Direct supervisor ID (level 1)
     */
    @Column(name = "direct_supervisor_id")
    private Long directSupervisorId;

    /**
     * Level-2 supervisor ID
     */
    @Column(name = "level2_supervisor_id")
    private Long level2SupervisorId;

    /**
     * Superior department ID (for final approval)
     */
    @Column(name = "superior_dept_id")
    private Long superiorDeptId;

    /**
     * Pending approvers list (for parallel approval tracking)
     */
    @Column(name = "pending_approvers", columnDefinition = "BIGINT[]")
    private List<Long> pendingApprovers;

    /**
     * Approved approvers list (for parallel approval tracking)
     */
    @Column(name = "approved_approvers", columnDefinition = "BIGINT[]")
    private List<Long> approvedApprovers;

    /**
     * Rejected approvers list (for parallel approval tracking)
     */
    @Column(name = "rejected_approvers", columnDefinition = "BIGINT[]")
    private List<Long> rejectedApprovers;

    // ==================== Helper Methods ====================

    /**
     * Check if this is the first approval step
     */
    public boolean isFirstStep() {
        return currentStepOrder != null && currentStepOrder == 1;
    }

    /**
     * Check if this is the final approval step
     */
    public boolean isFinalStep() {
        return currentStepOrder != null && currentStepOrder == 3;
    }

    /**
     * Check if this approval is still pending
     */
    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status) || "IN_PROGRESS".equalsIgnoreCase(status);
    }

    /**
     * Check if this approval is completed
     */
    public boolean isCompleted() {
        return "APPROVED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status);
    }

    /**
     * Check if this approval is rejected
     */
    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(status);
    }
}
