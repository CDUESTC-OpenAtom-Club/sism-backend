package com.sism.strategy.application;

import com.sism.exception.ConflictException;
import com.sism.strategy.domain.enums.IndicatorStatus;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.domain.repository.OrganizationRepository;
import com.sism.strategy.domain.Cycle;
import com.sism.strategy.domain.Indicator;
import com.sism.strategy.domain.event.PlanCreatedEvent;
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
import com.sism.strategy.infrastructure.StrategyOrgProperties;
import com.sism.task.domain.StrategicTask;
import com.sism.task.domain.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.time.Duration;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * PlanApplicationService - 计划应用服务
 * 处理计划的业务逻辑，包括计划的创建、更新、查询等操作
 */
@Service("strategyPlanApplicationService")
@RequiredArgsConstructor
@Slf4j
public class PlanApplicationService {
    private static final String ROLE_CODE_APPROVER = "APPROVER";
    private static final String ROLE_CODE_STRATEGY_DEPT_HEAD = "STRATEGY_DEPT_HEAD";
    private static final String ROLE_CODE_VICE_PRESIDENT = "VICE_PRESIDENT";
    private static final Duration ORG_NAMES_CACHE_TTL = Duration.ofMinutes(1);


    private final PlanRepository planRepository;
    private final CycleRepository cycleRepository;
    private final IndicatorRepository indicatorRepository;
    private final OrganizationRepository organizationRepository;
    private final BasicTaskWeightValidationService basicTaskWeightValidationService;
    private final TaskRepository taskRepository;
    private final DomainEventPublisher eventPublisher;
    private final PlanWorkflowSnapshotQueryService planWorkflowSnapshotQueryService;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final PlatformTransactionManager transactionManager;
    private final PlanIntegrityService planIntegrityService;
    private final StrategyOrgProperties strategyOrgProperties;

    private volatile Map<Long, String> orgNamesByIdCache = Map.of();
    private volatile long orgNamesCacheUpdatedAtMillis = 0L;
    private final Object orgNamesCacheLock = new Object();

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

        assertNoActivePlanConflict(request.getCycleId(), planLevel, createdByOrgId, targetOrgId, null);

        Plan plan = Plan.create(
                request.getCycleId(),
                targetOrgId,
                createdByOrgId,
                planLevel
        );

