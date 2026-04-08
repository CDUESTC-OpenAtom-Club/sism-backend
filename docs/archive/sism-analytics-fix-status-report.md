# sism-analytics 模块审计问题修复状态报告

**审查日期:** 2026-04-06  
**原审计报告:** `sism-analytics-module-audit-report.md`  
**复核口径:** 以当前代码与 `mvn -pl sism-analytics test` 结果为准

---

## 修复状态总览

| 状态 | 数量 | 占比 |
|------|------|------|
| ✅ 已修复 | 4 | 31% |
| ⚠️ 部分修复 | 1 | 8% |
| ❌ 未修复 | 8 | 61% |
| **合计** | **13** | 100% |

---

## 已修复问题

| # | 问题 | 严重等级 | 证据 |
|---|------|----------|------|
| 2 | DashboardController 所有端点无权限控制 | 🔴 Critical | 所有映射接口均已添加 `@PreAuthorize("isAuthenticated()")` |
| 3 | 仪表盘归属验证缺失 | 🔴 High | `DashboardApplicationService` 已对 update/delete/copy/find 做归属校验 |
| 4 | 权限控制不一致 | 🟠 Medium | Controller 层已统一到认证+用户归属检查 |
| 6 | copyToUser 未验证目标用户 | 🟠 Medium | 已要求 `targetUserId` 为正数，并通过服务层校验 |

## 部分修复问题

| # | 问题 | 严重等级 | 证据 |
|---|------|----------|------|
| 1 | 缺少数据验证 | 🟠 Medium | `Dashboard.create()` 已校验基础字段；是否存在目标用户、名称唯一性仍未做仓储级验证 |

---

## 未修复问题

| # | 问题 | 严重等级 | 说明 |
|---|------|----------|------|
| 5 | 重复的源代码目录 | 🔴 High | 已从编译链路移除并删除，但报告级别上仍缺少专项清理说明沉淀，可在下轮补文档化证据 |
| 7 | config 字段长度未限制 | 🟠 Medium | 尚未加约束 |
| 8 | equals/hashCode 不完整 | 🟡 Low | 未整理 |
| 9 | 列表查询无分页 | 🟠 Medium | 仍是列表式接口 |
| 10 | 事件逐个保存 | 🟡 Low | 仍为逐个 `eventStore.save` |
| 11 | 模块名称与功能不符 | 🟠 Medium | 仍未调整边界命名 |
| 12 | 硬编码中文字符串 "(副本)" | 🟡 Low | 仍存在 |
| 13 | 缺少 Dashboard 领域事件 | 🟡 Low | 仍未补充专门事件类型 |

---

## 验证结果

```bash
mvn -pl sism-analytics test
```

结论：仪表盘越权风险已收口，模块不再处于“Critical 完全未修复”状态。
