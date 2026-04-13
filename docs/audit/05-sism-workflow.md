# 审计报告：sism-workflow 模块（工作流引擎）

**审计日期:** 2026-04-12
**审计范围:** 68个Java源文件，涵盖审批流程定义、实例运行、任务调度、事件监听。

---

## 一、Critical 严重 (共5个)

### C-01. 大规模赋值(Mass Assignment)漏洞 — 实体直接作为 RequestBody
**文件:** `interfaces/rest/WorkflowController.java:66-247`
**状态:** 已修复（2026-04-12）
**描述:** 多个REST端点直接将 `AuditInstance`、`WorkflowTask`、`AuditFlowDef` JPA实体作为 `@RequestBody` 接收。攻击者可构造请求体直接设置 `status`、`isDeleted`、`requesterId` 等任意字段，绕过业务校验。
**修复建议:** 使用专门的DTO接收请求参数，禁止将JPA实体直接暴露为REST请求体。

### C-02. 身份冒用漏洞 — userId 通过请求参数传入
**文件:** `interfaces/rest/WorkflowController.java:92-191`
**状态:** 已修复（2026-04-12）
**描述:** 几乎所有端点通过 `@RequestParam Long userId` 接收用户ID，而非从 `@AuthenticationPrincipal` 中提取。任何已认证用户可冒充其他用户审批、驳回、取消、转办。对比：`BusinessWorkflowController` 正确使用了 `@AuthenticationPrincipal`。
**修复建议:** 所有用户身份从 `@AuthenticationPrincipal CurrentUser` 获取，移除所有 `@RequestParam Long userId`。

### C-03. 事件监听器吞噬异常 — 工作流状态不一致
**文件:** `application/PlanWorkflowEventListener.java:43-46`, `ReportWorkflowEventListener.java:138-148`
**状态:** 未修复（截至 2026-04-12）
**描述:** `catch (Exception ex)` 仅记录日志后不重新抛出。若工作流启动失败，业务模块状态（"审批中"）与工作流模块状态（未启动）永久不一致。无重试机制、无补偿逻辑。
**修复建议:** 引入死信队列或重试机制；在事务回滚时恢复业务状态。

### C-04. 跨模块直接SQL操作 — 严重架构违规
**文件:** `application/ReportWorkflowEventListener.java:69-258`
**状态:** 未修复（截至 2026-04-12）
**描述:** 通过 `JdbcTemplate` 直接读写 `public.plan_report` 表，完全绕过 `sism-execution` 模块的领域模型。若 plan_report 表结构变更，运行时崩溃且编译期无法发现。
**修复建议:** 通过 `sism-execution` 模块的应用服务接口操作报告数据。

### C-05. REST端点调用不支持操作必定返回500
**文件:** `interfaces/rest/WorkflowController.java:177-193`, `domain/runtime/model/AuditInstance.java:300-306`
**状态:** 已修复（2026-04-12）
**描述:** `/instances/{id}/transfer` 和 `/instances/{id}/add-approver` 两个端点最终调用 `throw new UnsupportedOperationException()`。每次请求都返回500错误。
**修复建议:** 移除这两个公开端点，或在Service层提前校验并返回有意义错误。

---

## 二、High 高 (共7个)

### H-01. 全表扫描 + 内存分页 — 无法水平扩展
**文件:** `application/query/WorkflowReadModelService.java:109-147`
**状态:** 已修复（2026-04-12）
**描述:** `getMyPendingTasks` 加载所有PENDING实例到内存再过滤排序分页。`getMyApprovedInstances` 同理。实例数增长后严重内存占用和延迟。
**修复建议:** 将过滤和分页下推到数据库查询层。

### H-02. N+1 查询 — 循环中逐条查用户/组织
**文件:** `application/query/WorkflowReadModelService.java:480-529`
**状态:** 已修复（2026-04-12）
**描述:** `enrichTaskResponse()` 对每个待办任务触发4-5次额外DB查询（flowDef、user、org等）。`ApproverResolver.canUserApprove()` 同理。
**修复建议:** 批量预加载用户名和组织名，用 `WHERE id IN (...)` 代替逐条查询。

### H-03. 两个几乎相同的Repository实现 — 200行死代码
**文件:** `infrastructure/persistence/JpaWorkflowRepositoryInternal.java` vs `WorkflowRepositoryFacade.java`
**状态:** 已修复（2026-04-12）
**描述:** 两类都实现 `WorkflowRepository`，代码几乎完全一致。`JpaWorkflowRepositoryInternal` 被 `@Primary` 覆盖，纯死代码。
**修复建议:** 删除 `JpaWorkflowRepositoryInternal`。

