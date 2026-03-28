package com.sism.analytics.interfaces.rest;

import com.sism.analytics.application.DashboardApplicationService;
import com.sism.analytics.domain.Dashboard;
import com.sism.analytics.interfaces.dto.CreateDashboardRequest;
import com.sism.analytics.interfaces.dto.DashboardDTO;
import com.sism.analytics.interfaces.dto.UpdateDashboardRequest;
import com.sism.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DashboardController - 仪表板API控制器
 * 提供仪表板管理相关的REST API端点
 */
@RestController
@RequestMapping("/api/v1/analytics/dashboard")
@RequiredArgsConstructor
@Tag(name = "分析仪表盘", description = "仪表盘管理接口")
public class DashboardController {

    private final DashboardApplicationService dashboardApplicationService;

    // ==================== Dashboard Endpoints ====================

    @PostMapping
    @Operation(summary = "创建新仪表盘")
    public ResponseEntity<ApiResponse<DashboardDTO>> createDashboard(@RequestBody CreateDashboardRequest request) {
        Dashboard dashboard = dashboardApplicationService.createDashboard(
                request.getName(),
                request.getDescription(),
                request.getUserId(),
                request.getIsPublic() != null && request.getIsPublic(),
                request.getConfig()
        );
        return ResponseEntity.ok(ApiResponse.success(toDashboardDTO(dashboard)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新仪表盘信息")
    public ResponseEntity<ApiResponse<DashboardDTO>> updateDashboard(
            @PathVariable Long id,
            @RequestBody UpdateDashboardRequest request) {
        Dashboard dashboard = dashboardApplicationService.updateDashboard(
                id,
                request.getName(),
                request.getDescription(),
                request.getIsPublic() != null && request.getIsPublic(),
                request.getConfig()
        );
        return ResponseEntity.ok(ApiResponse.success(toDashboardDTO(dashboard)));
    }

    @PutMapping("/{id}/config")
    @Operation(summary = "更新仪表盘配置")
    public ResponseEntity<ApiResponse<DashboardDTO>> updateDashboardConfig(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        Dashboard dashboard = dashboardApplicationService.updateDashboardConfig(id, request.get("config"));
        return ResponseEntity.ok(ApiResponse.success(toDashboardDTO(dashboard)));
    }

    @PostMapping("/{id}/make-public")
    @Operation(summary = "将仪表盘设为公开")
    public ResponseEntity<ApiResponse<DashboardDTO>> makePublic(@PathVariable Long id) {
        Dashboard dashboard = dashboardApplicationService.makePublic(id);
        return ResponseEntity.ok(ApiResponse.success(toDashboardDTO(dashboard)));
    }

    @PostMapping("/{id}/make-private")
    @Operation(summary = "将仪表盘设为私有")
    public ResponseEntity<ApiResponse<DashboardDTO>> makePrivate(@PathVariable Long id) {
        Dashboard dashboard = dashboardApplicationService.makePrivate(id);
        return ResponseEntity.ok(ApiResponse.success(toDashboardDTO(dashboard)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除仪表盘")
    public ResponseEntity<ApiResponse<Void>> deleteDashboard(@PathVariable Long id) {
        dashboardApplicationService.deleteDashboard(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/copy")
    @Operation(summary = "复制仪表盘给其他用户")
    public ResponseEntity<ApiResponse<DashboardDTO>> copyDashboard(
            @PathVariable Long id,
            @RequestBody Map<String, Long> request) {
        Dashboard dashboard = dashboardApplicationService.copyDashboardToUser(id, request.get("targetUserId"));
        return ResponseEntity.ok(ApiResponse.success(toDashboardDTO(dashboard)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取仪表盘")
    public ResponseEntity<ApiResponse<DashboardDTO>> getDashboardById(@PathVariable Long id) {
        return dashboardApplicationService.findDashboardById(id)
                .map(dashboard -> ResponseEntity.ok(ApiResponse.success(toDashboardDTO(dashboard))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "根据用户ID获取仪表盘")
    public ResponseEntity<ApiResponse<List<DashboardDTO>>> getDashboardsByUserId(@PathVariable Long userId) {
        List<Dashboard> dashboards = dashboardApplicationService.findDashboardsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(dashboards.stream().map(this::toDashboardDTO).collect(Collectors.toList())));
    }

    @GetMapping("/user/{userId}/public")
    @Operation(summary = "根据用户ID获取公开仪表盘")
    public ResponseEntity<ApiResponse<List<DashboardDTO>>> getPublicDashboardsByUserId(@PathVariable Long userId) {
        List<Dashboard> dashboards = dashboardApplicationService.findPublicDashboardsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(dashboards.stream().map(this::toDashboardDTO).collect(Collectors.toList())));
    }

    @GetMapping("/public")
    @Operation(summary = "获取所有公开仪表盘")
    public ResponseEntity<ApiResponse<List<DashboardDTO>>> getAllPublicDashboards() {
        List<Dashboard> dashboards = dashboardApplicationService.findAllPublicDashboards();
        return ResponseEntity.ok(ApiResponse.success(dashboards.stream().map(this::toDashboardDTO).collect(Collectors.toList())));
    }

    @GetMapping("/user/{userId}/search")
    @Operation(summary = "按名称搜索仪表盘")
    public ResponseEntity<ApiResponse<List<DashboardDTO>>> searchDashboardsByName(
            @PathVariable Long userId,
            @RequestParam String name) {
        List<Dashboard> dashboards = dashboardApplicationService.searchDashboardsByName(userId, name);
        return ResponseEntity.ok(ApiResponse.success(dashboards.stream().map(this::toDashboardDTO).collect(Collectors.toList())));
    }

    @GetMapping("/count/user/{userId}")
    @Operation(summary = "统计用户的仪表盘数量")
    public ResponseEntity<ApiResponse<Long>> countDashboardsByUserId(@PathVariable Long userId) {
        long count = dashboardApplicationService.countDashboardsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/count/public")
    @Operation(summary = "统计公开仪表盘数量")
    public ResponseEntity<ApiResponse<Long>> countPublicDashboards() {
        long count = dashboardApplicationService.countPublicDashboards();
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * 将 Dashboard 实体转换为 DTO
     */
    private DashboardDTO toDashboardDTO(Dashboard dashboard) {
        return DashboardDTO.builder()
                .id(dashboard.getId())
                .name(dashboard.getName())
                .description(dashboard.getDescription())
                .userId(dashboard.getUserId())
                .isPublic(dashboard.isPublic())
                .config(dashboard.getConfig())
                .createdAt(dashboard.getCreatedAt())
                .updatedAt(dashboard.getUpdatedAt())
                .build();
    }
}
