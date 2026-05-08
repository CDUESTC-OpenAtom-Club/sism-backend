package com.sism.workflow.domain.repository;

import com.sism.workflow.domain.AuditStatus;
import com.sism.workflow.domain.definition.AuditFlowDef;
import com.sism.workflow.domain.runtime.AuditInstance;
import com.sism.workflow.domain.runtime.WorkflowTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface WorkflowRepository {

    List<AuditFlowDef> findAllAuditFlowDefs();

    Page<AuditFlowDef> findAllAuditFlowDefs(Pageable pageable);

    Optional<AuditFlowDef> findAuditFlowDefById(Long id);

    Optional<AuditFlowDef> findAuditFlowDefByCode(String flowCode);

    List<AuditFlowDef> findAuditFlowDefsByEntityType(String entityType);

    AuditFlowDef saveAuditFlowDef(AuditFlowDef flowDef);

    Optional<AuditInstance> findById(Long id);

    List<AuditInstance> findAll();

    List<AuditInstance> findByBusinessTypeAndBusinessId(String businessType, Long businessId);

    List<AuditInstance> findByBusinessId(Long businessId);

    List<AuditInstance> findByStatus(AuditStatus status);

    List<AuditInstance> findByCurrentApproverId(Long approverId);

    List<AuditInstance> findByInitiatorId(Long initiatorId);

    AuditInstance save(AuditInstance auditInstance);

    void delete(AuditInstance auditInstance);

    boolean existsById(Long id);

    Optional<AuditInstance> findAuditInstanceById(Long instanceId);

    List<AuditInstance> findPendingAuditInstancesByUserId(Long userId);

    List<AuditInstance> findApprovedAuditInstancesByUserId(Long userId);

    List<AuditInstance> findAppliedAuditInstancesByUserId(Long userId);

    List<AuditInstance> findAuditInstanceHistory(Long instanceId);

    AuditInstance saveAuditInstance(AuditInstance instance);

    long countAuditInstances();

    long countPendingAuditInstances();

    long countApprovedAuditInstances();

    long countRejectedAuditInstances();

    boolean hasActiveInstance(Long businessEntityId, String entityType);

    Page<AuditInstance> findAuditInstancesByFlowDefId(Long flowDefId, Pageable pageable);

    List<WorkflowTask> findAllWorkflowTasks();

    Optional<WorkflowTask> findWorkflowTaskById(Long id);

    List<WorkflowTask> findWorkflowTasksByStatus(String status);

    List<WorkflowTask> findWorkflowTasksByAssigneeId(Long assigneeId);

    WorkflowTask saveWorkflowTask(WorkflowTask task);
}
