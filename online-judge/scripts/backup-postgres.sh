#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

BACKUP_DIR="${OJ_BACKUP_DIR:-backups}"
mkdir -p "${BACKUP_DIR}"

STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="${BACKUP_DIR}/onlinejudge-${STAMP}.sql"
DB_NAME="${POSTGRES_DB:-onlinejudge}"
DB_USER="${POSTGRES_USER:-onlinejudge}"

docker compose exec -T postgres pg_dump -U "${DB_USER}" "${DB_NAME}" > "${OUT}"
if [[ ! -s "${OUT}" ]]; then
  rm -f -- "${OUT}"
  echo "Postgres backup failed: output is empty" >&2
  exit 1
fi
echo "Postgres backup saved to ${OUT}"
