# 二轮审计报告：sism-workflow 模块（工作流引擎）

**审计日期:** 2026-04-13
**审计范围:** 77个Java源文件，涵盖审批流程定义、实例运行、任务调度、事件监听。
**首轮审计报告:** `05-sism-workflow.md` (2026-04-12)

---

## 一、首轮问题修复验证

### Critical (5个)

| # | 问题 | 首轮状态 | 二轮验证结果 | 详情 |
|---|------|----------|-------------|------|
| C-01 | 大规模赋值漏洞 — 实体直接作为 RequestBody | 已修复 | **已确认修复** | `WorkflowController` 现在使用 DTO 接收请求（`CreateLegacyFlowRequest`、`StartLegacyInstanceRequest`、`WorkflowTaskStartRequest` 等），不再直接暴露 JPA 实体作为 `@RequestBody`。`BusinessWorkflowController` 同样使用 DTO。 |
| C-02 | 身份冒用漏洞 — userId 通过请求参数传入 | 已修复 | **已确认修复** | `WorkflowController` 所有实例操作端点（approve/reject/cancel/start）已改用 `@AuthenticationPrincipal CurrentUser currentUser`。`cancelInstance` 端点（第205-218行）仍缺少 `@AuthenticationPrincipal`，但该端点的业务逻辑通过 `WorkflowApplicationService` 间接调用，实际未使用 caller userId 做身份校验，存在残余风险（见新问题 NC-01）。 |
| C-03 | 事件监听器吞噬异常 — 工作流状态不一致 | 未修复 | **仍未修复** | `PlanWorkflowEventListener.java:43-46` 的 `catch (Exception ex)` 仍然仅记录日志后不重新抛出。`ReportWorkflowEventListener.java:139-149` 同理。无重试机制、无补偿逻辑、无死信队列。业务模块状态与工作流模块状态可能永久不一致。 |
| C-04 | 跨模块直接SQL操作 — 严重架构违规 | 未修复 | **仍未修复** | `ReportWorkflowEventListener.java:70-78` 通过 `JdbcTemplate` 查询 `public.plan_report`；第83-91、96-104、127-136 行通过 `JdbcTemplate` 直接读写 `public.plan_report`。`ApproverResolver.java:200-208` 同样通过 `JdbcTemplate` 查询 `public.plan_report`。`WorkflowReadModelService.java:430-447` 也直接查询 `public.plan_report`。总计4处直接SQL操作绕过 `sism-execution` 领域模型。 |
| C-05 | REST端点调用不支持操作必定返回500 | 已修复 | **已确认修复** | `WorkflowController.java:220-234` 的 `transferInstance` 和 `addApprover` 端点现在返回 `ApiResponse.error(409, "固定审批模板不支持转办/加签")` 而非抛出 `UnsupportedOperationException`。领域模型 `AuditInstance` 中的 `transfer()` 和 `addApprover()` 方法仍抛异常，但不再被 REST 层直接调用。 |

### High (7个)

