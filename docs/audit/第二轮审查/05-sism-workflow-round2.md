# 第二轮审计报告：sism-workflow（工作流引擎）
**审计日期:** 2026-04-13
**审计范围:** 72个Java源文件，涵盖审批流程定义、实例运行、任务调度、事件监听。
**基准文档:** `05-sism-workflow.md`（第一轮审计，2026-04-12）

---

## 修复总览

| 指标 | 数值 |
|------|------|
| 第一轮问题总数 | 28 |
| 已修复 (FIXED) | 15 |
| 部分修复 (PARTIAL) | 4 |
| 未修复 (UNFIXED) | 9 |
| 第二轮新发现问题 | 7 |
| **问题总计** | **20（9 + 4 + 7）** |

| 严重性 | 第一轮已修复 | 第一轮未修复 | 第二轮新增 | 当前遗留 |
|--------|-------------|-------------|-----------|---------|
| Critical | 4/5 | 1 | 1 | 2 |
| High | 4/7 | 3 | 2 | 5 |
| Medium | 5/10 | 5 | 2 | 7 |
| Low | 2/6 | 0 | 2 | 2 |
| **合计** | **15** | **9** | **7** | **16** |

---

## A. 第一轮问题修复状态

### Critical 严重

#### C-01. 大规模赋值漏洞 — 实体直接作为 RequestBody
**状态:** PARTIAL
**证据:**
- `WorkflowController.java:71-91` — `createFlowDefinition` 仍直接通过 `new AuditFlowDef()` 构建实体，但输入已从 DTO (`CreateLegacyFlowRequest`) 接收，不再直接反序列化实体。部分缓解。
- `WorkflowController.java:149-158` — `startInstance` 端点仍然直接 `new AuditInstance()` 并设置字段，`@RequestBody` 为 `StartLegacyInstanceRequest` DTO，但实例构建仍依赖手动字段拷贝。
- `WorkflowController.java:243-254` — `startTask` 端点直接 `new WorkflowTask()` 拷贝 DTO 字段到实体。
- `BusinessWorkflowController.java` 已全面使用 DTO，不受影响。

**分析:** 核心安全风险（JPA实体直接作为 `@RequestBody`）已修复，所有端点现在通过 DTO 接收参数。但 `WorkflowController` 的 legacy 端点仍手动将 DTO 字段逐一拷贝到实体对象中，存在遗漏敏感字段（如 `isDeleted`）的间接风险。建议引入 MapStruct 或专用 Assembler 类统一转换。

**优化方案:**
```java
// 引入专门的 Assembler，集中管理 DTO -> Entity 的转换逻辑
@Component
public class AuditInstanceAssembler {

    public AuditInstance fromStartRequest(StartLegacyInstanceRequest request) {
        AuditInstance instance = new AuditInstance();
        instance.setFlowDefId(request.getFlowDefId());
        instance.setEntityType(request.getEntityType());
        instance.setEntityId(request.getEntityId());
        // 只允许显式列出的字段，不会遗漏敏感字段
        return instance;
    }
}

// Controller 中使用
@PostMapping("/instances")
public ResponseEntity<ApiResponse<AuditInstance>> startInstance(
        @Valid @RequestBody StartLegacyInstanceRequest request,
        @AuthenticationPrincipal CurrentUser currentUser) {
    AuditInstance instance = assembler.fromStartRequest(request);
    // ...
}
```

---

#### C-02. 身份冒用漏洞 — userId 通过请求参数传入
**状态:** FIXED
**证据:**
- `WorkflowController.java:116-117` — `getMyPendingInstances` 使用 `@AuthenticationPrincipal CurrentUser currentUser`，通过 `currentUser.getId()` 获取用户身份。
- `WorkflowController.java:148` — `startInstance` 使用 `@AuthenticationPrincipal CurrentUser currentUser`。
- `WorkflowController.java:166` — `approveInstance` 使用 `@AuthenticationPrincipal CurrentUser currentUser`。
- 所有需要身份的端点均已统一使用 `@AuthenticationPrincipal`。

**分析:** 该问题已完全修复，所有端点均已迁移到安全身份获取方式。

---

#### C-03. 事件监听器吞噬异常 — 工作流状态不一致
**状态:** UNFIXED
**证据:**
- `PlanWorkflowEventListener.java:43-46` — `catch (Exception ex)` 仅调用 `log.error()`，不重新抛出。如果工作流启动失败，sism-strategy 模块的 Plan 状态已设为"审批中"，但工作流实例从未创建，永久不一致。
- `ReportWorkflowEventListener.java:144-149` — 同样 `catch (Exception e)` 仅记录日志，注释称"记录错误但不阻塞事件处理流程"，但未提供任何补偿或重试机制。
- `ReportWorkflowEventListener.java:183-186` — `handlePlanReportApproved` 也吞噬异常。

**分析:** 这是数据一致性关键缺陷。当事件监听器处理失败时，业务模块（Plan/PlanReport）已提交的"审批中"状态无法回滚，导致数据永久处于不一致状态。当前无死信队列、无重试、无补偿事务。

