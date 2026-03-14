IndicatorController
- Base path: /api/v1/indicators

- Endpoints (highlights)
  - GET / (Get all indicators) with Last-Modified caching via IndicatorQueryService
  - GET /{id} (Get indicator by ID)
  - GET /task/{taskId} (Get indicators by task)
  - GET /task/{taskId}/root (Get root indicators by task)
  - GET /owner/{ownerOrgId} (Indicators by owner org)
  - GET /target/{targetOrgId} (Indicators by target org)
  - GET /target/{orgId}/hierarchy (Indicators by target org hierarchy)
  - GET /search?keyword= (Search indicators)
  - POST / (Create indicator)
  - PUT /{id} (Update indicator)
  - DELETE /{id} (Archive/Delete indicator)
  - POST /{id}/distribute (Distribute indicator)
  - POST /{id}/distribute/batch (Batch distribute)
  - GET /{id}/distributed (Get distributed indicators)
  - POST /{id}/withdraw (Withdraw distribution)
  - GET /{id}/distribution-eligibility (Check eligibility for distribution)
  - Other filtering endpoints: /filter, /qualitative, /quantitative, /by-role-context, /for-filling, /workflow-context

- Observations
  - Mix of read and write operations; heavy CQRS separation with IndicatorQueryService for reads
  - Multiple workflow-related endpoints for distribution and review

- Recommendations
  - Consider further splitting read-model/controller logic to dedicated ReadModel controller if complexity grows
