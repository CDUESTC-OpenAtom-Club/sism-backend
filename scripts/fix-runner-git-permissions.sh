#!/bin/bash
# 一次性修复 GitHub Actions self-hosted runner 的 Git 权限问题
# 需要在服务器上以 root 或 sudo 权限执行

set -e

echo "=========================================="
echo "修复 GitHub Actions Runner Git 权限"
echo "=========================================="
echo ""

# 检查是否有 sudo 权限
if [ "$EUID" -ne 0 ]; then 
    echo "错误: 请使用 sudo 运行此脚本"
    echo "用法: sudo bash scripts/fix-runner-git-permissions.sh"
    exit 1
fi

# 配置
RUNNER_BASE="/opt/sism/actions-runner"
WORK_BASE="$RUNNER_BASE/_work"
BACKEND_WORK="$WORK_BASE/sism-backend/sism-backend"
FRONTEND_WORK="$WORK_BASE/strategic-task-management/strategic-task-management"

# 获取 runner 用户（通常是运行 runner 服务的用户）
RUNNER_USER="sism"  # 根据实际情况修改

echo "配置信息:"
echo "  Runner 用户: $RUNNER_USER"
echo "  工作目录: $WORK_BASE"
echo ""

# 函数: 修复目录权限
fix_directory_permissions() {
    local dir=$1
    local name=$2
    
    if [ -d "$dir" ]; then
        echo "修复 $name 权限: $dir"
        
        # 修复所有者
        chown -R $RUNNER_USER:$RUNNER_USER "$dir"
        
        # 修复权限
        find "$dir" -type d -exec chmod 755 {} \;
        find "$dir" -type f -exec chmod 644 {} \;
        
        # 修复 .git 目录特殊权限
        if [ -d "$dir/.git" ]; then
            chmod -R u+rwX "$dir/.git"
            # 清理可能的锁文件
            rm -f "$dir/.git/index.lock" 2>/dev/null || true
            rm -f "$dir/.git/HEAD.lock" 2>/dev/null || true
            rm -f "$dir/.git/refs/heads/*.lock" 2>/dev/null || true
        fi
        
        echo "  ✓ $name 权限已修复"
    else
        echo "  ⚠ $name 目录不存在: $dir"
    fi
    echo ""
}

# 修复 runner 基础目录
echo "1. 修复 Runner 基础目录"
if [ -d "$RUNNER_BASE" ]; then
    chown -R $RUNNER_USER:$RUNNER_USER "$RUNNER_BASE"
    echo "  ✓ Runner 基础目录权限已修复"
else
    echo "  ⚠ Runner 基础目录不存在: $RUNNER_BASE"
fi
echo ""

# 修复工作目录
echo "2. 修复工作目录"
if [ -d "$WORK_BASE" ]; then
    chown -R $RUNNER_USER:$RUNNER_USER "$WORK_BASE"
    chmod -R u+rwX "$WORK_BASE"
    echo "  ✓ 工作目录权限已修复"
else
    echo "  ⚠ 工作目录不存在: $WORK_BASE"
fi
echo ""

# 修复后端仓库
echo "3. 修复后端仓库"
fix_directory_permissions "$BACKEND_WORK" "后端仓库"

# 修复前端仓库
echo "4. 修复前端仓库"
fix_directory_permissions "$FRONTEND_WORK" "前端仓库"

# 修复临时目录
echo "5. 修复临时目录"
TEMP_DIR="$WORK_BASE/_temp"
if [ -d "$TEMP_DIR" ]; then
    chown -R $RUNNER_USER:$RUNNER_USER "$TEMP_DIR"
    chmod -R u+rwX "$TEMP_DIR"
    echo "  ✓ 临时目录权限已修复"
else
    echo "  ⚠ 临时目录不存在: $TEMP_DIR"
fi
echo ""

# 清理 Git 对象数据库
echo "6. 清理 Git 对象数据库"
for repo in "$BACKEND_WORK" "$FRONTEND_WORK"; do
    if [ -d "$repo/.git" ]; then
        echo "  清理: $repo"
        cd "$repo"
        sudo -u $RUNNER_USER git fsck --full 2>/dev/null || true
        sudo -u $RUNNER_USER git gc --prune=now 2>/dev/null || true
        echo "  ✓ Git 对象数据库已清理"
    fi
done
echo ""

# 验证权限
echo "7. 验证权限"
echo "  后端仓库所有者:"
ls -ld "$BACKEND_WORK" 2>/dev/null || echo "    目录不存在"
echo "  前端仓库所有者:"
ls -ld "$FRONTEND_WORK" 2>/dev/null || echo "    目录不存在"
echo ""

echo "=========================================="
echo "✓ 权限修复完成！"
echo "=========================================="
echo ""
echo "下一步:"
echo "  1. 重启 GitHub Actions Runner 服务"
echo "     sudo systemctl restart actions.runner.*.service"
echo ""
echo "  2. 或者手动重启 runner"
echo "     cd $RUNNER_BASE"
echo "     sudo -u $RUNNER_USER ./svc.sh stop"
echo "     sudo -u $RUNNER_USER ./svc.sh start"
echo ""
echo "  3. 重新推送代码触发 CI/CD"
echo ""
