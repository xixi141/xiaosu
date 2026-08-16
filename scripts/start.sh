#!/usr/bin/env bash
# 本地生产模式一条命令：构建后端 jar + 前端产物，java -jar 直接跑
set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  echo "[start.sh] 缺少 .env 文件，请先执行: cp .env.example .env 并填入 API Key"
  exit 1
fi
set -a
# shellcheck disable=SC1091
source .env
set +a

echo "[start.sh] 构建后端..."
(cd backend && mvn -q -DskipTests package)

echo "[start.sh] 构建前端..."
(cd web && pnpm install --silent && pnpm build)

echo "[start.sh] 启动服务 (http://localhost:${SERVER_PORT:-8080})"
java -jar backend/target/xiaosu-backend-0.1.0.jar
