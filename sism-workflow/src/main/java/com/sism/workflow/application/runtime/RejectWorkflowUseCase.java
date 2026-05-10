package com.sism.workflow.application.runtime;

import com.sism.workflow.application.definition.WorkflowDefinitionQueryService;
import com.sism.workflow.application.support.ApproverResolver;
import com.sism.workflow.application.support.WorkflowEventDispatcher;
import com.sism.workflow.application.WorkflowBusinessStatusSyncService;
import com.sism.workflow.domain.definition.AuditFlowDef;
import com.sism.workflow.domain.definition.AuditStepDef;
import com.sism.workflow.domain.runtime.AuditInstance;
import com.sism.workflow.domain.runtime.AuditStepInstance;
import com.sism.workflow.domain.runtime.AuditInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RejectWorkflowUseCase {

    private final AuditInstanceRepository auditInstanceRepository;
    private final WorkflowEventDispatcher workflowEventDispatcher;
    private final WorkflowBusinessStatusSyncService workflowBusinessStatusSyncService;
    private final WorkflowDefinitionQueryService workflowDefinitionQueryService;
    private final ApproverResolver approverResolver;
    @Transactional
    public AuditInstance reject(AuditInstance instance, Long userId, String comment) {
        log.info("Rejecting workflow instance {}, before reject step count: {}", instance.getId(), instance.getStepInstances().size());
        instance.reject(userId, comment);
        createReturnedStep(instance);
        log.info("Rejected workflow instance {}, after return step creation count: {}, current pending step: {}",
                instance.getId(),
                instance.getStepInstances().size(),
                instance.resolveCurrentPendingStep().map(AuditStepInstance::getId).orElse(null));
        AuditInstance saved = auditInstanceRepository.save(instance);
        log.info("Saved rejected workflow instance {}, persisted step count: {}, current pending step: {}",
                saved.getId(),
                saved.getStepInstances().size(),
                saved.resolveCurrentPendingStep().map(AuditStepInstance::getId).orElse(null));
        workflowBusinessStatusSyncService.syncAfterWorkflowChanged(saved);
        workflowEventDispatcher.publish(saved);
        return saved;
    }

    private void createReturnedStep(AuditInstance instance) {
        AuditStepInstance rejectedStep = instance.getStepInstances().stream()
                .filter(step -> AuditInstance.STEP_STATUS_REJECTED.equals(step.getStatus()))
                .max(Comparator.comparing(step -> step.getStepNo() == null ? 0 : step.getStepNo()))
                .orElse(null);
        if (rejectedStep == null) {
            log.warn("Reject workflow instance {} could not find rejected step", instance.getId());
            instance.setStatus(AuditInstance.STATUS_REJECTED);
            return;
        }

        AuditFlowDef flowDef = workflowDefinitionQueryService.getAuditFlowDefById(instance.getFlowDefId());
        if (flowDef == null || flowDef.getSteps() == null || flowDef.getSteps().isEmpty()) {
            log.warn("Reject workflow instance {} has no flow definition steps", instance.getId());
            instance.setStatus(AuditInstance.STATUS_REJECTED);
            instance.setCompletedAt(java.time.LocalDateTime.now());
            return;
        }

        List<AuditStepDef> orderedSteps = flowDef.getSteps().stream()
                .sorted(Comparator.comparing(step -> step.getStepOrder() == null ? Integer.MAX_VALUE : step.getStepOrder()))
                .toList();

        int rejectedIndex = findStepIndexByDefinitionId(orderedSteps, rejectedStep.getStepDefId());
        AuditStepDef rejectedStepDef = rejectedIndex >= 0 ? orderedSteps.get(rejectedIndex) : null;
        if (rejectedIndex <= 0) {
            if (isTerminalStep(rejectedStepDef)) {
                log.info("Reject workflow instance {} stopped at terminal step, marking as rejected", instance.getId());
                instance.setStatus(AuditInstance.STATUS_REJECTED);
                instance.setCompletedAt(java.time.LocalDateTime.now());
                return;
            }

            log.info("Reject workflow instance {} has no earlier step but current step is not terminal, keeping in review", instance.getId());
            instance.setStatus(AuditInstance.STATUS_PENDING);
            instance.setCompletedAt(null);
            return;
        }

        AuditStepDef returnedStepDef = orderedSteps.get(rejectedIndex - 1);
        AuditStepInstance returnedStep = new AuditStepInstance();
        returnedStep.setStepNo(instance.nextStepInstanceNo());
        returnedStep.setStepDefId(returnedStepDef.getId());
        returnedStep.setStepName(returnedStepDef.getStepName());

        Long contextOrgId = resolveContextOrgId(instance, returnedStepDef);
        Long approverId = approverResolver.resolveAssignedApproverId(
                returnedStepDef,
                instance.getRequesterId(),
                contextOrgId,
                instance
        );
        returnedStep.setApproverId(approverId);
        returnedStep.setApproverOrgId(returnedStepDef.isSubmitStep()
                ? instance.getRequesterOrgId()
                : approverResolver.resolveApproverOrgId(returnedStepDef, contextOrgId, instance));
        returnedStep.setStatus(returnedStepDef.isSubmitStep()
                ? AuditInstance.STEP_STATUS_WITHDRAWN
                : AuditInstance.STEP_STATUS_PENDING);
        instance.addStepInstance(returnedStep);

        if (returnedStepDef.isSubmitStep()) {
            returnedStep.setComment("驳回后退回填报人重新提交");
            returnedStep.setApprovedAt(null);
        }

        log.info("Reject workflow instance {} appended returned step defId={}, stepNo={}, approverId={}",
                instance.getId(),
                returnedStep.getStepDefId(),
                returnedStep.getStepNo(),
                returnedStep.getApproverId());
        if (returnedStepDef.isSubmitStep()) {
            instance.setStatus(AuditInstance.STATUS_WITHDRAWN);
        } else {
            instance.setStatus(AuditInstance.STATUS_PENDING);
        }
        instance.setCompletedAt(null);
    }

    private Long resolveContextOrgId(AuditInstance instance, AuditStepDef targetStepDef) {
        if (targetStepDef != null && targetStepDef.isSubmitStep()) {
            return instance.getRequesterOrgId();
        }

        return instance.getStepInstances().stream()
                .filter(step -> AuditInstance.STEP_STATUS_APPROVED.equals(step.getStatus()))
                .max(Comparator.comparing(step -> step.getStepNo() == null ? 0 : step.getStepNo()))
                .map(AuditStepInstance::getApproverOrgId)
                .orElse(instance.getRequesterOrgId());
    }

    private int findStepIndexByDefinitionId(List<AuditStepDef> orderedSteps, Long stepDefId) {
        if (stepDefId == null) {
            return -1;
        }
        for (int i = 0; i < orderedSteps.size(); i++) {
            AuditStepDef step = orderedSteps.get(i);
            if (step.getId() != null && step.getId().equals(stepDefId)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isTerminalStep(AuditStepDef stepDef) {
        return stepDef != null && Boolean.TRUE.equals(stepDef.getIsTerminal());
    }

}
