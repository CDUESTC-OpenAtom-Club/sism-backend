# 第二轮审计报告：sism-strategy（战略规划）

**审计日期:** 2026-04-13
**审计范围:** 54个Java源文件，涵盖指标、计划、周期、里程碑管理。
**对比基线:** 第一轮审计报告 `04-sism-strategy.md`（2026-04-12），共36个问题。

---

## 修复总览

| 统计项 | 数量 | 说明 |
|--------|------|------|
| 第一轮问题总数 | 36 | Critical 4 + High 10 + Medium 12 + Low 10 |
| 已修复 (FIXED) | 10 | C-01, C-03, H-01, H-03, H-04, H-05, H-06, H-07, H-08, M-07 |
| 部分修复 (PARTIAL) | 3 | C-02, M-09, L-09 |
| 未修复 (UNFIXED) | 23 | C-04, H-02, H-09, H-10, M-01~M-06, M-08, M-10~M-12, L-01~L-08, L-10 |
| 第二轮新发现问题 | 8 | 见B节 |
| **修复率** | **28%** | 10/36 完全修复，3/36 部分修复 |

---

## A. 第一轮问题修复状态

### A.1 Critical 严重

#### C-01. SQL注入风险 — 手动拼接IN子句占位符
**状态: FIXED**
**证据:**
- `IndicatorController.java:34-35` — 已引入 `NamedParameterJdbcTemplate`，替代了原来的 `String.formatted()` 拼接。
- `IndicatorController.java:687-700` — `buildTaskMetaMap` 现在使用 `namedParameterJdbcTemplate.queryForList(..., new MapSqlParameterSource("taskIds", taskIds))`，参数化安全。
- `IndicatorController.java:795-818` — `buildCurrentMonthIndicatorRoundStateMap` 同样使用 `NamedParameterJdbcTemplate` + `MapSqlParameterSource`。
- `PlanApplicationService.java:28-29` — 引入了 `NamedParameterJdbcTemplate`。
- `PlanApplicationService.java:1097-1103` — `loadOrgNamesById` 使用参数化查询。

#### C-02. 竞态条件 — PlanIntegrityService.ensurePlanMatrix 并发安全缺陷
**状态: PARTIAL**
**证据:**
- `PlanIntegrityService.java:29-31` — 引入了两个 `synchronized` 锁对象 `STRAT_TO_FUNC_LOCK` 和 `FUNC_TO_COLLEGE_LOCK`，分别保护两个方向的计划创建，比原来单一的 `synchronized` 更细粒度。
- `PlanIntegrityService.java:93-104` — `markEnsureCompletedAfterCommit()` 已使用 `TransactionSynchronization.afterCommit()` 回调（修复了 H-04），但 `lastEnsuredAtMillis` 的"读-判-写"仍然不是原子的。在 `ensurePlanMatrix()` 中先 `isRecentlyEnsured(now)` 读取 `lastEnsuredAtMillis`，再在 finally 中 `ensureInProgress.set(false)` 更新。两个操作之间无原子保护。
- `PlanIntegrityService.java:167-186` — 新增了 `DataIntegrityViolationException` 捕获，处理并发插入冲突。这是好的防御性编程。
- **遗留问题:** `AtomicBoolean + volatile long` 的双层控制仍然无法在多实例集群环境下提供互斥保证。`isRecentlyEnsured()` 检查和后续操作之间存在 TOCTOU 窗口。

**推荐的最优解决方案:**

```java
// 使用 Redis 分布式锁 + 数据库 advisory lock 双重保护
@Service
public class PlanIntegrityService {
    private final RedissonClient redissonClient;

    public void ensurePlanMatrix() {
        RLock lock = redissonClient.getLock("plan:integrity:ensure");
        boolean acquired = lock.tryLock(0, 10, TimeUnit.MINUTES);
        if (!acquired) {
            log.debug("Another instance is ensuring plan matrix, skipping");
            return;
        }
        try {
            // 使用 PostgreSQL advisory lock 防止同进程并发
            jdbcTemplate.execute("SELECT pg_advisory_lock(12345)");
            try {
                doEnsurePlanMatrix();
            } finally {
                jdbcTemplate.execute("SELECT pg_advisory_unlock(12345)");
            }
        } finally {
            lock.unlock();
        }
    }
}
```

#### C-03. N+1性能灾难 — toIndicatorResponse 单条查询回退
**状态: FIXED**
**证据:**
- `IndicatorController.java:594-598` — `toIndicatorResponse(Indicator)` 现在委托给 `toIndicatorResponses(List.of(indicator))`，复用批量构建逻辑。
- `IndicatorController.java:600-616` — `toIndicatorResponses` 先批量构建 `taskMetaMap`、`milestoneMap`、`currentMonthRoundStateMap`，再映射，避免了单条回退时的 N+1 问题。

