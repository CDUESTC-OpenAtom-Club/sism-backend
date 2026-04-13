package com.sism.workflow.application.runtime;

import com.sism.workflow.domain.query.repository.WorkflowQueryRepository;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.repository.AuditInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkflowInstanceQueryService {

    private final AuditInstanceRepository auditInstanceRepository;
    private final WorkflowQueryRepository workflowQueryRepository;

    public List<AuditInstance> getAllAuditInstances() {
        return auditInstanceRepository.findAll();
    }

    public AuditInstance getAuditInstanceById(Long instanceId) {
        return auditInstanceRepository.findById(instanceId).orElse(null);
    }

    public List<AuditInstance> getPendingAuditInstancesByUserId(Long userId) {
        return workflowQueryRepository.findPendingAuditInstancesByUserId(userId);
    }

    public List<AuditInstance> getApprovedAuditInstancesByUserId(Long userId) {
        return workflowQueryRepository.findApprovedAuditInstancesByUserId(userId);
    }

    public List<AuditInstance> getAppliedAuditInstancesByUserId(Long userId) {
        return workflowQueryRepository.findAppliedAuditInstancesByUserId(userId);
    }

    public List<AuditInstance> getAuditInstanceHistory(Long instanceId) {
        return workflowQueryRepository.findAuditInstanceHistory(instanceId);
    }

    public Map<String, Object> getApprovalStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalInstances", auditInstanceRepository.countAll());
        stats.put("pendingCount", auditInstanceRepository.countPending());
        stats.put("approvedCount", auditInstanceRepository.countApproved());
        stats.put("rejectedCount", auditInstanceRepository.countRejected());
        return stats;
    }
}
