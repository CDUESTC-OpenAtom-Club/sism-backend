# 数据库表结构索引

**生成时间**: 2026-02-27 (基于冗余表清理更新)
**数据库**: strategic (PostgreSQL @ 175.24.139.148:8386)
**总表数**: 34 (不含已废弃表)
**外键约束**: 0 个 (V1.9 迁移已全部删除，适配分布式数据库)
**数据来源**: 实时数据库查询

---

## 📑 表分类

### 核心业务表 (Core Business Tables)

| 表名 | 说明 | 字段数 | 数据量 | 状态 |
|------|------|--------|--------|------|
| sys_org | 系统组织表 | 8 | 29条 | ✅ 使用中 |
| sys_user | 系统用户表 | 9 | 1条 | ✅ 使用中 |
| indicator | 指标表 | 33 | 711条 | ✅ 使用中 |
| sys_task | 战略任务表 | 16 | 4条 | ✅ 使用中 |
| cycle | 周期表 | 8 | 4条 | ✅ 使用中 |

**注**: 以下表已在数据库中重命名或废弃，不再使用：
- ~~`org`~~ → 现使用 `sys_org` (原 `org` 表已重命名为 `org_deprecated`)
- ~~`app_user`~~ → 现使用 `sys_user` (原 `sys_user` 表数据已移至 `sys_user`，旧表重命名为 `sys_user_deprecated`)
- ~~`task`~~ → 经 `strategic_task` 迁移至 `sys_task` (原 `task` 表已重命名为 `task_deprecated`)
- ~~`strategic_task`~~ → 现使用 `sys_task` (数据已迁移，表记录数为 0，已废弃)
- ~~`strategic_task_backup`~~ → 迁移完成后的备份表，已废弃删除
- ~~`milestone`~~ → 已被 `indicator_milestone` 替代（0条数据，已废弃）
- ~~`assessment_cycle`~~ → 已被 `cycle` 替代（数据冗余，已废弃）

### 计划与报告表 (Plan & Report Tables)

| 表名 | 说明 | 字段数 | 数据量 |
|------|------|--------|--------|
| plan | 计划表 | 9 | 2条 |
| plan_report | 计划报告表 | 12 | - |
| plan_report_indicator | 计划报告指标关联表 | 7 | - |
| plan_report_indicator_attachment | 计划报告指标附件表 | 6 | - |
| progress_report | 进度报告表 | 14 | - |
| indicator_milestone | 指标里程碑关联表 | 12 | - |

### 审批与审计表 (Approval & Audit Tables)

| 表名 | 说明 | 字段数 | 数据量 |
|------|------|--------|--------|
| approval_record | 审批记录表 | 6 | - |
| audit_flow_def | 审批流程定义表 | 10 | - |
| audit_step_def | 审批步骤定义表 | 10 | - |
| audit_instance | 审批实例表 | 13 | - |
| audit_action_log | 审批操作日志表 | 10 | - |
| audit_log | 审计日志表 | 12 | - |

### 预警与告警表 (Warning & Alert Tables)

| 表名 | 说明 | 字段数 | 数据量 |
|------|------|--------|--------|
| warn_rule | 预警规则表 | 7 | - |
| warn_level | 预警级别表 | 5 | - |
| 2_warn_event | 预警事件表 | 17 | - |
| 2_warn_summary_daily | 预警日汇总表 | 9 | - |
| alert_rule | 告警规则表 | 8 | - |
| alert_event | 告警事件表 | 14 | - |
| alert_window | 告警窗口表 | 7 | - |

### 临时任务表 (Ad-hoc Task Tables)

| 表名 | 说明 | 字段数 | 数据量 |
|------|------|--------|--------|
| adhoc_task | 临时任务表 | 14 | 0条 |
| adhoc_task_indicator_map | 临时任务指标映射表 | 2 | - |
| adhoc_task_target | 临时任务目标表 | 2 | - |

### 系统管理表 (System Management Tables)

| 表名 | 说明 | 字段数 | 数据量 |
|------|------|--------|--------|
| sys_role | 系统角色表 | 8 | - |
| sys_permission | 系统权限表 | 13 | - |
| sys_user_role | 用户角色关联表 | 4 | - |
| sys_role_permission | 角色权限关联表 | 4 | - |

### 辅助功能表 (Auxiliary Tables)

| 表名 | 说明 | 字段数 | 数据量 |
|------|------|--------|--------|
| attachment | 附件表 | 16 | - |
| common_log | 通用日志表 | 11 | - |
| refresh_tokens | 刷新令牌表 | 8 | - |
| idempotency_records | 幂等性记录表 | 9 | - |

### Flyway 迁移表 (Migration Tables)

| 表名 | 说明 | 字段数 |
|------|------|--------|
| flyway_schema_history | Flyway 迁移历史表 | 10 |

---

## 📊 统计信息

| 分类 | 表数量 | 占比 |
|------|--------|------|
| 核心业务表 | 5 | 14.7% |
| 计划与报告表 | 6 | 17.6% |
| 审批与审计表 | 6 | 17.6% |
| 预警与告警表 | 7 | 20.6% |
| 临时任务表 | 3 | 8.8% |
| 系统管理表 | 4 | 11.8% |
| 辅助功能表 | 4 | 11.8% |
| Flyway 迁移表 | 1 | 2.9% |
| **总计** | **34** | **100%** |

**注**: 以下废弃表不计入此统计：`org_deprecated`, `sys_user_deprecated`, `task_deprecated`, `strategic_task`, `strategic_task_backup`, `milestone`, `assessment_cycle`（共7张废弃表）。

---

## 🔗 表关系说明

