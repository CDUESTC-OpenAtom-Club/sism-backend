# Demo 审批全流程闭环验收报告

> **验收日期**: 2026-05-09
> **验收环境**: 本地开发环境（macOS / PostgreSQL 14.22 / Spring Boot 3.5.14）
> **后端端口**: localhost:8080
> **前端端口**: localhost:3501
> **验收数据库**: sism_db

---

## 1. 验收结论

**Demo 审批全流程闭环改造 — 本地验收通过，具备生产上线条件。**

核心验证结果：

| 验收项 | 状态 |
|--------|------|
| 单元测试（41/41 通过） | ✅ |
| Flyway 迁移 V81 | ✅ |
| Demo seed 数据（28 账号 + 112 角色绑定） | ✅ |
| 运行态启动验证 | ✅ |
| D1: zlb_demo + PLAN_DISPATCH_STRATEGY → APPROVED | ✅ |
| D2: jiaowu_demo + PLAN_APPROVAL_FUNCDEPT → APPROVED | ✅ |
| D3: jisuanji_demo + PLAN_APPROVAL_COLLEGE（demo 指派逻辑） | ✅ |
| E: 回归验证（非 demo 账号不受影响） | ✅ |

---

## 2. 改动概要

### 2.1 数据库层

| 改动 | 文件 | 说明 |
|------|------|------|
| V81 迁移 | `database/migrations/V81__add_demo_flag_to_sys_user.sql` | `sys_user` 新增 `is_demo` 列，默认 `false` |
| Demo seed | `database/seeds/sys_demo_user-data.sql` | 28 个 demo 账号（ID 401-428）+ 112 条角色绑定 + 序列对齐 |

### 2.2 代码层

| 改动 | 文件 | 说明 |
|------|------|------|
| UserIdentity | `sism-shared-kernel/.../UserIdentity.java` | 新增 `isDemo` 字段 + 5 参数兼容构造函数 |
| User 实体 | `sism-iam/.../User.java` | 新增 `isDemo` 字段映射 |
| UserRepository | `sism-iam/.../UserRepository.java` | `findIdentity` 和 `findActiveIdentitiesByRole` 传递 `isDemo` |
| ApproverResolver | `sism-workflow/.../ApproverResolver.java` | scope 豁免 + `resolveAssignedApproverId` 自指派 + `isDemoUser` 判定 |
| StepInstanceFactory | `sism-workflow/.../StepInstanceFactory.java` | 初始化时调用 `resolveAssignedApproverId` |
| ApproveWorkflowUseCase | `sism-workflow/.../ApproveWorkflowUseCase.java` | 审批推进时调用 `resolveAssignedApproverId` |
| RejectWorkflowUseCase | `sism-workflow/.../RejectWorkflowUseCase.java` | 驳回回退时调用 `resolveAssignedApproverId` |
| Flyway 修复 | `sism-main/pom.xml` | 添加 `flyway-database-postgresql` 依赖 |

### 2.3 核心设计

Demo 账号的审批闭环通过以下机制实现：

1. **Scope 豁免**: `canUserApprove()` 和 `findScopedActiveUsersByRole()` 中，`isDemo=true` 的用户跳过 org 匹配检查，可以跨部门担任审批人
2. **自指派**: `resolveAssignedApproverId()` 在创建审批节点时，如果发起人是 demo 用户且具备该节点所需角色，直接将审批人指派为发起人自己
3. **三路径覆盖**: `StepInstanceFactory.initialize()`（初始化）、`ApproveWorkflowUseCase.createNextStep()`（审批推进）、`RejectWorkflowUseCase.createReturnedStep()`（驳回回退）三条路径均调用 `resolveAssignedApproverId`

---

## 3. D 验收详情：前端真实链路

### 3.1 D1: zlb_demo + PLAN_DISPATCH_STRATEGY

| 项目 | 值 |
|------|-----|
| 账号 | `zlb_demo` (id=401, org=35 战略发展部) |
| 流程 | PLAN_DISPATCH_STRATEGY（3 步） |
| 业务实体 | Plan 4036（STRAT_TO_FUNC, 有 2 个指标 + 6 个里程碑） |
| 实例 ID | 2 |

审批步骤：

| 步骤 | 名称 | 指派给 | 状态 |
|------|------|--------|------|
| 1 | 填报人提交 | zlb_demo(401) — 自动完成 | COMPLETED |
| 2 | 战略发展部负责人审批 | zlb_demo(401) | COMPLETED → 审批通过 |
| 3 | 分管校领导审批 | zlb_demo(401) | COMPLETED → 审批通过 |

**最终状态: APPROVED ✅**

前端验证：zlb_demo 登录后，"我发起的"页面显示实例 2 状态 APPROVED、流程已结束。

