package com.sism.workflow.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.workflow.application.WorkflowApplicationService;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.WorkflowTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * WorkflowController - 工作流API控制器
 * 提供工作流相关的REST API端点
 */
@RestController
@RequestMapping("/api/v1/approval")
@RequiredArgsConstructor
@Tag(name = "Workflows", description = "Workflow and approval management endpoints")
public class WorkflowController {

    private final WorkflowApplicationService workflowApplicationService;

    // ==================== Audit Flow Definition Endpoints ====================

    @GetMapping("/flows")
    @Operation(summary = "Get all approval flow definitions")
    public ResponseEntity<ApiResponse<List<AuditFlowDef>>> getAllFlowDefinitions() {
        List<AuditFlowDef> flowDefs = workflowApplicationService.getAllAuditFlowDefs();
        return ResponseEntity.ok(ApiResponse.success(flowDefs));
    }

    @GetMapping("/flows/{id}")
    @Operation(summary = "Get approval flow definition by ID")
    public ResponseEntity<ApiResponse<AuditFlowDef>> getFlowDefinitionById(@PathVariable Long id) {
        AuditFlowDef flowDef = workflowApplicationService.getAuditFlowDefById(id);
        if (flowDef == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Flow definition not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(flowDef));
    }

    @GetMapping("/flows/code/{flowCode}")
    @Operation(summary = "Get approval flow definition by code")
    public ResponseEntity<ApiResponse<AuditFlowDef>> getFlowDefinitionByCode(@PathVariable String flowCode) {
        AuditFlowDef flowDef = workflowApplicationService.getAuditFlowDefByCode(flowCode);
        if (flowDef == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Flow definition not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(flowDef));
    }

    @GetMapping("/flows/entity-type/{entityType}")
    @Operation(summary = "Get approval flow definitions by entity type")
    public ResponseEntity<ApiResponse<List<AuditFlowDef>>> getFlowDefinitionsByEntityType(@PathVariable String entityType) {
        List<AuditFlowDef> flowDefs = workflowApplicationService.getAuditFlowDefsByEntityType(entityType);
        return ResponseEntity.ok(ApiResponse.success(flowDefs));
    }

    @PostMapping("/flows")
    @Operation(summary = "Create approval flow definition")
    public ResponseEntity<ApiResponse<AuditFlowDef>> createFlowDefinition(@RequestBody AuditFlowDef flowDef) {
        AuditFlowDef created = workflowApplicationService.createAuditFlowDef(flowDef);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    // ==================== Audit Instance Endpoints ====================

    @GetMapping("/instances")
    @Operation(summary = "Get all approval instances")
    public ResponseEntity<ApiResponse<List<AuditInstance>>> getAllInstances() {
        List<AuditInstance> instances = workflowApplicationService.getAllAuditInstances();
        return ResponseEntity.ok(ApiResponse.success(instances));
    }

    @GetMapping("/instances/{instanceId}")
    @Operation(summary = "Get approval instance by ID")
    public ResponseEntity<ApiResponse<AuditInstance>> getInstanceById(@PathVariable Long instanceId) {
        AuditInstance instance = workflowApplicationService.getAuditInstanceById(instanceId);
        if (instance == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Approval instance not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(instance));
    }

    @GetMapping("/instances/my-pending")
    @Operation(summary = "Get my pending approval instances")
    public ResponseEntity<ApiResponse<List<AuditInstance>>> getMyPendingInstances(@RequestParam Long userId) {
        List<AuditInstance> instances = workflowApplicationService.getPendingAuditInstancesByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(instances));
    }

