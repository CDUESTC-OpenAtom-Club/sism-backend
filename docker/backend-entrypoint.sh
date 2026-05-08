#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${DB_HOST:-postgres}"
DB_PORT="${DB_PORT:-5432}"
DB_URL="${DB_URL:?DB_URL is required}"
DB_USERNAME="${DB_USERNAME:?DB_USERNAME is required}"
DB_PASSWORD="${DB_PASSWORD:?DB_PASSWORD is required}"
JWT_SECRET="${JWT_SECRET:?JWT_SECRET is required}"
ALLOWED_ORIGINS="${ALLOWED_ORIGINS:?ALLOWED_ORIGINS is required}"

SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"

echo "Waiting for PostgreSQL at ${DB_HOST}:${DB_PORT}..."
until pg_isready -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USERNAME}" >/dev/null 2>&1; do
  sleep 2
done

exec java -jar /app/app.jar --spring.profiles.active="${SPRING_PROFILES_ACTIVE}"
