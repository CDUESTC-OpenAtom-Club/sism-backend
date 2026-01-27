#!/bin/bash
# ============================================
# 二级学院用户修复脚本
# 用途: 在生产数据库中添加二级学院测试用户
# 执行: sudo ./fix-college-users.sh
# ============================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}  二级学院用户修复脚本${NC}"
echo -e "${YELLOW}========================================${NC}"

# 数据库配置 (根据实际情况修改)
DB_NAME="${DB_NAME:-sism_prod}"
DB_USER="${DB_USER:-postgres}"

echo -e "\n${GREEN}[1/3] 检查数据库连接...${NC}"
if ! sudo -u postgres psql -d "$DB_NAME" -c "SELECT 1" > /dev/null 2>&1; then
    echo -e "${RED}错误: 无法连接到数据库 $DB_NAME${NC}"
    exit 1
fi
echo -e "${GREEN}✓ 数据库连接正常${NC}"

echo -e "\n${GREEN}[2/3] 检查二级学院组织机构...${NC}"
COLLEGE_COUNT=$(sudo -u postgres psql -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM org WHERE org_type = 'COLLEGE'")
echo -e "找到 ${COLLEGE_COUNT} 个二级学院"

echo -e "\n${GREEN}[3/3] 添加二级学院用户...${NC}"

# 密码: 123456 的 bcrypt 哈希值
PASSWORD_HASH='$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi'

sudo -u postgres psql -d "$DB_NAME" << EOF
-- 马克思主义学院用户 (org_id = 55)
INSERT INTO app_user (username, real_name, org_id, password_hash, is_active)
SELECT 'makesi', '马克思主义学院测试用户', 55, '$PASSWORD_HASH', TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'makesi');

-- 工学院用户 (org_id = 56)
INSERT INTO app_user (username, real_name, org_id, password_hash, is_active)
SELECT 'gongxue', '工学院测试用户', 56, '$PASSWORD_HASH', TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'gongxue');

-- 计算机学院用户 (org_id = 57)
INSERT INTO app_user (username, real_name, org_id, password_hash, is_active)
SELECT 'jisuanji', '计算机学院测试用户', 57, '$PASSWORD_HASH', TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'jisuanji');

-- 商学院用户 (org_id = 58)
INSERT INTO app_user (username, real_name, org_id, password_hash, is_active)
SELECT 'shangxue', '商学院测试用户', 58, '$PASSWORD_HASH', TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'shangxue');

-- 文理学院用户 (org_id = 59)
INSERT INTO app_user (username, real_name, org_id, password_hash, is_active)
SELECT 'wenli', '文理学院测试用户', 59, '$PASSWORD_HASH', TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'wenli');

-- 艺术与科技学院用户 (org_id = 60)
INSERT INTO app_user (username, real_name, org_id, password_hash, is_active)
SELECT 'yishu', '艺术与科技学院测试用户', 60, '$PASSWORD_HASH', TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'yishu');

-- 航空学院用户 (org_id = 61)
INSERT INTO app_user (username, real_name, org_id, password_hash, is_active)
SELECT 'hangkong', '航空学院测试用户', 61, '$PASSWORD_HASH', TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'hangkong');

-- 国际教育学院用户 (org_id = 62)
INSERT INTO app_user (username, real_name, org_id, password_hash, is_active)
SELECT 'guojijiaoyu', '国际教育学院测试用户', 62, '$PASSWORD_HASH', TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'guojijiaoyu');
EOF

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}  修复完成！验证结果:${NC}"
echo -e "${GREEN}========================================${NC}"

sudo -u postgres psql -d "$DB_NAME" -c "
SELECT u.user_id, u.username, u.real_name, o.org_name, u.is_active
FROM app_user u
JOIN org o ON u.org_id = o.org_id
WHERE o.org_type = 'COLLEGE'
ORDER BY u.user_id;
"

echo -e "\n${YELLOW}测试账号信息:${NC}"
echo -e "用户名: makesi, gongxue, jisuanji, shangxue, wenli, yishu, hangkong, guojijiaoyu"
echo -e "密码: 123456"
echo -e "\n${GREEN}请访问 https://blackevil.cn/login 测试登录${NC}"
