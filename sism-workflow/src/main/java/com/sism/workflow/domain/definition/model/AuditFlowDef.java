package com.sism.workflow.domain.definition.model;

import com.sism.shared.domain.model.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AuditFlowDef - 审批流定义聚合根
 * Defines approval flow templates for different entity types.
 */
@Getter
@Setter
@Entity
@Table(name = "audit_flow_def", schema = "public")
@Access(AccessType.FIELD)
public class AuditFlowDef extends AggregateRoot<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flow_code", nullable = false, unique = true)
    private String flowCode;

    @Column(name = "flow_name", nullable = false)
    private String flowName;

    @Column(name = "description")
    private String description;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "is_enabled", nullable = false)
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
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Workflow steps are required");
        }

        List<AuditStepDef> orderedSteps = steps.stream()
                .sorted(Comparator
                        .comparing((AuditStepDef step) -> step.getStepOrder() == null ? Integer.MAX_VALUE : step.getStepOrder())
                        .thenComparing(step -> step.getId() == null ? Long.MAX_VALUE : step.getId()))
                .toList();

        Set<Integer> orders = orderedSteps.stream()
                .map(AuditStepDef::getStepOrder)
                .collect(Collectors.toSet());
        if (orders.size() != orderedSteps.size()) {
            throw new IllegalArgumentException("Workflow step order must be unique");
        }

        for (int i = 0; i < orderedSteps.size(); i++) {
            AuditStepDef step = orderedSteps.get(i);
            int expectedOrder = i + 1;
            if (step.getStepOrder() == null || step.getStepOrder() != expectedOrder) {
                throw new IllegalArgumentException("Workflow step order must be continuous starting from 1");
            }
            step.validateForTemplate(i, i == 0);
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
