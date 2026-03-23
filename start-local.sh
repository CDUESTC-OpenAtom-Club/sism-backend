#!/bin/bash

# SISM Backend Local Development Start Script
# 启动本地后端服务（连接远程数据库）

set -e

JAR_PATH="sism-main/target/sism-main-1.0.0.jar"
ENV_FILE=".env"

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

echo "=== SISM 后端服务启动脚本 ==="
echo ""

# 设置 Java 环境
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH=$JAVA_HOME/bin:$PATH

echo "✓ Java 环境: $(java -version 2>&1 | head -1)"
echo ""

# 切换到脚本所在目录
cd "$(dirname "$0")"

# 显式加载 .env，避免回退到 application.yml 默认本地库
if [ -f "$ENV_FILE" ]; then
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
    echo "✓ 已加载环境文件: $(pwd)/$ENV_FILE"
    echo "✓ 当前数据库: ${DB_URL:-<missing DB_URL>}"
else
    echo "✗ 未找到环境文件: $(pwd)/$ENV_FILE"
    exit 1
fi
echo ""

echo "→ 重新构建后端模块并刷新本地 Maven 依赖..."
./mvnw -pl sism-main -am clean install -DskipTests -Dmaven.test.skip=true
echo "✓ 构建完成"

# 检查是否已有服务在运行
if lsof -i :8080 > /dev/null 2>&1; then
    cleanup_backend_processes
fi

echo "→ 启动后端服务..."
nohup env JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH="$JAVA_HOME/bin:$PATH" \
    java -jar "$JAR_PATH" > /tmp/sism-backend.log 2>&1 < /dev/null &
BACKEND_PID=$!
disown "$BACKEND_PID" 2>/dev/null || true

echo "✓ 后端服务已启动 (PID: $BACKEND_PID)"
echo ""
echo "等待服务就绪（最多60秒）..."

# 健康检查重试机制
MAX_RETRIES=30
RETRY_COUNT=0
HEALTH_OK=false

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -s http://localhost:8080/api/v1/auth/health > /dev/null 2>&1; then
        HEALTH_OK=true
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo -n "."
    sleep 2
done
echo ""

if [ "$HEALTH_OK" = false ]; then
    echo "✗ 服务启动超时，请检查日志:"
    tail -30 /tmp/sism-backend.log
    exit 1
fi

echo "✓ 服务启动成功！"
echo ""
echo "→ 验证关键接口..."

# 验证 /api/v1/plans 接口
if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/plans | grep -q "200\|401\|403"; then
    echo "✓ Plans 接口正常"
else
    echo "✗ Plans 接口异常"
fi

# 验证 /api/v1/workflows/my-tasks 接口
if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/workflows/my-tasks | grep -q "200\|401"; then
    echo "✓ Workflows 接口正常"
else
    echo "✗ Workflows 接口异常"
fi

echo ""
echo "服务地址: http://localhost:8080"
echo "健康检查: http://localhost:8080/api/v1/auth/health"
echo "日志文件: /tmp/sism-backend.log"
echo ""
echo "查看日志: tail -f /tmp/sism-backend.log"