### H-04. 三套并行仓储接口 — 职责重叠
**描述:** `WorkflowRepository`、`AuditInstanceRepository`、`WorkflowQueryRepository` 功能重叠，实现互相委托，增加不必要的复杂度。
**状态:** 未修复（截至 2026-04-12）
**修复建议:** 统一为 CQRS 模式：一个CommandRepository + 一个QueryRepository。

### H-05. ApproverResolver 硬编码默认角色/组织ID
**文件:** `application/support/ApproverResolver.java:43-49,301-329`
**状态:** 已修复（2026-04-12）
**描述:** 第二个构造函数使用硬编码角色ID(2L,3L,4L)和组织ID(35L-54L)。测试或配置缺失时可能意外使用，绕过安全配置。
**修复建议:** 移除双构造函数，强制要求Spring配置注入，未配置时启动失败。

### H-06. resolveCurrentPendingStep 选择逻辑可能错误
**文件:** `domain/runtime/model/AuditInstance.java:316-325`
**状态:** 未修复（截至 2026-04-12）
**描述:** 使用 `reversed()` 返回最大stepNo的PENDING步骤。异常场景（多个PENDING）时可能跳过中间步骤，与 `canRequesterWithdraw()` 逻辑矛盾。
**修复建议:** 明确业务意图，添加多PENDING步骤场景的单元测试。

### H-07. syncWorkflowTerminalStatus 无事务保护
**文件:** `application/ReportWorkflowEventListener.java:262-281`
**状态:** 已修复（2026-04-12）
**描述:** 循环中逐个修改保存 `AuditInstance`，无 `@Transactional`。第2个实例失败时，第1个已保存，部分实例状态不一致。
**修复建议:** 提取到独立的 `@Transactional` 服务方法。

---

## 三、Medium 中等 (共10个)

| # | 文件:行号 | 问题 | 类别 |
|---|---|---|---|
| M-01 | 5个文件多处 | "PLAN"/"PLAN_REPORT"/"PlanReport" 魔法字符串分散定义 | 代码质量 |
| M-02 | `AuditInstance.java:32-33` | `STATUS_PENDING` 和 `STATUS_IN_PROGRESS` 值相同("IN_REVIEW") | 混淆 |
| M-03 | `AuditStatus.java`/`WorkflowStatus.java` | 两个枚举未实际使用或几乎未使用 | 死代码 |
| M-04 | `PageResult.java:26` | `pageSize=0` 时除零异常 | Bug |
| M-05 | `AuditInstance.java:264`/`ApproverResolver.java:239` | 基于步骤名称中文字符串的脆弱业务判断 | Bug |
| M-06 | `WorkflowReadModelMapper.java:114-116` | `toExternalInstanceStatus` 直接返回输入，无转换逻辑 | 死代码 |
| M-07 | `BusinessWorkflowApplicationService.java:124,304,384,401` | `Long.parseLong` 无try-catch，返回500而非400 | Bug |
| M-08 | `AuditFlowDef.java:42-43` | Java字段 `isActive` 对应DB列 `is_enabled`，命名不一致 | 代码质量 |
| M-09 | `WorkflowReadModelService.java` 全文(604行) | 服务类过大，违反单一职责原则 | 架构 |
| M-10 | 多个查询服务 | 缺少 `@Transactional(readOnly=true)` | 性能 |

---

## 四、Low 低 (共6个)

| # | 文件:行号 | 问题 |
|---|---|---|
| L-01 | `ReportWorkflowEventListener.java` 多处 | 日志中包含Emoji字符，可能导致编码问题 |
| L-02 | `WorkflowTask.java:80-83` | history ElementCollection无界增长 |
| L-03 | 多个文件 | null返回值 vs 异常抛出处理不一致 |
| L-04 | 全模块 | 中英文混用注释和日志 |
| L-05 | `AuditStepDef.java:33-34` | stepOrder 对应 step_no 命名不一致 |
| L-06 | `AuditStepDef.java:42-43` | isRequired @Transient 永远为true，无法设为false |

---

## 汇总统计

| 严重性 | 数量 | 关键主题 |
|--------|------|----------|
| **Critical** | 5 | 实体暴露、身份冒用、异常吞噬、跨模块SQL、不支持操作 |
| **High** | 7 | 全表扫描、N+1查询、死代码Repository、硬编码配置 |
| **Medium** | 10 | 魔法字符串、枚举未用、除零风险、脆弱判断、上帝类 |
| **Low** | 6 | Emoji日志、无界集合、命名不一致 |
| **总计** | **28** | |

**最优先修复:**
1. **C-01+C-02** WorkflowController 安全漏洞 — 立即修复或下线
2. **C-03** 事件监听器异常处理 — 数据一致性保障
3. **C-04** 跨模块SQL — 架构违规
4. **H-01+H-02** 性能瓶颈 — 扩展性基础
