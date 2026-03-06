#!/bin/bash
# 本地模拟 CI/CD 流程测试脚本

set -e

echo "=========================================="
echo "本地 CI/CD 流程测试"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试步骤计数
STEP=0

# 函数: 打印步骤
print_step() {
    STEP=$((STEP + 1))
    echo ""
    echo -e "${GREEN}[步骤 $STEP]${NC} $1"
    echo "----------------------------------------"
}

# 函数: 打印成功
print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

# 函数: 打印错误
print_error() {
    echo -e "${RED}✗${NC} $1"
}

# 函数: 打印警告
print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# 检查当前目录
if [ ! -f "pom.xml" ]; then
    print_error "错误: 请在 sism-backend 根目录下运行此脚本"
    exit 1
fi

# 步骤 1: 检查 Git 状态
print_step "检查 Git 状态"
if git diff --quiet && git diff --cached --quiet; then
    print_success "工作目录干净"
else
    print_warning "工作目录有未提交的更改"
    git status --short
fi

# 步骤 2: 检查 Java 版本
print_step "检查 Java 版本"
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        print_success "Java 版本: $(java -version 2>&1 | head -n 1)"
    else
        print_error "Java 版本过低，需要 Java 17+"
        exit 1
    fi
else
    print_error "未找到 Java"
    exit 1
fi

# 步骤 3: 检查 Maven
print_step "检查 Maven"
if command -v mvn &> /dev/null; then
    print_success "Maven 版本: $(mvn -version | head -n 1)"
else
    print_error "未找到 Maven"
    exit 1
fi

# 步骤 4: 清理构建目录
print_step "清理构建目录"
if [ -d "target" ]; then
    rm -rf target
    print_success "已清理 target 目录"
else
    print_success "target 目录不存在，跳过清理"
fi

# 步骤 5: Maven 构建（跳过测试）
print_step "Maven 构建（跳过测试）"
echo "执行: mvn clean package -DskipTests"
if mvn clean package -B -DskipTests -Dmaven.test.skip=true -Dmaven.javadoc.skip=true; then
    print_success "构建成功"
else
    print_error "构建失败"
    exit 1
fi

# 步骤 6: 检查 JAR 文件
print_step "检查 JAR 文件"
JAR_FILE="target/sism-backend-1.0.0.jar"
if [ -f "$JAR_FILE" ]; then
    JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
    print_success "JAR 文件已生成: $JAR_FILE ($JAR_SIZE)"
else
    print_error "JAR 文件未找到: $JAR_FILE"
    exit 1
fi

# 步骤 7: 检查 .env 文件
print_step "检查环境配置"
if [ -f ".env" ]; then
    print_success "找到 .env 文件"
    
    # 检查必需的环境变量
    REQUIRED_VARS=("JWT_SECRET" "DB_URL" "DB_USERNAME" "DB_PASSWORD")
    for var in "${REQUIRED_VARS[@]}"; do
        if grep -q "^$var=" .env; then
            print_success "  $var 已配置"
        else
            print_warning "  $var 未配置"
        fi
    done
else
    print_warning ".env 文件不存在"
    echo "  提示: 复制 .env.example 并配置环境变量"
fi

# 步骤 8: 测试 JAR 启动（可选）
print_step "测试 JAR 启动（可选）"
echo "是否测试 JAR 启动？这将启动应用程序 10 秒后自动停止。"
echo "按 Enter 跳过，输入 'y' 继续测试："
read -t 5 -r TEST_START || TEST_START=""

if [ "$TEST_START" = "y" ] || [ "$TEST_START" = "Y" ]; then
    echo "启动应用程序..."
    java -jar "$JAR_FILE" &
    APP_PID=$!
    
    echo "等待 10 秒..."
    sleep 10
    
    # 测试健康检查
    if curl -sf http://localhost:8080/api/actuator/health > /dev/null 2>&1; then
        print_success "健康检查通过"
        curl -s http://localhost:8080/api/actuator/health | head -5
    else
        print_warning "健康检查失败（可能需要更长启动时间）"
    fi
    
    # 停止应用
    echo "停止应用程序..."
    kill $APP_PID 2>/dev/null || true
    wait $APP_PID 2>/dev/null || true
    print_success "应用程序已停止"
else
    print_warning "跳过 JAR 启动测试"
fi

# 步骤 9: 检查 Git 提交
print_step "检查 Git 提交"
LAST_COMMIT=$(git log -1 --oneline)
print_success "最新提交: $LAST_COMMIT"

# 步骤 10: 检查远程分支
print_step "检查远程分支"
git fetch origin main --quiet
LOCAL_COMMIT=$(git rev-parse main)
REMOTE_COMMIT=$(git rev-parse origin/main)

if [ "$LOCAL_COMMIT" = "$REMOTE_COMMIT" ]; then
    print_success "本地分支与远程分支同步"
else
    print_warning "本地分支与远程分支不同步"
    echo "  本地: $LOCAL_COMMIT"
    echo "  远程: $REMOTE_COMMIT"
    
    # 检查是否领先或落后
    AHEAD=$(git rev-list --count origin/main..main)
    BEHIND=$(git rev-list --count main..origin/main)
    
    if [ "$AHEAD" -gt 0 ]; then
        echo "  本地领先远程 $AHEAD 个提交"
    fi
    if [ "$BEHIND" -gt 0 ]; then
        echo "  本地落后远程 $BEHIND 个提交"
    fi
fi

# 总结
echo ""
echo "=========================================="
echo -e "${GREEN}✓ 本地 CI/CD 测试完成${NC}"
echo "=========================================="
echo ""
echo "测试结果:"
echo "  ✓ Git 状态检查"
echo "  ✓ Java 环境检查"
echo "  ✓ Maven 构建成功"
echo "  ✓ JAR 文件生成"
echo ""
echo "下一步:"
echo "  1. 确认所有更改已提交"
echo "  2. 推送到远程仓库: git push origin main"
echo "  3. 观察 GitHub Actions 执行情况"
echo ""
