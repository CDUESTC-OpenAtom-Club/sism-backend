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
                request.getIndicatorId(),
                request.getRuleId(),
                request.getWindowId(),
                request.getSeverity(),
                request.getActualPercent(),
                request.getExpectedPercent(),
                request.getGapPercent(),
                request.getDetailJson()
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
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + id));
        checkAlertAccessPermission(alert, authentication);
        Alert triggeredAlert = alertApplicationService.triggerAlert(id);
        return ResponseEntity.ok(ApiResponse.success(triggeredAlert));
    }

    // ==================== Read ====================

    @GetMapping
    @Operation(summary = "Get all alerts", description = "Query all alerts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Alert>>> getAllAlerts(Authentication authentication) {
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

    @GetMapping("/indicator/{indicatorId}")
    @Operation(summary = "Get alerts by indicator", description = "Query alerts by indicator ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Alert>>> getAlertsByIndicator(
            @PathVariable Long indicatorId,
            Authentication authentication
    ) {
        List<Alert> alerts = alertApplicationService.getAlertsByIndicatorId(indicatorId);
        List<Alert> filteredAlerts = filterAlertsByPermission(alerts, authentication);
        return ResponseEntity.ok(ApiResponse.success(filteredAlerts));
    }

    @GetMapping("/unresolved")
    @Operation(summary = "Get unresolved alerts", description = "Query all unresolved alerts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Alert>>> getUnresolvedAlerts(Authentication authentication) {
        List<Alert> alerts = alertApplicationService.getUnresolvedAlerts();
        List<Alert> filteredAlerts = filterAlertsByPermission(alerts, authentication);
        return ResponseEntity.ok(ApiResponse.success(filteredAlerts));
    }

    @GetMapping("/events/unclosed")
    @Operation(summary = "Get unclosed alert events", description = "Query all unclosed alert events (frontend compatibility)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Alert>>> getUnclosedAlertEvents(Authentication authentication) {
        // Alias for /unresolved to match frontend API expectations
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
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + id));
        checkAlertAccessPermission(alert, authentication);

        Long handledBy = extractUserId(authentication);
        Alert resolvedAlert = alertApplicationService.resolveAlert(
                id, handledBy, request.getResolution()
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

    @GetMapping("/stats")
    @Operation(summary = "Get alert stats", description = "Get alert statistics with severity breakdown")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAlertStats(Authentication authentication) {
        Map<String, Object> stats = alertApplicationService.getAlertStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ==================== 权限检查辅助方法 ====================

    private void checkAlertAccessPermission(Alert alert, Authentication authentication) {
        if (isAdmin(authentication)) {
            return;
        }
        // All authenticated users can access alerts for now
        // Future: check indicator ownership via indicatorId
    }

    private List<Alert> filterAlertsByPermission(List<Alert> alerts, Authentication authentication) {
        if (isAdmin(authentication)) {
            return alerts;
        }
        // All authenticated users can view alerts for now
        // Future: filter by indicator ownership
        return alerts;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CurrentUser currentUser) {
            return currentUser.getId();
        }
        String username = authentication.getName();
        try {
            return Long.parseLong(username);
        } catch (NumberFormatException e) {
            throw new AuthorizationException("无法获取当前用户ID，请联系管理员");
        }
    }
}
