package com.sism.workflow.domain.query.repository;

import com.sism.workflow.domain.runtime.model.AuditInstance;

import java.util.List;
import java.util.Optional;

public interface WorkflowQueryRepository {

    Optional<AuditInstance> findAuditInstanceById(Long instanceId);

    List<AuditInstance> findPendingAuditInstancesByUserId(Long userId);

    List<AuditInstance> findApprovedAuditInstancesByUserId(Long userId);

    List<AuditInstance> findAppliedAuditInstancesByUserId(Long userId);

    List<AuditInstance> findAuditInstanceHistory(Long instanceId);

}
