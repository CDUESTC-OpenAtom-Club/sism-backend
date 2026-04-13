# 第二轮审计报告：sism-iam（身份与访问管理）

**审计日期:** 2026-04-13
**审计范围:** 41个Java源文件，涵盖认证、授权、用户管理、角色权限、通知等。
**第一轮审计日期:** 2026-04-12 | **第一轮问题总数:** 32个

---

## 修复总览

| 指标 | 数量 | 占比 |
|------|------|------|
| 第一轮问题总数 | 32 | 100% |
| 已修复 (FIXED) | 13 | 40.6% |
| 部分修复 (PARTIAL) | 3 | 9.4% |
| 未修复 (UNFIXED) | 16 | 50.0% |
| **本轮新发现问题** | **11** | -- |

| 严重性 | 已修复 | 部分修复 | 未修复 |
|--------|--------|----------|--------|
| Critical (6) | 6 | 0 | 0 |
| High (7) | 3 | 0 | 4 |
| Medium (10) | 0 | 3 | 7 |
| Low (9) | 0 | 0 | 9 |

**结论:** 6个Critical级别问题全部修复，安全基座已建立。但High级别仍有4个未修复（含N+1查询、分层违规等），且新发现11个问题，包括刷新Token重放漏洞、角色接口无权限控制等安全风险。

---

## A. 第一轮问题修复状态

### Critical 严重 (共6个)

#### C-01. JWT 硬编码默认密钥 -- FIXED

**文件:** `application/JwtTokenService.java:23`

**修复证据:**
- 第23行: `@Value("${app.jwt.secret:${jwt.secret:}}")` -- 默认值改为空字符串，不再硬编码密钥
- 第43-49行: 新增 `@PostConstruct validateConfiguration()` 方法，校验密钥非空且 >= 256 bits (32 bytes)
- 缺少配置时启动即失败: `throw new IllegalStateException("JWT secret must be configured")`

```java
// 修复后代码 (JwtTokenService.java:42-49)
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

---

#### C-02. Token 黑名单集群失效 + 内存泄漏 -- FIXED

**文件:** `application/JwtTokenService.java:32`

**修复证据:**
- 第32-35行: 使用外部 `TokenBlacklistService` 替代本地 `ConcurrentHashMap.newKeySet()`
- `TokenBlacklistService` (shared-kernel): Redis优先存储 + 本地TTL降级策略
  - Redis TTL 自动过期，无内存泄漏
  - 本地存储有 MAX_LOCAL_BLACKLIST_SIZE=10,000 上限 + 5分钟定期清理
  - Redis 不可用时自动降级到本地缓存
- `blacklistToken()` (第170-176行) 和 `isBlacklisted()` (第178-180行) 均委托给 `TokenBlacklistService`

---

#### C-03. 登出端点为空操作 -- FIXED

**文件:** `interfaces/rest/AuthController.java:104-110`

**修复证据:**
- 第106-109行: 从 Authorization 头提取 Bearer Token 并调用 `authService.logout()`
- `AuthService.java:104-106`: `logout()` 调用 `jwtTokenService.blacklistToken(token)`
- 完整链路: Controller -> AuthService -> JwtTokenService -> TokenBlacklistService

```java
// AuthController.java:104-110
@PostMapping("/logout")
public ResponseEntity<ApiResponse<Void>> logout(
        @RequestHeader(value = "Authorization", required = false) String authorization) {
    if (authorization != null && authorization.startsWith("Bearer ")) {
        authService.logout(authorization.substring(7));
    }
    return ResponseEntity.ok(ApiResponse.success(null));
}
```

---

#### C-04. 注册接口无访问控制 -- FIXED

**文件:** `interfaces/rest/AuthController.java:52`

**修复证据:**
- 第52行: `@PreAuthorize("hasRole('ADMIN')")` -- 仅管理员可注册新用户

---

#### C-05. 用户管理接口全部无权限控制 -- FIXED

**文件:** `interfaces/rest/AuthController.java:129-257`

**修复证据:**
- `getAllUsers` (第129行): `@PreAuthorize("hasRole('ADMIN')")`
- `getUserById` (第157行): `@PreAuthorize("hasRole('ADMIN')")`
- `getUserByUsername` (第169行): `@PreAuthorize("hasRole('ADMIN')")`
- `getUsersByOrgId` (第181行): `@PreAuthorize("hasRole('ADMIN')")`
- `createUser` (第193行): `@PreAuthorize("hasRole('ADMIN')")`
- `updateUser` (第211行): `@PreAuthorize("hasRole('ADMIN')")`
- `deleteUser` (第230行): `@PreAuthorize("hasRole('ADMIN')")`
- `lockUser` (第241行): `@PreAuthorize("hasRole('ADMIN')")`
- `unlockUser` (第251行): `@PreAuthorize("hasRole('ADMIN')")`

---

#### C-06. 告警事件接口无认证和授权 -- FIXED

**文件:** `interfaces/rest/NotificationController.java:78-219`

**修复证据:**
- 读取操作: `@PreAuthorize("isAuthenticated()")` -- 第80/95/105/115/126/137/149行
- 写入操作: `@PreAuthorize("hasRole('ADMIN')")` -- 第159/186/198/213行
- 用户通知读取 (`/my`): 使用 `@AuthenticationPrincipal CurrentUser` 校验身份 (第45行)

---

### High 高 (共7个)

#### H-01. UserService.createUser() 密码明文存储 -- FIXED

**文件:** `application/service/UserService.java:50`

**修复证据:**
- 第50行: `user.setPassword(passwordEncoder.encode(password));`
- 构造器注入 `PasswordEncoder` (第27行)

---

#### H-02. 删除角色时查询逻辑错误 -- UNFIXED

**文件:** `interfaces/rest/RoleManagementController.java:149`

**问题描述:** `roleRepository.findByUserId(id)` 中的 `id` 是来自 `@PathVariable` 的**角色ID**，但 `findByUserId()` 期望的是**用户ID**。这意味着删除角色时检查的是"该角色ID作为用户ID去查用户角色关联"，语义完全错误，无法正确检测角色是否被用户使用。

**当前代码:**
```java
// RoleManagementController.java:148-151
List<Role> userRoles = roleRepository.findByUserId(id);  // id 是角色ID，语义错误
if (!userRoles.isEmpty()) {
    return ResponseEntity.badRequest().body(ApiResponse.error("Role is still in use by users"));
}
```

**最优修复方案:** 改用 `userRepository.findByRoleId(roleId)` 查询关联用户:

```java
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
    // 委托给 RoleManagementService，避免 Controller 直连仓储
    roleManagementService.deleteRole(id);
    return ResponseEntity.ok(ApiResponse.success(null));
}

