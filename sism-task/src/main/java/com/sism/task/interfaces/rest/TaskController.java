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
@Tag(name = "Tasks", description = "Task management endpoints")
public class TaskController {

    private final TaskApplicationService taskApplicationService;

    @PostMapping
    @Operation(summary = "Create a new strategic task")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse created = taskApplicationService.createTask(request);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(@PathVariable Long id) {
        TaskResponse task = taskApplicationService.getTaskById(id);
        if (task == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Task not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    @GetMapping
    @Operation(summary = "Get all tasks")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getAllTasks() {
        List<TaskResponse> tasks = taskApplicationService.getAllTasks();
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/search")
    @Operation(summary = "Search tasks with filters and pagination")
    public ResponseEntity<ApiResponse<PageResult<TaskResponse>>> searchTasks(
            @Parameter(description = "计划ID") @RequestParam(required = false) Long planId,
            @Parameter(description = "考核周期ID") @RequestParam(required = false) Long cycleId,
            @Parameter(description = "组织ID") @RequestParam(required = false) Long orgId,
            @Parameter(description = "创建组织ID") @RequestParam(required = false) Long createdByOrgId,
            @Parameter(description = "任务类型") @RequestParam(required = false) String taskType,
            @Parameter(description = "任务状态") @RequestParam(required = false) String status,
            @Parameter(description = "任务名称模糊搜索") @RequestParam(required = false) String taskName,
            @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(required = false, defaultValue = "10") Integer size) {
        TaskQueryRequest queryRequest = new TaskQueryRequest();
        queryRequest.setPlanId(planId);
        queryRequest.setCycleId(cycleId);
        queryRequest.setOrgId(orgId);
        queryRequest.setCreatedByOrgId(createdByOrgId);
        queryRequest.setTaskType(taskType != null ? TaskType.valueOf(taskType) : null);
        queryRequest.setStatus(status);
        queryRequest.setTaskName(taskName);
        queryRequest.setPage(page);
        queryRequest.setSize(size);

        PageResult<TaskResponse> result = taskApplicationService.searchTasks(queryRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate a task")
    public ResponseEntity<ApiResponse<TaskResponse>> activateTask(@PathVariable Long id) {
        TaskResponse activated = taskApplicationService.activateTask(id);
        return ResponseEntity.ok(ApiResponse.success(activated));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a task")
    public ResponseEntity<ApiResponse<TaskResponse>> completeTask(@PathVariable Long id) {
        TaskResponse completed = taskApplicationService.completeTask(id);
        return ResponseEntity.ok(ApiResponse.success(completed));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a task")
    public ResponseEntity<ApiResponse<TaskResponse>> cancelTask(@PathVariable Long id) {
        TaskResponse cancelled = taskApplicationService.cancelTask(id);
        return ResponseEntity.ok(ApiResponse.success(cancelled));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update entire task")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        TaskResponse updated = taskApplicationService.updateTask(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PutMapping("/{id}/name")
    @Operation(summary = "Update task name")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskName(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskNameRequest request) {
        request.setTaskId(id);
        TaskResponse updated = taskApplicationService.updateTaskName(id, request.getTaskName());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PutMapping("/{id}/sort-order")
    @Operation(summary = "Update task sort order")
    public ResponseEntity<ApiResponse<TaskResponse>> updateSortOrder(
            @PathVariable Long id,
            @RequestParam Integer sortOrder) {
        TaskResponse updated = taskApplicationService.updateSortOrder(id, sortOrder);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PutMapping("/{id}/desc")
    @Operation(summary = "Update task description")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskDesc(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskDescRequest request) {
        TaskResponse updated = taskApplicationService.updateTaskDesc(id, request.getTaskDesc());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PutMapping("/{id}/remark")
    @Operation(summary = "Update task remark")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskRemark(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRemarkRequest request) {
        TaskResponse updated = taskApplicationService.updateTaskRemark(id, request.getRemark());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long id) {
        taskApplicationService.deleteTask(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/by-org/{orgId}")
    @Operation(summary = "Get tasks by organization ID")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksByOrgId(
            @PathVariable @Parameter(description = "组织ID") Long orgId) {
        List<TaskResponse> tasks = taskApplicationService.getTasksByOrgId(orgId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/by-plan/{planId}")
    @Operation(summary = "Get tasks by plan ID")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksByPlanId(
            @PathVariable @Parameter(description = "计划ID") Long planId) {
        List<TaskResponse> tasks = taskApplicationService.getTasksByPlanId(planId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/by-cycle/{cycleId}")
    @Operation(summary = "Get tasks by cycle ID")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksByCycleId(
            @PathVariable @Parameter(description = "考核周期ID") Long cycleId) {
        List<TaskResponse> tasks = taskApplicationService.getTasksByCycleId(cycleId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/by-type/{taskType}")
    @Operation(summary = "Get tasks by task type")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksByType(
            @PathVariable @Parameter(description = "任务类型") TaskType taskType) {
        List<TaskResponse> tasks = taskApplicationService.getTasksByType(taskType);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/by-status/{status}")
    @Operation(summary = "Get tasks by status")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksByStatus(
            @PathVariable @Parameter(description = "任务状态") String status) {
        List<TaskResponse> tasks = taskApplicationService.getTasksByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }
}
