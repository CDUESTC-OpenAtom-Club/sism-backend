package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.dto.MilestoneCreateRequest;
import com.sism.dto.MilestoneUpdateRequest;
import com.sism.enums.MilestoneStatus;
import com.sism.service.MilestoneService;
import com.sism.vo.MilestoneVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Milestone Controller
 * Provides CRUD operations for milestones
 * 
 * Requirements: 5.1, 5.2, 5.3
 */
@Slf4j
@RestController
@RequestMapping("/milestones")
@RequiredArgsConstructor
@Tag(name = "Milestones", description = "Milestone management endpoints")
public class MilestoneController {

    private final MilestoneService milestoneService;

    /**
     * Get all milestones
     * GET /api/milestones
     */
    @GetMapping
    @Operation(summary = "Get all milestones", description = "Retrieve all milestones")
    public ResponseEntity<ApiResponse<List<MilestoneVO>>> getAllMilestones() {
        List<MilestoneVO> milestones = milestoneService.getAllMilestones();
        return ResponseEntity.ok(ApiResponse.success(milestones));
    }

    /**
     * Get milestone by ID
     * GET /api/milestones/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get milestone by ID", description = "Retrieve a specific milestone")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Milestone found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Milestone not found")
    })
    public ResponseEntity<ApiResponse<MilestoneVO>> getMilestoneById(
            @Parameter(description = "Milestone ID") @PathVariable Long id) {
        MilestoneVO milestone = milestoneService.getMilestoneById(id);
        return ResponseEntity.ok(ApiResponse.success(milestone));
    }

    /**
     * Get milestones by indicator ID
     * GET /api/milestones/indicator/{indicatorId}
     * Requirements: 5.3
     */
    @GetMapping("/indicator/{indicatorId}")
    @Operation(summary = "Get milestones by indicator", description = "Retrieve all milestones for a specific indicator")
    public ResponseEntity<ApiResponse<List<MilestoneVO>>> getMilestonesByIndicatorId(
            @Parameter(description = "Indicator ID") @PathVariable Long indicatorId) {
        List<MilestoneVO> milestones = milestoneService.getMilestonesByIndicatorId(indicatorId);
        return ResponseEntity.ok(ApiResponse.success(milestones));
    }

    /**
     * Get milestones by indicator ID ordered by due date
     * GET /api/milestones/indicator/{indicatorId}/by-date
     */
    @GetMapping("/indicator/{indicatorId}/by-date")
    @Operation(summary = "Get milestones by due date", description = "Retrieve milestones ordered by due date")
    public ResponseEntity<ApiResponse<List<MilestoneVO>>> getMilestonesByIndicatorIdOrderByDueDate(
            @Parameter(description = "Indicator ID") @PathVariable Long indicatorId) {
        List<MilestoneVO> milestones = milestoneService.getMilestonesByIndicatorIdOrderByDueDate(indicatorId);
        return ResponseEntity.ok(ApiResponse.success(milestones));
    }

    /**
     * Get milestones by status
     * GET /api/milestones/status/{status}
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get milestones by status", description = "Retrieve milestones with a specific status")
    public ResponseEntity<ApiResponse<List<MilestoneVO>>> getMilestonesByStatus(
            @Parameter(description = "Milestone status") @PathVariable MilestoneStatus status) {
        List<MilestoneVO> milestones = milestoneService.getMilestonesByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(milestones));
    }

    /**
     * Get overdue milestones
     * GET /api/milestones/overdue
     */
    @GetMapping("/overdue")
    @Operation(summary = "Get overdue milestones", description = "Retrieve all overdue milestones")
    public ResponseEntity<ApiResponse<List<MilestoneVO>>> getOverdueMilestones() {
        List<MilestoneVO> milestones = milestoneService.getOverdueMilestones();
        return ResponseEntity.ok(ApiResponse.success(milestones));
    }

