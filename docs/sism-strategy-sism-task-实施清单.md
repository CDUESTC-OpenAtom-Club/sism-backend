# `sism-strategy` 与 `sism-task` 职责重构实施清单

> 版本：v1.1
> 创建时间：2026-03-19
> 更新时间：2026-03-19
> 关联文档：`sism-strategy-sism-task-职责重构规划书.md`
> 状态：✅ 已完成

## 1. 使用方式

本清单不是纯概念说明，而是按以下粒度组织：

- 阶段
- 目标
- 需要修改的类/接口
- 建议动作
- 完成判定

建议执行顺序：

1. 先做接口收敛与语义冻结
2. 再做 `Plan` 归位
3. 再做 `Task` 中心化改造
4. 最后清理旧入口和重复实现

## 2. 总体实施顺序

```text
Phase 0 现状冻结
Phase 1 API 与语义收敛
Phase 2 Plan 从 execution 迁入 strategy
Phase 3 Task 中心化改造
Phase 4 查询与依赖方向收敛
Phase 5 旧入口下线与收尾
```

## 3. Phase 0：现状冻结

### 3.1 目标

- 冻结职责边界
- 避免在重构期间继续扩大重复逻辑
- 为后续迁移建立基线

### 3.2 需要处理的文件

- [sism-strategy-sism-task-职责重构规划书.md](/Users/blackevil/Documents/前端架构测试/sism-backend/docs/sism-strategy-sism-task-职责重构规划书.md)
- [BOUNDED_CONTEXT_MAP.md](/Users/blackevil/Documents/前端架构测试/sism-backend/docs/BOUNDED_CONTEXT_MAP.md)

### 3.3 动作

1. 在架构文档中明确以下口径：
   - `sism-strategy` 是规划主链路中心
   - `sism-task` 是任务中心
   - `sism-execution` 只保留执行期模型
2. 在 PR 评审规则中加入一条：
   - 不允许向 `sism-task` 增加指标、里程碑、计划下发逻辑
3. 在 PR 评审规则中加入一条：
   - 不允许向 `sism-execution` 增加新的 `Plan` 主链职责

### 3.4 完成判定

- 团队已确认文档版本
- 后续开发不再新增跨边界逻辑

## 4. Phase 1：API 与语义收敛

### 4.1 目标

- 明确哪些接口是主入口
- 标记哪些接口是过渡入口
- 先解决最危险的状态语义冲突

### 4.2 需要处理的计划相关接口

#### 主入口保留

- [sism-backend/sism-strategy/src/main/java/com/sism/strategy/interfaces/rest/PlanController.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/interfaces/rest/PlanController.java)

#### 过渡入口冻结/下线候选

- [sism-backend/sism-execution/src/main/java/com/sism/execution/interfaces/rest/PlanController.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-execution/src/main/java/com/sism/execution/interfaces/rest/PlanController.java)
- [sism-backend/sism-execution/src/main/java/com/sism/execution/interfaces/rest/ExecutionPlanController.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-execution/src/main/java/com/sism/execution/interfaces/rest/ExecutionPlanController.java)

### 4.3 需要处理的里程碑相关接口

#### 主入口保留

- [sism-backend/sism-strategy/src/main/java/com/sism/strategy/interfaces/rest/MilestoneController.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/interfaces/rest/MilestoneController.java)

#### 过渡入口冻结/迁移候选

- [sism-backend/sism-execution/src/main/java/com/sism/execution/interfaces/rest/MilestoneController.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-execution/src/main/java/com/sism/execution/interfaces/rest/MilestoneController.java)

### 4.4 需要处理的任务状态接口

- [sism-backend/sism-task/src/main/java/com/sism/task/interfaces/rest/TaskController.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-task/src/main/java/com/sism/task/interfaces/rest/TaskController.java)

重点接口：

- `POST /api/v1/tasks/{id}/activate`
- `POST /api/v1/tasks/{id}/complete`
- `POST /api/v1/tasks/{id}/cancel`

### 4.5 动作

1. 在 OpenAPI/接口文档中明确：
   - `strategy` 的 `PlanController` 是唯一规划计划主入口
   - `strategy` 的 `MilestoneController` 是唯一规划里程碑主入口
2. 将 `execution` 中的两个计划控制器标记为 `legacy` 或 `deprecated`
3. 将 `execution` 中的里程碑控制器标记为过渡接口，不再新增能力
4. 在 `TaskController` 中冻结任务状态接口：
   - 短期不删除
   - 增加文档说明这些接口不代表规划审批状态