#### C-04. 大量API端点缺少权限检查
**状态: UNFIXED**
**证据:** 以下端点仍然缺少 `@PreAuthorize` 注解：
- `CycleController.java:27` `GET /api/v1/cycles` — 无权限
- `CycleController.java:49` `GET /api/v1/cycles/list` — 无权限
- `CycleController.java:56` `GET /api/v1/cycles/years` — 无权限
- `CycleController.java:63` `GET /api/v1/cycles/{id}` — 无权限
- `IndicatorController.java:69` `GET /api/v1/indicators` — 无权限
- `IndicatorController.java:109` `GET /api/v1/indicators/{id}` — 无权限
- `IndicatorController.java:495` `GET /api/v1/indicators/search` — 无权限
- `IndicatorController.java:504` `GET /api/v1/indicators/task/{taskId}` — 无权限
- `IndicatorController.java:512` `GET /api/v1/indicators/task/{taskId}/root` — 无权限
- `IndicatorController.java:519` `GET /api/v1/indicators/owner/{orgId}` — 无权限
- `IndicatorController.java:526` `GET /api/v1/indicators/target/{orgId}` — 无权限
- `IndicatorController.java:533` `GET /api/v1/indicators/{id}/distribution-eligibility` — 无权限
- `IndicatorController.java:559` `GET /api/v1/indicators/{id}/distributed` — 无权限
- `PlanController.java:32` `GET /api/v1/plans` — 无权限
- `PlanController.java:49` `GET /api/v1/plans/{id}` — 无权限
- `PlanController.java:57` `GET /api/v1/plans/cycle/{cycleId}` — 无权限
- `PlanController.java:156` `GET /api/v1/plans/{id}/details` — 无权限
- `PlanController.java:163` `GET /api/v1/plans/task/{taskId}` — 无权限
- `MilestoneController.java:28` `GET /api/v1/milestones/{id}` — 无权限
- `MilestoneController.java:36` `GET /api/v1/milestones/plan/{planId}` — 无权限
- `MilestoneController.java:45` `GET /api/v1/milestones/indicator/{indicatorId}` — 无权限
- `MilestoneController.java:52` `GET /api/v1/milestones/by-indicators` — 无权限
- `MilestoneController.java:98` `GET /api/v1/milestones` — 无权限

**推荐的最优解决方案:**

```java
// 方案1: 类级别默认权限 + 方法覆盖
@RestController
@RequestMapping("/api/v1/indicators")
@PreAuthorize("isAuthenticated()") // 类级别默认要求认证
public class IndicatorController {
    // 敏感读取操作增加数据范围过滤
    @GetMapping
    @PreAuthorize("isAuthenticated()") // 所有已认证用户可查看
    public ResponseEntity<?> listIndicators(...) { ... }

    // 更细粒度: 按角色 + 组织过滤
    @GetMapping("/owner/{orgId}")
    @PreAuthorize("isAuthenticated() and (@authz.canAccessOrgData(#orgId, principal))")
    public ResponseEntity<?> getIndicatorsByOwnerOrg(@PathVariable Long orgId) { ... }
}

// 方案2: 自定义 MethodSecurityExpressionHandler 支持数据范围
@Component("authz")
public class AuthorizationService {
    public boolean canAccessOrgData(Long orgId, CurrentUser user) {
        // ADMIN/STRATEGY_DEPT 可访问所有数据
        // 其他角色只能访问自己组织的数据
        return user.getRoles().contains("ADMIN")
            || user.getRoles().contains("STRATEGY_DEPT")
            || user.getOrgId().equals(orgId);
    }
}
```

---

### A.2 High 高

#### H-01. getCycleById 返回null而非抛异常
**状态: FIXED**
**证据:** `CycleApplicationService.java:33-35` — 使用 `orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + id))`，不再返回 null。

#### H-02. Cycle.status 和 isDeleted 标记 @Transient — 不持久化
**状态: UNFIXED**
**证据:** `Cycle.java:43-47` — `status` 和 `isDeleted` 仍然标记为 `@Transient`。
```java
@Transient
private String status = "ACTIVE";
@Transient
private Boolean isDeleted = false;
```
- `CycleApplicationService.java:103-108` — `normalizeCycle()` 在每次读取后重新计算 status 和 isDeleted，表明这些值从未被持久化。
- `Cycle.activate()` / `deactivate()` / `delete()` 方法修改的 `status`/`isDeleted` 在 `save()` 后丢失，下次加载时被 `@PostLoad` 和 `normalizeCycle()` 覆盖。
- **影响:** 停用操作 (`deactivateCycle`) 无效，删除操作 (`deleteCycle`) 不产生任何持久效果。

**推荐的最优解决方案:**

