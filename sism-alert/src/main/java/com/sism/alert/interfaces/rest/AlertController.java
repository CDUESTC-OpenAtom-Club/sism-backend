package com.sism.alert.interfaces.rest;

import com.sism.alert.application.AlertApplicationService;
import com.sism.alert.domain.Alert;
import com.sism.alert.domain.repository.AlertRepository;
import com.sism.alert.interfaces.dto.AlertRequest;
import com.sism.alert.interfaces.dto.ResolveAlertRequest;
import com.sism.common.ApiResponse;
import com.sism.iam.application.dto.CurrentUser;
import com.sism.shared.domain.exception.AuthorizationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AlertController - 预警管理API控制器
 * 提供预警相关的REST API端点
 */
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Alert and warning management endpoints")
public class AlertController {

    private final AlertApplicationService alertApplicationService;
    private final AlertRepository alertRepository;

    // ==================== Create ====================

    @PostMapping
    @Operation(summary = "Create alert", description = "Create a new alert")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Alert>> createAlert(
            @Valid @RequestBody AlertRequest request,
            Authentication authentication
    ) {
        Alert alert = alertApplicationService.createAlert(
                request.getAlertType(),
                request.getTitle(),
                request.getDescription(),
                request.getSeverity(),
                request.getEntityType(),
                request.getEntityId()
        );
        return ResponseEntity.ok(ApiResponse.success(alert));
    }

