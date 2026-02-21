# Redis配置指南

## 概述

本文档说明如何配置和使用Redis作为SISM系统的分布式缓存和状态管理服务。

Redis集成是架构重构的第一阶段(P0),用于替换内存服务,实现系统的横向扩展能力。

## 功能用途

Redis在SISM系统中用于以下场景:

1. **分布式限流**: 替换InMemoryRateLimiter,实现跨实例的API限流
2. **Token黑名单**: 替换内存Token黑名单,实现分布式Token失效管理
3. **幂等性服务**: 替换内存幂等性记录,防止重复操作
4. **分布式缓存**: 缓存热点数据,减少数据库压力

## 配置说明

### 环境变量

Redis配置通过以下环境变量控制:

| 环境变量 | 说明 | 默认值 | 必需 |
|---------|------|--------|------|
| `REDIS_ENABLED` | 是否启用Redis | `false` | 是 |
| `REDIS_HOST` | Redis服务器地址 | `localhost` | 是 |
| `REDIS_PORT` | Redis服务器端口 | `6379` | 否 |
| `REDIS_PASSWORD` | Redis密码 | 空 | 生产环境必需 |
| `REDIS_DATABASE` | Redis数据库索引 | `0` | 否 |
| `REDIS_TIMEOUT` | 连接超时时间(毫秒) | `5000` | 否 |
| `REDIS_POOL_MAX_ACTIVE` | 连接池最大连接数 | `8` (开发), `20` (生产) | 否 |
| `REDIS_POOL_MAX_IDLE` | 连接池最大空闲连接数 | `8` (开发), `10` (生产) | 否 |
| `REDIS_POOL_MIN_IDLE` | 连接池最小空闲连接数 | `0` (开发), `5` (生产) | 否 |
| `REDIS_POOL_MAX_WAIT` | 连接池最大等待时间(毫秒) | `-1` (开发), `3000` (生产) | 否 |

### 开发环境配置

在开发环境中,可以使用本地Redis实例进行测试:

```bash
# 启动本地Redis (使用Docker)
docker run -d --name redis-dev -p 6379:6379 redis:7-alpine

# 设置环境变量
export REDIS_ENABLED=true
export REDIS_HOST=localhost
export REDIS_PORT=6379

# 启动应用
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 生产环境配置

生产环境必须配置Redis密码和适当的连接池参数:

```bash
# 设置环境变量
export REDIS_ENABLED=true
export REDIS_HOST=your-redis-server.com
export REDIS_PORT=6379
export REDIS_PASSWORD=your-secure-password
export REDIS_DATABASE=0
export REDIS_POOL_MAX_ACTIVE=20
export REDIS_POOL_MAX_IDLE=10
export REDIS_POOL_MIN_IDLE=5

# 启动应用
java -jar sism-backend.jar --spring.profiles.active=prod
```

## 配置文件

### application.yml

基础配置,默认禁用Redis:

```yaml
spring:
  data:
    redis:
      enabled: ${REDIS_ENABLED:false}
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
      timeout: ${REDIS_TIMEOUT:5000}
      lettuce:
        pool:
          max-active: ${REDIS_POOL_MAX_ACTIVE:8}
          max-idle: ${REDIS_POOL_MAX_IDLE:8}
          min-idle: ${REDIS_POOL_MIN_IDLE:0}
          max-wait: ${REDIS_POOL_MAX_WAIT:-1}
```

### application-dev.yml

开发环境配置,可以启用Redis进行测试:

```yaml
spring:
  data:
    redis:
      enabled: ${REDIS_ENABLED:false}
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
```

### application-prod.yml

生产环境配置,必须通过环境变量配置:

```yaml
spring:
  data:
    redis:
      enabled: ${REDIS_ENABLED:false}
      host: ${REDIS_HOST}  # 必须配置
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}  # 必须配置
      database: ${REDIS_DATABASE:0}
      timeout: ${REDIS_TIMEOUT:3000}
      lettuce:
        pool:
          max-active: ${REDIS_POOL_MAX_ACTIVE:20}
          max-idle: ${REDIS_POOL_MAX_IDLE:10}
          min-idle: ${REDIS_POOL_MIN_IDLE:5}
          max-wait: ${REDIS_POOL_MAX_WAIT:3000}
