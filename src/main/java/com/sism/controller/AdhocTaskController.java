package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.common.PageResult;
import com.sism.dto.AdhocTaskCreateRequest;
import com.sism.dto.AdhocTaskUpdateRequest;
import com.sism.enums.AdhocTaskStatus;
import com.sism.service.AdhocTaskService;
import com.sism.vo.AdhocTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Adhoc Task Controller
 * Provides CRUD operations for adhoc tasks
 * 
 * Requirements: 10.1, 10.2, 10.3, 10.4
 */
@Slf4j
@RestController
@RequestMapping("/api/adhoc-tasks")
@RequiredArgsConstructor
@Tag(name = "Adhoc Tasks", description = "Adhoc task management endpoints")
public class AdhocTaskController {

    private final AdhocTaskService adhocTaskService;

    /**
     * Get adhoc task by ID
     * GET /api/adhoc-tasks/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get adhoc task by ID", description = "Retrieve a specific adhoc task with targets and indicators")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Adhoc task found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Adhoc task not found")
    })
    public ResponseEntity<ApiResponse<AdhocTaskVO>> getAdhocTaskById(
            @Parameter(description = "Adhoc task ID") @PathVariable Long id) {
        AdhocTaskVO adhocTask = adhocTaskService.getAdhocTaskById(id);
        return ResponseEntity.ok(ApiResponse.success(adhocTask));
    }

    /**
     * Get adhoc tasks by cycle ID
     * GET /api/adhoc-tasks/cycle/{cycleId}
     */
    @GetMapping("/cycle/{cycleId}")
    @Operation(summary = "Get adhoc tasks by cycle", description = "Retrieve adhoc tasks for a specific assessment cycle")
    public ResponseEntity<ApiResponse<List<AdhocTaskVO>>> getAdhocTasksByCycleId(
            @Parameter(description = "Assessment cycle ID") @PathVariable Long cycleId) {
        List<AdhocTaskVO> adhocTasks = adhocTaskService.getAdhocTasksByCycleId(cycleId);
        return ResponseEntity.ok(ApiResponse.success(adhocTasks));
    }

