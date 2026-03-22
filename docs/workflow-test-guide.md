# SISM 审批流程测试文档

## 概述

本文档提供 SISM 系统 4 个审批流程的完整测试指南，包括用户账号映射、测试场景和预期结果。

**测试环境**: http://localhost:8080
**默认密码**: 所有测试账号密码均为 `admin123`

---

## 流程 1: PLAN_DISPATCH_STRATEGY - Plan下发审批（战略发展部）

### 流程说明
战略发展部发起的 Plan 下发审批流程，需要经过战略发展部终审。

### 审批步骤
1. **提交步骤** (SUBMIT) - 提交人自动完成
2. **战略发展部终审** (APPROVAL) - 战略发展部终审角色审批

### 测试账号

| 角色 | 用户名 | 姓名 | 组织 | 说明 |
|------|--------|------|------|------|
| 发起人 | `zlb_admin` | 战略管理员 | 战略发展部 | 提交 Plan 下发申请 |
| 终审人 | `zlb_final1` | 战略终审1 | 战略发展部 | 战略发展部终审角色 |
| 终审人 | `zlb_final2` | 战略终审2 | 战略发展部 | 战略发展部终审角色（备用） |

### 测试场景

#### 场景 1.1: 正常审批通过
1. 使用 `zlb_admin` 登录
2. 创建 Plan 并提交下发申请
3. 系统自动完成提交步骤，创建终审步骤
4. 使用 `zlb_final1` 登录
5. 在"我的待办"中找到该审批任务
6. 点击"通过"，填写审批意见
7. 验证 Plan 状态变为"已下发"

**预期结果**:
- 提交后自动创建 2 个步骤实例（提交步骤已完成 + 终审步骤待审批）
- 终审通过后，workflow 状态变为 `APPROVED`
- Plan 状态同步更新

#### 场景 1.2: 终审驳回
1. 使用 `zlb_admin` 登录提交 Plan 下发
2. 使用 `zlb_final2` 登录
3. 在"我的待办"中找到该审批任务
4. 点击"驳回"，填写驳回原因
5. 验证 workflow 状态变为 `REJECTED`

**预期结果**:
- 终审驳回后，workflow 状态变为 `REJECTED`
- 提交步骤恢复为 `PENDING` 状态
- Plan 状态同步更新为驳回状态

---

## 流程 2: PLAN_DISPATCH_FUNCDEPT - Plan下发审批（职能部门）

### 流程说明
职能部门发起的 Plan 下发审批流程，需要经过职能部门负责人审批和战略发展部终审。

### 审批步骤
1. **提交步骤** (SUBMIT) - 提交人自动完成
2. **职能部门负责人审批** (APPROVAL) - 职能部门负责人角色审批
3. **战略发展部终审** (APPROVAL) - 战略发展部终审角色审批

### 测试账号（以教务处为例）

| 角色 | 用户名 | 姓名 | 组织 | 说明 |
|------|--------|------|------|------|
| 发起人 | `jwc_reporter` | 教务处填报员 | 教务处 | 提交 Plan 下发申请 |
| 部门审批 | `jwc_leader` | 教务处负责人 | 教务处 | 职能部门负责人角色 |
| 终审人 | `zlb_final1` | 战略终审1 | 战略发展部 | 战略发展部终审角色 |

### 其他职能部门账号

**人事处**: `rsc_reporter`, `rsc_audit1`, `rsc_leader`
**财务处**: `cwc_reporter`, `cwc_audit1`, `cwc_leader`
**科研处**: `kyc_reporter`, `kyc_audit1`, `kyc_leader`
**学工处**: `xgc_reporter`, `xgc_audit1`, `xgc_leader`
**后勤处**: `hqc_reporter`, `hqc_audit1`, `hqc_leader`

### 测试场景

#### 场景 2.1: 正常审批通过（两级审批）
1. 使用 `jwc_reporter` 登录
2. 创建 Plan 并提交下发申请
3. 系统自动完成提交步骤，创建部门审批步骤
4. 使用 `jwc_leader` 登录
5. 在"我的待办"中找到该审批任务，点击"通过"
6. 系统动态创建战略终审步骤
7. 使用 `zlb_final1` 登录
8. 在"我的待办"中找到该审批任务，点击"通过"
9. 验证 Plan 状态变为"已下发"