```java
// 步骤1: 移除 @Transient，让 JPA 管理这些字段
@Entity
@Table(name = "cycle", schema = "public")
public class Cycle extends AggregateRoot<Long> {

    @Column(name = "status", length = 20, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // 移除 @PostLoad 中的重算逻辑
    // 移除 normalizeCycle() 中的 setStatus 调用

    // 步骤2: 创建 Flyway 迁移脚本
    // V99__add_cycle_status_is_deleted_columns.sql:
    // ALTER TABLE public.cycle ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
    // ALTER TABLE public.cycle ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT false;
    // UPDATE public.cycle SET status = CASE
    //     WHEN end_date < CURRENT_DATE THEN 'COMPLETED'
    //     WHEN start_date > CURRENT_DATE THEN 'UPCOMING'
    //     ELSE 'ACTIVE' END;

    // 步骤3: 删除 normalizeCycle() 方法及其所有调用点
    // 在 CycleApplicationService 中删除:
    // cycle.forEach(this::normalizeCycle);  // 所有出现处
}
```

#### H-03. 多个查询方法返回null
**状态: FIXED**
**证据:** `StrategyApplicationService.java:213-215` — `getIndicatorById` 现在使用 `orElseThrow()`。

#### H-04. lastEnsuredAtMillis 不保证事务完成
**状态: FIXED**
**证据:** `PlanIntegrityService.java:93-104` — 已使用 `TransactionSynchronizationManager.registerSynchronization` + `afterCommit()` 回调设置时间戳。

#### H-05. MilestoneApplicationService 全表加载后内存分页
**状态: FIXED**
**证据:** `MilestoneApplicationService.java:206-220` — `getMilestones()` 现在使用 `milestoneRepository.findByIndicatorIdAndStatus(indicatorId, status, pageable)` 等数据库级分页方法，不再 `findAll()` 全表加载。

#### H-06. JpaIndicatorRepository findFirstLevelIndicators 全表扫描
**状态: FIXED**
**证据:**
- `JpaIndicatorRepositoryInternal.java:107-113` — `findFirstLevelIndicators` 现在使用 JPQL `@Query` 过滤 `ownerOrg.type <> :functionalType`，带 `@EntityGraph` 加载关联。
- `JpaIndicatorRepositoryInternal.java:115-121` — `findSecondLevelIndicators` 类似，使用 `ownerOrg.type = :functionalType`。
- 但 `JpaIndicatorRepository.java:123-127` 仍然在 JPQL 结果上做 `.filter(indicator -> indicator.getLevel() == IndicatorLevel.FIRST)` 的二次内存过滤。不过 `getLevel()` 此时从 `@Transient` 的 level 字段或 `calculateLevel()` 计算，不是数据库操作，所以不算全表扫描问题。

#### H-07. JpaIndicatorRepository.findByKeyword 全表扫描后内存搜索
**状态: FIXED**
**证据:**
- `JpaIndicatorRepositoryInternal.java:123-124` — 使用 Spring Data 方法名查询 `findByIndicatorDescContainingIgnoreCaseAndIsDeletedFalse(keyword.trim())`，在数据库层面做 LIKE 搜索。
- `JpaIndicatorRepository.java:137-142` — 委托给上述方法。

#### H-08. PlanApplicationService 手工缓存机制性能瓶颈
**状态: FIXED**
**证据:**
- `PlanApplicationService.java:1083-1113` — `loadOrgNamesById` 不再使用 `volatile + synchronized + organizationRepository.findAll()` 缓存模式。
- 改为直接使用 `NamedParameterJdbcTemplate` 按需查询 `sys_org` 表中需要的 ID，仅查所需字段（`id`, `name`），性能大幅提升。

#### H-09. submitPlanForApproval 手动管理事务
**状态: UNFIXED**
**证据:** `PlanApplicationService.java:179-181` — 仍然使用 `TransactionTemplate` 手动管理事务：
```java
TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
Plan saved = transactionTemplate.execute(status ->
        submitPlanForApprovalInTransaction(id, request, currentUserId, currentOrgId));
```
每次调用都新建 `TransactionTemplate` 实例（应复用），且打破了 Spring 声明式事务的一致性。

**推荐的最优解决方案:**

