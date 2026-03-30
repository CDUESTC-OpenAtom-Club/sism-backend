package com.sism.workflow.application.query;

import com.sism.iam.domain.repository.UserRepository;
import com.sism.organization.domain.repository.OrganizationRepository;
import com.sism.strategy.domain.plan.Plan;
import com.sism.strategy.domain.repository.PlanRepository;
import com.sism.workflow.application.definition.WorkflowDefinitionQueryService;
import com.sism.workflow.application.support.ApproverResolver;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.definition.model.AuditStepDef;
import com.sism.workflow.domain.query.repository.WorkflowQueryRepository;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.AuditStepInstance;
import com.sism.workflow.domain.runtime.repository.AuditInstanceRepository;
import com.sism.workflow.interfaces.dto.PageResult;
import com.sism.workflow.interfaces.dto.WorkflowHistoryCardResponse;
import com.sism.workflow.interfaces.dto.WorkflowHistoryResponse;
import com.sism.workflow.interfaces.dto.WorkflowInstanceDetailResponse;
import com.sism.workflow.interfaces.dto.WorkflowInstanceResponse;
import com.sism.workflow.interfaces.dto.WorkflowTaskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowReadModelService {

    private static final String PLAN_ENTITY_TYPE = "PLAN";
    private static final String PLAN_REPORT_ENTITY_TYPE = "PLAN_REPORT";
    private static final String LEGACY_PLAN_REPORT_ENTITY_TYPE = "PlanReport";

    private final WorkflowDefinitionQueryService workflowDefinitionQueryService;
    private final AuditInstanceRepository auditInstanceRepository;
    private final WorkflowQueryRepository workflowQueryRepository;
    private final WorkflowReadModelMapper workflowReadModelMapper;
    private final ApproverResolver approverResolver;
    private final PlanRepository planRepository;
    private final JdbcTemplate jdbcTemplate;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public PageResult<com.sism.workflow.interfaces.dto.WorkflowDefinitionResponse> listDefinitions(int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        Page<AuditFlowDef> page = workflowDefinitionQueryService.getAllAuditFlowDefs(pageable);
        List<com.sism.workflow.interfaces.dto.WorkflowDefinitionResponse> items = page.getContent().stream()
                .map(workflowReadModelMapper::toDefinitionResponse)
                .toList();
        return PageResult.of(items, page.getTotalElements(), pageNum, pageSize);
    }

    public PageResult<WorkflowInstanceResponse> listInstances(String definitionId, int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        Page<AuditInstance> page = auditInstanceRepository.findByFlowDefId(Long.parseLong(definitionId), pageable);
        List<WorkflowInstanceResponse> items = page.getContent().stream()
                .map(this::buildInstanceSummary)
                .toList();
        return PageResult.of(items, page.getTotalElements(), pageNum, pageSize);
    }

    public WorkflowInstanceDetailResponse getInstanceDetail(String instanceId) {
        AuditInstance instance = auditInstanceRepository.findById(Long.parseLong(instanceId))
                .orElseThrow(() -> new IllegalArgumentException("Workflow instance not found: " + instanceId));
        return buildInstanceDetail(instance);
    }

    public WorkflowInstanceDetailResponse getInstanceDetailByBusiness(String entityType, Long entityId) {
        return findLatestCurrentInstance(entityType, entityId)
                .map(this::buildInstanceDetail)
                .orElse(null);
    }

    public List<WorkflowHistoryCardResponse> listInstanceHistoryByBusiness(String entityType, Long entityId) {
        List<AuditInstance> qualified = findInstancesByBusiness(entityType, entityId).stream()
                .filter(this::isHistoryQualified)
                .sorted(Comparator
                        .comparing(AuditInstance::getStartedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AuditInstance::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        Map<Long, Integer> roundNoByInstanceId = new HashMap<>();
        for (int index = 0; index < qualified.size(); index++) {
            roundNoByInstanceId.put(qualified.get(index).getId(), index + 1);
        }

        return qualified.stream()
                .sorted(Comparator
                        .comparing(AuditInstance::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AuditInstance::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(instance -> buildHistoryCard(instance, roundNoByInstanceId.getOrDefault(instance.getId(), 0)))
                .toList();
    }

    public PageResult<WorkflowTaskResponse> getMyPendingTasks(Long userId, int pageNum) {
        int pageSize = 10;
        List<AuditInstance> pendingInstances = auditInstanceRepository.findByStatus(AuditInstance.STATUS_PENDING);

        List<WorkflowTaskResponse> tasks = pendingInstances.stream()
                .flatMap(instance -> instance.getStepInstances().stream()
                        .filter(step -> AuditInstance.STEP_STATUS_PENDING.equals(step.getStatus()))
                        .filter(step -> canUserHandleStep(instance, step, userId))
                        .map(step -> enrichTaskResponse(instance, step)))
                .sorted(Comparator.comparing(WorkflowTaskResponse::getCreatedTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, tasks.size());
        List<WorkflowTaskResponse> pagedTasks = start < tasks.size() ? tasks.subList(start, end) : List.of();
        return PageResult.of(pagedTasks, tasks.size(), pageNum, pageSize);
    }

    public PageResult<WorkflowInstanceResponse> getMyApprovedInstances(Long userId, int pageNum, int pageSize) {
        List<WorkflowInstanceResponse> items = workflowQueryRepository.findApprovedAuditInstancesByUserId(userId).stream()
                .map(this::buildInstanceSummary)
                .sorted(Comparator
                        .comparing(WorkflowInstanceResponse::getEndTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(WorkflowInstanceResponse::getStartTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(WorkflowInstanceResponse::getInstanceId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return paginateInstanceResponses(items, pageNum, pageSize);
    }

    public PageResult<WorkflowInstanceResponse> getMyAppliedInstances(Long userId, int pageNum, int pageSize) {
        List<WorkflowInstanceResponse> items = workflowQueryRepository.findAppliedAuditInstancesByUserId(userId).stream()
                .map(this::buildInstanceSummary)
                .sorted(Comparator
                        .comparing(WorkflowInstanceResponse::getStartTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(WorkflowInstanceResponse::getInstanceId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return paginateInstanceResponses(items, pageNum, pageSize);
    }

    private WorkflowInstanceDetailResponse buildInstanceDetail(AuditInstance instance) {
        Map<Long, String> userNameCache = new HashMap<>();
        Map<Long, String> orgNameCache = new HashMap<>();
        Map<Long, AuditFlowDef> flowDefCache = new HashMap<>();
        Map<String, WorkflowBusinessContext> businessContextCache = new HashMap<>();

        WorkflowInstanceResponse summary = buildInstanceSummary(
                instance,
                userNameCache,
                orgNameCache,
                flowDefCache,
                businessContextCache
        );
        WorkflowInstanceDetailResponse response = new WorkflowInstanceDetailResponse();
        copySummary(summary, response);

        AuditFlowDef flowDef = resolveFlowDef(instance.getFlowDefId(), flowDefCache);
        Map<Long, AuditStepDef> stepDefById = flowDef == null || flowDef.getSteps() == null
                ? Map.of()
                : flowDef.getSteps().stream()
                .filter(step -> step.getId() != null)
                .collect(Collectors.toMap(AuditStepDef::getId, step -> step, (left, right) -> left, LinkedHashMap::new));

        List<WorkflowTaskResponse> tasks = instance.getStepInstances().stream()
                .sorted(Comparator.comparing(step -> step.getStepNo() == null ? Integer.MAX_VALUE : step.getStepNo()))
                .map(step -> enrichStepDetail(instance, step, stepDefById.get(step.getStepDefId()), userNameCache, orgNameCache))
                .toList();
        response.setTasks(tasks);
        response.setHistory(buildHistory(instance, userNameCache));
        return response;
    }

    private WorkflowInstanceResponse buildInstanceSummary(AuditInstance instance) {
        return buildInstanceSummary(instance, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    private WorkflowInstanceResponse buildInstanceSummary(
            AuditInstance instance,
            Map<Long, String> userNameCache,
            Map<Long, String> orgNameCache,
            Map<Long, AuditFlowDef> flowDefCache,
            Map<String, WorkflowBusinessContext> businessContextCache) {
        WorkflowInstanceResponse response = workflowReadModelMapper.toInstanceResponse(instance);
        WorkflowBusinessContext context = resolveBusinessContext(instance, orgNameCache, businessContextCache);
        AuditFlowDef flowDef = resolveFlowDef(instance.getFlowDefId(), flowDefCache);

        response.setEntityType(toExternalEntityType(instance.getEntityType()));
        response.setEntityId(instance.getEntityId());
        response.setBusinessEntityId(instance.getEntityId());
        response.setFlowCode(flowDef != null ? flowDef.getFlowCode() : null);
        response.setFlowName(flowDef != null ? flowDef.getFlowName() : null);
        response.setStarterName(resolveUserName(instance.getRequesterId(), userNameCache));
        response.setPlanId(context.planId());
        response.setPlanName(context.planName());
        response.setSourceOrgId(context.sourceOrgId());
        response.setSourceOrgName(context.sourceOrgName());
        response.setTargetOrgId(context.targetOrgId());
        response.setTargetOrgName(context.targetOrgName());
        response.setCurrentApproverName(resolveUserName(response.getCurrentApproverId(), userNameCache));
        return response;
    }

    private WorkflowTaskResponse enrichTaskResponse(AuditInstance instance, AuditStepInstance step) {
        WorkflowTaskResponse response = workflowReadModelMapper.toPendingTaskResponse(instance, step);
        Map<Long, String> orgNameCache = new HashMap<>();
        Map<Long, String> userNameCache = new HashMap<>();
        Map<Long, AuditFlowDef> flowDefCache = new HashMap<>();
        Map<String, WorkflowBusinessContext> businessContextCache = new HashMap<>();
        WorkflowBusinessContext context = resolveBusinessContext(instance, orgNameCache, businessContextCache);
        AuditFlowDef flowDef = resolveFlowDef(instance.getFlowDefId(), flowDefCache);
        response.setEntityType(toExternalEntityType(instance.getEntityType()));
        response.setEntityId(instance.getEntityId());
        response.setPlanId(context.planId());
        response.setPlanName(context.planName());
        response.setFlowCode(flowDef != null ? flowDef.getFlowCode() : null);
        response.setFlowName(flowDef != null ? flowDef.getFlowName() : null);
        response.setSourceOrgId(context.sourceOrgId());
        response.setSourceOrgName(context.sourceOrgName());
        response.setTargetOrgId(context.targetOrgId());
        response.setTargetOrgName(context.targetOrgName());
        response.setCurrentStepName(step.getStepName());
        response.setApproverOrgName(resolveOrgName(step.getApproverOrgId(), orgNameCache));
        return response;
    }

    private WorkflowTaskResponse enrichStepDetail(
            AuditInstance instance,
            AuditStepInstance step,
            AuditStepDef stepDef,
            Map<Long, String> userNameCache,
            Map<Long, String> orgNameCache) {
        WorkflowTaskResponse response = workflowReadModelMapper.toTaskResponse(step);
        response.setInstanceId(instance.getId() != null ? instance.getId().toString() : null);
        response.setEntityType(toExternalEntityType(instance.getEntityType()));
        response.setEntityId(instance.getEntityId());
        response.setCurrentStepName(step.getStepName());
        response.setAssigneeName(resolveUserName(step.getApproverId(), userNameCache));
        response.setApproverOrgName(resolveOrgName(step.getApproverOrgId(), orgNameCache));
        response.setStepType(stepDef != null ? stepDef.getStepType() : null);
        return response;
    }

    private List<WorkflowHistoryResponse> buildHistory(AuditInstance instance, Map<Long, String> userNameCache) {
        List<WorkflowHistoryResponse> history = new ArrayList<>();

        for (AuditStepInstance step : instance.getStepInstances()) {
            if (AuditInstance.STEP_STATUS_APPROVED.equals(step.getStatus())) {
                WorkflowHistoryResponse item = workflowReadModelMapper.toHistoryResponse(instance, step, "APPROVE");
                item.setOperatorName(resolveUserName(step.getApproverId(), userNameCache));
                history.add(item);
            } else if (AuditInstance.STEP_STATUS_REJECTED.equals(step.getStatus())) {
                WorkflowHistoryResponse item = workflowReadModelMapper.toHistoryResponse(instance, step, "REJECT");
                item.setOperatorName(resolveUserName(step.getApproverId(), userNameCache));
                history.add(item);
            } else if (AuditInstance.STEP_STATUS_WITHDRAWN.equals(step.getStatus())) {
                WorkflowHistoryResponse item = workflowReadModelMapper.toHistoryResponse(instance, step, "WITHDRAW");
                item.setOperatorName(resolveUserName(step.getApproverId(), userNameCache));
                history.add(item);
            }
        }

        return history;
    }

    private WorkflowHistoryCardResponse buildHistoryCard(AuditInstance instance, int roundNo) {
        Map<Long, String> userNameCache = new HashMap<>();
        Map<Long, String> orgNameCache = new HashMap<>();
        Map<Long, AuditFlowDef> flowDefCache = new HashMap<>();
        Map<String, WorkflowBusinessContext> businessContextCache = new HashMap<>();
        WorkflowBusinessContext context = resolveBusinessContext(instance, orgNameCache, businessContextCache);
        AuditFlowDef flowDef = resolveFlowDef(instance.getFlowDefId(), flowDefCache);
        return WorkflowHistoryCardResponse.builder()
                .instanceId(instance.getId() != null ? instance.getId().toString() : null)
                .instanceNo(buildInstanceNo(toExternalEntityType(instance.getEntityType()), instance.getEntityId(), roundNo))
                .roundNo(roundNo)
                .entityType(toExternalEntityType(instance.getEntityType()))
                .entityId(instance.getEntityId())
                .planId(context.planId())
                .planName(context.planName())
                .flowCode(flowDef != null ? flowDef.getFlowCode() : null)
                .flowName(flowDef != null ? flowDef.getFlowName() : null)
                .sourceOrgId(context.sourceOrgId())
                .sourceOrgName(context.sourceOrgName())
                .targetOrgId(context.targetOrgId())
                .targetOrgName(context.targetOrgName())
                .status(instance.getStatus())
                .startedAt(instance.getStartedAt())
                .completedAt(instance.getCompletedAt())
                .requesterId(instance.getRequesterId())
                .requesterName(resolveUserName(instance.getRequesterId(), userNameCache))
                .build();
    }

    private Optional<AuditInstance> findLatestCurrentInstance(String entityType, Long entityId) {
        return findInstancesByBusiness(entityType, entityId).stream()
                .max(Comparator
                        .comparing(AuditInstance::getStartedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AuditInstance::getId, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private PageResult<WorkflowInstanceResponse> paginateInstanceResponses(
            List<WorkflowInstanceResponse> items, int pageNum, int pageSize) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        int start = (safePageNum - 1) * safePageSize;
        int end = Math.min(start + safePageSize, items.size());
        List<WorkflowInstanceResponse> pagedItems = start < items.size() ? items.subList(start, end) : List.of();
        return PageResult.of(pagedItems, items.size(), safePageNum, safePageSize);
    }

    private List<AuditInstance> findInstancesByBusiness(String entityType, Long entityId) {
        if (entityId == null || entityType == null || entityType.isBlank()) {
            return List.of();
        }
        return normalizeEntityTypes(entityType).stream()
                .flatMap(type -> auditInstanceRepository.findByBusinessTypeAndBusinessId(type, entityId).stream())
                .distinct()
                .toList();
    }

    private boolean isHistoryQualified(AuditInstance instance) {
        if (instance == null || !AuditInstance.STATUS_APPROVED.equalsIgnoreCase(instance.getStatus())) {
            return false;
        }
        AuditFlowDef flowDef = resolveFlowDef(instance.getFlowDefId());
        if (flowDef == null || flowDef.getSteps() == null) {
            return false;
        }

        Set<Long> terminalStepIds = flowDef.getSteps().stream()
                .filter(step -> Boolean.TRUE.equals(step.getIsTerminal()))
                .map(AuditStepDef::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (terminalStepIds.isEmpty()) {
            return false;
        }

        return instance.getStepInstances().stream()
                .anyMatch(step -> terminalStepIds.contains(step.getStepDefId())
                        && AuditInstance.STEP_STATUS_APPROVED.equalsIgnoreCase(step.getStatus()));
    }

    private WorkflowBusinessContext resolveBusinessContext(
            AuditInstance instance,
            Map<Long, String> orgNameCache,
            Map<String, WorkflowBusinessContext> businessContextCache) {
        if (instance == null) {
            return WorkflowBusinessContext.empty();
        }

        String cacheKey = String.join(":",
                String.valueOf(instance.getEntityType()),
                String.valueOf(instance.getEntityId()));
        WorkflowBusinessContext cached = businessContextCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        WorkflowBusinessContext resolved;
        if (PLAN_ENTITY_TYPE.equalsIgnoreCase(instance.getEntityType())) {
            resolved = planRepository.findById(instance.getEntityId())
                    .map(this::buildPlanContext)
                    .orElseGet(() -> new WorkflowBusinessContext(
                            instance.getEntityId(),
                            "Plan " + instance.getEntityId(),
                            null,
                            null,
                            null,
                            null,
                            null
                    ));
            businessContextCache.put(cacheKey, resolved);
            return resolved;
        }

        if (isPlanReportEntityType(instance.getEntityType())) {
            resolved = buildPlanReportContext(instance.getEntityId());
            businessContextCache.put(cacheKey, resolved);
            return resolved;
        }

        resolved = WorkflowBusinessContext.empty();
        businessContextCache.put(cacheKey, resolved);
        return resolved;
    }

    private WorkflowBusinessContext buildPlanContext(Plan plan) {
        Long sourceOrgId = plan.getCreatedByOrgId();
        Long targetOrgId = plan.getTargetOrgId();
        return new WorkflowBusinessContext(
                plan.getId(),
                "Plan " + plan.getId(),
                sourceOrgId,
                resolveOrgName(sourceOrgId),
                targetOrgId,
                resolveOrgName(targetOrgId),
                null
        );
    }

    private WorkflowBusinessContext buildPlanReportContext(Long reportId) {
        Long sourceOrgId = null;
        String sourceOrgName = null;
        Long targetOrgId = null;
        String targetOrgName = null;
        Map<String, Object> reportRow = jdbcTemplate.query(
                """
                SELECT plan_id, report_org_id, report_month
                FROM public.plan_report
                WHERE id = ?
                """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    return Map.of(
                            "plan_id", rs.getObject("plan_id"),
                            "report_org_id", rs.getObject("report_org_id"),
                            "report_month", rs.getString("report_month")
                    );
                },
                reportId
        );
        Long planId = reportRow == null ? null : toLong(reportRow.get("plan_id"));
        Long reportOrgId = reportRow == null ? null : toLong(reportRow.get("report_org_id"));
        String reportMonth = reportRow == null ? null : (String) reportRow.get("report_month");
        String planName = planId == null ? null : "Plan " + planId;

        if (planId != null) {
            Optional<Plan> plan = planRepository.findById(planId);
            if (plan.isPresent()) {
                sourceOrgId = plan.get().getCreatedByOrgId();
                sourceOrgName = resolveOrgName(sourceOrgId);
                targetOrgId = plan.get().getTargetOrgId();
                targetOrgName = resolveOrgName(targetOrgId);
            }
        }

        String reportOrgName = resolveOrgName(reportOrgId);
        String displayName = (reportMonth == null ? "" : reportMonth + " ")
                + (reportOrgName == null ? "月报" : reportOrgName + "月报");

        return new WorkflowBusinessContext(
                planId,
                displayName.trim(),
                sourceOrgId,
                sourceOrgName,
                targetOrgId,
                targetOrgName,
                displayName.trim()
        );
    }

    private AuditFlowDef resolveFlowDef(Long flowDefId) {
        return resolveFlowDef(flowDefId, new HashMap<>());
    }

    private AuditFlowDef resolveFlowDef(Long flowDefId, Map<Long, AuditFlowDef> flowDefCache) {
        if (flowDefId == null) {
            return null;
        }
        if (flowDefCache.containsKey(flowDefId)) {
            return flowDefCache.get(flowDefId);
        }
        AuditFlowDef flowDef = workflowDefinitionQueryService.getAuditFlowDefById(flowDefId);
        flowDefCache.put(flowDefId, flowDef);
        return flowDef;
    }

    private boolean canUserHandleStep(AuditInstance instance, AuditStepInstance step, Long userId) {
        AuditFlowDef flowDef = workflowDefinitionQueryService.getAuditFlowDefById(instance.getFlowDefId());
        if (flowDef == null || flowDef.getSteps() == null) {
            return false;
        }

        AuditStepDef stepDef = flowDef.getSteps().stream()
                .filter(candidate -> candidate.getId() != null && candidate.getId().equals(step.getStepDefId()))
                .findFirst()
                .orElse(null);
        return approverResolver.canUserApprove(stepDef, userId, instance.getRequesterOrgId(), instance);
    }

    private String resolveUserName(Long userId) {
        return resolveUserName(userId, new HashMap<>());
    }

    private String resolveUserName(Long userId, Map<Long, String> userNameCache) {
        if (userId == null) {
            return null;
        }
        if (userNameCache.containsKey(userId)) {
            return userNameCache.get(userId);
        }
        String userName = userRepository.findById(userId)
                .map(user -> user.getRealName() != null && !user.getRealName().isBlank()
                        ? user.getRealName()
                        : (user.getUsername() == null || user.getUsername().isBlank() ? "User#" + userId : user.getUsername()))
                .orElse("User#" + userId);
        userNameCache.put(userId, userName);
        return userName;
    }

    private String resolveOrgName(Long orgId) {
        return resolveOrgName(orgId, new HashMap<>());
    }

    private String resolveOrgName(Long orgId, Map<Long, String> orgNameCache) {
        if (orgId == null) {
            return null;
        }
        if (orgNameCache.containsKey(orgId)) {
            return orgNameCache.get(orgId);
        }
        String orgName = organizationRepository.findById(orgId)
                .map(org -> org.getName() != null && !org.getName().isBlank() ? org.getName() : "Org#" + orgId)
                .orElse("Org#" + orgId);
        orgNameCache.put(orgId, orgName);
        return orgName;
    }

    private List<String> normalizeEntityTypes(String entityType) {
        if (isPlanReportEntityType(entityType)) {
            return List.of(PLAN_REPORT_ENTITY_TYPE, LEGACY_PLAN_REPORT_ENTITY_TYPE);
        }
        return List.of(entityType.toUpperCase());
    }

    private String toExternalEntityType(String entityType) {
        return isPlanReportEntityType(entityType) ? PLAN_REPORT_ENTITY_TYPE : entityType;
    }

    private boolean isPlanReportEntityType(String entityType) {
        return PLAN_REPORT_ENTITY_TYPE.equalsIgnoreCase(entityType)
                || LEGACY_PLAN_REPORT_ENTITY_TYPE.equalsIgnoreCase(entityType);
    }

    private String buildInstanceNo(String entityType, Long entityId, int roundNo) {
        return "%s-%s-%03d".formatted(entityType, entityId, Math.max(roundNo, 0));
    }

    private void copySummary(WorkflowInstanceResponse source, WorkflowInstanceDetailResponse target) {
        target.setInstanceId(source.getInstanceId());
        target.setDefinitionId(source.getDefinitionId());
        target.setStatus(source.getStatus());
        target.setEntityType(source.getEntityType());
        target.setEntityId(source.getEntityId());
        target.setBusinessEntityId(source.getBusinessEntityId());
        target.setFlowCode(source.getFlowCode());
        target.setFlowName(source.getFlowName());
        target.setStarterId(source.getStarterId());
        target.setStarterName(source.getStarterName());
        target.setPlanId(source.getPlanId());
        target.setPlanName(source.getPlanName());
        target.setSourceOrgId(source.getSourceOrgId());
        target.setSourceOrgName(source.getSourceOrgName());
        target.setTargetOrgId(source.getTargetOrgId());
        target.setTargetOrgName(source.getTargetOrgName());
        target.setStartTime(source.getStartTime());
        target.setEndTime(source.getEndTime());
        target.setCurrentTaskId(source.getCurrentTaskId());
        target.setCurrentStepName(source.getCurrentStepName());
        target.setCurrentApproverId(source.getCurrentApproverId());
        target.setCurrentApproverName(source.getCurrentApproverName());
        target.setCanWithdraw(source.getCanWithdraw());
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record WorkflowBusinessContext(
            Long planId,
            String planName,
            Long sourceOrgId,
            String sourceOrgName,
            Long targetOrgId,
            String targetOrgName,
            String displayName
    ) {
        private static WorkflowBusinessContext empty() {
            return new WorkflowBusinessContext(null, null, null, null, null, null, null);
        }
    }
}
