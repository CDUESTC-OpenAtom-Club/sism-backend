# sism-iam 模块审计报告

**审计日期:** 2026-04-06
**审计人员:** Claude Code (Automated Audit)
**模块版本:** 当前主分支

---

## 模块概览

| 项目 | 数据 |
|------|------|
| 模块名称 | sism-iam |
| 模块职责 | 身份认证与访问管理、用户通知 |
| Java 文件总数 | 45 |
| 分层结构 | DDD风格：domain → application → infrastructure → interfaces |
| 核心实体 | User, Role, Permission, Notification, UserNotification |
| Repository 数量 | 5 |
| Service 数量 | 5 |
| Controller 数量 | 4 |

### 包结构

```
com.sism.iam/
├── application/
│   ├── dto/                    # 数据传输对象
│   │   ├── CurrentUser.java
│   │   ├── LoginRequest.java
│   │   └── LoginResponse.java
│   ├── JwtTokenService.java    # JWT Token 服务
│   ├── UserDetailsServiceImpl.java
│   └── service/
│       ├── AuthService.java
│       ├── NotificationService.java
│       ├── RoleService.java
│       ├── UserNotificationService.java
│       └── UserService.java
├── domain/
│   ├── User.java
│   ├── Role.java
│   ├── Permission.java
│   ├── Notification.java
│   ├── UserNotification.java
│   └── repository/
│       ├── UserRepository.java
│       ├── RoleRepository.java
│       ├── PermissionRepository.java
│       ├── NotificationRepository.java
│       └── UserNotificationRepository.java
├── infrastructure/
│   ├── IamModuleConfig.java
│   └── persistence/
│       ├── JpaUserRepository.java
│       ├── JpaUserRepositoryInternal.java
│       ├── JpaRoleRepository.java
│       ├── JpaRoleRepositoryInternal.java
│       ├── JpaPermissionRepository.java
│       ├── JpaNotificationRepository.java
│       ├── JpaUserNotificationRepository.java
│       └── ...Internal.java
└── interfaces/
    └── rest/
        ├── AuthController.java
        ├── NotificationController.java
        ├── RoleManagementController.java
        └── UserProfileController.java
```

---

## 一、安全漏洞

### 🔴 Critical: JWT Secret 硬编码默认值

**文件:** `JwtTokenService.java`
**行号:** 21-22

```java
@Value("${jwt.secret:SismSecretKeyForJWTTokenGeneration2024VeryLongSecretKey}")
private String secret;
```

**问题描述:**
JWT 密钥使用硬编码的默认值。如果配置文件未设置 `jwt.secret`，系统将使用公开的默认密钥。攻击者可利用此密钥伪造任意用户的 JWT Token，完全绕过认证系统。

**风险影响:**
- 攻击者可伪造任意用户身份
- 可获取管理员权限
- 完全绕过认证系统

**严重等级:** 🔴 **Critical**

**建议修复:**
```java
@Value("${jwt.secret}")
private String secret;

@PostConstruct
public void validateSecret() {
    if (secret == null || secret.length() < 256 / 8) {
        throw new IllegalStateException("JWT secret must be configured and at least 32 characters");
    }
}
```

---

### 🔴 Critical: 用户注册接口无权限控制

**文件:** `AuthController.java`
**行号:** 50-59

```java
@PostMapping("/register")
@Operation(summary = "用户注册")
public ResponseEntity<ApiResponse<User>> register(@RequestBody RegisterRequest request) {
    User user = authService.register(
            request.getUsername(),
            request.getPassword(),
            request.getRealName()
    );
    return ResponseEntity.ok(ApiResponse.success(user));
}
```

**问题描述:**
用户注册接口完全开放，没有任何权限控制。任何人都可以创建新用户账户。

**风险影响:**
- 恶意批量注册攻击
- 创建未授权账户
- 系统数据污染
- 资源耗尽攻击

