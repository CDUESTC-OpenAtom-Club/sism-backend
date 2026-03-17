package com.sism.shared.domain.model.workflow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * AuditStepDef - 审批步骤定义
 * Defines a single step in an approval flow
 */
@Getter
@Setter
@Entity
@Table(name = "audit_step_def", schema = "public")
public class AuditStepDef {

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
    private String stepType;  // APPROVAL, NOTIFICATION, CONDITION

    @Column(name = "approver_type")
    private String approverType;  // USER, ROLE, ORG_MANAGER

    @Column(name = "approver_id")
    private Long approverId;

    @Column(name = "timeout_hours")
    private Integer timeoutHours;

    @Transient
    private Boolean isRequired = true;

    @Column(name = "can_skip")
    private Boolean canSkip = false;
}
