# sism-shared-kernel 模块审计报告

**审计日期:** 2026-04-06
**审计人员:** Claude Code (Automated Audit)
**模块版本:** 当前主分支

---

## 模块概览

| 项目 | 数据 |
|------|------|
| 模块名称 | sism-shared-kernel |
| 模块职责 | 共享内核、基础设施组件、领域基类、工具类、全局异常处理 |
| Java 文件总数 | 67 |
| 核心组件 | GlobalExceptionHandler, SecurityHeadersFilter, CorsConfig, RedisConfig, AggregateRoot, TokenBlacklistService |

### 包结构

```
com.sism/
├── common/                           # 通用响应类
│   ├── ApiResponse.java
│   ├── ErrorCodes.java
│   ├── ErrorResponse.java
│   └── PageResult.java
├── config/                           # 配置类
│   ├── CorsConfig.java
│   ├── EnvConfig.java
│   ├── JpaAuditConfig.java
│   ├── OpenApiConfig.java
│   ├── RedisConfig.java
│   ├── RequestLoggingFilter.java
│   ├── SecurityHeadersFilter.java
│   └── WebMvcConfig.java
├── enums/                            # 枚举定义
│   ├── AlertSeverity.java
│   ├── IndicatorLevel.java
│   ├── IndicatorStatus.java
│   ├── TaskType.java
│   └── ...
├── exception/                        # 异常类
│   ├── BusinessException.java
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── ...
├── shared/
│   ├── domain/
│   │   ├── exception/               # DDD 领域异常
│   │   └── model/
│   │       ├── base/                # 聚合根、实体基类
│   │       ├── valueobject/         # 值对象
│   │       ├── workflow/            # 工作流模型
│   │       └── attachment/          # 附件模型
│   └── infrastructure/
│       └── event/                   # 事件发布
└── util/                             # 工具类
    ├── CacheUtils.java
    ├── TokenBlacklistService.java
    └── ...
```

---

## 一、安全漏洞

### 🔴 High: TokenBlacklistService 使用内存存储

**文件:** `TokenBlacklistService.java`
**行号:** 17-59

```java
@Slf4j
public class TokenBlacklistService {

    // Token blacklist (in-memory, consider Redis for production)
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();  // ❌ 内存存储

    public void blacklist(String token) {
        if (token != null && !token.isEmpty()) {
            blacklist.add(token);
        }
    }

    public boolean isBlacklisted(String token) {
        return blacklist.contains(token);
    }
}
```

**问题描述:**
1. Token 黑名单存储在 JVM 内存中，应用重启后丢失
2. 多实例部署时无法共享黑名单状态
3. 无过期清理机制，内存持续增长
4. 注释中提到 "consider Redis for production" 但未实现

**风险影响:**
- 应用重启后已注销 Token 可再次使用
- 多实例环境下 Token 黑名单失效
- 潜在的内存泄漏

**严重等级:** 🔴 **High**

**建议修复:**
```java
@Service
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true")
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void blacklist(String token, long expirationSeconds) {
        String key = "blacklist:" + tokenHash(token);
        redisTemplate.opsForValue().set(key, "1", expirationSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean isBlacklisted(String token) {
        String key = "blacklist:" + tokenHash(token);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
```

---

### 🟠 Medium: CORS 默认配置过于宽松

**文件:** `CorsConfig.java`
**行号:** 22-23

```java
@Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:3500}")
private String allowedOrigins;
```

**问题描述:**
默认配置包含多个 localhost 地址，虽然适合开发环境，但如果生产环境未正确配置，可能导致跨域安全风险。

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
@Value("${app.cors.allowed-origins:}")
private String allowedOrigins;

@Bean
public CorsFilter corsFilter() {
    if (allowedOrigins == null || allowedOrigins.isBlank()) {
        log.warn("CORS allowed origins not configured, using strict default");
        // 生产环境默认不允许任何跨域
    }
    // ...
}
```

---

### 🟠 Medium: Redis 密码可能被日志记录

**文件:** `RedisConfig.java`
**行号:** 79-80

```java
log.info("Initializing Redis connection factory: host={}, port={}, database={}",
         host, port, database);
