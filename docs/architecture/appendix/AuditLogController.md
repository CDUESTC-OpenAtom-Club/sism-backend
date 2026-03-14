AuditLogController
- Base path: /api/v1/audit-logs

- Endpoints
 1) GET /
   - Summary: Query audit logs with filters
   - Description: Filter by entity type, action, time range, and actor; supports paging
   - Input: entityType, action, startDate, endDate, actorUserId, actorOrgId, page, size
   - Output: ApiResponse<PageResult<AuditLogVO>>
   - Service: auditLogService.queryAuditLogs(...)

 2) GET /entity-type/{entityType}
   - Summary: Get audit logs by entity type
   - Description: Retrieve logs for an entity type
   - Input: PathVariable AuditEntityType entityType; page, size
   - Output: ApiResponse<PageResult<AuditLogVO>>
   - Service: auditLogService.getAuditLogsByEntityType(entityType, page, size)

 3) GET /action/{action}
   - Summary: Get audit logs by action
   - Description: Retrieve logs for a specific action
   - Input: PathVariable AuditAction action; page, size
   - Output: ApiResponse<PageResult<AuditLogVO>>
   - Service: auditLogService.getAuditLogsByAction(action, page, size)

 4) GET /time-range
   - Summary: Get audit logs by time range
   - Description: Retrieve logs within a time window
   - Input: startDate, endDate, page, size
   - Output: ApiResponse<PageResult<AuditLogVO>>
   - Service: auditLogService.getAuditLogsByTimeRange(startDate, endDate, page, size)

 5) GET /trail/{entityType}/{entityId}
   - Summary: Get audit trail
   - Description: Retrieve complete audit trail for a specific entity
   - Input: PathVariable AuditEntityType entityType; PathVariable Long entityId
   - Output: ApiResponse<List<AuditLogVO>>
   - Service: auditLogService.getAuditTrail(entityType, entityId)

 6) GET /user/{userId}/recent
   - Summary: Get recent audit logs by user
   - Description: Retrieve recent logs for a user
   - Input: PathVariable Long userId; limit
   - Output: ApiResponse<List<AuditLogVO>>
   - Service: auditLogService.getRecentAuditLogsByUser(userId, limit)

 7) GET /search
   - Summary: Search audit logs
   - Description: Search logs by keyword; supports paging
   - Input: keyword, page, size
   - Output: ApiResponse<PageResult<AuditLogVO>>
   - Service: auditLogService.searchAuditLogsByReason(keyword, page, size)

 8) GET /{logId}/differences
   - Summary: Get data differences
   - Description: Retrieve formatted differences for an audit log
   - Input: PathVariable Long logId
   - Output: ApiResponse<Map<String, Object>>
   - Service: auditLogService.getFormattedDifferences(logId)

- DTOs
  - None explicitly declared here; uses VOs and simple types

- Outputs (VOs) and ApiResponse
  - AuditLogVO; ApiResponse wrapper; PageResult<AuditLogVO>

- Observations
  - Rich query surface for audits; supports time-range filtering
  - Uses AuditLogService for business logic

- Recommendations
  - Consider adding API docs to describe the time-range query parameter behavior and expected date formats
