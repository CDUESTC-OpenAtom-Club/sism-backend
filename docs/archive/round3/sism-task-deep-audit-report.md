# sism-task 模块第三轮深度审计报告

**审计日期:** 2026-04-06
**审计人员:** Claude Code (Automated Deep Audit)
**模块版本:** 当前主分支
**审计轮次:** 第三轮

---

## 模块概览

| 项目 | 数据 |
|------|------|
| 模块名称 | sism-task |
| 模块职责 | 战略任务管理、任务创建/查询/状态变更 |
| Java 文件总数 | 27 |
| 核心实体 | StrategicTask |
| Repository 数量 | 3 (TaskRepository + JpaTaskRepositoryInternal + PlanBindingRepository) |
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
│       ├── PlanBindingRepository.java
│       ├── TaskFlatView.java
│       ├── TaskNameView.java
│       ├── TaskNameTypeView.java
│       └── TaskSummaryView.java
└── interfaces/
    └── rest/
        └── TaskController.java
```

---

## 一、任务管理与分配审计

### ✅ 已修复的关键问题

1. **权限控制基础架构已完善**
   - 所有写操作端点已添加 `@PreAuthorize` 注解
   - 读操作端点添加了 `isAuthenticated()` 权限控制
   - 实现了组织级别的权限校验：`ensureCanOperateOnOrg` 和 `ensureCanAccessTask`
   - 实现了数据过滤：`filterTasksByPermission` 确保用户只能访问自己组织的任务

2. **业务规则校验增强**
   - 任务创建时验证计划绑定关系
   - 验证任务周期与计划周期一致性
   - 验证任务归属组织与计划目标组织一致性
   - 验证任务创建组织与计划创建组织一致性
   - 按计划级别验证组织类型兼容性

### 🟠 仍需改进的任务管理问题

#### 1. 任务分配和责任人管理缺失
**严重等级:** 🟠 Medium
**文件:** 所有相关文件

**问题描述:**
- 当前模块仅管理任务基本信息，但完全缺少任务分配和责任人管理功能
- 没有 `assignee`（责任人）字段或相关关联
- 没有任务分配接口或功能
- 无法跟踪任务执行情况和责任人

**风险影响:**
- 无法明确任务责任主体
- 无法进行任务分配和流转
- 缺乏任务执行跟踪能力

**建议修复:**
```java
// 在 StrategicTask.java 中添加
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = # sism-task 模块第三轮深度审计报告

**审计日期:** 2026-04-06
**审计人员:** Claude Code (Automated Deep Audit)
**模块版本:** 当前主分支
**审计轮次:** 第三轮

---

## 模块概览

| 项目 | 数据 |
|------|------|
| 模块名称 | sism-task |
| 模块职责 | 战略任务管理、任务创建/查询/状态变更 |
| Java 文件总数 | 27 |
| 核心实体 | StrategicTask |
| Repository 数量 | 3 (TaskRepository + JpaTaskRepositoryInternal + PlanBindingRepository) |
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
│       ├── PlanBindingRepository.java
│       ├── TaskFlatView.java
│       ├── TaskNameView.java
│       ├── TaskNameTypeView.java
│       └── TaskSummaryView.java
└── interfaces/
    └── rest/
        └── TaskController.java
```

---

## 一、任务管理与分配审计

### ✅ 已修复的关键问题

1. **权限控制基础架构已完善**
   - 所有写操作端点已添加 `@PreAuthorize` 注解
   - 读操作端点添加了 `isAuthenticated()` 权限控制
   - 实现了组织级别的权限校验：`ensureCanOperateOnOrg` 和 `ensureCanAccessTask`
   - 实现了数据过滤：`filterTasksByPermission` 确保用户只能访问自己组织的任务

2. **业务规则校验增强**
   - 任务创建时验证计划绑定关系
   - 验证任务周期与计划周期一致性
   - 验证任务归属组织与计划目标组织一致性
   - 验证任务创建组织与计划创建组织一致性
   - 按计划级别验证组织类型兼容性

### 🟠 仍需改进的任务管理问题

#### 1. 任务分配和责任人管理缺失
**严重等级:** 🟠 Medium
**文件:** 所有相关文件

**问题描述:**
- 当前模块仅管理任务基本信息，但完全缺少任务分配和责任人管理功能
- 没有 `assignee`（责任人）字段或相关关联
- 没有任务分配接口或功能
- 无法跟踪任务执行情况和责任人

**风险影响:**
- 无法明确任务责任主体
- 无法进行任务分配和流转
- 缺乏任务执行跟踪能力

**建议修复:**
```java
// 在 StrategicTask.java 中添加
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "assignee_id")
private SysUser assignee;

