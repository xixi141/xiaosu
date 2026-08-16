#!/usr/bin/env bash
# 把 knowledge/ 下所有文档导入知识库
set -euo pipefail
cd "$(dirname "$0")/.."
BASE_URL="${BASE_URL:-http://localhost:8080}"

for file in knowledge/*; do
  name=$(basename "$file")
  echo "[seed] 上传 $name ..."
  curl -sf -X POST "$BASE_URL/api/documents" -F "file=@$file" > /dev/null || {
    echo "[seed] $name 上传失败（服务未启动？）"
    exit 1
  }
done
echo "[seed] 完成"
