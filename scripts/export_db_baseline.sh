#!/bin/bash

# =============================================================================
# 数据库结构导出脚本 - 生成 Flyway 基线版本
# =============================================================================
# 用途：导出当前数据库的完整结构，生成新的 Flyway 基线版本
# 使用：./export_db_baseline.sh
# =============================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 数据库连接信息
DB_HOST="175.24.139.148"
DB_PORT="8386"
DB_NAME="strategic"
DB_USER="postgres"
DB_PASSWORD="64378561huaW"

# 输出文件
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
OUTPUT_DIR="${PROJECT_DIR}/src/main/resources/db/migration"
BACKUP_FILE="${SCRIPT_DIR}/db_structure_backup_${TIMESTAMP}.sql"
BASELINE_FILE="${OUTPUT_DIR}/V13__baseline_from_current_db.sql"

# 确保目录存在
mkdir -p "${OUTPUT_DIR}"
mkdir -p "${SCRIPT_DIR}"

echo -e "${GREEN}==============================================================================${NC}"
echo -e "${GREEN}数据库结构导出 - 生成 Flyway 基线版本${NC}"
echo -e "${GREEN}==============================================================================${NC}"
echo ""

# 检查 pg_dump 是否可用
if ! command -v pg_dump &> /dev/null; then
    echo -e "${RED}错误: pg_dump 未安装或不在 PATH 中${NC}"
    echo -e "${YELLOW}请安装 PostgreSQL 客户端工具${NC}"
    exit 1
fi

echo -e "${YELLOW}步骤 1/4: 导出数据库结构（仅 schema，不含数据）...${NC}"
PGPASSWORD="${DB_PASSWORD}" pg_dump \
    -h "${DB_HOST}" \
    -p "${DB_PORT}" \
    -U "${DB_USER}" \
    -d "${DB_NAME}" \
    --schema-only \
    --no-owner \
    --no-privileges \
    --no-tablespaces \
    --no-comments \
    -f "${BACKUP_FILE}"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ 数据库结构已导出到: ${BACKUP_FILE}${NC}"
else
    echo -e "${RED}✗ 导出失败${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}步骤 2/4: 清理导出文件（移除 Flyway 历史表）...${NC}"

# 创建清理后的版本（移除 flyway_schema_history 表）
cat "${BACKUP_FILE}" | \
    sed '/CREATE TABLE.*flyway_schema_history/,/;/d' | \
    sed '/ALTER TABLE.*flyway_schema_history/d' | \
    sed '/CREATE INDEX.*flyway_schema_history/d' > "${BASELINE_FILE}"

echo -e "${GREEN}✓ 已生成基线版本: ${BASELINE_FILE}${NC}"

echo ""
echo -e "${YELLOW}步骤 3/4: 添加文件头注释...${NC}"

# 添加注释头
cat > "${BASELINE_FILE}.tmp" << 'EOF'
-- =============================================================================
-- Flyway 基线版本 V13
-- =============================================================================
-- 说明：
-- 1. 此文件是从生产数据库导出的完整结构
-- 2. 用作 Flyway 的新基线版本
-- 3. 之前的 V1-V12 版本将被标记为"已执行"但不会实际运行
-- 4. 后续的数据库变更请创建 V14、V15 等新版本
-- =============================================================================

EOF

cat "${BASELINE_FILE}" >> "${BASELINE_FILE}.tmp"
mv "${BASELINE_FILE}.tmp" "${BASELINE_FILE}"

echo -e "${GREEN}✓ 已添加注释头${NC}"

echo ""
echo -e "${YELLOW}步骤 4/4: 生成配置更新说明...${NC}"

cat > "${SCRIPT_DIR}/BASELINE_SETUP_INSTRUCTIONS.md" << 'EOF'
# Flyway 基线设置说明

## 已完成的操作

1. ✅ 从当前数据库导出了完整结构
2. ✅ 生成了新的基线版本 `V13__baseline_from_current_db.sql`
3. ✅ 备份了原始导出文件

## 下一步操作

### 1. 更新 Flyway 配置

编辑 `sism-backend/src/main/resources/application.yml`，修改 Flyway 配置：

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 13  # 改为 13
    locations: classpath:db/migration
    validate-on-migrate: true
    out-of-order: false
```

### 2. 首次启动流程

当你启动应用时，Flyway 会：

1. 检测到数据库已存在且没有 `flyway_schema_history` 表
2. 自动执行 baseline，在数据库中创建 `flyway_schema_history` 表
3. 插入一条记录：`version: 13, description: << Flyway Baseline >>`
4. 将 V1-V13 的所有脚本标记为"已执行"（但不会实际运行 SQL）
5. 下次启动时，只会运行 V14 及以后的新版本

### 3. 验证步骤

启动应用后，连接数据库执行：

```sql
-- 查看 Flyway 历史记录
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- 应该看到：
-- version | description           | type     | success
-- --------|----------------------|----------|--------
-- 13      | << Flyway Baseline >> | BASELINE | true
```

### 4. 后续开发

从现在开始，所有新的数据库变更都应该创建新的版本：

- V14__add_new_feature.sql
- V15__modify_table.sql
- ...

## 安全保障

- ✅ 原始数据库结构已备份
- ✅ V1-V12 的脚本仍然保留（作为历史记录）
- ✅ 不会对现有数据库结构做任何修改
- ✅ 可以随时回滚（删除 flyway_schema_history 表即可重新开始）

## 文件清单

- `V13__baseline_from_current_db.sql` - 新的基线版本
- `db_structure_backup_*.sql` - 原始导出备份
- `BASELINE_SETUP_INSTRUCTIONS.md` - 本说明文件

EOF

echo -e "${GREEN}✓ 已生成配置说明: ${SCRIPT_DIR}/BASELINE_SETUP_INSTRUCTIONS.md${NC}"

echo ""
echo -e "${GREEN}==============================================================================${NC}"
echo -e "${GREEN}导出完成！${NC}"
echo -e "${GREEN}==============================================================================${NC}"
echo ""
echo -e "${YELLOW}生成的文件：${NC}"
echo -e "  1. 基线版本: ${GREEN}${BASELINE_FILE}${NC}"
echo -e "  2. 原始备份: ${GREEN}${BACKUP_FILE}${NC}"
echo -e "  3. 配置说明: ${GREEN}sism-backend/scripts/BASELINE_SETUP_INSTRUCTIONS.md${NC}"
echo ""
echo -e "${YELLOW}下一步：${NC}"
echo -e "  1. 查看配置说明: ${GREEN}cat scripts/BASELINE_SETUP_INSTRUCTIONS.md${NC}"
echo -e "  2. 更新 application.yml 中的 baseline-version 为 13"
echo -e "  3. 启动应用，Flyway 会自动完成基线化"
echo ""
