#!/bin/bash
# 清理Redis缓存 - 防止删除类文件后ClassNotFoundException
# 在部署重构代码前执行

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Redis配置（从环境变量或默认值）
REDIS_HOST=${REDIS_HOST:-localhost}
REDIS_PORT=${REDIS_PORT:-6379}
REDIS_CLI="redis-cli -h $REDIS_HOST -p $REDIS_PORT"

# 测试Redis连接
if ! $REDIS_CLI PING > /dev/null 2>&1; then
    log_error "无法连接到Redis服务器 ($REDIS_HOST:$REDIS_PORT)"
    log_error "请检查Redis是否运行，或设置环境变量:"
    log_error "  export REDIS_HOST=your_redis_host"
    log_error "  export REDIS_PORT=your_redis_port"
    exit 1
fi

echo "========== 清理Redis缓存 =========="
log_info "Redis服务器: $REDIS_HOST:$REDIS_PORT"
log_info "时间: $(date)"
echo ""

# 1. 备份当前Redis数据（可选）
log_info "[1/5] 备份Redis数据..."
BACKUP_FILE="redis-backup-$(date +%Y%m%d_%H%M%S).rdb"
$REDIS_CLI BGSAVE
log_info "✅ Redis数据已后台保存到 dump.rdb"

# 2. 扫描需要清理的缓存键
log_info "[2/5] 扫描DDD相关的缓存键..."

PATTERNS=(
    "*Attachment*"
    "*attachment*"
    "*Indicator*"
    "*indicator*"
    "*Task*"
    "*task*"
    "*Audit*"
    "*audit*"
    "*Strategic*"
    "*strategic*"
    "*com.sism.shared*"
    "*com.sism.strategy*"
)

TOTAL_KEYS=0
for pattern in "${PATTERNS[@]}"; do
    COUNT=$($REDIS_CLI --scan --pattern "$pattern" | wc -l)
    if [ "$COUNT" -gt 0 ]; then
        echo "  发现 $COUNT 个匹配 '$pattern' 的键"
        TOTAL_KEYS=$((TOTAL_KEYS + COUNT))
    fi
done

if [ "$TOTAL_KEYS" -eq 0 ]; then
    log_info "✅ 没有发现相关缓存键，无需清理"
    exit 0
fi

echo ""
log_warn "发现 $TOTAL_KEYS 个相关缓存键"

# 3. 生成清理报告
log_info "[3/5] 生成清理报告..."
REPORT_FILE="redis-cache-cleanup-report-$(date +%Y%m%d_%H%M%S).txt"

{
    echo "Redis缓存清理报告"
    echo "=================="
    echo "时间: $(date)"
    echo "Redis服务器: $REDIS_HOST:$REDIS_PORT"
    echo "发现键总数: $TOTAL_KEYS"
    echo ""
    echo "详细键列表:"
    for pattern in "${PATTERNS[@]}"; do
        echo ""
        echo "模式: $pattern"
        $REDIS_CLI --scan --pattern "$pattern" | head -10
        REMAINING=$($REDIS_CLI --scan --pattern "$pattern" | wc -l)
        if [ "$REMAINING" -gt 10 ]; then
            echo "... (还有 $((REMAINING - 10)) 个键)"
        fi
    done
} > "$REPORT_FILE"

log_info "✅ 报告已保存: $REPORT_FILE"

# 4. 确认清理
echo ""
log_warn "⚠️  即将删除 $TOTAL_KEYS 个缓存键"
echo ""
read -p "确认删除？(yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    log_info "❌ 已取消清理操作"
    exit 0
fi

# 5. 执行清理
log_info "[4/5] 开始清理缓存..."

DELETED=0
for pattern in "${PATTERNS[@]}"; do
    KEYS=$($REDIS_CLI --scan --pattern "$pattern")
    if [ -n "$KEYS" ]; then
        COUNT=$(echo "$KEYS" | $REDIS_CLI -x DEL | awk '{print $1}')
        DELETED=$((DELETED + COUNT))
        echo "  ✓ 删除 $COUNT 个匹配 '$pattern' 的键"
    fi
done

log_info "✅ 已删除 $DELETED 个缓存键"

# 6. 验证清理结果
log_info "[5/5] 验证清理结果..."

REMAINING=0
for pattern in "${PATTERNS[@]}"; do
    COUNT=$($REDIS_CLI --scan --pattern "$pattern" | wc -l)
    REMAINING=$((REMAINING + COUNT))
done

if [ "$REMAINING" -eq 0 ]; then
    log_info "✅ 所有相关缓存已清理"
else
    log_warn "⚠️ 仍有 $REMAINING 个缓存键未清理"
    echo "剩余键列表:"
    for pattern in "${PATTERNS[@]}"; do
        $REDIS_CLI --scan --pattern "$pattern" | head -5
    done
fi

echo ""
echo "========== 清理完成 =========="
echo "已删除键: $DELETED"
echo "剩余键: $REMAINING"
echo "报告文件: $REPORT_FILE"
echo "备份文件: $BACKUP_FILE (在Redis数据目录)"
echo ""

# 保存清理记录
cat > "redis-cache-cleanup-log-$(date +%Y%m%d_%H%M%S).txt" <<EOF
Redis缓存清理记录
时间: $(date)
Redis服务器: $REDIS_HOST:$REDIS_PORT
已删除键: $DELETED
剩余键: $REMAINING
备份文件: $BACKUP_FILE
报告文件: $REPORT_FILE

恢复命令（如果需要）:
  1. 停止Redis服务器
  2. 复制备份文件到Redis数据目录
  3. 启动Redis服务器
EOF

log_info "✅ 清理记录已保存"