// RoleManagementService.java 中已有的正确实现:
@Transactional
public void deleteRole(Long id) {
    Role role = roleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
    if (!userRepository.findByRoleId(id).isEmpty()) {  // 正确：用 roleId 查用户
        throw new IllegalStateException("Role is still in use by users");
    }
    roleRepository.delete(role);
}
```

**最佳方案:** 直接使用已有的 `RoleManagementService.deleteRole()` (第98-106行)，该方法使用了正确的 `userRepository.findByRoleId(id)`。

---

#### H-03. 角色列表 N+1 查询 -- UNFIXED

**文件:** `interfaces/rest/RoleManagementController.java:235-247`

**问题描述:** `convertToResponse()` 方法对每个角色触发:
1. `role.getPermissions().size()` -- LAZY加载，每个角色1次SQL
2. `userRepository.findByRoleId(role.getId()).size()` -- 每个角色1次额外SQL
3. 10个角色 = 20+次额外SQL查询

**当前代码:**
```java
// RoleManagementController.java:235-247
private RoleResponse convertToResponse(Role role) {
    // ...
    response.setPermissionCount(role.getPermissions().size());  // N+1: lazy load
    response.setUserCount(userRepository.findByRoleId(role.getId()).size());  // N+1: per-role query
    // ...
}
```

**最优修复方案:** 使用批量查询 + DTO投影，一次性获取所有角色的关联统计:

```java
// 方案1：在 Controller/Service 中使用批量统计
@GetMapping
public ResponseEntity<ApiResponse<PageResult<RoleResponse>>> listRoles(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int pageSize) {

    Page<Role> rolePage = roleRepository.findAll(PageRequest.of(page, pageSize));
    List<Role> roles = rolePage.getContent();

    // 批量查询权限数 -- 使用 EntityGraph 或 JOIN FETCH
    // 批量查询用户数 -- 单次 GROUP BY 查询
    Set<Long> roleIds = roles.stream().map(Role::getId).collect(Collectors.toSet());
    Map<Long, Long> userCounts = roleManagementService.countUsersByRoleIds(roleIds);

    List<RoleResponse> responses = roles.stream().map(role -> {
        RoleResponse resp = new RoleResponse();
        resp.setId(role.getId());
        resp.setRoleCode(role.getRoleCode());
        resp.setRoleName(role.getRoleName());
        resp.setDescription(role.getDescription());
        resp.setIsEnabled(role.getIsEnabled());
        resp.setPermissionCount(role.getPermissions() != null ? role.getPermissions().size() : 0);
        resp.setUserCount(userCounts.getOrDefault(role.getId(), 0L).intValue());
        resp.setCreateTime(role.getCreatedAt());
        return resp;
    }).collect(Collectors.toList());

    return ResponseEntity.ok(ApiResponse.success(PageResult.of(responses,
            rolePage.getTotalElements(), rolePage.getNumber(), rolePage.getSize())));
}

// 方案2（推荐）：在 RoleRepository 添加带 JOIN 的批量查询
@Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions")
Page<Role> findAllWithPermissions(Pageable pageable);
```

---

#### H-04. 用户列表全量加载 + N+1 查询 -- UNFIXED

**文件:** `interfaces/rest/AuthController.java:128-151`

**问题描述:**
1. `userService.findAll()` 加载全部用户到内存 (第135行)
2. `toUserListItemResponse()` 访问 `user.getRoles()` 触发懒加载N+1 (第362行)
3. 先全量加载再内存分页 (第139-148行)，用户量大时性能灾难

**当前代码:**
```java
// AuthController.java:135-148
List<UserListItemResponse> allUsers = userService.findAll().stream()
        .map(this::toUserListItemResponse)  // 触发N+1: user.getRoles()
        .toList();
int start = Math.min(safePage * safeSize, allUsers.size());  // 内存分页
```

**最优修复方案:** 使用数据库分页 + `@EntityGraph` 预加载角色:

```java
// 1. UserRepository 新增分页方法
Page<User> findAll(Pageable pageable);

// 2. JpaUserRepositoryInternal 添加 EntityGraph 分页查询
@EntityGraph(attributePaths = "roles")
Page<User> findAll(Pageable pageable);

// 3. UserService 添加分页方法
public Page<User> findPage(int page, int size) {
    return userRepository.findAll(PageRequest.of(page, Math.min(size, 100)));
}

