package com.sism.strategy.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.common.PageResult;
import com.sism.strategy.application.MilestoneApplicationService;
import com.sism.strategy.interfaces.dto.BatchSaveMilestonesRequest;
import com.sism.strategy.interfaces.dto.MilestoneResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Primary planning entrypoint for milestone operations.
 */
@RestController
@RequestMapping("/api/v1/milestones")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "里程碑", description = "战略侧里程碑主要接口。这是规划的权威入口点。")
public class MilestoneController {

    private final MilestoneApplicationService milestoneApplicationService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取里程碑")
    public ResponseEntity<ApiResponse<MilestoneResponse>> getMilestoneById(@PathVariable Long id) {
        return milestoneApplicationService.getMilestoneById(id)
                .map(milestone -> ResponseEntity.ok(ApiResponse.success(milestone)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/plan/{planId}")
    @Operation(summary = "根据计划ID获取里程碑")
    public ResponseEntity<ApiResponse<java.util.List<MilestoneResponse>>> getMilestonesByPlan(@PathVariable Long planId) {
        java.util.List<MilestoneResponse> milestones = milestoneApplicationService.getMilestonesByPlanId(planId);
        return ResponseEntity.ok(ApiResponse.success(milestones));
    }

    @GetMapping("/indicator/{indicatorId}")
    @Operation(summary = "根据指标ID获取里程碑")
    public ResponseEntity<ApiResponse<java.util.List<MilestoneResponse>>> getMilestonesByIndicatorId(@PathVariable Long indicatorId) {
        java.util.List<MilestoneResponse> milestones = milestoneApplicationService.getMilestonesByIndicatorId(indicatorId);
        return ResponseEntity.ok(ApiResponse.success(milestones));
    }

    @GetMapping("/by-indicators")
    @Operation(summary = "批量获取多个指标ID的里程碑")
    public ResponseEntity<ApiResponse<java.util.Map<Long, java.util.List<MilestoneResponse>>>> getMilestonesByIndicatorIds(
            @RequestParam("ids") java.util.List<Long> indicatorIds) {
        java.util.Map<Long, java.util.List<MilestoneResponse>> result = milestoneApplicationService.getMilestonesByIndicatorIds(indicatorIds);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STRATEGY_DEPT')")
    @Operation(summary = "创建新里程碑", description = "战略侧里程碑管理的主要入口点。")
    public ResponseEntity<ApiResponse<MilestoneResponse>> createMilestone(
            @Valid @RequestBody com.sism.strategy.interfaces.dto.CreateMilestoneRequest request) {
        MilestoneResponse response = milestoneApplicationService.createMilestone(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STRATEGY_DEPT')")
    @Operation(summary = "更新里程碑")
    public ResponseEntity<ApiResponse<MilestoneResponse>> updateMilestone(
            @PathVariable Long id,
            @Valid @RequestBody com.sism.strategy.interfaces.dto.UpdateMilestoneRequest request) {
        MilestoneResponse response = milestoneApplicationService.updateMilestone(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/indicator/{indicatorId}/batch")
    @PreAuthorize("hasAnyRole('ADMIN','STRATEGY_DEPT')")
    @Operation(summary = "为指标批量保存里程碑")
    public ResponseEntity<ApiResponse<java.util.List<MilestoneResponse>>> saveMilestones(
            @PathVariable Long indicatorId,
            @Valid @RequestBody BatchSaveMilestonesRequest request) {
        java.util.List<MilestoneResponse> response =
                milestoneApplicationService.saveMilestones(indicatorId, request.getMilestones());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STRATEGY_DEPT')")
    @Operation(summary = "删除里程碑")
    public ResponseEntity<ApiResponse<Void>> deleteMilestone(@PathVariable Long id) {
        milestoneApplicationService.deleteMilestone(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping
    @Operation(summary = "分页获取所有里程碑")
    public ResponseEntity<ApiResponse<PageResult<MilestoneResponse>>> listMilestones(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long indicatorId,
            @RequestParam(required = false) String status) {
        Page<MilestoneResponse> resultPage = milestoneApplicationService.getMilestones(page, size, indicatorId, status);
        PageResult<MilestoneResponse> pageResult = PageResult.of(
                resultPage.getContent(),
                (int) resultPage.getTotalElements(),
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(pageResult));
    }
}
