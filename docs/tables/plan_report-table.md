# 计划报告表 (plan_report) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 2

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('plan_report | |
| plan_id | bigint | - | ✓ | - | |
| report_month | character varying | 255 | ✓ | - | |
| created_by | bigint | - | ✓ | - | |
| report_org_type | character varying | 16 | ✓ | - | |
| report_org_id | bigint | - | ✓ | - | |
| status | character varying | 16 | ✓ | 'DRAFT'::character v | |
| submitted_at | timestamp with time zone | - | ✗ | - | |
| remark | text | - | ✗ | - | |
| created_at | timestamp with time zone | - | ✓ | CURRENT_TIMESTAMP | |
| updated_at | timestamp with time zone | - | ✓ | CURRENT_TIMESTAMP | |
| is_deleted | boolean | - | ✓ | false | |

---

## 示例数据

显示前 2 条记录

| id | plan_id | report_month | created_by | report_org_type | report_org_id | status | submitted_at | remark | created_at | updated_at | is_deleted |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 9101 | 9001 | 202602 | 1548 | FUNC_DEPT | 44 | IN_REVIEW | 2026-02-10 | 教务处2月填报（示例） | 2026-02-10 | 2026-02-10 | ✗ |
| 9102 | 9002 | 202602 | 1560 | COLLEGE | 56 | APPROVED | 2026-02-12 | 工学院2月填报（示例） | 2026-02-12 | 2026-02-12 | ✗ |

---

## 统计信息

- 总记录数: 2
- 字段数: 12
