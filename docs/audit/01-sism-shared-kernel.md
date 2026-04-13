# 审计报告：sism-shared-kernel 模块

**审计日期:** 2026-04-12
**审计范围:** 85个Java文件，涵盖config、domain model、exception、util、infrastructure、enums六大子包。

---

## 一、Critical 严重 (共11个)

### C-1. RedisConfig: Jackson 反序列化远程代码执行漏洞
**文件:** `config/RedisConfig.java:146-150`
**严重性:** Critical
**状态:** 已修复（2026-04-12）
**描述:** `activateDefaultTyping` 配合 `LaissezFaireSubTypeValidator` 和 `NON_FINAL`，允许反序列化时实例化任意类。若Redis被攻陷，攻击者可构造恶意payload实现RCE。
**修复建议:** 移除 `activateDefaultTyping`，或在域对象上使用 `@JsonTypeInfo` 显式声明允许的类型。

### C-2. AuditLog: JPA 重复 @Id 映射
**文件:** `shared/domain/model/audit/AuditLog.java:26`
**严重性:** Critical
**状态:** 已修复（2026-04-12）
**描述:** 继承 `AggregateRoot<Long>`（已有 `id` 字段），又声明了 `logId` 带 `@Id @GeneratedValue`。JPA 检测到两个ID字段，将导致运行时映射失败。
**修复建议:** 删除 `logId`，使用继承的 `id`；或将父类 `id` 标记为 `@Transient`。

### C-3. Attachment.canPublish(): 领域实体中执行文件系统I/O + 路径遍历风险
**文件:** `shared/domain/model/attachment/Attachment.java:82-86`
**严重性:** Critical
**状态:** 已修复（2026-04-12）
**描述:** `canPublish()` 调用 `Files.exists(Paths.get(objectKey))`，执行阻塞式磁盘I/O。若 `objectKey` 含 `../`，可探测任意路径是否存在（信息泄露）。
**修复建议:** 将文件存在性检查移至基础设施服务 `AttachmentStorageService`，并验证 `objectKey` 在允许的基目录内。

### C-4. Entity/AggregateRoot equals/hashCode: null ID 时所有新实体相等
**文件:** `shared/domain/model/base/Entity.java:62-73`, `AggregateRoot.java:97-108`
**严重性:** Critical
**状态:** 已修复（2026-04-12）
**描述:** `equals()` 基于 `getId()`。未持久化实体 ID 为 null 时，`Objects.equals(null,null)=true`，`Objects.hash(null)=0`。同类所有新实例被视为相等，放入 HashSet 会静默丢失数据。
**修复建议:** ID 为 null 时回退到实例身份比较 (`this == o`)，或使用业务键。

### C-5. EntityId.equals() 忽略 entityType — 类型安全形同虚设
**文件:** `shared/domain/model/valueobject/EntityId.java:53-57`
**严重性:** Critical
**状态:** 已修复（2026-04-12）
**描述:** `equals()` 仅比较 `value`，不比较 `entityType`。`EntityId<User>("123")` 等于 `EntityId<Task>("123")`，违背类型安全ID的设计初衷。
**修复建议:** 在 `equals()` 和 `hashCode()` 中加入 `entityType`。

### C-6. 两套并行的异常体系互相冲突
**文件:** `exception/BusinessException.java` vs `shared/domain/exception/BusinessException.java`（及 ResourceNotFoundException 同理）
**严重性:** Critical
**状态:** 部分缓解（2026-04-12）
**描述:** 存在两套完全独立的异常层次：`com.sism.exception` 包用 `int` 错误码；`com.sism.shared.domain.exception` 包用 `String` 错误码 + `LocalDateTime`。`GlobalExceptionHandler` 必须分别注册处理器。共享域异常（AuthenticationException、AuthorizationException）各自直接继承 RuntimeException，无公共基类。
**修复建议:** 合并为统一的异常体系，一个基础异常含 `String code`、`int httpStatus`、`LocalDateTime timestamp`。

### C-7. GlobalExceptionHandler 错误码与 ErrorCodes 定义不匹配
**文件:** `exception/GlobalExceptionHandler.java:99,294,312,424`
**严重性:** Critical
**状态:** 已修复（2026-04-12）
**描述:** 多处错误码语义错配：