// 4. AuthController 使用数据库分页
@GetMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<UserListPageResponse>> getAllUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

    int safeSize = Math.min(Math.max(size, 1), 100);
    Page<User> userPage = userService.findPage(page, safeSize);

    Page<UserListItemResponse> dtoPage = userPage.map(this::toUserListItemResponse);
    return ResponseEntity.ok(ApiResponse.success(UserListPageResponse.fromPage(dtoPage)));
}
```

---

#### H-05. 刷新Token丢失角色信息 -- FIXED

**文件:** `application/JwtTokenService.java:119-143`

**修复证据:**
- 第131行: `List<String> roleCodes = getRolesFromToken(refreshToken);` -- 从旧Token提取角色
- 第133-134行: 传入提取的角色码生成新Token

```java
// JwtTokenService.java:119-134
public Map<String, Object> refreshToken(String refreshToken) {
    // ...
    String username = extractUsername(refreshToken);
    Long userId = getUserIdFromToken(refreshToken);
    // ...
    List<String> roleCodes = getRolesFromToken(refreshToken);  // 从旧Token提取
    String newAccessToken = generateToken(user, roleCodes);     // 传入角色
    // ...
}
```

---

#### H-06. 登录防暴力破解机制未集成 -- FIXED

**文件:** `application/service/AuthService.java:42,48,56`

**修复证据:**
- 第27行: 注入 `LoginAttemptService`
- 第42行: 登录前检查 `loginAttemptService.assertNotBlocked(request.getUsername(), clientKey);`
- 第48行: 密码错误时记录失败 `loginAttemptService.recordFailure(...)`
- 第56行: 登录成功时清除计数 `loginAttemptService.recordSuccess(...)`

---

#### H-07. UserProfileController 绕过应用层直连仓储 -- UNFIXED

**文件:** `interfaces/rest/UserProfileController.java:35-36`

**问题描述:** Controller 仍然直接注入 `UserRepository` 和 `PasswordEncoder`，完全绕过已有的 `UserProfileService`。`UserProfileService` 已实现了全部所需功能（查用户、改资料、改密码），但未被使用。

**当前代码:**
```java
// UserProfileController.java:35-36 -- 仍在直连仓储
private final UserRepository userRepository;
private final PasswordEncoder passwordEncoder;
```

**同时存在未使用的 UserProfileService:**
```java
// UserProfileService.java -- 已有完整实现但未被Controller使用
@Service
public class UserProfileService {
    public User findCurrentUser(Authentication auth) { ... }
    public User updateProfile(User user, String realName) { ... }
    public User changePassword(User user, String oldPwd, String newPwd, String confirmPwd) {
        PasswordPolicy.validateLength(newPassword);  // 包含密码策略校验
        // ...
    }
}
```

**最优修复方案:** Controller 委托给 UserProfileService:

```java
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;  // 替换 UserRepository + PasswordEncoder

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(Authentication auth) {
        User user = userProfileService.findCurrentUser(auth);
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success(convertToProfileResponse(user)));
    }

    @PostMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request, Authentication auth) {
        User user = userProfileService.findCurrentUser(auth);
        userProfileService.changePassword(user,
                request.getOldPassword(), request.getNewPassword(), request.getConfirmPassword());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

---

### Medium 中等 (共10个)

#### M-01. markAllNotificationsAsRead 返回硬编码 readCount=5 -- PARTIAL

**文件:** `application/service/NotificationService.java:163-168`

**状态分析:**
- `NotificationService.markAllNotificationsAsRead()` (第163-168行): 仍返回硬编码 `readCount=5`
- 但 `UserNotificationService.markAllNotificationsAsRead()` (第189-195行): 已正确实现，调用 `userNotificationRepository.markAllAsRead(currentUserId)` 返回真实更新数
- 问题: `NotificationController` (第64行) 仍调用 `NotificationService` 而非 `UserNotificationService`

**最优修复方案:** NotificationController 的 `markAllNotificationsAsRead` 应委托给 `UserNotificationService`，并传入当前用户ID:

```java
// NotificationController.java
@PostMapping("/read-all")
public ResponseEntity<ApiResponse<Map<String, Object>>> markAllNotificationsAsRead(
        @AuthenticationPrincipal CurrentUser currentUser) {
    if (currentUser == null) {
        return ResponseEntity.status(401).body(ApiResponse.error(2000, "未登录"));
    }
    return ResponseEntity.ok(ApiResponse.success(
            userNotificationService.markAllNotificationsAsRead(currentUser.getId())));
}
```

---

#### M-02. LoginResponse 冗余字段 token/accessToken 和 type/tokenType -- UNFIXED

**文件:** `application/dto/LoginResponse.java:17-22`

**当前代码:** 仍同时存在 `token`/`accessToken` 和 `type`/`tokenType` 冗余字段对。

**最优修复方案:** 保留 `accessToken`/`tokenType` (更标准)，移除 `token`/`type`，添加 `@JsonProperty` 兼容旧客户端:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    @Deprecated(since = "2.0", forRemoval = true)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // 仅反序列化兼容
    private String token; // 废弃：使用 accessToken

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String tokenType; // 固定 "Bearer"
    private Long userId;
    private String username;
    private String realName;
    private UserInfo user;
}
```

---

#### M-03. Notification 实体命名不符 -- UNFIXED

**文件:** `domain/Notification.java`

**问题:** 实体名"Notification"映射 `alert_event` 表，与告警模块语义混叠。同时存在 `UserNotification` 实体（真正的用户通知）。

**最优修复方案:** 重命名为 `AlertEvent`:

```java
@Entity
@Table(name = "alert_event")
@Access(AccessType.FIELD)
public class AlertEvent extends AggregateRoot<Long> {
    // 字段不变，仅类名更正
}
// 同步重命名: NotificationRepository -> AlertEventRepository
// 同步重命名: NotificationService -> AlertEventService
```

---

#### M-04. Permission 未继承 AggregateRoot -- UNFIXED

**文件:** `domain/Permission.java:13`

**问题:** `Permission` 是唯一不继承 `AggregateRoot` 的领域实体，与其他实体不一致。

**最优修复方案:**
```java
@Getter
@Setter
@Entity
@Table(name = "sys_permission", schema = "public")
public class Permission extends AggregateRoot<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ... 其余字段不变

    @Override
    public void validate() {
        if (permissionCode == null || permissionCode.isBlank()) {
            throw new IllegalArgumentException("Permission code is required");
        }
        if (permissionName == null || permissionName.isBlank()) {
            throw new IllegalArgumentException("Permission name is required");
        }
    }
}
```

---

#### M-05. getMyNotifications 忽略 userId 参数 -- UNFIXED

**文件:** `application/service/NotificationService.java:133-147`

**问题描述:** `getMyNotifications(Long userId, ...)` 接收 `userId` 但完全忽略，调用 `notificationRepository.findAll(pageable)` 返回所有用户的告警事件。

**当前代码:**
```java
// NotificationService.java:133-147
public Page<Map<String, Object>> getMyNotifications(Long userId, int page, int size, String status) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Notification> allNotifications = notificationRepository.findAll(pageable); // 忽略userId!
    // ...
}
```

**最优修复方案:** NotificationController 的 `/my` 端点应使用 `UserNotificationService.getMyNotifications()` (已有正确实现):

```java
// NotificationController.java -- 修改 /my 端点
@GetMapping("/my")
public ResponseEntity<ApiResponse<Map<String, Object>>> getMyNotifications(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String status,
        @AuthenticationPrincipal CurrentUser currentUser) {
    if (currentUser == null) {
        return ResponseEntity.status(401).body(ApiResponse.error(2000, "未登录"));
    }
    // 使用 UserNotificationService 而非 NotificationService
    Page<Map<String, Object>> notifications =
            userNotificationService.getMyNotifications(currentUser.getId(), page, size, status);
    // ...
}
```

---

#### M-06. RoleService.createRole() 返回未持久化对象 -- UNFIXED

**文件:** `application/service/RoleService.java:23-39`

**问题描述:** `createRole()` 标注了 `@Transactional` 但不执行任何持久化操作，返回一个瞬态(transient)对象。Controller 端另行调用 `roleRepository.save()`。

**当前代码:**
```java
// RoleService.java:23-39
@Transactional
public Role createRole(String roleCode, String roleName, String description) {
    // ... 创建对象但未保存
    return role; // transient object
}