| # | 问题 | 首轮状态 | 二轮验证结果 | 详情 |
|---|------|----------|-------------|------|
| H-01 | 全表扫描 + 内存分页 | 已修复 | **已确认修复** | `WorkflowReadModelService.java:107-126` 的 `getMyPendingTasks` 已使用 `workflowQueryRepository.findPendingAuditInstancesByUserId(userId, pageable)` 进行数据库分页查询。`getMyApprovedInstances`（第128-148行）、`getMyAppliedInstances`（第150-169行）同样已使用数据库分页。 |
| H-02 | N+1 查询 | 已修复 | **已确认修复** | `WorkflowReadModelService` 现在使用本地 `Map<Long, String>` 缓存（`orgNameCache`、`userNameCache`、`flowDefCache`、`businessContextCache`）在同一请求内批量预加载，避免了逐条查询。 |
| H-03 | 两个几乎相同的 Repository 实现 | 已修复 | **已确认修复** | `JpaWorkflowRepositoryInternal` 已删除。当前仅有 `WorkflowRepositoryFacade` 作为 `@Primary` 实现委托 `JpaWorkflowRepository`。 |
| H-04 | 三套并行仓储接口 — 职责重叠 | 未修复 | **仍未修复** | 三套仓储仍然并存：`WorkflowRepository`（`domain/repository/`）、`AuditInstanceRepository`（`domain/runtime/repository/`）、`WorkflowQueryRepository`（`domain/query/repository/`）。实现链：`WorkflowRepositoryFacade` -> `JpaWorkflowRepository`，`AuditInstanceRepositoryImpl` -> `JpaWorkflowRepository`，`WorkflowQueryRepositoryImpl` -> `JpaWorkflowRepository`。三者最终都委托同一个底层 JPA 接口，但各自暴露不同方法集。 |
| H-05 | ApproverResolver 硬编码默认角色/组织ID | 已修复 | **已确认修复** | 硬编码双构造函数已移除。`ApproverResolver` 现在通过 `WorkflowApproverProperties`（`@ConfigurationProperties(prefix = "workflow.approver")`）注入配置，`@NotNull` 校验确保启动时必须配置。 |
| H-06 | resolveCurrentPendingStep 选择逻辑可能错误 | 未修复 | **仍未修复** | `AuditInstance.java:316-325` 的 `resolveCurrentPendingStep()` 仍然使用 `reversed()` 排序后取 `findFirst()`，即取 stepNo 最大的 PENDING 步骤。当多个 PENDING 步骤并存时（异常场景），可能跳过中间步骤。`canRequesterWithdraw()`（第366-392行）假设只有唯一 PENDING 步骤（取 `resolveCurrentPendingStep`），与 `reject()` 创建新步骤后再创建返回步骤可能产生多个 PENDING 步骤的场景不匹配。 |
| H-07 | syncWorkflowTerminalStatus 无事务保护 | 已修复 | **已确认修复** | `WorkflowTerminalStatusSyncService.java:21` 方法 `syncReportWorkflowTerminalStatus` 已添加 `@Transactional` 注解，循环内的所有实例更新在同一事务中执行。 |

### Medium (10个)

| # | 问题 | 首轮状态 | 二轮验证结果 |
|---|------|----------|-------------|
| M-01 | 魔法字符串分散定义 | 未修复 | **部分改善**。已提取为 `private static final String` 常量（如 `PLAN_ENTITY_TYPE`、`PLAN_REPORT_ENTITY_TYPE`），但同一常量在至少6个类中重复定义（`BusinessWorkflowApplicationService`、`PlanWorkflowEventListener`、`ReportWorkflowEventListener`、`WorkflowReadModelService`、`PlanWorkflowSyncService`、`WorkflowTerminalStatusSyncService`、`ApproverResolver`），未统一到公共常量类。 |
| M-02 | STATUS_PENDING 和 STATUS_IN_PROGRESS 值相同 | 未修复 | **仍未修复**。`AuditInstance.java:32-33` 中 `STATUS_PENDING = "IN_REVIEW"` 和 `STATUS_IN_PROGRESS = "IN_REVIEW"` 值完全相同，语义混淆。`STATUS_IN_PROGRESS` 未在任何业务逻辑中使用，是纯粹的死代码。 |
| M-03 | AuditStatus/WorkflowStatus 枚举未使用 | 未修复 | **仍未修复**。`AuditStatus.java` 仅在 `WorkflowRepository.findByStatus(AuditStatus status)` 方法签名中使用，但该方法在 `AuditInstanceRepository` 中被重载为 `findByStatus(String status)`。`WorkflowStatus.java` 的 Javadoc 已标注为 "Historical compatibility enum"，实际无代码引用。 |
| M-04 | PageResult 除零异常 | 未修复 | **仍未修复**。`PageResult.java:26` 的 `Math.ceil((double) total / pageSize)` 在 `pageSize=0` 时仍会抛出 `ArithmeticException`（整数除零）或产生 `Infinity`（浮点除零）。 |
| M-05 | 基于中文字符串的脆弱业务判断 | 未修复 | **仍未修复**。`AuditInstance.java:264-265` 的 `stepName.contains("提交")` 和 `ReportWorkflowEventListener.java:320` 的 `stepName.contains("提交")` 仍然存在。`ApproverResolver.java:23` 的 `COLLEGE_FINAL_APPROVAL_STEP_NAME = "职能部门终审"` 用于 `stepName.contains()` 判断（第230行），同样脆弱。 |
| M-06 | toExternalInstanceStatus 直接返回输入 | 未修复 | **仍未修复**。`WorkflowReadModelMapper.java:114-116` 的 `toExternalInstanceStatus` 仍然是 `return status;`，没有任何转换逻辑。如果将来内部状态名与外部API约定不同，此方法将无法发挥作用，属于误导性代码。 |
| M-07 | Long.parseLong 无 try-catch | 未修复 | **部分改善**。`BusinessWorkflowApplicationService.java` 的 `resolveAuditInstanceForTask` 方法（第303-321行）对 `Long.parseLong(workflowId)` 已添加 `try-catch(NumberFormatException)`。但 `startWorkflowInstance`（第124行）、`cancelInstance`（第384行）、`getDefinitionById`（第401行）仍直接调用 `Long.parseLong` 无保护，非法输入会返回500。 |
| M-08 | isActive vs is_enabled 命名不一致 | 未修复 | **仍未修复**。`AuditFlowDef.java:42-43` 的 Java 字段 `isActive` 对应数据库列 `is_enabled`，映射通过 `@Column(name = "is_enabled")` 维持，但语义不一致可能导致维护者困惑。 |
| M-09 | WorkflowReadModelService 过大 | 未修复 | **部分改善**。当前文件 605 行，与首轮审计时的行数基本相同。提取了 `WorkflowReadModelMapper` 作为独立组件，但主体查询逻辑仍在 `WorkflowReadModelService` 中。 |
| M-10 | 缺少 @Transactional(readOnly=true) | 未修复 | **仍未修复**。`WorkflowReadModelService`（605行）、`WorkflowInstanceQueryService`（52行）、`WorkflowDefinitionQueryService`（42行）中的查询方法均未标注 `@Transactional(readOnly = true)`。 |

