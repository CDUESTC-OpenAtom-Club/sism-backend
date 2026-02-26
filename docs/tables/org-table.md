# 组织表 (org) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 27

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| org_id | bigint | - | ✓ | nextval('org_org_id_ | |
| created_at | timestamp without time zone | - | ✓ | - | |
| updated_at | timestamp without time zone | - | ✓ | - | |
| is_active | boolean | - | ✓ | - | |
| org_name | character varying | 100 | ✓ | - | |
| org_type | character varying | 255 | ✓ | - | |
| sort_order | integer | - | ✓ | - | |

---

## 示例数据

显示前 10 条记录

| org_id | created_at | updated_at | is_active | org_name | org_type | sort_order |
|---|---|---|---|---|---|---|
| 100 | 2026-02-11 | 2026-02-11 | ✓ | 马克思主义学院 | COLLEGE | 1 |
| 101 | 2026-02-11 | 2026-02-11 | ✓ | 工学院 | COLLEGE | 2 |
| 102 | 2026-02-11 | 2026-02-11 | ✓ | 计算机学院 | COLLEGE | 3 |
| 103 | 2026-02-11 | 2026-02-11 | ✓ | 商学院 | COLLEGE | 4 |
| 104 | 2026-02-11 | 2026-02-11 | ✓ | 文理学院 | COLLEGE | 5 |
| 105 | 2026-02-11 | 2026-02-11 | ✓ | 艺术与科技学院 | COLLEGE | 6 |
| 106 | 2026-02-11 | 2026-02-11 | ✓ | 航空学院 | COLLEGE | 7 |
| 107 | 2026-02-11 | 2026-02-11 | ✓ | 国际教育学院 | COLLEGE | 8 |
| 200 | 2026-02-11 | 2026-02-11 | ✓ | 党委办公室 | 党委联络部 | FUNCTIONAL_DEPT | 1 |
| 201 | 2026-02-11 | 2026-02-11 | ✓ | 纪委办公室 | 监察处 | FUNCTIONAL_DEPT | 2 |

---

## 统计信息

- 总记录数: 27
- 字段数: 7
