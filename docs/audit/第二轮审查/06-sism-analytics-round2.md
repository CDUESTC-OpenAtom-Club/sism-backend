# 第二轮审计报告：sism-analytics（数据分析）

**审计日期:** 2026-04-13
**范围:** 37 个 Java 源文件全面复检
**参照:** 第一轮报告 `06-sism-analytics.md` (2026-04-12)

---

## 修复总览

| 指标 | 数值 |
|------|------|
| 第一轮问题总数 | 26 |
| 已确认修复 | **10** (38.5%) |
| 部分修复 | **2** (7.7%) |
| 未修复 | **14** (53.8%) |
| 第二轮新发现 | **18** |

---

## A. 严重问题 (Critical) — 4 项全部修复 ✅

| # | 问题 | 验证证据 |
|---|------|---------|
| C-01 | 路径遍历 | `AnalyticsFileStorageService:66-76` 检查 `resolvedRoot.equals(filesystemRoot)` + `ensureInsideRoot()` |
| C-02 | 客户端提供 filePath 任意读取 | DTO 已移除 filePath，路径由 `prepareManagedExportFile()` 生成 |
| C-03 | 公开方法无权限控制 | 状态变更方法改为包级私有，公开版本需 `currentUserId` |
| C-04 | SQL 列索引硬编码 | `NativeDashboardSummaryQueryRepository` 定义列数常量 + `requireColumnCount()` 校验 |

---

## B. 高优先级问题 (High) — 6 项全部修复 ✅

| # | 问题 | 验证证据 |
|---|------|---------|
| H-01 | 文件下载 OOM | `DataExportController:324-333` 改用 `InputStreamResource` + 50MB 上限 |
| H-02 | 缓存无法刷新 | `DashboardSummaryService:104-108` 暴露 `evictCachedSummaries()` |
| H-03 | 事务内文件 I/O | `ExportService` 方法无 `@Transactional`，委托给 Application Service |
| H-04 | DRY 违反 | `BaseApplicationService:52-66` 统一 `publishAndSaveEvents()` |
| H-05 | LIKE 注入 | `escapeLikePattern()` + 所有查询使用 `ESCAPE '\\'` |
| H-06 | Dashboard 删除事件时序 | `Dashboard.delete()` 内直接调用 `addEvent()` |

---

## C. 中等问题 (Medium) — 0/8 修复

### M-01. Report.generate() 允许重复生成 ❌
**文件:** `domain/Report.java:144-155`

```java
public void generate(String filePath, Long fileSize) {
    if (status.equals(STATUS_FAILED)) {  // 仅阻止 FAILED，不阻止 GENERATED
        throw new IllegalStateException("Cannot regenerate a failed report");
    }
    // GENERATED 状态的报告可被覆盖！
    this.status = STATUS_GENERATED;
    this.filePath = filePath;
}
```

**最优解 — 状态机模式：**
```java
public void generate(String filePath, Long fileSize) {
    Set<String> allowedFrom = Set.of(STATUS_PENDING, STATUS_FAILED);
    if (!allowedFrom.contains(status)) {
        throw new IllegalStateException(
            "Cannot generate report: current status=%s, allowed from=%s"
                .formatted(status, allowedFrom));
    }
    this.status = STATUS_GENERATED;
    this.filePath = Objects.requireNonNull(filePath);
    this.fileSize = fileSize;
    this.generatedAt = LocalDateTime.now();
}
```

### M-03. DataExport.createdAt 标注 @Transient ❌
**文件:** `domain/DataExport.java:77-78`

```java
@Transient  // 永远不会被持久化！
private LocalDateTime createdAt;
```

**API 响应始终返回 `null`。**

**最优解:** 移除 `@Transient`，添加 `@Column`：
```java
@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;
```

### M-05. 表名与领域不匹配 ❌
```java
// Report 实体映射到 execution 模块的表
@Table(name = "progress_report")  // 应为 analytics_report
// DataExport 实体映射到通用任务表
@Table(name = "adhoc_task")       // 应为 data_export
```
**风险:** 与 execution 模块数据碰撞。

### M-06. 逐条删除过期导出 + 嵌套事务 ❌
**文件:** `application/ExportService.java:220-244`

```java
@Transactional
public void cleanupExpiredExports() {
    for (DataExport export : expiredExports) {
        dataExportApplicationService.deleteDataExport(export.getId());
        // 每次调用都有独立 @Transactional → 嵌套事务
        // 且 Files.deleteIfExists 不参与事务回滚
    }
}
```

