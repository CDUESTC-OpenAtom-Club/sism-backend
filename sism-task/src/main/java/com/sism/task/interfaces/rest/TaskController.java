package com.sism.task.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.task.application.TaskApplicationService;
import com.sism.task.application.dto.CreateTaskRequest;
import com.sism.task.application.dto.TaskResponse;
import com.sism.task.application.dto.UpdateTaskNameRequest;
import io.swagger.v3.oas.annotations.Operation;
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
            @RequestParam String taskDesc) {
        TaskResponse updated = taskApplicationService.updateTaskDesc(id, taskDesc);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }
}
