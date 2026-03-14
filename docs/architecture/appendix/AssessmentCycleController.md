AssessmentCycleController
- Base path: /api/v1/cycles

- Endpoints
 1) GET /
   - Summary: Get all assessment cycles
   - Description: Retrieve all assessment cycles ordered by year descending
   - Input: none
   - Output: ApiResponse<List<AssessmentCycleVO>>
   - Service: assessmentCycleService.getAllCycles()

  2) GET /{cycleId}
   - Summary: Get assessment cycle by ID
   - Description: Retrieve a specific assessment cycle by its ID
   - Input: PathVariable Long cycleId
   - Output: ApiResponse<AssessmentCycleVO>
   - Service: assessmentCycleService.getCycleById(cycleId)

  3) GET /year/{year}
   - Summary: Get assessment cycle by year
   - Description: Retrieve assessment cycle for a specific year
   - Input: PathVariable Integer year
   - Output: ApiResponse<AssessmentCycleVO>
   - Service: assessmentCycleService.getCycleByYear(year)

  4) GET /active
   - Summary: Get active or future cycles
   - Description: Retrieve assessment cycles that are currently active or will be active in the future
   - Input: none
   - Output: ApiResponse<List<AssessmentCycleVO>>
   - Service: assessmentCycleService.getActiveOrFutureCycles()

  5) GET /date/{date}
   - Summary: Get cycle by date
   - Description: Retrieve assessment cycle that contains a specific date
   - Input: PathVariable String date (ISO: yyyy-MM-dd)
   - Output: ApiResponse<AssessmentCycleVO>
   - Service: assessmentCycleService.getCycleByDate(LocalDate.parse(date))

- DTOs
  - No DTOs exposed here; uses VO and standard date input

- Outputs (VOs) and ApiResponse
  - AssessmentCycleVO
  - ApiResponse wrapper

- Observations
  - Thin controller; delegates to AssessmentCycleService
  - Uses LocalDate parsing for date endpoint

- Recommendations
  - Consider adding API annotations to document date parsing expectations and error cases