**预期结果**:
- 提交后创建 2 个步骤实例（提交已完成 + 部门审批待审批）
- 部门审批通过后，动态创建第 3 个步骤实例（战略终审待审批）
- 战略终审通过后，workflow 状态变为 `APPROVED`

#### 场景 2.2: 部门负责人驳回
1. 使用 `rsc_reporter` 登录提交 Plan 下发
2. 使用 `rsc_leader` 登录
3. 在"我的待办"中点击"驳回"
4. 验证 workflow 状态变为 `REJECTED`

**预期结果**:
- 部门审批驳回后，workflow 状态变为 `REJECTED`
- 提交步骤恢复为 `PENDING` 状态

#### 场景 2.3: 战略终审驳回
1. 使用 `cwc_reporter` 登录提交 Plan 下发
2. 使用 `cwc_leader` 登录通过部门审批
3. 使用 `zlb_final2` 登录
4. 在"我的待办"中点击"驳回"
5. 验证部门审批步骤恢复为 `PENDING`

**预期结果**:
- 战略终审驳回后，workflow 状态仍为 `IN_REVIEW`
- 部门审批步骤恢复为 `PENDING` 状态
- `cwc_leader` 可以重新审批

---

## 流程 3: PLAN_APPROVAL_FUNCDEPT - Plan审批流程（职能部门）

### 流程说明
职能部门的 Plan 审批流程，需要经过部门初审、部门负责人审批和战略发展部终审。

### 审批步骤
1. **提交步骤** (SUBMIT) - 提交人自动完成
2. **职能部门初审** (APPROVAL) - 职能部门初审角色审批
3. **职能部门负责人审批** (APPROVAL) - 职能部门负责人角色审批
4. **战略发展部终审** (APPROVAL) - 战略发展部终审角色审批

### 测试账号（以教务处为例）

| 角色 | 用户名 | 姓名 | 组织 | 说明 |
|------|--------|------|------|------|
| 发起人 | `jwc_reporter` | 教务处填报员 | 教务处 | 提交 Plan 审批申请 |
| 初审人 | `jwc_audit1` | 教务处初审员 | 教务处 | 职能部门初审角色 |
| 部门审批 | `jwc_leader` | 教务处负责人 | 教务处 | 职能部门负责人角色 |
| 终审人 | `zlb_final1` | 战略终审1 | 战略发展部 | 战略发展部终审角色 |

### 测试场景

#### 场景 3.1: 正常审批通过（三级审批）
1. 使用 `kyc_reporter` 登录创建并提交 Plan
2. 系统自动完成提交步骤，创建初审步骤
3. 使用 `kyc_audit1` 登录，在"我的待办"中通过初审
4. 系统动态创建部门审批步骤
5. 使用 `kyc_leader` 登录，在"我的待办"中通过部门审批
6. 系统动态创建战略终审步骤
7. 使用 `zlb_final1` 登录，在"我的待办"中通过终审
8. 验证 Plan 状态变为"已审批"

**预期结果**:
- 提交后创建 2 个步骤实例（提交已完成 + 初审待审批）
- 初审通过后，动态创建第 3 个步骤实例（部门审批待审批）
- 部门审批通过后，动态创建第 4 个步骤实例（战略终审待审批）
- 战略终审通过后，workflow 状态变为 `APPROVED`

#### 场景 3.2: 初审驳回
1. 使用 `xgc_reporter` 登录提交 Plan
2. 使用 `xgc_audit1` 登录驳回初审
3. 验证 workflow 状态变为 `REJECTED`

**预期结果**:
- 初审驳回后，workflow 状态变为 `REJECTED`
- 提交步骤恢复为 `PENDING` 状态

#### 场景 3.3: 部门负责人驳回
1. 使用 `hqc_reporter` 登录提交 Plan
2. 使用 `hqc_audit1` 登录通过初审
3. 使用 `hqc_leader` 登录驳回部门审批
4. 验证初审步骤恢复为 `PENDING`

**预期结果**:
- 部门审批驳回后，workflow 状态仍为 `IN_REVIEW`
- 初审步骤恢复为 `PENDING` 状态
- `hqc_audit1` 可以重新审批

