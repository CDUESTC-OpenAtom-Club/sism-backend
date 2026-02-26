# audit_flow_def (audit_flow_def) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 4

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('audit_flow_ | |
| flow_code | character varying | 64 | ✓ | - | |
| flow_name | character varying | 128 | ✓ | - | |
| biz_type | character varying | 32 | ✓ | - | |
| is_enabled | boolean | - | ✓ | true | |
| remark | character varying | 512 | ✗ | - | |
| created_at | timestamp with time zone | - | ✓ | now() | |
| updated_at | timestamp with time zone | - | ✓ | now() | |

---

## 示例数据

显示前 4 条记录

| id | flow_code | flow_name | biz_type | is_enabled | remark | created_at | updated_at |
|---|---|---|---|---|---|---|---|
| 1 | PLAN_DISPATCH_STRATEGY | Plan下发审批（战略发展部） | PLAN | ✓ | 填报人 → 战略发展部负责人 → 分管校领导 | 2026-02-03 | 2026-02-03 |
| 2 | PLAN_DISPATCH_FUNCDEPT | Plan下发审批（职能部门） | PLAN | ✓ | 填报人 → 职能部门负责人 → 分管校领导 | 2026-02-03 | 2026-02-03 |
| 4 | PLAN_REPORT_COLLEGE | 二级学院月度填报审批 | PLAN_REPORT | ✓ | 填报人 → 学院院长 → 分管职能部门负责人 → 分管校领导 → 战略发展部 | 2026-02-03 | 2026-02-03 |
| 3 | PLAN_REPORT_FUNC | 职能部门月度填报审批 | PLAN_REPORT | ✓ | 填报人 → 职能部门负责人 → 分管校领导 → 战略发展部 | 2026-02-03 | 2026-02-03 |

---

## 统计信息

- 总记录数: 4
- 字段数: 8
