# Docker 内存配置指南

## 1. 当前 Dockerfile 分析

### 1.1 现有配置

当前 Dockerfile 位于项目根目录 `/sism-backend/Dockerfile`，关键配置如下：

```dockerfile
FROM eclipse-temurin:17-jre-jammy AS runtime

# 入口脚本直接启动 JVM，无任何内存参数
ENTRYPOINT ["/app/backend-entrypoint.sh"]
```

入口脚本 `docker/backend-entrypoint.sh` 中的启动命令：

```bash
exec java org.springframework.boot.loader.launch.JarLauncher \
    --spring.profiles.active="${SPRING_PROFILES_ACTIVE}"
```

### 1.2 发现的问题

| 问题 | 严重程度 | 说明 |
|------|----------|------|
| **未设置 JVM 堆内存参数** | 高 | 没有 `-Xms` / `-Xmx` 参数，JVM 将使用默认值（物理内存的 1/4），在容器环境中可能导致 OOM |
| **未设置容器内存限制** | 高 | 没有 `--memory` 或 `MEM_LIMIT` 环境变量，容器可使用宿主机全部内存 |
| **未配置元空间(MaxMetaspaceSize)** | 中 | 默认无限制，可能导致容器内存溢出 |
| **未启用容器感知** | 低 | eclipse-temurin:17 默认已支持容器感知（UseContainerSupport=true），但显式配置更安全 |
| **缺少 GC 日志配置** | 低 | 生产环境应配置 GC 日志以便排查内存问题 |

## 2. 科学的内存配比建议

### 2.1 容器内存构成

一个 Java 容器的内存不仅仅只有 JVM 堆。完整的内存构成如下：

```
容器内存限制 = JVM 堆 + 元空间 + 线程栈 + 本地内存(NIO/GC/代码缓存) + 操作系统开销
```

各部分典型占比：

| 内存区域 | 占容器内存比例 | 说明 |
|----------|---------------|------|
| JVM 堆 (-Xmx) | 70-80% | 主要的数据存储区域 |
| 元空间 (MaxMetaspaceSize) | 5-8% | 类元数据，Spring Boot 应用通常需要 128-256MB |
| 线程栈 | 3-5% | 每线程 1MB，200 线程约 200MB |
| 本地内存 (NIO/代码缓存) | 5-10% | DirectByteBuffer、JIT 编译缓存 |
| 操作系统/容器开销 | 2-5% | cgroup、文件系统缓存等 |

### 2.2 推荐配置方案

#### 方案 A：生产环境（推荐）

适用场景：500 并发用户，中等业务量

| 参数 | 值 | 说明 |
|------|-----|------|
| 容器内存限制 | 2GB | `--memory=2g` |
| JVM 堆 (-Xms/-Xmx) | 1400MB | 约占容器的 70% |
| 元空间 | 256MB | `MaxMetaspaceSize=256m` |
| 线程栈 | 512KB | `-Xss512k`（降低单线程内存占用） |
| 预留 | ~290MB | 给本地内存和 OS |

```bash
JAVA_OPTS="-Xms1400m -Xmx1400m -XX:MaxMetaspaceSize=256m -Xss512k"
```

#### 方案 B：高流量环境

适用场景：1000+ 并发用户，大业务量

| 参数 | 值 | 说明 |
|------|-----|------|
| 容器内存限制 | 4GB | `--memory=4g` |
| JVM 堆 | 2800MB | 约占容器的 70% |
| 元空间 | 384MB | |
| 预留 | ~800MB | |

```bash
JAVA_OPTS="-Xms2800m -Xmx2800m -XX:MaxMetaspaceSize=384m -Xss512k"
```

#### 方案 C：开发/测试环境

| 参数 | 值 | 说明 |
|------|-----|------|
| 容器内存限制 | 1GB | |
| JVM 堆 | 700MB | |
| 元空间 | 128MB | |

```bash
JAVA_OPTS="-Xms700m -Xmx700m -XX:MaxMetaspaceSize=128m"
```

### 2.3 配比公式

