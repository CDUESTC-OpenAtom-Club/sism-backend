# sism-strategy 模块审计报告

**审计日期:** 2026-04-06
**审计人员:** Claude Code (Automated Audit)
**模块版本:** 当前主分支

---

## 模块概览

| 项目 | 数据 |
|------|------|
| 模块名称 | sism-strategy |
| 模块职责 | 战略规划、指标管理、里程碑管理、计划审批 |
| Java 文件总数 | 51 |
| 核心实体 | Plan, Indicator, Cycle, Milestone |
| Service 数量 | 8 |
| Controller 数量 | 4 |

### 包结构

```
com.sism.strategy/
├── application/                      # 应用服务层
│   ├── PlanApplicationService.java   # ⚠️ 1400+ 行
│   ├── StrategyApplicationService.java
│   ├── CycleApplicationService.java
│   ├── MilestoneApplicationService.java
│   └── ...
├── domain/                           # 领域模型
│   ├── Indicator.java               # 指标聚合根
│   ├── Cycle.java
│   ├── plan/
│   │   ├── Plan.java
│   │   ├── PlanLevel.java
│   │   └── PlanStatus.java
│   ├── enums/
│   ├── event/
│   └── repository/
├── infrastructure/
│   └── persistence/
└── interfaces/
    ├── dto/
    └── rest/
        ├── PlanController.java
        ├── IndicatorController.java   # ⚠️ 1100+ 行
        └── ...
```

---

## 一、安全漏洞

### 🔴 High: 多个 API 端点缺少权限控制

**文件:** `PlanController.java`
**行号:** 63-99

```java
@PostMapping
@Operation(summary = "创建新规划")
public ResponseEntity<ApiResponse<PlanResponse>> createPlan(
        @Valid @RequestBody CreatePlanRequest request) {  // ❌ 无 @PreAuthorize
    PlanResponse response = planApplicationService.createPlan(request);
    return ResponseEntity.ok(ApiResponse.success(response));
}

@DeleteMapping("/{id}")
@Operation(summary = "删除规划")
public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable Long id) {  // ❌ 无 @PreAuthorize
    planApplicationService.deletePlan(id);
    return ResponseEntity.ok(ApiResponse.success());
}

@PostMapping("/{id}/approve")
@Operation(summary = "审批通过规划")
public ResponseEntity<ApiResponse<PlanResponse>> approvePlan(@PathVariable Long id) {  // ❌ 无 @PreAuthorize
    PlanResponse response = planApplicationService.approvePlan(id);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

**问题描述:**
关键业务操作（创建、删除、审批）缺少权限控制，任何认证用户都可以执行。

**风险影响:**
- 未授权的计划创建/删除
- 审批流程被绕过
- 数据完整性风险

**严重等级:** 🔴 **High**

**建议修复:**
```java
@PostMapping
@PreAuthorize("hasAnyRole('ADMIN', 'STRATEGY_DEPT')")
@Operation(summary = "创建新规划")
public ResponseEntity<ApiResponse<PlanResponse>> createPlan(...) { }

@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<Void>> deletePlan(...) { }

@PostMapping("/{id}/approve")
@PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
public ResponseEntity<ApiResponse<PlanResponse>> approvePlan(...) { }
```

---

### 🔴 High: IndicatorController 同样缺少权限控制

**文件:** `IndicatorController.java`
**行号:** 197-279, 338-395

```java
@PostMapping
@Operation(summary = "创建新指标")
public ResponseEntity<ApiResponse<IndicatorResponse>> createIndicator(...) { }  // ❌ 无权限控制

@DeleteMapping("/{id}")
@Operation(summary = "删除指标")
public ResponseEntity<ApiResponse<Void>> deleteIndicator(@PathVariable Long id) { }  // ❌ 无权限控制

