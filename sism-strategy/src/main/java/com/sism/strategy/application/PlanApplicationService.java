package com.sism.strategy.application;

import com.sism.execution.domain.model.plan.Plan;
import com.sism.execution.domain.model.plan.PlanLevel;
import com.sism.execution.domain.repository.PlanRepository;
import com.sism.strategy.domain.Cycle;
import com.sism.strategy.domain.Indicator;
import com.sism.strategy.domain.repository.CycleRepository;
import com.sism.strategy.domain.repository.IndicatorRepository;
import com.sism.strategy.interfaces.dto.CreatePlanRequest;
import com.sism.strategy.interfaces.dto.PlanResponse;
import com.sism.strategy.interfaces.dto.UpdatePlanRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * PlanApplicationService - 计划应用服务
 * 处理计划的业务逻辑，包括计划的创建、更新、查询等操作
 */
@Service("strategyPlanApplicationService")
@RequiredArgsConstructor
public class PlanApplicationService {

    private final PlanRepository planRepository;
    private final CycleRepository cycleRepository;
    private final IndicatorRepository indicatorRepository;

    /**
     * 创建计划
     */
    @Transactional
    public PlanResponse createPlan(CreatePlanRequest request) {
        // 验证周期是否存在
        Cycle cycle = cycleRepository.findById(request.getCycleId())
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + request.getCycleId()));

        // 确定计划层级
        PlanLevel planLevel = determinePlanLevel(request.getPlanType());

        // 默认使用当前组织ID作为创建者和目标组织
        Long createdByOrgId = request.getCreatedByOrgId() != null
                ? request.getCreatedByOrgId()
                : 1L; // 默认值

        Long targetOrgId = request.getTargetOrgId() != null
                ? request.getTargetOrgId()
                : 1L; // 默认值

        Plan plan = Plan.create(
                request.getCycleId(),
                targetOrgId,
                createdByOrgId,
                planLevel
        );

        Plan saved = planRepository.save(plan);
        return convertToResponse(saved, cycle.getYear().toString());
    }

    /**
     * 更新计划
     */
    @Transactional
    public PlanResponse updatePlan(Long id, UpdatePlanRequest request) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));

        if (request.getTargetOrgId() != null) {
            plan.setTargetOrgId(request.getTargetOrgId());
        }

        if (request.getCreatedByOrgId() != null) {
            plan.setCreatedByOrgId(request.getCreatedByOrgId());
        }

        Plan updated = planRepository.save(plan);
        return convertToResponse(updated, null);
    }

    /**
     * 删除计划
     */
    @Transactional
    public void deletePlan(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));

        planRepository.delete(plan);
    }

    /**
     * 发布计划
     */
    @Transactional
    public PlanResponse publishPlan(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));

        plan.activate();
        Plan saved = planRepository.save(plan);
        return convertToResponse(saved, null);
    }

    /**
     * 归档计划
     */
    @Transactional
    public PlanResponse archivePlan(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));

        plan.complete();
        Plan saved = planRepository.save(plan);
        return convertToResponse(saved, null);
    }

    /**
     * 根据ID查询计划
     */
    public Optional<PlanResponse> getPlanById(Long id) {
        return planRepository.findById(id)
                .map(plan -> convertToResponse(plan, null));
    }

    /**
     * 查询所有计划
     */
    public List<PlanResponse> getAllPlans() {
        return planRepository.findAll().stream()
                .map(plan -> convertToResponse(plan, null))
                .collect(Collectors.toList());
    }

    /**
     * 分页查询计划
     */
    public Page<PlanResponse> getPlans(int page, int size, Integer year, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Plan> allPlans = planRepository.findAll();

        // 应用过滤
        List<Plan> filteredPlans = allPlans.stream()
                .filter(plan -> {
                    boolean matchYear = year == null; // 如果需要年份过滤，需要关联Cycle表
                    boolean matchStatus = status == null || status.equals(plan.getStatus());
                    return matchYear && matchStatus;
                })
                .collect(Collectors.toList());

        // 分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredPlans.size());

        if (start >= filteredPlans.size()) {
            return new PageImpl<>(List.of(), pageable, filteredPlans.size());
        }

        List<PlanResponse> pageContent = filteredPlans.subList(start, end).stream()
                .map(plan -> convertToResponse(plan, null))
                .collect(Collectors.toList());

        return new PageImpl<>(pageContent, pageable, filteredPlans.size());
    }

    /**
     * 根据周期ID查询计划
     */
    public List<PlanResponse> getPlansByCycle(Long cycleId) {
        Cycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + cycleId));

        return planRepository.findByCycleId(cycleId).stream()
                .map(plan -> convertToResponse(plan, cycle.getYear().toString()))
                .collect(Collectors.toList());
    }

    /**
     * 获取计划详情（包含指标和里程碑）
     */
    public PlanDetailsResponse getPlanDetails(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));

        Cycle cycle = cycleRepository.findById(plan.getCycleId()).orElse(null);

        PlanResponse planResponse = convertToResponse(plan, cycle != null ? cycle.getYear().toString() : null);

        PlanDetailsResponse details = new PlanDetailsResponse();
        details.setId(planResponse.getId());
        details.setPlanName(planResponse.getPlanName());
        details.setDescription(planResponse.getDescription());
        details.setPlanType(planResponse.getPlanType());
        details.setStatus(planResponse.getStatus());
        details.setStartDate(planResponse.getStartDate());
        details.setEndDate(planResponse.getEndDate());
        details.setOwnerDepartment(planResponse.getOwnerDepartment());
        details.setCompletionPercentage(planResponse.getCompletionPercentage());
        details.setIndicatorCount(planResponse.getIndicatorCount());
        details.setMilestoneCount(planResponse.getMilestoneCount());
        details.setCreateTime(planResponse.getCreateTime());
        details.setYear(planResponse.getYear());
        details.setCycleId(planResponse.getCycleId());
        details.setTargetOrgId(planResponse.getTargetOrgId());
        details.setCreatedByOrgId(planResponse.getCreatedByOrgId());
        details.setPlanLevel(planResponse.getPlanLevel());

        // 查询相关指标
        List<InternalIndicatorResponse> indicators = indicatorRepository.findAll().stream()
                .filter(indicator -> id.equals(indicator.getTaskId()))
                .map(this::convertIndicatorToResponse)
                .collect(Collectors.toList());
        details.setIndicators(indicators);

        // TODO: 查询相关里程碑（需要扩展MilestoneRepository支持按planId查询）

        return details;
    }

    /**
     * 确定计划层级
     */
    private PlanLevel determinePlanLevel(String planType) {
        if (planType == null) {
            return PlanLevel.STRATEGIC;
        }

        String typeUpper = planType.toUpperCase();
        if (typeUpper.equals("OPERATION") || typeUpper.equals("OPERATIONAL")) {
            return PlanLevel.OPERATIONAL;
        } else if (typeUpper.equals("COMPREHENSIVE")) {
            return PlanLevel.COMPREHENSIVE;
        } else {
            return PlanLevel.STRATEGIC;
        }
    }

    /**
     * 将Plan实体转换为响应DTO
     */
    private PlanResponse convertToResponse(Plan plan, String year) {
        return PlanResponse.builder()
                .id(plan.getId())
                .planName("Plan " + plan.getId()) // 计划名称需要从Plan实体获取或单独存储
                .description(null) // 需要从Plan实体获取或单独存储
                .planType(plan.getPlanLevel() != null ? plan.getPlanLevel().name() : "STRATEGY")
                .status(plan.getStatus())
                .startDate(plan.getCreatedAt())
                .endDate(plan.getUpdatedAt())
                .ownerDepartment(null) // 需要从Plan实体获取或单独存储
                .completionPercentage(0)
                .indicatorCount(0) // 需要查询关联的指标数量
                .milestoneCount(0) // 需要查询关联的里程碑数量
                .createTime(plan.getCreatedAt())
                .year(year)
                .cycleId(plan.getCycleId())
                .targetOrgId(plan.getTargetOrgId())
                .createdByOrgId(plan.getCreatedByOrgId())
                .planLevel(plan.getPlanLevel() != null ? plan.getPlanLevel().name() : null)
                .build();
    }

    /**
     * 将Indicator实体转换为响应DTO
     */
    private InternalIndicatorResponse convertIndicatorToResponse(Indicator indicator) {
        return InternalIndicatorResponse.builder()
                .id(indicator.getId())
                .indicatorName(indicator.getName())
                .indicatorCode("IND" + indicator.getId())
                .indicatorDesc(indicator.getDescription())
                .cycleId(indicator.getTaskId()) // 使用taskId作为cycleId（临时方案）
                .ownerOrgId(indicator.getOwnerOrg() != null ? indicator.getOwnerOrg().getId() : null)
                .targetOrgId(indicator.getTargetOrg() != null ? indicator.getTargetOrg().getId() : null)
                .weightPercent(indicator.getWeight())
                .status(indicator.getStatus() != null ? indicator.getStatus().name() : "DRAFT")
                .progress(indicator.getProgress())
                .createdAt(indicator.getCreatedAt())
                .updatedAt(indicator.getUpdatedAt())
                .build();
    }

    /**
     * 计划详情响应DTO
     */
    @lombok.Data
    public static class PlanDetailsResponse extends PlanResponse {
        private List<InternalIndicatorResponse> indicators;
        private List<InternalMilestoneResponse> milestones;
    }

    /**
     * 指标响应DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class InternalIndicatorResponse {
        private Long id;
        private String indicatorName;
        private String indicatorCode;
        private String indicatorDesc;
        private Long cycleId;
        private Long ownerOrgId;
        private Long targetOrgId;
        private java.math.BigDecimal weightPercent;
        private String status;
        private Integer progress;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
    }

    /**
     * 里程碑响应DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class InternalMilestoneResponse {
        private Long id;
        private String milestoneName;
        private String description;
        private java.time.LocalDateTime targetDate;
        private String status;
        private Integer priority;
        private Integer completionPercentage;
        private Long planId;
        private java.time.LocalDateTime createTime;
    }
}