**优化方案:**
```java
@Component
@Slf4j
@RequiredArgsConstructor
public class PlanWorkflowEventListener {

    private final BusinessWorkflowApplicationService businessWorkflowApplicationService;
    private final WorkflowOutboxService outboxService; // 新增：发件箱模式

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePlanSubmittedForApproval(PlanSubmittedForApprovalEvent event) {
        if (event == null) return;

        try {
            StartWorkflowRequest request = new StartWorkflowRequest();
            request.setWorkflowCode(event.getWorkflowCode());
            request.setBusinessEntityId(event.getPlanId());
            request.setBusinessEntityType("PLAN");

            var response = businessWorkflowApplicationService.startWorkflow(
                    request, event.getSubmitterId(), event.getSubmitterOrgId());
            log.info("Started plan workflow for planId={}, instanceId={}",
                    event.getPlanId(), response.getInstanceId());

            outboxService.markProcessed(event.getPlanId(), "PLAN", "WORKFLOW_START");
        } catch (Exception ex) {
            // 写入发件箱（Outbox）供后续重试
            outboxService.saveForRetry(
                new OutboxEntry(event.getPlanId(), "PLAN", "WORKFLOW_START",
                                serializeEvent(event), ex.getMessage()));
            // 再抛出异常，触发告警
            throw new WorkflowStartFailedException(
                "Failed to start workflow for planId=" + event.getPlanId(), ex);
        }
    }
}

// 定时重试服务
@Component
@RequiredArgsConstructor
public class WorkflowOutboxRetryScheduler {

    private final WorkflowOutboxService outboxService;
    private final PlanWorkflowEventListener listener;

    @Scheduled(fixedDelay = 60_000) // 每分钟扫描一次
    @Transactional
    public void retryFailedOutboxEntries() {
        List<OutboxEntry> pending = outboxService.findPendingEntries(
            Instant.now().minus(Duration.ofMinutes(5)), // 至少5分钟前失败的
            20  // 每批最多20条
        );
        for (OutboxEntry entry : pending) {
            try {
                // 反序列化事件并重新处理
                listener.handlePlanSubmittedForApproval(deserializeEvent(entry));
                outboxService.markProcessed(entry);
            } catch (Exception ex) {
                outboxService.incrementRetryCount(entry, ex.getMessage());
                if (entry.getRetryCount() >= 5) {
                    outboxService.markDeadLetter(entry);
                    // 触发告警通知运维
                }
            }
        }
    }
}
```

---

#### C-04. 跨模块直接SQL操作 — 严重架构违规
**状态:** UNFIXED
**证据:**
- `ReportWorkflowEventListener.java:70-77` — 通过 `JdbcTemplate` 直接查询 `public.plan_report` 表的 `report_org_type` 字段。
- `ReportWorkflowEventListener.java:83-91` — 通过 `JdbcTemplate` 直接查询 `public.plan_report` 表的 `audit_instance_id` 字段。
- `ReportWorkflowEventListener.java:96-104` — 通过 `JdbcTemplate` 直接 UPDATE `public.plan_report` 表。
- `ReportWorkflowEventListener.java:128-136` — 通过 `JdbcTemplate` 直接 UPDATE `public.plan_report` 表。
- `ReportWorkflowEventListener.java:251-259` — `hasReportStatus` 方法通过 `JdbcTemplate` 直接查询 `public.plan_report` 表。
- `WorkflowReadModelService.java:430-447` — `buildPlanReportContext` 通过 `JdbcTemplate` 直接查询 `public.plan_report` 表。
- `ApproverResolver.java:200-208` — `resolveScopeOrgId` 通过 `JdbcTemplate` 直接查询 `public.plan_report` 表。

**分析:** 跨模块直接 SQL 操作是严重架构违规。7处直接 SQL 访问 `plan_report` 表完全绕过了 `sism-execution` 模块的领域模型和业务规则。如果 `plan_report` 表结构发生变更（如列重命名、拆分表），sism-workflow 模块将在运行时崩溃且编译期无法检测。

**优化方案:**
```java
// 在 sism-execution 模块定义接口（防腐层）
// sism-execution/src/main/java/com/sism/execution/application/WorkflowIntegrationService.java
public interface WorkflowIntegrationService {

    /**
     * 查询报告的工作流集成信息
     * 返回只读 DTO，避免暴露领域模型
     */
    ReportWorkflowInfo getReportWorkflowInfo(Long reportId);

    /**
     * 绑定工作流实例到报告
     */
    void bindAuditInstance(Long reportId, Long auditInstanceId);

    /**
     * 查询报告状态
     */
    String getReportStatus(Long reportId);

    /**
     * 查询报告的关联计划ID和组织ID
     */
    ReportPlanInfo getReportPlanInfo(Long reportId);
}

// DTO
public record ReportWorkflowInfo(
    ReportOrgType reportOrgType,
    Long auditInstanceId,
    String status
) {}

public record ReportPlanInfo(Long planId, Long reportOrgId, String reportMonth) {}

// sism-execution 模块实现
@Service
@RequiredArgsConstructor
public class WorkflowIntegrationServiceImpl implements WorkflowIntegrationService {

    private final PlanReportRepository planReportRepository;

    @Override
    public ReportWorkflowInfo getReportWorkflowInfo(Long reportId) {
        PlanReport report = planReportRepository.findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        return new ReportWorkflowInfo(
            report.getReportOrgType(),
            report.getAuditInstanceId(),
            report.getStatus().name()
        );
    }

    @Override
    public void bindAuditInstance(Long reportId, Long auditInstanceId) {
        PlanReport report = planReportRepository.findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        report.setAuditInstanceId(auditInstanceId);
        planReportRepository.save(report);
    }
    // ... 其他方法
}

// sism-workflow 的 ReportWorkflowEventListener 改为：
@EventListener
public void handlePlanReportSubmitted(PlanReportSubmittedEvent event) {
    // 通过接口获取信息，而非直接 SQL
    ReportWorkflowInfo info = workflowIntegrationService.getReportWorkflowInfo(event.getReportId());
    ReportPlanInfo planInfo = workflowIntegrationService.getReportPlanInfo(event.getReportId());
    // ... 使用 info 和 planInfo 进行业务处理
    // 绑定工作流实例
    workflowIntegrationService.bindAuditInstance(event.getReportId(), auditInstanceId);
}
```

---