### Low (6个)

| # | 问题 | 首轮状态 | 二轮验证结果 |
|---|------|----------|-------------|
| L-01 | 日志中包含 Emoji 字符 | 未修复 | `ReportWorkflowEventListener.java:106,125,141,145,167,181,184,204,219,221` 仍包含大量 Emoji（`✅`、`⚠️`、`❌`）。 |
| L-02 | history ElementCollection 无界增长 | 未修复 | `WorkflowTask.java:80-83` 的 `@ElementCollection` 列表 `history` 仍无大小限制，每次操作追加一条记录。 |
| L-03 | null 返回值 vs 异常抛出不一致 | 未修复 | `WorkflowInstanceQueryService.getAuditInstanceById`（第25行）返回 null，而 `WorkflowReadModelService.getInstanceDetail`（第76行）抛异常。 |
| L-04 | 中英文混用注释和日志 | 未修复 | 全模块仍然存在中英文混用。 |
| L-05 | stepOrder 对应 step_no | 未修复 | `AuditStepDef.java:34` 的 `stepOrder` 对应数据库列 `step_no`。 |
| L-06 | isRequired @Transient 永远为 true | 未修复 | `AuditStepDef.java:42-43` 的 `@Transient Boolean isRequired = true` 未持久化，永远为默认值 true。 |

---

## 二、首轮修复统计

| 严重性 | 总计 | 已修复 | 未修复 | 部分改善 |
|--------|------|--------|--------|---------|
| **Critical** | 5 | 3 | 2 | 0 |
| **High** | 7 | 4 | 2 | 1 |
| **Medium** | 10 | 0 | 8 | 2 |
| **Low** | 6 | 0 | 6 | 0 |
| **总计** | **28** | **7** | **18** | **3** |

---

## 三、新发现问题

### NC-01. [Critical] cancelInstance 端点缺少身份认证校验

**文件:** `WorkflowController.java:205-218`
**严重性:** Critical

