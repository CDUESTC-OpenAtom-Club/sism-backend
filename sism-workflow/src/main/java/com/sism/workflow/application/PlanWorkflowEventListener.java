package com.sism.workflow.application;

import com.sism.strategy.domain.event.PlanSubmittedForApprovalEvent;
import com.sism.workflow.interfaces.dto.SelectedApproverRequest;
import com.sism.workflow.interfaces.dto.StartWorkflowRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePlanSubmittedForApproval(PlanSubmittedForApprovalEvent event) {
        if (event == null) {
            return;
        }

        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setWorkflowCode(event.getWorkflowCode());
        request.setBusinessEntityId(event.getPlanId());
        request.setBusinessEntityType(PLAN_ENTITY_TYPE);
        request.setSelectedApprovers(event.getSelectedApprovers().stream()
                .map(item -> {
                    SelectedApproverRequest selected = new SelectedApproverRequest();
                    selected.setStepDefId(item.getStepDefId());
                    selected.setApproverId(item.getApproverId());
                    return selected;
                })
                .toList());

        var response = businessWorkflowApplicationService.startWorkflow(
                request,
                event.getSubmitterId(),
                event.getSubmitterOrgId()
        );
        log.info("Started plan workflow for planId={}, instanceId={}", event.getPlanId(), response.getInstanceId());
    }
}
