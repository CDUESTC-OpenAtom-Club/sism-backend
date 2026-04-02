package com.sism.workflow.domain.repository;

import com.sism.workflow.domain.AuditStatus;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.WorkflowTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * WorkflowRepository - 工作流仓储接口
 * 定义在领域层，由基础设施层实现
 */
public interface WorkflowRepository {

    // ==================== Audit Flow Definition ====================

    List<AuditFlowDef> findAllAuditFlowDefs();

    default Page<AuditFlowDef> findAllAuditFlowDefs(Pageable pageable) {
        throw new UnsupportedOperationException("Pagination not implemented");
    }

    Optional<AuditFlowDef> findAuditFlowDefById(Long id);

    Optional<AuditFlowDef> findAuditFlowDefByCode(String flowCode);

    List<AuditFlowDef> findAuditFlowDefsByEntityType(String entityType);

    AuditFlowDef saveAuditFlowDef(AuditFlowDef flowDef);

    // ==================== Audit Instance ====================

    /**
     * 根据ID查询审批实例
     */
    Optional<AuditInstance> findById(Long id);

    /**
     * 查询所有审批实例
     */
    List<AuditInstance> findAll();

    /**
     * 根据业务类型和业务ID查询审批实例
     */
    List<AuditInstance> findByBusinessTypeAndBusinessId(String businessType, Long businessId);

    /**
     * 根据业务ID查询审批实例
     */
    List<AuditInstance> findByBusinessId(Long businessId);

    /**
     * 根据状态查询审批实例
     */
    List<AuditInstance> findByStatus(AuditStatus status);

    /**
     * 根据当前审批人查询审批实例
     */
    List<AuditInstance> findByCurrentApproverId(Long approverId);

    /**
     * 根据发起人查询审批实例
     */
    List<AuditInstance> findByInitiatorId(Long initiatorId);

    /**
     * 保存审批实例
     */
    AuditInstance save(AuditInstance auditInstance);

    /**
     * 删除审批实例
     */
    void delete(AuditInstance auditInstance);

    /**
     * 检查审批实例是否存在
     */
    boolean existsById(Long id);

    // ==================== Additional Query Methods ====================

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

    // ==================== New Methods for Business Workflow ====================

    /**
     * 检查是否有活跃的工作流实例
     */
    boolean hasActiveInstance(Long businessEntityId, String entityType);

    /**
     * 根据工作流定义ID分页查询实例
     */
    Page<AuditInstance> findAuditInstancesByFlowDefId(Long flowDefId, Pageable pageable);

    // ==================== Workflow Task ====================

    List<WorkflowTask> findAllWorkflowTasks();

    Optional<WorkflowTask> findWorkflowTaskById(Long id);

    List<WorkflowTask> findWorkflowTasksByStatus(String status);

    List<WorkflowTask> findWorkflowTasksByAssigneeId(Long assigneeId);

    WorkflowTask saveWorkflowTask(WorkflowTask task);
}
