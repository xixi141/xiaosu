#!/usr/bin/env bash
# 把 knowledge/ 下所有文档导入知识库
set -euo pipefail
cd "$(dirname "$0")/.."
BASE_URL="${BASE_URL:-http://localhost:8080}"

# Windows Git Bash 下必须用系统原生 curl：MSYS2 的 /mingw64/bin/curl 会把中文文件名
# 按 ANSI 码页(GBK)转换后发出，服务端按 UTF-8 解码 → 文件名乱码入库（实测见 AI_USAGE.md）
CURL=curl
[ -x /c/Windows/System32/curl.exe ] && CURL=/c/Windows/System32/curl.exe

for file in knowledge/*; do
  name=$(basename "$file")
  echo "[seed] 上传 $name ..."
  "$CURL" -sf -X POST "$BASE_URL/api/documents" -F "file=@$file" > /dev/null || {
    echo "[seed] $name 上传失败（服务未启动？）"
    exit 1
  }
done
echo "[seed] 完成"
