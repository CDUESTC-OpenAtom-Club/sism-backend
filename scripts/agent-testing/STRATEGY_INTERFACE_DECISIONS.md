# 策略执行接口清单与重大决策记录

更新时间：2026-03-16（America/Los_Angeles）
适用范围：`sism-backend/scripts/agent-testing` 多 Agent API 测试 + `strategic-task-management` 前端 Playwright 页面回归

## 1. 目标

本文档用于明确两件事：

1. 执行当前测试策略时，系统必须提供哪些接口（含最小响应约束）。
2. 已经做出的重大技术决策是什么、为什么这么做、风险和回滚点是什么。

## 2. 测试策略概览

- 策略 A：前端页面稳定性回归（Playwright，Mock 数据模式）。
- 策略 B：多 Agent 业务流程 API 回归（IAM + Strategy + Functional + College + Workflow）。

## 3. 必需接口清单（必须具备）

### 3.1 认证与用户（IAM）

| 接口 | 用途 | 最低响应要求 |
| --- | --- | --- |
| `POST /auth/login` | 登录，生成会话 | `code=200`，`data.token`，`data.refreshToken`，`data.user` |
| `POST /auth/refresh` | 刷新 token | `code=200`，`data.token` |
| `POST /auth/logout` | 退出登录 | `code=200` 或幂等成功 |
| `GET /users/profile` | 校验 token 可用性 | `code=200`，`data.username` |
| `GET /users` | IAM 用户列表测试 | `code=200`，`data` 为数组 |

### 3.2 指标域（Strategy/Functional/College）

| 接口 | 用途 | 最低响应要求 |
| --- | --- | --- |
| `GET /indicators` | 列表查询、筛选 | `code=200`，支持分页对象或数组返回（已做兼容） |
| `POST /indicators` | 创建指标 | `code=200`，`data.id` 或等价主键，`data.status` |
| `GET /indicators/{id}` | 指标详情 | `code=200`，返回单指标实体 |
| `PUT /indicators/{id}` | 更新指标 | `code=200`，返回更新后对象 |
| `DELETE /indicators/{id}` | 删除草稿 | 成功码（项目内可为 `204` 或业务成功码） |
| `POST /indicators/{id}/distribute` | 下发触发审批 | `code=200`，指标状态进入审批链 |
| `GET /indicators/{id}/children` | 父子指标关系校验 | `code=200`，`data` 为数组 |
| `GET /indicators/{id}/parent` | 子到父反查 | `code=200`，返回父指标 |

### 3.3 填报域（College/Functional/Workflow）

| 接口 | 用途 | 最低响应要求 |
| --- | --- | --- |
| `GET /reports` | 查询待审批/草稿填报 | `code=200`，分页或数组 |
| `POST /reports` | 创建草稿填报 | `code=200`，`data.status=DRAFT` |
| `GET /reports/{id}` | 填报详情 | `code=200` |
| `PUT /reports/{id}` | 修改草稿/审批中数据 | `code=200` |
| `DELETE /reports/{id}` | 删除填报 | 成功码 |
| `POST /reports/{id}/submit` | 提交审批 | `code=200`，状态进入 `PENDING` |
| `POST /reports/{id}/withdraw` | 撤回填报 | `code=200`，状态回退到 `DRAFT` |
| `GET /indicators/{indicatorId}/reports` | 按指标查历史填报 | `code=200`，数组或分页 |

### 3.4 工作流域（审批）

| 接口 | 用途 | 最低响应要求 |
| --- | --- | --- |
| `GET /workflow/definitions` | 工作流定义查询 | `code=200`，数组 |
| `GET /workflow/definitions/{id}` | 定义详情 | `code=200`，`data.steps` |
| `GET /workflow/instances` | 审批实例查询 | `code=200`，可按实体过滤 |
| `GET /workflow/instances/{id}` | 实例详情 | `code=200` |
| `GET /workflow/instances/{id}/timeline` | 时间轴完整性验证 | `code=200`，节点需含 `operator/timestamp/action/stepName` |
| `GET /workflow/instances/{id}/history` | 历史与驳回意见校验 | `code=200`，数组 |
| `POST /workflow/instances/{id}/approve` | 审批通过 | `code=200`，状态推进 |
| `POST /workflow/instances/{id}/reject` | 审批驳回 | `code=200`，状态回退并记录意见 |
| `POST /workflow/instances/{id}/withdraw` | 流程撤回 | `code=200` |

