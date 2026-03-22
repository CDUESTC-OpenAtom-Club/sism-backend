package com.sism.workflow.application.runtime;

import com.sism.workflow.application.definition.WorkflowDefinitionQueryService;
import com.sism.workflow.application.support.ApproverResolver;
import com.sism.workflow.application.support.WorkflowEventDispatcher;
import com.sism.workflow.application.PlanWorkflowSyncService;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.definition.model.AuditStepDef;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.AuditStepInstance;
import com.sism.workflow.domain.runtime.repository.AuditInstanceRepository;
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
    private final PlanWorkflowSyncService planWorkflowSyncService;
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
        planWorkflowSyncService.syncAfterWorkflowChanged(saved);
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
        if (rejectedIndex <= 0) {
            log.info("Reject workflow instance {} reached first step, marking as terminal rejected", instance.getId());
            instance.setStatus(AuditInstance.STATUS_REJECTED);
            instance.setCompletedAt(java.time.LocalDateTime.now());
            return;
        }

        AuditStepDef returnedStepDef = orderedSteps.get(rejectedIndex - 1);
        AuditStepInstance returnedStep = new AuditStepInstance();
        returnedStep.setStepNo(instance.nextStepInstanceNo());
        returnedStep.setStepDefId(returnedStepDef.getId());
        returnedStep.setStepName(returnedStepDef.getStepName());

        Long contextOrgId = resolveContextOrgId(instance, returnedStepDef);
        Long approverId = returnedStepDef.isSubmitStep()
                ? instance.getRequesterId()
                : approverResolver.resolveApproverId(returnedStepDef, instance.getRequesterId(), contextOrgId);
        returnedStep.setApproverId(approverId);
        returnedStep.setApproverOrgId(returnedStepDef.isSubmitStep()
                ? instance.getRequesterOrgId()
                : approverResolver.resolveApproverOrgId(approverId));
        returnedStep.setStatus(AuditInstance.STEP_STATUS_PENDING);
        instance.addStepInstance(returnedStep);
        log.info("Reject workflow instance {} appended returned step defId={}, stepNo={}, approverId={}",
                instance.getId(),
                returnedStep.getStepDefId(),
                returnedStep.getStepNo(),
                returnedStep.getApproverId());
        instance.setStatus(AuditInstance.STATUS_PENDING);
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
}