| 位置 | 映射到 | 实际应为 |
|---|---|---|
| L99 UnauthorizedException → 2002 | USER_DISABLED | 2004 INVALID_TOKEN |
| L294 AuthorizationException → 2003 | USER_LOCKED | 2006 INSUFFICIENT_PERMISSION |
| L312 TechnicalException → 3001 | TASK_NOT_FOUND | 与任务域冲突 |
| L424 通用Exception → 1000 | 未定义 | 应为 500 INTERNAL_ERROR |

**修复建议:** 重新对齐所有处理器错误码与 `ErrorCodes.java` 定义。

### C-8. BusinessException.message 字段遮蔽 RuntimeException.getMessage()
**文件:** `exception/BusinessException.java:10-20`
**严重性:** Critical
**状态:** 已修复（2026-04-12）
**描述:** Lombok `@Getter` 为 `message` 字段生成 getter，但不会覆盖 `RuntimeException.getMessage()`。反射代码检查字段 vs getter 会得到不同值。
**修复建议:** 删除 `message` 字段，直接使用继承的 `getMessage()`。

### C-9. SysTaskMigration: 每次启动执行破坏性DDL，无事务保护
**文件:** `util/SysTaskMigration.java:13-16,38`
**严重性:** Critical
**状态:** 已修复（2026-04-12）
**描述:** `@Component + CommandLineRunner` 使其在每次启动时执行 `DROP TABLE + ALTER TABLE`。无 `@Transactional`，崩溃后数据库状态不一致。应通过 Flyway 管理。
**修复建议:** 移除 `@Component`，转为 Flyway 迁移脚本。

### C-10. DatabaseDataChecker: Long 自动拆箱 NPE
**文件:** `util/DatabaseDataChecker.java:114,128`
**严重性:** Critical
**状态:** 已修复（2026-04-12）
**描述:** `queryForObject(..., Long.class)` 可能返回 null，直接 `if (count > 0)` 自动拆箱触发 NPE。
**修复建议:** `if (count != null && count > 0)`

### C-11. TokenBlacklistService: 原始JWT作Redis key + Redis宕机阻断认证
**文件:** `util/TokenBlacklistService.java:155-157,76`
**严重性:** Critical
**状态:** 已修复（2026-04-12）
**描述:** Redis key 为完整JWT字符串（数百字符），泄露即可重放。`isBlacklisted()` 无 try-catch，Redis宕机时异常传播到认证过滤器，锁定所有用户。
**修复建议:** SHA-256 哈希后作 key；`isBlacklisted()` 包装 try-catch 回退内存检查。

---

## 二、High 高 (共14个)

### H-1. CorsConfig: 通配符origin + credentials = CORS保护失效
**文件:** `config/CorsConfig.java:50-56`
**严重性:** High
**状态:** 已修复（2026-04-12）
**描述:** `*` origin 配合 `allowCredentials=true` 等同于禁用CORS。无校验/警告。
**修复建议:** 拒绝 `*` 与 credentials 的组合。
```java
if (trimmedOrigin.equals("*") && allowCredentials) {
    log.error("Wildcard origin '*' with credentials is insecure.");
    throw new IllegalStateException("Cannot use wildcard CORS origin with credentials");
}
```

### H-2. EnvConfig.getBoolean(): "0"/"no"/"off" 全部返回 true
**文件:** `config/EnvConfig.java:38`
**严重性:** High
**状态:** 已修复（2026-04-12）
**描述:** 任何非"false"字符串返回true，`"0"`、`"no"`、`"off"` 均被视为 true。
**修复建议:** 使用 `Boolean.parseBoolean()` 或识别常见 falsy 值。

### H-3. EnvConfigValidator: JWT_SECRET 属性key不匹配
**文件:** `config/EnvConfigValidator.java:111`
**严重性:** High
**状态:** 已修复（2026-04-12）
**描述:** 查找 `jwt.secret`，但 `EnvConfigPostProcessor` 将 `JWT_SECRET` 映射为 `app.jwt.secret`。JWT校验永远找不到值。
**修复建议:** 改为 `environment.getProperty("app.jwt.secret")`。

