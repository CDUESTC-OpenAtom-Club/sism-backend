# sism-workflow 模块审计问题修复状态报告

**审查日期:** 2026-04-06
**原审计报告:** sism-workflow-module-audit-report.md
**审查人员:** Claude Code (Automated Review)

---

## 修复状态总览

| 状态 | 数量 | 占比 |
|------|------|------|
| ✅ 已修复 | 3 | 20% |
| ⚠️ 部分修复 | 6 | 40% |
| ❌ 未修复 | 6 | 40% |
| 🔍 无法验证 | 0 | 0% |
| **合计** | **15** | 100% |

---

## 详细审查结果

### 已修复问题

| # | 问题 | 严重等级 | 状态 | 证据 |
|---|------|----------|------|------|
| 1 | N+1 查询风险 - StepInstances | 🟠 Medium | ✅ 已修复 | `AuditInstanceRepositoryImpl.findById()` 现使用 `JOIN FETCH a.stepInstances`，多数查询方法已添加 JOIN FETCH |
| 2 | 魔法字符串 - 步骤状态 | 🟡 Low | ✅ 已修复 | 定义了 `STEP_STATUS_PENDING/WAITING/APPROVED/REJECTED/WITHDRAWN` 常量，代码中统一引用 |
| 3 | 事件发布未持久化 | 🟠 Medium | ✅ 已修复 | `WorkflowEventDispatcher.publish()` 现在先调用 `eventStore.save(event)` 再发布 |

### 部分修复问题

| # | 问题 | 严重等级 | 状态 | 证据 |
|---|------|----------|------|------|
| 4 | BusinessWorkflowController 缺少权限控制 | 🔴 High | ⚠️ 部分修复 | 服务层添加了角色和权限码验证（`approverResolver.canUserApprove`、`ensureUserHasApprovalPermission`），但 Controller 层仍缺少 `@PreAuthorize` 注解 |
| 5 | 审批人判断逻辑过于简单 | 🟡 Low | ⚠️ 部分修复 | 新增 `ApproverResolver` 组件实现角色匹配和组织范围检查，但旧方法仍被 PlanWorkflowSyncService 使用 |
| 6 | 实例列表查询无分页 | 🟠 Medium | ⚠️ 部分修复 | BusinessWorkflowController 已添加分页；WorkflowController（legacy）仍无分页 |
| 7 | API 路径命名不一致 | 🟠 Medium | ⚠️ 部分修复 | 新增 `ApprovalFlowCompatibilityController`（`/api/v1/approval/flows`），但 `/legacy-flows` 仍存在 |
| 8 | 异常处理不一致 | 🟠 Medium | ⚠️ 部分修复 | 新控制器和服务统一使用抛异常模式；WorkflowController 仍返回 200 + 错误码 |
| 9 | 模块耦合 - PlanWorkflowSyncService | 🟠 Medium | ⚠️ 部分修复 | 改用 `ObjectProvider` 使依赖可选，但仍直接调用其他模块方法 |

### 未修复问题

| # | 问题 | 严重等级 | 说明 |
|---|------|----------|------|
| 10 | WorkflowController 审批操作无权限控制 | 🔴 Critical | approveInstance/rejectInstance 仍无 `@PreAuthorize`，userId 仍从请求参数获取 |
| 11 | 流程定义创建无权限控制 | 🟠 Medium | createDefinition 仍任何用户可创建 |
| 12 | STATUS_PENDING = STATUS_IN_PROGRESS（值重复） | 🔴 High | 仍都为 `"IN_REVIEW"` |
| 13 | Controller 接收未使用的 @RequestBody | 🟠 Medium | approveInstance/rejectInstance 仍接收 AuditInstance 但忽略 |
| 14 | 不支持的操作仍暴露为 API | 🟠 Medium | transfer/addApprover 仍会抛出 UnsupportedOperationException |
| 15 | 工作流定义重复查询 | 🟡 Low | 每次审批仍查询数据库 |

---

## 修复质量评估

### 按严重等级统计

| 严重等级 | 总数 | 已修复 | 部分修复 | 修复率 |
|----------|------|--------|----------|--------|
| 🔴 Critical | 1 | 0 | 0 | 0% |
| 🔴 High | 2 | 0 | 1 | 0% |
| 🟠 Medium | 9 | 2 | 5 | 22% |
| 🟡 Low | 3 | 1 | 0 | 33% |
| **合计** | **15** | **3** | **6** | **20%** |

### 关键成果

1. **N+1 查询已修复** — StepInstances 关联查询使用 JOIN FETCH
2. **事件持久化已实现** — WorkflowEventDispatcher 现在保存事件后再发布
3. **模块耦合有所缓解** — 使用 ObjectProvider 使跨模块依赖可选

### 仍需关注的问题

| 优先级 | 问题 | 影响 |
|--------|------|------|
| P0 | WorkflowController 审批无权限控制 | 任何用户可通过传递 userId 审批任何流程 |
| P1 | 状态常量重复值 | 代码混乱，潜在逻辑错误 |
| P2 | Legacy Controller 未统一改造 | 技术债务积累 |

### 整体结论

sism-workflow 模块修复率 20%。**最严重的 Critical 问题（WorkflowController 审批无权限控制）仍未修复**，这是整个系统中最高风险的安全漏洞之一。新版 BusinessWorkflowController 在服务层添加了权限验证，但 legacy WorkflowController 完全未加固。建议立即处理 WorkflowController 的安全问题。

---

**审查完成日期:** 2026-04-06
