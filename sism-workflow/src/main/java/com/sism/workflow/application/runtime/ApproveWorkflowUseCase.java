package com.sism.workflow.application.runtime;

import com.sism.workflow.application.support.ApproverResolver;
import com.sism.workflow.application.support.WorkflowEventDispatcher;
import com.sism.workflow.application.PlanWorkflowSyncService;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.definition.model.AuditStepDef;
import com.sism.workflow.domain.definition.repository.FlowDefinitionRepository;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.AuditStepInstance;
import com.sism.workflow.domain.runtime.repository.AuditInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApproveWorkflowUseCase {

    private final AuditInstanceRepository auditInstanceRepository;
    private final WorkflowEventDispatcher workflowEventDispatcher;
    private final PlanWorkflowSyncService planWorkflowSyncService;
    private final FlowDefinitionRepository flowDefinitionRepository;
    private final ApproverResolver approverResolver;

    @Transactional
    public AuditInstance approve(AuditInstance instance, Long userId, String comment) {
        instance.approve(userId, comment);

        // 如果还在审批中，尝试创建下一个步骤
        if (AuditInstance.STATUS_PENDING.equals(instance.getStatus())) {
            boolean hasNextStep = createNextStep(instance);
            // 如果没有下一个步骤，标记为已完成
            if (!hasNextStep) {
                instance.setStatus(AuditInstance.STATUS_APPROVED);
                instance.setCompletedAt(java.time.LocalDateTime.now());
            }
        }

        AuditInstance saved = auditInstanceRepository.save(instance);
        planWorkflowSyncService.syncAfterWorkflowChanged(saved);
        workflowEventDispatcher.publish(saved);
        return saved;
    }

    private boolean createNextStep(AuditInstance instance) {
        AuditFlowDef flowDef = flowDefinitionRepository.findById(instance.getFlowDefId()).orElse(null);
        if (flowDef == null || flowDef.getSteps() == null || flowDef.getSteps().isEmpty()) {
            return false;
        }

        List<AuditStepDef> orderedSteps = flowDef.getSteps().stream()
                .sorted(Comparator.comparing(step -> step.getStepOrder() == null ? Integer.MAX_VALUE : step.getStepOrder()))
                .toList();

        int currentStepCount = instance.getStepInstances().size();
        if (currentStepCount >= orderedSteps.size()) {
            return false;
        }

        AuditStepDef nextStepDef = orderedSteps.get(currentStepCount);
        AuditStepInstance nextStep = new AuditStepInstance();
        nextStep.setStepNo(currentStepCount + 1);
        nextStep.setStepDefId(nextStepDef.getId());
        nextStep.setStepName(nextStepDef.getStepName());

        // 使用当前审批人的组织ID来解析下一步审批人
        Long contextOrgId = getCurrentApproverOrgId(instance);
        Long approverId = approverResolver.resolveApproverId(nextStepDef, instance.getRequesterId(), contextOrgId);
        nextStep.setApproverId(approverId);
        nextStep.setApproverOrgId(approverResolver.resolveApproverOrgId(approverId));
        nextStep.setStatus(AuditInstance.STEP_STATUS_PENDING);
        instance.addStepInstance(nextStep);
        return true;
    }

    private Long getCurrentApproverOrgId(AuditInstance instance) {
        if (instance.getStepInstances() == null || instance.getStepInstances().isEmpty()) {
            return instance.getRequesterOrgId();
        }

        // 获取最后一个已审批步骤的审批人组织
        return instance.getStepInstances().stream()
                .filter(step -> AuditInstance.STEP_STATUS_APPROVED.equals(step.getStatus()))
                .max(Comparator.comparing(step -> step.getStepNo() == null ? 0 : step.getStepNo()))
                .map(step -> step.getApproverOrgId())
                .orElse(instance.getRequesterOrgId());
    }
}
