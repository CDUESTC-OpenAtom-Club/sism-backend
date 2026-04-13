# 审计报告：sism-task 模块（任务管理）

**审计日期:** 2026-04-12
**审计范围:** 26个Java源文件，涵盖任务CRUD、状态管理、查询。

---

## 一、Critical 严重 (共4个)

### C-01. 任务状态字段 @Transient，状态变更从未持久化
**文件:** `domain/StrategicTask.java:81-82`
**描述:** `status` 字段标为 `@Transient`，所有状态转换操作（activate/complete/cancel）仅修改内存字段。save后不写数据库，下次加载时 `@PostLoad` 重置为 DRAFT。**整个任务生命周期状态管理完全失效。**
**修复建议:** 添加Flyway迁移 `ALTER TABLE sys_task ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'`，移除 `@Transient`。
**状态:** 已修复（2026-04-12）。`StrategicTask.status` 已改为持久化列映射，状态流转不再依赖 `@Transient/@PostLoad`。

### C-02. 原生查询缺少任务状态列，读取路径状态信息丢失
**文件:** `infrastructure/persistence/JpaTaskRepositoryInternal.java:93-251`
**描述:** 所有原生投影查询只选取 `planStatus`，完全没有选取任务自身 `status`。`TaskFlatView.getTaskStatus()` 永远为null，`defaultStatus()` 默认 "DRAFT"。
**修复建议:** 各原生查询添加 `t.status AS taskStatus`。
**状态:** 已修复（2026-04-12）。任务投影查询已补 `taskStatus` 列。

### C-03. taskStatus 过滤条件硬编码为 'DRAFT'，搜索过滤完全无效
**文件:** `infrastructure/persistence/JpaTaskRepositoryInternal.java:201,216`
**描述:** `LOWER('DRAFT') = LOWER(:taskStatus)` 是字面量比较，无论传什么参数都只匹配 DRAFT。
**修复建议:** 配合C-01修复后改为 `LOWER(t.status) = LOWER(:taskStatus)`。
**状态:** 已修复（2026-04-12）。搜索条件已改为基于任务自身状态过滤。

### C-04. findById 不过滤已删除记录，可操作已软删除的任务
**文件:** `infrastructure/persistence/JpaTaskRepositoryInternal.java`
**描述:** 继承的 `findById` 无 `isDeleted = false` 条件。攻击者可重新加载和修改已删除任务。
**修复建议:** 覆盖 `findById` 添加 `AND t.isDeleted = false`。
**状态:** 已修复（2026-04-12）。仓储已覆盖 `findById` 并过滤软删除记录。

---

## 二、High 高 (共4个)

### H-01. 两个同名 TaskType 枚举类
**文件:** `domain/TaskType.java` vs `domain/enums/TaskType.java`
**描述:** 完全相同内容的两个枚举，后者标注 "Legacy alias"。违反DRY，修改时容易遗漏。
**修复建议:** 删除 `domain.enums.TaskType`，统一引用。
**状态:** 已修复（2026-04-12）。重复枚举已删除。

### H-02. updateTask 直接使用 setter 绕过领域逻辑
**文件:** `application/TaskApplicationService.java:118-129`
**描述:** 直接调用 `task.setTaskType()`/`setPlanId()` 等 setter，破坏DDD聚合根封装。
**修复建议:** 在 `StrategicTask` 添加 `reassign()` 领域方法含业务校验。
**状态:** 已修复（2026-04-12）。`updateTask` 已改为走聚合根 `reassign()` 和显式领域方法。

### H-03. getTaskById 绕过领域仓储使用内部JPA接口
**文件:** `application/TaskApplicationService.java:195-198`
**描述:** 直接注入 `JpaTaskRepositoryInternal`（基础设施层）而非 `TaskRepository`（领域层），违反DDD分层。
**修复建议:** 通过 `TaskRepository` 接口提供查询方法。
**状态:** 已修复（2026-04-12）。应用服务已通过 `TaskRepository` 暴露的投影查询读取任务。

### H-04. getTasksByOrgId 缺少权限过滤 — 水平越权
**文件:** `interfaces/rest/TaskController.java:213-221`
**描述:** 直接返回指定组织所有任务，未经过 `filterTasksByPermission`。非管理员可查看任意组织任务。
**修复建议:** 添加 `filterTasksByPermission` 调用。
**状态:** 已修复（2026-04-12）。控制器已对 `/by-org/{orgId}` 响应做权限过滤。

---

## 三、Medium 中等 (共7个)

| # | 文件:行号 | 问题 | 类别 |
|---|---|---|---|
| M-01 | `TaskApplicationService.java:202-206` | 部分缓解（2026-04-13）：`getAllTasks()` 已改为走扁平投影查询，避免全量实体加载；但接口仍保持“返回全部任务”的既有契约，未引入分页 | 性能 |
| M-02 | `TaskApplicationService.java:403-413` | 已修复（2026-04-13）：领域事件改为事务提交后再发布，回滚不会向消费者暴露虚假事件 | 架构 |
| M-03 | `TaskApplicationService.java:432-446` | 已修复（2026-04-13）：非法 sortBy 改为显式抛错，不再静默回退默认排序 | 代码质量 |
| M-04 | `TaskResponse.java:83` | 已修复（2026-04-13）：错误消息已统一为中文表述 | 代码质量 |
| M-05 | TaskSummaryView + AdhocScopeType + AdhocTaskStatus | 已修复（2026-04-13）：未使用接口和枚举已删除 | 死代码 |
| M-06 | `StrategicTask.java:85-86` | 已修复（2026-04-13）：实体级 `@Formula` 已移除，命令响应改为优先回读扁平投影，不再在实体加载时附带计划状态子查询 | 性能 |
| M-07 | `TaskController.java:88` | 已修复（2026-04-13）：非法 taskType 参数改为明确 400 错误 | Bug |

---

## 四、Low 低 (共3个)

| # | 文件:行号 | 问题 |
|---|---|---|
| L-01 | `StrategicTask.java:173` | 已修复（2026-04-13）：空值判断已统一为 `== null` | 风格 |
| L-02 | `TaskApplicationService.java:191` | 已修复（2026-04-13）：无事件时不再执行空发布流程 | 代码质量 |
| L-03 | `TaskApplicationService.java:337-351` | 已修复（2026-04-13）：未使用私有方法已删除 | 死代码 |

---

## 汇总统计

| 严重性 | 数量 |
|--------|------|
| **Critical** | 4 |
| **High** | 4 |
| **Medium** | 7 |
| **Low** | 3 |
| **总计** | **18** |

**最优先修复:** C-01/C-02/C-03 相互关联，共同导致任务状态管理完全失效，应作为整体一次性修复。