5. 在 `TaskResponse` 语义上拆分说明：
   - 当前 `status` 属于计划投影状态还是任务自身状态
   - 若暂时无法拆字段，先在接口文档中说明

### 4.6 建议修改的类

- [sism-backend/sism-task/src/main/java/com/sism/task/application/dto/TaskResponse.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-task/src/main/java/com/sism/task/application/dto/TaskResponse.java)
- [sism-backend/sism-task/src/main/java/com/sism/task/application/TaskApplicationService.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-task/src/main/java/com/sism/task/application/TaskApplicationService.java)
- [sism-backend/sism-task/src/main/java/com/sism/task/domain/StrategicTask.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-task/src/main/java/com/sism/task/domain/StrategicTask.java)

### 4.7 完成判定

- 计划主入口只剩 `strategy.PlanController`
- 里程碑主入口只剩 `strategy.MilestoneController`
- 团队对任务状态接口的语义形成统一说明

## 5. Phase 2：Plan 从 execution 迁入 strategy

### 5.1 目标

- 让 `Plan` 的物理位置和业务所有权一致
- 清理 `strategy` 对 `execution.Plan` 的反向依赖

### 5.2 需要迁移的核心类

#### 从 `execution` 迁出

- [sism-backend/sism-execution/src/main/java/com/sism/execution/domain/model/plan/Plan.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-execution/src/main/java/com/sism/execution/domain/model/plan/Plan.java)
- [sism-backend/sism-execution/src/main/java/com/sism/execution/domain/model/plan/PlanLevel.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-execution/src/main/java/com/sism/execution/domain/model/plan/PlanLevel.java)
- [sism-backend/sism-execution/src/main/java/com/sism/execution/domain/model/plan/PlanStatus.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-execution/src/main/java/com/sism/execution/domain/model/plan/PlanStatus.java)
- [sism-backend/sism-execution/src/main/java/com/sism/execution/domain/event/PlanCreatedEvent.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-execution/src/main/java/com/sism/execution/domain/event/PlanCreatedEvent.java)
- [sism-backend/sism-execution/src/main/java/com/sism/execution/domain/repository/PlanRepository.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-execution/src/main/java/com/sism/execution/domain/repository/PlanRepository.java)
- [sism-backend/sism-execution/src/main/java/com/sism/execution/infrastructure/persistence/JpaPlanRepository.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-execution/src/main/java/com/sism/execution/infrastructure/persistence/JpaPlanRepository.java)
- [sism-backend/sism-execution/src/main/java/com/sism/execution/infrastructure/persistence/JpaPlanRepositoryInternal.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-execution/src/main/java/com/sism/execution/infrastructure/persistence/JpaPlanRepositoryInternal.java)

#### 在 `strategy` 中更新依赖的类

- [sism-backend/sism-strategy/src/main/java/com/sism/strategy/application/PlanApplicationService.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/application/PlanApplicationService.java)
- [sism-backend/sism-strategy/src/main/java/com/sism/strategy/application/BasicTaskWeightValidationService.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/application/BasicTaskWeightValidationService.java)

### 5.3 动作

1. 在 `sism-strategy` 新建 `plan` 领域包，迁入：
   - `Plan`
   - `PlanLevel`
   - `PlanStatus`
   - `PlanCreatedEvent`
2. 在 `sism-strategy` 新建 `PlanRepository` 及其 JPA 实现
3. 修改 `PlanApplicationService` 中的 import，切换到 `strategy` 包下的 `Plan`
4. 修改 `BasicTaskWeightValidationService` 与其他依赖 `PlanStatus` 的代码
5. 更新 `sism-strategy/pom.xml`
   - 去掉对 `sism-execution` 中 `Plan` 模型的依赖需求
6. 更新 `sism-execution/pom.xml`
   - 如后续仍需读 `Plan`，改为依赖 `sism-strategy`

### 5.4 迁移顺序

1. 先复制类到 `strategy`
2. 再修改 `strategy` 内部引用
3. 再修改 `execution` 内部引用
4. 最后删除 `execution` 中旧的 `Plan` 相关实现

### 5.5 完成判定

- `Plan` 不再定义在 `sism-execution`
- `sism-strategy` 不再 import `com.sism.execution.domain.model.plan.*`
- `execution` 仅消费 `Plan`，不再拥有 `Plan`

## 6. Phase 3：Task 中心化改造

### 6.1 目标

- 让 `sism-task` 从“战略任务 CRUD 模块”升级为“任务中心”
- 为未来 `AdhocTask` 留出结构空间

### 6.2 当前要修改的核心类

