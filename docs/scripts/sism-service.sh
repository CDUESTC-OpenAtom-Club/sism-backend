#!/bin/bash
# ============================================
# SISM 服务管理脚本
# ============================================
# 用途: 统一管理 SISM 相关服务
# 使用: ./sism-service.sh [start|stop|restart|status]
# ============================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# 服务名称
BACKEND_SERVICE="sism-backend"
NGINX_SERVICE="nginx"
POSTGRES_SERVICE="postgresql"

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_header() {
    echo -e "${CYAN}============================================${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}============================================${NC}"
}

# 检查服务状态
check_service_status() {
    local service=$1
    
    if systemctl is-active --quiet "$service" 2>/dev/null; then
        return 0
    else
        return 1
    fi
}

# 获取服务状态文本
get_status_text() {
    local service=$1
    
    if check_service_status "$service"; then
        echo -e "${GREEN}运行中${NC}"
    else
        echo -e "${RED}已停止${NC}"
    fi
}

# 启动服务
start_service() {
    local service=$1
    local name=$2
    
    log_info "启动 $name..."
    
    if check_service_status "$service"; then
        log_warn "$name 已在运行"
        return 0
    fi
    
    sudo systemctl start "$service"
    
    # 等待服务启动
    sleep 2
    
    if check_service_status "$service"; then
        log_success "$name 启动成功"
        return 0
    else
        log_error "$name 启动失败"
        return 1
    fi
}

# 停止服务
stop_service() {
    local service=$1
    local name=$2
    
    log_info "停止 $name..."
    
    if ! check_service_status "$service"; then
        log_warn "$name 未在运行"
        return 0
    fi
    
    sudo systemctl stop "$service"
    
    # 等待服务停止
    sleep 2
    
    if ! check_service_status "$service"; then
        log_success "$name 已停止"
        return 0
    else
        log_error "$name 停止失败"
        return 1
    fi
}

# 重启服务
restart_service() {
    local service=$1
    local name=$2
    
    log_info "重启 $name..."
    
    sudo systemctl restart "$service"
    
    # 等待服务重启
    sleep 3
    
    if check_service_status "$service"; then
        log_success "$name 重启成功"
        return 0
    else
        log_error "$name 重启失败"
        return 1
    fi
}

# 启动所有服务
start_all() {
    log_header "启动 SISM 服务"
    
    # 按依赖顺序启动
    start_service "$POSTGRES_SERVICE" "PostgreSQL"
    sleep 2
    start_service "$BACKEND_SERVICE" "SISM Backend"
    sleep 3
    start_service "$NGINX_SERVICE" "Nginx"
    
    echo
    show_status
}

# 停止所有服务
stop_all() {
    log_header "停止 SISM 服务"
    
    # 按反向依赖顺序停止
    stop_service "$NGINX_SERVICE" "Nginx"
    stop_service "$BACKEND_SERVICE" "SISM Backend"
    # 通常不停止 PostgreSQL，因为可能有其他服务依赖
    log_warn "PostgreSQL 服务未停止 (可能有其他服务依赖)"
    
    echo
    show_status
}

# 重启所有服务
restart_all() {
    log_header "重启 SISM 服务"
    
    restart_service "$POSTGRES_SERVICE" "PostgreSQL"
    sleep 2
    restart_service "$BACKEND_SERVICE" "SISM Backend"
    sleep 3
    restart_service "$NGINX_SERVICE" "Nginx"
    
    echo
    show_status
}

# 显示服务状态
show_status() {
    log_header "SISM 服务状态"
    
    printf "%-20s: %s\n" "PostgreSQL" "$(get_status_text $POSTGRES_SERVICE)"
    printf "%-20s: %s\n" "SISM Backend" "$(get_status_text $BACKEND_SERVICE)"
    printf "%-20s: %s\n" "Nginx" "$(get_status_text $NGINX_SERVICE)"
    
    echo
    
    # 显示端口监听状态
    log_info "端口监听状态:"
    echo "----------------------------------------"
    
    # PostgreSQL
    if netstat -tlnp 2>/dev/null | grep -q ":5432 " || ss -tlnp 2>/dev/null | grep -q ":5432 "; then
        printf "  %-10s: ${GREEN}监听中${NC}\n" "5432"
    else
        printf "  %-10s: ${RED}未监听${NC}\n" "5432"
    fi
    
    # Backend
    if netstat -tlnp 2>/dev/null | grep -q ":8080 " || ss -tlnp 2>/dev/null | grep -q ":8080 "; then
        printf "  %-10s: ${GREEN}监听中${NC}\n" "8080"
    else
        printf "  %-10s: ${RED}未监听${NC}\n" "8080"
    fi
    
    # Nginx HTTP
    if netstat -tlnp 2>/dev/null | grep -q ":80 " || ss -tlnp 2>/dev/null | grep -q ":80 "; then
        printf "  %-10s: ${GREEN}监听中${NC}\n" "80"
    else
        printf "  %-10s: ${RED}未监听${NC}\n" "80"
    fi
    
    # Nginx HTTPS
    if netstat -tlnp 2>/dev/null | grep -q ":443 " || ss -tlnp 2>/dev/null | grep -q ":443 "; then
        printf "  %-10s: ${GREEN}监听中${NC}\n" "443"
    else
        printf "  %-10s: ${RED}未监听${NC}\n" "443"
    fi
    
    echo "----------------------------------------"
}

