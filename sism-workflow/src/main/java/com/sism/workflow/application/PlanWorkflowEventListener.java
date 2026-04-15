package com.sism.workflow.application;

import com.sism.strategy.domain.event.PlanSubmittedForApprovalEvent;
import com.sism.workflow.interfaces.dto.StartWorkflowRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class PlanWorkflowEventListener {

    private static final String PLAN_ENTITY_TYPE = "PLAN";
    private static final int MAX_START_ATTEMPTS = 3;

    private final BusinessWorkflowApplicationService businessWorkflowApplicationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePlanSubmittedForApproval(PlanSubmittedForApprovalEvent event) {
        if (event == null) {
            return;
        }

        try {
            StartWorkflowRequest request = new StartWorkflowRequest();
            request.setWorkflowCode(event.getWorkflowCode());
            request.setBusinessEntityId(event.getPlanId());
            request.setBusinessEntityType(PLAN_ENTITY_TYPE);

            var response = startWorkflowWithRetry(request, event.getSubmitterId(), event.getSubmitterOrgId());
            log.info("Started plan workflow for planId={}, instanceId={}", event.getPlanId(), response.getInstanceId());
        } catch (Exception ex) {
            log.error("Failed to start plan workflow for planId={}, workflowCode={}: {}",
                    event.getPlanId(), event.getWorkflowCode(), ex.getMessage(), ex);
            throw new IllegalStateException(
                    "Failed to start plan workflow for planId=" + event.getPlanId() + ", workflowCode=" + event.getWorkflowCode(),
                    ex
            );
        }
    }

    private com.sism.workflow.interfaces.dto.WorkflowInstanceResponse startWorkflowWithRetry(
            StartWorkflowRequest request,
            Long submitterId,
            Long submitterOrgId
    ) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_START_ATTEMPTS; attempt++) {
            try {
                return businessWorkflowApplicationService.startWorkflow(request, submitterId, submitterOrgId);
            } catch (IllegalStateException e) {
                throw e;
            } catch (RuntimeException e) {
                lastFailure = e;
                if (attempt == MAX_START_ATTEMPTS) {
                    break;
                }
                log.warn("Retrying plan workflow start, attempt={}/{}, workflowCode={}, entityId={}, reason={}",
                        attempt + 1, MAX_START_ATTEMPTS, request.getWorkflowCode(), request.getBusinessEntityId(), e.getMessage());
            }
        }
        throw lastFailure == null ? new IllegalStateException("Unknown plan workflow start failure") : lastFailure;
    }
}
