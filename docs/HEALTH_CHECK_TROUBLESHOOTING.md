# Health Check 响应慢问题排查指南

## 问题描述

执行 `curl http://localhost:8080/api/actuator/health` 响应缓慢（超过 1-2 秒）。

## 常见原因

### 1. 数据库连接问题 ⭐ 最常见

**症状：** Health 端点需要 5-30 秒才返回

**原因：**
- PostgreSQL 服务未启动
- 数据库连接配置错误
- 网络延迟（连接远程数据库）
- 连接池耗尽

**排查步骤：**

```bash
# 1. 检查 PostgreSQL 是否运行
psql -U postgres -c "SELECT version();"

# 2. 测试数据库连接
psql -U postgres -d strategic -c "SELECT 1;"

# 3. 查看连接池状态（在应用日志中）
# 搜索关键词: HikariPool, connection
```

**解决方案：**

```yaml
# application.yml - 缩短超时时间
spring:
  datasource:
    hikari:
      connection-timeout: 5000      # 5 秒（默认 30 秒）
      validation-timeout: 3000      # 3 秒
      connection-test-query: SELECT 1
```

---

### 2. DNS 解析问题

**症状：** 使用 `localhost` 慢，但 `127.0.0.1` 快

**原因：** 系统优先尝试 IPv6 (`::1`)，超时后才回退到 IPv4

**测试：**

```bash
# 对比这两个命令的速度
curl http://localhost:8080/api/actuator/health
curl http://127.0.0.1:8080/api/actuator/health
```

**解决方案：**

1. **临时方案：** 使用 `127.0.0.1` 代替 `localhost`

2. **永久方案：** 修改 `C:\Windows\System32\drivers\etc\hosts`

```
127.0.0.1       localhost
::1             localhost
```

3. **应用层方案：** 强制使用 IPv4

```yaml
# application.yml
server:
  address: 0.0.0.0  # 或 127.0.0.1
```

或启动时添加 JVM 参数：

```bash
java -Djava.net.preferIPv4Stack=true -jar sism-backend.jar
```

---

### 3. 不必要的健康检查组件

**症状：** 日志中出现 Mail、Redis、RabbitMQ 等组件的连接错误

**原因：** Spring Boot 自动配置了未使用的健康检查

**排查：**

```bash
# 查看详细健康信息
curl http://localhost:8080/api/actuator/health | jq .
```

查找 `"status": "DOWN"` 或响应时间长的组件。

**解决方案：**

```yaml
# application.yml
management:
  health:
    mail:
      enabled: false
    rabbit:
      enabled: false
    redis:
      enabled: false
    mongo:
      enabled: false
    elasticsearch:
      enabled: false
```

---

### 4. 代理配置干扰

**症状：** 终端设置了代理后变慢

**排查：**

```bash
# 检查代理环境变量
echo %HTTP_PROXY%
echo %HTTPS_PROXY%
```

**解决方案：**

```bash
# 临时禁用代理
set HTTP_PROXY=
set HTTPS_PROXY=

# 或使用 --noproxy 参数
curl --noproxy "*" http://localhost:8080/api/actuator/health
```

---

### 5. 应用处于调试模式

**症状：** IDE 调试模式下响应极慢

**原因：** 断点、方法断点、异常断点拦截了请求

**解决方案：**
1. 检查 IDE 断点列表（IntelliJ: Ctrl+Shift+F8）
2. 点击 "Mute Breakpoints" 禁用所有断点
3. 移除 "Method Breakpoints"（性能杀手）

---

### 6. 磁盘 I/O 问题

**症状：** `diskSpace` 健康检查慢

**原因：** 磁盘 I/O 繁忙或网络驱动器

**解决方案：**

```yaml
# application.yml
management:
  health:
    diskspace:
      enabled: false  # 或设置合理阈值
      threshold: 10MB
```

---

## 快速诊断工具

### 方法 1: 使用诊断脚本

```bash
cd sism-backend
diagnose-health.bat
```

### 方法 2: 手动测试

```bash
# 1. 测试整体响应时间
curl -w "\n响应时间: %{time_total}s\n" http://127.0.0.1:8080/api/actuator/health

# 2. 查看详细信息
curl http://127.0.0.1:8080/api/actuator/health | jq .

# 3. 单独测试数据库
curl http://127.0.0.1:8080/api/actuator/health/db

# 4. 测试 Liveness（不检查外部依赖）
curl http://127.0.0.1:8080/api/actuator/health/liveness
```

### 方法 3: 启用调试日志

```yaml
# application-dev.yml
logging:
  level:
    org.springframework.boot.actuate: DEBUG
    org.springframework.boot.actuate.health: TRACE
```

重启应用后查看日志，找出慢的组件。

---

## 优化建议

### 1. 生产环境配置

```yaml
management:
  endpoint:
    health:
      show-details: when-authorized  # 不对外暴露详情
  health:
    db:
      enabled: true
    diskspace:
      enabled: true
      threshold: 100MB
    # 其他组件按需启用
```

### 2. 开发环境快速配置

使用 `application-fast-health.yml` 配置：

```bash
mvnw spring-boot:run -Dspring-boot.run.profiles=dev,fast-health
```

### 3. 使用 Liveness/Readiness 探针

```bash
# Liveness: 应用是否存活（不检查外部依赖）
curl http://localhost:8080/api/actuator/health/liveness

# Readiness: 应用是否就绪（检查所有依赖）
curl http://localhost:8080/api/actuator/health/readiness
```

Kubernetes 部署时推荐使用：
- Liveness Probe → `/api/actuator/health/liveness`
- Readiness Probe → `/api/actuator/health/readiness`

---

## 已应用的优化

✅ 缩短数据库连接超时时间（30s → 5s）  
✅ 添加连接验证超时（3s）  
✅ 禁用未使用的健康检查组件（Mail, Redis, RabbitMQ 等）  
✅ 添加快速验证查询 `SELECT 1`  
✅ 提供诊断脚本和快速配置文件  

---

## 测试验证

```bash
# 1. 重启应用
cd sism-backend
mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 2. 等待启动完成（看到 "Started SismApplication"）

# 3. 测试响应时间（应该 < 1 秒）
curl -w "\n响应时间: %{time_total}s\n" http://127.0.0.1:8080/api/actuator/health

# 4. 如果还是慢，运行诊断脚本
diagnose-health.bat
```

---

## 参考资料

- [Spring Boot Actuator 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [HikariCP 配置说明](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [Health Endpoint 配置](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.endpoints.health)
