# sism-task 模块审计报告

**审计日期:** 2026-04-06
**审计人员:** Claude Code (Automated Audit)
**模块版本:** 当前主分支

---

## 模块概览

| 项目 | 数据 |
|------|------|
| 模块名称 | sism-task |
| 模块职责 | 战略任务管理、任务创建/查询/状态变更 |
| Java 文件总数 | 27 |
| 核心实体 | StrategicTask |
| Repository 数量 | 2 (TaskRepository + JpaTaskRepositoryInternal) |
| Service 数量 | 1 |
| Controller 数量 | 1 |

### 包结构

```
com.sism.task/
├── application/
│   ├── TaskApplicationService.java
│   └── dto/
│       ├── CreateTaskRequest.java
│       ├── TaskQueryRequest.java
│       ├── TaskResponse.java
│       ├── UpdateTaskDescRequest.java
│       ├── UpdateTaskNameRequest.java
│       ├── UpdateTaskRemarkRequest.java
│       └── UpdateTaskRequest.java
├── domain/
│   ├── StrategicTask.java           # 聚合根
│   ├── TaskCategory.java            # 任务类别
│   ├── TaskType.java                # 任务类型枚举
│   ├── enums/
│   │   ├── AdhocScopeType.java
│   │   └── AdhocTaskStatus.java
│   ├── event/
│   │   ├── TaskCreatedEvent.java
│   │   └── TaskStatusChangedEvent.java
│   └── repository/
│       └── TaskRepository.java
├── infrastructure/
│   ├── TaskModuleConfig.java
│   └── persistence/
│       ├── JpaTaskRepository.java
│       ├── JpaTaskRepositoryInternal.java
│       ├── TaskFlatView.java
│       ├── TaskNameView.java
│       ├── TaskNameTypeView.java
│       └── TaskSummaryView.java
└── interfaces/
    └── rest/
        └── TaskController.java
```

---

## 一、安全漏洞

### 🔴 Critical: 所有 API 端点完全缺少权限控制

**文件:** `TaskController.java`
**行号:** 全文

```java
@PostMapping
@Operation(summary = "创建新任务")
public ResponseEntity<ApiResponse<TaskResponse>> createTask(@Valid @RequestBody CreateTaskRequest request) {
    // ❌ 无 @PreAuthorize 注解
}

@DeleteMapping("/{id}")
@Operation(summary = "删除任务(软删除)")
public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long id) {
    // ❌ 无权限控制
}

@PostMapping("/{id}/activate")
@Operation(summary = "激活任务")
public ResponseEntity<ApiResponse<TaskResponse>> activateTask(@PathVariable Long id) {
    // ❌ 无权限控制
}

@PostMapping("/{id}/complete")
@Operation(summary = "完成任务")
public ResponseEntity<ApiResponse<TaskResponse>> completeTask(@PathVariable Long id) {
    // ❌ 无权限控制
}

@PutMapping("/{id}")
@Operation(summary = "更新任务")
public ResponseEntity<ApiResponse<TaskResponse>> updateTask(...) {
    // ❌ 无权限控制
}
```

**问题描述:**
控制器中 15+ 个 API 端点全部缺少权限控制注解。任何认证用户都可以：
1. 创建任意任务
2. 删除任意任务
3. 修改任意任务
4. 变更任务状态

**风险影响:**
- 未授权的数据修改
- 任务数据被恶意篡改或删除
- 业务流程被绕过

**严重等级:** 🔴 **Critical**

**建议修复:**
```java
@PostMapping
@PreAuthorize("hasAnyRole('ADMIN', 'STRATEGY_DEPT')")
@Operation(summary = "创建新任务")
public ResponseEntity<ApiResponse<TaskResponse>> createTask(...) { }

@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
@Operation(summary = "删除任务(软删除)")
public ResponseEntity<ApiResponse<Void>> deleteTask(...) { }

@PostMapping("/{id}/activate")
@PreAuthorize("hasAnyRole('ADMIN', 'STRATEGY_DEPT', 'FUNC_DEPT')")
@Operation(summary = "激活任务")
public ResponseEntity<ApiResponse<TaskResponse>> activateTask(...) { }
```

---

### 🔴 High: 任务操作无业务规则校验

**文件:** `TaskController.java` 和 `TaskApplicationService.java`

**问题描述:**
1. 任务创建时未验证用户是否有权限操作该组织
2. 任务状态变更未验证用户角色
3. 任务删除未验证是否有关联数据