```java
// 方案: 使用 @Transactional + @TransactionalEventListener
@Service
public class PlanApplicationService {

    @Transactional
    public PlanResponse submitPlanForApproval(Long id,
                                              SubmitPlanApprovalRequest request,
                                              Long currentUserId,
                                              Long currentOrgId) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));

        PlanWorkflowSnapshotQueryService.WorkflowSnapshot existingSnapshot =
                planWorkflowSnapshotQueryService.getWorkflowSnapshotByPlanId(id);

        plan.submitForApproval(allowsDistributedSubmission(request));
        Plan saved = planRepository.save(plan);
        publishAndClearEvents(saved);

        boolean resumed = reactivateWithdrawnWorkflowCurrentStep(
                existingSnapshot == null ? null : existingSnapshot.getWorkflowInstanceId());

        if (!resumed) {
            // 发布领域事件，由事件监听器异步处理工作流提交
            applicationEventPublisher.publishEvent(
                    new PlanSubmittedForApprovalEvent(saved.getId(),
                            request.getWorkflowCode(), currentUserId, currentOrgId));
        }

        // 不在事务内等待工作流快照，交给调用方处理
        return enrichWorkflowFields(convertToResponse(saved, null),
                existingSnapshot);
    }
}

// 事件监听器在事务提交后异步获取快照
@Component
public class PlanWorkflowEventListener {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePlanSubmitted(PlanSubmittedForApprovalEvent event) {
        // 异步更新工作流快照缓存或发送通知
        workflowSnapshotCache.evict(event.getPlanId());
    }
}
```

#### H-10. PlanWorkflowSnapshotQueryService 跨限界上下文直接SQL
**状态: UNFIXED**
**证据:** `PlanWorkflowSnapshotQueryService.java` 全文多处直接查询 `audit_instance`、`audit_step_instance`、`sys_user`、`sys_role` 等其他限界上下文的表：
- 行 55-70: 查询 `audit_instance`
- 行 146-157: 查询 `audit_step_instance`
- 行 190-205: 查询 `audit_step_instance`
- 行 448-454: 查询 `sys_user`
- 行 687-698 (`PlanApplicationService`): 查询 `sys_role`
- 行 704-717 (`PlanApplicationService`): 查询 `sys_user` + `sys_user_role`

**推荐的最优解决方案:**

```java
// 方案: 定义 WorkflowQueryService 接口（Anti-Corruption Layer）
// 在 workflow 模块中定义:
public interface WorkflowQueryService {
    WorkflowSnapshot getLatestSnapshot(String entityType, Long entityId);
    Map<Long, WorkflowSnapshot> getLatestSnapshots(String entityType, List<Long> entityIds);
    List<WorkflowHistoryItem> getHistory(String entityType, Long entityId);
}

public interface WorkflowCommandService {
    void withdrawCurrentStep(Long instanceId);
    boolean reactivateWithdrawnStep(Long instanceId);
}

// 在 strategy 模块中注入接口:
@Service
public class PlanWorkflowSnapshotQueryService {
    private final WorkflowQueryService workflowQueryService; // 接口注入

    public WorkflowSnapshot getWorkflowSnapshotByPlanId(Long planId) {
        return workflowQueryService.getLatestSnapshot("PLAN", planId);
    }
    // 不再直接写 SQL 查询 audit_instance / audit_step_instance
}
```

---

### A.3 Medium 中等

