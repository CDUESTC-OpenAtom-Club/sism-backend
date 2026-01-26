package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.common.PageResult;
import com.sism.enums.AlertSeverity;
import com.sism.enums.AlertStatus;
import com.sism.service.AlertService;
import com.sism.vo.AlertEventVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Alert Controller
 * Provides query and handling operations for alert events
 * 
 * Requirements: 6.4, 6.5
 */
@Slf4j
@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Alert event management endpoints")
public class AlertController {

    private final AlertService alertService;

    /**
     * Get alert by ID
     * GET /api/alerts/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get alert by ID", description = "Retrieve a specific alert event")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Alert found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Alert not found")
    })
    public ResponseEntity<ApiResponse<AlertEventVO>> getAlertById(
            @Parameter(description = "Alert event ID") @PathVariable Long id) {
        AlertEventVO alert = alertService.getAlertById(id);
        return ResponseEntity.ok(ApiResponse.success(alert));
    }

    /**
     * Get open alerts sorted by severity and time
     * GET /api/alerts/open
     * Requirements: 6.4
     */
    @GetMapping("/open")
    @Operation(summary = "Get open alerts", description = "Retrieve open alerts sorted by severity and time")
    public ResponseEntity<ApiResponse<PageResult<AlertEventVO>>> getOpenAlerts(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AlertEventVO> alertPage = alertService.getOpenAlerts(pageable);
        PageResult<AlertEventVO> result = new PageResult<>(
            alertPage.getContent(), 
            alertPage.getTotalElements(), 
            page, 
            size
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get critical open alerts
     * GET /api/alerts/critical
     */
    @GetMapping("/critical")
    @Operation(summary = "Get critical alerts", description = "Retrieve critical open alerts")
    public ResponseEntity<ApiResponse<PageResult<AlertEventVO>>> getCriticalOpenAlerts(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AlertEventVO> alertPage = alertService.getCriticalOpenAlerts(pageable);
        PageResult<AlertEventVO> result = new PageResult<>(
            alertPage.getContent(), 
            alertPage.getTotalElements(), 
            page, 
            size
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get alerts by severity
     * GET /api/alerts/severity/{severity}
     * Requirements: 6.4
     */
    @GetMapping("/severity/{severity}")
    @Operation(summary = "Get alerts by severity", description = "Retrieve alerts with a specific severity level")
    public ResponseEntity<ApiResponse<PageResult<AlertEventVO>>> getAlertsBySeverity(
            @Parameter(description = "Alert severity") @PathVariable AlertSeverity severity,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AlertEventVO> alertPage = alertService.getAlertsBySeverity(severity, pageable);
        PageResult<AlertEventVO> result = new PageResult<>(
            alertPage.getContent(), 
            alertPage.getTotalElements(), 
            page, 
            size
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get alerts by status
     * GET /api/alerts/status/{status}
     * Requirements: 6.4
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get alerts by status", description = "Retrieve alerts with a specific status")
    public ResponseEntity<ApiResponse<PageResult<AlertEventVO>>> getAlertsByStatus(
            @Parameter(description = "Alert status") @PathVariable AlertStatus status,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AlertEventVO> alertPage = alertService.getAlertsByStatus(status, pageable);
        PageResult<AlertEventVO> result = new PageResult<>(
            alertPage.getContent(), 
            alertPage.getTotalElements(), 
            page, 
            size
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get alerts by severity and status
     * GET /api/alerts/filter
     */
    @GetMapping("/filter")
    @Operation(summary = "Filter alerts", description = "Retrieve alerts filtered by severity and status")
    public ResponseEntity<ApiResponse<PageResult<AlertEventVO>>> getAlertsBySeverityAndStatus(
            @Parameter(description = "Alert severity") @RequestParam AlertSeverity severity,
            @Parameter(description = "Alert status") @RequestParam AlertStatus status,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AlertEventVO> alertPage = alertService.getAlertsBySeverityAndStatus(severity, status, pageable);
        PageResult<AlertEventVO> result = new PageResult<>(
            alertPage.getContent(), 
            alertPage.getTotalElements(), 
            page, 
            size
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get alerts by indicator ID
     * GET /api/alerts/indicator/{indicatorId}
     */
    @GetMapping("/indicator/{indicatorId}")
    @Operation(summary = "Get alerts by indicator", description = "Retrieve alerts for a specific indicator")
    public ResponseEntity<ApiResponse<PageResult<AlertEventVO>>> getAlertsByIndicator(
            @Parameter(description = "Indicator ID") @PathVariable Long indicatorId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AlertEventVO> alertPage = alertService.getAlertsByIndicator(indicatorId, pageable);
        PageResult<AlertEventVO> result = new PageResult<>(
            alertPage.getContent(), 
            alertPage.getTotalElements(), 
            page, 
            size
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get alerts by target organization
     * GET /api/alerts/org/{orgId}
     */
    @GetMapping("/org/{orgId}")
    @Operation(summary = "Get alerts by organization", description = "Retrieve alerts for a specific organization")
    public ResponseEntity<ApiResponse<PageResult<AlertEventVO>>> getAlertsByTargetOrg(
            @Parameter(description = "Organization ID") @PathVariable Long orgId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AlertEventVO> alertPage = alertService.getAlertsByTargetOrg(orgId, pageable);
        PageResult<AlertEventVO> result = new PageResult<>(
            alertPage.getContent(), 
            alertPage.getTotalElements(), 
            page, 
            size
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get alert statistics
     * GET /api/alerts/statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get alert statistics", description = "Retrieve alert statistics summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAlertStatistics() {
        Map<String, Object> stats = alertService.getAlertStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Generate alerts for an alert window
     * POST /api/alerts/generate/{windowId}
     */
    @PostMapping("/generate/{windowId}")
    @Operation(summary = "Generate alerts", description = "Generate alert events for an alert window")
    public ResponseEntity<ApiResponse<List<AlertEventVO>>> generateAlerts(
            @Parameter(description = "Alert window ID") @PathVariable Long windowId) {
        log.info("Generating alerts for window: {}", windowId);
        List<AlertEventVO> alerts = alertService.generateAlertsForWindow(windowId);
        return ResponseEntity.ok(ApiResponse.success("Alerts generated successfully", alerts));
    }

    /**
     * Start handling an alert
     * POST /api/alerts/{id}/start
     */
    @PostMapping("/{id}/start")
    @Operation(summary = "Start handling alert", description = "Mark an alert as being handled")
    public ResponseEntity<ApiResponse<AlertEventVO>> startHandlingAlert(
            @Parameter(description = "Alert event ID") @PathVariable Long id,
            @Parameter(description = "Handler user ID") @RequestParam Long handledById) {
        log.info("Starting to handle alert: {} by user: {}", id, handledById);
        AlertEventVO alert = alertService.startHandlingAlert(id, handledById);
        return ResponseEntity.ok(ApiResponse.success("Alert handling started", alert));
    }

    /**
     * Handle an alert (resolve)
     * POST /api/alerts/{id}/handle
     * Requirements: 6.5
     */
    @PostMapping("/{id}/handle")
    @Operation(summary = "Handle alert", description = "Record handler and notes, resolve the alert")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Alert handled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Alert already closed")
    })
    public ResponseEntity<ApiResponse<AlertEventVO>> handleAlert(
            @Parameter(description = "Alert event ID") @PathVariable Long id,
            @Parameter(description = "Handler user ID") @RequestParam Long handledById,
            @Parameter(description = "Handling notes") @RequestParam(required = false) String handledNote) {
        log.info("Handling alert: {} by user: {}", id, handledById);
        AlertEventVO alert = alertService.handleAlert(id, handledById, handledNote);
        return ResponseEntity.ok(ApiResponse.success("Alert handled successfully", alert));
    }

    /**
     * Close an alert
     * POST /api/alerts/{id}/close
     * Requirements: 6.5
     */
    @PostMapping("/{id}/close")
    @Operation(summary = "Close alert", description = "Close an alert event")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Alert closed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Alert already closed")
    })
    public ResponseEntity<ApiResponse<AlertEventVO>> closeAlert(
            @Parameter(description = "Alert event ID") @PathVariable Long id,
            @Parameter(description = "Handler user ID") @RequestParam Long handledById,
            @Parameter(description = "Closing notes") @RequestParam(required = false) String handledNote) {
        log.info("Closing alert: {} by user: {}", id, handledById);
        AlertEventVO alert = alertService.closeAlert(id, handledById, handledNote);
        return ResponseEntity.ok(ApiResponse.success("Alert closed successfully", alert));
    }
}