@PostMapping("/{id}/distribute")
@Operation(summary = "分发指标到目标组织")
public ResponseEntity<ApiResponse<IndicatorResponse>> distributeIndicator(...) { }  // ❌ 无权限控制
```

**严重等级:** 🔴 **High**

---

### 🟠 Medium: 硬编码的角色 ID 和组织 ID

**文件:** `PlanApplicationService.java`
**行号:** 56-61

```java
private static final String PLAN_APPROVAL_WORKFLOW_CODE_FUNCDEPT = "PLAN_APPROVAL_FUNCDEPT";
private static final String PLAN_APPROVAL_WORKFLOW_CODE_COLLEGE = "PLAN_APPROVAL_COLLEGE";
private static final Long ROLE_APPROVER = 2L;              // ❌ 硬编码
private static final Long ROLE_STRATEGY_DEPT_HEAD = 3L;    // ❌ 硬编码
private static final Long ROLE_VICE_PRESIDENT = 4L;        // ❌ 硬编码
private static final Long STRATEGY_ORG_ID = 35L;           // ❌ 硬编码
```

**问题描述:**
角色 ID 和组织 ID 硬编码在服务类中，如果数据库中的 ID 变化，系统将无法正常工作。

**风险影响:**
- 数据库 ID 变化导致功能失效
- 难以在不同环境间迁移
- 维护困难

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
@Value("${app.workflow.role-approver-id:2}")
private Long roleApproverId;

@Value("${app.workflow.role-strategy-dept-head-id:3}")
private Long roleStrategyDeptHeadId;

@Value("${app.org.strategy-org-id:35}")
private Long strategyOrgId;
```

---

### 🟠 Medium: 指标催办接口权限验证不充分

**文件:** `IndicatorController.java`
**行号:** 115-166

```java
@PostMapping("/{id}/reminders")
public ResponseEntity<ApiResponse<Map<String, Object>>> sendIndicatorReminder(
        @PathVariable Long id,
        @RequestBody(required = false) ReminderRequest request,
        @AuthenticationPrincipal CurrentUser currentUser) {
    // ...
    if (!Objects.equals(indicator.getOwnerOrg().getId(), currentUser.getOrgId())) {
        return ResponseEntity.status(403).body(ApiResponse.error(403, "当前用户无权催办该指标"));
    }
    // ❌ 仅检查组织 ID，未检查用户角色/权限
}
```

**问题描述:**
催办接口仅检查用户是否属于指标所属组织，未验证用户是否有权限发起催办。

**严重等级:** 🟠 **Medium**

---

## 二、潜在 Bug 和逻辑错误

### 🔴 High: PlanApplicationService.awaitWorkflowSnapshot 忙等待

**文件:** `PlanApplicationService.java`
**行号:** 971-999

```java
private PlanWorkflowSnapshotQueryService.WorkflowSnapshot awaitWorkflowSnapshot(Long planId, Duration timeout) {
    // ...
    do {
        latestSnapshot = planWorkflowSnapshotQueryService.getWorkflowSnapshotByPlanId(planId);
        if (isReadyForSubmitResponse(latestSnapshot)) {
            return latestSnapshot;
        }

        if (System.currentTimeMillis() >= deadline) {
            break;
        }

        try {
            Thread.sleep(200L);  // ❌ 忙等待，阻塞线程
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            break;
        }
    } while (true);

    return latestSnapshot;
}
```

**问题描述:**
使用忙等待模式轮询工作流状态，阻塞当前线程。这可能导致：
1. 线程池耗尽
2. 响应延迟
3. 资源浪费

**严重等级:** 🔴 **High**

**建议修复:**
```java
// 方案1: 使用异步处理
@Async
public CompletableFuture<PlanResponse> submitPlanForApprovalAsync(...) {
    // ...
}

// 方案2: 使用事件驱动
@EventListener
public void onWorkflowSnapshotReady(WorkflowSnapshotReadyEvent event) {
    // 处理就绪的工作流
}

// 方案3: 直接返回，前端轮询
public PlanResponse submitPlanForApproval(...) {
    // 提交后立即返回，让前端通过其他接口查询状态
}
```

---

### 🟠 Medium: Indicator 实体中 redundant 方法

**文件:** `Indicator.java`
**行号:** 158-172

