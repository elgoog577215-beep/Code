#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: bash scripts/restore-postgres.sh backups/onlinejudge-YYYYMMDD-HHMMSS.sql" >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

INPUT="$1"
if [[ ! -f "${INPUT}" ]]; then
  echo "Backup file not found: ${INPUT}" >&2
  exit 1
fi

DB_NAME="${POSTGRES_DB:-onlinejudge}"
DB_USER="${POSTGRES_USER:-onlinejudge}"

cat "${INPUT}" | docker compose exec -T postgres psql -U "${DB_USER}" "${DB_NAME}"
echo "Postgres restore completed from ${INPUT}"
