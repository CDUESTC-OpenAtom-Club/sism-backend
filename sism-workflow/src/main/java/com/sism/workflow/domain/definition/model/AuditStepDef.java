package com.sism.workflow.domain.definition.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * AuditStepDef - 审批步骤定义
 * Defines a single step in an approval flow.
 */
@Getter
@Setter
@Entity
@Table(name = "audit_step_def", schema = "public")
public class AuditStepDef {

    public static final String STEP_TYPE_SUBMIT = "SUBMIT";
    public static final String STEP_TYPE_APPROVAL = "APPROVAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flow_id", nullable = false)
    @JsonIgnore
    private AuditFlowDef flowDef;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Column(name = "step_no", nullable = false)
    private Integer stepOrder;

    @Column(name = "step_type")
    private String stepType;

    @Column(name = "role_id")
    private Long roleId;

    @Transient
    private Boolean isRequired = true;

    @Column(name = "is_terminal")
    private Boolean isTerminal = false;

    public void validateForTemplate(int index, boolean firstStep) {
        if (stepName == null || stepName.isBlank()) {
            throw new IllegalArgumentException("Workflow step name is required");
        }
        if (stepOrder == null || stepOrder <= 0) {
            throw new IllegalArgumentException("Workflow step order must be positive");
        }
        if (stepType == null || stepType.isBlank()) {
            throw new IllegalArgumentException("Workflow step type is required: " + stepName);
        }

        String effectiveType = stepType.trim().toUpperCase();
        if (!STEP_TYPE_SUBMIT.equals(effectiveType) && !STEP_TYPE_APPROVAL.equals(effectiveType)) {
            throw new IllegalArgumentException("Unsupported workflow step type: " + stepType);
        }

        if (firstStep) {
            if (!STEP_TYPE_SUBMIT.equals(effectiveType)) {
                throw new IllegalArgumentException("The first workflow step must be SUBMIT");
            }
        } else if (!STEP_TYPE_APPROVAL.equals(effectiveType)) {
            throw new IllegalArgumentException("Workflow steps after the first must be APPROVAL");
        }

        if (STEP_TYPE_SUBMIT.equals(effectiveType) && roleId != null) {
            throw new IllegalArgumentException("SUBMIT step must not define role assignment: " + stepName);
        }
        if (STEP_TYPE_APPROVAL.equals(effectiveType) && (roleId == null || roleId <= 0)) {
            throw new IllegalArgumentException("APPROVAL step must define role assignment: " + stepName);
        }
    }

    public String resolveEffectiveStepType() {
        if (stepType != null && !stepType.isBlank()) {
            return stepType.trim().toUpperCase();
        }

        // Compatibility fallback for legacy rows before step_type is backfilled.
        if (stepName != null && stepName.contains("提交")) {
            return STEP_TYPE_SUBMIT;
        }

        return STEP_TYPE_APPROVAL;
    }

    public boolean hasExplicitStepType() {
        return stepType != null && !stepType.isBlank();
    }

    public boolean isSubmitStep() {
        return STEP_TYPE_SUBMIT.equals(resolveEffectiveStepType());
    }

    public boolean isApprovalStep() {
        return STEP_TYPE_APPROVAL.equals(resolveEffectiveStepType());
    }
}
