# 审计报告：sism-execution 模块（任务执行）

**审计日期:** 2026-04-12
**审计范围:** 45个Java源文件，涵盖interfaces/application/domain/infrastructure四层。

---

## 一、Critical 严重 (共4个)

### C-01. PlanReport 大量核心字段使用 @Transient，数据不持久化
**文件:** `domain/model/report/PlanReport.java:60-94`
**状态:** 部分缓解（2026-04-12）
**描述:** `title`、`content`、`summary`、`progress`、`issues`、`nextPlan`、`submittedBy`、`approvedBy`、`approvedAt`、`rejectionReason` 等核心业务字段全部标记为 `@Transient`，不会持久化到数据库。`updateContent()` 写入的数据在实体重新加载后全部丢失。`reject()` 设置的 `rejectionReason` 也不会持久化。
**修复建议:** 将这些字段对应的数据库列从 `@Transient` 改为实际 `@Column` 映射，或在数据库表中添加对应列。

### C-02. syncApprovedIndicatorProgress N+1 查询 + progress null 风险
**文件:** `application/ReportApplicationService.java:485-498`
**状态:** 已修复（2026-04-12）
**描述:** 在循环内逐个查找和保存 Indicator 实体。N个指标产生 2N+1 次数据库访问。且 `detail.progress()` 可能为 null，直接传给 `indicator.setProgress()` 可能引发后续异常。
**修复建议:** 批量加载 Indicator 并验证 progress 的 null 安全性：
```java
List<Long> indicatorIds = details.stream()
    .map(PlanReportIndicatorSnapshot::indicatorId).toList();
Map<Long, Indicator> indicators = indicatorRepository.findAllById(indicatorIds)
    .stream().collect(Collectors.toMap(Indicator::getId, Function.identity()));
for (PlanReportIndicatorSnapshot detail : details) {
    Indicator indicator = indicators.get(detail.indicatorId());
    if (indicator != null && detail.progress() != null) {
        indicator.setProgress(detail.progress());
    }
}
indicatorRepository.saveAll(indicators.values());
```

### C-03. 重复的领域服务与应用服务，事件双发风险
**文件:** `domain/service/PlanReportDomainService.java` (全文) 与 `application/ReportApplicationService.java`
**状态:** 已修复（2026-04-12）
**描述:** 两个类都实现了 submit/approve/reject 操作，使用不同的事件发布机制。`PlanReportDomainService` 用 Spring `ApplicationEventPublisher`，`ReportApplicationService` 用 `DomainEventPublisher`。Controller 只调用了 ApplicationService，DomainService 当前是死代码。如误用会导致聚合根事件不清除（内存泄漏）和事件格式不一致。
**修复建议:** 移除 `PlanReportDomainService`，或重构为纯领域规则验证服务。

### C-04. searchReports 权限过滤后分页数据不一致
**文件:** `interfaces/rest/ReportController.java:164-181`
**状态:** 已修复（2026-04-12）
**描述:** 先从数据库获取分页结果，再对内存中的内容列表做权限过滤。导致 `totalElements` 包含无权访问的记录数，当前页数据少于 `size` 条，前端分页组件显示错误。
**修复建议:** 将组织权限条件（`reportOrgId`）添加到数据库查询中，而非内存中过滤。

---

## 二、High 高 (共7个)

### H-01. 字符串比较日期范围，格式不一致时结果错误
**文件:** `infrastructure/persistence/JpaPlanReportRepository.java:40-44`
**描述:** `BETWEEN :startMonth AND :endMonth` 对 String 类型 `reportMonth`（yyyy-MM）做范围查询。若输入格式非严格 yyyy-MM（如 `2026-1` vs `2026-01`），字符串比较结果错误。
**修复建议:** 将 `reportMonth` 改为 `YearMonth` 或至少添加格式验证。

### H-02. PlanReport 状态使用 String 而非枚举
**文件:** `domain/model/report/PlanReport.java:26-30`
**描述:** 状态用 String 常量，数据库可存任意字符串。存在 `STATUS_SUBMITTED_LEGACY = "IN_REVIEW"` 遗留值，`isSubmitted()` 需同时检查两个值，脆弱且易遗漏。
**修复建议:** 创建 `PlanReportStatus` 枚举，用 `@Enumerated(EnumType.STRING)` 映射。

### H-03. MilestoneApplicationService 缺少基本输入校验
**文件:** `application/MilestoneApplicationService.java:38-57`
**描述:** `createMilestone` 直接设置字段值，无业务校验：`indicatorId` 可为 null，`milestoneName` 可为空，`dueDate` 不验证是否在过去，`progress` 不验证 0-100 范围，`status` 可为 null。
**修复建议:** 在 `Milestone` 实体或应用服务中添加校验逻辑。

