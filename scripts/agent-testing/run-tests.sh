#!/bin/bash

# SISM 多Agent API测试框架启动脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 检查Node.js是否安装
check_nodejs() {
    if ! command -v node &> /dev/null; then
        print_error "Node.js 未安装，请先安装 Node.js 14+"
        exit 1
    fi

    local node_version=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
    if [ "$node_version" -lt 14 ]; then
        print_error "Node.js 版本过低，需要 14+，当前版本: $(node -v)"
        exit 1
    fi

    print_success "Node.js 版本检查通过: $(node -v)"
}

# 检查依赖是否安装
check_dependencies() {
    if [ ! -d "node_modules" ]; then
        print_info "依赖未安装，正在安装..."
        npm install
        print_success "依赖安装完成"
    else
        print_success "依赖已安装"
    fi
}

# 检查后端服务是否运行
check_backend() {
    local base_url=$(grep -o '"baseUrl"[^,]*' config/test-users.json | grep -o 'http://[^"]*')
    local host=$(echo $base_url | sed -n 's|http://\(.*\):.*|\1|p')
    local port=$(echo $base_url | sed -n 's|.*:\([0-9]*\).*|\1|p')

    if command -v curl &> /dev/null; then
        if curl -s -o /dev/null -w "%{http_code}" "$base_url/health" 2>/dev/null | grep -q "200\|404"; then
            print_success "后端服务运行正常"
        else
            print_warning "后端服务可能未启动，或健康检查端点不存在"
            print_info "请确保后端服务在 $base_url 运行"
        fi
    else
        print_warning "curl 未安装，跳过后端服务检查"
    fi
}

# 显示帮助信息
show_help() {
    cat << EOF
SISM 多Agent API测试框架

用法: ./run-tests.sh [选项]

选项:
    -h, --help          显示此帮助信息
    -a, --all           运行所有测试（顺序执行）
    -p, --parallel      并行执行所有测试
    -w, --workflow ID   运行指定的工作流测试
    -i, --iam           只运行IAM认证Agent
    -s, --strategy      只运行战略部门Agent
    -f, --functional    只运行职能部门Agent
    -c, --college       只运行学院Agent
    -w, --workflow      只运行工作流Agent
    -r, --report        打开最新的测试报告

工作流ID:
    indicator-distribution    指标创建与下发流程
    indicator-split           指标拆分流程
    report-submission         学院填报流程
    multi-level-approval      多级审批流程
    approval-rejection        审批驳回流程
    approval-timeline         审批时间轴测试
    report-withdrawal         填报撤回测试
    parent-child-relationship 父子指标关系测试
    period-identification     周期标识测试
    data-change-tracking      数据变更追踪测试

示例:
    ./run-tests.sh --all                  # 运行所有测试
    ./run-tests.sh --parallel             # 并行执行
    ./run-tests.sh --workflow report-submission  # 测试填报流程
    ./run-tests.sh --iam                  # 只测试认证
    ./run-tests.sh --report               # 查看测试报告

EOF
}

# 打开最新报告
open_report() {
    local latest_report=$(ls -t reports/*.html 2>/dev/null | head -n 1)

    if [ -z "$latest_report" ]; then
        print_error "没有找到测试报告"
        exit 1
    fi

    print_info "打开报告: $latest_report"

    if [[ "$OSTYPE" == "darwin"* ]]; then
        open "$latest_report"
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        xdg-open "$latest_report"
    else
        print_warning "无法自动打开报告，请手动打开: $latest_report"
    fi
}

# 主函数
main() {
    echo "=================================="
    echo "SISM 多Agent API测试框架"
    echo "=================================="
    echo ""

    # 解析命令行参数
    if [ $# -eq 0 ]; then
        show_help
        exit 0
    fi

    while [ $# -gt 0 ]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -a|--all)
                check_nodejs
                check_dependencies
                check_backend
                print_info "开始执行所有测试..."
                node agents/master-agent.js
                exit $?
                ;;
            -p|--parallel)
                check_nodejs
                check_dependencies
                check_backend
                print_info "开始并行执行所有测试..."
                node agents/master-agent.js --parallel
                exit $?
                ;;
            -w|--workflow)
                shift
                check_nodejs
                check_dependencies
                check_backend
                print_info "执行工作流测试: $1"
                node agents/master-agent.js --workflow "$1"
                exit $?
                ;;
            -i|--iam)
                check_nodejs
                check_dependencies
                check_backend
                node agents/iam-agent.js
                exit $?
                ;;
            -s|--strategy)
                check_nodejs
                check_dependencies
                check_backend
                node agents/strategy-agent.js
                exit $?
                ;;
            -f|--functional)
                check_nodejs
                check_dependencies
                check_backend
                node agents/functional-agent.js
                exit $?
                ;;
            -c|--college)
                check_nodejs
                check_dependencies
                check_backend
                node agents/college-agent.js
                exit $?
                ;;
            --workflow-agent)
                check_nodejs
                check_dependencies
                check_backend
                node agents/workflow-agent.js
                exit $?
                ;;
            -r|--report)
                open_report
                exit 0
                ;;
            *)
                print_error "未知选项: $1"
                show_help
                exit 1
                ;;
        esac
        shift
    done
}

# 执行主函数
main "$@"
