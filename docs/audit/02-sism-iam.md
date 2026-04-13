# 审计报告：sism-iam 模块（身份认证与访问管理）

**审计日期:** 2026-04-12
**审计范围:** 41个Java源文件，涵盖认证、授权、用户管理、角色权限、通知等。

---

## 一、Critical 严重 (共6个)

### C-01. JWT 硬编码默认密钥
**文件:** `application/JwtTokenService.java:23`
**状态:** 已修复（2026-04-12）
**描述:** JWT密钥以硬编码默认值写在代码中：`@Value("${jwt.secret:SismSecretKeyForJWTTokenGeneration2024VeryLongSecretKey}")`。若部署时未配置 `jwt.secret`，任何人看到源码即可伪造任意用户Token获取管理员权限。
**修复建议:** 移除默认值，`@PostConstruct` 校验密钥长度>=256位。

### C-02. Token 黑名单内存存储，集群失效 + 内存泄漏
**文件:** `application/JwtTokenService.java:32`
**状态:** 部分缓解（2026-04-12）
**描述:** `ConcurrentHashMap.newKeySet()` 存储黑名单。多实例部署时各实例黑名单不共享（已登出Token在其他实例仍有效）；过期Token永不清理，长期运行OOM。
**修复建议:** 使用Redis存储黑名单，设置与Token过期时间相同的TTL。

### C-03. 登出端点为空操作，Token未失效
**文件:** `interfaces/rest/AuthController.java:102-106`
**状态:** 已修复（2026-04-12）
**描述:** `/logout` 直接返回200，未将Token加入黑名单。`JwtTokenService.blacklistToken()` 存在但从未被调用。用户"登出"后Token仍有效。
**修复建议:** 从请求提取Token并调用 `jwtTokenService.blacklistToken(token)`。

### C-04. 注册接口无访问控制
**文件:** `interfaces/rest/AuthController.java:50-59`
**状态:** 已修复（2026-04-12）
**描述:** `/register` 完全开放，攻击者可批量注册用户、创建管理员角色用户。且返回完整User实体。
**修复建议:** 添加 `@PreAuthorize("hasRole('ADMIN')")` 或实现邀请码机制。

### C-05. 用户管理接口全部无权限控制
**文件:** `interfaces/rest/AuthController.java:123-243`
**状态:** 已修复（2026-04-12）
**描述:** 所有用户管理接口（查询所有用户、按ID查询、创建、更新、删除、锁定、解锁）均无 `@PreAuthorize`。任何已认证用户都可执行所有用户管理操作。
**修复建议:** 所有管理端点添加 `@PreAuthorize("hasRole('ADMIN')")`。

### C-06. 告警事件接口无任何认证和授权
**文件:** `interfaces/rest/NotificationController.java:78-206`
**状态:** 已修复（2026-04-12）
**描述:** 告警事件CRUD端点无 `@AuthenticationPrincipal` 校验或 `@PreAuthorize`。任何未认证用户都可创建/修改/删除告警事件。
**修复建议:** 所有敏感操作添加认证校验和权限注解。

---

## 二、High 高 (共7个)

### H-01. UserService.createUser() 密码明文存储
**文件:** `application/service/UserService.java:46-48`
**状态:** 已修复（2026-04-12）
**描述:** `user.setPassword(password)` 直接设置明文密码，未调用 `PasswordEncoder.encode()`。通过 `POST /api/v1/auth/users` 创建的用户密码明文存储在数据库。
**修复建议:** 注入 `PasswordEncoder` 并加密密码。

### H-02. 删除角色时查询逻辑错误
**文件:** `interfaces/rest/RoleManagementController.java:149-150`
**描述:** `roleRepository.findByUserId(id)` 传入的是角色ID，但方法期望用户ID。导致删除角色时无法正确检测是否有用户关联。
**修复建议:** 使用 `userRepository.findByRoleId(id)`。

### H-03. 角色列表 N+1 查询
**文件:** `interfaces/rest/RoleManagementController.java:235-247`
**描述:** 对每个角色执行 `role.getPermissions().size()`（懒加载）+ `userRepository.findByRoleId(role.getId()).size()`，10个角色产生20+次额外SQL。
**修复建议:** 使用批量查询或DTO投影。

### H-04. 用户列表全量加载 + N+1 查询
**文件:** `interfaces/rest/AuthController.java:129-131,288-297`
**描述:** `findAll()` 加载全部用户到内存，再对每个用户访问 `user.getRoles()` 触发N+1查询。且先全量加载再内存分页，用户量大时性能灾难。
**修复建议:** 使用数据库分页 + `@EntityGraph`/`JOIN FETCH` 加载角色。

