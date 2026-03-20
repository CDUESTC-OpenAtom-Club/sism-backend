package com.sism.workflow.application.definition;

import com.sism.workflow.application.support.ApproverResolver;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.definition.model.AuditStepDef;
import com.sism.workflow.interfaces.dto.WorkflowDefinitionPreviewResponse;
import com.sism.workflow.interfaces.dto.WorkflowStepPreviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowPreviewQueryService {

    private final WorkflowDefinitionQueryService workflowDefinitionQueryService;
    private final ApproverResolver approverResolver;

    public WorkflowDefinitionPreviewResponse getPreviewByCode(String workflowCode, Long requesterOrgId) {
        AuditFlowDef flowDef = workflowDefinitionQueryService.getAuditFlowDefByCode(workflowCode);
        if (flowDef == null) {
            throw new IllegalArgumentException("Workflow definition not found: " + workflowCode);
        }

        List<WorkflowStepPreviewResponse> steps = flowDef.getSteps().stream()
                .sorted(Comparator.comparing(step -> step.getStepOrder() == null ? Integer.MAX_VALUE : step.getStepOrder()))
                .map(step -> toStepPreview(step, requesterOrgId))
                .toList();

        return WorkflowDefinitionPreviewResponse.builder()
                .workflowCode(flowDef.getFlowCode())
                .workflowName(flowDef.getFlowName())
                .entityType(flowDef.getEntityType())
                .steps(steps)
                .build();
    }

    private WorkflowStepPreviewResponse toStepPreview(AuditStepDef step, Long requesterOrgId) {
        boolean selectable = step.isApprovalStep();
        return WorkflowStepPreviewResponse.builder()
                .stepDefId(step.getId())
                .stepOrder(step.getStepOrder())
                .stepName(step.getStepName())
                .stepType(step.resolveEffectiveStepType())
                .roleId(step.getRoleId())
                .selectable(selectable)
                .candidateApprovers(selectable
                        ? approverResolver.resolveCandidates(step, requesterOrgId)
                        : List.of())
                .build();
    }
}
