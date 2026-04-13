# 审计报告：sism-strategy 模块（战略规划）

**审计日期:** 2026-04-12
**审计范围:** 53个Java源文件，涵盖指标、计划、周期、里程碑管理。

---

## 一、Critical 严重 (共4个)

### C-01. SQL注入风险 — 手动拼接IN子句占位符
**文件:** `IndicatorController.java:675-689,782-808`
**状态:** 已修复（2026-04-12）
**描述:** 使用 `String.formatted()` 手动拼接SQL的IN子句 `?` 占位符。虽然占位符是 `?`，但绕过了JPA参数化保护，且taskIds过大时可能超出数据库参数上限。
```java
String placeholders = taskIds.stream().map(id -> "?").collect(Collectors.joining(","));
jdbcTemplate.queryForList("... WHERE t.task_id IN (%s)".formatted(placeholders), taskIds.toArray());
```
**修复建议:** 使用 `NamedParameterJdbcTemplate` + `MapSqlParameterSource` 传入集合参数。

### C-02. 竞态条件 — PlanIntegrityService.ensurePlanMatrix 并发安全缺陷
**文件:** `PlanIntegrityService.java:39-83`
**状态:** 部分缓解（2026-04-12）
**描述:** `AtomicBoolean + volatile long` 做并发控制，但 `lastEnsuredAtMillis` 的"读-判-写"非原子；`synchronized` 锁与 `AtomicBoolean` 形成两套独立机制不保证互斥；集群环境下完全失效。
**修复建议:** 使用分布式锁或数据库 `SELECT ... FOR UPDATE`。

### C-03. N+1性能灾难 — toIndicatorResponse 单条查询回退
**文件:** `IndicatorController.java:579-586`
**状态:** 已修复（2026-04-12）
**描述:** 单条转换方法内部调用 `buildTaskMetaMap`/`buildMilestoneMap`/`buildCurrentMonthIndicatorRoundStateMap`，每个指标触发3次独立数据库查询，未命中缓存还额外查库。
**修复建议:** 对单条查询端点复用批量方法或使用JOIN查询。

### C-04. 大量API端点缺少权限检查
**文件:** `CycleController.java:28-70`, `IndicatorController.java:66-104`, `PlanController.java:32-55`, `MilestoneController.java:29-34`
**状态:** 未修复（截至 2026-04-12）
**描述:** 所有读取API端点无 `@PreAuthorize` 注解，任何已认证用户可访问所有计划、指标、周期、里程碑数据。企业战略管理系统敏感数据可能泄露。
**修复建议:** 所有端点添加 `@PreAuthorize("isAuthenticated()")` 或更细粒度的角色控制。

---

## 二、High 高 (共10个)

### H-01. getCycleById 返回null而非抛异常
**文件:** `CycleApplicationService.java:33-39`
**状态:** 已修复（2026-04-12）
**描述:** 找不到实体时返回null，调用方容易遗忘检查导致NPE。
**修复建议:** 使用 `orElseThrow()`。

### H-02. Cycle.status 和 isDeleted 标记 @Transient — 不持久化
**文件:** `Cycle.java:43-47`
**状态:** 未修复（截至 2026-04-12）
**描述:** `status` 和 `isDeleted` 标为 `@Transient`，停用/删除操作实际无效。`status` 通过 `@PostLoad` 重新计算，但 `isDeleted` 始终false。
**修复建议:** 去除 `@Transient`，改为真实数据库列。

### H-03. 多个查询方法返回null
**文件:** `StrategyApplicationService.java:213-219`
**状态:** 已修复（2026-04-12）
**描述:** `getIndicatorById` 等方法在找不到实体时返回null，与模块内其他"抛异常"模式不一致。
**修复建议:** 统一使用 `orElseThrow()` 或返回 `Optional<T>`。

### H-04. lastEnsuredAtMillis 不保证事务完成
**文件:** `PlanIntegrityService.java:74`
**状态:** 已修复（2026-04-12）
**描述:** 时间戳在事务可能未提交时就已更新，事务回滚后后续调用被跳过。
**修复建议:** 移到事务提交后回调中。

### H-05. MilestoneApplicationService 全表加载后内存分页
**文件:** `MilestoneApplicationService.java:205-233`
**状态:** 已修复（2026-04-12）
**描述:** `milestoneRepository.findAll()` 加载全部里程碑到内存再过滤分页，数据增长后性能灾难。
**修复建议:** 使用数据库层面的 `Specification` 或 `@Query`。

### H-06. JpaIndicatorRepository findFirstLevelIndicators 全表扫描
**文件:** `JpaIndicatorRepository.java:123-134`
**状态:** 已修复（2026-04-12）
**描述:** 先 `findAll()` 再 Stream 过滤，且触发懒加载N+1问题。
**修复建议:** 使用JPQL JOIN查询直接过滤。

### H-07. JpaIndicatorRepository.findByKeyword 全表扫描后内存搜索
**文件:** `JpaIndicatorRepository.java:137-148`
**状态:** 已修复（2026-04-12）
**描述:** 全量加载后用 `String.contains()` 过滤，无法利用数据库索引或全文搜索。
**修复建议:** 使用 `LIKE` 的JPQL查询或PostgreSQL全文搜索。