```

**问题描述:**
虽然当前日志未包含密码，但密码通过 `@Value` 注入，可能在其他地方被意外记录。

**建议修复:** 确保密码字段标记为敏感，使用 Spring Boot 的 `@ConfigurationProperties` 并设置 `JsonIgnore`。

---

### 🟡 Low: DomainEvent 接口每次调用生成新 ID

**文件:** `DomainEvent.java`
**行号:** 15-17

```java
default String getEventId() {
    return UUID.randomUUID().toString();  // ❌ 每次调用生成新 ID
}
```

**问题描述:**
`getEventId()` 是默认方法，每次调用都会生成新的 UUID。如果多次调用同一事件的 `getEventId()`，会返回不同的值。

**风险影响:**
- 事件追踪不一致
- 日志分析困难

**严重等级:** 🟡 **Low**

**建议修复:**
事件 ID 应在创建时生成并存储：
```java
public abstract class BaseDomainEvent implements DomainEvent {
    private final String eventId = UUID.randomUUID().toString();
    private final LocalDateTime occurredOn = LocalDateTime.now();

    @Override
    public String getEventId() {
        return eventId;
    }
}
```

---

## 二、潜在 Bug 和逻辑错误

### 🟠 Medium: EnvConfig 静态初始化可能失败

**文件:** `EnvConfig.java`
**行号:** 10-20

```java
static {
    try {
        File projectDir = new File(System.getProperty("user.dir"));
        dotenv = Dotenv.configure()
                .directory(projectDir.getAbsolutePath())
                .ignoreIfMissing()
                .load();
    } catch (Exception e) {
        System.err.println("Failed to load .env file: " + e.getMessage());  // ❌ 仅打印错误
    }
}
```

**问题描述:**
1. 静态块初始化失败时 `dotenv` 可能为 null
2. 后续调用 `get()` 方法可能导致 NullPointerException
3. 使用 `System.err` 而非日志框架

**风险影响:**
- 应用启动时可能静默失败
- 环境变量读取不可靠

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
static {
    try {
        File projectDir = new File(System.getProperty("user.dir"));
        dotenv = Dotenv.configure()
                .directory(projectDir.getAbsolutePath())
                .ignoreIfMissing()
                .load();
    } catch (Exception e) {
        // 静默失败，使用系统环境变量
        dotenv = null;
    }
}

public static String get(String key) {
    // 先检查系统环境变量，再检查 .env
    String systemEnv = System.getenv(key);
    if (systemEnv != null) {
        return systemEnv;
    }
    return dotenv != null ? dotenv.get(key) : null;
}
```

---

### 🟠 Medium: GlobalExceptionHandler 中异常处理重复

**文件:** `GlobalExceptionHandler.java`
**行号:** 52-240`

```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) { ... }

@ExceptionHandler(com.sism.shared.domain.exception.BusinessException.class)
public ResponseEntity<ApiResponse<Void>> handleSharedBusinessException(...) { ... }

@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(...) { ... }