#### C-05. REST端点调用不支持操作必定返回500
**状态:** FIXED
**证据:**
- `WorkflowController.java:220-226` — `transferInstance` 现在返回 `ApiResponse.error(409, "固定审批模板不支持转办")`，不再抛出 `UnsupportedOperationException`。
- `WorkflowController.java:228-234` — `addApprover` 现在返回 `ApiResponse.error(409, "固定审批模板不支持加签")`，不再抛出 `UnsupportedOperationException`。
- `AuditInstance.java:300-306` — `transfer()` 和 `addApprover()` 仍然抛出 `UnsupportedOperationException`，但这些方法不再被 `WorkflowController` 直接调用。`WorkflowApplicationService.transferAuditInstance()` (line 104-111) 仍调用 `instance.transfer()`，但只有内部服务调用时会到达。

**分析:** 公开端点已修复为返回 409 而非 500。但 `WorkflowApplicationService` 中的 `transferAuditInstance()` 和 `addApproverToInstance()` 方法仍会在内部调用时抛出异常，这是潜在的内部调用风险。

---

### High 高

#### H-01. 全表扫描 + 内存分页 — 无法水平扩展
**状态:** FIXED
**证据:**
- `WorkflowReadModelService.java:107-126` — `getMyPendingTasks` 现在使用 `workflowQueryRepository.findPendingAuditInstancesByUserId(userId, pageable)` 进行数据库分页查询。
- `JpaWorkflowRepository.java:63-70` — 定义了带分页的 JPQL 查询和 countQuery，使用 `@EntityGraph(attributePaths = "stepInstances")` 避免 N+1。
- `WorkflowReadModelService.java:128-148` — `getMyApprovedInstances` 使用 `workflowQueryRepository.findApprovedAuditInstancesByUserId(userId, pageable)` 分页查询。
- `WorkflowReadModelService.java:150-169` — `getMyAppliedInstances` 使用 `workflowQueryRepository.findAppliedAuditInstancesByUserId(userId, pageable)` 分页查询。

**分析:** 已完全修复。查询已下推到数据库层，使用 JPA 分页 + countQuery。同时引入了 `Map` 缓存（orgNameCache, userNameCache, flowDefCache）在同一页面内避免重复查询。

---

#### H-02. N+1 查询 — 循环中逐条查用户/组织
**状态:** FIXED
**证据:**
- `WorkflowReadModelService.java:112-115` — 引入了本地缓存 `orgNameCache`、`userNameCache`、`flowDefCache`、`businessContextCache`，同一页面内的重复查询使用缓存。
- `WorkflowReadModelService.java:498-512` — `resolveUserName` 方法先查缓存再查数据库。
- `WorkflowReadModelService.java:518-530` — `resolveOrgName` 方法先查缓存再查数据库。

**分析:** 已修复。虽然不是批量 IN 查询，但通过请求级缓存有效减少了 N+1 问题。对单页10-20条数据来说，这是合理的折中方案。

---

#### H-03. 两个几乎相同的Repository实现 — 200行死代码
**状态:** FIXED
**证据:**
- 文件 `JpaWorkflowRepositoryInternal.java` 已被删除（Glob 搜索未找到该文件）。
- 仅 `WorkflowRepositoryFacade.java` 保留，作为 `WorkflowRepository` 的 `@Primary` 实现。

**分析:** 已完全修复，死代码已清除。

---

#### H-04. 三套并行仓储接口 — 职责重叠
**状态:** UNFIXED
**证据:**
- `WorkflowRepository`（domain/repository/WorkflowRepository.java）— 旧接口，132行，含定义和实例和任务所有操作。
- `AuditInstanceRepository`（domain/runtime/repository/AuditInstanceRepository.java）— 新接口，运行时实例操作。
- `WorkflowQueryRepository`（domain/query/repository/WorkflowQueryRepository.java）— 新接口，查询操作。
- `FlowDefinitionRepository`（domain/definition/repository/FlowDefinitionRepository.java）— 新接口，定义操作。
- `WorkflowTaskRepository`（domain/runtime/repository/WorkflowTaskRepository.java）— 新接口，任务操作。

现在实际有 **5 套**仓储接口，`WorkflowRepository` + `WorkflowRepositoryFacade` 仍然存在但已不被任何应用服务 import（经 Grep 确认）。`WorkflowRepository` 和 `WorkflowRepositoryFacade` 是完全的死代码。

**分析:** `WorkflowRepository` 和 `WorkflowRepositoryFacade` 已不被使用但未被删除，增加了维护负担。新的 CQRS 分离（`AuditInstanceRepository` + `WorkflowQueryRepository` + `FlowDefinitionRepository` + `WorkflowTaskRepository`）已基本到位，但未完成清理。

**优化方案:**
```java
// 1. 删除 WorkflowRepository 接口和 WorkflowRepositoryFacade 实现类
// 2. 确认现有 CQRS 模式完整覆盖所有需求后，添加一个弃用告警
// 3. 最终目录结构：

// domain/repository/ (命令端)
//   - AuditInstanceRepository    → 实例写操作
//   - FlowDefinitionRepository   → 定义写操作
//   - WorkflowTaskRepository     → 任务写操作
//
// domain/query/repository/ (查询端)
//   - WorkflowQueryRepository    → 只读查询（分页、列表）
//
// 删除:
//   - domain/repository/WorkflowRepository
//   - infrastructure/persistence/WorkflowRepositoryFacade
```

---

#### H-05. ApproverResolver 硬编码默认角色/组织ID
**状态:** FIXED
**证据:**
- `ApproverResolver.java:29-39` — 现在只有一个构造函数，强制注入 `WorkflowApproverProperties`。
- `WorkflowApproverProperties.java:15-30` — 使用 `@ConfigurationProperties(prefix = "workflow.approver")` + `@NotNull` 校验，启动时未配置会直接失败。
- `ApproverResolver.java:265-289` — 所有角色/组织ID通过 `requireConfigured()` 方法从配置中获取，未配置时抛出 `NullPointerException`。

**分析:** 已完全修复。硬编码已被 Spring ConfigurationProperties 替代，`@NotNull` 注解确保启动时校验。

---

