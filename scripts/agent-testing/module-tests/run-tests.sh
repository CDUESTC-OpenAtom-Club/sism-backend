#!/bin/bash

# SISM模块测试启动脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

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

# 显示帮助信息
show_help() {
    cat << EOF
SISM模块API测试工具

用法: ./run-tests.sh [选项]

选项:
    -h, --help          显示此帮助信息

模块测试:
    -a, --all           运行所有模块测试
    -i, --iam           IAM认证授权模块
    -s, --strategy      战略管理模块
    -e, --execution     执行管理模块
    -w, --workflow      工作流模块
    -o, --organization  组织管理模块
    -y, --analytics     数据分析模块
    -t, --task          任务管理模块
    -l, --alert         告警管理模块

完整流程:
    -f, --full          运行完整业务流程测试

示例:
    ./run-tests.sh --all                   # 运行所有模块
    ./run-tests.sh --iam                  # 只测试IAM模块
    ./run-tests.sh --full                 # 运行完整业务流程

模块说明:
    IAM          - 认证授权，用户登录、Token管理
    Strategy     - 战略管理，指标创建、下发
    Execution    - 执行管理，填报创建、提交
    Workflow     - 工作流，多级审批、驳回
    Organization - 组织管理，组织树、成员
    Analytics    - 数据分析，仪表盘、进度
    Task         - 任务管理，创建、分配
    Alert        - 告警管理，告警查询、处理

EOF
}

# 检查依赖
check_dependencies() {
    if ! command -v node &> /dev/null; then
        print_error "Node.js 未安装"
        exit 1
    fi

    if ! command -v npm &> /dev/null; then
        print_error "npm 未安装"
        exit 1
    fi

    print_success "Node.js $(node -v) and npm $(npm -v)"
}

# 安装依赖
install_dependencies() {
    if [ ! -d "node_modules" ]; then
        print_info "安装依赖..."
        npm install
        print_success "依赖安装完成"
    fi
}

# 运行单个模块测试
run_module_test() {
    local module=$1
    local test_file=$2

    print_info "运行 $module 模块测试..."
    echo ""

    if node "$test_file"; then
        print_success "$module 模块测试通过"
        return 0
    else
        print_error "$module 模块测试失败"
        return 1
    fi
}

# 主函数
main() {
    echo "=================================="
    echo "SISM模块API测试工具"
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
            --install)
                check_dependencies
                install_dependencies
                exit 0
                ;;
            -a|--all)
                check_dependencies
                install_dependencies

                print_info "运行所有模块测试..."
                echo ""

                npm run test:all
                exit $?
                ;;
            -i|--iam)
                check_dependencies
                run_module_test "IAM" "iam/test-auth.js"
                exit $?
                ;;
            -s|--strategy)
                check_dependencies
                run_module_test "Strategy" "strategy/test-indicator.js"
                exit $?
                ;;
            -e|--execution)
                check_dependencies
                run_module_test "Execution" "execution/test-report.js"
                exit $?
                ;;
            -w|--workflow)
                check_dependencies
                run_module_test "Workflow" "workflow/test-workflow.js"
                exit $?
                ;;
            -o|--organization)
                check_dependencies
                run_module_test "Organization" "organization/test-organization.js"
                exit $?
                ;;
            -y|--analytics)
                check_dependencies
                run_module_test "Analytics" "analytics/test-analytics.js"
                exit $?
                ;;
            -t|--task)
                check_dependencies
                run_module_test "Task" "task/test-task.js"
                exit $?
                ;;
            -l|--alert)
                check_dependencies
                run_module_test "Alert" "alert/test-alert.js"
                exit $?
                ;;
            -f|--full)
                check_dependencies
                print_info "运行完整业务流程测试..."
                echo ""
                node run-full-workflow.js
                exit $?
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

main "$@"
