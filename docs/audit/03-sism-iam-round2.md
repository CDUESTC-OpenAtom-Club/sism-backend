# 二轮审计报告：sism-iam 模块（身份认证与访问管理）

**审计日期:** 2026-04-13
**审计范围:** 51个Java源文件（含基础设施层、测试），涵盖认证、授权、用户管理、角色权限、通知等。
**参照基准:** 一轮审计报告 `02-sism-iam.md`（2026-04-12，共32项问题）

---

## 一、一轮问题修复状态总览

| 严重性 | 总数 | 已修复 | 部分修复 | 未修复 |
|--------|------|--------|----------|--------|
| **Critical** | 6 | 5 | 1 | 0 |
| **High** | 7 | 3 | 0 | 4 |
| **Medium** | 10 | 0 | 1 | 9 |
| **Low** | 9 | 0 | 0 | 9 |
| **总计** | **32** | **8** | **2** | **22** |

**修复率: 25%（8/32完全修复，2/32部分修复）**

---

## 二、Critical 严重（6项）

### C-01. JWT 硬编码默认密钥 -- FIXED

**文件:** `application/JwtTokenService.java:23,42-50`

**修复验证:**
```java
// Line 23 - 已移除硬编码默认值
@Value("${app.jwt.secret:${jwt.secret:}}")
private String secret;

// Lines 42-50 - 新增 @PostConstruct 校验
@PostConstruct
void validateConfiguration() {
    if (secret == null || secret.isBlank()) {
        throw new IllegalStateException("JWT secret must be configured");
    }
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
        throw new IllegalStateException("JWT secret must be at least 256 bits (32 bytes)");
    }
}
```

启动时强制校验密钥配置，缺少或长度不足直接失败。修复完善。

---

### C-02. Token 黑名单内存存储 -- FIXED

**文件:** `application/JwtTokenService.java:32`, `sism-shared-kernel/.../TokenBlacklistService.java`

**修复验证:** `JwtTokenService` 不再自行管理黑名单集合，改用 `TokenBlacklistService`（shared-kernel 统一服务）：
- Redis 主存储：使用 `redisTemplate.opsForValue().set(key, "1", ttl)` 设置带 TTL 的键值
- 本地降级：`ConcurrentHashMap<String, Instant>` 带 TTL 过期 + 5分钟定期清理 + 10,000 上限淘汰
- SHA-256 哈希后存储 Token，避免明文泄露

此问题已由架构层面的 `TokenBlacklistService` 彻底解决。

---

### C-03. 登出端点为空操作 -- FIXED

**文件:** `interfaces/rest/AuthController.java:104-110`

**修复验证:**
```java
@PostMapping("/logout")
public ResponseEntity<ApiResponse<Void>> logout(
        @RequestHeader(value = "Authorization", required = false) String authorization) {
    if (authorization != null && authorization.startsWith("Bearer ")) {
        authService.logout(authorization.substring(7));
    }
    return ResponseEntity.ok(ApiResponse.success(null));
}
```
`authService.logout()` 调用 `jwtTokenService.blacklistToken(token)`，完整链路打通。

---

### C-04. 注册接口无访问控制 -- FIXED

**文件:** `interfaces/rest/AuthController.java:52`

**修复验证:** `@PreAuthorize("hasRole('ADMIN')")` 注解已添加。仅管理员可注册用户。

---

### C-05. 用户管理接口全部无权限控制 -- FIXED

**文件:** `interfaces/rest/AuthController.java:129-257`

**修复验证:** 所有 9 个用户管理端点均已添加 `@PreAuthorize("hasRole('ADMIN')")`：
- `GET /users` (line 129)
- `GET /users/{id}` (line 157)
- `GET /users/username/{username}` (line 169)
- `GET /users/org/{orgId}` (line 181)
- `POST /users` (line 193)
- `PUT /users/{id}` (line 211)
- `DELETE /users/{id}` (line 230)
- `POST /users/{id}/lock` (line 241)
- `POST /users/{id}/unlock` (line 253)

---

### C-06. 告警事件接口无任何认证和授权 -- FIXED

**文件:** `interfaces/rest/NotificationController.java:80-213`

