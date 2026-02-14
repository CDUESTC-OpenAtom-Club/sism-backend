# plan_report_indicator_attachment (plan_report_indicator_attachment) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 2

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('plan_report | |
| plan_report_indicator_id | bigint | - | ✓ | - | |
| attachment_id | bigint | - | ✓ | - | |
| sort_order | integer | - | ✓ | 0 | |
| created_by | bigint | - | ✓ | - | |
| created_at | timestamp with time zone | - | ✓ | now() | |

---

## 示例数据

显示前 2 条记录

| id | plan_report_indicator_id | attachment_id | sort_order | created_by | created_at |
|---|---|---|---|---|---|
| 9111001 | 911001 | 1002 | 1 | 1548 | 2026-02-10 |
| 9111002 | 912002 | 1003 | 1 | 1560 | 2026-02-12 |

---

## 统计信息

- 总记录数: 2
- 字段数: 6
