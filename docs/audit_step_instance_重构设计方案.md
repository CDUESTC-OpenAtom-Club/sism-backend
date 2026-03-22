# audit_step_instance 重构设计方案

## 1. 目标

本方案只讨论设计，不包含数据库变更执行，不包含生产数据修改。

目标是把 `audit_step_instance` 从“字段语义混杂、部分重复、主流程未完全使用”的状态，收敛成一张职责明确的“审批节点实例表”。

本次设计遵循以下原则：

1. 不改数据库现状，只定义目标模型与代码使用方式。
2. 一个字段只承担一种明确语义，避免同义字段并存。
3. 节点实例表只记录“运行中的节点事实”，不重复承担模板定义职责。
4. 审批流程以“节点实例”为核心，而不是靠多套兼容字段拼接语义。

## 2. 表职责定义

`audit_step_instance` 的唯一职责应为：

- 记录某个审批实例在某个节点上的实际执行情况
- 记录该节点由谁处理、何时处理、处理结果是什么
- 记录该节点与模板步骤 `audit_step_def` 的映射关系

它不应承担以下职责：

- 重新定义流程模板字段
- 保存与模板重复的多套状态语义
- 保存未启用功能的预留字段

## 3. 目标字段模型

### 3.1 保留并作为主流程字段使用

以下字段建议作为主流程白名单字段：

- `id`
  - 节点实例主键

- `instance_id`
  - 所属审批实例
  - 关联 `audit_instance.id`

- `step_def_id`
  - 所属模板步骤
  - 关联 `audit_step_def.id`
  - 这是节点实例和模板定义的正式关联键

- `step_no`
  - 当前实例中的步骤顺序
  - 用于排序、推进、回退

- `step_name`
  - 节点展示名
  - 冗余保留是合理的，因为它服务于历史追溯和页面展示

- `status`
  - 节点实例当前状态
  - 本设计中唯一合法状态字段

- `approver_id`
  - 当前节点应处理的具体用户

- `approver_org_id`
  - 当前审批人所属组织
  - 用于体现按组织动态路由后的归属结果

- `comment`
  - 节点处理意见
  - 作为主意见字段保留

- `approved_at`
  - 审批通过或驳回被确认的业务时间
  - 可视为“审批决定时间”

- `created_at`
  - 节点实例创建时间
  - 代表该节点何时被插入或激活到流程中

### 3.2 明确视为重复或不再推荐使用

- `step_status`
  - 与 `status` 语义重复
  - 不应与 `status` 并存为两套状态入口

- `started_at`
  - 与 `created_at` 在当前审批节点语义下重复
  - 当前系统不存在单独“节点已创建但尚未开始”的清晰阶段，保留意义弱

- `skipped_at`
  - 当前无跳过节点功能需求

- `skipped_reason`
  - 当前无跳过节点功能需求

- `handled_by`
  - 当前无代理审批、转办审批能力
  - 与 `approver_id` 重复且会引入双处理人语义冲突

- `handled_comment`
  - 与 `comment` 重复
  - 当前不存在原始意见与展示意见分层能力

- `handled_at`
  - 与 `approved_at` 的当前使用语义高度重叠
  - 会引入“到底以哪个时间为准”的额外歧义

- `ended_at`
  - 当前节点状态机没有独立的生命周期结束语义
  - 与处理完成时间模型重复，暂不纳入目标主模型

### 3.3 字段取舍

- `step_no`

`step_no` 作为实例层正式顺序字段保留。

当前设计建议：

- `step_no` 参与顺序表达与实例定位
- `step_index` 已被移除，不再保留兼容写法
- `step_code` 为历史遗留字段，直接删除，不再保留

## 4. 状态模型设计

本表只保留一个状态字段：`status`

目标状态集合：

- `PENDING`
  - 当前节点待处理

- `APPROVED`
  - 当前节点已通过

- `REJECTED`
  - 当前节点被驳回

本设计不再把 `WAITING` 作为节点实例表的核心状态。

原因：

- 你的目标流程是逐节点流转
- 节点应在真正进入处理阶段时才产生 `PENDING`
- 后续节点不应依赖 `WAITING` 占位

如果未来系统仍保留“一次性生成所有节点实例”的实现，`WAITING` 只是过渡态，不应作为长期目标设计。

## 5. 与模板表的关系设计

### 5.1 `step_def_id` 必须成为正式关联点

当前最核心的问题之一，是节点实例与模板步骤之间映射不完整。

目标设计要求：

- 每个 `audit_step_instance` 必须指向一个 `audit_step_def`
- 节点实例启动时，从模板复制必要展示信息：
  - `step_name`
  - 审批人解析规则结果
- 但不复制模板的全部结构语义

