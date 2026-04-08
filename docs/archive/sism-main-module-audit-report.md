# sism-main 模块审计报告

**审计日期:** 2026-04-06
**审计人员:** Claude Code (Automated Audit)
**模块版本:** 当前主分支

---

## 模块概览

| 项目 | 数据 |
|------|------|
| 模块名称 | sism-main |
| 模块职责 | 应用入口、全局安全配置、WebSocket通信、附件管理 |
| Java 文件总数 | 12 |
| 核心组件 | SecurityConfig, WebSocketConfig, AttachmentController, SismMainApplication |

### 包结构

```
com.sism/
├── config/
│   ├── SecurityConfig.java           # 安全配置（核心）
│   ├── WebSocketConfig.java          # WebSocket 配置
│   ├── SismWebSocketHandler.java     # WebSocket 处理器
│   └── WebSocketNotificationService.java
└── com.sism.main/
    ├── SismMainApplication.java      # 应用入口
    ├── application/
    │   └── AttachmentApplicationService.java
    ├── bootstrap/
    │   └── PlanIntegrityBootstrap.java
    ├── config/
    │   └── UploadResourceConfig.java
    └── interfaces/
        ├── dto/
        │   └── AttachmentResponse.java
        └── rest/
            ├── AttachmentController.java
            ├── SystemHealthController.java
            └── WebSocketNotificationController.java
```

---

## 一、安全漏洞

### 🔴 Critical: WebSocket 连接无身份验证

**文件:** `SismWebSocketHandler.java`
**行号:** 133-142

```java
private String extractUserId(WebSocketSession session) {
    if (session.getUri() == null) {
        return null;
    }

    return UriComponentsBuilder.fromUri(session.getUri())
            .build()
            .getQueryParams()
            .getFirst("userId");  // ❌ 直接从 URL 参数获取用户ID，无验证
}
```

**问题描述:**
WebSocket 连接通过 URL 查询参数 `userId` 标识用户，没有任何身份验证。攻击者可以：
1. 伪造任意用户ID连接 WebSocket
2. 接收其他用户的实时通知
3. 冒充其他用户身份

**攻击示例:**
```
ws://server/ws/notifications?userId=1  // 冒充用户1
ws://server/ws/notifications?userId=2  // 冒充用户2
```

**风险影响:**
- 实时通知数据泄露
- 用户身份冒充
- 违反访问控制策略

**严重等级:** 🔴 **Critical**

**建议修复:**
```java
private String extractUserId(WebSocketSession session) {
    // 方案1: 从 URL 参数获取 JWT Token 并验证
    String token = UriComponentsBuilder.fromUri(session.getUri())
            .build()
            .getQueryParams()
            .getFirst("token");

    if (token == null || !jwtTokenService.validateToken(token)) {
        log.warn("Invalid or missing WebSocket token");
        return null;
    }

    return String.valueOf(jwtTokenService.getUserIdFromToken(token));

    // 方案2: 使用 HTTP 握手时的 Principal
    // return session.getPrincipal() != null ? session.getPrincipal().getName() : null;
}
```

---

### 🔴 Critical: WebSocket 端点完全开放

**文件:** `SecurityConfig.java`
**行号:** 192

```java
// Public endpoints - WebSocket
.requestMatchers("/ws/**", "/api/v1/ws/**").permitAll()  // ❌ 完全开放
```

**问题描述:**
WebSocket 端点在安全配置中被标记为公开访问，无需任何认证即可连接。

**严重等级:** 🔴 **Critical**

**建议修复:**
```java
// WebSocket endpoints should require authentication
.requestMatchers("/ws/**").authenticated()
.requestMatchers("/api/v1/ws/**").authenticated()
```

---

### 🔴 High: WebSocket 测试接口无权限控制

**文件:** `WebSocketNotificationController.java`
**行号:** 30-57

```java
@PostMapping("/test/{userId}")
@Operation(summary = "发送测试通知给特定用户")
public ResponseEntity<ApiResponse<Map<String, Object>>> sendTestNotification(
        @PathVariable String userId,  // ❌ 无权限控制，任何人可向任意用户发送通知
        @RequestParam(defaultValue = "APPROVAL_REQUIRED") String type,
        // ...
) {
    boolean delivered = notificationService.sendToUser(userId, type, title, content, ...);
    // ...
}
```