    /**
     * Get upcoming milestones
     * GET /api/milestones/upcoming?days=7
     */
    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming milestones", description = "Retrieve milestones due within specified days")
    public ResponseEntity<ApiResponse<List<MilestoneVO>>> getUpcomingMilestones(
            @Parameter(description = "Number of days to look ahead") 
            @RequestParam(defaultValue = "7") int days) {
        List<MilestoneVO> milestones = milestoneService.getUpcomingMilestones(days);
        return ResponseEntity.ok(ApiResponse.success(milestones));
    }

    /**
     * Get weight validation for an indicator
     * GET /api/milestones/indicator/{indicatorId}/weight-validation
     * Requirements: 5.2, 5.4
     */
    @GetMapping("/indicator/{indicatorId}/weight-validation")
    @Operation(summary = "Validate milestone weights", 
               description = "Check if milestone weights sum to 100% for an indicator")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateWeights(
            @Parameter(description = "Indicator ID") @PathVariable Long indicatorId) {
        MilestoneService.WeightValidationResult result = milestoneService.validateWeightSum(indicatorId);
        Map<String, Object> response = Map.of(
            "isValid", result.isValid(),
            "actualSum", result.actualSum(),
            "expectedSum", result.expectedSum(),
            "message", result.getMessage()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get total weight for an indicator
     * GET /api/milestones/indicator/{indicatorId}/total-weight
     * Requirements: 5.1
     */
    @GetMapping("/indicator/{indicatorId}/total-weight")
    @Operation(summary = "Get total weight", description = "Calculate total weight percentage for an indicator")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalWeight(
            @Parameter(description = "Indicator ID") @PathVariable Long indicatorId) {
        BigDecimal totalWeight = milestoneService.calculateTotalWeight(indicatorId);
        return ResponseEntity.ok(ApiResponse.success(totalWeight));
    }

    /**
     * Create a new milestone
     * POST /api/milestones
     * Requirements: 5.1
     */
    @PostMapping
    @Operation(summary = "Create milestone", description = "Create a new milestone")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Milestone created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ApiResponse<MilestoneVO>> createMilestone(
            @Valid @RequestBody MilestoneCreateRequest request) {
        log.info("Creating milestone: {} for indicator: {}", request.getMilestoneName(), request.getIndicatorId());
        MilestoneVO milestone = milestoneService.createMilestone(request);
        return ResponseEntity.ok(ApiResponse.success("Milestone created successfully", milestone));
    }

    /**
     * Update an existing milestone
     * PUT /api/milestones/{id}
     * Requirements: 5.2
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update milestone", description = "Update an existing milestone")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Milestone updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Milestone not found")
    })
    public ResponseEntity<ApiResponse<MilestoneVO>> updateMilestone(
            @Parameter(description = "Milestone ID") @PathVariable Long id,
            @Valid @RequestBody MilestoneUpdateRequest request) {
        log.info("Updating milestone: {}", id);
        MilestoneVO milestone = milestoneService.updateMilestone(id, request);
        return ResponseEntity.ok(ApiResponse.success("Milestone updated successfully", milestone));
    }

    /**
     * Update milestone status
     * PATCH /api/milestones/{id}/status
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update milestone status", description = "Update the status of a milestone")
    public ResponseEntity<ApiResponse<MilestoneVO>> updateMilestoneStatus(
            @Parameter(description = "Milestone ID") @PathVariable Long id,
            @Parameter(description = "New status") @RequestParam MilestoneStatus status) {
        log.info("Updating milestone {} status to: {}", id, status);
        MilestoneVO milestone = milestoneService.updateMilestoneStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Milestone status updated", milestone));
    }

    /**
     * Delete a milestone
     * DELETE /api/milestones/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete milestone", description = "Delete a milestone")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Milestone deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Milestone not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteMilestone(
            @Parameter(description = "Milestone ID") @PathVariable Long id) {
        log.info("Deleting milestone: {}", id);
        milestoneService.deleteMilestone(id);
        return ResponseEntity.ok(ApiResponse.success("Milestone deleted successfully", null));
    }

    // ==================== Pairing Mechanism APIs (配对机制) ====================

    /**
     * Get the next milestone to report for an indicator (catch-up rule)
     * GET /api/milestones/indicator/{indicatorId}/next-to-report
     */
    @GetMapping("/indicator/{indicatorId}/next-to-report")
    @Operation(summary = "Get next milestone to report", 
               description = "Get the earliest unpaired milestone that needs to be reported (catch-up rule)")
    public ResponseEntity<ApiResponse<MilestoneVO>> getNextMilestoneToReport(
            @Parameter(description = "Indicator ID") @PathVariable Long indicatorId) {
        MilestoneVO milestone = milestoneService.getNextMilestoneToReport(indicatorId);
        if (milestone == null) {
            return ResponseEntity.ok(ApiResponse.success("All milestones have been reported", null));
        }
        return ResponseEntity.ok(ApiResponse.success(milestone));
    }

    /**
     * Get all unpaired milestones for an indicator
     * GET /api/milestones/indicator/{indicatorId}/unpaired
     */
    @GetMapping("/indicator/{indicatorId}/unpaired")
    @Operation(summary = "Get unpaired milestones", 
               description = "Get all milestones that have not been paired with an approved report")
    public ResponseEntity<ApiResponse<List<MilestoneVO>>> getUnpairedMilestones(
            @Parameter(description = "Indicator ID") @PathVariable Long indicatorId) {
        List<MilestoneVO> milestones = milestoneService.getUnpairedMilestones(indicatorId);
        return ResponseEntity.ok(ApiResponse.success(milestones));
    }

    /**
     * Check if a milestone is paired
     * GET /api/milestones/{id}/is-paired
     */
    @GetMapping("/{id}/is-paired")
    @Operation(summary = "Check if milestone is paired", 
               description = "Check if a milestone has an approved progress report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> isMilestonePaired(
            @Parameter(description = "Milestone ID") @PathVariable Long id) {
        boolean isPaired = milestoneService.isMilestonePaired(id);
        Map<String, Object> response = Map.of(
            "milestoneId", id,
            "isPaired", isPaired,
            "message", isPaired ? "里程碑已配对（有审核通过的填报记录）" : "里程碑未配对（无审核通过的填报记录）"
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get pairing status summary for an indicator
     * GET /api/milestones/indicator/{indicatorId}/pairing-status
     */
    @GetMapping("/indicator/{indicatorId}/pairing-status")
    @Operation(summary = "Get pairing status", 
               description = "Get summary of milestone pairing status for an indicator")
    public ResponseEntity<ApiResponse<MilestoneService.PairingStatusSummary>> getPairingStatus(
            @Parameter(description = "Indicator ID") @PathVariable Long indicatorId) {
        MilestoneService.PairingStatusSummary status = milestoneService.getPairingStatus(indicatorId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Check if a specific milestone can be reported on (catch-up rule validation)
     * GET /api/milestones/indicator/{indicatorId}/can-report/{milestoneId}
     */
    @GetMapping("/indicator/{indicatorId}/can-report/{milestoneId}")
    @Operation(summary = "Check if can report on milestone", 
               description = "Validate if a milestone can be reported on based on catch-up rule")
    public ResponseEntity<ApiResponse<Map<String, Object>>> canReportOnMilestone(
            @Parameter(description = "Indicator ID") @PathVariable Long indicatorId,
            @Parameter(description = "Milestone ID") @PathVariable Long milestoneId) {
        boolean canReport = milestoneService.canReportOnMilestone(indicatorId, milestoneId);
        MilestoneVO nextToReport = milestoneService.getNextMilestoneToReport(indicatorId);
        
        Map<String, Object> response = Map.of(
            "milestoneId", milestoneId,
            "canReport", canReport,
            "message", canReport ? "可以填报此里程碑" : "需要先填报更早的里程碑",
            "nextMilestoneToReport", nextToReport != null ? nextToReport : "所有里程碑已完成"
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
