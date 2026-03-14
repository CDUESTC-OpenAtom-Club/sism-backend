#!/bin/bash
# ============================================
# SISM 数据库初始化脚本
# ============================================
# 用途: 创建数据库、用户并初始化 Schema
# 使用: sudo ./init-database.sh
# ============================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# 配置变量 (可通过环境变量覆盖)
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-sism_prod}"
DB_USER="${DB_USER:-sism_user}"
DB_PASSWORD="${DB_PASSWORD:-}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
INIT_SQL_PATH="${INIT_SQL_PATH:-/opt/sism/database/init.sql}"

# 检查是否以 root 或 postgres 用户运行
check_permissions() {
    if [[ $EUID -ne 0 ]] && [[ $(whoami) != "postgres" ]]; then
        log_error "请以 root 用户或 postgres 用户运行此脚本"
        log_info "使用: sudo $0"
        exit 1
    fi
}

# 检查 PostgreSQL 是否运行
check_postgresql() {
    log_info "检查 PostgreSQL 服务状态..."
    
    if ! systemctl is-active --quiet postgresql; then
        log_error "PostgreSQL 服务未运行"
        log_info "请先启动 PostgreSQL: sudo systemctl start postgresql"
        exit 1
    fi
    
    log_success "PostgreSQL 服务正在运行"
}

# 检查初始化 SQL 文件
check_init_sql() {
    log_info "检查初始化 SQL 文件..."
    
    if [[ ! -f "$INIT_SQL_PATH" ]]; then
        log_error "初始化 SQL 文件不存在: $INIT_SQL_PATH"
        log_info "请确保 init.sql 文件已复制到正确位置"
        exit 1
    fi
    
    log_success "找到初始化 SQL 文件: $INIT_SQL_PATH"
}

# 提示输入密码
prompt_password() {
    if [[ -z "$DB_PASSWORD" ]]; then
        log_warn "未设置数据库密码"
        echo -n "请输入数据库用户 $DB_USER 的密码: "
        read -s DB_PASSWORD
        echo
        
        if [[ -z "$DB_PASSWORD" ]]; then
            log_error "密码不能为空"
            exit 1
        fi
        
        echo -n "请再次输入密码确认: "
        read -s DB_PASSWORD_CONFIRM
        echo
        
        if [[ "$DB_PASSWORD" != "$DB_PASSWORD_CONFIRM" ]]; then
            log_error "两次输入的密码不一致"
            exit 1
        fi
    fi
}

# 检查数据库是否已存在
check_database_exists() {
    log_info "检查数据库 $DB_NAME 是否已存在..."
    
    if sudo -u postgres psql -lqt | cut -d \| -f 1 | grep -qw "$DB_NAME"; then
        log_warn "数据库 $DB_NAME 已存在"
        echo -n "是否删除并重新创建? (y/N): "
        read -r response
        
        if [[ "$response" =~ ^[Yy]$ ]]; then
            log_info "删除现有数据库..."
            sudo -u postgres psql -c "DROP DATABASE IF EXISTS $DB_NAME;"
            log_success "数据库已删除"
        else
            log_info "保留现有数据库，跳过创建步骤"
            return 1
        fi
    fi
    
    return 0
}

# 检查用户是否已存在
check_user_exists() {
    log_info "检查用户 $DB_USER 是否已存在..."
    
    if sudo -u postgres psql -tAc "SELECT 1 FROM pg_roles WHERE rolname='$DB_USER'" | grep -q 1; then
        log_warn "用户 $DB_USER 已存在"
        echo -n "是否更新密码? (y/N): "
        read -r response
        
        if [[ "$response" =~ ^[Yy]$ ]]; then
            log_info "更新用户密码..."
            sudo -u postgres psql -c "ALTER USER $DB_USER WITH PASSWORD '$DB_PASSWORD';"
            log_success "密码已更新"
        fi
        return 1
    fi
    
    return 0
}

# 创建数据库用户
create_user() {
    if check_user_exists; then
        log_info "创建数据库用户 $DB_USER..."
        sudo -u postgres psql -c "CREATE USER $DB_USER WITH PASSWORD '$DB_PASSWORD';"
        log_success "用户 $DB_USER 创建成功"
    fi
}

