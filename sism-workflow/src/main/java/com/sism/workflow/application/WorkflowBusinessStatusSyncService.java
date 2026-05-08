package com.sism.workflow.application;

import com.sism.shared.domain.workflow.WorkflowBusinessStatusPort;
import com.sism.workflow.domain.runtime.AuditInstance;
import com.sism.workflow.domain.runtime.AuditStepInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowBusinessStatusSyncService {

    private final List<WorkflowBusinessStatusPort> statusPorts;

    public void syncAfterWorkflowChanged(AuditInstance instance) {
        if (instance == null || instance.getEntityId() == null) {
            return;
        }
        String comment = latestRejectedComment(instance);
        Long workflowInstanceId = hasReturnedSubmitStep(instance) ? instance.getId() : null;
        for (WorkflowBusinessStatusPort statusPort : statusPorts) {
            statusPort.syncBusinessStatus(
                    instance.getEntityType(),
                    instance.getEntityId(),
                    normalizeStatus(instance),
                    instance.getRequesterId(),
                    comment,
                    workflowInstanceId
            );
        }
    }

    private String normalizeStatus(AuditInstance instance) {
        if (AuditInstance.STATUS_PENDING.equals(instance.getStatus()) && hasReturnedSubmitStep(instance)) {
            return "RETURNED";
        }
        return instance.getStatus();
    }

    private String latestRejectedComment(AuditInstance instance) {
        return instance.getStepInstances().stream()
                .filter(step -> AuditInstance.STEP_STATUS_REJECTED.equals(step.getStatus()))
                .reduce((first, second) -> second)
                .map(step -> step.getComment() == null || step.getComment().isBlank() ? "审批驳回" : step.getComment())
                .orElse(null);
    }

    private boolean hasReturnedSubmitStep(AuditInstance instance) {
        if (instance == null || instance.getStepInstances() == null || instance.getStepInstances().isEmpty()) {
            return false;
        }
        return instance.getStepInstances().stream()
                .filter(step -> AuditInstance.STEP_STATUS_WITHDRAWN.equals(step.getStatus()))
                .max(Comparator
                        .comparing((AuditStepInstance step) -> step.getStepNo() == null ? Integer.MIN_VALUE : step.getStepNo())
                        .thenComparing(step -> step.getCreatedAt() == null ? java.time.LocalDateTime.MIN : step.getCreatedAt()))
                .filter(step -> {
                    if (instance.getRequesterId() != null && instance.getRequesterId().equals(step.getApproverId())) {
                        return true;
                    }
                    String stepName = step.getStepName();
                    return stepName != null && stepName.contains("提交");
                })
                .isPresent();
    }
}
