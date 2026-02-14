#!/bin/bash
# ============================================
# SISM 快速部署脚本
# ============================================
# 用途: 一键部署 SISM 系统
# 使用: sudo ./deploy.sh
# ============================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# 配置
SISM_HOME="/opt/sism"
SISM_USER="sism"
FRONTEND_DIR="/var/www/sism"
LOG_DIR="/var/log/sism"
BACKUP_DIR="/var/backups/sism"

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
    echo
    echo -e "${CYAN}============================================${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}============================================${NC}"
}

# 检查 root 权限
check_root() {
    if [[ $EUID -ne 0 ]]; then
        log_error "请以 root 用户运行此脚本"
        log_info "使用: sudo $0"
        exit 1
    fi
}

# 检查必要的文件
check_files() {
    log_header "检查部署文件"
    
    local missing=false
    
    # 检查后端 JAR
    if [[ ! -f "sism-backend/target/sism-backend-1.0.0.jar" ]]; then
        log_error "后端 JAR 文件不存在，请先执行构建"
        log_info "cd sism-backend && ./mvnw clean package -DskipTests"
        missing=true
    else
        log_success "后端 JAR 文件存在"
    fi
    
    # 检查前端构建
    if [[ ! -d "strategic-task-management/dist" ]]; then
        log_error "前端构建目录不存在，请先执行构建"
        log_info "cd strategic-task-management && npm run build:prod"
        missing=true
    else
        log_success "前端构建目录存在"
    fi
    
    # 检查数据库初始化脚本
    if [[ ! -f "strategic-task-management/database/init.sql" ]]; then
        log_error "数据库初始化脚本不存在"
        missing=true
    else
        log_success "数据库初始化脚本存在"
    fi
    
    if [[ "$missing" == "true" ]]; then
        exit 1
    fi
}

# 创建目录结构
create_directories() {
    log_header "创建目录结构"
    
    mkdir -p "$SISM_HOME/backend"
    mkdir -p "$SISM_HOME/scripts"
    mkdir -p "$SISM_HOME/database"
    mkdir -p "$FRONTEND_DIR"
    mkdir -p "$LOG_DIR"
    mkdir -p "$BACKUP_DIR"
    
    log_success "目录结构创建完成"
}

# 创建系统用户
create_user() {
    log_header "创建系统用户"
    
    if id "$SISM_USER" &>/dev/null; then
        log_info "用户 $SISM_USER 已存在"
    else
        useradd -r -s /bin/false "$SISM_USER"
        log_success "用户 $SISM_USER 创建成功"
    fi
}

# 部署后端
deploy_backend() {
    log_header "部署后端服务"
    
    # 复制 JAR 文件
    cp sism-backend/target/sism-backend-1.0.0.jar "$SISM_HOME/backend/"
    ln -sf "$SISM_HOME/backend/sism-backend-1.0.0.jar" "$SISM_HOME/backend/sism-backend.jar"
    
    # 创建环境配置文件
    if [[ ! -f "$SISM_HOME/backend/.env" ]]; then
        log_info "创建环境配置文件..."
        cat > "$SISM_HOME/backend/.env" << 'EOF'
# 数据库配置
DB_HOST=localhost
DB_PORT=5432
DB_NAME=sism_prod
DB_USERNAME=sism_user
DB_PASSWORD=CHANGE_ME

# JWT 配置
JWT_SECRET=CHANGE_ME_TO_A_SECURE_256_BIT_KEY

# 服务器配置
SERVER_PORT=8080
LOG_PATH=/var/log/sism

# CORS 配置
ALLOWED_ORIGINS=https://sism.example.com

# Swagger 配置
SWAGGER_ENABLED=false
EOF
        chmod 600 "$SISM_HOME/backend/.env"
        log_warn "请编辑 $SISM_HOME/backend/.env 配置数据库密码和 JWT 密钥"
    fi
    
    # 设置权限
    chown -R "$SISM_USER:$SISM_USER" "$SISM_HOME"
    chown -R "$SISM_USER:$SISM_USER" "$LOG_DIR"
    chown -R "$SISM_USER:$SISM_USER" "$BACKUP_DIR"
    
    log_success "后端部署完成"
}

