#!/bin/bash

# 测试两级主管审批流程
# 使用方式: ./scripts/test-approval-workflow.sh

set -e

BASE_URL="http://localhost:8080/api"
PLAN_ID=9001

echo "=========================================="
echo "两级主管审批流程测试"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 步骤 1: 获取 JWT Token
echo -e "${YELLOW}步骤 1: 获取测试用户 Token${NC}"
echo "----------------------------------------"

# 填报人 (假设使用 admin 用户作为填报人)
echo "获取填报人 Token (admin)..."
REPORTER_TOKEN=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.accessToken')

if [ "$REPORTER_TOKEN" == "null" ] || [ -z "$REPORTER_TOKEN" ]; then
  echo -e "${RED}❌ 获取填报人 Token 失败${NC}"
  exit 1
fi
echo -e "${GREEN}✓ 填报人 Token: ${REPORTER_TOKEN:0:20}...${NC}"

# 一级主管 (假设使用 zhangsan 作为一级主管)
echo "获取一级主管 Token (zhangsan)..."
LEVEL1_TOKEN=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","password":"password123"}' | jq -r '.data.accessToken')

if [ "$LEVEL1_TOKEN" == "null" ] || [ -z "$LEVEL1_TOKEN" ]; then
  echo -e "${RED}❌ 获取一级主管 Token 失败${NC}"
  exit 1
fi
echo -e "${GREEN}✓ 一级主管 Token: ${LEVEL1_TOKEN:0:20}...${NC}"

# 二级主管 (假设使用 lisi 作为二级主管)
echo "获取二级主管 Token (lisi)..."
LEVEL2_TOKEN=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"lisi","password":"password123"}' | jq -r '.data.accessToken')

if [ "$LEVEL2_TOKEN" == "null" ] || [ -z "$LEVEL2_TOKEN" ]; then
  echo -e "${RED}❌ 获取二级主管 Token 失败${NC}"
  exit 1
fi
echo -e "${GREEN}✓ 二级主管 Token: ${LEVEL2_TOKEN:0:20}...${NC}"

echo ""

# 步骤 2: 提交计划
echo -e "${YELLOW}步骤 2: 填报人提交计划${NC}"
echo "----------------------------------------"
echo "提交计划 ID: ${PLAN_ID}"

SUBMIT_RESPONSE=$(curl -s -X POST "${BASE_URL}/plans/approval/${PLAN_ID}/submit?userId=1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${REPORTER_TOKEN}")

echo "响应: ${SUBMIT_RESPONSE}"

INSTANCE_ID=$(echo $SUBMIT_RESPONSE | jq -r '.data.id // empty')
if [ -z "$INSTANCE_ID" ]; then
  echo -e "${RED}❌ 提交计划失败${NC}"
  echo "响应: ${SUBMIT_RESPONSE}"
  exit 1
fi

echo -e "${GREEN}✓ 计划提交成功，审批实例 ID: ${INSTANCE_ID}${NC}"
echo ""

# 等待 1 秒
sleep 1

# 步骤 3: 一级主管审批
echo -e "${YELLOW}步骤 3: 一级主管审批${NC}"
echo "----------------------------------------"
echo "审批实例 ID: ${INSTANCE_ID}"

APPROVE1_RESPONSE=$(curl -s -X POST "${BASE_URL}/plans/approval/instances/${INSTANCE_ID}/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${LEVEL1_TOKEN}" \
  -d '{"approverId":2,"comment":"一级主管同意，请二级主管审核"}')

echo "响应: ${APPROVE1_RESPONSE}"

APPROVE1_SUCCESS=$(echo $APPROVE1_RESPONSE | jq -r '.code')
if [ "$APPROVE1_SUCCESS" != "200" ]; then
  echo -e "${RED}❌ 一级主管审批失败${NC}"
  exit 1
fi

echo -e "${GREEN}✓ 一级主管审批成功${NC}"
echo ""

# 等待 1 秒
sleep 1

# 步骤 4: 二级主管审批
echo -e "${YELLOW}步骤 4: 二级主管审批${NC}"
echo "----------------------------------------"
echo "审批实例 ID: ${INSTANCE_ID}"

APPROVE2_RESPONSE=$(curl -s -X POST "${BASE_URL}/plans/approval/instances/${INSTANCE_ID}/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${LEVEL2_TOKEN}" \
  -d '{"approverId":3,"comment":"二级主管最终批准"}')

echo "响应: ${APPROVE2_RESPONSE}"

APPROVE2_SUCCESS=$(echo $APPROVE2_RESPONSE | jq -r '.code')
if [ "$APPROVE2_SUCCESS" != "200" ]; then
  echo -e "${RED}❌ 二级主管审批失败${NC}"
  exit 1
fi

echo -e "${GREEN}✓ 二级主管审批成功${NC}"
echo ""

# 步骤 5: 验证最终状态
echo -e "${YELLOW}步骤 5: 验证审批流程完成${NC}"
echo "----------------------------------------"

STATUS_RESPONSE=$(curl -s -X GET "${BASE_URL}/plans/approval/plans/${PLAN_ID}/status" \
  -H "Authorization: Bearer ${REPORTER_TOKEN}")

echo "审批状态: ${STATUS_RESPONSE}"

FINAL_STATUS=$(echo $STATUS_RESPONSE | jq -r '.data.status')
if [ "$FINAL_STATUS" == "APPROVED" ]; then
  echo -e "${GREEN}✓ 审批流程已完成，状态: APPROVED${NC}"
else
  echo -e "${YELLOW}⚠ 当前状态: ${FINAL_STATUS}${NC}"
fi

echo ""
echo "=========================================="
echo -e "${GREEN}测试完成！${NC}"
echo "=========================================="
