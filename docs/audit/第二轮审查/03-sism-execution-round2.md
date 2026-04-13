# 第二轮审计报告：sism-execution（执行管理）

**审计日期:** 2026-04-13
**范围:** 41 个 Java 源文件全面复检
**参照:** 第一轮报告 `03-sism-execution.md` (2026-04-12)

---

## 修复总览

| 指标 | 数值 |
|------|------|
| 第一轮问题总数 | 27 |
| 已确认修复 | **8** (29.6%) |
| 部分修复 | **2** (7.4%) |
| 未修复 | **16** (59.3%) |
| 超出范围 | 1 |
| 第二轮新发现 | **10** |

---

## A. 严重问题 (Critical) — 4 项

### C-01. PlanReport 核心字段 @Transient 数据丢失 ❌ 未修复
**文件:** `domain/model/report/PlanReport.java:60-94`

```java
// 以下核心业务字段仍标注 @Transient，JPA 不持久化：
@Transient private String title;          // 第 61 行
@Transient private String content;        // 第 63 行
@Transient private String summary;        // 第 66 行
@Transient private Integer progress;      // 第 69 行
@Transient private String issues;         // 第 72 行
@Transient private String nextPlan;       // 第 75 行
@Transient private String submittedBy;    // 第 78 行
@Transient private String approvedBy;     // 第 81 行
@Transient private LocalDateTime approvedAt; // 第 87 行
@Transient private String rejectionReason;   // 第 90 行
@Transient private List<PlanReportIndicatorDetail> indicatorDetails; // 第 93 行
```

**影响:** `updateContent()` 写入 transient 字段 → 数据库 reload 后全部丢失。`submit()` 设置 `submittedBy` → 从不持久化。`reject()` 设置 `rejectionReason` → 从不持久化。

**代码注释甚至承认了这个问题 (第 185 行):**
```java
// "These fields are transient - store in remark field instead if needed."
```

**最优解 — 分层持久化策略：**

```java
// 方案一：核心字段持久化到 report 主表（推荐）
@Entity
@Table(name = "plan_report")
public class PlanReport extends AggregateRoot<Long> {
    @Column(name = "title")
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    // ... 移除所有 @Transient 注解

    // 关联指标走子表，用 @OneToMany
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "report_id")
    private List<PlanReportIndicatorDetail> indicatorDetails = new ArrayList<>();
}
```

```sql
-- Flyway 迁移：添加缺失列
ALTER TABLE plan_report
    ADD COLUMN IF NOT EXISTS title VARCHAR(500),
    ADD COLUMN IF NOT EXISTS content TEXT,
    ADD COLUMN IF NOT EXISTS summary TEXT,
    ADD COLUMN IF NOT EXISTS progress INTEGER,
    ADD COLUMN IF NOT EXISTS issues TEXT,
    ADD COLUMN IF NOT EXISTS next_plan TEXT,
    ADD COLUMN IF NOT EXISTS submitted_by VARCHAR(100),
    ADD COLUMN IF NOT EXISTS approved_by VARCHAR(100),
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
```

### C-02. syncApprovedIndicatorProgress N+1 查询 ✅ 已修复
批量加载 + lookup map + `saveAll()`。完全符合审计建议。

### C-03. 重复领域服务/应用服务 ✅ 已修复
`PlanReportDomainService.java` 已删除，仅保留 `ReportApplicationService`。

### C-04. searchReports 内存分页 ✅ 已修复
数据库层面过滤，`totalElements` 正确。**但同模式残留在其他端点**（见新发现 NEW-01）。

---

## B. 高优先级问题 (High) — 7 项

### H-01. 字符串日期范围比较 ❌ 未修复
**文件:** `infrastructure/persistence/JpaPlanReportRepository.java:40-44`