### H-4. JpaAuditConfig: Principal类型处理错误 + 异常被吞
**文件:** `config/JpaAuditConfig.java:39-45`
**严重性:** High
**状态:** 已修复（2026-04-12）
**描述:** 假设 principal 是 Long 或 String，实际 JWT 实现通常用 UserDetails。类型不匹配时返回 empty 且不记录日志，审计字段永远为空。
**修复建议:** 处理 UserDetails 类型，失败时记录 debug 日志。
```java
Object principal = authentication.getPrincipal();
if (principal instanceof UserDetails ud) {
    return Optional.of(Long.parseLong(ud.getUsername()));
} else if (principal instanceof Long l) {
    return Optional.of(l);
} else if (principal instanceof String s) {
    return Optional.of(Long.parseLong(s));
} else {
    log.warn("Unsupported principal type: {}", principal.getClass().getName());
    return Optional.empty();
}
```

### H-5. RequestLoggingFilter: 缓存完整响应体到内存
**文件:** `config/RequestLoggingFilter.java:60`
**严重性:** High
**状态:** 已修复（2026-04-12）
**描述:** `ContentCachingResponseWrapper` 缓存全部响应体。大文件下载/报表导致内存压力。
**修复建议:** 移除 ResponseWrapper，直接使用 `response.getStatus()`。

### H-6. AggregateRoot 与 Entity 大量代码重复
**文件:** `shared/domain/model/base/Entity.java` & `AggregateRoot.java`
**严重性:** High
**状态:** 已修复（2026-04-12）
**描述:** 两个类有相同字段(id, createdAt, updatedAt)、构造函数、markUpdated()、equals/hashCode/toString。DDD中聚合根是特殊的实体，应继承。
**修复建议:** `AggregateRoot<ID> extends Entity<ID>`，仅添加事件发布基础设施。

### H-7. 所有聚合根使用 @Setter 违反DDD封装
**文件:** Attachment、AuditLog、AuditFlowDef、AuditInstance、WorkflowTask
**严重性:** High
**状态:** 已修复（2026-04-12）
**描述:** `@Setter` 为所有字段（含 id、status）生成 public setter，外部代码可绕过领域不变式直接修改状态。
**修复建议:** 替换为特定字段setter或protected setter，状态变更仅通过领域方法。

### H-8. AuditInstance.approve(): 单步审批即关闭整个流程
**文件:** `shared/domain/model/workflow/AuditInstance.java:89-96`
**严重性:** High
**状态:** 已修复（2026-04-12）
**描述:** `approve()` 直接将实例状态设为 APPROVED，不检查剩余步骤、不递增 currentStepIndex。多步审批中一个审批人即可跳过所有人。
**修复建议:** `approve()` 仅批准当前步骤，递增 currentStepIndex，全部步骤完成后才设为 APPROVED。

### H-9. AuditInstance.transfer()/addApprover() 是空方法
**文件:** `shared/domain/model/workflow/AuditInstance.java:121-129`
**严重性:** High
**状态:** 已修复（2026-04-12）
**描述:** 方法体为空（仅有注释）。调用方认为操作成功，实际无任何状态变更。
**修复建议:** 实现逻辑或抛出 `UnsupportedOperationException`。

### H-10. WorkflowTask 状态转换无前置条件检查
**文件:** `shared/domain/model/workflow/WorkflowTask.java:100-127`
**严重性:** High
**状态:** 已修复（2026-04-12）
**描述:** `start()/complete()/fail()/cancel()` 不检查当前状态。已完成任务可被restart，已取消可被fail。
**修复建议:** 添加状态前置条件（参考 approve/reject 的模式）。

### H-11. JPA字段遮蔽: 子类重新声明父类 createdAt/updatedAt
**文件:** Attachment.java:75-79, AuditLog.java:65, AuditFlowDef.java:49-53
**严重性:** High
**状态:** 已修复（2026-04-12）
**描述:** 父类字段未标 `@Transient`，JPA 映射两套字段，导致 "重复列映射" 异常或数据不一致。
**修复建议:** 父类加 `@MappedSuperclass`，删除子类重复声明；或父类字段标 `@Transient`。

