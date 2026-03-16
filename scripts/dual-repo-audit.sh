#!/usr/bin/env bash
set -euo pipefail

echo "=== Dual Repository Audit (SysUserRepository vs UserRepository) ==="
ROOT_DIR=$(pwd)

echo "Scanning for repository usage references..."
grep -RIn --exclude-dir=.git "SysUserRepository|UserRepository" "$ROOT_DIR/sism-backend" || true

echo "Scanning for domain ownership annotations in codebase..."
grep -RIn --include='*.java' "class\s+SysUser|class\s+User" "$ROOT_DIR/sism-backend" || true

echo "If mixed usage detected, plan consolidation per ADR-014 recommendations."

exit 0