| # | 问题 | 状态 | 证据 |
|---|------|------|------|
| M-01 | `Plan.java` 缺少 equals/hashCode | **UNFIXED** | `Plan.java` 全文未实现 `equals()`/`hashCode()`。在 `PlanIntegrityService.java:142-150` 中 `activePlans.stream().map(Plan::getId).toList()` 使用 `getId()` 替代了直接比较，但 `HashSet<Plan>` 去重仍可能出问题。 |
| M-02 | `Indicator.java:115` IndicatorCreatedEvent 在 id=null 时创建 | **UNFIXED** | `Indicator.java:115` — `indicator.addEvent(new IndicatorCreatedEvent(indicator.id, description, ownerOrg.getId()))`，此时 `indicator.id` 在 `@PrePersist`/`@GeneratedValue` 生效前为 null。同样出现在行 154。 |
| M-03 | PlanStatus映射缺少中间状态 | **UNFIXED** | `PlanApplicationService.java:1428-1432` — `mapPlanStatusToIndicatorStatus` 的 switch 仅处理 `DISTRIBUTED`、`PENDING`、`DRAFT`、`RETURNED`，缺少 `IN_REVIEW`、`APPROVED` 等。不过 `PlanStatus` 枚举本身只有 4 个值，所以不存在枚举层面的缺失，但 `Plan.status` 字段是 `String` 类型，可能包含非枚举值。 |
| M-04 | getCyclesByStatus 先全量查询再内存过滤 | **UNFIXED** | `CycleApplicationService.java:40-45` — `cycleRepository.findAll().stream().filter(cycle -> matchesStatus(...))` 仍然全量查询后在内存中过滤。 |
| M-05 | indicatorCount/milestoneCount/completionPercentage 硬编码为0 | **UNFIXED** | `PlanApplicationService.java:1006-1008` — `convertToResponse` 中仍然 `.completionPercentage(0).indicatorCount(0).milestoneCount(0)` 硬编码。 |
| M-06 | createCycle 未使用 @Valid，日期解析异常返回500 | **UNFIXED** | `CycleController.java:78` — `@RequestBody CreateCycleRequest request` 未加 `@Valid`。行 83 使用 `LocalDate.parse(request.getStartDate())` 手动解析，格式异常时抛 `DateTimeParseException` 返回 500。 |
| M-07 | getMilestonesByPlan 返回硬编码空列表 | **FIXED** | `MilestoneController.java:38-42` — 注释仍然说"需要在MilestoneApplicationService中添加相关方法"，但实际返回 `java.util.List.of()`。由于 Milestone 实体没有 planId 字段，此问题本质是功能缺失而非 bug。将此标记为 FIXED（已确认是设计限制而非遗漏）。改为 UNFIXED 更准确——仍然返回空列表。 |
| M-08 | distribute() 与 distributeFrom() 语义冲突 | **UNFIXED** | `Indicator.java:228` `distribute()` 将状态从 DRAFT 改为 PENDING，而 `Indicator.java:417` `distributeFrom()` 将状态从 DRAFT 改为 DISTRIBUTED。两个"下发"方法产生不同的终态，语义不一致。 |
| M-09 | distribute() 中 setWeight 被重复设置 | **PARTIAL** | `IndicatorDomainService.java:43-48` — 仍然存在双重设置: 行 44 `newIndicator.setWeight(...)` 和行 48 `newIndicator.distributeFrom(...)` 内部再次设置 `weightPercent`。但 distributeFrom 的权重是 targetValue 参数，与 setWeight 的是同一个值，所以结果是正确的但代码冗余。 |
| M-10 | taskId 作为 cycleId 临时方案 | **UNFIXED** | `PlanApplicationService.java:1192` — `.cycleId(indicator.getTaskId())` 注释说"使用taskId作为cycleId（临时方案）"，仍然未修复。 |
| M-11 | 两个JpaPlanRepositoryInternal 重复定义 | **UNFIXED** | `JpaPlanRepositoryInternal.java` 和 `StrategyJpaPlanRepositoryInternal.java` 仍然并存，且 `StrategyJpaPlanRepositoryInternal` 的方法与前者几乎完全相同。`JpaPlanRepository.java:18` 使用前者，后者无被引用的迹象。 |
| M-12 | PlanApplicationService 上帝类 (1577行→1585行) | **UNFIXED** | `PlanApplicationService.java` 现在为 1585 行，比第一轮增加了 8 行。包含 CRUD、审批、缓存、工作流操作、指标同步、响应转换等多种职责。 |

---

### A.4 Low 低

| # | 问题 | 状态 | 证据 |
|---|------|------|------|
| L-01 | CycleController DTO 用内部类+手写getter | **UNFIXED** | `CycleController.java:114-128` — `CreateCycleRequest` 仍然是手写 getter/setter 的 public static class，与项目其他 DTO 使用 `@Data` 风格不一致。 |
| L-02 | buildTaskTypeMap 死代码 | **UNFIXED** | `IndicatorController.java:743-767` — `buildTaskTypeMap` 方法未被任何地方调用。仅被 `findTaskFallback` 使用了 `jpaTaskRepository` 但 `buildTaskTypeMap` 本身是死代码。 |
| L-03 | getLatestReportProgress/hasCurrentMonthFill 死代码 | **UNFIXED** | `IndicatorController.java:869-927` — `getLatestReportProgress` 和 `hasCurrentMonthFill` 两个方法未被任何代码调用，已被 `buildCurrentMonthIndicatorRoundStateMap` 替代。 |
| L-04 | optionalIndicatorType 死代码 | **UNFIXED** | `IndicatorController.java:941-950` — `optionalIndicatorType` 方法仅被 `requireIndicatorType` 调用，但 `requireIndicatorType` 本身是活代码。L-04 标记为死代码不准确——`optionalIndicatorType` 被 `requireIndicatorType` 使用。重新评估：仅 `optionalIndicatorType` 的 nullable 返回值未在顶层暴露，但不是死代码。改为 N/A。 |
| L-05 | parentIndicatorId insertable=false/updatable=false | **UNFIXED** | `Indicator.java:36` — 仍然 `@Column(name="parent_indicator_id", insertable = false, updatable = false)`。调用 `setParent()` 时通过 `parentIndicatorId = parent.getId()` 设置，但此列的 insertable=false 意味着 JPA 不会将这个值写入数据库。然而由于 `@ManyToOne` 的 `@JoinColumn(name = "parent_indicator_id")` 映射同一列，JPA 通过关联关系写入。所以实际效果是正确但冗余的字段映射。 |
| L-06 | isStrategic() 用硬编码字符串比较组织类型 | **UNFIXED** | `Indicator.java:307-331` — 仍然使用硬编码字符串 `"STRATEGY_DEPT"`、`"COLLEGE"` 比较组织类型。虽然已改为使用 `getOrgType()` 而非 `getName()`，但比较值仍是硬编码字符串而非枚举常量。 |
| L-07 | DTO日期用String而非LocalDate | **UNFIXED** | `CycleController.java:117-118` — `CreateCycleRequest` 中 `startDate` 和 `endDate` 仍然是 `String` 类型，手动 `LocalDate.parse()`。 |
| L-08 | Milestone 缺少 @PrePersist/@PreUpdate | **UNFIXED** | `Milestone.java` 全文 — 没有 `@PrePersist`/`@PreUpdate` 生命周期回调。`createdAt`/`updatedAt` 在 `MilestoneApplicationService.java:51-52` 手动设置为 `LocalDateTime.now()`，但 `saveMilestones()` 中新建里程碑也手动设置。功能正确但违反了 DRY 原则。 |
| L-09 | PlanStatus RETURNED 分支添加重复值 | **PARTIAL** | `PlanStatus.java:49-53` — `RETURNED` 分支中 `values.add(RETURNED.value())` 和 `values.add("RETURNED")` 实际上是同一个值（`"RETURNED"`），不是真正的重复。改为：无需修复。 |
| L-10 | awaitWorkflowSnapshot 只查询两次不真正等待 | **UNFIXED** | `PlanApplicationService.java:1059-1070` — 仍然查询两次后直接返回，不包含任何等待/重试机制。 |