@ExceptionHandler(com.sism.shared.domain.exception.ResourceNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handleSharedResourceNotFoundException(...) { ... }
```

**问题描述:**
存在两套异常体系（`com.sism.exception` 和 `com.sism.shared.domain.exception`），导致重复的异常处理器。这增加了维护成本和潜在的不一致行为。

**风险影响:**
- 代码重复
- 维护困难
- 行为可能不一致

**严重等级:** 🟠 **Medium**

**建议修复:** 统一使用一套异常体系，或让一套继承另一套。

---

### 🟡 Low: Percentage 构造函数边界检查不包含边界值

**文件:** `Percentage.java`
**行号:** 16-23

```java
public Percentage(BigDecimal value) {
    if (value == null) {
        throw new IllegalArgumentException("Percentage value cannot be null");
    }
    if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(HUNDRED) > 0) {
        throw new IllegalArgumentException("Percentage must be between 0 and 100");
    }
    this.value = value;
}
```

**问题描述:**
注释说 "between 0 and 100" 但实际包含边界值 (>= 0 且 <= 100)，文档与实现不一致。

**严重等级:** 🟡 **Low**

**建议修复:** 更新错误消息：
```java
throw new IllegalArgumentException("Percentage must be between 0 and 100 (inclusive)");
```

---

### 🟡 Low: RequestLoggingFilter 请求 ID 过短

**文件:** `RequestLoggingFilter.java`
**行号:** 118-120

```java
private String generateRequestId() {
    return UUID.randomUUID().toString().substring(0, 8);  // ❌ 仅 8 字符
}
```

**问题描述:**
请求 ID 仅使用 UUID 的前 8 个字符，碰撞概率较高。如果有大量请求，可能出现重复 ID。

**严重等级:** 🟡 **Low**

**建议修复:**
```java
private String generateRequestId() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
}
```

---

## 三、性能瓶颈

### 🟠 Medium: DomainEventPublisher 事件发布无异步处理

**文件:** `DomainEventPublisher.java`
**行号:** 33-49

```java
public void publish(DomainEvent event) {
    // ...
    try {
        eventStore.save(event);  // ❌ 同步保存到数据库
    } catch (Exception e) {
        log.warn("Failed to persist event {}, continuing with in-process publish: {}",
                event.getEventId(), e.getMessage());
    }

    applicationEventPublisher.publishEvent(event);  // ❌ 同步发布
}
```

**问题描述:**
事件发布是同步操作，会阻塞当前线程。如果事件处理耗时，会影响 API 响应时间。

**风险影响:**
- API 响应延迟
- 事件处理失败影响主流程

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
@Async
public void publishAsync(DomainEvent event) {
    publish(event);
}

// 或使用事件总线
@Autowired
private ApplicationEventPublisher applicationEventPublisher;
```

---

### 🟡 Low: CacheUtils 创建多个 ObjectMapper 实例

**文件:** `CacheUtils.java`
**行号:** 33

```java
private static final ObjectMapper objectMapper = new ObjectMapper();
```

**问题描述:**
每个使用 CacheUtils 的类都会加载这个静态 ObjectMapper。虽然使用了 static final，但 ObjectMapper 配置可能与其他地方不一致。

**严重等级:** 🟡 **Low**

**建议修复:** 使用 Spring 管理的单例 ObjectMapper。

---

## 四、代码质量和可维护性

### 🟠 Medium: 异常类层次结构重复

**文件:** `com.sism.exception` 和 `com.sism.shared.domain.exception`

```
com.sism.exception/
├── BusinessException.java
├── ResourceNotFoundException.java
├── UnauthorizedException.java
└── ...

com.sism.shared.domain.exception/
├── BusinessException.java
├── ResourceNotFoundException.java
├── AuthenticationException.java
└── ...
```

**问题描述:**
存在两套异常体系，功能重叠。这违反了 DRY 原则，增加了维护成本。

**严重等级:** 🟠 **Medium**

**建议修复:** 统一使用一套异常体系，推荐使用 `com.sism.shared.domain.exception`（DDD 风格）。

---

### 🟠 Medium: SecurityHeadersFilter 硬编码 API 路径前缀

**文件:** `SecurityHeadersFilter.java`
**行号:** 71-75

```java
if (request.getRequestURI().startsWith("/api/")) {  // ❌ 硬编码路径
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Expires", "0");
}
```

**问题描述:**
API 路径前缀硬编码，如果配置了不同的路径前缀（如 `/api/v1/`），可能导致缓存控制不生效。

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
@Value("${app.api.prefix:/api}")
private String apiPrefix;

if (request.getRequestURI().startsWith(apiPrefix)) {
    // ...
}
```

---

### 🟡 Low: 枚举类未实现通用接口

**文件:** `com.sism.enums/` 目录下的枚举

```java
public enum AlertSeverity { ... }
public enum IndicatorLevel { ... }
public enum TaskType { ... }
```

**问题描述:**
枚举类没有实现通用接口（如 `CodeEnum` 或 `LabeledEnum`），无法统一处理枚举值和标签。

**建议修复:**
```java
public interface BaseEnum {
    String getCode();
    String getLabel();
}

public enum AlertSeverity implements BaseEnum {
    HIGH("HIGH", "高"),
    MEDIUM("MEDIUM", "中"),
    LOW("LOW", "低");

