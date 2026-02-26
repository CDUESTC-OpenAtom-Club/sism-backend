# 2_warn_summary_daily (2_warn_summary_daily) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 0

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('warn_summar | |
| summary_date | date | - | ✓ | - | |
| org_type | character varying | 16 | ✓ | - | |
| org_id | bigint | - | ✓ | - | |
| plan_id | bigint | - | ✗ | - | |
| task_id | bigint | - | ✗ | - | |
| level_id | bigint | - | ✓ | - | |
| warn_count | integer | - | ✓ | 0 | |
| created_at | timestamp with time zone | - | ✓ | CURRENT_TIMESTAMP | |

---

## 数据状态

⚠️ 表中暂无数据

---

## 统计信息

- 总记录数: 0
- 字段数: 9
