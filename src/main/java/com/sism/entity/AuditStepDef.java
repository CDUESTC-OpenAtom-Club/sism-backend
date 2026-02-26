package com.sism.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Audit Step Definition entity
 * Defines individual steps within an audit workflow
 */
@Getter
@Setter
@Entity
@Table(name = "audit_step_def", indexes = {
    @Index(name = "idx_flow_id", columnList = "flow_id"),
    @Index(name = "idx_flow_step_order", columnList = "flow_id, step_order")
})
public class AuditStepDef extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull(message = "Flow ID is required")
    @Column(name = "flow_id", nullable = false)
    private Long flowId;

    @NotNull(message = "Step order is required")
    @Min(value = 1, message = "Step order must be at least 1")
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @NotBlank(message = "Step name is required")
    @Size(max = 100, message = "Step name must not exceed 100 characters")
    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @NotBlank(message = "Approver role is required")
    @Size(max = 50, message = "Approver role must not exceed 50 characters")
    @Column(name = "approver_role", nullable = false, length = 50)
    private String approverRole;

    @NotNull(message = "Is required flag is required")
    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flow_id", referencedColumnName = "id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_audit_step_flow"))
    private AuditFlowDef auditFlowDef;

    // ==================== New Fields for Multi-Level Approval ====================

    /**
     * Approver type: ROLE, USER, SUPERVISOR, DYNAMIC_USER
     * - ROLE: Approver determined by role code
     * - USER: Specific user(s) assigned
     * - SUPERVISOR: Supervisor of the submitter (resolved dynamically)
     * - DYNAMIC_USER: Users resolved based on context
     */
    @Column(name = "approver_type", length = 20)
    private String approverType = "ROLE";

    /**
     * List of approver IDs (used when approver_type is USER or DYNAMIC_USER)
     */
    @Column(name = "approver_ids", columnDefinition = "TEXT[]")
    private List<Long> approverIds;

    /**
     * Approval mode: SEQUENTIAL, PARALLEL
     * - SEQUENTIAL: One approver at a time
     * - PARALLEL: Multiple approvers can approve concurrently (for joint approval)
     */
    @Column(name = "approval_mode", length = 20)
    private String approvalMode = "SEQUENTIAL";

    /**
     * Supervisor level (used when approver_type is SUPERVISOR)
     * - 1: Direct supervisor
     * - 2: Level-2 supervisor
     */
    @Column(name = "supervisor_level")
    private Integer supervisorLevel;

    // ==================== Helper Methods ====================

    /**
     * Check if this step uses sequential approval
     */
    public boolean isSequential() {
        return "SEQUENTIAL".equalsIgnoreCase(approvalMode);
    }

    /**
     * Check if this step uses parallel (joint) approval
     */
    public boolean isParallel() {
        return "PARALLEL".equalsIgnoreCase(approvalMode);
    }

    /**
     * Check if approver is resolved dynamically (SUPERVISOR type)
     */
    public boolean isDynamicApprover() {
        return "SUPERVISOR".equalsIgnoreCase(approverType);
    }
}
