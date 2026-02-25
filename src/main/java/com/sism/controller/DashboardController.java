package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.service.IndicatorService;
import com.sism.service.SysOrgService;
import com.sism.vo.IndicatorVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Dashboard Controller for SISM (Strategic Indicator Management System).
 * 
 * <p>This controller provides aggregated statistics and summary data for the
 * system dashboard. It calculates real-time metrics for strategic indicator
 * completion, departmental performance, and alert status.
 * 
 * <h2>Dashboard Metrics</h2>
 * <ul>
 *   <li><b>Completion Rate</b>: Percentage of completed indicators</li>
 *   <li><b>Total Score</b>: Weighted average progress (max 120 points)</li>
 *   <li><b>Warning Count</b>: Number of indicators requiring attention</li>
 *   <li><b>Alert Distribution</b>: Severe, moderate, and normal indicators</li>
 * </ul>
 * 
 * <h2>Alert Severity Levels</h2>
 * <ul>
 *   <li><b>Severe</b>: Progress < 30% (requires immediate action)</li>
 *   <li><b>Moderate</b>: Progress 30-59% (needs attention)</li>
 *   <li><b>Normal</b>: Progress >= 60% (on track)</li>
 * </ul>
 * 
 * <h2>Department Progress</h2>
 * <p>Calculates average progress and completion statistics for each department,
 * enabling comparative analysis and performance tracking across organizational units.
 * 
 * <h2>API Endpoints</h2>
 * <ul>
 *   <li>GET /api/dashboard/summary - Overall system summary</li>
 *   <li>GET /api/dashboard/department-progress - Department-level statistics</li>
 * </ul>
 * 
 * <h2>Response Example</h2>
 * <pre>
 * {
 *   "completionRate": 75.5,
 *   "totalScore": 90.6,
 *   "warningCount": 12,
 *   "totalIndicators": 100,
 *   "completedIndicators": 75,
 *   "alertIndicators": {
 *     "severe": 5,
 *     "moderate": 7,
 *     "normal": 88
 *   }
 * }
 * </pre>
 * 
 * @author SISM Development Team
 * @version 1.0
 * @since 1.0
 * @see com.sism.service.IndicatorService
 * @see com.sism.service.SysOrgService
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard and statistics endpoints")
public class DashboardController {

    private final IndicatorService indicatorService;
    private final SysOrgService orgService;