**问题描述:**
此接口允许任何用户向任意用户发送 WebSocket 通知，包括：
1. 可被滥用于向其他用户发送垃圾通知
2. 可模拟系统通知进行钓鱼攻击
3. 注释说明是 "Debug-only"，但在生产环境可见

**风险影响:**
- 通知系统滥用
- 钓鱼攻击向量
- 生产环境安全风险

**严重等级:** 🔴 **High**

**建议修复:**
```java
@PostMapping("/test/{userId}")
@PreAuthorize("hasRole('ADMIN')")  // 仅管理员可用
@Profile("dev")  // 仅开发环境启用
@Operation(summary = "发送测试通知（仅开发环境）")
public ResponseEntity<ApiResponse<Map<String, Object>>> sendTestNotification(...) {
    // ...
}
```

---

### 🟠 Medium: 文件上传路径可配置但未验证

**文件:** `AttachmentApplicationService.java`
**行号:** 31-32, 138-139

```java
@Value("${file.upload.path:${user.home}/.sism/uploads}")
private String uploadPath;

private Path resolveUploadRoot() {
    return Paths.get(uploadPath).toAbsolutePath().normalize();  // ❌ 未验证路径安全性
}
```

**问题描述:**
上传路径通过配置文件设置，但未验证：
1. 路径是否在安全范围内
2. 是否可被配置为系统敏感目录
3. 路径是否存在写入权限

**风险影响:**
- 配置错误可能导致文件写入危险位置
- 路径遍历风险

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
private Path resolveUploadRoot() {
    Path path = Paths.get(uploadPath).toAbsolutePath().normalize();

    // 验证路径安全性
    if (!path.startsWith(Paths.get("/var/sism/uploads").toAbsolutePath())) {
        throw new IllegalStateException("Upload path must be within allowed directory");
    }

    return path;
}
```

---

### 🟠 Medium: JWT 异常被静默忽略

**文件:** `SecurityConfig.java`
**行号:** 87-89

```java
try {
    if (jwtTokenService.validateToken(token)) {
        // ...
    }
} catch (Exception ignored) {
    // Token validation failed, continue without authentication  // ❌ 吞掉所有异常
}
```

**问题描述:**
JWT 验证过程中的所有异常被静默忽略，无法区分：
- Token 过期
- Token 被篡改
- 签名无效
- 其他系统错误

**风险影响:**
- 安全事件难以审计
- 无法追踪攻击尝试

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
} catch (ExpiredJwtException e) {
    log.debug("Token expired for request to {}", request.getRequestURI());
} catch (JwtException e) {
    log.warn("Invalid JWT token for request to {}: {}", request.getRequestURI(), e.getMessage());
} catch (Exception e) {
    log.error("Unexpected error during JWT validation", e);
}
```

---

### 🟠 Medium: 上传文件扩展名未严格验证

**文件:** `AttachmentApplicationService.java`
**行号:** 46-53

```java
String originalName = file.getOriginalFilename() == null ? "unknown" : Paths.get(file.getOriginalFilename()).getFileName().toString();
String extension = "";
int extensionIndex = originalName.lastIndexOf('.');
if (extensionIndex >= 0 && extensionIndex < originalName.length() - 1) {
    extension = originalName.substring(extensionIndex + 1);  // ❌ 未验证扩展名白名单
}

String objectKey = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
```

**问题描述:**
文件扩展名直接从原始文件名提取，未验证是否在允许的白名单内。可能上传恶意可执行文件。

**风险影响:**
- 上传恶意可执行文件
- 可能的服务器端代码执行

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
    "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
    "jpg", "jpeg", "png", "gif", "webp",
    "txt", "csv", "zip"
);

