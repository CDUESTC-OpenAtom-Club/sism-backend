# common_log (common_log) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 0

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| log_id | bigint | - | ✓ | nextval('common_log_ | |
| entity_type | USER-DEFINED | - | ✓ | - | |
| entity_id | bigint | - | ✓ | - | |
| action | USER-DEFINED | - | ✓ | - | |
| before_json | jsonb | - | ✗ | - | |
| after_json | jsonb | - | ✗ | - | |
| changed_fields | jsonb | - | ✗ | - | |
| reason | text | - | ✗ | - | |
| actor_user_id | bigint | - | ✗ | - | |
| actor_org_id | bigint | - | ✗ | - | |
| created_at | timestamp without time zone | - | ✓ | CURRENT_TIMESTAMP | |

---

## 数据状态

⚠️ 表中暂无数据

---

## 统计信息

- 总记录数: 0
- 字段数: 11