### H-12. EventStoreInMemory: ArrayList 并发修改线程不安全
**文件:** `shared/infrastructure/event/EventStoreInMemory.java:28`
**严重性:** High
**状态:** 已修复（2026-04-12）
**描述:** ConcurrentHashMap 内的 ArrayList 在并发 add/remove 时会 `ArrayIndexOutOfBoundsException`。
**修复建议:** 使用 `CopyOnWriteArrayList`。

### H-13. WriteRepository.executeUpdate(): JPQL注入攻击面
**文件:** `shared/domain/repository/WriteRepository.java:44`
**严重性:** High
**状态:** 已修复（2026-04-12）
**描述:** 接受原始JPQL字符串，任意调用者可注入更新/删除。且无任何代码使用此方法。
**修复建议:** 删除此方法。

### H-14. PlanLevel: 跨3个包存在3个同名枚举，值不同
**文件:** `com.sism.enums.PlanLevel` vs `sism-strategy` 中两个 `PlanLevel`
**严重性:** High
**状态:** 已修复（2026-04-12）
**描述:** 共享内核版本（STRAT_TO_FUNC, FUNC_TO_COLLEGE）与策略模块版本（STRATEGIC, OPERATIONAL, COMPREHENSIVE...）值完全不同，且共享内核版本无代码引用（死代码）。
**修复建议:** 合并为一个权威枚举，删除共享内核中未使用版本。

---

## 三、Medium 中等 (共28个)

| # | 文件:行号 | 问题 | 类别 |
|---|---|---|---|
| M-1 | `CorsConfig.java:23` | 无 `@Profile` 限制CORS默认值，生产环境可能使用localhost配置 | 安全 |
| M-2 | `EnvConfigPostProcessor.java:154` | 未映射的环境变量自动转为Spring属性，`JAVA_HOME`→`java.home` 覆盖JVM属性 | 安全/Bug |
| M-3 | `EnvConfigPostProcessor.java:61` | 使用 System.out/err 替代日志 | 代码质量 |
| M-4 | `RedisConfig.java:114-119` | 启动时ping Redis，不可达则整个应用无法启动 | 架构 |
| M-5 | `RedisConfig.java:145-152` | Redis的ObjectMapper与全局ObjectMapper隔离，序列化行为不一致 | 架构 |
| M-6 | `RequestLoggingFilter.java:91-94` | 客户端 `X-Request-ID` 未经校验直接注入响应；8字符UUID熵不足(32bit) | 安全 |
| M-7 | `RequestLoggingFilter.java:107-112` | MDC userId 永久停留在 "pending"，JwtAuthenticationFilter未调用 setUserId | Bug |
| M-8 | `SecurityHeadersFilter.java:35` | 使用已废弃的 `X-XSS-Protection`；**缺少 `Content-Security-Policy` 头** | 安全 |
| M-9 | `WebMvcConfig.java:18-27` | 手动注册Swagger资源与springdoc自动配置冲突 | 架构 |
| M-10 | `OpenApiConfig.java:46` | 全局SecurityRequirement强制所有端点（含公开端点）需要认证 | 代码质量 |
| M-11 | `CacheUtilsObjectMapperConfig.java:25` | set-once guard无警告，两个ObjectMapper bean时静默选择第一个 | Bug |
| M-12 | `ErrorCodes.java` | SUCCESS=200等与HTTP状态码重叠；与ErrorResponse字符串码("AUTH_003")形成三套独立错误码体系 | 代码质量 |
| M-13 | `ApiResponse.success(String)` | 泛型T=String时与 `success(String message)` 歧义，数据字段静默为null | Bug |
| M-14 | `PageResult.last` | total=0时 totalPages=0，`page >= -1` 恒true，空结果集任意页均 last=true | Bug |
| M-15 | `ValidationException.fieldErrors` | GlobalExceptionHandler 只用 `getMessage()`，fieldErrors 从未传递给客户端 | Bug |
| M-16 | `LoggingUtils.java:32` | "auth"关键词匹配 "author"/"authority" 等无害字段，过度脱敏阻碍调试 | 代码质量 |
| M-17 | `TokenBlacklistService.java:32` | `localBlacklist` ConcurrentHashMap 无大小限制，Redis宕机时OOM风险 | 性能 |
| M-18 | `CacheUtils.java:193,230,291` | 缓存TTL硬编码(300s/120s)，不可配置 | 代码质量 |
| M-19 | `ApiResponse/ErrorResponse` | 时间戳格式不一致：ApiResponse用LocalDateTime(无时区)，ErrorResponse用Instant(UTC) | 代码质量 |
| M-20 | `GlobalExceptionHandler.java:449,485` | `catch(Exception ignored){}` 吞异常，不记录日志 | 代码质量 |
| M-21 | 共享域异常 | AuthenticationException/AuthorizationException 无 `Throwable cause` 构造函数，异常链断裂 | 代码质量 |
| M-22 | 两个不相关的基类层次 | `BaseEntity`(@MappedSuperclass) vs `Entity<ID>`(纯领域)，新实体不知继承哪个 | 架构 |
| M-23 | AuditInstance/WorkflowTask | 状态用String常量而非枚举，`setStatus("banana")` 编译通过 | 代码质量 |
| M-24 | AuditStepDef.stepType/approverType | 无类型字符串，编译时无约束 | 代码质量 |
| M-25 | DomainEvent WeakHashMap | 新JVM对象获得不同UUID，反序列化后eventId不一致 | Bug |
| M-26 | EventStoreDatabase | 删除事件先全量加载再逐条删除，内存+性能问题 | 性能 |
| M-27 | EventStoreDatabase | save()抛异常但findById等静默返回空，无法区分"无数据"与"数据库故障" | 架构 |
| M-28 | EventStoreInMemory 无条件注册 | 与 EventStoreDatabase 并存时 NoUniqueBeanDefinitionException | Bug |

