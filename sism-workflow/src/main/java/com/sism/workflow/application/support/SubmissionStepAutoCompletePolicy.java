package com.sism.workflow.application.support;

import com.sism.workflow.domain.definition.model.AuditStepDef;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.AuditStepInstance;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;

@Component
public class SubmissionStepAutoCompletePolicy {

    public void apply(AuditInstance instance, AuditStepDef firstStepDef, Long requesterId) {
        if (instance.getStepInstances() == null || instance.getStepInstances().isEmpty()) {
            return;
        }

        AuditStepInstance firstStep = instance.getStepInstances().stream()
                .sorted(Comparator.comparing(step -> step.getStepNo() == null ? Integer.MAX_VALUE : step.getStepNo()))
                .findFirst()
                .orElse(null);

        if (firstStep == null) {
            return;
        }

        if (!shouldAutoCompleteSubmissionStep(firstStep, firstStepDef, requesterId)) {
            return;
        }

        firstStep.setStatus(AuditInstance.STEP_STATUS_APPROVED);
        firstStep.setApprovedAt(LocalDateTime.now());
        firstStep.setComment("系统自动完成提交流程节点");

        AuditStepInstance nextStep = instance.getStepInstances().stream()
                .filter(step -> !step.equals(firstStep))
                .filter(step -> AuditInstance.STEP_STATUS_WAITING.equals(step.getStatus()))
                .sorted(Comparator.comparing(step -> step.getStepNo() == null ? Integer.MAX_VALUE : step.getStepNo()))
                .findFirst()
                .orElse(null);

        if (nextStep != null) {
            nextStep.setStatus(AuditInstance.STEP_STATUS_PENDING);
        }
    }

    public boolean shouldAutoCompleteSubmissionStep(AuditStepInstance firstStep, AuditStepDef firstStepDef, Long requesterId) {
        if (!AuditInstance.STEP_STATUS_PENDING.equals(firstStep.getStatus())) {
            return false;
        }

        if (firstStepDef == null || !firstStepDef.isSubmitStep()) {
            return false;
        }

        return requesterId != null && requesterId.equals(firstStep.getApproverId());
    }
}
