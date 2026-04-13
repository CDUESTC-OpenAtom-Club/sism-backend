# 审计报告：sism-alert 模块（预警管理）

**审计日期:** 2026-04-12
**审计范围:** 15个Java源文件，涵盖预警CRUD、统计、权限过滤。

---

## 一、Critical 严重 (共3个)

### C-01. extractUserId 缺乏 authentication 空值检查
**文件:** `interfaces/rest/AlertController.java:222`
**描述:** `extractUserId` 直接访问 `authentication.getPrincipal()` 而未对 `authentication` 做 null 检查。若安全过滤器配置异常或被绕过，抛出未捕获的 NPE。
**修复建议:** 添加 `Objects.requireNonNull(authentication, "authentication required")`。
**状态:** 已修复（2026-04-12）

### C-02. 删除预警接口权限控制不足
**文件:** `interfaces/rest/AlertController.java:171-181`
**描述:** `deleteAlert` 仅要求 `isAuthenticated()`，任何已认证用户可永久删除预警。删除不可逆，应限制管理员。
**修复建议:** 改为 `@PreAuthorize("hasRole('ADMIN')")`。
**状态:** 已修复（2026-04-12）

### C-03. API 直接返回 JPA 实体，泄露内部结构
**文件:** `interfaces/rest/AlertController.java`（所有返回Alert的端点）
**描述:** 所有端点直接返回 `Alert` JPA 实体而非 DTO。暴露实体内部结构、`detailJson` 敏感上下文信息、`AggregateRoot` 基类字段。模块已定义 `AlertStatsDTO` 但无 `AlertResponse` DTO。
**修复建议:** 创建 `AlertResponse` DTO，在 Controller 中统一转换。
**状态:** 已修复（2026-04-12）

---

## 二、High 高 (共4个)

### H-01. 全量加载预警后内存过滤 — 性能与安全问题
**文件:** `interfaces/rest/AlertController.java:73-79,103-139`
**描述:** 5个端点均采用"先查全部再内存过滤"模式。预警数量增长时产生大量不必要DB I/O和内存占用。
**修复建议:** 在数据库查询层加入指标ID过滤条件。
**状态:** 已修复（2026-04-12）

### H-02. 统计查询产生大量冗余数据库访问
**文件:** `application/AlertAccessService.java:76-126`
**描述:** `buildAlertStatsForCurrentOrg()` 执行 1次指标查询 + 8次预警计数 = 9次DB查询。可用1条 `GROUP BY severity, status` SQL替代。
**修复建议:** 使用单一聚合查询。
**状态:** 已修复（2026-04-12）

### H-03. Alert.severity 存储为 String 缺乏数据库约束
**文件:** `domain/Alert.java:80-81`
**描述:** `severity` 用 `String` 而非 `@Enumerated(EnumType.STRING)` 的枚举。数据库无约束可写入任意字符串。与 `status` 字段用枚举的方式不一致。
**修复建议:** 改为 `AlertSeverity` 枚举 + 数据库 CHECK 约束。
**状态:** 已修复（2026-04-13）。领域模型已切换为 `AlertSeverity + @Enumerated(EnumType.STRING)`，并补充 Hibernate `@Check` 约束元数据。

### H-04. 跨模块直接依赖 — alert 引用 strategy 领域实体
**文件:** `application/AlertAccessService.java:9-10`
**描述:** 直接依赖 `com.sism.strategy.domain.Indicator` 和其 Repository，违反限界上下文隔离原则。
**修复建议:** 定义反腐败层，通过接口/端口抽象指标访问。
**状态:** 已修复（2026-04-12）。`AlertAccessService` 已改为依赖模块内 `IndicatorAccessPort`，strategy 依赖下沉到 infrastructure 适配器。

---

## 三、Medium 中等 (共5个)

| # | 文件:行号 | 问题 | 类别 |
|---|---|---|---|
| M-01 | AlertController + AlertAccessService | `isAdmin()` 方法重复实现，违反DRY | 代码质量 | 已修复（2026-04-13） |
| M-02 | `AlertStatsDTO.java` | DTO已定义但未使用，`getAlertStats()` 返回 `Map<String,Object>` | 死代码 | 已修复（2026-04-13） |
| M-03 | `AlertApplicationService.java:98-103` | `getUnresolvedAlerts()` 两次独立查询合并，非事务性 | Bug | 已修复（2026-04-13） |
| M-04 | `Alert.java:94-96` | `canPublish()` 仅RESOLVED返回true，与实际事件发布行为不匹配 | 语义 | 已修复（2026-04-13） |
| M-05 | AlertController | 所有查询端点无分页支持，返回 `List<Alert>` | 性能 | 已修复（2026-04-13）。新增分页查询端点并保留原列表端点兼容。 |

---

## 四、Low 低 (共2个)

| # | 文件:行号 | 问题 |
|---|---|---|
| L-01 | `ResolveAlertRequest.java:16` | `resolvedBy` 字段多余，Controller从Authentication提取 | 已修复（2026-04-13） |
| L-02 | `AlertRepository.java:30` | `findBySeverity(String)` 用String而非枚举，风格不一致 | 已修复（2026-04-13）。当前仓储统一使用 `AlertSeverity`。 |

---

## 汇总统计

| 严重性 | 数量 |
|--------|------|
| **Critical** | 3 |
| **High** | 4 |
| **Medium** | 5 |
| **Low** | 2 |
| **总计** | **14** |