**最优解 — 批量删除 + 事务一致性：**
```java
@Transactional
public void cleanupExpiredExports() {
    List<DataExport> expired = dataExportRepository.findByStatusAndCreatedAtBefore(
        STATUS_COMPLETED, LocalDateTime.now().minusDays(retentionDays));

    // 先删除数据库记录
    dataExportRepository.deleteAll(expired);  // 单次批量 DELETE

    // 事务提交后再删除文件
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                expired.forEach(export -> {
                    try { Files.deleteIfExists(Path.of(export.getFilePath())); }
                    catch (IOException e) { log.warn("Failed to delete file: {}", export.getFilePath()); }
                });
            }
        });
}
```

### M-08. 事件保存部分失败 ❌
```java
// BaseApplicationService:52-66
for (DomainEvent event : events) {
    eventStore.save(event);  // 第3个失败时前2个已保存
}
eventPublisher.publishAll(events);  // 发布全部包括未保存的！
```

**最优解 — 事务批处理：**
```java
protected void publishAndSaveEvents(AggregateRoot<?> aggregate) {
    List<DomainEvent> events = aggregate.getDomainEvents();
    if (events.isEmpty()) return;

    // 批量保存：要么全部成功，要么全部回滚
    eventStore.saveAll(events);
    aggregate.clearEvents();

    // 仅发布已确认保存的事件
    eventPublisher.publishAll(events);
}
```

---

## D. 第二轮新发现问题

### NH-01~02. [HIGH] CacheManager 注入无 @Qualifier
**文件:** `DashboardSummaryService.java:36`, `@Cacheable` 注解

```java
// 注入无 Qualifier → 可能拿到错误的 CacheManager
private final CacheManager cacheManager;

// @Cacheable 未指定 cacheManager → 可能不走 Caffeine 缓存
@Cacheable(cacheNames = "dashboard-summary", key = "'summary'")
```

**最优解:**
```java
// 方案一：构造器注入指定 Qualifier
public DashboardSummaryService(
    @Qualifier("analyticsCacheManager") CacheManager cacheManager) {
    this.cacheManager = cacheManager;
}

// 方案二：@Cacheable 指定 cacheManager
@Cacheable(cacheManager = "analyticsCacheManager",
           cacheNames = "dashboard-summary", key = "'summary'")
```

### NH-04. [HIGH] ExportService 绕过文件存储服务安全边界
**文件:** `application/ExportService.java:53-56`

```java
// 直接构建路径，未调用 ensureInsideRoot()
Path exportRoot = analyticsFileStorageService.resolveExportRoot();
Path filePath = exportRoot.resolve(fileName);
```

**最优解:** 所有文件路径操作统一走 `AnalyticsFileStorageService`：
```java
Path filePath = analyticsFileStorageService.prepareManagedExportFile(exportName);
```

### NH-05. [HIGH] Controller 端点双重获取竞态条件
**文件:** `DataExportController.java:86-96`

`findDataExportById()` 获取一次 → `completeDataExport()` 内部再获取一次 → 两次之间状态可能已变更。

**最优解:** 单次获取 + 乐观锁：
```java
// Controller 仅传递 ID 和参数
DataExport export = dataExportApplicationService.completeDataExport(
    id, currentUserId, managedPath, fileSize);

// Application Service 内部单次获取 + 版本检查
public DataExport completeDataExport(Long id, Long userId, String path, Long size) {
    DataExport export = findOwnedByCurrentUser(id, userId);
    export.complete(path, size);
    return dataExportRepository.save(export);  // @Version 字段自动检查
}
```

---

## E. 剩余问题统计

| 严重度 | 第一轮剩余 | 第二轮新增 | 总计 |
|--------|-----------|-----------|------|
| Critical | 0 | 0 | 0 |
| High | 0 | 5 | 5 |
| Medium | 8 | 8 | 16 |
| Low | 8 | 5 | 13 |
| **总计** | **16** | **18** | **34** |

## F. Top 5 优先修复

| 优先级 | 问题 | 理由 |
|--------|------|------|
| 🔴 P0 | NH-01/02 CacheManager Qualifier | 缓存静默失效，数据不一致 |
| 🔴 P0 | NH-04 ExportService 绕过安全边界 | 部分重新引入 C-01 路径安全问题 |
| 🟡 P1 | M-03 @Transient createdAt | API 响应永远返回 null |
| 🟡 P1 | M-05 表名不匹配 | 跨模块数据碰撞风险 |
| 🟢 P2 | M-08 事件保存部分失败 | 事件溯源数据不一致 |