```java
public String getName() {
    return this.indicatorDesc;  // ❌ getName() 和 getDescription() 返回相同值
}

public void setName(String name) {
    this.indicatorDesc = name;
}

public String getDescription() {
    return this.indicatorDesc;
}

public void setDescription(String description) {
    this.indicatorDesc = description;
}
```

**问题描述:**
`getName()`/`setName()` 和 `getDescription()`/`setDescription()` 操作同一个字段，可能导致语义混淆。

**严重等级:** 🟠 **Medium**

**建议修复:** 移除冗余方法，统一使用一个命名。

---

### 🟠 Medium: Indicator.create 工厂方法重复

**文件:** `Indicator.java`
**行号:** 99-156

```java
public static Indicator create(String description, SysOrg ownerOrg,
                                SysOrg targetOrg, String indicatorType) { ... }

public static Indicator create(String name, String description, BigDecimal weight,
                                SysOrg ownerOrg, SysOrg targetOrg,
                                String indicatorType) { ... }
```

**问题描述:**
两个重载的 `create` 方法参数不同但行为相似，可能导致混淆。第二个方法忽略了 `name` 参数。

**严重等级:** 🟠 **Medium**

---

### 🟡 Low: Plan 状态使用字符串而非枚举

**文件:** `Plan.java`
**行号:** 56-57

```java
@Column(name = "status", nullable = false)
private String status;  // ❌ 使用字符串而非枚举
```

**对比:** `Indicator.java`
```java
@Enumerated(EnumType.STRING)
@Column(name = "status", length = 20)
private IndicatorStatus status = IndicatorStatus.DRAFT;  // ✅ 正确使用枚举
```

**问题描述:**
Plan 状态使用字符串存储，而 Indicator 使用枚举。这种不一致可能导致：
1. 类型安全问题
2. 状态值拼写错误

**严重等级:** 🟡 **Low**

---

## 三、性能瓶颈

### 🔴 High: PlanApplicationService 超大类

**文件:** `PlanApplicationService.java`
**行数:** 1465 行

**问题描述:**
单个服务类超过 1400 行，包含：
- 10+ 个公开方法
- 30+ 个私有方法
- 多个内嵌 DTO 类
- 大量原生 SQL 查询

这违反了单一职责原则，难以维护和测试。

**严重等级:** 🔴 **High**

**建议重构:**
```
PlanApplicationService (协调者)
├── PlanQueryService (查询)
├── PlanCommandService (命令)
├── PlanWorkflowService (工作流)
├── PlanIndicatorSyncService (指标同步)
└── PlanRepository (数据访问)
```

---

### 🔴 High: Controller 直接执行大量数据库查询

**文件:** `IndicatorController.java`
**行号:** 638-822

```java
private Map<Long, TaskMetaSnapshot> buildTaskMetaMap(List<Indicator> indicators) {
    // ...
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT t.task_id AS task_id, ...
            FROM public.sys_task t
            LEFT JOIN public.plan p ON p.id = t.plan_id
            LEFT JOIN public.cycle c ON c.id = p.cycle_id
            WHERE t.task_id IN (%s)
            """.formatted(placeholders),
            taskIds.toArray()
    );
    // ...
}

private Map<Long, CurrentMonthIndicatorRoundState> buildCurrentMonthIndicatorRoundStateMap(...) {
    // 另一个复杂 SQL
}
```

**问题描述:**
Controller 层直接使用 JdbcTemplate 执行复杂 SQL 查询，违反分层架构原则。

**风险影响:**
- 分层混乱
- 难以测试
- SQL 分散在多处

**严重等级:** 🔴 **High**

**建议修复:** 将这些查询移至专门的 Repository 或 QueryService。

---

### 🟠 Medium: N+1 查询风险 - 指标查询

**文件:** `Indicator.java`
**行号:** 39-56

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "owner_org_id", nullable = false)
private SysOrg ownerOrg;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "target_org_id", nullable = false)
private SysOrg targetOrg;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_indicator_id")
private Indicator parentIndicator;

