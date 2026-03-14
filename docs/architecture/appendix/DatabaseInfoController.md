DatabaseInfoController
- Base path: /api/debug/database

- Endpoints
 1) GET /tables
   - Summary: List tables with counts
   - Description: Development-only endpoint to inspect database tables and row counts
   - Output: ApiResponse-like structure? (returns Map<String, Object>)
   - Service: listTables()

- Observations
  - Non-Spring-Blacklist development-only: no security annotation; relies on runtime usage
  - Uses JdbcTemplate and DataSource to introspect schema

- Recommendations
  - Consider wrapping in ApiResponse for consistency with rest of API surface
