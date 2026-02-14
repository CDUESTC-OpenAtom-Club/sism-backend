# sys_user (sys_user) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 57

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('user_id_seq | |
| username | character varying | 50 | ✓ | - | |
| real_name | character varying | 50 | ✓ | - | |
| org_id | bigint | - | ✓ | - | |
| password_hash | character varying | 255 | ✓ | - | |
| sso_id | character varying | 100 | ✗ | - | |
| is_active | boolean | - | ✓ | true | |
| created_at | timestamp without time zone | - | ✓ | CURRENT_TIMESTAMP | |
| updated_at | timestamp without time zone | - | ✓ | CURRENT_TIMESTAMP | |

---

## 示例数据

显示前 10 条记录

| id | username | real_name | org_id | password_hash | sso_id | is_active | created_at | updated_at |
|---|---|---|---|---|---|---|---|---|
| 1583 | baowei | 吴保卫 | 42 | $2b$10$W7i/6np.3TGvsFT8VlsvQedIaMpEz7RiuimkIq1kSRG | NULL | ✓ | 2026-01-28 | 2026-01-30 |
| 1584 | zonghe | 郑综合 | 43 | $2b$10$W7i/6np.3TGvsFT8VlsvQedIaMpEz7RiuimkIq1kSRG | NULL | ✓ | 2026-01-28 | 2026-01-30 |
| 1586 | caiwu | 褚财务 | 46 | $2b$10$W7i/6np.3TGvsFT8VlsvQedIaMpEz7RiuimkIq1kSRG | NULL | ✓ | 2026-01-28 | 2026-01-30 |
| 1587 | zhaosheng | 卫招生 | 47 | $2b$10$W7i/6np.3TGvsFT8VlsvQedIaMpEz7RiuimkIq1kSRG | NULL | ✓ | 2026-01-28 | 2026-01-30 |
| 1588 | jiuye | 蒋就业 | 48 | $2b$10$W7i/6np.3TGvsFT8VlsvQedIaMpEz7RiuimkIq1kSRG | NULL | ✓ | 2026-01-28 | 2026-01-30 |
| 1589 | shiyanshi | 沈实验 | 49 | $2b$10$W7i/6np.3TGvsFT8VlsvQedIaMpEz7RiuimkIq1kSRG | NULL | ✓ | 2026-01-28 | 2026-01-30 |
| 1590 | shuzi | 韩数字 | 50 | $2b$10$W7i/6np.3TGvsFT8VlsvQedIaMpEz7RiuimkIq1kSRG | NULL | ✓ | 2026-01-28 | 2026-01-30 |
| 1539 | zhangsan | 张三 | 35 | $2b$10$W7i/6np.3TGvsFT8VlsvQedIaMpEz7RiuimkIq1kSRG | NULL | ✓ | 2026-01-19 | 2026-01-28 |
| 1540 | func_36 | 党委办公室 | 党委统战部负责人 | 36 | $2b$10$W7i/6np.3TGvsFT8VlsvQedIaMpEz7RiuimkIq1kSRG | NULL | ✓ | 2026-01-19 | 2026-01-30 |
| 1541 | func_37 | 纪委办公室 | 监察处负责人 | 37 | $2b$10$W7i/6np.3TGvsFT8VlsvQedIaMpEz7RiuimkIq1kSRG | NULL | ✓ | 2026-01-19 | 2026-01-30 |

---

## 统计信息

- 总记录数: 57
- 字段数: 9