**修复验证:** 所有告警事件端点已添加权限控制：
- 查询类：`@PreAuthorize("isAuthenticated()")` (lines 80, 95, 105, 115, 125, 137, 149)
- 写入类：`@PreAuthorize("hasRole('ADMIN')")` (lines 159, 186, 198, 213)
- 用户通知 `/my`：使用 `@AuthenticationPrincipal` 校验 (line 45-48)

**遗留问题:** `/read-all`（line 62-66）和 `/{id}/read`（line 68-72）两个端点仍然缺少 `@AuthenticationPrincipal` 校验，任何已认证用户可为任意用户标记已读（详见 NEW-04）。

---

## 三、High 高（7项）

### H-01. UserService.createUser() 密码明文存储 -- FIXED

**文件:** `application/service/UserService.java:50`

**修复验证:**
```java
user.setPassword(passwordEncoder.encode(password));
```
`AuthService.register()` (line 90) 同样使用 `passwordEncoder.encode(password)`。密码全部加密存储。

---

### H-02. 删除角色时查询逻辑错误 -- UNFIXED

**文件:** `interfaces/rest/RoleManagementController.java:149`

**现状:** 仍然使用 `roleRepository.findByUserId(id)`，其中 `id` 是被删除的角色 ID，而非用户 ID。

```java
// Line 149 - BUG 仍在
List<Role> userRoles = roleRepository.findByUserId(id);
```

`JpaRoleRepository.findByUserId()` 内部实现为 `jpaRepository.findByUsers_Id(userId)`（`JpaRoleRepository.java:42`），语义是"查找包含指定用户 ID 的角色列表"。传入角色 ID 意味着查询"哪些角色的用户集合中包含 ID 等于角色 ID 的用户"，结果几乎恒为空，导致角色永远可以被删除，即使有用户正在使用。

**最优修复方案:** 替换为 `userRepository.findByRoleId(id)`，检查返回列表是否为空：

```java
List<User> usersWithRole = userRepository.findByRoleId(id);
if (!usersWithRole.isEmpty()) {
    return ResponseEntity.badRequest().body(ApiResponse.error("Role is still in use by users"));
}
```

**影响:** 线上角色可被误删，导致用户权限丢失。

---

### H-03. 角色列表 N+1 查询 -- UNFIXED

**文件:** `interfaces/rest/RoleManagementController.java:235-247`

**现状:** `convertToResponse()` 方法仍然逐角色执行：
```java
response.setPermissionCount(role.getPermissions().size());   // Line 242 - 懒加载触发额外 SQL
response.setUserCount(userRepository.findByRoleId(role.getId()).size()); // Line 243 - 每个角色一次查询
```

10 个角色产生至少 20 次额外 SQL 查询（每个角色 1 次权限加载 + 1 次用户统计）。

**最优修复方案:**

1. 使用 `@EntityGraph(attributePaths = "permissions")` 加载角色时预加载权限
2. 使用批量查询替代逐个统计：
```java
// 批量统计
Set<Long> roleIds = rolePage.getContent().stream().map(Role::getId).collect(Collectors.toSet());
Map<Long, Long> userCounts = userRepository.countUsersByRoleIds(roleIds);
Map<Long, Integer> permCounts = rolePage.getContent().stream()
    .collect(Collectors.toMap(Role::getId, r -> r.getPermissions().size()));
```
3. 使用 JPQL 投影直接返回 DTO，避免懒加载问题

**影响:** 角色列表页面响应时间随数据量线性增长，数据库压力随并发用户数倍增。

---

### H-04. 用户列表全量加载 + N+1 查询 -- UNFIXED

**文件:** `interfaces/rest/AuthController.java:128-151`

**现状:**
```java
// Line 135 - 全量加载所有用户到内存
List<UserListItemResponse> allUsers = userService.findAll().stream()
        .map(this::toUserListItemResponse)
        .toList();

// Lines 139-148 - 内存分页（全量数据已在内存）
var userPage = new PageImpl<>(
        allUsers.subList(start, end),
        PageRequest.of(safePage, safeSize),
        allUsers.size()
);
```

`toUserListItemResponse()` (line 362) 访问 `user.getRoles()` 触发 N+1 查询。即使 `JpaUserRepositoryInternal.findByUsername` 使用了 `@EntityGraph(attributePaths = "roles")`，但 `findAll()` 没有使用 `@EntityGraph`。

**最优修复方案:**