### H-04. DTO 中 userId 字段多余且不安全
**文件:** `interfaces/dto/ApprovePlanReportRequest.java`, `SubmitPlanReportRequest.java`, `RejectPlanReportRequest.java`
**状态:** 已修复（2026-04-12）
**描述:** 这些 DTO 都包含 `@NotNull userId` 字段，但 Controller 从未使用 DTO 中的 userId（使用 `@AuthenticationPrincipal CurrentUser`）。且这些 DTO 实际上是死代码——Controller 端点没有接收它们。
**修复建议:** 移除未使用的 DTO 类。

### H-05. PlanReport.isDeleted 使用 Boolean 包装类型，NPE 风险
**文件:** `domain/model/report/PlanReport.java:97`
**状态:** 已修复（2026-04-12）
**描述:** `private Boolean isDeleted = false;` 若数据库中为 null，自动拆箱 `if (report.getIsDeleted())` 触发 NPE。
**修复建议:** 改为 `boolean isDeleted = false;`，数据库列定义 `DEFAULT false NOT NULL`。

### H-06. findByUniqueKey 不包含 isDeleted 过滤
**文件:** `infrastructure/persistence/JpaPlanReportRepository.java:98-104`
**状态:** 已修复（2026-04-12）
**描述:** 唯一键查询无 `isDeleted = false` 条件，逻辑删除的报告仍可通过唯一键查询找到。
**修复建议:** 添加 `AND pr.isDeleted = false`。

### H-07. Milestone 贫血模型，缺乏封装和不变量保护
**文件:** `domain/model/milestone/Milestone.java` 与 `application/MilestoneApplicationService.java:43-56`
**描述:** Milestone 是纯贫血模型，无工厂方法、无校验、无默认值设置。`createdAt`/`updatedAt` 由应用服务手动设置。与 `PlanReport` 的 `createDraft()` 工厂方法模式不一致。
**修复建议:** 为 `Milestone` 添加 `create()` 工厂方法，含参数校验和默认值。

---

## 三、Medium 中等 (共10个)

| # | 文件:行号 | 问题 | 类别 |
|---|---|---|---|
| M-01 | `JpaMilestoneRepositoryInternal.java` 全文 | 与 `JpaExecutionMilestoneRepositoryInternal` 完全重复，死代码 | 代码质量 |
| M-02 | `PlanReportQueryRequest.java:18-20` | `title`/`minProgress`/`maxProgress` 字段存在但查询未使用 | 代码质量 |
| M-03 | `MilestoneController.java:49` | `status` 可为 null，`MilestoneStatus.from(null)` 返回 null 存入数据库 | Bug |
| M-04 | 两个 ApplicationService | 分页大小无上限，`size=999999` 可导致 OOM | 安全 |
| M-05 | `ReportApplicationService.java:144-158` | `updateReport` 未充分校验报告状态，已提交/已审批报告的指标明细可被修改 | Bug |
| M-06 | `AggregateRoot.java:98-108` | 新建实体 id 为 null 时 equals 行为异常（继承自 shared-kernel 问题） | Bug |
| M-07 | `JdbcPlanReportIndicatorRepository.java:124` | 变量名 `snapshotsById` 有误导性 | 代码质量 |
| M-08 | `MilestoneApplicationService.java:177` | 变量名 `m` 含义不清 | 代码质量 |
| M-09 | `PlanReport.java:187-199` | `updateContent` 更新 Transient 字段，save 后数据丢失（关联 C-01） | 数据完整性 |
| M-10 | `ReportApplicationService.java:320-321` | 幂等判断因 Transient 字段 `rejectionReason` 从 DB 加载后为 null 永远不成立 | Bug |

---

## 四、Low 低 (共6个)

| # | 文件:行号 | 问题 |
|---|---|---|
| L-01 | 全部文件 | 中英文注释混用，风格不统一 |
| L-02 | `MilestoneApplicationService.java` | 冗余 import |
| L-03 | `PlanReportResponse.java:70` | `indicatorDetails` 可能为 null 导致 NPE |
| L-04 | `Milestone.java` | 缺少 `@PrePersist`/`@PreUpdate` 时间戳管理，与 PlanReport 不一致 |
| L-05 | `ApprovePlanReportRequest` 等 | 未使用的 DTO 类 |
| L-06 | `JpaPlanReportRepository.java` | 多个查询方法缺少 `isDeleted = false` 条件 |

---

## 汇总统计

| 严重性 | 数量 | 关键主题 |
|--------|------|----------|
| **Critical** | 4 | 核心字段不持久化、N+1查询、重复服务、分页权限不一致 |
| **High** | 7 | 字符串日期比较、状态无类型、缺少校验、Boolean拆箱NPE、软删除过滤缺失 |
| **Medium** | 10 | 死代码、无效查询字段、分页无上限、幂等判断失效 |
| **Low** | 6 | 注释混用、冗余import、NPE风险、时间戳管理不一致 |
| **总计** | **27** | |

**优先修复建议：**
1. **C-01** 修复 `@Transient` 字段问题 — 数据完整性基础
2. **C-04** 修复分页权限过滤 — 功能正确性
3. **C-03** 清理重复领域服务 — 架构一致性
4. **C-02** 修复 N+1 查询 — 性能瓶颈
