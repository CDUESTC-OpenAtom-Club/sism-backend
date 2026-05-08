package com.sism.shared.domain.model.workflow;

import com.sism.shared.domain.model.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AuditFlowDef - 审批流定义聚合根
 * Defines approval flow templates for different entity types
 */
@Getter
@Entity
@Table(name = "audit_flow_def")
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
    private String entityType;  // INDICATOR, PLAN_REPORT, etc.

    @Column(name = "is_enabled", nullable = false)
    private Boolean isActive = true;

    @Column(name = "version")
    private Integer version = 1;

    @OneToMany(mappedBy = "flowDef", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<AuditStepDef> steps = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime persistedCreatedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime persistedUpdatedAt;

    @Override
    public boolean canPublish() {
        return isActive != null && isActive;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        assertIdUnchanged(this.id, id);
        this.id = id;
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return persistedCreatedAt;
    }

    @Override
    public void setCreatedAt(LocalDateTime createdAt) {
        super.setCreatedAt(createdAt);
        this.persistedCreatedAt = createdAt;
    }

    @Override
    public LocalDateTime getUpdatedAt() {
        return persistedUpdatedAt;
    }

    @Override
    public void setUpdatedAt(LocalDateTime updatedAt) {
        super.setUpdatedAt(updatedAt);
        this.persistedUpdatedAt = updatedAt;
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

    public void setFlowCode(String flowCode) {
        this.flowCode = flowCode;
    }

    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public void setSteps(List<AuditStepDef> steps) {
        this.steps.clear();
        if (steps != null) {
            steps.forEach(this::addStep);
        }
    }

    @PrePersist
    protected void onCreate() {
        setCreatedAt(LocalDateTime.now());
        setUpdatedAt(LocalDateTime.now());
    }

    @PreUpdate
    protected void onUpdate() {
        setUpdatedAt(LocalDateTime.now());
    }
}