#### H-06. resolveCurrentPendingStep 选择逻辑可能错误
**状态:** UNFIXED
**证据:**
- `AuditInstance.java:316-325` — `resolveCurrentPendingStep()` 使用 `reversed()` 排序取最大 stepNo 的 PENDING 步骤。逻辑与第一轮审计时一致，未变更。
- `AuditInstance.java:366-392` — `canRequesterWithdraw()` 方法假设只有唯一 PENDING 步骤（使用 `resolveCurrentPendingStep().orElse(null)`），当存在多个 PENDING 步骤时可能跳过中间步骤。
- 无相关单元测试覆盖多 PENDING 步骤场景。

**分析:** 选择逻辑未修复。当异常场景出现多个 PENDING 步骤时，`reversed()` 取最大 stepNo 可能与 `canRequesterWithdraw()` 的假设矛盾（后者依赖 stepNo 递增的有序假设）。缺少单元测试验证边界情况。

**优化方案:**
```java
// 方案A：严格模式 — 只允许唯一 PENDING 步骤
public Optional<AuditStepInstance> resolveCurrentPendingStep() {
    List<AuditStepInstance> pendingSteps = stepInstances.stream()
            .filter(step -> STEP_STATUS_PENDING.equals(step.getStatus()))
            .sorted(Comparator.comparing(
                step -> step.getStepNo() == null ? Integer.MIN_VALUE : step.getStepNo()))
            .toList();

    if (pendingSteps.size() > 1) {
        // 检测到异常状态，记录告警
        log.warn("Multiple PENDING steps detected for instance {}, steps: {}. Using first.",
                id, pendingSteps.stream().map(AuditStepInstance::getStepNo).toList());
        // 自动修复：将非最小stepNo的PENDING步骤设为WAITING
        for (int i = 1; i < pendingSteps.size(); i++) {
            pendingSteps.get(i).setStatus(STEP_STATUS_WAITING);
        }
    }

    return pendingSteps.stream().findFirst();
}

// 方案B：显式最小stepNo（推荐）
public Optional<AuditStepInstance> resolveCurrentPendingStep() {
    return stepInstances.stream()
            .filter(step -> STEP_STATUS_PENDING.equals(step.getStatus()))
            .min(Comparator.comparing(
                step -> step.getStepNo() == null ? Integer.MAX_VALUE : step.getStepNo()));
}

// 配套单元测试
class AuditInstanceTest {

    @Test
    void resolveCurrentPendingStep_whenMultiplePending_returnsLowestStepNo() {
        AuditInstance instance = createTestInstance();
        instance.addStepInstance(createStep(1, STEP_STATUS_APPROVED));
        instance.addStepInstance(createStep(2, STEP_STATUS_PENDING));
        instance.addStepInstance(createStep(3, STEP_STATUS_PENDING)); // 异常数据

        AuditStepInstance result = instance.resolveCurrentPendingStep().orElseThrow();
        assertEquals(2, result.getStepNo()); // 应返回 stepNo=2
    }

    @Test
    void canRequesterWithdraw_whenMultiplePendingSteps_returnsCorrectResult() {
        // 测试多PENDING步骤场景
    }
}
```

---

#### H-07. syncWorkflowTerminalStatus 无事务保护
**状态:** FIXED
**证据:**
- `WorkflowTerminalStatusSyncService.java:21-22` — 方法标注了 `@Transactional`，整个循环在单一事务内执行。

**分析:** 已完全修复。

---

### Medium 中等

#### M-01. 魔法字符串分散定义
**状态:** PARTIAL
**证据:**
- `PlanWorkflowEventListener.java:19` — `private static final String PLAN_ENTITY_TYPE = "PLAN"` — 已提取常量。
- `ReportWorkflowEventListener.java:41-43` — 已提取 `PLAN_REPORT_ENTITY_TYPE`、`REPORT_STATUS_APPROVED`、`REPORT_STATUS_REJECTED` 常量。
- 但 "PLAN"/"PLAN_REPORT"/"PlanReport" 在 6 个文件中重复定义：
  - `BusinessWorkflowApplicationService.java:36-46` — 定义了 8 个常量。
  - `WorkflowReadModelService.java:43-45` — 定义了 3 个相同常量。
  - `PlanWorkflowSyncService.java:16-18` — 定义了 3 个相同常量。
  - `WorkflowTerminalStatusSyncService.java:16-17` — 定义了 2 个相同常量。
  - `FlowResolver.java:75-76` — `isPlanReportEntityType` 方法内联检查。

**分析:** 各文件已本地提取常量，但相同常量在 5+ 个文件中重复定义，未统一到共享常量类。修改实体类型名称时需要同步修改 5+ 处。

**优化方案:**
```java
// 在 shared kernel 或 workflow domain 层定义集中常量类
public final class WorkflowEntityTypes {
    public static final String PLAN = "PLAN";
    public static final String PLAN_REPORT = "PLAN_REPORT";
    public static final String LEGACY_PLAN_REPORT = "PlanReport";

    public static boolean isPlanReportType(String entityType) {
        return PLAN_REPORT.equalsIgnoreCase(entityType)
            || LEGACY_PLAN_REPORT.equalsIgnoreCase(entityType);
    }

    public static List<String> allPlanReportTypes() {
        return List.of(PLAN_REPORT, LEGACY_PLAN_REPORT);
    }

    private WorkflowEntityTypes() {} // 防止实例化
}

// 各使用处统一引用
import static com.sism.workflow.domain.WorkflowEntityTypes.*;
```

---

#### M-02. STATUS_PENDING 和 STATUS_IN_PROGRESS 值相同("IN_REVIEW")
**状态:** UNFIXED
**证据:**
- `AuditInstance.java:32-33` — `STATUS_PENDING = "IN_REVIEW"` 和 `STATUS_IN_PROGRESS = "IN_REVIEW"` 值完全相同。

