package com.sism.strategy.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.common.PageResult;
import com.sism.strategy.application.PlanApplicationService;
import com.sism.strategy.application.StrategyApplicationService;
import com.sism.strategy.interfaces.dto.PlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * PlanController - 计划管理控制器
 * 提供计划的REST API接口
 */
@RestController("strategyPlanController")
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
@Tag(name = "Plans", description = "Plan management endpoints")
public class PlanController {

    private final PlanApplicationService planApplicationService;
    private final StrategyApplicationService strategyApplicationService;

    @GetMapping
    @Operation(summary = "Get all plans with pagination")
    public ResponseEntity<ApiResponse<PageResult<PlanResponse>>> listPlans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status) {
        Page<PlanResponse> resultPage = planApplicationService.getPlans(page, size, year, status);
        PageResult<PlanResponse> pageResult = PageResult.of(
                resultPage.getContent(),
                (int) resultPage.getTotalElements(),
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(pageResult));
    }

    @GetMapping("/{id:[0-9]+}")
    @Operation(summary = "Get plan by ID")
    public ResponseEntity<ApiResponse<PlanResponse>> getPlanById(@PathVariable Long id) {
        return planApplicationService.getPlanById(id)
                .map(plan -> ResponseEntity.ok(ApiResponse.success(plan)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cycle/{cycleId}")
    @Operation(summary = "Get plans by cycle ID")
    public ResponseEntity<ApiResponse<java.util.List<PlanResponse>>> getPlansByCycle(@PathVariable Long cycleId) {
        java.util.List<PlanResponse> plans = planApplicationService.getPlansByCycle(cycleId);
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    @PostMapping
    @Operation(summary = "Create a new plan")
    public ResponseEntity<ApiResponse<PlanResponse>> createPlan(
            @Valid @RequestBody com.sism.strategy.interfaces.dto.CreatePlanRequest request) {
        PlanResponse response = planApplicationService.createPlan(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a plan")
    public ResponseEntity<ApiResponse<PlanResponse>> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody com.sism.strategy.interfaces.dto.UpdatePlanRequest request) {
        PlanResponse response = planApplicationService.updatePlan(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a plan")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable Long id) {
        planApplicationService.deletePlan(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a plan")
    public ResponseEntity<ApiResponse<PlanResponse>> publishPlan(@PathVariable Long id) {
        PlanResponse response = planApplicationService.publishPlan(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive a plan")
    public ResponseEntity<ApiResponse<PlanResponse>> archivePlan(@PathVariable Long id) {
        PlanResponse response = planApplicationService.archivePlan(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit plan for approval")
    public ResponseEntity<ApiResponse<PlanResponse>> submitPlanForApproval(@PathVariable Long id) {
        PlanResponse response = planApplicationService.submitPlanForApproval(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a plan")
    public ResponseEntity<ApiResponse<PlanResponse>> approvePlan(@PathVariable Long id) {
        PlanResponse response = planApplicationService.approvePlan(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a plan")
    public ResponseEntity<ApiResponse<PlanResponse>> rejectPlan(@PathVariable Long id) {
        PlanResponse response = planApplicationService.rejectPlan(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw plan to draft")
    public ResponseEntity<ApiResponse<PlanResponse>> withdrawPlan(@PathVariable Long id) {
        PlanResponse response = planApplicationService.withdrawPlan(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id:[0-9]+}/details")
    @Operation(summary = "Get plan details with indicators and milestones")
    public ResponseEntity<ApiResponse<PlanApplicationService.PlanDetailsResponse>> getPlanDetails(@PathVariable Long id) {
        PlanApplicationService.PlanDetailsResponse response = planApplicationService.getPlanDetails(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "Get plan by task ID")
    public ResponseEntity<ApiResponse<PlanResponse>> getPlanByTaskId(@PathVariable Long taskId) {
        // 通过 taskId 查找关联的 plan
        return planApplicationService.getPlanByTaskId(taskId)
                .map(plan -> ResponseEntity.ok(ApiResponse.success(plan)))
                .orElse(ResponseEntity.ok(ApiResponse.error(404, "Plan not found for task: " + taskId)));
    }
}