    @PostMapping("/{id}/trigger")
    @Operation(summary = "Trigger alert", description = "Manually trigger an alert")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Alert>> triggerAlert(
            @PathVariable Long id,
            Authentication authentication
    ) {
        // 验证alert是否存在
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + id));

        // 检查权限：管理员或预警关联实体的相关用户可以触发
        checkAlertAccessPermission(alert, authentication);

        Alert triggeredAlert = alertApplicationService.triggerAlert(id);
        return ResponseEntity.ok(ApiResponse.success(triggeredAlert));
    }

    // ==================== Read ====================

    @GetMapping
    @Operation(summary = "Get all alerts", description = "Query all alerts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Alert>>> getAllAlerts(Authentication authentication) {
        // 非管理员只能查询与自己相关的预警
        List<Alert> alerts = alertApplicationService.getAllAlerts();
        List<Alert> filteredAlerts = filterAlertsByPermission(alerts, authentication);
        return ResponseEntity.ok(ApiResponse.success(filteredAlerts));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get alert by ID", description = "Query alert by ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Alert>> getAlertById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return alertApplicationService.getAlertById(id)
                .map(alert -> {
                    // 检查权限：只能查看自己有权限的预警
                    checkAlertAccessPermission(alert, authentication);
                    return ResponseEntity.ok(ApiResponse.success(alert));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get alerts by status", description = "Query alerts by status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Alert>>> getAlertsByStatus(
            @PathVariable String status,
            Authentication authentication
    ) {
        List<Alert> alerts = alertApplicationService.getAlertsByStatus(status);
        List<Alert> filteredAlerts = filterAlertsByPermission(alerts, authentication);
        return ResponseEntity.ok(ApiResponse.success(filteredAlerts));
    }

    @GetMapping("/type/{alertType}")
    @Operation(summary = "Get alerts by type", description = "Query alerts by type")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Alert>>> getAlertsByType(
            @PathVariable String alertType,
            Authentication authentication
    ) {
        List<Alert> alerts = alertApplicationService.getAlertsByType(alertType);
        List<Alert> filteredAlerts = filterAlertsByPermission(alerts, authentication);
        return ResponseEntity.ok(ApiResponse.success(filteredAlerts));
    }

    @GetMapping("/severity/{severity}")
    @Operation(summary = "Get alerts by severity", description = "Query alerts by severity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Alert>>> getAlertsBySeverity(
            @PathVariable String severity,
            Authentication authentication
    ) {
        List<Alert> alerts = alertApplicationService.getAlertsBySeverity(severity);
        List<Alert> filteredAlerts = filterAlertsByPermission(alerts, authentication);
        return ResponseEntity.ok(ApiResponse.success(filteredAlerts));
    }

    @GetMapping("/entity")
    @Operation(summary = "Get alerts by entity", description = "Query alerts by entity type and ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Alert>>> getAlertsByEntity(
            @RequestParam String entityType,
            @RequestParam Long entityId,
            Authentication authentication
    ) {
        List<Alert> alerts = alertApplicationService.getAlertsByEntity(entityType, entityId);
        // 检查权限：只能查询与自己相关的实体的预警
        if (!hasEntityAccessPermission(entityType, entityId, authentication)) {
            throw new AuthorizationException("没有权限访问该实体的预警");
        }
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/unresolved")
    @Operation(summary = "Get unresolved alerts", description = "Query all unresolved alerts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Alert>>> getUnresolvedAlerts(Authentication authentication) {
        List<Alert> alerts = alertApplicationService.getUnresolvedAlerts();
        List<Alert> filteredAlerts = filterAlertsByPermission(alerts, authentication);
        return ResponseEntity.ok(ApiResponse.success(filteredAlerts));
    }

    // ==================== Update ====================

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Resolve alert", description = "Confirm and resolve an alert")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Alert>> resolveAlert(
            @PathVariable Long id,
            @Valid @RequestBody ResolveAlertRequest request,
            Authentication authentication
    ) {
        // 验证alert是否存在
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + id));

        // // 检查权限：只有预警关联实体的相关用户或管理员可以解决
        checkAlertAccessPermission(alert, authentication);

        // 使用当前用户ID作为解决人
        Long resolvedBy = extractUserId(authentication);

        Alert resolvedAlert = alertApplicationService.resolveAlert(
                id,
                resolvedBy,
                request.getResolution()
        );
        return ResponseEntity.ok(ApiResponse.success(resolvedAlert));
    }

    // ==================== Statistics ====================

    @GetMapping("/count")
    @Operation(summary = "Count alerts", description = "Get total alert count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countAlerts(Authentication authentication) {
        long total = alertApplicationService.countAlerts();
        long pending = alertApplicationService.countByStatus(Alert.STATUS_PENDING);
        long triggered = alertApplicationService.countByStatus(Alert.STATUS_TRIGGERED);
        long resolved = alertApplicationService.countByStatus(Alert.STATUS_RESOLVED);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "total", total,
                "pending", pending,
                "triggered", triggered,
                "resolved", resolved
        )));
    }

    // ==================== 权限检查辅助方法 ====================

    /**
     * 检查当前用户是否有权限访问指定预警
     */
    private void checkAlertAccessPermission(Alert alert, Authentication authentication) {
        // 管理员拥有所有权限
        if (isAdmin(authentication)) {
            return;
        }

        // 检查预警关联实体的访问权限
        if (alert.getEntityType() != null && alert.getEntityId() != null) {
            if (!hasEntityAccessPermission(alert.getEntityType(), alert.getEntityId(), authentication)) {
                throw new AuthorizationException("没有权限访问该预警");
            }
        }
    }

    /**
     * 过滤用户有权限访问的预警列表
     */
    private List<Alert> filterAlertsByPermission(List<Alert> alerts, Authentication authentication) {
        // 管理员可以查看所有预警
        if (isAdmin(authentication)) {
            return alerts;
        }

        // 非管理员只能查看与自己相关的预警
        return alerts.stream()
                .filter(alert -> alert.getEntityType() == null || alert.getEntityId() == null ||
                        hasEntityAccessPermission(alert.getEntityType(), alert.getEntityId(), authentication))
                .toList();
    }

    /**
     * 检查用户是否有权限访问指定实体
     * TODO: 需要根据实际的业务逻辑实现
     */
    private boolean hasEntityAccessPermission(String entityType, Long entityId, Authentication authentication) {
        // 管理员可以访问所有实体
        if (isAdmin(authentication)) {
            return true;
        }

        // TODO: 根据entityType和entityId查询相关实体，
        // 检查当前用户是否有权限访问该实体
        // 例如：如果entityType是"TASK"，需要查询Task并检查负责人或参与人

        // 暂时返回true，待实现具体权限逻辑
        return true;
    }

    /**
     * 检查当前用户是否是管理员
     */
    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * 从Authentication中提取用户ID
     */
    private Long extractUserId(Authentication authentication) {
        // 尝试从CurrentUser获取用户ID
        if (authentication.getPrincipal() instanceof CurrentUser currentUser) {
            return currentUser.getId();
        }

        // 如果不是CurrentUser，尝试从name属性中解析
        String username = authentication.getName();
        try {
            return Long.parseLong(username);
        } catch (NumberFormatException e) {
            throw new AuthorizationException("无法获取当前用户ID，请联系管理员");
        }
    }
}