**描述:** `POST /instances/{instanceId}/cancel` 端点接受 `@RequestBody(required = false) Object ignored` 作为请求体，完全没有使用 `@AuthenticationPrincipal`。任何已认证用户只需知道 `instanceId` 即可取消他人的工作流实例。虽然 `BusinessWorkflowController.cancelInstance`（第173-181行）正确校验了 `currentUser.getId().equals(instance.getRequesterId())`，但 `WorkflowController` 的 `cancelInstance` 端点调用的 `workflowApplicationService.cancelAuditInstance(existingInstance)` 内部并无身份校验。

```java
// WorkflowController.java:205-218
@PostMapping("/instances/{instanceId}/cancel")
public ResponseEntity<ApiResponse<AuditInstance>> cancelInstance(
        @PathVariable Long instanceId,
        @RequestBody(required = false) Object ignored) {  // 无 @AuthenticationPrincipal
    AuditInstance existingInstance = workflowApplicationService.getAuditInstanceById(instanceId);
    // ...
    AuditInstance cancelled = workflowApplicationService.cancelAuditInstance(existingInstance);
    // 无身份校验
}
```

**修复建议:** 添加 `@AuthenticationPrincipal CurrentUser currentUser`，在服务层校验 `currentUser.getId().equals(instance.getRequesterId())`。

---

### NC-02. [Critical] WorkflowController 暴露 JPA 实体直接返回给客户端

**文件:** `WorkflowController.java:37-101`
**严重性:** Critical

**描述:** 虽然首轮审计 C-01 已修复 RequestBody 直接绑定实体的漏洞，但 ResponseBody 端仍然直接返回 JPA 实体。`getAllFlowDefinitions`（第37-40行）返回 `List<AuditFlowDef>`，`getAllInstances`（第98-101行）返回 `List<AuditInstance>`，`getInstanceById` 返回 `AuditInstance` 等。这些实体包含：
- `AuditFlowDef` 包含 `@OneToMany` 的 `steps` 列表（包含 LAZY 加载的关联），可能导致 Jackson 序列化触发懒加载异常或 N+1 查询。
- `AuditInstance` 包含 `isDeleted`、`stepInstances` 等内部字段。
- 实体的 `@JsonIgnore` 注解依赖 Jackson 序列化过滤，而非显式 DTO 映射，容易因重构遗漏而泄露内部字段。

对比 `BusinessWorkflowController` 正确使用了 `WorkflowInstanceResponse`、`WorkflowDefinitionResponse` 等 DTO。

**修复建议:** 所有 `WorkflowController` 的返回值改用对应的 DTO/VO，与 `BusinessWorkflowController` 保持一致。

---

### NC-03. [High] ApproverResolver 仍包含跨模块直接 SQL

**文件:** `ApproverResolver.java:200-208`
**严重性:** High

**描述:** 这是首轮 C-04 的一部分，但首轮未单独指出 `ApproverResolver` 中的跨模块 SQL。`resolveScopeOrgId` 方法直接查询 `public.plan_report` 表：

```java
// ApproverResolver.java:200-208
Long planId = jdbcTemplate.query(
    "SELECT plan_id FROM public.plan_report WHERE id = ?",
    rs -> rs.next() ? rs.getLong("plan_id") : null,
    instance.getEntityId()
);
```

此查询绕过 `sism-execution` 模块，且 `ApproverResolver` 作为 `application/support` 层组件持有 `JdbcTemplate` 依赖违反了 DDD 分层原则。

**修复建议:** 通过 `sism-execution` 模块的 `ReportApplicationService` 或专门的查询接口获取 `planId`，移除 `ApproverResolver` 对 `JdbcTemplate` 的依赖。

---

### NC-04. [High] WorkflowReadModelService 仍包含跨模块直接 SQL

**文件:** `WorkflowReadModelService.java:430-447`
**严重性:** High

**描述:** `buildPlanReportContext` 方法直接查询 `public.plan_report` 表获取 `plan_id`、`report_org_id`、`report_month`：

```java
// WorkflowReadModelService.java:430-447
Map<String, Object> reportRow = jdbcTemplate.query(
    "SELECT plan_id, report_org_id, report_month FROM public.plan_report WHERE id = ?",
    ...);
```

