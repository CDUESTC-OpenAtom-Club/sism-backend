# strategic_task (strategic_task) 完整数据

**生成时间**: 2026-02-13 (更新)
**数据库**: strategic (PostgreSQL)
**总记录数**: 4

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| task_id | bigint | - | ✓ | nextval('strategic_t | |
| created_at | timestamp without time zone | - | ✓ | - | |
| updated_at | timestamp without time zone | - | ✓ | - | |
| remark | text | - | ✗ | - | |
| sort_order | integer | - | ✓ | - | |
| task_desc | text | - | ✗ | - | |
| task_name | character varying | 200 | ✓ | - | |
| task_type | character varying | 255 | ✓ | - | |
| created_by_org_id | bigint | - | ✓ | - | |
| cycle_id | bigint | - | ✓ | - | |
| org_id | bigint | - | ✓ | - | |
| is_deleted | boolean | - | ✓ | - | |
| plan_id | bigint | - | ✓ | - | |
| name | character varying | 255 | ✓ | - | |
| type | USER-DEFINED | - | ✓ | - | |

---

## 数据状态

✅ 表中有数据 (4 条记录，关联 sys_org 表)

---

## 统计信息

- 总记录数: 4
- 字段数: 15
