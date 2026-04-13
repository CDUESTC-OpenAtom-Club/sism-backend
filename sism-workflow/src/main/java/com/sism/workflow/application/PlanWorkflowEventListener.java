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

            var response = businessWorkflowApplicationService.startWorkflow(
                    request,
                    event.getSubmitterId(),
                    event.getSubmitterOrgId()
            );
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
}
