#!/usr/bin/env bash
# 跑全部自动化测试（Mock 模型离线跑，不花 API 钱；live 测试默认排除）
set -euo pipefail
cd "$(dirname "$0")/../backend"
echo "[test.sh] 运行后端测试..."
mvn test
echo "[test.sh] 全部通过"