```java
// reportMonth 是 String 列，BETWEEN 是字典序比较
@Query("SELECT pr FROM PlanReport pr WHERE pr.reportMonth BETWEEN :startMonth AND :endMonth")
```
`"2026-1"` vs `"2026-01"` 导致结果错误。

**最优解:**
```java
// 方案一：数据库层面转换为日期类型（推荐）
@Query("SELECT pr FROM PlanReport pr WHERE " +
       "CAST(CONCAT(pr.reportMonth, '-01') AS DATE) BETWEEN " +
       "CAST(CONCAT(:startMonth, '-01') AS DATE) AND " +
       "CAST(CONCAT(:endMonth, '-01') AS DATE)")

// 方案二：DTO 入口校验格式
public record ReportMonthRange(
    @Pattern(regexp = "\\d{4}-\\d{2}") String startMonth,
    @Pattern(regexp = "\\d{4}-\\d{2}") String endMonth
) {}
```

### H-02. PlanReport 状态使用字符串常量 ❌ 未修复
**文件:** `PlanReport.java:26-30`

```java
public static final String STATUS_DRAFT = "DRAFT";
public static final String STATUS_SUBMITTED = "SUBMITTED";
public static final String STATUS_SUBMITTED_LEGACY = "IN_REVIEW";  // 遗留值
```
`isSubmitted()` 必须检查两个值，`setStatus("banana")` 编译通过。

**最优解:** 参见 shared-kernel M-23 的枚举方案，统一为 `ReportStatus` 枚举 + `JpaConverter`。

### H-03. MilestoneApplicationService 缺少输入验证 ⚠️ 部分修复
DTO 已添加 `@NotNull`/`@NotBlank`/`@Min`/`@Max`，但 Service 层仍缺少：
- `dueDate` 过去日期校验
- `status` null 透传（`MilestoneStatus.from(null)` 返回 null）

### H-04. 未使用 DTO ✅ 已修复
### H-05. Boolean isDeleted NPE ✅ 已修复
### H-06. findByUniqueKey 缺少 isDeleted ✅ 已修复

### H-07. Milestone 贫血领域模型 ❌ 未修复
**文件:** `domain/model/milestone/Milestone.java`

```java
// 时间戳由 Application Service 手动设置 — 违反 DDD
milestone.setCreatedAt(LocalDateTime.now());  // MilestoneApplicationService:53
milestone.setUpdatedAt(LocalDateTime.now());  // MilestoneApplicationService:54
```

**最优解 — 富领域模型：**
```java
@Entity
public class Milestone extends Entity<Long> {
    // 工厂方法
    public static Milestone create(Long indicatorId, String name,
                                   BigDecimal targetProgress, LocalDate dueDate) {
        Milestone m = new Milestone();
        m.indicatorId = requireNonNull(indicatorId);
        m.name = requireNonBlank(name);
        m.targetProgress = validateProgress(targetProgress);
        m.dueDate = requireNonNull(dueDate);
        if (dueDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Due date cannot be in the past");
        }
        m.status = MilestoneStatus.PENDING.name();
        m.createdAt = LocalDateTime.now();
        return m;
    }

    // 领域行为
    public void complete(BigDecimal actualProgress) {
        if (getStatusEnum() != MilestoneStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only IN_PROGRESS milestones can be completed");
        }
        this.actualProgress = actualProgress;
        this.status = MilestoneStatus.COMPLETED.name();
        this.completedAt = LocalDateTime.now();
    }
}
```

---

## C. 中等问题 (Medium) — 10 项

### M-01. 重复 JpaMilestoneRepositoryInternal ❌ 死代码
`JpaMilestoneRepositoryInternal.java` 是 `JpaExecutionMilestoneRepositoryInternal.java` 的完全重复，未被注入或引用。

### M-04. 无分页大小上限 ❌ 未修复
```java
private int normalizePageSize(int size) {
    return Math.max(size, 1);  // 无上限！size=999999 导致 OOM
}
```
**最优解:**
```java
private static final int MAX_PAGE_SIZE = 200;

private int normalizePageSize(int size) {
    return Math.clamp(size, 1, MAX_PAGE_SIZE);  // Java 21+
    // 或 Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
}
```

