package com.sism.shared.domain.model.workflow;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AuditStepInstance - 审批步骤实例
 * Represents a single step in an approval workflow instance
 */
@Getter
@Setter
@Entity
@Table(name = "audit_step_instance")
public class AuditStepInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    private AuditInstance instance;

    @Column(name = "step_index", nullable = false)
    private Integer stepIndex;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Column(name = "status")
    private String status;  // PENDING, APPROVED, REJECTED, SKIPPED

    @Column(name = "approver_id")
    private Long approverId;

    @Column(name = "approver_name")
    private String approverName;

    @Column(name = "comment")
    private String comment;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
