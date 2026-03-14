OrgController
- Base path: /api/v1/orgs

- Endpoints
 1) GET /
   - Summary: Get all active organizations
   - Description: Retrieve all active organizations with optional type filter and ETag caching
   - Input: Optional type Enum; If-None-Match header
   - Output: ApiResponse<List<SysOrgVO>>
   - Service: orgService.getOrgsByType(type)

 2) GET /hierarchy and /tree
   - Summary: Get organization hierarchy
   - Description: Retrieve org hierarchy with ETag caching (alias for backward compatibility)
   - Input: If-None-Match header
   - Output: ApiResponse<List<OrgTreeVO>>
   - Service: orgService.getOrgHierarchy()

 3) GET /{orgId}/hierarchy
   - Summary: Get organization subtree
   - Description: Retrieve organization hierarchy starting from a specific organization
   - Input: PathVariable Long orgId
   - Output: ApiResponse<OrgTreeVO>
   - Service: orgService.getOrgHierarchyFrom(orgId)

 4) GET /{orgId}
   - Summary: Get organization by ID
   - Description: Retrieve a specific organization
   - Input: PathVariable Long orgId
   - Output: ApiResponse<SysOrgVO>
   - Service: orgService.getOrgsByType(null) filter by id

 5) GET /{orgId}/descendants
   - Summary: Get all descendant IDs
   - Description: Retrieve all descendant organization IDs
   - Input: PathVariable Long orgId
   - Output: ApiResponse<List<Long>>
   - Service: orgService.getDescendantOrgIds(orgId)

- Observations
  - Boundary: main org surface; a separate deprecated OrganizationController exists for legacy surface

- Recommendations
  - Consider consolidating boundary to avoid duplicate endpoints between OrgController and OrganizationController (deprecated)
