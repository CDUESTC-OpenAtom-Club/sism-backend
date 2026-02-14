# 数据库表结构索引

**生成时间**: 2026-02-13 (更新)
**数据库**: strategic (PostgreSQL)
**总表数**: 40 (含3个已废弃表)
**外键约束**: 0 个(已全部删除,适配分布式数据库)

---

## 📑 表分类

### 核心业务表 (Core Business Tables)

| 表名 | 说明 | 字段数 | 状态 | 文档 |
|------|------|--------|------|------|
| sys_org | 系统组织表 | 7 | ✅ 使用中 | [查看详情](./tables/sys_org-table.md) |
| sys_user | 系统用户表 | 9 | ✅ 使用中 | [查看详情](./tables/sys_user-table.md) |
| indicator | 指标表 | 31 | ✅ 使用中 | [查看详情](./tables/indicator-table.md) |
| strategic_task | 战略任务表 | 15 | ✅ 使用中 (4条数据) | [查看详情](./tables/strategic_task-table.md) |
| milestone | 里程碑表 | 13 | ✅ 使用中 | [查看详情](./tables/milestone-table.md) |
| cycle | 周期表 | 8 | ✅ 使用中 | [查看详情](./tables/cycle-table.md) |
| assessment_cycle | 评估周期表 | 8 | ✅ 使用中 | [查看详情](./tables/assessment_cycle-table.md) |
| org_deprecated | 组织表(已废弃) | 7 | ⚠️ 已废弃 | 已迁移至sys_org |
| sys_user_deprecated | 用户表(已废弃) | 9 | ⚠️ 已废弃 | 旧sys_user数据 |
| task_deprecated | 任务表(已废弃) | 10 | ⚠️ 已废弃 | 已迁移至strategic_task |

### 计划与报告表 (Plan & Report Tables)

| 表名 | 说明 | 字段数 | 文档 |
|------|------|--------|------|
| plan | 计划表 | 9 | [查看详情](./tables/plan-table.md) |
| plan_report | 计划报告表 | 12 | [查看详情](./tables/plan_report-table.md) |
| plan_report_indicator | 计划报告指标关联表 | 7 | [查看详情](./tables/plan_report_indicator-table.md) |
| plan_report_indicator_attachment | 计划报告指标附件表 | 6 | [查看详情](./tables/plan_report_indicator_attachment-table.md) |
| progress_report | 进度报告表 | 14 | [查看详情](./tables/progress_report-table.md) |

### 审批与审计表 (Approval & Audit Tables)

| 表名 | 说明 | 字段数 | 文档 |
|------|------|--------|------|
| approval_record | 审批记录表 | 6 | [查看详情](./tables/approval_record-table.md) |
| audit_flow_def | 审批流程定义表 | 8 | [查看详情](./tables/audit_flow_def-table.md) |
| audit_step_def | 审批步骤定义表 | 9 | [查看详情](./tables/audit_step_def-table.md) |
| audit_instance | 审批实例表 | 11 | [查看详情](./tables/audit_instance-table.md) |
| audit_action_log | 审批操作日志表 | 9 | [查看详情](./tables/audit_action_log-table.md) |
| audit_log | 审计日志表 | 11 | [查看详情](./tables/audit_log-table.md) |

### 预警与告警表 (Warning & Alert Tables)

| 表名 | 说明 | 字段数 | 文档 |
|------|------|--------|------|
| warn_rule | 预警规则表 | 7 | [查看详情](./tables/warn_rule-table.md) |
| warn_level | 预警级别表 | 5 | [查看详情](./tables/warn_level-table.md) |
| 2_warn_event | 预警事件表 | 17 | [查看详情](./tables/2_warn_event-table.md) |
| 2_warn_summary_daily | 预警日汇总表 | 9 | [查看详情](./tables/2_warn_summary_daily-table.md) |
| alert_rule | 告警规则表 | 8 | [查看详情](./tables/alert_rule-table.md) |
| alert_event | 告警事件表 | 14 | [查看详情](./tables/alert_event-table.md) |
| alert_window | 告警窗口表 | 7 | [查看详情](./tables/alert_window-table.md) |

### 临时任务表 (Ad-hoc Task Tables)

