# sism-analytics 模块审计报告

**审计日期:** 2026-04-06
**审计人员:** Claude Code (Automated Audit)
**模块版本:** 当前主分支

---

## 模块概览

| 项目 | 数据 |
|------|------|
| 模块名称 | sism-analytics |
| 模块职责 | 仪表盘管理、数据导出、报表管理 |
| Java 文件总数 | 37 (主目录) + 28 (重复目录) |
| 核心实体 | Dashboard, DataExport, Report |
| Repository 数量 | 3 |
| Service 数量 | 5 |
| Controller 数量 | 4 |

### 包结构

```
com.sism.analytics/
├── application/
│   ├── DashboardApplicationService.java
│   ├── DashboardSummaryService.java
│   ├── DataExportApplicationService.java
│   ├── ExportService.java
│   └── ReportApplicationService.java
├── domain/
│   ├── Dashboard.java
│   ├── DataExport.java
│   ├── ExportFormat.java
│   ├── Report.java
│   └── ReportType.java
├── infrastructure/
│   ├── AnalyticsModuleConfig.java
│   └── repository/
│       ├── DashboardRepository.java
│       ├── DataExportRepository.java
│       └── ReportRepository.java
└── interfaces/
    ├── dto/
    │   ├── DashboardDTO.java
    │   ├── DashboardSummaryDTO.java
    │   ├── DataExportDTO.java
    │   └── ...
    └── rest/
        ├── DashboardController.java
        ├── DashboardSummaryController.java
        ├── DataExportController.java
        └── ReportController.java

⚠️ 发现重复目录: src/main 2/ 和 src/test 2/
```

---

## 一、安全漏洞

### 🔴 Critical: DashboardController 所有端点无权限控制

**文件:** `DashboardController.java`
**行号:** 全文

```java
@RestController
@RequestMapping("/api/v1/analytics/dashboard")
public class DashboardController {

    @PostMapping
    @Operation(summary = "创建新仪表盘")
    public ResponseEntity<ApiResponse<DashboardDTO>> createDashboard(@RequestBody CreateDashboardRequest request) {
        // ❌ 无 @PreAuthorize
        // ❌ 未验证 request.getUserId() 是否为当前登录用户
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除仪表盘")
    public ResponseEntity<ApiResponse<Void>> deleteDashboard(@PathVariable Long id) {
        // ❌ 任何用户都可以删除任何仪表盘！
    }

    @PostMapping("/{id}/copy")
    @Operation(summary = "复制仪表盘给其他用户")
    public ResponseEntity<ApiResponse<DashboardDTO>> copyDashboard(
            @PathVariable Long id,
            @RequestBody Map<String, Long> request) {
        // ❌ 任何用户可以将仪表盘复制给任何用户！
        Dashboard dashboard = dashboardApplicationService.copyDashboardToUser(id, request.get("targetUserId"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新仪表盘信息")
    public ResponseEntity<ApiResponse<DashboardDTO>> updateDashboard(...) {
        // ❌ 任何用户都可以修改任何仪表盘
    }
}
```

**问题描述:**
1. DashboardController 的所有 API 端点都没有权限控制
2. 任何用户可以创建、修改、删除、复制任何用户的仪表盘
3. 可以查看其他用户的私有仪表盘

**攻击示例:**
```bash
# 删除其他用户的仪表盘
DELETE /api/v1/analytics/dashboard/123

# 将其他用户的仪表盘复制给自己
POST /api/v1/analytics/dashboard/123/copy
{"targetUserId": 999}
```

**风险影响:**
- 仪表盘数据被未授权修改/删除
- 用户隐私泄露
- 数据完整性问题

**严重等级:** 🔴 **Critical**

**建议修复:**
```java
@RestController
@RequestMapping("/api/v1/analytics/dashboard")
public class DashboardController {

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DashboardDTO>> createDashboard(
            @RequestBody CreateDashboardRequest request,
            @AuthenticationPrincipal CurrentUser currentUser) {
        // 强制使用当前用户ID
        Dashboard dashboard = dashboardApplicationService.createDashboard(
                request.getName(),
                request.getDescription(),
                currentUser.getId(),  // 使用当前用户ID
                request.getIsPublic() != null && request.getIsPublic(),
                request.getConfig()
        );
        // ...
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteDashboard(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {
        // 验证仪表盘归属
        Dashboard dashboard = dashboardApplicationService.findById(id);
        if (!dashboard.getUserId().equals(currentUser.getId())) {
            throw new AuthorizationException("无权删除此仪表盘");
        }
        // ...
    }
}
```

---

### 🔴 High: 仪表盘归属验证缺失

**文件:** `DashboardApplicationService.java`