### 3.5 周期、看板与治理页面（前端回归所需）

| 接口 | 用途 | 最低响应要求 |
| --- | --- | --- |
| `GET /assessment-cycles` | 周期选择与过滤 | `code=200` |
| `POST /cycles` | 创建考核周期 | `code=200`，`data.id` |
| `POST /cycles/{id}/activate` | 激活周期 | `code=200`，状态 `ACTIVE` |
| `GET /cycles/{id}` | 周期详情回读 | `code=200` |
| `GET /dashboard` / `GET /dashboard/overview` | 看板统计 | `code=200` |
| `GET /dashboard/department-progress` | 部门进度图表 | `code=200`，列表数据 |
| `GET /dashboard/recent-activities` | 最近活动 | `code=200`，列表数据 |
| `GET /alerts/stats` | 告警统计 | `code=200` |
| `GET /alerts/events/unclosed` | 未闭环告警 | `code=200` |
| `GET /plans/approval/pending` | 待审批计划列表页 | `code=200`，列表/分页 |
| `GET /plans/approval/pending/count` | 待审批数量徽标 | `code=200`，数值 |
| `POST /plans/approval/instances/{id}/approve` | 计划审批通过 | `code=200` |
| `POST /plans/approval/instances/{id}/reject` | 计划审批驳回 | `code=200` |
| `GET /admin/users` | 管理控制台用户列表 | `code=200`，分页对象 |
| `POST /admin/users` | 创建用户 | `code=200` |
| `PUT /admin/users/{id}` | 更新用户 | `code=200` |
| `PATCH /admin/users/{id}/status` | 启停用户 | `code=200` |
| `PUT /admin/users/{id}/password` | 重置密码 | `code=200` |
| `DELETE /admin/users/{id}` | 删除用户 | `code=200` |

## 4. 关键状态流转约束（接口正确性的核心判据）

- 指标：`DRAFT -> PENDING -> DISTRIBUTED`。
- 指标驳回：`PENDING -> DRAFT`（必须保留驳回意见）。
- 填报：`DRAFT -> PENDING -> APPROVED`。
- 填报撤回：`PENDING -> DRAFT`。
- 审批时间轴：节点顺序完整，时间戳可解析，操作人不可空。

## 5. 本轮重大决策（必须同步）

1. 决策：前端测试以 Mock 模式为主，优先保证页面逻辑可运行。  
原因：当前目标是验证前端架构与页面稳定性，先消除运行时崩溃，再收敛真实后端差异。  
风险：真实后端契约偏差会被延后暴露。  
回滚/补偿：下一阶段增加后端联调回归（同一套路由脚本，切换非 Mock 环境）。

2. 决策：将指标列表查询改为“数组/分页双形态兼容”（已落地到 `indicator/api/query.ts`）。  
原因：Mock 中存在数组返回与分页返回并存，导致 `IndicatorList` 的 `data` 出现 `undefined` 告警。  
风险：弱化了接口契约一致性约束。  
回滚/补偿：后端契约稳定后可收紧为单一分页结构并移除兼容分支。

3. 决策：页面可访问性判定遵循角色路由守卫，不把 `403`/重定向当作缺陷。  
原因：例如职能部门访问 `/admin/console` 返回 `403`，这是权限正确行为。  
风险：可能掩盖“误配置导致的过度限制”。  
回滚/补偿：补充角色-路由矩阵断言，明确每个角色的允许/禁止页面。

4. 决策：Playwright 登录步骤采用 DOM 注入点击作为稳定回归手段。  
原因：标准 `locator.click()` 在当前登录页未稳定触发提交事件，直接导致无请求发出。  
风险：与真实用户行为存在偏差。  
回滚/补偿：后续修复登录表单事件链后，恢复标准输入/点击流程。

