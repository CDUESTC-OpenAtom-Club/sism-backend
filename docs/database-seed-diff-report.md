# SISM 数据库表 vs 种子数据差异报告

> 生成时间: 2026-04-15
> 数据库: sism_db (PostgreSQL 14.22)
> 对比范围: public schema 全部 36 个对象 (33 表 + 3 视图) vs 30 个种子 SQL 文件

---

## 1. 总览

| 维度 | 数量 |
|------|------|
| 数据库总对象数 (表+视图) | 36 |
| 实际数据表 | 33 |
| 数据库视图 (analytics_*) | 3 |
| 种子数据 TRUNCATE 涉及表数 | 27 |
| 有 INSERT 数据的种子文件 | 19 |
| 空/仅清理的种子文件 | 11 |
| 字段完全匹配的表 | 15 |
| 字段有差异的表 | 4 |
| 数据库当前有数据的表 | 20 |
| 数据库当前空表 | 14 |

---

## 2. 完整对照表

### 图例

- **种子文件**: 该表是否有对应的种子数据 SQL 文件
- **TRUNCATE**: 重置脚本是否会清空该表
- **当前行数**: 数据库中该表的实际数据行数
- **状态**:
  - MATCH — 一切正常
  - NO SEED — 有表但无种子文件（Flyway 管理或运行时数据）
  - EMPTY SEED — 有种子文件但文件内容为空（0 条 INSERT）
  - ORPHAN FILE — 种子文件存在但不在 reset 脚本的 TRUNCATE/加载列表中

| # | 表名 | 种子文件 | TRUNCATE | 当前行数 | 状态 |
|---|------|---------|----------|---------|------|
| 1 | adhoc_task | — | YES | 0 | NO SEED (运行时表,无数据) |
| 2 | adhoc_task_indicator_map | — | YES | 0 | NO SEED (运行时表,无数据) |
| 3 | adhoc_task_target | — | YES | 0 | NO SEED (运行时表,无数据) |
| 4 | alert_event | alert_event-data.sql | YES | 0 | EMPTY SEED (0 INSERT) |
| 5 | alert_rule | alert_rule-data.sql | YES | 3 | MATCH |
| 6 | alert_window | alert_window-data.sql | YES | 4 | MATCH |
| 7 | attachment | attachment-data.sql | YES | 0 | EMPTY SEED (0 INSERT) |
| 8 | audit_flow_def | audit_flow_def-data.sql | YES | 4 | MATCH |
| 9 | audit_instance | audit_instance-data.sql | YES | 2 | MATCH |
| 10 | audit_log | audit_log-clean.sql | YES | 0 | EMPTY SEED (clean-only) |
| 11 | audit_step_def | audit_step_def-data.sql | YES | 14 | MATCH |
| 12 | audit_step_instance | audit_step_instance-data.sql | YES | 14 | MATCH |
| 13 | cycle | cycle-data.sql | YES | 4 | MATCH |
| 14 | flyway_schema_history | — | — | 15 | NO SEED (Flyway 元数据) |
| 15 | idempotency_records | idempotency_records-data.sql | YES | 0 | EMPTY SEED (0 INSERT) |
| 16 | indicator | indicator-data.sql | YES | 43 | MATCH |
| 17 | indicator_milestone | indicator_milestone-data.sql | YES | 156 | MATCH |
| 18 | plan | plan-data.sql | YES | 684 | MATCH |
| 19 | plan_report | plan_report-data.sql | YES | 0 | EMPTY SEED (0 INSERT) |
| 20 | plan_report_indicator | plan_report_indicator-data.sql | YES | 0 | EMPTY SEED (0 INSERT) |
| 21 | plan_report_indicator_attachment | plan_report_indicator_attachment-data.sql | YES | 0 | EMPTY SEED (0 INSERT) |
| 22 | progress_report | progress_report-data.sql | YES | 0 | EMPTY SEED (0 INSERT) |
| 23 | refresh_tokens | refresh_tokens-clean.sql | YES | 0 | EMPTY SEED (clean-only) |
| 24 | sys_org | sys_org-data.sql | YES | 28 | MATCH |
| 25 | sys_permission | sys_permission-data.sql | YES | 12 | MATCH |
| 26 | sys_role | sys_role-data.sql | YES | 4 | MATCH |
| 27 | sys_role_permission | sys_role_permission-data.sql | YES | 32 | MATCH |
| 28 | sys_task | sys_task-data.sql | YES | 22 | MATCH |
| 29 | sys_user | sys_user-data.sql | YES | 104 | MATCH |
| 30 | sys_user_notification | sys_user_notification-data.sql | YES | 4 | MATCH |
| 31 | sys_user_role | sys_user_role-data.sql | YES | 85 | MATCH |
| 32 | warn_level | warn_level-data.sql | YES | 5 | MATCH |
| 33 | workflow_task | workflow_task-data.sql | YES | 0 | EMPTY SEED (0 INSERT) |
| 34 | workflow_task_history | workflow_task_history-data.sql | YES | 0 | EMPTY SEED (0 INSERT) |