    @GetMapping("/instances/my-approved")
    @Operation(summary = "Get my approved instances")
    public ResponseEntity<ApiResponse<List<AuditInstance>>> getMyApprovedInstances(@RequestParam Long userId) {
        List<AuditInstance> instances = workflowApplicationService.getApprovedAuditInstancesByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(instances));
    }

    @GetMapping("/instances/my-applied")
    @Operation(summary = "Get my applied instances")
    public ResponseEntity<ApiResponse<List<AuditInstance>>> getMyAppliedInstances(@RequestParam Long userId) {
        List<AuditInstance> instances = workflowApplicationService.getAppliedAuditInstancesByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(instances));
    }

    @GetMapping("/instances/{instanceId}/history")
    @Operation(summary = "Get approval instance history")
    public ResponseEntity<ApiResponse<List<AuditInstance>>> getInstanceHistory(@PathVariable Long instanceId) {
        List<AuditInstance> instances = workflowApplicationService.getAuditInstanceHistory(instanceId);
        return ResponseEntity.ok(ApiResponse.success(instances));
    }

    @PostMapping("/instances")
    @Operation(summary = "Start approval instance")
    public ResponseEntity<ApiResponse<AuditInstance>> startInstance(
            @RequestBody AuditInstance instance,
            @RequestParam Long requesterId,
            @RequestParam Long requesterOrgId) {
        AuditInstance started = workflowApplicationService.startAuditInstance(instance, requesterId, requesterOrgId);
        return ResponseEntity.ok(ApiResponse.success(started));
    }

    @PostMapping("/instances/{instanceId}/approve")
    @Operation(summary = "Approve approval instance")
    public ResponseEntity<ApiResponse<AuditInstance>> approveInstance(
            @PathVariable Long instanceId,
            @RequestBody AuditInstance instance,
            @RequestParam Long userId,
            @RequestParam(required = false) String comment) {
        // 先从数据库获取完整的实例信息
        AuditInstance existingInstance = workflowApplicationService.getAuditInstanceById(instanceId);
        if (existingInstance == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Approval instance not found"));
        }
        // 使用数据库中的实例进行操作
        AuditInstance approved = workflowApplicationService.approveAuditInstance(existingInstance, userId, comment);
        return ResponseEntity.ok(ApiResponse.success(approved));
    }

    @PostMapping("/instances/{instanceId}/reject")
    @Operation(summary = "Reject approval instance")
    public ResponseEntity<ApiResponse<AuditInstance>> rejectInstance(
            @PathVariable Long instanceId,
            @RequestBody AuditInstance instance,
            @RequestParam Long userId,
            @RequestParam(required = false) String comment) {
        // 先从数据库获取完整的实例信息
        AuditInstance existingInstance = workflowApplicationService.getAuditInstanceById(instanceId);
        if (existingInstance == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Approval instance not found"));
        }
        // 使用数据库中的实例进行操作
        AuditInstance rejected = workflowApplicationService.rejectAuditInstance(existingInstance, userId, comment);
        return ResponseEntity.ok(ApiResponse.success(rejected));
    }

    @PostMapping("/instances/{instanceId}/cancel")
    @Operation(summary = "Cancel approval instance")
    public ResponseEntity<ApiResponse<AuditInstance>> cancelInstance(
            @PathVariable Long instanceId,
            @RequestBody AuditInstance instance) {
        // 先从数据库获取完整的实例信息
        AuditInstance existingInstance = workflowApplicationService.getAuditInstanceById(instanceId);
        if (existingInstance == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Approval instance not found"));
        }
        // 使用数据库中的实例进行操作
        AuditInstance cancelled = workflowApplicationService.cancelAuditInstance(existingInstance);
        return ResponseEntity.ok(ApiResponse.success(cancelled));
    }

    @PostMapping("/instances/{instanceId}/transfer")
    @Operation(summary = "Transfer approval instance")
    public ResponseEntity<ApiResponse<AuditInstance>> transferInstance(
            @PathVariable Long instanceId,
            @RequestParam Long targetUserId) {
        AuditInstance transferred = workflowApplicationService.transferAuditInstance(instanceId, targetUserId);
        return ResponseEntity.ok(ApiResponse.success(transferred));
    }

    @PostMapping("/instances/{instanceId}/add-approver")
    @Operation(summary = "Add approver to instance")
    public ResponseEntity<ApiResponse<AuditInstance>> addApprover(
            @PathVariable Long instanceId,
            @RequestParam Long approverId) {
        AuditInstance updated = workflowApplicationService.addApproverToInstance(instanceId, approverId);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    // ==================== Workflow Task Endpoints ====================

    @PostMapping("/tasks")
    @Operation(summary = "Start workflow task")
    public ResponseEntity<ApiResponse<WorkflowTask>> startTask(
            @RequestBody WorkflowTask task,
            @RequestParam Long operatorId,
            @RequestParam Long operatorOrgId) {
        WorkflowTask started = workflowApplicationService.startWorkflowTask(task, operatorId, operatorOrgId);
        return ResponseEntity.ok(ApiResponse.success(started));
    }

    @PostMapping("/tasks/{id}/complete")
    @Operation(summary = "Complete workflow task")
    public ResponseEntity<ApiResponse<WorkflowTask>> completeTask(
            @PathVariable Long id,
            @RequestBody WorkflowTask task,
            @RequestParam String result) {
        // 这里假设 WorkflowTask 没有对应的 get 方法，需要根据实际情况调整
        // 如果有类似 getWorkflowTaskById 的方法，应该先获取完整实例
        WorkflowTask completed = workflowApplicationService.completeWorkflowTask(task, result);
        return ResponseEntity.ok(ApiResponse.success(completed));
    }

    @PostMapping("/tasks/{id}/fail")
    @Operation(summary = "Fail workflow task")
    public ResponseEntity<ApiResponse<WorkflowTask>> failTask(
            @PathVariable Long id,
            @RequestBody WorkflowTask task,
            @RequestParam String errorMessage) {
        WorkflowTask failed = workflowApplicationService.failWorkflowTask(task, errorMessage);
        return ResponseEntity.ok(ApiResponse.success(failed));
    }

    @PostMapping("/tasks/{id}/approve")
    @Operation(summary = "Approve workflow task")
    public ResponseEntity<ApiResponse<WorkflowTask>> approveTask(
            @PathVariable Long id,
            @RequestBody WorkflowTask task,
            @RequestParam Long approverId,
            @RequestParam(required = false) String comment) {
        WorkflowTask approved = workflowApplicationService.approveWorkflowTask(task, approverId, comment);
        return ResponseEntity.ok(ApiResponse.success(approved));
    }

    @PostMapping("/tasks/{id}/reject")
    @Operation(summary = "Reject workflow task")
    public ResponseEntity<ApiResponse<WorkflowTask>> rejectTask(
            @PathVariable Long id,
            @RequestBody WorkflowTask task,
            @RequestParam Long approverId,
            @RequestParam(required = false) String comment) {
        WorkflowTask rejected = workflowApplicationService.rejectWorkflowTask(task, approverId, comment);
        return ResponseEntity.ok(ApiResponse.success(rejected));
    }

    // ==================== Statistics ====================

    @GetMapping("/statistics")
    @Operation(summary = "Get approval statistics")
    public ResponseEntity<ApiResponse<Object>> getStatistics() {
        Object stats = workflowApplicationService.getApprovalStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