// 验证扩展名
String extension = extractExtension(originalName);
if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
    throw new IllegalArgumentException("File type not allowed: " + extension);
}
```

---

## 二、潜在 Bug 和逻辑错误

### 🟠 Medium: 附件上传 URL 临时值问题

**文件:** `AttachmentApplicationService.java`
**行号:** 67-81

```java
Long id = jdbcTemplate.queryForObject(
        """
        INSERT INTO public.attachment (...)
        VALUES ('FILE', NULL, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, NULL, false)
        RETURNING id
        """,
        Long.class,
        objectKey,
        "/api/v1/attachments/__TEMP__/download",  // ❌ 临时 URL
        // ...
);

String publicUrl = "/api/v1/attachments/" + id + "/download";
jdbcTemplate.update(
        "UPDATE public.attachment SET public_url = ? WHERE id = ?",  // ❌ 立即更新
        publicUrl,
        id
);
```

**问题描述:**
1. 插入时使用临时 URL `__TEMP__`，然后立即更新
2. 这是两次数据库操作，增加了失败风险
3. 如果更新失败，会留下无效 URL

**风险影响:**
- 事务不完整时数据不一致
- 性能开销

**严重等级:** 🟠 **Medium**

**建议修复:**
使用单个 INSERT 语句或在事务中执行：
```java
// 方案: 先生成 ID 或使用数据库函数
Long id = jdbcTemplate.queryForObject(
    "SELECT nextval('attachment_id_seq')", Long.class);

String publicUrl = "/api/v1/attachments/" + id + "/download";
jdbcTemplate.update(
    "INSERT INTO attachment (id, public_url, ...) VALUES (?, ?, ...)",
    id, publicUrl, ...
);
```

---

### 🟡 Low: 文件查找回退路径过多

**文件:** `AttachmentApplicationService.java`
**行号:** 142-163

```java
private Path resolveFilePath(String objectKey) {
    List<Path> candidates = new ArrayList<>();
    Path uploadRoot = resolveUploadRoot();
    candidates.add(uploadRoot.resolve(objectKey).normalize());

    Path workingDirectory = Paths.get("").toAbsolutePath().normalize();
    candidates.add(workingDirectory.resolve("uploads").resolve(objectKey).normalize());
    candidates.add(workingDirectory.resolve("sism-main").resolve("uploads").resolve(objectKey).normalize());

    Path parentDirectory = workingDirectory.getParent();
    if (parentDirectory != null) {
        candidates.add(parentDirectory.resolve("uploads").resolve(objectKey).normalize());
    }

    for (Path candidate : candidates) {
        if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
            return candidate;
        }
    }

    return candidates.get(0);  // ❌ 返回不存在的路径
}
```

**问题描述:**
1. 多个回退路径增加了路径遍历风险
2. 最后返回不存在的路径可能导致下游错误
3. 路径逻辑复杂，难以维护

**风险影响:**
- 文件查找失败
- 维护困难

**严重等级:** 🟡 **Low**

**建议修复:** 统一文件存储路径，移除多个回退逻辑。

---

### 🟡 Low: Bootstrap 无错误处理

**文件:** `PlanIntegrityBootstrap.java`
**行号:** 18-21

```java
@Override
public void run(ApplicationArguments args) {
    log.info("[PlanIntegrityBootstrap] Ensuring baseline plan matrix");
    planIntegrityService.ensurePlanMatrix();  // ❌ 无 try-catch，异常会导致应用启动失败
}
```

**问题描述:**
启动时的初始化操作没有错误处理，如果失败会导致整个应用无法启动。

**风险影响:**
- 应用启动失败
- 难以诊断问题

**严重等级:** 🟡 **Low**

**建议修复:**
```java
@Override
public void run(ApplicationArguments args) {
    try {
        log.info("[PlanIntegrityBootstrap] Ensuring baseline plan matrix");
        planIntegrityService.ensurePlanMatrix();
    } catch (Exception e) {
        log.error("[PlanIntegrityBootstrap] Failed to ensure plan matrix", e);
        // 根据业务需求决定是否抛出异常
    }
}
```

---

## 三、性能瓶颈

### 🟠 Medium: WebSocket 无连接数限制

**文件:** `SismWebSocketHandler.java`
**行号:** 30-31`