- [sism-backend/sism-task/src/main/java/com/sism/task/domain/StrategicTask.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-task/src/main/java/com/sism/task/domain/StrategicTask.java)
- [sism-backend/sism-task/src/main/java/com/sism/task/domain/TaskType.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-task/src/main/java/com/sism/task/domain/TaskType.java)
- [sism-backend/sism-task/src/main/java/com/sism/task/application/TaskApplicationService.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-task/src/main/java/com/sism/task/application/TaskApplicationService.java)
- [sism-backend/sism-task/src/main/java/com/sism/task/application/dto/CreateTaskRequest.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-task/src/main/java/com/sism/task/application/dto/CreateTaskRequest.java)
- [sism-backend/sism-task/src/main/java/com/sism/task/application/dto/TaskResponse.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-task/src/main/java/com/sism/task/application/dto/TaskResponse.java)
- [sism-backend/sism-task/src/main/java/com/sism/task/interfaces/rest/TaskController.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-task/src/main/java/com/sism/task/interfaces/rest/TaskController.java)

### 6.3 建议新增的类

建议先加轻量扩展点，不要一次性过度抽象。

可选新增：

- `com.sism.task.domain.TaskCategory`
  - 用于区分 `STRATEGIC / ADHOC / ...`
- `com.sism.task.domain.TaskSource`
  - 用于区分任务来源，如 `PLAN / MANUAL / SYSTEM`
- `com.sism.task.application.dto.TaskSummaryResponse`
  - 为统一任务列表准备轻量视图

### 6.4 动作

1. 为 `TaskResponse` 明确两个层面的状态字段
   - `planStatus`：规划投影状态
   - `taskStatus`：任务自身状态
2. 短期内保留 `StrategicTask`，但将其表述为任务中心中的一种任务
3. 在 `CreateTaskRequest` 中为未来任务分类预留字段
   - 若暂不开放给前端，可先内部预留
4. 在 `TaskApplicationService` 中梳理命令边界：
   - 基础编辑命令保留
   - 与规划审批重复的状态命令冻结
5. 在 `TaskController` 中为未来统一任务接口预留命名空间
   - 当前可不拆新 controller
   - 但后续新增任务类型时不再以“战略任务专用”思路扩写

### 6.5 完成判定

- `sism-task` 文档和代码不再把自己描述成仅服务战略任务
- 任务查询 DTO 可以区分 `planStatus` 与 `taskStatus`
- 新增临时任务时无需再重拆模块

## 7. Phase 4：查询与依赖方向收敛

### 7.1 目标

- 明确什么查询放 `strategy`
- 明确什么查询放 `task`
- 清理重复聚合逻辑

### 7.2 保留在 `strategy` 的查询

- `Plan` 详情查询
- `Plan -> Task -> Indicator -> Milestone` 聚合查询
- 指标树查询
- 里程碑按指标节点查询

涉及类：

- [sism-backend/sism-strategy/src/main/java/com/sism/strategy/application/PlanApplicationService.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/application/PlanApplicationService.java)
- [sism-backend/sism-strategy/src/main/java/com/sism/strategy/application/StrategyApplicationService.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/application/StrategyApplicationService.java)
- [sism-backend/sism-strategy/src/main/java/com/sism/strategy/application/MilestoneApplicationService.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/application/MilestoneApplicationService.java)

### 7.3 保留在 `task` 的查询

- 纯任务列表
- 按 `planId / cycleId / orgId / taskType` 查询任务
- 统一任务检索

涉及类：

- [sism-backend/sism-task/src/main/java/com/sism/task/application/TaskApplicationService.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-task/src/main/java/com/sism/task/application/TaskApplicationService.java)
- [sism-backend/sism-task/src/main/java/com/sism/task/infrastructure/persistence/JpaTaskRepositoryInternal.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-task/src/main/java/com/sism/task/infrastructure/persistence/JpaTaskRepositoryInternal.java)

### 7.4 动作

1. 保持依赖方向：
   - `strategy -> task`
   - `execution -> strategy`
   - `task` 不反向依赖 `strategy`
2. 不在 `task` 中新增 `indicatorRepository`、`milestoneRepository`
3. 不在 `strategy` 中复制纯任务搜索接口
4. 对重复查询做分类：
   - 面向规划聚合的留 `strategy`
   - 面向任务中心的留 `task`

### 7.5 完成判定

- 模块依赖方向稳定
- 任务中心和规划中心的查询职责不再混用

## 8. Phase 5：旧入口下线与收尾 ✅ 已完成

### 8.1 目标

- 下线重复控制器
- 清理历史遗留接口
- 完成最终文档收口