// 添加方法
public void assignTo(SysUser user) {
    this.assignee = user;
    this.updatedAt = LocalDateTime.now();
    this.addEvent(new TaskAssignedEvent(this.id, user.getId()));
}

// 在 TaskController.java 中添加接口
@PutMapping("/{id}/assign")
@PreAuthorize("hasAnyRole('ADMIN', 'STRATEGY_DEPT')")
public ResponseEntity<ApiResponse<TaskResponse>> assignTask(
        @PathVariable Long id,
        @RequestBody AssignTaskRequest request) {
    TaskResponse assigned = taskApplicationService.assignTask(id, request.getUserId());
    return ResponseEntity.ok(ApiResponse.success(assigned));
}
```

#### 2. 任务状态机设计不完整
**严重等级:** 🟠 Medium
**文件:** `StrategicTask.java`

**问题描述:**
- 状态使用字符串常量而非枚举，类型安全性较差
- 缺少状态转换历史记录
- 状态变更没有审计轨迹
- 无法处理复杂的状态转换规则

**风险影响:**
- 状态转换逻辑分散在各个方法中
- 难以维护和扩展
- 缺少审计和追溯能力

**建议修复:**
```java
// 使用枚举替代字符串常量
public enum TaskStatus {
    DRAFT("草稿"), ACTIVE("激活"), COMPLETED("完成"), CANCELLED("取消");
}

// 添加状态转换记录
@ElementCollection
@CollectionTable(name = "task_status_history", joinColumns = @JoinColumn(name = "task_id"))
@OrderBy("changedAt DESC")
private List<TaskStatusHistory> statusHistory = new ArrayList<>();

// 状态变更方法重构
public void activate() {
    if (!status.equals(TaskStatus.DRAFT)) {
        throw new IllegalStateException("任务未处于草稿状态，无法激活");
    }
    changeStatus(TaskStatus.ACTIVE);
}

private void changeStatus(TaskStatus newStatus) {
    TaskStatus oldStatus = this.status;
    this.status = newStatus;
    this.updatedAt = LocalDateTime.now();
    this.statusHistory.add(new TaskStatusHistory(oldStatus, newStatus, LocalDateTime.now()));
    this.addEvent(new TaskStatusChangedEvent(this.id, oldStatus, newStatus));
}
```

---

## 二、API 安全与权限审计

### ✅ 已修复的关键问题

1. **基本权限控制已实现**
   - 写操作（create/activate/complete/cancel/update/delete）已添加 `@PreAuthorize("hasAnyRole('ADMIN', 'STRATEGY_DEPT')")`
   - 删除操作仅允许 ADMIN：`@PreAuthorize("hasRole('ADMIN')")`
   - 读操作已添加 `@PreAuthorize("isAuthenticated()")`

2. **组织级权限验证**
   - 实现了 `filterTasksByPermission()` 方法过滤用户可访问的任务
   - `canAccessTask()` 方法验证用户所属组织与任务组织的匹配性
   - `ensureCanOperateOnOrg()` 确保用户只能操作自己组织的任务

### ⚠️ 部分修复的问题

#### 1. 读操作端点权限控制不完善
**严重等级:** 🟠 Medium
**文件:** `TaskController.java` (Lines 43-236)

**问题描述:**
- 所有 GET/读操作端点均使用 `@PreAuthorize("isAuthenticated()")`，权限过于宽松
- 任何认证用户都可以查询其他组织的任务数据
- `/api/v1/tasks/search` 等接口存在数据泄露风险

**风险影响:**
- 数据泄露风险
- 信息安全违规
- 组织间数据隔离失效

**建议修复:**
```java
// 读操作权限控制优化
@GetMapping("/{id}")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<ApiResponse<TaskResponse>> getTask(
        @PathVariable Long id,
        Authentication authentication) {
    TaskResponse task = taskApplicationService.getTaskById(id);
    ensureCanAccessTask(authentication, task); // ✅ 已添加验证
    return ResponseEntity.ok(ApiResponse.success(task));
}

