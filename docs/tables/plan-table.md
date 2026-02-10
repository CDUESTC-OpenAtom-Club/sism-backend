# 计划表 (plan) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 2

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('plan_id_seq | |
| cycle_id | bigint | - | ✓ | - | |
| created_at | timestamp without time zone | - | ✓ | - | |
| updated_at | timestamp without time zone | - | ✓ | - | |
| is_deleted | boolean | - | ✓ | false | |
| target_org_id | bigint | - | ✓ | - | |
| created_by_org_id | bigint | - | ✓ | - | |
| plan_level | USER-DEFINED | - | ✓ | - | |
| status | character varying | - | ✓ | 'DRAFT'::character v | |

---

## 示例数据

显示前 2 条记录

| id | cycle_id | created_at | updated_at | is_deleted | target_org_id | created_by_org_id | plan_level | status |
|---|---|---|---|---|---|---|---|---|
| 9001 | 90 | 2026-02-01 | 2026-02-01 | ✗ | 44 | 35 | STRAT_TO_FUNC | IN_REVIEW |
| 9002 | 90 | 2026-02-02 | 2026-02-02 | ✗ | 56 | 44 | FUNC_TO_COLLEGE | APPROVED |

---

## 统计信息

- 总记录数: 2
- 字段数: 9
