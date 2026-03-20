package com.sism.workflow.application.support;

import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.definition.model.AuditStepDef;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.AuditStepInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class StepInstanceFactory {

    private final ApproverResolver approverResolver;
    private final SubmissionStepAutoCompletePolicy submissionStepAutoCompletePolicy;

    public void initialize(AuditInstance instance,
                           AuditFlowDef flowDef,
                           Long requesterId,
                           Long requesterOrgId,
                           Map<Long, Long> selectedApprovers) {
        if (instance.getStepInstances() != null && !instance.getStepInstances().isEmpty()) {
            return;
        }

        List<AuditStepDef> stepDefs = flowDef != null && flowDef.getSteps() != null ? flowDef.getSteps() : List.of();
        if (stepDefs.isEmpty()) {
            AuditStepInstance fallback = new AuditStepInstance();
            fallback.setStepIndex(1);
            fallback.setStepName("默认审批");
            fallback.setStatus(AuditInstance.STEP_STATUS_PENDING);
            fallback.setApproverId(requesterId);
            instance.addStepInstance(fallback);
            return;
        }

        List<AuditStepDef> orderedStepDefs = stepDefs.stream()
                .sorted(Comparator
                        .comparing((AuditStepDef step) -> step.getStepOrder() == null ? Integer.MAX_VALUE : step.getStepOrder())
                        .thenComparing(step -> step.getId() == null ? Long.MAX_VALUE : step.getId()))
                .toList();

        int order = 1;
        for (AuditStepDef stepDef : orderedStepDefs) {
            validateStepDefinition(stepDef);
            AuditStepInstance step = new AuditStepInstance();
            Integer stepOrder = stepDef.getStepOrder();
            int index = stepOrder != null && stepOrder > 0 ? stepOrder : order;
            step.setStepIndex(index);
            step.setStepDefId(stepDef.getId());
            step.setStepName(stepDef.getStepName() != null ? stepDef.getStepName() : "审批步骤" + index);
            Long resolvedApproverId;
            if (stepDef.isSubmitStep()) {
                resolvedApproverId = requesterId;
            } else if (selectedApprovers != null && stepDef.getId() != null && selectedApprovers.containsKey(stepDef.getId())) {
                resolvedApproverId = selectedApprovers.get(stepDef.getId());
                approverResolver.validateSelectedApprover(stepDef, resolvedApproverId);
            } else {
                resolvedApproverId = approverResolver.resolveApproverId(stepDef, requesterId, requesterOrgId);
            }
            step.setApproverId(resolvedApproverId);
            step.setStatus(order == 1 ? AuditInstance.STEP_STATUS_PENDING : AuditInstance.STEP_STATUS_WAITING);
            instance.addStepInstance(step);
            order++;
        }

        submissionStepAutoCompletePolicy.apply(instance, orderedStepDefs.get(0), requesterId);
    }

    private void validateStepDefinition(AuditStepDef stepDef) {
        if (!stepDef.hasExplicitStepType()) {
            log.warn("Workflow step is missing explicit step_type, using compatibility fallback: stepName={}, inferredType={}",
                    stepDef.getStepName(), stepDef.resolveEffectiveStepType());
        }
        if (stepDef.isApprovalStep() && (stepDef.getRoleId() == null || stepDef.getRoleId() <= 0)) {
            throw new IllegalStateException("Workflow approval step is missing role assignment: " + stepDef.getStepName());
        }
    }
}