@GetMapping("/search")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<ApiResponse<PageResult<TaskResponse>>> searchTasks(...) {
    PageResult<TaskResponse> result = taskApplicationService.searchTasks(queryRequest);
    List<TaskResponse> filteredItems = filterTasksByPermission(result.getItems(), authentication); // ✅ 已添加过滤
    return ResponseEntity.ok(ApiResponse.success(PageResult.of(
            filteredItems,
            filteredItems.size(),
            result.getPage(),
            result.getPageSize()
    )));
}
```

#### 2. 缺少 API 访问日志
**严重等级:** 🟡 Low
**文件:** `TaskController.java`

**问题描述:**
- 没有记录 API 访问日志
- 缺少用户操作轨迹记录
- 难以进行安全审计和问题排查

**风险影响:**
- 缺乏操作审计能力
- 问题排查困难
- 无法追溯安全事件

**建议修复:**
```java
// 使用 AOP 或拦截器记录访问日志
@PostMapping
@PreAuthorize("hasAnyRole('ADMIN', 'STRATEGY_DEPT')")
@Operation(summary = "创建新任务")
public ResponseEntity<ApiResponse<TaskResponse>> createTask(...) {
    log.info("用户 {} 创建任务: {}", currentUser.getUsername(), request.getName());
    TaskResponse created = taskApplicationService.createTask(request);
    log.debug("任务创建成功: {}", created.getId());
    return ResponseEntity.ok(ApiResponse.success(created));
}
```

---

## 三、数据库交互审计

### ✅ 已修复的关键问题

1. **字段持久化问题已解决**
   - `status` 字段已添加 `@Column` 注解，不再是 Transient
   - `taskCategory` 已添加 `@Enumerated(EnumType.STRING)` 注解

2. **Repository 设计优化**
   - `PlanBindingRepository` 已从服务层提取出来
   - 使用专门的 DTO 类型 `PlanBindingInfo` 封装查询结果
   - JpaTaskRepositoryInternal 提供了完整的查询方法

### 🔴 高风险数据库问题

#### 1. searchTasks 内存分页仍未解决
**严重等级:** 🔴 High
**文件:** `TaskApplicationService.java` (Lines 205-243)

**问题描述:**
- `searchTasks` 方法仍然执行内存分页
- 先加载所有匹配记录到内存，然后在内存中过滤、排序、分页
- 当任务数量达到 10,000+ 条时，会造成严重的性能问题

**风险影响:**
- 内存溢出风险
- 响应延迟
- 数据库压力
- 服务崩溃风险

**代码示例:**
```java
// ❌ 内存分页
List<TaskResponse> matches = jpaTaskRepository.findFlatViewsByCriteria(...).stream()
        .map(TaskResponse::fromView)
        .filter(response -> request.getPlanStatus() == null || ...)  // 内存过滤
        .filter(response -> request.getTaskStatus() == null || ...) // 内存过滤
        .toList();

// 内存排序
Comparator<TaskResponse> comparator = buildSearchComparator(request.getSortBy());
matches = matches.stream().sorted(comparator).toList(); // 内存排序

// 内存分页
int fromIndex = Math.min(page * size, matches.size());
int toIndex = Math.min(fromIndex + size, matches.size());
List<TaskResponse> items = matches.subList(fromIndex, toIndex); // 内存分页
```

**建议修复:**
```java
// 使用数据库分页替代内存分页
@Transactional(readOnly = true)
public PageResult<TaskResponse> searchTasks(TaskQueryRequest request) {
    Pageable pageable = PageRequest.of(
            request.getPage() != null ? request.getPage() : 0,
            request.getSize() != null ? request.getSize() : 10,
            buildPageSort(request.getSortBy(), request.getSortDirection())
    );

    // 数据库层面过滤和分页
    Page<TaskFlatView> results = jpaTaskRepository.findFlatViewsByCriteria(
            request.getPlanId(),
            request.getCycleId(),
            request.getOrgId(),
            request.getCreatedByOrgId(),
            request.getTaskType(),
            request.getName(),
            request.getPlanStatus(),
            request.getTaskStatus(),
            pageable
    );

    return PageResult.of(
            results.getContent().stream().map(TaskResponse::fromView).toList(),
            results.getTotalElements(),
            results.getNumber(),
            results.getSize()
    );
}

