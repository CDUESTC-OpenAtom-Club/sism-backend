# sys_org (sys_org) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 29

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('org_id_seq' | |
| name | character varying | 100 | ✓ | - | |
| type | USER-DEFINED | - | ✓ | - | |
| parent_org_id | bigint | - | ✗ | - | |
| is_active | boolean | - | ✓ | true | |
| sort_order | integer | - | ✓ | 0 | |
| created_at | timestamp without time zone | - | ✓ | CURRENT_TIMESTAMP | |
| updated_at | timestamp without time zone | - | ✓ | CURRENT_TIMESTAMP | |

---

## 示例数据

显示前 10 条记录

| id | name | type | parent_org_id | is_active | sort_order | created_at | updated_at |
|---|---|---|---|---|---|---|---|
| 35 | 战略发展部 | STRATEGY_DEPT | NULL | ✓ | 0 | 2026-01-19 | 2026-01-19 |
| 36 | 党委办公室 | 党委统战部 | FUNCTIONAL_DEPT | NULL | ✓ | 1 | 2026-01-19 | 2026-01-19 |
| 37 | 纪委办公室 | 监察处 | FUNCTIONAL_DEPT | NULL | ✓ | 2 | 2026-01-19 | 2026-01-19 |
| 38 | 党委宣传部 | 宣传策划部 | FUNCTIONAL_DEPT | NULL | ✓ | 3 | 2026-01-19 | 2026-01-19 |
| 39 | 党委组织部 | 党委教师工作部 | FUNCTIONAL_DEPT | NULL | ✓ | 4 | 2026-01-19 | 2026-01-19 |
| 40 | 人力资源部 | FUNCTIONAL_DEPT | NULL | ✓ | 5 | 2026-01-19 | 2026-01-19 |
| 41 | 党委学工部 | 学生工作处 | FUNCTIONAL_DEPT | NULL | ✓ | 6 | 2026-01-19 | 2026-01-19 |
| 42 | 党委保卫部 | 保卫处 | FUNCTIONAL_DEPT | NULL | ✓ | 7 | 2026-01-19 | 2026-01-19 |
| 43 | 学校综合办公室 | FUNCTIONAL_DEPT | NULL | ✓ | 8 | 2026-01-19 | 2026-01-19 |
| 44 | 教务处 | FUNCTIONAL_DEPT | NULL | ✓ | 9 | 2026-01-19 | 2026-01-19 |

---

## 统计信息

- 总记录数: 29
- 字段数: 8
