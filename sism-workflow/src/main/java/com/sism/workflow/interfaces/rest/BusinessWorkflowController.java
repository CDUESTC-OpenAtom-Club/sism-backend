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

/**
 * BusinessWorkflowController - 业务工作流控制器
 * 管理指标下发、报告审批等核心业务流程的工作流操作
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
    @Operation(summary = "Start a business workflow by code")
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
    @Operation(summary = "Approve a workflow task")
    public ResponseEntity<ApiResponse<WorkflowInstanceResponse>> approveTask(
            @PathVariable String taskId,
            @Valid @RequestBody ApprovalRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        WorkflowInstanceResponse response = workflowService.approveTask(
                taskId, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/tasks/{taskId}/reject")
    @Operation(summary = "Reject a workflow task")
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
    @Operation(summary = "Reassign a workflow task")
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
    @Operation(summary = "Cancel a workflow instance")
    public ResponseEntity<ApiResponse<Void>> cancelInstance(
            @PathVariable String instanceId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        workflowService.cancelInstance(instanceId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
