# FINAL DELIVERY SPEC

## Frontend deployment SHA

- Target frontend deployment SHA: `fe903a617cfea5cb0b8b7b9343874360660bef91`

## Frontend image version strategy

- Frontend image builds must publish both:
  - immutable SHA tag `${GITHUB_SHA}`
  - rolling tag `main`
- Deployment validation must use the SHA tag as the source of truth.
- Runtime builds must inject `VITE_APP_COMMIT_HASH=${GITHUB_SHA}` so the delivered page can self-report the deployed commit.

## Backend deployment strategy

- `sism-backend` is the single system deployment entrypoint.
- Backend workflow records GitHub Deployment metadata for the whole system.
- Frontend build can trigger a backend `workflow_dispatch` deployment for `frontend-only` rollout.
- Remote deployment runs asynchronously on the server and writes progress to:
  - `/opt/sism-stack/production/deploy-status.env`
  - `/opt/sism-stack/production/deploy-run.log`

## Production acceptance checklist

### 1. Static asset verification

- Confirm the browser loads the expected JS/CSS assets from the current frontend build.
- Confirm console output includes `VITE_APP_COMMIT_HASH`.
- Confirm the reported hash equals the target deployment SHA.

### 2. Contract alignment verification

- Call `/api/v1/indicators`.
- Confirm the frontend rendering shape matches the current backend DTO contract.
- No `404` / `500` responses are allowed for the validated path.

### 3. Backend health verification

- `GET /api/v1/actuator/health` must return:
  - HTTP `200`
  - body contains `status=UP`

### 4. Database migration verification

- `flyway_schema_history` must contain the latest expected migration version.
- No pending migrations are allowed.

### 5. Business smoke verification

- Complete one full chain:
  - indicator fill
  - submit approval
  - status update

## Deployment evidence to capture

- GitHub Actions run URL for frontend build
- GitHub Actions run URL for backend deployment
- Server-side `deploy-status.env`
- Server-side `deploy-run.log`
- `podman ps` output
- browser console screenshot showing commit hash
- HTTP output for:
  - `/api/v1/actuator/health`
  - login request
