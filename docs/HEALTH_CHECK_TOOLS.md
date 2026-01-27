# Health Check 工具使用指南

## 工具列表

### 1. 诊断脚本 (`diagnose-health.bat`)

**用途：** 快速定位 Health 端点响应慢的具体原因

**使用方法：**

```bash
cd sism-backend
diagnose-health.bat
```

**输出内容：**
- 基本连通性测试
- 详细健康信息（JSON 格式）
- 数据库健康检查
- Liveness 探针测试
- Readiness 探针测试

**适用场景：**
- 首次遇到响应慢问题
- 需要了解哪个组件导致慢
- 排查生产环境问题

---

### 2. 性能测试脚本 (`test-health-performance.bat`)

**用途：** 对比优化前后的性能差异

**使用方法：**

```bash
cd sism-backend
test-health-performance.bat
```

**测试内容：**
- 使用 `127.0.0.1` 的响应时间（5 次）
- 使用 `localhost` 的响应时间（3 次）
- Liveness 探针响应时间（3 次）

**性能标准：**
- 优秀: < 0.5 秒
- 良好: 0.5 - 1.0 秒
- 一般: 1.0 - 2.0 秒
- 较慢: > 2.0 秒

**适用场景：**
- 验证优化效果
- 性能回归测试
- 对比不同配置的性能

---

### 3. 快速健康检查配置 (`application-fast-health.yml`)

**用途：** 提供最小化的健康检查配置，用于快速启动和测试

**使用方法：**

```bash
# 启动时激活 fast-health profile
mvnw spring-boot:run -Dspring-boot.run.profiles=dev,fast-health

# 或在 IDE 中设置 Active Profiles: dev,fast-health
```

**配置特点：**
- 数据库连接超时 2 秒
- 验证超时 1 秒
- 禁用磁盘空间检查
- 只保留核心健康检查

**适用场景：**
- 开发环境快速启动
- 单元测试
- CI/CD 流水线

---

## 常用命令速查

### 基本测试

```bash
# 测试 Health 端点（显示响应时间）
curl -w "\n响应时间: %{time_total}s\n" http://127.0.0.1:8080/api/actuator/health

# 查看详细健康信息（需要 jq 工具）
curl -s http://127.0.0.1:8080/api/actuator/health | jq .

# 不使用 jq 的版本
curl -s http://127.0.0.1:8080/api/actuator/health
```

### 单独测试各组件

```bash
# 数据库健康检查
curl http://127.0.0.1:8080/api/actuator/health/db

# Liveness 探针（不检查外部依赖）
curl http://127.0.0.1:8080/api/actuator/health/liveness

# Readiness 探针（检查所有依赖）
curl http://127.0.0.1:8080/api/actuator/health/readiness

# 磁盘空间检查
curl http://127.0.0.1:8080/api/actuator/health/diskSpace
```

### 使用 PowerShell

```powershell
# 测试响应时间
Measure-Command { Invoke-WebRequest -Uri "http://127.0.0.1:8080/api/actuator/health" -UseBasicParsing }

# 查看详细信息
(Invoke-WebRequest -Uri "http://127.0.0.1:8080/api/actuator/health").Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

---

## 故障排查流程

### 步骤 1: 运行诊断脚本

```bash
diagnose-health.bat
```

观察输出，找出响应时间最长的组件。

### 步骤 2: 根据结果采取行动

#### 如果数据库检查慢：

```bash
# 检查 PostgreSQL 是否运行
psql -U postgres -c "SELECT version();"

# 测试数据库连接
psql -U postgres -d strategic -c "SELECT 1;"

# 查看 .env 配置
type .env
```

#### 如果 localhost 慢但 127.0.0.1 快：

DNS 解析问题，使用 `127.0.0.1` 或修复 hosts 文件。

#### 如果整体都慢：

检查代理设置、防火墙、杀毒软件。

### 步骤 3: 应用优化配置

已在 `application.yml` 中应用的优化：
- ✅ 缩短数据库连接超时
- ✅ 禁用未使用的健康检查
- ✅ 添加快速验证查询

### 步骤 4: 验证优化效果

```bash
# 重启应用
mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 运行性能测试
test-health-performance.bat
```

---

## 生产环境建议

### 1. 使用专用的健康检查端点

```yaml
# application-prod.yml
management:
  endpoint:
    health:
      show-details: when-authorized  # 不对外暴露详情
  server:
    port: 9090  # 使用独立端口
```

### 2. 配置合理的超时时间

```yaml
spring:
  datasource:
    hikari:
      connection-timeout: 5000
      validation-timeout: 3000
```

### 3. 监控健康检查性能

使用 Prometheus + Grafana 监控：
- `http_server_requests_seconds{uri="/api/actuator/health"}`
- 设置告警阈值（如 > 2 秒）

### 4. 使用 Kubernetes 探针

```yaml
# deployment.yaml
livenessProbe:
  httpGet:
    path: /api/actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5

readinessProbe:
  httpGet:
    path: /api/actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  timeoutSeconds: 3
```

---

## 参考文档

- [完整排查指南](./HEALTH_CHECK_TROUBLESHOOTING.md)
- [Spring Boot Actuator 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [HikariCP 配置](https://github.com/brettwooldridge/HikariCP)

---

## 常见问题 FAQ

**Q: 为什么 Liveness 探针比 Health 端点快？**

A: Liveness 只检查应用是否存活，不检查外部依赖（如数据库）。Health 端点会检查所有配置的组件。

**Q: 生产环境应该使用哪个端点？**

A: 
- 负载均衡器健康检查 → `/actuator/health/liveness`
- Kubernetes Liveness Probe → `/actuator/health/liveness`
- Kubernetes Readiness Probe → `/actuator/health/readiness`
- 监控系统 → `/actuator/health`（需要详细信息）

**Q: 如何完全禁用某个健康检查？**

A: 在 `application.yml` 中设置：

```yaml
management:
  health:
    <component>:
      enabled: false
```

**Q: 响应时间多少算正常？**

A: 
- 开发环境: < 1 秒
- 生产环境: < 500 毫秒
- Kubernetes 探针: < 200 毫秒（建议）
