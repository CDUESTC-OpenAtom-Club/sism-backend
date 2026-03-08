package com.sism.service;

import com.sism.dto.PlanCreateRequest;
import com.sism.dto.PlanUpdateRequest;
import com.sism.entity.Plan;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.PlanRepository;
import com.sism.repository.SysOrgRepository;
import com.sism.vo.PlanVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for plan management
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

    private final PlanRepository planRepository;
    private final SysOrgRepository orgRepository;
    private final AuditInstanceService auditInstanceService;

    /**
     * Get all plans
     */
    public List<PlanVO> getAllPlans() {
        return planRepository.findAll().stream()
                .filter(plan -> plan.getIsDeleted() == null || !plan.getIsDeleted())
                .map(this::toPlanVO)
                .collect(Collectors.toList());
    }

    /**
     * Get plan by ID
     */
    public PlanVO getPlanById(Long id) {
        Plan plan = findPlanById(id);
        return toPlanVO(plan);
    }

    /**
     * Get plans by cycle ID
     */
    public List<PlanVO> getPlansByCycleId(Long cycleId) {
        return planRepository.findByCycleId(cycleId).stream()
                .filter(plan -> plan.getIsDeleted() == null || !plan.getIsDeleted())
                .map(this::toPlanVO)
                .collect(Collectors.toList());
    }

    /**
     * Get plans by target organization ID
     */
    public List<PlanVO> getPlansByTargetOrgId(Long targetOrgId) {
        return planRepository.findByTargetOrgId(targetOrgId).stream()
                .filter(plan -> plan.getIsDeleted() == null || !plan.getIsDeleted())
                .map(this::toPlanVO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new plan
     */
    @Transactional
    public PlanVO createPlan(PlanCreateRequest request) {
        log.info("Creating plan for cycle: {} and target org: {}", request.getCycleId(), request.getTargetOrgId());

        // Validate organizations exist
        orgRepository.findById(request.getTargetOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Target Organization", request.getTargetOrgId()));
        orgRepository.findById(request.getCreatedByOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Created By Organization", request.getCreatedByOrgId()));

        Plan plan = Plan.builder()
                .cycleId(request.getCycleId())
                .targetOrgId(request.getTargetOrgId())
                .createdByOrgId(request.getCreatedByOrgId())
                .planLevel(request.getPlanLevel())
                .status(request.getStatus())
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Plan savedPlan = planRepository.save(plan);
        log.info("Successfully created plan with ID: {}", savedPlan.getId());

        return toPlanVO(savedPlan);
    }

    /**
     * Update an existing plan
     */
    @Transactional
    public PlanVO updatePlan(Long id, PlanUpdateRequest request) {
        Plan plan = findPlanById(id);

        if (request.getTargetOrgId() != null) {
            orgRepository.findById(request.getTargetOrgId())
                    .orElseThrow(() -> new ResourceNotFoundException("Target Organization", request.getTargetOrgId()));
            plan.setTargetOrgId(request.getTargetOrgId());
        }
        if (request.getPlanLevel() != null) {
            plan.setPlanLevel(request.getPlanLevel());
        }
        if (request.getStatus() != null) {
            plan.setStatus(request.getStatus());
        }

        plan.setUpdatedAt(LocalDateTime.now());
        Plan updatedPlan = planRepository.save(plan);

        return toPlanVO(updatedPlan);
    }

    /**
     * Delete a plan (soft delete)
     */
    @Transactional
    public void deletePlan(Long id) {
        Plan plan = findPlanById(id);
        plan.setIsDeleted(true);
        plan.setUpdatedAt(LocalDateTime.now());
        planRepository.save(plan);
        log.info("Soft deleted plan with ID: {}", id);
    }

    /**
     * Approve a plan
     */
    @Transactional
    public PlanVO approvePlan(Long id) {
        Plan plan = findPlanById(id);
        plan.setStatus("APPROVED");
        plan.setUpdatedAt(LocalDateTime.now());
        Plan approvedPlan = planRepository.save(plan);
        log.info("Approved plan with ID: {}", id);
        return toPlanVO(approvedPlan);
    }

    /**
     * Submit a plan for approval workflow
     * 
     * @param planId The plan ID to submit
     * @param userId The user ID who is submitting
     * @return The updated plan VO
     */
    @Transactional
    public PlanVO submitPlanForApproval(Long planId, Long userId) {
        log.info("Submitting plan for approval: planId={}, userId={}", planId, userId);

        Plan plan = findPlanById(planId);

        // Validate status: must be DRAFT to submit
        if (!"DRAFT".equals(plan.getStatus())) {
            throw new IllegalStateException("Only DRAFT plans can be submitted for approval. Current status: " + plan.getStatus());
        }

        // Determine which flow to use based on target organization type
        String flowCode = determineFlowCode(plan);
        log.debug("Using flow code: {} for plan: {}", flowCode, planId);

        // Create audit instance and start workflow
        try {
            auditInstanceService.createAuditInstance(
                flowCode,
                com.sism.enums.AuditEntityType.PLAN,
                planId,
                userId
            );

            // Update plan status to IN_REVIEW
            plan.setStatus("IN_REVIEW");
            plan.setUpdatedAt(LocalDateTime.now());
            Plan updatedPlan = planRepository.save(plan);

            log.info("Successfully submitted plan {} for approval", planId);
            return toPlanVO(updatedPlan);

        } catch (Exception e) {
            log.error("Failed to submit plan for approval: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to submit plan for approval: " + e.getMessage(), e);
        }
    }

    /**
     * Find plan entity by ID
     */
    private Plan findPlanById(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", id));
        if (plan.getIsDeleted() != null && plan.getIsDeleted()) {
            throw new ResourceNotFoundException("Plan", id);
        }
        return plan;
    }

    /**
     * Convert Plan entity to VO
     */
    private PlanVO toPlanVO(Plan plan) {
        return new PlanVO(
            plan.getId(),
            plan.getCycleId(),
            plan.getTargetOrgId(),
            plan.getCreatedByOrgId(),
            plan.getPlanLevel(),
            plan.getStatus(),
            plan.getCreatedAt(),
            plan.getUpdatedAt(),
            plan.getIsDeleted()
        );
    }

    /**
     * Determine which approval flow to use based on plan's target organization
     * 
     * @param plan The plan entity
     * @return The flow code to use
     */
    private String determineFlowCode(Plan plan) {
        // Get target organization
        var targetOrg = orgRepository.findById(plan.getTargetOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Target Organization", plan.getTargetOrgId()));

        // Determine flow based on organization type
        String orgType = targetOrg.getType().name();

        return switch (orgType) {
            case "STRATEGY_DEPT" -> "PLAN_DISPATCH_STRATEGY";
            case "FUNCTIONAL_DEPT" -> "PLAN_DISPATCH_FUNCDEPT";
            case "COLLEGE" -> "PLAN_DISPATCH_FUNCDEPT"; // Use same flow as functional dept
            default -> {
                log.warn("Unknown org type: {}, defaulting to PLAN_DISPATCH_FUNCDEPT", orgType);
                yield "PLAN_DISPATCH_FUNCDEPT";
            }
        };
    }


    /**
     * Get count of pending plan approvals for a user
     *
     * @param userId The user ID to check pending approvals for
     * @return Count of plans pending approval
     */
    public Long getPendingApprovalCount(Long userId) {
        log.info("PlanService.getPendingApprovalCount called for userId: {}", userId);
        
        if (auditInstanceService == null) {
            log.error("CRITICAL: auditInstanceService is NULL! Dependency injection failed!");
            return 0L;
        }
        
        try {
            log.debug("Calling auditInstanceService.getPendingApprovalsForUser({})", userId);
            
            // Get all audit instances where user is a pending approver
            List<com.sism.entity.AuditInstance> pendingInstances = 
                auditInstanceService.getPendingApprovalsForUser(userId);
            
            log.debug("Retrieved {} pending audit instances for user {}", 
                     pendingInstances != null ? pendingInstances.size() : 0, userId);

            // Filter for PLAN entity type only
            long count = pendingInstances.stream()
                    .filter(instance -> {
                        boolean isPlan = instance.getEntityType() == com.sism.enums.AuditEntityType.PLAN;
                        log.trace("Instance {}: entityType={}, isPlan={}", 
                                 instance.getId(), instance.getEntityType(), isPlan);
                        return isPlan;
                    })
                    .count();

            log.info("Found {} pending plan approvals for user {}", count, userId);
            return count;

        } catch (Exception e) {
            log.error("Error getting pending approval count for user {}: {}", 
                     userId, e.getMessage(), e);
            return 0L;
        }
    }

}
