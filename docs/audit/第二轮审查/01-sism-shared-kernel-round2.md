# 第二轮审计报告：sism-shared-kernel（共享内核）

**审计日期:** 2026-04-13
**审计员:** Claude Code Round-2 Audit
**范围:** 66 个 Java 源文件全面复检
**参照:** 第一轮报告 `01-sism-shared-kernel.md` (2026-04-12)

---

## 修复总览

| 指标 | 数值 |
|------|------|
| 第一轮问题总数 | 68 |
| 已确认修复 | **35** (51.5%) |
| 部分修复 | **9** (13.2%) |
| 未修复 | **24** (35.3%) |
| 第二轮新发现 | **6** |

---

## A. 严重问题 (Critical) — 第一轮 11 项

### C-01. RedisConfig Jackson 反序列化 RCE ✅ 已修复
**文件:** `config/RedisConfig.java:142-143`

```java
// 修复后：使用安全默认构造函数，无 activateDefaultTyping
GenericJackson2JsonRedisSerializer jsonSerializer =
        new GenericJackson2JsonRedisSerializer();
```
**评价:** 正确。默认构造函数内部使用带 `DefaultTyping.NON_FINAL` 的安全配置，避免任意类实例化。

---

### C-02. AuditLog JPA 双重 @Id 映射 ⚠️ 功能缓解但设计不当
**文件:** `domain/model/audit/AuditLog.java:22-25`

```java
@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "log_id")
private Long logId;
```

**问题:** `AuditLog` 继承 `AggregateRoot<Long>`（内含 `@Transient protected ID id`），又自行声明 `logId` 带 `@Id`。通过第 70-77 行的桥接方法（`getId()` → `logId`）避免了 JPA 映射失败，但两个独立 ID 字段（父类 `id` transient + 子类 `logId` 持久化）令人困惑且脆弱。

**最优解:** 重构基类为 `@MappedSuperclass`，让子类声明 `@Id` 字段无需桥接：

```java
@MappedSuperclass
public abstract class Entity<ID extends Serializable> {
    // 移除 @Transient protected ID id
    public abstract ID getId();
}

@Entity
public class AuditLog extends AggregateRoot<Long> {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;  // 直接使用 id 命名，无需桥接
}
```

---

### C-03. Attachment.canPublish() 路径遍历 ✅ 已修复
**文件:** `domain/model/attachment/Attachment.java:78-80`

```java
public boolean canPublish() {
    return objectKey != null && !objectKey.isBlank();
}
```
**评价:** 不再调用 `Files.exists()`，无文件系统 I/O，路径遍历向量已移除。

---

### C-04. Entity/AggregateRoot equals/hashCode null ID ✅ 已修复
**文件:** `domain/model/base/Entity.java:68-84`

```java
// equals(): null-ID 实体互不相等
if (getId() == null || entity.getId() == null) {
    return false;
}
// hashCode(): null-ID 使用 identity hash
if (getId() == null) {
    return System.identityHashCode(this);
}
```

---

### C-05. EntityId.equals() 忽略 entityType ✅ 已修复
**文件:** `domain/model/valueobject/EntityId.java:53-64`

```java
return Objects.equals(value, entityId.value)
        && Objects.equals(entityType, entityId.entityType);
```

---

### C-06. 双异常层级 ❌ 未修复
**文件:** 两个完全独立的异常体系并存

| 层级 1 (旧) | 层级 2 (DDD) | 差异 |
|---|---|---|
| `com.sism.exception.BusinessException` | `com.sism.shared.domain.exception.BusinessException` | int code vs String code |
| `com.sism.exception.ResourceNotFoundException` | `com.sism.shared.domain.exception.ResourceNotFoundException` | 独立实现 |
| `GlobalExceptionHandler` 第 55 行处理 | 第 233 行处理 | 双重注册 |

**影响:** 每添加新异常类型需要选择阵营 + 注册新处理器。

**最优解 — 统一为 DDD 异常体系：**

```java
// 1. 保留 shared.domain.exception.BusinessException 作为唯一基类
// 2. 将 com.sism.exception.BusinessException 标记 @Deprecated
// 3. GlobalExceptionHandler 仅保留一套处理器
// 4. 逐步迁移：旧异常类继承新基类作为过渡
@Deprecated
public class LegacyBusinessException extends com.sism.shared.domain.exception.BusinessException {
    public LegacyBusinessException(int code, String message) {
        super(message, String.valueOf(code));
    }
}
```

