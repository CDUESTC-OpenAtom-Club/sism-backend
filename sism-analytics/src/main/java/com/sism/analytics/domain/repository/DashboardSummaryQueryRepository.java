package com.sism.analytics.domain.repository;

import java.util.List;

public interface DashboardSummaryQueryRepository {

    long countActiveIndicators();

    long countCompletedIndicators();

    double averageActiveProgress();

    double averageActiveProgressByType(String type);

    long countUnresolvedAlertsBySeverity(String severity);

    DashboardSummaryMetrics fetchDashboardSummaryMetrics();

    List<DepartmentProgressRow> fetchDepartmentProgressRows();

    List<RecentActivityRow> fetchRecentActivityRows(int limit);

    record DepartmentProgressRow(
            String departmentName,
            double averageProgress,
            long totalIndicators,
            long completedIndicators,
            long alertCount
    ) {
    }

    record RecentActivityRow(
            long id,
            String description,
            String status,
            double progress,
            String updatedAt,
            String orgName
    ) {
    }

    record DashboardSummaryMetrics(
            long totalIndicators,
            long completedIndicators,
            double averageProgress,
            double basicScore,
            double developmentScore,
            long severeAlerts,
            long moderateAlerts,
            long normalAlerts
    ) {
    }
}
