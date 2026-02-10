# sys_role (sys_role) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 6

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('sys_role_id | |
| role_code | character varying | 64 | ✓ | - | |
| role_name | character varying | 128 | ✓ | - | |
| data_access_mode | character varying | 16 | ✓ | 'OWN_ORG'::character | |
| is_enabled | boolean | - | ✓ | true | |
| remark | text | - | ✗ | - | |
| created_at | timestamp with time zone | - | ✓ | CURRENT_TIMESTAMP | |
| updated_at | timestamp with time zone | - | ✓ | CURRENT_TIMESTAMP | |

---

## 示例数据

显示前 6 条记录

| id | role_code | role_name | data_access_mode | is_enabled | remark | created_at | updated_at |
|---|---|---|---|---|---|---|---|
| 5 | ROLE_REPORTER | 填报人 | OWN_ORG | ✓ | 负责计划、指标、月度进度的具体填报 | 2026-02-03 | 2026-02-03 |
| 6 | ROLE_FUNC_DEPT_HEAD | 职能部门负责人 | OWN_ORG | ✓ | 职能部门主要负责人，负责本部门计划/指标审批 | 2026-02-03 | 2026-02-03 |
| 7 | ROLE_COLLEGE_DEAN | 二级学院院长 | OWN_ORG | ✓ | 二级学院负责人，负责学院相关指标与月报审批 | 2026-02-03 | 2026-02-03 |
| 9 | ROLE_VICE_PRESIDENT | 分管校领导 | ALL | ✓ | 校级分管领导，参与职能部门与学院流程的统一审批节点 | 2026-02-03 | 2026-02-03 |
| 10 | ROLE_STRATEGY_OFFICE | 战略发展部 | ALL | ✓ | 战略发展部审批与最终确认角色 | 2026-02-03 | 2026-02-03 |
| 8 | STRATEGY_DEPT_HEAD | 战略发展部负责人 | ALL | ✓ | 分管若干职能部门的校领导，参与学院流程中的审批 | 2026-02-03 | 2026-02-03 |

---

## 统计信息

- 总记录数: 6
- 字段数: 8
