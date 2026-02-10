# audit_instance (audit_instance) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 4

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('audit_insta | |
| flow_id | bigint | - | ✓ | - | |
| biz_type | character varying | 32 | ✓ | - | |
| biz_id | bigint | - | ✓ | - | |
| current_step_id | bigint | - | ✗ | - | |
| status | character varying | 32 | ✓ | 'DRAFT'::character v | |
| started_at | timestamp with time zone | - | ✗ | - | |
| finished_at | timestamp with time zone | - | ✗ | - | |
| created_by | bigint | - | ✓ | - | |
| created_at | timestamp with time zone | - | ✓ | now() | |
| updated_at | timestamp with time zone | - | ✓ | now() | |

---

## 示例数据

显示前 4 条记录

| id | flow_id | biz_type | biz_id | current_step_id | status | started_at | finished_at | created_by | created_at | updated_at |
|---|---|---|---|---|---|---|---|---|---|---|
| 99001 | 1 | PLAN | 9001 | 2 | IN_REVIEW | 2026-02-01 | NULL | 1576 | 2026-02-01 | 2026-02-01 |
| 99002 | 2 | PLAN | 9002 | 6 | APPROVED | 2026-02-02 | 2026-02-02 | 1548 | 2026-02-02 | 2026-02-02 |
| 99003 | 3 | PLAN_REPORT | 9101 | 8 | IN_REVIEW | 2026-02-10 | NULL | 1548 | 2026-02-10 | 2026-02-10 |
| 99004 | 4 | PLAN_REPORT | 9102 | 15 | APPROVED | 2026-02-12 | 2026-02-12 | 1560 | 2026-02-12 | 2026-02-12 |

---

## 统计信息

- 总记录数: 4
- 字段数: 11