---

## 四、Low 低 (共15个)

| # | 文件:行号 | 问题 |
|---|---|---|
| L-1 | `EnvConfig.java:11-12` | 双重检查锁可用AtomicReference简化 |
| L-2 | `EnvConfigValidator.java:128-130` | `convertToPropertyName()` 死代码 |
| L-3 | `OpenApiConfig.java:24,37` | 硬编码版本号1.0.0；占位符license URL |
| L-4 | `SecurityHeadersFilter` | 缺少 `Strict-Transport-Security` (HSTS) 头 |
| L-5 | `AggregateRoot.setId()` | 允许持久化后修改ID，破坏equals/hashCode契约 |
| L-6 | `WorkflowTask.moveToNextStep()` | `this.currentStep = this.nextStep` 若nextStep为null则currentStep变null |
| L-7 | `WorkflowTask.reject()` | 设STATUS_FAILED而非STATUS_REJECTED，与AuditInstance语义不一致 |
| L-8 | `AuditLog.ipAddress` | 无长度约束和格式验证 |
| L-9 | `AuditStepInstance.createdAt` | 无@PrePersist初始化，入库后为null |
| L-10 | `AuditStepDef` | 无基类、无equals/hashCode、无验证 |
| L-11 | `DateRange.contains()` | 不检查null输入 |
| L-12 | `DateRange.equals()` | 忽略displayFormat字段 |
| L-13 | `DateRange.format()` | 每次调用创建新DateTimeFormatter |
| L-14 | `IndicatorLevel` | 6个常量仅表示2个概念值，PRIMARY/FIRST重复 |
| L-15 | `StoredEvent` | @Data生成equals/hashCode对JPA实体有害 |

---

## 汇总统计

| 严重性 | 数量 | 关键主题 |
|--------|------|----------|
| **Critical** | 11 | 反序列化RCE、JPA映射错误、路径遍历、equals/hashCode致命缺陷、破坏性迁移、异常体系分裂 |
| **High** | 14 | CORS绕过、环境变量解析错误、审计字段失效、DDD封装违反、工作流状态机bug、并发安全 |
| **Medium** | 28 | 安全头缺失、错误码体系混乱、线程安全、内存泄漏、接口不一致 |
| **Low** | 15 | 死代码、硬编码值、缺少文档 |
| **总计** | **68** | |

---

**最需优先修复的Top 5：**
1. **C-1** Redis反序列化RCE — 立即修复
2. **C-3** Attachment路径遍历 — 安全漏洞
3. **C-9** SysTaskMigration每次启动删表 — 数据丢失风险
4. **C-6** 双异常体系 + C-7 错误码错配 — 影响全系统错误处理
5. **H-8** 审批流程单步关闭 — 业务逻辑严重错误