```java
@Transactional
public Dashboard updateDashboard(Long dashboardId, String name, String description, boolean isPublic, String config) {
    Dashboard dashboard = findById(dashboardId);
    // ❌ 未验证 dashboard 是否属于当前用户
    dashboard.update(name, description, isPublic, config);
    // ...
}

@Transactional
public void deleteDashboard(Long dashboardId) {
    Dashboard dashboard = findById(dashboardId);
    // ❌ 未验证 dashboard 是否属于当前用户
    dashboard.delete();
    // ...
}
```

**问题描述:**
服务层方法没有验证仪表盘的所有权，任何用户都可以操作其他用户的仪表盘。

**严重等级:** 🔴 **High**

---

### 🟠 Medium: 权限控制不一致

**文件:** `DashboardSummaryController.java` vs `DashboardController.java`

```java
// DashboardSummaryController - 有权限控制 ✅
@GetMapping
@PreAuthorize("isAuthenticated()")
public ResponseEntity<ApiResponse<DashboardSummaryDTO>> getDashboardSummary() { }

// DashboardController - 无权限控制 ❌
@PostMapping
public ResponseEntity<ApiResponse<DashboardDTO>> createDashboard(...) { }
```

**问题描述:**
同一模块内不同控制器的安全策略不一致。

**严重等级:** 🟠 **Medium**

---

## 二、潜在 Bug 和逻辑错误

### 🔴 High: 重复的源代码目录

**问题描述:**
存在两个源代码目录：
- `src/main/java/` - 主目录
- `src/main 2/java/` - 重复目录
- `src/test/java/` - 测试目录
- `src/test 2/java/` - 重复测试目录

**风险影响:**
- 构建时可能使用错误的文件版本
- 维护困难
- 潜在的版本不一致

**严重等级:** 🔴 **High**

**建议修复:**
删除 `src/main 2/` 和 `src/test 2/` 目录。

---

### 🟠 Medium: copyToUser 未验证目标用户

**文件:** `Dashboard.java`
**行号:** 147-159

```java
public Dashboard copyToUser(Long targetUserId) {
    Objects.requireNonNull(targetUserId, "Target user ID cannot be null");
    // ❌ 未验证 targetUserId 是否是有效用户
    // ❌ 任何用户都可以将仪表盘复制给任何用户

    Dashboard copied = new Dashboard();
    copied.name = this.name + " (副本)";
    copied.userId = targetUserId;
    // ...
}
```

**问题描述:**
复制仪表盘时未验证目标用户是否存在，可能导致孤儿数据。

**严重等级:** 🟠 **Medium**

---

### 🟠 Medium: config 字段长度未限制

**文件:** `Dashboard.java`
**行号:** 41-42

```java
@Column(name = "config", columnDefinition = "TEXT")
private String config;  // ❌ 未限制长度
```

**问题描述:**
`config` 字段使用 `TEXT` 类型但未限制长度，恶意用户可以提交超大配置导致数据库存储问题。

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
public void updateConfig(String config) {
    if (config != null && config.length() > 100000) {  // 限制为 100KB
        throw new IllegalArgumentException("Config size exceeds maximum limit");
    }
    this.config = config;
    this.updatedAt = LocalDateTime.now();
}
```

---

### 🟡 Low: equals/hashCode 不完整

**文件:** `Dashboard.java`
**行号:** 162-176

```java
@Override
public boolean equals(Object o) {
    // ...
    return isPublic == dashboard.isPublic &&
            deleted == dashboard.deleted &&
            Objects.equals(getId(), dashboard.getId()) &&
            Objects.equals(name, dashboard.name) &&
            Objects.equals(userId, dashboard.userId);  // ❌ 未包含 description, config, createdAt
}
```

**问题描述:**
`equals` 和 `hashCode` 方法未包含所有业务字段，可能导致逻辑错误。

**严重等级:** 🟡 **Low**

---

## 三、性能瓶颈

### 🟠 Medium: 列表查询无分页

**文件:** `DashboardController.java`

```java
@GetMapping("/user/{userId}")
public ResponseEntity<ApiResponse<List<DashboardDTO>>> getDashboardsByUserId(@PathVariable Long userId) {
    List<Dashboard> dashboards = dashboardApplicationService.findDashboardsByUserId(userId);  // ❌ 无分页
    return ResponseEntity.ok(ApiResponse.success(dashboards.stream().map(this::toDashboardDTO).collect(Collectors.toList())));
}

