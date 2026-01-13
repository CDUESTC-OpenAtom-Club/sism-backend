package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.common.PageResult;
import com.sism.enums.AuditAction;
import com.sism.enums.AuditEntityType;
import com.sism.service.AuditLogService;
import com.sism.vo.AuditLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Audit Log Controller
 * Provides query and filtering operations for audit logs
 * 
 * Requirements: 7.5
 */
@Slf4j
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Audit log query and filtering endpoints")
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Query audit logs with filtering
     * GET /api/audit-logs
     * Requirements: 7.5 - Support filtering by entity type, operation type, and time range
     */
    @GetMapping
    @Operation(summary = "Query audit logs", 
               description = "Query audit logs with optional filters for entity type, action, time range, and actor")
    public ResponseEntity<ApiResponse<PageResult<AuditLogVO>>> queryAuditLogs(
            @Parameter(description = "Entity type filter") 
            @RequestParam(required = false) AuditEntityType entityType,
            @Parameter(description = "Action type filter") 
            @RequestParam(required = false) AuditAction action,
            @Parameter(description = "Start date filter (ISO format)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date filter (ISO format)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Actor user ID filter") 
            @RequestParam(required = false) Long actorUserId,
            @Parameter(description = "Actor organization ID filter") 
            @RequestParam(required = false) Long actorOrgId,
            @Parameter(description = "Page number") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "20") int size) {
        
        PageResult<AuditLogVO> result = auditLogService.queryAuditLogs(
            entityType, action, startDate, endDate, actorUserId, actorOrgId, page, size
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get audit logs by entity type
     * GET /api/audit-logs/entity-type/{entityType}
     */
    @GetMapping("/entity-type/{entityType}")
    @Operation(summary = "Get audit logs by entity type", 
               description = "Retrieve audit logs for a specific entity type")
    public ResponseEntity<ApiResponse<PageResult<AuditLogVO>>> getAuditLogsByEntityType(
            @Parameter(description = "Entity type") @PathVariable AuditEntityType entityType,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        
        PageResult<AuditLogVO> result = auditLogService.getAuditLogsByEntityType(entityType, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get audit logs by action type
     * GET /api/audit-logs/action/{action}
     */
    @GetMapping("/action/{action}")
    @Operation(summary = "Get audit logs by action", 
               description = "Retrieve audit logs for a specific action type")
    public ResponseEntity<ApiResponse<PageResult<AuditLogVO>>> getAuditLogsByAction(
            @Parameter(description = "Action type") @PathVariable AuditAction action,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        
        PageResult<AuditLogVO> result = auditLogService.getAuditLogsByAction(action, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get audit logs by time range
     * GET /api/audit-logs/time-range
     */
    @GetMapping("/time-range")
    @Operation(summary = "Get audit logs by time range", 
               description = "Retrieve audit logs within a specific time range")
    public ResponseEntity<ApiResponse<PageResult<AuditLogVO>>> getAuditLogsByTimeRange(
            @Parameter(description = "Start date (ISO format)", required = true) 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)", required = true) 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        
        PageResult<AuditLogVO> result = auditLogService.getAuditLogsByTimeRange(startDate, endDate, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get audit trail for a specific entity
     * GET /api/audit-logs/trail/{entityType}/{entityId}
     */
    @GetMapping("/trail/{entityType}/{entityId}")
    @Operation(summary = "Get audit trail", 
               description = "Retrieve complete audit trail for a specific entity")
    public ResponseEntity<ApiResponse<List<AuditLogVO>>> getAuditTrail(
            @Parameter(description = "Entity type") @PathVariable AuditEntityType entityType,
            @Parameter(description = "Entity ID") @PathVariable Long entityId) {
        
        List<AuditLogVO> auditTrail = auditLogService.getAuditTrail(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success(auditTrail));
    }

    /**
     * Get recent audit logs by user
     * GET /api/audit-logs/user/{userId}/recent
     */
    @GetMapping("/user/{userId}/recent")
    @Operation(summary = "Get recent audit logs by user", 
               description = "Retrieve recent audit logs for a specific user")
    public ResponseEntity<ApiResponse<List<AuditLogVO>>> getRecentAuditLogsByUser(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Maximum number of results") @RequestParam(defaultValue = "10") int limit) {
        
        List<AuditLogVO> auditLogs = auditLogService.getRecentAuditLogsByUser(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(auditLogs));
    }

    /**
     * Search audit logs by reason keyword
     * GET /api/audit-logs/search
     */
    @GetMapping("/search")
    @Operation(summary = "Search audit logs", 
               description = "Search audit logs by reason/comment keyword")
    public ResponseEntity<ApiResponse<PageResult<AuditLogVO>>> searchAuditLogs(
            @Parameter(description = "Search keyword") @RequestParam String keyword,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        
        PageResult<AuditLogVO> result = auditLogService.searchAuditLogsByReason(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get formatted data differences for an audit log
     * GET /api/audit-logs/{logId}/differences
     */
    @GetMapping("/{logId}/differences")
    @Operation(summary = "Get data differences", 
               description = "Retrieve formatted data differences for an audit log entry")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFormattedDifferences(
            @Parameter(description = "Audit log ID") @PathVariable Long logId) {
        
        Map<String, Object> differences = auditLogService.getFormattedDifferences(logId);
        if (differences == null) {
            return ResponseEntity.ok(ApiResponse.success("No differences found", null));
        }
        return ResponseEntity.ok(ApiResponse.success(differences));
    }
}