**风险影响:**
- 跨组织数据篡改
- 业务规则绕过

**严重等级:** 🔴 **High**

**建议修复:**
```java
@Transactional
public TaskResponse createTask(CreateTaskRequest request, CurrentUser currentUser) {
    // 验证用户有权限操作目标组织
    if (!currentUser.canOperateOrg(request.getOrgId())) {
        throw new UnauthorizedException("无权操作该组织的任务");
    }
    // ...
}
```

---

## 二、潜在 Bug 和逻辑错误

### 🔴 Critical: StrategicTask.status 是 Transient 字段，不持久化

**文件:** `StrategicTask.java`
**行号:** 78-83

```java
@Transient
private TaskCategory taskCategory = TaskCategory.STRATEGIC;

// 当前战略任务的 taskStatus 仍为任务域内部状态，不映射数据库列。
@Transient
private String status = STATUS_DRAFT;  // ❌ 不持久化！
```

**问题描述:**
1. `status` 字段标记为 `@Transient`，意味着它不会被持久化到数据库
2. 每次从数据库加载任务时，状态都会重置为 `STATUS_DRAFT`
3. `activate()`, `complete()`, `cancel()` 等状态变更操作实际上无效
4. 注释说 "任务域内部状态" 但代码设计了完整的状态机

**风险影响:**
- 任务状态变更完全失效
- 业务逻辑错误
- 数据不一致

**严重等级:** 🔴 **Critical**

**建议修复:**
```java
// 方案1: 添加数据库列并映射
@Column(name = "status", length = 20)
private String status = STATUS_DRAFT;

// 方案2: 如果状态确实来源于 Plan，则移除状态变更方法
// 并从 Plan 状态派生任务状态
```

---

### 🟠 Medium: TaskCategory 同样是 Transient 字段

**文件:** `StrategicTask.java`
**行号:** 78-79

```java
@Transient
private TaskCategory taskCategory = TaskCategory.STRATEGIC;  // ❌ 不持久化
```

**问题描述:**
`taskCategory` 同样标记为 `@Transient`，创建时传入的值无法持久化，每次加载后重置为默认值。

**严重等级:** 🟠 **Medium**

---

### 🟠 Medium: validatePlanBinding 使用 EntityManager 原生查询

**文件:** `TaskApplicationService.java`
**行号:** 287-335

```java
private void validatePlanBinding(Long planId, Long cycleId, SysOrg org, SysOrg createdByOrg) {
    // ...
    Optional<?> planRow = entityManager.createNativeQuery("""
            SELECT p.cycle_id, p.target_org_id, p.created_by_org_id, p.plan_level
            FROM public.plan p
            WHERE p.id = :planId
              AND COALESCE(p.is_deleted, false) = false
            """)
            .setParameter("planId", planId)
            .getResultStream()
            .findFirst();
    // ...
}
```

**问题描述:**
1. 服务层直接使用 `EntityManager` 执行原生 SQL
2. 绕过了 Repository 层，违反分层架构
3. SQL 硬编码在服务类中，难以维护

**风险影响:**
- 架构不一致
- 难以测试和复用

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
// 在 PlanRepository 中添加验证方法
Optional<PlanBindingInfo> findBindingInfoById(Long planId);

// 或使用 PlanApplicationService
planApplicationService.validateTaskBinding(planId, cycleId, org, createdByOrg);
```

---

### 🟡 Low: 异常消息混合中英文

**文件:** `TaskApplicationService.java` 和 `StrategicTask.java`

```java
// TaskApplicationService.java
throw new IllegalArgumentException("组织不存在: " + request.getOrgId());  // 中文
throw new IllegalArgumentException("计划不存在: " + planId);              // 中文
throw new IllegalArgumentException("任务周期与计划周期不一致");            // 中文

