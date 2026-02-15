package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.dto.PlanCreateRequest;
import com.sism.dto.PlanUpdateRequest;
import com.sism.service.PlanService;
import com.sism.vo.PlanVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Plan Controller for SISM (Strategic Indicator Management System).
 * 
 * <p>This controller manages strategic plans which link assessment cycles to target
 * organizations. Plans define the scope and level of strategic planning for different
 * organizational units.
 * 
 * <h2>Plan Hierarchy</h2>
 * <pre>
 * Assessment Cycle (e.g., 2024 Annual Plan)
 *   └── Plan (links cycle to organization)
 *         ├── Plan Level: STRAT_TO_FUNC or FUNC_TO_COLLEGE
 *         └── Target Organization
 * </pre>
 * 
 * <h2>Plan Levels</h2>
 * <ul>
 *   <li><b>STRAT_TO_FUNC</b>: Strategic Development Dept to Functional Dept</li>
 *   <li><b>FUNC_TO_COLLEGE</b>: Functional Dept to Secondary College</li>
 * </ul>
 * 
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Plan creation and assignment to organizations</li>
 *   <li>Plan approval workflow</li>
 *   <li>Filtering by cycle and target organization</li>
 *   <li>Soft deletion for audit trail</li>
 * </ul>
 * 
 * <h2>API Endpoints</h2>
 * <ul>
 *   <li>GET /api/plans - List all plans</li>
 *   <li>GET /api/plans/{id} - Get plan details</li>
 *   <li>GET /api/plans/cycle/{cycleId} - Filter by cycle</li>
 *   <li>GET /api/plans/target-org/{targetOrgId} - Filter by organization</li>
 *   <li>POST /api/plans - Create new plan</li>
 *   <li>PUT /api/plans/{id} - Update plan</li>
 *   <li>POST /api/plans/{id}/approve - Approve plan</li>
 *   <li>DELETE /api/plans/{id} - Soft delete plan</li>
 * </ul>
 * 
 * @author SISM Development Team
 * @version 1.0
 * @since 1.0
 * @see com.sism.service.PlanService
 * @see com.sism.entity.Plan
 */
@Slf4j
@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
@Tag(name = "Plans", description = "Plan management endpoints")
public class PlanController {

    private final PlanService planService;

    /**
     * Get all plans
     * GET /api/plans
     */
    @GetMapping
    @Operation(summary = "Get all plans", description = "Retrieve all active plans")
    public ResponseEntity<ApiResponse<List<PlanVO>>> getAllPlans() {
        List<PlanVO> plans = planService.getAllPlans();
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    /**
     * Get plan by ID
     * GET /api/plans/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get plan by ID", description = "Retrieve a specific plan")
    public ResponseEntity<ApiResponse<PlanVO>> getPlanById(
            @Parameter(description = "Plan ID") @PathVariable Long id) {
        PlanVO plan = planService.getPlanById(id);
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    /**
     * Get plans by cycle ID
     * GET /api/plans/cycle/{cycleId}
     */
    @GetMapping("/cycle/{cycleId}")
    @Operation(summary = "Get plans by cycle", description = "Retrieve all plans for a specific cycle")
    public ResponseEntity<ApiResponse<List<PlanVO>>> getPlansByCycleId(
            @Parameter(description = "Cycle ID") @PathVariable Long cycleId) {
        List<PlanVO> plans = planService.getPlansByCycleId(cycleId);
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    /**
     * Get plans by target organization ID
     * GET /api/plans/target-org/{targetOrgId}
     */
    @GetMapping("/target-org/{targetOrgId}")
    @Operation(summary = "Get plans by target org", description = "Retrieve plans for a specific target organization")
    public ResponseEntity<ApiResponse<List<PlanVO>>> getPlansByTargetOrgId(
            @Parameter(description = "Target organization ID") @PathVariable Long targetOrgId) {
        List<PlanVO> plans = planService.getPlansByTargetOrgId(targetOrgId);
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    /**
     * Create a new plan
     * POST /api/plans
     */
    @PostMapping
    @Operation(summary = "Create plan", description = "Create a new plan")
    public ResponseEntity<ApiResponse<PlanVO>> createPlan(
            @Valid @RequestBody PlanCreateRequest request) {
        log.info("Creating plan for cycle: {} and target org: {}", request.getCycleId(), request.getTargetOrgId());
        PlanVO plan = planService.createPlan(request);
        return ResponseEntity.ok(ApiResponse.success("Plan created successfully", plan));
    }

    /**
     * Update an existing plan
     * PUT /api/plans/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update plan", description = "Update an existing plan")
    public ResponseEntity<ApiResponse<PlanVO>> updatePlan(
            @Parameter(description = "Plan ID") @PathVariable Long id,
            @Valid @RequestBody PlanUpdateRequest request) {
        log.info("Updating plan: {}", id);
        PlanVO plan = planService.updatePlan(id, request);
        return ResponseEntity.ok(ApiResponse.success("Plan updated successfully", plan));
    }

    /**
     * Delete a plan (soft delete)
     * DELETE /api/plans/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete plan", description = "Soft delete a plan")
    public ResponseEntity<ApiResponse<Void>> deletePlan(
            @Parameter(description = "Plan ID") @PathVariable Long id) {
        log.info("Deleting plan: {}", id);
        planService.deletePlan(id);
        return ResponseEntity.ok(ApiResponse.success("Plan deleted successfully", null));
    }

    /**
     * Approve a plan
     * POST /api/plans/{id}/approve
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve plan", description = "Approve a plan")
    public ResponseEntity<ApiResponse<PlanVO>> approvePlan(
            @Parameter(description = "Plan ID") @PathVariable Long id) {
        log.info("Approving plan: {}", id);
        PlanVO plan = planService.approvePlan(id);
        return ResponseEntity.ok(ApiResponse.success("Plan approved successfully", plan));
    }
}
