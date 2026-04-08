# SISM 后端项目 — 审计问题修复状态全局汇总报告

**审查日期:** 2026-04-06  
**基线口径:** 以当前代码、模块测试结果与整仓 `mvn test -q` 结果为准，旧报告中与代码不一致的状态已重算。

---

## 一、全局修复率总览

| 模块 | 总问题数 | 已修复 | 部分修复 | 未修复 | 修复率 | 当前结论 |
|------|----------|--------|----------|--------|--------|----------|
| **sism-iam** | 18 | 13 | 1 | 4 | **72%** | Critical/High 已收口 |
| **sism-main** | 17 | 10 | 0 | 7 | **59%** | Critical/High 已收口，剩余以 P2 为主 |
| **sism-execution** | 14 | 5 | 1 | 8 | **36%** | 关键权限与持久化问题已收口 |
| **sism-workflow** | 15 | 8 | 5 | 2 | **53%** | Legacy 安全风险已显著下降 |
| **sism-task** | 14 | 3 | 1 | 10 | **21%** | 读接口权限已补齐，性能/规则问题仍在 |
| **sism-shared-kernel** | 17 | 2 | 3 | 12 | **12%** | 仍以结构性问题为主 |
| **sism-strategy** | 19 | 3 | 3 | 13 | **16%** | 剩余高风险写端点权限已补齐 |
| **sism-analytics** | 13 | 4 | 1 | 8 | **31%** | 仪表盘越权问题已收口 |
| **sism-alert** | 11 | 2 | 0 | 9 | **18%** | 权限过滤与未解决预警行为已落地 |
| **sism-organization** | 11 | 3 | 0 | 8 | **27%** | 权限与组织树 NPE 已收口 |
| **合计** | **149** | **53** | **15** | **81** | **36%** | |

---

## 二、本轮已确认收口的关键问题

### 已完成的 P0 / Critical

| 模块 | 问题 | 当前状态 |
|------|------|----------|
| sism-workflow | WorkflowController 审批操作无权限控制 | ✅ 已收口，改为使用当前登录用户上下文 |
| sism-analytics | DashboardController 无权限控制 | ✅ 已收口，所有映射接口均要求认证 |
| sism-organization | 所有组织 API 无权限控制 | ✅ 已收口 |
| sism-organization | getParentOrg 导致组织树 NPE | ✅ 已收口 |
| sism-task | 所有 API 端点无权限控制 | ✅ 已从“部分修复”提升为“读写端点均已保护” |
| sism-execution | updateReport 与 GET 端点开放 | ✅ 已收口 |

### 已完成的 P1 / High

| 模块 | 问题 | 当前状态 |
|------|------|----------|
| sism-strategy | Plan / Indicator 剩余高风险写操作端点无权限控制 | ✅ 已收口 |
| sism-analytics | 仪表盘归属验证缺失 | ✅ 已收口 |
| sism-alert | 权限检查逻辑空实现 | ✅ 已收口为最小可用访问控制 |
| sism-workflow | STATUS_PENDING / STATUS_IN_PROGRESS 值重复 | ✅ 已收口，常量不再重复 |

---

## 三、当前仍真实存在的 P2 及以下剩余项

以下项经复核后仍然存在，属于下一轮收口重点：

| 优先级 | 模块 | 问题 |
|--------|------|------|
| P2 | sism-task | `searchTasks` 仍基于内存结果做二次过滤/分页 |
| P2 | sism-strategy | `awaitWorkflowSnapshot` 仍为轮询等待模型 |
| P2 | sism-execution | 仍存在跨模块直接更新与 `System.err` |
| P2 | sism-main | WebSocket/CORS 配置与上传路径校验仍有剩余结构性问题 |
| P2 | sism-shared-kernel | `EnvConfig`、异常层、事件/聚合通用机制仍未统一 |
| P2 | sism-alert / sism-analytics | 统计、分页、实体建模与低优先级设计项仍未收口 |

---

## 四、重复目录清理结果

本轮已完成重复源码目录清理：

- `sism-analytics/src/main 2`
- `sism-analytics/src/test 2`
- `sism-alert/src/main/java 2`

当前这些目录已不再存在，也不会继续参与编译链路。

---

## 五、验证结果

本轮实际通过的关键验证包括：

```bash
mvn -pl sism-organization test
mvn -pl sism-strategy test
mvn -pl sism-analytics test
mvn -pl sism-task test
mvn -pl sism-alert test
mvn -pl sism-workflow test
mvn test -q
```

结论：整仓测试当前通过，代码状态与旧版审计汇总已不一致，本文件已按最新代码现实重算。

---

## 六、下一步建议

1. 继续处理剩余的 P2 结构问题，优先 `task` 分页、`strategy` 轮询、`execution` 跨模块更新。
2. 对 `shared-kernel` 做单独一轮基础设施一致性整理，避免剩余结构问题在业务模块复制扩散。
3. 将各模块 `fix-status-report` 按同一标准持续维护，避免再次出现“代码已修复但报告仍显示未修复”的漂移。