// 在 JpaTaskRepositoryInternal.java 中添加数据库查询方法
@Query(value = """
        SELECT
            t.task_id AS id,
            t.name AS name,
            t."desc" AS desc,
            t.task_type AS taskType,
            t.plan_id AS planId,
            t.cycle_id AS cycleId,
            t.org_id AS orgId,
            t.created_by_org_id AS createdByOrgId,
            t.sort_order AS sortOrder,
            COALESCE(p.status, 'DRAFT') AS planStatus,
            COALESCE(t.task_category, 'STRATEGIC') AS taskCategory,
            COALESCE(t.status, 'DRAFT') AS taskStatus,
            t.remark AS remark,
            t.created_at AS createdAt,
            t.updated_at AS updatedAt
        FROM sys_task t
        LEFT JOIN plan p ON p.id = t.plan_id
        WHERE COALESCE(t.is_deleted, false) = false
          AND (:planId IS NULL OR t.plan_id = :planId)
          AND (:cycleId IS NULL OR t.cycle_id = :cycleId)
          AND (:orgId IS NULL OR t.org_id = :orgId)
          AND (:createdByOrgId IS NULL OR t.created_by_org_id = :createdByOrgId)
          AND (:taskType IS NULL OR t.task_type = CAST(:taskType AS TEXT))
          AND (:name IS NULL OR t.name ILIKE CONCAT('%', CAST(:name AS TEXT), '%'))
          AND (:planStatus IS NULL OR p.status = CAST(:planStatus AS TEXT))
          AND (:taskStatus IS NULL OR t.status = CAST(:taskStatus AS TEXT))
        ORDER BY t.sort_order ASC, t.task_id ASC
        """, countQuery = """
        SELECT COUNT(t.task_id)
        FROM sys_task t
        LEFT JOIN plan p ON p.id = t.plan_id
        WHERE COALESCE(t.is_deleted, false) = false
          AND (:planId IS NULL OR t.plan_id = :planId)
          AND (:cycleId IS NULL OR t.cycle_id = :cycleId)
          AND (:orgId IS NULL OR t.org_id = :orgId)
          AND (:createdByOrgId IS NULL OR t.created_by_org_id = :createdByOrgId)
          AND (:taskType IS NULL OR t.task_type = CAST(:taskType AS TEXT))
          AND (:name IS NULL OR t.name ILIKE CONCAT('%', CAST(:name AS TEXT), '%'))
          AND (:planStatus IS NULL OR p.status = CAST(:planStatus AS TEXT))
          AND (:taskStatus IS NULL OR t.status = CAST(:taskStatus AS TEXT))
        """, nativeQuery = true)
Page<TaskFlatView> findFlatViewsByCriteria(
        @Param("planId") Long planId,
        @Param("cycleId") Long cycleId,
        @Param("orgId") Long orgId,
        @Param("createdByOrgId") Long createdByOrgId,
        @Param("taskType") String taskType,
        @Param("name") String name,
        @Param("planStatus") String planStatus,
        @Param("taskStatus") String taskStatus,
        Pageable pageable);
```

#### 2. 原生 SQL 查询参数处理不安全
**严重等级:** 🟠 Medium
**文件:** `JpaTaskRepositoryInternal.java`

**问题描述:**
- 使用空字符串作为"忽略此条件"的标记，设计不够优雅
- `CAST` 操作在每行数据上执行，可能影响索引使用
- `ILIKE CONCAT` 无法有效使用索引

**风险影响:**
- 查询性能下降
- 索引失效
- 难以维护和扩展

**建议修复:**
```java
// 使用动态查询或 JPA Criteria API 替代空字符串标记
@Query("""
        SELECT t FROM StrategicTask t
        WHERE t.isDeleted = false
          AND (:planId IS NULL OR t.planId = :planId)
          AND (:cycleId IS NULL OR t.cycleId = :cycleId)
          AND (:orgId IS NULL OR t.org.id = :orgId)
          AND (:createdByOrgId IS NULL OR t.createdByOrg.id = :createdByOrgId)
          AND (:taskType IS NULL OR t.taskType = :taskType)
          AND (:name IS NULL OR t.name LIKE %:name%)
        ORDER BY t.sortOrder ASC, t.id ASC
        """)
Page<StrategicTask> findByCriteria(
        @Param("planId") Long planId,
        @Param("cycleId") Long cycleId,
        @Param("orgId") Long orgId,
        @Param("createdByOrgId") Long createdByOrgId,
        @Param("taskType") TaskType taskType,
        @Param("name") String name,
        Pageable pageable);