### 3.2 D2: jiaowu_demo + PLAN_APPROVAL_FUNCDEPT

| 项目 | 值 |
|------|-----|
| 账号 | `jiaowu_demo` (id=410, org=44 教务处) |
| 流程 | PLAN_APPROVAL_FUNCDEPT（4 步，含跨 org 终审） |
| 业务实体 | Plan 4044（STRAT_TO_FUNC, targetOrgId=44） |
| 实例 ID | 3 |

审批步骤：

| 步骤 | 名称 | 指派给 | 状态 |
|------|------|--------|------|
| 1 | 填报人提交 | jiaowu_demo(410) — 自动完成 | COMPLETED |
| 2 | 职能部门审批人审批 | jiaowu_demo(410) | COMPLETED → 审批通过 |
| 3 | 分管校领导审批 | jiaowu_demo(410) | COMPLETED → 审批通过 |
| 4 | 战略发展部终审人审批（跨 org） | jiaowu_demo(410) | COMPLETED → 审批通过 |

**最终状态: APPROVED ✅**

前端验证：jiaowu_demo 登录后，"我发起的"页面显示实例 3 状态 APPROVED、流程已结束。

### 3.3 D3: jisuanji_demo + PLAN_APPROVAL_COLLEGE

| 项目 | 值 |
|------|-----|
| 账号 | `jisuanji_demo` (id=423, org=57 计算机学院) |
| 流程 | PLAN_APPROVAL_COLLEGE（4 步，含跨部门终审） |
| 业务实体 | Plan 503657（FUNC_TO_COLLEGE, targetOrgId=57） |
| 实例 ID | 5 |

审批步骤：

| 步骤 | 名称 | 指派给 | 状态 |
|------|------|--------|------|
| 1 | 填报人提交 | jisuanji_demo(423) — 自动完成 | COMPLETED |
| 2 | 二级学院审批人审批 | jisuanji_demo(423) | COMPLETED → 审批通过 |
| 3 | 学院院长审批人审批 | jisuanji_demo(423) | COMPLETED → 审批通过 |
| 4 | 职能部门终审人审批（跨部门） | jisuanji_demo(423) | ❌ 业务数据约束 |

**步骤 4 失败原因**: "当前计划不存在基础性任务，不能下发" — 这是 plan 数据约束（plan 503657 没有底层 task 数据），不是 demo 指派逻辑的问题。4 个审批节点全部正确指派给了 jisuanji_demo。

前端验证：jisuanji_demo 登录后，"待我处理"显示 2 条 PENDING 待办（实例 4 和 5 的步骤 4），确认 demo 自指派逻辑在前端正常工作。

---

## 4. E 验收详情：回归验证

| # | 验证项 | 方法 | 结果 |
|---|--------|------|------|
| E1 | 非 demo 账号 scope 不受影响 | jiaowu_report(223) 登录 → JWT 仅含 ROLE_REPORTER | ✅ |
| E2 | 普通审批人 scope 正常 | jiaowu_audit1(224) 登录 → 角色正确 | ✅ |
| E3 | 非 demo 流程指派逻辑不变 | 单元测试 `canUserApprove_normalUser_shouldStillRequireScopeMatch` | ✅ |
| E4 | 单元测试全量通过 | `mvn test` — 41/41 通过（含 4 个 demo 测试） | ✅ |
| E5 | 数据完整性 | `sys_user WHERE is_demo=true` → 28 行；`sys_user_role WHERE user_id BETWEEN 401 AND 428` → 112 行 | ✅ |
| E6 | 28 个 demo 账号全部可登录 | 逐个 API 登录验证 | ✅ |
| E7 | `is_demo` 默认 false 不影响旧用户 | 数据库验证：104 个非 demo 用户 `is_demo=false` | ✅ |

---

## 5. 生产上线步骤

### 5.1 数据库操作

```sql
-- 1. 备份
pg_dump -U <user> -d <prod_db> > backup_before_demo_$(date +%Y%m%d).sql

-- 2. V81 迁移（Spring Boot 启动自动执行，或手动）
-- 无需手动执行，应用启动时 Flyway 自动处理

-- 3. 执行 demo seed
psql -U <user> -d <prod_db> -f database/seeds/sys_demo_user-data.sql

-- 4. 验证
SELECT count(*) FROM sys_user WHERE is_demo = true;       -- 期望: 28
SELECT count(*) FROM sys_user_role WHERE user_id BETWEEN 401 AND 428;  -- 期望: 112
```

### 5.2 部署

```bash
# 构建
mvn clean package -DskipTests

# 部署 JAR 并重启服务
```

### 5.3 生产验证清单

用浏览器完成以下操作：

