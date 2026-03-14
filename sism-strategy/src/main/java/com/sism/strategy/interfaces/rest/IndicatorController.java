package com.sism.strategy.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.execution.domain.model.milestone.Milestone;
import com.sism.organization.domain.SysOrg;
import com.sism.strategy.application.StrategyApplicationService;
import com.sism.strategy.domain.Indicator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/indicators")
@RequiredArgsConstructor
@Tag(name = "Indicators", description = "Indicator management endpoints")
public class IndicatorController {

    private final StrategyApplicationService strategyApplicationService;

    @PostMapping
    @Operation(summary = "Create a new indicator")
    public ResponseEntity<ApiResponse<Indicator>> createIndicator(
            @RequestParam String indicatorDesc,
            @RequestParam SysOrg ownerOrg,
            @RequestParam SysOrg targetOrg) {
        Indicator created = strategyApplicationService.createIndicator(indicatorDesc, ownerOrg, targetOrg);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @GetMapping
    @Operation(summary = "Get all indicators")
    public ResponseEntity<ApiResponse<List<Indicator>>> getAllIndicators() {
        List<Indicator> indicators = strategyApplicationService.getAllIndicators();
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get indicator by ID")
    public ResponseEntity<ApiResponse<Indicator>> getIndicatorById(@PathVariable Long id) {
        Indicator indicator = strategyApplicationService.getIndicatorById(id);
        if (indicator == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Indicator not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(indicator));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit indicator for review")
    public ResponseEntity<ApiResponse<Indicator>> submitForReview(
            @PathVariable Long id,
            @RequestBody Indicator indicator) {
        Indicator submitted = strategyApplicationService.submitIndicatorForReview(indicator);
        return ResponseEntity.ok(ApiResponse.success(submitted));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve indicator")
    public ResponseEntity<ApiResponse<Indicator>> approveIndicator(
            @PathVariable Long id,
            @RequestBody Indicator indicator) {
        Indicator approved = strategyApplicationService.approveIndicator(indicator);
        return ResponseEntity.ok(ApiResponse.success(approved));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject indicator")
    public ResponseEntity<ApiResponse<Indicator>> rejectIndicator(
            @PathVariable Long id,
            @RequestBody Indicator indicator) {
        Indicator rejected = strategyApplicationService.rejectIndicator(indicator);
        return ResponseEntity.ok(ApiResponse.success(rejected));
    }

    @PostMapping("/{id}/distribute")
    @Operation(summary = "Distribute indicator to target organization")
    public ResponseEntity<ApiResponse<Indicator>> distributeIndicator(@PathVariable Long id) {
        Indicator distributed = strategyApplicationService.distributeIndicator(id);
        return ResponseEntity.ok(ApiResponse.success(distributed));
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw distributed indicator")
    public ResponseEntity<ApiResponse<Indicator>> withdrawIndicator(
            @PathVariable Long id,
            @RequestBody WithdrawRequest request) {
        Indicator withdrawn = strategyApplicationService.withdrawIndicator(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(withdrawn));
    }

    @GetMapping("/search")
    @Operation(summary = "Search indicators by keyword")
    public ResponseEntity<ApiResponse<Page<Indicator>>> searchIndicators(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Indicator> result = strategyApplicationService.searchIndicators(keyword, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "Get indicators by task ID")
    public ResponseEntity<ApiResponse<List<Indicator>>> getIndicatorsByTaskId(@PathVariable Long taskId) {
        List<Indicator> indicators = strategyApplicationService.getIndicatorsByTaskId(taskId);
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    @GetMapping("/{id}/distribution-status")
    @Operation(summary = "Get indicator distribution status")
    public ResponseEntity<ApiResponse<String>> getDistributionStatus(@PathVariable Long id) {
        Indicator indicator = strategyApplicationService.getIndicatorById(id);
        if (indicator == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Indicator not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(indicator.getDistributionStatus().toString()));
    }

    @PostMapping("/{id}/milestones")
    @Operation(summary = "Create milestones for indicator")
    public ResponseEntity<ApiResponse<CreateMilestonesResponse>> createMilestones(
            @PathVariable Long id,
            @RequestBody CreateMilestonesRequest request) {
        List<Milestone> milestones = strategyApplicationService.createMilestones(id, request);
        CreateMilestonesResponse response = new CreateMilestonesResponse();
        response.setCreatedCount(milestones.size());
        response.setMilestones(milestones);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/milestones")
    @Operation(summary = "Get milestones by indicator ID")
    public ResponseEntity<ApiResponse<List<Milestone>>> getMilestonesByIndicatorId(@PathVariable Long id) {
        List<Milestone> milestones = strategyApplicationService.getMilestonesByIndicatorId(id);
        return ResponseEntity.ok(ApiResponse.success(milestones));
    }

    @GetMapping("/milestones/{milestoneId}/is-paired")
    @Operation(summary = "Check if milestone is paired")
    public ResponseEntity<ApiResponse<Boolean>> isMilestonePaired(@PathVariable Long milestoneId) {
        boolean result = strategyApplicationService.isMilestonePaired(milestoneId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ==================== Request/Response DTOs ====================

    @lombok.Data
    public static class WithdrawRequest {
        private String reason;
    }

    @lombok.Data
    public static class CreateMilestonesRequest {
        private List<MilestoneRequest> milestones;
    }

    @lombok.Data
    public static class MilestoneRequest {
        private Integer month;
        private Integer targetProgress;
        private String deadline;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class CreateMilestonesResponse {
        private Integer createdCount;
        private List<Milestone> milestones;
    }
}
