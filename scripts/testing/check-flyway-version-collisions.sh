#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

migration_files="$(find "$ROOT_DIR" -path '*/src/main/resources/db/migration/V*.sql' -type f | sort)"

if [ -z "$migration_files" ]; then
  echo "No Flyway migration files found."
  exit 0
fi

duplicates="$(
  printf '%s\n' "$migration_files" \
    | sed -E 's#.*\/(V[0-9]+)__.*#\1#' \
    | sort \
    | uniq -d
)"

if [ -n "$duplicates" ]; then
  echo "Duplicate Flyway versions detected:"
  while IFS= read -r version; do
    [ -n "$version" ] || continue
    echo "  $version"
    printf '%s\n' "$migration_files" | grep "/${version}__" | sed 's#^#    - #'
  done <<< "$duplicates"
  exit 1
fi

echo "Flyway migration versions are unique."
