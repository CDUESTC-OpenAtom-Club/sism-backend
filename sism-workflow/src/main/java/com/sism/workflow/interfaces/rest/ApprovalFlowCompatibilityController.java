package com.sism.workflow.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.workflow.application.definition.WorkflowDefinitionQueryService;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.definition.model.AuditStepDef;
import com.sism.workflow.interfaces.dto.ApprovalFlowStepResponse;
import com.sism.workflow.interfaces.dto.ApprovalFlowTemplateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/approval/flows")
@RequiredArgsConstructor
public class ApprovalFlowCompatibilityController {

    private final WorkflowDefinitionQueryService workflowDefinitionQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ApprovalFlowTemplateResponse>>> listFlows() {
        List<ApprovalFlowTemplateResponse> response = workflowDefinitionQueryService.getAllAuditFlowDefs().stream()
                .map(this::toTemplateResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApprovalFlowTemplateResponse>> getFlowById(@PathVariable Long id) {
        AuditFlowDef flowDef = workflowDefinitionQueryService.getAuditFlowDefById(id);
        if (flowDef == null) {
            throw new IllegalArgumentException("Workflow definition not found: " + id);
        }
        return ResponseEntity.ok(ApiResponse.success(toTemplateResponse(flowDef)));
    }

    @GetMapping("/code/{flowCode}")
    public ResponseEntity<ApiResponse<ApprovalFlowTemplateResponse>> getFlowByCode(@PathVariable String flowCode) {
        AuditFlowDef flowDef = workflowDefinitionQueryService.getAuditFlowDefByCode(flowCode);
        if (flowDef == null) {
            throw new IllegalArgumentException("Workflow definition not found: " + flowCode);
        }
        return ResponseEntity.ok(ApiResponse.success(toTemplateResponse(flowDef)));
    }

    @GetMapping("/entity-type/{entityType}")
    public ResponseEntity<ApiResponse<List<ApprovalFlowTemplateResponse>>> getFlowsByEntityType(
            @PathVariable String entityType) {
        List<ApprovalFlowTemplateResponse> response = workflowDefinitionQueryService
                .getAuditFlowDefsByEntityType(entityType).stream()
                .map(this::toTemplateResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private ApprovalFlowTemplateResponse toTemplateResponse(AuditFlowDef flowDef) {
        List<ApprovalFlowStepResponse> steps = flowDef.getSteps().stream()
                .sorted(Comparator
                        .comparing((AuditStepDef step) -> step.getStepOrder() == null ? Integer.MAX_VALUE : step.getStepOrder())
                        .thenComparing(step -> step.getId() == null ? Long.MAX_VALUE : step.getId()))
                .map(step -> ApprovalFlowStepResponse.builder()
                        .id(step.getId())
                        .stepName(step.getStepName())
                        .stepOrder(step.getStepOrder())
                        .stepType(step.resolveEffectiveStepType())
                        .approverType(step.isSubmitStep() ? "SUBMIT" : "ROLE")
                        .approverId(step.getRoleId())
                        .timeoutHours(null)
                        .isRequired(Boolean.TRUE.equals(step.getIsRequired()))
                        .canSkip(false)
                        .build())
                .toList();

        return ApprovalFlowTemplateResponse.builder()
                .id(flowDef.getId())
                .flowCode(flowDef.getFlowCode())
                .flowName(flowDef.getFlowName())
                .description(flowDef.getDescription())
                .entityType(flowDef.getEntityType())
                .isActive(Boolean.TRUE.equals(flowDef.getIsActive()))
                .version(flowDef.getVersion())
                .steps(steps)
                .stepCount(steps.size())
                .createdAt(flowDef.getCreatedAt())
                .updatedAt(flowDef.getUpdatedAt())
                .build();
    }
}
