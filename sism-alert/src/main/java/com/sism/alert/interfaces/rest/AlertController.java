package com.sism.alert.interfaces.rest;

import com.sism.alert.application.AlertApplicationService;
import com.sism.alert.domain.Alert;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    // ==================== Create ====================

    @PostMapping
    @Operation(summary = "Create alert", description = "Create a new alert")
    public ResponseEntity<Alert> createAlert(@RequestBody AlertRequest request) {
        Alert alert = alertApplicationService.createAlert(
                request.getAlertType(),
                request.getTitle(),
                request.getDescription(),
                request.getSeverity(),
                request.getEntityType(),
                request.getEntityId()
        );
        return ResponseEntity.ok(alert);
    }

    @PostMapping("/{id}/trigger")
    @Operation(summary = "Trigger alert", description = "Manually trigger an alert")
    public ResponseEntity<Alert> triggerAlert(@PathVariable Long id) {
        Alert alert = alertApplicationService.triggerAlert(id);
        return ResponseEntity.ok(alert);
    }

    // ==================== Read ====================

    @GetMapping
    @Operation(summary = "Get all alerts", description = "Query all alerts")
    public ResponseEntity<List<Alert>> getAllAlerts() {
        return ResponseEntity.ok(alertApplicationService.getAllAlerts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get alert by ID", description = "Query alert by ID")
    public ResponseEntity<Alert> getAlertById(@PathVariable Long id) {
        return alertApplicationService.getAlertById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get alerts by status", description = "Query alerts by status")
    public ResponseEntity<List<Alert>> getAlertsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(alertApplicationService.getAlertsByStatus(status));
    }

    @GetMapping("/type/{alertType}")
    @Operation(summary = "Get alerts by type", description = "Query alerts by type")
    public ResponseEntity<List<Alert>> getAlertsByType(@PathVariable String alertType) {
        return ResponseEntity.ok(alertApplicationService.getAlertsByType(alertType));
    }

    @GetMapping("/severity/{severity}")
    @Operation(summary = "Get alerts by severity", description = "Query alerts by severity")
    public ResponseEntity<List<Alert>> getAlertsBySeverity(@PathVariable String severity) {
        return ResponseEntity.ok(alertApplicationService.getAlertsBySeverity(severity));
    }

    @GetMapping("/entity")
    @Operation(summary = "Get alerts by entity", description = "Query alerts by entity type and ID")
    public ResponseEntity<List<Alert>> getAlertsByEntity(
            @RequestParam String entityType,
            @RequestParam Long entityId) {
        return ResponseEntity.ok(alertApplicationService.getAlertsByEntity(entityType, entityId));
    }

    @GetMapping("/unresolved")
    @Operation(summary = "Get unresolved alerts", description = "Query all unresolved alerts")
    public ResponseEntity<List<Alert>> getUnresolvedAlerts() {
        return ResponseEntity.ok(alertApplicationService.getUnresolvedAlerts());
    }

    // ==================== Update ====================

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Resolve alert", description = "Confirm and resolve an alert")
    public ResponseEntity<Alert> resolveAlert(
            @PathVariable Long id,
            @RequestBody ResolveAlertRequest request) {
        Alert alert = alertApplicationService.resolveAlert(
                id,
                request.getResolvedBy(),
                request.getResolution()
        );
        return ResponseEntity.ok(alert);
    }

    // ==================== Statistics ====================

    @GetMapping("/count")
    @Operation(summary = "Count alerts", description = "Get total alert count")
    public ResponseEntity<Map<String, Long>> countAlerts() {
        long total = alertApplicationService.countAlerts();
        long pending = alertApplicationService.countByStatus(Alert.STATUS_PENDING);
        long triggered = alertApplicationService.countByStatus(Alert.STATUS_TRIGGERED);
        long resolved = alertApplicationService.countByStatus(Alert.STATUS_RESOLVED);
        return ResponseEntity.ok(Map.of(
                "total", total,
                "pending", pending,
                "triggered", triggered,
                "resolved", resolved
        ));
    }

    // ==================== Request DTOs ====================

    public static class AlertRequest {
        private String alertType;
        private String title;
        private String description;
        private String severity;
        private String entityType;
        private Long entityId;

        public String getAlertType() { return alertType; }
        public void setAlertType(String alertType) { this.alertType = alertType; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getEntityType() { return entityType; }
        public void setEntityType(String entityType) { this.entityType = entityType; }
        public Long getEntityId() { return entityId; }
        public void setEntityId(Long entityId) { this.entityId = entityId; }
    }

    public static class ResolveAlertRequest {
        private Long resolvedBy;
        private String resolution;

        public Long getResolvedBy() { return resolvedBy; }
        public void setResolvedBy(Long resolvedBy) { this.resolvedBy = resolvedBy; }
        public String getResolution() { return resolution; }
        public void setResolution(String resolution) { this.resolution = resolution; }
    }
}
