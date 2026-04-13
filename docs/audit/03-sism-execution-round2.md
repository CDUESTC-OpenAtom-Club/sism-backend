# 第二轮审计报告：sism-execution 模块（任务执行）

**审计日期:** 2026-04-13
**审计范围:** 41个Java源文件，涵盖 domain/application/infrastructure/interfaces 四层。
**对比基准:** `docs/audit/03-sism-execution.md`（第一轮，2026-04-12）

---

## 一、第一轮问题修复状态

### 严重级别（Critical）

| 编号 | 问题 | 状态 | 验证说明 |
|------|------|------|----------|
| C-01 | PlanReport 核心字段 @Transient 不持久化 | **未修复** | `PlanReport.java:60-94`，`title`/`content`/`summary`/`progress`/`issues`/`nextPlan`/`submittedBy`/`approvedBy`/`approvedAt`/`rejectionReason` 仍为 `@Transient`。`enrichReportMetadata()` 在内存中填充这些字段，但 save 后重新加载全部丢失。 |
| C-02 | syncApprovedIndicatorProgress N+1 查询 | **已修复** | `ReportApplicationService.java:498-528`，已改为批量加载 `findByIds`，使用 Map 查找，null 安全检查 `detail.progress() == null` 跳过，最终 `saveAll` 批量保存。 |
| C-03 | 重复领域服务与应用服务 | **已修复** | `PlanReportDomainService` 已删除（Glob 确认不存在）。`ReportApplicationService` 为唯一实现。 |
| C-04 | searchReports 权限过滤后分页不一致 | **已修复** | `ReportController.java:164-172`，`searchReports` 端点对非管理员调用 `findReportsByConditionsForOrg`，将 `reportOrgId` 直接传入数据库查询，不再内存过滤。 |

### 高级别（High）

| 编号 | 问题 | 状态 | 验证说明 |
|------|------|------|----------|
| H-01 | 字符串比较日期范围，格式不一致 | **未修复** | `JpaPlanReportRepository.java:40-44`，`BETWEEN :startMonth AND :endMonth` 仍对 String 类型 `reportMonth` 做字符串范围比较。输入 `2026-1` vs `2026-01` 会产生错误结果。 |
| H-02 | PlanReport 状态使用 String 而非枚举 | **未修复** | `PlanReport.java:26-30`，状态仍为 String 常量，`STATUS_SUBMITTED_LEGACY = "IN_REVIEW"` 仍存在。`isSubmitted()` 需同时检查两个值。 |
| H-03 | MilestoneApplicationService 缺少输入校验 | **未修复** | `MilestoneApplicationService.java:38-57`，`createMilestone` 方法无业务校验：`indicatorId` 可为 null、`milestoneName` 可为空、`dueDate` 不验证、`progress` 无 0-100 范围校验。虽然 `CreateMilestoneRequest` DTO 有 Bean Validation 注解，但 Service 层本身可被其他路径绕过。 |
| H-04 | DTO 中 userId 字段多余 | **已修复** | `RejectPlanReportRequest` 仅含 `reason` 字段。`ApprovePlanReportRequest` 和 `SubmitPlanReportRequest` 已删除。Controller 使用 `@AuthenticationPrincipal CurrentUser`。 |
| H-05 | PlanReport.isDeleted Boolean 包装类型 NPE | **已修复** | `PlanReport.java:96-97`，已改为 `boolean isDeleted = false;`（原始类型），配合 `@Column(name = "is_deleted", nullable = false)`。 |
| H-06 | findByUniqueKey 缺 isDeleted 过滤 | **已修复** | `JpaPlanReportRepository.java:99`，已添加 `AND pr.isDeleted = false` 条件。 |
| H-07 | Milestone 贫血模型 | **未修复** | `Milestone.java` 无工厂方法、无校验、无不变量保护。`createdAt`/`updatedAt` 由 `MilestoneApplicationService.java:53-54` 手动设置。 |

### 中级别（Medium）

