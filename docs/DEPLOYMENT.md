# SISM 战略指标管理系统 - 生产部署指南

## 目录

1. [服务器环境要求](#1-服务器环境要求)
2. [后端部署](#2-后端部署)
3. [前端部署](#3-前端部署)
4. [数据库配置](#4-数据库配置)
5. [Nginx 配置](#5-nginx-配置)
6. [服务管理](#6-服务管理)
7. [健康检查与监控](#7-健康检查与监控)
8. [故障排查](#8-故障排查)

---

## 1. 服务器环境要求

### 1.1 硬件要求

| 组件 | 最低配置 | 推荐配置 |
|------|---------|---------|
| CPU | 2 核 | 4 核+ |
| 内存 | 4 GB | 8 GB+ |
| 磁盘 | 50 GB SSD | 100 GB+ SSD |
| 网络 | 100 Mbps | 1 Gbps |

### 1.2 软件要求

| 软件 | 版本要求 | 说明 |
|------|---------|------|
| 操作系统 | CentOS 7+ / Ubuntu 20.04+ / Debian 11+ | 推荐 Ubuntu 22.04 LTS |
| JDK | 17+ | 推荐 OpenJDK 17 或 Amazon Corretto 17 |
| Node.js | 18+ | 仅构建时需要，生产环境可选 |
| PostgreSQL | 15+ | 或兼容的 OpenTenBase |
| Nginx | 1.20+ | 反向代理和静态资源服务 |
| Maven | 3.8+ | 仅构建时需要 |

### 1.3 安装依赖

#### Ubuntu/Debian

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装 JDK 17
sudo apt install -y openjdk-17-jdk

# 安装 Node.js 18 (构建用)
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# 安装 Nginx
sudo apt install -y nginx

# 安装 PostgreSQL 15
sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
sudo apt update
sudo apt install -y postgresql-15

# 验证安装
java -version
node -v
nginx -v
psql --version
```

#### CentOS/RHEL

```bash
# 安装 JDK 17
sudo yum install -y java-17-openjdk java-17-openjdk-devel

# 安装 Node.js 18
curl -fsSL https://rpm.nodesource.com/setup_18.x | sudo bash -
sudo yum install -y nodejs

# 安装 Nginx
sudo yum install -y epel-release
sudo yum install -y nginx

# 安装 PostgreSQL 15
sudo yum install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm
sudo yum install -y postgresql15-server postgresql15
sudo /usr/pgsql-15/bin/postgresql-15-setup initdb
sudo systemctl enable postgresql-15
sudo systemctl start postgresql-15
```

---

## 2. 后端部署

### 2.1 构建后端 JAR 包

```bash
# 进入后端项目目录
cd sism-backend

# 清理并构建（跳过测试以加快构建）
./mvnw clean package -DskipTests

# 或使用完整测试构建
./mvnw clean package

# 构建产物位置
ls -la target/sism-backend-1.0.0.jar
```

### 2.2 创建部署目录

```bash
# 创建应用目录
sudo mkdir -p /opt/sism/backend
sudo mkdir -p /opt/sism/scripts
sudo mkdir -p /var/log/sism
sudo mkdir -p /var/backups/sism

# 设置权限
sudo useradd -r -s /bin/false sism
sudo chown -R sism:sism /opt/sism
sudo chown -R sism:sism /var/log/sism
sudo chown -R sism:sism /var/backups/sism
```

### 2.3 部署 JAR 包

```bash
# 复制 JAR 包
sudo cp target/sism-backend-1.0.0.jar /opt/sism/backend/

# 创建符号链接（便于版本管理）
sudo ln -sf /opt/sism/backend/sism-backend-1.0.0.jar /opt/sism/backend/sism-backend.jar

# 设置权限
sudo chown sism:sism /opt/sism/backend/sism-backend*.jar
```

### 2.4 配置环境变量

创建环境配置文件 `/opt/sism/backend/.env`:

```bash
sudo tee /opt/sism/backend/.env << 'EOF'
# 数据库配置
DB_HOST=localhost
DB_PORT=5432
DB_NAME=sism_prod
DB_USERNAME=sism_user
DB_PASSWORD=your_secure_password_here

# JWT 配置 (生成安全的随机密钥)
JWT_SECRET=your_256_bit_secret_key_here_at_least_32_characters

# 服务器配置
SERVER_PORT=8080

# 日志配置
LOG_PATH=/var/log/sism

# CORS 配置
ALLOWED_ORIGINS=https://blackevil.cn

# Swagger 配置 (生产环境建议禁用)
SWAGGER_ENABLED=false
EOF

# 设置权限（仅 root 和 sism 用户可读）
sudo chmod 600 /opt/sism/backend/.env
sudo chown sism:sism /opt/sism/backend/.env
```

### 2.5 创建 Systemd 服务

创建服务文件 `/etc/systemd/system/sism-backend.service`:

```bash
sudo tee /etc/systemd/system/sism-backend.service << 'EOF'
[Unit]
Description=SISM Backend Service
After=network.target postgresql.service
Requires=postgresql.service

[Service]
Type=simple
User=sism
Group=sism
WorkingDirectory=/opt/sism/backend
EnvironmentFile=/opt/sism/backend/.env
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar /opt/sism/backend/sism-backend.jar --spring.profiles.active=prod
ExecStop=/bin/kill -TERM $MAINPID
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=sism-backend

# 安全配置
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/var/log/sism

[Install]
WantedBy=multi-user.target
EOF

# 重新加载 systemd 配置
sudo systemctl daemon-reload

# 启用开机自启
sudo systemctl enable sism-backend
```

### 2.6 启动后端服务

```bash
# 启动服务
sudo systemctl start sism-backend

# 查看状态
sudo systemctl status sism-backend

# 查看日志
sudo journalctl -u sism-backend -f
```

---

## 3. 前端部署

### 3.1 构建前端静态资源

```bash
# 进入前端项目目录
cd strategic-task-management

# 安装依赖
npm ci

# 类型检查
npm run type-check

# 生产构建
npm run build:prod

# 构建产物位置
ls -la dist/
```

### 3.2 部署静态资源

```bash
# 创建前端目录
sudo mkdir -p /var/www/sism

# 复制构建产物
sudo cp -r dist/* /var/www/sism/

# 设置权限
sudo chown -R www-data:www-data /var/www/sism
sudo chmod -R 755 /var/www/sism
```

### 3.3 配置前端环境变量

前端环境变量在构建时注入，确保 `.env.production` 配置正确：

```bash
# strategic-task-management/.env.production
VITE_API_BASE_URL=https://sism.example.com/api
VITE_APP_TITLE=战略指标管理系统
VITE_APP_ENV=production
```

---

## 4. 数据库配置

### 4.1 创建数据库和用户

```bash
# 切换到 postgres 用户
sudo -u postgres psql

# 创建数据库用户
CREATE USER sism_user WITH PASSWORD 'your_secure_password_here';

# 创建数据库
CREATE DATABASE sism_prod OWNER sism_user;

# 授予权限
GRANT ALL PRIVILEGES ON DATABASE sism_prod TO sism_user;

# 退出
\q
```

### 4.2 初始化数据库 Schema

```bash
# 使用初始化脚本
sudo -u postgres psql -d sism_prod -f /path/to/strategic-task-management/database/init.sql

# 或者使用提供的初始化脚本
sudo /opt/sism/scripts/init-database.sh
```

### 4.3 配置 PostgreSQL 连接

编辑 `/etc/postgresql/15/main/pg_hba.conf`:

```
# 允许本地连接
local   sism_prod   sism_user                           md5
host    sism_prod   sism_user   127.0.0.1/32            md5
host    sism_prod   sism_user   ::1/128                 md5
```

重启 PostgreSQL:

```bash
sudo systemctl restart postgresql
```

---

## 5. Nginx 配置

详见 [nginx/sism.conf](nginx/sism.conf) 配置文件。

### 5.1 安装 SSL 证书

```bash
# 使用 Let's Encrypt (推荐)
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d sism.example.com

# 或手动安装证书
sudo mkdir -p /etc/nginx/ssl
sudo cp your_certificate.crt /etc/nginx/ssl/sism.crt
sudo cp your_private_key.key /etc/nginx/ssl/sism.key
sudo chmod 600 /etc/nginx/ssl/sism.key
```

### 5.2 部署 Nginx 配置

```bash
# 复制配置文件
sudo cp docs/nginx/sism.conf /etc/nginx/sites-available/sism

# 创建符号链接
sudo ln -s /etc/nginx/sites-available/sism /etc/nginx/sites-enabled/

# 测试配置
sudo nginx -t

# 重新加载 Nginx
sudo systemctl reload nginx
```

---

## 6. 服务管理

### 6.1 常用命令

```bash
# 后端服务
sudo systemctl start sism-backend    # 启动
sudo systemctl stop sism-backend     # 停止
sudo systemctl restart sism-backend  # 重启
sudo systemctl status sism-backend   # 状态

# Nginx
sudo systemctl reload nginx          # 重新加载配置
sudo systemctl restart nginx         # 重启

# PostgreSQL
sudo systemctl restart postgresql    # 重启数据库
```

### 6.2 使用管理脚本

```bash
# 启动所有服务
sudo /opt/sism/scripts/sism-service.sh start

# 停止所有服务
sudo /opt/sism/scripts/sism-service.sh stop

# 重启所有服务
sudo /opt/sism/scripts/sism-service.sh restart

# 查看状态
sudo /opt/sism/scripts/sism-service.sh status
```

---

## 7. 健康检查与监控

### 7.1 健康检查端点

| 端点 | 说明 |
|------|------|
| `GET /actuator/health` | 应用健康状态 |
| `GET /actuator/health/liveness` | 存活探针 |
| `GET /actuator/health/readiness` | 就绪探针 |
| `GET /actuator/info` | 应用信息 |

### 7.2 手动健康检查

```bash
# 检查后端健康状态
curl -s http://localhost:8080/actuator/health | jq

# 检查数据库连接
curl -s http://localhost:8080/actuator/health/db | jq

# 使用健康检查脚本
sudo /opt/sism/scripts/health-check.sh
```

### 7.3 日志查看

```bash
# 查看后端日志
tail -f /var/log/sism/sism-prod.log

# 查看 Nginx 访问日志
tail -f /var/log/nginx/sism_access.log

# 查看 Nginx 错误日志
tail -f /var/log/nginx/sism_error.log

# 使用 journalctl
sudo journalctl -u sism-backend -f --since "1 hour ago"
```

---

## 8. 故障排查

### 8.1 常见问题

#### 后端无法启动

```bash
# 检查 Java 版本
java -version

# 检查端口占用
sudo netstat -tlnp | grep 8080

# 检查日志
sudo journalctl -u sism-backend -n 100 --no-pager
```

#### 数据库连接失败

```bash
# 测试数据库连接
psql -h localhost -U sism_user -d sism_prod -c "SELECT 1"

# 检查 PostgreSQL 状态
sudo systemctl status postgresql

# 检查 pg_hba.conf 配置
sudo cat /etc/postgresql/15/main/pg_hba.conf | grep sism
```

#### Nginx 502 错误

```bash
# 检查后端是否运行
curl -s http://localhost:8080/actuator/health

# 检查 Nginx 错误日志
sudo tail -f /var/log/nginx/sism_error.log

# 检查 SELinux (CentOS)
sudo setsebool -P httpd_can_network_connect 1
```

### 8.2 性能调优

#### JVM 参数调整

编辑 `/etc/systemd/system/sism-backend.service`:

```bash
# 增加堆内存
ExecStart=/usr/bin/java -Xms1g -Xmx4g -XX:+UseG1GC -jar ...
```

#### PostgreSQL 调优

编辑 `/etc/postgresql/15/main/postgresql.conf`:

```
shared_buffers = 256MB
effective_cache_size = 1GB
work_mem = 16MB
maintenance_work_mem = 128MB
```

---

## 附录

### A. 目录结构

```
/opt/sism/
├── backend/
│   ├── sism-backend.jar -> sism-backend-1.0.0.jar
│   ├── sism-backend-1.0.0.jar
│   └── .env
├── scripts/
│   ├── sism-service.sh
│   ├── health-check.sh
│   ├── init-database.sh
│   └── backup-database.sh
└── backups/

/var/www/sism/
├── index.html
├── assets/
└── ...

/var/log/sism/
├── sism-prod.log
└── sism-prod.log.*.gz

/var/backups/sism/
└── sism_YYYYMMDD_HHMMSS.sql.gz
```

### B. 端口清单

| 端口 | 服务 | 说明 |
|------|------|------|
| 80 | Nginx | HTTP (重定向到 HTTPS) |
| 443 | Nginx | HTTPS |
| 8080 | Spring Boot | 后端 API (内部) |
| 5432 | PostgreSQL | 数据库 (内部) |

### C. 联系方式

如有问题，请联系系统管理员。