### 8.2 已清理的文件 (2026-03-19)

#### 计划旧入口 (已删除)

- `PlanController.java` ✅ 已删除
- `ExecutionPlanController.java` ✅ 已删除
- `ExecutionApplicationService.java` ✅ 已删除

#### 里程碑旧入口 (已删除)

- `MilestoneController.java` ✅ 已删除
- `MilestoneApplicationService.java` ✅ 已删除
- `Milestone.java` ✅ 已删除
- `ExecutionMilestoneRepository.java` ✅ 已删除
- `JpaExecutionMilestoneRepository.java` ✅ 已删除
- `JpaExecutionMilestoneRepositoryInternal.java` ✅ 已删除
- `JpaMilestoneRepositoryInternal.java` ✅ 已删除
- `CreateMilestoneRequest.java` ✅ 已删除
- `MilestoneResponse.java` ✅ 已删除
- `UpdateMilestoneRequest.java` ✅ 已删除

### 8.3 动作记录

1. ✅ 旧入口标记为 deprecated (之前已完成)
2. ✅ 确认前端已切换到主入口 (`/api/tasks`, `/api/plans`, `/api/milestones`)
3. ✅ 删除旧控制器与过渡服务 (2026-03-19)
4. ✅ 更新本文档

### 8.4 完成判定

- ✅ `execution` 中不再保留 `Plan` 与 `Milestone` 的重复管理入口
- ✅ 对外 API 语义清晰一致

## 9. 按类分配的改动清单

### 9.1 `sism-strategy`

保留并增强：

- `CycleApplicationService`
- `PlanApplicationService`
- `StrategyApplicationService`
- `MilestoneApplicationService`
- `BasicTaskWeightValidationService`
- `Indicator`
- `Milestone`

新增或迁入：

- `Plan`
- `PlanLevel`
- `PlanStatus`
- `PlanRepository`
- `JpaPlanRepository`
- `JpaPlanRepositoryInternal`

### 9.2 `sism-task`

保留并重构：

- `StrategicTask`
- `TaskApplicationService`
- `TaskController`
- `TaskRepository`
- `JpaTaskRepository`
- `JpaTaskRepositoryInternal`
- `TaskResponse`
- `CreateTaskRequest`
- `TaskType`

新增：

- `TaskCategory` 或等价分类枚举
- `planStatus/taskStatus` 语义拆分
- 为 `AdhocTask` 预留的扩展点

### 9.3 `sism-execution`

保留：

- `PlanReport`
- `PlanReportRepository`
- `ReportApplicationService`
- `ReportController`

迁出或下线：

- `Plan`
- `PlanRepository`
- `JpaPlanRepository`
- `ExecutionApplicationService`
- `execution` 下的 `PlanController`
- `execution` 下的 `MilestoneController`
- `execution` 下重复的里程碑模型与仓储

## 10. 最小可执行批次

如果希望降低风险，建议按 3 个最小批次执行。

### 批次 A：只收敛入口与文档

内容：

- 标记旧计划/里程碑入口
- 澄清任务状态语义
- 更新文档

不动：

- 实体位置
- 模块依赖

### 批次 B：迁移 `Plan`

内容：

- 把 `Plan` 及其仓储从 `execution` 迁到 `strategy`
- 修改 `strategy` 内部 import 与依赖

不动：

- `Task` 中心化抽象

### 批次 C：任务中心化

内容：

- 拆分 `planStatus / taskStatus`
- 为临时任务预留结构
- 清理旧任务状态接口

## 11. 执行建议

建议按下面顺序开实际任务单：

1. 任务单 01：冻结旧计划/里程碑入口并更新接口文档
2. 任务单 02：迁移 `Plan` 相关领域模型到 `sism-strategy`
3. 任务单 03：清理 `strategy` 对 `execution.Plan` 的依赖
4. 任务单 04：重构 `TaskResponse` 状态字段语义
5. 任务单 05：冻结或改造 `TaskController` 状态命令接口
6. 任务单 06：为 `sism-task` 增加任务分类扩展点
7. 任务单 07：下线 `execution` 中重复的 `Plan/Milestone` 入口

## 12. 交付标准

当以下条件满足时，可视为本清单执行完成：

1. `Plan` 已归位到 `sism-strategy`
2. `Task` 已明确成为任务中心的核心对象
3. `Milestone` 只保留 `Indicator` 归属，不新增 `plan_id`
4. `execution` 不再承担规划主链入口
5. 前端可以清晰区分：
   - 任务接口
   - 规划接口
   - 执行接口