| 编号 | 问题 | 状态 | 验证说明 |
|------|------|------|----------|
| M-01 | JpaMilestoneRepositoryInternal 重复死代码 | **未修复** | `JpaMilestoneRepositoryInternal.java`（13行）仍存在，与 `JpaExecutionMilestoneRepositoryInternal.java` 重复。无任何代码注入此接口，确认为死代码。 |
| M-02 | PlanReportQueryRequest 无效字段 | **未修复** | `PlanReportQueryRequest.java:18-20`，`title`/`minProgress`/`maxProgress` 仍存在但查询未使用。 |
| M-03 | MilestoneStatus.from(null) 返回 null 存入 DB | **未修复** | `MilestoneStatus.java:14`，`from(null)` 返回 null。`MilestoneController.java:49` 传入 `MilestoneStatus.from(request.getStatus())`，若 status 为 null 则 Milestone.status 列存 null。 |
| M-04 | 分页大小无上限 | **未修复** | `ReportApplicationService.java:712` 和 `MilestoneApplicationService.java:224`，`normalizePageSize` 仅 `Math.max(size, 1)`，无上限限制。`size=999999` 可导致 OOM。 |
| M-05 | updateReport 指标明细状态校验 | **部分修复** | `ReportApplicationService.java:152` 调用 `updateContent()` 检查 DRAFT 状态。后续 `upsertIndicatorDetail` 不单独校验状态，但因 `updateContent` 先行抛出异常，执行流受保护。然而 `upsertIndicatorDetail` 作为独立逻辑单元缺少自身状态校验，设计脆弱。 |
| M-06 | AggregateRoot equals 对新建实体异常 | **未修复** | 继承自 shared-kernel，非本模块范围。 |
| M-07 | snapshotsById 变量名误导 | **未修复** | `JdbcPlanReportIndicatorRepository.java:124`，变量 `snapshotsById` 以 `pri.id`（plan_report_indicator 主键）为 key，变量名暗示以 snapshot 概念索引，实际含义不清。 |
| M-08 | 变量名 m 含义不清 | **未修复** | `MilestoneApplicationService.java:179`，lambda 内 `Map<String, Object> m` 命名过于简短。 |
| M-09 | updateContent 写 Transient 字段 | **未修复** | `PlanReport.java:187-199`，直接关联 C-01。调用 `updateContent` 后 save，数据在重新加载后丢失。 |
| M-10 | 幂等判断因 Transient rejectionReason 失效 | **未修复** | `ReportApplicationService.java:320-321`，`report.getRejectionReason()` 是 `@Transient` 字段，从 DB 加载后为 null。`Objects.equals(reason, report.getRejectionReason())` 永远为 false（reason 非 null 时），幂等保护无效。 |

### 低级别（Low）

| 编号 | 问题 | 状态 | 验证说明 |
|------|------|------|----------|
| L-01 | 中英文注释混用 | **部分** | 仍有混用，非关键问题。 |
| L-02 | MilestoneApplicationService 冗余 import | **已修复** | 当前 import 均被使用。 |
| L-03 | PlanReportResponse indicatorDetails NPE | **部分修复** | `PlanReportResponse.java:70` 直接调用 `report.getIndicatorDetails().stream()`。`PlanReportSimpleResponse.java:41` 已做 null 检查。若 `fromEntity` 在未 enrichment 的报告上调用，`PlanReportResponse` 会 NPE。 |
| L-04 | Milestone 缺少 @PrePersist/@PreUpdate | **未修复** | `Milestone.java` 无 JPA 生命周期回调，依赖应用层手动设置时间戳。 |
| L-05 | 未使用 DTO 类 | **已修复** | 旧 DTO 已删除。`RejectPlanReportRequest` 正常使用。 |
| L-06 | JpaPlanReportRepository 多个查询缺 isDeleted | **部分修复** | 分页查询和条件查询已添加 `isDeleted = false`。但以下 7 个列表方法仍缺少过滤：`findByReportOrgId(Long)`、`findByReportOrgType`、`findByReportMonth`、`findByStatus(String)`、`findByReportOrgIdAndStatus`、`findByOrgIdAndMonthRange`、`findByStatusAndOrgType`。见下方 NEW-3。 |

### 修复统计汇总

