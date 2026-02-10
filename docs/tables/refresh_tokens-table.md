# 刷新令牌表 (refresh_tokens) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 7

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('refresh_tok | |
| created_at | timestamp without time zone | - | ✓ | - | |
| device_info | character varying | 255 | ✗ | - | |
| expires_at | timestamp without time zone | - | ✓ | - | |
| ip_address | character varying | 45 | ✗ | - | |
| revoked_at | timestamp without time zone | - | ✗ | - | |
| token_hash | character varying | 64 | ✓ | - | |
| user_id | bigint | - | ✓ | - | |

---

## 示例数据

显示前 7 条记录

| id | created_at | device_info | expires_at | ip_address | revoked_at | token_hash | user_id |
|---|---|---|---|---|---|---|---|
| 565 | 2026-02-10 | curl/8.7.1 | 2026-02-17 | 0:0:0:0:0:0:0:1 | NULL | 075bbd68fc4f7d336834098081a12a4124ec822f543ea22e2c | 1 |
| 560 | 2026-02-10 | curl/8.7.1 | 2026-02-17 | 0:0:0:0:0:0:0:1 | 2026-02-10 | 91d21114837244dd392b5d1dde2037759f03aaf97fe3639290 | 1 |
| 566 | 2026-02-10 | curl/8.7.1 | 2026-02-17 | 0:0:0:0:0:0:0:1 | NULL | 78f9a03a6599926277d5b5cd66501fde663891eb35c5993c9f | 1 |
| 561 | 2026-02-10 | curl/8.7.1 | 2026-02-17 | 0:0:0:0:0:0:0:1 | 2026-02-10 | 2bbe233f2c7e6775d63b1e247b77aabb20d65cff34219967cd | 1 |
| 562 | 2026-02-10 | curl/8.7.1 | 2026-02-17 | 0:0:0:0:0:0:0:1 | NULL | 430f220ed18b40360b1ceeaeec9b1af3f4d793ef899d370dac | 1 |
| 563 | 2026-02-10 | curl/8.7.1 | 2026-02-17 | 0:0:0:0:0:0:0:1 | NULL | 05e27a46563364675ed295834b57401679cea97b053eac245d | 1 |
| 564 | 2026-02-10 | curl/8.7.1 | 2026-02-17 | 0:0:0:0:0:0:0:1 | NULL | b9997123b272329c9576f1730ab6a77965aa6d8ab91acaa7e9 | 1 |

---

## 统计信息

- 总记录数: 7
- 字段数: 8