这样可以同时满足：

- 历史可追溯
- 模板与实例可关联
- 页面展示不必每次回查模板表

### 5.2 模板职责与实例职责边界

`audit_step_def` 负责：

- 节点顺序
- 节点类型
- 审批人解析规则

`audit_step_instance` 负责：

- 当前实例中该节点最终由谁处理
- 该节点当前结果是什么
- 该节点何时被创建、何时作出审批决定

## 6. 审批人设计

根据你现在的业务要求，审批人不应写死。

目标设计应为：

1. 前端发起审批时，携带当前业务上下文
2. 后端根据组织结构和角色规则解析审批人
3. 解析结果写入节点实例：
   - `approver_id`
   - `approver_org_id`

依赖来源：

- `sys_org`
- `sys_user`
- `sys_user_role`
- `sys_role`

页面展示审批人时，统一通过 `approver_id -> sys_user.id` 查询 `real_name` 或 `username`，不再依赖节点实例表冗余保存审批人姓名。

因此，`audit_step_instance` 保存的是“解析后的最终处理人标识与组织归属”，而不是角色规则本身，也不再冗余保存审批人姓名快照。

## 7. 时间字段设计

### 7.1 保留

- `created_at`
  - 节点实例创建时间

- `approved_at`
  - 审批结果生效时间
  - 统一作为当前节点的审批决定时间

### 7.2 去掉

- `started_at`
  - 与 `created_at` 重复，不再保留为目标字段

- `handled_at`
  - 当前与 `approved_at` 重复，不再保留为目标字段

- `ended_at`
  - 当前无独立生命周期结束语义，不再保留为目标字段

### 7.3 时间语义建议

推荐约束如下：

- 节点创建时：
  - `created_at` 有值

- 如果是明确的审批通过或驳回：
  - `approved_at` 有值

## 8. 意见字段设计

当前存在：

- `comment`

目标设计建议如下：

- 只保留 `comment`
  - 作为审批节点唯一意见字段

原因：

- 当前没有意见加工、脱敏、格式化回写等明确业务能力
- `comment` 与 `handled_comment` 并存会带来双写和读取歧义
- 若未来确实出现原始输入留痕需求，再单独扩展

## 9. 目标代码模型建议

如果只从代码设计出发，不改数据库，`AuditStepInstance` 应按以下语义重构：

- 主字段：
  - `id`
  - `instance`
  - `stepDefId`
  - `stepIndex`
  - `stepName`
  - `status`
  - `approverId`
  - `approverOrgId`
  - `comment`
  - `approvedAt`
  - `createdAt`

- 非主流程字段：
  - `stepNo`

- 不再纳入代码主模型：
  - `stepStatus`
  - `startedAt`
  - `skippedAt`
  - `skippedReason`

## 10. 对当前系统的实际影响

如果未来按此设计推进，代码层需要改动的点主要有：

1. `AuditStepInstance` 实体映射
2. `WorkflowApplicationService.initializeStepInstances(...)`
3. `AuditInstance.approve(...)`
4. `AuditInstance.reject(...)`
5. 审批待办查询和详情 VO 映射

但这些属于后续实现，不属于本设计文件的执行范围。

补充说明：

- 当前 `sism-workflow` 的 `AuditStepInstance` 主实体尚未接入 `handled_by`、`handled_comment`、`handled_at`、`ended_at`
- 因此本轮“代码层删除”的实际动作是继续保持这些字段不进入实体、DTO、查询主链路，而不是回退既有业务能力

## 11. 本次设计结论

对 `audit_step_instance` 的目标设计结论如下：

1. `step_def_id` 必须纳入主流程，作为实例与模板的正式关联键。
2. `step_status` 与 `status` 重复，目标设计中只保留 `status`。
3. `skipped_at`、`skipped_reason` 当前无功能需求，设计上移出主模型。
4. `started_at` 与 `created_at` 重复，目标设计中不再保留。
5. `handled_by`、`handled_comment`、`handled_at`、`ended_at` 当前不纳入目标主模型。
6. `approver_name` 可视为冗余字段，目标设计中不再作为主模型字段保留，展示时统一通过 `approver_id` 关联 `sys_user` 获取。
7. `approver_org_id` 作为动态路由结果可保留，但不是当前审批主链路的必需字段。
8. 审批人应由组织结构与角色规则动态解析后写入实例，而不是在模板中固化成某个固定人。

## 12. 建议的下一步

建议后续按顺序推进，不直接跳到数据库修改：

1. 先确认这份目标设计
2. 再做代码层模型收口
3. 最后才决定是否做数据库迁移与历史数据修正

这样风险最低，也符合你当前“不改数据库，只先改设计”的要求。
