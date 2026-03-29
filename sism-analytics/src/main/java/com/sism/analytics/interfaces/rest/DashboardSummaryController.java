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
@Tag(name = "仪表盘汇总", description = "仪表盘汇总和分析接口")
public class DashboardSummaryController {

    private final DashboardSummaryService dashboardSummaryService;

    @GetMapping
    @Operation(summary = "获取仪表盘汇总", description = "获取聚合的仪表盘数据，包括得分、完成率和预警数量")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DashboardSummaryDTO>> getDashboardSummary() {
        DashboardSummaryDTO summary = dashboardSummaryService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/department-progress")
    @Operation(summary = "获取部门进度", description = "获取按部门/组织分组的进度数据")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DepartmentProgressDTO>>> getDepartmentProgress() {
        List<DepartmentProgressDTO> progress = dashboardSummaryService.getDepartmentProgress();
        return ResponseEntity.ok(ApiResponse.success(progress));
    }

    @GetMapping("/recent-activities")
    @Operation(summary = "获取最近活动", description = "获取最近的指标变更和更新")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentActivities() {
        List<Map<String, Object>> activities = dashboardSummaryService.getRecentActivities();
        return ResponseEntity.ok(ApiResponse.success(activities));
    }
}