---

### C-07. GlobalExceptionHandler 错误码不匹配 ✅ 已修复
C-08. BusinessException.message 字段遮蔽 ✅ 已修复
C-09. SysTaskMigration 每次启动运行破坏性 DDL ✅ 已修复
C-10. DatabaseDataChecker Long 自动拆箱 NPE ✅ 已修复

### C-11. TokenBlacklistService JWT 作为 Redis 键 ✅ 已修复
**文件:** `util/TokenBlacklistService.java:188-189`

```java
private String redisKey(String token) {
    return REDIS_KEY_PREFIX + sha256(token);  // SHA-256 哈希
}
```
`isBlacklisted()` 封装 try-catch + Redis 失败回退内存。本地黑名单有 `MAX_LOCAL_BLACKLIST_SIZE = 10,000` 上限。

---

## B. 高优先级问题 (High) — 第一轮 14 项

### H-01~H-06 ✅ 全部已修复
- H-01: CorsConfig 拒绝 `*` + credentials 组合
- H-02: EnvConfig.getBoolean() 使用 switch 表达式
- H-03: EnvConfigValidator 优先检查 `app.jwt.secret`
- H-04: JpaAuditConfig 处理 UserDetails/Long/String/反射
- H-05: RequestLoggingFilter 不再缓存响应体
- H-06: AggregateRoot 继承 Entity，只添加领域事件

### H-07. 聚合根 public setter 违反 DDD 封装 ⚠️ 部分修复
**文件:** `AuditFlowDef.java`, `AuditInstance.java`, `WorkflowTask.java`

```java
// 问题：外部可直接调用绕过领域方法
auditInstance.setStatus("APPROVED");  // 绕过 approve() 业务逻辑
workflowTask.setCurrentStepIndex(99);  // 绕过 moveToNextStep()
```

**AuditStepDef 仍有 `@Setter` 注解。**

**最优解 — 使用包级私有 setter + 领域门面方法：**

```java
@Entity
public class AuditInstance extends AggregateRoot<Long> {
    // 包级私有 setter，仅限同包的领域服务调用
    void setStatusInternal(String status) { this.status = status; }

    // 公开的领域方法，包含完整业务逻辑
    public void approve(Long userId) {
        if (!STATUS_PENDING.equals(status)) {
            throw new IllegalStateException("Cannot approve: not in PENDING state");
        }
        if (hasNextStep()) {
            this.currentStepIndex++;
            this.result = "Step approved by user " + userId;
            return;
        }
        setStatusInternal(STATUS_APPROVED);
        // ... 领域事件、时间戳等
    }
}
```

### H-08~H-10 ✅ 已修复
- H-08: AuditInstance.approve() 正确处理多步工作流
- H-09: transfer()/addApprover() 抛出 UnsupportedOperationException
- H-10: WorkflowTask 所有状态转换增加前置条件检查

### H-11. JPA 字段遮蔽 createdAt/updatedAt ❌ 未修复
**文件:** `Attachment.java:71-75`, `AuditFlowDef.java:47-52`, `AuditLog.java:63-64`

```java
// 父类 Entity<ID> 声明 @Transient protected LocalDateTime createdAt
// 子类重新声明：
@Column(name = "created_at", nullable = false)
private LocalDateTime createdAt;  // 遮蔽父类字段！
```

**最优解 — 统一基类时间戳策略：**

```java
@MappedSuperclass
public abstract class TimestampedEntity<ID extends Serializable> extends Entity<ID> {
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

### H-12~H-14 ✅ 已修复 (EventStore 并发、JPQL 注入)

---

## C. 中等问题 (Medium) — 第一轮 28 项

### 已修复 (4项): M-11, M-17, M-05, H-12

### 未修复重点问题：

#### M-01. CorsConfig 生产环境无 @Profile ❌
```java
// 默认值 localhost 仅适用开发，但无 @Profile("dev") 保护
@Value("${cors.allowed-origins:http://localhost:5173,...}")
private String allowedOrigins;
```
**最优解:** 使用 Spring Profile 分离配置：
```java
@Configuration
@Profile("dev")
public class DevCorsConfig { /* localhost origins */ }