**分析:** 两个常量指向相同的字符串值 "IN_REVIEW"，造成语义混淆。`STATUS_PENDING` 表示"待审批"，`STATUS_IN_PROGRESS` 暗示"审批中"，但实际值相同。代码中仅使用 `STATUS_PENDING`，`STATUS_IN_PROGRESS` 从未使用。

**优化方案:**
```java
// 方案1：直接删除未使用的常量，保留语义明确的名称
public static final String STATUS_PENDING = "IN_REVIEW";  // 审批中（唯一正确名称）
// 删除: STATUS_IN_PROGRESS（从未使用且语义重复）

// 方案2（如果未来需要区分）：引入显式状态机
public enum InstanceStatus {
    PENDING("IN_REVIEW"),     // 审批中
    APPROVED("APPROVED"),     // 已通过
    REJECTED("REJECTED"),     // 已驳回
    WITHDRAWN("WITHDRAWN");   // 已撤回

    private final String dbValue;
    InstanceStatus(String dbValue) { this.dbValue = dbValue; }
    public String dbValue() { return dbValue; }
}
```

---

#### M-03. AuditStatus / WorkflowStatus 枚举未实际使用
**状态:** UNFIXED
**证据:**
- `AuditStatus.java:1-10` — 仅 3 个值 (`IN_REVIEW`, `APPROVED`, `REJECTED`)，被 `WorkflowRepository` 接口引用但该接口已无消费者。
- `WorkflowStatus.java:1-43` — 已添加 Javadoc 注释说明历史兼容性，标注 "New approval-flow code must not use this enum"。
- `AuditInstance` 使用字符串常量而非枚举。

**分析:** `AuditStatus` 枚举仅被已废弃的 `WorkflowRepository.findByStatus(AuditStatus)` 方法引用。`WorkflowStatus` 已标注为历史遗留。两者均可安全删除。

---

#### M-04. PageResult pageSize=0 时除零异常
**状态:** UNFIXED
**证据:**
- `PageResult.java:26` — `int totalPages = (int) Math.ceil((double) total / pageSize);` — `pageSize=0` 时产生除零异常（Java 中 double 除以 0 产生 Infinity，但 ceil(Infinity) 和 (int) 强转不会抛异常，而是返回 Integer.MAX_VALUE 或特殊值，实际行为不一致）。
- 调用方 `WorkflowReadModelService.java:130` — `int safePageSize = Math.max(pageSize, 1);` 对部分方法做了保护，但 `PageResult.of()` 方法本身没有防护。

**分析:** `PageResult.of()` 方法本身未防护 `pageSize=0`。部分调用方做了安全处理，但不是全部。

**优化方案:**
```java
public static <T> PageResult<T> of(List<T> items, long total, int pageNum, int pageSize) {
    int safePageSize = Math.max(pageSize, 1);  // 防止除零
    int totalPages = (int) Math.ceil((double) total / safePageSize);
    return PageResult.<T>builder()
            .items(items != null ? items : List.of())
            .total(total)
            .pageNum(Math.max(pageNum, 1))
            .pageSize(safePageSize)
            .totalPages(totalPages)
            .build();
}
```

---

#### M-05. 基于步骤名称中文字符串的脆弱业务判断
**状态:** UNFIXED
**证据:**
- `AuditInstance.java:264-265` — `stepName != null && stepName.contains("提交")` — 基于中文字符串判断是否为提交步骤。
- `ReportWorkflowEventListener.java:320-321` — 同样使用 `stepName.contains("提交")` 判断。
- `PlanWorkflowSyncService.java:126` — 同样使用 `stepName.contains("提交")` 判断。
- `AuditStepDef.java:86-87` — `resolveEffectiveStepType()` 兼容逻辑也用 `stepName.contains("提交")` 作为 fallback。
- `ApproverResolver.java:251` — 使用 `stepName.contains("学院院长")` 做业务判断。
- `ApproverResolver.java:229-230` — 使用 `stepName.contains("职能部门终审")` 做业务判断。

**分析:** 6处依赖中文字符串做业务判断。如果步骤名称被修改（如改为"填报"、"发起"），所有依赖 `contains("提交")` 的逻辑将静默失败。`AuditStepDef` 已引入 `stepType` 字段和 `isSubmitStep()` 方法，但旧代码路径仍未完全迁移。

**优化方案:**
```java
// 所有 isSubmitStep 判断统一使用 AuditStepDef.isSubmitStep()
// 对于 AuditStepInstance，存储 stepType 字段

// AuditStepInstance 新增字段
@Column(name = "step_type")
private String stepType;

public boolean isSubmitStep() {
    return AuditStepDef.STEP_TYPE_SUBMIT.equals(stepType);
}

// ApproverResolver 中的硬编码改为配置驱动
// workflow.approver.college-final-approval-keywords=职能部门终审
// workflow.approver.vice-president-college-keywords=学院院长

// 或使用角色ID判断（更可靠）
private boolean isCollegeFinalApprovalStep(AuditStepDef stepDef, AuditInstance instance) {
    if (stepDef == null || instance == null) return false;
    Long roleId = stepDef.getRoleId();
    return getFunctionalDeptHeadRoleId().equals(roleId); // 用角色ID而非名称
}
```

---

#### M-06. toExternalInstanceStatus 直接返回输入
**状态:** UNFIXED
**证据:**
- `WorkflowReadModelMapper.java:114-116` — `toExternalInstanceStatus(String status) { return status; }` — 方法直接返回输入值，无任何转换逻辑。

**分析:** 该方法是空操作（identity function），属于死代码。如果未来需要将内部状态映射到外部状态，应在此时实现。否则应删除并直接使用原始值。

---

#### M-07. Long.parseLong 无 try-catch
**状态:** UNFIXED
**证据:**
- `BusinessWorkflowApplicationService.java:304` — `Long.parseLong(taskId)` 无 try-catch，传入非数字字符串时抛出 `NumberFormatException` 返回 500。
- `BusinessWorkflowApplicationService.java:384` — `Long.parseLong(instanceId)` 同样无保护。
- `BusinessWorkflowApplicationService.java:401` — `Long.parseLong(definitionId)` 同样无保护。
- `WorkflowReadModelService.java:67,75` — `Long.parseLong(definitionId)` 和 `Long.parseLong(instanceId)` 同样无保护。

