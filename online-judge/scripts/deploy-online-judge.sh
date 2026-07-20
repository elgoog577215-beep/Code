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
bash scripts/check-database-schema-readiness.sh
bash scripts/check-discipline-data-quality.sh
bash scripts/check-test-case-semantic-quality.sh

for attempt in $(seq 1 30); do
  if curl --fail --silent --show-error --max-time 5 \
    "http://127.0.0.1:${SERVER_PORT:-8081}/app/" >/dev/null; then
    break
  fi
  if [[ "${attempt}" == "30" ]]; then
    echo "应用在等待窗口内未通过页面探针。" >&2
    exit 1
  fi
  sleep 2
done

nginx -t

for attempt in $(seq 1 30); do
  if curl --fail --silent --show-error --max-time 5 \
    --resolve "${PUBLIC_HOST}:443:127.0.0.1" \
    "https://${PUBLIC_HOST}${PUBLIC_PATH}" >/dev/null \
    && curl --fail --silent --show-error --max-time 5 \
      --resolve "${PUBLIC_HOST}:443:127.0.0.1" \
      "https://${PUBLIC_HOST}${PUBLIC_PATH}api/system/readiness" >/dev/null; then
    break
  fi
  if [[ "${attempt}" == "30" ]]; then
    echo "主域名公开入口 https://${PUBLIC_HOST}${PUBLIC_PATH} 未通过页面或 readiness 探针。" >&2
    exit 1
  fi
  sleep 2
done

echo "人工生产部署完成：https://${PUBLIC_HOST}${PUBLIC_PATH}"
