# SISM Backend 生产环境部署指南

## 目录

1. [部署架构](#部署架构)
2. [环境准备](#环境准备)
3. [配置示例](#配置示例)
4. [部署步骤](#部署步骤)
5. [监控与维护](#监控与维护)
6. [故障排查](#故障排查)

## 部署架构

### 推荐架构

```
                    ┌─────────────────┐
                    │   Load Balancer │
                    │   (Nginx/ALB)   │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
         ┌────▼────┐    ┌────▼────┐   ┌────▼────┐
         │ App 1   │    │ App 2   │   │ App 3   │
         │ :8080   │    │ :8080   │   │ :8080   │
         └────┬────┘    └────┬────┘   └────┬────┘
              │              │              │
              └──────────────┼──────────────┘
                             │
                    ┌────────▼────────┐
                    │   PostgreSQL    │
                    │   (Primary)     │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │   PostgreSQL    │
                    │   (Replica)     │
                    └─────────────────┘
```

### 组件说明

1. **负载均衡器**: Nginx 或云服务商的负载均衡器（如 AWS ALB）
2. **应用服务器**: 多个 Spring Boot 实例，实现高可用
3. **数据库**: PostgreSQL 主从复制，读写分离
4. **缓存**: Redis（用于频率限制、会话管理）
5. **监控**: Prometheus + Grafana
6. **日志**: ELK Stack 或云日志服务

## 环境准备

### 系统要求

- **操作系统**: Ubuntu 20.04 LTS 或 CentOS 8
- **Java**: OpenJDK 17
- **内存**: 最低 2GB，推荐 4GB+
- **CPU**: 最低 2 核，推荐 4 核+
- **磁盘**: 最低 20GB，推荐 50GB+

### 安装依赖

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装 Java 17
sudo apt install openjdk-17-jdk -y

# 验证安装
java -version

# 安装 PostgreSQL 客户端
sudo apt install postgresql-client -y

# 安装监控工具
sudo apt install htop iotop -y
```

## 配置示例

### 场景 1: 单机部署

适用于小型应用或测试环境。

**.env.production**:
```bash
# JWT 配置
JWT_SECRET=<使用 openssl rand -base64 32 生成>
JWT_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# 数据库配置 - 本地数据库
DB_URL=jdbc:postgresql://localhost:5432/strategic_prod?stringtype=unspecified
DB_USERNAME=sism_prod
DB_PASSWORD=<强密码>

# CORS 配置
CORS_ALLOWED_ORIGINS=https://yourdomain.com

# 连接池配置 - 小规模
DB_POOL_MAX_SIZE=10
DB_POOL_MIN_IDLE=5

# 频率限制 - 内存存储
RATE_LIMIT_ENABLED=true
RATE_LIMIT_STORAGE=memory

# 日志配置
LOG_LEVEL_ROOT=INFO
LOG_PATH=/var/log/sism

# Swagger - 禁用
SWAGGER_ENABLED=false
```

### 场景 2: 分布式部署（推荐）

适用于生产环境，支持高可用和负载均衡。

**.env.production**:
```bash
# JWT 配置
JWT_SECRET=<使用 openssl rand -base64 32 生成>
JWT_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000
JWT_MAX_SESSIONS=5

# 数据库配置 - 远程数据库集群
DB_URL=jdbc:postgresql://db-cluster.internal:5432/strategic_prod?stringtype=unspecified&ssl=true&sslmode=require
DB_USERNAME=sism_prod
DB_PASSWORD=<强密码>

# CORS 配置 - 多个域名
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com,https://app.yourdomain.com

# 连接池配置 - 大规模
DB_POOL_MAX_SIZE=30
DB_POOL_MIN_IDLE=15
DB_CONNECTION_TIMEOUT=30000
DB_IDLE_TIMEOUT=600000
DB_MAX_LIFETIME=1800000
DB_LEAK_DETECTION=0

# 频率限制 - Redis 存储（支持分布式）
RATE_LIMIT_ENABLED=true
RATE_LIMIT_STORAGE=redis
RATE_LIMIT_LOGIN_LIMIT=5
RATE_LIMIT_API_LIMIT=100

# 幂等性配置
IDEMPOTENCY_ENABLED=true
IDEMPOTENCY_TTL=300

# 日志配置
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=INFO
LOG_LEVEL_SPRING=WARN
LOG_PATH=/var/log/sism

# Swagger - 禁用
SWAGGER_ENABLED=false

# 环境标识
ENVIRONMENT=production
```

### 场景 3: Docker 部署

使用 Docker 容器化部署。

**docker-compose.yml**:
```yaml
version: '3.8'

services:
  app:
    image: sism-backend:latest
    ports:
      - "8080:8080"
    environment:
      - JWT_SECRET=${JWT_SECRET}
      - DB_URL=jdbc:postgresql://db:5432/strategic?stringtype=unspecified
      - DB_USERNAME=sism_prod
      - DB_PASSWORD=${DB_PASSWORD}
      - CORS_ALLOWED_ORIGINS=https://yourdomain.com
      - LOG_LEVEL_ROOT=INFO
      - DB_POOL_MAX_SIZE=20
      - RATE_LIMIT_ENABLED=true
      - SWAGGER_ENABLED=false
    depends_on:
      - db
    restart: unless-stopped
    volumes:
      - ./logs:/var/log/sism
    networks:
      - sism-network

  db:
    image: postgres:14
    environment:
      - POSTGRES_DB=strategic
      - POSTGRES_USER=sism_prod
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    restart: unless-stopped
    networks:
      - sism-network

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/nginx/ssl
    depends_on:
      - app
    restart: unless-stopped
    networks:
      - sism-network

volumes:
  postgres-data:

networks:
  sism-network:
    driver: bridge
```

**.env** (用于 docker-compose):
```bash
JWT_SECRET=<使用 openssl rand -base64 32 生成>
DB_PASSWORD=<强密码>
```

### 场景 4: Kubernetes 部署

使用 Kubernetes 进行容器编排。

**deployment.yaml**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sism-backend
  labels:
    app: sism-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: sism-backend
  template:
    metadata:
      labels:
        app: sism-backend
    spec:
      containers:
      - name: sism-backend
        image: sism-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: sism-secrets
              key: jwt-secret
        - name: DB_URL
          value: "jdbc:postgresql://postgres-service:5432/strategic?stringtype=unspecified"
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: sism-secrets
              key: db-username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: sism-secrets
              key: db-password
        - name: CORS_ALLOWED_ORIGINS
          value: "https://yourdomain.com"
        - name: LOG_LEVEL_ROOT
          value: "INFO"
        - name: DB_POOL_MAX_SIZE
          value: "30"
        - name: RATE_LIMIT_ENABLED
          value: "true"
        - name: SWAGGER_ENABLED
          value: "false"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /api/actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /api/actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
```

## 部署步骤

### 1. 准备应用包

```bash
# 编译打包
cd sism-backend
mvn clean package -DskipTests

# 验证 JAR 文件
ls -lh target/sism-backend-*.jar
```

### 2. 配置环境变量

```bash
# 创建配置文件
sudo mkdir -p /opt/sism
sudo cp .env.production.example /opt/sism/.env

# 编辑配置
sudo nano /opt/sism/.env

# 生成 JWT 密钥
openssl rand -base64 32

# 设置文件权限
sudo chmod 600 /opt/sism/.env
sudo chown sism:sism /opt/sism/.env
```

### 3. 创建系统服务

创建 systemd 服务文件：

```bash
sudo nano /etc/systemd/system/sism-backend.service
```

内容：
```ini
[Unit]
Description=SISM Backend Service
After=network.target postgresql.service

[Service]
Type=simple
User=sism
Group=sism
WorkingDirectory=/opt/sism
EnvironmentFile=/opt/sism/.env
ExecStart=/usr/bin/java -jar /opt/sism/sism-backend.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=sism-backend

# 资源限制
LimitNOFILE=65536
LimitNPROC=4096

# 安全设置
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/var/log/sism

[Install]
WantedBy=multi-user.target
```

### 4. 启动服务

```bash
# 重新加载 systemd
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start sism-backend

# 查看状态
sudo systemctl status sism-backend

# 设置开机自启
sudo systemctl enable sism-backend

# 查看日志
sudo journalctl -u sism-backend -f
```

### 5. 配置 Nginx 反向代理

```bash
sudo nano /etc/nginx/sites-available/sism
```

内容：
```nginx
upstream sism_backend {
    server localhost:8080;
    # 如果有多个实例
    # server localhost:8081;
    # server localhost:8082;
}

server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;
    
    # 重定向到 HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com www.yourdomain.com;
    
    # SSL 证书
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    
    # SSL 配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    
    # 日志
    access_log /var/log/nginx/sism-access.log;
    error_log /var/log/nginx/sism-error.log;
    
    # 代理配置
    location /api {
        proxy_pass http://sism_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # 缓冲设置
        proxy_buffering on;
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;
    }
    
    # 健康检查
    location /api/actuator/health {
        proxy_pass http://sism_backend;
        access_log off;
    }
}
```

启用配置：
```bash
sudo ln -s /etc/nginx/sites-available/sism /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 6. 验证部署

```bash
# 检查服务状态
sudo systemctl status sism-backend

# 检查健康状态
curl https://yourdomain.com/api/actuator/health

# 检查日志
sudo journalctl -u sism-backend -n 100

# 测试 API
curl -X POST https://yourdomain.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

## 监控与维护

### 1. 日志管理

```bash
# 查看实时日志
sudo journalctl -u sism-backend -f

# 查看最近 100 条日志
sudo journalctl -u sism-backend -n 100

# 查看特定时间段的日志
sudo journalctl -u sism-backend --since "2024-01-01" --until "2024-01-02"

# 查看错误日志
sudo journalctl -u sism-backend -p err

# 日志轮转配置
sudo nano /etc/logrotate.d/sism
```

日志轮转配置：
```
/var/log/sism/*.log {
    daily
    rotate 30
    compress
    delaycompress
    notifempty
    create 0640 sism sism
    sharedscripts
    postrotate
        systemctl reload sism-backend > /dev/null 2>&1 || true
    endscript
}
```

### 2. 性能监控

使用 Prometheus 和 Grafana 监控应用性能。

**prometheus.yml**:
```yaml
scrape_configs:
  - job_name: 'sism-backend'
    metrics_path: '/api/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

关键指标：
- JVM 内存使用
- GC 频率和时间
- HTTP 请求数和响应时间
- 数据库连接池状态
- 错误率

### 3. 健康检查

```bash
# 创建健康检查脚本
sudo nano /opt/sism/health-check.sh
```

内容：
```bash
#!/bin/bash

HEALTH_URL="http://localhost:8080/api/actuator/health"
MAX_RETRIES=3
RETRY_DELAY=5

for i in $(seq 1 $MAX_RETRIES); do
    response=$(curl -s -o /dev/null -w "%{http_code}" $HEALTH_URL)
    
    if [ "$response" = "200" ]; then
        echo "✅ Health check passed"
        exit 0
    fi
    
    echo "⚠️ Health check failed (attempt $i/$MAX_RETRIES)"
    sleep $RETRY_DELAY
done

echo "❌ Health check failed after $MAX_RETRIES attempts"
exit 1
```

设置定时任务：
```bash
# 编辑 crontab
crontab -e

# 每 5 分钟检查一次
*/5 * * * * /opt/sism/health-check.sh >> /var/log/sism/health-check.log 2>&1
```

### 4. 备份策略

#### 数据库备份

```bash
# 创建备份脚本
sudo nano /opt/sism/backup-db.sh
```

内容：
```bash
#!/bin/bash

BACKUP_DIR="/backup/sism"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="strategic_prod"
DB_USER="sism_prod"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 备份数据库
pg_dump -h localhost -U $DB_USER -d $DB_NAME | gzip > $BACKUP_DIR/db_backup_$DATE.sql.gz

# 删除 30 天前的备份
find $BACKUP_DIR -name "db_backup_*.sql.gz" -mtime +30 -delete

echo "Backup completed: db_backup_$DATE.sql.gz"
```

设置定时备份：
```bash
# 每天凌晨 2 点备份
0 2 * * * /opt/sism/backup-db.sh >> /var/log/sism/backup.log 2>&1
```

#### 配置文件备份

```bash
# 备份配置文件
sudo cp /opt/sism/.env /backup/sism/.env.$(date +%Y%m%d)
```

### 5. 更新部署

```bash
# 创建更新脚本
sudo nano /opt/sism/update.sh
```

内容：
```bash
#!/bin/bash

set -e

echo "=== SISM Backend 更新部署 ==="

# 备份当前版本
echo "1. 备份当前版本..."
cp /opt/sism/sism-backend.jar /opt/sism/sism-backend.jar.backup

# 停止服务
echo "2. 停止服务..."
sudo systemctl stop sism-backend

# 复制新版本
echo "3. 部署新版本..."
cp target/sism-backend-*.jar /opt/sism/sism-backend.jar

# 启动服务
echo "4. 启动服务..."
sudo systemctl start sism-backend

# 等待启动
echo "5. 等待服务启动..."
sleep 10

# 健康检查
echo "6. 健康检查..."
if /opt/sism/health-check.sh; then
    echo "✅ 更新成功"
    rm /opt/sism/sism-backend.jar.backup
else
    echo "❌ 更新失败，回滚..."
    sudo systemctl stop sism-backend
    mv /opt/sism/sism-backend.jar.backup /opt/sism/sism-backend.jar
    sudo systemctl start sism-backend
    exit 1
fi
```

## 故障排查

### 常见问题

#### 1. 应用无法启动

**检查步骤**:
```bash
# 查看服务状态
sudo systemctl status sism-backend

# 查看日志
sudo journalctl -u sism-backend -n 100

# 检查配置文件
cat /opt/sism/.env

# 检查端口占用
sudo netstat -tlnp | grep 8080

# 检查 Java 进程
ps aux | grep java
```

**常见原因**:
- 配置文件错误
- 端口被占用
- 数据库连接失败
- 内存不足

#### 2. 数据库连接失败

**检查步骤**:
```bash
# 测试数据库连接
psql -h localhost -U sism_prod -d strategic_prod

# 检查数据库服务
sudo systemctl status postgresql

# 查看数据库日志
sudo tail -f /var/log/postgresql/postgresql-*.log

# 检查防火墙
sudo ufw status
```

#### 3. 内存溢出

**检查步骤**:
```bash
# 查看内存使用
free -h

# 查看 Java 进程内存
ps aux | grep java

# 查看 JVM 堆内存
jmap -heap <pid>
```

**解决方案**:
```bash
# 调整 JVM 参数
sudo nano /etc/systemd/system/sism-backend.service

# 修改 ExecStart
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar /opt/sism/sism-backend.jar
```

#### 4. 性能问题

**检查步骤**:
```bash
# 查看 CPU 使用
top

# 查看线程数
ps -eLf | grep java | wc -l

# 查看数据库连接
psql -U sism_prod -d strategic_prod -c "SELECT count(*) FROM pg_stat_activity;"

# 查看慢查询
psql -U sism_prod -d strategic_prod -c "SELECT * FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;"
```

## 安全加固

### 1. 系统安全

```bash
# 配置防火墙
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable

# 禁用 root 登录
sudo nano /etc/ssh/sshd_config
# PermitRootLogin no

# 配置 fail2ban
sudo apt install fail2ban
sudo systemctl enable fail2ban
```

### 2. 应用安全

- 使用强密钥和密码
- 定期更换密钥
- 启用 HTTPS
- 配置 CORS 白名单
- 启用频率限制
- 禁用 Swagger UI
- 定期更新依赖

### 3. 数据库安全

- 使用专用数据库用户
- 限制数据库访问
- 启用 SSL 连接
- 定期备份
- 监控异常访问

## 相关文档

- [配置指南](configuration-guide.md)
- [数据库连接池配置](database-connection-pool-configuration.md)
- [API 文档](IndicatorController-API文档.md)

## 支持

如有问题，请联系：
- 技术支持: support@yourdomain.com
- 紧急联系: emergency@yourdomain.com
