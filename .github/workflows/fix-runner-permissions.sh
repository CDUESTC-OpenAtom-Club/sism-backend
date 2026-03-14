#!/bin/bash
# 修复 GitHub Actions self-hosted runner 的 Git 权限问题

set -e

echo "=== 修复 GitHub Actions Runner Git 权限问题 ==="

# 获取 runner 用户
RUNNER_USER=$(whoami)
echo "当前用户: $RUNNER_USER"

# 工作目录
WORK_DIR="/opt/sism/actions-runner/_work/sism-backend/sism-backend"

if [ -d "$WORK_DIR" ]; then
    echo "修复工作目录权限: $WORK_DIR"
    
    # 修复 .git 目录权限
    if [ -d "$WORK_DIR/.git" ]; then
        sudo chown -R $RUNNER_USER:$RUNNER_USER "$WORK_DIR/.git"
        sudo chmod -R u+rwX "$WORK_DIR/.git"
        echo "✓ .git 目录权限已修复"
    fi
    
    # 修复整个工作目录权限
    sudo chown -R $RUNNER_USER:$RUNNER_USER "$WORK_DIR"
    sudo chmod -R u+rwX "$WORK_DIR"
    echo "✓ 工作目录权限已修复"
    
    # 清理可能损坏的 Git 对象
    cd "$WORK_DIR"
    if [ -d ".git/objects" ]; then
        echo "清理 Git 对象数据库..."
        git fsck --full 2>/dev/null || true
        git gc --prune=now 2>/dev/null || true
        echo "✓ Git 对象数据库已清理"
    fi
else
    echo "⚠ 工作目录不存在: $WORK_DIR"
fi

# 修复 runner 临时目录权限
TEMP_DIR="/opt/sism/actions-runner/_work/_temp"
if [ -d "$TEMP_DIR" ]; then
    echo "修复临时目录权限: $TEMP_DIR"
    sudo chown -R $RUNNER_USER:$RUNNER_USER "$TEMP_DIR"
    sudo chmod -R u+rwX "$TEMP_DIR"
    echo "✓ 临时目录权限已修复"
fi

echo ""
echo "=== 权限修复完成 ==="
echo ""
echo "请在服务器上执行以下命令："
echo "  cd /opt/sism/actions-runner/_work/sism-backend/sism-backend"
echo "  sudo bash .github/workflows/fix-runner-permissions.sh"
