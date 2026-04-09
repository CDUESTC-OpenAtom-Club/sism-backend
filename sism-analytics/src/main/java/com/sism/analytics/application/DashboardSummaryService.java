package com.sism.analytics.application;

import com.sism.analytics.domain.repository.DashboardSummaryQueryRepository;
import com.sism.analytics.interfaces.dto.DashboardSummaryDTO;
import com.sism.analytics.interfaces.dto.DepartmentProgressDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DashboardSummaryService - 仪表盘汇总服务
 * Reads analytics-owned dashboard views through a query repository.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardSummaryService {

    private static final String ALERT_SEVERITY_CRITICAL = "CRITICAL";
    private static final String ALERT_SEVERITY_WARNING = "WARNING";
    private static final String ALERT_SEVERITY_INFO = "INFO";
    private static final String INDICATOR_TYPE_BASIC = "基础性指标";
    private static final String INDICATOR_TYPE_DEVELOPMENT = "发展性指标";
    private static final int RECENT_ACTIVITY_LIMIT = 20;

    private final DashboardSummaryQueryRepository dashboardSummaryQueryRepository;

    /**
     * Get dashboard summary aggregating indicator stats
     */
    @Cacheable(cacheNames = "dashboard-summary", key = "'summary'")
    public DashboardSummaryDTO getDashboardSummary() {
        DashboardSummaryQueryRepository.DashboardSummaryMetrics metrics =
                dashboardSummaryQueryRepository.fetchDashboardSummaryMetrics();

        long totalIndicators = metrics.totalIndicators();
        long completedIndicators = metrics.completedIndicators();
        double completionRate = totalIndicators > 0 ? metrics.averageProgress() : 0.0;
        double basicScore = metrics.basicScore();
        double developmentScore = metrics.developmentScore();
        double totalScore = totalIndicators > 0 ? (basicScore + developmentScore) / 2.0 : 0.0;
        long severeCount = metrics.severeAlerts();
        long moderateCount = metrics.moderateAlerts();
        long normalCount = metrics.normalAlerts();
        long warningCount = severeCount + moderateCount + normalCount;

        return DashboardSummaryDTO.builder()
                .totalScore(round2(totalScore))
                .basicScore(round2(basicScore))
                .developmentScore(round2(developmentScore))
                .completionRate(round2(completionRate))
                .warningCount(warningCount)
                .totalIndicators(totalIndicators)
                .completedIndicators(completedIndicators)
                .alertIndicators(DashboardSummaryDTO.AlertIndicators.builder()
                        .severe(severeCount)
                        .moderate(moderateCount)
                        .normal(normalCount)
                        .build())
                .build();
    }

    /**
     * Get department progress grouped by target_org_id
     */
    @SuppressWarnings("unchecked")
    @Cacheable(cacheNames = "department-progress", key = "'progress'")
    public List<DepartmentProgressDTO> getDepartmentProgress() {
        return dashboardSummaryQueryRepository.fetchDepartmentProgressRows().stream()
                .map(this::toDepartmentProgress)
                .toList();
    }

    /**
     * Get recent activities - recent indicator changes
     */
    @SuppressWarnings("unchecked")
    @Cacheable(cacheNames = "recent-activities", key = "'recent'")
    public List<Map<String, Object>> getRecentActivities() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DashboardSummaryQueryRepository.RecentActivityRow row : dashboardSummaryQueryRepository.fetchRecentActivityRows(RECENT_ACTIVITY_LIMIT)) {
            Map<String, Object> activity = new LinkedHashMap<>();
            activity.put("id", row.id());
            activity.put("description", row.description());
            activity.put("status", row.status());
            activity.put("progress", row.progress());
            activity.put("updatedAt", row.updatedAt());
            activity.put("orgName", row.orgName());
            result.add(activity);
        }
        return result;
    }

    private DepartmentProgressDTO toDepartmentProgress(DashboardSummaryQueryRepository.DepartmentProgressRow row) {
        double progress = row.averageProgress();
        return DepartmentProgressDTO.builder()
                .dept(row.departmentName())
                .progress(round2(progress))
                .score(round2(progress))
                .status(resolveDepartmentStatus(progress))
                .totalIndicators(row.totalIndicators())
                .completedIndicators(row.completedIndicators())
                .alertCount(row.alertCount())
                .build();
    }

    private String resolveDepartmentStatus(double progress) {
        if (progress >= 80) {
            return "on_track";
        }
        if (progress >= 50) {
            return "at_risk";
        }
        return "behind";
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
