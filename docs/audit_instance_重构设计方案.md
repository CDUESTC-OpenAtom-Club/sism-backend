# audit_instance 重构设计方案

## 1. 目标

本方案只讨论设计，不包含数据库变更执行，不包含生产数据修改。

目标是把 `audit_instance` 从“字段语义漂移、双轨定位并存、实例标识不够收敛”的状态，收敛成一张职责明确的“审批实例主表”。

本次设计遵循以下原则：

1. 不改数据库现状，只定义目标模型与代码使用方式。
2. 审批实例表只记录流程级事实，不承担节点级细节。
3. 一个业务语义只保留一套主字段，不长期维持双轨字段。
4. 审批实例作为聚合根，负责表达“当前流程走到哪里、对应什么业务对象、最终结果是什么”。

## 2. 表职责定义

`audit_instance` 的唯一职责应为：

- 记录某个业务对象发起的一次审批流程
- 记录该流程使用了哪条审批流模板
- 记录当前流程整体状态
- 记录当前流程推进到哪个节点
- 记录流程发起人、发起组织、启动时间、结束时间和最终结果

它不应承担以下职责：

- 保存节点级审批人细节
- 用多套字段重复表达当前节点位置
- 用兼容字段重复表达业务对象标识

## 3. 目标字段模型

### 3.1 保留并作为主流程字段使用

以下字段建议作为主流程白名单字段：

- `id`
  - 审批实例主键

- `flow_def_id`
  - 审批实例所属流程模板 ID
  - 关联 `audit_flow_def.id`
  - 必须保留，作为实例与流程模板的正式关联键

- `entity_type`
  - 业务实体类型
  - 例如：`INDICATOR`、`PLAN_REPORT`

- `entity_id`
  - 业务实体主键
  - 配合 `entity_type` 一起唯一确定审批对象

- `status`
  - 流程实例整体状态
  - 这是实例级唯一状态字段

- `requester_id`
  - 发起人用户 ID

- `requester_org_id`
  - 发起人所属组织 ID

- `started_at`
  - 流程开始时间

- `completed_at`
  - 流程结束时间

- `is_deleted`
  - 逻辑删除标记

- `created_at`
  - 实例记录创建时间

- `updated_at`
  - 实例记录更新时间

### 3.2 明确视为重复或不再推荐使用

- `current_step_id`
  - 当前无保留必要
  - 当前节点应通过节点实例表反查，而不是由实例表保存

- `current_step_index`
  - 当前无保留必要
  - 当前节点应通过 `audit_step_instance` 中状态为 `PENDING` 的记录确定

- `biz_id`
  - 与 `entity_type + entity_id` 的语义重叠
  - 如果不是外部系统对接的正式业务编号，则不应作为主流程字段继续使用

- `created_by`
  - 与 `requester_id` 语义重复
  - 目标设计中应统一以 `requester_id` 表示发起人

- `title`
  - 可通过业务对象动态查询得到
  - 例如直接查询 plan 名称、指标名称
  - 不应在实例表冗余保存

- `result`
  - 与实例最终状态和节点意见语义重叠
  - 当前无保留必要

### 3.3 当前实例表应聚焦保存的最小事实

实例表只应保留：

- 它走的是哪条流程模板：`flow_def_id`
- 它审批的是谁：`entity_type + entity_id`
- 它是谁发起的：`requester_id + requester_org_id`
- 它当前整体处于什么状态：`status`
- 它何时开始、何时结束：`started_at + completed_at`
- 它是否逻辑删除：`is_deleted`

## 4. 实例状态模型设计

`audit_instance` 只保留一个实例状态字段：`status`

目标状态集合建议为：

- `DRAFT`
  - 草稿态
  - 尚未正式发起审批

- `IN_REVIEW`
  - 审批中
  - 当前已有待处理节点

- `APPROVED`
  - 实例审批通过

- `REJECTED`
  - 实例审批驳回

- `WITHDRAWN`
  - 发起人撤回

本设计中不建议：

- 用 `PENDING` 和 `IN_REVIEW` 同时表示实例待审批
- 用 `STATUS_PENDING = IN_REVIEW` 这类别名方式长期存在

原因：

- 实例状态应直接表达业务含义
- 节点级的 `PENDING` 和实例级的“审批中”不是同一层语义
- 用别名会导致前后端、数据库、文档长期混乱

## 5. 业务对象标识设计

### 5.1 `entity_type + entity_id` 作为唯一主标识

目标设计建议明确：

- `entity_type`
- `entity_id`

这两个字段共同组成实例所对应的业务对象标识。

例如：

- `INDICATOR + 30058`
- `PLAN_REPORT + 108`

这样可以直接表达：

- 哪类业务对象进入审批
- 哪个具体对象正在审批

### 5.2 `biz_id` 不再作为主标识字段

如果 `biz_id` 只是历史兼容或旧接口遗留字段，那么它不应继续作为主流程判断字段。

目标设计建议：

- 主流程只认 `entity_type + entity_id`
- `biz_id` 视为历史兼容字段
- 后续如果没有明确外部系统依赖，可以逐步退出主模型