| 修复状态 | 数量 | 编号 |
|----------|------|------|
| **已修复** | 8 | C-02, C-03, C-04, H-04, H-05, H-06, L-02, L-05 |
| **部分修复** | 3 | M-05, L-03, L-06 |
| **未修复** | 16 | C-01, H-01, H-02, H-03, H-07, M-01~M-04, M-06~M-10, L-01, L-04 |
| **修复率** | **29.6%** | 8/27 已修复，3/27 部分修复 |

---

## 二、第二轮新发现问题

### 严重级别（Critical）

无新发现。

### 高级别（High）

#### NEW-01. getReportsByStatusPaginated 仍存在内存过滤后分页不一致

**文件:** `interfaces/rest/ReportController.java:250-266`
**关联:** 第一轮 C-04 同类问题，修复了 `/search` 端点但遗漏此端点

**描述:** `getReportsByStatusPaginated` 端点先从数据库获取分页结果（含 `totalElements`），再用 `filterReportsByPermission` 在内存中过滤非管理员所在组织的报告。`totalElements` 包含无权访问的记录数，当前页实际数据少于 `size` 条，前端分页组件显示错误。

```java
// ReportController.java:255-266
Page<PlanReport> reportPage = reportApplicationService.findReportsByStatus(status, page, size);
List<PlanReportSimpleResponse> responses = filterReportsByPermission(reportPage.getContent(), currentUser)
        .stream()
        .map(PlanReportSimpleResponse::fromEntity)
        .collect(Collectors.toList());
PageResult<PlanReportSimpleResponse> pageResult = PageResult.of(
        responses,
        reportPage.getTotalElements(), // 包含其他组织的数据
        reportPage.getNumber(),
        reportPage.getSize()
);
```

**最佳实践方案:** 与 `/search` 端点保持一致，对非管理员将 orgId 条件传入数据库查询，而非内存过滤。Repository 层已有 `findByReportOrgIdAndStatus` 方法，应增加带分页的版本，或扩展 `findByConditions` 支持按状态+组织分页查询。

#### NEW-02. PlanReport @Setter 暴露状态修改，绕过领域不变量

**文件:** `domain/model/report/PlanReport.java:11`（Lombok `@Setter`）
**受影响方法:** `ReportApplicationService.java:303,324,339,351`

**描述:** `PlanReport` 类级 `@Setter` 使 `setStatus()` 公开可调用。`ReportApplicationService` 在 `markWorkflowApproved`（行303）、`markWorkflowRejected`（行324）、`markWorkflowWithdrawn`（行339）、`markWorkflowReturnedForResubmission`（行351）中直接调用 `report.setStatus()`，完全绕过聚合根的 `submit()`/`approve()`/`reject()` 方法所提供的状态机校验和事件发布机制。

这意味着：
- `markWorkflowWithdrawn` 将状态改为 DRAFT 但不发布领域事件
- `markWorkflowRejected` 设置 rejectionReason 到 @Transient 字段（永远不持久化）
- 无状态转换合法性检查（如从 APPROVED 直接设为 DRAFT 不报错）

**最佳实践方案:**
1. 移除类级 `@Setter`，为需要外部设置的字段添加细粒度 setter
2. 为工作流场景新增领域方法：`withdrawToDraft()`、`returnForResubmission()`、`forceApprove()`、`forceReject()`，内含状态校验和事件发布
3. 将 `status` setter 设为 private/package-private

#### NEW-03. 7个列表查询方法缺少 isDeleted 过滤，逻辑删除的报告仍被返回

**文件:** `infrastructure/persistence/JpaPlanReportRepository.java:25,28,31,34,37,40,47`

**描述:** 以下方法不包含 `isDeleted = false` 条件：

| 方法 | 行号 | 被调用方 |
|------|------|----------|
| `findByReportOrgId(Long)` | 25 | `findReportsByOrgId(Long)` -> `ReportController.getReportsByOrgId` |
| `findByReportOrgType(ReportOrgType)` | 28 | `findReportsByOrgType` -> `ReportController.getReportsByOrgType` |
| `findByReportMonth(String)` | 31 | `findReportsByMonth` -> `ReportController.getReportsByMonth` |
| `findByStatus(String)` | 34 | `findReportsByStatus` -> `ReportController.getReportsByStatus`, `findPendingReports` |
| `findByReportOrgIdAndStatus(Long, String)` | 37 | 未直接使用但接口定义 |
| `findByOrgIdAndMonthRange(Long, String, String)` | 40 | `findReportsByOrgAndMonthRange` |
| `findByStatusAndOrgType(String, ReportOrgType)` | 47 | `findReportsByStatusAndOrgType`（接口有，Service 未调用） |

