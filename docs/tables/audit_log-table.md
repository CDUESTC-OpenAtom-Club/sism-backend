# 审计日志表 (audit_log) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 0

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| log_id | bigint | - | ✓ | nextval('audit_log_l | |
| action | character varying | 255 | ✓ | - | |
| after_json | jsonb | - | ✗ | - | |
| before_json | jsonb | - | ✗ | - | |
| changed_fields | jsonb | - | ✗ | - | |
| created_at | timestamp without time zone | - | ✓ | - | |
| entity_id | bigint | - | ✓ | - | |
| entity_type | character varying | 255 | ✓ | - | |
| reason | text | - | ✗ | - | |
| actor_org_id | bigint | - | ✗ | - | |
| actor_user_id | bigint | - | ✗ | - | |

---

## 数据状态

⚠️ 表中暂无数据

---

## 统计信息

- 总记录数: 0
- 字段数: 11
