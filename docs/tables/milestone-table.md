# 里程碑表 (milestone) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 0

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| milestone_id | bigint | - | ✓ | nextval('milestone_m | |
| created_at | timestamp without time zone | - | ✓ | - | |
| updated_at | timestamp without time zone | - | ✓ | - | |
| due_date | date | - | ✓ | - | |
| is_paired | boolean | - | ✗ | - | |
| milestone_desc | text | - | ✗ | - | |
| milestone_name | character varying | 200 | ✓ | - | |
| sort_order | integer | - | ✓ | - | |
| status | character varying | 255 | ✓ | - | |
| target_progress | integer | - | ✗ | - | |
| weight_percent | numeric | - | ✓ | - | |
| indicator_id | bigint | - | ✓ | - | |
| inherited_from | bigint | - | ✗ | - | |

---

## 数据状态

⚠️ 表中暂无数据

---

## 统计信息

- 总记录数: 0
- 字段数: 13