| 表名 | 说明 | 字段数 | 文档 |
|------|------|--------|------|
| adhoc_task | 临时任务表 | 14 | [查看详情](./tables/adhoc_task-table.md) |
| adhoc_task_indicator_map | 临时任务指标映射表 | 2 | [查看详情](./tables/adhoc_task_indicator_map-table.md) |
| adhoc_task_target | 临时任务目标表 | 2 | [查看详情](./tables/adhoc_task_target-table.md) |

### 系统管理表 (System Management Tables)

| 表名 | 说明 | 字段数 | 状态 | 文档 |
|------|------|--------|------|------|
| sys_role | 系统角色表 | 8 | ✅ 使用中 | [查看详情](./tables/sys_role-table.md) |
| sys_permission | 系统权限表 | 13 | ✅ 使用中 | [查看详情](./tables/sys_permission-table.md) |
| sys_user_role | 用户角色关联表 | 4 | ✅ 使用中 | [查看详情](./tables/sys_user_role-table.md) |
| sys_role_permission | 角色权限关联表 | 4 | ✅ 使用中 | [查看详情](./tables/sys_role_permission-table.md) |

**注**: `sys_user` 表已移至核心业务表分类。

### 辅助功能表 (Auxiliary Tables)

| 表名 | 说明 | 字段数 | 文档 |
|------|------|--------|------|
| attachment | 附件表 | 16 | [查看详情](./tables/attachment-table.md) |
| common_log | 通用日志表 | 11 | [查看详情](./tables/common_log-table.md) |
| refresh_tokens | 刷新令牌表 | 8 | [查看详情](./tables/refresh_tokens-table.md) |
| idempotency_records | 幂等性记录表 | 9 | [查看详情](./tables/idempotency_records-table.md) |
| assessment_cycle | 评估周期表 | 8 | [查看详情](./tables/assessment_cycle-table.md) |
| strategic_task | 战略任务表 | 15 | [查看详情](./tables/strategic_task-table.md) |
| indicator_milestone | 指标里程碑关联表 | 12 | [查看详情](./tables/indicator_milestone-table.md) |

---

## 📊 统计信息

| 分类 | 表数量 | 占比 |
|------|--------|------|
| 核心业务表 | 10 (含3个已废弃) | 25% |
| 计划与报告表 | 5 | 12.5% |
| 审批与审计表 | 6 | 15% |
| 预警与告警表 | 7 | 17.5% |
| 临时任务表 | 3 | 7.5% |
| 系统管理表 | 4 | 10% |
| 辅助功能表 | 5 | 12.5% |
| **总计** | **40** | **100%** |

**注**: 
- `org` 表已重命名为 `org_deprecated`，现使用 `sys_org`
- `app_user` 表已重命名为 `sys_user`，旧 `sys_user` 数据保存在 `sys_user_deprecated`
- `task` 表已重命名为 `task_deprecated`，现使用 `strategic_task`
- `assessment_cycle` 表已从 `cycle` 表同步数据，用于战略任务周期管理

---

## 🔗 表关系说明

