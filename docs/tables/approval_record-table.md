# 审批记录表 (approval_record) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 0

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| approval_id | bigint | - | ✓ | nextval('approval_re | |
| acted_at | timestamp without time zone | - | ✓ | - | |
| action | character varying | 255 | ✓ | - | |
| comment | text | - | ✗ | - | |
| approver_id | bigint | - | ✓ | - | |
| report_id | bigint | - | ✓ | - | |

---

## 数据状态

⚠️ 表中暂无数据

---

## 统计信息

- 总记录数: 0
- 字段数: 6
