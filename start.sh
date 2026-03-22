#!/bin/bash

set -e

ENV_FILE=".env"

# Kill known SISM backend JVMs first, then fall back to the port.
cleanup_backend_processes() {
    local pids=""

    pids=$(ps aux | grep -E "sism-main-1.0.0.jar|sism-backend-1.0.0.jar" | grep -v grep | awk '{print $2}' || true)
    if [ -n "$pids" ]; then
        echo "⚠ 检测到旧的 SISM 后端进程，按进程名停止..."
        echo "$pids" | xargs kill -9 2>/dev/null || true
        sleep 2
    fi

    pids=$(lsof -ti :8080 2>/dev/null || true)
    if [ -n "$pids" ]; then
        echo "⚠ 端口 8080 仍被占用，按端口兜底停止..."
        echo "$pids" | xargs kill -9 2>/dev/null || true
        sleep 2
    fi
}

# Load environment variables from .env file
if [ ! -f "$ENV_FILE" ]; then
    echo "✗ 未找到环境文件: $(pwd)/$ENV_FILE"
    exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

echo "✓ 已加载环境文件: $(pwd)/$ENV_FILE"
echo "✓ 当前数据库: ${DB_URL:-<missing DB_URL>}"

cleanup_backend_processes

# Start Spring Boot application (skip tests)
mvn spring-boot:run -Dmaven.test.skip=true