    /**
     * Get complete dashboard data
     * GET /api/dashboard
     */
    @GetMapping
    @Operation(summary = "Get complete dashboard data", description = "Retrieve all dashboard data including scores, completion rate, and department progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardData() {
        log.info("Fetching complete dashboard data");

        try {
            // Get all active indicators
            List<IndicatorVO> indicators = indicatorService.getAllActiveIndicators();

            // Calculate basic statistics
            int totalIndicators = indicators.size();
            long completedIndicators = indicators.stream()
                .filter(ind -> ind.getProgress() >= 100)
                .count();
            double avgProgress = indicators.stream()
                .mapToDouble(IndicatorVO::getProgress)
                .average()
                .orElse(0.0);

            // Calculate scores (basic and development)
            double basicScore = avgProgress * 0.6;  // 60% weight for basic indicators
            double developmentScore = avgProgress * 0.4;  // 40% weight for development indicators
            double totalScore = basicScore + developmentScore;

            // Calculate completion rate
            double completionRate = totalIndicators > 0
                ? (completedIndicators * 100.0 / totalIndicators)
                : 0.0;

            // Count alerts by severity
            Map<String, Long> alertCounts = indicators.stream()
                .collect(Collectors.groupingBy(
                    ind -> {
                        double progress = ind.getProgress();
                        if (progress < 30) return "severe";
                        else if (progress < 60) return "moderate";
                        else return "normal";
                    },
                    Collectors.counting()
                ));

            // Calculate warning count
            int warningCount = Math.toIntExact(
                alertCounts.getOrDefault("severe", 0L) +
                alertCounts.getOrDefault("moderate", 0L)
            );

            // Prepare alert indicators
            Map<String, Object> alertIndicators = new HashMap<>();
            alertIndicators.put("severe", alertCounts.getOrDefault("severe", 0L));
            alertIndicators.put("moderate", alertCounts.getOrDefault("moderate", 0L));
            alertIndicators.put("normal", alertCounts.getOrDefault("normal", 0L));

            // Prepare department progress
            List<Map<String, Object>> departmentProgress = indicators.stream()
                .collect(Collectors.groupingBy(
                    ind -> ind.getResponsibleDept() != null ? ind.getResponsibleDept() : "未分配",
                    Collectors.toList()
                ))
                .entrySet().stream()
                .map(entry -> {
                    List<IndicatorVO> deptIndicators = entry.getValue();
                    double deptAvgProgress = deptIndicators.stream()
                        .mapToDouble(IndicatorVO::getProgress)
                        .average()
                        .orElse(0.0);

                    Map<String, Object> deptData = new HashMap<>();
                    deptData.put("department", entry.getKey());
                    deptData.put("progress", Math.round(deptAvgProgress));
                    deptData.put("status",
                        deptAvgProgress >= 80 ? "success" :
                        deptAvgProgress >= 50 ? "warning" : "exception");
                    return deptData;
                })
                .collect(Collectors.toList());

            // Prepare tasks by status (placeholder data for now)
            Map<String, Object> tasksByStatus = new HashMap<>();
            tasksByStatus.put("draft", 0);
            tasksByStatus.put("active", totalIndicators);
            tasksByStatus.put("completed", Math.toIntExact(completedIndicators));
            tasksByStatus.put("cancelled", 0);

            // Prepare monthly progress (placeholder data)
            List<Map<String, Object>> monthlyProgress = new ArrayList<>();
            for (int month = 1; month <= 12; month++) {
                Map<String, Object> monthData = new HashMap<>();
                monthData.put("month", month);
                monthData.put("year", 2026);
                monthData.put("progress", Math.round(avgProgress * (month / 12.0)));
                monthData.put("target", 100);
                monthlyProgress.add(monthData);
            }

            // Build response
            Map<String, Object> dashboardData = new HashMap<>();
            dashboardData.put("totalScore", Math.round(totalScore * 100) / 100.0);
            dashboardData.put("basicScore", Math.round(basicScore * 100) / 100.0);
            dashboardData.put("developmentScore", Math.round(developmentScore * 100) / 100.0);
            dashboardData.put("completionRate", Math.round(completionRate * 100) / 100.0);
            dashboardData.put("warningCount", warningCount);
            dashboardData.put("totalIndicators", totalIndicators);
            dashboardData.put("completedIndicators", Math.toIntExact(completedIndicators));
            dashboardData.put("alertIndicators", alertIndicators);
            dashboardData.put("departmentProgress", departmentProgress);
            dashboardData.put("tasksByStatus", tasksByStatus);
            dashboardData.put("monthlyProgress", monthlyProgress);

            log.info("Dashboard data calculated successfully");
            return ResponseEntity.ok(ApiResponse.success(dashboardData));

        } catch (Exception e) {
            log.error("Error fetching dashboard data", e);
            throw e;
        }
    }

    /**
     * Get dashboard summary
     * GET /api/dashboard/summary
     */
    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary", description = "Retrieve dashboard summary with completion rate and alerts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardSummary() {
        log.info("Fetching dashboard summary");
        
        try {
            // Get all active indicators
            List<IndicatorVO> indicators = indicatorService.getAllActiveIndicators();
            
            // Calculate statistics
            int totalIndicators = indicators.size();
            double totalProgress = indicators.stream()
                .mapToDouble(IndicatorVO::getProgress)
                .average()
                .orElse(0.0);
            
            long completedCount = indicators.stream()
                .filter(i -> i.getProgress() >= 100)
                .count();
            
            double completionRate = totalIndicators > 0 
                ? (completedCount * 100.0 / totalIndicators) 
                : 0.0;
            
            // Calculate alert statistics
            long severeAlerts = indicators.stream()
                .filter(i -> i.getProgress() < 30)
                .count();
            
            long moderateAlerts = indicators.stream()
                .filter(i -> i.getProgress() >= 30 && i.getProgress() < 60)
                .count();
            
            long normalCount = indicators.stream()
                .filter(i -> i.getProgress() >= 60)
                .count();
            
            int warningCount = (int)(severeAlerts + moderateAlerts);
            
            // Calculate total score (weighted average)
            double totalScore = totalProgress * 1.2; // Max score 120
            
            // Build response
            Map<String, Object> summary = new HashMap<>();
            summary.put("completionRate", Math.round(completionRate * 10) / 10.0);
            summary.put("totalScore", Math.round(totalScore * 10) / 10.0);
            summary.put("warningCount", warningCount);
            summary.put("totalIndicators", totalIndicators);
            summary.put("completedIndicators", completedCount);
            
            Map<String, Long> alertIndicators = new HashMap<>();
            alertIndicators.put("severe", severeAlerts);
            alertIndicators.put("moderate", moderateAlerts);
            alertIndicators.put("normal", normalCount);
            summary.put("alertIndicators", alertIndicators);
            
            log.info("Dashboard summary calculated: {} indicators, {}% completion", 
                totalIndicators, Math.round(completionRate));
            
            return ResponseEntity.ok(ApiResponse.success(summary));
        } catch (Exception e) {
            log.error("Error fetching dashboard summary", e);
            throw e;
        }
    }