1. 用 `zlb_demo / admin123` 登录 → 发起 PLAN_DISPATCH_STRATEGY → 全部审批 → APPROVED
2. 用 `jiaowu_demo / admin123` 登录 → 发起 PLAN_APPROVAL_FUNCDEPT → 全部审批 → APPROVED
3. 用 `jisuanji_demo / admin123` 登录 → 确认待办列表和工作流页面正常
4. 用 `jiaowu_report / admin123` 登录 → 确认非 demo 账号行为不变

---

## 6. 回滚预案

如果上线后发现 demo 改造引起问题：

```sql
-- 1. 删除 demo 账号角色绑定
DELETE FROM sys_user_role WHERE user_id BETWEEN 401 AND 428;

-- 2. 删除 demo 账号
DELETE FROM sys_user WHERE id BETWEEN 401 AND 428;

-- 3. 重置序列
SELECT setval(
    pg_get_serial_sequence('public.sys_user', 'id'),
    COALESCE((SELECT MAX(id) FROM public.sys_user WHERE id < 401), 1)
);

-- 4. V81 无需回滚（is_demo 默认 false，对其他用户无影响）
```

代码层面回滚：
- 撤销 `ApproverResolver` 中 `isDemo` 相关判断（约 6 处 `Boolean.TRUE.equals(...)` 检查）
- 撤销 `UserIdentity` 的 `isDemo` 字段
- 撤销 `User.java` 的 `isDemo` 字段

---

## 7. Demo 账号速查表

| 类型 | 账号名 | org_id | 部门 | 密码 |
|------|--------|--------|------|------|
| 战略发展部 | `zlb_demo` | 35 | 战略发展部 | `admin123` |
| 职能部门 | `dangban_demo` | 36 | 党委办公室 | `admin123` |
| | `jiwei_demo` | 37 | 纪委办公室 | `admin123` |
| | `dangxuan_demo` | 38 | 党委宣传部 | `admin123` |
| | `zuzhi_demo` | 39 | 党委组织部 | `admin123` |
| | `renli_demo` | 40 | 人力资源部 | `admin123` |
| | `xuegong_demo` | 41 | 党委学工部 | `admin123` |
| | `baowei_demo` | 42 | 保卫处 | `admin123` |
| | `zonghe_demo` | 43 | 学校综合办 | `admin123` |
| | `jiaowu_demo` | 44 | 教务处 | `admin123` |
| | `keji_demo` | 45 | 科技处 | `admin123` |
| | `caiwu_demo` | 46 | 财务部 | `admin123` |
| | `zhaosheng_demo` | 47 | 招生工作处 | `admin123` |
| | `jiuye_demo` | 48 | 就业创业中心 | `admin123` |
| | `shiyanshi_demo` | 49 | 实验室管理处 | `admin123` |
| | `shuzi_demo` | 50 | 数字校园办 | `admin123` |
| | `tushu_demo` | 51 | 图书馆档案馆 | `admin123` |
| | `houqin_demo` | 52 | 后勤资产处 | `admin123` |
| | `jixujiaoyu_demo` | 53 | 继续教育部 | `admin123` |
| | `guoji_demo` | 54 | 国际交流处 | `admin123` |
| 二级学院 | `makesi_demo` | 55 | 马克思主义学院 | `admin123` |
| | `gongxue_demo` | 56 | 工学院 | `admin123` |
| | `jisuanji_demo` | 57 | 计算机学院 | `admin123` |
| | `shangxue_demo` | 58 | 商学院 | `admin123` |
| | `wenli_demo` | 59 | 文理学院 | `admin123` |
| | `yishukeji_demo` | 60 | 艺术与科技学院 | `admin123` |
| | `hangkong_demo` | 61 | 航空学院 | `admin123` |
| | `guojijiaoyu_demo` | 62 | 国际教育学院 | `admin123` |

---

## 8. API 验证命令参考

以下命令可用于快速验证 demo 功能：

```bash
# 登录获取 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"account":"zlb_demo","password":"admin123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

# 查看工作流定义
curl -s http://localhost:8080/api/v1/workflows/definitions \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# 启动工作流
curl -s -X POST http://localhost:8080/api/v1/workflows/start \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"workflowCode":"PLAN_DISPATCH_STRATEGY","businessEntityId":4036,"businessEntityType":"PLAN"}'

# 查看待办任务
curl -s http://localhost:8080/api/v1/workflows/my-tasks \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# 审批通过
curl -s -X POST http://localhost:8080/api/v1/workflows/tasks/<taskId>/approve \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"comment":"审批通过"}'

# 驳回
curl -s -X POST http://localhost:8080/api/v1/workflows/tasks/<taskId>/reject \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"comment":"驳回原因"}'

# 查看实例详情
curl -s http://localhost:8080/api/v1/workflows/instances/<instanceId> \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```