**严重等级:** 🔴 **Critical**

**建议修复:**
```java
@PostMapping("/register")
@PreAuthorize("hasRole('ADMIN')")
@Operation(summary = "用户注册（仅管理员）")
public ResponseEntity<ApiResponse<User>> register(@RequestBody RegisterRequest request) {
    // ...
}
```

---

### 🔴 High: 用户查询接口无权限控制

**文件:** `AuthController.java`
**行号:** 123-145

```java
@GetMapping("/users")
@Operation(summary = "查询所有用户")
public ResponseEntity<ApiResponse<UserListPageResponse>> getAllUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "100") int size
) {
    List<UserListItemResponse> allUsers = userService.findAll().stream()
            .map(this::toUserListItemResponse)
            .toList();
    // ...
}
```

**问题描述:**
`/api/v1/auth/users` 接口完全开放，无 `@PreAuthorize` 注解。任何人都可以获取所有用户的详细信息。

**风险影响:**
- 用户信息泄露
- 可用于后续定向攻击
- 违反数据保护法规

**严重等级:** 🔴 **High**

**建议修复:**
```java
@GetMapping("/users")
@PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
@Operation(summary = "查询所有用户")
public ResponseEntity<ApiResponse<UserListPageResponse>> getAllUsers(...) {
    // ...
}
```

---

### 🔴 High: 用户 CRUD 操作权限控制不一致

**文件:** `AuthController.java`
**行号:** 183-243

```java
@PostMapping("/users")  // ❌ 无权限控制
public ResponseEntity<ApiResponse<UserSummaryResponse>> createUser(...) { }

@DeleteMapping("/users/{id}")  // ❌ 无权限控制
public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) { }

@PostMapping("/users/{id}/lock")  // ❌ 无权限控制
public ResponseEntity<ApiResponse<Void>> lockUser(@PathVariable Long id) { }
```

**对比:** `RoleManagementController.java`
```java
@PostMapping
@PreAuthorize("hasRole('ADMIN')")  // ✅ 有权限控制
public ResponseEntity<ApiResponse<RoleResponse>> createRole(...) { }
```

**问题描述:**
用户管理的敏感操作没有权限控制，而角色管理的相同操作有控制。权限控制策略不一致。

**风险影响:**
- 未授权的用户创建/删除
- 账户锁定滥用

**严重等级:** 🔴 **High**

**建议修复:** 为所有用户管理接口添加 `@PreAuthorize("hasRole('ADMIN')")`

---

### 🟠 Medium: Token 验证不检查黑名单

**文件:** `JwtTokenService.java`
**行号:** 58-64

```java
public boolean validateToken(String token) {
    try {
        return !isTokenExpired(token);
    } catch (Exception e) {
        return false;
    }
}
```

**问题描述:**
1. Token 验证仅检查过期时间，未检查是否已被加入黑名单
2. 用户登出后 Token 仍有效
3. 异常被吞掉，无法区分问题原因

**风险影响:**
- 登出后 Token 仍可使用
- 无法强制用户下线

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
public boolean validateToken(String token) {
    try {
        Claims claims = extractClaims(token);
        return !isTokenExpired(claims) && !tokenBlacklistService.isBlacklisted(token);
    } catch (ExpiredJwtException e) {
        log.warn("Token expired");
        return false;
    } catch (JwtException e) {
        log.warn("Invalid token");
        return false;
    }
}
```

---

### 🟠 Medium: 头像上传路径遍历风险

**文件:** `UserProfileController.java`
**行号:** 231-246

```java
private String saveAvatarFile(MultipartFile file, Long userId) throws IOException {
    String originalFilename = file.getOriginalFilename();  // ❌ 未验证
    String extension = "";
    if (originalFilename != null && originalFilename.contains(".")) {
        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
    }
    // ...
}
```

**问题描述:**
`originalFilename` 直接来自客户端，可能包含路径遍历字符。扩展名直接从原始文件名提取未做验证。

**风险影响:**
- 可能的路径遍历攻击
- 恶意文件扩展名

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
String extension = "";
int dotIndex = originalFilename.lastIndexOf('.');
if (dotIndex > 0) {
    extension = originalFilename.substring(dotIndex);
    if (!extension.matches("^\\.[a-zA-Z0-9]{1,5}$")) {
        extension = ".jpg";
    }
}
```