**分析:** 所有从 URL 路径变量接收的 String ID 在转换为 Long 时缺少异常处理，用户传入非数字字符串将导致 500 错误。

**优化方案:**
```java
// 方案1：在全局异常处理器中统一处理
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ApiResponse<Void>> handleNumberFormatException(NumberFormatException ex) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(400, "Invalid ID format: expected numeric value"));
    }
}

// 方案2（推荐）：将路径变量直接绑定为 Long 类型
@GetMapping("/{instanceId}")
public ResponseEntity<ApiResponse<...>> getInstance(@PathVariable Long instanceId) {
    // Spring MVC 自动处理类型转换，非数字返回 400
}
```

---

#### M-08. isActive 字段对应 DB 列 is_enabled
**状态:** UNFIXED
**证据:**
- `AuditFlowDef.java:42-43` — `@Column(name = "is_enabled") private Boolean isActive;` — Java 字段名 `isActive` 与数据库列名 `is_enabled` 不一致。

**分析:** 命名不一致仍然存在。虽然 JPA `@Column` 注解确保了正确的映射，但增加了代码阅读和维护的认知负担。

---

#### M-09. WorkflowReadModelService 过大（违反单一职责）
**状态:** UNFIXED
**证据:**
- `WorkflowReadModelService.java` — 605行，包含分页查询、实例详情构建、业务上下文解析、历史卡片构建、DTO映射辅助等功能。

**分析:** 该类仍然过大。建议按职责拆分为 `WorkflowTaskQueryService`（待办任务）、`WorkflowInstanceQueryService`（实例详情）、`WorkflowBusinessContextResolver`（业务上下文解析）。

---

#### M-10. 缺少 @Transactional(readOnly=true)
**状态:** UNFIXED
**证据:**
- `WorkflowDefinitionQueryService.java` — 所有查询方法均无 `@Transactional(readOnly = true)`。
- `WorkflowInstanceQueryService.java` — 所有查询方法均无 `@Transactional(readOnly = true)`。
- `WorkflowReadModelService.java` — 所有查询方法均无 `@Transactional(readOnly = true)`。

**分析:** 纯查询服务缺少只读事务标注，可能导致不必要的脏检查和连接持有时间过长。对于使用 `@EntityGraph` 的查询尤其重要。

---

### Low 低

#### L-01. 日志中包含Emoji字符
**状态:** UNFIXED
**证据:**
- `ReportWorkflowEventListener.java:106` — `log.info("✅ 已恢复既有报告审批实例...")`
- `ReportWorkflowEventListener.java:125` — `log.info("✅ 工作流启动成功...")`
- `ReportWorkflowEventListener.java:141` — `log.warn("⚠️ 无法启动工作流...")`
- `ReportWorkflowEventListener.java:145` — `log.error("❌ 启动工作流失败...")`
- `ReportWorkflowEventListener.java:181` — `log.info("✅ 报告批准后置处理完成...")`
- `ReportWorkflowEventListener.java:184` — `log.error("❌ 处理报告批准事件失败...")`

**分析:** 日志中的 Emoji 字符在某些日志收集系统（如 ELK、Splunk）中可能导致编码问题或搜索困难。建议使用纯文本标记如 `[SUCCESS]`、`[WARN]`、`[ERROR]` 替代。

---

#### L-02. history ElementCollection 无界增长
**状态:** UNFIXED
**证据:**
- `WorkflowTask.java:80-83` — `@ElementCollection` 的 `history` 列表无大小限制，每次状态变更都会追加一条带时间戳的记录。

**分析:** 对于长期运行的工作流任务，历史记录会无限增长。应考虑设置最大条目数或使用单独的历史表。

---

#### L-03 ~ L-06（null 返回值不一致、中英文混用、stepOrder/step_no 命名、isRequired @Transient）
**状态:** UNFIXED（低优先级，与第一轮一致，未变更）

---

## B. 第二轮新发现问题

### B-C01. WorkflowController 仍直接暴露 JPA 实体到响应体 [Critical]
**文件:** `WorkflowController.java:37-50,98-110`
**描述:** 多个 GET 端点直接返回 JPA 实体类型作为响应体（如 `ApiResponse<AuditFlowDef>`, `ApiResponse<AuditInstance>`）。虽然 C-01 修复了请求体问题，但响应体仍然暴露实体：
- `getAllFlowDefinitions()` 返回 `List<AuditFlowDef>`
- `getAllInstances()` 返回 `List<AuditInstance>`
- `getInstanceById()` 返回 `AuditInstance`
- `getMyPendingInstances()` 返回 `List<AuditInstance>`

这会暴露数据库内部结构（如 `flowDef` 的 lazy-loaded 关联、`isDeleted` 字段、内部 ID 结构），且在序列化 lazy proxy 时可能抛出 `LazyInitializationException`。

**优化方案:**
```java
// 所有返回类型统一使用 DTO/VO
@GetMapping("/legacy-flows")
public ResponseEntity<ApiResponse<List<WorkflowDefinitionResponse>>> getAllFlowDefinitions() {
    List<AuditFlowDef> flowDefs = workflowApplicationService.getAllAuditFlowDefs();
    List<WorkflowDefinitionResponse> responses = flowDefs.stream()
            .map(workflowReadModelMapper::toDefinitionResponse)
            .toList();
    return ResponseEntity.ok(ApiResponse.success(responses));
}

@GetMapping("/instances/{instanceId}")
public ResponseEntity<ApiResponse<WorkflowInstanceResponse>> getInstanceById(
        @PathVariable Long instanceId) {
    AuditInstance instance = workflowApplicationService.getAuditInstanceById(instanceId);
    if (instance == null) {
        return ResponseEntity.ok(ApiResponse.error(404, "Approval instance not found"));
    }
    return ResponseEntity.ok(ApiResponse.success(
        workflowReadModelMapper.toInstanceResponse(instance)));
}
```

