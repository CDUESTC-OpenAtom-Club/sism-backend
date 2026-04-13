# 第二轮审计报告：sism-alert（预警管理）

**审计日期:** 2026-04-13
**范围:** 17 个 Java 源文件全面复检
**参照:** 第一轮报告 `07-sism-alert.md` (2026-04-12)

---

## 修复总览

| 指标 | 数值 |
|------|------|
| 第一轮问题总数 | 14 |
| 已确认修复 | **14** (100%) |
| 第二轮新发现 | **5** |

**评价:** sism-alert 是所有模块中修复率最高的模块，14 项问题全部修复。

---

## A. 第一轮问题修复状态

### Critical (3项) — 全部修复 ✅

| # | 问题 | 验证 |
|---|------|------|
| C-01 | `extractUserId` 无 authentication 空值检查 | `AlertController:281-283` 先检查 `authentication == null` |
| C-02 | 删除预警接口权限不足 | `AlertController:239` 改为 `@PreAuthorize("hasRole('ADMIN')")` |
| C-03 | API 直接返回 JPA 实体 | 全部返回 `AlertResponse` DTO |

### High (4项) — 全部修复 ✅

| # | 问题 | 验证 |
|---|------|------|
| H-01 | 全量加载内存过滤 | 改为数据库层分页查询 |
| H-02 | 统计查询冗余 DB 访问 | 优化为精确计数 + GROUP BY |
| H-03 | severity 存为 String 无约束 | `@Enumerated(EnumType.STRING)` + `@Check` 约束 |
| H-04 | 跨模块直接依赖 | 通过 `IndicatorAccessPort` 接口解耦 |

### Medium (5项) — 全部修复 ✅
### Low (2项) — 全部修复 ✅

---

## B. 第二轮新发现问题

### NM-01. [MEDIUM] Alert.validate() 重复 null 检查
**文件:** `domain/Alert.java:121-126`

```java
if (severity == null) {  // 第 121 行
    throw new IllegalArgumentException("Severity is required");
}
if (severity == null) {  // 第 124 行 — 死代码
    throw new IllegalArgumentException("Severity must be INFO, WARNING, or CRITICAL");
}
```

**最优解:**
```java
public void validate() {
    if (severity == null) {
        throw new IllegalArgumentException("Severity is required (INFO, WARNING, or CRITICAL)");
    }
    // 移除重复检查
    if (indicatorId == null) {
        throw new IllegalArgumentException("Indicator ID is required");
    }
}
```

### NM-02. [MEDIUM] countAlerts 管理员/非管理员统计路径不对称
**文件:** `AlertController.java:254-267`

管理员分支 4 次 DB 查询，非管理员分支 1 次优化查询。且管理员路径使用字符串常量 `Alert.STATUS_OPEN`，非管理员使用枚举 `AlertStatus.OPEN`。

**最优解 — 统一统计路径：**
```java
// 统一走 AlertAccessService，内部区分权限
public AlertStatsResponse getAlertStats(Authentication auth) {
    AlertAccessContext ctx = resolveContext(auth);
    return alertAccessService.buildAlertStats(ctx);
}

// AlertAccessService 内部
public AlertStatsResponse buildAlertStats(AlertAccessContext ctx) {
    List<Long> indicatorIds = resolveAccessibleIndicators(ctx);
    return AlertStatsResponse.from(
        alertRepository.countByStatusGrouped(indicatorIds)  // 单次 GROUP BY
    );
}
```

### NM-03. [MEDIUM] AlertAccessService.resolveAllIndicatorIds() 死代码
**文件:** `application/AlertAccessService.java:247-249`

private 方法，无调用方。直接删除。

### NL-01. [LOW] 领域事件 record 缺少 eventId/occurredOn
**文件:** `domain/event/AlertCreatedEvent.java` 等

三个事件 record 未实现 `DomainEvent` 接口的 `getEventId()` 和 `getOccurredOn()`。

**最优解:**
```java
public record AlertCreatedEvent(
    Long alertId,
    Long indicatorId,
    String severity
) implements DomainEvent {
    // DomainEvent 默认方法提供 eventId 和 occurredOn（如果基类已实现）
}
```

### NL-02. [LOW] StrategyIndicatorAccessAdapter 全量加载 Indicator 仅取 ID
**文件:** `infrastructure/strategy/StrategyIndicatorAccessAdapter.java:34-40`

```java
indicatorRepository.findAll()  // 全量加载所有实体
    .stream().map(Indicator::getId).toList();
```

**最优解 — 投影查询：**
```java
// IndicatorRepository 新增方法
@Query("SELECT i.id FROM Indicator i")
List<Long> findAllIds();

// Adapter 调用
@Override
public List<Long> findAllIndicatorIds() {
    return indicatorRepository.findAllIds();  // 仅查 ID 列
}
```

---

## C. 总结

| 严重度 | 第一轮 | 第二轮新发现 |
|--------|--------|-------------|
| Critical | 3 (✅ 全部修复) | 0 |
| High | 4 (✅ 全部修复) | 0 |
| Medium | 5 (✅ 全部修复) | 3 |
| Low | 2 (✅ 全部修复) | 2 |
| **总计** | **14** | **5** |

**模块评级:** ⭐⭐⭐⭐⭐ (5/5) — 修复率最高，代码质量显著提升。新发现问题均为 Medium/Low 级别。
