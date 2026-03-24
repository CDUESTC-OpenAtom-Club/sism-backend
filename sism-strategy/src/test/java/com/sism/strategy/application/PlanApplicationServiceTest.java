package com.sism.strategy.application;

import com.sism.strategy.domain.plan.Plan;
import com.sism.strategy.domain.plan.PlanLevel;
import com.sism.strategy.domain.plan.PlanStatus;
import com.sism.strategy.domain.repository.PlanRepository;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.organization.domain.repository.OrganizationRepository;
import com.sism.strategy.domain.Cycle;
import com.sism.strategy.domain.Indicator;
import com.sism.strategy.domain.repository.CycleRepository;
import com.sism.strategy.domain.repository.IndicatorRepository;
import com.sism.strategy.interfaces.dto.PlanResponse;
import com.sism.strategy.interfaces.dto.SubmitPlanApprovalRequest;
import com.sism.task.domain.StrategicTask;
import com.sism.task.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.same;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("Plan Application Service Tests")
class PlanApplicationServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private CycleRepository cycleRepository;

    @Mock
    private IndicatorRepository indicatorRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private BasicTaskWeightValidationService basicTaskWeightValidationService;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private PlanWorkflowSnapshotQueryService planWorkflowSnapshotQueryService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PlanApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PlanApplicationService(
                planRepository,
                cycleRepository,
                indicatorRepository,
                organizationRepository,
                basicTaskWeightValidationService,
                taskRepository,
                eventPublisher,
                planWorkflowSnapshotQueryService,
                jdbcTemplate
        );
    }

    @Test
    @DisplayName("Should load plan by task relation instead of treating taskId as planId")
    void shouldLoadPlanByTaskRelation() {
        StrategicTask task = new StrategicTask();
        task.setId(92071L);
        task.setPlanId(1L);

        Plan plan = Plan.create(90L, 35L, 35L, PlanLevel.STRATEGIC);
        plan.setId(1L);

        when(taskRepository.findById(92071L)).thenReturn(Optional.of(task));
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));

        Optional<com.sism.strategy.interfaces.dto.PlanResponse> result = service.getPlanByTaskId(92071L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(taskRepository).findById(92071L);
        verify(planRepository).findById(1L);
    }

    @Test
    @DisplayName("Should query paged plans from repository instead of loading all plans")
    void shouldUsePagedPlanRepositoryQuery() {
        Cycle cycle = new Cycle();
        cycle.setId(2026L);

        Plan plan = Plan.create(2026L, 35L, 35L, PlanLevel.STRATEGIC);
        plan.setId(1L);

        when(cycleRepository.findByYear(2026)).thenReturn(List.of(cycle));
        when(planRepository.findPage(eq(List.of(2026L)), eq(List.of("DISTRIBUTED", "APPROVED", "ACTIVE", "PUBLISHED")), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(plan), PageRequest.of(0, 20), 1));

        var result = service.getPlans(0, 20, 2026, "DISTRIBUTED");

        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getId());
        verify(cycleRepository).findByYear(2026);
        verify(planRepository).findPage(eq(List.of(2026L)), eq(List.of("DISTRIBUTED", "APPROVED", "ACTIVE", "PUBLISHED")), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should persist plan as pending before publishing approval workflow event")
    void shouldPersistPlanAsPendingBeforePublishingApprovalWorkflowEvent() {
        Plan plan = Plan.create(2026L, 35L, 35L, PlanLevel.STRATEGIC);
        plan.setId(10L);

        SubmitPlanApprovalRequest request = new SubmitPlanApprovalRequest();
        request.setWorkflowCode("PLAN_DISPATCH_STRATEGY");

        when(planRepository.findById(10L)).thenReturn(Optional.of(plan));
        when(planRepository.save(same(plan))).thenReturn(plan);

        PlanResponse response = service.submitPlanForApproval(10L, request, 188L, 35L);

        assertEquals(PlanStatus.PENDING.value(), plan.getStatus());
        assertEquals(PlanStatus.PENDING.value(), response.getStatus());
        verify(planRepository).save(same(plan));
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("Should allow functional department approval submission from distributed state")
    void shouldAllowFunctionalDepartmentApprovalSubmissionFromDistributedState() {
        Plan plan = Plan.create(2026L, 36L, 35L, PlanLevel.STRAT_TO_FUNC);
        plan.setId(4036L);
        plan.activate();

        SubmitPlanApprovalRequest request = new SubmitPlanApprovalRequest();
        request.setWorkflowCode("PLAN_APPROVAL_FUNCDEPT");

        when(planRepository.findById(4036L)).thenReturn(Optional.of(plan));
        when(planRepository.save(same(plan))).thenReturn(plan);

        PlanResponse response = service.submitPlanForApproval(4036L, request, 191L, 36L);

        assertEquals(PlanStatus.PENDING.value(), plan.getStatus());
        assertEquals(PlanStatus.PENDING.value(), response.getStatus());
        verify(planRepository).save(same(plan));
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("Should batch workflow snapshot lookup when loading paged plans")
    void shouldBatchWorkflowSnapshotLookupWhenLoadingPagedPlans() {
        Plan first = Plan.create(2026L, 35L, 35L, PlanLevel.STRATEGIC);
        first.setId(1L);
        Plan second = Plan.create(2026L, 36L, 35L, PlanLevel.STRATEGIC);
        second.setId(2L);

        when(planRepository.findPage(eq(List.of()), eq(List.of()), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 20), 2));
        when(organizationRepository.findAll()).thenReturn(List.of());
        when(planWorkflowSnapshotQueryService.getWorkflowSnapshotsByPlanIds(List.of(1L, 2L)))
                .thenReturn(Map.of(
                        1L,
                        PlanWorkflowSnapshotQueryService.WorkflowSnapshot.builder()
                                .workflowInstanceId(101L)
                                .workflowStatus("IN_REVIEW")
                                .build()
                ));

        var result = service.getPlans(0, 20, null, null);

        assertEquals(2, result.getTotalElements());
        assertEquals(101L, result.getContent().get(0).getWorkflowInstanceId());
        assertEquals("IN_REVIEW", result.getContent().get(0).getWorkflowStatus());
        assertEquals(null, result.getContent().get(1).getWorkflowInstanceId());
        verify(planWorkflowSnapshotQueryService).getWorkflowSnapshotsByPlanIds(List.of(1L, 2L));
        verify(planWorkflowSnapshotQueryService, never()).getWorkflowSnapshotByPlanId(any());
        verify(planRepository, times(1)).findPage(eq(List.of()), eq(List.of()), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should return empty page when year filter has no matching cycles")
    void shouldReturnEmptyPageWhenYearHasNoMatchingCycles() {
        when(cycleRepository.findByYear(2030)).thenReturn(List.of());

        var result = service.getPlans(0, 20, 2030, null);

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(cycleRepository).findByYear(2030);
        verify(planRepository, never()).findPage(any(), any(), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should mark plan as returned when workflow rejects it")
    void shouldMarkPlanAsReturnedWhenWorkflowRejectsIt() {
        Plan plan = Plan.create(2026L, 35L, 35L, PlanLevel.STRATEGIC);
        plan.setId(1L);

        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));

        service.markWorkflowRejected(1L, "Need update");

        assertEquals(PlanStatus.RETURNED.value(), plan.getStatus());
        verify(planRepository).save(plan);
    }

    @Test
    @DisplayName("Should reset plan to draft when workflow is withdrawn before first approval")
    void shouldResetPlanToDraftWhenWorkflowIsWithdrawn() {
        Plan plan = Plan.create(2026L, 35L, 35L, PlanLevel.STRATEGIC);
        plan.setId(11L);
        plan.returnForRevision();

        when(planRepository.findById(11L)).thenReturn(Optional.of(plan));

        service.markWorkflowWithdrawn(11L);

        assertEquals(PlanStatus.DRAFT.value(), plan.getStatus());
        verify(planRepository).save(plan);
    }

    @Test
    @DisplayName("Should map returned plan to editable response")
    void shouldMapReturnedPlanToEditableResponse() {
        Plan plan = Plan.create(2026L, 35L, 35L, PlanLevel.STRATEGIC);
        plan.setId(1L);
        plan.returnForRevision();

        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(organizationRepository.findAll()).thenReturn(List.of());
        when(planWorkflowSnapshotQueryService.getWorkflowSnapshotByPlanId(1L)).thenReturn(
                PlanWorkflowSnapshotQueryService.WorkflowSnapshot.builder()
                        .workflowInstanceId(88L)
                        .workflowStatus("IN_REVIEW")
                        .startedAt(java.time.LocalDateTime.now())
                        .lastRejectReason("Need update")
                        .currentStepName("部门审批")
                        .currentApproverId(100L)
                        .currentApproverName("审核人A")
                        .canWithdraw(false)
                        .build()
        );

        PlanResponse response = service.getPlanById(1L).orElseThrow();

        assertEquals(PlanStatus.RETURNED.value(), response.getStatus());
        assertEquals("Need update", response.getLastRejectReason());
        assertTrue(response.getCanEdit());
        assertTrue(response.getCanResubmit());
        assertEquals("IN_REVIEW", response.getWorkflowStatus());
        assertEquals("部门审批", response.getCurrentStepName());
    }

    @Test
    @DisplayName("Should keep workflow-pending plan not editable in response")
    void shouldKeepWorkflowPendingPlanNotEditableInResponse() {
        Plan plan = Plan.create(2026L, 35L, 35L, PlanLevel.STRATEGIC);
        plan.setId(2L);

        when(planRepository.findById(2L)).thenReturn(Optional.of(plan));
        when(organizationRepository.findAll()).thenReturn(List.of());
        when(planWorkflowSnapshotQueryService.getWorkflowSnapshotByPlanId(2L)).thenReturn(
                PlanWorkflowSnapshotQueryService.WorkflowSnapshot.builder()
                        .workflowInstanceId(98L)
                        .workflowStatus("PENDING")
                        .startedAt(java.time.LocalDateTime.now())
                        .build()
        );

        PlanResponse response = service.getPlanById(2L).orElseThrow();

        assertEquals(PlanStatus.DRAFT.value(), response.getStatus());
        assertEquals("PENDING", response.getWorkflowStatus());
        assertEquals(98L, response.getWorkflowInstanceId());
    }

    @Test
    @DisplayName("Should include workflow snapshot and history in plan details")
    void shouldIncludeWorkflowSnapshotAndHistoryInPlanDetails() {
        Plan plan = Plan.create(2026L, 35L, 35L, PlanLevel.STRATEGIC);
        plan.setId(3L);

        Cycle cycle = new Cycle();
        cycle.setId(2026L);
        cycle.setYear(2026);

        when(planRepository.findById(3L)).thenReturn(Optional.of(plan));
        when(cycleRepository.findById(2026L)).thenReturn(Optional.of(cycle));
        when(organizationRepository.findAll()).thenReturn(List.of());
        when(taskRepository.findByPlanId(3L)).thenReturn(List.of());
        when(planWorkflowSnapshotQueryService.getWorkflowSnapshotByPlanId(3L)).thenReturn(
                PlanWorkflowSnapshotQueryService.WorkflowSnapshot.builder()
                        .workflowInstanceId(99L)
                        .workflowStatus("IN_REVIEW")
                        .currentStepName("学院审批")
                        .currentApproverId(66L)
                        .currentApproverName("审批人B")
                        .canWithdraw(true)
                        .build()
        );
        when(planWorkflowSnapshotQueryService.getWorkflowHistoryByPlanId(3L)).thenReturn(List.of(
                PlanWorkflowSnapshotQueryService.WorkflowHistoryItem.builder()
                        .taskId(1001L)
                        .stepName("职能部门审批")
                        .operatorId(55L)
                        .operatorName("审批人A")
                        .action("APPROVE")
                        .comment("同意")
                        .build()
        ));

        PlanApplicationService.PlanDetailsResponse response = service.getPlanDetails(3L);

        assertEquals(99L, response.getWorkflowInstanceId());
        assertEquals("IN_REVIEW", response.getWorkflowStatus());
        assertEquals("学院审批", response.getCurrentStepName());
        assertEquals(66L, response.getCurrentApproverId());
        assertEquals("审批人B", response.getCurrentApproverName());
        assertTrue(response.getCanWithdraw());
        assertNotNull(response.getWorkflowHistory());
        assertEquals(1, response.getWorkflowHistory().size());
        assertEquals("职能部门审批", response.getWorkflowHistory().get(0).getStepName());
    }

    @Test
    @DisplayName("Should include latest report progress in plan details indicators")
    void shouldIncludeLatestReportProgressInPlanDetailsIndicators() {
        Plan plan = createPlanWithCycle(4L, 2026L);
        StrategicTask task = createTask(41001L, 4L);
        Indicator indicator = createIndicatorMock(2002L, 41001L, 0, "形成党委统战领域专项推进台账");

        when(planRepository.findById(4L)).thenReturn(Optional.of(plan));
        when(cycleRepository.findById(2026L)).thenReturn(Optional.of(createCycle(2026L, 2026)));
        when(organizationRepository.findAll()).thenReturn(List.of());
        when(taskRepository.findByPlanId(4L)).thenReturn(List.of(task));
        when(indicatorRepository.findByTaskId(41001L)).thenReturn(List.of(indicator));
        when(planWorkflowSnapshotQueryService.getWorkflowHistoryByPlanId(4L)).thenReturn(List.of());
        when(jdbcTemplate.queryForList(contains("MAX(pr.created_at)"), any(Object[].class)))
                .thenReturn(List.of(Map.of("indicator_id", 2002L, "report_progress", 20)));
        when(jdbcTemplate.queryForList(contains("FROM public.plan_report pr"), any(Object[].class)))
                .thenReturn(List.of());

        PlanApplicationService.PlanDetailsResponse response = service.getPlanDetails(4L);

        assertNotNull(response.getIndicators());
        assertEquals(1, response.getIndicators().size());
        assertEquals(20, response.getIndicators().get(0).getReportProgress());
        assertEquals("NONE", response.getIndicators().get(0).getProgressApprovalStatus());
    }

    @Test
    @DisplayName("Should expose current draft pending fields from latest active report")
    void shouldExposeCurrentDraftPendingFieldsFromLatestActiveReport() {
        Plan plan = createPlanWithCycle(5L, 2026L);
        StrategicTask task = createTask(51001L, 5L);
        Indicator indicator = createIndicatorMock(2002L, 51001L, 0, "形成党委统战领域专项推进台账");

        when(planRepository.findById(5L)).thenReturn(Optional.of(plan));
        when(cycleRepository.findById(2026L)).thenReturn(Optional.of(createCycle(2026L, 2026)));
        when(organizationRepository.findAll()).thenReturn(List.of());
        when(taskRepository.findByPlanId(5L)).thenReturn(List.of(task));
        when(indicatorRepository.findByTaskId(51001L)).thenReturn(List.of(indicator));
        when(planWorkflowSnapshotQueryService.getWorkflowHistoryByPlanId(5L)).thenReturn(List.of());
        when(jdbcTemplate.queryForList(contains("FROM public.plan_report pr"), any(Object[].class)))
                .thenReturn(List.of(Map.of("report_id", 9001L, "report_status", "DRAFT")));
        when(jdbcTemplate.queryForList(contains("FROM public.plan_report_indicator pri"), any(Object[].class)))
                .thenReturn(List.of(Map.of(
                        "plan_report_indicator_id", 7001L,
                        "indicator_id", 2002L,
                        "pending_progress", 20,
                        "pending_remark", "任务完成"
                )));
        when(jdbcTemplate.queryForList(contains("FROM public.plan_report_indicator_attachment pria"), any(Object[].class)))
                .thenReturn(List.of(
                        Map.of("plan_report_indicator_id", 7001L, "attachment_value", "https://files.example.com/a.pdf"),
                        Map.of("plan_report_indicator_id", 7001L, "attachment_value", "推进台账.xlsx")
                ));

        PlanApplicationService.PlanDetailsResponse response = service.getPlanDetails(5L);

        assertEquals(1, response.getIndicators().size());
        var indicatorResponse = response.getIndicators().get(0);
        assertEquals(9001L, indicatorResponse.getCurrentReportId());
        assertEquals("DRAFT", indicatorResponse.getProgressApprovalStatus());
        assertEquals(20, indicatorResponse.getPendingProgress());
        assertEquals("任务完成", indicatorResponse.getPendingRemark());
        assertEquals(List.of("https://files.example.com/a.pdf", "推进台账.xlsx"), indicatorResponse.getPendingAttachments());
    }

    @Test
    @DisplayName("Should mark latest rejected active report as rejected even when older draft exists")
    void shouldMarkLatestRejectedActiveReportAsRejected() {
        Plan plan = createPlanWithCycle(6L, 2026L);
        StrategicTask task = createTask(61001L, 6L);
        Indicator indicator = createIndicatorMock(2001L, 61001L, 15, "完成党委办公室年度重点工作分解与落实");

        when(planRepository.findById(6L)).thenReturn(Optional.of(plan));
        when(cycleRepository.findById(2026L)).thenReturn(Optional.of(createCycle(2026L, 2026)));
        when(organizationRepository.findAll()).thenReturn(List.of());
        when(taskRepository.findByPlanId(6L)).thenReturn(List.of(task));
        when(indicatorRepository.findByTaskId(61001L)).thenReturn(List.of(indicator));
        when(planWorkflowSnapshotQueryService.getWorkflowHistoryByPlanId(6L)).thenReturn(List.of());
        when(jdbcTemplate.queryForList(contains("FROM public.plan_report pr"), any(Object[].class)))
                .thenReturn(List.of(
                        Map.of("report_id", 9102L, "report_status", "REJECTED"),
                        Map.of("report_id", 9101L, "report_status", "DRAFT")
                ));
        when(jdbcTemplate.queryForList(contains("FROM public.plan_report_indicator pri"), any(Object[].class)))
                .thenReturn(List.of(Map.of(
                        "plan_report_indicator_id", 7101L,
                        "indicator_id", 2001L,
                        "pending_progress", 18,
                        "pending_remark", "退回后待修改"
                )));
        when(jdbcTemplate.queryForList(contains("FROM public.plan_report_indicator_attachment pria"), any(Object[].class)))
                .thenReturn(List.of());

        PlanApplicationService.PlanDetailsResponse response = service.getPlanDetails(6L);

        var indicatorResponse = response.getIndicators().get(0);
        assertEquals(9102L, indicatorResponse.getCurrentReportId());
        assertEquals("REJECTED", indicatorResponse.getProgressApprovalStatus());
        assertEquals(18, indicatorResponse.getPendingProgress());
        assertEquals("退回后待修改", indicatorResponse.getPendingRemark());
    }

    @Test
    @DisplayName("Should return empty pending fields when no active report exists")
    void shouldReturnEmptyPendingFieldsWhenNoActiveReportExists() {
        Plan plan = createPlanWithCycle(7L, 2026L);
        StrategicTask task = createTask(71001L, 7L);
        Indicator indicator = createIndicatorMock(2045L, 71001L, 0, "形成党委统战领域专项推进台账");

        when(planRepository.findById(7L)).thenReturn(Optional.of(plan));
        when(cycleRepository.findById(2026L)).thenReturn(Optional.of(createCycle(2026L, 2026)));
        when(organizationRepository.findAll()).thenReturn(List.of());
        when(taskRepository.findByPlanId(7L)).thenReturn(List.of(task));
        when(indicatorRepository.findByTaskId(71001L)).thenReturn(List.of(indicator));
        when(planWorkflowSnapshotQueryService.getWorkflowHistoryByPlanId(7L)).thenReturn(List.of());
        when(jdbcTemplate.queryForList(contains("MAX(pr.created_at)"), any(Object[].class)))
                .thenReturn(List.of());
        when(jdbcTemplate.queryForList(contains("FROM public.plan_report pr"), any(Object[].class)))
                .thenReturn(List.of());

        PlanApplicationService.PlanDetailsResponse response = service.getPlanDetails(7L);

        var indicatorResponse = response.getIndicators().get(0);
        assertEquals("NONE", indicatorResponse.getProgressApprovalStatus());
        assertEquals(null, indicatorResponse.getCurrentReportId());
        assertEquals(null, indicatorResponse.getPendingProgress());
        assertEquals(null, indicatorResponse.getPendingRemark());
        assertEquals(List.of(), indicatorResponse.getPendingAttachments());
    }

    private Plan createPlanWithCycle(Long planId, Long cycleId) {
        Plan plan = Plan.create(cycleId, 35L, 35L, PlanLevel.STRATEGIC);
        plan.setId(planId);
        return plan;
    }

    private Cycle createCycle(Long cycleId, int year) {
        Cycle cycle = new Cycle();
        cycle.setId(cycleId);
        cycle.setYear(year);
        return cycle;
    }

    private StrategicTask createTask(Long taskId, Long planId) {
        StrategicTask task = new StrategicTask();
        task.setId(taskId);
        task.setPlanId(planId);
        return task;
    }

    private Indicator createIndicatorMock(Long indicatorId, Long taskId, int progress, String indicatorName) {
        Indicator indicator = org.mockito.Mockito.mock(Indicator.class);
        when(indicator.getId()).thenReturn(indicatorId);
        when(indicator.getName()).thenReturn(indicatorName);
        when(indicator.getDescription()).thenReturn(indicatorName);
        when(indicator.getTaskId()).thenReturn(taskId);
        when(indicator.getWeight()).thenReturn(java.math.BigDecimal.valueOf(50));
        when(indicator.getProgress()).thenReturn(progress);
        when(indicator.getCreatedAt()).thenReturn(java.time.LocalDateTime.now());
        when(indicator.getUpdatedAt()).thenReturn(java.time.LocalDateTime.now());
        return indicator;
    }
}