1. `UserRepository` 新增分页方法 `Page<User> findAll(Pageable pageable)` 带 `@EntityGraph("roles")`
2. 新增计数查询 `long count()`
3. Controller 使用数据库分页：
```java
Page<User> userPage = userService.findPaged(pageable);
```
4. 删除 `PaginationPolicy` 已定义但未使用的 `toPageRequest()` 应用

**影响:** 1000+ 用户时 API 响应时间秒级，内存占用 MB 级，生产环境定时炸弹。

---

### H-05. 刷新Token丢失角色信息 -- FIXED

**文件:** `application/JwtTokenService.java:119-143`

**修复验证:**
```java
// Line 131 - 从旧 Token 中提取角色信息
List<String> roleCodes = getRolesFromToken(refreshToken);

// Line 133 - 将角色信息传入新 Token
String newAccessToken = generateToken(user, roleCodes);
```

刷新后角色信息完整保留。`getRolesFromToken()` (lines 96-105) 正确处理了 null 和空值。

---

### H-06. 登录防暴力破解机制完全未生效 -- FIXED

**文件:** `application/service/AuthService.java:27,42-56`

**修复验证:** `AuthService` 完整集成了 `LoginAttemptService`：
```java
// Line 42 - 登录前检查是否被封禁
loginAttemptService.assertNotBlocked(request.getUsername(), clientKey);

// Line 48 - 密码错误记录失败
loginAttemptService.recordFailure(request.getUsername(), clientKey);

// Line 56 - 登录成功清除计数
loginAttemptService.recordSuccess(request.getUsername(), clientKey);
```

`InMemoryLoginAttemptService` 配置：最多 5 次失败（line 16），锁定 900 秒（line 19），使用 `ConcurrentHashMap.compute()` 原子操作。

---

### H-07. UserProfileController 绕过应用层直连仓储 -- UNFIXED

**文件:** `interfaces/rest/UserProfileController.java:35-36`

**现状:** Controller 仍然直接注入 `UserRepository` 和 `PasswordEncoder`：
```java
private final UserRepository userRepository;
private final PasswordEncoder passwordEncoder;
```

而 `UserProfileService`（完整的正确实现）已存在但未被使用：
- `UserProfileService.findCurrentUser()` -- 正确的用户查找
- `UserProfileService.updateProfile()` -- 正确的资料更新
- `UserProfileService.changePassword()` -- 正确的密码修改，包含 `PasswordPolicy.validateLength()` 校验

**Controller 的 `changePassword()` (line 105) 缺少 `PasswordPolicy.validateLength()` 校验**，允许设置弱密码（如 1 位密码）。

**最优修复方案:** Controller 改为注入 `UserProfileService`，移除直接的 Repository 和 PasswordEncoder 依赖：
```java
private final UserProfileService userProfileService;
```

**影响:** 密码策略形同虚设，用户可通过个人中心设置任意长度弱密码。

---

## 四、Medium 中等（10项）

### M-01. markAllNotificationsAsRead 返回硬编码 readCount=5 -- UNFIXED

**文件:** `application/service/NotificationService.java:163-168`

**现状:** 仍然返回硬编码值：
```java
result.put("readCount", 5);
```

`NotificationController` (line 64-66) 调用的是 `NotificationService`（旧实现），而非 `UserNotificationService`（正确实现，`UserNotificationService.java:189-195` 使用 `markAllAsRead` 返回真实计数）。

**最优修复方案:** NotificationController 的 `/read-all` 端点改为调用 `UserNotificationService.markAllNotificationsAsRead(currentUserId)`，同时传入当前用户 ID 实现按用户标记。

---

### M-02. LoginResponse 冗余字段 -- UNFIXED

**文件:** `application/dto/LoginResponse.java:17-22`

**现状:** 仍然同时存在两套冗余字段：
```java
private String token;           // Line 17 - 与 accessToken 重复
private String accessToken;     // Line 18
private String type = "Bearer"; // Line 21 - 与 tokenType 重复
private String tokenType = "Bearer"; // Line 22
```

**最优修复方案:** 保留 `accessToken`/`tokenType`，移除 `token`/`type`，添加 `@JsonIgnore` 或使用 `@JsonProperty` 兼容旧客户端。

---

### M-03. Notification 实体命名与职责不符 -- UNFIXED

**文件:** `domain/Notification.java`

**现状:** 类名 `Notification` 映射 `alert_event` 表。代码注释已添加说明（"映射到: alert_event 表"），但命名仍然容易与 `UserNotification`（真正的用户通知）混淆。

