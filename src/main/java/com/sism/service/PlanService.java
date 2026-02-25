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
}