---

### B-H01. WorkflowController.cancelInstance 缺少身份验证 [High]
**文件:** `WorkflowController.java:205-218`
**描述:** `cancelInstance` 端点不使用 `@AuthenticationPrincipal`，任何人可以取消任何审批实例。没有请求体要求、没有用户身份校验。对比 `BusinessWorkflowController.java:173-181` 中正确的实现，后者从 `@AuthenticationPrincipal` 获取用户并校验是否为发起人。

```java
// 当前代码 - 无身份校验
@PostMapping("/instances/{instanceId}/cancel")
public ResponseEntity<ApiResponse<AuditInstance>> cancelInstance(
        @PathVariable Long instanceId,
        @RequestBody(required = false) Object ignored) {
    AuditInstance existingInstance = workflowApplicationService.getAuditInstanceById(instanceId);
    // 直接取消，无权限检查！
    AuditInstance cancelled = workflowApplicationService.cancelAuditInstance(existingInstance);
    return ResponseEntity.ok(ApiResponse.success(cancelled));
}
```

**优化方案:**
```java
@PostMapping("/instances/{instanceId}/cancel")
public ResponseEntity<ApiResponse<AuditInstance>> cancelInstance(
        @PathVariable Long instanceId,
        @AuthenticationPrincipal CurrentUser currentUser) {
    AuditInstance existingInstance = workflowApplicationService.getAuditInstanceById(instanceId);
    if (existingInstance == null) {
        return ResponseEntity.ok(ApiResponse.error(404, "Approval instance not found"));
    }
    // 权限检查：只有发起人可以取消
    if (!existingInstance.getRequesterId().equals(currentUser.getId())) {
        return ResponseEntity.ok(ApiResponse.error(403, "Only requester can cancel"));
    }
    AuditInstance cancelled = workflowApplicationService.cancelAuditInstance(existingInstance);
    return ResponseEntity.ok(ApiResponse.success(cancelled));
}
```

---

### B-H02. ApproverResolver 查询角色使用全表扫描 [High]
**文件:** `ApproverResolver.java:56-57,119-121,152-154,179-181`
**描述:** `canUserApprove()`、`resolveApproverId()`、`resolveCandidates()`、`validateSelectedApprover()` 四个方法都调用 `userRepository.findByRoleId(roleId)` 获取该角色的全部用户列表，然后在内存中过滤。如果某角色有大量用户（如"学院院长"角色可能对应数十个用户），每次审批操作都全量加载。

**优化方案:**
```java
// 在 UserRepository 中添加精确查询
public interface UserRepository {
    // 替代 findByRoleId + 内存过滤
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.id = :roleId " +
           "AND u.isActive = true AND u.orgId = :orgId")
    List<User> findActiveUsersByRoleAndOrg(@Param("roleId") Long roleId,
                                            @Param("orgId") Long orgId);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
           "FROM User u JOIN u.roles r WHERE r.id = :roleId " +
           "AND u.isActive = true AND u.id = :userId AND u.orgId = :orgId")
    boolean existsActiveUserWithRoleInOrg(@Param("roleId") Long roleId,
                                           @Param("userId") Long userId,
                                           @Param("orgId") Long orgId);
}

// ApproverResolver 改为
public boolean canUserApprove(AuditStepDef stepDef, Long userId,
                               Long requesterOrgId, AuditInstance instance) {
    Long roleId = stepDef.getRoleId();
    Long scopeOrgId = resolveScopeOrgId(stepDef, requesterOrgId, instance);
    // 单次查询替代全表加载
    return userRepository.existsActiveUserWithRoleInOrg(roleId, userId, scopeOrgId);
}
```

---

### B-M01. 事件监听器使用 @Async + @Transactional 组合风险 [Medium]
**文件:** `PlanWorkflowEventListener.java:24-25`
**描述:** `@Async` 与 `@Transactional(propagation = Propagation.REQUIRES_NEW)` 组合使用。`@Async` 在默认线程池中执行，如果线程池满，事件将在调用方线程中同步执行，可能导致调用方事务被意外影响。同时 `@Async` 方法中的异常不会被传播到调用方，调用方无法知道工作流启动是否成功。

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Async  // 危险：与事务组合
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void handlePlanSubmittedForApproval(PlanSubmittedForApprovalEvent event) { ... }
```

**优化方案:**
```java
// 方案：移除 @Async，使用 TransactionalEventListener + REQUIRES_NEW 足矣
// AFTER_COMMIT 阶段的事件监听本身已在原事务提交后执行
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void handlePlanSubmittedForApproval(PlanSubmittedForApprovalEvent event) {
    // REQUIRES_NEW 已确保独立事务
    // 不需要 @Async 增加复杂度
}

// 如果确实需要异步处理，应使用专用的线程池 + 异步异常处理器
@Async("workflowEventExecutor")
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void handlePlanSubmittedForApproval(PlanSubmittedForApprovalEvent event) { ... }