// RoleManagementController.java:88-94
Role role = roleService.createRole(...); // 未持久化
role = roleRepository.save(role);        // Controller直连仓储保存
```

**最优修复方案:** `RoleService.createRole()` 应内部完成持久化:

```java
@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    @Transactional
    public Role createRole(String roleCode, String roleName, String description) {
        Role role = new Role();
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setDescription(description);
        role.setIsEnabled(true);
        return roleRepository.save(role);  // 内部完成持久化
    }
}
```

---

#### M-07. InMemoryLoginAttemptService TOCTOU 竞态 -- PARTIAL

**文件:** `application/service/InMemoryLoginAttemptService.java:23-38`

**状态分析:**
- `recordFailure()` (第41-55行): 已使用 `ConcurrentHashMap.compute()` 原子操作
- `assertNotBlocked()` (第23-37行): 仍使用非原子的 `get()` + `remove()` 模式

**最优修复方案:** 使用 `computeIfPresent()` 原子操作:

```java
@Override
public void assertNotBlocked(String username, String clientKey) {
    String key = buildKey(username, clientKey);
    Instant now = Instant.now();

    AtomicReference<IllegalStateException> blocked = new AtomicReference<>(null);
    attempts.computeIfPresent(key, (k, state) -> {
        if (state.lockedUntil() != null && state.lockedUntil().isAfter(now)) {
            long remaining = Duration.between(now, state.lockedUntil()).toSeconds();
            blocked.set(new IllegalStateException(
                    "登录失败次数过多，请在 " + Math.max(remaining, 1) + " 秒后重试"));
            return state; // 保持锁定状态
        }
        return null; // 原子移除已过期条目
    });

    if (blocked.get() != null) {
        throw blocked.get();
    }
}
```

---

#### M-08. UserNotificationService JSON 手动拼接不安全 -- UNFIXED

**文件:** `application/service/UserNotificationService.java:93-105,239-244`

**问题描述:** `escapeJson()` 仅转义 `\` 和 `"`，未处理换行符 `\n`、制表符 `\t`、回车 `\r` 及其他控制字符。攻击者可通过注入特殊字符构造恶意 JSON。

**当前代码:**
```java
// UserNotificationService.java:239-244
private static String escapeJson(String value) {
    if (value == null) return "";
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
    // 缺少: \n \r \t \b \f 及其他控制字符
}
```

**最优修复方案:** 使用 `Jackson` 的 `ObjectMapper` 或完整转义:

```java
private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

private String buildMetadataJson(Long indicatorId, String indicatorName,
        String targetOrgName, String senderUserName, String source, String reason) {
    try {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("indicatorId", indicatorId);
        metadata.put("indicatorName", indicatorName);
        metadata.put("targetOrgName", targetOrgName);
        metadata.put("senderUserName", senderUserName);
        metadata.put("source", source);
        metadata.put("reason", reason);
        return OBJECT_MAPPER.writeValueAsString(metadata);
    } catch (JsonProcessingException e) {
        throw new IllegalStateException("Failed to serialize metadata", e);
    }
}
```

---

#### M-09. RoleManagementController 与 RoleManagementService 大量重复逻辑 -- UNFIXED

**文件:** `interfaces/rest/RoleManagementController.java` 全文