#### 场景 3.4: 战略终审驳回
1. 使用 `jwc_reporter` 登录提交 Plan
2. 使用 `jwc_audit1` 登录通过初审
3. 使用 `jwc_leader` 登录通过部门审批
4. 使用 `zlb_final2` 登录驳回战略终审
5. 验证部门审批步骤恢复为 `PENDING`

**预期结果**:
- 战略终审驳回后，workflow 状态仍为 `IN_REVIEW`
- 部门审批步骤恢复为 `PENDING` 状态
- `jwc_leader` 可以重新审批

---

## 流程 4: PLAN_APPROVAL_COLLEGE - Plan审批流程（二级学院）

### 流程说明
二级学院的 Plan 审批流程，需要经过学院初审、学院负责人审批和战略发展部终审。

### 审批步骤
1. **提交步骤** (SUBMIT) - 提交人自动完成
2. **二级学院初审** (APPROVAL) - 二级学院初审角色审批
3. **二级学院负责人审批** (APPROVAL) - 二级学院负责人角色审批
4. **战略发展部终审** (APPROVAL) - 战略发展部终审角色审批

### 测试账号（以计算机学院为例）

| 角色 | 用户名 | 姓名 | 组织 | 说明 |
|------|--------|------|------|------|
| 发起人 | `jsjxy_reporter` | 计算机学院填报员 | 计算机学院 | 提交 Plan 审批申请 |
| 初审人 | `jsjxy_audit1` | 计算机学院初审员 | 计算机学院 | 二级学院初审角色 |
| 学院审批 | `jsjxy_leader` | 计算机学院负责人 | 计算机学院 | 二级学院负责人角色 |
| 终审人 | `zlb_final1` | 战略终审1 | 战略发展部 | 战略发展部终审角色 |

### 其他学院账号

**经管学院**: `jgxy_reporter`, `jgxy_audit1`, `jgxy_leader`
**外语学院**: `wyxy_reporter`, `wyxy_audit1`, `wyxy_leader`
**艺术学院**: `ysxy_reporter`, `ysxy_audit1`, `ysxy_leader`
**体育学院**: `tyxy_reporter`, `tyxy_audit1`, `tyxy_leader`
**医学院**: `yxy_reporter`, `yxy_audit1`, `yxy_leader`

### 测试场景

#### 场景 4.1: 正常审批通过（三级审批）
1. 使用 `jsjxy_reporter` 登录创建并提交 Plan
2. 系统自动完成提交步骤，创建初审步骤
3. 使用 `jsjxy_audit1` 登录，在"我的待办"中通过初审
4. 系统动态创建学院审批步骤
5. 使用 `jsjxy_leader` 登录，在"我的待办"中通过学院审批
6. 系统动态创建战略终审步骤
7. 使用 `zlb_final1` 登录，在"我的待办"中通过终审
8. 验证 Plan 状态变为"已审批"

**预期结果**:
- 提交后创建 2 个步骤实例（提交已完成 + 初审待审批）
- 初审通过后，动态创建第 3 个步骤实例（学院审批待审批）
- 学院审批通过后，动态创建第 4 个步骤实例（战略终审待审批）
- 战略终审通过后，workflow 状态变为 `APPROVED`

#### 场景 4.2: 初审驳回
1. 使用 `jgxy_reporter` 登录提交 Plan
2. 使用 `jgxy_audit1` 登录驳回初审
3. 验证 workflow 状态变为 `REJECTED`

**预期结果**:
- 初审驳回后，workflow 状态变为 `REJECTED`
- 提交步骤恢复为 `PENDING` 状态

#### 场景 4.3: 学院负责人驳回
1. 使用 `wyxy_reporter` 登录提交 Plan
2. 使用 `wyxy_audit1` 登录通过初审
3. 使用 `wyxy_leader` 登录驳回学院审批
4. 验证初审步骤恢复为 `PENDING`

**预期结果**:
- 学院审批驳回后，workflow 状态仍为 `IN_REVIEW`
- 初审步骤恢复为 `PENDING` 状态
- `wyxy_audit1` 可以重新审批

#### 场景 4.4: 战略终审驳回
1. 使用 `ysxy_reporter` 登录提交 Plan
2. 使用 `ysxy_audit1` 登录通过初审
3. 使用 `ysxy_leader` 登录通过学院审批
4. 使用 `zlb_final2` 登录驳回战略终审
5. 验证学院审批步骤恢复为 `PENDING`

