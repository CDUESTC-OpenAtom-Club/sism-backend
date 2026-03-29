package com.sism.task.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.common.PageResult;
import com.sism.task.application.TaskApplicationService;
import com.sism.task.application.dto.*;
import com.sism.task.domain.TaskType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "任务管理", description = "任务中心相关接口。战略任务是当前的任务类型之一。")
public class TaskController {

    private final TaskApplicationService taskApplicationService;

    @PostMapping
    @Operation(summary = "创建新任务", description = "创建战略任务，并为未来任务类型保留明确的任务类别语义。")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse created = taskApplicationService.createTask(request);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取任务")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(@PathVariable Long id) {
        TaskResponse task = taskApplicationService.getTaskById(id);
        if (task == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Task not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    @GetMapping
    @Operation(summary = "获取所有任务")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getAllTasks() {
        List<TaskResponse> tasks = taskApplicationService.getAllTasks();
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索任务(带过滤和分页)", description = "搜索响应区分计划状态和任务自身状态。")
    public ResponseEntity<ApiResponse<PageResult<TaskResponse>>> searchTasks(
            @Parameter(description = "计划ID") @RequestParam(required = false) Long planId,
            @Parameter(description = "考核周期ID") @RequestParam(required = false) Long cycleId,
            @Parameter(description = "组织ID") @RequestParam(required = false) Long orgId,
            @Parameter(description = "创建组织ID") @RequestParam(required = false) Long createdByOrgId,
            @Parameter(description = "任务类型") @RequestParam(required = false) String taskType,
            @Parameter(description = "规划投影状态") @RequestParam(required = false) String planStatus,
            @Parameter(description = "任务自身状态") @RequestParam(required = false) String taskStatus,
            @Parameter(description = "任务名称模糊搜索") @RequestParam(required = false) String name,
            @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(required = false, defaultValue = "10") Integer size) {
        TaskQueryRequest queryRequest = new TaskQueryRequest();
        queryRequest.setPlanId(planId);
        queryRequest.setCycleId(cycleId);
        queryRequest.setOrgId(orgId);
        queryRequest.setCreatedByOrgId(createdByOrgId);
        queryRequest.setTaskType(taskType != null ? TaskType.valueOf(taskType) : null);
        queryRequest.setPlanStatus(planStatus);
        queryRequest.setTaskStatus(taskStatus);
        queryRequest.setName(name);
        queryRequest.setPage(page);
        queryRequest.setSize(size);

        PageResult<TaskResponse> result = taskApplicationService.searchTasks(queryRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "激活任务", description = "仅更新任务状态。此接口不代表计划审批或分发状态。")
    public ResponseEntity<ApiResponse<TaskResponse>> activateTask(@PathVariable Long id) {
        TaskResponse activated = taskApplicationService.activateTask(id);
        return ResponseEntity.ok(ApiResponse.success(activated));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成任务", description = "仅更新任务状态。此接口不代表计划审批或分发状态。")
    public ResponseEntity<ApiResponse<TaskResponse>> completeTask(@PathVariable Long id) {
        TaskResponse completed = taskApplicationService.completeTask(id);
        return ResponseEntity.ok(ApiResponse.success(completed));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消任务", description = "仅更新任务状态。此接口不代表计划审批或分发状态。")
    public ResponseEntity<ApiResponse<TaskResponse>> cancelTask(@PathVariable Long id) {
        TaskResponse cancelled = taskApplicationService.cancelTask(id);
        return ResponseEntity.ok(ApiResponse.success(cancelled));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新任务")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        TaskResponse updated = taskApplicationService.updateTask(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PutMapping("/{id}/name")
    @Operation(summary = "更新任务名称")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskName(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskNameRequest request) {
        TaskResponse updated = taskApplicationService.updateTaskName(id, request.getName());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PutMapping("/{id}/sort-order")
    @Operation(summary = "更新任务排序顺序")
    public ResponseEntity<ApiResponse<TaskResponse>> updateSortOrder(
            @PathVariable Long id,
            @RequestParam Integer sortOrder) {
        TaskResponse updated = taskApplicationService.updateSortOrder(id, sortOrder);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PutMapping("/{id}/desc")
    @Operation(summary = "更新任务描述")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskDesc(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskDescRequest request) {
        TaskResponse updated = taskApplicationService.updateTaskDesc(id, request.getDesc());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PutMapping("/{id}/remark")
    @Operation(summary = "更新任务备注")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskRemark(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRemarkRequest request) {
        TaskResponse updated = taskApplicationService.updateTaskRemark(id, request.getRemark());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除任务(软删除)")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long id) {
        taskApplicationService.deleteTask(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/by-org/{orgId}")
    @Operation(summary = "根据组织ID获取任务列表")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksByOrgId(
            @PathVariable @Parameter(description = "组织ID") Long orgId) {
        List<TaskResponse> tasks = taskApplicationService.getTasksByOrgId(orgId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/by-plan/{planId}")
    @Operation(summary = "根据计划ID获取任务列表")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksByPlanId(
            @PathVariable @Parameter(description = "计划ID") Long planId) {
        List<TaskResponse> tasks = taskApplicationService.getTasksByPlanId(planId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/by-cycle/{cycleId}")
    @Operation(summary = "根据考核周期ID获取任务列表")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksByCycleId(
            @PathVariable @Parameter(description = "考核周期ID") Long cycleId) {
        List<TaskResponse> tasks = taskApplicationService.getTasksByCycleId(cycleId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/by-type/{taskType}")
    @Operation(summary = "根据任务类型获取任务列表")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksByType(
            @PathVariable @Parameter(description = "任务类型") TaskType taskType) {
        List<TaskResponse> tasks = taskApplicationService.getTasksByType(taskType);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }
}
