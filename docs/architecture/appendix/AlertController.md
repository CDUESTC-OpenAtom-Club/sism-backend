AlertController
- Base path: /api/v1/alerts

- Endpoints
 1) GET /{id}
   - Summary: Get alert by ID
   - Description: Retrieve a specific alert event
   - Input: PathVariable Long id
   - Output: ApiResponse<AlertEventVO>
   - Service: alertService.getAlertById(id)
   - Notes: 200 on success; 404 if not found

 2) GET /open
   - Summary: Get open alerts
   - Description: Retrieve open alerts sorted by severity and time
   - Input: Page params page, size
   - Output: ApiResponse<PageResult<AlertEventVO>>
   - Service: alertService.getOpenAlerts(pageable)

 3) GET /critical
   - Summary: Get critical open alerts
   - Description: Retrieve critical open alerts
   - Input: Page params page, size
   - Output: ApiResponse<PageResult<AlertEventVO>>
   - Service: alertService.getCriticalOpenAlerts(pageable)

 4) GET /severity/{severity}
   - Summary: Get alerts by severity
   - Description: Retrieve alerts with a specific severity level
   - Input: PathVariable AlertSeverity severity; page, size
   - Output: ApiResponse<PageResult<AlertEventVO>>
   - Service: alertService.getAlertsBySeverity(severity, pageable)

 5) GET /status/{status}
   - Summary: Get alerts by status
   - Description: Retrieve alerts with a specific status
   - Input: PathVariable AlertStatus status; page, size
   - Output: ApiResponse<PageResult<AlertEventVO>>
   - Service: alertService.getAlertsByStatus(status, pageable)

 6) GET /filter
   - Summary: Filter alerts by severity and status
   - Description: Retrieve alerts filtered by severity and status
   - Input: RequestParam AlertSeverity severity; RequestParam AlertStatus status; page, size
   - Output: ApiResponse<PageResult<AlertEventVO>>
   - Service: alertService.getAlertsBySeverityAndStatus(severity, status, pageable)

 7) GET /indicator/{indicatorId}
   - Summary: Get alerts by indicator
   - Description: Retrieve alerts for a specific indicator
   - Input: PathVariable Long indicatorId; page, size
   - Output: ApiResponse<PageResult<AlertEventVO>>
   - Service: alertService.getAlertsByIndicator(indicatorId, pageable)

 8) GET /org/{orgId}
   - Summary: Get alerts by organization
   - Description: Retrieve alerts for a specific organization
   - Input: PathVariable Long orgId; page, size
   - Output: ApiResponse<PageResult<AlertEventVO>>
   - Service: alertService.getAlertsByTargetOrg(orgId, pageable)

 9) GET /statistics
   - Summary: Get alert statistics
   - Description: Retrieve alert statistics summary
   - Input: none
   - Output: ApiResponse<Map<String, Object>>
   - Service: alertService.getAlertStatistics()

 10) POST /generate/{windowId}
    - Summary: Generate alerts
    - Description: Generate alert events for an alert window
    - Input: PathVariable Long windowId
    - Output: ApiResponse<List<AlertEventVO>>
    - Service: alertService.generateAlertsForWindow(windowId)

 11) POST /{id}/start
     - Summary: Start handling alert
     - Description: Mark an alert as being handled by current user
     - Input: PathVariable Long id
     - Output: ApiResponse<AlertEventVO>
     - Service: alertService.startHandlingAlert(id, handledById)

 12) POST /{id}/handle
     - Summary: Handle alert
     - Description: Record handler and notes, resolve the alert
     - Input: PathVariable Long id; RequestParam String handledNote (optional)
     - Output: ApiResponse<AlertEventVO>
     - Service: alertService.handleAlert(id, handledById, handledNote)

 13) POST /{id}/close
     - Summary: Close alert
     - Description: Close an alert event
     - Input: PathVariable Long id; RequestParam String handledNote (optional)
     - Output: ApiResponse<AlertEventVO>
     - Service: alertService.closeAlert(id, handledById, handledNote)

 14) POST /{id}/close
     - Note: Duplicate path in source was resolved by code; included above as method 13

- DTOs
  - No DTOs declared in AlertController methods beyond path/query params; uses AlertSeverity, AlertStatus enums and simple request params

- Outputs (VOs) and ApiResponse
  - AlertEventVO; ApiResponse wrapper; PageResult<AlertEventVO> for paged results; Map<String, Object> for statistics

- Observations
  - Controller uses Security context to resolve current user for some endpoints
  - Hooks into AlertService for business logic
  - Uses ApiResponse and PageResult consistently

- Recommendations
  - Validate any exposed endpoints for potential sensitive data exposure in statistics and open endpoints
