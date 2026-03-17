package com.sism.analytics.application;

import com.sism.analytics.interfaces.dto.DashboardSummaryDTO;
import com.sism.analytics.interfaces.dto.DepartmentProgressDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * DashboardSummaryService - 仪表盘汇总服务
 * Uses native SQL against indicator, sys_org, alert_event tables
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class DashboardSummaryService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Get dashboard summary aggregating indicator stats
     */
    public DashboardSummaryDTO getDashboardSummary() {
        // Count total and completed indicators
        long totalIndicators = countScalar(
                "SELECT COUNT(*) FROM indicator WHERE is_deleted = false");
        long completedIndicators = countScalar(
                "SELECT COUNT(*) FROM indicator WHERE is_deleted = false AND status = 'COMPLETED'");

        // Calculate average progress as completion rate
        double completionRate = 0.0;
        if (totalIndicators > 0) {
            Object avgResult = entityManager.createNativeQuery(
                    "SELECT COALESCE(AVG(progress), 0) FROM indicator WHERE is_deleted = false")
                    .getSingleResult();
            completionRate = toDouble(avgResult);
        }

        // Calculate scores by type
        double basicScore = queryAverageProgress("基础性指标");
        double developmentScore = queryAverageProgress("发展性指标");
        double totalScore = totalIndicators > 0 ? (basicScore + developmentScore) / 2.0 : 0.0;

        // Count alerts by severity from alert_event
        long severeCount = countScalar(
                "SELECT COUNT(*) FROM alert_event WHERE severity = 'CRITICAL' AND status != 'RESOLVED'");
        long moderateCount = countScalar(
                "SELECT COUNT(*) FROM alert_event WHERE severity = 'MAJOR' AND status != 'RESOLVED'");
        long normalCount = countScalar(
                "SELECT COUNT(*) FROM alert_event WHERE severity = 'MINOR' AND status != 'RESOLVED'");
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
    public List<DepartmentProgressDTO> getDepartmentProgress() {
        String sql = """
                SELECT
                    o.name AS dept,
                    COALESCE(AVG(i.progress), 0) AS avg_progress,
                    COUNT(i.id) AS total_indicators,
                    COUNT(CASE WHEN i.status = 'COMPLETED' THEN 1 END) AS completed_indicators,
                    COALESCE((SELECT COUNT(*) FROM alert_event ae
                              WHERE ae.indicator_id IN (SELECT i2.id FROM indicator i2 WHERE i2.target_org_id = o.id AND i2.is_deleted = false)
                              AND ae.status != 'RESOLVED'), 0) AS alert_count
                FROM sys_org o
                LEFT JOIN indicator i ON i.target_org_id = o.id AND i.is_deleted = false
                WHERE o.is_deleted = false AND o.is_active = true
                GROUP BY o.id, o.name
                ORDER BY o.name
                """;

        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
        List<DepartmentProgressDTO> result = new ArrayList<>();

        for (Object[] row : rows) {
            String dept = (String) row[0];
            double progress = toDouble(row[1]);
            long total = toLong(row[2]);
            long completed = toLong(row[3]);
            long alertCount = toLong(row[4]);

            double score = progress; // score equals avg progress
            String status;
            if (progress >= 80) {
                status = "on_track";
            } else if (progress >= 50) {
                status = "at_risk";
            } else {
                status = "behind";
            }

            result.add(DepartmentProgressDTO.builder()
                    .dept(dept)
                    .progress(round2(progress))
                    .score(round2(score))
                    .status(status)
                    .totalIndicators(total)
                    .completedIndicators(completed)
                    .alertCount(alertCount)
                    .build());
        }

        return result;
    }

    /**
     * Get recent activities - recent indicator changes
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRecentActivities() {
        String sql = """
                SELECT i.id, i.indicator_desc, i.status, i.progress, i.updated_at,
                       o.name AS org_name
                FROM indicator i
                LEFT JOIN sys_org o ON o.id = i.target_org_id
                WHERE i.is_deleted = false AND i.updated_at IS NOT NULL
                ORDER BY i.updated_at DESC
                LIMIT 20
                """;

        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] row : rows) {
            Map<String, Object> activity = new LinkedHashMap<>();
            activity.put("id", toLong(row[0]));
            activity.put("description", row[1]);
            activity.put("status", row[2]);
            activity.put("progress", toDouble(row[3]));
            activity.put("updatedAt", row[4] != null ? row[4].toString() : null);
            activity.put("orgName", row[5]);
            result.add(activity);
        }

        return result;
    }

    // ==================== Helper methods ====================

    private double queryAverageProgress(String type) {
        try {
            Object result = entityManager.createNativeQuery(
                            "SELECT COALESCE(AVG(progress), 0) FROM indicator WHERE is_deleted = false AND type = :type")
                    .setParameter("type", type)
                    .getSingleResult();
            return toDouble(result);
        } catch (Exception e) {
            log.debug("No indicators found for type: {}", type);
            return 0.0;
        }
    }

    private long countScalar(String sql) {
        Object result = entityManager.createNativeQuery(sql).getSingleResult();
        return toLong(result);
    }

    private static long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(value.toString());
    }

    private static double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number n) return n.doubleValue();
        return Double.parseDouble(value.toString());
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