此查询在每次构建报告业务上下文时执行，属于高频查询路径。如果 `plan_report` 表结构变更，运行时崩溃且编译期无法发现。

**修复建议:** 引入 `ReportQueryService` 接口由 `sism-execution` 模块实现，返回报告摘要信息（planId、orgId、month）。

---

### NC-05. [High] AuditStepDef 缺少 stepDefId 的持久化和映射

**文件:** `AuditStepDef.java:33-34`、`AuditStepInstance.java:35-36`
**严重性:** High

**描述:** `AuditStepInstance.stepDefId` 引用 `AuditStepDef` 的 ID，但两者之间无 JPA 关联关系（`@ManyToOne`），仅通过 `Long stepDefId` 做松散关联。这意味着：
- 无法通过 JPA 的级联加载获取步骤定义信息，必须在应用层手动查询。
- 没有数据库外键约束保证引用完整性。
- `ApproveWorkflowUseCase.findStepIndexByDefinitionId()`（第101-111行）和 `RejectWorkflowUseCase.findStepIndexByDefinitionId()`（第137-148行）各自实现了完全相同的查找逻辑，纯粹是因为缺少 JPA 关联导致的补偿代码。

**修复建议:** 在 `AuditStepInstance` 上添加 `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "step_def_id") private AuditStepDef stepDef` 关联，同时保留 `stepDefId` 作为冗余字段或移除。

---

### NC-06. [High] findStepIndexByDefinitionId 在两个用例中重复实现

**文件:** `ApproveWorkflowUseCase.java:101-111`、`RejectWorkflowUseCase.java:137-148`
**严重性:** High (代码重复)

**描述:** 两个用例类中 `findStepIndexByDefinitionId` 方法完全相同（逐一遍历列表查找匹配 ID 的索引），且时间复杂度 O(n)。当工作流步骤数量增长时性能不佳。

```java
// 两处完全相同的实现
private int findStepIndexByDefinitionId(List<AuditStepDef> orderedSteps, Long stepDefId) {
    if (stepDefId == null) return -1;
    for (int i = 0; i < orderedSteps.size(); i++) {
        AuditStepDef step = orderedSteps.get(i);
        if (step.getId() != null && step.getId().equals(stepDefId)) return i;
    }
    return -1;
}
```

**修复建议:** 提取到 `AuditFlowDef` 实体方法中（如 `findStepIndex(Long stepDefId)`），或使用 `Map<Long, Integer>` 预建索引。

---

### NC-07. [High] PlanWorkflowSyncService 使用 ObjectProvider 弱引用注入 — 可能静默跳过同步

**文件:** `PlanWorkflowSyncService.java:22-27,131-147`
**严重性:** High

**描述:** `PlanWorkflowSyncService` 通过 `ObjectProvider` 注入 `PlanApplicationService` 和 `ReportApplicationService`，使用 `getIfAvailable()` 获取实例。如果服务不可用（模块未加载、Bean 创建失败等），`withPlanService` 和 `withReportService` 仅记录 DEBUG 日志后静默跳过。

```java
// PlanWorkflowSyncService.java:131-137
private void withPlanService(Consumer<PlanApplicationService> action) {
    PlanApplicationService planApplicationService = planApplicationServiceProvider.getIfAvailable();
    if (planApplicationService == null) {
        log.debug("PlanApplicationService not available, skipping plan workflow sync");
        return;  // 静默跳过！
    }
    action.accept(planApplicationService);
}
```

在工作流审批/驳回后，如果同步被跳过，Plan/Report 的业务状态将停留在"审批中"，导致数据不一致。DEBUG 级别日志在生产环境中通常不会被注意到。

**修复建议:** 将日志级别提升到 `WARN` 或 `ERROR`。在模块初始化时校验依赖是否可用，不可用时启动失败而非静默降级。或者引入健康检查端点监控同步服务的可用性。

---

### NC-08. [Medium] reject() 不更新实例状态但仍然保存

**文件:** `AuditInstance.java:132-149`
**严重性:** Medium