```java
private final Map<String, Set<WebSocketSession>> sessionsByUserId = new ConcurrentHashMap<>();
private final Map<String, String> userIdBySessionId = new ConcurrentHashMap<>();
```

**问题描述:**
1. 无单个用户的连接数限制
2. 无总连接数限制
3. 可能被滥用进行资源耗尽攻击

**风险影响:**
- 内存耗尽
- 资源耗尽攻击

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
private static final int MAX_CONNECTIONS_PER_USER = 5;
private static final int MAX_TOTAL_CONNECTIONS = 10000;

@Override
public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    String userId = extractUserId(session);

    // 检查用户连接数限制
    Set<WebSocketSession> userSessions = sessionsByUserId.get(userId);
    if (userSessions != null && userSessions.size() >= MAX_CONNECTIONS_PER_USER) {
        session.close(CloseStatus.POLICY_VIOLATION.withReason("Too many connections"));
        return;
    }

    // 检查总连接数限制
    if (userIdBySessionId.size() >= MAX_TOTAL_CONNECTIONS) {
        session.close(CloseStatus.POLICY_VIOLATION.withReason("Server at capacity"));
        return;
    }
    // ...
}
```

---

### 🟡 Low: ObjectMapper 多实例创建

**文件:** `SecurityConfig.java:45` 和 `SismWebSocketHandler.java:29`

```java
// SecurityConfig.java
private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();  // 实例1

// SismWebSocketHandler.java
private final ObjectMapper objectMapper = new ObjectMapper();  // 实例2
```

**问题描述:**
多个 ObjectMapper 实例被创建，而 Spring 通常会自动配置一个共享的 ObjectMapper Bean。这可能导致不一致的序列化行为和资源浪费。

**严重等级:** 🟡 **Low**

**建议修复:**
```java
@Autowired
private ObjectMapper objectMapper;  // 使用 Spring 管理的单例
```

---

## 四、代码质量和可维护性

### 🟠 Medium: SecurityConfig 内部类过于复杂

**文件:** `SecurityConfig.java`
**行号:** 59-161

```java
@Bean
public OncePerRequestFilter jwtAuthenticationFilter(...) {
    return new OncePerRequestFilter() {
        @Override
        protected void doFilterInternal(...) { ... }

        private UserDetails buildUserDetailsFromToken(...) { ... }

        private Map<String, Object> decodeTokenPayload(...) { ... }

        private Long parseLongClaim(...) { ... }

        private List<String> parseRoleClaims(...) { ... }
    };
}
```

**问题描述:**
JWT 过滤器作为匿名内部类实现，包含多个私有方法，代码超过 100 行。这违反了单一职责原则。

**严重等级:** 🟠 **Medium**

**建议修复:**
提取为独立的 `JwtAuthenticationFilter` 类：
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // 独立的类，更容易测试和维护
}
```

---

### 🟠 Medium: CORS 配置过于宽松

**文件:** `WebSocketConfig.java`
**行号:** 26-28

```java
@Override
public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(sismWebSocketHandler, "/ws/notifications")
            .setAllowedOriginPatterns("*");  // ❌ 允许所有来源
}
```

**问题描述:**
WebSocket 允许所有来源连接，可能导致跨站 WebSocket 劫持攻击。

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
.setAllowedOrigins("https://your-domain.com", "https://app.your-domain.com")
// 或从配置读取
.setAllowedOrigins(allowedOrigins.split(","))
```

---

### 🟡 Low: 硬编码的公开端点列表

**文件:** `SecurityConfig.java`
**行号:** 181-197

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register",
                     "/api/v1/auth/validate", "/api/v1/auth/logout",
                     "/api/v1/auth/refresh", "/api/v1/auth/health").permitAll()
    // ... 更多硬编码路径
)
```

**问题描述:**
公开端点列表硬编码，不易维护。新增端点时需要修改代码。

**建议修复:**
使用配置属性或常量类：
```java
@Value("${security.public-endpaths}")
private String[] publicEndpoints;

// 或使用常量类
public static final String[] PUBLIC_ENDPOINTS = { ... };
```

---

### 🟡 Low: 重复的 scanBasePackages 配置