**最优修复方案:** 重命名为 `AlertEvent`，包路径移至告警模块（如 `com.sism.alert.domain.AlertEvent`）。如暂不移出，至少重命名避免歧义。

---

### M-04. Permission 未继承 AggregateRoot -- UNFIXED

**文件:** `domain/Permission.java:13`

**现状:** `public class Permission` 是唯一不继承 `AggregateRoot` 的领域实体。缺少 `validate()` 方法，缺少审计时间戳的 `@PrePersist`/`@PreUpdate` 回调。

**最优修复方案:**
```java
public class Permission extends AggregateRoot<Long> {
    @Override
    public void validate() {
        if (permissionCode == null || permissionCode.isBlank())
            throw new IllegalArgumentException("Permission code is required");
        if (permissionName == null || permissionName.isBlank())
            throw new IllegalArgumentException("Permission name is required");
    }
}
```

---

### M-05. getMyNotifications 忽略 userId 参数 -- UNFIXED

**文件:** `application/service/NotificationService.java:133-147`

**现状:** 仍然忽略 `userId` 参数，返回所有用户的告警事件：
```java
// Line 135 - 未过滤 userId
Page<Notification> allNotifications = notificationRepository.findAll(pageable);
```

`NotificationController` `/my` 端点 (lines 39-59) 传入的 `userId` 被完全丢弃。任何已认证用户可看到所有用户的告警通知。

**影响:** 信息泄露 -- 用户 A 可以看到用户 B 的告警通知内容。

---

### M-06. RoleService.createRole() 返回未持久化对象 -- UNFIXED

**文件:** `application/service/RoleService.java:23-39`

**现状:** `createRole()` 只创建对象不持久化：
```java
return role;  // Line 38 - 未调用 repository.save()
```

Controller (line 93) 和 `RoleManagementService` (line 70) 各自调用 `roleRepository.save()`，职责分散。三个层级（Controller -> RoleManagementService -> RoleService）中，创建和持久化被分离到不同调用方，违反单一职责。

**最优修复方案:** `RoleService.createRole()` 内部完成持久化：
```java
@Transactional
public Role createRole(String roleCode, String roleName, String description) {
    Role role = new Role();
    // ... set fields ...
    role.validate();
    return roleRepository.save(role);
}
```
Controller 和 RoleManagementService 不再需要自己调用 save。

---

### M-07. InMemoryLoginAttemptService 非原子操作 -- PARTIAL

**文件:** `application/service/InMemoryLoginAttemptService.java:23-37`

**部分修复:** `recordFailure()` 已改用 `ConcurrentHashMap.compute()` 原子操作（line 44），解决了写入竞态。

**遗留问题:** `assertNotBlocked()` (lines 23-37) 中 get + remove 仍为非原子操作：
```java
AttemptState state = attempts.get(buildKey(username, clientKey));  // Line 24
// ... 中间可能被其他线程修改 ...
if (state.lockedUntil() != null && !state.lockedUntil().isAfter(now)) {
    attempts.remove(buildKey(username, clientKey));  // Line 36 - 非原子
}
```

TOCTOU 窗口：线程 A 检查到锁定已过期但尚未 remove 时，线程 B 可能同时进入 recordFailure 重新锁定。

**实际影响:** 低。因为即使并发触发，最坏结果是一个本应清除的过期记录被新的失败计数覆盖，不会导致安全绕过。

---

### M-08. UserNotificationService JSON 手动拼接 -- UNFIXED

**文件:** `application/service/UserNotificationService.java:93-105,239-244`

**现状:** 仍然使用文本块手动拼接 JSON，`escapeJson()` 仅处理反斜杠和双引号：
```java
private static String escapeJson(String value) {
    if (value == null) return "";
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
}
```

**未处理的字符:** 换行符 `\n`、制表符 `\t`、回车符 `\r`、控制字符 `\u0000-\u001F`、Unicode 特殊字符。如果 `indicatorName`、`targetOrgName`、`senderUserName` 或 `reason` 包含换行或制表符，生成的 JSON 将格式错误（malformed JSON），可能导致前端解析失败。

