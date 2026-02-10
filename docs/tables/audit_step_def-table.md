# audit_step_def (audit_step_def) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 15

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('audit_step_ | |
| flow_id | bigint | - | ✓ | - | |
| step_no | integer | - | ✓ | - | |
| step_code | character varying | 64 | ✓ | - | |
| step_name | character varying | 128 | ✓ | - | |
| role_id | bigint | - | ✓ | - | |
| is_terminal | boolean | - | ✓ | false | |
| created_at | timestamp with time zone | - | ✓ | now() | |
| updated_at | timestamp with time zone | - | ✓ | now() | |

---

## 示例数据

显示前 10 条记录

| id | flow_id | step_no | step_code | step_name | role_id | is_terminal | created_at | updated_at |
|---|---|---|---|---|---|---|---|---|
| 12 | 4 | 20 | COLLEGE_DEAN_APPROVE | 学院院长审批 | 7 | ✗ | 2026-02-03 | 2026-02-03 |
| 5 | 2 | 20 | FUNC_DEPT_HEAD_APPROVE | 职能部门负责人审批 | 6 | ✗ | 2026-02-03 | 2026-02-03 |
| 8 | 3 | 20 | FUNC_DEPT_HEAD_APPROVE | 职能部门负责人审批 | 6 | ✗ | 2026-02-03 | 2026-02-03 |
| 13 | 4 | 30 | FUNC_DEPT_HEAD_APPROVE | 分管职能部门负责人审批 | 6 | ✗ | 2026-02-03 | 2026-02-03 |
| 1 | 1 | 10 | DRAFTER_SUBMIT | 填报人提交 | 5 | ✗ | 2026-02-03 | 2026-02-03 |
| 4 | 2 | 10 | DRAFTER_SUBMIT | 填报人提交 | 5 | ✗ | 2026-02-03 | 2026-02-03 |
| 7 | 3 | 10 | DRAFTER_SUBMIT | 填报人提交 | 5 | ✗ | 2026-02-03 | 2026-02-03 |
| 11 | 4 | 10 | DRAFTER_SUBMIT | 填报人提交 | 5 | ✗ | 2026-02-03 | 2026-02-03 |
| 2 | 1 | 20 | STRATEGY_DEPT_HEAD_APPROVE | 战略发展部负责人审批 | 8 | ✗ | 2026-02-03 | 2026-02-03 |
| 10 | 3 | 40 | STRATEGY_OFFICE_APPROVE | 战略发展部确认 | 10 | ✓ | 2026-02-03 | 2026-02-03 |

---

## 统计信息

- 总记录数: 15
- 字段数: 9
