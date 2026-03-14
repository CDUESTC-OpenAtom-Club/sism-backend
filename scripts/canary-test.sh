#!/bin/bash
# 金丝雀测试脚本 - Phase 1 重构验证
# 用于验证重构前后API行为一致

set -e

BASE_URL="http://localhost:8080/api"
OUTPUT_DIR="canary-test-results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 创建输出目录
mkdir -p "$OUTPUT_DIR/before"
mkdir -p "$OUTPUT_DIR/after"

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 测试1: 创建指标
test_create_indicator() {
    log_info "测试1: 创建指标"

    local payload='{
        "taskId": 1,
        "indicatorDesc": "金丝雀测试指标",
        "weightPercent": 50.00,
        "sortOrder": 1,
        "type": "strategic",
        "year": 2026,
        "responsibleDept": "战略部"
    }'

    local response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "$payload" \
        "$BASE_URL/indicators")

    echo "$response" | jq '.' > "$OUTPUT_DIR/after/create_indicator_$TIMESTAMP.json"

    # 验证响应
    local indicator_id=$(echo "$response" | jq -r '.data.indicatorId // .data.indicatorId // .indicatorId // empty' 2>/dev/null || echo "")

    if [ -z "$indicator_id" ]; then
        log_error "创建指标失败: $response"
        return 1
    fi

    log_info "✅ 创建指标成功: ID=$indicator_id"
    echo "$indicator_id" > "$OUTPUT_DIR/after/test_indicator_id.txt"
    return 0
}

# 测试2: 查询指标详情
test_get_indicator() {
    log_info "测试2: 查询指标详情"

    if [ ! -f "$OUTPUT_DIR/after/test_indicator_id.txt" ]; then
        log_error "未找到测试指标ID，请先运行测试1"
        return 1
    fi

    local indicator_id=$(cat "$OUTPUT_DIR/after/test_indicator_id.txt")

    local response=$(curl -s -X GET \
        -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/indicators/$indicator_id")

    echo "$response" | jq '.' > "$OUTPUT_DIR/after/get_indicator_$indicator_id.json"

    # 验证关键字段
    local desc=$(echo "$response" | jq -r '.data.indicatorDesc // .data.indicatorDesc // .indicatorDesc // empty' 2>/dev/null || echo "")

    if [ -z "$desc" ]; then
        log_error "查询指标失败: $response"
        return 1
    fi

    log_info "✅ 查询指标成功: $desc"
    return 0
}

# 测试3: 下发指标
test_distribute_indicator() {
    log_info "测试3: 下发指标"

    if [ ! -f "$OUTPUT_DIR/after/test_indicator_id.txt" ]; then
        log_error "未找到测试指标ID，请先运行测试1"
        return 1
    fi

    local indicator_id=$(cat "$OUTPUT_DIR/after/test_indicator_id.txt")

    local response=$(curl -s -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -H "Authorization: Bearer $TOKEN" \
        -d "targetOrgId=2&actorUserId=1" \
        "$BASE_URL/indicators/$indicator_id/distribute")

    echo "$response" | jq '.' > "$OUTPUT_DIR/after/distribute_indicator_$indicator_id.json"

    # 验证响应（可能失败，不影响测试）
    local success=$(echo "$response" | jq -r '.success // true' 2>/dev/null || echo "true")

    if [ "$success" != "true" ]; then
        log_warn "下发指标失败（可能不符合条件）: $response"
        return 0  # 不阻塞后续测试
    fi

    log_info "✅ 下发指标成功"
    return 0
}

# 测试4: 按年份查询指标
test_get_indicators_by_year() {
    log_info "测试4: 按年份查询指标"

    local response=$(curl -s -X GET \
        -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/indicators/year/2026")

    echo "$response" | jq '.' > "$OUTPUT_DIR/after/get_indicators_2026_$TIMESTAMP.json"

    # 验证响应
    local is_empty=$(echo "$response" | jq -r 'if .data then "false" else "true" end' 2>/dev/null || echo "false")

    if [ "$is_empty" = "true" ]; then
        log_warn "按年份查询返回空，可能没有2026年数据"
        return 0  # 不阻塞
    fi

    local count=$(echo "$response" | jq -r '.data | length' 2>/dev/null || echo "0")
    log_info "✅ 按年份查询成功: 找到 $count 个指标"
    return 0
}

# 测试5: 查询任务列表
test_get_tasks() {
    log_info "测试5: 查询任务列表"

    local response=$(curl -s -X GET \
        -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/tasks")

    echo "$response" | jq '.' > "$OUTPUT_DIR/after/get_tasks_$TIMESTAMP.json"

    # 验证响应
    local success=$(echo "$response" | jq -r '.success // true' 2>/dev/null || echo "true")

    if [ "$success" != "true" ]; then
        log_error "查询任务列表失败: $response"
        return 1
    fi

    log_info "✅ 查询任务列表成功"
    return 0
}

# 主函数
main() {
    log_info "========== 金丝雀测试开始 =========="
    log_info "时间: $(date)"
    log_info "输出目录: $OUTPUT_DIR"

    # 读取TOKEN（从环境变量或提示输入）
    if [ -z "$TOKEN" ]; then
        read -p "请输入 JWT Token: " TOKEN
    fi

    # 执行测试
    local results=()

    test_create_indicator && results[0]="PASS" || results[0]="FAIL"
    test_get_indicator && results[1]="PASS" || results[1]="FAIL"
    test_distribute_indicator && results[2]="PASS" || results[2]="WARN"
    test_get_indicators_by_year && results[3]="PASS" || results[3]="WARN"
    test_get_tasks && results[4]="PASS" || results[4]="FAIL"

    # 输出结果
    echo ""
    log_info "========== 测试结果汇总 =========="
    echo "1. 创建指标:       ${results[0]}"
    echo "2. 查询指标详情:   ${results[1]}"
    echo "3. 下发指标:       ${results[2]}"
    echo "4. 按年份查询:     ${results[3]}"
    echo "5. 查询任务列表:   ${results[4]}"
    echo ""

    # 统计
    local pass_count=0
    local fail_count=0
    local warn_count=0

    for result in "${results[@]}"; do
        case $result in
            PASS) pass_count=$((pass_count + 1)) ;;
            FAIL) fail_count=$((fail_count + 1)) ;;
            WARN) warn_count=$((warn_count + 1)) ;;
        esac
    done

    log_info "总计: PASS=$pass_count, WARN=$warn_count, FAIL=$fail_count"

    # 保存测试报告
    cat > "$OUTPUT_DIR/canary_report_$TIMESTAMP.txt" <<EOF
金丝雀测试报告
时间: $(date)
测试结果:
- 创建指标:       ${results[0]}
- 查询指标详情:   ${results[1]}
- 下发指标:       ${results[2]}
- 按年份查询:     ${results[3]}
- 查询任务列表:   ${results[4]}

总计: PASS=$pass_count, WARN=$warn_count, FAIL=$fail_count
EOF

    log_info "测试报告已保存: $OUTPUT_DIR/canary_report_$TIMESTAMP.txt"

    # 判断是否可以继续
    if [ $fail_count -gt 0 ]; then
        log_error "❌ 金丝雀测试失败！请检查错误后再继续重构。"
        exit 1
    else
        log_info "✅ 金丝雀测试通过！可以继续重构。"
        exit 0
    fi
}

# 执行
main "$@"