**最优修复方案:** 使用 `Jackson ObjectMapper` 或 `JSONObject` 构建结构化 JSON：
```java
ObjectMapper mapper = new ObjectMapper();
Map<String, Object> metadata = Map.of(
    "indicatorId", indicatorId,
    "indicatorName", indicatorName,
    "targetOrgName", targetOrgName,
    "senderUserName", senderUserName,
    "source", normalizedSource,
    "reason", reason
);
String metadataJson = mapper.writeValueAsString(metadata);
```

---

### M-09. Controller 与 Service 大量重复逻辑 -- UNFIXED

**文件:** `interfaces/rest/RoleManagementController.java` 全文, `application/service/RoleManagementService.java` 全文

**现状:** `RoleManagementService` 已创建（包含完整的 CRUD 逻辑），但 `RoleManagementController` 完全未使用它。Controller 仍直接注入 4 个仓储：
```java
// Lines 38-41
private final RoleService roleService;
private final RoleRepository roleRepository;
private final UserRepository userRepository;
private final PermissionRepository permissionRepository;
```

**重复逻辑对照表:**

| 功能 | Controller 代码位置 | RoleManagementService 对应方法 | 状态 |
|------|---------------------|-------------------------------|------|
| 创建角色 | Lines 77-95 | `createRole()` (Lines 63-71) | 重复 |
| 更新角色 | Lines 99-135 | `updateRole()` (Lines 74-96) | 重复 |
| 删除角色 | Lines 139-156 | `deleteRole()` (Lines 98-106) | 重复 |
| 分配权限 | Lines 160-188 | `assignPermissions()` (Lines 109-120) | 重复 |
| 移除权限 | Lines 207-231 | `removePermission()` (Lines 130-137) | 重复 |

**最优修复方案:** Controller 改为仅注入 `RoleManagementService`，移除所有直接仓储依赖。

---

### M-10. User 实体与基类字段冲突 -- UNFIXED

**文件:** `domain/User.java:56-60`, `sism-shared-kernel/.../Entity.java:18-21`

**现状:** `Entity<ID>` 基类声明了 `@Transient` 的 `createdAt`/`updatedAt` 字段：
```java
// Entity.java
@Transient
protected LocalDateTime createdAt;
@Transient
protected LocalDateTime updatedAt;
```

`User` 子类又声明了同名字段带 `@Column`：
```java
// User.java:56-60
@Column(name = "created_at")
private LocalDateTime createdAt;
@Column(name = "updated_at")
private LocalDateTime updatedAt;
```

Java 字段隐藏（field hiding）+ JPA 注解冲突：基类的 `@Transient` 与子类的 `@Column` 同时作用于同名字段。虽然 Hibernate 通常按子类注解优先处理，但这种模式：
1. 违反 DDD 中聚合根的统一基类设计
2. 使 IDE 和静态分析工具产生混淆警告
3. `Role`、`Notification`、`UserNotification` 实体也存在同样的双重声明

**最优修复方案:** 从 `Entity<ID>` 基类移除 `@Transient` 的 `createdAt`/`updatedAt` 字段，改为在聚合根基类中定义带 `@Column` 的时间戳字段，或在具体实体中统一使用 `@PrePersist`/`@PreUpdate` 管理时间戳。

---

## 五、Low 低（9项）

| # | 一轮编号 | 状态 | 文件:行号 | 问题说明 |
|---|---------|------|-----------|----------|
| 1 | L-01 | **UNFIXED** | `LoginRequest.java:9-11` | 仍然缺少 `@NotBlank` 校验注解，`AuthService.login()` 用手写 null/blank 判断替代 |
| 2 | L-02 | **UNFIXED** | `UserDetailsServiceImpl.java:42-44` | 空角色码仍赋予 `ROLE_UNKNOWN` 权限，应过滤掉空白角色码 |
| 3 | L-03 | **UNFIXED** | `AuthController.java:54-60` | 注册端点返回类型为 `ApiResponse<User>`，暴露完整 User 实体（虽有 `@JsonIgnore` 在 password 上） |
| 4 | L-04 | **UNFIXED** | `AuthController.java:133` | `defaultValue = "100"` 无上限，`PaginationPolicy.MAX_SIZE` 已定义但未使用 |
| 5 | L-05 | **UNFIXED** | `CurrentUser.java:53-55` | `getPassword()` 仍返回 null。虽不影响运行，但违反 `UserDetails` 契约 |
| 6 | L-06 | **UNFIXED** | `NotificationController.java:161-169` | POST 创建资源仍使用 `@RequestParam`，应改为 `@RequestBody` 接收 JSON |
| 7 | L-07 | **UNFIXED** | `RoleManagementController.java:175` | `Set.copyOf(request.getPermissionIds())` 当 `permissionIds` 为 null 时抛 NPE |
| 8 | L-08 | **UNFIXED** | `NotificationStatus.java` 全文 | 枚举已定义但从未被引用，属于死代码 |
| 9 | L-09 | **UNFIXED** | `UserProfileController.java:179-183` | `UpdateProfileRequest` 仍含 `email`/`phone` 字段，但 User 实体无这些字段，请求参数被静默丢弃 |