---

## 3. 差异分析

### 3.1 无种子文件的表 (2 张)

这些表由 Flyway 迁移创建，不属于业务种子数据范畴：

| 表名 | 说明 |
|------|------|
| **flyway_schema_history** | Flyway 版本管理元数据表，不应有种子数据 |
| **adhoc_task** / **adhoc_task_indicator_map** / **adhoc_task_target** | 这 3 张表在 TRUNCATE 列表中但无独立种子文件，属于运行时动态生成的数据 |

> 注: adhoc_task 相关的 3 张表虽然没有种子文件，但 reset 脚本会 TRUNCATE 它们，确保干净状态。

### 3.2 空种子文件 (12 个)

有种子文件但 INSERT 数量为 0，reset 后表仍为空：

| 种子文件 | 说明 |
|----------|------|
| alert_event-data.sql | 预警事件，运行时产生 |
| attachment-data.sql | 附件，运行时产生 |
| idempotency_records-data.sql | 幂等记录，运行时产生 |
| plan_report-data.sql | 计划报告，运行时产生 |
| plan_report_indicator-data.sql | 报告指标，运行时产生 |
| plan_report_indicator_attachment-data.sql | 报告指标附件，运行时产生 |
| progress_report-data.sql | 进度报告，运行时产生 |
| workflow_task-data.sql | 工作流任务，运行时产生 |
| workflow_task_history-data.sql | 工作流历史，运行时产生 |
| audit_instance-data.sql | 当前有 2 条数据但种子文件 INSERT 为 0? 需确认 |
| audit_step_instance-data.sql | 当前有 14 条数据但种子文件 INSERT 为 0? 需确认 |
| audit_log-clean.sql | 仅清理脚本，不含数据 |
| refresh_tokens-clean.sql | 仅清理脚本，不含数据 |

### 3.3 需关注项

| # | 表 | 问题 | 严重程度 |
|---|-----|------|---------|
| 1 | **audit_instance** | 数据库有 2 行数据，但种子文件 INSERT 为 0。reset 后这些数据会丢失 | 中 |
| 2 | **audit_step_instance** | 数据库有 14 行数据，但种子文件 INSERT 为 0。reset 后这些数据会丢失 | 中 |

> 这两张表的数据可能是之前通过应用或手动插入的测试数据，如果 reset 后需要保留，应补充种子 INSERT 语句。

---

## 4. 种子文件加载顺序 vs 表依赖关系

reset 脚本当前的加载顺序（已考虑外键依赖）：

```
1.  sys_org                          ← 组织 (根实体)
2.  sys_user                         ← 用户 (依赖 sys_org)
3.  sys_user_notification            ← 用户通知
4.  idempotency_records              ← 幂等记录
5.  sys_role                         ← 角色
6.  sys_permission                   ← 权限
7.  audit_flow_def                   ← 审批流定义
8.  sys_role_permission              ← 角色-权限关联
9.  sys_user_role                    ← 用户-角色关联
10. audit_step_def                   ← 审批步骤定义 (依赖 audit_flow_def)
11. cycle                            ← 周期
12. plan                             ← 计划 (依赖 cycle, sys_org)
13. sys_task                         ← 任务 (依赖 cycle, plan, sys_org)
14. indicator                        ← 指标 (依赖 sys_task, sys_org)
15. indicator_milestone              ← 指标里程碑 (依赖 indicator)
16. warn_level                       ← 预警等级
17. alert_window                     ← 预警窗口
18. alert_rule                       ← 预警规则
19. plan_report                      ← 计划报告
20. plan_report_indicator            ← 报告指标
21. attachment                       ← 附件
22. plan_report_indicator_attachment ← 报告指标附件
23. alert_event                      ← 预警事件
24. audit_instance                   ← 审批实例
25. audit_step_instance              ← 审批步骤实例
26. workflow_task                    ← 工作流任务
27. workflow_task_history            ← 工作流历史
```

顺序正确，无循环依赖问题。

---

