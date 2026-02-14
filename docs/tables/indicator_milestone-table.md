# indicator_milestone (indicator_milestone) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 60

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('indicator_m | |
| indicator_id | bigint | - | ✓ | - | |
| milestone_name | character varying | 200 | ✓ | - | |
| milestone_desc | text | - | ✗ | - | |
| due_date | date | - | ✓ | - | |
| status | USER-DEFINED | - | ✓ | 'NOT_STARTED'::miles | |
| sort_order | integer | - | ✓ | 0 | |
| created_at | timestamp without time zone | - | ✓ | CURRENT_TIMESTAMP | |
| updated_at | timestamp without time zone | - | ✓ | CURRENT_TIMESTAMP | |
| target_progress | integer | - | ✗ | 0 | |
| is_paired | boolean | - | ✗ | false | |
| inherited_from | bigint | - | ✗ | - | |

---

## 示例数据

显示前 10 条记录

| id | indicator_id | milestone_name | milestone_desc | due_date | status | sort_order | created_at | updated_at | target_progress | is_paired | inherited_from |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 12407 | 13422 | 重点学科建设进度 - 党委办公室 | 党委统战部-4月 | NULL | 2026-04-30 | NOT_STARTED | 3 | 2026-02-09 | 2026-02-09 | 0 | ✗ | NULL |
| 12408 | 13422 | 重点学科建设进度 - 党委办公室 | 党委统战部-1月 | NULL | 2026-01-31 | NOT_STARTED | 0 | 2026-02-09 | 2026-02-09 | 0 | ✗ | NULL |
| 12409 | 13422 | 重点学科建设进度 - 党委办公室 | 党委统战部-6月 | NULL | 2026-06-30 | NOT_STARTED | 5 | 2026-02-09 | 2026-02-09 | 0 | ✗ | NULL |
| 12410 | 13422 | 重点学科建设进度 - 党委办公室 | 党委统战部-2月 | NULL | 2026-02-28 | NOT_STARTED | 1 | 2026-02-09 | 2026-02-09 | 0 | ✗ | NULL |
| 12411 | 13422 | 重点学科建设进度 - 党委办公室 | 党委统战部-5月 | NULL | 2026-05-31 | NOT_STARTED | 4 | 2026-02-09 | 2026-02-09 | 0 | ✗ | NULL |
| 12412 | 13422 | 重点学科建设进度 - 党委办公室 | 党委统战部-3月 | NULL | 2026-03-31 | NOT_STARTED | 2 | 2026-02-09 | 2026-02-09 | 0 | ✗ | NULL |
| 12413 | 13422 | 重点学科建设进度 - 党委办公室 | 党委统战部-7月 | NULL | 2026-07-31 | NOT_STARTED | 6 | 2026-02-09 | 2026-02-09 | 0 | ✗ | NULL |
| 12414 | 13422 | 重点学科建设进度 - 党委办公室 | 党委统战部-8月 | NULL | 2026-08-31 | NOT_STARTED | 7 | 2026-02-09 | 2026-02-09 | 0 | ✗ | NULL |
| 12415 | 13422 | 重点学科建设进度 - 党委办公室 | 党委统战部-10月 | NULL | 2026-10-31 | NOT_STARTED | 9 | 2026-02-09 | 2026-02-09 | 0 | ✗ | NULL |
| 12416 | 13422 | 重点学科建设进度 - 党委办公室 | 党委统战部-12月 | NULL | 2026-12-31 | NOT_STARTED | 11 | 2026-02-09 | 2026-02-09 | 0 | ✗ | NULL |

---

## 统计信息

- 总记录数: 60
- 字段数: 12
