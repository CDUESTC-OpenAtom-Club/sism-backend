# Bounded Context Map - SISM 架构设计

> 版本：v1.0  
> 创建时间：2026-03-12  
> 状态：已完成

## 1. 业务领域识别

基于战略指标管理系统（SISM）的当前模块边界，我们识别出以下 7 个 Bounded Context：

### 1.0 当前评审护栏

- 不允许向 `sism-task` 新增指标、里程碑、计划下发逻辑。
- 不允许向 `sism-execution` 新增新的 `Plan` 主链职责。
- 新增规划主链能力时，默认先落在 `sism-strategy`，再判断是否需要跨上下文读取。

### 1.1 Organization Context（组织管理上下文）
**职责**：管理组织结构、用户、角色和权限
- 组织层级管理（一级、二级、三级组织）
- 用户账号管理
- 角色和权限分配
- 用户-组织关系维护

### 1.2 Strategy Context（战略规划上下文）
**职责**：管理规划主链路骨架对象和规划编排
- 周期管理（Cycle）
- 计划批次管理（Plan）
- 指标树定义和层级管理（Indicator Tree）
- 指标节点里程碑管理（Milestone）
- 计划下发、审批前置校验和指标联动

### 1.3 Task Context（任务中心上下文）
**职责**：管理统一任务模型及其扩展能力
- 任务中心统一模型
- 当前战略任务（StrategicTask）
- 未来临时任务/专项任务扩展点
- 任务分类、来源和生命周期语义
- 纯任务列表与统一检索

### 1.4 Execution Context（执行管理上下文）
**职责**：管理执行期填报、快照和分析支撑
- 计划报告填报
- 进度跟踪
- 执行期快照/汇总
- 执行分析输入模型

### 1.5 Workflow Context（工作流引擎上下文）
**职责**：提供通用的审批流程引擎
- 审批流定义
- 审批实例管理
- 审批步骤执行
- 审批历史记录

### 1.6 Analytics Context（数据分析上下文）
**职责**：提供数据统计、分析和可视化
- 仪表板数据聚合
- 指标完成率统计
- 组织绩效分析
- 趋势分析

### 1.7 Auth Context（用户认证上下文）
**职责**：处理用户认证和会话管理
- 用户登录/登出
- JWT Token 生成和验证
- 刷新令牌管理
- Token 黑名单

---

## 2. Context 边界定义

### 2.1 Organization Context

**核心聚合根**：
- User（用户）
- Organization（组织）
- Role（角色）

**对外接口**：
- 用户信息查询
- 组织结构查询
- 权限验证

**依赖关系**：
- 被所有其他 Context 依赖（提供用户和组织信息）

**边界规则**：
- 只负责组织结构和用户管理，不涉及业务逻辑
- 不直接处理指标、计划等业务实体

---

### 2.2 Strategy Context

**核心聚合根**：
- Cycle（周期）
- Plan（计划）
- Indicator（指标）
- Milestone（里程碑）

**对外接口**：
- 计划主入口
- 指标树查询与分解
- 里程碑管理
- 规划聚合查询（Plan -> Task -> Indicator -> Milestone）

**依赖关系**：
- 依赖 Organization Context（获取组织和用户信息）
- 依赖 Task Context（读取任务中心中的 StrategicTask）
- 依赖 Workflow Context（提交审批）
- 被 Execution Context 依赖（执行期读取规划对象）
- 被 Analytics Context 依赖（统计分析）

**边界规则**：
- 拥有 Cycle / Plan / Indicator / Milestone 的主业务所有权
- 负责计划是否可下发、指标如何联动、里程碑如何挂靠
- 可以读取 Task，但不拥有 Task
- 不负责执行期填报

---

### 2.3 Task Context

**核心聚合根**：
- Task（任务中心）
- StrategicTask（当前任务实现）

**对外接口**：
- 任务创建与维护
- 任务列表和检索
- 按 planId / cycleId / orgId / taskType 查询任务

**依赖关系**：
- 依赖 Organization Context（获取组织和用户信息）
- 被 Strategy Context 依赖（规划链路装配任务）
- 被 Analytics Context 依赖（读取任务中心数据）

**边界规则**：
- 拥有 `sys_task` 及任务模型演进
- 不拥有 Indicator / Milestone / Plan 下发规则
- 可以拥有任务自身生命周期，但不复制 Plan 审批状态

---

### 2.4 Execution Context

**核心聚合根**：
- ProgressReport（进度报告）

**对外接口**：
- 进度更新
- 报告提交

**依赖关系**：
- 依赖 Strategy Context（关联指标）
- 依赖 Organization Context（获取组织信息）
- 依赖 Workflow Context（提交审批）
- 被 Analytics Context 依赖（统计分析）

**边界规则**：
- 负责执行期填报和报告，不再长期拥有 Plan
- 可以消费 Plan / Milestone，但不作为其主业务归属上下文

---

### 2.5 Workflow Context

**核心聚合根**：
- ApprovalFlow（审批流定义）
- ApprovalInstance（审批实例）

**对外接口**：
- 创建审批实例
- 执行审批操作（通过/驳回/转办/加签）
- 查询审批状态和历史

**依赖关系**：
- 依赖 Organization Context（获取审批人信息）
- 被 Strategy Context 依赖（指标审批）
- 被 Execution Context 依赖（计划审批）

**边界规则**：
- 提供通用的审批流程引擎，不关心具体业务
- 通过回调机制通知业务 Context 审批结果

---

### 2.6 Analytics Context

**核心聚合根**：
- Dashboard（仪表板）
- StatisticsReport（统计报告）

**对外接口**：
- 概览统计
- 指标统计
- 组织绩效统计
- 趋势分析