**问题描述:** `RoleManagementService` 已封装了完整的角色 CRUD 逻辑（创建/更新/删除/权限分配），但 Controller 完全未使用，仍自行实现所有业务逻辑 + 直连仓储。

**当前架构问题:**
- Controller (第38-41行): 注入 `RoleService` + `RoleRepository` + `UserRepository` + `PermissionRepository` -- 4个依赖
- `RoleManagementService` (第22-29行): 已封装相同的 `RoleService` + `RoleRepository` + `UserRepository` + `PermissionRepository`

**最优修复方案:** Controller 仅保留 HTTP 层关注点，全部业务委托:

```java
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleManagementController {
    private final RoleManagementService roleManagementService; // 单一依赖

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request) {
        Role role = roleManagementService.createRole(
                request.getRoleCode(), request.getRoleName(), request.getDescription());
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(role)));
    }

    // 其他端点同理...
}
```

---

#### M-10. User.java createdAt/updatedAt 与基类字段冲突 -- PARTIAL

**文件:** `domain/User.java:56-60`

**状态分析:**
- `Entity<ID>` 基类中 `id`/`createdAt`/`updatedAt` 标注 `@Transient`，JPA 忽略
- `User` 子类定义了 `@Column(name = "created_at")` 和 `@Column(name = "updated_at")`，JPA 使用这些字段
- **功能正确:** JPA 映射工作正常，数据能正确持久化
- **设计隐患:** 字段遮蔽(field shadowing)，基类的 `markUpdated()` 方法修改的是基类的 `@Transient` 字段，而非子类的 `@Column` 字段。调用 `markUpdated()` 不会触发数据库更新。

**最优修复方案:** 统一时间戳管理到基类:

```java
// Entity.java -- 移除 @Transient，让子类继承基类字段
public abstract class Entity<ID> {
    @Column(name = "created_at", updatable = false)
    protected LocalDateTime createdAt;

    @Column(name = "updated_at")
    protected LocalDateTime updatedAt;
}

// User.java -- 删除重复字段，继承基类即可
@Entity
@Table(name = "sys_user")
public class User extends AggregateRoot<Long> {
    // 移除 createdAt / updatedAt 字段声明
    // 继承基类的 @Column 字段
}
```

---

### Low 低 (共9个)

#### L-01. LoginRequest 缺少 @NotBlank 校验 -- UNFIXED

**文件:** `application/dto/LoginRequest.java:9-12`

**问题:** `username` 和 `password` 字段无 `@NotBlank` 注解。`AuthService.login()` 中有手动空值检查 (第34-38行)，但这是运行时校验而非声明式校验，不触发 Spring MVC 的 `MethodArgumentNotValidException`。

**最优修复方案:**
```java
@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

---

#### L-02. UserDetailsServiceImpl 空角色码赋予 ROLE_UNKNOWN -- UNFIXED

**文件:** `application/UserDetailsServiceImpl.java:41-48`

**问题:** 空角色码仍赋予 `ROLE_UNKNOWN`，可能被误用于权限判断。

**最优修复方案:** 过滤空角色码:
```java
private SimpleGrantedAuthority toAuthority(String roleCode) {
    if (roleCode == null || roleCode.isBlank()) {
        return null; // 或 throw new IllegalArgumentException("Invalid role code");
    }
    return new SimpleGrantedAuthority(
            roleCode.startsWith("ROLE_") ? roleCode : "ROLE_" + roleCode);
}
// 在 loadUserByUsername 中过滤 null:
List<SimpleGrantedAuthority> authorities = roleCodes.stream()
        .map(this::toAuthority)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
```

---

#### L-03. 注册返回完整 User 实体 -- UNFIXED

**文件:** `interfaces/rest/AuthController.java:54-61`

**问题:** `register()` 返回 `ResponseEntity<ApiResponse<User>>`，暴露 `password_hash`（虽然标注了 `@JsonIgnore`）和其他内部字段。

**最优修复方案:** 使用已有的 `UserSummaryResponse` DTO:
```java
@PostMapping("/register")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<UserSummaryResponse>> register(@RequestBody RegisterRequest request) {
    User user = authService.register(request.getUsername(), request.getPassword(), request.getRealName());
    return ResponseEntity.ok(ApiResponse.success(UserSummaryResponse.fromUser(user)));
}
```

---

#### L-04. 分页默认100条无上限 -- UNFIXED

**文件:** `interfaces/rest/AuthController.java:133`

**问题:** `@RequestParam(defaultValue = "100") int size` 无上限约束，可传入 `size=999999` 导致全量加载。

**最优修复方案:** 使用已有的 `PaginationPolicy`:
```java
@GetMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<UserListPageResponse>> getAllUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    PageRequest pageRequest = PaginationPolicy.toPageRequest(page, size); // 强制 max=100
    // ...
}
```

---

#### L-05. CurrentUser.getPassword() 返回 null -- UNFIXED

**文件:** `application/dto/CurrentUser.java:53-55`

**问题:** 违反 `UserDetails` 契约。Spring Security 某些内部代码可能依赖此方法返回非 null 值。

**最优修复方案:** 存储密码的哈希占位符:
```java
public class CurrentUser implements UserDetails {
    private static final String PASSWORD_PLACEHOLDER = "[PROTECTED]";

    @Override
    public String getPassword() {
        return PASSWORD_PLACEHOLDER;
    }
}
```

---

#### L-06. POST 创建资源使用 @RequestParam -- UNFIXED

**文件:** `interfaces/rest/NotificationController.java:158-169`

**问题:** `createNotification()` 使用多个 `@RequestParam` 而非 `@RequestBody` DTO，违反 RESTful 规范。

**最优修复方案:**
```java
@PostMapping
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Notification> createNotification(
        @Valid @RequestBody CreateNotificationRequest request) {
    // ...
}

