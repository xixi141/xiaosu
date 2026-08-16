#!/usr/bin/env bash
# 一条命令启动前后端（Windows Git Bash / Linux / macOS 通用）
set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  echo "[dev.sh] 缺少 .env 文件，请先执行: cp .env.example .env 并填入 API Key"
  exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

echo "[dev.sh] 启动后端 (http://localhost:${SERVER_PORT:-8080}) ..."
(cd backend && mvn -q spring-boot:run) &
BACKEND_PID=$!

echo "[dev.sh] 启动前端 (http://localhost:5173) ..."
(cd web && pnpm dev) &
WEB_PID=$!

trap 'echo "[dev.sh] 停止服务"; kill $BACKEND_PID $WEB_PID 2>/dev/null || true' EXIT INT TERM
wait