# 显示日志
show_logs() {
    local service=$1
    local lines=${2:-50}
    
    case "$service" in
        backend)
            log_header "SISM Backend 日志 (最近 $lines 行)"
            sudo journalctl -u "$BACKEND_SERVICE" -n "$lines" --no-pager
            ;;
        nginx)
            log_header "Nginx 访问日志 (最近 $lines 行)"
            sudo tail -n "$lines" /var/log/nginx/sism_access.log 2>/dev/null || log_warn "日志文件不存在"
            ;;
        nginx-error)
            log_header "Nginx 错误日志 (最近 $lines 行)"
            sudo tail -n "$lines" /var/log/nginx/sism_error.log 2>/dev/null || log_warn "日志文件不存在"
            ;;
        postgres)
            log_header "PostgreSQL 日志 (最近 $lines 行)"
            sudo journalctl -u "$POSTGRES_SERVICE" -n "$lines" --no-pager
            ;;
        *)
            log_error "未知的服务: $service"
            echo "可用选项: backend, nginx, nginx-error, postgres"
            ;;
    esac
}

# 显示帮助
show_help() {
    echo "SISM 服务管理脚本"
    echo
    echo "用法: $0 <命令> [选项]"
    echo
    echo "命令:"
    echo "  start           - 启动所有服务"
    echo "  stop            - 停止所有服务"
    echo "  restart         - 重启所有服务"
    echo "  status          - 显示服务状态"
    echo "  logs <service>  - 显示服务日志"
    echo "  help            - 显示帮助"
    echo
    echo "单独服务管理:"
    echo "  start-backend   - 仅启动后端服务"
    echo "  stop-backend    - 仅停止后端服务"
    echo "  restart-backend - 仅重启后端服务"
    echo "  start-nginx     - 仅启动 Nginx"
    echo "  stop-nginx      - 仅停止 Nginx"
    echo "  restart-nginx   - 仅重启 Nginx"
    echo "  reload-nginx    - 重新加载 Nginx 配置"
    echo
    echo "日志选项:"
    echo "  logs backend      - 后端服务日志"
    echo "  logs nginx        - Nginx 访问日志"
    echo "  logs nginx-error  - Nginx 错误日志"
    echo "  logs postgres     - PostgreSQL 日志"
    echo
    echo "示例:"
    echo "  $0 start          # 启动所有服务"
    echo "  $0 status         # 查看服务状态"
    echo "  $0 logs backend   # 查看后端日志"
}

# 主函数
main() {
    case "$1" in
        start)
            start_all
            ;;
        stop)
            stop_all
            ;;
        restart)
            restart_all
            ;;
        status)
            show_status
            ;;
        start-backend)
            start_service "$BACKEND_SERVICE" "SISM Backend"
            ;;
        stop-backend)
            stop_service "$BACKEND_SERVICE" "SISM Backend"
            ;;
        restart-backend)
            restart_service "$BACKEND_SERVICE" "SISM Backend"
            ;;
        start-nginx)
            start_service "$NGINX_SERVICE" "Nginx"
            ;;
        stop-nginx)
            stop_service "$NGINX_SERVICE" "Nginx"
            ;;
        restart-nginx)
            restart_service "$NGINX_SERVICE" "Nginx"
            ;;
        reload-nginx)
            log_info "重新加载 Nginx 配置..."
            sudo nginx -t && sudo systemctl reload nginx
            log_success "Nginx 配置已重新加载"
            ;;
        logs)
            show_logs "$2" "${3:-50}"
            ;;
        help|--help|-h|"")
            show_help
            ;;
        *)
            log_error "未知命令: $1"
            echo
            show_help
            exit 1
            ;;
    esac
}

# 运行主函数
main "$@"