    /**
     * Get adhoc tasks by cycle ID with pagination
     * GET /api/adhoc-tasks/cycle/{cycleId}/page
     */
    @GetMapping("/cycle/{cycleId}/page")
    @Operation(summary = "Get adhoc tasks by cycle (paginated)", 
               description = "Retrieve adhoc tasks for a cycle with pagination")
    public ResponseEntity<ApiResponse<PageResult<AdhocTaskVO>>> getAdhocTasksByCycleIdPaged(
            @Parameter(description = "Assessment cycle ID") @PathVariable Long cycleId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdhocTaskVO> adhocTaskPage = adhocTaskService.getAdhocTasksByCycleId(cycleId, pageable);
        PageResult<AdhocTaskVO> result = new PageResult<>(
            adhocTaskPage.getContent(), 
            adhocTaskPage.getTotalElements(), 
            page, 
            size
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get adhoc tasks by creator organization ID
     * GET /api/adhoc-tasks/creator/{creatorOrgId}
     */
    @GetMapping("/creator/{creatorOrgId}")
    @Operation(summary = "Get adhoc tasks by creator org", 
               description = "Retrieve adhoc tasks created by a specific organization")
    public ResponseEntity<ApiResponse<List<AdhocTaskVO>>> getAdhocTasksByCreatorOrgId(
            @Parameter(description = "Creator organization ID") @PathVariable Long creatorOrgId) {
        List<AdhocTaskVO> adhocTasks = adhocTaskService.getAdhocTasksByCreatorOrgId(creatorOrgId);
        return ResponseEntity.ok(ApiResponse.success(adhocTasks));
    }

    /**
     * Get adhoc tasks by status
     * GET /api/adhoc-tasks/status/{status}
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get adhoc tasks by status", description = "Retrieve adhoc tasks with a specific status")
    public ResponseEntity<ApiResponse<List<AdhocTaskVO>>> getAdhocTasksByStatus(
            @Parameter(description = "Adhoc task status") @PathVariable AdhocTaskStatus status) {
        List<AdhocTaskVO> adhocTasks = adhocTaskService.getAdhocTasksByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(adhocTasks));
    }

    /**
     * Get adhoc tasks by status with pagination
     * GET /api/adhoc-tasks/status/{status}/page
     */
    @GetMapping("/status/{status}/page")
    @Operation(summary = "Get adhoc tasks by status (paginated)", 
               description = "Retrieve adhoc tasks with a specific status with pagination")
    public ResponseEntity<ApiResponse<PageResult<AdhocTaskVO>>> getAdhocTasksByStatusPaged(
            @Parameter(description = "Adhoc task status") @PathVariable AdhocTaskStatus status,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdhocTaskVO> adhocTaskPage = adhocTaskService.getAdhocTasksByStatus(status, pageable);
        PageResult<AdhocTaskVO> result = new PageResult<>(
            adhocTaskPage.getContent(), 
            adhocTaskPage.getTotalElements(), 
            page, 
            size
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Search adhoc tasks by keyword
     * GET /api/adhoc-tasks/search?keyword=xxx
     */
    @GetMapping("/search")
    @Operation(summary = "Search adhoc tasks", description = "Search adhoc tasks by keyword")
    public ResponseEntity<ApiResponse<List<AdhocTaskVO>>> searchAdhocTasks(
            @Parameter(description = "Search keyword") @RequestParam String keyword) {
        List<AdhocTaskVO> adhocTasks = adhocTaskService.searchAdhocTasks(keyword);
        return ResponseEntity.ok(ApiResponse.success(adhocTasks));
    }

    /**
     * Get overdue adhoc tasks
     * GET /api/adhoc-tasks/overdue
     */
    @GetMapping("/overdue")
    @Operation(summary = "Get overdue adhoc tasks", description = "Retrieve all overdue adhoc tasks")
    public ResponseEntity<ApiResponse<List<AdhocTaskVO>>> getOverdueAdhocTasks() {
        List<AdhocTaskVO> adhocTasks = adhocTaskService.getOverdueAdhocTasks();
        return ResponseEntity.ok(ApiResponse.success(adhocTasks));
    }

    /**
     * Get adhoc tasks included in alert calculation
     * GET /api/adhoc-tasks/include-in-alert
     */
    @GetMapping("/include-in-alert")
    @Operation(summary = "Get adhoc tasks in alert", 
               description = "Retrieve adhoc tasks included in alert calculation")
    public ResponseEntity<ApiResponse<List<AdhocTaskVO>>> getAdhocTasksIncludedInAlert() {
        List<AdhocTaskVO> adhocTasks = adhocTaskService.getAdhocTasksIncludedInAlert();
        return ResponseEntity.ok(ApiResponse.success(adhocTasks));
    }

    /**
     * Get target organizations for an adhoc task
     * GET /api/adhoc-tasks/{id}/targets
     */
    @GetMapping("/{id}/targets")
    @Operation(summary = "Get target organizations", 
               description = "Retrieve target organizations for an adhoc task")
    public ResponseEntity<ApiResponse<List<AdhocTaskVO.AdhocTaskTargetVO>>> getTargetOrganizations(
            @Parameter(description = "Adhoc task ID") @PathVariable Long id) {
        List<AdhocTaskVO.AdhocTaskTargetVO> targets = adhocTaskService.getTargetOrganizations(id);
        return ResponseEntity.ok(ApiResponse.success(targets));
    }

    /**
     * Get mapped indicators for an adhoc task
     * GET /api/adhoc-tasks/{id}/indicators
     */
    @GetMapping("/{id}/indicators")
    @Operation(summary = "Get mapped indicators", 
               description = "Retrieve mapped indicators for an adhoc task")
    public ResponseEntity<ApiResponse<List<AdhocTaskVO.AdhocTaskIndicatorVO>>> getMappedIndicators(
            @Parameter(description = "Adhoc task ID") @PathVariable Long id) {
        List<AdhocTaskVO.AdhocTaskIndicatorVO> indicators = adhocTaskService.getMappedIndicators(id);
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    /**
     * Create a new adhoc task
     * POST /api/adhoc-tasks
     * Requirements: 10.1, 10.2, 10.3, 10.4
     */
    @PostMapping
    @Operation(summary = "Create adhoc task", description = "Create a new adhoc task with scope type handling")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Adhoc task created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ApiResponse<AdhocTaskVO>> createAdhocTask(
            @Valid @RequestBody AdhocTaskCreateRequest request) {
        log.info("Creating adhoc task: {} with scope type: {}", request.getTaskTitle(), request.getScopeType());
        AdhocTaskVO adhocTask = adhocTaskService.createAdhocTask(request);
        return ResponseEntity.ok(ApiResponse.success("Adhoc task created successfully", adhocTask));
    }

    /**
     * Update an existing adhoc task
     * PUT /api/adhoc-tasks/{id}
     * Requirements: 10.4, 10.5
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update adhoc task", description = "Update an existing adhoc task")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Adhoc task updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot update completed/canceled task"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Adhoc task not found")
    })
    public ResponseEntity<ApiResponse<AdhocTaskVO>> updateAdhocTask(
            @Parameter(description = "Adhoc task ID") @PathVariable Long id,
            @Valid @RequestBody AdhocTaskUpdateRequest request) {
        log.info("Updating adhoc task: {}", id);
        AdhocTaskVO adhocTask = adhocTaskService.updateAdhocTask(id, request);
        return ResponseEntity.ok(ApiResponse.success("Adhoc task updated successfully", adhocTask));
    }

    /**
     * Activate an adhoc task (DRAFT -> ACTIVE)
     * POST /api/adhoc-tasks/{id}/activate
     */
    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate adhoc task", description = "Activate a draft adhoc task")
    public ResponseEntity<ApiResponse<AdhocTaskVO>> activateAdhocTask(
            @Parameter(description = "Adhoc task ID") @PathVariable Long id) {
        log.info("Activating adhoc task: {}", id);
        AdhocTaskVO adhocTask = adhocTaskService.activateAdhocTask(id);
        return ResponseEntity.ok(ApiResponse.success("Adhoc task activated", adhocTask));
    }

    /**
     * Complete an adhoc task (ACTIVE -> COMPLETED)
     * POST /api/adhoc-tasks/{id}/complete
     */
    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete adhoc task", description = "Mark an active adhoc task as completed")
    public ResponseEntity<ApiResponse<AdhocTaskVO>> completeAdhocTask(
            @Parameter(description = "Adhoc task ID") @PathVariable Long id) {
        log.info("Completing adhoc task: {}", id);
        AdhocTaskVO adhocTask = adhocTaskService.completeAdhocTask(id);
        return ResponseEntity.ok(ApiResponse.success("Adhoc task completed", adhocTask));
    }

    /**
     * Cancel an adhoc task (DRAFT/ACTIVE -> CANCELED)
     * POST /api/adhoc-tasks/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel adhoc task", description = "Cancel an adhoc task")
    public ResponseEntity<ApiResponse<AdhocTaskVO>> cancelAdhocTask(
            @Parameter(description = "Adhoc task ID") @PathVariable Long id) {
        log.info("Canceling adhoc task: {}", id);
        AdhocTaskVO adhocTask = adhocTaskService.cancelAdhocTask(id);
        return ResponseEntity.ok(ApiResponse.success("Adhoc task canceled", adhocTask));
    }

    /**
     * Delete an adhoc task (only DRAFT tasks)
     * DELETE /api/adhoc-tasks/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete adhoc task", description = "Delete a draft adhoc task")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Adhoc task deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Only draft tasks can be deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Adhoc task not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteAdhocTask(
            @Parameter(description = "Adhoc task ID") @PathVariable Long id) {
        log.info("Deleting adhoc task: {}", id);
        adhocTaskService.deleteAdhocTask(id);
        return ResponseEntity.ok(ApiResponse.success("Adhoc task deleted successfully", null));
    }
}
