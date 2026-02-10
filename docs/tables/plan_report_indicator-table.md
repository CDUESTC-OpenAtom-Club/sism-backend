# plan_report_indicator (plan_report_indicator) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 4

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('plan_report | |
| report_id | bigint | - | ✓ | - | |
| indicator_id | bigint | - | ✓ | - | |
| progress | integer | - | ✓ | 0 | |
| milestone_note | text | - | ✗ | - | |
| comment | text | - | ✗ | - | |
| created_at | timestamp with time zone | - | ✓ | CURRENT_TIMESTAMP | |

---

## 示例数据

显示前 4 条记录

| id | report_id | indicator_id | progress | milestone_note | comment | created_at |
|---|---|---|---|---|---|---|
| 911001 | 9101 | 91101 | 35 | 2月里程碑：完成初评数据收集 | 进度偏慢，需加速 | 2026-02-10 |
| 911002 | 9101 | 91202 | 20 | 2月里程碑：数据治理方案评审通过 | 等待会议排期 | 2026-02-10 |
| 912001 | 9102 | 92101 | 60 | 2月里程碑：课程改革方案提交教务处 | 按计划推进 | 2026-02-12 |
| 912002 | 9102 | 92202 | 55 | 2月里程碑：完成一次内部质量评估 | 需补充佐证材料 | 2026-02-12 |

---

## 统计信息

- 总记录数: 4
- 字段数: 7
