package com.sism.strategy.application;

import com.sism.execution.domain.model.plan.Plan;
import com.sism.execution.domain.model.plan.PlanLevel;
import com.sism.execution.domain.repository.PlanRepository;
import com.sism.organization.domain.repository.OrganizationRepository;
import com.sism.strategy.domain.Cycle;
import com.sism.strategy.domain.repository.CycleRepository;
import com.sism.strategy.domain.repository.IndicatorRepository;
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

    private PlanApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PlanApplicationService(
                planRepository,
                cycleRepository,
                indicatorRepository,
                organizationRepository,
                basicTaskWeightValidationService,
                taskRepository
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
}