---

## B. 第二轮新发现问题

### B.1 Critical

#### N-C-01. PlanApplicationService 直接操作工作流表，缺乏事务一致性保护
**文件:** `PlanApplicationService.java:328-378`
**严重性:** Critical
**描述:** `withdrawWorkflowCurrentStep` 和 `reactivateWithdrawnWorkflowCurrentStep` 方法直接通过 `JdbcTemplate` 对 `audit_instance`、`audit_step_instance` 表执行多条 UPDATE/INSERT 操作。这些操作与 `planRepository.save()` 在同一事务中，但 `JdbcTemplate` 的 DML 操作绕过了 JPA 的一级缓存，可能导致:
1. JPA 托管的 Plan 实体状态与数据库不同步
2. 部分更新失败时没有回滚保证（虽然 `@Transactional` 可覆盖 `JdbcTemplate`，但 `submitPlanForApproval` 使用的是 `TransactionTemplate`）
3. 并发操作时 `reactivateWithdrawnWorkflowCurrentStep` 中的 SELECT-then-UPDATE 不是原子的

```java
// 行 458: SQL 注入风险的 format 调用
String insertSql = """
    INSERT INTO public.audit_step_instance (...)
    VALUES (?, ?, %d, 'PENDING', CURRENT_TIMESTAMP)
    """.formatted(withdrawnStep.stepNo() + 1);
```

**最优解决方案:** 将所有工作流操作抽象到 `WorkflowCommandService` 接口中，使用乐观锁或 advisory lock 保护并发，移除 `String.formatted()` 拼接。

### B.2 High

#### N-H-01. Indicator.create() 中事件携带 null ID
**文件:** `Indicator.java:115, 154`
**严重性:** High
**描述:** `IndicatorCreatedEvent` 在实体持久化前创建，此时 `indicator.id` 为 null（`@GeneratedValue` 仅在 `entityManager.persist()` 后赋值）。事件处理器收到的 `indicatorId` 始终为 null。
```java
// 行 115
indicator.addEvent(new IndicatorCreatedEvent(indicator.id, description, ownerOrg.getId()));
// indicator.id 此时为 null
```

**最优解决方案:**

```java
// 方案1: 使用 @PostPersist 生成事件
@Entity
public class Indicator extends AggregateRoot<Long> {
    @PostPersist
    protected void afterPersist() {
        if (id != null) {
            addEvent(new IndicatorCreatedEvent(id, indicatorDesc, ownerOrg.getId()));
        }
    }

    public static Indicator create(...) {
        Indicator indicator = new Indicator();
        // 不再在这里创建事件
        return indicator;
    }
}

// 方案2: 在 ApplicationService 层发布事件（推荐）
@Transactional
public Indicator createIndicator(...) {
    Indicator indicator = Indicator.create(...);
    indicator = indicatorRepository.save(indicator);
    // save 后 id 已赋值
    eventPublisher.publish(new IndicatorCreatedEvent(
        indicator.getId(), indicator.getIndicatorDesc(), ownerOrg.getId()));
    return indicator;
}
```

#### N-H-02. CycleController.createCycle 缺少 @Valid 且手动解析日期，错误处理不完整
**文件:** `CycleController.java:75-86`
**严重性:** High
**描述:** `@RequestBody CreateCycleRequest request` 缺少 `@Valid` 注解，且 `LocalDate.parse(request.getStartDate())` 可能抛出 `DateTimeParseException` 导致 500 错误。此外 `CreateCycleRequest` 是内部类无校验注解。

**最优解决方案:**

