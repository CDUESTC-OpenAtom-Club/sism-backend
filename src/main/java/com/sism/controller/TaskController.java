package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.dto.TaskCreateRequest;
import com.sism.dto.TaskUpdateRequest;
import com.sism.service.TaskService;
import com.sism.vo.TaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Strategic Task Controller for SISM (Strategic Indicator Management System).
 * 
 * <p>This controller manages strategic tasks which represent high-level organizational
 * objectives that are decomposed into measurable indicators. Tasks serve as the top-level
 * organizational unit in the strategic planning hierarchy.
 * 
 * <h2>Task Hierarchy</h2>
 * <pre>
 * Assessment Cycle
 *   └── Strategic Task
 *         └── Indicators (decomposed from task)
 *               └── Milestones (time-based progress tracking)
 * </pre>
 * 
 * <h2>Key Features</h2>
 * <ul>
 *   <li>CRUD operations for strategic tasks</li>
 *   <li>Task filtering by cycle and organization</li>
 *   <li>Keyword search across task names and descriptions</li>
 *   <li>Soft deletion for data integrity</li>
 * </ul>
 * 
 * <h2>API Endpoints</h2>
 * <ul>
 *   <li>GET /api/tasks - List all tasks</li>
 *   <li>GET /api/tasks/{id} - Get task details</li>
 *   <li>GET /api/tasks/cycle/{cycleId} - Filter by cycle</li>
 *   <li>GET /api/tasks/org/{orgId} - Filter by organization</li>
 *   <li>GET /api/tasks/search?keyword=xxx - Search tasks</li>
 *   <li>POST /api/tasks - Create new task</li>
 *   <li>PUT /api/tasks/{id} - Update task</li>
 *   <li>DELETE /api/tasks/{id} - Soft delete task</li>
 * </ul>
 * 
 * @author SISM Development Team
 * @version 1.0
 * @since 1.0
 * @see com.sism.service.TaskService
 * @see com.sism.entity.StrategicTask
 */
@Slf4j
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Tag(name = "Strategic Tasks", description = "Strategic task management endpoints")
public class TaskController {

    private final TaskService taskService;

    /**
     * Get all tasks
     * GET /api/tasks
     */
    @GetMapping
    @Operation(summary = "Get all tasks", description = "Retrieve all strategic tasks")
    public ResponseEntity<ApiResponse<List<TaskVO>>> getAllTasks() {
        List<TaskVO> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    /**
     * Get task by ID
     * GET /api/tasks/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID", description = "Retrieve a specific strategic task")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Task found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<ApiResponse<TaskVO>> getTaskById(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        TaskVO task = taskService.getTaskById(id);
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    /**
     * Get tasks by cycle ID
     * GET /api/tasks/cycle/{cycleId}
     */
    @GetMapping("/cycle/{cycleId}")
    @Operation(summary = "Get tasks by cycle", description = "Retrieve tasks for a specific assessment cycle")
    public ResponseEntity<ApiResponse<List<TaskVO>>> getTasksByCycleId(
            @Parameter(description = "Assessment cycle ID") @PathVariable Long cycleId) {
        // TODO: 需要通过plan表关联cycle，暂时返回空列表
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    /**
     * Get tasks by organization ID
     * GET /api/tasks/org/{orgId}
     */
    @GetMapping("/org/{orgId}")
    @Operation(summary = "Get tasks by organization", description = "Retrieve tasks for a specific organization")
    public ResponseEntity<ApiResponse<List<TaskVO>>> getTasksByOrgId(
            @Parameter(description = "Organization ID") @PathVariable Long orgId) {
        // TODO: 需要通过plan表关联org，暂时返回空列表
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    /**
     * Search tasks by keyword
     * GET /api/tasks/search?keyword=xxx
     */
    @GetMapping("/search")
    @Operation(summary = "Search tasks", description = "Search tasks by keyword")
    public ResponseEntity<ApiResponse<List<TaskVO>>> searchTasks(
            @Parameter(description = "Search keyword") @RequestParam String keyword) {
        List<TaskVO> tasks = taskService.searchTasks(keyword);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    /**
     * Create a new task
     * POST /api/tasks
     */
    @PostMapping
    @Operation(summary = "Create task", description = "Create a new strategic task")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Task created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ApiResponse<TaskVO>> createTask(
            @Valid @RequestBody TaskCreateRequest request) {
        log.info("Creating task: {}", request.getTaskName());
        TaskVO task = taskService.createTask(request);
        return ResponseEntity.ok(ApiResponse.success("Task created successfully", task));
    }

    /**
     * Update an existing task
     * PUT /api/tasks/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update task", description = "Update an existing strategic task")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Task updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<ApiResponse<TaskVO>> updateTask(
            @Parameter(description = "Task ID") @PathVariable Long id,
            @Valid @RequestBody TaskUpdateRequest request) {
        log.info("Updating task: {}", id);
        TaskVO task = taskService.updateTask(id, request);
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", task));
    }

    /**
     * Delete a task
     * DELETE /api/tasks/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete task", description = "Delete a strategic task")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Task deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        log.info("Deleting task: {}", id);
        taskService.deleteTask(id);
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully", null));
    }
}
