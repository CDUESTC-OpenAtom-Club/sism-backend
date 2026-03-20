package com.sism.workflow.application;

import com.sism.strategy.application.PlanApplicationService;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PlanWorkflowSyncService {

    private static final String PLAN_ENTITY_TYPE = "PLAN";

    private final ObjectProvider<PlanApplicationService> planApplicationServiceProvider;

    public PlanWorkflowSyncService(ObjectProvider<PlanApplicationService> planApplicationServiceProvider) {
        this.planApplicationServiceProvider = planApplicationServiceProvider;
    }

    public void syncAfterWorkflowChanged(AuditInstance instance) {
        if (instance == null
                || instance.getEntityId() == null
                || !PLAN_ENTITY_TYPE.equalsIgnoreCase(instance.getEntityType())) {
            return;
        }

        withPlanService(planApplicationService -> {
            if (AuditInstance.STATUS_APPROVED.equals(instance.getStatus())) {
                planApplicationService.markWorkflowApproved(instance.getEntityId());
                return;
            }

            if (AuditInstance.STATUS_WITHDRAWN.equals(instance.getStatus())) {
                planApplicationService.markWorkflowWithdrawn(instance.getEntityId());
                return;
            }

            instance.getStepInstances().stream()
                    .filter(step -> AuditInstance.STEP_STATUS_REJECTED.equals(step.getStatus()))
                    .reduce((first, second) -> second)
                    .ifPresent(step -> planApplicationService.markWorkflowRejected(
                            instance.getEntityId(),
                            step.getComment() == null || step.getComment().isBlank() ? "Rejected" : step.getComment()));
        });
    }

    private void withPlanService(java.util.function.Consumer<PlanApplicationService> action) {
        PlanApplicationService planApplicationService = planApplicationServiceProvider.getIfAvailable();
        if (planApplicationService == null) {
            log.debug("PlanApplicationService not available, skipping plan workflow sync");
            return;
        }
        action.accept(planApplicationService);
    }
}
