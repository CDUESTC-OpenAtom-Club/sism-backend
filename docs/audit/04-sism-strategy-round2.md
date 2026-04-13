# 第二轮审计报告：sism-strategy

**审计日期:** 2026-04-13
**审计范围:** 复审第一轮标记为已修复/部分缓解的问题，检查修复质量。

## 修复验证

### ✅ 验证通过

**C-01. SQL注入风险 — IN子句改用 NamedParameterJdbcTemplate**
- **状态:** 已修复，验证通过
- **验证详情:** `IndicatorController.java:687-700` 中 `buildTaskMetaMap` 已替换为 `NamedParameterJdbcTemplate` + `MapSqlParameterSource("taskIds", taskIds)`，第795-819行 `buildCurrentMonthIndicatorRoundStateMap` 同样使用 `NamedParameterJdbcTemplate`。`PlanApplicationService.java:1097-1104` 的 `loadOrgNamesById` 也已使用 `NamedParameterJdbcTemplate`。`PlanApplicationService.java:1255-1268` 的 `getCurrentReportContext` 同样正确使用。
- **评价:** 修复方案正确，完全消除了SQL注入风险，且支持集合参数的优雅传递。

**C-03. N+1性能灾难 — 单条查询回退改为复用批量方法**
- **状态:** 已修复，验证通过
- **验证详情:** `IndicatorController.java:594-598` 中 `toIndicatorResponse(Indicator indicator)` 现在委托给 `toIndicatorResponses(List.of(indicator))`，复用批量构建 `taskMetaMap`/`milestoneMap`/`currentMonthRoundStateMap` 的逻辑，不再为单个指标触发独立查询。
- **评价:** 修复方案简洁有效，消除了N+1查询的根本原因。

**H-01. getCycleById 返回null → 改为 orElseThrow**
- **状态:** 已修复，验证通过
- **验证详情:** `CycleApplicationService.java:33-38` 中 `getCycleById` 使用 `orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + id))`，调用方 `CycleController.java:66-72` 正确捕获异常并返回404。
- **评价:** 修复正确，与模块内其他方法风格统一。

**H-03. 多个查询方法返回null → 改为 orElseThrow**
- **状态:** 已修复，验证通过
- **验证详情:** `StrategyApplicationService.java:213-215` 中 `getIndicatorById` 使用 `orElseThrow(() -> new IllegalArgumentException("Indicator not found: " + id))`。`getIndicatorByIdAndOwnerOrgId`(第218-220行)同理。所有调用方均正确处理异常。
- **评价:** 修复正确，异常处理风格统一。

**H-04. lastEnsuredAtMillis 不保证事务完成 → 移到事务提交后回调**
- **状态:** 已修复，验证通过
- **验证详情:** `PlanIntegrityService.java:93-104` 中 `markEnsureCompletedAfterCommit()` 正确使用 `TransactionSynchronizationManager.registerSynchronization` 注册 `afterCommit` 回调，在事务提交后才更新 `lastEnsuredAtMillis`。同时处理了事务同步未激活的降级场景（第94-96行）。
- **评价:** 修复方案严谨，覆盖了无事务上下文的边界场景。

**H-05. MilestoneApplicationService 全表加载后内存分页 → 数据库分页**
- **状态:** 已修复，验证通过
- **验证详情:** `MilestoneApplicationService.java:206-220` 中 `getMilestones` 方法已使用 `milestoneRepository.findByIndicatorIdAndStatus`、`milestoneRepository.findByIndicatorId`、`milestoneRepository.findByStatus` 等数据库分页查询，取代了原先的 `findAll()` + 内存过滤。
- **评价:** 修复正确，分页逻辑下推到数据库层。

**H-06. JpaIndicatorRepository findFirstLevelIndicators 全表扫描 → JPQL查询**
- **状态:** 已修复，验证通过
- **验证详情:** `JpaIndicatorRepositoryInternal.java:108-113` 中 `findFirstLevelIndicators` 已改为JPQL查询 `SELECT i FROM Indicator i WHERE i.isDeleted = false AND i.ownerOrg.type <> :functionalType`，配合 `@EntityGraph` 初始化关联实体，避免N+1。
- **评价:** 修复正确，消除了全表扫描和懒加载问题。