@Data
public static class CreateNotificationRequest {
    @NotNull private Long indicatorId;
    @NotNull private Long ruleId;
    @NotNull private Long windowId;
    @NotBlank private String severity;
    @NotBlank private String status;
    private BigDecimal actualPercent;
    private BigDecimal expectedPercent;
    private BigDecimal gapPercent;
}
```

---

#### L-07. permissionIds 可能为 null 导致 NPE -- UNFIXED

**文件:** `interfaces/rest/RoleManagementController.java:175`

**问题:** `Set.copyOf(request.getPermissionIds())` -- 如果 `permissionIds` 为 null，抛出 `NullPointerException`。

**最优修复方案:**
```java
@Data
public static class AssignPermissionsRequest {
    @NotNull(message = "permissionIds is required")
    private List<Long> permissionIds;
}
// 或在代码中防御:
Set<Long> permissionIds = Optional.ofNullable(request.getPermissionIds())
        .filter(list -> !list.isEmpty())
        .map(Set::copyOf)
        .orElseThrow(() -> new IllegalArgumentException("permissionIds must not be empty"));
```

---

#### L-08. NotificationStatus 枚举死代码 -- UNFIXED

**文件:** `domain/NotificationStatus.java`

**问题:** 枚举已定义但全项目无任何引用。

**最优修复方案:** 删除该枚举，或在 Notification 实体中使用它替代 String 类型的 status 字段:
```java
// 方案1: 删除死代码
// 删除 NotificationStatus.java

// 方案2: 在 Notification 中使用
@Enumerated(EnumType.STRING)
@Column(name = "status", nullable = false)
private NotificationStatus status;
```

---

#### L-09. UpdateProfileRequest 含 email/phone 但 User 实体无这些字段 -- UNFIXED

**文件:** `interfaces/rest/UserProfileController.java:175-186`

**问题:** `UpdateProfileRequest` 包含 `email` 和 `phone` 字段（带校验注解），但 `User` 实体无对应字段，数据永远无法持久化。`updateProfile()` 方法仅保存 `realName` (第68行)。

**最优修复方案:** 移除无用字段，或为 User 实体添加对应列:
```java
// 方案1: 移除无用字段 (推荐)
@Data
public static class UpdateProfileRequest {
    @NotBlank(message = "Real name is required")
    private String realName;
    private String avatar;
}

// 方案2: 为 User 添加字段
@Column(name = "email")
private String email;

@Column(name = "phone")
private String phone;
```

---

## B. 第二轮新发现问题

### NEW-01. [High] 刷新 Token 未失效，存在重放攻击风险

**文件:** `application/JwtTokenService.java:119-143`

**问题:** `refreshToken()` 方法生成新的 access/refresh token 后，未将旧 refresh token 加入黑名单。攻击者截获 refresh token 后可在有效期内无限次刷新，即使用户已"登出"（access token 失效但 refresh token 仍可用）。

**最优修复方案:**
```java
public Map<String, Object> refreshToken(String refreshToken) {
    if (!validateToken(refreshToken)) {
        throw new IllegalArgumentException("Invalid refresh token");
    }

    // 验证是 refresh token 而非 access token
    Claims claims = extractClaims(refreshToken);
    if (!"refresh".equals(claims.get("type", String.class))) {
        throw new IllegalArgumentException("Not a refresh token");
    }

    String username = claims.getSubject();
    Long userId = claims.get("userId", Long.class);
    Long orgId = claims.get("orgId", Long.class);
    List<String> roleCodes = getRolesFromToken(refreshToken);

    // 将旧 refresh token 加入黑名单 -- 防止重放
    blacklistToken(refreshToken);

    User user = new User();
    user.setId(userId);
    user.setUsername(username);
    user.setOrgId(orgId);

    String newAccessToken = generateToken(user, roleCodes);
    String newRefreshToken = generateRefreshToken(user, roleCodes);

    Map<String, Object> result = new HashMap<>();
    result.put("accessToken", newAccessToken);
    result.put("refreshToken", newRefreshToken);
    result.put("expiresIn", expiration / 1000);
    result.put("tokenType", "Bearer");
    return result;
}
```

---

### NEW-02. [High] 角色列表/详情接口无权限控制

**文件:** `interfaces/rest/RoleManagementController.java:46-70`

**问题:** `listRoles()` (第46行) 和 `getRoleById()` (第62行) 无 `@PreAuthorize` 注解。任何已认证用户（甚至可能是未认证用户）都能获取完整的角色列表和角色详情，包含权限数量、用户数量等敏感信息。

**当前代码:**
```java
@GetMapping  // 无 @PreAuthorize
public ResponseEntity<ApiResponse<PageResult<RoleResponse>>> listRoles(...) { ... }

@GetMapping("/{id}")  // 无 @PreAuthorize
public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable Long id) { ... }
```

**最优修复方案:**
```java
@GetMapping
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<PageResult<RoleResponse>>> listRoles(...) { ... }

@GetMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable Long id) { ... }

// 如果需要普通用户查看自己的角色，提供独立的只读端点:
@GetMapping("/me")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<ApiResponse<List<String>>> getMyRoles(...) { ... }
```

---

### NEW-03. [High] 密码修改后未失效已有 Token

**文件:** `interfaces/rest/UserProfileController.java:80-110`

**问题:** 用户修改密码后，旧的 JWT Token 仍然有效。攻击者获取旧 Token 后，即使受害者已修改密码，攻击者仍可使用旧 Token 访问系统。

**最优修复方案:** 密码修改后，将用户所有已签发的 Token 加入黑名单:

```java
// 方案1: 使用 Token 版本号（推荐）
// User 实体添加字段:
@Column(name = "token_version")
private Integer tokenVersion = 0;

