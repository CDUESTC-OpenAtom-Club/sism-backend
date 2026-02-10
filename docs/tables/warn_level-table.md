# warn_level (warn_level) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 5

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('warn_level_ | |
| level_code | character varying | 32 | ✓ | - | |
| level_name | character varying | 64 | ✓ | - | |
| severity | integer | - | ✓ | - | |
| remark | text | - | ✗ | - | |

---

## 示例数据

显示前 5 条记录

| id | level_code | level_name | severity | remark |
|---|---|---|---|---|
| 1 | OK | 正常 | 0 | NULL |
| 2 | INFO | 提示 | 10 | NULL |
| 3 | WARN | 预警 | 20 | NULL |
| 4 | MAJOR | 严重 | 30 | NULL |
| 5 | CRITICAL | 危急 | 40 | NULL |

---

## 统计信息

- 总记录数: 5
- 字段数: 5