# 部署前端
deploy_frontend() {
    log_header "部署前端资源"
    
    # 复制前端构建产物
    cp -r strategic-task-management/dist/* "$FRONTEND_DIR/"
    
    # 设置权限
    chown -R www-data:www-data "$FRONTEND_DIR"
    chmod -R 755 "$FRONTEND_DIR"
    
    log_success "前端部署完成"
}

# 部署脚本
deploy_scripts() {
    log_header "部署管理脚本"
    
    # 复制脚本
    cp docs/scripts/*.sh "$SISM_HOME/scripts/"
    chmod +x "$SISM_HOME/scripts/"*.sh
    
    # 复制数据库初始化脚本
    cp strategic-task-management/database/init.sql "$SISM_HOME/database/"
    
    log_success "脚本部署完成"
}

# 安装 Systemd 服务
install_service() {
    log_header "安装 Systemd 服务"
    
    cat > /etc/systemd/system/sism-backend.service << EOF
[Unit]
Description=SISM Backend Service
After=network.target postgresql.service
Requires=postgresql.service

[Service]
Type=simple
User=$SISM_USER
Group=$SISM_USER
WorkingDirectory=$SISM_HOME/backend
EnvironmentFile=$SISM_HOME/backend/.env
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar $SISM_HOME/backend/sism-backend.jar --spring.profiles.active=prod
ExecStop=/bin/kill -TERM \$MAINPID
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=sism-backend

NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=$LOG_DIR

[Install]
WantedBy=multi-user.target
EOF

    systemctl daemon-reload
    systemctl enable sism-backend
    
    log_success "Systemd 服务安装完成"
}

# 部署 Nginx 配置
deploy_nginx() {
    log_header "部署 Nginx 配置"
    
    if [[ -f "docs/nginx/sism.conf" ]]; then
        cp docs/nginx/sism.conf /etc/nginx/sites-available/sism
        
        # 创建符号链接
        if [[ ! -L /etc/nginx/sites-enabled/sism ]]; then
            ln -s /etc/nginx/sites-available/sism /etc/nginx/sites-enabled/
        fi
        
        # 测试配置
        if nginx -t; then
            log_success "Nginx 配置有效"
        else
            log_error "Nginx 配置无效，请检查配置文件"
        fi
    else
        log_warn "Nginx 配置文件不存在，请手动配置"
    fi
}

# 设置定时任务
setup_cron() {
    log_header "设置定时任务"
    
    # 数据库备份 (每天凌晨 2 点)
    (crontab -l 2>/dev/null | grep -v "backup-database.sh"; echo "0 2 * * * $SISM_HOME/scripts/backup-database.sh full >> $LOG_DIR/backup.log 2>&1") | crontab -
    
    # 健康检查 (每 5 分钟)
    (crontab -l 2>/dev/null | grep -v "health-check.sh"; echo "*/5 * * * * $SISM_HOME/scripts/health-check.sh --quiet >> $LOG_DIR/health-check.log 2>&1") | crontab -
    
    log_success "定时任务设置完成"
}

# 显示部署摘要
show_summary() {
    log_header "部署完成"
    
    echo "部署目录:"
    echo "  后端: $SISM_HOME/backend/"
    echo "  前端: $FRONTEND_DIR/"
    echo "  脚本: $SISM_HOME/scripts/"
    echo "  日志: $LOG_DIR/"
    echo "  备份: $BACKUP_DIR/"
    echo
    echo "下一步操作:"
    echo "  1. 编辑环境配置: sudo nano $SISM_HOME/backend/.env"
    echo "  2. 初始化数据库: sudo $SISM_HOME/scripts/init-database.sh"
    echo "  3. 配置 SSL 证书: sudo certbot --nginx -d sism.example.com"
    echo "  4. 启动服务: sudo $SISM_HOME/scripts/sism-service.sh start"
    echo "  5. 检查状态: sudo $SISM_HOME/scripts/health-check.sh"
    echo
    echo "管理命令:"
    echo "  启动服务: sudo systemctl start sism-backend"
    echo "  停止服务: sudo systemctl stop sism-backend"
    echo "  查看日志: sudo journalctl -u sism-backend -f"
}

# 主函数
main() {
    log_header "SISM 部署脚本"
    
    check_root
    check_files
    create_directories
    create_user
    deploy_backend
    deploy_frontend
    deploy_scripts
    install_service
    deploy_nginx
    setup_cron
    show_summary
}

# 运行主函数
main "$@"
