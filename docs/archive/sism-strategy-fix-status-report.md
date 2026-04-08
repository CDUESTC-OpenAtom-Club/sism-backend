# sism-strategy 模块审计问题修复状态报告

**审查日期:** 2026-04-06
**原审计报告:** sism-strategy-module-audit-report.md
**审查人员:** Claude Code (Automated Review)

---

## 修复状态总览

| 状态 | 数量 | 占比 |
|------|------|------|
| ✅ 已修复 | 1 | 5% |
| ⚠️ 部分修复 | 4 | 21% |
| ❌ 未修复 | 14 | 74% |
| 🔍 无法验证 | 0 | 0% |
| **合计** | **19** | 100% |

---

## 详细审查结果

### 已修复问题

| # | 问题 | 严重等级 | 状态 | 证据 |
|---|------|----------|------|------|
| 1 | 催办端点权限检查不足 | 🟠 Medium | ✅ 已修复 | 添加了 `@PreAuthorize("hasAnyRole('ADMIN','STRATEGY_DEPT')")` 注解 + 编程式角色检查 |

### 部分修复问题

| # | 问题 | 严重等级 | 状态 | 证据 |
|---|------|----------|------|------|
| 2 | PlanController 多个端点缺少 @PreAuthorize | 🔴 High | ⚠️ 部分修复 | createPlan、deletePlan、approvePlan、rejectPlan 已添加权限注解；updatePlan、publishPlan、archivePlan、submitPlanForApproval、withdrawPlan 等仍无权限控制 |
| 3 | IndicatorController 多个端点缺少 @PreAuthorize | 🔴 High | ⚠️ 部分修复 | createIndicator、deleteIndicator、distributeIndicator、batchDistribute、sendReminder 已添加；submitForReview、approveIndicator、rejectIndicator、withdrawIndicator、breakdownIndicator、activateIndicator、terminateIndicator 仍无权限控制 |
| 4 | Indicator 实体懒加载关联 N+1 查询风险 | 🟠 Medium | ⚠️ 部分修复 | 主要查询方法已添加 `@EntityGraph(attributePaths = {"ownerOrg", "targetOrg"})`，但部分查询方法仍缺少 |
| 5 | Plan 状态使用 String 而非枚举 | 🟡 Low | ⚠️ 部分修复 | 已创建 `PlanStatus` 枚举并在服务层使用，但实体字段仍为 String 未使用 `@Enumerated` |
| 6 | Repository 接口定义不一致 | 🟡 Low | ⚠️ 部分修复 | IndicatorRepository 和 PlanRepository 接口方法与实现一致性改善，但内部 Repository 仍有未暴露的方法 |

### 未修复问题

| # | 问题 | 严重等级 | 说明 |
|---|------|----------|------|
| 7 | 硬编码的角色 ID 和组织 ID | 🟠 Medium | 仍为 `private static final Long` 常量，未改为配置注入 |
| 8 | awaitWorkflowSnapshot 忙等待模式 | 🔴 High | 仍使用 `Thread.sleep(200L)` + `do...while(true)` 循环 |
| 9 | Indicator 冗余的 getName()/getDescription() | 🟠 Medium | 两对方法仍操作同一字段 |
| 10 | Indicator.create 工厂方法重复 | 🟠 Medium | 两个重载版本仍存在，name 参数被忽略 |
| 11 | PlanApplicationService 超大类（现 1512 行） | 🔴 High | 从 1465 行增长至 1512 行，未拆分 |
| 12 | Controller 直接通过 JdbcTemplate 执行 SQL | 🔴 High | IndicatorController 仍注入 JdbcTemplate 执行原始 SQL |
| 13 | loadOrgNamesById 每次加载所有组织 | 🟠 Medium | 仍调用 `organizationRepository.findAll()`，无缓存 |
| 14 | Controller 内嵌大量 DTO 类 | 🔴 High | 仍包含 10+ 静态内部 DTO 类 |
| 15 | 服务层方法过长 | 🟠 Medium | reactivateWithdrawnWorkflowCurrentStep 仍约 170 行 |
| 16 | 异常消息混合中英文 | 🟠 Medium | 仍未统一 |
| 17 | 魔法数字 - 催办进度阈值 | 🟡 Low | 仍为 `if (progress >= 50)` |
| 18 | 服务层直接使用 JdbcTemplate | 🔴 High | 仍使用原始 SQL 执行工作流相关查询 |
| 19 | 领域事件持久化后未清除 | 🟠 Medium | 保存聚合后无 `clearEvents()` 调用 |

---

## 修复质量评估

### 按严重等级统计

| 严重等级 | 总数 | 已修复 | 部分修复 | 修复率 |
|----------|------|--------|----------|--------|
| 🔴 High | 6 | 0 | 2 | 0% |
| 🟠 Medium | 10 | 1 | 2 | 10% |
| 🟡 Low | 3 | 0 | 2 | 0% |
| **合计** | **19** | **1** | **4** | **5%** |

### 整体结论

sism-strategy 模块修复率最低（5%），是所有模块中修复进度最差的。虽然部分关键端点添加了权限控制（createPlan、deletePlan、approvePlan 等），但仍有关键操作端点（submitForReview、approveIndicator、rejectIndicator 等）缺少权限。架构层面的问题（超大类、Controller 直接使用 JdbcTemplate、服务层 SQL 操作等）完全未触及。此模块需要重点投入修复资源。

---

**审查完成日期:** 2026-04-06
