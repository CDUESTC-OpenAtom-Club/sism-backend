# 前端数据字典 (Frontend Data Dictionary)

> 本文档从前端 Mock 数据和 TypeScript 类型定义中提取，作为数据对齐的标准参考。

## 1. StrategicIndicator 接口 (战略指标)

| 字段名 | TypeScript 类型 | 必填 | 业务含义 | 示例值 |
|--------|----------------|------|----------|--------|
| `id` | `string` | ✅ | 指标唯一标识符 | `"2026-101"`, `"2026-101-1"` |
| `name` | `string` | ✅ | 指标名称/描述 | `"优质就业比例不低于15%"` |
| `isQualitative` | `boolean` | ✅ | 是否为定性指标 | `false` (定量), `true` (定性) |
| `type1` | `'定性' \| '定量'` | ✅ | 指标类型1 | `"定量"`, `"定性"` |
| `type2` | `'发展性' \| '基础性'` | ✅ | 指标类型2 | `"发展性"`, `"基础性"` |
| `progress` | `number` | ✅ | 当前进度百分比 (0-100) | `8`, `15`, `25` |
| `createTime` | `string` | ✅ | 创建时间（中文格式） | `"2026年1月5日"` |
| `weight` | `number` | ✅ | 权重百分比 | `20`, `25`, `50` |
| `remark` | `string` | ✅ | 备注说明 | `"力争突破"`, `"中长期发展规划内容"` |
| `canWithdraw` | `boolean` | ✅ | 是否可撤回 | `false`, `true` |
| `milestones` | `Milestone[]` | ✅ | 里程碑列表 | 见 Milestone 接口 |
| `targetValue` | `number` | ✅ | 目标值 | `15`, `95`, `87` |
| `actualValue` | `number` | ❌ | 实际值（可选） | `12`, `90` |
| `unit` | `string` | ✅ | 单位 | `"%"`, `"篇"`, `"人"`, `"家/专业"` |
| `responsibleDept` | `string` | ✅ | 责任部门 | `"就业创业指导中心"`, `"计算机学院"` |
| `responsiblePerson` | `string` | ✅ | 责任人 | `"张老师"`, `"赵院长"` |
| `status` | `string` | ✅ | 指标状态 | `"draft"`, `"active"`, `"archived"`, `"distributed"`, `"pending"`, `"approved"` |
| `isStrategic` | `boolean` | ✅ | 是否为战略级指标 | `true` (战略级), `false` (二级学院) |
| `approvalStatus` | `ApprovalStatus` | ❌ | 审批状态（可选） | `"pending"`, `"approved"`, `"rejected"` |
| `alertLevel` | `AlertLevel` | ❌ | 预警级别（可选） | `"severe"`, `"moderate"`, `"normal"` |
| `taskContent` | `string` | ❌ | 关联的战略任务内容 | `"全力促进毕业生多元化高质量就业创业"` |
| `ownerDept` | `string` | ❌ | 发布方部门 | `"战略发展部"`, `"就业创业指导中心"` |
| `parentIndicatorId` | `string` | ❌ | 父指标ID（二级指标关联） | `"2026-101"` |
| `year` | `number` | ❌ | 年份 | `2026` |
| `statusAudit` | `StatusAuditEntry[]` | ❌ | 审批/操作历史 | 见 StatusAuditEntry 接口 |
| `progressApprovalStatus` | `ProgressApprovalStatus` | ❌ | 进度审批状态 | `"none"`, `"draft"`, `"pending"`, `"approved"`, `"rejected"` |
| `pendingProgress` | `number` | ❌ | 待审批的进度值 | `20`, `28` |
| `pendingRemark` | `string` | ❌ | 待审批的说明 | `"已完成Q1就业数据统计"` |
| `pendingAttachments` | `string[]` | ❌ | 待审批的附件URL列表 | `["url1", "url2"]` |


## 2. Milestone 接口 (里程碑)

| 字段名 | TypeScript 类型 | 必填 | 业务含义 | 示例值 |
|--------|----------------|------|----------|--------|
| `id` | `string` | ✅ | 里程碑唯一标识符 | `"hist-2026-1"`, `"hist-2026-m3"` |
| `name` | `string` | ✅ | 里程碑名称 | `"Q1: 阶段目标"`, `"3月: 阶段目标"` |
| `targetProgress` | `number` | ✅ | 目标进度百分比 | `25`, `50`, `75`, `100` |
| `deadline` | `string` | ✅ | 截止日期 (YYYY-MM-DD) | `"2026-03-31"`, `"2026-06-30"` |
| `status` | `'pending' \| 'completed' \| 'overdue'` | ✅ | 状态 | `"pending"`, `"completed"`, `"overdue"` |
| `isPaired` | `boolean` | ❌ | 是否已配对（有审核通过的填报记录） | `true`, `false` |
| `weightPercent` | `number` | ❌ | 权重百分比 | `25` |
| `sortOrder` | `number` | ❌ | 排序顺序 | `1`, `2`, `3` |
| `indicatorId` | `string` | ❌ | 关联的指标ID | `"2026-101"` |

## 3. StatusAuditEntry 接口 (状态审计日志)

