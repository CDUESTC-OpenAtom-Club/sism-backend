# 修复任务 01: sism-iam 角色和用户管理

**模块**: sism-iam  
**实现率**: 17.9%  
**优先级**: 🔴 P0 (最高)  
**工作量**: 40-50 小时  
**关键路径**: 是 (认证和授权基础)

---

## 📋 问题概述

### 完整问题列表

| 问题类型 | 描述 | 影响 | 严重度 |
|---------|------|------|--------|
| 缺失控制器 | RoleManagementController (角色权限管理) | 无法管理角色和权限 | 🔴 致命 |
| 缺失控制器 | UserProfileController (用户个人中心) | 无法查看/编辑个人资料 | 🔴 致命 |
| API路径不匹配 | 文档:/api/v1/users/* → 实际:/api/v1/auth/users/* | 前端集成困难 | 🟡 中等 |
| 认证模式 | 多个端点需要显式userId参数而非Spring Security | 安全问题 | 🟡 中等 |
| 功能实现不完整 | 个人中心只有查询，缺修改/绑定等操作 | 用户体验不足 | 🟡 中等 |
| 权限验证 | 某些端点缺少权限校验 | 安全漏洞 | 🔴 致命 |

### 支撑数据

```
缺失API端点数:  17 个
- 角色管理:     11 个
- 用户个人中心: 6 个

影响模块：10+
需要认证改造的端点: 8+ 个
```

---

## 🎯 缺失的控制器详解

### 1. RoleManagementController (缺失)

**位置**: `sism-iam/src/main/java/.../interface/controller/RoleManagementController.java`

**需要实现的 API**:

```java
@RestController
@RequestMapping("/api/v1/roles")
public class RoleManagementController {

    // 1. 角色查询
    @GetMapping
    public ResponseEntity<PageResult<RoleResponse>> listRoles(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize
    ) { }
    
    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable Long id) { }
    
    // 2. 角色创建
    @PostMapping
    public ResponseEntity<RoleResponse> createRole(
        @Valid @RequestBody CreateRoleRequest request
    ) { }
    
    // 3. 角色更新
    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> updateRole(
        @PathVariable Long id,
        @Valid @RequestBody UpdateRoleRequest request
    ) { }
    
    // 4. 角色删除
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) { }
    
    // 5. 权限管理
    @PostMapping("/{id}/permissions")
    public ResponseEntity<RoleResponse> assignPermissions(
        @PathVariable Long id,
        @RequestBody AssignPermissionsRequest request
    ) { }
    
    @GetMapping("/{id}/permissions")
    public ResponseEntity<List<PermissionResponse>> getRolePermissions(
        @PathVariable Long id
    ) { }
    
    @DeleteMapping("/{id}/permissions/{permissionId}")
    public ResponseEntity<Void> removePermission(
        @PathVariable Long id,
        @PathVariable Long permissionId
    ) { }
    
    // 6. 用户分配
    @PostMapping("/{id}/users")
    public ResponseEntity<Void> assignUsersToRole(
        @PathVariable Long id,
        @RequestBody AssignUsersRequest request
    ) { }
    
    @GetMapping("/{id}/users")
    public ResponseEntity<PageResult<UserResponse>> getRoleUsers(
        @PathVariable Long id,
        @RequestParam(defaultValue = "1") int pageNum
    ) { }
    
    @DeleteMapping("/{id}/users/{userId}")
    public ResponseEntity<Void> removeUserFromRole(
        @PathVariable Long id,
        @PathVariable Long userId
    ) { }
}
```

**需要创建的 DTO 类**:

```java
// 请求类
@Data
class CreateRoleRequest {
    @NotBlank
    private String roleName;
    private String description;
    private Integer priority;
}

@Data
class UpdateRoleRequest {
    private String roleName;
    private String description;
    private Integer priority;
}

@Data
class AssignPermissionsRequest {
    private List<Long> permissionIds;
}

@Data
class AssignUsersRequest {
    private List<Long> userIds;
}

// 响应类
@Data
class RoleResponse {
    private Long id;
    private String roleName;
    private String description;
    private Integer priority;
    private Integer userCount;
    private Integer permissionCount;
    private LocalDateTime createTime;
}

@Data
class PermissionResponse {
    private Long id;
    private String permissionName;
    private String permissionCode;
    private String description;
    private String category;
}
```

**关键实现要点**:

```java
// 1. 权限检查
@Secured({"ROLE_ADMIN"})
public ResponseEntity<RoleResponse> createRole(...) {
    // 只有管理员可以创建角色
}

// 2. 角色重名校验
if (roleService.existsByRoleName(request.getRoleName())) {
    throw new RoleNameDuplicateException();
}

// 3. 权限分配时验证权限存在
Set<Permission> permissions = 
    permissionService.findByIds(request.getPermissionIds());
if (permissions.size() != request.getPermissionIds().size()) {
    throw new PermissionNotFoundException();
}

// 4. 删除角色前检查是否有用户
long userCount = userService.countByRoleId(id);
if (userCount > 0) {
    throw new RoleStillInUseException();
}
```

**工作量**: 15-20 小时

---

### 2. UserProfileController (缺失)

**位置**: `sism-iam/src/main/java/.../interface/controller/UserProfileController.java`

**需要实现的 API**:

```java
@RestController
@RequestMapping("/api/v1/profile")
public class UserProfileController {

    // 1. 获取个人资料
    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(
        @AuthenticationPrincipal UserDetails userDetails
    ) { }
    
    // 2. 更新个人资料
    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
        @Valid @RequestBody UpdateProfileRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) { }
    
    // 3. 修改密码
    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(
        @Valid @RequestBody ChangePasswordRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) { }
    
    // 4. 绑定第三方账号
    @PostMapping("/bind-account")
    public ResponseEntity<Void> bindThirdPartyAccount(
        @Valid @RequestBody BindAccountRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) { }
    
    // 5. 解绑第三方账号
    @DeleteMapping("/accounts/{accountId}")
    public ResponseEntity<Void> unbindAccount(
        @PathVariable Long accountId,
        @AuthenticationPrincipal UserDetails userDetails
    ) { }
    
    // 6. 获取绑定账号列表
    @GetMapping("/accounts")
    public ResponseEntity<List<LinkedAccountResponse>> getLinkedAccounts(
        @AuthenticationPrincipal UserDetails userDetails
    ) { }
}
```

**需要创建的 DTO 类**:

```java
@Data
class UserProfileResponse {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private String department;
    private String position;
    private String avatar;
    private LocalDateTime lastLoginTime;
    private List<String> roles;
}

@Data
class UpdateProfileRequest {
    @NotBlank
    private String realName;
    
    @Email
    private String email;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$")
    private String phone;
    
    private String avatar;
}

@Data
class ChangePasswordRequest {
    @NotBlank
    private String oldPassword;
    
    @NotBlank
    @Size(min = 8)
    private String newPassword;
    
    @NotBlank
    private String confirmPassword;
}

@Data
class BindAccountRequest {
    @NotBlank
    private String platform;  // wechat, qq, dingtalk 等
    
    @NotBlank
    private String accountId;
}

@Data
class LinkedAccountResponse {
    private Long id;
    private String platform;
    private String platformNickname;
    private LocalDateTime bindTime;
}
```

**关键实现要点**:

```java
// 1. 修改密码时验证旧密码
if (!passwordEncoder.matches(
    request.getOldPassword(), 
    user.getPassword())) {
    throw new WrongPasswordException();
}

// 2. 新密码确认
if (!request.getNewPassword()
    .equals(request.getConfirmPassword())) {
    throw new PasswordNotMatchException();
}

// 3. 绑定账号时检查重复
if (linkedAccountService.existsByPlatformAndAccountId(
    request.getPlatform(), 
    request.getAccountId())) {
    throw new AccountAlreadyBindException();
}

// 4. 解绑时检查是否为最后一个账号
if (linkedAccountService.countByUserId(userId) == 1) {
    throw new CannotUnbindLastAccountException();
}
```

**工作量**: 10-15 小时

---

## 🔧 认证模式改造

### 问题描述

当前多个端点要求显式传递 `userId` 参数：

```
❌ 当前做法:
POST /api/v1/approval/instances/{id}/approve?userId=123
POST /api/v1/notifications/user/123/read-all

✅ 正确做法:
使用 Spring Security 的 @AuthenticationPrincipal 注解
```

### 需要改造的端点 (8+)

```
1. /api/v1/approval/instances/my-pending
2. /api/v1/approval/instances/{id}/approve
3. /api/v1/approval/instances/{id}/reject
4. /api/v1/notifications/user/{userId}/*
5. 其他需要当前用户信息的端点
```

### 改造步骤

**步骤1: 创建用户主体包装类**

```java
@Data
@Getter
public class CurrentUser implements Principal {
    private Long id;
    private String username;
    private String email;
    private Set<String> roles;
    
    // 从 Spring Security 提取
    public static CurrentUser from(Authentication auth) {
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        // 转换逻辑
    }
}
```

**步骤2: 修改现有端点**

```java
// ❌ 改造前
@PostMapping("/{id}/approve")
public ResponseEntity<Void> approve(
    @PathVariable Long id,
    @RequestParam Long userId  // ❌ 不安全
) { }

// ✅ 改造后
@PostMapping("/{id}/approve")
public ResponseEntity<Void> approve(
    @PathVariable Long id,
    @AuthenticationPrincipal CurrentUser currentUser  // ✅ 安全
) { 
    // 自动获取当前用户ID
    Long userId = currentUser.getId();
}
```

**步骤3: 添加权限检查**

```java
// 某些操作需要权限检查
@PostMapping("/{id}/approve")
@PreAuthorize("hasRole('APPROVER')")
public ResponseEntity<Void> approve(
    @PathVariable Long id,
    @AuthenticationPrincipal CurrentUser currentUser
) { }
```

**涉及模块**: sism-workflow, sism-execution, sism-iam (核心)  
**工作量**: 8-12 小时

---

## 📋 API 路径规范化

### 问题: 用户 API 路径不一致

```
文档说: /api/v1/users/*
实际用: /api/v1/auth/users/*
```

### 解决方案

**选项1: 修改实现匹配文档** (推荐)

```java
// 将 UserController 的路径改为 /api/v1/users
@RequestMapping("/api/v1/users")
```

**选项2: 更新文档** (如果实现有更多依赖)

根据实际情况，如果实现已经大量使用 `/api/v1/auth/users`，就修改文档。

**决议**: 改为 `/api/v1/users` (更RESTful)

**涉及文件**:
- UserController (main controller)
- 所有相关的集成测试
- API 文档更新

**工作量**: 2-3 小时

---

## 📊 工作量评估

| 任务 | 工时 | 优先级 |
|------|------|--------|
| RoleManagementController 实现 | 15-20h | 🔴 P0 |
| UserProfileController 实现 | 10-15h | 🔴 P0 |
| 认证模式改造 | 8-12h | 🟠 P1 |
| API 路径规范化 | 2-3h | 🟠 P1 |
| 单元测试补充 | 8-10h | 🟡 P2 |
| **总计** | **43-60h** | |

---

## ✅ 完成标准

- [ ] RoleManagementController 全部 11 个 API 可用
- [ ] UserProfileController 全部 6 个 API 可用
- [ ] 所有涉及用户身份的端点都使用 @AuthenticationPrincipal
- [ ] 所有角色权限检查都正确实现
- [ ] API 路径统一为 `/api/v1/users`
- [ ] 单元测试覆盖率 > 80%
- [ ] API 文档同步更新
- [ ] 集成测试全部通过

---

## 🚀 实施步骤

### Phase 1: 基础设施 (Day 1)

1. 创建 CurrentUser 用户主体类
2. 修改已有端点使用 @AuthenticationPrincipal
3. 添加权限检查注解

### Phase 2: 角色管理 (Day 2-3)

1. 创建 RoleManagementController
2. 创建所有必要的 DTO
3. 实现 Service 层逻辑
4. 添加权限校验

### Phase 3: 用户个人中心 (Day 4)

1. 创建 UserProfileController
2. 实现修改资料、修改密码等功能
3. 实现第三方账号绑定

### Phase 4: 测试和文档 (Day 5)

1. 补充单元测试
2. 运行集成测试
3. 更新 API 文档
4. 性能测试

---

## 📖 相关文件

| 文件 | 说明 |
|-----|------|
| sism-iam/src/.../RoleService.java | 角色业务逻辑 |
| sism-iam/src/.../UserService.java | 用户业务逻辑 |
| sism-shared-kernel/.../CurrentUser.java | 用户上下文 |

---

## 🔗 前置运行任务

实施本任务前需要完成:
- [ ] 修复任务-04: TaskApplicationService 完整实现 (中等依赖)
- [ ] 快速修复指南: Repository JPA 实现 (高度依赖)

---

**创建时间**: 2026-03-14  
**状态**: 待实施  
**业主**: IAM 团队  
**下一步**: 分解为 JIRA ticket