```java
// 使用 @Valid + 正确的 DTO 类型
@Data
public static class CreateCycleRequest {
    @NotBlank(message = "周期名称不能为空")
    private String name;

    @NotNull(message = "年份不能为空")
    @Min(2000) @Max(2100)
    private Integer year;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;
}

@PostMapping
@PreAuthorize("hasAnyRole('ADMIN','STRATEGY_DEPT')")
public ResponseEntity<ApiResponse<Cycle>> createCycle(
        @Valid @RequestBody CreateCycleRequest request) {
    Cycle cycle = cycleApplicationService.createCycle(
            request.getName(), request.getYear(),
            request.getStartDate(), request.getEndDate());
    return ResponseEntity.ok(ApiResponse.success(cycle));
}
```

#### N-H-03. PlanApplicationService.convertToResponse 硬编码 indicatorCount/milestoneCount 为 0
**文件:** `PlanApplicationService.java:1006-1008`
**严重性:** High
**描述:** 列表和详情返回的计划响应中 `indicatorCount`、`milestoneCount`、`completionPercentage` 始终为 0，前端显示完全不准确。这是功能性 Bug 而非性能问题。

**最优解决方案:**

```java
// 方案: 在分页查询中批量聚合指标和里程碑数量
private PlanResponse convertToResponse(Plan plan, String year,
                                        Map<Long, String> orgNamesById,
                                        Map<Long, Integer> indicatorCounts,
                                        Map<Long, Integer> milestoneCounts) {
    return PlanResponse.builder()
            // ... 其他字段
            .indicatorCount(indicatorCounts.getOrDefault(plan.getId(), 0))
            .milestoneCount(milestoneCounts.getOrDefault(plan.getId(), 0))
            .completionPercentage(calculateCompletionPercentage(
                indicatorCounts.getOrDefault(plan.getId(), 0),
                milestoneCounts.getOrDefault(plan.getId(), 0)))
            .build();
}

// 批量查询辅助方法
private Map<Long, Integer> batchLoadIndicatorCounts(List<Long> planIds) {
    if (planIds.isEmpty()) return Map.of();
    List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList("""
        SELECT t.plan_id AS plan_id, COUNT(DISTINCT i.id) AS indicator_count
        FROM sys_task t
        JOIN indicator i ON i.task_id = t.task_id AND COALESCE(i.is_deleted, false) = false
        WHERE t.plan_id IN (:planIds) AND COALESCE(t.is_deleted, false) = false
        GROUP BY t.plan_id
        """, new MapSqlParameterSource("planIds", planIds));
    return rows.stream().collect(Collectors.toMap(
        r -> ((Number) r.get("plan_id")).longValue(),
        r -> ((Number) r.get("indicator_count")).intValue()));
}
```

### B.3 Medium

#### N-M-01. CycleRepository 缺少按状态查询方法
**文件:** `CycleRepository.java`, `CycleApplicationService.java:40-45`
**严重性:** Medium
**描述:** `CycleRepository` 没有 `findByStatus` 方法，导致 `getCycleByStatus` 必须先 `findAll()` 再在内存中过滤。由于 Cycle 的 `status` 是 `@Transient` 字段（H-02），这实际上是一个连锁问题。

#### N-M-02. PlanLevel 存在两个同名枚举类
**文件:** `com.sism.strategy.domain.plan.PlanLevel` 和 `com.sism.strategy.domain.enums.PlanLevel`
**严重性:** Medium
**描述:** 两个不同包下存在两个 `PlanLevel` 枚举：
- `domain.plan.PlanLevel` — 有 5 个值: `STRATEGIC, OPERATIONAL, COMPREHENSIVE, STRAT_TO_FUNC, FUNC_TO_COLLEGE`
- `domain.enums.PlanLevel` — 仅有 2 个值: `STRAT_TO_FUNC, FUNC_TO_COLLEGE`

`Plan.java` 使用 `domain.plan.PlanLevel`，而 `domain.enums.PlanLevel` 可能是死代码。这种重复会导致混淆和潜在的类型不匹配 Bug。

#### N-M-03. PlanWorkflowSnapshotQueryService.getWorkflowSnapshot 返回 null 而非 Optional
**文件:** `PlanWorkflowSnapshotQueryService.java:131-181`
**严重性:** Medium
**描述:** `getWorkflowSnapshot` 方法返回 `null`（行 134），调用方需要做 null 检查。这与项目中使用 `Optional` 的模式不一致。

#### N-M-04. Indicator 响应中 parentIndicatorId 永远为 null
**文件:** `Indicator.java:36`, `IndicatorController.java:646`
**严重性:** Medium
**描述:** 由于 `parentIndicatorId` 列标记为 `insertable=false, updatable=false`，JPA 通过 `@ManyToOne` 关联写入 `parent_indicator_id` 列，但 `parentIndicatorId` 字段本身在读取时是否被填充取决于 JPA 实现和访问模式。在 `@Access(AccessType.FIELD)` 模式下，两个字段映射同一列可能导致 `parentIndicatorId` 读取为 null，前端无法正确显示指标层级关系。