    private final String code;
    private final String label;
    // ...
}
```

---

### 🟡 Low: GlobalExceptionHandler 方法过长

**文件:** `GlobalExceptionHandler.java`
**行数:** 500+ 行

**问题描述:**
异常处理器类超过 500 行，包含 20+ 个处理方法。这违反了单一职责原则。

**建议修复:** 按异常类型拆分为多个处理器：
- `BusinessExceptionHandler`
- `AuthenticationExceptionHandler`
- `ValidationExceptionHandler`
- `TechnicalExceptionHandler`

---

## 五、架构最佳实践

### 🟠 Medium: AggregateRoot 的 validate() 方法可能未被调用

**文件:** `AggregateRoot.java`
**行号:** 85-88

```java
/**
 * 验证聚合根的业务规则
 * 子类必须实现此方法进行业务验证
 */
public abstract void validate();
```

**问题描述:**
`validate()` 方法是抽象的，但未在 Repository 保存时自动调用。依赖开发者手动调用，容易被忽略。

**风险影响:**
- 业务规则验证可能被跳过
- 无效数据可能被持久化

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
@PrePersist
@PreUpdate
protected void validateBeforeSave() {
    validate();
}
```

---

### 🟡 Low: Repository 接口过于分散

**文件:** `com.sism.shared.domain.repository/`

```
├── ReadRepository.java
├── WriteRepository.java
├── Repository.java
├── AttachmentRepository.java
└── AuditLogRepository.java
```

**问题描述:**
定义了多个细粒度的 Repository 接口（ReadRepository, WriteRepository, Repository），但实际使用中可能过于复杂。

**建议:** 评估这些接口是否真正实现了关注点分离，还是增加了不必要的复杂性。

---

### 🟡 Low: 值对象未实现 Comparable

**文件:** `Percentage.java`, `DateRange.java`, `EntityId.java`

```java
public class Percentage {  // ❌ 未实现 Comparable
    // ...
}
```

**问题描述:**
值对象通常需要比较功能，但未实现 `Comparable` 接口。

**建议修复:**
```java
public class Percentage implements Comparable<Percentage> {
    @Override
    public int compareTo(Percentage other) {
        return this.value.compareTo(other.value);
    }
}
```

---

## 六、审计总结

### 问题统计

| 严重等级 | 数量 | 类别分布 |
|----------|------|----------|
| 🔴 High | 1 | 安全（Token 黑名单内存存储） |
| 🟠 Medium | 8 | 安全、Bug、架构、代码质量 |
| 🟡 Low | 8 | 代码质量、架构、性能 |

### 最紧急修复项

| 优先级 | 问题 | 影响 |
|--------|------|------|
| P1 | TokenBlacklistService 使用内存存储 | 多实例/重启后 Token 黑名单失效 |
| P1 | 异常处理体系重复 | 维护困难、行为不一致 |
| P2 | CORS 默认配置宽松 | 潜在的跨域安全风险 |
| P2 | AggregateRoot.validate() 未自动调用 | 业务规则可能被跳过 |

### 整体评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 安全性 | 🟠 需改进 | Token 黑名单实现存在风险 |
| 可靠性 | ✅ 良好 | 异常处理完善 |
| 性能 | ✅ 良好 | 整体设计合理 |
| 可维护性 | 🟠 需改进 | 存在代码重复和结构问题 |
| 架构合规性 | ✅ 良好 | 遵循 DDD 设计原则 |

### 亮点

1. **安全头配置完善**: SecurityHeadersFilter 实现了完整的安全头设置
2. **请求日志完善**: RequestLoggingFilter 提供了完整的请求上下文日志
3. **值对象设计良好**: Percentage 等值对象实现了类型安全和不可变性
4. **全局异常处理完善**: 覆盖了所有常见异常类型

### 关键建议

1. **实现 Redis Token 黑名单**: 替换内存存储为 Redis 实现
2. **统一异常体系**: 合并两套异常类，减少重复
3. **自动调用验证**: 在 JPA 生命周期回调中自动调用 `validate()`
4. **异步事件发布**: 考虑使用 `@Async` 或消息队列处理事件发布

---

**审计完成日期:** 2026-04-06
**下一步行动:** 修复 Token 黑名单实现后再部署多实例生产环境