逻辑删除的报告会出现在待审批列表、按月查询、按组织查询等结果中。尤其是 `findPendingReports()` 调用 `findByStatus("SUBMITTED")` 不过滤已删除报告，可能导致已删除的待审批报告仍显示在审批列表中。

**最佳实践方案:** 为所有列表查询方法添加 `WHERE pr.isDeleted = false` 条件。或使用 Hibernate `@Where` 注解在实体级别自动过滤：
```java
@Entity
@Table(name = "plan_report")
@Where(clause = "is_deleted = false")
public class PlanReport extends AggregateRoot<Long> { ... }
```

### 中级别（Medium）

#### NEW-04. Milestone.isPaired Boolean 包装类型 NPE 风险

**文件:** `domain/model/milestone/Milestone.java:56`

**描述:** `private Boolean isPaired;` 使用包装类型。若数据库中为 null，`milestone.getIsPaired()` 返回 null。`MilestoneResponse.fromEntity`（`MilestoneResponse.java:47`）直接传递 `milestone.getIsPaired()` 给 builder，前端接收 null 而非布尔值。虽然 `MilestoneApplicationService.java:51,161` 做了 null 检查，但 `fromEntity` 转换层未做防护。

**最佳实践方案:** 改为 `boolean isPaired = false;`（原始类型），数据库列 `DEFAULT false NOT NULL`。与已修复的 H-05 保持一致。

#### NEW-05. countReportsByStatus 非管理员全量内存计数

**文件:** `interfaces/rest/ReportController.java:313-316`

**描述:** 非管理员调用 `findReportsByStatus(status)` 返回全部报告列表，再 stream filter 计数。当报告量达到数万条时，大量数据加载到内存仅用于计数，严重影响性能。

```java
// ReportController.java:314-316
reportApplicationService.findReportsByStatus(status).stream()
    .filter(report -> Objects.equals(report.getReportOrgId(), requireCurrentOrgId(currentUser)))
    .count();
```

**最佳实践方案:** 在 Repository 层增加 `countByStatusAndOrgId(String status, Long orgId)` 方法，直接在数据库完成计数查询。

#### NEW-06. createReport 方法复杂度过高

**文件:** `application/ReportApplicationService.java:64-139`

**描述:** `createReport` 方法长 75 行，包含 4 个 return 路径，处理 5 种业务场景：
1. 已存在但已逻辑删除 -> 恢复
2. 已存在且为草稿 -> 复用
3. 已存在且已驳回 -> 重置为草稿
4. 已存在且已提交 -> 抛冲突异常
5. 不存在或已审批 -> 新建

该方法圈复杂度约 10，难以测试和维护。且场景 5 中的历史日志记录逻辑（行122-135）判断 `previousRound` 是否为 APPROVED/REJECTED，但 `previousRound` 只在 SUBMITTED 情况被跳过后赋值（行113），逻辑流向不清晰。

**最佳实践方案:** 将 5 种场景提取为独立的私有方法或使用策略模式，降低单个方法复杂度。可参考：

```java
public PlanReport createReport(...) {
    return findLatestReport(planId, reportMonth, reportOrgType, reportOrgId)
        .map(existing -> handleExistingReport(existing, createdBy))
        .orElseGet(() -> createNewReport(reportMonth, reportOrgId, reportOrgType, planId, createdBy));
}

private PlanReport handleExistingReport(PlanReport existing, Long createdBy) {
    if (existing.isDeleted()) return restoreDeletedReport(existing, createdBy);
    if (existing.getStatus().equals(STATUS_DRAFT)) return reuseDraft(existing, createdBy);
    if (existing.getStatus().equals(STATUS_REJECTED)) return resetRejectedReport(existing, createdBy);
    if (existing.getStatus().equals(STATUS_SUBMITTED)) throw new ConflictException(...);
    return null; // APPROVED -> fall through to create new
}
```

