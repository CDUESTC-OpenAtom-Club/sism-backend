# audit_action_log (audit_action_log) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 10

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('audit_actio | |
| instance_id | bigint | - | ✓ | - | |
| step_id | bigint | - | ✗ | - | |
| action | character varying | 32 | ✓ | - | |
| from_step_id | bigint | - | ✗ | - | |
| to_step_id | bigint | - | ✗ | - | |
| operator_id | bigint | - | ✓ | - | |
| comment | character varying | 2000 | ✗ | - | |
| created_at | timestamp with time zone | - | ✓ | now() | |

---

## 示例数据

显示前 10 条记录

| id | instance_id | step_id | action | from_step_id | to_step_id | operator_id | comment | created_at |
|---|---|---|---|---|---|---|---|---|
| 99101 | 99001 | 1 | SUBMIT | NULL | 2 | 1576 | 提交下发（示例） | 2026-02-01 |
| 99102 | 99002 | 4 | SUBMIT | NULL | 5 | 1548 | 教务处提交下发至工学院（示例） | 2026-02-02 |
| 99103 | 99002 | 5 | APPROVE | 5 | 6 | 1548 | 职能部门负责人审批通过（示例） | 2026-02-02 |
| 99104 | 99002 | 6 | APPROVE | 6 | NULL | 1538 | 分管校领导审批通过（示例） | 2026-02-02 |
| 99105 | 99003 | 7 | SUBMIT | NULL | 8 | 1548 | 教务处提交月度填报（示例） | 2026-02-10 |
| 99106 | 99004 | 11 | SUBMIT | NULL | 12 | 1560 | 工学院提交月度填报（示例） | 2026-02-12 |
| 99107 | 99004 | 12 | APPROVE | 12 | 13 | 1560 | 学院院长审批通过（示例） | 2026-02-12 |
| 99108 | 99004 | 13 | AUTO_APPROVE | 13 | 14 | 1548 | 负责人填报触发自动通过（示例） | 2026-02-12 |
| 99109 | 99004 | 14 | APPROVE | 14 | 15 | 1538 | 分管校领导审批通过（示例） | 2026-02-12 |
| 99110 | 99004 | 15 | APPROVE | 15 | NULL | 1576 | 战略发展部确认归档（示例） | 2026-02-12 |

---

## 统计信息

- 总记录数: 10
- 字段数: 9
