#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

if ! docker version --format '{{.Server.Version}}' >/dev/null 2>&1; then
  echo "Docker is not running. Start Docker or OrbStack first." >&2
  exit 1
fi

docker compose up --build -d
echo "Wenzhong OJ is starting at http://localhost:${SERVER_PORT:-8081}/app/"
echo "Teacher/system status: http://localhost:${SERVER_PORT:-8081}/app/teacher-management"