### 核心关系链
```
sys_org (系统组织) - 29条数据
  ├── sys_user (系统用户) - 1条数据
  │   ├── indicator (指标) - 711条
  │   │   └── indicator_milestone (关联)
  │   ├── sys_task (战略任务) - 4条
  │   ├── plan (计划) - 2条
  │   │   └── plan_report (计划报告)
  │   └── progress_report (进度报告)
  └── cycle (周期) - 4条
```

### 审批流程链
```
audit_flow_def (流程定义)
  ├── audit_step_def (步骤定义)
  └── audit_instance (流程实例)
      ├── audit_action_log (操作日志)
      └── approval_record (审批记录)
```

### 预警告警链
```
warn_rule (预警规则)
  ├── warn_level (预警级别)
  └── 2_warn_event (预警事件)
       └── 2_warn_summary_daily (日汇总)

alert_rule (告警规则)
  ├── alert_event (告警事件)
  └── alert_window (告警窗口)
```

---

## 📝 使用说明

1. **文档结构**
   - 本文档为主索引，列出所有表及基本信息
   - 点击表名可查看详细表结构（待补充）
   - 每个表文档包含：字段类型、长度、约束、示例数据

2. **数据来源**
   - 本文档基于 2026-02-15 实时数据库查询生成
   - 所有表结构、字段数、数据量均为实际值

3. **表状态说明**
   - ✅ 使用中：系统正在使用的表
   - ⚠️ 已废弃：已重命名或迁移，不再使用

---

## 🔍 快速查找

### 按功能查找
- **用户管理**:
  - `sys_user` - ✅ **系统用户表**（1条数据）
  - `sys_role`, `sys_user_role` - 系统角色相关表
- **组织管理**:
  - `sys_org` - ✅ **正在使用**（29个组织：1个战略发展部 + 19个职能部门 + 8个二级学院 + 1个其他）
- **指标管理**:
  - `indicator` - 指标表（711条数据）
  - `indicator_milestone` - 指标里程碑关联表
- **任务管理**:
  - `sys_task` - ✅ **战略任务**（4条数据，关联计划和周期）
  - `adhoc_task` - ✅ **临时任务**（0条数据，灵活创建）
- **计划报告**:
  - `plan` - 计划表（2条数据）
  - `plan_report`, `progress_report` - 报告表
- **审批流程**: audit_flow_def, audit_instance, approval_record
- **预警告警**: warn_rule, alert_rule, alert_event
- **日志审计**: audit_log, common_log, audit_action_log

### 按数据量查找
- **核心表** (数据量较大):
  - sys_org: 29条
  - indicator: 711条
  - sys_task: 4条
  - plan: 2条
  - cycle: 4条
- **配置表** (数据量小):
  - sys_role, sys_permission, warn_level, alert_rule
- **日志表** (数据量可能大):
  - audit_log, common_log, alert_event, 2_warn_event

---

## 🔄 迁移历史

### ✅ V1.9: 删除所有外键约束 (已完成)

**执行时间**: 2026-02-13

**变更内容**:
- 删除所有 68 个外键约束，适配分布式数据库架构
- 修复指向废弃表的外键引用问题
- 数据完整性现在由应用层 Service 代码保证

**影响**:
- 性能提升：避免外键验证开销
- 支持分布式部署：数据分片和跨库
- 需要在 Service 层添加数据验证逻辑

### ✅ V1.5: org → sys_org 迁移 (已完成)

**执行时间**: 2026-02-10

**变更内容**:
- `org` 表重命名为 `org_deprecated`（27条数据保留）
- 所有数据迁移至 `sys_org`（29条数据）
- 后端实体类从 `Org` 更新为 `SysOrg`

### ✅ V1.7: app_user → sys_user 迁移 (已完成)

**执行时间**: 2026-02-10

**变更内容**:
- 旧 `sys_user` 表重命名为 `sys_user_deprecated`（57条遗留数据保留）
- `app_user` 表重命名为 `sys_user`（1条活跃数据）
- 后端实体类统一使用 `SysUser`

### ✅ V1.8: task → strategic_task 迁移 (已完成)

**执行时间**: 2026-02-11

**变更内容**:
- `task` 表的40条数据迁移至 `strategic_task`（实际迁移4条有效数据）
- `task` 表重命名为 `task_deprecated`
- 同步 `cycle` 表数据到 `assessment_cycle` 表

### ✅ V2.0: 冗余表清理 (已完成)

**执行时间**: 2026-02-27

**变更内容**:
- `strategic_task` 数据迁移至 `sys_task`（4条有效数据），`strategic_task` 标记废弃
- `strategic_task_backup` 备份表已完成使命，标记废弃删除
- `milestone` (0条数据) 废弃，功能已由 `indicator_milestone` 关联表替代
- `assessment_cycle` (冗余数据) 废弃，功能已由 `cycle` 表统一管理

**影响**:
- 活跃表总数从 38 减少至 34
- 核心任务表统一为 `sys_task`，命名与 `sys_org`、`sys_user` 风格一致

---

## 📋 重要提示

1. **外键约束**: 已全部删除（V1.9），数据完整性由应用层代码保证
2. **表命名**: 核心实体使用 `Sys` 前缀（`SysOrg`, `SysUser`, `SysTask`）
3. **废弃表**: `org_deprecated`, `sys_user_deprecated`, `task_deprecated`, `strategic_task`, `strategic_task_backup`, `milestone`, `assessment_cycle` 共7张，保留或已删除，不再使用
4. **周期表**: 统一使用 `cycle`，`assessment_cycle` 已废弃
5. **任务表**: 统一使用 `sys_task`，`adhoc_task` 用于临时灵活任务

---

**文档生成时间**: 2026-02-27
**下次更新建议**: 补充各个表的详细结构文档