**H-07. JpaIndicatorRepository.findByKeyword 全表扫描 → LIKE查询**
- **状态:** 已修复，验证通过
- **验证详情:** `JpaIndicatorRepositoryInternal.java:124` 添加了 `findByIndicatorDescContainingIgnoreCaseAndIsDeletedFalse(String keyword)` 方法，使用Spring Data JPA方法命名约定生成 `LIKE` 查询。`StrategyApplicationService` 通过 `indicatorRepository.findByKeyword(keyword)` 调用。
- **评价:** 修复正确，利用数据库索引进行搜索。

**H-08. PlanApplicationService 手工缓存 → 批量查询替代**
- **状态:** 已修复，验证通过
- **验证详情:** `PlanApplicationService.java:1083-1113` 中 `loadOrgNamesById(Collection<Plan>)` 使用 `NamedParameterJdbcTemplate` 批量查询 `sys_org` 表中的名称，通过 `IN (:orgIds)` 参数收集所有需要的组织ID后一次性查询。`loadOrgNamesById(Plan)` 委托给批量方法。原先的 `volatile + synchronized` 全表缓存机制已被完全移除。
- **评价:** 修复方案简洁有效，消除了手工缓存带来的并发瓶颈和全表查询问题。

### ⚠️ 部分修复 / 修复不完整

**C-02. 竞态条件 — PlanIntegrityService.ensurePlanMatrix 并发安全缺陷**
- **状态:** 部分缓解
- **当前代码分析:** `PlanIntegrityService.java:40-86`
  - `AtomicBoolean ensureInProgress` + `volatile long lastEnsuredAtMillis` 的双机制仍然存在
  - `lastEnsuredAtMillis` 的"读-判-写"（第42-44行 `isRecentlyEnsured(now)` 与第54行重新获取 `now`）仍然不是原子操作
  - 新增了两个 `synchronized` 锁 `STRAT_TO_FUNC_LOCK` 和 `FUNC_TO_COLLEGE_LOCK`（第30-31行），用于保护计划创建逻辑
  - `markEnsureCompletedAfterCommit()` 正确使用事务提交后回调
  - **但核心问题未解决：** 集群环境下 `volatile` + `AtomicBoolean` 仍然完全失效；`ensureInProgress` 在 finally 块中释放（第84行），但如果两个线程同时通过 `isRecentlyEnsured` 检查（因为 `lastEnsuredAtMillis` 已过期），都进入 `compareAndSet`，只有一个能成功，另一个被丢弃——这可能导致竞态窗口内的计划创建操作被跳过
- **改进建议:** 使用数据库级别的分布式锁，例如：
  ```java
  // 方案一：使用 SELECT ... FOR UPDATE 获取分布式锁
  @Transactional
  public void ensurePlanMatrix() {
      // 在 cycle 表或专门的 lock 表上获取行锁
      Cycle lockCycle = cycleRepository.findById(1L)
          .orElseThrow();
      // ... 后续逻辑在事务内执行，数据库保证互斥
  }

  // 方案二（推荐）：使用 ShedLock 库
  @SchedulerLock(name = "ensurePlanMatrix", lockAtLeastFor = "PT5M", lockAtMostFor = "PT10M")
  public void ensurePlanMatrix() { ... }
  ```

### 🔴 未修复

**C-04. 大量API端点缺少权限检查**
- **状态:** 未修复
- **验证详情:**
  - `CycleController.java:28-46` — `getAllCycles` 无 `@PreAuthorize`
  - `CycleController.java:49-53` — `getAllCyclesList` 无 `@PreAuthorize`
  - `CycleController.java:57-61` — `getAvailableYears` 无 `@PreAuthorize`
  - `CycleController.java:63-72` — `getCycleById` 无 `@PreAuthorize`
  - `IndicatorController.java:69-107` — `listIndicators` 无 `@PreAuthorize`
  - `IndicatorController.java:109-118` — `getIndicatorById` 无 `@PreAuthorize`
  - `IndicatorController.java:495-502` — `searchIndicators` 无 `@PreAuthorize`
  - `IndicatorController.java:504-509` — `getIndicatorsByTaskId` 无 `@PreAuthorize`
  - `IndicatorController.java:519-523` — `getIndicatorsByOwnerOrg` 无 `@PreAuthorize`
  - `PlanController.java:33-47` — `listPlans` 无 `@PreAuthorize`
  - `PlanController.java:49-55` — `getPlanById` 无 `@PreAuthorize`
  - `PlanController.java:57-61` — `getPlansByCycle` 无 `@PreAuthorize`
  - `PlanController.java:156-161` — `getPlanDetails` 无 `@PreAuthorize`
  - `MilestoneController.java:28-34` — `getMilestoneById` 无 `@PreAuthorize`
  - `MilestoneController.java:36-43` — `getMilestonesByPlan` 无 `@PreAuthorize`
