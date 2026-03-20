# `sism-strategy` 与 `sism-task` 职责重构规划书

> 版本：v2.0  
> 创建时间：2026-03-19  
> 状态：待评审

## 1. 背景

当前后端模块已经按照 Maven 多模块形式拆分，但在 `sism-strategy` 与 `sism-task` 之间，存在“数据库主链路清晰、模块边界不够清晰”的问题。

结合现有数据库结构，核心业务链路应理解为：

```text
Cycle -> Plan -> Task -> Indicator Tree(root/child)
Indicator(any node) -> Milestone
```

其中：

- `cycle` 对应周期
- `plan` 对应一批同时下发的计划/容器
- `sys_task` 对应计划下的任务
- `indicator` 对应任务下的指标树
- `indicator_milestone` 对应挂在任意指标节点下的里程碑

从数据库关系看，这条链路是稳定且明确的。但从当前模块实现看：

- `sism-task` 拥有 `Task` 聚合与 `sys_task` 表访问
- `sism-strategy` 拥有 `Cycle / Indicator / Milestone`，并承担部分 `Plan` 编排逻辑
- `Plan` 实体当前位于 `sism-execution`

因此，现阶段的核心问题不是“数据库关系设计错误”，而是“模块职责边界、状态边界、编排边界”需要重新收敛。

同时，根据后续业务规划，系统不仅存在规划链路中的战略任务，还会逐步引入临时任务等非规划任务。因此，`Task` 不能再被简单视为“只服务 `Strategy` 的中间节点”，而应被视为一个可独立演进的任务中心。

## 2. 现状结论

### 2.1 已确认的数据库主链路

根据数据库导出文档：

- `plan.cycle_id` 表示 `Cycle -> Plan`
- `sys_task.plan_id` 表示 `Plan -> Task`
- `indicator.task_id` 表示 `Task -> Root Indicator`
- `indicator.parent_indicator_id` 表示 `Indicator -> Child Indicator`
- `indicator_milestone.indicator_id` 表示 `Indicator(any node) -> Milestone`

因此，里程碑不是只归属于子指标，而是归属于任意指标节点。

### 2.2 当前代码中的模块分工

当前模块大致承担如下职责：

- `sism-task`
  - `StrategicTask` 聚合
  - `sys_task` 读写
  - 任务创建、修改、删除、查询
- `sism-strategy`
  - `Cycle`
  - `Indicator`
  - `Milestone`
  - 计划详情拼装
  - 指标权重校验
  - 计划与指标状态联动
- `sism-execution`
  - 当前持有 `Plan` 实体与仓储
  - 还承载计划报告等执行期能力

### 2.3 当前最主要的问题

当前真正的问题主要有四类。

#### 问题一：`Task` 的状态归属不清

`Task` 一方面被描述为“状态从 `Plan` 获取”，另一方面又在领域模型和接口中拥有独立的 `activate / complete / cancel` 状态流转能力。

这会导致三个结果：

- `Task` 是否拥有独立生命周期无法回答
- `Task` 的状态规则容易与 `Plan` 状态冲突
- 战略任务与未来临时任务是否共享同一状态机无法回答
- 前端和调用方很难判断任务状态应以哪个接口结果为准

#### 问题二：`Plan` 的业务所有权与物理位置不一致

从数据库主链路与业务语义看，`Plan` 更接近“战略规划批次/容器”，天然处于 `Cycle` 与 `Task` 之间。

但当前：

- `Cycle / Indicator / Milestone` 在 `sism-strategy`
- `Task` 在 `sism-task`
- `Plan` 却落在 `sism-execution`

这会导致“链路中间节点”的语义与模块位置脱节。

#### 问题三：`sism-strategy` 对下游查询和联动的职责过重

当前 `sism-strategy` 除了拥有自己的聚合外，还负责：

- 通过 `plan -> task -> indicator` 做详情聚合
- 在计划下发时校验基础任务指标权重
- 在计划状态变化后批量同步指标状态

这些能力并不是错误的，但需要明确这是一种“编排职责”，而不是“重复拥有 Task”。

#### 问题四：`sism-task` 的定位过窄，尚未体现“任务中心”能力

当前 `sism-task` 基本只围绕 `StrategicTask` 展开，导致它看起来像是“从规划链中拆出来的一张表模块”。

这会带来两个后果：

- 从当前视角看，`sism-task` 容易被误判为可并回 `sism-strategy`
- 从未来视角看，临时任务、专项任务等能力缺少稳定落点

## 3. 重构目标

本次重构不追求一次性重建全部模块，而追求以下四个目标：

