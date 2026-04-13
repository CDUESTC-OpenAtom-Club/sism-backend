# 第二轮审计报告：sism-task（任务管理）

**审计日期:** 2026-04-13
**范围:** 22 个 Java 源文件全面复检
**参照:** 第一轮报告 `09-sism-task.md` (2026-04-12)

---

## 修复总览

| 指标 | 数值 |
|------|------|
| 第一轮问题总数 | 18 |
| 已确认修复 | **17** (94.4%) |
| 部分缓解 | **1** (5.6%) |
| 第二轮新发现 | **7** |

---

## A. 第一轮问题修复状态

### Critical (4项) — 全部修复 ✅

| # | 问题 | 验证 |
|---|------|------|
| C-01 | 任务状态 @Transient | `StrategicTask:77-78` 已改为持久化列 |
| C-02 | 原生查询缺任务状态列 | 所有查询含 `COALESCE(t.status, 'DRAFT')` |
| C-03 | taskStatus 硬编码 DRAFT | 使用 `LOWER(COALESCE(t.status, 'DRAFT')) = LOWER(:taskStatus)` |
| C-04 | findById 不过滤已删除 | 已添加 `AND t.isDeleted = false` |

### High (4项) — 全部修复 ✅

| # | 问题 | 验证 |
|---|------|------|
| H-01 | 两个同名 TaskType 枚举 | 仅存 `com.sism.task.domain.TaskType` |
| H-02 | updateTask 使用 setter | 改用 `task.updateName()`/`task.updateDesc()`/`task.reassign()` |
| H-03 | getTaskById 绕过领域仓储 | 改用 `taskRepository.findFlatViewById()` |
| H-04 | getTasksByOrgId 缺权限过滤 | 调用 `filterTasksByPermission()` |

### Medium (7项) — 6 修复 + 1 部分缓解

| # | 状态 | 说明 |
|---|------|------|
| M-01 | ⚠️ 部分缓解 | `getAllTasks()` 改用扁平投影但无分页 |
| M-02~M-07 | ✅ 修复 | 事务后发布/非法 sortBy 抛异常/统一中文/删除未用接口/移除 @Formula/参数校验 |

### Low (3项) — 全部修复 ✅

---

## B. 第二轮新发现问题

### NH-01. [HIGH] getPlanStatus() 硬编码返回 DRAFT
**文件:** `domain/StrategicTask.java:80-82`

```java
public String getPlanStatus() {
    return TaskStatus.DRAFT.value();  // 永远返回 DRAFT！
}
```

`TaskResponse.fromEntity()` 回退路径使用此方法，命令路径返回的任务 planStatus 永远是 "DRAFT"，可能误导前端判断。

**最优解:**
```java
// 方案一：移除方法，fromEntity 不设置 planStatus
// 方案二：标记为 UNKNOWN
public String getPlanStatus() {
    return "UNKNOWN";  // 明确表示命令路径无法获取真实状态
}

// TaskResponse.fromEntity:
.planStatus("UNKNOWN")  // 或前端从查询路径获取
```

### NM-01. [MEDIUM] 查询方法缺少 @Transactional(readOnly = true)
**文件:** `TaskApplicationService.java:198-274`

`getTasksByOrgId`、`getTasksByCreatedByOrgId`、`getTasksByPlanId`、`getTasksByType`、`getTasksByPlanIdAndCycleId` 均无 `@Transactional(readOnly = true)`。

**最优解:**
```java
@Transactional(readOnly = true)
public List<TaskFlatView> getTasksByOrgId(Long orgId) { ... }

@Transactional(readOnly = true)
public List<TaskFlatView> getTasksByType(String taskType) { ... }
```

### NM-02. [MEDIUM] validatePlanBinding 硬编码跨模块业务规则
**文件:** `TaskApplicationService.java:285-325`

硬编码 `"FUNC_TO_COLLEGE"`、`"STRAT_TO_FUNC"` 字符串和 OrgType 比较。

**最优解 — 策略接口：**
```java
// 定义在 shared-kernel 的跨上下文接口
public interface PlanAssignmentPolicy {
    boolean canAssignToOrg(String planLevel, OrgType orgType);
}

// Task 模块注入
@RequiredArgsConstructor
public class TaskApplicationService {
    private final PlanAssignmentPolicy planAssignmentPolicy;

    private void validatePlanBinding(StrategicTask task) {
        if (!planAssignmentPolicy.canAssignToOrg(planLevel, task.getOrg().getType())) {
            throw new BusinessException("Invalid assignment");
        }
    }
}
```

### NM-03. [MEDIUM] 6参数与11参数原生查询行为不一致
**文件:** `JpaTaskRepositoryInternal.java:126-159` vs `161-266`

6 参数版本缺少 `accessibleOrgId`、`planStatus`、`taskStatus` 过滤，且参数处理风格不一致（`CAST` vs `COALESCE`）。

**最优解:** 合并为单一带可选参数的查询方法：
```java
// 使用 JPA Specification 或 MyBatis 动态 SQL
Page<TaskFlatView> findByCriteria(TaskQueryCriteria criteria, Pageable pageable);
```

### NM-04. [MEDIUM] TaskResponse.fromEntity() 可能触发 LazyInitializationException
`task.getOrg().getId()` 在事务外调用 LAZY 关联。

**最优解:** 在查询方法上确保 `@Transactional(readOnly = true)` 或改用 `@EntityGraph`：
```java
@EntityGraph(attributePaths = {"org", "createdByOrg"})
Optional<StrategicTask> findById(Long id);
```

### NL-01. [LOW] StrategicTask 暴露 public setter
`setPlanId`、`setCycleId`、`setTaskType`、`setOrg`、`setCreatedByOrg`、`setId` 等公共 setter 绕过领域方法。

### NL-02. [LOW] TaskNameView/TaskNameTypeView 投影不含 status 字段

---

## C. 总结

| 严重度 | 第一轮 | 第二轮新发现 |
|--------|--------|-------------|
| Critical | 4 (✅ 全部修复) | 0 |
| High | 4 (✅ 全部修复) | 1 |
| Medium | 7 (6✅ + 1⚠️) | 4 |
| Low | 3 (✅ 全部修复) | 2 |
| **总计** | **18** | **7** |

**模块评级:** ⭐⭐⭐⭐ (4/5) — 修复率高，新发现多为架构设计问题。