### M-09/M-10. 依赖 C-01 的 transient 字段问题 ❌

---

## D. 第二轮新发现问题

### NEW-01. [HIGH] 多个 Controller 端点仍使用内存权限过滤 + 错误 totalElements ❌
**文件:** `ReportController.java:247-266`

```java
// 与 C-04 修复的 searchReports 相同的问题模式，但存在于 7 个其他端点：
Page<PlanReport> reportPage = reportApplicationService.findReportsByStatus(status, page, size);
List<PlanReportSimpleResponse> responses = filterReportsByPermission(
    reportPage.getContent(), currentUser)  // 内存过滤
    .stream().map(PlanReportSimpleResponse::fromEntity).toList();
PageResult<PlanReportSimpleResponse> pageResult = PageResult.of(
    responses,
    reportPage.getTotalElements(),  // 包含无权限的记录数！
    ...
);
```
**受影响端点:** `getReportsByStatusPaginated`, `getReportsByMonth`, `getReportsByStatus`, `getPendingReports`, `getReportsByOrgType`, `getReportsByPlanId`, `countReportsByStatus`

**最优解:** 统一走数据库层权限过滤：
```java
// 所有端点统一调用带 orgId 的仓储方法
Page<PlanReport> page = isAdmin
    ? reportApplicationService.findReportsByStatus(status, page, size)
    : reportApplicationService.findReportsByStatusForOrg(status, currentOrgId, page, size);
```

### NEW-02. [HIGH] updateReport 状态检查与指标更新之间存在竞态条件
**文件:** `ReportApplicationService.java:161-200`

`updateContent()` 检查 DRAFT 状态 → 但 `upsertIndicatorDetail()` 无状态守卫 → 并发请求可在两个操作之间改变状态。

**最优解:** 使用乐观锁：
```java
@Version
private Long version;  // PlanReport 实体添加版本字段
```

### NEW-05. [MEDIUM] JpaPlanReportRepository 7 个查询缺少 isDeleted 过滤 ❌
```java
// 以下方法返回软删除的记录：
findByReportOrgId(Long)                    // 第 25 行
findByReportOrgType(ReportOrgType)          // 第 28 行
findByReportMonth(String)                   // 第 31 行
findByStatus(String)                        // 第 34 行
findByReportOrgIdAndStatus(Long, String)    // 第 37 行
findByOrgIdAndMonthRange(...)               // 第 40 行
findByStatusAndOrgType(...)                 // 第 47 行
```

### NEW-07. [MEDIUM] PlanReport.createDraft 不校验 reportMonth 格式
**文件:** `PlanReport.java:111-135`
任何非空字符串都被接受，与 H-01 字符串日期比较问题叠加。

---

## E. 剩余问题统计

| 严重度 | 第一轮剩余 | 第二轮新增 | 总计 |
|--------|-----------|-----------|------|
| Critical | 1 (C-01) | 0 | 1 |
| High | 3 | 2 | 5 |
| Medium | 8 | 5 | 13 |
| Low | 4 | 3 | 7 |
| **总计** | **16** | **10** | **26** |

## F. Top 5 优先修复

| 优先级 | 问题 | 理由 |
|--------|------|------|
| 🔴 P0 | C-01 @Transient 数据丢失 | 核心业务数据不持久化，功能形同虚设 |
| 🔴 P0 | NEW-01 内存权限过滤 | 数据泄露 + 分页不一致 |
| 🟡 P1 | NEW-05 7个查询缺 isDeleted | 软删除数据泄露 |
| 🟡 P1 | H-02 字符串状态 | 与 M-23 (shared-kernel) 同类问题 |
| 🟢 P2 | H-07 贫血模型 | 代码可维护性 |