```

## Redis配置类

`RedisConfig.java`提供了以下功能:

1. **连接工厂**: 配置Lettuce客户端和连接池
2. **RedisTemplate**: 配置序列化器(Key使用String,Value使用Jackson2Json)
3. **连接验证**: 启动时验证Redis连接可用性
4. **条件启用**: 通过`@ConditionalOnProperty`控制是否启用

## 健康检查

Redis健康检查集成到Spring Boot Actuator:

```bash
# 检查应用健康状态
curl http://localhost:8080/api/actuator/health

# 响应示例
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0.0"
      }
    }
  }
}
```

## 数据结构

Redis中存储的数据结构:

### 限流计数器

```
Key: rate_limit:{identifier}
Value: Integer (请求计数)
TTL: 根据时间窗口设置 (如60秒)

示例:
rate_limit:user:123 = 45
TTL: 60秒
```

### Token黑名单

```
Key: token_blacklist:{token}
Value: Boolean (true表示已失效)
TTL: Token过期时间

示例:
token_blacklist:eyJhbGc... = true
TTL: 3600秒
```

### 幂等性记录

```
Key: idempotency:{idempotencyKey}
Value: Boolean (true表示已处理)
TTL: 24小时

示例:
idempotency:create_indicator_abc123 = true
TTL: 86400秒
```

## 故障处理

### Redis连接失败

当Redis连接失败时,系统会:

1. 记录详细错误日志
2. 降级到内存实现(InMemoryRateLimiter, InMemoryTokenBlacklist)
3. 返回降级警告
4. 定期重试Redis连接

### 监控指标

系统记录以下Redis相关指标:

- `redis.connection.active`: 活跃连接数
- `redis.connection.idle`: 空闲连接数
- `redis.operation.duration`: 操作耗时
- `rate_limiter.fallback`: 降级次数

## 性能优化

### 连接池配置建议

根据应用负载调整连接池参数:

**低负载** (< 100 QPS):
```yaml
max-active: 8
max-idle: 4
min-idle: 2
```

**中负载** (100-500 QPS):
```yaml
max-active: 20
max-idle: 10
min-idle: 5
```

**高负载** (> 500 QPS):
```yaml
max-active: 50
max-idle: 20
min-idle: 10
```

### 超时配置建议

- **开发环境**: `timeout: 5000` (5秒)
- **生产环境**: `timeout: 3000` (3秒)
- **高可用环境**: `timeout: 1000` (1秒)

## 安全建议

1. **生产环境必须配置密码**: 使用强密码保护Redis
2. **使用专用数据库**: 为SISM分配独立的Redis数据库索引
3. **限制网络访问**: 配置防火墙规则,只允许应用服务器访问Redis
4. **启用TLS**: 生产环境建议启用Redis TLS加密
5. **定期备份**: 配置Redis持久化(RDB或AOF)

## 故障排查

### 连接失败

```bash
# 检查Redis是否运行
redis-cli ping

# 检查网络连接
telnet redis-host 6379

# 检查应用日志
tail -f /var/log/sism/sism-prod.log | grep Redis
```

### 性能问题

```bash
# 监控Redis性能
redis-cli --stat

# 查看慢查询
redis-cli slowlog get 10

# 检查内存使用
redis-cli info memory
```

### 连接池耗尽

```bash
# 检查连接池状态
curl http://localhost:8080/api/actuator/metrics/redis.connection.active

# 增加连接池大小
export REDIS_POOL_MAX_ACTIVE=50
```

## 迁移计划

从内存服务迁移到Redis的步骤:

1. **阶段1**: 部署Redis服务器
2. **阶段2**: 配置应用连接Redis (REDIS_ENABLED=true)
3. **阶段3**: 验证Redis功能正常
4. **阶段4**: 监控性能指标
5. **阶段5**: 移除内存服务实现

## 参考资料

- [Spring Data Redis官方文档](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [Lettuce客户端文档](https://lettuce.io/core/release/reference/)
- [Redis官方文档](https://redis.io/documentation)
- [架构重构设计文档](../.kiro/specs/architecture-refactoring/design.md)

## 联系支持

如有问题,请联系:
- 技术支持: tech-support@sism.com
- 架构团队: architecture@sism.com
