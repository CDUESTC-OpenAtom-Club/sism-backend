package com.sism.strategy.application;

import com.sism.organization.domain.OrgType;
import com.sism.organization.domain.SysOrg;
import com.sism.strategy.domain.Indicator;
import com.sism.strategy.domain.repository.IndicatorRepository;
import com.sism.strategy.domain.repository.PlanRepository;
import com.sism.task.domain.StrategicTask;
import com.sism.task.domain.TaskType;
import com.sism.task.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Basic Task Weight Validation Service Tests")
class BasicTaskWeightValidationServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private IndicatorRepository indicatorRepository;

    @Mock
    private PlanRepository planRepository;

    private BasicTaskWeightValidationService service;

    @BeforeEach
    void setUp() {
        service = new BasicTaskWeightValidationService(taskRepository, indicatorRepository, planRepository);
    }

    @Test
    @DisplayName("Should pass when basic root indicator weights sum to 100")
    void shouldPassWhenBasicWeightEquals100() {
        StrategicTask basicTask = buildTask(1001L, 7L, TaskType.BASIC);
        Indicator indicatorA = buildIndicator(2001L, 1001L, 49L, BigDecimal.valueOf(40));
        Indicator indicatorB = buildIndicator(2002L, 1001L, 49L, BigDecimal.valueOf(60));

        when(taskRepository.findByPlanId(7L)).thenReturn(List.of(basicTask));
        when(indicatorRepository.findAll()).thenReturn(List.of(indicatorA, indicatorB));

        assertDoesNotThrow(() -> service.validatePlanBasicWeight(7L, 49L));
    }

    @Test
    @DisplayName("Should fail when basic root indicator weights do not sum to 100")
    void shouldFailWhenBasicWeightNotEquals100() {
        StrategicTask basicTask = buildTask(1001L, 7L, TaskType.BASIC);
        Indicator indicatorA = buildIndicator(2001L, 1001L, 49L, BigDecimal.valueOf(25));
        Indicator indicatorB = buildIndicator(2002L, 1001L, 49L, BigDecimal.valueOf(25));

        when(taskRepository.findByPlanId(7L)).thenReturn(List.of(basicTask));
        when(indicatorRepository.findAll()).thenReturn(List.of(indicatorA, indicatorB));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.validatePlanBasicWeight(7L, 49L)
        );

        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("基础性任务指标权重合计必须为100"));
    }

    private StrategicTask buildTask(Long taskId, Long planId, TaskType taskType) {
        SysOrg org = SysOrg.create("战略发展部", OrgType.admin);
        StrategicTask task = StrategicTask.create("测试任务", taskType, planId, 1L, org, org);
        task.setId(taskId);
        task.setPlanId(planId);
        task.setTaskType(taskType);
        task.setIsDeleted(false);
        return task;
    }

    private Indicator buildIndicator(Long id, Long taskId, Long targetOrgId, BigDecimal weight) {
        SysOrg ownerOrg = SysOrg.create("战略发展部", OrgType.admin);
        SysOrg targetOrg = SysOrg.create("实验室建设管理处", OrgType.admin);
        targetOrg.setId(targetOrgId);

        Indicator indicator = Indicator.create("测试指标", ownerOrg, targetOrg, "定量");
        indicator.setId(id);
        indicator.setTaskId(taskId);
        indicator.setParent(null);
        indicator.setWeightPercent(weight);
        indicator.setIsDeleted(false);
        return indicator;
    }
}
