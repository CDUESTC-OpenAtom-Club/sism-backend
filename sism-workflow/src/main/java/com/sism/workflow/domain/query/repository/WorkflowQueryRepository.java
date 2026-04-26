package com.sism.workflow.domain.query.repository;

import com.sism.workflow.domain.runtime.model.AuditInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface WorkflowQueryRepository {

    Optional<AuditInstance> findAuditInstanceById(Long instanceId);

    Optional<AuditInstance> findPendingAuditInstanceByStepIdAndUserId(Long stepInstanceId, Long userId);

    List<AuditInstance> findPendingAuditInstancesByUserId(Long userId);

    Page<AuditInstance> findPendingAuditInstancesByUserId(Long userId, Pageable pageable);

    long countPendingTasksByUserId(Long userId);

    List<PendingTaskIdentity> findPendingTaskIdentitiesByUserId(Long userId);

    List<AuditInstance> findApprovedAuditInstancesByUserId(Long userId);

    Page<AuditInstance> findApprovedAuditInstancesByUserId(Long userId, Pageable pageable);

    List<AuditInstance> findAppliedAuditInstancesByUserId(Long userId);

    Page<AuditInstance> findAppliedAuditInstancesByUserId(Long userId, Pageable pageable);

    List<AuditInstance> findAuditInstanceHistory(Long instanceId);

    record PendingTaskIdentity(
            Long stepInstanceId,
            Long instanceId,
            String entityType,
            Long entityId
    ) {
    }
}
