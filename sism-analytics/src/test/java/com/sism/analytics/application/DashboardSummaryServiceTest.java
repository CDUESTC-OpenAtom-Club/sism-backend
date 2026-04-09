package com.sism.analytics.application;

import com.sism.analytics.domain.repository.DashboardSummaryQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DashboardSummaryServiceTest {

    @Test
    @DisplayName("getDashboardSummary should use canonical alert severities")
    void getDashboardSummaryShouldUseCanonicalAlertSeverities() {
        DashboardSummaryQueryRepository queryRepository = mock(DashboardSummaryQueryRepository.class);
        DashboardSummaryService service = new DashboardSummaryService(queryRepository);

        when(queryRepository.fetchDashboardSummaryMetrics()).thenReturn(
                new DashboardSummaryQueryRepository.DashboardSummaryMetrics(
                        10L, 4L, 60.0, 70.0, 50.0, 2L, 3L, 5L
                )
        );

        var result = service.getDashboardSummary();

        assertEquals(2L, result.getAlertIndicators().getSevere());
        assertEquals(3L, result.getAlertIndicators().getModerate());
        assertEquals(5L, result.getAlertIndicators().getNormal());
        assertEquals(10L, result.getWarningCount());
        verify(queryRepository).fetchDashboardSummaryMetrics();
        verifyNoMoreInteractions(queryRepository);
    }

    @Test
    @DisplayName("getDepartmentProgress should map repository rows into DTOs")
    void getDepartmentProgressShouldMapRows() {
        DashboardSummaryQueryRepository queryRepository = mock(DashboardSummaryQueryRepository.class);
        DashboardSummaryService service = new DashboardSummaryService(queryRepository);

        when(queryRepository.fetchDepartmentProgressRows()).thenReturn(List.of(
                new DashboardSummaryQueryRepository.DepartmentProgressRow("部门A", 82.36, 10, 8, 1),
                new DashboardSummaryQueryRepository.DepartmentProgressRow("部门B", 49.9, 5, 1, 2)
        ));

        var result = service.getDepartmentProgress();

        assertEquals(2, result.size());
        assertEquals("on_track", result.get(0).getStatus());
        assertEquals(82.36, result.get(0).getProgress());
        assertEquals("behind", result.get(1).getStatus());
    }

    @Test
    @DisplayName("getRecentActivities should preserve repository ordering and fields")
    void getRecentActivitiesShouldMapRows() {
        DashboardSummaryQueryRepository queryRepository = mock(DashboardSummaryQueryRepository.class);
        DashboardSummaryService service = new DashboardSummaryService(queryRepository);

        when(queryRepository.fetchRecentActivityRows(20)).thenReturn(List.of(
                new DashboardSummaryQueryRepository.RecentActivityRow(1L, "指标A", "COMPLETED", 88.0, "2026-04-07T01:00:00", "职能部门")
        ));

        var result = service.getRecentActivities();

        assertEquals(1, result.size());
        assertEquals("指标A", result.get(0).get("description"));
        assertEquals("职能部门", result.get(0).get("orgName"));
    }
}
