#!/bin/bash

# SISM 多Agent API测试框架 - 安装脚本

set -e

echo "=================================="
echo "SISM 多Agent API测试框架"
echo "环境准备"
echo "=================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 检查Node.js
if ! command -v node &> /dev/null; then
    echo "❌ Node.js 未安装"
    echo "请访问 https://nodejs.org/ 下载安装 Node.js 14+"
    exit 1
fi

echo -e "${GREEN}✅${NC} Node.js 版本: $(node -v)"

# 检查npm
if ! command -v npm &> /dev/null; then
    echo "❌ npm 未安装"
    exit 1
fi

echo -e "${GREEN}✅${NC} npm 版本: $(npm -v)"

# 进入脚本目录
cd "$(dirname "$0")"

# 安装依赖
echo ""
echo "📦 安装项目依赖..."
echo ""

npm install

echo ""
echo -e "${GREEN}✅${NC} 依赖安装完成！"
echo ""
echo "=================================="
echo "📋 下一步操作："
echo "=================================="
echo ""
echo "1. 配置测试环境（如需要）："
echo "   vim config/test-users.json"
echo ""
echo "2. 启动后端服务："
echo "   cd sism-backend"
echo "   mvn spring-boot:run"
echo ""
echo "3. 运行测试："
echo "   ./run-tests.sh --all"
echo ""
echo "4. 查看帮助："
echo "   ./run-tests.sh --help"
echo ""
echo -e "${YELLOW}💡 提示：首次运行请先阅读 QUICKSTART.md${NC}"
echo ""
