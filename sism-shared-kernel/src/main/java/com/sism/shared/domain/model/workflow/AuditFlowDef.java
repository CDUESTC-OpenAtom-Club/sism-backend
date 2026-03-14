package com.sism.shared.domain.model.workflow;

import com.sism.shared.domain.model.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AuditFlowDef - 审批流定义聚合根
 * Defines approval flow templates for different entity types
 */
@Getter
@Setter
@Entity
@Table(name = "audit_flow_def")
public class AuditFlowDef extends AggregateRoot<Long> {

    @Column(name = "flow_code", nullable = false, unique = true)
    private String flowCode;

    @Column(name = "flow_name", nullable = false)
    private String flowName;

    @Column(name = "description")
    private String description;

    @Column(name = "entity_type", nullable = false)
    private String entityType;  // INDICATOR, PLAN_REPORT, etc.

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "version")
    private Integer version = 1;

    @OneToMany(mappedBy = "flowDef", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<AuditStepDef> steps = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public boolean canPublish() {
        return isActive != null && isActive;
    }

    @Override
    public void validate() {
        if (flowCode == null || flowCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Flow code is required");
        }
        if (flowName == null || flowName.trim().isEmpty()) {
            throw new IllegalArgumentException("Flow name is required");
        }
        if (entityType == null || entityType.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity type is required");
        }
    }

    public void addStep(AuditStepDef step) {
        steps.add(step);
        step.setFlowDef(this);
    }

    public void removeStep(AuditStepDef step) {
        steps.remove(step);
        step.setFlowDef(null);
    }

    public int getStepCount() {
        return steps.size();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