// 或者使用 Specifications
public static Specification<StrategicTask> withCriteria(TaskQueryRequest request) {
    return (root, query, cb) -> {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("isDeleted"), false));
        
        if (request.getPlanId() != null) {
            predicates.add(cb.equal(root.get("planId"), request.getPlanId()));
        }
        if (request.getCycleId() != null) {
            predicates.add(cb.equal(root.get("cycleId"), request.getCycleId()));
        }
        if (request.getOrgId() != null) {
            predicates.add(cb.equal(root.get("org").get("id"), request.getOrgId()));
        }
        if (request.getCreatedByOrgId() != null) {
            predicates.add(cb.equal(root.get("createdByOrg").get("id"), request.getCreatedByOrgId()));
        }
        if (request.getTaskType() != null) {
            predicates.add(cb.equal(root.get("taskType"), request.getTaskType()));
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("name")), "%" + request.getName().toLowerCase() + "%"));
        }
        
        return cb.and(predicates.toArray(new Predicate[0]));
    };
}
```

---

## 四、业务逻辑实现审计

### ✅ 已修复的关键问题

1. **任务创建验证已完善**
   - 计划绑定验证已提取到 `PlanBindingRepository`
   - 添加了组织类型和计划级别的兼容性验证
   - 引入了任务分类和类型的规范管理

2. **事件发布机制优化**
   - `publishAndSaveEvents` 方法统一处理领域事件
   - 事件先存储到事件存储，再异步发布

### 🔴 高风险业务逻辑问题

#### 1. validatePlanBinding 直接使用 EntityManager
**严重等级:** 🟠 High
**文件:** `PlanBindingRepository.java`

**问题描述:**
- `PlanBindingRepository` 内部仍使用 EntityManager + 原生 SQL 查询
- 没有使用 Spring Data JPA 的查询方法
- 查询结果使用 Object[] 处理，类型不安全

**风险影响:**
- 违反分层架构原则
- 代码难以维护和测试
- 类型安全问题

**建议修复:**
```java
// 方案1: 使用 Spring Data JPA
public interface PlanRepository extends JpaRepository<Plan, Long> {
    @Query("""
            SELECT new com.sism.task.infrastructure.persistence.PlanBindingRepository.PlanBindingInfo(
                p.cycleId, p.targetOrgId, p.createdByOrgId, p.planLevel
            )
            FROM Plan p
            WHERE p.id = :planId
              AND p.isDeleted = false
            """)
    Optional<PlanBindingInfo> findBindingInfoById(@Param("planId") Long planId);
}

// 方案2: 使用 JdbcTemplate 替代 EntityManager
@Repository
public class PlanBindingRepository {
    private final JdbcTemplate jdbcTemplate;
    
    public Optional<PlanBindingInfo> findByPlanId(Long planId) {
        String sql = """
                SELECT cycle_id, target_org_id, created_by_org_id, plan_level
                FROM public.plan
                WHERE id = ?
                  AND COALESCE(is_deleted, false) = false
                """;
        return jdbcTemplate.query(sql, new Object[]{planId}, rs -> {
            if (rs.next()) {
                return Optional.of(new PlanBindingInfo(
                        rs.getLong("cycle_id"),
                        rs.getLong("target_org_id"),
                        rs.getLong("created_by_org_id"),
                        rs.getString("plan_level").trim().toUpperCase()
                ));
            }
            return Optional.empty();
        });
    }
}
```

#### 2. 异常处理不规范
**严重等级:** 🟠 Medium
**文件:** `TaskApplicationService.java`

**问题描述:**
- 仍在使用 `IllegalArgumentException` 而非业务异常
- 异常消息混合中英文
- 没有统一的异常处理机制

**风险影响:**
- 错误信息不规范
- 难以进行异常统一处理
- 用户体验差

**代码示例:**
```java
// ❌ 不规范的异常处理
public TaskResponse createTask(CreateTaskRequest request) {
    SysOrg org = organizationRepository.findById(request.getOrgId())
            .orElseThrow(() -> new IllegalArgumentException("组织不存在: " + request.getOrgId()));
    // ...
}

// StrategicTask.java
public static StrategicTask create(String name, TaskType taskType, ...) {
    if (name == null || name.trim().isEmpty()) {
        throw new IllegalArgumentException("Task name cannot be null or empty");
    }
    // ...
}
```

**建议修复:**
```java
// 定义业务异常类
@Getter
public abstract class BusinessException extends RuntimeException {
    private final String code;
    private final Map<String, Object> data;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.data = new HashMap<>();
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.data = new HashMap<>();
    }

    public BusinessException addData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }
}

