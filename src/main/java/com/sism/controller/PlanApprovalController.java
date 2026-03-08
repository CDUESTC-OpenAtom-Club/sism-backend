package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.dto.PlanApprovalRequest;
import com.sism.dto.PlanRejectionRequest;
import com.sism.entity.AuditInstance;
import com.sism.entity.SysUser;
import com.sism.exception.BusinessException;
import com.sism.repository.UserRepository;
import com.sism.service.ApprovalService;
import com.sism.service.AuditInstanceService;
import com.sism.vo.PlanVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Plan approval workflow operations
 */
@Slf4j
@RestController
@RequestMapping("/api/plans/approval")
@RequiredArgsConstructor
@Tag(name = "Plan Approval", description = "Plan approval workflow management")
public class PlanApprovalController {

    private final ApprovalService approvalService;
    private final AuditInstanceService auditInstanceService;
    private final com.sism.service.PlanService planService;
    private final UserRepository userRepository;

    /**
     * Submit a plan for approval
     * 
     * @param planId The plan ID
     * @param userId The user ID submitting the plan
     * @return The updated plan
     */
    @PostMapping("/{planId}/submit")
    @Operation(summary = "Submit plan for approval", description = "Submit a plan to start the approval workflow")
    public ResponseEntity<ApiResponse<PlanVO>> submitPlanForApproval(
            @PathVariable Long planId,
            @RequestParam Long userId) {
        
        log.info("Submitting plan for approval: planId={}, userId={}", planId, userId);
        
        PlanVO plan = planService.submitPlanForApproval(planId, userId);
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    /**
     * Approve a plan at the current step
     * 
     * @param instanceId The audit instance ID
     * @param request Request body containing optional comment
     * @return Success response
     */
    @PostMapping("/instances/{instanceId}/approve")
    @Operation(summary = "Approve plan", description = "Approve a plan at the current approval step")
    public ResponseEntity<ApiResponse<String>> approvePlan(
            @PathVariable Long instanceId,
            @Valid @RequestBody PlanApprovalRequest request) {
        
        Long approverId = getCurrentUserId();
        log.info("Approving plan: instanceId={}, approverId={}", instanceId, approverId);
        
        approvalService.approvePlan(instanceId, approverId, request.getComment());
        return ResponseEntity.ok(ApiResponse.success("Plan approved successfully"));
    }

    /**
     * Reject a plan at the current step
     * 
     * @param instanceId The audit instance ID
     * @param request Request body containing reason
     * @return Success response
     */
    @PostMapping("/instances/{instanceId}/reject")
    @Operation(summary = "Reject plan", description = "Reject a plan at the current approval step")
    public ResponseEntity<ApiResponse<String>> rejectPlan(
            @PathVariable Long instanceId,
            @Valid @RequestBody PlanRejectionRequest request) {
        
        Long approverId = getCurrentUserId();
        log.info("Rejecting plan: instanceId={}, approverId={}", instanceId, approverId);
        
        approvalService.rejectPlan(instanceId, approverId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Plan rejected successfully"));
    }

    /**
     * Get pending plan approvals for a user
     * 
     * @param userId The user ID
     * @return List of pending approval instances
     */
    @GetMapping("/pending")
    @Operation(summary = "Get pending approvals", description = "Get all pending plan approvals for a user")
    public ResponseEntity<ApiResponse<List<AuditInstance>>> getPendingApprovals(
            @RequestParam Long userId) {
        
        log.info("Getting pending approvals for user: {}", userId);
        
        List<AuditInstance> instances = approvalService.getPendingPlanApprovalsForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(instances));
    }

    /**
     * Get approval status for a plan
     * 
     * @param planId The plan ID
     * @return The active audit instance if exists
     */
    @GetMapping("/plans/{planId}/status")
    @Operation(summary = "Get plan approval status", description = "Get the current approval status for a plan")
    public ResponseEntity<ApiResponse<AuditInstance>> getPlanApprovalStatus(
            @PathVariable Long planId) {
        
        log.info("Getting approval status for plan: {}", planId);
        
        return approvalService.getPlanApprovalStatus(planId)
                .map(instance -> ResponseEntity.ok(ApiResponse.success(instance)))
                .orElse(ResponseEntity.ok(ApiResponse.success(null)));
    }

    /**
     * Count pending approvals for a user
     * 
     * @param userId The user ID
     * @return Count of pending approvals
     */
    @GetMapping("/pending/count")
    @Operation(summary = "Count pending approvals", description = "Count pending plan approvals for a user")
    public ResponseEntity<ApiResponse<Long>> countPendingApprovals(
            @RequestParam Long userId) {
        
        log.info("Counting pending approvals for user: {}", userId);
        
        long count = approvalService.countPendingPlanApprovalsForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * Get current step description for an instance
     * 
     * @param instanceId The audit instance ID
     * @return Step description
     */
    @GetMapping("/instances/{instanceId}/current-step")
    @Operation(summary = "Get current step", description = "Get the current approval step description")
    public ResponseEntity<ApiResponse<String>> getCurrentStep(
            @PathVariable Long instanceId) {
        
        log.info("Getting current step for instance: {}", instanceId);
        
        AuditInstance instance = auditInstanceService.getAuditInstanceById(instanceId);
        String stepDescription = auditInstanceService.getCurrentStepDescription(instance);
        
        return ResponseEntity.ok(ApiResponse.success(stepDescription));
    }

    /**
     * Extract current user ID from security context
     * 
     * @return The current user's ID
     * @throws BusinessException if user is not authenticated
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof String username) {
                    return userRepository.findByUsername(username)
                            .map(SysUser::getId)
                            .orElseThrow(() -> new BusinessException("User not found"));
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get current user ID: {}", e.getMessage(), e);
        }
        throw new BusinessException("User not authenticated");
    }
}