// StrategicTask.java
throw new IllegalArgumentException("Task name cannot be null or empty");  // 英文
throw new IllegalArgumentException("Task type cannot be null");           // 英文
throw new IllegalStateException("Cannot activate task: not in DRAFT state");  // 英文
```

**问题描述:**
异常消息中英文混用，缺乏一致性。

**严重等级:** 🟡 **Low**

---

## 三、性能瓶颈

### 🔴 High: searchTasks 内存分页

**文件:** `TaskApplicationService.java`
**行号:** 209-246

```java
@Transactional(readOnly = true)
public PageResult<TaskResponse> searchTasks(TaskQueryRequest request) {
    int page = request.getPage() != null ? request.getPage() : 0;
    int size = request.getSize() != null ? request.getSize() : 10;

    List<TaskResponse> matches = jpaTaskRepository.findFlatViewsByCriteria(...).stream()
            .map(TaskResponse::fromView)
            .filter(response -> request.getPlanStatus() == null || ...)  // ❌ 内存过滤
            .filter(response -> request.getTaskStatus() == null || ...)  // ❌ 内存过滤
            .toList();

    // 内存排序
    Comparator<TaskResponse> comparator = buildSearchComparator(request.getSortBy());
    if (comparator != null) {
        matches = matches.stream().sorted(comparator).toList();  // ❌ 内存排序
    }

    // 内存分页
    int fromIndex = Math.min(page * size, matches.size());
    int toIndex = Math.min(fromIndex + size, matches.size());
    List<TaskResponse> items = matches.subList(fromIndex, toIndex);  // ❌ 内存分页
}
```

**问题描述:**
1. 先从数据库加载所有匹配记录到内存
2. 在内存中进行过滤、排序、分页
3. 如果有 10,000+ 任务，会造成严重的内存和性能问题

**风险影响:**
- 内存溢出风险
- 响应延迟
- 数据库压力

**严重等级:** 🔴 **High**

**建议修复:**
```java
// 使用数据库分页
@Query(value = "... ORDER BY ... LIMIT :limit OFFSET :offset", nativeQuery = true)
List<TaskFlatView> findFlatViewsByCriteria(..., Pageable pageable);

// 或使用 Spring Data 的分页支持
Page<TaskFlatView> findFlatViewsByCriteria(..., Pageable pageable);
```

---

### 🟠 Medium: Native SQL 查询参数处理不安全

**文件:** `JpaTaskRepositoryInternal.java`
**行号:** 139-140

```java
AND (CAST(:taskType AS TEXT) = '' OR t.task_type = CAST(:taskType AS TEXT))
AND (CAST(:name AS TEXT) = '' OR t.name ILIKE CONCAT('%', CAST(:name AS TEXT), '%'))
```

**问题描述:**
1. 使用空字符串作为"忽略此条件"的标记，设计不够优雅
2. `CAST` 操作在每行数据上执行，可能影响索引使用
3. `ILIKE CONCAT` 可能无法使用索引

**风险影响:**
- 查询性能下降
- 索引失效

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
// 使用动态查询或 JPA Criteria API
// 或使用 COALESCE 更优雅地处理 NULL 参数
```

---

### 🟠 Medium: getTaskById 先查 Entity 再查 View

**文件:** `TaskApplicationService.java`
**行号:** 194-199

```java
@Transactional(readOnly = true)
public TaskResponse getTaskById(Long id) {
    return jpaTaskRepository.findFlatViewById(id)  // 查询 1
            .map(TaskResponse::fromView)
            .orElseThrow(...);
}

private TaskResponse toCommandResponse(StrategicTask task) {
    return TaskResponse.fromEntity(task, loadPlanStatus(task.getId()));  // 查询 2
}
```

**问题描述:**
在更新操作后，`toCommandResponse` 会再次查询数据库获取 `planStatus`，存在不必要的二次查询。

**严重等级:** 🟠 **Medium**

---

## 四、代码质量和可维护性

### 🟠 Medium: Service 层混合使用两个 Repository

**文件:** `TaskApplicationService.java`

```java
@Service
@RequiredArgsConstructor
public class TaskApplicationService {

    private final TaskRepository taskRepository;           // 领域 Repository
    private final JpaTaskRepositoryInternal jpaTaskRepository;  // JPA 内部 Repository
    // ...
}
```

**问题描述:**
1. 同时注入 `TaskRepository` 和 `JpaTaskRepositoryInternal`
2. 不同方法使用不同的 Repository
3. 职责混乱，违反单一职责原则

**风险影响:**
- 代码不一致
- 维护困难

**严重等级:** 🟠 **Medium**

**建议修复:**
统一使用 `TaskRepository`，将 `JpaTaskRepositoryInternal` 的查询方法移至适配器实现。

---

### 🟠 Medium: 缺少全局异常处理

**文件:** `TaskApplicationService.java`

```java
public TaskResponse createTask(CreateTaskRequest request) {
    SysOrg org = organizationRepository.findById(request.getOrgId())
            .orElseThrow(() -> new IllegalArgumentException("组织不存在: " + request.getOrgId()));
    // ...
}
```