5. 决策：将 `WebSocket 无 userId` 与 `ECharts 容器尺寸` 归为非阻塞告警。  
原因：当前不影响页面加载、路由跳转和核心业务交互。  
风险：长期累积会影响可观测性与图表初始化稳定性。  
回滚/补偿：单独立项处理（用户上下文注入与图表挂载时机优化）。

## 6. 2026-03-16 实测结论（摘要）

- 管理员角色：核心页面可加载，未发现阻塞级 JS 错误。
- 职能部门角色：核心页面可加载；访问管理员页正确返回 `403`；访问战略任务页正确回落到 `dashboard`。
- 已修复：`/indicators` 页面 `IndicatorList data=undefined` 告警（接口返回形态兼容后消失）。

## 7. 后续执行与沟通机制

- 任何新增“影响接口契约、权限策略、状态机”的决策，必须先同步后落地。
- 本文档作为基线，后续每次策略升级按日期追加变更记录。

## 8. 2026-03-17 角色联动专项测试（新增）

测试时间：2026-03-17（America/Los_Angeles）  
测试环境：`strategic-task-management` 前端 Mock 模式，`http://127.0.0.1:3500`

### 8.1 测试范围

- 二级学院角色（`secondary_college`）页面可访问性与运行时稳定性。
- 三角色联动矩阵（`strategic_dept` / `functional_dept` / `secondary_college`）：
  - 路由守卫是否按角色生效。
  - 审批相关页面是否仅对具备权限角色开放。
  - 管理控制台是否仅管理员可访问。

### 8.2 账号与样本路径

- 战略部：`admin / 123456`
- 职能部门：`kychu / 123456`
- 二级学院：`jsxy / 123456`

覆盖路径：

- `/dashboard`
- `/indicators`
- `/plans`
- `/distribution`
- `/audit/pending`
- `/audit/plan/9001`
- `/strategic-tasks`
- `/admin/console`
- `/messages`
- `/profile`
- `/fills/indicator/1001`

### 8.3 结论（角色联动是否有效）

1. 二级学院角色联动隔离有效：  
二级学院访问 `/distribution`、`/audit/*`、`/strategic-tasks` 均被正确回退到 `/dashboard`；访问 `/admin/console` 正确进入 `/403`。

2. 审批联动链路对战略部与职能部门均有效：  
两者均可进入 `/audit/pending` 与 `/audit/plan/9001`，待审页可见“审核”动作入口，符合当前权限模型（`AUDIT_APPROVE/AUDIT_REJECT` 赋予这两个角色）。

3. 管理控制台隔离有效：  
仅战略部可访问 `/admin/console`，职能部门与二级学院均受限。

4. 运行时稳定性总体可接受：  
核心页面未出现阻塞级 JS 崩溃；存在非阻塞告警（WebSocket userId 缺失、部分图表尺寸告警）。

### 8.4 本轮发现并已修复的问题

- 问题：`/fills/indicator/1001` 在三角色下出现资源 `404`，页面提示“指标不存在”。
- 根因：Mock 中间件缺少 `GET /api/indicators/:id` 处理分支，导致填报详情页读取指标失败。
- 修复：已在前端 Mock 中补齐 `GET /api/indicators/:id`，并增加数据结构兼容映射（兼容 `id`/`indicatorId` 字段）。
- 结果：二级学院可正常打开填报页，出现“保存草稿 / 提交审核”按钮，控制台无错误级日志。

### 8.5 新增重大决策（本轮）

6. 决策：角色联动验收以“路由守卫 + 审批动作可见性 + 跨角色隔离”三维矩阵作为基线。  
原因：仅验证单页面是否打开不足以证明联动有效，需要同时覆盖可达性与动作权限。  
风险：仍未覆盖真实后端状态同步延迟（Mock 中通常为即时状态）。  
补偿：下一阶段增加“角色A操作 -> 角色B回读”的接口联调回归脚本。

7. 决策：`/fills/indicator/:id` 不做临时绕过，直接补齐 Mock 接口能力后再继续测试。  
原因：该问题直接阻断二级学院核心业务（填报）。  
风险：若临时绕过，可能形成“测试通过但链路失真”。  
补偿：已完成接口补齐并执行三角色回归，后续保留此路径为固定回归用例。
