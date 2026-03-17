package com.sism.workflow.infrastructure.persistence;

import com.sism.shared.domain.model.workflow.AuditFlowDef;
import com.sism.shared.domain.model.workflow.AuditInstance;
import com.sism.shared.domain.model.workflow.WorkflowTask;
import com.sism.workflow.domain.repository.WorkflowRepository;
import com.sism.workflow.domain.AuditStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JpaWorkflowRepository - 工作流仓储 JPA 实现
 * 继承自 JpaRepository 和 WorkflowRepository
 * 注意：跨实体操作（AuditFlowDef、WorkflowTask）由 JpaWorkflowRepositoryImpl 处理
 */
@Repository
public interface JpaWorkflowRepository extends JpaRepository<AuditInstance, Long>, WorkflowRepository {

    // ==================== Audit Flow Definition ====================

    @Query("SELECT d FROM AuditFlowDef d")
    List<AuditFlowDef> findAllAuditFlowDefs();

    @Query("SELECT d FROM AuditFlowDef d WHERE d.id = :id")
    Optional<AuditFlowDef> findAuditFlowDefById(@Param("id") Long id);

    @Query("SELECT d FROM AuditFlowDef d WHERE d.flowCode = :flowCode")
    Optional<AuditFlowDef> findAuditFlowDefByCode(@Param("flowCode") String flowCode);

    @Query("SELECT d FROM AuditFlowDef d WHERE d.entityType = :entityType")
    List<AuditFlowDef> findAuditFlowDefsByEntityType(@Param("entityType") String entityType);

    // 注意：saveAuditFlowDef 由 WorkflowRepositoryFacade 实现
    default AuditFlowDef saveAuditFlowDef(AuditFlowDef flowDef) {
        throw new UnsupportedOperationException("Use WorkflowRepositoryFacade for this operation");
    }

    // ==================== Audit Instance ====================

    @Override
    Optional<AuditInstance> findById(Long id);

    @Override
    List<AuditInstance> findAll();

    @Query("SELECT a FROM AuditInstance a WHERE a.entityType = :businessType AND a.entityId = :businessId")
    List<AuditInstance> findByBusinessTypeAndBusinessId(@Param("businessType") String businessType,
                                                         @Param("businessId") Long businessId);

    @Query("SELECT a FROM AuditInstance a WHERE a.entityId = :businessId")
    List<AuditInstance> findByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT a FROM AuditInstance a WHERE a.status = :status")
    List<AuditInstance> findByStatus(@Param("status") AuditStatus status);

    // 注意：AuditInstance 没有 currentApproverId 字段，此方法返回空列表
    // 实际的审批人查询应通过 stepInstances 关联查询
    default List<AuditInstance> findByCurrentApproverId(Long approverId) {
        return List.of();
    }

    @Query("SELECT a FROM AuditInstance a WHERE a.requesterId = :initiatorId")
    List<AuditInstance> findByInitiatorId(@Param("initiatorId") Long initiatorId);

    @Override
    AuditInstance save(AuditInstance auditInstance);

    @Override
    void delete(AuditInstance auditInstance);

    @Override
    boolean existsById(Long id);

    // ==================== Additional Query Methods ====================

    @Override
    @Query("SELECT a FROM AuditInstance a WHERE a.id = :instanceId")
    Optional<AuditInstance> findAuditInstanceById(@Param("instanceId") Long instanceId);

    @Query("SELECT a FROM AuditInstance a WHERE a.status = 'IN_REVIEW' AND a.requesterId = :userId")
    List<AuditInstance> findPendingAuditInstancesByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM AuditInstance a WHERE a.status = 'APPROVED' AND a.requesterId = :userId")
    List<AuditInstance> findApprovedAuditInstancesByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM AuditInstance a WHERE a.requesterId = :userId")
    List<AuditInstance> findAppliedAuditInstancesByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM AuditInstance a WHERE a.id = :instanceId")
    List<AuditInstance> findAuditInstanceHistory(@Param("instanceId") Long instanceId);

    @Override
    default AuditInstance saveAuditInstance(AuditInstance instance) {
        return save(instance);
    }

    @Query("SELECT COUNT(a) FROM AuditInstance a")
    long countAuditInstances();

    @Query("SELECT COUNT(a) FROM AuditInstance a WHERE a.status = 'IN_REVIEW'")
    long countPendingAuditInstances();

    @Query("SELECT COUNT(a) FROM AuditInstance a WHERE a.status = 'APPROVED'")
    long countApprovedAuditInstances();

    @Query("SELECT COUNT(a) FROM AuditInstance a WHERE a.status = 'REJECTED'")
    long countRejectedAuditInstances();

    // ==================== Custom Methods ====================

    @Query("SELECT COUNT(a) > 0 FROM AuditInstance a WHERE " +
            "a.entityId = :businessEntityId AND " +
            "a.entityType = :entityType AND " +
            "a.status = 'IN_REVIEW'")
    boolean hasActiveInstance(@Param("businessEntityId") Long businessEntityId,
                              @Param("entityType") String entityType);

    @Query("SELECT a FROM AuditInstance a WHERE a.flowDefId = :flowDefId")
    Page<AuditInstance> findAuditInstancesByFlowDefId(@Param("flowDefId") Long flowDefId, Pageable pageable);

    // ==================== Workflow Task ====================

    @Query("SELECT t FROM WorkflowTask t")
    List<WorkflowTask> findAllWorkflowTasks();

    @Query("SELECT t FROM WorkflowTask t WHERE t.id = :id")
    Optional<WorkflowTask> findWorkflowTaskById(@Param("id") Long id);

    @Query("SELECT t FROM WorkflowTask t WHERE t.status = :status")
    List<WorkflowTask> findWorkflowTasksByStatus(@Param("status") String status);

    @Query("SELECT t FROM WorkflowTask t WHERE t.assigneeId = :assigneeId")
    List<WorkflowTask> findWorkflowTasksByAssigneeId(@Param("assigneeId") Long assigneeId);

    // WorkflowTask 保存方法 - 使用 WorkflowTaskJpaRepository
    default WorkflowTask saveWorkflowTask(WorkflowTask task) {
        throw new UnsupportedOperationException("WorkflowTask operations should be handled directly by WorkflowTaskJpaRepository");
    }
}