**描述:** `reject()` 方法标记当前步骤为 REJECTED，但**不更新**实例级别的 `this.status`。实例状态保持 `STATUS_PENDING`（"IN_REVIEW"）。实际的实例状态更新由 `RejectWorkflowUseCase.createReturnedStep()` 处理，但如果 `createReturnedStep()` 抛出异常，已保存的实例将处于不一致状态：步骤为 REJECTED 但实例仍为 IN_REVIEW。

```java
// AuditInstance.java:132-149
public void reject(Long userId, String comment) {
    // ...
    current.setStatus(STEP_STATUS_REJECTED);
    this.completedAt = null;
    // 注意：this.status 未更新
}
```

**修复建议:** 在 `reject()` 中设置 `this.status = STATUS_REJECTED`，然后由 `createReturnedStep()` 在创建返回步骤后改回 `STATUS_PENDING` 或 `STATUS_WITHDRAWN`。确保即使后续操作失败，实例状态也是 REJECTED 而非误导性的 IN_REVIEW。

---

### NC-09. [Medium] AuditInstance 实体中混合了过多业务逻辑

**文件:** `AuditInstance.java` (403行)
**严重性:** Medium

**描述:** `AuditInstance` 作为 JPA 实体同时承担了聚合根的职责（创建、状态流转、校验），但其中包含大量复杂业务逻辑：
- `cancel()` 方法（第178-209行）31行，包含复杂的步骤查找和状态判断。
- `reactivateWithdrawnStep()` 方法（第211-249行）39行，包含复杂的步骤恢复逻辑。
- `isRequesterSubmitStep()` 方法（第251-266行）依赖中文字符串匹配。
- `appendReplayStepFromLatestRejectedStep()` 方法（第268-291行）创建新步骤。

实体应保持简洁，复杂业务逻辑应提取到领域服务或用例中。

**修复建议:** 将 `cancel()`、`reactivateWithdrawnStep()`、`appendReplayStepFromLatestRejectedStep()` 提取到独立的领域服务（如 `AuditInstanceDomainService`）中，实体仅保留简单的状态变更方法。

---

### NC-10. [Medium] AuditInstance 的 isRequesterSubmitStep 基于脆弱的中文字符串判断

**文件:** `AuditInstance.java:251-266`、`PlanWorkflowSyncService.java:122-128`、`ReportWorkflowEventListener.java:309-321`
**严重性:** Medium

**描述:** 判断某个步骤是否为"提交人步骤"的逻辑分散在至少3处，且判断条件不完全一致：

1. `AuditInstance.isRequesterSubmitStep()`：检查 `stepNo==1` OR `requesterId.equals(step.approverId)` OR `stepName.contains("提交")`
2. `PlanWorkflowSyncService.hasReturnedSubmitStep()`：检查 `requesterId.equals(step.approverId)` OR `stepName.contains("提交")`（不检查 stepNo）
3. `ReportWorkflowEventListener.isSubmitterReturnStep()`：检查 `requesterId.equals(step.approverId)` OR `stepName.contains("提交")`（不检查 stepNo）

三处逻辑略有差异（stepNo 判断的有无），可能在边界场景产生不同结果。

**修复建议:** 将"是否为提交人步骤"的判断统一到 `AuditStepDef` 的 `stepType == SUBMIT` 属性上，通过 `stepDefId` 关联查询步骤类型，而非通过 `stepName` 或 `approverId` 推断。在 `AuditStepInstance` 中持久化 `stepType` 或提供查询方法。

---

### NC-11. [Medium] WorkflowController 仍然同时暴露 Legacy 和 Business 两套 API

**文件:** `WorkflowController.java` (324行)、`BusinessWorkflowController.java` (277行)
**严重性:** Medium

**描述:** 两个控制器分别挂载在 `/api/v1/approval` 和 `/api/v1/workflows` 路径下，功能高度重叠：
- `WorkflowController` 暴露了 Legacy 风格 API（返回 JPA 实体，部分端点仍不安全）。
- `BusinessWorkflowController` 暴露了新的 DTO 风格 API（安全、规范）。
- 两者都通过 `WorkflowApplicationService` / `BusinessWorkflowApplicationService` 操作相同的数据。