1. 让每个业务对象只存在一个明确的“拥有模块”
2. 让每条状态规则只存在一个明确的“主导模块”
3. 保留 `sism-task` 作为可演进的任务中心，而不是临时拆分模块
4. 让跨模块查询和编排可保留，但边界明确
5. 让后续接口、权限、前端集成时能够稳定回答“这件事该改哪个模块”

## 4. 目标职责边界

### 4.1 目标业务链路

建议将业务链路统一表述为：

```text
Strategy Context:
  Cycle -> Plan -> Indicator Tree -> Milestone

Task Context:
  Task Center
    - StrategicTask
    - AdhocTask (future)
    - Other task types (future)

完整业务链路:
  Cycle -> Plan -> Task -> Indicator Tree(root/child)
  Indicator(any node) -> Milestone
```

这里需要特别说明：

- `Task` 仍位于 `Plan` 与 `Indicator` 之间，是完整链路中的关键节点
- 但 `Indicator Tree` 是指标域内结构，不应并入 `Task`
- `Milestone` 依附于指标节点，不应提升为 `Task` 或 `Plan` 的直属子对象
- `Task` 在这条链路中既是规划链路节点，也是独立任务中心的一个具体落地对象

### 4.2 目标模块边界

#### `sism-strategy`

建议收敛为“战略规划主链路模块”，负责：

- `Cycle`
- `Plan`
- `Indicator`
- `Milestone`
- 计划下发与审批相关规则
- 指标树规则
- 计划与指标联动规则
- 基于规划主链路的聚合查询
- 面向规划链路的任务装配与读取

边界原则：

- 拥有规划主链路中的骨架对象
- 拥有所有“计划是否可下发、指标如何联动、里程碑如何挂靠”的核心规则
- 可读取 `Task`，但不拥有 `Task`
- 不负责通用任务模型演进

#### `sism-task`

建议收敛为“任务中心模块”，负责：

- `Task` 统一模型与扩展点
- 当前 `StrategicTask`
- 未来 `AdhocTask`
- 任务基础字段维护
- 任务归属组织维护
- 任务类型与分类体系
- 任务生命周期规则
- 任务查询与检索能力
- 跨来源任务的统一视图

边界原则：

- 拥有 `sys_task`
- 长期拥有“任务”这一业务概念，而不仅是一张战略任务表
- 不拥有 `Indicator`
- 不拥有 `Milestone`
- 不拥有计划下发规则
- 不主导规划链路状态联动
- 可以拥有任务自己的生命周期，但不能复制 `Plan` 的下发/审批状态

#### `sism-execution`

建议收敛为“执行期填报与分析支撑模块”，负责：

- `plan_report`
- 进度填报
- 执行期快照/汇总
- 可能的预警、统计输入模型

边界原则：

- 不再作为 `Plan` 的长期业务归属模块
- 可以消费 `Plan`，但不应长期拥有 `Plan` 作为核心聚合

## 5. 关键设计决策

### 5.1 `Task` 是否保留独立状态机

建议结论：

**保留 `Task` 作为独立任务中心，但重构其状态语义，避免与 `Plan` 状态重复。**

原因：

- 后续会引入临时任务等非规划任务，任务域需要具备独立演进能力
- 当前战略任务确实依附于 `Plan`，不能简单复制一套计划状态
- 未来不同任务类型可能需要不同生命周期
- 如果继续把“计划状态”和“任务状态”混为一谈，会长期制造双状态源问题

目标处理方式：

- 区分两类状态：
  - `Plan` 投影状态：反映任务所处规划链路阶段，如 `DRAFT / PENDING / DISTRIBUTED`
  - `Task` 自身状态：反映任务执行或处理生命周期，仅在确有业务需要时启用
- 对当前 `StrategicTask`，短期内以 `Plan` 投影状态为主，冻结继续扩张的任务独立状态接口
- 对未来 `AdhocTask`，允许在 `sism-task` 内定义独立状态机
- 前端接口层明确区分“规划状态”和“任务状态”，避免使用一个字段承载两种语义

### 5.2 `Plan` 是否迁移出 `sism-execution`

建议结论：

**是，但分阶段迁移。**

原因：

- `Plan` 在业务上位于 `Cycle` 与 `Task` 之间
- `Plan` 主要承担规划、下发、审批前置校验、指标联动等职责
- 当前 `sism-strategy` 已经承担大部分 `Plan` 编排逻辑
- 将 `Plan` 归位后，`sism-task` 可以更专注于任务中心建设，而非被迫承接规划语义

迁移策略：

- 第一阶段先统一“语义归属”
- 第二阶段再迁移实体、仓储与应用服务
- 第三阶段再处理依赖回收和模块瘦身

