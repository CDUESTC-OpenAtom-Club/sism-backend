package com.sism.strategy.infrastructure;

import com.sism.shared.domain.workflow.WorkflowBusinessStatusPort;
import com.sism.strategy.application.PlanApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StrategyWorkflowBusinessStatusAdapter implements WorkflowBusinessStatusPort {

    private static final String PLAN_ENTITY_TYPE = "PLAN";

    private final PlanApplicationService planApplicationService;

    @Override
    public void syncBusinessStatus(String entityType, Long entityId, String status, Long operatorId, String comment, Long workflowInstanceId) {
        if (entityId == null || !PLAN_ENTITY_TYPE.equalsIgnoreCase(entityType) || status == null) {
            return;
        }

        if ("APPROVED".equalsIgnoreCase(status)) {
            planApplicationService.markWorkflowApproved(entityId);
            return;
        }
        if ("WITHDRAWN".equalsIgnoreCase(status)) {
            planApplicationService.markWorkflowWithdrawn(entityId);
            return;
        }
        if ("PENDING".equalsIgnoreCase(status)) {
            if (workflowInstanceId != null) {
                planApplicationService.markWorkflowPending(entityId);
            } else {
                planApplicationService.markWorkflowPending(entityId);
            }
            return;
        }
        if ("RETURNED".equalsIgnoreCase(status)) {
            planApplicationService.markWorkflowWithdrawn(entityId);
            return;
        }
        if ("REJECTED".equalsIgnoreCase(status)) {
            planApplicationService.markWorkflowRejected(
                    entityId,
                    comment == null || comment.isBlank() ? "Rejected" : comment
            );
        }
    }
}