**预期结果**:
- 战略终审驳回后，workflow 状态仍为 `IN_REVIEW`
- 学院审批步骤恢复为 `PENDING` 状态
- `ysxy_leader` 可以重新审批

---

## 通用测试要点

### 1. 动态步骤生成验证
- 提交后只创建第一个审批步骤（提交步骤自动完成）
- 每次审批通过后，动态创建下一个步骤
- 数据库 `audit_step_instance` 表中步骤数量逐步增加

### 2. 驳回回退验证
- 驳回后，上一个已完成步骤恢复为 `PENDING` 状态
- 驳回后，当前步骤状态变为 `REJECTED`
- 如果驳回第一个审批步骤，workflow 状态变为 `REJECTED`

### 3. 字段完整性验证
检查 `audit_step_instance` 表中每个步骤实例的字段：
- `step_no`: 步骤序号（1, 2, 3...）
- `approver_id`: 审批人 ID（不为空）
- `approver_org_id`: 审批人组织 ID（不为空）
- `step_def_id`: 步骤定义 ID（不为空）
- `status`: 步骤状态（PENDING/APPROVED/REJECTED）

### 4. API 接口测试

#### 获取我的待办任务
```bash
curl -X GET "http://localhost:8080/api/v1/workflows/my-tasks" \
  -H "Authorization: Bearer <token>"
```

#### 审批通过
```bash
curl -X POST "http://localhost:8080/api/v1/workflows/{instanceId}/approve" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"comment": "同意"}'
```

#### 审批驳回
```bash
curl -X POST "http://localhost:8080/api/v1/workflows/{instanceId}/reject" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"comment": "不同意，需要修改"}'
```

### 5. 日志检查
查看后端日志确认步骤创建：
```bash
tail -f /tmp/sism-backend.log | grep "StepInstanceFactory\|ApproveWorkflowUseCase"
```

---

## 测试数据准备

### 登录获取 Token
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "zlb_admin",
    "password": "admin123"
  }'
```

### 创建测试 Plan
```bash
curl -X POST "http://localhost:8080/api/v1/plans" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "planName": "测试Plan-战略发展部",
    "cycleId": 1,
    "description": "测试审批流程"
  }'
```

---

## 常见问题

### Q1: 提交后没有创建审批步骤？
**检查**:
- 确认 `audit_flow_def` 表中流程定义存在
- 确认 `audit_step_def` 表中步骤定义存在
- 查看后端日志是否有异常

### Q2: 审批通过后没有创建下一个步骤？
**检查**:
- 确认当前步骤状态已变为 `APPROVED`
- 确认流程定义中还有下一个步骤
- 查看 `ApproveWorkflowUseCase.createNextStep()` 日志

### Q3: approver_org_id 为空？
**检查**:
- 确认 `ApproverResolver.resolveApproverOrgId()` 方法正常工作
- 确认审批人账号的 `org_id` 字段不为空

### Q4: 驳回后上一步骤没有恢复？
**检查**:
- 确认 `AuditInstance.reject()` 方法正确执行
- 确认上一个步骤状态从 `APPROVED` 变为 `PENDING`
- 查看数据库 `audit_step_instance` 表中步骤状态

---

## 测试报告模板

### 测试执行记录

| 流程 | 场景 | 测试人 | 测试时间 | 结果 | 备注 |
|------|------|--------|----------|------|------|
| PLAN_DISPATCH_STRATEGY | 场景 1.1 | | | ✓/✗ | |
| PLAN_DISPATCH_FUNCDEPT | 场景 2.1 | | | ✓/✗ | |
| PLAN_APPROVAL_FUNCDEPT | 场景 3.1 | | | ✓/✗ | |
| PLAN_APPROVAL_COLLEGE | 场景 4.1 | | | ✓/✗ | |

### 缺陷记录

| 缺陷ID | 流程 | 场景 | 描述 | 严重程度 | 状态 |
|--------|------|------|------|----------|------|
| | | | | | |

---

**文档版本**: 1.0
**最后更新**: 2026-03-22
**维护人**: SISM 开发团队
