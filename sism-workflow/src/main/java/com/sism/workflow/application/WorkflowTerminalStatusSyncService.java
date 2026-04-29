package com.sism.workflow.application;

import com.sism.workflow.domain.runtime.AuditInstance;
import com.sism.workflow.domain.runtime.AuditInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowTerminalStatusSyncService {

    private static final String PLAN_REPORT_ENTITY_TYPE = "PLAN_REPORT";
    private static final String LEGACY_PLAN_REPORT_ENTITY_TYPE = "PlanReport";

    private final AuditInstanceRepository auditInstanceRepository;

    @Transactional
    public int syncReportWorkflowTerminalStatus(Long reportId, String terminalStatus, Long operatorId, String comment) {
        List<AuditInstance> instances = auditInstanceRepository.findByBusinessTypeAndBusinessId(PLAN_REPORT_ENTITY_TYPE, reportId);
        if (instances.isEmpty()) {
            instances = auditInstanceRepository.findByBusinessTypeAndBusinessId(LEGACY_PLAN_REPORT_ENTITY_TYPE, reportId);
        }
        if (instances.isEmpty()) {
            return 0;
        }

        instances.stream()
                .filter(instance -> AuditInstance.STATUS_PENDING.equals(instance.getStatus()))
                .sorted(Comparator.comparing(AuditInstance::getStartedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(instance -> {
                    instance.completeExternally(terminalStatus, operatorId, comment);
                    auditInstanceRepository.save(instance);
                });
        return instances.size();
    }
}
