#!/bin/bash
# SISM 数据库同步脚本
# 从远程数据库同步到本地数据库

set -e

# 配置参数
REMOTE_HOST="175.24.139.148"
REMOTE_PORT="8386"
REMOTE_USER="postgres"
REMOTE_DB="strategic"
REMOTE_PASSWORD="64378561huaW"

LOCAL_HOST="localhost"
LOCAL_PORT="5432"
LOCAL_USER="blackevil"
LOCAL_DB="sism_db"

BACKUP_DIR="/Users/blackevil/sism-db-backups"
BACKUP_FILE="${BACKUP_DIR}/sism-remote-$(date +%Y%m%d_%H%M%S).dump"

# 创建备份目录
mkdir -p "${BACKUP_DIR}"

echo "========================================="
echo "SISM 数据库同步"
echo "========================================="
echo "远程: ${REMOTE_HOST}:${REMOTE_PORT}/${REMOTE_DB}"
echo "本地: ${LOCAL_HOST}:${LOCAL_PORT}/${LOCAL_DB}"
echo "========================================="

# 1. 从远程导出数据
echo ""
echo "[1/3] 正在从远程数据库导出..."
PGPASSWORD="${REMOTE_PASSWORD}" pg_dump -h "${REMOTE_HOST}" -p "${REMOTE_PORT}" -U "${REMOTE_USER}" -d "${REMOTE_DB}" -Fc -f "${BACKUP_FILE}"

if [ $? -eq 0 ]; then
    echo "✓ 导出成功: ${BACKUP_FILE}"
else
    echo "✗ 导出失败"
    exit 1
fi

# 2. 恢复到本地数据库
echo ""
echo "[2/3] 正在恢复到本地数据库..."
pg_restore -h "${LOCAL_HOST}" -p "${LOCAL_PORT}" -U "${LOCAL_USER}" -d "${LOCAL_DB}" -c --no-owner --no-acl -v "${BACKUP_FILE}" 2>&1 | tail -20

if [ $? -eq 0 ] || [ $? -eq 1 ]; then
    echo "✓ 恢复完成 (部分警告可忽略)"
else
    echo "✗ 恢复失败"
    exit 1
fi

# 3. 验证数据
echo ""
echo "[3/3] 验证数据同步..."
LOCAL_TABLES=$(psql -h "${LOCAL_HOST}" -p "${LOCAL_PORT}" -U "${LOCAL_USER}" -d "${LOCAL_DB}" -t -c "SELECT COUNT(*) FROM pg_tables WHERE schemaname = 'public'")
LOCAL_PLANS=$(psql -h "${LOCAL_HOST}" -p "${LOCAL_PORT}" -U "${LOCAL_USER}" -d "${LOCAL_DB}" -t -c "SELECT COUNT(*) FROM plan")
echo "✓ 本地表数量: ${LOCAL_TABLES}"
echo "✓ 本地 plan 记录数: ${LOCAL_PLANS}"

echo ""
echo "========================================="
echo "✓ 数据库同步完成！"
echo "========================================="
