# SISM IAM 个人中心模块设计方案

更新时间：2026-03-19

## 1. 设计目标

为 `sism-iam` 补齐“个人中心”能力，使其从当前的控制器占位实现，升级为可落地、可扩展、可审计的正式模块。

本模块一期目标包括：
- 查询当前用户个人资料
- 更新当前用户基础资料
- 上传并更新头像
- 修改当前用户密码
- 记录最后登录时间
- 管理第三方绑定账号

本模块不在一期内解决的内容：
- 完整 OAuth2 / OpenID Connect 登录
- 第三方回调页面与前端授权流程
- 文件对象存储平台的最终选型

## 2. 当前现状

当前实现入口：
[`UserProfileController.java`](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/src/main/java/com/sism/iam/interfaces/rest/UserProfileController.java)

当前问题：
- `User` 聚合只有 `username / password / realName / ssoId / orgId / isActive`，无法承接头像、最后登录时间等资料字段
- `updateProfile` 只更新 `realName`
- 头像更新没有独立接口
- 第三方账号绑定、解绑、查询接口都是空实现
- `lastLoginTime` 固定返回 `null`
- 现有 `CurrentUser` 里保留了历史 `email` 字段，但当前 `User` 实体并不承接该字段，存在模型漂移

结论：
- 个人中心不应继续堆在 `UserProfileController` 内部 DTO 和注释里
- 需要补一个独立的“资料子模型 + 绑定账号子模型”

## 3. 模块边界

建议仍归属 `sism-iam` 模块，不拆新 Maven module。

推荐的包结构：

```text
sism-iam
├── application
│   ├── dto
│   │   └── profile/
│   └── service
│       ├── UserProfileService.java
│       ├── AvatarStorageService.java
│       └── LinkedAccountService.java
├── domain
│   ├── UserProfile.java
│   ├── LinkedAccount.java
│   └── repository
│       ├── UserProfileRepository.java
│       └── LinkedAccountRepository.java
├── infrastructure
│   └── persistence
│       ├── JpaUserProfileRepository.java
│       ├── JpaUserProfileRepositoryInternal.java
│       ├── JpaLinkedAccountRepository.java
│       └── JpaLinkedAccountRepositoryInternal.java
└── interfaces
    └── rest
        ├── UserProfileController.java
        └── UserAvatarController.java
```

## 4. 领域建模

### 4.1 User 与 UserProfile 的关系

建议保持：
- `User` 负责认证身份与账户主状态
- `UserProfile` 负责个人资料扩展信息

这样可以避免把个人资料字段持续堆进 `sys_user` 主表。

推荐职责划分：

`User`
- username
- passwordHash
- realName
- ssoId
- orgId
- isActive
- createdAt / updatedAt
- lastLoginAt

`UserProfile`
- userId
- avatarUrl
- phone
- email
- bio
- createdAt / updatedAt

### 4.2 LinkedAccount 模型

新增 `LinkedAccount` 聚合，用于记录第三方绑定关系。

推荐字段：
- id
- userId
- platform
- externalAccountId
- externalUnionId
- platformNickname
- platformAvatarUrl
- bindStatus
- boundAt
- unboundAt
- createdAt
- updatedAt
- isDeleted

推荐平台枚举值：
- `WECHAT`
- `DINGTALK`
- `QQ`
- `FEISHU`
- `CUSTOM_SSO`

推荐绑定状态：
- `BOUND`
- `UNBOUND`

## 5. 数据库设计

### 5.1 sys_user 扩展

在 `sys_user` 上新增：
- `last_login_at TIMESTAMP`

说明：
- `last_login_at` 属于认证账户行为，放在 `sys_user` 比放在 `user_profile` 更合理

### 5.2 user_profile 表

