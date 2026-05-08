package com.sism.strategy.application;

import com.sism.organization.domain.OrgType;
import com.sism.organization.domain.SysOrg;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import com.sism.strategy.domain.indicator.Indicator;
import com.sism.strategy.domain.repository.IndicatorRepository;
import com.sism.task.domain.task.StrategicTask;
import com.sism.task.domain.task.TaskType;
import com.sism.task.domain.repository.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Strategy Application Service Tests")
class StrategyApplicationServiceTest {

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private EventStore eventStore;

    @Mock
    private IndicatorRepository indicatorRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private BasicTaskWeightValidationService basicTaskWeightValidationService;

    @Test
    @DisplayName("Should keep provided task id when creating indicator for functional to college flow")
    void shouldKeepProvidedTaskIdWhenCreatingIndicator() {
        StrategyApplicationService service = new StrategyApplicationService(
                eventPublisher,
                eventStore,
                indicatorRepository,
                taskRepository,
                basicTaskWeightValidationService
        );

        SysOrg ownerOrg = SysOrg.create("发展规划处", OrgType.functional);
        ownerOrg.setId(41L);
        SysOrg targetOrg = SysOrg.create("计算机学院", OrgType.academic);
        targetOrg.setId(61L);

        when(indicatorRepository.save(any(Indicator.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createIndicator(
                "新增指标",
                ownerOrg,
                targetOrg,
                901L,
                null,
                "定量",
                BigDecimal.valueOf(20),
                1,
                "备注",
                0
        );

        ArgumentCaptor<Indicator> indicatorCaptor = ArgumentCaptor.forClass(Indicator.class);
        verify(indicatorRepository).save(indicatorCaptor.capture());
        assertEquals(901L, indicatorCaptor.getValue().getTaskId());
        verify(taskRepository, never()).findByPlanId(any());
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should not remap task id or create plan task during distribution")
    void shouldNotRemapTaskIdOrCreatePlanTaskDuringDistribution() {
        StrategyApplicationService service = new StrategyApplicationService(
                eventPublisher,
                eventStore,
                indicatorRepository,
                taskRepository,
                basicTaskWeightValidationService
        );

        SysOrg ownerOrg = SysOrg.create("发展规划处", OrgType.functional);
        ownerOrg.setId(41L);
        SysOrg sourceTargetOrg = SysOrg.create("发展规划处", OrgType.functional);
        sourceTargetOrg.setId(41L);
        SysOrg collegeOrg = SysOrg.create("计算机学院", OrgType.academic);
        collegeOrg.setId(61L);

        Indicator indicator = Indicator.create("待下发指标", ownerOrg, sourceTargetOrg, "定量");
        indicator.setId(301L);
        indicator.setTaskId(902L);

        StrategicTask sourceTask = StrategicTask.create(
                "既有任务",
                TaskType.BASIC,
                Long.valueOf(7001L),
                Long.valueOf(2026L),
                sourceTargetOrg,
                ownerOrg
        );
        sourceTask.setId(902L);

        when(indicatorRepository.findById(301L)).thenReturn(Optional.of(indicator));
        when(taskRepository.findById(902L)).thenReturn(Optional.of(sourceTask));
        when(indicatorRepository.save(any(Indicator.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.distributeIndicator(301L, collegeOrg, null);

        ArgumentCaptor<Indicator> indicatorCaptor = ArgumentCaptor.forClass(Indicator.class);
        verify(indicatorRepository).save(indicatorCaptor.capture());
        assertEquals(902L, indicatorCaptor.getValue().getTaskId());
        verify(taskRepository, never()).findByPlanId(any());
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when indicator is missing instead of returning null")
    void shouldThrowWhenIndicatorMissing() {
        StrategyApplicationService service = new StrategyApplicationService(
                eventPublisher,
                eventStore,
                indicatorRepository,
                taskRepository,
                basicTaskWeightValidationService
        );

        when(indicatorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getIndicatorById(999L));
    }
}
