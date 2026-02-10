# 2_warn_event (2_warn_event) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 0

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('warn_event_ | |
| milestone_id | bigint | - | ✓ | - | |
| indicator_id | bigint | - | ✓ | - | |
| event_date | date | - | ✓ | - | |
| metric_type | character varying | 16 | ✓ | - | |
| target_progress | integer | - | ✗ | - | |
| latest_progress | integer | - | ✗ | - | |
| target_value | numeric | - | ✗ | - | |
| latest_value | numeric | - | ✗ | - | |
| gap_value | numeric | - | ✓ | - | |
| level_id | bigint | - | ✓ | - | |
| message | text | - | ✗ | - | |
| is_ack | boolean | - | ✓ | false | |
| ack_by | bigint | - | ✗ | - | |
| ack_at | timestamp with time zone | - | ✗ | - | |
| ack_note | text | - | ✗ | - | |
| calc_at | timestamp with time zone | - | ✓ | CURRENT_TIMESTAMP | |

---

## 数据状态

⚠️ 表中暂无数据

---

## 统计信息

- 总记录数: 0
- 字段数: 17