---

## 六、二轮新增问题

### NEW-01. [High] NotificationController 的用户通知端点调用了错误的服务

**文件:** `interfaces/rest/NotificationController.java:39-72`

**描述:** `/my`、`/read-all`、`/read` 三个端点调用 `NotificationService`（处理 `alert_event` 表），而非 `UserNotificationService`（处理 `sys_user_notification` 表）。

- `NotificationService.getMyNotifications()` (line 133-147) 忽略 userId 参数，返回所有告警事件
- `NotificationService.markAllNotificationsAsRead()` (line 163-168) 返回硬编码 readCount=5，不操作数据库
- `NotificationService.markNotificationAsRead()` (line 152-158) 只返回一个 Map，不更新数据库

正确的实现已存在于 `UserNotificationService`：
- `getMyNotifications()` -- 按 userId 过滤，支持状态筛选
- `markAllNotificationsAsRead()` -- 真正执行 `markAllAsRead` SQL
- `markNotificationAsRead()` -- 验证 recipientUserId 后执行更新

**影响:** 用户通知功能完全失效 -- 看到的是所有用户的告警事件而非自己的通知，标记已读操作不生效。

**最优修复方案:** NotificationController 注入 `UserNotificationService`，三个端点改为调用该服务的对应方法。

---

### NEW-02. [High] NotificationService.createNotification 提前持久化，actualPercent/gapPercent/expectedPercent 永不存储

**文件:** `application/service/NotificationService.java:79`, `interfaces/rest/NotificationController.java:171-179`

**描述:** `NotificationService.createNotification()` 在 line 79 执行了 `notificationRepository.save(notification)`。Controller 在 lines 175-177 尝试设置 `actualPercent`/`expectedPercent`/`gapPercent`，但此时数据已经持久化且未再次保存。

```java
// NotificationService.java:79 - 此时 actualPercent 等字段为 null
return notificationRepository.save(notification);

// NotificationController.java:175-179 - 设置了但不再保存
if (actualPercent != null) notification.setActualPercent(actualPercent);
// ... 没有第二次 save 调用
return ResponseEntity.status(201).body(notification);
```

此外，`Notification` 实体定义 `actualPercent`/`expectedPercent`/`gapPercent` 为 `nullable = false`（lines 43-49），但 `createNotification()` 不设置这些字段，会导致 NOT NULL 约束冲突（取决于数据库是否严格校验）。

**最优修复方案:** 将 `actualPercent`/`expectedPercent`/`gapPercent` 作为参数传入 `createNotification()` 方法，在 save 之前设置完毕。

---

### NEW-03. [High] RoleManagementService 已创建但未被任何地方调用

**文件:** `application/service/RoleManagementService.java` 全文

**描述:** `RoleManagementService` 包含完整的角色 CRUD 逻辑（139行代码），但全项目无任何类注入或调用它。`RoleManagementController` 仍直接注入并使用 `RoleService` + 各个 Repository。

这属于"半完成的重构"：创建了正确的应用层服务，但未完成 Controller 的切换。`RoleManagementService` 是纯粹的死代码。

**影响:** 代码维护混乱，未来开发者不确定应该使用哪个 Service。

---

### NEW-04. [Medium] NotificationController /read-all 和 /read 端点缺少用户身份校验

**文件:** `interfaces/rest/NotificationController.java:62-72`

**描述:** 两个标记已读端点均无 `@AuthenticationPrincipal` 参数：
```java
// Line 62-66 - 无用户身份校验
@PostMapping("/read-all")
public ResponseEntity<ApiResponse<Map<String, Object>>> markAllNotificationsAsRead() {
    return ResponseEntity.ok(ApiResponse.success(notificationService.markAllNotificationsAsRead()));
}

// Line 68-72 - 无用户身份校验，任何人可为任意通知标记已读
@PostMapping("/{id}/read")
public ResponseEntity<ApiResponse<Map<String, Object>>> markNotificationAsRead(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(notificationService.markNotificationAsRead(id)));
}
```

