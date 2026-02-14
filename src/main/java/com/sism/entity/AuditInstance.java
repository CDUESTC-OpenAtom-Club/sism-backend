package com.sism.entity;

import com.sism.enums.AuditEntityType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
}