---

## 二、潜在 Bug 和逻辑错误

### 🔴 High: UserService.createUser 未验证密码强度且密码未加密

**文件:** `UserService.java`
**行号:** 30-55

```java
@Transactional
public User createUser(String username, String password, ...) {
    if (username == null || username.isBlank()) {
        throw new IllegalArgumentException("Username is required");
    }
    // ❌ 缺少密码强度验证
    // ❌ 密码未加密！

    User user = new User();
    user.setPassword(password);  // ❌ 严重Bug：密码未加密！
    // ...
}
```

**问题描述:**
1. **严重Bug:** `createUser` 方法直接将原始密码设置到用户对象，未进行加密
2. 缺少密码强度验证

**对比:** `AuthService.register()` 正确实现:
```java
user.setPassword(passwordEncoder.encode(password));  // ✅ 正确
```

**风险影响:**
- 数据库存储明文密码
- 用户凭证泄露风险

**严重等级:** 🔴 **High**

**建议修复:**
```java
public User createUser(String username, String password, ...) {
    // 验证密码强度
    if (password == null || password.length() < 8) {
        throw new IllegalArgumentException("Password must be at least 8 characters");
    }

    User user = new User();
    user.setPassword(passwordEncoder.encode(password));  // 加密密码
    // ...
}
```

---

### 🟠 Medium: RoleManagementController 删除角色检查逻辑错误

**文件:** `RoleManagementController.java`
**行号:** 148-156

```java
@DeleteMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
    // ...
    List<Role> userRoles = roleRepository.findByUserId(id);  // ❌ 参数错误！
    if (!userRoles.isEmpty()) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Role is still in use by users"));
    }
}
```

**问题描述:**
`findByUserId(id)` 方法参数 `id` 是角色ID，但方法语义是"根据用户ID查找角色"。当前逻辑会错误地查找某个用户的所有角色，而不是使用该角色的用户列表。

**风险影响:**
- 删除角色检查失效
- 可能删除正在使用的角色

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
List<User> usersWithRole = userRepository.findByRoleId(id);
if (!usersWithRole.isEmpty()) {
    return ResponseEntity.badRequest().body(ApiResponse.error("Role is still in use by users"));
}
```

---

### 🟠 Medium: NotificationService.getMyNotifications 数据泄露

**文件:** `NotificationService.java`
**行号:** 133-147

```java
public Page<Map<String, Object>> getMyNotifications(Long userId, int page, int size, String status) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Notification> allNotifications = notificationRepository.findAll(pageable);  // ❌ 返回所有告警事件
    // userId 参数完全未使用
}
```

**问题描述:**
`userId` 参数完全未使用，返回所有系统级告警事件而非用户特定通知。

**风险影响:**
- 数据泄露
- 用户可见其他用户的告警信息

**严重等级:** 🟠 **Medium**

**建议修复:** 删除此遗留方法，使用 `UserNotificationService.getMyNotifications()` 的正确实现。

---

## 三、性能瓶颈

### 🟠 Medium: AuthController.getAllUsers 内存分页

**文件:** `AuthController.java`
**行号:** 123-145

```java
@GetMapping("/users")
public ResponseEntity<ApiResponse<UserListPageResponse>> getAllUsers(...) {
    List<UserListItemResponse> allUsers = userService.findAll().stream()  // ❌ 加载所有用户到内存
            .map(this::toUserListItemResponse)
            .toList();

    var userPage = new PageImpl<>(  // ❌ 内存中手动分页
            allUsers.subList(start, end), ...);
}
```

**问题描述:**
先加载所有用户到内存，然后在内存中手动分页。如果有 10,000+ 用户，会造成大量内存占用和不必要的延迟。

**风险影响:**
- 内存溢出风险
- 响应延迟
- 数据库压力

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
@GetMapping("/users")
public ResponseEntity<ApiResponse<UserListPageResponse>> getAllUsers(...) {
    Pageable pageable = PageRequest.of(page, size);
    Page<User> userPage = userRepository.findAll(pageable);  // 数据库分页

    Page<UserListItemResponse> responsePage = userPage.map(this::toUserListItemResponse);
    return ResponseEntity.ok(ApiResponse.success(UserListPageResponse.fromPage(responsePage)));
}
```