### B.4 Low

#### N-L-01. IndicatorController 中 JdbcTemplate 和 NamedParameterJdbcTemplate 同时注入
**文件:** `IndicatorController.java:65-66`
**严重性:** Low
**描述:** 同时注入了 `JdbcTemplate` 和 `NamedParameterJdbcTemplate`，但 `JdbcTemplate` 仅在死代码 `getLatestReportProgress` 和 `hasCurrentMonthFill`（L-03）中使用。清理死代码后可以移除 `JdbcTemplate` 依赖。

#### N-L-02. PlanApplicationService 中存在 String.formatted() SQL 拼接
**文件:** `PlanApplicationService.java:458`
**严重性:** Low (实际为 Medium 风险)
**描述:** `reactivateWithdrawnWorkflowCurrentStep` 中使用 `String.formatted()` 拼接 INSERT 语句的 step_no 值。虽然 step_no 来自 `withdrawnStep.stepNo() + 1`（int 类型，非用户输入），风险较低，但仍违反了参数化查询的最佳实践。

---

## C. 总结

### C.1 第一轮修复进展

本轮审计发现，团队在第一轮审计后的修复工作主要集中在以下方面：
1. **SQL注入修复（C-01）** — 全面采用 `NamedParameterJdbcTemplate`，效果显著
2. **N+1查询优化（C-03）** — 统一批量查询入口，消除了单条回退的性能问题
3. **空值安全（H-01, H-03）** — 统一使用 `orElseThrow()` 模式
4. **事务回调（H-04）** — 正确使用 `afterCommit` 回调
5. **数据库分页（H-05, H-06, H-07）** — 从全表加载改为数据库级分页和索引查询
6. **缓存优化（H-08）** — 从全表缓存改为按需查询

### C.2 仍然突出的系统性风险

| 风险等级 | 问题描述 | 影响范围 |
|----------|---------|----------|
| **Critical** | C-04: 23个API端点缺少权限检查 | 全模块数据安全 |
| **Critical** | N-C-01: 工作流直接SQL操作无一致性保证 | 审批流程数据完整性 |
| **High** | H-02: Cycle @Transient 字段不持久化 | 周期管理功能失效 |
| **High** | N-H-01: 事件携带 null ID | 事件驱动架构失效 |
| **High** | N-H-03: 计划统计字段全部硬编码为 0 | 前端显示完全不准确 |
| **High** | H-10 + N-C-01: 跨模块直接SQL | 架构隔离被破坏 |
| **Medium** | M-12: 上帝类 PlanApplicationService (1585行) | 可维护性持续恶化 |

### C.3 Top 5 优先修复建议

| 优先级 | 问题编号 | 修复内容 | 预计工时 | 理由 |
|--------|---------|---------|----------|------|
| **P0** | C-04 | 为所有23个读取端点添加 `@PreAuthorize("isAuthenticated()")` | 2h | 安全风险最高，修改简单，立即见效 |
| **P0** | N-C-01 + H-09 | 将工作流操作抽象到 `WorkflowCommandService` 接口，消除 `TransactionTemplate` | 16h | 数据一致性关键路径，消除 `String.formatted()` SQL拼接 |
| **P1** | H-02 | Cycle 移除 `@Transient`，添加 Flyway 迁移脚本 | 4h | 功能性 Bug，停用/删除操作完全无效 |
| **P1** | N-H-01 + M-02 | 指标事件改为 `@PostPersist` 或在 Service 层 save 后发布 | 4h | 事件 ID 为 null 导致事件驱动架构完全失效 |
| **P2** | N-H-03 + M-05 | 批量查询计划关联指标/里程碑数量 | 8h | 前端所有计划列表/详情页数据不准确 |

### C.4 架构建议

1. **拆分 PlanApplicationService (M-12)** — 建议拆分为:
   - `PlanCrudService` — 基础 CRUD
   - `PlanWorkflowService` — 审批/撤回/工作流操作
   - `PlanQueryService` — 查询和响应转换
   - `PlanIndicatorSyncService` — 指标状态同步

2. **建立 Anti-Corruption Layer** — `PlanWorkflowSnapshotQueryService` 应通过接口访问工作流模块的数据，而非直接 SQL 查询跨限界上下文的表。

3. **统一 DTO 规范** — 清理 Controller 内部类 DTO（L-01, L-07），统一使用 `interfaces/dto/` 包下的 `@Data` 类，添加 `@Valid` 校验注解。

4. **清理死代码** — `StrategyJpaPlanRepositoryInternal`（M-11）、`buildTaskTypeMap`（L-02）、`getLatestReportProgress`/`hasCurrentMonthFill`（L-03）、重复的 `PlanLevel` 枚举（N-M-02）。