可复用 [`flyway-migration-guide.md`](/Users/blackevil/Documents/前端架构测试/sism-backend/docs/flyway-migration-guide.md#L191) 里已有思路，但建议补齐字段。

建议结构：

```sql
CREATE TABLE user_profile (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES sys_user(id),
    email VARCHAR(255),
    phone VARCHAR(32),
    avatar_url VARCHAR(500),
    bio TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_profile_user_id UNIQUE (user_id),
    CONSTRAINT uk_user_profile_email UNIQUE (email)
);
```

约束建议：
- `user_id` 唯一，保证每个用户只有一份资料
- `email` 可空但唯一
- `phone` 一期先不做唯一强约束，避免历史脏数据阻塞迁移

### 5.3 linked_account 表

建议新增：

```sql
CREATE TABLE linked_account (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES sys_user(id),
    platform VARCHAR(50) NOT NULL,
    external_account_id VARCHAR(255) NOT NULL,
    external_union_id VARCHAR(255),
    platform_nickname VARCHAR(255),
    platform_avatar_url VARCHAR(500),
    bind_status VARCHAR(32) NOT NULL DEFAULT 'BOUND',
    bound_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unbound_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_linked_account_platform_ext UNIQUE (platform, external_account_id)
);
```

索引建议：
- `(user_id, is_deleted)`
- `(platform, external_account_id)`

## 6. API 设计

### 6.1 获取个人资料

保留：
- `GET /api/v1/profile`

返回结构建议：

```json
{
  "id": 1,
  "username": "admin",
  "realName": "系统管理员",
  "orgId": 1001,
  "isActive": true,
  "email": "admin@example.com",
  "phone": "13800000000",
  "avatar": "https://cdn.example.com/avatar/1.png",
  "roles": ["系统管理员"],
  "createdAt": "2026-03-01T10:00:00",
  "lastLoginTime": "2026-03-19T09:30:00"
}
```

### 6.2 更新个人资料

保留：
- `PUT /api/v1/profile`

请求体建议只保留真正允许用户修改的字段：

```json
{
  "realName": "张三",
  "email": "zhangsan@example.com",
  "phone": "13800000000",
  "bio": "负责学院年度指标推进"
}
```

说明：
- `avatar` 不再放进该接口
- 头像走独立上传接口，避免把 Base64、文件流和资料更新混在一起

### 6.3 上传头像

新增：
- `POST /api/v1/profile/avatar`

请求：
- `multipart/form-data`
- 字段名：`file`

返回：

```json
{
  "avatar": "https://cdn.example.com/avatar/1_20260319.png",
  "url": "https://cdn.example.com/avatar/1_20260319.png"
}
```

说明：
- 控制器只负责接收文件
- 存储策略封装在 `AvatarStorageService`
- 一期可先落本地磁盘或 Nginx 静态目录，后续再切 OSS / MinIO

### 6.4 修改密码

保留：
- `POST /api/v1/profile/password`

规则：
- 校验旧密码
- 校验新密码一致性
- 校验新密码复杂度
- 修改成功后刷新 `updated_at`

增强建议：
- 修改成功后主动使当前 refresh token 失效
- 记录密码修改审计日志

### 6.5 第三方账号绑定

建议调整为更 RESTful 的路径：
- `POST /api/v1/profile/linked-accounts`
- `DELETE /api/v1/profile/linked-accounts/{id}`
- `GET /api/v1/profile/linked-accounts`

绑定请求建议：

```json
{
  "platform": "WECHAT",
  "externalAccountId": "wx_open_id_xxx",
  "externalUnionId": "wx_union_id_xxx",
  "platformNickname": "张三微信"
}
```

绑定规则：
- 同一 `(platform, externalAccountId)` 只能绑定一个用户
- 同一用户可绑定多个平台账号
- 可配置是否允许同平台多账号

解绑规则：
- 如果系统启用了“第三方账号登录且无密码登录”能力，需要检查不能解绑最后一个可登录凭证
- 当前仓库仍以用户名密码为主，因此一期可先只做基础解绑，不做“最后一个凭证”强约束

## 7. 应用服务设计

### 7.1 UserProfileService

建议职责：
- `getCurrentProfile(userId)`
- `updateCurrentProfile(userId, command)`
- `updateAvatar(userId, avatarUrl)`
- `changePassword(userId, oldPassword, newPassword, confirmPassword)`
- `recordLastLogin(userId, loginTime)`

### 7.2 LinkedAccountService

建议职责：
- `bindAccount(userId, command)`
- `unbindAccount(userId, linkedAccountId)`
- `listAccounts(userId)`
- `existsByPlatformAndExternalAccountId(platform, externalAccountId)`

### 7.3 AvatarStorageService

建议抽象成接口，避免直接把磁盘路径写死在控制器里。

```java
public interface AvatarStorageService {
    String store(Long userId, MultipartFile file);
    void delete(String avatarUrl);
}
```

一期实现：
- `LocalAvatarStorageService`

二期实现：
- `OssAvatarStorageService` 或 `MinioAvatarStorageService`

## 8. 认证链路改造

当前 `AuthService.login(...)` 成功后只签发 token，没有更新登录信息。

文件：
[`AuthService.java`](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/src/main/java/com/sism/iam/application/service/AuthService.java)

建议在登录成功后增加：
- 更新 `sys_user.last_login_at`
- 可选记录 `last_login_ip`
- 可选记录 `last_login_user_agent`

一期最小改造：
- 只更新 `last_login_at`

## 9. 兼容与迁移策略

### 9.1 向后兼容

为了不阻塞现有前端：
- 保留 `GET /api/v1/profile`
- 保留 `PUT /api/v1/profile`
- 保留 `POST /api/v1/profile/password`

对第三方账号接口：
- 可以继续兼容旧路径
  - `POST /api/v1/profile/bind-account`
  - `DELETE /api/v1/profile/accounts/{accountId}`
  - `GET /api/v1/profile/accounts`
- 但内部实现转到新 service

### 9.2 数据迁移

推荐 Flyway 顺序：

1. `Vxxx__add_last_login_at_to_sys_user.sql`
2. `Vxxx__create_user_profile_table.sql`
3. `Vxxx__create_linked_account_table.sql`
4. `Vxxx__init_user_profile_from_sys_user.sql`（如有历史资料要迁移）

### 9.3 回填策略

对已有用户：
- 自动创建空 `user_profile`
- `last_login_at` 初始为 `NULL`
- 历史没有头像和第三方账号则保持空值

## 10. 测试设计

### 单元测试

- `UserProfileServiceTest`
  - 查询个人资料
  - 更新资料
  - 修改密码成功
  - 旧密码错误
  - 新密码确认不一致

- `LinkedAccountServiceTest`
  - 绑定成功
  - 重复绑定失败
  - 查询绑定列表
  - 解绑成功
  - 越权解绑失败

### 集成测试

- `UserProfileControllerTest`
  - 获取当前资料
  - 更新资料
  - 修改密码
  - 获取绑定账号列表

- `AuthServiceIntegrationTest`
  - 登录成功后 `last_login_at` 被刷新

## 11. 分阶段实施建议

### Phase 1：最小闭环

- 新增 `last_login_at`
- 新增 `user_profile`
- `GET /profile`
- `PUT /profile`
- `POST /profile/password`
- 登录记录 `last_login_at`

### Phase 2：头像能力

- 新增头像上传接口
- 增加存储服务抽象
- 本地存储落地

### Phase 3：第三方绑定

- 新增 `linked_account`
- 实现绑定、解绑、查询
- 补充唯一性和越权校验

## 12. 推荐结论

对于“第一个模块”，建议你把范围定义为：

**用户个人中心模块一期 = 资料查询 + 资料更新 + 密码修改 + 最后登录时间记录**

原因：
- 这部分直接闭合当前 `UserProfileController` 的核心缺口
- 对数据库和接口影响最小
- 能最快把“返回空值 / 占位逻辑”中的一大块收掉

而头像上传与第三方绑定建议放到二期：
- 头像涉及文件存储方案
- 第三方绑定涉及新表、新约束、新权限和未来 OAuth 对接

## 13. 实施入口建议

如果按“一期先落地”来做，建议开发顺序如下：

1. 给 `sys_user` 增加 `last_login_at`
2. 创建 `user_profile`
3. 新增 `UserProfile` 实体与仓储
4. 抽出 `UserProfileService`
5. 重构 `UserProfileController`
6. 在 `AuthService.login(...)` 中记录最后登录时间
7. 补单测与集成测试
