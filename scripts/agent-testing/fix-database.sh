#!/bin/bash

# 数据库快速修复脚本

echo "=================================="
echo "SISM 数据库初始化脚本"
echo "=================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 检查是否在正确的目录
if [ ! -f "../../pom.xml" ]; then
    echo -e "${RED}❌ 请在 sism-backend/scripts/agent-testing 目录下运行此脚本${NC}"
    exit 1
fi

echo "请选择数据库配置方式："
echo ""
echo "1) 使用远程数据库 (175.24.139.148:8386)"
echo "2) 使用本地数据库 (localhost:5432)"
echo "3) 跳过，手动处理"
echo ""
read -p "请输入选择 (1-3): " choice

case $choice in
    1)
        echo -e "${YELLOW}使用远程数据库...${NC}"

        # 检查数据库连接
        echo "测试数据库连接..."
        if PGPASSWORD="64378561huaW" psql -h 175.24.139.148 -p 8386 -U postgres -d strategic -c "SELECT 1;" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ 数据库连接成功${NC}"
        else
            echo -e "${RED}❌ 数据库连接失败${NC}"
            echo "请检查："
            echo "  - 数据库地址: 175.24.139.148:8386"
            echo "  - 数据库名: strategic"
            echo "  - 用户名: postgres"
            echo "  - 密码: 64378561huaW"
            exit 1
        fi

        # 检查表是否存在
        echo "检查表结构..."
        TABLE_COUNT=$(PGPASSWORD="64378561huaW" psql -h 175.24.139.148 -p 8386 -U postgres -d strategic -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null | xargs)

        if [ "$TABLE_COUNT" -gt 0 ]; then
            echo -e "${GREEN}✅ 数据库已包含 $TABLE_COUNT 个表${NC}"
            echo "是否要重新初始化数据库？这将删除所有数据！"
            read -p "确认重新初始化？(yes/no): " confirm
            if [ "$confirm" != "yes" ]; then
                echo "跳过数据库初始化"
                exit 0
            fi
        fi

        # 执行Flyway迁移
        echo "执行数据库迁移..."
        cd ../../sism-main

        # 设置Flyway环境变量
        export FLYWAY_URL=jdbc:postgresql://175.24.139.148:8386/strategic
        export FLYWAY_USER=postgres
        export FLYWAY_PASSWORD=64378561huaW

        mvn flyway:migrate

        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ 数据库迁移完成${NC}"

            # 加载测试数据（如果存在）
            if [ -f "../../database/seeds/seed-data.sql" ]; then
                echo "加载测试数据..."
                PGPASSWORD="64378561huaW" psql -h 175.24.139.148 -p 8386 -U postgres -d strategic -f ../../database/seeds/seed-data.sql
                echo -e "${GREEN}✅ 测试数据加载完成${NC}"
            fi
        else
            echo -e "${RED}❌ 数据库迁移失败${NC}"
            exit 1
        fi

        ;;

    2)
        echo -e "${YELLOW}使用本地数据库...${NC}"

        # 检查PostgreSQL是否运行
        if ! command -v psql &> /dev/null; then
            echo -e "${RED}❌ PostgreSQL 未安装${NC}"
            echo "请先安装PostgreSQL: brew install postgresql"
            exit 1
        fi

        # 检查服务是否运行
        if ! pg_isready > /dev/null 2>&1; then
            echo -e "${RED}❌ PostgreSQL 服务未运行${NC}"
            echo "请启动PostgreSQL服务: brew services start postgresql"
            exit 1
        fi

        echo "请输入本地PostgreSQL配置:"
        read -p "数据库名 (默认: sism_dev): " dbname
        dbname=${dbname:-sism_dev}

        read -p "用户名 (默认: $USER): " dbuser
        dbuser=${dbuser:-$USER}

        # 创建数据库
        echo "创建数据库 $dbname..."
        createdb -U $dbuser $dbname 2>/dev/null || echo "数据库可能已存在"

        # 更新.env文件
        echo "更新.env配置..."
        cd ../..
        sed -i.bak "s|DB_URL=jdbc:postgresql://.*|DB_URL=jdbc:postgresql://localhost:5432/$dbname|" .env
        sed -i.bak "s|DB_USERNAME=.*|DB_USERNAME=$dbuser|" .env
        sed -i.bak "s|DB_PASSWORD=.*|DB_PASSWORD=|" .env
        sed -i.bak "s|FLYWAY_ENABLED=false|FLYWAY_ENABLED=true|" .env

        echo -e "${GREEN}✅ .env配置已更新${NC}"

        # 执行迁移
        cd sism-main
        mvn flyway:migrate

        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ 数据库迁移完成${NC}"

            # 加载测试数据
            if [ -f "../database/seeds/seed-data.sql" ]; then
                echo "加载测试数据..."
                psql -U $dbuser -d $dbname -f ../database/seeds/seed-data.sql
                echo -e "${GREEN}✅ 测试数据加载完成${NC}"
            fi

            # 重启后端服务
            echo "重启后端服务..."
            lsof -ti:8080 | xargs kill -9 2>/dev/null
            sleep 2
            mvn spring-boot:run -Dspring-boot.run.profiles=dev > /tmp/sism-backend.log 2>&1 &
            echo "后端正在启动，请等待20秒..."
            sleep 20
            echo -e "${GREEN}✅ 后端已重启${NC}"
        fi

        ;;

    3)
        echo "跳过数据库初始化"
        echo ""
        echo "请手动执行以下步骤："
        echo "1. 确保数据库已创建"
        echo "2. 运行Flyway迁移: mvn flyway:migrate"
        echo "3. 加载测试数据（如有）"
        echo "4. 重启后端服务"
        ;;

    *)
        echo -e "${RED}❌ 无效选择${NC}"
        exit 1
        ;;
esac

echo ""
echo "=================================="
echo -e "${GREEN}✅ 数据库准备完成！${NC}"
echo "=================================="
echo ""
echo "下一步："
echo "  1. 测试登录:"
echo "     curl -X POST http://localhost:8080/api/v1/auth/login \\"
echo "       -H 'Content-Type: application/json' \\"
echo "       -d '{\"username\":\"admin\",\"password\":\"admin123\"}'"
echo ""
echo "  2. 运行测试:"
echo "     ./run-tests.sh --all"
echo ""