**文件:** `SismMainApplication.java`
**行号:** 29-46

```java
@SpringBootApplication(scanBasePackages = {"com.sism.iam", "com.sism.organization", ...})
@EnableAsync
@Import(AttachmentController.class)
@EntityScan(basePackages = "com.sism.**.domain")
@EnableJpaRepositories(basePackages = "com.sism.**.infrastructure.persistence")
@ComponentScan(basePackages = {"com.sism.iam", "com.sism.organization", ...})  // ❌ 与 scanBasePackages 重复
public class SismMainApplication { ... }
```

**问题描述:**
`@SpringBootApplication(scanBasePackages = ...)` 已经包含组件扫描功能，`@ComponentScan` 是多余的。

**严重等级:** 🟡 **Low**

---

## 五、架构最佳实践

### 🟠 Medium: 附件服务直接使用 JdbcTemplate

**文件:** `AttachmentApplicationService.java`

```java
@Service
@RequiredArgsConstructor
public class AttachmentApplicationService {

    private final JdbcTemplate jdbcTemplate;  // ❌ 直接使用 JdbcTemplate，跳过 Repository 层

    @Transactional
    public AttachmentResponse upload(...) throws IOException {
        Long id = jdbcTemplate.queryForObject(...);  // 原生 SQL
        jdbcTemplate.update(...);
    }
}
```

**问题描述:**
服务层直接使用 JdbcTemplate 执行原生 SQL，绕过了 Repository 层。这违反了分层架构原则，且：
1. 无法使用 JPA 实体映射
2. 无法利用 Hibernate 缓存
3. SQL 分散在业务代码中

**严重等级:** 🟠 **Medium**

**建议修复:**
创建 `Attachment` 实体和 `AttachmentRepository`，使用标准的 JPA 操作。

---

### 🟡 Low: WebSocket 处理器缺少心跳机制

**文件:** `SismWebSocketHandler.java`

**问题描述:**
WebSocket 处理器没有实现心跳/Ping-Pong 机制，无法检测断开的连接。

**风险影响:**
- 僵尸连接占用资源
- 无法及时清理断开的会话

**建议修复:**
```java
@Override
public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    // 启动心跳定时器
    scheduledExecutor.scheduleAtFixedRate(() -> {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage("{\"type\":\"ping\"}"));
        }
    }, 30, 30, TimeUnit.SECONDS);
}
```

---

## 六、审计总结

### 问题统计

| 严重等级 | 数量 | 类别分布 |
|----------|------|----------|
| 🔴 Critical | 2 | WebSocket 安全漏洞 |
| 🔴 High | 1 | WebSocket 测试接口权限 |
| 🟠 Medium | 8 | 安全、Bug、架构、代码质量 |
| 🟡 Low | 5 | 性能、代码质量 |

### 最紧急修复项

| 优先级 | 问题 | 影响 |
|--------|------|------|
| P0 | WebSocket 连接无身份验证 | 任意用户可冒充他人接收通知 |
| P0 | WebSocket 端点完全开放 | 无认证即可连接 |
| P1 | WebSocket 测试接口无权限 | 可向任意用户发送通知 |
| P1 | CORS 配置过于宽松 | 跨站 WebSocket 劫持风险 |

### 整体评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 安全性 | 🔴 需改进 | WebSocket 安全存在严重漏洞 |
| 可靠性 | ✅ 良好 | 整体架构稳定 |
| 性能 | ✅ 一般 | 缺少连接数限制 |
| 可维护性 | 🟠 需改进 | SecurityConfig 过于复杂 |
| 架构合规性 | 🟠 需改进 | 附件服务绕过 Repository 层 |

### 关键建议

1. **立即修复 WebSocket 身份验证**: 使用 JWT Token 验证连接
2. **移除或保护测试接口**: 生产环境禁用或添加权限控制
3. **添加 WebSocket 连接限制**: 防止资源耗尽
4. **重构附件服务**: 使用标准的 Repository 模式

---

**审计完成日期:** 2026-04-06
**下一步行动:** 修复 WebSocket 安全漏洞后再部署生产环境