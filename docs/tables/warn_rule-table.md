# warn_rule (warn_rule) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 5

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('warn_rule_i | |
| metric_type | character varying | 16 | ✓ | - | |
| min_gap | numeric | - | ✓ | - | |
| max_gap | numeric | - | ✗ | - | |
| level_id | bigint | - | ✓ | - | |
| message_tpl | character varying | 500 | ✗ | - | |
| sort_order | integer | - | ✓ | 0 | |

---

## 示例数据

显示前 5 条记录

| id | metric_type | min_gap | max_gap | level_id | message_tpl | sort_order |
|---|---|---|---|---|---|---|
| 2 | PROGRESS | -100.00 | 0.00 | 1 | 进度已达到或超过里程碑要求 | 10 |
| 3 | PROGRESS | 0.00 | 10.00 | 2 | 进度略低于里程碑要求（差距 {gap}%） | 20 |
| 4 | PROGRESS | 10.00 | 20.00 | 3 | 进度滞后于里程碑要求（差距 {gap}%），需关注 | 30 |
| 5 | PROGRESS | 20.00 | 30.00 | 4 | 进度严重滞后（差距 {gap}%），请立即整改 | 40 |
| 6 | PROGRESS | 30.00 | NULL | 5 | 进度严重失控（差距 {gap}%），存在重大风险 | 50 |

---

## 统计信息

- 总记录数: 5
- 字段数: 7