任何已认证用户可为任意用户的全量通知标记已读，或为任意 ID 的通知标记已读。

**最优修复方案:** 添加 `@AuthenticationPrincipal CurrentUser currentUser` 参数，并传入 `currentUser.getId()` 调用 `UserNotificationService.markAllNotificationsAsRead(currentUserId)` / `markNotificationAsRead(id, currentUserId)`。

---

### NEW-05. [Medium] UserProfileService 已创建但未被任何地方调用

**文件:** `application/service/UserProfileService.java` 全文

**描述:** 与 NEW-03 类似，`UserProfileService`（71行代码）包含正确的用户资料和密码管理逻辑，但 `UserProfileController` 未使用它，仍直接操作 `UserRepository`。

`UserProfileService.changePassword()` 调用了 `PasswordPolicy.validateLength()`，而 Controller 的直接实现缺少此校验。

---

### NEW-06. [Medium] AuthService.register() 和 UserService.createUser() 均未调用 PasswordPolicy

**文件:** `application/service/AuthService.java:76-95`, `application/service/UserService.java:33-57`

**描述:** 两个创建用户的方法都直接接受明文密码后加密存储，但均未调用 `PasswordPolicy.validateLength()` 校验密码强度。`UserProfileService.changePassword()` (line 59) 是唯一调用密码策略的地方，但它未被使用。

```java
// AuthService.register() - Lines 82-91
if (password == null || password.isBlank()) {
    throw new IllegalArgumentException("请输入密码");
}
// 缺少: PasswordPolicy.validateLength(password);
user.setPassword(passwordEncoder.encode(password));
```

**影响:** 用户可通过注册接口或管理员创建接口设置 1 位密码。

**最优修复方案:** 在 `AuthService.register()` 和 `UserService.createUser()` 中添加 `PasswordPolicy.validateLength(password)` 调用。

---

### NEW-07. [Medium] JwtTokenService 存在无参构造器导致黑名单失效

**文件:** `application/JwtTokenService.java:38-40`

**描述:** 无参构造器将 `tokenBlacklistService` 设为 null：
```java
public JwtTokenService() {
    this.tokenBlacklistService = null;
}
```

`isBlacklisted()` (line 179) 在 `tokenBlacklistService` 为 null 时直接返回 false，使黑名单完全失效。虽然 Spring DI 通常使用有参构造器，但无参构造器的存在是一个安全后门 -- 如果未来有人通过反射或非 DI 方式实例化，Token 将永远不被拦截。

**最优修复方案:** 移除无参构造器，或将其标记为 `@Deprecated` 并在内部抛出异常。

---

### NEW-08. [Medium] RoleManagementController.convertToResponse 可能触发 LazyInitializationException

**文件:** `interfaces/rest/RoleManagementController.java:235-247`

**描述:** `listRoles()` (line 46-60) 未标注 `@Transactional(readOnly = true)`，Controller 方法默认不在事务中执行。`roleRepository.findAll(pageable)` 返回的 Role 实体的 `permissions` 集合为 `FetchType.LAZY`（`Role.java:45`）。当 `convertToResponse()` 访问 `role.getPermissions().size()` (line 242) 时，Hibernate Session 已关闭，将抛出 `LazyInitializationException`。

**最优修复方案:** 在 Controller 方法上添加 `@Transactional(readOnly = true)`，或改用 `@EntityGraph` 预加载权限。

---

### NEW-09. [Low] JpaRoleRepository.findAll(Pageable) 缺少 @Override 注解

**文件:** `infrastructure/persistence/JpaRoleRepository.java:65`

**描述:** `findAll(Pageable)` 方法实现了 `RoleRepository` 接口的同名方法，但缺少 `@Override` 注解。其他所有方法均有 `@Override`。此方法签名与接口完全匹配，仅为代码风格不一致。

---

### NEW-10. [Low] AuthService.logout() 不校验 Token 有效性即加入黑名单

**文件:** `application/service/AuthService.java:104-106`