### 5.3 `Milestone` 是否按 `Plan` 建立直接关系

建议结论：

**不新增 `plan_id`，继续保持 `Milestone -> Indicator` 的单一归属。**

原因：

- 数据库已经确认 `indicator_milestone.indicator_id` 是唯一直属外键
- 里程碑需要支持挂在根指标和子指标下
- 若额外引入 `plan_id`，会造成双归属与一致性问题

计划详情若需要里程碑，应通过：

```text
Plan -> Tasks -> Indicators -> Milestones
```

进行聚合查询，而不是改变表结构归属。

### 5.4 `sism-task` 是否应并回 `sism-strategy`

建议结论：

**不建议作为主方案并回。**

原因：

- 后续明确存在临时任务等非规划任务需求
- `Task` 已具备演进为任务中心的业务基础
- 若此时并回，未来仍可能再次拆出，形成反复迁移

仅在以下前提全部成立时，才考虑并回作为备选方案：

- 未来 6-12 个月内确认不存在非规划任务域
- `Task` 永远只作为 `Plan` 的附属节点存在
- 团队确认不建设统一任务中心

## 6. 目标职责矩阵

| 对象/规则 | 当前主要模块 | 目标主要模块 | 说明 |
| --- | --- | --- | --- |
| Cycle | sism-strategy | sism-strategy | 保持不变 |
| Plan 实体 | sism-execution | sism-strategy | 中期迁移 |
| Plan 下发/审批前规则 | sism-strategy | sism-strategy | 保持不变并进一步集中 |
| Task | sism-task | sism-task | 收敛为任务中心核心对象 |
| Task 类型体系 | 分散/弱化 | sism-task | 统一承载 Strategic/Adhoc 等任务分类 |
| Task 状态命令 | sism-task | sism-task（重定义） | 区分计划投影状态与任务自身状态 |
| Indicator | sism-strategy | sism-strategy | 保持不变 |
| Indicator Tree | sism-strategy | sism-strategy | 保持不变 |
| Milestone | sism-strategy | sism-strategy | 保持不变 |
| Plan 详情聚合 | sism-strategy | sism-strategy | 明确为查询/编排职责 |
| 执行填报/报告 | sism-execution | sism-execution | 保持不变 |

## 7. 分阶段实施方案

### 第一阶段：边界澄清期

目标：

- 不大搬代码
- 先统一职责口径
- 明确 `sism-task` 的长期定位
- 优先消除最危险的职责冲突

改动建议：

1. 输出正式职责说明文档，并纳入仓库
2. 明确声明 `sism-task` 的目标是任务中心，而不是战略任务壳模块
3. 梳理任务状态语义，区分：
   - 规划投影状态
   - 任务自身状态
4. 对当前战略任务相关状态接口进行冻结评估：
   - 不立即扩张
   - 逐个确认是保留、改名还是废弃
5. 在代码注释和接口文档中统一说明：
   - `Plan` 状态由规划链路主导
   - `Task` 未来可以拥有自身状态，但不能复制 `Plan` 状态

阶段产出：

- 团队内部对职责边界形成一致认知
- `sism-task` 的长期保留理由清晰
- 新需求不再继续混用计划状态与任务状态

### 第二阶段：应用层收敛期

目标：

- 让 `Plan` 的应用逻辑单点收敛到 `sism-strategy`
- 让 `sism-task` 逐步从“战略任务 CRUD”收敛成“任务中心”

改动建议：

1. 将 `Plan` 的应用服务统一放到 `sism-strategy`
2. 保留 `TaskRepository` 供 `strategy` 查询，但只作为下游依赖
3. 将任务相关的复杂详情查询分两类：
   - 纯任务查询留在 `sism-task`
   - `plan -> task -> indicator -> milestone` 聚合查询放在 `sism-strategy`
4. 明确 `sism-task` 不再新增任何指标、里程碑相关逻辑
5. 为后续 `AdhocTask` 预留模型和接口扩展点

阶段产出：

- `Task` 成为稳定的被依赖模块
- `Plan` 相关逻辑不再分散
- `sism-task` 的演进方向从一开始就与未来需求一致

### 第三阶段：领域模型迁移期

目标：

- 让模块物理位置与业务所有权一致
- 让 `sism-task` 的领域模型具备扩展空间

改动建议：

1. 将 `Plan` 实体与仓储接口从 `sism-execution` 迁移至 `sism-strategy`
2. 将 `sism-execution` 中与计划主链无关但与执行填报相关的能力保留
3. 在 `sism-task` 中抽象统一任务基类、任务类型或任务分类扩展点
4. 清理旧依赖方向，确保：