**依赖关系**：
- 依赖 Strategy Context（读取指标数据）
- 依赖 Execution Context（读取计划和进度数据）
- 依赖 Organization Context（读取组织结构）

**边界规则**：
- 只读查询，不修改业务数据
- 使用 CQRS 模式，可能有独立的查询模型

---

### 2.7 Auth Context

**核心聚合根**：
- AuthSession（认证会话）
- RefreshToken（刷新令牌）

**对外接口**：
- 用户登录
- Token 刷新
- 用户登出
- Token 验证

**依赖关系**：
- 依赖 Organization Context（验证用户凭证）
- 被所有其他 Context 依赖（身份验证）

**边界规则**：
- 只负责认证和会话管理，不负责授权
- 授权由 Organization Context 的权限系统处理

---

## 3. Context Map（上下文映射图）

```
┌─────────────────────────────────────────────────────────────────┐
│                         Auth Context                            │
│                    (用户认证和会话管理)                            │
│                                                                 │
│  - 用户登录/登出                                                  │
│  - JWT Token 管理                                                │
│  - 刷新令牌                                                       │
└────────────────────────┬────────────────────────────────────────┘
                         │ 依赖
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Organization Context                         │
│                    (组织和用户管理)                                │
│                                                                  │
│  - 组织结构                                                       │
│  - 用户管理                                                       │
│  - 角色权限                                                       │
└──────┬──────────────────┬──────────────────┬────────────────────┘
       │                  │                  │
       │ 提供用户/组织信息 │                  │
       │                  │                  │
       ▼                  ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Strategy   │  │  Execution   │  │   Workflow   │
│   Context    │  │   Context    │  │   Context    │
│              │  │              │  │              │
│ - 战略任务    │  │ - 绩效计划     │  │ - 审批流      │
│ - 指标管理    │  │ - 里程碑       │  │ - 审批实例    │
│ - 指标下发    │  │ - 进度报告     │  │ - 审批历史    │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
       │ 关联指标        │ 提交审批        │
       └────────┬────────┴─────────────────┘
                │
                │ 读取业务数据
                ▼
       ┌──────────────────┐
       │    Analytics     │
       │     Context      │
       │                  │
       │ - 仪表板         │
       │ - 统计分析       │
       │ - 趋势分析       │
       └──────────────────┘
```

---

## 4. Context 之间的集成模式

### 4.1 Shared Kernel（共享内核）
**适用于**：所有 Context 共享基础设施
- 共享值对象（EntityId, Money, DateRange, Percentage）
- 共享实体基类（Entity, AggregateRoot）
- 共享领域事件接口（DomainEvent）

### 4.2 Customer-Supplier（客户-供应商）
**Organization Context → 其他 Context**
- Organization Context 作为供应商，提供用户和组织信息
- 其他 Context 作为客户，消费这些信息
- 通过明确的 API 接口进行交互

### 4.3 Conformist（遵奉者）
**Auth Context → Organization Context**
- Auth Context 遵循 Organization Context 的用户模型
- 不定义自己的用户实体，直接使用 Organization Context 的用户

### 4.4 Published Language（发布语言）
**Workflow Context → 业务 Context**
- Workflow Context 定义标准的审批接口
- 业务 Context 通过标准接口集成审批功能
- 使用事件通知审批结果

### 4.5 Separate Ways（各行其道）
**Analytics Context ↔ 其他 Context**
- Analytics Context 可能使用独立的查询模型（CQRS）
- 通过数据同步或事件订阅获取数据
- 不直接调用其他 Context 的领域模型

---

## 5. 技术实现映射

### 5.1 包结构
```
com.sism/
├── shared/              # Shared Kernel
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── interfaces/
├── organization/        # Organization Context
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── interfaces/
├── strategy/           # Strategy Context
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── interfaces/
├── execution/          # Execution Context
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── interfaces/
├── workflow/           # Workflow Context
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── interfaces/
└── analytics/          # Analytics Context
    ├── domain/
    ├── application/
    ├── infrastructure/
    └── interfaces/
```

### 5.2 DDD 四层架构
每个 Context 内部采用标准的 DDD 四层架构：

1. **Domain Layer（领域层）**
   - 聚合根、实体、值对象
   - 领域服务
   - Repository 接口
   - 领域事件

2. **Application Layer（应用层）**
   - 应用服务（协调领域对象）
   - DTO（数据传输对象）
   - 事务管理

3. **Infrastructure Layer（基础设施层）**
   - Repository 实现
   - 外部服务集成
   - 持久化适配器
   - 消息发布

4. **Interface Layer（接口层）**
   - REST Controllers
   - VO（视图对象）
   - DTO Mapper
   - 异常处理

---

## 6. 迁移策略

### 6.1 现有代码映射
将现有的传统三层架构代码映射到新的 Bounded Context：

| 现有代码 | 目标 Context |
|---------|-------------|
| SysUser, SysOrg, SysRole | Organization Context |
| StrategicTask, Indicator | Strategy Context |
| Plan, Milestone, ProgressReport | Execution Context |
| AuditFlowDef, AuditInstance | Workflow Context |
| Dashboard 相关 | Analytics Context |
| AuthController, JWT | Auth Context |

### 6.2 渐进式重构
1. 创建新的包结构
2. 在新包中实现领域模型
3. 保持现有 API 兼容
4. 逐步迁移业务逻辑到领域模型
5. 最终移除旧代码

---

## 7. 验收标准

- [x] 识别出 6 个 Bounded Context
- [x] 明确每个 Context 的职责范围
- [x] 定义 Context 之间的依赖关系
- [x] 绘制 Context Map
- [ ] 创建对应的包结构
- [ ] 团队评审通过

---

**文档状态**：✅ 已完成  
**下一步**：创建包结构（任务 1.1.3）
