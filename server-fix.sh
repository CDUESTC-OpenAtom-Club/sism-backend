#!/bin/bash
# 服务器端快速修复脚本
# 用途: 在生产服务器上执行，修复 keyan 用户登录和重启服务
# 日期: 2026-01-28

set -e

echo "========================================="
echo "SISM 生产环境快速修复脚本"
echo "========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查是否在服务器上运行
if [ ! -f "/opt/sism/backend/sism-backend.jar" ]; then
    echo -e "${RED}错误: 未找到 /opt/sism/backend/sism-backend.jar${NC}"
    echo "此脚本必须在生产服务器上运行"
    exit 1
fi

# 步骤 1: 修复数据库
echo -e "${YELLOW}步骤 1: 修复数据库用户密码${NC}"
echo "-----------------------------------------"

# 创建临时 SQL 文件
cat > /tmp/fix-users.sql << 'EOF'
-- 修复测试用户密码
-- 密码: 123456
-- 哈希: $2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi

UPDATE app_user 
SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi',
    is_active = TRUE,
    updated_at = CURRENT_TIMESTAMP
WHERE username IN ('zhanlue', 'jiaowu', 'keyan', 'renshi', 'xuesheng', 'admin');

-- 验证修复
SELECT username, real_name, is_active, 
       CASE 
           WHEN password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi' 
           THEN '✓ 正确' 
           ELSE '✗ 错误' 
       END AS password_status
FROM app_user
WHERE username IN ('zhanlue', 'jiaowu', 'keyan', 'renshi', 'xuesheng', 'admin')
ORDER BY username;
EOF

# 执行 SQL（需要数据库密码）
echo "正在连接数据库..."
if [ -z "$DB_PASSWORD" ]; then
    echo -e "${YELLOW}请输入数据库密码:${NC}"
    read -s DB_PASSWORD
fi

PGPASSWORD=$DB_PASSWORD psql -h localhost -U sism_user -d sism_db -f /tmp/fix-users.sql

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ 数据库修复成功${NC}"
else
    echo -e "${RED}✗ 数据库修复失败${NC}"
    exit 1
fi

# 清理临时文件
rm -f /tmp/fix-users.sql

# 步骤 2: 重启后端服务
echo ""
echo -e "${YELLOW}步骤 2: 重启后端服务${NC}"
echo "-----------------------------------------"

echo "停止服务..."
sudo systemctl stop sism-backend

echo "等待 3 秒..."
sleep 3

echo "启动服务..."
sudo systemctl start sism-backend

echo "等待服务启动 (15秒)..."
sleep 15

# 步骤 3: 健康检查
echo ""
echo -e "${YELLOW}步骤 3: 健康检查${NC}"
echo "-----------------------------------------"

for i in 1 2 3; do
    echo "尝试 $i/3..."
    if curl -sf http://localhost:8080/api/actuator/health > /dev/null; then
        echo -e "${GREEN}✓ 服务健康检查通过${NC}"
        
        # 测试 cycles API
        echo ""
        echo "测试 Cycles API..."
        # 先登录获取 token
        TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
            -H "Content-Type: application/json" \
            -d '{"username":"zhanlue","password":"123456"}' | \
            grep -o '"token":"[^"]*' | cut -d'"' -f4)
        
        if [ -n "$TOKEN" ]; then
            CYCLES=$(curl -s -H "Authorization: Bearer $TOKEN" \
                http://localhost:8080/api/cycles)
            
            if echo "$CYCLES" | grep -q '"code":0'; then
                echo -e "${GREEN}✓ Cycles API 正常工作${NC}"
            else
                echo -e "${RED}✗ Cycles API 返回错误${NC}"
                echo "$CYCLES"
            fi
        fi
        
        # 测试 keyan 登录
        echo ""
        echo "测试 keyan 用户登录..."
        KEYAN_RESULT=$(curl -s -X POST http://localhost:8080/api/auth/login \
            -H "Content-Type: application/json" \
            -d '{"username":"keyan","password":"123456"}')
        
        if echo "$KEYAN_RESULT" | grep -q '"code":0'; then
            echo -e "${GREEN}✓ keyan 用户登录成功${NC}"
        else
            echo -e "${RED}✗ keyan 用户登录失败${NC}"
            echo "$KEYAN_RESULT"
        fi
        
        echo ""
        echo -e "${GREEN}=========================================${NC}"
        echo -e "${GREEN}修复完成！${NC}"
        echo -e "${GREEN}=========================================${NC}"
        exit 0
    fi
    
    if [ $i -lt 3 ]; then
        echo "等待 5 秒后重试..."
        sleep 5
    fi
done

echo -e "${RED}✗ 健康检查失败${NC}"
echo ""
echo "查看服务日志:"
sudo journalctl -u sism-backend -n 50 --no-pager

exit 1
