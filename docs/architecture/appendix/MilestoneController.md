MilestoneController
- Base path: /api/v1/milestones

- Endpoints
 1) GET / (Get all milestones)
 2) GET /{id} (Get milestone by ID)
 3) GET /indicator/{indicatorId} (Filter by indicator)
 4) GET /indicator/{indicatorId}/by-date (Order by date)
 5) GET /status/{status} (Milestones by status)
 6) GET /overdue (Overdue milestones)
 7) GET /upcoming (Upcoming milestones)
 8) GET /indicator/{indicatorId}/weight-validation (Weight validation)
 9) GET /indicator/{indicatorId}/total-weight (Total weight)
 10) POST / (Create milestone)
 11) PUT /{id} (Update milestone)
 12) PATCH /{id}/status (Update milestone status)
 13) DELETE /{id} (Delete milestone)
 14) GET /indicator/{indicatorId}/next-to-report (Next milestone to report)
 15) GET /indicator/{indicatorId}/unpaired (Unpaired milestones)
 16) GET /{id}/is-paired (Check if milestone is paired)
 17) GET /indicator/{indicatorId}/pairing-status (Pairing status for indicator)
 18) GET /indicator/{indicatorId}/can-report/{milestoneId} (Can report on milestone)

- Observations
- Recommendations
