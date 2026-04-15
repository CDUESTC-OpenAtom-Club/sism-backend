package com.sism.analytics.interfaces.rest;

import com.sism.analytics.application.DashboardApplicationService;
import com.sism.analytics.domain.Dashboard;
import com.sism.analytics.interfaces.dto.CopyDashboardRequest;
import com.sism.analytics.interfaces.dto.CreateDashboardRequest;
import com.sism.analytics.interfaces.dto.DashboardDTO;
import com.sism.analytics.interfaces.dto.UpdateDashboardRequest;
import com.sism.iam.application.dto.CurrentUser;
import com.sism.common.ApiResponse;
import com.sism.common.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DashboardDTO>> createDashboard(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateDashboardRequest request) {
        Long currentUserId = requireCurrentUserId(currentUser);
        Dashboard dashboard = dashboardApplicationService.createDashboard(
                request.getName(),
                request.getDescription(),
                currentUserId,
                request.getIsPublic() != null && request.getIsPublic(),
                request.getConfig()
        );
        return ResponseEntity.ok(ApiResponse.success(toDashboardDTO(dashboard)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新仪表盘信息")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DashboardDTO>> updateDashboard(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateDashboardRequest request) {
        Long currentUserId = requireCurrentUserId(currentUser);
        Dashboard dashboard = dashboardApplicationService.updateDashboard(
                id,
                currentUserId,
                request.getName(),
                request.getDescription(),
                request.getIsPublic() != null && request.getIsPublic(),
                request.getConfig()
        );
        return ResponseEntity.ok(ApiResponse.success(toDashboardDTO(dashboard)));
    }

    @PutMapping("/{id}/config")
    @Operation(summary = "更新仪表盘配置")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DashboardDTO>> updateDashboardConfig(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        Dashboard dashboard = dashboardApplicationService.updateDashboardConfig(id, requireCurrentUserId(currentUser), request.get("config"));
        return ResponseEntity.ok(ApiResponse.success(toDashboardDTO(dashboard)));
    }

    @PostMapping("/{id}/make-public")
    @Operation(summary = "将仪表盘设为公开")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DashboardDTO>> makePublic(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id) {
        Dashboard dashboard = dashboardApplicationService.makePublic(id, requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(toDashboardDTO(dashboard)));
    }

    @PostMapping("/{id}/make-private")
    @Operation(summary = "将仪表盘设为私有")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DashboardDTO>> makePrivate(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id) {
        Dashboard dashboard = dashboardApplicationService.makePrivate(id, requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(toDashboardDTO(dashboard)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除仪表盘")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteDashboard(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long id) {
        dashboardApplicationService.deleteDashboard(id, requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/copy")
    @Operation(summary = "复制仪表盘给其他用户")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DashboardDTO>> copyDashboard(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody CopyDashboardRequest request) {
        Dashboard dashboard = dashboardApplicationService.copyDashboardToUser(id, requireCurrentUserId(currentUser), request.getTargetUserId());
        return ResponseEntity.ok(ApiResponse.success(toDashboardDTO(dashboard)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取仪表盘")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DashboardDTO>> getDashboardById(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long id) {
        return dashboardApplicationService.findDashboardById(id, requireCurrentUserId(currentUser))
                .map(dashboard -> ResponseEntity.ok(ApiResponse.success(toDashboardDTO(dashboard))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/user/{userId}", params = {"!pageNum", "!pageSize"})
    @Operation(summary = "根据用户ID获取仪表盘")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DashboardDTO>>> getDashboardsByUserId(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long userId) {
        ensureCurrentUserOwnsRequestedUser(requireCurrentUserId(currentUser), userId);
        List<Dashboard> dashboards = dashboardApplicationService.findDashboardsByUserId(userId, requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(dashboards.stream().map(this::toDashboardDTO).collect(Collectors.toList())));
    }

    @GetMapping(value = "/user/{userId}", params = {"pageNum", "pageSize"})
    @Operation(summary = "分页根据用户ID获取仪表盘")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResult<DashboardDTO>>> getDashboardsByUserIdPage(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long currentUserId = requireCurrentUserId(currentUser);
        ensureCurrentUserOwnsRequestedUser(currentUserId, userId);
        var page = dashboardApplicationService.findDashboardsByUserId(userId, currentUserId, pageNum, pageSize);
        return ResponseEntity.ok(ApiResponse.success(PageResult.of(page.map(this::toDashboardDTO))));
    }

    @GetMapping("/user/{userId}/page")
    @Operation(summary = "分页根据用户ID获取仪表盘(显式分页路径)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResult<DashboardDTO>>> getDashboardsByUserIdPagePath(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return getDashboardsByUserIdPage(currentUser, userId, pageNum, pageSize);
    }

    @GetMapping(value = "/user/{userId}/public", params = {"!pageNum", "!pageSize"})
    @Operation(summary = "根据用户ID获取公开仪表盘")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DashboardDTO>>> getPublicDashboardsByUserId(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long userId) {
        ensureCurrentUserOwnsRequestedUser(requireCurrentUserId(currentUser), userId);
        List<Dashboard> dashboards = dashboardApplicationService.findPublicDashboardsByUserId(userId, requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(dashboards.stream().map(this::toDashboardDTO).collect(Collectors.toList())));
    }

    @GetMapping(value = "/user/{userId}/public", params = {"pageNum", "pageSize"})
    @Operation(summary = "分页根据用户ID获取公开仪表盘")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResult<DashboardDTO>>> getPublicDashboardsByUserIdPage(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long currentUserId = requireCurrentUserId(currentUser);
        ensureCurrentUserOwnsRequestedUser(currentUserId, userId);
        var page = dashboardApplicationService.findPublicDashboardsByUserId(userId, currentUserId, pageNum, pageSize);
        return ResponseEntity.ok(ApiResponse.success(PageResult.of(page.map(this::toDashboardDTO))));
    }

    @GetMapping("/user/{userId}/public/page")
    @Operation(summary = "分页根据用户ID获取公开仪表盘(显式分页路径)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResult<DashboardDTO>>> getPublicDashboardsByUserIdPagePath(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return getPublicDashboardsByUserIdPage(currentUser, userId, pageNum, pageSize);
    }

    @GetMapping(value = "/public", params = {"!pageNum", "!pageSize"})
    @Operation(summary = "获取所有公开仪表盘")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DashboardDTO>>> getAllPublicDashboards() {
        List<Dashboard> dashboards = dashboardApplicationService.findAllPublicDashboards();
        return ResponseEntity.ok(ApiResponse.success(dashboards.stream().map(this::toDashboardDTO).collect(Collectors.toList())));
    }

    @GetMapping(value = "/public", params = {"pageNum", "pageSize"})
    @Operation(summary = "分页获取所有公开仪表盘")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResult<DashboardDTO>>> getAllPublicDashboardsPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        var page = dashboardApplicationService.findAllPublicDashboards(pageNum, pageSize);
        return ResponseEntity.ok(ApiResponse.success(PageResult.of(page.map(this::toDashboardDTO))));
    }

    @GetMapping("/public/page")
    @Operation(summary = "分页获取所有公开仪表盘(显式分页路径)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResult<DashboardDTO>>> getAllPublicDashboardsPagePath(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return getAllPublicDashboardsPage(pageNum, pageSize);
    }

    @GetMapping(value = "/user/{userId}/search", params = {"name", "!pageNum", "!pageSize"})
    @Operation(summary = "按名称搜索仪表盘")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DashboardDTO>>> searchDashboardsByName(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long userId,
            @RequestParam String name) {
        ensureCurrentUserOwnsRequestedUser(requireCurrentUserId(currentUser), userId);
        List<Dashboard> dashboards = dashboardApplicationService.searchDashboardsByName(userId, requireCurrentUserId(currentUser), name);
        return ResponseEntity.ok(ApiResponse.success(dashboards.stream().map(this::toDashboardDTO).collect(Collectors.toList())));
    }

    @GetMapping(value = "/user/{userId}/search", params = {"name", "pageNum", "pageSize"})
    @Operation(summary = "分页按名称搜索仪表盘")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResult<DashboardDTO>>> searchDashboardsByNamePage(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long userId,
            @RequestParam String name,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long currentUserId = requireCurrentUserId(currentUser);
        ensureCurrentUserOwnsRequestedUser(currentUserId, userId);
        var page = dashboardApplicationService.searchDashboardsByName(userId, currentUserId, name, pageNum, pageSize);
        return ResponseEntity.ok(ApiResponse.success(PageResult.of(page.map(this::toDashboardDTO))));
    }

    @GetMapping("/user/{userId}/search/page")
    @Operation(summary = "分页按名称搜索仪表盘(显式分页路径)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResult<DashboardDTO>>> searchDashboardsByNamePagePath(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long userId,
            @RequestParam String name,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return searchDashboardsByNamePage(currentUser, userId, name, pageNum, pageSize);
    }

    @GetMapping("/count/user/{userId}")
    @Operation(summary = "统计用户的仪表盘数量")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Long>> countDashboardsByUserId(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long userId) {
        ensureCurrentUserOwnsRequestedUser(requireCurrentUserId(currentUser), userId);
        long count = dashboardApplicationService.countDashboardsByUserId(userId, requireCurrentUserId(currentUser));
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/count/public")
    @Operation(summary = "统计公开仪表盘数量")
    @PreAuthorize("isAuthenticated()")
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

    private Long requireCurrentUserId(CurrentUser currentUser) {
        if (currentUser == null || currentUser.getId() == null || currentUser.getId() <= 0) {
            throw new AccessDeniedException("当前用户未登录或无效");
        }
        return currentUser.getId();
    }

    private void ensureCurrentUserOwnsRequestedUser(Long currentUserId, Long requestedUserId) {
        if (requestedUserId == null || requestedUserId <= 0) {
            throw new IllegalArgumentException("用户ID必须为正数");
        }
        if (!currentUserId.equals(requestedUserId)) {
            throw new AccessDeniedException("不能为其他用户创建仪表盘");
        }
    }
}
