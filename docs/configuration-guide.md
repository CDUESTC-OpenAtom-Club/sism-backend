# SISM Backend 配置指南

## 目录

1. [快速开始](#快速开始)
2. [必需配置](#必需配置)
3. [可选配置](#可选配置)
4. [环境特定配置](#环境特定配置)
5. [配置验证](#配置验证)
6. [常见问题](#常见问题)
7. [最佳实践](#最佳实践)

## 快速开始

### 1. 复制配置模板

```bash
cd sism-backend
cp .env.example .env
```

### 2. 编辑必需配置

打开 `.env` 文件，至少需要配置以下变量：

```bash
# 生成 JWT 密钥
JWT_SECRET=$(openssl rand -base64 32)

# 配置数据库连接
DB_URL=jdbc:postgresql://localhost:5432/strategic?stringtype=unspecified
DB_USERNAME=postgres
DB_PASSWORD=your_password_here
```

### 3. 启动应用

```bash
mvn spring-boot:run
```

### 4. 验证启动

访问健康检查端点：
```bash
curl http://localhost:8080/api/actuator/health
```

预期响应：
```json
{
  "status": "UP"
}
```

## 必需配置

以下配置项是必需的，应用启动时会验证这些配置是否存在。

### JWT_SECRET

**用途**: JWT token 签名密钥

**要求**:
- 至少 256 位（32 字符）
- 使用强随机字符串
- 不同环境使用不同密钥

**生成方法**:
```bash
# 使用 OpenSSL 生成
openssl rand -base64 32

# 使用 Python 生成
python3 -c "import secrets; print(secrets.token_urlsafe(32))"

# 使用 Node.js 生成
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
```

**示例**:
```bash
JWT_SECRET=xK8vN2mP9qR4sT6uW1yZ3aB5cD7eF0gH2iJ4kL6mN8oP
```

**安全提示**:
- ⚠️ 绝不要在代码中硬编码
- ⚠️ 绝不要提交到版本控制系统
- ⚠️ 定期更换（建议每 3-6 个月）
- ⚠️ 生产环境必须使用强随机密钥

### DB_URL

**用途**: 数据库连接 URL

**格式**:
```
jdbc:postgresql://host:port/database?stringtype=unspecified
```

**参数说明**:
- `host`: 数据库服务器地址
- `port`: 数据库端口（PostgreSQL 默认 5432）
- `database`: 数据库名称
- `stringtype=unspecified`: 允许字符串值作为 ENUM 列（必需）

**示例**:
```bash
# 本地开发
DB_URL=jdbc:postgresql://localhost:5432/strategic?stringtype=unspecified

# 远程服务器
DB_URL=jdbc:postgresql://db.example.com:5432/strategic?stringtype=unspecified

# 使用 SSL 连接
DB_URL=jdbc:postgresql://db.example.com:5432/strategic?stringtype=unspecified&ssl=true&sslmode=require
```

### DB_USERNAME 和 DB_PASSWORD

**用途**: 数据库认证凭据

**开发环境**:
```bash
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

**生产环境建议**:
- 使用专用数据库用户（不要使用 postgres 超级用户）
- 遵循最小权限原则
- 使用强密码
- 定期更换密码

**创建专用用户示例**:
```sql
-- 创建用户
CREATE USER sism_app WITH PASSWORD 'strong_password_here';

-- 授予权限
GRANT CONNECT ON DATABASE strategic TO sism_app;
GRANT USAGE ON SCHEMA public TO sism_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO sism_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO sism_app;
```

## 可选配置

以下配置项有默认值，可以根据需要覆盖。

### JWT 配置

#### JWT_EXPIRATION

**用途**: 访问 token 有效期（毫秒）

**默认值**: 900000（15 分钟）

**配置建议**:
```bash
# 开发环境 - 方便调试
JWT_EXPIRATION=86400000  # 24 小时

# 生产环境 - 更安全
JWT_EXPIRATION=900000    # 15 分钟
JWT_EXPIRATION=3600000   # 1 小时（如果用户体验要求）
```

**注意事项**:
- 时间越短越安全，但用户需要更频繁地重新登录
- 建议配合 refresh token 使用
- 移动应用可以设置较长时间

#### JWT_REFRESH_EXPIRATION

**用途**: 刷新 token 有效期（毫秒）

**默认值**: 604800000（7 天）

**配置建议**:
```bash
JWT_REFRESH_EXPIRATION=604800000   # 7 天
JWT_REFRESH_EXPIRATION=2592000000  # 30 天
```

### CORS 配置

#### CORS_ALLOWED_ORIGINS

**用途**: 允许跨域访问的前端地址

**格式**: 多个地址用逗号分隔，不要有空格

**开发环境**:
```bash
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
```

**生产环境**:
```bash
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
```

**安全提示**:
- ⚠️ 生产环境绝不要使用 `*`
- ⚠️ 必须包含协议（http:// 或 https://）
- ⚠️ 不要包含尾部斜杠
- ⚠️ 只添加信任的域名

#### 其他 CORS 配置

```bash
# 允许的 HTTP 方法
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS

# 允许的请求头
CORS_ALLOWED_HEADERS=Authorization,Content-Type,X-Requested-With

# 是否允许携带凭证
CORS_ALLOW_CREDENTIALS=true

# 预检请求缓存时间（秒）
CORS_MAX_AGE=3600
```

### 数据库连接池配置

详细配置说明请参考：[数据库连接池配置文档](database-connection-pool-configuration.md)

#### 快速配置

**开发环境**:
```bash
DB_POOL_MAX_SIZE=10
DB_POOL_MIN_IDLE=5
DB_CONNECTION_TIMEOUT=5000
DB_IDLE_TIMEOUT=600000
DB_MAX_LIFETIME=1800000
DB_LEAK_DETECTION=60000
```

**生产环境**:
```bash
DB_POOL_MAX_SIZE=20
DB_POOL_MIN_IDLE=10
DB_CONNECTION_TIMEOUT=30000
DB_IDLE_TIMEOUT=600000
DB_MAX_LIFETIME=1800000
DB_LEAK_DETECTION=0  # 禁用以提高性能
```

### 日志配置

#### 日志级别

```bash
# 根日志级别
LOG_LEVEL_ROOT=INFO

# 应用日志级别
LOG_LEVEL_APP=DEBUG

# Spring 框架日志级别
LOG_LEVEL_SPRING=INFO

# Spring Security 日志级别
LOG_LEVEL_SECURITY=INFO

# SQL 语句日志级别
LOG_LEVEL_SQL=DEBUG

# SQL 参数绑定日志级别
LOG_LEVEL_SQL_BINDER=TRACE
```

**日志级别说明**:
- `TRACE`: 最详细的日志，包含所有信息
- `DEBUG`: 调试信息，用于开发和故障排查
- `INFO`: 重要信息，记录关键操作
- `WARN`: 警告信息，可能的问题
- `ERROR`: 错误信息，需要关注
- `FATAL`: 致命错误，导致应用崩溃

**环境建议**:
```bash
# 开发环境 - 详细日志
LOG_LEVEL_ROOT=DEBUG
LOG_LEVEL_APP=DEBUG
LOG_LEVEL_SQL=DEBUG

# 生产环境 - 精简日志
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=INFO
LOG_LEVEL_SQL=INFO
LOG_LEVEL_SQL_BINDER=INFO
```

### 频率限制配置

```bash
# 启用频率限制
RATE_LIMIT_ENABLED=true

# 存储方式（memory 或 redis）
RATE_LIMIT_STORAGE=memory

# 登录接口限制
RATE_LIMIT_LOGIN_LIMIT=5
RATE_LIMIT_LOGIN_WINDOW=60

# 通用 API 限制
RATE_LIMIT_API_LIMIT=100
RATE_LIMIT_API_WINDOW=60
```

## 环境特定配置

### 开发环境 (.env.development)

```bash
# JWT 配置 - 长有效期方便调试
JWT_SECRET=dev-secret-key-change-this-in-production
JWT_EXPIRATION=86400000  # 24 小时

# 数据库配置 - 本地数据库
DB_URL=jdbc:postgresql://localhost:5432/strategic?stringtype=unspecified
DB_USERNAME=postgres
DB_PASSWORD=postgres

# CORS 配置 - 允许本地开发服务器
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000

# 日志配置 - 详细日志
LOG_LEVEL_ROOT=DEBUG
LOG_LEVEL_APP=DEBUG
LOG_LEVEL_SQL=DEBUG

# 连接池配置 - 小连接池
DB_POOL_MAX_SIZE=10
DB_POOL_MIN_IDLE=5
DB_LEAK_DETECTION=60000  # 启用泄漏检测

# Swagger - 启用
SWAGGER_ENABLED=true

# 频率限制 - 宽松限制
RATE_LIMIT_ENABLED=true
RATE_LIMIT_LOGIN_LIMIT=10
RATE_LIMIT_API_LIMIT=200
```

### 测试环境 (.env.testing)

```bash
# JWT 配置
JWT_SECRET=test-secret-key-change-this
JWT_EXPIRATION=3600000  # 1 小时

# 数据库配置 - 测试数据库
DB_URL=jdbc:postgresql://test-db.internal:5432/strategic_test?stringtype=unspecified
DB_USERNAME=sism_test
DB_PASSWORD=test_password_here

# CORS 配置 - 测试环境域名
CORS_ALLOWED_ORIGINS=https://test.yourdomain.com

# 日志配置 - 适中日志
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
LOG_LEVEL_SQL=INFO

# 连接池配置 - 中等连接池
DB_POOL_MAX_SIZE=15
DB_POOL_MIN_IDLE=8
DB_LEAK_DETECTION=30000

# Swagger - 启用
SWAGGER_ENABLED=true

# 频率限制 - 正常限制
RATE_LIMIT_ENABLED=true
RATE_LIMIT_LOGIN_LIMIT=5
RATE_LIMIT_API_LIMIT=100
```

### 生产环境 (.env.production)

```bash
# JWT 配置 - 强安全性
JWT_SECRET=<使用 openssl rand -base64 32 生成>
JWT_EXPIRATION=900000  # 15 分钟
JWT_REFRESH_EXPIRATION=604800000  # 7 天
JWT_MAX_SESSIONS=5

# 数据库配置 - 生产数据库
DB_URL=jdbc:postgresql://prod-db.internal:5432/strategic_prod?stringtype=unspecified&ssl=true&sslmode=require
DB_USERNAME=sism_prod
DB_PASSWORD=<强密码>

# CORS 配置 - 生产域名
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
CORS_ALLOW_CREDENTIALS=true

# 日志配置 - 精简日志
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=INFO
LOG_LEVEL_SPRING=WARN
LOG_LEVEL_SECURITY=INFO
LOG_LEVEL_SQL=INFO
LOG_LEVEL_SQL_BINDER=INFO
LOG_PATH=/var/log/sism

# 连接池配置 - 大连接池
DB_POOL_MAX_SIZE=30
DB_POOL_MIN_IDLE=15
DB_CONNECTION_TIMEOUT=30000
DB_IDLE_TIMEOUT=600000
DB_MAX_LIFETIME=1800000
DB_LEAK_DETECTION=0  # 禁用以提高性能

# Swagger - 禁用
SWAGGER_ENABLED=false

# 频率限制 - 严格限制
RATE_LIMIT_ENABLED=true
RATE_LIMIT_STORAGE=redis  # 使用 Redis 支持分布式
RATE_LIMIT_LOGIN_LIMIT=5
RATE_LIMIT_LOGIN_WINDOW=60
RATE_LIMIT_API_LIMIT=100
RATE_LIMIT_API_WINDOW=60

# 幂等性配置
IDEMPOTENCY_ENABLED=true
IDEMPOTENCY_TTL=300

# 环境标识
ENVIRONMENT=production
```

## 配置验证

### 启动前检查清单

#### 必需配置检查

```bash
# 检查 JWT_SECRET 是否设置
echo $JWT_SECRET | wc -c  # 应该 >= 32

# 检查数据库连接
psql -h localhost -U postgres -d strategic -c "SELECT 1"

# 检查环境变量是否加载
cat .env | grep -v "^#" | grep -v "^$"
```

#### 配置验证脚本

创建 `scripts/validate-config.sh`:

```bash
#!/bin/bash

echo "=== SISM Backend 配置验证 ==="

# 加载 .env 文件
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
else
    echo "❌ .env 文件不存在"
    exit 1
fi

# 检查必需配置
echo "检查必需配置..."

if [ -z "$JWT_SECRET" ]; then
    echo "❌ JWT_SECRET 未设置"
    exit 1
elif [ ${#JWT_SECRET} -lt 32 ]; then
    echo "❌ JWT_SECRET 长度不足 32 字符"
    exit 1
else
    echo "✅ JWT_SECRET 已设置"
fi

if [ -z "$DB_URL" ]; then
    echo "❌ DB_URL 未设置"
    exit 1
else
    echo "✅ DB_URL 已设置"
fi

if [ -z "$DB_USERNAME" ]; then
    echo "❌ DB_USERNAME 未设置"
    exit 1
else
    echo "✅ DB_USERNAME 已设置"
fi

if [ -z "$DB_PASSWORD" ]; then
    echo "❌ DB_PASSWORD 未设置"
    exit 1
else
    echo "✅ DB_PASSWORD 已设置"
fi

echo ""
echo "=== 配置验证通过 ==="
```

使用方法:
```bash
chmod +x scripts/validate-config.sh
./scripts/validate-config.sh
```

### 运行时配置检查

启动应用后，可以通过以下方式检查配置：

```bash
# 检查健康状态
curl http://localhost:8080/api/actuator/health

# 查看环境变量（敏感信息会被隐藏）
curl http://localhost:8080/api/actuator/env

# 查看配置属性
curl http://localhost:8080/api/actuator/configprops
```

## 常见问题

### Q1: 应用启动失败，提示 JWT_SECRET 未设置

**原因**: .env 文件未创建或未正确加载

**解决方案**:
```bash
# 1. 确认 .env 文件存在
ls -la .env

# 2. 检查文件内容
cat .env | grep JWT_SECRET

# 3. 确保没有语法错误（如多余的空格、引号）
# 正确: JWT_SECRET=your-secret-key
# 错误: JWT_SECRET = "your-secret-key"
```

### Q2: 数据库连接失败

**可能原因**:
1. 数据库服务未启动
2. 连接参数错误
3. 网络不通
4. 防火墙阻止

**排查步骤**:
```bash
# 1. 检查数据库服务状态
sudo systemctl status postgresql

# 2. 测试数据库连接
psql -h localhost -U postgres -d strategic

# 3. 检查防火墙
sudo ufw status

# 4. 检查 PostgreSQL 配置
cat /etc/postgresql/*/main/pg_hba.conf
```

### Q3: CORS 错误

**错误信息**: "Access to XMLHttpRequest has been blocked by CORS policy"

**解决方案**:
```bash
# 1. 检查 CORS_ALLOWED_ORIGINS 配置
echo $CORS_ALLOWED_ORIGINS

# 2. 确保包含前端地址
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000

# 3. 确保格式正确（无空格、包含协议、无尾部斜杠）
# 正确: http://localhost:5173
# 错误: http://localhost:5173/
# 错误: localhost:5173
```

### Q4: Swagger UI 无法访问

**可能原因**:
1. SWAGGER_ENABLED 设置为 false
2. 端口或路径错误

**解决方案**:
```bash
# 1. 检查 Swagger 配置
echo $SWAGGER_ENABLED

# 2. 确保设置为 true（或不设置，默认为 true）
SWAGGER_ENABLED=true

# 3. 访问正确的 URL
# http://localhost:8080/api/swagger-ui.html
```

### Q5: 连接池耗尽

**错误信息**: "Connection is not available, request timed out after 30000ms"

**原因**: 连接泄漏或连接池配置过小

**解决方案**:
```bash
# 1. 启用连接泄漏检测
DB_LEAK_DETECTION=60000

# 2. 增加连接池大小
DB_POOL_MAX_SIZE=20

# 3. 检查代码是否正确关闭连接
# 使用 try-with-resources 或 @Transactional
```

## 最佳实践

### 1. 配置文件管理

```bash
# 项目结构
sism-backend/
├── .env                    # 当前激活的配置（不提交）
├── .env.example            # 配置模板（提交）
├── .env.development        # 开发环境配置模板（提交）
├── .env.testing            # 测试环境配置模板（提交）
├── .env.production         # 生产环境配置模板（提交）
└── .gitignore              # 确保 .env 被忽略
```

**.gitignore 配置**:
```
.env
.env.local
```

### 2. 密钥管理

**开发环境**:
- 可以使用简单的密钥
- 团队共享配置文件

**生产环境**:
- 使用密钥管理服务（如 AWS Secrets Manager、HashiCorp Vault）
- 定期轮换密钥
- 限制访问权限
- 审计密钥使用

### 3. 环境隔离

**原则**:
- 不同环境使用不同的配置
- 不同环境使用不同的数据库
- 不同环境使用不同的密钥

**实现**:
```bash
# 使用环境变量切换配置
export ENV=production
cp .env.production .env

# 或使用 Spring Profiles
mvn spring-boot:run -Dspring-boot.run.profiles=production
```

### 4. 配置验证

**启动时验证**:
- 检查必需配置是否存在
- 验证配置格式是否正确
- 测试数据库连接

**运行时监控**:
- 监控配置变更
- 记录配置使用情况
- 定期审计配置

### 5. 文档维护

**保持文档更新**:
- 添加新配置时更新 .env.example
- 更新配置说明文档
- 记录配置变更历史

**提供示例**:
- 为每个配置项提供示例值
- 说明配置的用途和影响
- 提供不同环境的配置建议

## 配置优先级

配置加载顺序（后面的会覆盖前面的）:

1. `application.yml` - 默认配置
2. `application-{profile}.yml` - 环境特定配置
3. 环境变量 - 系统环境变量
4. `.env` 文件 - 本地环境变量
5. 命令行参数 - 启动参数

**示例**:
```bash
# application.yml 中设置
server.port: 8080

# .env 中覆盖
SERVER_PORT=9090

# 命令行参数最终覆盖
mvn spring-boot:run -Dserver.port=7070
```

## 相关文档

- [数据库连接池配置](database-connection-pool-configuration.md)
- [API 文档](IndicatorController-API文档.md)
- [部署指南](../README.md)

## 支持

如有问题，请联系：
- 开发团队: dev@yourdomain.com
- 技术支持: support@yourdomain.com
- 文档反馈: docs@yourdomain.com