@GetMapping("/public")
public ResponseEntity<ApiResponse<List<DashboardDTO>>> getAllPublicDashboards() {
    List<Dashboard> dashboards = dashboardApplicationService.findAllPublicDashboards();  // ❌ 无分页
    // ...
}
```

**问题描述:**
所有列表查询都没有分页，数据量大时会有性能问题。

**严重等级:** 🟠 **Medium**

---

### 🟡 Low: 事件存储和发布冗余

**文件:** `DashboardApplicationService.java`
**行号:** 163-172

```java
private void publishAndSaveEvents(Dashboard dashboard) {
    List<DomainEvent> events = dashboard.getDomainEvents();
    if (events != null && !events.isEmpty()) {
        for (DomainEvent event : events) {
            eventStore.save(event);  // 逐个保存
        }
        eventPublisher.publishAll(events);
        dashboard.clearEvents();
    }
}
```

**问题描述:**
事件逐个保存，可以优化为批量保存。

**严重等级:** 🟡 **Low**

---

## 四、代码质量和可维护性

### 🟠 Medium: 模块名称与功能不符

**问题描述:**
模块名为 `sism-analytics`（分析），但主要功能是仪表盘 CRUD 管理，缺少真正的数据分析功能：
- 没有数据聚合逻辑
- 没有统计计算
- 没有可视化生成

`DashboardSummaryService` 提供了一些汇总数据，但功能有限。

**严重等级:** 🟠 **Medium**

**建议:**
1. 重命名模块为 `sism-dashboard`
2. 或添加真正的分析功能

---

### 🟠 Medium: 缺少数据验证

**文件:** `DashboardApplicationService.java`

```java
@Transactional
public Dashboard createDashboard(String name, String description, Long userId, boolean isPublic, String config) {
    Dashboard dashboard = Dashboard.create(name, description, userId, isPublic, config);
    // ❌ 未验证 userId 是否有效
    // ❌ 未验证 name 是否重复
}
```

**严重等级:** 🟠 **Medium**

---

### 🟡 Low: 硬编码字符串 - "(副本)"

**文件:** `Dashboard.java`
**行号:** 151

```java
copied.name = this.name + " (副本)";  // ❌ 硬编码中文字符串
```

**建议:** 使用国际化或配置。

---

## 五、架构最佳实践

### ✅ 亮点: 软删除实现

Dashboard 实体实现了软删除模式：

```java
public void delete() {
    this.deleted = true;
    this.updatedAt = LocalDateTime.now();
}
```

查询方法也正确过滤了已删除记录：
```java
Optional<Dashboard> findByIdAndNotDeleted(Long id);
List<Dashboard> findByUserIdAndNotDeleted(Long userId);
```

---

### ✅ 亮点: 领域事件

服务层正确使用了领域事件：

```java
private void publishAndSaveEvents(Dashboard dashboard) {
    List<DomainEvent> events = dashboard.getDomainEvents();
    // ...
    eventStore.save(event);
    eventPublisher.publishAll(events);
    dashboard.clearEvents();
}
```

---

### 🟡 Low: 缺少 Dashboard 领域事件

虽然框架支持事件发布，但 Dashboard 实体没有定义任何领域事件：

```java
public static Dashboard create(...) {
    // ...
    // ❌ 没有添加 DashboardCreatedEvent
}

public void delete() {
    this.deleted = true;
    // ❌ 没有添加 DashboardDeletedEvent
}
```

---

## 六、审计总结

### 问题统计

| 严重等级 | 数量 | 类别分布 |
|----------|------|----------|
| 🔴 Critical | 1 | 安全（Dashboard 无权限控制） |
| 🔴 High | 2 | 安全、Bug（重复目录、归属验证） |
| 🟠 Medium | 6 | 安全、Bug、性能、代码质量 |
| 🟡 Low | 4 | 代码质量、性能 |

### 最紧急修复项

| 优先级 | 问题 | 影响 |
|--------|------|------|
| P0 | Dashboard 无权限控制 | 任何用户可操作任何仪表盘 |
| P1 | 删除重复目录 | 构建混乱 |
| P1 | 归属验证缺失 | 越权操作 |
| P2 | 列表无分页 | 性能问题 |

### 整体评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 安全性 | 🔴 需改进 | 核心控制器无权限控制 |
| 可靠性 | ✅ 良好 | 软删除、领域事件设计合理 |
| 性能 | 🟠 需改进 | 缺少分页 |
| 可维护性 | 🟠 需改进 | 存在重复目录、命名不符 |
| 架构合规性 | ✅ 良好 | DDD 分层清晰 |

### 亮点

1. **软删除实现**: 正确实现了软删除模式
2. **领域事件框架**: 事件发布机制完善
3. **DashboardSummaryController 安全**: 有权限注解
4. **聚合根设计**: Dashboard 继承 AggregateRoot

### 关键建议

1. **立即添加权限控制**: 为 DashboardController 所有端点添加 `@PreAuthorize`
2. **验证归属权**: 服务层验证用户是否拥有仪表盘
3. **删除重复目录**: 移除 `src/main 2/` 和 `src/test 2/`
4. **添加分页支持**: 为列表查询添加分页
5. **重命名模块**: 改为 `sism-dashboard` 或添加分析功能

---

**审计完成日期:** 2026-04-06
**下一步行动:** 修复权限控制问题后再部署生产环境