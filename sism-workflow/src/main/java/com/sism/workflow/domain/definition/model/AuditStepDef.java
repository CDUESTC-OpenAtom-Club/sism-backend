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
