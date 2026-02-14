# 进度报告表 (progress_report) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 0

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| report_id | bigint | - | ✓ | nextval('progress_re | |
| created_at | timestamp without time zone | - | ✓ | - | |
| updated_at | timestamp without time zone | - | ✓ | - | |
| achieved_milestone | boolean | - | ✓ | - | |
| is_final | boolean | - | ✓ | - | |
| narrative | text | - | ✗ | - | |
| percent_complete | numeric | - | ✓ | - | |
| reported_at | timestamp without time zone | - | ✗ | - | |
| status | character varying | 255 | ✓ | - | |
| version_no | integer | - | ✓ | - | |
| adhoc_task_id | bigint | - | ✗ | - | |
| indicator_id | bigint | - | ✓ | - | |
| milestone_id | bigint | - | ✗ | - | |
| reporter_id | bigint | - | ✓ | - | |

---

## 数据状态

⚠️ 表中暂无数据

---

## 统计信息

- 总记录数: 0
- 字段数: 14
