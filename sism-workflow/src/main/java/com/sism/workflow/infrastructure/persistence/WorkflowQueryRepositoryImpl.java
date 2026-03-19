package com.sism.workflow.infrastructure.persistence;

import com.sism.workflow.domain.query.repository.WorkflowQueryRepository;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WorkflowQueryRepositoryImpl implements WorkflowQueryRepository {

    private final JpaWorkflowRepository jpaWorkflowRepository;

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
}
