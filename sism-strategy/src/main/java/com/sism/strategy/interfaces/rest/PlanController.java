package com.sism.strategy.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.common.PageResult;
import com.sism.iam.application.dto.CurrentUser;
import com.sism.strategy.application.PlanApplicationService;
import com.sism.strategy.application.StrategyApplicationService;
import com.sism.strategy.interfaces.dto.PlanResponse;
import com.sism.strategy.interfaces.dto.SubmitPlanApprovalRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Primary planning entrypoint for plan operations.
 */
@RestController("strategyPlanController")
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
@Tag(name = "规划管理", description = "战略侧规划主要接口。这是规划的权威入口点。")
public class PlanController {

    private final PlanApplicationService planApplicationService;
    private final StrategyApplicationService strategyApplicationService;

    @GetMapping
    @Operation(summary = "分页获取所有规划", description = "战略侧规划查询的主要入口点。")
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
    @Operation(summary = "根据ID获取规划")
    public ResponseEntity<ApiResponse<PlanResponse>> getPlanById(@PathVariable Long id) {
        return planApplicationService.getPlanById(id)
                .map(plan -> ResponseEntity.ok(ApiResponse.success(plan)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cycle/{cycleId}")
    @Operation(summary = "根据考核周期ID获取规划")
    public ResponseEntity<ApiResponse<java.util.List<PlanResponse>>> getPlansByCycle(@PathVariable Long cycleId) {
        java.util.List<PlanResponse> plans = planApplicationService.getPlansByCycle(cycleId);
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STRATEGY_DEPT')")
    @Operation(summary = "创建新规划", description = "战略侧创建规划批次的主要入口点。")
    public ResponseEntity<ApiResponse<PlanResponse>> createPlan(
            @Valid @RequestBody com.sism.strategy.interfaces.dto.CreatePlanRequest request) {
        PlanResponse response = planApplicationService.createPlan(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STRATEGY_DEPT')")
    @Operation(summary = "更新规划")
    public ResponseEntity<ApiResponse<PlanResponse>> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody com.sism.strategy.interfaces.dto.UpdatePlanRequest request) {
        PlanResponse response = planApplicationService.updatePlan(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除规划")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable Long id) {
        planApplicationService.deletePlan(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','STRATEGY_DEPT')")
    @Operation(summary = "发布规划")
    public ResponseEntity<ApiResponse<PlanResponse>> publishPlan(@PathVariable Long id) {
        PlanResponse response = planApplicationService.publishPlan(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN','STRATEGY_DEPT')")
    @Operation(summary = "归档规划")
    public ResponseEntity<ApiResponse<PlanResponse>> archivePlan(@PathVariable Long id) {
        PlanResponse response = planApplicationService.archivePlan(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN','STRATEGY_DEPT')")
    @Operation(summary = "提交规划审批")
    public ResponseEntity<ApiResponse<PlanResponse>> submitPlanForApproval(
            @PathVariable Long id,
            @Valid @RequestBody SubmitPlanApprovalRequest request,
            @AuthenticationPrincipal CurrentUser currentUser) {
        PlanResponse response = planApplicationService.submitPlanForApproval(
                id,
                request,
                currentUser.getId(),
                currentUser.getOrgId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/submit-dispatch")
    @PreAuthorize("hasAnyRole('ADMIN','STRATEGY_DEPT')")
    @Operation(summary = "提交规划分发审批")
    public ResponseEntity<ApiResponse<PlanResponse>> submitPlanForDispatchApproval(
            @PathVariable Long id,
            @Valid @RequestBody SubmitPlanApprovalRequest request,
            @AuthenticationPrincipal CurrentUser currentUser) {
        return submitPlanForApproval(id, request, currentUser);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER')")
    @Operation(summary = "审批通过规划")
    public ResponseEntity<ApiResponse<PlanResponse>> approvePlan(@PathVariable Long id) {
        PlanResponse response = planApplicationService.approvePlan(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER')")
    @Operation(summary = "拒绝规划")
    public ResponseEntity<ApiResponse<PlanResponse>> rejectPlan(@PathVariable Long id) {
        PlanResponse response = planApplicationService.rejectPlan(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasAnyRole('ADMIN','STRATEGY_DEPT')")
    @Operation(summary = "撤回规划至草稿")
    public ResponseEntity<ApiResponse<PlanResponse>> withdrawPlan(@PathVariable Long id) {
        PlanResponse response = planApplicationService.withdrawPlan(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id:[0-9]+}/details")
    @Operation(summary = "获取规划详情(含指标和里程碑)")
    public ResponseEntity<ApiResponse<PlanApplicationService.PlanDetailsResponse>> getPlanDetails(@PathVariable Long id) {
        PlanApplicationService.PlanDetailsResponse response = planApplicationService.getPlanDetails(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "根据任务ID获取规划")
    public ResponseEntity<ApiResponse<PlanResponse>> getPlanByTaskId(@PathVariable Long taskId) {
        // 通过 taskId 查找关联的 plan
        return planApplicationService.getPlanByTaskId(taskId)
                .map(plan -> ResponseEntity.ok(ApiResponse.success(plan)))
                .orElse(ResponseEntity.ok(ApiResponse.error(404, "Plan not found for task: " + taskId)));
    }
}
