package com.sism.execution.infrastructure.persistence;

import com.sism.execution.application.ReportApplicationService;
import com.sism.shared.domain.workflow.WorkflowBusinessStatusPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExecutionWorkflowBusinessStatusAdapter implements WorkflowBusinessStatusPort {

    private static final String PLAN_REPORT_ENTITY_TYPE = "PLAN_REPORT";
    private static final String LEGACY_PLAN_REPORT_ENTITY_TYPE = "PlanReport";

    private final ReportApplicationService reportApplicationService;

    @Override
    public void syncBusinessStatus(String entityType, Long entityId, String status, Long operatorId, String comment, Long workflowInstanceId) {
        if (entityId == null || status == null || entityType == null) {
            return;
        }
        if (!PLAN_REPORT_ENTITY_TYPE.equalsIgnoreCase(entityType)
                && !LEGACY_PLAN_REPORT_ENTITY_TYPE.equalsIgnoreCase(entityType)) {
            return;
        }

        if ("APPROVED".equalsIgnoreCase(status)) {
            reportApplicationService.markWorkflowApproved(entityId, operatorId);
            return;
        }
        if ("WITHDRAWN".equalsIgnoreCase(status)) {
            reportApplicationService.markWorkflowWithdrawn(entityId);
            return;
        }
        if ("RETURNED".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status)) {
            if (workflowInstanceId != null) {
                reportApplicationService.markWorkflowReturnedForResubmission(entityId, workflowInstanceId);
            } else {
                reportApplicationService.markWorkflowWithdrawn(entityId);
            }
            return;
        }
        if ("REJECTED".equalsIgnoreCase(status)) {
            reportApplicationService.markWorkflowRejected(
                    entityId,
                    operatorId,
                    comment == null || comment.isBlank() ? "审批驳回" : comment
            );
        }
    }
}