```
推荐堆大小 = 容器内存限制 * 0.70 ~ 0.75

示例：
  2GB 容器 -> 1400MB ~ 1500MB 堆
  4GB 容器 -> 2800MB ~ 3000MB 堆
  8GB 容器 -> 5600MB ~ 6000MB 堆
```

**关键原则：**
- `-Xms` 和 `-Xmx` 设为相同值，避免运行时堆扩缩带来的性能开销
- 堆占容器限制的 70-75%，不超过 80%
- 预留至少 250MB 给非堆内存和操作系统

## 3. 推荐的 Dockerfile 改进

### 3.1 改进后的 Dockerfile

```dockerfile
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY sism-shared-kernel/pom.xml sism-shared-kernel/pom.xml
COPY sism-iam/pom.xml sism-iam/pom.xml
COPY sism-organization/pom.xml sism-organization/pom.xml
COPY sism-strategy/pom.xml sism-strategy/pom.xml
COPY sism-task/pom.xml sism-task/pom.xml
COPY sism-workflow/pom.xml sism-workflow/pom.xml
COPY sism-execution/pom.xml sism-execution/pom.xml
COPY sism-analytics/pom.xml sism-analytics/pom.xml
COPY sism-alert/pom.xml sism-alert/pom.xml
COPY sism-main/pom.xml sism-main/pom.xml

RUN mvn -B -pl sism-main -am dependency:go-offline

COPY . .

RUN mvn -B -pl sism-main -am package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
RUN java -Djarmode=layertools -jar /workspace/sism-main/target/sism-main-1.0.0.jar extract --destination /workspace/layers

FROM eclipse-temurin:17-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl postgresql-client \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /workspace/layers/dependencies/ ./
COPY --from=build /workspace/layers/spring-boot-loader/ ./
COPY --from=build /workspace/layers/snapshot-dependencies/ ./
COPY --from=build /workspace/layers/application/ ./
COPY docker/backend-entrypoint.sh /app/backend-entrypoint.sh

RUN chmod +x /app/backend-entrypoint.sh

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=10 \
  CMD curl -fsS http://localhost:8080/api/v1/actuator/health || exit 1

ENTRYPOINT ["/app/backend-entrypoint.sh"]
```

### 3.2 改进后的 entrypoint 脚本

```bash
#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${DB_HOST:-postgres}"
DB_PORT="${DB_PORT:-5432}"
DB_URL="${DB_URL:?DB_URL is required}"
DB_USERNAME="${DB_USERNAME:?DB_USERNAME is required}"
DB_PASSWORD="${DB_PASSWORD:?DB_PASSWORD is required}"
JWT_SECRET="${JWT_SECRET:?JWT_SECRET is required}"
ALLOWED_ORIGINS="${ALLOWED_ORIGINS:?ALLOWED_ORIGINS is required}"

SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"

# ========== JVM 内存配置 ==========
# 可通过环境变量覆盖默认值
JAVA_HEAP_SIZE="${JAVA_HEAP_SIZE:-1400m}"
JAVA_METASPACE_SIZE="${JAVA_METASPACE_SIZE:-256m}"
JAVA_STACK_SIZE="${JAVA_STACK_SIZE:-512k}"

# 构建 JVM 选项
JAVA_OPTS="${JAVA_OPTS:-}"
JAVA_OPTS="${JAVA_OPTS} -Xms${JAVA_HEAP_SIZE} -Xmx${JAVA_HEAP_SIZE}"
JAVA_OPTS="${JAVA_OPTS} -XX:MaxMetaspaceSize=${JAVA_METASPACE_SIZE}"
JAVA_OPTS="${JAVA_OPTS} -Xss${JAVA_STACK_SIZE}"

# 容器感知（eclipse-temurin:17 默认开启，显式确认）
JAVA_OPTS="${JAVA_OPTS} -XX:+UseContainerSupport"

# GC 配置 - G1GC 适合中等堆大小
JAVA_OPTS="${JAVA_OPTS} -XX:+UseG1GC"
JAVA_OPTS="${JAVA_OPTS} -XX:MaxGCPauseMillis=200"
JAVA_OPTS="${JAVA_OPTS} -XX:G1HeapRegionSize=4m"

# GC 日志（生产环境排查问题用）
JAVA_OPTS="${JAVA_OPTS} -Xlog:gc*:file=/app/logs/gc.log:time,uptime,level,tags:filecount=5,filesize=10m"

# OOM 时自动 dump 堆（方便事后分析）
JAVA_OPTS="${JAVA_OPTS} -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPTS="${JAVA_OPTS} -XX:HeapDumpPath=/app/logs/heapdump.hprof"

# Spring Boot 特定配置
JAVA_OPTS="${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom"

echo "Waiting for PostgreSQL at ${DB_HOST}:${DB_PORT}..."
until pg_isready -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USERNAME}" >/dev/null 2>&1; do
  sleep 2
done

echo "Starting JVM with options: ${JAVA_OPTS}"
exec java ${JAVA_OPTS} org.springframework.boot.loader.launch.JarLauncher \
    --spring.profiles.active="${SPRING_PROFILES_ACTIVE}"
```

