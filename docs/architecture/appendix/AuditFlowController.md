AuditFlowController
- Base path: /api/v1/audit-flows

- Endpoints
 1) GET /
   - Summary: Get all audit flows
   - Description: Retrieve all audit flow definitions
   - Output: ApiResponse<List<AuditFlowVO>>
   - Service: auditFlowService.getAllAuditFlows()

 2) GET /{id}
   - Summary: Get audit flow by ID
   - Description: Retrieve a specific audit flow with its steps
   - Input: PathVariable Long id
   - Output: ApiResponse<AuditFlowVO>
   - Service: auditFlowService.getAuditFlowById(id)

 3) GET /code/{flowCode}
   - Summary: Get audit flow by code
   - Description: Retrieve an audit flow by its code
   - Input: PathVariable String flowCode
   - Output: ApiResponse<AuditFlowVO>
   - Service: auditFlowService.getAuditFlowByCode(flowCode)

 4) GET /entity-type/{entityType}
   - Summary: Get audit flows by entity type
   - Description: Retrieve audit flows for a specific entity type (deprecated)
   - Input: PathVariable AuditEntityType entityType
   - Output: ApiResponse<List<AuditFlowVO>> (empty list in current implementation)

 5) POST /
   - Summary: Create audit flow
   - Description: Create a new audit flow definition
   - Input: @Valid @RequestBody AuditFlowCreateRequest request
   - Output: ApiResponse<AuditFlowVO>
   - Service: auditFlowService.createAuditFlow(request)

 6) PUT /{id}
   - Summary: Update audit flow
   - Description: Update an existing audit flow definition
   - Input: PathVariable Long id; @Valid @RequestBody AuditFlowUpdateRequest request
   - Output: ApiResponse<AuditFlowVO>
   - Service: auditFlowService.updateAuditFlow(id, request)

 7) DELETE /{id}
   - Summary: Delete audit flow
   - Description: Delete an audit flow and all its steps
   - Input: PathVariable Long id
   - Output: ApiResponse<Void>
   - Service: auditFlowService.deleteAuditFlow(id)

 8) POST /steps
   - Summary: Add audit step
   - Description: Add a new step to an audit flow
   - Input: @Valid @RequestBody AuditStepCreateRequest request
   - Output: ApiResponse<AuditStepVO>
   - Service: auditFlowService.addAuditStep(request)

 9) GET /{flowId}/steps
   - Summary: Get audit steps
   - Description: Retrieve all steps for an audit flow
   - Input: PathVariable Long flowId
   - Output: ApiResponse<List<AuditStepVO>>
   - Service: auditFlowService.getAuditStepsByFlowId(flowId)

- DTOs
  - AuditFlowCreateRequest; AuditFlowUpdateRequest; AuditStepCreateRequest

- Outputs (VOs) and ApiResponse
  - AuditFlowVO; AuditStepVO; ApiResponse wrapper

- Observations
  - Mix of CRUD and child entity management; steps are treated as part of the flow
  - Some endpoints may be considered for boundary extraction in the future

- Recommendations
  - Consider clarifying deprecated entity-type endpoint behavior and remove if not needed
