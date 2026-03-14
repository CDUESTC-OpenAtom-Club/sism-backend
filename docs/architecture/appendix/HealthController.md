HealthController
- Base path: /api/v1/health

- Endpoints
 1) GET /
   - Summary: Health check
   - Description: Check if the backend system is running normally
   - Output: ApiResponse<Map<String, Object>> with status, timestamp, service, version
   - Behavior: Returns 200 OK with health map

- Observations
  - Lightweight endpoint used by monitoring tools

- Recommendations
  - Consider exposing liveness/readiness separately if needed for Kubernetes probes