@Configuration
@Profile("prod")
@ConditionalOnProperty(name = "cors.allowed-origins")
public class ProdCorsConfig { /* 生产域名 */ }
```

#### M-08. SecurityHeadersFilter 缺少 CSP + HSTS ❌
```java
// 当前仅有废弃的 X-XSS-Protection
response.setHeader("X-XSS-Protection", "1; mode=block");  // 现代浏览器已弃用
```
**最优解:**
```java
response.setHeader("Content-Security-Policy",
    "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:");
response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
response.setHeader("X-Content-Type-Options", "nosniff");
// 移除 X-XSS-Protection（Chrome 78+, Firefox 69+ 已弃用）
```

#### M-12. 三重错误码系统 ❌
三套独立的错误码体系并存：
1. `ErrorCodes.java` — 整数常量 (`BAD_REQUEST=400`)
2. `ErrorResponse.java` — 字符串码 (`"AUTH_003"`)
3. `shared.domain.exception.BusinessException` — 字符串码 (`"BUSINESS_ERROR"`)

**最优解:** 统一为枚举 + ErrorCode 类：

```java
public enum ErrorCode {
    BAD_REQUEST(400, "BAD_REQUEST", "请求参数错误"),
    INVALID_CREDENTIALS(2001, "AUTH_001", "用户名或密码错误"),
    INTERNAL_ERROR(5000, "SYS_001", "系统内部错误");

    private final int numericCode;
    private final String stringCode;
    private final String message;
}
```

#### M-23. AuditInstance/WorkflowTask 字符串常量代替枚举 ❌
```java
// 当前：任何字符串都可传入
public static final String STATUS_PENDING = "PENDING";
auditInstance.setStatus("banana");  // 编译通过！
```
**最优解:**
```java
public enum AuditStatus {
    PENDING, APPROVED, REJECTED, CANCELLED;

    @Converter(autoApply = true)
    public static class JpaConverter extends AttributeConverter<AuditStatus, String> {
        @Override public String convertToDatabaseColumn(AuditStatus attr) {
            return attr == null ? null : attr.name();
        }
        @Override public AuditStatus convertToEntityAttribute(String db) {
            return db == null ? null : valueOf(db);
        }
    }
}
```

#### M-28/N-4. EventStore Bean 冲突 ❌ (新发现 — 启动失败风险)
`EventStoreInMemory` 有 `@Component` 无条件；`EventStoreDatabase` 有 `@ConditionalOnProperty`。两者同时注册时 `DomainEventPublisher` 注入失败。

**最优解:**
```java
@Component
@ConditionalOnMissingBean(EventStore.class)  // 仅在没有其他实现时注册
public class EventStoreInMemory implements EventStore { ... }
```

---

## D. 第二轮新发现问题

| 编号 | 严重度 | 问题 | 文件 |
|------|--------|------|------|
| N-01 | Medium | AuditLog.logId 列映射不匹配 | `AuditLog.java:24` |
| N-02 | Medium | AuditStepDef 无基类、无 equals/hashCode | `AuditStepDef.java` |
| N-03 | Medium | EventStore 双注册导致启动失败 | `EventStoreInMemory.java` |
| N-04 | Medium | DatabaseDataChecker SQL 拼接风险 | `DatabaseDataChecker.java:68` |
| N-05 | Low | CacheUtils 类级别 synchronized | `CacheUtils.java:46` |
| N-06 | Low | 工作流模型继承不一致 | 多个文件 |

---

## E. 剩余问题统计

| 严重度 | 剩余数量 |
|--------|---------|
| Critical | 2 (C-02 双 ID, C-06 双异常) |
| High | 2 (H-07 封装, H-11 字段遮蔽) |
| Medium | 19 |
| Low | 12 |
| **总计** | **35** |

## F. Top 5 优先修复

| 优先级 | 问题 | 理由 |
|--------|------|------|
| 🔴 P0 | M-28 EventStore Bean 冲突 | 生产启动失败 |
| 🔴 P0 | C-06 双异常层级 | 架构混乱、维护成本翻倍 |
| 🟡 P1 | H-11 JPA 字段遮蔽 | 数据持久化隐患 |
| 🟡 P1 | H-07 DDD 封装违反 | 领域逻辑可被绕过 |
| 🟢 P2 | M-08 + M-23 安全头 + 枚举 | 安全合规 |
