# 用户表 (app_user) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 1

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| user_id | bigint | - | ✓ | nextval('app_user_us | |
| created_at | timestamp without time zone | - | ✓ | - | |
| updated_at | timestamp without time zone | - | ✓ | - | |
| is_active | boolean | - | ✓ | - | |
| password_hash | character varying | 255 | ✓ | - | |
| real_name | character varying | 50 | ✓ | - | |
| sso_id | character varying | 100 | ✗ | - | |
| username | character varying | 50 | ✓ | - | |
| org_id | bigint | - | ✓ | - | |

---

## 示例数据

显示前 1 条记录

| user_id | created_at | updated_at | is_active | password_hash | real_name | sso_id | username | org_id |
|---|---|---|---|---|---|---|---|---|
| 1 | 2026-02-10 | 2026-02-10 | ✓ | $2a$10$a..SZwK76zQnA7MEa5LeaOsOC2YeVfGTKZ0bLF3Sm00 | 保卫处测试用户 | NULL | baowei | 205 |

---

## 统计信息

- 总记录数: 1
- 字段数: 9
