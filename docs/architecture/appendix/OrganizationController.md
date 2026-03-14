OrganizationController (Deprecated)
- Base path: /api/v1/organizations

- Endpoints
 1) GET /tree
   - Summary: Get organization tree (DEPRECATED)
   - Description: Deprecated: use /api/orgs/tree or /api/orgs/hierarchy
   - Output: ApiResponse<List<Map<String, Object>>>

 2) GET /
   - Summary: Get all organizations (DEPRECATED)
   - Description: Deprecated: use /api/orgs instead
   - Output: ApiResponse<List<Map<String, Object>>>

 3) GET /{id}
   - Summary: Get organization by ID (DEPRECATED)
   - Description: Deprecated: use OrgController alternatives
   - Output: ApiResponse<Map<String, Object>>

 4) GET /descendants
   - Summary: Get all descendant IDs
   - Description: Deprecated boundary; use OrgController descendants
   - Output: ApiResponse<List<Long>>

- Observations
- Recommendations