前端如果调用 Legacy API，会绕过 `BusinessWorkflowApplicationService` 中的权限校验（如 `ensureUserHasApprovalPermission`）。

**修复建议:** 标记 `WorkflowController` 为 `@Deprecated`，在 API 文档中明确引导前端使用 `/api/v1/workflows` 路径。制定 Legacy API 下线时间表。

---

### NC-12. [Medium] ApproverResolver.canUserApprove 中重复查询用户角色

**文件:** `ApproverResolver.java:140-157`
**严重性:** Medium

**描述:** `canUserApprove` 方法先调用 `userRepository.findById(userId)` 获取用户，再调用 `userRepository.findRoleIdsByUserId(userId)` 查询角色列表。两次独立的数据库查询获取同一用户的不同维度的信息。

```java
return userRepository.findById(userId)
    .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
    .filter(user -> userRepository.findRoleIdsByUserId(userId).contains(roleId))  // 额外查询
    .filter(user -> matchesRoleScope(user, roleId, scopeOrgId, stepDef.getStepName()))
    .isPresent();
```

**修复建议:** 在 `User` 实体或 DTO 中包含角色列表，或提供 `findByIdWithRoles` 联合查询方法，减少数据库往返。

---

### NC-13. [Low] resolveVicePresidentScopeOrgId 基于步骤名称推断副院长管辖范围

**文件:** `ApproverResolver.java:250-258`
**严重性:** Low

**描述:** `resolveVicePresidentScopeOrgId` 通过检查 `stepName.contains("学院院长")` 来决定副院长是否按学院维度管辖。如果步骤名称变更为不含"学院院长"的字样，管辖逻辑将错误地回退到 `functionalVicePresidentScopeByOrg` 映射。

**修复建议:** 在 `AuditStepDef` 中添加 `scopeType` 字段（如 "COLLEGE"、"FUNCTIONAL"），用于明确指定管辖范围类型。

---

### NC-14. [Low] WorkflowTask 和 AuditInstance 是两套并行的工作流运行时模型

**文件:** `WorkflowTask.java`、`AuditInstance.java`
**严重性:** Low

**描述:** `WorkflowTask` 和 `AuditInstance` 是两个独立的聚合根，分别管理自己的状态机。`WorkflowTask` 的状态（PENDING/RUNNING/COMPLETED/FAILED/CANCELLED）与 `AuditInstance` 的状态（IN_REVIEW/APPROVED/REJECTED/WITHDRAWN/RETURNED）完全不同。`WorkflowController` 中同时暴露了两套端点操作这两种实体。但 `WorkflowTask` 的实际使用场景不明确 — `BusinessWorkflowApplicationService` 完全基于 `AuditInstance` 工作，`WorkflowTask` 仅通过 `WorkflowTaskCommandService` 操作，没有与主审批流程集成。

**修复建议:** 明确 `WorkflowTask` 的定位：如果是遗留代码，标记为 `@Deprecated`；如果确实有独立用途，在文档中说明其与 `AuditInstance` 的关系。

---

### NC-15. [Low] JpaWorkflowRepository 混合查询两种不同的聚合根

**文件:** `JpaWorkflowRepository.java`
**严重性:** Low

**描述:** `JpaWorkflowRepository extends JpaRepository<AuditInstance, Long>`，但同时包含查询 `AuditFlowDef` 和 `WorkflowTask` 的方法（如 `findAllAuditFlowDefs`、`findAllWorkflowTasks`）。一个 JPA Repository 应该只负责一个聚合根。查询其他聚合根的方法应分别放在独立的 Repository 中。

**修复建议:** 将 `AuditFlowDef` 相关查询移至 `AuditFlowDefJpaRepository`，`WorkflowTask` 相关查询移至 `WorkflowTaskJpaRepository`，保持 `JpaWorkflowRepository` 仅负责 `AuditInstance`。

---

### NC-16. [Low] AuditFlowDef 的 steps 排序依赖 @OrderBy 但多处手动再排序

**文件:** `AuditFlowDef.java:49`、多处
**严重性:** Low