// 具体业务异常
public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resourceType, Object id) {
        super("RESOURCE_NOT_FOUND", String.format("%s 不存在: %s", resourceType, id));
        addData("type", resourceType);
        addData("id", id);
    }
}

public class BusinessRuleException extends BusinessException {
    public BusinessRuleException(String rule, String message) {
        super("BUSINESS_RULE_VIOLATION", message);
        addData("rule", rule);
    }
}

// 使用统一的异常处理
@Transactional
public TaskResponse createTask(CreateTaskRequest request) {
    SysOrg org = organizationRepository.findById(request.getOrgId())
            .orElseThrow(() -> new ResourceNotFoundException("组织", request.getOrgId()));
    
    validatePlanBinding(request.getPlanId(), request.getCycleId(), org, createdByOrg);
    // ...
}

private void validatePlanBinding(Long planId, Long cycleId, SysOrg org, SysOrg createdByOrg) {
    if (planCycleId != cycleId.longValue()) {
        throw new BusinessRuleException("PLAN_CYCLE_MISMATCH", "任务周期与计划周期不一致");
    }
    if (planTargetOrgId != org.getId()) {
        throw new BusinessRuleException("PLAN_ORG_MISMATCH", "任务归属组织与计划目标组织不一致");
    }
    // ...
}

// 全局异常处理
@RestControllerAdvice(basePackages = "com.sism.task")
public class TaskExceptionHandler extends GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: {} ({})", ex.getMessage(), ex.getCode());
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationException(AuthorizationException ex) {
        log.warn("权限异常: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("AUTHORIZATION_FAILED", ex.getMessage()));
    }
}
```

---

## 五、性能优化审计

### 🔴 高风险性能问题

#### 1. searchTasks 内存分页未解决
**严重等级:** 🔴 High
**文件:** `TaskApplicationService.java` (Lines 205-243)

**问题描述:** 已在数据库交互审计中详细描述

#### 2. 双重查询问题
**严重等级:** 🟠 Medium
**文件:** `TaskApplicationService.java`

**问题描述:**
- `toCommandResponse` 方法调用 `loadPlanStatus()` 触发额外查询
- 在写操作后会先查找实体，然后再查询 TaskFlatView

**风险影响:**
- 每个写操作都会执行额外查询
- 数据库压力增加
- 响应时间延长

**代码示例:**
```java
private TaskResponse toCommandResponse(StrategicTask task) {
    return TaskResponse.fromEntity(task, loadPlanStatus(task.getId())); // 额外查询
}

private String loadPlanStatus(Long taskId) {
    return jpaTaskRepository.findFlatViewById(taskId) // 数据库查询
            .map(TaskFlatView::getPlanStatus)
            .orElse(StrategicTask.STATUS_DRAFT);
}
```

**建议修复:**
```java
// 方案1: 在创建 TaskResponse 时直接使用关联数据
public static TaskResponse fromEntity(StrategicTask task, String planStatus) {
    TaskResponse response = new TaskResponse();
    response.setId(task.getId());
    response.setName(task.getName());
    // ...
    response.setPlanStatus(planStatus);
    return response;
}

// 方案2: 在保存前加载 planStatus
@Transactional
public TaskResponse createTask(CreateTaskRequest request) {
    // ...
    task.validate();
    taskRepository.save(task);
    
    // 在事务内获取 planStatus
    String planStatus = loadPlanStatusFromPlanId(request.getPlanId());
    publishAndSaveEvents(task);
    return TaskResponse.fromEntity(task, planStatus);
}

private String loadPlanStatusFromPlanId(Long planId) {
    return planRepository.findById(planId)
            .map(Plan::getStatus)
            .orElse(StrategicTask.STATUS_DRAFT);
}

// 方案3: 添加字段到 StrategicTask 并维护一致性
@Column(name = "plan_status", length = 64)
private String planStatus = STATUS_DRAFT;

