package com.sism.execution.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.common.PageResult;
import com.sism.execution.application.MilestoneApplicationService;
import com.sism.execution.domain.model.milestone.Milestone;
import com.sism.execution.interfaces.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MilestoneController - 里程碑API控制器
 * 提供里程碑管理相关的REST API端点
 */
@RestController("executionMilestoneController")
@RequestMapping("/api/v1/milestones")
@RequiredArgsConstructor
@Tag(name = "Milestones", description = "Milestone management endpoints")
public class MilestoneController {

    private final MilestoneApplicationService milestoneApplicationService;

    // ==================== Milestone CRUD Operations ====================

    @PostMapping
    @Operation(summary = "创建里程碑", description = "创建一个新的里程碑")
    public ResponseEntity<ApiResponse<MilestoneResponse>> createMilestone(
            @Valid @RequestBody CreateMilestoneRequest request) {
        Milestone milestone = milestoneApplicationService.createMilestone(
                request.getIndicatorId(),
                request.getMilestoneName(),
                request.getDescription(),
                request.getDueDate(),
                request.getTargetProgress(),
                request.getStatus(),
                request.getSortOrder(),
                request.getIsPaired(),
                request.getInheritedFrom()
        );
        return ResponseEntity.ok(ApiResponse.success(MilestoneResponse.fromEntity(milestone)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新里程碑", description = "更新里程碑的信息")
    public ResponseEntity<ApiResponse<MilestoneResponse>> updateMilestone(
            @Parameter(description = "里程碑ID") @PathVariable Long id,
            @Valid @RequestBody UpdateMilestoneRequest request) {
        Milestone milestone = milestoneApplicationService.updateMilestone(
                id,
                request.getIndicatorId(),
                request.getMilestoneName(),
                request.getDescription(),
                request.getDueDate(),
                request.getTargetProgress(),
                request.getStatus(),
                request.getSortOrder(),
                request.getIsPaired(),
                request.getInheritedFrom()
        );
        return ResponseEntity.ok(ApiResponse.success(MilestoneResponse.fromEntity(milestone)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除里程碑", description = "删除指定的里程碑")
    public ResponseEntity<ApiResponse<Void>> deleteMilestone(
            @Parameter(description = "里程碑ID") @PathVariable Long id) {
        milestoneApplicationService.deleteMilestone(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询里程碑详情", description = "获取指定里程碑的完整信息")
    public ResponseEntity<ApiResponse<MilestoneResponse>> getMilestoneById(
            @Parameter(description = "里程碑ID") @PathVariable Long id) {
        Milestone milestone = milestoneApplicationService.findMilestoneById(id)
                .orElseThrow(() -> new IllegalArgumentException("Milestone not found"));
        return ResponseEntity.ok(ApiResponse.success(MilestoneResponse.fromEntity(milestone)));
    }

    // ==================== Milestone Query Operations ====================

    @GetMapping
    @Operation(summary = "分页查询所有里程碑", description = "获取所有里程碑，支持分页")
    public ResponseEntity<ApiResponse<PageResult<MilestoneResponse>>> getAllMilestones(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        Page<Milestone> milestonePage = milestoneApplicationService.findAllMilestones(page, size);
        List<MilestoneResponse> responses = milestonePage.getContent().stream()
                .map(MilestoneResponse::fromEntity)
                .collect(Collectors.toList());
        PageResult<MilestoneResponse> pageResult = PageResult.of(
                responses,
                milestonePage.getTotalElements(),
                milestonePage.getNumber(),
                milestonePage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(pageResult));
    }

    @GetMapping("/list")
    @Operation(summary = "查询所有里程碑（列表）", description = "获取所有里程碑列表，不分页")
    public ResponseEntity<ApiResponse<List<MilestoneResponse>>> getAllMilestonesList() {
        List<Milestone> milestones = milestoneApplicationService.findAllMilestones();
        List<MilestoneResponse> responses = milestones.stream()
                .map(MilestoneResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/indicator/{indicatorId}")
    @Operation(summary = "根据指标ID查询里程碑", description = "获取指定指标的所有里程碑")
    public ResponseEntity<ApiResponse<List<MilestoneResponse>>> getMilestonesByIndicatorId(
            @Parameter(description = "指标ID") @PathVariable Long indicatorId) {
        List<Milestone> milestones = milestoneApplicationService.findMilestonesByIndicatorId(indicatorId);
        List<MilestoneResponse> responses = milestones.stream()
                .map(MilestoneResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "根据状态查询里程碑（列表）", description = "获取指定状态的所有里程碑列表")
    public ResponseEntity<ApiResponse<List<MilestoneResponse>>> getMilestonesByStatus(
            @Parameter(description = "里程碑状态") @PathVariable String status) {
        List<Milestone> milestones = milestoneApplicationService.findMilestonesByStatus(status);
        List<MilestoneResponse> responses = milestones.stream()
                .map(MilestoneResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/status/{status}/page")
    @Operation(summary = "根据状态分页查询里程碑", description = "获取指定状态的里程碑，支持分页")
    public ResponseEntity<ApiResponse<PageResult<MilestoneResponse>>> getMilestonesByStatusPaginated(
            @Parameter(description = "里程碑状态") @PathVariable String status,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        Page<Milestone> milestonePage = milestoneApplicationService.findMilestonesByStatus(status, page, size);
        List<MilestoneResponse> responses = milestonePage.getContent().stream()
                .map(MilestoneResponse::fromEntity)
                .collect(Collectors.toList());
        PageResult<MilestoneResponse> pageResult = PageResult.of(
                responses,
                milestonePage.getTotalElements(),
                milestonePage.getNumber(),
                milestonePage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(pageResult));
    }

    @GetMapping("/exists/{id}")
    @Operation(summary = "检查里程碑是否存在", description = "检查指定ID的里程碑是否存在")
    public ResponseEntity<ApiResponse<Boolean>> checkMilestoneExists(
            @Parameter(description = "里程碑ID") @PathVariable Long id) {
        boolean exists = milestoneApplicationService.existsById(id);
        return ResponseEntity.ok(ApiResponse.success(exists));
    }
}