// JWT Token 的 claims 中包含 tokenVersion:
claims.put("tokenVersion", user.getTokenVersion());

// 验证时检查版本号:
public boolean validateToken(String token) {
    Claims claims = extractClaims(token);
    Long userId = claims.get("userId", Long.class);
    Integer tokenVersion = claims.get("tokenVersion", Integer.class);
    User user = userRepository.findById(userId).orElse(null);
    return user != null && tokenVersion.equals(user.getTokenVersion());
}

// 修改密码时递增版本号:
@Transactional
public User changePassword(...) {
    // ... 验证和加密
    user.setTokenVersion(user.getTokenVersion() + 1); // 旧Token自动失效
    return userRepository.save(user);
}

// 方案2: 简单实现 -- 黑名单化当前 Token
@PostMapping("/password")
public ResponseEntity<ApiResponse<Void>> changePassword(
        @RequestBody ChangePasswordRequest request,
        @RequestHeader("Authorization") String authorization,
        Authentication auth) {
    User user = userProfileService.findCurrentUser(auth);
    userProfileService.changePassword(user, ...);
    // 将当前 Token 加入黑名单
    if (authorization.startsWith("Bearer ")) {
        jwtTokenService.blacklistToken(authorization.substring(7));
    }
    return ResponseEntity.ok(ApiResponse.success(null));
}
```

---

### NEW-04. [Medium] 通知已读/全部已读接口无用户身份校验

**文件:** `interfaces/rest/NotificationController.java:62-72`

**问题:** `markAllNotificationsAsRead()` (第62-66行) 和 `markNotificationAsRead()` (第68-72行) 无 `@AuthenticationPrincipal` 参数，任何已认证用户可操作任何通知。且底层 `NotificationService` 使用硬编码假数据（见 M-01）。

**最优修复方案:** 委托给 `UserNotificationService` 并传入当前用户:
```java
@PostMapping("/read-all")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<ApiResponse<Map<String, Object>>> markAllNotificationsAsRead(
        @AuthenticationPrincipal CurrentUser currentUser) {
    if (currentUser == null) {
        return ResponseEntity.status(401).body(ApiResponse.error(2000, "未登录"));
    }
    return ResponseEntity.ok(ApiResponse.success(
            userNotificationService.markAllNotificationsAsRead(currentUser.getId())));
}

@PostMapping("/{id}/read")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<ApiResponse<Map<String, Object>>> markNotificationAsRead(
        @PathVariable Long id,
        @AuthenticationPrincipal CurrentUser currentUser) {
    if (currentUser == null) {
        return ResponseEntity.status(401).body(ApiResponse.error(2000, "未登录"));
    }
    return ResponseEntity.ok(ApiResponse.success(
            userNotificationService.markNotificationAsRead(id, currentUser.getId())));
}
```

---

### NEW-05. [Medium] RoleManagementService 存在但完全未使用

**文件:** `application/service/RoleManagementService.java` 全文 (138行)

**问题:** `RoleManagementService` 是完整的角色管理应用服务，包含创建/更新/删除/权限分配等全部逻辑，但 `RoleManagementController` 未使用任何方法。这是死代码，且与 H-07 (UserProfileService 未使用) 形成相同的架构问题模式。

**最优修复方案:** 见 M-09 修复方案，Controller 委托给 `RoleManagementService`。

---

### NEW-06. [Medium] InMemoryLoginAttemptService 无定时清理任务

**文件:** `application/service/InMemoryLoginAttemptService.java:14`

**问题:** 已过期的登录尝试记录仅在被再次访问时清理 (`assertNotBlocked`)。长期不登录的用户对应的记录永远留在内存中，导致内存泄漏。在生产环境使用 Redis 时此问题不存在，但本地降级模式下会触发。

**最优修复方案:**
```java
@Service
public class InMemoryLoginAttemptService implements LoginAttemptService {
    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    // 每10分钟清理过期记录
    @Scheduled(fixedRate = Duration.ofMinutes(10).toMillis())
    public void purgeExpiredAttempts() {
        Instant now = Instant.now();
        attempts.entrySet().removeIf(entry -> {
            Instant lockedUntil = entry.getValue().lockedUntil();
            return lockedUntil != null && !lockedUntil.isAfter(now);
        });
    }
}
```

---

### NEW-07. [Medium] UserProfileController.changePassword() 未调用 PasswordPolicy

**文件:** `interfaces/rest/UserProfileController.java:80-110`

**问题:** Controller 直接修改密码 (第105行) 但未调用 `PasswordPolicy.validateLength()`。而 `UserProfileService.changePassword()` (第59行) 正确调用了密码策略。这再次证明 H-07 的问题 -- Controller 绕过 Service 层导致业务规则遗漏。

**当前代码:**
```java
// UserProfileController.java:104-106 -- 缺少密码策略校验
user.setPassword(passwordEncoder.encode(request.getNewPassword()));
user.setUpdatedAt(LocalDateTime.now());
userRepository.save(user);
```

**最优修复方案:** 见 H-07，委托给 `UserProfileService.changePassword()`。

---

### NEW-08. [Low] NotificationService 与 UserNotificationService 职责混淆

**文件:** `application/service/NotificationService.java` 和 `application/service/UserNotificationService.java`

**问题:** 两个 Service 都处理"通知"相关逻辑，但 `NotificationService` 实际处理告警事件 (alert_event)，`UserNotificationService` 处理用户通知 (sys_user_notification)。`NotificationController` 同时使用两者但命名不清晰，容易导致维护混淆。

**最优修复方案:**
- `NotificationService` 重命名为 `AlertEventService`
- `NotificationController` 拆分为 `AlertEventController` + `UserNotificationController`
- 或至少在注释和类名中明确区分

---

### NEW-09. [Low] AuthService 使用硬编码的 clientKey

**文件:** `application/service/AuthService.java:41`

**问题:** `loginAttemptService.assertNotBlocked(request.getUsername(), clientKey)` 中 `clientKey` 固定为 `"global"`，失去了按 IP/客户端维度的锁定能力。攻击者可通过更换用户名绕过单个用户维度的限制，或对大量不同用户名各尝试4次而不触发锁定。

**最优修复方案:** 从请求中提取客户端 IP:
```java
// AuthService 或 AuthController 中注入 HttpServletRequest
public LoginResponse login(LoginRequest request, String clientIp) {
    String clientKey = clientIp != null ? clientIp : "global";
    loginAttemptService.assertNotBlocked(request.getUsername(), clientKey);
    // ...
}