**描述:** `logout()` 直接调用 `jwtTokenService.blacklistToken(token)`，不校验 Token 是否有效、是否已过期或是否为合法格式。恶意请求可通过此接口向黑名单注入大量无效字符串。

```java
public void logout(String token) {
    jwtTokenService.blacklistToken(token);
}
```

**最优修复方案:** 在加入黑名单前校验 Token 格式（如 `jwtTokenService.validateToken(token)`），或至少验证非空非空白（`JwtTokenService.blacklistToken()` 已有 null/blank 检查）。

---

## 七、汇总统计

### 按严重性统计

| 严重性 | 一轮遗留 | 二轮新增 | 总计 |
|--------|----------|----------|------|
| **Critical** | 0（全部修复或降级） | 0 | 0 |
| **High** | 4（H-02, H-03, H-04, H-07） | 3（NEW-01, NEW-02, NEW-03） | **7** |
| **Medium** | 9（M-01~M-10 除 M-07 部分） | 5（NEW-04~NEW-08） | **14** |
| **Low** | 9（L-01~L-09） | 2（NEW-09, NEW-10） | **11** |
| **总计** | **22** | **10** | **32** |

### 按类别统计

| 类别 | 数量 | 关键问题 |
|------|------|----------|
| 安全 | 4 | 用户通知信息泄露(M-05, NEW-04), 密码策略绕过(NEW-06), Token黑名单后门(NEW-07) |
| 性能 | 3 | N+1查询(H-03), 全量加载(H-04), 缺事务(H-02导致错误删除) |
| Bug | 5 | 查询逻辑错误(H-02), 数据未存储(NEW-02), 服务调用错误(NEW-01) |
| 架构 | 7 | 死代码(NEW-03, NEW-05), 重复逻辑(M-06, M-09), 分层违反(H-07) |
| 代码质量 | 13 | 冗余字段(M-02), 命名不符(M-03), 基类冲突(M-10), 校验缺失等 |

### 修复进度对比

```
一轮修复前:  6 Critical + 7 High + 10 Medium + 9 Low = 32 问题
二轮修复后:  0 Critical + 7 High   + 14 Medium + 11 Low  = 32 问题
```

Critical 问题全部消除，说明安全基础已夯实。但 High/Medium 问题未减反增（10 个新增），根因是"半完成重构"：创建了正确的 Service 层（`RoleManagementService`、`UserProfileService`、`UserNotificationService`）但未将 Controller 切换到新实现，导致死代码和重复逻辑。

---

## 八、优先修复建议

### 第一优先级（本周）

| 顺序 | 问题 | 原因 |
|------|------|------|
| 1 | **H-02** 删除角色查询逻辑错误 | 可导致线上角色被误删 |
| 2 | **NEW-01** NotificationController 调用错误服务 | 用户通知功能完全失效 |
| 3 | **NEW-02** 告警事件 actualPercent 等字段永不存储 | 数据写入不完整 |
| 4 | **M-05** getMyNotifications 忽略 userId | 信息泄露 |
| 5 | **NEW-06** 注册/创建用户未校验密码强度 | 弱密码可被创建 |

### 第二优先级（两周内）

| 顺序 | 问题 | 原因 |
|------|------|------|
| 6 | **H-04** 用户列表全量加载 + N+1 | 性能隐患 |
| 7 | **H-03** 角色列表 N+1 查询 | 性能隐患 |
| 8 | **H-07** + **NEW-05** UserProfileController 切换到 UserProfileService | 密码策略绕过 |
| 9 | **M-09** + **NEW-03** RoleManagementController 切换到 RoleManagementService | 消除重复逻辑和死代码 |
| 10 | **M-06** RoleService.createRole 应包含持久化 | 职责归一 |

### 第三优先级（一个月内）

- M-01, M-08: JSON 拼接安全问题
- M-02: LoginResponse 冗余字段清理
- M-03, M-04: 实体命名和基类一致性
- M-10, NEW-08: 基类字段冲突和 LazyInitializationException
- L-01~L-09: 低优先级代码质量改善

---

**审计结论:** 一轮 Critical 问题全部修复，安全基础显著改善。但存在系统性的"重构未完成"问题 -- 新建的正确服务未被接入，旧代码继续运行。建议以 Controller 层的"服务切换"为抓手，集中完成 H-07、M-09、NEW-01、NEW-05 四项切换工作，一次性消除 15+ 遗留问题。