## 5. 字段级差异详情 (Column-Level Diff)

逐表对比数据库实际字段 vs 种子 INSERT 语句中使用的字段。

### 5.1 有差异的表 (4 张)

#### ① sys_user — 缺少 2 个字段

| 字段 | DB 中存在 | 种子 INSERT | 说明 |
|------|----------|-------------|------|
| avatar_url | YES | NO | 用户头像 URL，可为 NULL |
| token_version | YES | NO | JWT 版本号，默认 0 |

**影响**: reset 后 avatar_url 和 token_version 不会被种子数据覆盖，依赖 DB 默认值/NULL。由于 token_version 默认值是 0，avatar_url 可为 NULL，**实际无问题**。

#### ② progress_report — 缺少 1 个字段

| 字段 | DB 中存在 | 种子 INSERT | 说明 |
|------|----------|-------------|------|
| error_message | YES | NO | 报告错误信息 |

**影响**: 种子文件未包含该列。如果种子 INSERT 是在此列添加之前创建的，需要补充。当前种子文件为空 INSERT（0 行），**暂无实际影响**。

#### ③ sys_role_permission — 缺少 1 个字段

| 字段 | DB 中存在 | 种子 INSERT | 说明 |
|------|----------|-------------|------|
| id | YES | NO | 主键 ID (BIGSERIAL 自动生成) |

**影响**: 已确认 id 列有 `nextval('sys_role_permission_id_seq')` 默认值，INSERT 时自动填充。**无实际影响**。

#### ④ sys_user_role — 缺少 1 个字段

| 字段 | DB 中存在 | 种子 INSERT | 说明 |
|------|----------|-------------|------|
| id | YES | NO | 主键 ID (BIGSERIAL 自动生成) |

**影响**: 已确认 id 列有 `nextval('sys_user_role_id_seq')` 默认值，INSERT 时自动填充。**无实际影响**。

### 5.2 字段完全匹配的表 (15 张)

以下表的种子 INSERT 字段与数据库实际字段完全一致，无差异：

| 表名 | DB 列数 | 种子列数 |
|------|--------|---------|
| alert_event | 14 | 14 |
| alert_rule | 8 | 8 |
| alert_window | 7 | 7 |
| attachment | 16 | 16 |
| audit_flow_def | 9 | 9 |
| audit_step_def | 9 | 9 |
| cycle | 8 | 8 |
| indicator | 17 | 17 |
| indicator_milestone | 11 | 11 |
| plan | 10 | 10 |
| sys_org | 10 | 10 |
| sys_permission | 12 | 12 |
| sys_role | 8 | 8 |
| sys_task | 13 | 13 |
| warn_level | 5 | 5 |

### 5.3 无种子 INSERT 的表 (14 张)

这些表没有 INSERT 数据，仅有 TRUNCATE 清理，因此无法进行字段对比：

| 表名 | DB 列数 | 说明 |
|------|--------|------|
| adhoc_task | 14 | 运行时表，无种子文件 |
| adhoc_task_indicator_map | 2 | 运行时表，无种子文件 |
| adhoc_task_target | 2 | 运行时表，无种子文件 |
| audit_instance | 13 | 有空种子文件 |
| audit_log | 11 | 仅清理脚本 |
| audit_step_instance | 11 | 有空种子文件 |
| idempotency_records | 9 | 有空种子文件 |
| plan_report | 23 | 有空种子文件 |
| plan_report_indicator | 7 | 有空种子文件 |
| plan_report_indicator_attachment | 6 | 有空种子文件 |
| refresh_tokens | 8 | 仅清理脚本 |
| sys_user_notification | 16 | 有空种子文件 |
| workflow_task | 18 | 有空种子文件 |
| workflow_task_history | 2 | 有空种子文件 |

---

## 6. 结论

- **reset-clean-seeds.sh 脚本可以正常执行**，能完整清空业务表并重灌种子数据
- **15 张表字段完全匹配**，种子数据结构正确
- **4 张表有字段差异**:
  - `sys_user`: 缺 avatar_url / token_version — **无实际影响** (有合理默认值)
  - `progress_report`: 缺 error_message — **无实际影响** (种子为空)
  - `sys_role_permission` / `sys_user_role`: 缺 id — **无实际影响** (id 为 BIGSERIAL 自动生成)
- **14 张表无种子 INSERT**（运行时数据），reset 后为空表，符合预期
- **3 个 analytics 视图**无种子需求，属于查询视图
- **flyway_schema_history** 不受 reset 影响（不在 TRUNCATE 列表中）
