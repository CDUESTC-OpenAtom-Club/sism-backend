#!/bin/bash

# ===================================================================
# 撤回功能接口测试脚本
# ===================================================================

BASE_URL="http://localhost:8080/api/v1"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log() {
    echo -e "${NC}[$(date '+%H:%M:%S')]${NC} $1"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# ===================================================================
# 步骤1: 登录获取Token
# ===================================================================
log "步骤1: 登录获取Token..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

TOKEN=$(echo "$LOGIN_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('accessToken',''))" 2>/dev/null)

if [ -z "$TOKEN" ]; then
    log_error "登录失败，无法获取Token"
    echo "$LOGIN_RESPONSE"
    exit 1
fi

log_success "登录成功"
echo "Token: ${TOKEN:0:50}..."
echo ""

# ===================================================================
# 步骤2: 创建测试Plan
# ===================================================================
log "步骤2: 创建测试Plan..."
PLAN_RESPONSE=$(curl -s -X POST "$BASE_URL/plans" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "planName": "撤回测试Plan-'$(date +%s)'",
    "planType": "STRAT_TO_FUNC",
    "targetOrgId": 37,
    "year": 2026,
    "cycleId": 4
  }')

PLAN_ID=$(echo "$PLAN_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('id',''))" 2>/dev/null)

if [ -z "$PLAN_ID" ]; then
    log_error "创建Plan失败"
    echo "$PLAN_RESPONSE"
    exit 1
fi

log_success "Plan创建成功，ID: $PLAN_ID"
echo ""

# ===================================================================
# 步骤3: 启动审批流程
# ===================================================================
log "步骤3: 启动审批流程..."
WORKFLOW_RESPONSE=$(curl -s -X POST "$BASE_URL/workflows/start" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"workflowCode\": \"PLAN_DISPATCH_STRATEGY\",
    \"businessEntityId\": $PLAN_ID,
    \"businessEntityType\": \"PLAN\"
  }")

INSTANCE_ID=$(echo "$WORKFLOW_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('instanceId',''))" 2>/dev/null)

if [ -z "$INSTANCE_ID" ]; then
    log_error "启动审批流程失败"
    echo "$WORKFLOW_RESPONSE"
    exit 1
fi

log_success "审批流程启动成功，实例ID: $INSTANCE_ID"
echo ""

# ===================================================================
# 步骤4: 检查实例状态
# ===================================================================
log "步骤4: 检查实例状态（确认可以撤回）..."
INSTANCE_DETAIL=$(curl -s -X GET "$BASE_URL/workflows/instances/$INSTANCE_ID" \
  -H "Authorization: Bearer $TOKEN")

echo "$INSTANCE_DETAIL" | python3 -c "
import sys, json
d = json.load(sys.stdin)
data = d.get('data', {})
print(f\"  实例状态: {data.get('status')}\")
print(f\"  发起人ID: {data.get('starterId')}\")
print(f\"  当前节点: {data.get('currentStepName')}\")
print(f\"  当前审批人: {data.get('currentApproverName')}\")
print(f\"  是否可撤回: {data.get('canWithdraw')}\")
print(f\"  \n  任务列表:\")
for task in data.get('tasks', []):
    status_icon = '✓' if task.get('status') == 'COMPLETED' else '○'
    print(f\"    {status_icon} {task.get('taskName')}: {task.get('status')} (操作人: {task.get('assigneeName')})\")
"

CAN_WITHDRAW=$(echo "$INSTANCE_DETAIL" | python3 -c "import sys,json; d=json.load(sys.stdin); print(str(d.get('data',{}).get('canWithdraw')).lower())" 2>/dev/null)

if [ "$CAN_WITHDRAW" != "true" ]; then
    log_error "实例状态显示不可撤回，测试终止"
    exit 1
fi

log_success "实例状态确认: 可以撤回"
echo ""

# ===================================================================
# 步骤5: 执行撤回操作
# ===================================================================
log "步骤5: 执行撤回操作..."
CANCEL_RESPONSE=$(curl -s -X POST "$BASE_URL/workflows/$INSTANCE_ID/cancel" \
  -H "Authorization: Bearer $TOKEN")

CANCEL_SUCCESS=$(echo "$CANCEL_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(str(d.get('success',False)))" 2>/dev/null)

if [ "$CANCEL_SUCCESS" != "True" ]; then
    log_error "撤回操作失败"
    echo "$CANCEL_RESPONSE"
    exit 1
fi

log_success "撤回操作成功"
echo ""

# ===================================================================
# 步骤6: 验证撤回后的状态
# ===================================================================
log "步骤6: 验证撤回后的状态..."
INSTANCE_AFTER=$(curl -s -X GET "$BASE_URL/workflows/instances/$INSTANCE_ID" \
  -H "Authorization: Bearer $TOKEN")

INSTANCE_STATUS=$(echo "$INSTANCE_AFTER" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('status',''))" 2>/dev/null)
CAN_WITHDRAW_AFTER=$(echo "$INSTANCE_AFTER" | python3 -c "import sys,json; d=json.load(sys.stdin); print(str(d.get('data',{}).get('canWithdraw',False)))" 2>/dev/null)

echo "  撤回后实例状态: $INSTANCE_STATUS"
echo "  撤回后可撤回: $CAN_WITHDRAW_AFTER"

if [ "$INSTANCE_STATUS" == "REJECTED" ] || [ "$INSTANCE_STATUS" == "WITHDRAWN" ] || [ "$INSTANCE_STATUS" == "CANCELLED" ]; then
    log_success "实例状态正确变更为: $INSTANCE_STATUS"
else
    log_warning "实例状态为: $INSTANCE_STATUS (预期: REJECTED/WITHDRAWN/CANCELLED)"
fi

if [ "$CAN_WITHDRAW_AFTER" == "False" ] || [ "$CAN_WITHDRAW_AFTER" == "false" ]; then
    log_success "撤回后不可再次撤回（正确）"
else
    log_warning "撤回后仍然可以撤回（异常）"
fi
echo ""

# ===================================================================
# 步骤7: 验证Plan状态
# ===================================================================
log "步骤7: 验证Plan可以继续编辑..."
PLAN_AFTER=$(curl -s -X GET "$BASE_URL/plans/$PLAN_ID" \
  -H "Authorization: Bearer $TOKEN")

echo "$PLAN_AFTER" | python3 -c "
import sys, json
d = json.load(sys.stdin)
data = d.get('data', {})
print(f\"  Plan状态: {data.get('status')}\")
print(f\"  可编辑: {data.get('canEdit')}\")
print(f\"  可重新提交: {data.get('canResubmit')}\")
"

echo ""
log_success "========== 测试完成 =========="
echo ""
echo "测试数据:"
echo "  Plan ID: $PLAN_ID"
echo "  实例 ID: $INSTANCE_ID"
echo ""
echo "清理命令:"
echo "  删除Plan: curl -X DELETE $BASE_URL/plans/$PLAN_ID -H \"Authorization: Bearer \$TOKEN\""
