#!/bin/bash
# skills-SpringAI-agent 启动脚本

set -e

# 加载环境变量
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

echo "========================================="
echo "  skills-SpringAI-agent 启动中..."
echo "========================================="
echo "  端口: ${SERVER_PORT:-8080}"
echo "  应用: skills-SpringAI-agent"
echo "========================================="

# 下载依赖并启动
./mvnw spring-boot:run