### 核心关系链
```
sys_org (系统组织) ✅ 使用中
  ├── sys_user (系统用户) ✅ 使用中
  │   ├── indicator (指标)
  │   │   ├── milestone (里程碑)
  │   │   ├── strategic_task (战略任务) ✅ 使用中
  │   │   └── indicator_milestone (关联)
  │   ├── plan (计划)
  │   │   └── plan_report (计划报告)
  │   └── progress_report (进度报告)
  ├── cycle (周期)
  └── assessment_cycle (评估周期) ✅ 使用中

org_deprecated (已废弃) ⚠️ 已重命名，不再使用
sys_user_deprecated (已废弃) ⚠️ 旧数据保留，不再使用
task_deprecated (已废弃) ⚠️ 已迁移至strategic_task，不再使用
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

1. 点击表名链接查看详细的表结构文档
2. 每个表文档包含：
   - 表结构定义
   - 字段说明
   - 约束信息
   - 示例数据
   - 使用场景

3. 文档格式参考 `org-table-complete-data.md`

---

## 🔍 快速查找

### 按功能查找
- **用户管理**: 
  - `sys_user` - ✅ **系统用户表**（实际使用，1条数据，对应后端 SysUser 实体）
  - `sys_user_deprecated` - ⚠️ **已废弃**（57条旧数据，已重命名保留）
  - `sys_role`, `sys_user_role` - 系统角色相关表
- **组织管理**: 
  - `sys_org` - ✅ **正在使用**（29个组织：1个战略发展部 + 19个职能部门 + 8个二级学院 + 1个其他）
  - `org_deprecated` - ⚠️ **已废弃**（27条旧数据，已重命名保留）
- **指标管理**: 
  - `indicator` - 指标表（711条数据）
  - `indicator_milestone` - 指标里程碑关联表
- **任务管理**: 
  - `strategic_task` - ✅ **战略任务**（关联计划和周期，4条数据，后端有 StrategicTask 实体）
  - `adhoc_task` - ✅ **临时任务**（灵活创建，0条数据，后端有 AdhocTask 实体）
  - `task_deprecated` - ⚠️ **已废弃**（40条旧数据，已迁移至strategic_task）
- **计划报告**: 
  - `plan` - 计划表（2条数据）
  - `plan_report`, `progress_report` - 报告表
- **审批流程**: audit_flow_def, audit_instance, approval_record
- **预警告警**: warn_rule, alert_rule, alert_event
- **日志审计**: audit_log, common_log, audit_action_log

### 按数据量查找
- **配置表** (数据量小): warn_level, sys_role, sys_permission
- **业务表** (数据量中): 
  - sys_org (29条：1战略+19职能+8学院+1其他)
  - indicator (711条)
  - strategic_task (4条)
  - plan (2条)
  - milestone (0条)
- **用户表**: 
  - sys_user (1条活跃)
  - sys_user_deprecated (57条已废弃)
- **周期表**:
  - cycle (4条：2023-2026年度)
  - assessment_cycle (4条：从cycle同步)
- **任务表**:
  - strategic_task (4条，使用中)
  - adhoc_task (0条，可用)
  - task_deprecated (40条，已废弃)
- **已废弃表**: 
  - org_deprecated (27条)
  - sys_user_deprecated (57条)
  - task_deprecated (40条)
- **日志表** (数据量大): audit_log, common_log, alert_event

---


---

## 🔄 迁移历史

### ✅ V1.9: 删除所有外键约束 (已完成)

**执行时间**: 2026-02-13

**变更内容**:
- 删除所有 68 个外键约束,适配分布式数据库架构
- 修复指向废弃表的外键引用问题:
  - `indicator.task_id` → `task_deprecated` (已废弃)
  - `adhoc_task.org_id` → `org_deprecated` (已废弃)
  - `adhoc_task_target.org_id` → `org_deprecated` (已废弃)
  - `progress_report.user_id` → `sys_user_deprecated` (已废弃)
  - `audit_log.user_id` → `sys_user_deprecated` (已废弃)
  - `audit_log.org_id` → `org_deprecated` (已废弃)
  - `alert_event.user_id` → `sys_user_deprecated` (已废弃)
  - `common_log.user_id` → `sys_user_deprecated` (已废弃)
  - `sys_user_role.user_id` → `sys_user_deprecated` (已废弃)
- 删除了以下表的外键约束:
  - 核心业务表: indicator(4个), strategic_task(3个), sys_user(1个), milestone(2个), sys_org(1个)
  - 计划报告表: progress_report(6个), plan_report_indicator(1个), plan_report_indicator_attachment(2个)
  - 审批审计表: approval_record(3个), audit_log(4个), audit_flow相关表(7个)
  - 预警告警表: warn_rule(1个), alert_rule(2个), alert_event(5个), alert_window(2个)
  - 临时任务表: adhoc_task(5个), adhoc_task_indicator_map(2个), adhoc_task_target(3个)
  - 系统管理表: sys_user_role(2个), sys_role_permission(2个), refresh_tokens(1个)

**迁移结果**:
- **外键约束总数**: 0 个(已全部删除)
- **数据完整性**: 现由应用层 Service 代码保证
- **性能提升**: 避免外键验证开销,支持分布式部署
- **扩展性**: 支持数据分片和跨库部署

**迁移脚本**:
- `sism-backend/database/migrations/V1.9__remove_all_foreign_keys.sql`
- `sism-backend/database/scripts/run-v1.9-remove-foreign-keys.js`
- `sism-backend/database/scripts/run-v1.9-remove-remaining-fk.js`

**重要提示**:
- ⚠️ 数据完整性现在由应用层代码保证
- ⚠️ 建议在 Service 层添加数据验证逻辑(如 orgExists(), userExists())
- ⚠️ 建议添加单元测试验证数据一致性
- ⚠️ 已修复指向废弃表的外键引用问题
- ⚠️ 适配分布式数据库架构,提升性能和扩展性

---

### ✅ V1.5: org → sys_org 迁移 (已完成)

**执行时间**: 2026-02-10

**变更内容**:
- `org` 表重命名为 `org_deprecated` (27条数据保留)
- 所有数据迁移至 `sys_org` (29条数据)
- 后端实体类从 `Org` 更新为 `SysOrg`
- 字段名称统一：`org_id` → `id`, `org_name` → `name`, `org_type` → `type`
- 更新所有外键引用

**详细文档**: `SYS-ORG-MIGRATION-COMPLETE.md`

### ✅ V1.7: app_user → sys_user 迁移 (已完成)

**执行时间**: 2026-02-10

**变更内容**:
- 旧 `sys_user` 表重命名为 `sys_user_deprecated` (57条遗留数据保留)
- `app_user` 表重命名为 `sys_user` (1条活跃数据)
- 后端实体类从 `AppUser` 更新为 `SysUser`
- 字段名称统一：`user_id` → `id`（与 `sys_org` 保持一致）
- 更新所有 Repository 方法和 getter 调用

**详细文档**: `APP-USER-TO-SYS-USER-MIGRATION-COMPLETE.md`, `NAMING-CONVENTION-MIGRATION-SUMMARY.md`

### ✅ V1.8: task → strategic_task 迁移 (已完成)

**执行时间**: 2026-02-11

**变更内容**:
- 同步 `cycle` 表数据到 `assessment_cycle` 表（4条周期记录）
- 将 `task` 表的40条数据迁移至 `strategic_task`（实际迁移4条有效数据）
- `task` 表重命名为 `task_deprecated`
- 更新所有外键约束指向 `sys_org` 表
- 解决了 `cycle_id` 外键约束问题

**迁移结果**:
- `strategic_task`: 4条记录
- `task_deprecated`: 40条记录（保留）
- `assessment_cycle`: 4条周期记录（2023-2026年度）

**迁移脚本**: 
- `sism-backend/database/migrations/V1.8__migrate_task_to_strategic_task.sql`
- `sism-backend/database/scripts/sync-cycle-to-assessment-cycle.js`
- `sism-backend/database/scripts/run-v1.8-migration.js`

---

## ✨ 命名规范统一

经过 V1.5 和 V1.7 两次迁移，系统核心实体命名现已完全统一：

| 实体类 | 表名 | 主键列 | 实体字段 | Getter方法 | 数据量 | 状态 |
|--------|------|--------|----------|-----------|--------|------|
| `SysOrg` | `sys_org` | `id` | `id` | `getId()` | 29条 | ✅ 使用中 |
| `SysUser` | `sys_user` | `id` | `id` | `getId()` | 1条 | ✅ 使用中 |
| `StrategicTask` | `strategic_task` | `id` | `id` | `getId()` | 4条 | ✅ 使用中 |

**命名模式**: 
- 系统核心实体使用 `Sys` 前缀
- 业务实体使用描述性名称（如 `StrategicTask`）
- 所有实体主键统一为 `id`

---

## 📋 任务表对比

系统中存在三个任务相关的表，功能和状态各不相同：

| 表名 | 后端实体 | 数据量 | 功能定位 | 状态 |
|------|---------|--------|---------|------|
| `strategic_task` | `StrategicTask` | 4条 | 战略任务（关联计划和周期） | ✅ 使用中 |
| `adhoc_task` | `AdhocTask` | 0条 | 临时任务（灵活创建） | ✅ 可用 |
| `task_deprecated` | `Task` | 40条 | 基础任务（已废弃） | ⚠️ 已废弃 |

**迁移完成**: `task` 表数据已迁移至 `strategic_task`，现统一使用 `strategic_task` 作为主要任务表。

**详细对比**: 参见 `TASK-TABLES-COMPARISON.md`
