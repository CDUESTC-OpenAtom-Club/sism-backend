package com.sism.strategy.application;

import com.sism.strategy.domain.enums.IndicatorStatus;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.domain.repository.OrganizationRepository;
import com.sism.strategy.domain.Cycle;
import com.sism.strategy.domain.Indicator;
import com.sism.strategy.domain.event.PlanSubmittedForApprovalEvent;
import com.sism.strategy.domain.plan.Plan;
import com.sism.strategy.domain.plan.PlanLevel;
import com.sism.strategy.domain.plan.PlanStatus;
import com.sism.strategy.domain.repository.CycleRepository;
import com.sism.strategy.domain.repository.IndicatorRepository;
import com.sism.strategy.domain.repository.PlanRepository;
import com.sism.strategy.interfaces.dto.CreatePlanRequest;
import com.sism.strategy.interfaces.dto.PlanResponse;
import com.sism.strategy.interfaces.dto.SubmitPlanApprovalRequest;
import com.sism.strategy.interfaces.dto.UpdatePlanRequest;
import com.sism.task.domain.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * PlanApplicationService - 计划应用服务
 * 处理计划的业务逻辑，包括计划的创建、更新、查询等操作
 */
@Service("strategyPlanApplicationService")
@RequiredArgsConstructor
@Slf4j
public class PlanApplicationService {
    private static final String PLAN_APPROVAL_WORKFLOW_CODE_FUNCDEPT = "PLAN_APPROVAL_FUNCDEPT";
    private static final String PLAN_APPROVAL_WORKFLOW_CODE_COLLEGE = "PLAN_APPROVAL_COLLEGE";


