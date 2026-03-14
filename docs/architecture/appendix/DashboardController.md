DashboardController
- Base path: /api/v1/dashboard

- Endpoints
 1) GET /
   - Summary: Get complete dashboard data
   - Description: Retrieve aggregated metrics including scores, completion rate, and department progress
   - Output: ApiResponse<Map<String, Object>>
   - Service: internal computations using indicatorService and orgService

 2) GET /summary
   - Summary: Get dashboard summary
   - Description: Retrieve dashboard summary including completion rate and alerts
   - Output: ApiResponse<Map<String, Object>>
   - Service: getDashboardSummary()

 3) GET /department-progress
   - Summary: Get department progress
   - Description: Retrieve per-department progress statistics
   - Output: ApiResponse<List<Map<String, Object>>>
   - Service: getDepartmentProgress()

 4) GET /stats
   - Summary: Get dashboard stats (alias for summary)
   - Description: Returns same as summary; year filter supported
   - Input: Optional year param
   - Output: ApiResponse<Map<String, Object>>
   - Service: delegates to getDashboardSummary()

 5) GET /recent-activities
   - Summary: Get recent activities
   - Description: Retrieve recent system activities (extension point)
   - Output: ApiResponse<List<Map<String, Object>>>
   - Service: placeholder for future integration

- DTOs
  - None exposed beyond VO and Map-based responses

- Outputs (VOs) and ApiResponse
  - IndicatorVO; ApiResponse wrapper; Map<String, Object> structures

- Observations
  - Heavy use of IndicatorVO to compute dashboard metrics; relies on indicatorService
  - Some endpoints are placeholders for future enhancements (recent-activities)

- Recommendations
  - ID future enhancements to align with CQRS or read-model services for dashboards