// 在任务更新时同步 planStatus
public void updatePlan(Long planId) {
    this.planId = planId;
    this.planStatus = loadPlanStatusFromPlanId(planId);
    this.updatedAt = LocalDateTime.now();
}
```

#### 3. 查询方法性能优化空间
**严重等级:** 🟠 Medium
**文件:** `JpaTaskRepositoryInternal.java`

**问题描述:**
- 查询中使用了 `LEFT JOIN FETCH` 可能导致重复数据
- 没有针对高频查询的优化策略
- 缺少查询性能监控和分析

**风险影响:**
- 查询性能下降
- 数据库资源浪费
- 应用响应延迟

**建议修复:**
```java
// 添加查询性能优化
@Query("""
        SELECT t FROM StrategicTask t
        WHERE t.isDeleted = false
          AND t.planId = :planId
        ORDER BY t.sortOrder ASC, t.id ASC
        """)
@QueryHints(value = {
    @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE, value = "100"),
    @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true"),
    @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHE_REGION, value = "taskPlanQueries")
})
List<StrategicTask> findByPlanId(@Param("planId") Long planId);

// 使用实体图优化关联查询
@EntityGraph(attributePaths = {"org", "createdByOrg"})
@Query("""
        SELECT t FROM StrategicTask t
        WHERE t.isDeleted = false
          AND t.org.id = :orgId
        """)
List<StrategicTask> findByOrgIdWithDetails(@Param("orgId") Long orgId);

// 分析慢查询
// 启用 Hibernate 的统计功能
// spring.jpa.properties.hibernate.generate_statistics=true
// spring.jpa.properties.hibernate.session_factory.statistics=true
```

---

## 六、审计总结

### 问题统计

| 严重等级 | 数量 | 类别分布 | 状态 |
|----------|------|----------|------|
| 🔴 Critical | 0 | - | ✅ 已修复 |
| 🔴 High | 1 | 性能 | ❌ 未修复 |
| 🟠 Medium | 6 | 任务管理、安全、数据库、业务逻辑、性能 | 部分修复 |
| 🟡 Low | 4 | 代码质量、架构 | ❌ 未修复 |

### 最紧急修复项

| 优先级 | 问题 | 影响 |
|--------|------|------|
| P0 | searchTasks 内存分页 | 大数据量时服务崩溃风险 |
| P1 | 任务分配和责任人管理缺失 | 业务流程不完整 |
| P1 | validatePlanBinding 直接使用 EntityManager | 架构和可维护性问题 |
| P2 | 任务状态机设计不完整 | 扩展性和维护性问题 |

### 整体评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 安全性 | 🟠 需改进 | 基本权限控制已实现，但读操作权限过于宽松 |
| 可靠性 | ✅ 良好 | 字段持久化问题已解决，基础架构稳定 |
| 性能 | 🔴 需改进 | 内存分页问题严重影响性能 |
| 可维护性 | 🟠 需改进 | 异常处理和架构设计有待优化 |
| 业务完整性 | 🟠 需改进 | 缺少任务分配和责任人管理 |

### 亮点

1. **DDD 分层清晰** - domain/application/infrastructure/interfaces 结构良好
2. **权限控制基础架构已建立** - 实现了组织级别的权限验证
3. **事件驱动设计** - 领域事件发布和存储机制完善
4. **软删除实现** - 使用 `isDeleted` 字段实现软删除，数据可恢复
5. **Repository 接口设计** - 领域层接口定义清晰

### 关键建议

1. **立即修复 searchTasks 内存分页** - 将其改为数据库分页
2. **补充任务分配功能** - 添加强制的责任人管理
3. **优化数据库查询** - 改进 PlanBindingRepository 和查询性能
4. **完善异常处理** - 使用统一的业务异常类
5. **重构状态机设计** - 使用枚举和状态转换记录

---

## 七、修复优先级路线图

### 第一阶段（P0-P1）- 立即修复（0-2周）
1. **修复 searchTasks 内存分页问题** - 改为数据库分页
2. **优化 PlanBindingRepository** - 使用 Spring Data JPA 替代 EntityManager
3. **补充任务分配功能** - 添加责任人字段和分配接口

### 第二阶段（P2）- 短期优化（2-4周）
4. **重构任务状态机** - 使用枚举和状态历史记录
5. **完善异常处理** - 引入业务异常类
6. **改进双重查询问题** - 优化响应构建逻辑

### 第三阶段（P3）- 长期改进（4-8周）
7. **添加任务操作审计** - 记录所有用户操作轨迹
8. **优化查询性能** - 使用二级缓存和查询优化
9. **完善 API 文档** - 补充 Swagger 注解和业务说明

---

**审计完成日期:** 2026-04-06
**下一步行动:** 按修复优先级路线图实施修复，重点关注 P0 和 P1 级问题

