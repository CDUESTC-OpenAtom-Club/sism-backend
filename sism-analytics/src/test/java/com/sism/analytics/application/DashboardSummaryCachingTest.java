package com.sism.analytics.application;

import com.sism.analytics.domain.repository.DashboardSummaryQueryRepository;
import com.sism.analytics.interfaces.dto.DashboardSummaryDTO;
import com.sism.analytics.interfaces.dto.DepartmentProgressDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = DashboardSummaryCachingTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DashboardSummaryCachingTest {

    @Autowired
    private DashboardSummaryService dashboardSummaryService;

    @Autowired
    private DashboardSummaryQueryRepository dashboardSummaryQueryRepository;

    @Test
    void dashboardSummaryShouldBeCachedAcrossRepeatedCalls() {
        when(dashboardSummaryQueryRepository.fetchDashboardSummaryMetrics())
                .thenReturn(new DashboardSummaryQueryRepository.DashboardSummaryMetrics(
                        10L, 4L, 60.0, 70.0, 50.0, 2L, 3L, 5L
                ));

        DashboardSummaryDTO first = dashboardSummaryService.getDashboardSummary();
        DashboardSummaryDTO second = dashboardSummaryService.getDashboardSummary();

        assertEquals(first, second);
        verify(dashboardSummaryQueryRepository, times(1)).fetchDashboardSummaryMetrics();
    }

    @Test
    void departmentProgressShouldBeCachedAcrossRepeatedCalls() {
        when(dashboardSummaryQueryRepository.fetchDepartmentProgressRows())
                .thenReturn(List.of(
                        new DashboardSummaryQueryRepository.DepartmentProgressRow("部门A", 82.36, 10, 8, 1)
                ));

        List<DepartmentProgressDTO> first = dashboardSummaryService.getDepartmentProgress();
        List<DepartmentProgressDTO> second = dashboardSummaryService.getDepartmentProgress();

        assertEquals(first, second);
        verify(dashboardSummaryQueryRepository, times(1)).fetchDepartmentProgressRows();
    }

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean("analyticsCacheManager")
        CacheManager analyticsCacheManager() {
            return new ConcurrentMapCacheManager("dashboard-summary", "department-progress", "recent-activities");
        }

        @Bean
        DashboardSummaryQueryRepository dashboardSummaryQueryRepository() {
            return mock(DashboardSummaryQueryRepository.class);
        }

        @Bean
        DashboardSummaryService dashboardSummaryService(DashboardSummaryQueryRepository dashboardSummaryQueryRepository) {
            return new DashboardSummaryService(dashboardSummaryQueryRepository, analyticsCacheManager());
        }
    }
}