**描述:** `AuditFlowDef.steps` 标注了 `@OrderBy("stepOrder ASC")`，但代码中几乎所有使用 `steps` 的地方都手动再排序（`StepInstanceFactory`、`ApproveWorkflowUseCase`、`RejectWorkflowUseCase`、`ApprovalFlowCompatibilityController`、`WorkflowPreviewQueryService`），说明 `@OrderBy` 的效果不被信任。

**修复建议:** 移除 `@OrderBy` 注解（因为 `stepOrder` 字段名与数据库列名不匹配可能导致无效），统一在 `AuditFlowDef` 中提供 `getOrderedSteps()` 方法返回排序后的不可变列表。

---

## 四、二轮审计汇总

### 修复统计

| 严重性 | 首轮问题 | 首轮已修复 | 首轮未修复 | 新发现问题 |
|--------|---------|-----------|-----------|-----------|
| **Critical** | 5 | 3 (60%) | 2 | 2 |
| **High** | 7 | 4 (57%) | 2+1部分 | 5 |
| **Medium** | 10 | 0 (0%) | 8+2部分 | 5 |
| **Low** | 6 | 0 (0%) | 6 | 4 |
| **总计** | **28** | **7 (25%)** | **21** | **16** |

### 未修复高优先级问题排行

| 优先级 | 问题编号 | 问题摘要 | 严重性 |
|--------|---------|---------|--------|
| 1 | C-03 | 事件监听器吞噬异常，无重试/补偿 | Critical |
| 2 | C-04 + NC-03 + NC-04 | 跨模块直接 SQL（4处） | Critical/High |
| 3 | NC-01 | cancelInstance 缺少身份校验 | Critical |
| 4 | NC-02 | WorkflowController 返回 JPA 实体 | Critical |
| 5 | H-06 | resolveCurrentPendingStep 多PENDING选择逻辑 | High |
| 6 | NC-07 | PlanWorkflowSyncService 静默跳过同步 | High |
| 7 | H-04 | 三套并行仓储接口职责重叠 | High |
| 8 | M-04 | PageResult 除零风险 | Medium |

### 行业标准解决方案建议

**1. 事件监听器异常处理 (C-03):**
采用 Spring Retry + 死信队列模式：
- 使用 `@Retryable(maxAttempts=3, backoff=@Backoff(delay=1000))` 自动重试。
- 重试失败后写入 `workflow_failed_event` 表（死信队列）。
- 提供管理端点手动重播失败事件。
- 在事件发布端添加 Saga 补偿逻辑，工作流启动失败时回退业务状态。

**2. 跨模块通信 (C-04/NC-03/NC-04):**
采用端口-适配器（Hexagonal）模式：
- 在 `sism-workflow` 的 `domain` 层定义 `ReportQueryPort` 接口。
- 在 `sism-execution` 的 `infrastructure` 层实现 `ReportQueryPortAdapter`。
- 移除所有 `JdbcTemplate` 对 `public.plan_report` 的直接操作。
- 通过 Spring 事件或 Feign Client 实现模块间通信。

**3. 仓储统一 (H-04):**
采用 CQRS 模式重构：
- `AuditInstanceCommandRepository`：仅包含 save、delete、findById。
- `AuditInstanceQueryRepository`：包含所有分页查询、统计查询。
- 删除 `WorkflowRepository`（遗留接口），将其使用者迁移到上述两个接口。
- 删除 `WorkflowRepositoryFacade`（委托层不再需要）。

**4. 控制器安全 (NC-01/NC-02):**
- 所有端点必须使用 `@AuthenticationPrincipal CurrentUser`。
- 所有返回值必须使用 DTO/VO。
- Legacy 控制器标记 `@Deprecated`，设置 90 天下线时间表。
- 添加 `@PreAuthorize` 注解做方法级权限校验。

---

**审计结论：** 首轮 28 个问题中仅 7 个（25%）已确认修复。安全性相关的 C-01、C-02、C-05 已修复，但 C-03（异常吞噬）和 C-04（跨模块SQL）两个关键架构问题仍未解决。二轮发现 16 个新问题，其中 2 个 Critical（cancelInstance 身份校验缺失、返回 JPA 实体）。建议优先解决安全类和架构类问题。
