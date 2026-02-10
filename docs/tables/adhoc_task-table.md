# adhoc_task (adhoc_task) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 0

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| adhoc_task_id | bigint | - | ✓ | nextval('adhoc_task_ | |
| created_at | timestamp without time zone | - | ✓ | - | |
| updated_at | timestamp without time zone | - | ✓ | - | |
| due_at | date | - | ✗ | - | |
| include_in_alert | boolean | - | ✓ | - | |
| open_at | date | - | ✗ | - | |
| require_indicator_report | boolean | - | ✓ | - | |
| scope_type | character varying | 255 | ✓ | - | |
| status | character varying | 255 | ✓ | - | |
| task_desc | text | - | ✗ | - | |
| task_title | character varying | 200 | ✓ | - | |
| creator_org_id | bigint | - | ✓ | - | |
| cycle_id | bigint | - | ✓ | - | |
| indicator_id | bigint | - | ✗ | - | |

---

## 数据状态

⚠️ 表中暂无数据

---

## 统计信息

- 总记录数: 0
- 字段数: 14