### H-05. 刷新Token丢失角色信息
**文件:** `application/JwtTokenService.java:101-124`
**状态:** 已修复（2026-04-12）
**描述:** `refreshToken()` 构造空User对象（roles为空），新Token中roles字段为空，用户所有权限丢失。
**修复建议:** 从旧Token的claims中直接提取roles，或从数据库重新加载。

### H-06. 登录防暴力破解机制完全未生效
**文件:** `application/service/InMemoryLoginAttemptService.java:14`
**状态:** 已修复（2026-04-12）
**描述:** `AuthService.login()` 从未调用 `LoginAttemptService` 的任何方法，防暴力破解形同虚设。且内存实现无过期清理、多实例不共享。
**修复建议:** 在 `AuthService.login()` 中集成调用；添加定时清理；考虑Redis实现。

### H-07. UserProfileController 绕过应用层直连仓储
**文件:** `interfaces/rest/UserProfileController.java:35-36`
**描述:** Controller 直接注入 `UserRepository` 和 `PasswordEncoder`，绕过 `UserProfileService`。违反DDD分层，且缺少 `PasswordPolicy.validateLength()` 校验。
**修复建议:** 移除Controller中的Repository，改用 `UserProfileService`。

---

## 三、Medium 中等 (共10个)

| # | 文件:行号 | 问题 | 类别 |
|---|---|---|---|
| M-01 | `NotificationService.java:163-168` | `markAllNotificationsAsRead` 返回硬编码 `readCount=5`，未真正更新数据库 | Bug |
| M-02 | `LoginResponse.java:17-22` | `token`/`accessToken` 和 `type`/`tokenType` 冗余字段 | 代码质量 |
| M-03 | `Notification.java` | 实体命名"Notification"但映射 `alert_event` 表，应属告警模块 | 架构 |
| M-04 | `Permission.java` | 未继承 `AggregateRoot`，与其他实体不一致 | 架构 |
| M-05 | `NotificationService.java:133-147` | `getMyNotifications` 忽略userId参数，返回所有用户的告警 | 安全 |
| M-06 | `RoleService.java:23-39` | `createRole()` 返回未持久化对象，Controller和Service各自save | Bug |
| M-07 | `InMemoryLoginAttemptService.java:23-38` | `assertNotBlocked` 先get再remove非原子操作，TOCTOU竞态 | 并发 |
| M-08 | `UserNotificationService.java:93-105` | JSON手动拼接，`escapeJson()` 未处理换行/制表符 | 安全 |
| M-09 | `RoleManagementController.java` | 与 `RoleManagementService` 大量重复逻辑（校验、创建、删除） | DRY |
| M-10 | `User.java:56-60` | createdAt/updatedAt 与 AggregateRoot 基类字段冲突 | Bug |

---

## 四、Low 低 (共9个)

| # | 文件:行号 | 问题 |
|---|---|---|
| L-01 | `LoginRequest.java` | 缺少 `@NotBlank` 参数校验注解 |
| L-02 | `UserDetailsServiceImpl.java:41-48` | 空角色码赋予 `ROLE_UNKNOWN` 权限，应过滤 |
| L-03 | `AuthController.java:58` | 注册返回完整User实体，应返回DTO |
| L-04 | `AuthController.java:128` | 分页默认100条无上限，且内存分页 |
| L-05 | `CurrentUser.java:53-55` | `getPassword()` 返回null，违反UserDetails契约 |
| L-06 | `NotificationController.java:152-170` | POST创建资源使用@RequestParam而非@RequestBody |
| L-07 | `RoleManagementController.java:175` | `permissionIds` 可能为null导致NPE |
| L-08 | `NotificationStatus.java` | 枚举已定义但从未使用（死代码） |
| L-09 | `UserProfileController.java:175-186` | UpdateProfileRequest含email/phone但User实体无这些字段 |

---

## 汇总统计

| 严重性 | 数量 | 关键主题 |
|--------|------|----------|
| **Critical** | 6 | JWT硬编码密钥、Token黑名单失效、登出空操作、注册/管理接口无权限控制 |
| **High** | 7 | 密码明文存储、查询逻辑错误、N+1查询、刷新Token丢权限、防暴力破解失效 |
| **Medium** | 10 | 硬编码假数据、命名不符、竞态条件、JSON注入、重复逻辑 |
| **Low** | 9 | 校验缺失、死代码、风格不一致 |
| **总计** | **32** | |

**优先修复建议：**
1. **C-01~C-06** 认证安全体系基础性缺陷 — 立即修复
2. **H-01** 密码明文存储 — 立即修复
3. **H-05** 刷新Token丢权限 — 影响所有用户
4. **H-06** 防暴力破解未集成 — 安全功能形同虚设
