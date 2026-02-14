# idempotency_records (idempotency_records) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 0

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('idempotency | |
| created_at | timestamp without time zone | - | ✓ | - | |
| expires_at | timestamp without time zone | - | ✓ | - | |
| http_method | character varying | 10 | ✗ | - | |
| idempotency_key | character varying | 64 | ✓ | - | |
| request_path | character varying | 255 | ✗ | - | |
| response_body | text | - | ✗ | - | |
| status | character varying | 20 | ✗ | - | |
| status_code | integer | - | ✗ | - | |

---

## 数据状态

⚠️ 表中暂无数据

---

## 统计信息

- 总记录数: 0
- 字段数: 9
