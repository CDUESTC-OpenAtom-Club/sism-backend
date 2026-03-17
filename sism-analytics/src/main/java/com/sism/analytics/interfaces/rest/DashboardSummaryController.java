package com.sism.analytics.interfaces.rest;

import com.sism.analytics.application.DashboardSummaryService;
import com.sism.analytics.interfaces.dto.DashboardSummaryDTO;
import com.sism.analytics.interfaces.dto.DepartmentProgressDTO;
import com.sism.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * DashboardSummaryController - 仪表盘汇总API
 * Provides dashboard summary endpoints consumed by the frontend
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard summary and analytics endpoints")
public class DashboardSummaryController {

    private final DashboardSummaryService dashboardSummaryService;

    @GetMapping
    @Operation(summary = "Get dashboard summary", description = "Get aggregated dashboard data including scores, completion rates, and alert counts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DashboardSummaryDTO>> getDashboardSummary() {
        DashboardSummaryDTO summary = dashboardSummaryService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/department-progress")
    @Operation(summary = "Get department progress", description = "Get progress data grouped by department/organization")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DepartmentProgressDTO>>> getDepartmentProgress() {
        List<DepartmentProgressDTO> progress = dashboardSummaryService.getDepartmentProgress();
        return ResponseEntity.ok(ApiResponse.success(progress));
    }

    @GetMapping("/recent-activities")
    @Operation(summary = "Get recent activities", description = "Get recent indicator changes and updates")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentActivities() {
        List<Map<String, Object>> activities = dashboardSummaryService.getRecentActivities();
        return ResponseEntity.ok(ApiResponse.success(activities));
    }
}