    private final PlanRepository planRepository;
    private final CycleRepository cycleRepository;
    private final IndicatorRepository indicatorRepository;
    private final OrganizationRepository organizationRepository;
    private final BasicTaskWeightValidationService basicTaskWeightValidationService;
    private final TaskRepository taskRepository;
    private final DomainEventPublisher eventPublisher;
    private final PlanWorkflowSnapshotQueryService planWorkflowSnapshotQueryService;

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
     * 发布计划（下发）
     * 同时同步所有关联指标的状态为 DISTRIBUTED
     */
    @Transactional
    public PlanResponse publishPlan(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));

        basicTaskWeightValidationService.validatePlanBasicWeight(plan.getId(), plan.getTargetOrgId());
        plan.activate();
        Plan saved = planRepository.save(plan);

        // 同步所有关联指标的状态
        syncIndicatorStatusWithPlan(saved);

        return convertToResponse(saved, null);
    }

    /**
     * 提交计划审批
     */
    @Transactional
    public PlanResponse submitPlanForApproval(Long id,
                                              SubmitPlanApprovalRequest request,
                                              Long currentUserId,
                                              Long currentOrgId) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));

        plan.submitForApproval(allowsDistributedSubmission(request));
        Plan saved = planRepository.save(plan);
        eventPublisher.publish(new PlanSubmittedForApprovalEvent(
                saved.getId(),
                request.getWorkflowCode(),
                currentUserId,
                currentOrgId
        ));
        return enrichWorkflowFields(convertToResponse(saved, null), saved);
    }

    private boolean allowsDistributedSubmission(SubmitPlanApprovalRequest request) {
        if (request == null || request.getWorkflowCode() == null) {
            return false;
        }

        return PLAN_APPROVAL_WORKFLOW_CODE_FUNCDEPT.equals(request.getWorkflowCode())
                || PLAN_APPROVAL_WORKFLOW_CODE_COLLEGE.equals(request.getWorkflowCode());
    }

    /**
     * 审批通过计划
     * 同时同步所有关联指标的状态为 DISTRIBUTED
     */
    @Transactional
    public PlanResponse approvePlan(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));

        basicTaskWeightValidationService.validatePlanBasicWeight(plan.getId(), plan.getTargetOrgId());
        plan.approve();
        Plan saved = planRepository.save(plan);

        // 同步所有关联指标的状态
        syncIndicatorStatusWithPlan(saved);

        return convertToResponse(saved, null);
    }

    /**
     * 驳回计划
     */
    @Transactional
    public PlanResponse rejectPlan(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));

        plan.returnForRevision();
        Plan saved = planRepository.save(plan);
        return convertToResponse(saved, null);
    }

    /**
     * 撤回计划到草稿
     */
    @Transactional
    public PlanResponse withdrawPlan(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));

        plan.withdraw();
        Plan saved = planRepository.save(plan);
        return convertToResponse(saved, null);
    }

    @Transactional
    public void markWorkflowApproved(Long planId) {
        planRepository.findById(planId).ifPresent(plan -> {
            basicTaskWeightValidationService.validatePlanBasicWeight(plan.getId(), plan.getTargetOrgId());
            plan.approve();
            Plan saved = planRepository.save(plan);
            syncIndicatorStatusWithPlan(saved);
        });
    }

    @Transactional
    public void markWorkflowRejected(Long planId, String reason) {
        planRepository.findById(planId).ifPresent(plan -> {
            plan.returnForRevision();
            planRepository.save(plan);
        });
    }

    @Transactional
    public void markWorkflowWithdrawn(Long planId) {
        planRepository.findById(planId).ifPresent(plan -> {
            plan.withdraw();
            planRepository.save(plan);
        });
    }

    /**
     * 归档计划
     */
    @Transactional
    public PlanResponse archivePlan(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));

        plan.setIsDeleted(true);
        Plan saved = planRepository.save(plan);
        return convertToResponse(saved, null);
    }

    /**
     * 根据ID查询计划
     */
    public Optional<PlanResponse> getPlanById(Long id) {
        Map<Long, String> orgNamesById = loadOrgNamesById();
        return planRepository.findById(id)
                .map(plan -> enrichWorkflowFields(convertToResponse(plan, null, orgNamesById), plan));
    }

    /**
     * 根据Task ID查询关联的Plan
     * Task 与 Plan 通过 sys_task.plan_id 关联。
     */
    public Optional<PlanResponse> getPlanByTaskId(Long taskId) {
        Map<Long, String> orgNamesById = loadOrgNamesById();
        return taskRepository.findById(taskId)
                .map(com.sism.task.domain.StrategicTask::getPlanId)
                .flatMap(planRepository::findById)
                .map(plan -> enrichWorkflowFields(convertToResponse(plan, null, orgNamesById), plan));
    }

    /**
     * 查询所有计划
     */
    public List<PlanResponse> getAllPlans() {
        Map<Long, String> orgNamesById = loadOrgNamesById();
        List<Plan> plans = planRepository.findAll();
        Map<Long, PlanWorkflowSnapshotQueryService.WorkflowSnapshot> workflowSnapshotsByPlanId =
                planWorkflowSnapshotQueryService.getWorkflowSnapshotsByPlanIds(
                        plans.stream().map(Plan::getId).toList()
                );
        return plans.stream()
                .map(plan -> enrichWorkflowFields(
                        convertToResponse(plan, null, orgNamesById),
                        workflowSnapshotsByPlanId.get(plan.getId())))
                .collect(Collectors.toList());
    }

    /**
     * 分页查询计划
     */
    public Page<PlanResponse> getPlans(int page, int size, Integer year, String status) {
        long startedAt = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Long> cycleIds = year == null
                ? List.of()
                : cycleRepository.findByYear(year).stream()
                .map(Cycle::getId)
                .toList();

        if (year != null && cycleIds.isEmpty()) {
            log.info(
                    "Loaded plans page={}, size={}, year={}, status={}, results=0, total=0, durationMs={}",
                    page,
                    size,
                    year,
                    status,
                    System.currentTimeMillis() - startedAt
            );
            return Page.empty(pageable);
        }

        List<String> queryStatuses = (status == null || status.isBlank())
                ? List.of()
                : PlanStatus.expandQueryStatuses(status);

        Page<Plan> planPage = planRepository.findPage(cycleIds, queryStatuses, pageable);
        Map<Long, String> orgNamesById = loadOrgNamesById();
        Map<Long, PlanWorkflowSnapshotQueryService.WorkflowSnapshot> workflowSnapshotsByPlanId =
                planWorkflowSnapshotQueryService.getWorkflowSnapshotsByPlanIds(
                        planPage.getContent().stream().map(Plan::getId).toList()
                );
        Page<PlanResponse> responsePage = planPage.map(
                plan -> enrichWorkflowFields(
                        convertToResponse(plan, null, orgNamesById),
                        workflowSnapshotsByPlanId.get(plan.getId())));

        log.info(
                "Loaded plans page={}, size={}, year={}, status={}, results={}, total={}, durationMs={}",
                page,
                size,
                year,
                status,
                responsePage.getNumberOfElements(),
                responsePage.getTotalElements(),
                System.currentTimeMillis() - startedAt
        );
        return responsePage;
    }

    /**
     * 根据周期ID查询计划
     */
    public List<PlanResponse> getPlansByCycle(Long cycleId) {
        Cycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + cycleId));

        Map<Long, String> orgNamesById = loadOrgNamesById();
        List<Plan> plans = planRepository.findByCycleId(cycleId);
        Map<Long, PlanWorkflowSnapshotQueryService.WorkflowSnapshot> workflowSnapshotsByPlanId =
                planWorkflowSnapshotQueryService.getWorkflowSnapshotsByPlanIds(
                        plans.stream().map(Plan::getId).toList()
                );
        return plans.stream()
                .map(plan -> enrichWorkflowFields(
                        convertToResponse(plan, cycle.getYear().toString(), orgNamesById),
                        workflowSnapshotsByPlanId.get(plan.getId())))
                .collect(Collectors.toList());
    }

    /**
     * 获取计划详情（包含指标和里程碑）
     */
    public PlanDetailsResponse getPlanDetails(Long id) {
        long startedAt = System.currentTimeMillis();
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));

        Cycle cycle = cycleRepository.findById(plan.getCycleId()).orElse(null);

        PlanResponse planResponse = convertToResponse(
                plan,
                cycle != null ? cycle.getYear().toString() : null,
                loadOrgNamesById()
        );
        planResponse = enrichWorkflowFields(planResponse, plan);

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
        details.setCanEdit(planResponse.getCanEdit());
        details.setCanResubmit(planResponse.getCanResubmit());
        details.setWorkflowInstanceId(planResponse.getWorkflowInstanceId());
        details.setWorkflowStatus(planResponse.getWorkflowStatus());
        details.setCurrentStepName(planResponse.getCurrentStepName());
        details.setCurrentApproverId(planResponse.getCurrentApproverId());
        details.setCurrentApproverName(planResponse.getCurrentApproverName());
        details.setCanWithdraw(planResponse.getCanWithdraw());

        // 查询计划下所有任务的指标（指标按 taskId 关联，不是按 planId 直接关联）
        String planStatus = PlanStatus.fromRaw(plan.getStatus()).value();
        List<InternalIndicatorResponse> indicators = taskRepository.findByPlanId(id).stream()
                .flatMap(task -> indicatorRepository.findByTaskId(task.getId()).stream())
                .map(indicator -> convertIndicatorToResponse(indicator, planStatus))
                .collect(Collectors.toList());
        details.setIndicators(indicators);
        details.setWorkflowHistory(planWorkflowSnapshotQueryService.getWorkflowHistoryByPlanId(plan.getId()));

        // TODO: 查询相关里程碑（需要扩展MilestoneRepository支持按planId查询）

        log.info(
                "Loaded plan details id={}, indicators={}, durationMs={}",
                id,
                indicators.size(),
                System.currentTimeMillis() - startedAt
        );
        return details;
    }

    /**
     * 确定计划层级
     */
    private PlanLevel determinePlanLevel(String planType) {
        if (planType == null) {
            return PlanLevel.STRAT_TO_FUNC;
        }

        String typeUpper = planType.toUpperCase();
        if (typeUpper.equals("OPERATION") || typeUpper.equals("OPERATIONAL")) {
            return PlanLevel.FUNC_TO_COLLEGE;
        } else if (typeUpper.equals("COMPREHENSIVE")) {
            return PlanLevel.FUNC_TO_COLLEGE;
        } else {
            return PlanLevel.STRAT_TO_FUNC;
        }
    }

    /**
     * 将Plan实体转换为响应DTO
     */
    private PlanResponse convertToResponse(Plan plan, String year) {
        return convertToResponse(plan, year, loadOrgNamesById());
    }

    private PlanResponse convertToResponse(Plan plan, String year, Map<Long, String> orgNamesById) {
        String targetOrgName = plan.getTargetOrgId() == null ? null : orgNamesById.get(plan.getTargetOrgId());

        return PlanResponse.builder()
                .id(plan.getId())
                .planName("Plan " + plan.getId()) // 计划名称需要从Plan实体获取或单独存储
                .description(null) // 需要从Plan实体获取或单独存储
                .planType(plan.getPlanLevel() != null ? plan.getPlanLevel().name() : "STRATEGY")
                .status(PlanStatus.fromRaw(plan.getStatus()).value())
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
                .targetOrgName(targetOrgName) // 设置目标组织名称
                .createdByOrgId(plan.getCreatedByOrgId())
                .planLevel(plan.getPlanLevel() != null ? plan.getPlanLevel().name() : null)
                .canEdit(plan.isEditable())
                .canResubmit(plan.isEditable())
                .workflowStatus(null)
                .currentStepName(null)
                .currentApproverId(null)
                .currentApproverName(null)
                .canWithdraw(null)
                .build();
    }

    private PlanResponse enrichWorkflowFields(PlanResponse response, Plan plan) {
        if (response == null || plan == null || plan.getId() == null) {
            return response;
        }

        PlanWorkflowSnapshotQueryService.WorkflowSnapshot workflowSnapshot =
                planWorkflowSnapshotQueryService.getWorkflowSnapshotByPlanId(plan.getId());
        return enrichWorkflowFields(response, workflowSnapshot);
    }

    private PlanResponse enrichWorkflowFields(PlanResponse response,
                                              PlanWorkflowSnapshotQueryService.WorkflowSnapshot workflowSnapshot) {
        if (response == null) {
            return response;
        }
        if (workflowSnapshot == null) {
            return response;
        }

        response.setWorkflowInstanceId(workflowSnapshot.getWorkflowInstanceId());
        response.setSubmittedBy(workflowSnapshot.getStarterId());
        response.setSubmittedByName(workflowSnapshot.getStarterName());
        response.setSubmittedAt(workflowSnapshot.getStartedAt());
        response.setLastRejectReason(workflowSnapshot.getLastRejectReason());
        response.setWorkflowStatus(workflowSnapshot.getWorkflowStatus());
        response.setCurrentStepName(workflowSnapshot.getCurrentStepName());
        response.setCurrentApproverId(workflowSnapshot.getCurrentApproverId());
        response.setCurrentApproverName(workflowSnapshot.getCurrentApproverName());
        response.setCanWithdraw(workflowSnapshot.getCanWithdraw());
        return response;
    }

    private Map<Long, String> loadOrgNamesById() {
        return organizationRepository.findAll().stream()
                .collect(Collectors.toMap(SysOrg::getId, SysOrg::getOrgName, (existing, replacement) -> existing));
    }

    /**
     * 将Indicator实体转换为响应DTO
     * 指标状态统一使用 Plan 的状态
     */
    private InternalIndicatorResponse convertIndicatorToResponse(Indicator indicator, String planStatus) {
        // 使用 Plan 的状态作为指标状态
        String effectiveStatus = planStatus != null ? planStatus :
                (indicator.getStatus() != null ? indicator.getStatus().name() : "DRAFT");

        return InternalIndicatorResponse.builder()
                .id(indicator.getId())
                .indicatorName(indicator.getName())
                .indicatorCode("IND" + indicator.getId())
                .indicatorDesc(indicator.getDescription())
                .cycleId(indicator.getTaskId()) // 使用taskId作为cycleId（临时方案）
                .ownerOrgId(indicator.getOwnerOrg() != null ? indicator.getOwnerOrg().getId() : null)
                .targetOrgId(indicator.getTargetOrg() != null ? indicator.getTargetOrg().getId() : null)
                .weightPercent(indicator.getWeight())
                .status(effectiveStatus)
                .progress(indicator.getProgress())
                .createdAt(indicator.getCreatedAt())
                .updatedAt(indicator.getUpdatedAt())
                .build();
    }

    /**
     * 将Indicator实体转换为响应DTO（兼容旧方法，用于非Plan关联场景）
     */
    private InternalIndicatorResponse convertIndicatorToResponse(Indicator indicator) {
        return convertIndicatorToResponse(indicator, null);
    }

    /**
     * 同步指标状态与 Plan 状态
     * 当 Plan 状态变更时，统一更新所有关联指标的状态
     */
    @Transactional
    public void syncIndicatorStatusWithPlan(Plan plan) {
        // 获取 Plan 对应的状态
        IndicatorStatus targetStatus = mapPlanStatusToIndicatorStatus(plan.getStatus());

        // 查找计划下所有任务关联的指标（指标按 taskId 关联）
        List<Indicator> indicators = taskRepository.findByPlanId(plan.getId()).stream()
                .flatMap(task -> indicatorRepository.findByTaskId(task.getId()).stream())
                .collect(Collectors.toList());

        // 更新所有指标的状态
        for (Indicator indicator : indicators) {
            indicator.setStatus(targetStatus);
            indicatorRepository.save(indicator);
        }
    }

    /**
     * 将 Plan 状态映射为 Indicator 状态
     */
    private IndicatorStatus mapPlanStatusToIndicatorStatus(String planStatus) {
        return switch (PlanStatus.fromRaw(planStatus)) {
            case DISTRIBUTED -> IndicatorStatus.DISTRIBUTED;
            case PENDING, DRAFT, RETURNED -> IndicatorStatus.DRAFT;
        };
    }

    /**
     * 计划详情响应DTO
     */
    @lombok.Data
    public static class PlanDetailsResponse extends PlanResponse {
        private List<InternalIndicatorResponse> indicators;
        private List<InternalMilestoneResponse> milestones;
        private List<PlanWorkflowSnapshotQueryService.WorkflowHistoryItem> workflowHistory;
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
