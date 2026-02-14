# 指标表 (indicator) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 711

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('indicator_i | |
| task_id | bigint | - | ✓ | - | |
| parent_indicator_id | bigint | - | ✗ | - | |
| level | character varying | - | ✓ | - | 指标级别 (IndicatorLevel枚举) |
| owner_org_id | bigint | - | ✓ | - | 所属部门ID (外键 → sys_org.id) |
| target_org_id | bigint | - | ✓ | - | 目标部门ID (外键 → sys_org.id) |
| indicator_desc | text | - | ✓ | - | |
| weight_percent | numeric | - | ✓ | 0 | |
| sort_order | integer | - | ✓ | 0 | |
| remark | text | - | ✗ | - | |
| created_at | timestamp without time zone | - | ✓ | CURRENT_TIMESTAMP | |
| updated_at | timestamp without time zone | - | ✓ | CURRENT_TIMESTAMP | |
| type | character varying | 20 | ✓ | '定量'::character vary | |
| progress | integer | - | ✗ | 0 | |
| is_deleted | boolean | - | ✓ | false | |
| status | character varying | 20 | ✗ | 'ACTIVE' | 指标状态 (IndicatorStatus枚举，默认ACTIVE) |
| indicator_id | bigint | - | ✓ | nextval('indicator_i | |
| actual_value | numeric | - | ✗ | - | |
| can_withdraw | boolean | - | ✗ | - | |
| is_qualitative | boolean | - | ✗ | - | |
| pending_attachments | jsonb | - | ✗ | - | |
| pending_progress | integer | - | ✗ | - | |
| pending_remark | text | - | ✗ | - | |
| progress_approval_status | character varying | 20 | ✗ | - | |
| responsible_person | character varying | 100 | ✗ | - | |
| status_audit | jsonb | - | ✗ | - | |
| target_value | numeric | - | ✗ | - | |
| type1 | character varying | 20 | ✗ | - | |
| type2 | character varying | 20 | ✗ | - | |
| unit | character varying | 50 | ✗ | - | |
| owner_dept | character varying | 100 | ✗ | - | |
| responsible_dept | character varying | 100 | ✗ | - | |
| year | integer | - | ✗ | - | |

---

## 示例数据

显示前 10 条记录

| id | task_id | parent_indicator_id | indicator_desc | weight_percent | sort_order | remark | created_at | updated_at | type | progress | is_deleted | indicator_id | actual_value | can_withdraw | is_qualitative | pending_attachments | pending_progress | pending_remark | progress_approval_status | responsible_person | status_audit | target_value | type1 | type2 | unit | owner_dept | responsible_dept | year |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 14885 | 20 | NULL | 测试 | 100.00 | 0 | 测试 | 2026-02-09 | 2026-02-09 | 基础性 | 0 | ✗ | 700 | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | 战略发展部 | 未分配 | 2026 |
| 13422 | 27 | NULL | 重点学科建设进度 - 党委办公室 | 党委统战部 | 20.00 | 5 |  | 2026-01-19 | 2026-02-09 | 定量 | 5 | ✗ | 52 | NULL | ✓ | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | 战略发展部 | 党委办公室 | 党委统战部 | 2026 |
| 14010 | 20 | NULL | 加强科研创新指标 - 党委办公室 | 党委统战部 | 20.00 | 1 | NULL | 2026-01-19 | 2026-02-09 | 定量 | 0 | ✗ | 469 | NULL | ✓ | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | 战略发展部 | 党委办公室 | 党委统战部 | 2026 |
| 13970 | 40 | NULL | 本学院论文发表数 - 艺术与科技学院 | 29.00 | 2 | 统计学院师生发表的学术论文数量，按期刊级别分类统计 | 2026-01-19 | 2026-02-09 | 定量 | 0 | ✗ | 570 | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | 科技处 | 艺术与科技学院 | 2026 |
| 13978 | 41 | NULL | 本学院论文发表数 - 马克思主义学院 | 29.00 | 3 | 统计学院师生发表的学术论文数量，按期刊级别分类统计 | 2026-01-19 | 2026-02-09 | 定量 | 0 | ✗ | 571 | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | 科技处 | 马克思主义学院 | 2026 |
| 13618 | 18 | NULL | 本学院科研项目数 - 计算机学院 | 21.49 | 3 | NULL | 2026-01-19 | 2026-02-09 | 定量 | 0 | ✗ | 586 | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | 科技处 | 计算机学院 | 2026 |
| 13621 | 19 | NULL | 本学院国际交流项目 - 文理学院 | 30.02 | 3 | NULL | 2026-01-19 | 2026-02-09 | 定量 | 0 | ✗ | 587 | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | 科技处 | 文理学院 | 2026 |
| 13629 | 19 | NULL | 本学院科研项目数 - 工学院 | 29.95 | 1 | NULL | 2026-01-19 | 2026-02-09 | 定量 | 0 | ✗ | 589 | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | 科技处 | 工学院 | 2026 |
| 13991 | 41 | NULL | 本学院专利申请数 - 马克思主义学院 | 29.00 | 3 | 统计本学院师生申请的发明专利、实用新型、外观设计专利数量 | 2026-01-19 | 2026-02-09 | 定量 | 0 | ✗ | 593 | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | 科技处 | 马克思主义学院 | 2026 |
| 14004 | 42 | NULL | 本学院专利申请数 - 商学院 | 26.00 | 5 | 统计本学院师生申请的发明专利、实用新型、外观设计专利数量 | 2026-01-19 | 2026-02-09 | 定量 | 0 | ✗ | 594 | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | NULL | 科技处 | 商学院 | 2026 |

---

## 统计信息

- 总记录数: 711
- 字段数: 31 (新增: level, owner_org_id, target_org_id, status)