### H-08. PlanApplicationService 手工缓存机制性能瓶颈
**文件:** `PlanApplicationService.java:82-84,1085-1105`
**状态:** 已修复（2026-04-12）
**描述:** `volatile + synchronized` 手工缓存，每次 `loadOrgNamesById()` 执行 `organizationRepository.findAll()` 全表查询，高并发下 `synchronized` 成瓶颈。
**修复建议:** 使用Caffeine或Guava Cache替代。

### H-09. submitPlanForApproval 手动管理事务
**文件:** `PlanApplicationService.java:183-193`
**状态:** 未修复（截至 2026-04-12）
**描述:** 使用 `TransactionTemplate` 而非 `@Transactional`，打破Spring声明式事务一致性。
**修复建议:** 使用 `@Transactional` + `@TransactionalEventListener`。

### H-10. PlanWorkflowSnapshotQueryService 跨限界上下文直接SQL
**文件:** `PlanWorkflowSnapshotQueryService.java:55-70`
**状态:** 未修复（截至 2026-04-12）
**描述:** 直接SQL查询 `audit_instance`、`audit_step_instance`、`sys_user` 等其他限界上下文的表，违反DDD隔离原则。
**修复建议:** 通过工作流上下文的应用服务或查询接口访问。

---

## 三、Medium 中等 (共12个)

| # | 文件:行号 | 问题 | 类别 |
|---|---|---|---|
| M-01 | `Plan.java` | 缺少 equals/hashCode 实现，HashSet去重失效 | Bug |
| M-02 | `Indicator.java:115-116` | IndicatorCreatedEvent 在 id=null 时创建 | Bug |
| M-03 | `PlanApplicationService.java:1420` | PlanStatus映射缺少 IN_REVIEW 等中间状态 | Bug |
| M-04 | `CycleApplicationService.java:41-46` | getCyclesByStatus 先全量查询再内存过滤 | 性能 |
| M-05 | `PlanApplicationService.java:1003-1031` | indicatorCount/milestoneCount/completionPercentage 硬编码为0 | Bug |
| M-06 | `CycleController.java:76-83` | createCycle 未使用 @Valid，日期解析异常返回500 | Bug |
| M-07 | `MilestoneController.java:36-43` | getMilestonesByPlan 返回硬编码空列表 | 功能缺失 |
| M-08 | `Indicator.java:228,417` | distribute() 与 distributeFrom() 语义冲突 | 代码质量 |
| M-09 | `IndicatorDomainService.java:28-51` | distribute() 中 setWeight 被重复设置 | 代码质量 |
| M-10 | `PlanApplicationService.java:1184` | taskId 作为 cycleId 临时方案，前端显示错误数据 | Bug |
| M-11 | 两个JpaPlanRepositoryInternal | 重复定义，StrategyJpaPlanRepositoryInternal 是死代码 | 死代码 |
| M-12 | `PlanApplicationService.java` 全文(1577行) | 上帝类，职责过重：CRUD+审批+缓存+同步+查询 | 架构 |

---

## 四、Low 低 (共10个)

| # | 文件:行号 | 问题 |
|---|---|---|
| L-01 | `CycleController.java:112-126` | 请求DTO用内部类+手写getter，与项目@Data风格不一致 |
| L-02 | `IndicatorController.java:732-756` | buildTaskTypeMap 死代码 |
| L-03 | `IndicatorController.java:859-917` | getLatestReportProgress/hasCurrentMonthFill 死代码 |
| L-04 | `IndicatorController.java:931-940` | optionalIndicatorType 死代码 |
| L-05 | `Indicator.java:36-37` | parentIndicatorId 列 insertable=false/updatable=false，setter无效 |
| L-06 | `Indicator.java:307-331` | isStrategic() 用硬编码字符串比较组织类型 |
| L-07 | `CycleController.java:112-126` | DTO日期用String而非LocalDate |
| L-08 | `Milestone.java` | 缺少 @PrePersist/@PreUpdate 时间戳管理 |
| L-09 | `PlanStatus.java:50-52` | RETURNED分支添加重复值 |
| L-10 | `PlanApplicationService.java:1065-1076` | awaitWorkflowSnapshot 只查询两次不真正等待 |

---

## 汇总统计

| 严重性 | 数量 | 关键主题 |
|--------|------|----------|
| **Critical** | 4 | SQL注入、竞态条件、N+1性能灾难、权限缺失 |
| **High** | 10 | @Transient字段、null返回、全表扫描、手工缓存、跨限界SQL |
| **Medium** | 12 | 硬编码占位值、方法语义冲突、死代码、上帝类 |
| **Low** | 10 | 死代码、风格不一致、硬编码字符串 |
| **总计** | **36** | |

**优先修复建议：**
1. **C-01** SQL注入 — 立即修复
2. **C-04** 权限缺失 — 立即修复
3. **C-02** 竞态条件 + **H-02** Cycle @Transient — 数据正确性
4. **H-05/H-06/H-07** 全表扫描 — 性能瓶颈
5. **M-12** PlanApplicationService 拆分 — 可维护性