---

### 🟠 Medium: N+1 查询问题 - Role/Permission EAGER 加载

**文件:** `Role.java`
**行号:** 45-52

```java
@ManyToMany(fetch = FetchType.EAGER)  // ❌ EAGER 加载
@JoinTable(...)
private Set<Permission> permissions = new HashSet<>();
```

**文件:** `User.java`
**行号:** 48-54

```java
@ManyToMany(fetch = FetchType.EAGER)  // ❌ EAGER 加载
@JoinTable(...)
private Set<Role> roles = new HashSet<>();
```

**问题描述:**
User-Role 和 Role-Permission 都使用 `FetchType.EAGER`，在某些查询场景下会产生 N+1 查询问题。

**风险影响:**
- 数据库查询性能下降
- 不必要的数据加载

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
@ManyToMany(fetch = FetchType.LAZY)
private Set<Role> roles = new HashSet<>();

// 在 Repository 中使用 EntityGraph
@EntityGraph(attributePaths = "roles")
Optional<User> findByUsername(String username);
```

---

### 🟡 Low: RoleManagementController.convertToResponse 每次查询用户数

**文件:** `RoleManagementController.java`
**行号:** 235-247

```java
private RoleResponse convertToResponse(Role role) {
    response.setUserCount(userRepository.findByRoleId(role.getId()).size());  // ❌ 每个角色触发一次查询
}
```

**问题描述:**
在角色列表查询时，每个角色都会触发一次用户数量查询。如果有 20 个角色，会产生 1 + 20 = 21 次数据库查询。

**严重等级:** 🟡 **Low**

**建议修复:** 使用批量查询或缓存用户数统计。

---

## 四、代码质量和可维护性

### 🟠 Medium: Controller 内嵌 DTO 类定义

**文件:** `AuthController.java`
**行号:** 247-366

```java
@lombok.Data
public static class RegisterRequest { ... }

@lombok.Data
public static class CreateUserRequest { ... }
// ... 多个 DTO 定义
```

**问题描述:**
Controller 内定义了 10+ 个 DTO/Response 类，违反了关注点分离原则。

**严重等级:** 🟠 **Medium**

**建议修复:** 将这些 DTO 移至 `com.sism.iam.interfaces.dto` 或 `com.sism.iam.application.dto` 包。

---

### 🟠 Medium: 异常处理过于宽泛

**文件:** `JwtTokenService.java`
**行号:** 58-64

```java
public boolean validateToken(String token) {
    try {
        return !isTokenExpired(token);
    } catch (Exception e) {  // ❌ 捕获所有异常
        return false;
    }
}
```

**问题描述:**
捕获 `Exception` 太宽泛，会吞掉所有异常包括 `NullPointerException` 等。应该只捕获 JWT 相关异常。

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
catch (ExpiredJwtException e) {
    log.warn("Token expired");
    return false;
} catch (JwtException e) {
    log.warn("Invalid token");
    return false;
}
```

---

### 🟡 Low: 魔法字符串 - 状态和类型

**文件:** 多处

