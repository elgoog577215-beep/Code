#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 || "$1" != "--confirm-build" ]]; then
  echo "生产构建必须由人工明确确认。" >&2
  echo "用法：deploy-online-judge --confirm-build" >&2
  exit 2
fi

export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

REPO_ROOT="${OJ_DEPLOY_REPO_ROOT:-/opt/Code}"
APP_ROOT="${REPO_ROOT}/online-judge"
LOCK_FILE="${OJ_DEPLOY_LOCK_FILE:-/var/lock/online-judge-deploy.lock}"
PUBLIC_HOST="${OJ_PUBLIC_HOST:-tuotuzju.com}"
PUBLIC_PATH="${OJ_PUBLIC_PATH:-/code/}"
CADDY_CONFIG="${OJ_CADDY_CONFIG:-/etc/caddy/Caddyfile}"

if [[ "${PUBLIC_PATH}" != /* ]]; then
  echo "生产公开路径必须以 / 开头：${PUBLIC_PATH}" >&2
  exit 2
fi
if [[ "${PUBLIC_PATH}" != */ ]]; then
  PUBLIC_PATH="${PUBLIC_PATH}/"
fi

if [[ ! -d "${REPO_ROOT}/.git" || ! -d "${APP_ROOT}" ]]; then
  echo "部署目录不存在：${APP_ROOT}" >&2
  exit 1
fi

exec 9>"${LOCK_FILE}"
flock -n 9 || {
  echo "已有部署任务正在运行。" >&2
  exit 1
}

for attempt in 1 2 3; do
  if git -C "${REPO_ROOT}" fetch origin main \
    && git -C "${REPO_ROOT}" checkout main \
    && git -C "${REPO_ROOT}" pull --ff-only origin main; then
    break
  fi
  if [[ "${attempt}" == "3" ]]; then
    echo "无法将服务器仓库快进到 origin/main。" >&2
    exit 1
  fi
  sleep 5
done

cd "${APP_ROOT}"
bash scripts/build-school-images.sh --confirm-build
bash scripts/start-school.sh

docker compose ps

for attempt in $(seq 1 30); do
  if curl --fail --silent --show-error --max-time 5 \
    "http://127.0.0.1:${SERVER_PORT:-8081}/code/" >/dev/null \
    && curl --fail --silent --show-error --max-time 5 \
      "http://127.0.0.1:${SERVER_PORT:-8081}/code/api/system/readiness" >/dev/null; then
    break
  fi
  if [[ "${attempt}" == "30" ]]; then
    echo "应用在等待窗口内未通过 /code/ 页面或 API 前缀探针。" >&2
    exit 1
  fi
  sleep 2
done

bash scripts/check-database-schema-readiness.sh
bash scripts/check-discipline-data-quality.sh
bash scripts/check-test-case-semantic-quality.sh

caddy validate --config "${CADDY_CONFIG}"

PROBE_DIR="$(mktemp -d)"
trap 'rm -rf -- "${PROBE_DIR}"' EXIT

for attempt in $(seq 1 30); do
  if curl --fail --silent --show-error --max-time 5 \
    --resolve "${PUBLIC_HOST}:443:127.0.0.1" \
    "https://${PUBLIC_HOST}${PUBLIC_PATH}" >"${PROBE_DIR}/code.html" \
    && curl --fail --silent --show-error --max-time 5 \
      --resolve "${PUBLIC_HOST}:443:127.0.0.1" \
      "https://${PUBLIC_HOST}${PUBLIC_PATH}api/system/readiness" >"${PROBE_DIR}/readiness.json" \
    && curl --fail --silent --show-error --max-time 5 \
      --resolve "${PUBLIC_HOST}:443:127.0.0.1" \
      "https://${PUBLIC_HOST}${PUBLIC_PATH}student" >/dev/null; then
    break
  fi
  if [[ "${attempt}" == "30" ]]; then
    echo "主域名公开入口 https://${PUBLIC_HOST}${PUBLIC_PATH} 未通过页面、API 或 SPA 探针。" >&2
    exit 1
  fi
  sleep 2
done

ASSET_PATH="$(sed -nE 's#.*(src|href)="(/code/assets/[^"]+)".*#\2#p' "${PROBE_DIR}/code.html" | sed -n '1p')"
if [[ "${ASSET_PATH}" != /code/assets/* ]]; then
  echo "正式页面未引用 /code/assets/ 下的构建资源。" >&2
  exit 1
fi
curl --fail --silent --show-error --max-time 5 \
  --resolve "${PUBLIC_HOST}:443:127.0.0.1" \
  "https://${PUBLIC_HOST}${ASSET_PATH}" >/dev/null

for platform_path in /app/ /download/; do
  curl --fail --silent --show-error --max-time 5 \
    --resolve "${PUBLIC_HOST}:443:127.0.0.1" \
    --dump-header "${PROBE_DIR}/platform.headers" \
    "https://${PUBLIC_HOST}${platform_path}" >/dev/null
  if grep -qi '^x-proxy-source: Code-8081' "${PROBE_DIR}/platform.headers"; then
    echo "主平台路径 ${platform_path} 被 Online Judge 接管。" >&2
    exit 1
  fi
done

echo "人工生产部署完成：https://${PUBLIC_HOST}${PUBLIC_PATH}"
