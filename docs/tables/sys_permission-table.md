# sys_permission (sys_permission) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 12

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('sys_permiss | |
| perm_code | character varying | 128 | ✓ | - | |
| perm_name | character varying | 128 | ✓ | - | |
| perm_type | character varying | 16 | ✓ | - | |
| parent_id | bigint | - | ✗ | - | |
| route_path | character varying | 256 | ✗ | - | |
| page_key | character varying | 128 | ✗ | - | |
| action_key | character varying | 128 | ✗ | - | |
| sort_order | integer | - | ✓ | 0 | |
| is_enabled | boolean | - | ✓ | true | |
| remark | text | - | ✗ | - | |
| created_at | timestamp with time zone | - | ✓ | CURRENT_TIMESTAMP | |
| updated_at | timestamp with time zone | - | ✓ | CURRENT_TIMESTAMP | |

---

## 示例数据

显示前 10 条记录

| id | perm_code | perm_name | perm_type | parent_id | route_path | page_key | action_key | sort_order | is_enabled | remark | created_at | updated_at |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | PAGE_DASHBOARD | 数据看板 | PAGE | NULL | /dashboard | dashboard | NULL | 10 | ✓ | 各级组织数据看板（按角色与组织归属范围展示） | 2026-02-03 | 2026-02-03 |
| 2 | PAGE_STRATEGY_TASK | 战略任务管理 | PAGE | NULL | /strategy/task | strategy_task | NULL | 20 | ✓ | 战略发展部使用：战略任务下发、审核、填报审核 | 2026-02-03 | 2026-02-03 |
| 3 | PAGE_DATA_REPORT | 数据填报 | PAGE | NULL | /data/report | data_report | NULL | 30 | ✓ | 职能部门/二级学院：按指标进行数据填报与提交 | 2026-02-03 | 2026-02-03 |
| 4 | PAGE_INDICATOR_DISPATCH | 指标下发与审批 | PAGE | NULL | /indicator/dispatch | indicator_dispatch | NULL | 40 | ✓ | 职能部门使用：指标下发（提交）、下发审核、填报审核 | 2026-02-03 | 2026-02-03 |
| 5 | BTN_STRATEGY_TASK_DISPATCH_SUBMIT | 下发（提交） | BUTTON | 2 | NULL | NULL | dispatch_submit | 10 | ✓ | 战略任务下发提交（发起审批） | 2026-02-03 | 2026-02-03 |
| 6 | BTN_STRATEGY_TASK_DISPATCH_APPROVE | 下发审核 | BUTTON | 2 | NULL | NULL | dispatch_approve | 20 | ✓ | 战略任务下发审批（战略发展负责人/分管校长共用） | 2026-02-03 | 2026-02-03 |
| 7 | BTN_STRATEGY_TASK_REPORT_APPROVE | 填报审核 | BUTTON | 2 | NULL | NULL | report_approve | 30 | ✓ | 战略任务填报审核（用于审核填报数据） | 2026-02-03 | 2026-02-03 |
| 8 | BTN_DATA_REPORT_SUBMIT | 提交 | BUTTON | 3 | NULL | NULL | submit | 10 | ✓ | 数据填报提交（发起审批） | 2026-02-03 | 2026-02-03 |
| 9 | BTN_DATA_REPORT_APPROVE | 审核 | BUTTON | 3 | NULL | NULL | approve | 20 | ✓ | 数据填报审核（各级负责人共用） | 2026-02-03 | 2026-02-03 |
| 10 | BTN_INDICATOR_DISPATCH_SUBMIT | 下发（提交） | BUTTON | 4 | NULL | NULL | dispatch_submit | 10 | ✓ | 指标下发提交（发起审批） | 2026-02-03 | 2026-02-03 |

---

## 统计信息

- 总记录数: 12
- 字段数: 13
