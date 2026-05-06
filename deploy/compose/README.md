# Container Stack Deployment

This directory is the only supported deployment manifest for the SISM container
stack.

## Files

- `docker-compose.yml`: frontend + backend + postgres stack
- `.env.example`: required runtime variables
- `nginx/frontend.conf`: SPA + `/api` reverse proxy config

## Usage

1. Copy `.env.example` to `.env`
2. Fill in database credentials, JWT secret, allowed origins, and image tags
3. Run:

```bash
docker compose pull
docker compose up -d
docker compose ps
```

## Notes

- Empty databases must start from active Flyway `V1` baseline plus `V2+`
  migrations.
- Archived legacy migrations under `db/migration-archive/` must never be added
  back to runtime Flyway locations.
- The GitHub Actions deployment workflow can bootstrap `/opt/sism-stack/<env>/.env`
  on first deploy. It auto-generates `JWT_SECRET` and `POSTGRES_PASSWORD`, keeps
  them stable on later deploys, and only refreshes image tags plus missing
  defaults.
- If GHCR images remain private, configure server-side `docker login` in
  advance or provide optional GHCR credentials to the workflow. Public images
  need no extra registry secret.