# 创建数据库
create_database() {
    if check_database_exists; then
        log_info "创建数据库 $DB_NAME..."
        sudo -u postgres psql -c "CREATE DATABASE $DB_NAME OWNER $DB_USER ENCODING 'UTF8' LC_COLLATE 'en_US.UTF-8' LC_CTYPE 'en_US.UTF-8' TEMPLATE template0;"
        log_success "数据库 $DB_NAME 创建成功"
    fi
}

# 授予权限
grant_privileges() {
    log_info "授予用户权限..."
    sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;"
    sudo -u postgres psql -d "$DB_NAME" -c "GRANT ALL ON SCHEMA public TO $DB_USER;"
    log_success "权限授予成功"
}

# 初始化 Schema
init_schema() {
    log_info "初始化数据库 Schema..."
    
    # 使用 sism_user 执行初始化脚本
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$INIT_SQL_PATH"
    
    if [[ $? -eq 0 ]]; then
        log_success "Schema 初始化成功"
    else
        log_error "Schema 初始化失败"
        exit 1
    fi
}

# 验证初始化结果
verify_init() {
    log_info "验证初始化结果..."
    
    # 检查表数量
    TABLE_COUNT=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -tAc "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE';")
    
    log_info "创建的表数量: $TABLE_COUNT"
    
    # 检查关键表是否存在
    REQUIRED_TABLES=("org" "app_user" "assessment_cycle" "strategic_task" "indicator" "milestone" "progress_report" "audit_log")
    
    for table in "${REQUIRED_TABLES[@]}"; do
        EXISTS=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -tAc "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '$table');")
        
        if [[ "$EXISTS" == "t" ]]; then
            log_success "表 $table 存在"
        else
            log_error "表 $table 不存在"
        fi
    done
    
    # 检查初始数据
    ORG_COUNT=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -tAc "SELECT COUNT(*) FROM org;")
    USER_COUNT=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -tAc "SELECT COUNT(*) FROM app_user;")
    
    log_info "组织记录数: $ORG_COUNT"
    log_info "用户记录数: $USER_COUNT"
    
    log_success "数据库初始化验证完成"
}

# 配置 pg_hba.conf
configure_pg_hba() {
    log_info "配置 pg_hba.conf..."
    
    PG_HBA_PATH=$(sudo -u postgres psql -tAc "SHOW hba_file;")
    
    # 检查是否已配置
    if grep -q "$DB_NAME.*$DB_USER" "$PG_HBA_PATH"; then
        log_info "pg_hba.conf 已包含 $DB_USER 的配置"
        return
    fi
    
    echo -n "是否自动配置 pg_hba.conf? (y/N): "
    read -r response
    
    if [[ "$response" =~ ^[Yy]$ ]]; then
        # 备份原文件
        sudo cp "$PG_HBA_PATH" "${PG_HBA_PATH}.bak"
        
        # 添加配置
        sudo tee -a "$PG_HBA_PATH" > /dev/null << EOF

# SISM 数据库访问配置
local   $DB_NAME    $DB_USER                            md5
host    $DB_NAME    $DB_USER    127.0.0.1/32            md5
host    $DB_NAME    $DB_USER    ::1/128                 md5
EOF
        
        log_success "pg_hba.conf 配置已添加"
        log_warn "请重启 PostgreSQL 使配置生效: sudo systemctl restart postgresql"
    fi
}

# 显示连接信息
show_connection_info() {
    echo
    echo "============================================"
    echo "数据库初始化完成!"
    echo "============================================"
    echo "连接信息:"
    echo "  主机: $DB_HOST"
    echo "  端口: $DB_PORT"
    echo "  数据库: $DB_NAME"
    echo "  用户: $DB_USER"
    echo
    echo "连接命令:"
    echo "  psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME"
    echo
    echo "JDBC URL:"
    echo "  jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME"
    echo "============================================"
}

# 主函数
main() {
    echo "============================================"
    echo "SISM 数据库初始化脚本"
    echo "============================================"
    echo
    
    check_permissions
    check_postgresql
    check_init_sql
    prompt_password
    
    create_user
    create_database
    grant_privileges
    init_schema
    verify_init
    configure_pg_hba
    show_connection_info
}

# 运行主函数
main "$@"
