package com.sism.strategy.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.common.PageResult;
import com.sism.iam.application.service.UserNotificationService;
import com.sism.organization.domain.repository.OrganizationRepository;
import com.sism.strategy.application.MilestoneApplicationService;
import com.sism.strategy.application.StrategyApplicationService;
import com.sism.strategy.domain.Indicator;
import com.sism.strategy.domain.enums.IndicatorStatus;
import com.sism.task.infrastructure.persistence.JpaTaskRepositoryInternal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndicatorControllerTest {

    private StrategyApplicationService strategyApplicationService;
    private MilestoneApplicationService milestoneApplicationService;
    private OrganizationRepository organizationRepository;
    private JpaTaskRepositoryInternal jpaTaskRepository;
    private JdbcTemplate jdbcTemplate;
    private UserNotificationService userNotificationService;
    private IndicatorController controller;

    @BeforeEach
    void setUp() {
        strategyApplicationService = mock(StrategyApplicationService.class);
        milestoneApplicationService = mock(MilestoneApplicationService.class);
        organizationRepository = mock(OrganizationRepository.class);
        jpaTaskRepository = mock(JpaTaskRepositoryInternal.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        userNotificationService = mock(UserNotificationService.class);

        controller = new IndicatorController(
                strategyApplicationService,
                milestoneApplicationService,
                organizationRepository,
                jpaTaskRepository,
                jdbcTemplate,
                userNotificationService
        );

        when(milestoneApplicationService.getAllMilestones()).thenReturn(List.of());
        stubJdbcQueries();
    }

    @Test
    @DisplayName("listIndicators should populate cycleId and year from task-plan-cycle chain")
    void shouldPopulateCycleIdAndYearWhenListingIndicators() {
        Indicator indicator = createIndicator(2004L, 41003L);
        when(strategyApplicationService.getIndicatorsByYear(2026, PageRequest.of(0, 1)))
                .thenReturn(new PageImpl<>(List.of(indicator), PageRequest.of(0, 1), 1));

        ResponseEntity<ApiResponse<PageResult<IndicatorController.IndicatorResponse>>> response =
                controller.listIndicators(0, 1, null, null, 2026);

        IndicatorController.IndicatorResponse item = response.getBody().getData().getItems().get(0);
        assertEquals(4L, item.getCycleId());
        assertEquals(2026, item.getYear());
        assertEquals(41003L, item.getTaskId());
        verify(strategyApplicationService).getIndicatorsByYear(2026, PageRequest.of(0, 1));
    }

    @Test
    @DisplayName("getIndicatorById should populate cycleId and year from task-plan-cycle chain")
    void shouldPopulateCycleIdAndYearWhenGettingById() {
        Indicator indicator = createIndicator(2005L, 41003L);
        when(strategyApplicationService.getIndicatorById(2005L)).thenReturn(indicator);

        ResponseEntity<ApiResponse<IndicatorController.IndicatorResponse>> response =
                controller.getIndicatorById(2005L);

        IndicatorController.IndicatorResponse item = response.getBody().getData();
        assertNotNull(item);
        assertEquals(4L, item.getCycleId());
        assertEquals(2026, item.getYear());
        assertEquals(41003L, item.getTaskId());
    }

    private void stubJdbcQueries() {
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            if (sql.contains("FROM public.sys_task")) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("task_id", 41003L);
                row.put("task_name", "重点任务");
                row.put("task_type", "QUANTITATIVE");
                row.put("cycle_id", 4L);
                row.put("year", 2026);
                return List.of(row);
            }
            if (sql.contains("FROM public.plan_report_indicator")) {
                return List.of();
            }
            return List.of();
        }).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));
    }

    private Indicator createIndicator(Long indicatorId, Long taskId) {
        Indicator indicator = new Indicator();
        indicator.setId(indicatorId);
        indicator.setTaskId(taskId);
        indicator.setIndicatorDesc("年度重点指标");
        indicator.setStatus(IndicatorStatus.DISTRIBUTED);
        indicator.setProgress(20);
        indicator.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        indicator.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 0, 0));
        indicator.setWeightPercent(java.math.BigDecimal.TEN);
        indicator.setSortOrder(1);
        indicator.setType("定量");
        return indicator;
    }
}