    /**
     * Get dashboard stats (alias for summary)
     * GET /api/dashboard/stats
     */
    @GetMapping("/stats")
    @Operation(summary = "Get dashboard stats", description = "Retrieve dashboard statistics (alias for summary)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats(
            @RequestParam(required = false) Integer year) {
        log.info("Fetching dashboard stats for year: {}", year);
        // For now, return same as summary (year filtering can be added later)
        return getDashboardSummary();
    }

    /**
     * Get department progress
     * GET /api/dashboard/department-progress
     */
    @GetMapping("/department-progress")
    @Operation(summary = "Get department progress", description = "Retrieve progress statistics by department")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDepartmentProgress() {
        log.info("Fetching department progress");

        try {
            List<IndicatorVO> indicators = indicatorService.getAllActiveIndicators();

            // Group by target organization (responsible department)
            Map<String, List<IndicatorVO>> byDept = indicators.stream()
                .filter(i -> i.getTargetOrgId() != null)
                .collect(Collectors.groupingBy(i -> {
                    // Use organization ID as key since we need org name mapping
                    return String.valueOf(i.getTargetOrgId());
                }));

            List<Map<String, Object>> deptProgress = new ArrayList<>();

            byDept.forEach((deptId, deptIndicators) -> {
                double avgProgress = deptIndicators.stream()
                    .mapToDouble(IndicatorVO::getProgress)
                    .average()
                    .orElse(0.0);

                long alertCount = deptIndicators.stream()
                    .filter(i -> i.getProgress() < 60)
                    .count();

                long completed = deptIndicators.stream()
                    .filter(i -> i.getProgress() >= 100)
                    .count();

                Map<String, Object> deptData = new HashMap<>();
                deptData.put("deptId", Long.valueOf(deptId));
                deptData.put("dept", deptIndicators.get(0).getResponsibleDept() != null
                    ? deptIndicators.get(0).getResponsibleDept()
                    : "部门 " + deptId);
                deptData.put("progress", Math.round(avgProgress));
                deptData.put("score", Math.round(avgProgress * 1.2));
                deptData.put("status", avgProgress >= 80 ? "success" : avgProgress >= 50 ? "warning" : "exception");
                deptData.put("totalIndicators", deptIndicators.size());
                deptData.put("completedIndicators", completed);
                deptData.put("alertCount", alertCount);

                deptProgress.add(deptData);
            });

            // Sort by progress descending
            deptProgress.sort((a, b) ->
                Long.compare((Long)b.get("progress"), (Long)a.get("progress")));

            log.info("Department progress calculated for {} departments", deptProgress.size());

            return ResponseEntity.ok(ApiResponse.success(deptProgress));
        } catch (Exception e) {
            log.error("Error fetching department progress", e);
            throw e;
        }
    }

    /**
     * Get recent activities
     * GET /api/dashboard/recent-activities
     */
    @GetMapping("/recent-activities")
    @Operation(summary = "Get recent activities", description = "Retrieve recent system activities")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentActivities() {
        log.info("Fetching recent activities");

        // Note: Activity tracking requires audit log integration
        // This endpoint can be extended to fetch recent activities from AuditLog
        List<Map<String, Object>> activities = new ArrayList<>();

        return ResponseEntity.ok(ApiResponse.success(activities));
    }
}
