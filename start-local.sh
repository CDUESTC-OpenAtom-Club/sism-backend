#!/bin/bash

# SISM Backend Local Development Start Script
# 启动本地后端服务（连接远程数据库）

set -e

echo "=== SISM 后端服务启动脚本 ==="
echo ""

# 设置 Java 环境
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH=$JAVA_HOME/bin:$PATH

echo "✓ Java 环境: $(java -version 2>&1 | head -1)"
echo ""

# 切换到脚本所在目录
cd "$(dirname "$0")"

# 检查 JAR 文件是否存在
if [ ! -f "target/sism-backend-1.0.0.jar" ]; then
    echo "⚠ JAR 文件不存在，正在构建..."
    JAVA_HOME=/opt/homebrew/opt/openjdk@17 /opt/homebrew/bin/mvn clean package -DskipTests -Dmaven.test.skip=true -q
    echo "✓ 构建完成"
fi

# 检查是否已有服务在运行
if lsof -i :8080 > /dev/null 2>&1; then
    echo "⚠ 端口 8080 已被占用，正在停止旧服务..."
    ps aux | grep "sism-backend-1.0.0.jar" | grep -v grep | awk '{print $2}' | xargs kill -9 2>/dev/null || true
    sleep 2
fi

echo "→ 启动后端服务..."
JAVA_HOME=/opt/homebrew/opt/openjdk@17 java -jar target/sism-backend-1.0.0.jar > /tmp/sism-backend.log 2>&1 &
BACKEND_PID=$!

echo "✓ 后端服务已启动 (PID: $BACKEND_PID)"
echo ""
echo "等待服务就绪..."
sleep 10

# 检查服务状态
if curl -s http://localhost:8080/api/actuator/health > /dev/null 2>&1; then
    echo "✓ 服务启动成功！"
    echo ""
    echo "服务地址: http://localhost:8080"
    echo "健康检查: http://localhost:8080/api/actuator/health"
    echo "日志文件: /tmp/sism-backend.log"
    echo ""
    echo "查看日志: tail -f /tmp/sism-backend.log"
else
    echo "✗ 服务启动失败，请检查日志:"
    tail -20 /tmp/sism-backend.log
    exit 1
fi
