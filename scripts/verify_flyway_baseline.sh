#!/bin/bash

# =============================================================================
# Flyway 基线验证脚本
# =============================================================================
# 用途：启动应用后验证 Flyway 基线是否正确设置
# 使用：./verify_flyway_baseline.sh
# =============================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 数据库连接信息
DB_HOST="175.24.139.148"
DB_PORT="8386"
DB_NAME="strategic"
DB_USER="postgres"
DB_PASSWORD="64378561huaW"

echo -e "${GREEN}==============================================================================${NC}"
echo -e "${GREEN}Flyway 基线验证${NC}"
echo -e "${GREEN}==============================================================================${NC}"
echo ""

# 检查 psql 是否可用
if ! command -v psql &> /dev/null; then
    echo -e "${RED}错误: psql 未安装或不在 PATH 中${NC}"
    echo -e "${YELLOW}提示: psql 已随 libpq 一起安装，请确保 PATH 正确${NC}"
    exit 1
fi

echo -e "${BLUE}查询 Flyway 历史记录...${NC}"
echo ""

PGPASSWORD="${DB_PASSWORD}" psql \
    -h "${DB_HOST}" \
    -p "${DB_PORT}" \
    -U "${DB_USER}" \
    -d "${DB_NAME}" \
    -c "SELECT installed_rank, version, description, type, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;" \
    2>/dev/null

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✓ Flyway 历史记录查询成功${NC}"
    echo ""
    echo -e "${YELLOW}预期结果：${NC}"
    echo -e "  - 应该看到 version: 13, type: BASELINE 的记录"
    echo -e "  - V1-V13 的所有版本都应该被标记为已执行"
    echo -e "  - 后续新增的 V14+ 版本会在下次启动时执行"
else
    echo ""
    echo -e "${YELLOW}注意: flyway_schema_history 表不存在${NC}"
    echo -e "${YELLOW}这是正常的，请先启动应用，Flyway 会自动创建并初始化${NC}"
fi

echo ""
echo -e "${GREEN}==============================================================================${NC}"
