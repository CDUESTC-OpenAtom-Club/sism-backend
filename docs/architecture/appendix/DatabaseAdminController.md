DatabaseAdminController
- Base path: /api/v1/admin/database

- Endpoints
 1) GET /tables
   - Summary: List all database tables
   - Description: Development-only: list tables with column counts
   - Output: ApiResponse<Map<String, Object>>
   - Service: checker.getAllTables()

 2) GET /counts
   - Summary: Get record counts
   - Description: Development-only: get record counts for all tables
   - Output: ApiResponse<Map<String, Long>>
   - Service: checker.getAllTableCounts()

 3) GET /quality-check
   - Summary: Check data quality
   - Description: Development-only: check for orphaned records and data issues
   - Output: ApiResponse<Map<String, Object>>
   - Service: checker.checkDataQuality()

 4) GET /sample
   - Summary: Get sample data
   - Description: Development-only: get sample data from key tables
   - Output: ApiResponse<Map<String, Object>>
   - Service: checker.sampleData()

 5) GET /report
   - Summary: Full database report
   - Description: Development-only: complete database status report
   - Output: ApiResponse<Map<String, Object>>
   - Service: adminService.getDatabaseReport()

- Observations
  - All endpoints gated behind dev profile
  - Provides helpful tooling for debugging and data inspection

- Recommendations
  - Consider auditing exposure in development environment and ensure safe toggling