| 字段名 | TypeScript 类型 | 必填 | 业务含义 | 示例值 |
|--------|----------------|------|----------|--------|
| `id` | `string` | ✅ | 审计记录唯一标识符 | `"audit-101-1-1"` |
| `timestamp` | `Date` | ✅ | 操作时间 | `new Date('2026-01-12')` |
| `operator` | `string` | ✅ | 操作人用户名 | `"jyc-admin"`, `"zhao-dean"` |
| `operatorName` | `string` | ✅ | 操作人姓名 | `"就业中心管理员"`, `"赵院长"` |
| `operatorDept` | `string` | ✅ | 操作人部门 | `"就业创业指导中心"`, `"计算机学院"` |
| `action` | `string` | ✅ | 操作类型 | `"submit"`, `"approve"`, `"reject"`, `"revoke"`, `"update"`, `"distribute"`, `"withdraw"` |
| `comment` | `string` | ❌ | 操作备注 | `"下发子指标给计算机学院"` |
| `previousStatus` | `string` | ❌ | 变更前状态 | `"draft"`, `"pending"` |
| `newStatus` | `string` | ❌ | 变更后状态 | `"active"`, `"approved"` |
| `previousProgress` | `number` | ❌ | 变更前进度 | `0`, `15` |
| `newProgress` | `number` | ❌ | 变更后进度 | `15`, `20` |

## 4. 枚举类型定义

### 4.1 ProgressApprovalStatus (进度审批状态)

| 值 | 含义 |
|----|------|
| `none` | 无待审批 |
| `draft` | 草稿 |
| `pending` | 待审批 |
| `approved` | 已通过 |
| `rejected` | 已驳回 |

### 4.2 ApprovalStatus (审批状态)

| 值 | 含义 |
|----|------|
| `pending` | 待审批 |
| `approved` | 已通过 |
| `rejected` | 已驳回 |

### 4.3 AlertLevel (预警级别)

| 值 | 含义 |
|----|------|
| `severe` | 严重 |
| `moderate` | 中等 |
| `normal` | 正常 |

### 4.4 Indicator Status (指标状态)

| 值 | 含义 |
|----|------|
| `draft` | 草稿 |
| `active` | 进行中 |
| `archived` | 已归档 |
| `distributed` | 已下发 |
| `pending` | 待审批 |
| `approved` | 已通过 |

### 4.5 Milestone Status (里程碑状态)

| 值 | 含义 |
|----|------|
| `pending` | 待完成 |
| `completed` | 已完成 |
| `overdue` | 逾期未完成 |

## 5. Mock 数据统计

### 5.1 指标数量统计 (indicators2026.ts)

| 类别 | 数量 |
|------|------|
| 总指标数 | 约 45 条 |
| 战略级指标 (isStrategic: true) | 约 20 条 |
| 二级学院指标 (isStrategic: false) | 约 25 条 |
| 定量指标 (type1: '定量') | 约 38 条 |
| 定性指标 (type1: '定性') | 约 7 条 |
| 发展性指标 (type2: '发展性') | 约 25 条 |
| 基础性指标 (type2: '基础性') | 约 20 条 |

### 5.2 部门分布

| 部门类型 | 示例 |
|----------|------|
| 职能部门 | 就业创业指导中心、教务处、科技处、招生工作处、财务部、后勤资产处 |
| 二级学院 | 计算机学院、商学院、工学院、航空学院、文理学院、艺术与科技学院、国际教育学院、马克思主义学院 |
| 党群部门 | 党委办公室、党委统战部、纪委办公室、党委学工部 |

### 5.3 审计日志示例

Mock 数据中包含丰富的 `statusAudit` 审计日志，记录了指标的完整生命周期：
- 下发 (distribute)
- 提交 (submit)
- 审批通过 (approve)
- 驳回 (reject)
- 进度更新 (update)

## 6. 字段映射建议

### 6.1 前端字段 → 数据库列映射

| 前端字段 | 建议数据库列名 | 数据类型 |
|----------|---------------|----------|
| `id` | `indicator_id` | VARCHAR(50) |
| `name` | `indicator_desc` | VARCHAR(500) |
| `isQualitative` | `is_qualitative` | BOOLEAN |
| `type1` | `type1` | VARCHAR(20) |
| `type2` | `type2` | VARCHAR(20) |
| `progress` | `progress` | INTEGER |
| `createTime` | `created_at` | TIMESTAMP |
| `weight` | `weight_percent` | DECIMAL(5,2) |
| `remark` | `remark` | TEXT |
| `canWithdraw` | `can_withdraw` | BOOLEAN |
| `targetValue` | `target_value` | DECIMAL(10,2) |
| `actualValue` | `actual_value` | DECIMAL(10,2) |
| `unit` | `unit` | VARCHAR(50) |
| `responsibleDept` | `target_org_id` (FK) | BIGINT |
| `responsiblePerson` | `responsible_person` | VARCHAR(100) |
| `status` | `status` | VARCHAR(20) |
| `isStrategic` | `level` | VARCHAR(20) |
| `ownerDept` | `owner_org_id` (FK) | BIGINT |
| `parentIndicatorId` | `parent_indicator_id` (FK) | BIGINT |
| `year` | `year` | INTEGER |
| `statusAudit` | `status_audit` | JSONB |
| `progressApprovalStatus` | `progress_approval_status` | VARCHAR(20) |
| `pendingProgress` | `pending_progress` | INTEGER |
| `pendingRemark` | `pending_remark` | TEXT |
| `pendingAttachments` | `pending_attachments` | JSONB |

---

*文档生成时间: 2026-01-19*
*数据来源: strategic-task-management/src/data/indicators/indicators2026.ts, strategic-task-management/src/types/index.ts*
