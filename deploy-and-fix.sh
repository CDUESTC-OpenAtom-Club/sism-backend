#!/bin/bash
# 生产环境自动部署和修复脚本
# 日期: 2026-01-28
# 用途: 自动完成部署验证和数据库修复

set -e  # 遇到错误立即退出

echo "========================================="
echo "生产环境自动部署和修复脚本"
echo "========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 配置变量（请根据实际情况修改）
DB_HOST="${DB_HOST:-localhost}"
DB_USER="${DB_USER:-sism_user}"
DB_NAME="${DB_NAME:-sism_db}"
BACKEND_SERVICE="sism-backend"
BACKEND_JAR_PATH="/opt/sism/backend/sism-backend-1.0.0.jar"

echo -e "${CYAN}步骤 1: 检查后端服务状态${NC}"
echo "-----------------------------------------"
sudo systemctl status $BACKEND_SERVICE --no-pager | head -n 10
echo ""

echo -e "${CYAN}步骤 2: 检查 JAR 文件${NC}"
echo "-----------------------------------------"
if [ -f "$BACKEND_JAR_PATH" ]; then
    echo -e "${GREEN}✓${NC} JAR 文件存在"
    ls -lh $BACKEND_JAR_PATH
    echo "修改时间: $(stat -c %y $BACKEND_JAR_PATH)"
else
    echo -e "${RED}✗${NC} JAR 文件不存在: $BACKEND_JAR_PATH"
    exit 1
fi
echo ""

echo -e "${CYAN}步骤 3: 下载数据库修复脚本${NC}"
echo "-----------------------------------------"
cd /tmp
if [ -f "diagnose-and-fix-production.sql" ]; then
    rm diagnose-and-fix-production.sql
fi

wget -q https://raw.githubusercontent.com/CDUESTC-OpenAtom-Club/sism-backend/main/database/scripts/diagnose-and-fix-production.sql

if [ -f "diagnose-and-fix-production.sql" ]; then
    echo -e "${GREEN}✓${NC} 修复脚本下载成功"
else
    echo -e "${RED}✗${NC} 修复脚本下载失败"
    exit 1
fi
echo ""

echo -e "${CYAN}步骤 4: 执行数据库修复脚本${NC}"
echo "-----------------------------------------"
echo "连接数据库: $DB_HOST / $DB_NAME"
echo ""

# 执行修复脚本
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -U $DB_USER -d $DB_NAME -f diagnose-and-fix-production.sql

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✓${NC} 数据库修复脚本执行成功"
else
    echo ""
    echo -e "${RED}✗${NC} 数据库修复脚本执行失败"
    exit 1
fi
echo ""

echo -e "${CYAN}步骤 5: 重启后端服务${NC}"
echo "-----------------------------------------"
echo "停止服务..."
sudo systemctl stop $BACKEND_SERVICE

echo "等待 3 秒..."
sleep 3

echo "启动服务..."
sudo systemctl start $BACKEND_SERVICE

echo "等待服务启动..."
sleep 10

echo "检查服务状态..."
sudo systemctl status $BACKEND_SERVICE --no-pager | head -n 10

if sudo systemctl is-active --quiet $BACKEND_SERVICE; then
    echo -e "${GREEN}✓${NC} 服务启动成功"
else
    echo -e "${RED}✗${NC} 服务启动失败"
    echo "查看日志:"
    sudo journalctl -u $BACKEND_SERVICE -n 50 --no-pager
    exit 1
fi
echo ""

echo -e "${CYAN}步骤 6: 验证修复结果${NC}"
echo "-----------------------------------------"

# 等待服务完全启动
echo "等待服务完全启动 (15秒)..."
sleep 15

# 测试健康检查
echo "测试健康检查..."
HEALTH_RESPONSE=$(curl -s http://localhost:8080/api/actuator/health)
HEALTH_STATUS=$(echo $HEALTH_RESPONSE | jq -r '.status' 2>/dev/null || echo "ERROR")

if [ "$HEALTH_STATUS" = "UP" ]; then
    echo -e "${GREEN}✓${NC} 健康检查通过: $HEALTH_STATUS"
else
    echo -e "${RED}✗${NC} 健康检查失败: $HEALTH_STATUS"
    echo "响应: $HEALTH_RESPONSE"
fi
echo ""

# 测试登录
echo "测试用户登录..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zhanlue","password":"123456"}')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.token' 2>/dev/null || echo "")

if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
    echo -e "${GREEN}✓${NC} zhanlue 用户登录成功"
else
    echo -e "${RED}✗${NC} zhanlue 用户登录失败"
    echo "响应: $LOGIN_RESPONSE"
fi
echo ""

# 测试 keyan 用户登录
echo "测试 keyan 用户登录..."
KEYAN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"keyan","password":"123456"}')

KEYAN_TOKEN=$(echo $KEYAN_RESPONSE | jq -r '.data.token' 2>/dev/null || echo "")

if [ -n "$KEYAN_TOKEN" ] && [ "$KEYAN_TOKEN" != "null" ]; then
    echo -e "${GREEN}✓${NC} keyan 用户登录成功"
else
    echo -e "${RED}✗${NC} keyan 用户登录失败"
    echo "响应: $KEYAN_RESPONSE"
fi
echo ""

# 测试考核周期 API
if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
    echo "测试考核周期 API..."
    CYCLE_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
      http://localhost:8080/api/cycles)
    
    CYCLE_CODE=$(echo $CYCLE_RESPONSE | jq -r '.code' 2>/dev/null || echo "ERROR")
    
    if [ "$CYCLE_CODE" = "0" ]; then
        CYCLE_COUNT=$(echo $CYCLE_RESPONSE | jq -r '.data | length' 2>/dev/null || echo "0")
        echo -e "${GREEN}✓${NC} 考核周期 API 正常，返回 $CYCLE_COUNT 个周期"
        echo $CYCLE_RESPONSE | jq '.data[] | {year, cycleName}' 2>/dev/null || echo "无法解析数据"
    else
        echo -e "${RED}✗${NC} 考核周期 API 失败"
        echo "响应: $CYCLE_RESPONSE"
    fi
fi
echo ""

echo "========================================="
echo "部署和修复完成！"
echo "========================================="
echo ""
echo "验证结果总结:"
echo "- 后端服务: $(sudo systemctl is-active $BACKEND_SERVICE)"
echo "- 健康检查: $HEALTH_STATUS"
echo "- zhanlue 登录: $([ -n "$TOKEN" ] && [ "$TOKEN" != "null" ] && echo '成功' || echo '失败')"
echo "- keyan 登录: $([ -n "$KEYAN_TOKEN" ] && [ "$KEYAN_TOKEN" != "null" ] && echo '成功' || echo '失败')"
echo "- 考核周期 API: $([ "$CYCLE_CODE" = "0" ] && echo '成功' || echo '失败')"
echo ""
echo "下一步:"
echo "1. 访问 https://blackevil.cn 测试前端"
echo "2. 使用浏览器测试完整功能"
echo "3. 更新验证报告"
echo ""