@OneToMany(mappedBy = "parentIndicator",
           cascade = {CascadeType.PERSIST, CascadeType.MERGE},
           fetch = FetchType.LAZY)
private List<Indicator> childIndicators = new ArrayList<>();
```

**问题描述:**
Indicator 实体有多个 LAZY 关联，在列表查询时可能产生 N+1 问题。

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
// 在 Repository 中使用 EntityGraph
@EntityGraph(attributePaths = {"ownerOrg", "targetOrg"})
List<Indicator> findByTaskId(Long taskId);
```

---

### 🟠 Medium: loadOrgNamesById 每次查询所有组织

**文件:** `PlanApplicationService.java`
**行号:** 1008-1011

```java
private Map<Long, String> loadOrgNamesById() {
    return organizationRepository.findAll().stream()  // ❌ 每次加载所有组织
            .collect(Collectors.toMap(SysOrg::getId, SysOrg::getOrgName, (existing, replacement) -> existing));
}
```

**问题描述:**
每次需要组织名称时都查询所有组织，如果组织数量大，会造成性能问题。

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
// 方案1: 按需查询
private Map<Long, String> loadOrgNamesById(Set<Long> orgIds) {
    return organizationRepository.findByIds(orgIds).stream()
            .collect(Collectors.toMap(SysOrg::getId, SysOrg::getOrgName));
}

// 方案2: 使用缓存
@Cacheable("orgNames")
public Map<Long, String> getAllOrgNames() { ... }
```

---

## 四、代码质量和可维护性

### 🔴 High: Controller 内嵌大量 DTO 类

**文件:** `IndicatorController.java`
**行号:** 936-1103

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public static class IndicatorResponse { ... }  // 30+ 字段

@Data
public static class CreateIndicatorRequest { ... }

@Data
public static class UpdateIndicatorRequest { ... }

@Data
public static class DistributeIndicatorRequest { ... }

// ... 更多内嵌 DTO
```

**问题描述:**
Controller 内定义了 10+ 个 DTO 类，违反关注点分离原则。这些类应该位于 `interfaces/dto` 包中。

**严重等级:** 🔴 **High**

**建议修复:** 将这些 DTO 移至 `com.sism.strategy.interfaces.dto` 包。

---

### 🟠 Medium: Service 方法过长

**文件:** `PlanApplicationService.java`

| 方法 | 行数 |
|------|------|
| `submitPlanForApprovalInTransaction` | 100+ |
| `reactivateWithdrawnWorkflowCurrentStep` | 120+ |
| `getPlanDetails` | 80+ |
| `getCurrentReportContext` | 140+ |

**问题描述:**
多个方法超过 80 行，违反单一职责原则。

**严重等级:** 🟠 **Medium**

**建议修复:** 将这些方法拆分为更小的私有方法或提取到专门的类。

---

### 🟠 Medium: 异常消息混合中英文

**文件:** `Indicator.java` 和 `PlanApplicationService.java`

```java
// Indicator.java
throw new IllegalArgumentException("指标描述不能为空");
throw new IllegalArgumentException("Indicator type cannot be empty");

// PlanApplicationService.java
throw new IllegalArgumentException("Cycle not found: " + request.getCycleId());
throw new ConflictException(String.format(
    "Plan already exists for cycleId=%s, planLevel=%s, ...", ...));
```

**问题描述:**
异常消息混合使用中文和英文，缺乏一致性。

**严重等级:** 🟠 **Medium**

**建议修复:** 统一使用一种语言，或使用国际化消息。

---

### 🟡 Low: 魔法数字 - 催办进度阈值

**文件:** `IndicatorController.java`
**行号:** 135-137

```java
Integer progress = indicator.getProgress() != null ? indicator.getProgress() : 0;
if (progress >= 50) {  // ❌ 魔法数字
    return ResponseEntity.badRequest().body(ApiResponse.error(400, "当前指标未滞后，无需催办"));
}
```

**问题描述:**
进度阈值 50% 硬编码，应提取为配置或常量。

**建议修复:**
```java
private static final int REMINDER_PROGRESS_THRESHOLD = 50;
// 或从配置读取
@Value("${app.indicator.reminder-threshold:50}")
private int reminderThreshold;
```