## 6. 当前节点定位设计

### 6.1 当前节点不在实例表冗余保存

当前实例表里同时有：

- `current_step_index`
- `current_step_id`

这是明显的双轨设计。

目标设计建议：

- `audit_instance` 不再保存“当前节点位置”字段
- 当前节点统一通过 `audit_step_instance` 反查

原因：

- 当前节点本来就是节点实例层的事实
- 实例表只需要表达流程整体状态
- 在实例表保存 `current_step_index` 或 `current_step_id` 都会带来同步一致性问题

### 6.2 当前节点详情由步骤实例表负责

真正的节点详情由 `audit_step_instance` 承担：

- 当前节点处理人
- 当前节点状态
- 当前节点处理意见

这样职责边界更清晰。

## 7. 发起人字段设计

当前表中存在：

- `requester_id`
- `requester_org_id`
- `created_by`

目标设计建议：

- `requester_id`
  - 作为发起人唯一用户标识

- `requester_org_id`
  - 作为发起时所在组织标识

- `created_by`
  - 视为与 `requester_id` 重复
  - 不再纳入目标主模型

这样可以避免：

- 发起人字段双轨并存
- `created_by` 和 `requester_id` 不一致

## 8. 时间字段设计

### 8.1 保留

- `started_at`
  - 流程正式启动时间

- `completed_at`
  - 流程结束时间

- `created_at`
  - 记录创建时间

- `updated_at`
  - 记录更新时间

### 8.2 时间语义建议

- 创建实例记录时：
  - `created_at` 有值

- 正式发起审批时：
  - `started_at` 有值

- 流程结束时：
  - `completed_at` 有值

- 任意状态变更时：
  - `updated_at` 更新

## 9. 结果字段设计

### 9.1 `title`

目标设计中不建议保留 `title`。

原因：

- 它可以通过业务对象动态查询得到
- 例如直接查询 plan 名称、指标名称
- 保留它只会形成实例表冗余快照

### 9.2 `result`

目标设计中不建议保留 `result`。

原因：

- 实例最终状态已经由 `status` 表达
- 具体意见应由节点实例表承担
- 若需要展示结论，可由最后一个已处理节点的意见动态拼装

## 10. 目标代码模型建议

如果只从代码设计出发，不改数据库，`AuditInstance` 应按以下语义重构：

- 主字段：
  - `id`
  - `flowDefId`
  - `entityType`
  - `entityId`
  - `status`
  - `requesterId`
  - `requesterOrgId`
  - `startedAt`
  - `completedAt`
  - `isDeleted`
  - `createdAt`
  - `updatedAt`

- 非主流程字段：
  - 无

- 不再纳入代码主模型：
  - `currentStepId`
  - `currentStepIndex`
  - `bizId`
  - `createdBy`
  - `title`
  - `result`

## 11. 与 `audit_step_instance` 的边界

`audit_instance` 负责：

- 流程整体状态
- 发起人是谁
- 当前审批针对哪个业务对象
- 流程最终是否通过、驳回或撤回

`audit_step_instance` 负责：

- 当前走到哪个节点
- 当前步骤由谁处理
- 节点处理意见
- 节点处理时间
- 节点状态
- 节点与模板步骤的映射

因此，实例表不应重复存储节点级明细。

## 12. 对当前系统的实际影响

如果未来按此设计推进，代码层需要改动的点主要有：

1. `AuditInstance` 实体映射
2. `WorkflowApplicationService.startAuditInstance(...)`
3. `AuditInstance.approve(...)`
4. `AuditInstance.reject(...)`
5. 审批详情 VO 中当前步骤的拼装逻辑

但这些属于后续实现，不属于本设计文件的执行范围。

## 13. 本次设计结论

对 `audit_instance` 的目标设计结论如下：

1. `entity_type + entity_id` 作为唯一业务对象标识，`biz_id` 视为冗余兼容字段，不再纳入目标主模型。
2. `flow_def_id` 必须保留，作为实例与流程模板的正式关联键。
3. `current_step_index`、`current_step_id` 都不再作为实例表目标主模型字段保留，当前节点统一通过 `audit_step_instance` 反查。
4. `requester_id`、`requester_org_id` 保留，`created_by` 视为与发起人语义重复，不再纳入目标主模型。
5. `status` 是实例级唯一状态字段，目标状态收敛为 `DRAFT / IN_REVIEW / APPROVED / REJECTED / WITHDRAWN`。
6. `title` 和 `result` 视为冗余展示字段，不再纳入目标主模型。
7. `started_at`、`completed_at`、`created_at`、`updated_at` 保留，并明确分工。

## 14. 建议的下一步

建议后续按顺序推进，不直接跳到数据库修改：

1. 先确认这份目标设计
2. 再收口 `AuditInstance` 的代码主模型
3. 再同步 `WorkflowApplicationService` 的实例状态和当前节点推进逻辑
4. 最后才决定是否做数据库迁移与历史字段清理

这样风险最低，也符合你当前“不改数据库，只先改设计”的要求。
