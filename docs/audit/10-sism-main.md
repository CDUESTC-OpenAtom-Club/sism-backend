# 审计报告：sism-main 模块（应用入口与安全配置）

**审计日期:** 2026-04-12
**审计范围:** 12个Java源文件，涵盖安全配置、WebSocket、附件管理、启动引导。

---

## 一、Critical 严重 (共4个)

### C-01. WebSocket 连接无认证，任意用户可冒充他人接收通知
**文件:** `config/SismWebSocketHandler.java:133-142`
**描述:** `extractUserId` 从URL查询参数获取 `userId`，无身份验证。任何人可通过 `/ws/notifications?userId=123` 冒充用户123接收审批通知等敏感信息。
**修复建议:** 增加JWT认证验证，连接URL改为 `/ws/notifications?token=<jwt>`。
**状态:** 已修复（2026-04-12）。`SismWebSocketHandler` 已改为只接受 `token` 查询参数，并通过 `JwtTokenService` 验证后提取用户ID。

### C-02. 测试通知接口无访问控制，生产环境暴露
**文件:** `interfaces/rest/WebSocketNotificationController.java:30-57`
**描述:** `POST /api/v1/ws/notifications/test/{userId}` 无 `@PreAuthorize`，SecurityConfig 中 `/api/v1/ws/**` 为 `permitAll()`。任何人可向任意用户发送伪造通知。
**修复建议:** 添加 `@Profile("dev")` + `@PreAuthorize("hasRole('ADMIN')")`。
**状态:** 已修复（2026-04-12）。测试通知控制器已限制为 `dev` profile 且要求管理员权限，`/api/v1/ws/**` 也不再全局 `permitAll`。

### C-03. JWT 认证过滤器静默吞没所有异常
**文件:** `config/SecurityConfig.java:87-89`
**描述:** `catch (Exception ignored)` 完全忽略JWT验证异常（签名无效、篡改等）。安全事件不被记录，无法安全审计。
**修复建议:** 至少记录 `log.warn("JWT validation failed: {}", e.getMessage())`。
**状态:** 已修复（2026-04-12）。JWT 过滤器现在会记录校验失败日志，并继续无认证放行后续过滤链。

### C-04. 附件下载路径遍历风险
**文件:** `application/AttachmentApplicationService.java:142-163`
**描述:** `resolveFilePath` 从数据库读取 `objectKey` 后 `resolve`，只调 `.normalize()` 但不检查路径是否仍在允许目录内。若 `object_key` 被篡改为 `../../etc/passwd` 可读任意文件。
**修复建议:** 添加 `if (!candidate.startsWith(uploadRoot)) throw new SecurityException(...)` 。
**状态:** 已修复（2026-04-12）。附件路径解析现在会校验规范化后的候选路径必须仍位于允许根目录内。

---

## 二、High 高 (共4个)

### H-01. WebSocket CORS 允许所有来源
**文件:** `config/WebSocketConfig.java:26-27`
**描述:** `setAllowedOriginPatterns("*")` 允许任意域名WebSocket连接，结合C-01可从恶意网站冒充用户。
**修复建议:** 限制为已知前端域名。
**状态:** 已修复（2026-04-12）。WebSocket 允许来源改为可配置白名单，默认仅本地开发前端地址。

### H-02. JWT 不查数据库，权限变更不即时生效
**文件:** `config/SecurityConfig.java:94-121`
**描述:** 直接从JWT payload解码用户信息。管理员修改用户角色后，旧Token在过期前仍有效。
**修复建议:** 实现Token黑名单/版本机制，或缩短JWT有效期。
**状态:** 已修复（2026-04-12）。JWT 过滤器认证成功后会回源 `UserDetailsServiceImpl` 加载当前用户与角色，不再直接信任 token 内角色快照。

### H-03. 文件上传两次数据库操作
**文件:** `application/AttachmentApplicationService.java:57-83`
**描述:** 先INSERT带临时URL `__TEMP__`，再UPDATE。INSERT成功但UPDATE失败时残留临时URL记录。
**修复建议:** 先获取ID再一次性INSERT。
**状态:** 已修复（2026-04-12）。上传落库已改为单次 INSERT，不再写入临时 URL 再 UPDATE。

### H-04. 上传目录静态资源公开暴露
**文件:** `main/config/UploadResourceConfig.java:15-18`
**描述:** `/uploads/**` 和 `/api/v1/uploads/**` 直接映射为静态资源，不经过安全过滤器。知道文件名即可直接访问附件，绕过下载权限检查。
**修复建议:** 移除静态资源映射，统一通过 `AttachmentController.download()` 接口。
**状态:** 已修复（2026-04-12）。静态资源映射配置已删除，附件只能通过受鉴权保护的下载接口访问。

---

## 三、Medium 中等 (共7个)

| # | 文件:行号 | 问题 | 类别 |
|---|---|---|---|
| M-01 | `AttachmentController.java:111-126` | 已修复（2026-04-13）：改为直接处理 `CurrentUser`，去掉反射提取身份 | 性能 |
| M-02 | `SecurityConfig.java:45` | 已修复（2026-04-13）：`SismWebSocketHandler` 已注入 Spring 管理的 `ObjectMapper` | 代码质量 |
| M-03 | `SismMainApplication.java:29-45` | 已修复（2026-04-13）：移除重复 `@ComponentScan`、多余 `@Import` 和冗余 `@EntityScan` | 代码质量 |
| M-04 | `SismWebSocketHandler.java:30-31` | 已修复（2026-04-13）：增加定时和按需会话清理，发送失败/关闭后即时剔除 session | 内存 |
| M-05 | `AttachmentApplicationService.java:35-41` | 已修复（2026-04-13）：增加 20MB 大小限制和文件类型白名单 | 安全 |
| M-06 | `PlanIntegrityBootstrap.java:22-25` | 已修复（2026-04-13）：默认记录失败并继续启动，可配置开启 fail-fast | 可用性 |
| M-07 | `SecurityConfig.java:192` | 已修复（2026-04-13）：`/api/v1/ws/**` 已不再全局 `permitAll` | 安全 |

---

## 四、Low 低 (共3个)

| # | 文件:行号 | 问题 |
|---|---|---|
| L-01 | `WebSocketNotificationService.java` | 已修复（2026-04-13）：统一为 `@RequiredArgsConstructor` 注入风格 |
| L-02 | `SystemHealthController.java:24-29` | 已修复（2026-04-13）：兼容健康检查接口已基于 `ApplicationAvailability` 返回状态 |
| L-03 | `SismWebSocketHandler.java:75-77` | 已修复（2026-04-13）：未使用的 `broadcast` 方法已删除 |

---

## 汇总统计

| 严重性 | 数量 |
|--------|------|
| **Critical** | 4 |
| **High** | 4 |
| **Medium** | 7 |
| **Low** | 3 |
| **总计** | **18** |

**修复结论:**
1. `Critical/High` 已全部完成修复并回归验证。
2. `Medium/Low` 已全部完成修复或文档与现状对齐。
3. 当前 `10-sism-main.md` 已收完。
