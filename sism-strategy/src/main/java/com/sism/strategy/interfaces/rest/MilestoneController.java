package com.sism.strategy.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.common.PageResult;
import com.sism.strategy.application.MilestoneApplicationService;
import com.sism.strategy.interfaces.dto.MilestoneResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * MilestoneController - 里程碑管理控制器
 * 提供里程碑的REST API接口
 */
@RestController
@RequestMapping("/api/v1/milestones")
@RequiredArgsConstructor
@Tag(name = "Milestones", description = "Milestone management endpoints")
public class MilestoneController {

    private final MilestoneApplicationService milestoneApplicationService;

    @GetMapping("/{id}")
    @Operation(summary = "Get milestone by ID")
    public ResponseEntity<ApiResponse<MilestoneResponse>> getMilestoneById(@PathVariable Long id) {
        return milestoneApplicationService.getMilestoneById(id)
                .map(milestone -> ResponseEntity.ok(ApiResponse.success(milestone)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/plan/{planId}")
    @Operation(summary = "Get milestones by plan ID")
    public ResponseEntity<ApiResponse<java.util.List<MilestoneResponse>>> getMilestonesByPlan(@PathVariable Long planId) {
        // 注意：MilestoneApplicationService当前没有getMilestonesByPlanId方法
        // 这里返回空列表，需要在MilestoneApplicationService中添加相关方法
        java.util.List<MilestoneResponse> milestones = java.util.List.of();
        return ResponseEntity.ok(ApiResponse.success(milestones));
    }

    @GetMapping("/indicator/{indicatorId}")
    @Operation(summary = "Get milestones by indicator ID")
    public ResponseEntity<ApiResponse<java.util.List<MilestoneResponse>>> getMilestonesByIndicatorId(@PathVariable Long indicatorId) {
        java.util.List<MilestoneResponse> milestones = milestoneApplicationService.getMilestonesByIndicatorId(indicatorId);
        return ResponseEntity.ok(ApiResponse.success(milestones));
    }

    @PostMapping
    @Operation(summary = "Create a new milestone")
    public ResponseEntity<ApiResponse<MilestoneResponse>> createMilestone(
            @Valid @RequestBody com.sism.strategy.interfaces.dto.CreateMilestoneRequest request) {
        MilestoneResponse response = milestoneApplicationService.createMilestone(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a milestone")
    public ResponseEntity<ApiResponse<MilestoneResponse>> updateMilestone(
            @PathVariable Long id,
            @Valid @RequestBody com.sism.strategy.interfaces.dto.UpdateMilestoneRequest request) {
        MilestoneResponse response = milestoneApplicationService.updateMilestone(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a milestone")
    public ResponseEntity<ApiResponse<Void>> deleteMilestone(@PathVariable Long id) {
        milestoneApplicationService.deleteMilestone(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping
    @Operation(summary = "Get all milestones with pagination")
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