#### NEW-07. MilestoneController 创建/更新无用户身份追踪

**文件:** `interfaces/rest/MilestoneController.java:41,60`

**描述:** `createMilestone` 和 `updateMilestone` 端点不接收 `@AuthenticationPrincipal`，无法记录操作人信息。`Milestone` 实体也无 `createdBy`/`updatedBy` 字段。任何通过权限验证的用户可匿名创建/修改里程碑，缺乏审计追踪。

**最佳实践方案:**
1. 添加 `@AuthenticationPrincipal CurrentUser currentUser` 参数
2. Milestone 实体增加 `createdBy`/`updatedBy` 字段
3. 在 Service 层记录操作人

### 低级别（Low）

#### NEW-08. PlanReport.reportMonth 缺少格式校验

**文件:** `domain/model/report/PlanReport.java:113-114`

**描述:** `createDraft` 仅校验 `reportMonth` 非空非空白，不验证 `yyyy-MM` 格式。输入 `2026-1`、`2026/01`、`abc` 均可通过校验。此问题与 H-01 相互影响——若格式不规范，字符串日期范围查询结果不可预期。

**最佳实践方案:** 在 `createDraft` 中添加格式校验：
```java
if (!reportMonth.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
    throw new IllegalArgumentException("Report month must be in yyyy-MM format");
}
```
或在类型层面使用 `YearMonth` 值对象替代 String。

#### NEW-09. JDBC 动态 IN 子句占位符生成

**文件:** `infrastructure/persistence/JdbcPlanReportIndicatorRepository.java:148,206-207`
**文件:** `infrastructure/persistence/JdbcWorkflowApprovalMetadataQuery.java:30`

**描述:** 使用 `String.format` + `Collections.nCopies` 动态拼接 IN 子句占位符。每次不同参数数量生成不同 SQL 字符串，导致数据库 PreparedStatement 缓存失效（Plan Cache Pollution）。高并发场景下可能引发性能问题。

**最佳实践方案:** 对于已知上限的查询（如按 reportId 批量查询），可使用固定大小 IN 子句或数组参数（PostgreSQL 支持 `ANY(?)` 语法）：
```java
// 使用 PostgreSQL ANY 语法
jdbcTemplate.query(
    "SELECT ... WHERE pri.report_id = ANY(?)",
    rs -> { ... },
    reportIds.toArray(new Long[0])
);
```

---

## 三、未修复问题的优先修复建议

### 最高优先级（影响数据完整性）

**1. C-01 @Transient 字段问题（核心架构缺陷）**

这是整个模块最根本的问题，阻塞了多个关联问题的解决。建议的完整方案：

- 方案 A：将 `content`/`summary`/`progress`/`issues`/`nextPlan`/`title` 从 `@Transient` 改为 `@Column`，直接持久化
- 方案 B：这些字段的数据实际已通过 `plan_report_indicator` 表的明细记录存储（由 `upsertIndicatorDetail` 管理），那么 `PlanReport` 上的 @Transient 字段应改为从明细数据聚合计算，而非独立存储
- 方案 C（最小改动）：仅将 `rejectionReason` 改为 `@Column`（解决 M-10 幂等判断失效），其他字段保持 @Transient 但在代码中标注为"仅内存聚合展示用"

**文件路径:** `/sism-backend/sism-execution/src/main/java/com/sism/execution/domain/model/report/PlanReport.java`

**2. H-02 状态枚举化**

```java
// 替换 String 常量为枚举
public enum PlanReportStatus {
    DRAFT, SUBMITTED, APPROVED, REJECTED;

    public boolean isSubmitted() {
        return this == SUBMITTED;
    }
}
```
同时添加数据库迁移将遗留 `IN_REVIEW` 值更新为 `SUBMITTED`，消除 `STATUS_SUBMITTED_LEGACY`。

**文件路径:** `/sism-backend/sism-execution/src/main/java/com/sism/execution/domain/model/report/PlanReport.java`

