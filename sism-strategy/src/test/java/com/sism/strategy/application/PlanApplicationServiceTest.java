package com.sism.strategy.application;

import com.sism.strategy.domain.plan.Plan;
import com.sism.strategy.domain.plan.PlanLevel;
import com.sism.strategy.domain.plan.PlanStatus;
import com.sism.strategy.domain.repository.PlanRepository;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.organization.domain.repository.OrganizationRepository;
import com.sism.strategy.domain.Cycle;
import com.sism.strategy.domain.repository.CycleRepository;
import com.sism.strategy.domain.repository.IndicatorRepository;
import com.sism.strategy.interfaces.dto.PlanResponse;
import com.sism.task.domain.StrategicTask;
import com.sism.task.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
                planWorkflowSnapshotQueryService
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
}
