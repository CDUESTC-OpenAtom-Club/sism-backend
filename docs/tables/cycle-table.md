# 周期表 (cycle) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 4

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('cycle_id_se | |
| cycle_name | character varying | 100 | ✓ | - | |
| year | integer | - | ✓ | - | |
| start_date | date | - | ✓ | - | |
| end_date | date | - | ✓ | - | |
| description | text | - | ✗ | - | |
| created_at | timestamp without time zone | - | ✓ | CURRENT_TIMESTAMP | |
| updated_at | timestamp without time zone | - | ✓ | CURRENT_TIMESTAMP | |

---

## 示例数据

显示前 4 条记录

| id | cycle_name | year | start_date | end_date | description | created_at | updated_at |
|---|---|---|---|---|---|---|---|
| 5 | 2023年度战略目标考核 | 2023 | 2023-01-01 | 2023-12-31 | 2023年度学校战略发展目标考核周期 | 2026-01-19 | 2026-01-19 |
| 6 | 2024年度战略目标考核 | 2024 | 2024-01-01 | 2024-12-31 | 2024年度学校战略发展目标考核周期 | 2026-01-19 | 2026-01-19 |
| 7 | 2025年度战略目标考核 | 2025 | 2025-01-01 | 2025-12-31 | 2025年度学校战略发展目标考核周期 | 2026-01-19 | 2026-01-19 |
| 90 | 2026年度战略目标考核（示例数据） | 2026 | 2026-01-01 | 2026-12-31 | 用于演示 plan / report / 审批流的示例周期 | 2026-02-05 | 2026-02-05 |

---

## 统计信息

- 总记录数: 4
- 字段数: 8
