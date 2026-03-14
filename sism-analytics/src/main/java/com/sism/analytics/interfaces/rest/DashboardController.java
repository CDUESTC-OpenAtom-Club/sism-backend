package com.sism.analytics.interfaces.rest;

import com.sism.analytics.application.DashboardApplicationService;
import com.sism.analytics.domain.Dashboard;
import com.sism.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * DashboardController - 仪表板API控制器
 * 提供仪表板管理相关的REST API端点
 */
@RestController
@RequestMapping("/api/v1/analytics/dashboard")
@RequiredArgsConstructor
@Tag(name = "Analytics Dashboards", description = "Dashboard management endpoints")
public class DashboardController {

    private final DashboardApplicationService dashboardApplicationService;

    // ==================== Dashboard Endpoints ====================

    @PostMapping
    @Operation(summary = "Create a new dashboard")
    public ResponseEntity<ApiResponse<Dashboard>> createDashboard(@RequestBody CreateDashboardRequest request) {
        Dashboard dashboard = dashboardApplicationService.createDashboard(
                request.getName(),
                request.getDescription(),
                request.getUserId(),
                request.getIsPublic() != null && request.getIsPublic(),
                request.getConfig()
        );
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update dashboard information")
    public ResponseEntity<ApiResponse<Dashboard>> updateDashboard(
            @PathVariable Long id,
            @RequestBody UpdateDashboardRequest request) {
        Dashboard dashboard = dashboardApplicationService.updateDashboard(
                id,
                request.getName(),
                request.getDescription(),
                request.getIsPublic() != null && request.getIsPublic(),
                request.getConfig()
        );
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @PutMapping("/{id}/config")
    @Operation(summary = "Update dashboard configuration")
    public ResponseEntity<ApiResponse<Dashboard>> updateDashboardConfig(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        Dashboard dashboard = dashboardApplicationService.updateDashboardConfig(id, request.get("config"));
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @PostMapping("/{id}/make-public")
    @Operation(summary = "Make dashboard public")
    public ResponseEntity<ApiResponse<Dashboard>> makePublic(@PathVariable Long id) {
        Dashboard dashboard = dashboardApplicationService.makePublic(id);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @PostMapping("/{id}/make-private")
    @Operation(summary = "Make dashboard private")
    public ResponseEntity<ApiResponse<Dashboard>> makePrivate(@PathVariable Long id) {
        Dashboard dashboard = dashboardApplicationService.makePrivate(id);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a dashboard")
    public ResponseEntity<ApiResponse<Void>> deleteDashboard(@PathVariable Long id) {
        dashboardApplicationService.deleteDashboard(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/copy")
    @Operation(summary = "Copy dashboard to another user")
    public ResponseEntity<ApiResponse<Dashboard>> copyDashboard(
            @PathVariable Long id,
            @RequestBody Map<String, Long> request) {
        Dashboard dashboard = dashboardApplicationService.copyDashboardToUser(id, request.get("targetUserId"));
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dashboard by ID")
    public ResponseEntity<ApiResponse<Dashboard>> getDashboardById(@PathVariable Long id) {
        return dashboardApplicationService.findDashboardById(id)
                .map(dashboard -> ResponseEntity.ok(ApiResponse.success(dashboard)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get dashboards by user ID")
    public ResponseEntity<ApiResponse<List<Dashboard>>> getDashboardsByUserId(@PathVariable Long userId) {
        List<Dashboard> dashboards = dashboardApplicationService.findDashboardsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(dashboards));
    }

    @GetMapping("/user/{userId}/public")
    @Operation(summary = "Get public dashboards by user ID")
    public ResponseEntity<ApiResponse<List<Dashboard>>> getPublicDashboardsByUserId(@PathVariable Long userId) {
        List<Dashboard> dashboards = dashboardApplicationService.findPublicDashboardsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(dashboards));
    }

    @GetMapping("/public")
    @Operation(summary = "Get all public dashboards")
    public ResponseEntity<ApiResponse<List<Dashboard>>> getAllPublicDashboards() {
        List<Dashboard> dashboards = dashboardApplicationService.findAllPublicDashboards();
        return ResponseEntity.ok(ApiResponse.success(dashboards));
    }

    @GetMapping("/user/{userId}/search")
    @Operation(summary = "Search dashboards by name")
    public ResponseEntity<ApiResponse<List<Dashboard>>> searchDashboardsByName(
            @PathVariable Long userId,
            @RequestParam String name) {
        List<Dashboard> dashboards = dashboardApplicationService.searchDashboardsByName(userId, name);
        return ResponseEntity.ok(ApiResponse.success(dashboards));
    }

    @GetMapping("/count/user/{userId}")
    @Operation(summary = "Count dashboards by user ID")
    public ResponseEntity<ApiResponse<Long>> countDashboardsByUserId(@PathVariable Long userId) {
        long count = dashboardApplicationService.countDashboardsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/count/public")
    @Operation(summary = "Count public dashboards")
    public ResponseEntity<ApiResponse<Long>> countPublicDashboards() {
        long count = dashboardApplicationService.countPublicDashboards();
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    // ==================== Request DTOs ====================

    @lombok.Data
    public static class CreateDashboardRequest {
        private String name;
        private String description;
        private Long userId;
        private Boolean isPublic;
        private String config;
    }

    @lombok.Data
    public static class UpdateDashboardRequest {
        private String name;
        private String description;
        private Boolean isPublic;
        private String config;
    }
}