```java
notification.setStatus("HANDLED");  // ❌ 魔法字符串
String passwordPattern = "^(?=.*[a-z])...";  // ❌ 未定义为常量
```

**问题描述:**
状态和类型使用硬编码字符串，没有使用枚举或常量。

**建议修复:**
```java
public enum NotificationStatus {
    NEW, ACKNOWLEDGED, HANDLED, CLOSED
}
```

---

### 🟡 Low: 重复代码 - 用户查找和验证

**文件:** `UserProfileController.java`
**行号:** 多处

```java
String username = authentication.getName();
Optional<User> userOpt = userRepository.findByUsername(username);
if (userOpt.isEmpty()) {
    return ResponseEntity.notFound().build();
}
User user = userOpt.get();
```

**问题描述:**
相同的用户查找和验证逻辑重复 4 次。

**建议修复:**
```java
private User getCurrentUserOrThrow(Authentication auth) {
    return userRepository.findByUsername(auth.getName())
            .orElseThrow(() -> new NotFoundException("User not found"));
}
```

---

## 五、架构最佳实践

### 🟠 Medium: Repository 接口定义层违反 DDD

**文件:** `UserRepository.java`
**行号:** 52-54

```java
public interface UserRepository {
    default List<String> findPermissionCodesByUserId(Long userId) {
        return List.of();  // ❌ 领域层接口中的默认实现
    }
}
```

**问题描述:**
领域层的 Repository 接口不应有默认实现。应该由基础设施层实现。

**严重等级:** 🟠 **Medium**

**建议修复:** 移除 default 方法，保持纯接口定义。

---

### 🟠 Medium: Service 层事务处理不完整

**文件:** `RoleService.java`
**行号:** 24-38

```java
@Transactional
public Role createRole(...) {
    Role role = new Role();
    // ...
    return role;  // ❌ 返回未持久化的对象
}
```

**问题描述:**
`RoleService.createRole` 方法标记了 `@Transactional` 但没有调用 Repository 保存对象。实际保存操作在 Controller 中完成。这违反了 Service 层职责。

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
@Transactional
public Role createRole(String roleCode, String roleName, String description) {
    Role role = new Role();
    role.setRoleCode(roleCode);
    role.setRoleName(roleName);
    role.setDescription(description);
    return roleRepository.save(role);  // Service 完成保存
}
```

---

## 六、审计总结

### 问题统计

| 严重等级 | 数量 | 类别分布 |
|----------|------|----------|
| 🔴 Critical | 2 | 安全漏洞（JWT默认密钥、注册接口无权限） |
| 🔴 High | 4 | 安全漏洞、Bug（密码未加密、用户API无权限） |
| 🟠 Medium | 9 | 安全、性能、Bug、代码质量 |
| 🟡 Low | 5 | 代码质量、架构 |

### 最紧急修复项

| 优先级 | 问题 | 影响 |
|--------|------|------|
| P0 | JWT Secret 硬编码默认值 | 认证系统完全失效 |
| P0 | UserService.createUser 密码未加密 | 密码明文存储 |
| P0 | 用户注册接口无权限控制 | 任何人可创建账户 |
| P1 | 用户管理 API 无权限控制 | 未授权的用户操作 |
| P1 | 用户查询 API 无权限控制 | 信息泄露 |

### 整体评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 安全性 | ⚠️ 需改进 | 存在严重的安全漏洞，需立即修复 |
| 可靠性 | ⚠️ 需改进 | 存在密码未加密等严重 Bug |
| 性能 | ✅ 一般 | 存在 N+1 和内存分页问题 |
| 可维护性 | ✅ 良好 | DDD 分层清晰，但 DTO 管理需改进 |
| 架构合规性 | ✅ 良好 | 总体符合 DDD 架构规范 |

---

**审计完成日期:** 2026-04-06
**下一步行动:** 修复 Critical 和 High 级别问题后再部署生产环境