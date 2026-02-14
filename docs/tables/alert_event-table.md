# alert_event (alert_event) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 0

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| event_id | bigint | - | ✓ | nextval('alert_event | |
| created_at | timestamp without time zone | - | ✓ | - | |
| updated_at | timestamp without time zone | - | ✓ | - | |
| actual_percent | numeric | - | ✓ | - | |
| detail_json | jsonb | - | ✗ | - | |
| expected_percent | numeric | - | ✓ | - | |
| gap_percent | numeric | - | ✓ | - | |
| handled_note | text | - | ✗ | - | |
| severity | character varying | 255 | ✓ | - | |
| status | character varying | 255 | ✓ | - | |
| handled_by | bigint | - | ✗ | - | |
| indicator_id | bigint | - | ✓ | - | |
| rule_id | bigint | - | ✓ | - | |
| window_id | bigint | - | ✓ | - | |

---

## 数据状态

⚠️ 表中暂无数据

---

## 统计信息

- 总记录数: 0
- 字段数: 14