- **评价:** 所有读取端点仍然无权限控制，任何已认证用户可访问所有战略数据。这是严重的安全隐患。

**H-02. Cycle.status 和 isDeleted 标记 @Transient — 不持久化**
- **状态:** 未修复
- **验证详情:** `Cycle.java:43-47` 中 `status` 和 `isDeleted` 仍然标记为 `@Transient`。`activate()`/`deactivate()`/`delete()` 方法修改的是瞬态字段，不会持久化到数据库。每次从数据库加载后，`@PostLoad` 回调（第118-126行）重新通过 `deriveStatus()` 计算 status，但 `isDeleted` 始终恢复为 `false`（除非在 `normalizeCycle` 中处理null值）。
- **评价:** `deactivateCycle()` 和 `deleteCycle()` 操作实际上是无效的——修改仅在当前JVM实例的生命周期内有效，数据库中不反映这些变更。

**H-09. submitPlanForApproval 手动管理事务**
- **状态:** 未修复
- **验证详情:** `PlanApplicationService.java:175-185` 仍然使用 `TransactionTemplate` 手动管理事务，而非 `@Transactional` 注解。`submitPlanForApprovalInTransaction`（第187-210行）在 `TransactionTemplate.execute` 内部执行，事务边界由编程式控制。
- **评价:** 虽然功能上可行，但与模块内其他方法（均使用 `@Transactional`）风格不一致，增加了维护复杂度。

**H-10. PlanWorkflowSnapshotQueryService 跨限界上下文直接SQL**
- **状态:** 未修复
- **验证详情:** `PlanWorkflowSnapshotQueryService.java:55-70` 直接查询 `audit_instance` 和 `audit_step_instance` 表（属于 `sism-workflow` 模块），`PlanApplicationService.java:333-377` 的 `withdrawWorkflowCurrentStep`/`reactivateWithdrawnWorkflowCurrentStep` 方法通过 `JdbcTemplate` 直接操作 `audit_instance`/`audit_step_instance`/`audit_step_def`/`sys_user`/`sys_role` 等表。
- **评价:** sism-strategy 模块直接操作了 sism-workflow 模块的数据库表，违反了DDD限界上下文隔离原则。表结构变更会导致运行时错误且编译期无法发现。

### Medium/Low 级别未修复问题确认

| 编号 | 状态 | 验证结果 |
|------|------|----------|
| M-01 | 未修复 | `Plan.java` 仍然缺少 `equals/hashCode` |
| M-02 | 未修复 | `Indicator.java:115` — `IndicatorCreatedEvent` 在 `indicator.id` 为null时创建（因为id由JPA持久化后生成） |
| M-03 | 未修复 | `PlanStatus.java` 的 `expandQueryStatuses` 已扩展（第31-64行），但 `RETURNED` 分支（第49-53行）仍包含重复值 `"RETURNED"` 出现两次 |
| M-04 | 未修复 | `CycleApplicationService.java:40-45` — `getCyclesByStatus` 仍然全量查询后内存过滤 |
| M-05 | 未修复 | `PlanApplicationService.java:1006-1008` — `indicatorCount`/`milestoneCount`/`completionPercentage` 仍然硬编码为0 |
| M-06 | 未修复 | `CycleController.java:78-85` — `createCycle` 未使用 `@Valid`，日期解析异常返回500 |
| M-07 | 未修复 | `MilestoneController.java:38-43` — `getMilestonesByPlan` 仍然返回硬编码空列表 `java.util.List.of()` |
| M-08 | 未修复 | `Indicator.java:228` 的 `distribute()` 与第417行 `distributeFrom()` 语义冲突 |
| M-09 | 未修复 | `IndicatorDomainService.java:28-51` — `distribute()` 中 weight 设置了两次（第44行和第48行） |
| M-10 | 未修复 | `PlanApplicationService.java:1192` — `convertIndicatorToResponse` 中 `cycleId` 使用 `indicator.getTaskId()` 作为临时方案 |
| M-11 | 未修复 | `StrategyJpaPlanRepositoryInternal.java` 仍然存在，作为独立的Repository接口 |
| M-12 | 未修复 | `PlanApplicationService.java` 仍然有1584行，未拆分 |
| L-01 | 未修复 | `CycleController.java:114-128` — `CreateCycleRequest` 仍使用手写getter |
| L-02 | 未修复 | `IndicatorController.java:743-767` — `buildTaskTypeMap` 方法仍然存在且未被调用 |
| L-03 | 未修复 | `IndicatorController.java:869-927` — `getLatestReportProgress`/`hasCurrentMonthFill` 方法仍然存在且未被调用 |
| L-04 | 未修复 | `IndicatorController.java:941-950` — `optionalIndicatorType` 方法仍然存在且未被调用 |
| L-05 | 未修复 | `Indicator.java:36` — `parentIndicatorId` 列仍为 `insertable=false/updatable=false` |
| L-06 | 部分 | `Indicator.java:307-331` — `isStrategic()` 已从硬编码组织名改为使用 `ownerOrg.getOrgType()` 比较枚举值 `"STRATEGY_DEPT"` 和 `"COLLEGE"`，但仍然使用字符串而非类型安全的枚举比较 |
| L-07 | 未修复 | `CycleController.java:114-128` — DTO日期仍然用 `String` 而非 `LocalDate` |
| L-08 | 未修复 | `Milestone.java` 仍然缺少 `@PrePersist`/`@PreUpdate` |
| L-09 | 未修复 | `PlanStatus.java:49-53` — `RETURNED` 分支添加 `"RETURNED"` 重复值 |
| L-10 | 未修复 | `PlanApplicationService.java:1059-1070` — `awaitWorkflowSnapshot` 只查询两次不真正等待 |

