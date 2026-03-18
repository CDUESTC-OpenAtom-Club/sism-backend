package com.sism.workflow.infrastructure.persistence;

import com.sism.shared.domain.model.workflow.AuditFlowDef;
import com.sism.shared.domain.model.workflow.AuditInstance;
import com.sism.shared.domain.model.workflow.WorkflowTask;
import com.sism.workflow.domain.AuditStatus;
import com.sism.workflow.domain.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * WorkflowRepositoryFacade - 工作流仓储门面类
 * 聚合 JpaWorkflowRepository、AuditFlowDefJpaRepository 和 WorkflowTaskJpaRepository
 * 作为主要的 WorkflowRepository 实现（@Primary）
 */
@Component
@Primary
@RequiredArgsConstructor
public class WorkflowRepositoryFacade implements WorkflowRepository {

    private final JpaWorkflowRepository jpaWorkflowRepository;
    private final AuditFlowDefJpaRepository auditFlowDefJpaRepository;
    private final WorkflowTaskJpaRepository workflowTaskJpaRepository;

    // ==================== Audit Flow Definition ====================

    @Override
    public List<AuditFlowDef> findAllAuditFlowDefs() {
        return jpaWorkflowRepository.findAllAuditFlowDefs();
    }

    @Override
    public Page<AuditFlowDef> findAllAuditFlowDefs(Pageable pageable) {
        return auditFlowDefJpaRepository.findAll(pageable);
    }

    @Override
    public Optional<AuditFlowDef> findAuditFlowDefById(Long id) {
        return jpaWorkflowRepository.findAuditFlowDefById(id);
    }

    @Override
    public Optional<AuditFlowDef> findAuditFlowDefByCode(String flowCode) {
        return jpaWorkflowRepository.findAuditFlowDefByCode(flowCode);
    }

    @Override
    public List<AuditFlowDef> findAuditFlowDefsByEntityType(String entityType) {
        return jpaWorkflowRepository.findAuditFlowDefsByEntityType(entityType);
    }

    @Override
    public AuditFlowDef saveAuditFlowDef(AuditFlowDef flowDef) {
        return auditFlowDefJpaRepository.save(flowDef);
    }

    // ==================== Audit Instance ====================

    @Override
    public Optional<AuditInstance> findById(Long id) {
        return jpaWorkflowRepository.findById(id);
    }

    @Override
    public List<AuditInstance> findAll() {
        return jpaWorkflowRepository.findAll();
    }

    @Override
    public List<AuditInstance> findByBusinessTypeAndBusinessId(String businessType, Long businessId) {
        return jpaWorkflowRepository.findByBusinessTypeAndBusinessId(businessType, businessId);
    }

    @Override
    public List<AuditInstance> findByBusinessId(Long businessId) {
        return jpaWorkflowRepository.findByBusinessId(businessId);
    }

    @Override
    public List<AuditInstance> findByStatus(AuditStatus status) {
        return jpaWorkflowRepository.findByStatus(status);
    }

    @Override
    public List<AuditInstance> findByCurrentApproverId(Long approverId) {
        return jpaWorkflowRepository.findByCurrentApproverId(approverId);
    }

    @Override
    public List<AuditInstance> findByInitiatorId(Long initiatorId) {
        return jpaWorkflowRepository.findByInitiatorId(initiatorId);
    }

    @Override
    public AuditInstance save(AuditInstance auditInstance) {
        return jpaWorkflowRepository.save(auditInstance);
    }

    @Override
    public void delete(AuditInstance auditInstance) {
        jpaWorkflowRepository.delete(auditInstance);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaWorkflowRepository.existsById(id);
    }

    // ==================== Additional Query Methods ====================

    @Override
    public Optional<AuditInstance> findAuditInstanceById(Long instanceId) {
        return jpaWorkflowRepository.findAuditInstanceById(instanceId);
    }

    @Override
    public List<AuditInstance> findPendingAuditInstancesByUserId(Long userId) {
        return jpaWorkflowRepository.findPendingAuditInstancesByUserId(userId);
    }

    @Override
    public List<AuditInstance> findApprovedAuditInstancesByUserId(Long userId) {
        return jpaWorkflowRepository.findApprovedAuditInstancesByUserId(userId);
    }

    @Override
    public List<AuditInstance> findAppliedAuditInstancesByUserId(Long userId) {
        return jpaWorkflowRepository.findAppliedAuditInstancesByUserId(userId);
    }

    @Override
    public List<AuditInstance> findAuditInstanceHistory(Long instanceId) {
        return jpaWorkflowRepository.findAuditInstanceHistory(instanceId);
    }

    @Override
    public AuditInstance saveAuditInstance(AuditInstance instance) {
        return jpaWorkflowRepository.saveAuditInstance(instance);
    }

    @Override
    public long countAuditInstances() {
        return jpaWorkflowRepository.countAuditInstances();
    }

    @Override
    public long countPendingAuditInstances() {
        return jpaWorkflowRepository.countPendingAuditInstances();
    }

    @Override
    public long countApprovedAuditInstances() {
        return jpaWorkflowRepository.countApprovedAuditInstances();
    }

    @Override
    public long countRejectedAuditInstances() {
        return jpaWorkflowRepository.countRejectedAuditInstances();
    }

    @Override
    public boolean hasActiveInstance(Long businessEntityId, String entityType) {
        return jpaWorkflowRepository.hasActiveInstance(businessEntityId, entityType);
    }

    @Override
    public Page<AuditInstance> findAuditInstancesByFlowDefId(Long flowDefId, Pageable pageable) {
        return jpaWorkflowRepository.findAuditInstancesByFlowDefId(flowDefId, pageable);
    }

    // ==================== Workflow Task ====================

    @Override
    public List<WorkflowTask> findAllWorkflowTasks() {
        return jpaWorkflowRepository.findAllWorkflowTasks();
    }

    @Override
    public Optional<WorkflowTask> findWorkflowTaskById(Long id) {
        return jpaWorkflowRepository.findWorkflowTaskById(id);
    }

    @Override
    public List<WorkflowTask> findWorkflowTasksByStatus(String status) {
        return jpaWorkflowRepository.findWorkflowTasksByStatus(status);
    }

    @Override
    public List<WorkflowTask> findWorkflowTasksByAssigneeId(Long assigneeId) {
        return jpaWorkflowRepository.findWorkflowTasksByAssigneeId(assigneeId);
    }

    @Override
    public WorkflowTask saveWorkflowTask(WorkflowTask task) {
        return workflowTaskJpaRepository.save(task);
    }
}
