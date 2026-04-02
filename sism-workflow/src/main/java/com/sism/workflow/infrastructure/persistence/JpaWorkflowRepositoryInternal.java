package com.sism.workflow.infrastructure.persistence;

import com.sism.workflow.domain.AuditStatus;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.repository.WorkflowRepository;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.WorkflowTask;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JpaWorkflowRepositoryInternal - 工作流仓储内部实现
 * 处理复杂查询和业务逻辑
 */
@Repository
@RequiredArgsConstructor
public class JpaWorkflowRepositoryInternal implements WorkflowRepository {

    private final JpaWorkflowRepository jpaWorkflowRepository;
    private final AuditFlowDefJpaRepository auditFlowDefJpaRepository;
    private final WorkflowTaskJpaRepository workflowTaskJpaRepository;

    // ==================== Audit Flow Definition ====================

    @Override
    public List<AuditFlowDef> findAllAuditFlowDefs() {
        return jpaWorkflowRepository.findAllAuditFlowDefs();
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

    @Override
    public Page<AuditFlowDef> findAllAuditFlowDefs(Pageable pageable) {
        return auditFlowDefJpaRepository.findAll(pageable);
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
        return jpaWorkflowRepository.findByStatus(status.name());
    }

    @Override
    public List<AuditInstance> findByCurrentApproverId(Long approverId) {
        // 查询当前审批人是指定用户的待审批实例
        // 通过 stepInstances 查找待审批的步骤实例
        // 这是一个复杂的查询，目前简化实现
        // 实际应该添加一个自定义查询方法
        return List.of();
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
        return jpaWorkflowRepository.save(instance);
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
