package com.sism.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

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
}