```text
sism-strategy -> sism-task
sism-execution -> sism-strategy
sism-task 不反向依赖 sism-strategy
```

阶段产出：

- 规划主链对象在物理模块上完成归位
- `sism-execution` 聚焦执行期模型
- `sism-task` 具备承接临时任务的结构基础

### 第四阶段：接口与前端收敛期

目标：

- 对外 API 语义与模块边界一致

改动建议：

1. 收敛 API 文档中的模块语义
2. 为任务状态接口提供迁移说明与命名收敛方案
3. 为前端提供新的查询边界说明：
   - 任务编辑相关接口使用 `task`
   - 规划详情、指标树、里程碑详情使用 `strategy`
   - 执行填报、报告使用 `execution`

阶段产出：

- 前后端协作边界更稳定
- 后续功能开发不再频繁跨模块摸索

## 8. 需要重点避免的反模式

### 8.1 不要把 `Milestone` 提升为 `Task` 或 `Plan` 的直属子对象

原因：

- 会破坏数据库现有归属结构
- 会丢失“任意指标节点均可挂里程碑”的灵活性

### 8.2 不要在 `sism-task` 中继续引入指标树逻辑

原因：

- `Task` 是容器下的任务节点，不是指标树根
- 将指标树规则下沉到 `Task` 会再次扩大耦合

### 8.3 不要把 `sism-task` 退化成“战略任务表 CRUD 模块”

原因：

- 这会削弱它作为任务中心的长期价值
- 未来临时任务落地时仍需再次大改边界

### 8.4 不要让 `sism-execution` 继续增长 `Plan` 主链职责

原因：

- 会使 `Plan` 在规划期与执行期之间反复摇摆
- 会加重后续迁移成本

### 8.5 不要通过冗余外键解决聚合查询问题

原因：

- 比如给 `Milestone` 增加 `plan_id`
- 短期方便查询，长期会制造双写和一致性问题

## 9. 风险与应对

### 风险一：前端依赖旧的任务状态接口与字段语义

应对：

- 先梳理哪些字段表达计划投影状态，哪些字段表达任务自身状态
- 对不清晰的接口先做文档澄清，再做逐步替换
- 保留一个过渡周期
- 同步输出迁移说明

### 风险二：`Plan` 迁移引发模块依赖调整

应对：

- 先迁应用层，再迁实体
- 先改语义边界，再改物理结构
- 每阶段保持 API 兼容

### 风险三：`sism-task` 被过度设计，提前承载过多未来需求

应对：

- 仅为临时任务保留明确扩展点，不一次性引入过重抽象
- 先收敛边界，再扩展模型
- 以实际任务类型落地节奏为准推进

### 风险四：团队继续按旧习惯跨模块加逻辑

应对：

- 在 PR 审查中增加“模块职责检查项”
- 在文档中明确“查询编排”和“领域拥有”的区别

## 10. 验收标准

当以下条件同时满足时，可认为本次职责重构达到预期：

1. 团队可以明确回答以下问题且答案一致：
   - `Task` 状态中哪些属于规划投影，哪些属于任务自身
   - `Plan` 应属于哪个模块
   - `Milestone` 应挂在哪一层
   - `sism-task` 为什么需要长期保留
2. `sism-task` 中不再新增指标、里程碑、计划下发相关逻辑
3. `sism-strategy` 成为规划主链路的唯一编排中心
4. `sism-task` 明确成为任务中心，而不只是战略任务壳模块
5. `sism-execution` 聚焦执行期填报与报告
6. 任务状态接口完成语义澄清与迁移计划

## 11. 推荐的最终职责口径

建议在后续文档、评审、接口说明中统一使用下面这段话：

> SISM 后端的规划主链路为 `Cycle -> Plan -> Task -> Indicator Tree`，其中里程碑挂载在任意指标节点下。  
> `sism-strategy` 拥有规划主链路中的 `Cycle / Plan / Indicator / Milestone` 及其核心规则，并负责跨 `Task` 的规划编排；  
> `sism-task` 作为任务中心，拥有 `Task` 及其类型体系、基础维护能力、生命周期扩展能力，未来承接临时任务等非规划任务；  
> `sism-execution` 聚焦执行期填报、报告与分析支撑，不再长期承担 `Plan` 的主业务所有权。

## 12. 后续建议

建议在本规划书评审通过后，再补充两份配套文档：

1. 《模块边界调整实施清单》
2. 《API 废弃与迁移说明》

这两份文档将分别解决：

- 谁先改、改哪些类、怎么拆阶段
- 前端和联调方如何平滑迁移