### 高优先级（影响安全与正确性）

**3. NEW-03 列表查询 isDeleted 过滤**

推荐使用 Hibernate `@Where` 注解全局过滤：
```java
@Where(clause = "is_deleted = false")
```
或为每个列表查询方法手动添加条件。

**文件路径:** `/sism-backend/sism-execution/src/main/java/com/sism/execution/infrastructure/persistence/JpaPlanReportRepository.java`

**4. NEW-02 状态封装保护**

移除 `@Setter`，将 `setStatus` 改为 private，新增领域方法处理工作流状态转换。

**文件路径:** `/sism-backend/sism-execution/src/main/java/com/sism/execution/domain/model/report/PlanReport.java`

---

## 四、第二轮统计

### 总体问题清单

| 来源 | Critical | High | Medium | Low | 合计 |
|------|----------|------|--------|-----|------|
| 第一轮未修复 | 1 | 3 | 8 | 2 | 14 |
| 第一轮部分修复 | 0 | 0 | 1 | 2 | 3 |
| 第二轮新发现 | 0 | 3 | 4 | 2 | 9 |
| **合计** | **1** | **6** | **13** | **6** | **26** |

### 模块健康度评估

| 维度 | 评级 | 说明 |
|------|------|------|
| **数据完整性** | 差 | C-01 @Transient 核心字段不持久化，M-10 幂等判断失效 |
| **安全性** | 中 | NEW-03 isDeleted 过滤缺失，NEW-07 缺审计追踪 |
| **性能** | 中 | C-02 已修复，但 NEW-05 全量内存计数，NEW-09 Plan Cache 污染 |
| **架构一致性** | 中 | C-03 已修复，但 NEW-02 状态封装被破坏，H-07 贫血模型未改 |
| **可维护性** | 中 | NEW-06 方法复杂度高，M-01 死代码未清理 |
| **代码规范** | 中 | L-01 注释混用，M-07/M-08 变量命名问题 |

### 第一轮修复质量评估

已修复的 8 个问题中：
- **C-02（N+1查询）:** 修复方案完整，使用了批量加载 + Map 查找 + saveAll，符合最佳实践。
- **C-03（重复服务）:** 干净删除，无残留引用。
- **C-04（分页权限）:** 修复了 `/search` 端点但遗漏了 `/status/{status}/page` 端点（NEW-01），属于部分覆盖。
- **H-05（Boolean NPE）:** 修复干净，改为原始类型。
- **H-06（isDeleted 过滤）:** 修复了 findByUniqueKey，但其他 7 个列表方法未覆盖（NEW-03）。

**修复风格:** 已修复问题方案合理，但修复范围不够全面，存在同类问题的遗漏（"修点不修面"）。建议后续修复时做全局扫描确保同模式问题全部覆盖。

---

## 五、修复路线图建议

| 优先级 | 任务 | 涉及问题 | 预估工作量 |
|--------|------|----------|-----------|
| P0 | C-01: 决定 @Transient 字段持久化策略并实施 | C-01, M-09, M-10 | 3-5天 |
| P1 | H-02 + NEW-02: 状态枚举化 + 封装保护 | H-02, NEW-02 | 2-3天 |
| P1 | NEW-03: 全局 isDeleted 过滤 | NEW-03, L-06 | 0.5天 |
| P1 | NEW-01 + H-01: 修复遗漏的分页端点 + reportMonth 格式校验 | NEW-01, H-01, NEW-08 | 1天 |
| P2 | H-03 + H-07 + NEW-07: Milestone 领域模型重构 | H-03, H-07, NEW-07, NEW-04, L-04 | 2-3天 |
| P2 | M-04: 分页大小上限限制 | M-04 | 0.5天 |
| P3 | M-01 + M-02 + M-07 + M-08: 代码清理 | M-01, M-02, M-07, M-08 | 1天 |
| P3 | NEW-05 + NEW-06 + NEW-09: 性能优化 | NEW-05, NEW-06, NEW-09 | 1-2天 |

---

**审计人:** Claude Code Audit Agent
**审计轮次:** 第二轮
**下次建议审计时间:** P0/P1 问题修复完成后
