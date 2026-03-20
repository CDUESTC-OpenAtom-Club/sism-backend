package com.sism.workflow.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.iam.application.dto.CurrentUser;
import com.sism.workflow.application.BusinessWorkflowApplicationService;
import com.sism.workflow.interfaces.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * BusinessWorkflowController - 业务工作流控制器
 * 管理固定模板驱动的线性审批流程。
 *
 * API 前缀: /api/v1/workflows
 */
@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@Tag(name = "Business Workflows", description = "Business workflow management endpoints")
public class BusinessWorkflowController {

    private final BusinessWorkflowApplicationService workflowService;

    // 1. 启动工作流

    @PostMapping("/start")
    @Operation(summary = "Start a fixed-template workflow and bind approvers to approval nodes")
    public ResponseEntity<ApiResponse<WorkflowInstanceResponse>> startWorkflow(
            @Valid @RequestBody StartWorkflowRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        WorkflowInstanceResponse response = workflowService.startWorkflow(
                request, currentUser.getId(), currentUser.getOrgId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{definitionId}/instances")
    @Operation(summary = "Start a workflow instance by definition ID")
    public ResponseEntity<ApiResponse<WorkflowInstanceResponse>> startWorkflowInstance(
            @PathVariable String definitionId,
            @Valid @RequestBody StartInstanceRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        WorkflowInstanceResponse response = workflowService.startWorkflowInstance(
                definitionId, request, currentUser.getId(), currentUser.getOrgId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 2. 查询工作流

    @GetMapping("/definitions")
    @Operation(summary = "List workflow definitions with pagination")
    public ResponseEntity<ApiResponse<PageResult<WorkflowDefinitionResponse>>> listDefinitions(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PageResult<WorkflowDefinitionResponse> result = workflowService.listDefinitions(pageNum, pageSize);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{definitionId}/instances")
    @Operation(summary = "List workflow instances for a definition")
    public ResponseEntity<ApiResponse<PageResult<WorkflowInstanceResponse>>> listInstances(
            @PathVariable String definitionId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PageResult<WorkflowInstanceResponse> result = workflowService.listInstances(definitionId, pageNum, pageSize);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/instances/{instanceId}")
    @Operation(summary = "Get workflow instance detail")
    public ResponseEntity<ApiResponse<WorkflowInstanceDetailResponse>> getInstanceDetail(
            @PathVariable String instanceId
    ) {
        WorkflowInstanceDetailResponse response = workflowService.getInstanceDetail(instanceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/instances/entity/{entityType}/{entityId}")
    @Operation(summary = "Get latest workflow instance detail by business entity")
    public ResponseEntity<ApiResponse<WorkflowInstanceDetailResponse>> getInstanceDetailByBusiness(
            @PathVariable String entityType,
            @PathVariable Long entityId
    ) {
        WorkflowInstanceDetailResponse response = workflowService.getInstanceDetailByBusiness(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-tasks")
    @Operation(summary = "Get my pending workflow tasks")
    public ResponseEntity<ApiResponse<PageResult<WorkflowTaskResponse>>> getMyPendingTasks(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "1") int pageNum
    ) {
        PageResult<WorkflowTaskResponse> result = workflowService.getMyPendingTasks(
                currentUser.getId(), pageNum);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 3. 工作流操作

    @PostMapping("/tasks/{taskId}/approve")
    @Operation(summary = "Approve the current workflow node and advance to the next node")
    public ResponseEntity<ApiResponse<WorkflowInstanceResponse>> approveTask(
            @PathVariable String taskId,
            @Valid @RequestBody ApprovalRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        WorkflowInstanceResponse response = workflowService.approveTask(
                taskId, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/tasks/{taskId}/decision")
    @Operation(summary = "Decide the current workflow node by task instance ID and boolean result")
    public ResponseEntity<ApiResponse<WorkflowInstanceResponse>> decideTask(
            @PathVariable String taskId,
            @Valid @RequestBody WorkflowTaskDecisionRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        WorkflowInstanceResponse response = workflowService.decideTask(
                taskId, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/tasks/{taskId}/reject")
    @Operation(summary = "Reject the current workflow node and roll back to the previous approved node")
    public ResponseEntity<ApiResponse<WorkflowInstanceResponse>> rejectTask(
            @PathVariable String taskId,
            @Valid @RequestBody RejectionRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        WorkflowInstanceResponse response = workflowService.rejectTask(
                taskId, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/tasks/{taskId}/reassign")
    @Operation(summary = "Reassign a workflow task (not supported for fixed approval templates)")
    public ResponseEntity<ApiResponse<WorkflowInstanceResponse>> reassignTask(
            @PathVariable String taskId,
            @Valid @RequestBody ReassignRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        WorkflowInstanceResponse response = workflowService.reassignTask(
                taskId, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{instanceId}/cancel")
    @Operation(summary = "Cancel a workflow instance before the first approval node is handled")
    public ResponseEntity<ApiResponse<Void>> cancelInstance(
            @PathVariable String instanceId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        workflowService.cancelInstance(instanceId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 4. 流程定义管理

    @GetMapping("/definitions/{definitionId}")
    @Operation(summary = "Get workflow definition by ID")
    public ResponseEntity<ApiResponse<WorkflowDefinitionResponse>> getDefinitionById(
            @PathVariable String definitionId
    ) {
        WorkflowDefinitionResponse response = workflowService.getDefinitionById(definitionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/definitions/code/{flowCode}")
    @Operation(summary = "Get workflow definition by code")
    public ResponseEntity<ApiResponse<WorkflowDefinitionResponse>> getDefinitionByCode(
            @PathVariable String flowCode
    ) {
        WorkflowDefinitionResponse response = workflowService.getDefinitionByCode(flowCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/definitions/code/{flowCode}/preview")
    @Operation(summary = "Preview workflow definition with candidate approvers")
    public ResponseEntity<ApiResponse<WorkflowDefinitionPreviewResponse>> getDefinitionPreviewByCode(
            @PathVariable String flowCode,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        WorkflowDefinitionPreviewResponse response = workflowService.getDefinitionPreview(flowCode, currentUser.getOrgId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/definitions/entity-type/{entityType}")
    @Operation(summary = "Get workflow definitions by entity type")
    public ResponseEntity<ApiResponse<List<WorkflowDefinitionResponse>>> getDefinitionsByEntityType(
            @PathVariable String entityType
    ) {
        List<WorkflowDefinitionResponse> response = workflowService.getDefinitionsByEntityType(entityType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/definitions")
    @Operation(
            summary = "Create a fixed workflow template with explicit ordered steps",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    implementation = CreateWorkflowDefinitionRequest.class
                            )
                    )
            )
    )
    public ResponseEntity<ApiResponse<WorkflowDefinitionResponse>> createDefinition(
            @Valid @RequestBody CreateWorkflowDefinitionRequest request
    ) {
        WorkflowDefinitionResponse response = workflowService.createDefinition(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 5. 实例查询

    @GetMapping("/my-approved")
    @Operation(summary = "Get my approved workflow instances")
    public ResponseEntity<ApiResponse<PageResult<WorkflowInstanceResponse>>> getMyApprovedInstances(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PageResult<WorkflowInstanceResponse> result = workflowService.getMyApprovedInstances(
                currentUser.getId(), pageNum, pageSize);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/my-applied")
    @Operation(summary = "Get my applied workflow instances")
    public ResponseEntity<ApiResponse<PageResult<WorkflowInstanceResponse>>> getMyAppliedInstances(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PageResult<WorkflowInstanceResponse> result = workflowService.getMyAppliedInstances(
                currentUser.getId(), pageNum, pageSize);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 6. 统计

    @GetMapping("/statistics")
    @Operation(summary = "Get workflow statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        Map<String, Object> stats = workflowService.getStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
