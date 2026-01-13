package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.dto.IndicatorCreateRequest;
import com.sism.dto.IndicatorUpdateRequest;
import com.sism.service.IndicatorService;
import com.sism.vo.IndicatorVO;
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
 * Indicator Controller
 * Provides CRUD operations for indicators with soft deletion
 * 
 * Requirements: 2.2, 2.3, 2.4, 2.5
 */
@Slf4j
@RestController
@RequestMapping("/api/indicators")
@RequiredArgsConstructor
@Tag(name = "Indicators", description = "Indicator management endpoints")
public class IndicatorController {

    private final IndicatorService indicatorService;

    /**
     * Get all active indicators
     * GET /api/indicators
     */
    @GetMapping
    @Operation(summary = "Get all indicators", description = "Retrieve all active indicators")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> getAllIndicators() {
        List<IndicatorVO> indicators = indicatorService.getAllActiveIndicators();
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    /**
     * Get indicator by ID
     * GET /api/indicators/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get indicator by ID", description = "Retrieve a specific indicator with children and milestones")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Indicator found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Indicator not found")
    })
    public ResponseEntity<ApiResponse<IndicatorVO>> getIndicatorById(
            @Parameter(description = "Indicator ID") @PathVariable Long id) {
        IndicatorVO indicator = indicatorService.getIndicatorById(id);
        return ResponseEntity.ok(ApiResponse.success(indicator));
    }

    /**
     * Get indicators by task ID
     * GET /api/indicators/task/{taskId}
     */
    @GetMapping("/task/{taskId}")
    @Operation(summary = "Get indicators by task", description = "Retrieve all indicators for a specific task")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> getIndicatorsByTaskId(
            @Parameter(description = "Task ID") @PathVariable Long taskId) {
        List<IndicatorVO> indicators = indicatorService.getIndicatorsByTaskId(taskId);
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    /**
     * Get root indicators by task ID (no parent)
     * GET /api/indicators/task/{taskId}/root
     */
    @GetMapping("/task/{taskId}/root")
    @Operation(summary = "Get root indicators", description = "Retrieve root indicators (no parent) for a task")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> getRootIndicatorsByTaskId(
            @Parameter(description = "Task ID") @PathVariable Long taskId) {
        List<IndicatorVO> indicators = indicatorService.getRootIndicatorsByTaskId(taskId);
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    /**
     * Get indicators by owner organization ID
     * GET /api/indicators/owner/{ownerOrgId}
     */
    @GetMapping("/owner/{ownerOrgId}")
    @Operation(summary = "Get indicators by owner org", description = "Retrieve indicators owned by an organization")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> getIndicatorsByOwnerOrgId(
            @Parameter(description = "Owner organization ID") @PathVariable Long ownerOrgId) {
        List<IndicatorVO> indicators = indicatorService.getIndicatorsByOwnerOrgId(ownerOrgId);
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    /**
     * Get indicators by target organization ID
     * GET /api/indicators/target/{targetOrgId}
     */
    @GetMapping("/target/{targetOrgId}")
    @Operation(summary = "Get indicators by target org", description = "Retrieve indicators targeting an organization")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> getIndicatorsByTargetOrgId(
            @Parameter(description = "Target organization ID") @PathVariable Long targetOrgId) {
        List<IndicatorVO> indicators = indicatorService.getIndicatorsByTargetOrgId(targetOrgId);
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    /**
     * Get indicators by target organization hierarchy
     * GET /api/indicators/target/{orgId}/hierarchy
     */
    @GetMapping("/target/{orgId}/hierarchy")
    @Operation(summary = "Get indicators by org hierarchy", 
               description = "Retrieve indicators where target org matches or is a descendant")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> getIndicatorsByTargetOrgHierarchy(
            @Parameter(description = "Organization ID") @PathVariable Long orgId) {
        List<IndicatorVO> indicators = indicatorService.getIndicatorsByTargetOrgHierarchy(orgId);
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    /**
     * Search indicators by description keyword
     * GET /api/indicators/search?keyword=xxx
     */
    @GetMapping("/search")
    @Operation(summary = "Search indicators", description = "Search indicators by description keyword")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> searchIndicators(
            @Parameter(description = "Search keyword") @RequestParam String keyword) {
        List<IndicatorVO> indicators = indicatorService.searchIndicators(keyword);
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    /**
     * Create a new indicator
     * POST /api/indicators
     * Requirements: 2.3
     */
    @PostMapping
    @Operation(summary = "Create indicator", description = "Create a new indicator")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Indicator created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ApiResponse<IndicatorVO>> createIndicator(
            @Valid @RequestBody IndicatorCreateRequest request) {
        log.info("Creating indicator: {}", request.getIndicatorDesc());
        IndicatorVO indicator = indicatorService.createIndicator(request);
        return ResponseEntity.ok(ApiResponse.success("Indicator created successfully", indicator));
    }

    /**
     * Update an existing indicator
     * PUT /api/indicators/{id}
     * Requirements: 2.4
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update indicator", description = "Update an existing indicator")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Indicator updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Indicator not found")
    })
    public ResponseEntity<ApiResponse<IndicatorVO>> updateIndicator(
            @Parameter(description = "Indicator ID") @PathVariable Long id,
            @Valid @RequestBody IndicatorUpdateRequest request) {
        log.info("Updating indicator: {}", id);
        IndicatorVO indicator = indicatorService.updateIndicator(id, request);
        return ResponseEntity.ok(ApiResponse.success("Indicator updated successfully", indicator));
    }

    /**
     * Delete (archive) an indicator
     * DELETE /api/indicators/{id}
     * Requirements: 2.5 - Soft deletion
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete indicator", description = "Soft delete (archive) an indicator")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Indicator deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Indicator not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteIndicator(
            @Parameter(description = "Indicator ID") @PathVariable Long id) {
        log.info("Deleting (archiving) indicator: {}", id);
        indicatorService.deleteIndicator(id);
        return ResponseEntity.ok(ApiResponse.success("Indicator archived successfully", null));
    }

    // ==================== Indicator Distribution APIs (指标下发) ====================

    /**
     * Distribute an indicator to a target organization
     * POST /api/indicators/{id}/distribute
     */
    @PostMapping("/{id}/distribute")
    @Operation(summary = "Distribute indicator", 
               description = "Distribute an indicator to a target organization, creating a child indicator with inherited milestones")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Indicator distributed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot distribute this indicator"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Indicator or organization not found")
    })
    public ResponseEntity<ApiResponse<IndicatorVO>> distributeIndicator(
            @Parameter(description = "Parent indicator ID") @PathVariable Long id,
            @Parameter(description = "Target organization ID") @RequestParam Long targetOrgId,
            @Parameter(description = "Custom description (optional)") @RequestParam(required = false) String customDesc,
            @Parameter(description = "Actor user ID") @RequestParam(required = false) Long actorUserId) {
        log.info("Distributing indicator {} to org {}", id, targetOrgId);
        IndicatorVO indicator = indicatorService.distributeIndicator(id, targetOrgId, customDesc, actorUserId);
        return ResponseEntity.ok(ApiResponse.success("指标下发成功", indicator));
    }

    /**
     * Batch distribute an indicator to multiple organizations
     * POST /api/indicators/{id}/distribute/batch
     */
    @PostMapping("/{id}/distribute/batch")
    @Operation(summary = "Batch distribute indicator", 
               description = "Distribute an indicator to multiple target organizations")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> batchDistributeIndicator(
            @Parameter(description = "Parent indicator ID") @PathVariable Long id,
            @Parameter(description = "Target organization IDs") @RequestBody List<Long> targetOrgIds,
            @Parameter(description = "Actor user ID") @RequestParam(required = false) Long actorUserId) {
        log.info("Batch distributing indicator {} to {} orgs", id, targetOrgIds.size());
        List<IndicatorVO> indicators = indicatorService.batchDistributeIndicator(id, targetOrgIds, actorUserId);
        return ResponseEntity.ok(ApiResponse.success("批量下发成功，共创建 " + indicators.size() + " 个子指标", indicators));
    }

    /**
     * Get all distributed (child) indicators from a parent
     * GET /api/indicators/{id}/distributed
     */
    @GetMapping("/{id}/distributed")
    @Operation(summary = "Get distributed indicators", 
               description = "Get all child indicators distributed from a parent indicator")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> getDistributedIndicators(
            @Parameter(description = "Parent indicator ID") @PathVariable Long id) {
        List<IndicatorVO> indicators = indicatorService.getDistributedIndicators(id);
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    /**
     * Check if an indicator can be distributed
     * GET /api/indicators/{id}/distribution-eligibility
     */
    @GetMapping("/{id}/distribution-eligibility")
    @Operation(summary = "Check distribution eligibility", 
               description = "Check if an indicator can be distributed to child organizations")
    public ResponseEntity<ApiResponse<IndicatorService.DistributionEligibility>> checkDistributionEligibility(
            @Parameter(description = "Indicator ID") @PathVariable Long id) {
        IndicatorService.DistributionEligibility eligibility = indicatorService.checkDistributionEligibility(id);
        return ResponseEntity.ok(ApiResponse.success(eligibility));
    }
}
