package com.sism.workflow.infrastructure.persistence;

import com.sism.workflow.domain.AuditStatus;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.WorkflowTask;
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
 * JpaWorkflowRepository - 工作流实例与查询 JPA 实现
 */
@Repository
public interface JpaWorkflowRepository extends JpaRepository<AuditInstance, Long> {

    @Query("SELECT d FROM AuditFlowDef d")
    List<AuditFlowDef> findAllAuditFlowDefs();

    @Query("SELECT d FROM AuditFlowDef d WHERE d.id = :id")
    Optional<AuditFlowDef> findAuditFlowDefById(@Param("id") Long id);

    @Query("SELECT d FROM AuditFlowDef d WHERE d.flowCode = :flowCode")
    Optional<AuditFlowDef> findAuditFlowDefByCode(@Param("flowCode") String flowCode);

    @Query("SELECT d FROM AuditFlowDef d WHERE d.entityType = :entityType")
    List<AuditFlowDef> findAuditFlowDefsByEntityType(@Param("entityType") String entityType);

    Optional<AuditInstance> findById(Long id);

    List<AuditInstance> findAll();

    @Query("SELECT a FROM AuditInstance a WHERE a.entityType = :businessType AND a.entityId = :businessId")
    List<AuditInstance> findByBusinessTypeAndBusinessId(@Param("businessType") String businessType,
                                                         @Param("businessId") Long businessId);

    @Query("SELECT a FROM AuditInstance a WHERE a.entityId = :businessId")
    List<AuditInstance> findByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT a FROM AuditInstance a WHERE a.status = :status")
    List<AuditInstance> findByStatus(@Param("status") AuditStatus status);

    @Query("SELECT a FROM AuditInstance a WHERE a.requesterId = :initiatorId")
    List<AuditInstance> findByInitiatorId(@Param("initiatorId") Long initiatorId);

    @Query("SELECT a FROM AuditInstance a WHERE a.id = :instanceId")
    Optional<AuditInstance> findAuditInstanceById(@Param("instanceId") Long instanceId);

    @Query("SELECT DISTINCT a FROM AuditInstance a JOIN a.stepInstances s WHERE s.id = :stepInstanceId")
    Optional<AuditInstance> findByStepInstanceId(@Param("stepInstanceId") Long stepInstanceId);

    @Query("SELECT DISTINCT a FROM AuditInstance a JOIN a.stepInstances s " +
            "WHERE a.status = 'IN_REVIEW' AND s.status = 'PENDING' AND s.approverId = :userId")
    List<AuditInstance> findPendingAuditInstancesByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT a FROM AuditInstance a JOIN a.stepInstances s " +
            "WHERE a.status = 'APPROVED' AND s.approverId = :userId")
    List<AuditInstance> findApprovedAuditInstancesByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM AuditInstance a WHERE a.requesterId = :userId")
    List<AuditInstance> findAppliedAuditInstancesByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM AuditInstance a WHERE a.id = :instanceId")
    List<AuditInstance> findAuditInstanceHistory(@Param("instanceId") Long instanceId);

    @Query("SELECT COUNT(a) FROM AuditInstance a")
    long countAuditInstances();

    @Query("SELECT COUNT(a) FROM AuditInstance a WHERE a.status = 'IN_REVIEW'")
    long countPendingAuditInstances();

    @Query("SELECT COUNT(a) FROM AuditInstance a WHERE a.status = 'APPROVED'")
    long countApprovedAuditInstances();

    @Query("SELECT COUNT(a) FROM AuditInstance a WHERE a.status = 'REJECTED'")
    long countRejectedAuditInstances();

    @Query("SELECT COUNT(a) > 0 FROM AuditInstance a WHERE " +
            "a.entityId = :businessEntityId AND " +
            "a.entityType = :entityType AND " +
            "a.status = 'IN_REVIEW'")
    boolean hasActiveInstance(@Param("businessEntityId") Long businessEntityId,
                              @Param("entityType") String entityType);

    @Query("SELECT a FROM AuditInstance a WHERE a.flowDefId = :flowDefId")
    Page<AuditInstance> findAuditInstancesByFlowDefId(@Param("flowDefId") Long flowDefId, Pageable pageable);

    @Query("SELECT t FROM WorkflowTask t")
    List<WorkflowTask> findAllWorkflowTasks();

    @Query("SELECT t FROM WorkflowTask t WHERE t.id = :id")
    Optional<WorkflowTask> findWorkflowTaskById(@Param("id") Long id);

    @Query("SELECT t FROM WorkflowTask t WHERE t.status = :status")
    List<WorkflowTask> findWorkflowTasksByStatus(@Param("status") String status);

    @Query("SELECT t FROM WorkflowTask t WHERE t.assigneeId = :assigneeId")
    List<WorkflowTask> findWorkflowTasksByAssigneeId(@Param("assigneeId") Long assigneeId);

}
