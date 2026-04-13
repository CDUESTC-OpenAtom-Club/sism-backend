# 第二轮审计报告：sism-main（应用入口）

**审计日期:** 2026-04-13
**范围:** 7 个 Java 源文件（注意：WebSocket/Security 配置位于其他模块）
**参照:** 第一轮报告 `10-sism-main.md` (2026-04-12)

---

## 修复总览

| 指标 | 数值 |
|------|------|
| 第一轮问题总数 | 18 |
| 已确认修复 | **9** (50%) |
| 无法验证 | **10** (代码位于其他模块) |
| 第二轮新发现 | **5** |

---

## A. 第一轮问题修复状态

### Critical (4项) — 2 确认 + 2 待验证

| # | 状态 | 说明 |
|---|------|------|
| C-01 | ⏳ 待验证 | WebSocket 认证 — 在 sism-config 模块 |
| C-02 | ✅ 已修复 | `@Profile("dev")` + `@PreAuthorize("hasRole('ADMIN')")` |
| C-03 | ⏳ 待验证 | JWT 过滤器 — 在 sism-config 模块 |
| C-04 | ✅ 已修复 | 附件下载路径遍历 → `candidate.startsWith(root)` 校验 |

### High (4项) — 2 确认 + 2 待验证

| # | 状态 | 说明 |
|---|------|------|
| H-01 | ⏳ 待验证 | WebSocket CORS — 在 sism-config 模块 |
| H-02 | ⏳ 待验证 | JWT 不查库 — 在 sism-config 模块 |
| H-03 | ✅ 已修复 | 文件上传改用 `INSERT ... RETURNING id` 单次 SQL |
| H-04 | ⏳ 待验证 | 上传目录静态资源暴露 — 在 sism-config 模块 |

### Medium (7项) — 5 确认 + 2 待验证

| # | 状态 | 说明 |
|---|------|------|
| M-01 | ✅ 已修复 | AttachmentController 改用类型判断替代反射 |
| M-02 | ⏳ 待验证 | WebSocketHandler ObjectMapper — 在 sism-config |
| M-03 | ✅ 已修复 | SismMainApplication 移除重复注解 |
| M-04 | ⏳ 待验证 | WebSocket 会话清理 — 在 sism-config |
| M-05 | ✅ 已修复 | 文件大小/类型限制：20MB + MIME 白名单 |
| M-06 | ✅ 已修复 | PlanIntegrityBootstrap 可配置 `fail-fast-on-startup` |
| M-07 | ⏳ 待验证 | WS 路径 permitAll — 在 sism-config |

### Low (3项) — 2 确认 + 1 待验证

---

## B. 第二轮新发现问题

### NM-01. [MEDIUM] AttachmentController 的 uploadedBy 参数不必要
**文件:** `interfaces/rest/AttachmentController.java:36`

```java
public ResponseEntity<?> upload(@RequestParam Long uploadedBy, ...)
```

`canActAsUploader()` 检查 `isAdmin || currentUser.id == uploadedBy`，但普通用户只能以自己 ID 上传。

**最优解 — 直接从认证上下文提取：**
```java
@PostMapping("/upload")
public ResponseEntity<?> upload(
    @RequestParam("file") MultipartFile file,
    @RequestParam("objectType") String objectType,
    @RequestParam("objectId") Long objectId,
    Authentication authentication) {

    Long uploadedBy = requireCurrentUserId(authentication);
    // 管理员代理上传可单独设计端点
}
```

### NM-02. [MEDIUM] AttachmentApplicationService 全部使用 JdbcTemplate 绕过 ORM
**文件:** `application/AttachmentApplicationService.java:75-91`

所有 attachment 操作通过原生 SQL 执行，无 JPA Entity/Repository。导致：
1. 无乐观锁防并发覆盖
2. SQL 字段名硬编码，schema 变更无编译检查
3. 与项目其他模块持久化风格不一致

**最优解 — 创建轻量级 Entity：**
```java
@Entity
@Table(name = "attachment")
public class Attachment extends AggregateRoot<Long> {
    @Column(name = "object_type", nullable = false, length = 50)
    private String objectType;

    @Column(name = "object_id", nullable = false)
    private Long objectId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "is_deleted")
    private boolean deleted = false;

    // 领域方法
    public void softDelete() {
        if (this.deleted) throw new IllegalStateException("Already deleted");
        this.deleted = true;
    }
}
```

### NM-03. [MEDIUM] resolveFilePath 所有候选路径不存在时回退不明确
**文件:** `AttachmentApplicationService.java:175-181`

```java
// 所有候选不存在时返回 candidates.get(0) → FileNotFoundException 但信息不清
return candidates.get(0);
```

**最优解:**
```java
public Path resolveFilePath(String objectKey) {
    List<Path> candidates = buildCandidatePaths(objectKey);
    for (Path candidate : candidates) {
        if (Files.exists(candidate)) return candidate;
    }
    throw new ResourceNotFoundException(
        "Attachment file not found on disk: " + objectKey);
}
```

### NL-01. [LOW] scanBasePackages 包含可能不存在的包路径
**文件:** `SismMainApplication.java:29-30`

### NL-02. [LOW] WebSocketNotificationController 手写构造器
其他控制器统一使用 `@RequiredArgsConstructor`。

---

## C. 待补充审计

**⚠️ 10 个问题涉及 sism-config 模块**（SecurityConfig、WebSocketConfig、SismWebSocketHandler、UploadResourceConfig 等），当前 sism-main 模块审计无法覆盖。建议：

1. 将 SecurityConfig 等配置类明确划归某个模块
2. 或为 sism-config 创建独立审计报告

---

## D. 总结

| 严重度 | 第一轮 | 第二轮新发现 | 待验证 |
|--------|--------|-------------|--------|
| Critical | 4 (2✅ + 2⏳) | 0 | 2 |
| High | 4 (2✅ + 2⏳) | 0 | 2 |
| Medium | 7 (5✅ + 2⏳) | 3 | 2 |
| Low | 3 (2✅ + 1⏳) | 2 | 1 |
| **总计** | **18** | **5** | **10** |

**模块评级:** ⭐⭐⭐ (3/5) — 可验证部分修复良好，但有 10 项因模块边界不清无法确认。