**问题描述:**
1. 使用 `IllegalArgumentException` 而非业务异常
2. 未定义专门的业务异常类
3. 与其他模块异常处理不一致

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
// 使用业务异常
throw new ResourceNotFoundException("组织", request.getOrgId());
throw new BusinessException("TASK_ORG_NOT_FOUND", "组织不存在: " + request.getOrgId());
```

---

### 🟡 Low: 魔法字符串 - 状态值

**文件:** `StrategicTask.java`
**行号:** 26-29

```java
public static final String STATUS_DRAFT = "DRAFT";
public static final String STATUS_ACTIVE = "ACTIVE";
public static final String STATUS_COMPLETED = "COMPLETED";
public static final String STATUS_CANCELLED = "CANCELLED";
```

**问题描述:**
状态使用字符串常量而非枚举，虽然定义了常量，但类型安全性较差。

**建议修复:**
```java
public enum TaskStatus {
    DRAFT, ACTIVE, COMPLETED, CANCELLED
}
```

---

### 🟡 Low: 缺少 API 版本控制策略

**文件:** `TaskController.java`

```java
@RequestMapping("/api/v1/tasks")
```

**问题描述:**
虽然使用了 `v1` 版本前缀，但未定义版本升级策略和向后兼容性指南。

---

## 五、架构最佳实践

### 🟠 Medium: 领域事件 ID 在构造函数中生成

**文件:** `TaskCreatedEvent.java`
**行号:** 18-24

```java
public TaskCreatedEvent(Long taskId, String taskName, Long orgId) {
    this.eventId = UUID.randomUUID().toString();  // ✅ 正确：在构造时生成
    this.occurredOn = LocalDateTime.now();
    this.taskId = taskId;
    this.taskName = taskName;
    this.orgId = orgId;
}
```

**问题描述:**
此处实现正确，事件 ID 在构造时生成并存储。这与 shared-kernel 中 `DomainEvent` 接口的默认方法设计形成对比。

**评估:** ✅ 正确实现

---

### 🟠 Medium: Repository 接口定义良好

**文件:** `TaskRepository.java`

**评估:**
Repository 接口定义清晰，文档完整，遵循 DDD 领域层接口设计原则。

---

### 🟡 Low: 缺少聚合根验证触发

**文件:** `StrategicTask.java`

```java
@Override
public void validate() {
    // 验证逻辑
}
```

**问题描述:**
虽然定义了 `validate()` 方法，但未在 `@PrePersist` 或 `@PreUpdate` 中自动调用（依赖 ApplicationService 手动调用）。

---

## 六、审计总结

### 问题统计

| 严重等级 | 数量 | 类别分布 |
|----------|------|----------|
| 🔴 Critical | 2 | 安全（无权限控制）、Bug（Transient 状态） |
| 🔴 High | 2 | 安全、性能（内存分页） |
| 🟠 Medium | 6 | Bug、性能、代码质量、架构 |
| 🟡 Low | 4 | 代码质量、架构 |

### 最紧急修复项

| 优先级 | 问题 | 影响 |
|--------|------|------|
| P0 | 所有 API 无权限控制 | 任何用户可操作所有任务 |
| P0 | status 字段不持久化 | 任务状态变更完全失效 |
| P1 | searchTasks 内存分页 | 大数据量时性能问题 |
| P1 | 无业务规则校验 | 跨组织数据篡改风险 |

### 整体评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 安全性 | 🔴 需改进 | 完全缺失权限控制 |
| 可靠性 | 🔴 需改进 | Transient 状态导致功能失效 |
| 性能 | 🟠 需改进 | 内存分页存在风险 |
| 可维护性 | ✅ 良好 | DDD 分层清晰 |
| 架构合规性 | 🟠 需改进 | 服务层直接使用 EntityManager |

### 亮点

1. **DDD 分层清晰**: domain/application/infrastructure/interfaces 结构良好
2. **Repository 接口设计**: 领域层接口定义清晰，文档完整
3. **领域事件实现正确**: 事件 ID 在构造时生成
4. **软删除实现**: 使用 `isDeleted` 字段实现软删除

### 关键建议

1. **立即添加权限控制**: 为所有写操作添加 `@PreAuthorize`
2. **修复 Transient 字段**: 决定状态持久化策略，要么添加数据库列，要么移除状态变更方法
3. **改为数据库分页**: 将内存分页改为数据库分页
4. **统一异常处理**: 使用业务异常类替代 `IllegalArgumentException`

---

**审计完成日期:** 2026-04-06
**下一步行动:** 修复 Critical 级别问题后再部署生产环境