// AuthController
@PostMapping("/login")
public ResponseEntity<ApiResponse<LoginResponse>> login(
        @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    String clientIp = extractClientIp(httpRequest);
    LoginResponse response = authService.login(request, clientIp);
    return ResponseEntity.ok(ApiResponse.success(response));
}

private String extractClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isBlank()) ip = request.getHeader("X-Real-IP");
    if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
    return ip.split(",")[0].trim();
}
```

---

### NEW-10. [Low] JwtTokenService 无参构造函数绕过黑名单

**文件:** `application/JwtTokenService.java:38-40`

**问题:** 无参构造函数将 `tokenBlacklistService` 设为 null。如果 Spring 因某种原因使用无参构造函数（例如测试配置、XML配置等），Token 黑名单机制将完全失效。

```java
// JwtTokenService.java:38-40
public JwtTokenService() {
    this.tokenBlacklistService = null; // 黑名单完全禁用
}
```

**最优修复方案:** 移除无参构造函数，确保黑名单服务始终可用:
```java
@Service
public class JwtTokenService {
    private final TokenBlacklistService tokenBlacklistService;

    // 单一构造函数，强制注入
    public JwtTokenService(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = Objects.requireNonNull(tokenBlacklistService,
                "TokenBlacklistService must not be null");
    }

    // blackToken/isBlacklisted 中移除 null 检查
    public void blacklistToken(String token) {
        if (token != null && !token.isBlank()) {
            tokenBlacklistService.blacklist(token);
        }
    }
}
```

---

### NEW-11. [Low] refreshToken 方法未验证 token 类型

**文件:** `application/JwtTokenService.java:119-122`

**问题:** `refreshToken()` 仅调用 `validateToken()`，未检查传入的 token 是否真的是 refresh token。攻击者可用 access token 冒充 refresh token 进行刷新。

**最优修复方案:**
```java
public Map<String, Object> refreshToken(String refreshToken) {
    if (!validateToken(refreshToken)) {
        throw new IllegalArgumentException("Invalid refresh token");
    }

    Claims claims = extractClaims(refreshToken);

    // 验证 token 类型
    String tokenType = claims.get("type", String.class);
    if (!"refresh".equals(tokenType)) {
        throw new IllegalArgumentException("Not a refresh token");
    }

    // ... 后续逻辑
}
```

---

## C. 总结

### Top 5 优先修复项

| 优先级 | 编号 | 严重性 | 问题 | 影响 |
|--------|------|--------|------|------|
| 1 | NEW-01 | **High** | Refresh Token 未失效，重放攻击 | 攻击者可无限刷新Token，即使目标用户已登出 |
| 2 | NEW-02 | **High** | 角色列表接口无权限控制 | 任何用户可枚举全部角色及权限信息 |
| 3 | NEW-03 | **High** | 密码修改后旧Token仍有效 | 修改密码无法驱逐攻击者的已获取Token |
| 4 | H-02 | **High** | 删除角色时查询逻辑错误 | 角色删除绕过用户关联检查，数据一致性风险 |
| 5 | H-04 | **High** | 用户列表全量加载 + N+1查询 | 100+用户时性能严重下降，可被利用DoS |

### 修复进度总结

| 类别 | 修复率 | 说明 |
|------|--------|------|
| Critical | **100%** (6/6) | 认证安全基座已建立 |
| High | **43%** (3/7) | 密码加密、Token刷新、防暴力破解已修复；N+1查询、分层违规、逻辑错误未修复 |
| Medium | **0%** (0/10) | 仅3个部分缓解，7个完全未动 |
| Low | **0%** (0/9) | 全部未修复 |
| 新发现 | -- | 11个新问题（3 High, 4 Medium, 4 Low） |

### 整体评估

第一轮审计的核心安全问题（JWT硬编码、无权限控制、密码明文存储、登出空操作）已全部修复，sism-iam 模块的安全基座已初步建立。但存在以下系统性问题:

1. **架构分层执行不一致:** `RoleManagementService` 和 `UserProfileService` 已编写但 Controller 未使用，形成"有设计无执行"的状态。建议统一要求 Controller 不得直连 Repository。
2. **两套通知系统混淆:** `NotificationService`（告警事件）和 `UserNotificationService`（用户通知）职责不清，Controller 调用了错误的 Service 导致数据泄露（M-05）和假数据返回（M-01）。
3. **Token 生命周期管理不完整:** 黑名单机制已实现，但 refresh token 重放（NEW-01）和密码修改后 Token 未失效（NEW-03）是新暴露的安全漏洞。
4. **性能隐患持续存在:** N+1 查询（H-03, H-04）和全表内存分页未修复，在生产数据量增长后将严重影响系统性能。