## 4. Docker Compose / Kubernetes 内存限制

### 4.1 Docker Compose 配置示例

```yaml
version: '3.8'
services:
  sism-backend:
    image: sism-backend:latest
    deploy:
      resources:
        limits:
          memory: 2G      # 容器内存上限
        reservations:
          memory: 1G      # 预留内存
    environment:
      - JAVA_HEAP_SIZE=1400m
      - JAVA_METASPACE_SIZE=256m
      - JAVA_STACK_SIZE=512k
    ports:
      - "8080:8080"
```

### 4.2 Kubernetes 配置示例

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
        - name: sism-backend
          resources:
            limits:
              memory: "2Gi"
              cpu: "2"
            requests:
              memory: "1Gi"
              cpu: "500m"
          env:
            - name: JAVA_HEAP_SIZE
              value: "1400m"
            - name: JAVA_METASPACE_SIZE
              value: "256m"
```

## 5. 内存监控建议

### 5.1 关键指标

| 指标 | 监控方式 | 告警阈值 |
|------|----------|----------|
| 容器内存使用率 | cAdvisor / Prometheus | > 85% |
| JVM 堆使用率 | JMX / Actuator | > 80% |
| GC 停顿时间 | GC 日志 / JMX | > 500ms |
| GC 频率 | GC 日志 | Full GC > 1次/小时 |
| 元空间使用率 | JMX | > 80% of MaxMetaspaceSize |

### 5.2 Actuator 端点

```bash
# 堆内存详情
curl http://localhost:8080/api/v1/actuator/metrics/jvm.memory.max
curl http://localhost:8080/api/v1/actuator/metrics/jvm.memory.used

# GC 详情
curl http://localhost:8080/api/v1/actuator/metrics/jvm.gc.pause

# 线程数
curl http://localhost:8080/api/v1/actuator/metrics/jvm.threads.live
```

### 5.3 常见 OOM 场景排查

| 场景 | 特征 | 解决方案 |
|------|------|----------|
| 堆溢出 | `java.lang.OutOfMemoryError: Java heap space` | 增大 `-Xmx` 或排查内存泄漏 |
| 元空间溢出 | `java.lang.OutOfMemoryError: Metaspace` | 增大 `MaxMetaspaceSize` |
| 线程溢出 | `java.lang.OutOfMemoryError: unable to create native thread` | 减小 `-Xss` 或增大容器内存 |
| 容器被杀 | Exit Code 137 (OOMKilled) | 增大容器内存限制 |

## 6. 总结

| 环境 | 容器内存 | 堆大小 | 元空间 | 预留比例 |
|------|----------|--------|--------|----------|
| 开发/测试 | 1GB | 700MB | 128MB | ~17% |
| 生产（默认） | 2GB | 1400MB | 256MB | ~17% |
| 生产（高流量） | 4GB | 2800MB | 384MB | ~20% |

**核心原则：**
1. 堆大小 = 容器限制 * 70-75%
2. `-Xms` 必须等于 `-Xmx`，避免动态扩缩
3. 必须设置 `MaxMetaspaceSize`，防止元空间无限增长
4. 生产环境必须配置 GC 日志和 OOM Dump
5. 容器必须设置 `--memory` 限制，防止影响宿主机其他服务