---

## 五、架构最佳实践

### 🔴 High: Service 层直接使用 JdbcTemplate

**文件:** `PlanApplicationService.java`

```java
public class PlanApplicationService {
    // ...
    private final JdbcTemplate jdbcTemplate;  // ❌ 绕过 Repository 层

    private void withdrawWorkflowCurrentStep(Long workflowInstanceId) {
        jdbcTemplate.update("""
            UPDATE public.audit_step_instance
            SET status = 'WITHDRAWN', ...
            """, ...);
    }
}
```

**问题描述:**
服务层直接使用 JdbcTemplate 执行原生 SQL，绕过了 Repository 层。这违反了 DDD 分层架构原则。

**严重等级:** 🔴 **High**

**建议修复:**
创建专门的 Repository 或 Domain Service 来处理这些操作：
```java
public interface WorkflowRepository {
    void withdrawStep(Long instanceId, Long stepId);
    void reactivateStep(Long instanceId, Long stepId);
}
```

---

### 🟠 Medium: 领域事件未被持久化后的聚合根清除

**文件:** `PlanApplicationService.java`
**行号:** 99-107

```java
@Transactional
public PlanResponse createPlan(CreatePlanRequest request) {
    // ...
    Plan plan = Plan.create(...);  // 添加了领域事件
    Plan saved = savePlanHandlingConflict(plan);
    // ❌ 未清除聚合根的事件
    return convertToResponse(saved, cycle.getYear().toString());
}
```

**问题描述:**
创建聚合根后添加了领域事件，但保存后未清除事件列表，可能导致事件被重复发布。

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
Plan saved = savePlanHandlingConflict(plan);
plan.clearEvents();  // 清除已发布的事件
return convertToResponse(saved, cycle.getYear().toString());
```

---

### 🟡 Low: Repository 接口定义不一致

**文件:** `PlanRepository.java` 和 `IndicatorRepository.java`

部分方法在接口中定义，部分在实现类中添加，导致接口定义不一致。

---

## 六、审计总结

### 问题统计

| 严重等级 | 数量 | 类别分布 |
|----------|------|----------|
| 🔴 High | 7 | 安全、性能、Bug、架构 |
| 🟠 Medium | 8 | 安全、Bug、性能、代码质量、架构 |
| 🟡 Low | 3 | 代码质量、Bug |

### 最紧急修复项

| 优先级 | 问题 | 影响 |
|--------|------|------|
| P0 | PlanApplicationService 1465 行超大类 | 严重违反 SRP，难以维护 |
| P0 | API 端点缺少权限控制 | 未授权操作风险 |
| P0 | Controller 直接执行 SQL | 架构混乱 |
| P1 | 忙等待阻塞线程 | 性能问题 |
| P1 | 硬编码的角色/组织 ID | 环境迁移困难 |

### 整体评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 安全性 | 🔴 需改进 | 多数关键操作无权限控制 |
| 可靠性 | 🟠 需改进 | 存在忙等待、N+1 等问题 |
| 性能 | 🟠 需改进 | 大量内存操作、缺少分页 |
| 可维护性 | 🔴 需改进 | 超大类、内嵌 DTO、代码重复 |
| 架构合规性 | 🔴 需改进 | 分层混乱、Service 直接使用 JdbcTemplate |

### 亮点

1. **DDD 领域模型设计良好**: Indicator 和 Plan 聚合根设计合理
2. **状态机实现**: 指标状态流转清晰
3. **事件驱动**: 使用领域事件进行解耦

### 关键建议

1. **拆分超大服务类**: 将 PlanApplicationService 拆分为多个专职服务
2. **添加权限控制**: 为所有关键操作添加 `@PreAuthorize`
3. **重构数据访问**: 将 SQL 查询移至 Repository 层
4. **统一异常消息**: 使用国际化或统一语言

---

**审计完成日期:** 2026-04-06
**下一步行动:** 优先拆分超大服务类，添加权限控制后再部署生产环境