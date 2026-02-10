# 数据库表结构索引

**生成时间**: 2026-02-11  
**数据库**: strategic (PostgreSQL)  
**总表数**: 40

---

## 📑 表分类

### 核心业务表 (Core Business Tables)

| 表名 | 说明 | 字段数 | 状态 | 文档 |
|------|------|--------|------|------|
| sys_org | 系统组织表 | 7 | ✅ 使用中 | [查看详情](./tables/sys_org-table.md) |
| app_user | 用户表 | 9 | ✅ 使用中 | [查看详情](./tables/app_user-table.md) |
| indicator | 指标表 | 31 | ✅ 使用中 | [查看详情](./tables/indicator-table.md) |
| task | 任务表 | 10 | ✅ 使用中 | [查看详情](./tables/task-table.md) |
| milestone | 里程碑表 | 13 | ✅ 使用中 | [查看详情](./tables/milestone-table.md) |
| cycle | 周期表 | 8 | ✅ 使用中 | [查看详情](./tables/cycle-table.md) |
| org_deprecated | 组织表(已废弃) | 7 | ⚠️ 已废弃 | 已迁移至sys_org |

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
| sys_user | 系统用户表 | 9 | ✅ 使用中 | [查看详情](./tables/sys_user-table.md) |
| sys_role | 系统角色表 | 8 | ✅ 使用中 | [查看详情](./tables/sys_role-table.md) |
| sys_permission | 系统权限表 | 13 | ✅ 使用中 | [查看详情](./tables/sys_permission-table.md) |
| sys_user_role | 用户角色关联表 | 4 | ✅ 使用中 | [查看详情](./tables/sys_user_role-table.md) |
| sys_role_permission | 角色权限关联表 | 4 | ✅ 使用中 | [查看详情](./tables/sys_role_permission-table.md) |

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
| 核心业务表 | 7 (含1个已废弃) | 17.5% |
| 计划与报告表 | 5 | 12.5% |
| 审批与审计表 | 6 | 15% |
| 预警与告警表 | 7 | 17.5% |
| 临时任务表 | 3 | 7.5% |
| 系统管理表 | 5 | 12.5% |
| 辅助功能表 | 7 | 17.5% |
| **总计** | **40** | **100%** |

**注**: `org` 表已重命名为 `org_deprecated`，现使用 `sys_org` 作为主要组织表。

---

## 🔗 表关系说明

### 核心关系链
```
sys_org (系统组织) ✅ 使用中
  ├── app_user (用户)
  │   ├── indicator (指标)
  │   │   ├── milestone (里程碑)
  │   │   ├── task (任务)
  │   │   └── indicator_milestone (关联)
  │   ├── plan (计划)
  │   │   └── plan_report (计划报告)
  │   └── progress_report (进度报告)
  └── cycle (周期)

org_deprecated (已废弃) ⚠️ 已重命名，不再使用
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
- **用户管理**: app_user, sys_user, sys_role, sys_user_role
- **组织管理**: 
  - `sys_org` - ✅ **正在使用**（29个组织：1个战略发展部 + 19个职能部门 + 8个二级学院 + 1个其他）
  - `org_deprecated` - ⚠️ **已废弃**（已重命名，数据已迁移至sys_org）
- **指标管理**: indicator, indicator_milestone
- **任务管理**: 
  - `task` - 基础任务（与指标关联，40条数据）
  - `adhoc_task` - 临时任务（灵活创建，暂无数据）
  - `strategic_task` - 战略任务（关联计划，暂无数据）
- **计划报告**: plan, plan_report, progress_report
- **审批流程**: audit_flow_def, audit_instance, approval_record
- **预警告警**: warn_rule, alert_rule, alert_event
- **日志审计**: audit_log, common_log, audit_action_log

### 按数据量查找
- **配置表** (数据量小): warn_level, sys_role, sys_permission, sys_org (29条)
- **业务表** (数据量中): indicator (711条), task (40条), milestone
- **日志表** (数据量大): audit_log, common_log, alert_event

---

## ⚠️ 重要变更说明

### sys_org 迁移 (2026-02-10)

**迁移状态**: ✅ **已完成**

**变更内容**:
1. **表结构变更**:
   - 原 `org` 表已重命名为 `org_deprecated`
   - 新 `sys_org` 表作为主要组织表使用
   - 字段名称变更：`org_id` → `id`, `org_name` → `name`, `org_type` → `type`

2. **数据迁移**:
   - 所有组织数据已从 `org` 迁移至 `sys_org`
   - `app_user` 表的外键已更新为指向 `sys_org`
   - `indicator` 表新增 `owner_org_id` 和 `target_org_id` 字段

3. **代码更新**:
   - 后端实体类已更新为使用 `SysOrg`
   - 所有 Repository、Service、Controller 已更新
   - API 响应字段名已更新（`id`, `name`, `type`）

4. **验证结果**:
   - ✅ 应用启动成功
   - ✅ API 端点正常工作
   - ✅ 单元测试通过
   - ✅ 数据完整性验证通过

**详细文档**: 参见 `SYS-ORG-MIGRATION-COMPLETE.md`