        Plan saved = savePlanHandlingConflict(plan);
        eventPublisher.publish(new PlanCreatedEvent(
                saved.getId(),
                saved.getPlanLevel().name(),
                saved.getTargetOrgId()
        ));
        return convertToResponse(saved, cycle.getYear().toString());
    }

    /**
     * 更新计划
     */
    @Transactional
    public PlanResponse updatePlan(Long id, UpdatePlanRequest request) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));

        Long nextTargetOrgId = request.getTargetOrgId() != null ? request.getTargetOrgId() : plan.getTargetOrgId();
        Long nextCreatedByOrgId = request.getCreatedByOrgId() != null ? request.getCreatedByOrgId() : plan.getCreatedByOrgId();
        assertNoActivePlanConflict(plan.getCycleId(), plan.getPlanLevel(), nextCreatedByOrgId, nextTargetOrgId, plan.getId());

        if (request.getTargetOrgId() != null) {
            plan.setTargetOrgId(request.getTargetOrgId());
        }

        if (request.getCreatedByOrgId() != null) {
            plan.setCreatedByOrgId(request.getCreatedByOrgId());
        }

        Plan updated = savePlanHandlingConflict(plan);
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
        publishAndClearEvents(saved);

        // 同步所有关联指标的状态
        syncIndicatorStatusWithPlan(saved);

        return convertToResponse(saved, null);
    }

    /**
     * 提交计划审批
     */
    public PlanResponse submitPlanForApproval(Long id,
                                              SubmitPlanApprovalRequest request,
                                              Long currentUserId,
                                              Long currentOrgId) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        Plan saved = transactionTemplate.execute(status ->
                submitPlanForApprovalInTransaction(id, request, currentUserId, currentOrgId));
        PlanWorkflowSnapshotQueryService.WorkflowSnapshot refreshedSnapshot =
                awaitWorkflowSnapshot(saved.getId());
        return enrichWorkflowFields(convertToResponse(saved, null), refreshedSnapshot);
    }

    private Plan submitPlanForApprovalInTransaction(Long id,
                                                    SubmitPlanApprovalRequest request,
                                                    Long currentUserId,
                                                    Long currentOrgId) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));
        PlanWorkflowSnapshotQueryService.WorkflowSnapshot existingSnapshot =
                planWorkflowSnapshotQueryService.getWorkflowSnapshotByPlanId(id);

        plan.submitForApproval(allowsDistributedSubmission(request));
        Plan saved = planRepository.save(plan);
        publishAndClearEvents(saved);
        boolean resumedWithdrawnWorkflow = reactivateWithdrawnWorkflowCurrentStep(
                existingSnapshot == null ? null : existingSnapshot.getWorkflowInstanceId());
        if (!resumedWithdrawnWorkflow) {
            eventPublisher.publish(new PlanSubmittedForApprovalEvent(
                    saved.getId(),
                    request.getWorkflowCode(),
                    currentUserId,
                    currentOrgId
            ));
        }
        return saved;
    }

    private boolean allowsDistributedSubmission(SubmitPlanApprovalRequest request) {
        if (request == null || request.getWorkflowCode() == null) {
            return false;
        }

        return isApprovalWorkflowCode(request.getWorkflowCode());
    }

    private boolean isApprovalWorkflowCode(String workflowCode) {
        if (workflowCode == null) {
            return false;
        }

        String normalized = workflowCode.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("PLAN_APPROVAL_");
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
        publishAndClearEvents(saved);

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
        publishAndClearEvents(saved);
        return convertToResponse(saved, null);
    }

    /**
     * 撤回计划到草稿
     */
    @Transactional
    public PlanResponse withdrawPlan(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));

        PlanWorkflowSnapshotQueryService.WorkflowSnapshot workflowSnapshot =
                planWorkflowSnapshotQueryService.getWorkflowSnapshotByPlanId(id);
        if (workflowSnapshot == null || !Boolean.TRUE.equals(workflowSnapshot.getCanWithdraw())) {
            throw new IllegalStateException("Plan is not in a withdrawable workflow state");
        }

        withdrawWorkflowCurrentStep(workflowSnapshot.getWorkflowInstanceId());
        plan.withdraw();
        Plan saved = planRepository.save(plan);
        publishAndClearEvents(saved);
        syncIndicatorStatusWithPlan(saved);
        PlanWorkflowSnapshotQueryService.WorkflowSnapshot refreshedSnapshot =
                planWorkflowSnapshotQueryService.getWorkflowSnapshotByPlanId(id);
        return enrichWorkflowFields(convertToResponse(saved, null), refreshedSnapshot);
    }

    @Transactional
    public void markWorkflowApproved(Long planId) {
        planRepository.findById(planId).ifPresent(plan -> {
            basicTaskWeightValidationService.validatePlanBasicWeight(plan.getId(), plan.getTargetOrgId());
            plan.approve();
            Plan saved = planRepository.save(plan);
            publishAndClearEvents(saved);
            syncIndicatorStatusWithPlan(saved);
        });
    }

    @Transactional
    public void markWorkflowPending(Long planId) {
        planRepository.findById(planId).ifPresent(plan -> {
            if (!PlanStatus.PENDING.value().equals(plan.getStatus())) {
                plan.submitForApproval(true);
                Plan saved = planRepository.save(plan);
                publishAndClearEvents(saved);
            }
        });
    }

    @Transactional
    public void markWorkflowRejected(Long planId, String reason) {
        planRepository.findById(planId).ifPresent(plan -> {
            plan.returnForRevision();
            Plan saved = planRepository.save(plan);
            publishAndClearEvents(saved);
        });
    }

    @Transactional
    public void markWorkflowWithdrawn(Long planId) {
        planRepository.findById(planId).ifPresent(plan -> {
            plan.withdraw();
            Plan saved = planRepository.save(plan);
            publishAndClearEvents(saved);
            syncIndicatorStatusWithPlan(saved);
        });
    }

    private void withdrawWorkflowCurrentStep(Long workflowInstanceId) {
        if (workflowInstanceId == null) {
            return;
        }

        List<WorkflowStepRow> submitterSteps = jdbcTemplate.query(
                """
                SELECT asi.id, asi.step_no
                FROM public.audit_step_instance asi
                JOIN public.audit_instance ai ON ai.id = asi.instance_id
                LEFT JOIN public.audit_step_def asd ON asd.id = asi.step_def_id
                WHERE asi.instance_id = ?
                  AND asi.status = 'APPROVED'
                  AND (
                        COALESCE(UPPER(asd.step_type), '') = 'SUBMIT'
                        OR asi.step_name LIKE '%提交%'
                        OR (ai.requester_id IS NOT NULL AND ai.requester_id = asi.approver_id)
                  )
                ORDER BY asi.step_no DESC NULLS LAST, asi.approved_at DESC NULLS LAST, asi.id DESC
                LIMIT 1
                """,
                (rs, _rowNum) -> new WorkflowStepRow(rs.getLong(1), rs.getInt(2), null, true, null, null),
                workflowInstanceId
        );

        if (!submitterSteps.isEmpty()) {
            jdbcTemplate.update("""
                    UPDATE public.audit_step_instance
                    SET status = 'WITHDRAWN',
                        approved_at = CURRENT_TIMESTAMP,
                        comment = '提交人撤回'
                    WHERE id = ?
                    """, submitterSteps.get(0).id());
        }

        jdbcTemplate.update("""
                UPDATE public.audit_step_instance
                SET status = 'WAITING'
                WHERE instance_id = ?
                  AND status = 'PENDING'
                  AND step_no > 1
                """, workflowInstanceId);

        jdbcTemplate.update("""
                UPDATE public.audit_instance
                SET status = 'WITHDRAWN',
                    completed_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, workflowInstanceId);
    }

    private boolean reactivateWithdrawnWorkflowCurrentStep(Long workflowInstanceId) {
        if (workflowInstanceId == null) {
            return false;
        }

        List<WorkflowStepRow> withdrawnSteps = jdbcTemplate.query(
                """
                SELECT asi.id,
                       asi.step_no,
                       asi.step_def_id,
                       COALESCE(UPPER(asd.step_type), '') = 'SUBMIT' AS is_submit,
                       ai.requester_id,
                       ai.requester_org_id
                FROM public.audit_step_instance asi
                JOIN public.audit_instance ai ON ai.id = asi.instance_id
                LEFT JOIN public.audit_step_def asd ON asd.id = asi.step_def_id
                WHERE asi.instance_id = ?
                  AND asi.status = 'WITHDRAWN'
                ORDER BY asi.step_no DESC NULLS LAST, asi.id DESC
                LIMIT 1
                """,
                (rs, _rowNum) -> new WorkflowStepRow(
                        rs.getLong(1),
                        rs.getInt(2),
                        rs.getLong(3),
                        rs.getBoolean(4),
                        rs.getLong(5),
                        rs.getLong(6)
                ),
                workflowInstanceId
        );

        if (withdrawnSteps.isEmpty()) {
            return false;
        }

        WorkflowStepRow withdrawnStep = withdrawnSteps.get(0);
        WorkflowInstanceContext workflowContext = loadWorkflowInstanceContext(workflowInstanceId);
        if (workflowContext == null) {
            log.warn("Workflow instance context missing for {}, fallback to withdrawn step context", workflowInstanceId);
            workflowContext = new WorkflowInstanceContext(
                    workflowInstanceId,
                    null,
                    withdrawnStep.requesterId(),
                    withdrawnStep.requesterOrgId(),
                    "PLAN",
                    null
            );
        }
        if (withdrawnStep.isSubmitStep()) {
            jdbcTemplate.update("""
                    UPDATE public.audit_step_instance
                    SET status = 'APPROVED',
                        approved_at = CURRENT_TIMESTAMP,
                        comment = '系统自动完成提交流程节点'
                    WHERE id = ?
                    """, withdrawnStep.id());

            if (workflowContext.flowDefId() == null) {
                if (withdrawnStep.stepNo() <= 1) {
                    jdbcTemplate.update("""
                            UPDATE public.audit_step_instance
                            SET status = 'PENDING',
                                approved_at = NULL,
                                comment = NULL
                            WHERE instance_id = ?
                              AND status = 'WAITING'
                            """, workflowInstanceId);
                } else {
                    String insertSql = """
                            INSERT INTO public.audit_step_instance (
                                step_def_id,
                                instance_id,
                                step_no,
                                status,
                                created_at
                            )
                            VALUES (?, ?, %d, 'PENDING', CURRENT_TIMESTAMP)
                            """.formatted(withdrawnStep.stepNo() + 1);
                    jdbcTemplate.update(insertSql,
                            withdrawnStep.stepDefId(),
                            workflowInstanceId
                    );
                }

                jdbcTemplate.update("""
                        UPDATE public.audit_instance
                        SET status = 'IN_REVIEW',
                            completed_at = NULL,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """, workflowInstanceId);
                return true;
            }

            WorkflowStepRow waitingStep = loadFirstWaitingWorkflowStep(workflowInstanceId);
            int restoredWaitingRows = 0;
            if (waitingStep != null) {
                WorkflowStepDefinition waitingStepDef = loadWorkflowStepDefinition(waitingStep.stepDefId());
                ResolvedWorkflowApprover resolvedApprover = resolveWorkflowApprover(waitingStepDef, workflowContext);
                restoredWaitingRows = jdbcTemplate.update("""
                        UPDATE public.audit_step_instance
                        SET status = 'PENDING',
                            approver_id = ?,
                            approver_org_id = ?,
                            approved_at = NULL,
                            comment = NULL
                        WHERE id = ?
                        """,
                        resolvedApprover.approverId(),
                        resolvedApprover.approverOrgId(),
                        waitingStep.id());
            }

            if (restoredWaitingRows == 0) {
                WorkflowStepDefinition nextStepDef = loadNextWorkflowStepDefinition(
                        workflowContext.flowDefId(),
                        withdrawnStep.stepDefId()
                );
                if (nextStepDef != null) {
                    ResolvedWorkflowApprover resolvedApprover = resolveWorkflowApprover(nextStepDef, workflowContext);
                    jdbcTemplate.update("""
                            INSERT INTO public.audit_step_instance (
                                instance_id,
                                step_no,
                                step_name,
                                step_def_id,
                                status,
                                approver_id,
                                approver_org_id,
                                comment,
                                approved_at,
                                created_at
                            )
                            VALUES (?, ?, ?, ?, 'PENDING', ?, ?, NULL, NULL, CURRENT_TIMESTAMP)
                            """,
                            workflowInstanceId,
                            withdrawnStep.stepNo() + 1,
                            nextStepDef.stepName(),
                            nextStepDef.id(),
                            resolvedApprover.approverId(),
                            resolvedApprover.approverOrgId()
                    );
                }
            }
        } else {
            WorkflowStepDefinition returnedStepDef = loadWorkflowStepDefinition(withdrawnStep.stepDefId());
            ResolvedWorkflowApprover resolvedApprover = resolveWorkflowApprover(returnedStepDef, workflowContext);
            jdbcTemplate.update("""
                    UPDATE public.audit_step_instance
                    SET status = 'PENDING',
                        approver_id = ?,
                        approver_org_id = ?,
                        approved_at = NULL,
                        comment = NULL
                    WHERE id = ?
                    """,
                    resolvedApprover.approverId(),
                    resolvedApprover.approverOrgId(),
                    withdrawnStep.id());
        }
        jdbcTemplate.update("""
                UPDATE public.audit_instance
                SET status = 'IN_REVIEW',
                    completed_at = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, workflowInstanceId);
        return true;
    }

    private WorkflowInstanceContext loadWorkflowInstanceContext(Long workflowInstanceId) {
        List<WorkflowInstanceContext> rows = jdbcTemplate.query(
                """
                SELECT id, flow_def_id, requester_id, requester_org_id, entity_type, entity_id
                FROM public.audit_instance
                WHERE id = ?
                """,
                (rs, _rowNum) -> new WorkflowInstanceContext(
                        rs.getLong("id"),
                        rs.getLong("flow_def_id"),
                        rs.getLong("requester_id"),
                        rs.getLong("requester_org_id"),
                        rs.getString("entity_type"),
                        rs.getLong("entity_id")
                ),
                workflowInstanceId
        );
        if (rows.isEmpty()) {
            return null;
        }
        return rows.get(0);
    }

    private WorkflowStepRow loadFirstWaitingWorkflowStep(Long workflowInstanceId) {
        List<WorkflowStepRow> rows = jdbcTemplate.query(
                """
                SELECT id, step_no, step_def_id
                FROM public.audit_step_instance
                WHERE instance_id = ?
                  AND status = 'WAITING'
                  AND step_no > 1
                ORDER BY step_no ASC, id ASC
                LIMIT 1
                """,
                (rs, _rowNum) -> new WorkflowStepRow(
                        rs.getLong("id"),
                        rs.getInt("step_no"),
                        rs.getLong("step_def_id"),
                        false,
                        null,
                        null
                ),
                workflowInstanceId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private WorkflowStepDefinition loadWorkflowStepDefinition(Long stepDefId) {
        if (stepDefId == null) {
            return null;
        }
        List<WorkflowStepDefinition> rows = jdbcTemplate.query(
                """
                SELECT id, step_name, step_no, step_type, role_id
                FROM public.audit_step_def
                WHERE id = ?
                """,
                (rs, _rowNum) -> new WorkflowStepDefinition(
                        rs.getLong("id"),
                        rs.getString("step_name"),
                        rs.getInt("step_no"),
                        rs.getString("step_type"),
                        rs.getObject("role_id") == null ? null : rs.getLong("role_id")
                ),
                stepDefId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private WorkflowStepDefinition loadNextWorkflowStepDefinition(Long flowDefId, Long currentStepDefId) {
        List<WorkflowStepDefinition> rows = jdbcTemplate.query(
                """
                SELECT next_def.id, next_def.step_name, next_def.step_no, next_def.step_type, next_def.role_id
                FROM public.audit_step_def current_def
                JOIN public.audit_step_def next_def
                  ON next_def.flow_id = current_def.flow_id
                 AND next_def.step_no = current_def.step_no + 1
                WHERE current_def.id = ?
                  AND current_def.flow_id = ?
                LIMIT 1
                """,
                (rs, _rowNum) -> new WorkflowStepDefinition(
                        rs.getLong("id"),
                        rs.getString("step_name"),
                        rs.getInt("step_no"),
                        rs.getString("step_type"),
                        rs.getObject("role_id") == null ? null : rs.getLong("role_id")
                ),
                currentStepDefId,
                flowDefId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private ResolvedWorkflowApprover resolveWorkflowApprover(
            WorkflowStepDefinition stepDef,
            WorkflowInstanceContext context
    ) {
        if (stepDef == null) {
            return new ResolvedWorkflowApprover(null, context.requesterOrgId());
        }
        if (stepDef.isSubmitStep()) {
            return new ResolvedWorkflowApprover(context.requesterId(), context.requesterOrgId());
        }

        Long approverOrgId = resolveWorkflowApproverOrgId(stepDef, context);
        Long approverId = resolveWorkflowApproverId(stepDef, approverOrgId);
        return new ResolvedWorkflowApprover(approverId, approverOrgId);
    }

    private Long resolveWorkflowApproverOrgId(
            WorkflowStepDefinition stepDef,
            WorkflowInstanceContext context
    ) {
        String roleCode = loadRoleCode(stepDef.roleId());
        if (ROLE_CODE_STRATEGY_DEPT_HEAD.equals(roleCode)) {
            return strategyOrgProperties.getStrategyOrgId();
        }
        if (ROLE_CODE_VICE_PRESIDENT.equals(roleCode)) {
            if (stepDef.stepName() != null && stepDef.stepName().contains("学院院长")) {
                return context.requesterOrgId();
            }
            return context.requesterOrgId();
        }
        if (ROLE_CODE_APPROVER.equals(roleCode) && stepDef.stepName() != null && stepDef.stepName().contains("职能部门终审")) {
            return planRepository.findById(context.entityId())
                    .map(Plan::getCreatedByOrgId)
                    .orElse(context.requesterOrgId());
        }
        return context.requesterOrgId();
    }

    private String loadRoleCode(Long roleId) {
        if (roleId == null) {
            return null;
        }
        List<String> roleCodes = jdbcTemplate.query(
                """
                SELECT role_code
                FROM public.sys_role
                WHERE id = ?
                LIMIT 1
                """,
                (rs, _rowNum) -> rs.getString(1),
                roleId
        );
        return roleCodes.isEmpty() ? null : roleCodes.get(0);
    }

    private Long resolveWorkflowApproverId(WorkflowStepDefinition stepDef, Long approverOrgId) {
        if (stepDef.roleId() == null || approverOrgId == null) {
            return null;
        }
        List<Long> approverIds = jdbcTemplate.query(
                """
                SELECT u.id
                FROM public.sys_user u
                JOIN public.sys_user_role ur ON ur.user_id = u.id
                WHERE ur.role_id = ?
                  AND u.org_id = ?
                  AND COALESCE(u.is_active, false) = true
                ORDER BY u.id ASC
                """,
                (rs, _rowNum) -> rs.getLong(1),
                stepDef.roleId(),
                approverOrgId
        );
        return approverIds.isEmpty() ? null : approverIds.get(0);
    }

    private record WorkflowStepRow(
            Long id,
            Integer stepNo,
            Long stepDefId,
            boolean isSubmitStep,
            Long requesterId,
            Long requesterOrgId
    ) {}

    private record WorkflowInstanceContext(
            Long id,
            Long flowDefId,
            Long requesterId,
            Long requesterOrgId,
            String entityType,
            Long entityId
    ) {}

    private record ResolvedWorkflowApprover(Long approverId, Long approverOrgId) {}

    private record WorkflowStepDefinition(
            Long id,
            String stepName,
            Integer stepOrder,
            String stepType,
            Long roleId
    ) {
        private boolean isSubmitStep() {
            return "SUBMIT".equalsIgnoreCase(String.valueOf(stepType));
        }
    }

    /**
     * 归档计划
     */
    @Transactional
    public PlanResponse archivePlan(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));

        plan.archive();
        Plan saved = planRepository.save(plan);
        publishAndClearEvents(saved);
        return convertToResponse(saved, null);
    }

    private void publishAndClearEvents(Plan plan) {
        if (plan == null) {
            return;
        }
        List<DomainEvent> events = plan.getDomainEvents();
        for (DomainEvent event : events) {
            eventPublisher.publish(event);
        }
        plan.clearEvents();
    }

    /**
     * 根据ID查询计划
     */
    public Optional<PlanResponse> getPlanById(Long id) {
        planIntegrityService.ensurePlanMatrix();
        Map<Long, String> orgNamesById = loadOrgNamesById();
        return planRepository.findById(id)
                .map(plan -> enrichWorkflowFields(convertToResponse(plan, null, orgNamesById), plan));
    }

    /**
     * 根据Task ID查询关联的Plan
     * Task 与 Plan 通过 sys_task.plan_id 关联。
     */
    public Optional<PlanResponse> getPlanByTaskId(Long taskId) {
        planIntegrityService.ensurePlanMatrix();
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
        planIntegrityService.ensurePlanMatrix();
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
        planIntegrityService.ensurePlanMatrix();
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
                safeLoadWorkflowSnapshotsByPlanIds(planPage.getContent().stream().map(Plan::getId).toList());
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
        planIntegrityService.ensurePlanMatrix();
        Cycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + cycleId));

        Map<Long, String> orgNamesById = loadOrgNamesById();
        List<Plan> plans = planRepository.findByCycleId(cycleId);
        Map<Long, PlanWorkflowSnapshotQueryService.WorkflowSnapshot> workflowSnapshotsByPlanId =
                safeLoadWorkflowSnapshotsByPlanIds(plans.stream().map(Plan::getId).toList());
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
        planIntegrityService.ensurePlanMatrix();
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
        details.setTargetOrgName(planResponse.getTargetOrgName());
        details.setCreatedByOrgId(planResponse.getCreatedByOrgId());
        details.setCreatedByOrgName(planResponse.getCreatedByOrgName());
        details.setPlanLevel(planResponse.getPlanLevel());
        details.setCanEdit(planResponse.getCanEdit());
        details.setCanResubmit(planResponse.getCanResubmit());
        details.setWorkflowInstanceId(planResponse.getWorkflowInstanceId());
        details.setWorkflowStatus(planResponse.getWorkflowStatus());
        details.setCurrentStepName(planResponse.getCurrentStepName());
        details.setCurrentApproverId(planResponse.getCurrentApproverId());
        details.setCurrentApproverName(planResponse.getCurrentApproverName());
        details.setCanWithdraw(planResponse.getCanWithdraw());

        String planStatus = PlanStatus.fromRaw(plan.getStatus()).value();
        List<Indicator> planIndicators = loadPlanIndicators(plan);
        List<Long> indicatorIds = planIndicators.stream()
                .map(Indicator::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
        Map<Long, Integer> reportProgressByIndicatorId = getLatestReportProgressByIndicatorIds(indicatorIds);
        CurrentReportContext currentReportContext = getCurrentReportContext(plan.getId(), indicatorIds);

        List<InternalIndicatorResponse> indicators = planIndicators.stream()
                .map(indicator -> convertIndicatorToResponse(
                        indicator,
                        planStatus,
                        reportProgressByIndicatorId.get(indicator.getId()),
                        currentReportContext))
                .collect(Collectors.toList());
        details.setIndicators(indicators);
        details.setWorkflowHistory(planWorkflowSnapshotQueryService.getWorkflowHistoryByPlanId(plan.getId()));

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
        String createdByOrgName = plan.getCreatedByOrgId() == null ? null : orgNamesById.get(plan.getCreatedByOrgId());

        return PlanResponse.builder()
                .id(plan.getId())
                .planName("Plan " + plan.getId()) // 计划名称需要从Plan实体获取或单独存储
                .description(null) // 需要从Plan实体获取或单独存储
                .planType(plan.getPlanLevel() != null ? plan.getPlanLevel().name() : "STRATEGY")
                .status(PlanStatus.fromRaw(plan.getStatus()).value())
                .startDate(plan.getCreatedAt())
                .endDate(plan.getUpdatedAt())
                .ownerDepartment(createdByOrgName)
                .completionPercentage(0)
                .indicatorCount(0) // 需要查询关联的指标数量
                .milestoneCount(0) // 需要查询关联的里程碑数量
                .createTime(plan.getCreatedAt())
                .year(year)
                .cycleId(plan.getCycleId())
                .targetOrgId(plan.getTargetOrgId())
                .targetOrgName(targetOrgName) // 设置目标组织名称
                .createdByOrgId(plan.getCreatedByOrgId())
                .createdByOrgName(createdByOrgName)
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

    private PlanWorkflowSnapshotQueryService.WorkflowSnapshot awaitWorkflowSnapshot(Long planId) {
        if (planId == null) {
            return null;
        }

        PlanWorkflowSnapshotQueryService.WorkflowSnapshot latestSnapshot =
                planWorkflowSnapshotQueryService.getWorkflowSnapshotByPlanId(planId);
        if (isReadyForSubmitResponse(latestSnapshot)) {
            return latestSnapshot;
        }
        return planWorkflowSnapshotQueryService.getWorkflowSnapshotByPlanId(planId);
    }

    private boolean isReadyForSubmitResponse(PlanWorkflowSnapshotQueryService.WorkflowSnapshot snapshot) {
        return snapshot != null
                && snapshot.getWorkflowInstanceId() != null
                && snapshot.getCurrentStepName() != null
                && Boolean.TRUE.equals(snapshot.getCanWithdraw());
    }

    private Map<Long, String> loadOrgNamesById() {
        long now = System.currentTimeMillis();
        Map<Long, String> cached = orgNamesByIdCache;
        if (!cached.isEmpty() && now - orgNamesCacheUpdatedAtMillis < ORG_NAMES_CACHE_TTL.toMillis()) {
            return cached;
        }

        synchronized (orgNamesCacheLock) {
            cached = orgNamesByIdCache;
            if (!cached.isEmpty() && now - orgNamesCacheUpdatedAtMillis < ORG_NAMES_CACHE_TTL.toMillis()) {
                return cached;
            }

            Map<Long, String> loaded = organizationRepository.findAll().stream()
                    .collect(Collectors.toMap(SysOrg::getId, SysOrg::getOrgName, (existing, replacement) -> existing));
            Map<Long, String> immutableLoaded = Collections.unmodifiableMap(loaded);
            orgNamesByIdCache = immutableLoaded;
            orgNamesCacheUpdatedAtMillis = System.currentTimeMillis();
            return immutableLoaded;
        }
    }

    private Map<Long, PlanWorkflowSnapshotQueryService.WorkflowSnapshot> safeLoadWorkflowSnapshotsByPlanIds(List<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return Map.of();
        }
        try {
            return planWorkflowSnapshotQueryService.getWorkflowSnapshotsByPlanIds(planIds);
        } catch (Exception ex) {
            log.warn("Failed to batch load workflow snapshots for plans={}, falling back to base plan responses: {}",
                    planIds.size(), ex.getMessage());
            return Map.of();
        }
    }

    private void assertNoActivePlanConflict(Long cycleId,
                                            PlanLevel planLevel,
                                            Long createdByOrgId,
                                            Long targetOrgId,
                                            Long currentPlanId) {
        List<Plan> activePlans = planRepository.findActiveByCycleIdAndPlanLevelAndCreatedByOrgIdAndTargetOrgId(
                cycleId,
                planLevel,
                createdByOrgId,
                targetOrgId
        );
        boolean hasConflict = activePlans.stream()
                .anyMatch(plan -> currentPlanId == null || !currentPlanId.equals(plan.getId()));
        if (hasConflict) {
            throw new ConflictException(String.format(
                    "Plan already exists for cycleId=%s, planLevel=%s, createdByOrgId=%s, targetOrgId=%s",
                    cycleId,
                    planLevel,
                    createdByOrgId,
                    targetOrgId
            ));
        }
    }

    private Plan savePlanHandlingConflict(Plan plan) {
        try {
            return planRepository.save(plan);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(String.format(
                    "Plan already exists for cycleId=%s, planLevel=%s, createdByOrgId=%s, targetOrgId=%s",
                    plan.getCycleId(),
                    plan.getPlanLevel(),
                    plan.getCreatedByOrgId(),
                    plan.getTargetOrgId()
            ));
        }
    }

    /**
     * 将Indicator实体转换为响应DTO
     * 指标状态统一使用 Plan 的状态
     */
    private InternalIndicatorResponse convertIndicatorToResponse(Indicator indicator,
                                                                String planStatus,
                                                                Integer reportProgress,
                                                                CurrentReportContext currentReportContext) {
        // 使用 Plan 的状态作为指标状态
        String effectiveStatus = planStatus != null ? planStatus :
                (indicator.getStatus() != null ? indicator.getStatus().name() : "DRAFT");
        PendingIndicatorState pendingIndicatorState = currentReportContext.getPendingState(indicator.getId());
        String ownerOrgName = indicator.getOwnerOrg() != null ? indicator.getOwnerOrg().getName() : null;
        String targetOrgName = indicator.getTargetOrg() != null ? indicator.getTargetOrg().getName() : null;
        String taskName = null;
        if (indicator.getTaskId() != null) {
            taskName = taskRepository.findById(indicator.getTaskId())
                    .map(StrategicTask::getName)
                    .orElse(null);
        }

        return InternalIndicatorResponse.builder()
                .id(indicator.getId())
                .indicatorName(indicator.getName())
                .indicatorCode("IND" + indicator.getId())
                .indicatorDesc(indicator.getDescription())
                .cycleId(indicator.getTaskId()) // 使用taskId作为cycleId（临时方案）
                .ownerOrgId(indicator.getOwnerOrg() != null ? indicator.getOwnerOrg().getId() : null)
                .ownerOrgName(ownerOrgName)
                .ownerDept(ownerOrgName)
                .targetOrgId(indicator.getTargetOrg() != null ? indicator.getTargetOrg().getId() : null)
                .targetOrgName(targetOrgName)
                .responsibleDept(targetOrgName)
                .taskName(taskName)
                .weightPercent(indicator.getWeight())
                .status(effectiveStatus)
                .progress(indicator.getProgress())
                .reportProgress(reportProgress)
                .currentReportId(currentReportContext.currentReportId())
                .progressApprovalStatus(currentReportContext.progressApprovalStatus())
                .pendingProgress(pendingIndicatorState.pendingProgress())
                .pendingRemark(pendingIndicatorState.pendingRemark())
                .pendingAttachments(pendingIndicatorState.pendingAttachments())
                .createdAt(indicator.getCreatedAt())
                .updatedAt(indicator.getUpdatedAt())
                .build();
    }

    /**
     * 将Indicator实体转换为响应DTO（兼容旧方法，用于非Plan关联场景）
     */
    private InternalIndicatorResponse convertIndicatorToResponse(Indicator indicator) {
        return convertIndicatorToResponse(indicator, null, null, CurrentReportContext.empty());
    }

    private CurrentReportContext getCurrentReportContext(Long planId, List<Long> indicatorIds) {
        if (planId == null) {
            return CurrentReportContext.empty();
        }

        List<Map<String, Object>> reportRows = jdbcTemplate.queryForList(
                """
                SELECT pr.id AS report_id, pr.status AS report_status
                FROM public.plan_report pr
                WHERE pr.plan_id = ?
                  AND pr.is_deleted = false
                  AND pr.status IN ('DRAFT', 'IN_REVIEW', 'REJECTED')
                ORDER BY pr.updated_at DESC NULLS LAST, pr.id DESC
                LIMIT 1
                """,
                planId
        );

        if (reportRows.isEmpty()) {
            return CurrentReportContext.empty();
        }

        Object reportIdValue = reportRows.get(0).get("report_id");
        if (!(reportIdValue instanceof Number reportIdNumber)) {
            return CurrentReportContext.empty();
        }

        Long currentReportId = reportIdNumber.longValue();
        String progressApprovalStatus = mapReportStatusToProgressApprovalStatus(reportRows.get(0).get("report_status"));

        if (indicatorIds == null || indicatorIds.isEmpty()) {
            return new CurrentReportContext(currentReportId, progressApprovalStatus, Map.of());
        }

        List<Map<String, Object>> pendingRows = namedParameterJdbcTemplate.queryForList(
                """
                SELECT pri.id AS plan_report_indicator_id,
                       pri.indicator_id AS indicator_id,
                       pri.progress AS pending_progress,
                       pri.comment AS pending_remark
                FROM public.plan_report_indicator pri
                WHERE pri.report_id = :reportId
                  AND pri.indicator_id IN (:indicatorIds)
                """,
                new MapSqlParameterSource()
                        .addValue("reportId", currentReportId)
                        .addValue("indicatorIds", indicatorIds)
        );

        if (pendingRows.isEmpty()) {
            return new CurrentReportContext(currentReportId, progressApprovalStatus, Map.of());
        }

        Map<Long, Long> reportIndicatorIdByIndicatorId = new HashMap<>();
        Map<Long, PendingIndicatorState> pendingStateByIndicatorId = new HashMap<>();
        for (Map<String, Object> row : pendingRows) {
            Object indicatorIdValue = row.get("indicator_id");
            if (!(indicatorIdValue instanceof Number indicatorIdNumber)) {
                continue;
            }

            Long indicatorId = indicatorIdNumber.longValue();
            Integer pendingProgress = row.get("pending_progress") instanceof Number pendingProgressNumber
                    ? pendingProgressNumber.intValue()
                    : null;
            String pendingRemark = row.get("pending_remark") == null
                    ? null
                    : String.valueOf(row.get("pending_remark"));

            pendingStateByIndicatorId.put(
                    indicatorId,
                    new PendingIndicatorState(pendingProgress, pendingRemark, List.of())
            );

            Object planReportIndicatorIdValue = row.get("plan_report_indicator_id");
            if (planReportIndicatorIdValue instanceof Number planReportIndicatorIdNumber) {
                reportIndicatorIdByIndicatorId.put(indicatorId, planReportIndicatorIdNumber.longValue());
            }
        }

        if (!reportIndicatorIdByIndicatorId.isEmpty()) {
            List<Long> planReportIndicatorIds = new ArrayList<>(reportIndicatorIdByIndicatorId.values());
            List<Map<String, Object>> attachmentRows = namedParameterJdbcTemplate.queryForList(
                    """
                    SELECT pria.plan_report_indicator_id AS plan_report_indicator_id,
                           COALESCE(NULLIF(a.public_url, ''), NULLIF(a.object_key, ''), a.original_name) AS attachment_value
                    FROM public.plan_report_indicator_attachment pria
                    JOIN public.attachment a ON a.id = pria.attachment_id
                    WHERE pria.plan_report_indicator_id IN (:reportIndicatorIds)
                      AND COALESCE(a.is_deleted, false) = false
                    ORDER BY pria.sort_order ASC, pria.id ASC
                    """,
                    new MapSqlParameterSource("reportIndicatorIds", planReportIndicatorIds)
            );

            Map<Long, List<String>> attachmentsByReportIndicatorId = new HashMap<>();
            for (Map<String, Object> row : attachmentRows) {
                Object reportIndicatorIdValue = row.get("plan_report_indicator_id");
                Object attachmentValue = row.get("attachment_value");
                if (!(reportIndicatorIdValue instanceof Number reportIndicatorIdNumber) || attachmentValue == null) {
                    continue;
                }
                String attachment = String.valueOf(attachmentValue).trim();
                if (attachment.isEmpty()) {
                    continue;
                }
                attachmentsByReportIndicatorId
                        .computeIfAbsent(reportIndicatorIdNumber.longValue(), ignored -> new ArrayList<>())
                        .add(attachment);
            }

            for (Map.Entry<Long, Long> entry : reportIndicatorIdByIndicatorId.entrySet()) {
                Long indicatorId = entry.getKey();
                Long planReportIndicatorId = entry.getValue();
                PendingIndicatorState pendingState = pendingStateByIndicatorId.get(indicatorId);
                if (pendingState == null) {
                    continue;
                }
                pendingStateByIndicatorId.put(
                        indicatorId,
                        new PendingIndicatorState(
                                pendingState.pendingProgress(),
                                pendingState.pendingRemark(),
                                attachmentsByReportIndicatorId.getOrDefault(planReportIndicatorId, List.of())
                        )
                );
            }
        }

        return new CurrentReportContext(currentReportId, progressApprovalStatus, pendingStateByIndicatorId);
    }

    private String mapReportStatusToProgressApprovalStatus(Object rawStatus) {
        if (rawStatus == null) {
            return "NONE";
        }

        String normalized = String.valueOf(rawStatus).trim().toUpperCase();
        return switch (normalized) {
            case "DRAFT" -> "DRAFT";
            case "SUBMITTED", "IN_REVIEW" -> "PENDING";
            case "REJECTED" -> "REJECTED";
            default -> "NONE";
        };
    }

    private Map<Long, Integer> getLatestReportProgressByIndicatorIds(List<Long> indicatorIds) {
        if (indicatorIds == null || indicatorIds.isEmpty()) {
            return Map.of();
        }

        List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(
                """
                SELECT pri.indicator_id AS indicator_id, pri.progress AS report_progress
                FROM public.plan_report_indicator pri
                INNER JOIN (
                    SELECT pri2.indicator_id, MAX(pr.created_at) AS latest_created_at
                    FROM public.plan_report_indicator pri2
                    INNER JOIN public.plan_report pr ON pri2.report_id = pr.id
                    WHERE pr.is_deleted = false
                    AND pri2.indicator_id IN (:indicatorIds)
                    GROUP BY pri2.indicator_id
                ) latest ON latest.indicator_id = pri.indicator_id
                INNER JOIN public.plan_report pr ON pri.report_id = pr.id
                WHERE pr.is_deleted = false
                AND pri.indicator_id IN (:indicatorIds)
                AND pr.created_at = latest.latest_created_at
                """,
                new MapSqlParameterSource("indicatorIds", indicatorIds)
        );

        Map<Long, Integer> reportProgressByIndicatorId = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object indicatorIdValue = row.get("indicator_id");
            Object reportProgressValue = row.get("report_progress");
            if (!(indicatorIdValue instanceof Number indicatorIdNumber)) {
                continue;
            }
            if (!(reportProgressValue instanceof Number reportProgressNumber)) {
                continue;
            }
            reportProgressByIndicatorId.put(indicatorIdNumber.longValue(), reportProgressNumber.intValue());
        }
        return reportProgressByIndicatorId;
    }

    /**
     * 同步指标状态与 Plan 状态
     * 当 Plan 状态变更时，统一更新所有关联指标的状态
     */
    @Transactional
    public void syncIndicatorStatusWithPlan(Plan plan) {
        // 获取 Plan 对应的状态
        IndicatorStatus targetStatus = mapPlanStatusToIndicatorStatus(plan.getStatus());

        List<Indicator> indicators = loadPlanIndicators(plan);

        // 更新所有指标的状态后一次性批量保存，避免逐条写库
        for (Indicator indicator : indicators) {
            indicator.setStatus(targetStatus);
        }
        indicatorRepository.saveAll(indicators);
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

    private List<Indicator> loadPlanIndicators(Plan plan) {
        List<StrategicTask> tasks = taskRepository.findByPlanId(plan.getId());
        List<Long> taskIds = tasks.stream()
                .filter(Objects::nonNull)
                .map(StrategicTask::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));

        if (!taskIds.isEmpty()) {
            List<Indicator> taskBoundIndicators = indicatorRepository.findByTaskIds(taskIds);
            if (!taskBoundIndicators.isEmpty()) {
                return taskBoundIndicators;
            }
        }

        if (plan.getPlanLevel() != PlanLevel.FUNC_TO_COLLEGE) {
            return List.of();
        }

        Long ownerOrgId = plan.getCreatedByOrgId();
        Long targetOrgId = plan.getTargetOrgId();
        if (ownerOrgId == null || targetOrgId == null) {
            return List.of();
        }

        Long cycleId = plan.getCycleId();
        List<Indicator> fallbackIndicators = indicatorRepository.findByOwnerOrgIdAndTargetOrgId(ownerOrgId, targetOrgId);
        if (fallbackIndicators.isEmpty()) {
            return List.of();
        }

        List<Long> fallbackTaskIds = fallbackIndicators.stream()
                .map(Indicator::getTaskId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        if (fallbackTaskIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> taskIdToCycle = taskRepository.findAllById(fallbackTaskIds).stream()
                .filter(task -> task != null && task.getId() != null)
                .collect(Collectors.toMap(
                        StrategicTask::getId,
                        StrategicTask::getCycleId,
                        (existing, ignored) -> existing
                ));

        return fallbackIndicators.stream()
                .filter(indicator -> {
                    Long taskId = indicator.getTaskId();
                    if (taskId == null) {
                        return false;
                    }
                    Long taskCycleId = taskIdToCycle.get(taskId);
                    return Objects.equals(taskCycleId, cycleId);
                })
                .collect(Collectors.toCollection(ArrayList::new));
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
        private String ownerOrgName;
        private String ownerDept;
        private Long targetOrgId;
        private String targetOrgName;
        private String responsibleDept;
        private String taskName;
        private java.math.BigDecimal weightPercent;
        private String status;
        private Integer progress;
        private Integer reportProgress;
        private Long currentReportId;
        private String progressApprovalStatus;
        private Integer pendingProgress;
        private String pendingRemark;
        private List<String> pendingAttachments;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
    }

    private record CurrentReportContext(Long currentReportId,
                                        String progressApprovalStatus,
                                        Map<Long, PendingIndicatorState> pendingStateByIndicatorId) {
        private static CurrentReportContext empty() {
            return new CurrentReportContext(null, "NONE", Map.of());
        }

        private PendingIndicatorState getPendingState(Long indicatorId) {
            if (indicatorId == null) {
                return PendingIndicatorState.empty();
            }
            return pendingStateByIndicatorId.getOrDefault(indicatorId, PendingIndicatorState.empty());
        }
    }

    private record PendingIndicatorState(Integer pendingProgress,
                                         String pendingRemark,
                                         List<String> pendingAttachments) {
        private PendingIndicatorState {
            pendingAttachments = pendingAttachments == null ? Collections.emptyList() : List.copyOf(pendingAttachments);
        }

        private static PendingIndicatorState empty() {
            return new PendingIndicatorState(null, null, List.of());
        }
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