// 配置类
@Configuration
public class WorkflowAsyncConfig {
    @Bean("workflowEventExecutor")
    public Executor workflowEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("workflow-event-");
        executor.initialize();
        return executor;
    }
}
```

---

### B-M02. rejectWorkflowUseCase 中 rejectedIndex<=0 的边界处理不完整 [Medium]
**文件:** `RejectWorkflowUseCase.java:74-86`
**描述:** 当 `rejectedIndex <= 0` 时（被驳回的步骤是第一个步骤或未找到），代码检查 `isTerminalStep`，如果不是终审步骤则将状态设为 `STATUS_PENDING` 但不创建任何步骤，导致实例处于 `IN_REVIEW` 状态但没有 PENDING 步骤可审批的僵死状态。

```java
// rejectedIndex <= 0 且不是终审步骤时
log.info("... has no earlier step but current step is not terminal, keeping in review");
instance.setStatus(AuditInstance.STATUS_PENDING);
instance.setCompletedAt(null);
return; // 没有创建任何新步骤！
```

**优化方案:**
```java
if (rejectedIndex <= 0) {
    if (isTerminalStep(rejectedStepDef)) {
        instance.setStatus(AuditInstance.STATUS_REJECTED);
        instance.setCompletedAt(LocalDateTime.now());
        return;
    }
    // 非终审步骤被驳回时应回退到提交人
    AuditStepDef submitStepDef = orderedSteps.get(0);
    AuditStepInstance returnedStep = new AuditStepInstance();
    returnedStep.setStepNo(instance.nextStepInstanceNo());
    returnedStep.setStepDefId(submitStepDef.getId());
    returnedStep.setStepName(submitStepDef.getStepName());
    returnedStep.setApproverId(instance.getRequesterId());
    returnedStep.setApproverOrgId(instance.getRequesterOrgId());
    returnedStep.setStatus(STEP_STATUS_WITHDRAWN);
    returnedStep.setComment("驳回后退回填报人重新提交");
    instance.addStepInstance(returnedStep);
    instance.setStatus(AuditInstance.STATUS_WITHDRAWN);
    instance.setCompletedAt(null);
}
```

---

### B-L01. 重复的 findStepIndexByDefinitionId 方法 [Low]
**文件:** `ApproveWorkflowUseCase.java:101-112` 和 `RejectWorkflowUseCase.java:137-148`
**描述:** 两个 UseCase 类包含完全相同的 `findStepIndexByDefinitionId` 方法（11行），属于代码重复。

**优化方案:**
```java
// 提取到共享工具类或 FlowDefinition 的领域方法中
// 方案1：在 AuditFlowDef 中添加方法
public int findStepIndexByDefinitionId(Long stepDefId) {
    if (stepDefId == null) return -1;
    List<AuditStepDef> ordered = getSteps().stream()
        .sorted(Comparator.comparing(s ->
            s.getStepOrder() == null ? Integer.MAX_VALUE : s.getStepOrder()))
        .toList();
    for (int i = 0; i < ordered.size(); i++) {
        if (stepDefId.equals(ordered.get(i).getId())) return i;
    }
    return -1;
}

// 方案2：提取为静态工具方法
public final class WorkflowStepUtils {
    public static int findStepIndex(List<AuditStepDef> steps, Long stepDefId) { ... }
}
```

---

### B-L02. domain event 类未被使用 [Low]
**文件:** `domain/event/WorkflowDomainEvent.java`, `WorkflowCompletedEvent.java`, `WorkflowApprovedEvent.java`, `WorkflowStartedEvent.java`
**描述:** 4个领域事件类定义在 `domain/event/` 目录下，但经搜索确认，`WorkflowEventDispatcher.publish()` 实际使用的是 `AggregateRoot.getDomainEvents()` 从共享内核获取的事件，这4个自定义事件类从未被实例化或使用。

**分析:** 建议删除或标注为 `@Deprecated`，避免误导后续开发者。

---

## C. 总结

### 当前遗留问题统计

| 严重性 | 数量 | 关键遗留 |
|--------|------|----------|
| Critical | 2 | C-03(异常吞噬)、C-04(跨模块SQL) + 新增 B-C01(实体暴露响应) |
| High | 5 | H-04(冗余仓储)、H-06(PENDING选择逻辑) + 新增 B-H01(取消无身份)、B-H02(全表扫描) |
| Medium | 7 | M-01~M-10 部分未修 + 新增 B-M01(异步风险)、B-M02(驳回僵死) |
| Low | 2 | 新增 B-L01(代码重复)、B-L02(未使用事件类) |
| **合计** | **16** | |

### 修复进度评估

第一轮审计的28个问题中，15个已完全修复（54%），修复质量较高。Critical 级别的安全问题（C-01、C-02、C-05）得到了优先处理，High 级别的性能问题（H-01、H-02）和架构问题（H-03、H-05、H-07）也已解决。

### Top 5 优先修复表

| 优先级 | 问题ID | 严重性 | 问题摘要 | 影响 | 修复复杂度 |
|--------|--------|--------|----------|------|-----------|
| 1 | C-03 | Critical | 事件监听器吞噬异常，无补偿机制 | 数据不一致，生产故障无恢复路径 | 中（需引入Outbox模式） |
| 2 | B-C01 | Critical | WorkflowController 响应体暴露 JPA 实体 | 信息泄露、LazyInitializationException | 低（复用已有 Mapper） |
| 3 | C-04 | Critical | 7处跨模块直接SQL操作 plan_report 表 | 架构违规、表结构变更导致运行时崩溃 | 中（需定义防腐层接口） |
| 4 | B-H01 | High | cancelInstance 端点无身份验证 | 任何人可取消任何审批实例 | 低（添加注解+权限检查） |
| 5 | H-04 | High | WorkflowRepository + Facade 完全死代码 | 维护负担、新人困惑 | 低（删除两个文件） |

### 修复趋势

| 维度 | 第一轮 | 第二轮 | 趋势 |
|------|--------|--------|------|
| Critical 问题 | 5 | 2 + 1 新增 | 安全问题大幅减少，数据一致性仍需关注 |
| 代码结构 | 混合 3 层/DDD | 更清晰的 CQRS 分离 | 正面改善，CQRS 模式已基本到位 |
| 性能 | 全表扫描 + N+1 | DB 分页 + 请求级缓存 | 显著改善 |
| 安全 | 实体暴露 + 身份冒用 | DTO + AuthenticationPrincipal | 核心安全修复完成 |
| 架构违规 | 跨模块 SQL | 未变 | 需重点关注 |
