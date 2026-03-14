AdhocTaskController
- Base path: /api/v1/adhoc-tasks

- Endpoints
 1) GET /{id}
   - Summary: Get adhoc task by ID
   - Description: Retrieve a specific adhoc task with targets and indicators
   - Input: PathVariable Long id
   - Output: ApiResponse<AdhocTaskVO>
   - Service: adhocTaskService.getAdhocTaskById(id)
   - Notes: 200 on success; 404 if not found (via global exception handling)

 2) GET /cycle/{cycleId}
   - Summary: Get adhoc tasks by cycle
   - Description: Retrieve adhoc tasks for a specific assessment cycle
   - Input: PathVariable Long cycleId
   - Output: ApiResponse<List<AdhocTaskVO>>
   - Service: adhocTaskService.getAdhocTasksByCycleId(cycleId)

 3) GET /cycle/{cycleId}/page
   - Summary: Get adhoc tasks by cycle (paginated)
   - Description: Retrieve adhoc tasks for a cycle with pagination
   - Input: PathVariable Long cycleId; RequestParam int page (default 0); RequestParam int size (default 10)
   - Output: ApiResponse<PageResult<AdhocTaskVO>>
   - Service: adhocTaskService.getAdhocTasksByCycleId(cycleId, Pageable)

 4) GET /creator/{creatorOrgId}
   - Summary: Get adhoc tasks by creator org
   - Description: Retrieve adhoc tasks created by a specific organization
   - Input: PathVariable Long creatorOrgId
   - Output: ApiResponse<List<AdhocTaskVO>>
   - Service: adhocTaskService.getAdhocTasksByCreatorOrgId(creatorOrgId)

 5) GET /status/{status}
   - Summary: Get adhoc tasks by status
   - Description: Retrieve adhoc tasks with a specific status
   - Input: PathVariable AdhocTaskStatus status
   - Output: ApiResponse<List<AdhocTaskVO>>
   - Service: adhocTaskService.getAdhocTasksByStatus(status)

 6) GET /status/{status}/page
   - Summary: Get adhoc tasks by status (paginated)
   - Description: Retrieve adhoc tasks with a specific status with pagination
   - Input: PathVariable AdhocTaskStatus status; RequestParam int page (default 0); RequestParam int size (default 10)
   - Output: ApiResponse<PageResult<AdhocTaskVO>>
   - Service: adhocTaskService.getAdhocTasksByStatus(status, Pageable)

 7) GET /search
   - Summary: Search adhoc tasks
   - Description: Search adhoc tasks by keyword
   - Input: RequestParam String keyword
   - Output: ApiResponse<List<AdhocTaskVO>>
   - Service: adhocTaskService.searchAdhocTasks(keyword)

 8) GET /overdue
   - Summary: Get overdue adhoc tasks
   - Description: Retrieve all overdue adhoc tasks
   - Input: none
   - Output: ApiResponse<List<AdhocTaskVO>>
   - Service: adhocTaskService.getOverdueAdhocTasks()

 9) GET /include-in-alert
   - Summary: Get adhoc tasks in alert
   - Description: Retrieve adhoc tasks included in alert calculation
   - Input: none
   - Output: ApiResponse<List<AdhocTaskVO>>
   - Service: adhocTaskService.getAdhocTasksIncludedInAlert()

 10) GET /{id}/targets
    - Summary: Get target organizations
    - Description: Retrieve target organizations for an adhoc task
    - Input: PathVariable Long id
    - Output: ApiResponse<List<AdhocTaskVO.AdhocTaskTargetVO>>
    - Service: adhocTaskService.getTargetOrganizations(id)

 11) GET /{id}/indicators
    - Summary: Get mapped indicators
    - Description: Retrieve mapped indicators for an adhoc task
    - Input: PathVariable Long id
    - Output: ApiResponse<List<AdhocTaskVO.AdhocTaskIndicatorVO>>
    - Service: adhocTaskService.getMappedIndicators(id)

 12) POST /
    - Summary: Create adhoc task
    - Description: Create a new adhoc task with scope type handling
    - Input: @Valid @RequestBody AdhocTaskCreateRequest request
    - Output: ApiResponse<AdhocTaskVO>
    - Service: adhocTaskService.createAdhocTask(request)

 13) PUT /{id}
    - Summary: Update adhoc task
    - Description: Update an existing adhoc task
    - Input: @PathVariable Long id; @Valid @RequestBody AdhocTaskUpdateRequest request
    - Output: ApiResponse<AdhocTaskVO>
    - Service: adhocTaskService.updateAdhocTask(id, request)

 14) POST /{id}/open
    - Summary: Open adhoc task
    - Description: Transition DRAFT -> OPEN
    - Input: PathVariable Long id
    - Output: ApiResponse<AdhocTaskVO>
    - Service: adhocTaskService.openAdhocTask(id)

 15) POST /{id}/close
    - Summary: Close adhoc task
    - Description: Transition OPEN -> CLOSED
    - Input: PathVariable Long id
    - Output: ApiResponse<AdhocTaskVO>
    - Service: adhocTaskService.closeAdhocTask(id)

 16) POST /{id}/archive
    - Summary: Archive adhoc task
    - Description: Archive an adhoc task
    - Input: PathVariable Long id
    - Output: ApiResponse<AdhocTaskVO>
    - Service: adhocTaskService.archiveAdhocTask(id)

 17) DELETE /{id}
    - Summary: Delete adhoc task
    - Description: Delete a draft adhoc task
    - Input: PathVariable Long id
    - Output: ApiResponse<Void>
    - Service: adhocTaskService.deleteAdhocTask(id)

- DTOs
  - AdhocTaskCreateRequest
  - AdhocTaskUpdateRequest
  - Validation: @Valid on create; validation constraints assumed on DTO fields

- Outputs (VOs) and ApiResponse
  - AdhocTaskVO; AdhocTaskVO.AdhocTaskTargetVO; AdhocTaskVO.AdhocTaskIndicatorVO
  - PageResult<AdhocTaskVO> used for paginated results
  - ApiResponse wrapper used across endpoints

- Observations
  - Controller appears thin; most logic resides in AdhocTaskService
  - Logging present in several methods; consistent with other controllers
  - Base path consistent with /api/v1; some comments in code reference non-versioned paths

- Recommendations
  - Consider unifying path conventions to always use /api/v1
  - Validate if any endpoints require additional parameter validation or security constraints