## 新发现问题

**N-01. IndicatorCreatedEvent 中 id 永远为null [Bug]**
- **文件:** `Indicator.java:115`
- **描述:** `create()` 静态工厂方法中 `new IndicatorCreatedEvent(indicator.id, ...)` 创建事件时 `indicator.id` 尚未由JPA生成，永远为null。虽然第一轮审计（M-02）已标记此问题，但第二轮复审发现这还影响了 `StrategyApplicationService.java:38` 中的 `publishAndSaveEvents(indicator)` —— 事件在 `save()` 之后发布，但事件中的id已在创建时固化为null。
- **建议修复:**
  ```java
  // 在 StrategyApplicationService.createIndicator 中
  indicator = indicatorRepository.save(indicator);
  // 在持久化后重新设置事件中的id，或改为在save之后创建事件
  indicator.clearEvents();
  indicator.addEvent(new IndicatorCreatedEvent(indicator.getId(), ...));
  publishAndSaveEvents(indicator);
  ```

**N-02. PlanApplicationService.createPlan 默认 orgId 硬编码为1L [Bug]**
- **文件:** `PlanApplicationService.java:93-97`
- **描述:** 当请求中未提供 `createdByOrgId` 或 `targetOrgId` 时，默认使用 `1L`。这在多组织环境下是错误的。
- **建议修复:** 应从 `@AuthenticationPrincipal CurrentUser` 获取当前用户的组织ID，或在没有提供时抛出异常而非使用硬编码默认值。

**N-03. StrategyOrgProperties 默认值硬编码 [代码质量]**
- **文件:** `StrategyOrgProperties.java:17-21`
- **描述:** `strategyOrgId` 和 `systemAdminOrgId` 默认值为 `35L`。如果配置缺失，将静默使用此默认值，可能导致错误组织被操作。
- **建议修复:** 使用 `@NotNull` 注解强制要求配置，或在应用启动时验证。

**N-04. IndicatorController.buildTaskMetaMap 中不必要的数据转换 [性能]**
- **文件:** `IndicatorController.java:676-681`
- **描述:** `taskIds` 先收集到 `LinkedHashSet` 再转为 `List`，多了一步无意义的中间转换。`LinkedHashSet` 用于去重，但后续传给 `NamedParameterJdbcTemplate` 时不需要保持插入顺序。
- **建议修复:** 直接使用 `Collectors.toSet()` + `List.copyOf(set)` 或 `new ArrayList<>(set)`。

## 总体评价

**修复率:** 8/12 已修复/部分缓解问题通过验证（67%），4个已标记问题实际未修复或修复不完整。

**修复质量:** 通过验证的8个修复质量较高，采用了业界标准方案（`NamedParameterJdbcTemplate`、`orElseThrow`、数据库分页、JPQL查询、事务提交后回调）。C-02（竞态条件）虽有改进但核心问题（集群环境失效）仍未解决。

**最大遗留风险:**
1. **C-04** 权限缺失 — 所有读取端点无权限控制，敏感战略数据暴露
2. **H-02** Cycle @Transient — 停用/删除操作无效
3. **H-10** 跨模块直接SQL — 架构违规，增加耦合和维护风险
