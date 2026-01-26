# 差距分析报告 (Gap Analysis Report)

> 本报告对比前端 TypeScript 接口与后端 Java Entity/VO 类，识别缺失或不一致的字段。

## 1. Indicator 字段对比分析

### 1.1 字段映射状态总览

| 前端字段 | 后端 Entity | 后端 VO | 数据库列 | 状态 | 说明 |
|----------|-------------|---------|----------|------|------|
| `id` | `indicatorId` | `indicatorId` | `indicator_id` | ✅ | 类型需转换 Long→String |
| `name` | `indicatorDesc` | `indicatorDesc` | `indicator_desc` | ✅ | 字段名不同但语义一致 |
| `isQualitative` | ❌ | ❌ | ❌ | ❌ 缺失 | 需新增 |
| `type1` | ❌ | ❌ | ❌ | ❌ 缺失 | 需新增 ('定性'\|'定量') |
| `type2` | ❌ | ❌ | ❌ | ❌ 缺失 | 需新增 ('发展性'\|'基础性') |
| `progress` | ❌ | ❌ | ❌ | ❌ 缺失 | 需新增或从 ProgressReport 计算 |
| `createTime` | `createdAt` | `createdAt` | `created_at` | ✅ | 格式需转换 |
| `weight` | `weightPercent` | `weightPercent` | `weight_percent` | ✅ | 类型一致 |
| `remark` | `remark` | `remark` | `remark` | ✅ | 完全一致 |
| `canWithdraw` | ❌ | ❌ | ❌ | ❌ 缺失 | 需新增 |
| `milestones` | `milestones` | `milestones` | - | ✅ | 关联关系存在 |
| `targetValue` | ❌ | ❌ | ❌ | ❌ 缺失 | 需新增 |
| `actualValue` | ❌ | ❌ | ❌ | ❌ 缺失 | 需新增 (可选字段) |
| `unit` | ❌ | ❌ | ❌ | ❌ 缺失 | 需新增 |
| `responsibleDept` | `targetOrg.name` | `targetOrgName` | `target_org_id` | ✅ | 通过关联获取 |
| `responsiblePerson` | ❌ | ❌ | ❌ | ❌ 缺失 | 需新增 |
| `status` | `status` | `status` | `status` | ⚠️ 类型不匹配 | 枚举值需对齐 |
| `isStrategic` | `level` | `level` | `level` | ⚠️ 需转换 | STRATEGIC→true, 其他→false |
| `ownerDept` | `ownerOrg.name` | `ownerOrgName` | `owner_org_id` | ✅ | 通过关联获取 |
| `parentIndicatorId` | `parentIndicator.id` | `parentIndicatorId` | `parent_indicator_id` | ✅ | 类型需转换 |
| `year` | `year` | `year` | `year` | ✅ | 完全一致 |
| `statusAudit` | ❌ | ❌ | ❌ | ❌ 缺失 | 需新增 (JSONB) |
| `progressApprovalStatus` | ❌ | ❌ | ❌ | ❌ 缺失 | 需新增 |
| `pendingProgress` | ❌ | ❌ | ❌ | ❌ 缺失 | 需新增 |
| `pendingRemark` | ❌ | ❌ | ❌ | ❌ 缺失 | 需新增 |
| `pendingAttachments` | ❌ | ❌ | ❌ | ❌ 缺失 | 需新增 (JSONB) |
| `taskContent` | - | `taskName` | - | ✅ | 通过 task 关联获取 |
| `approvalStatus` | ❌ | ❌ | ❌ | ❌ 缺失 | 可选字段，需新增 |
| `alertLevel` | ❌ | ❌ | ❌ | ❌ 缺失 | 可选字段，需新增 |

### 1.2 状态统计

| 状态 | 数量 | 占比 |
|------|------|------|
| ✅ 已存在 | 12 | 44% |
| ⚠️ 类型不匹配 | 2 | 7% |
| ❌ 缺失 | 13 | 48% |



## 2. Milestone 字段对比分析

### 2.1 字段映射状态总览

| 前端字段 | 后端 Entity | 后端 VO | 数据库列 | 状态 | 说明 |
|----------|-------------|---------|----------|------|------|
| `id` | `milestoneId` | `milestoneId` | `milestone_id` | ✅ | 类型需转换 Long→String |
| `name` | `milestoneName` | `milestoneName` | `milestone_name` | ✅ | 完全一致 |
| `targetProgress` | ❌ | ❌ | ❌ | ❌ 缺失 | 需新增 |
| `deadline` | `dueDate` | `dueDate` | `due_date` | ✅ | 字段名不同但语义一致 |
| `status` | `status` | `status` | `status` | ⚠️ 类型不匹配 | 枚举值需对齐 |
| `isPaired` | ❌ | ❌ | ❌ | ❌ 缺失 | 需新增 |
| `weightPercent` | `weightPercent` | `weightPercent` | `weight_percent` | ✅ | 完全一致 |
| `sortOrder` | `sortOrder` | `sortOrder` | `sort_order` | ✅ | 完全一致 |
| `indicatorId` | `indicator.id` | `indicatorId` | `indicator_id` | ✅ | 通过关联获取 |

### 2.2 状态统计

| 状态 | 数量 | 占比 |
|------|------|------|
| ✅ 已存在 | 6 | 67% |
| ⚠️ 类型不匹配 | 1 | 11% |
| ❌ 缺失 | 2 | 22% |

## 3. 待补充字段列表

### 3.1 Indicator 表需新增字段

| 字段名 | 数据库列名 | 数据类型 | 默认值 | 说明 |
|--------|-----------|----------|--------|------|
| `isQualitative` | `is_qualitative` | `BOOLEAN` | `FALSE` | 是否为定性指标 |
| `type1` | `type1` | `VARCHAR(20)` | `NULL` | '定性' \| '定量' |
| `type2` | `type2` | `VARCHAR(20)` | `NULL` | '发展性' \| '基础性' |
| `progress` | `progress` | `INTEGER` | `0` | 当前进度 (0-100) |
| `canWithdraw` | `can_withdraw` | `BOOLEAN` | `FALSE` | 是否可撤回 |
| `targetValue` | `target_value` | `DECIMAL(10,2)` | `NULL` | 目标值 |
| `actualValue` | `actual_value` | `DECIMAL(10,2)` | `NULL` | 实际值 |
| `unit` | `unit` | `VARCHAR(50)` | `NULL` | 单位 |
| `responsiblePerson` | `responsible_person` | `VARCHAR(100)` | `NULL` | 责任人 |
| `statusAudit` | `status_audit` | `JSONB` | `'[]'` | 审计日志 |
| `progressApprovalStatus` | `progress_approval_status` | `VARCHAR(20)` | `'none'` | 进度审批状态 |
| `pendingProgress` | `pending_progress` | `INTEGER` | `NULL` | 待审批进度 |
| `pendingRemark` | `pending_remark` | `TEXT` | `NULL` | 待审批说明 |
| `pendingAttachments` | `pending_attachments` | `JSONB` | `'[]'` | 待审批附件 |
| `approvalStatus` | `approval_status` | `VARCHAR(20)` | `NULL` | 审批状态 |
| `alertLevel` | `alert_level` | `VARCHAR(20)` | `NULL` | 预警级别 |

### 3.2 Milestone 表需新增字段

| 字段名 | 数据库列名 | 数据类型 | 默认值 | 说明 |
|--------|-----------|----------|--------|------|
| `targetProgress` | `target_progress` | `INTEGER` | `0` | 目标进度 (0-100) |
| `isPaired` | `is_paired` | `BOOLEAN` | `FALSE` | 是否已配对 |

## 4. 类型不匹配字段处理

### 4.1 Indicator.status 枚举对齐

**前端枚举值:**
- `draft`, `active`, `archived`, `distributed`, `pending`, `approved`

**后端枚举值 (IndicatorStatus):**
需要检查并对齐

### 4.2 Milestone.status 枚举对齐

**前端枚举值:**
- `pending`, `completed`, `overdue`

**后端枚举值 (MilestoneStatus):**
需要检查并对齐

### 4.3 isStrategic ↔ level 转换逻辑

```java
// 前端 → 后端
isStrategic: true  → level: STRATEGIC
isStrategic: false → level: SECONDARY (或其他)

// 后端 → 前端
level: STRATEGIC → isStrategic: true
level: 其他      → isStrategic: false
```

## 5. 数据类型转换说明

### 5.1 ID 类型转换

| 前端类型 | 后端类型 | 转换方向 |
|----------|----------|----------|
| `string` | `Long` | 前端 → 后端: `Long.parseLong(id)` |
| `string` | `Long` | 后端 → 前端: `String.valueOf(id)` |

### 5.2 日期时间转换

| 前端格式 | 后端类型 | 说明 |
|----------|----------|------|
| `"2026年1月5日"` | `LocalDateTime` | 需格式化转换 |
| `"2026-03-31"` | `LocalDate` | ISO 格式，直接转换 |

## 6. 后端 Entity 现有字段清单

### 6.1 Indicator Entity

| Java 属性 | 数据库列 | 类型 | 说明 |
|-----------|----------|------|------|
| `indicatorId` | `indicator_id` | `Long` | 主键 |
| `task` | `task_id` | `FK → StrategicTask` | 关联战略任务 |
| `parentIndicator` | `parent_indicator_id` | `FK → Indicator` | 父指标 |
| `childIndicators` | - | `List<Indicator>` | 子指标列表 |
| `level` | `level` | `IndicatorLevel` | 指标层级 |
| `ownerOrg` | `owner_org_id` | `FK → Org` | 发布方组织 |
| `targetOrg` | `target_org_id` | `FK → Org` | 责任组织 |
| `indicatorDesc` | `indicator_desc` | `String` | 指标描述 |
| `weightPercent` | `weight_percent` | `BigDecimal` | 权重 |
| `sortOrder` | `sort_order` | `Integer` | 排序 |
| `year` | `year` | `Integer` | 年份 |
| `status` | `status` | `IndicatorStatus` | 状态 |
| `remark` | `remark` | `String` | 备注 |
| `milestones` | - | `List<Milestone>` | 里程碑列表 |
| `createdAt` | `created_at` | `LocalDateTime` | 创建时间 (继承) |
| `updatedAt` | `updated_at` | `LocalDateTime` | 更新时间 (继承) |

### 6.2 Milestone Entity

| Java 属性 | 数据库列 | 类型 | 说明 |
|-----------|----------|------|------|
| `milestoneId` | `milestone_id` | `Long` | 主键 |
| `indicator` | `indicator_id` | `FK → Indicator` | 关联指标 |
| `milestoneName` | `milestone_name` | `String` | 里程碑名称 |
| `milestoneDesc` | `milestone_desc` | `String` | 里程碑描述 |
| `dueDate` | `due_date` | `LocalDate` | 截止日期 |
| `weightPercent` | `weight_percent` | `BigDecimal` | 权重 |
| `status` | `status` | `MilestoneStatus` | 状态 |
| `sortOrder` | `sort_order` | `Integer` | 排序 |
| `inheritedFrom` | `inherited_from` | `FK → Milestone` | 继承来源 |
| `createdAt` | `created_at` | `LocalDateTime` | 创建时间 (继承) |
| `updatedAt` | `updated_at` | `LocalDateTime` | 更新时间 (继承) |

## 7. 后端 VO 现有字段清单

### 7.1 IndicatorVO

| Java 属性 | 类型 | 说明 |
|-----------|------|------|
| `indicatorId` | `Long` | 指标ID |
| `taskId` | `Long` | 任务ID |
| `taskName` | `String` | 任务名称 |
| `parentIndicatorId` | `Long` | 父指标ID |
| `parentIndicatorDesc` | `String` | 父指标描述 |
| `level` | `IndicatorLevel` | 层级 |
| `ownerOrgId` | `Long` | 发布方ID |
| `ownerOrgName` | `String` | 发布方名称 |
| `targetOrgId` | `Long` | 责任方ID |
| `targetOrgName` | `String` | 责任方名称 |
| `indicatorDesc` | `String` | 指标描述 |
| `weightPercent` | `BigDecimal` | 权重 |
| `sortOrder` | `Integer` | 排序 |
| `year` | `Integer` | 年份 |
| `status` | `IndicatorStatus` | 状态 |
| `remark` | `String` | 备注 |
| `createdAt` | `LocalDateTime` | 创建时间 |
| `updatedAt` | `LocalDateTime` | 更新时间 |
| `childIndicators` | `List<IndicatorVO>` | 子指标 |
| `milestones` | `List<MilestoneVO>` | 里程碑 |

### 7.2 MilestoneVO

| Java 属性 | 类型 | 说明 |
|-----------|------|------|
| `milestoneId` | `Long` | 里程碑ID |
| `indicatorId` | `Long` | 指标ID |
| `indicatorDesc` | `String` | 指标描述 |
| `milestoneName` | `String` | 里程碑名称 |
| `milestoneDesc` | `String` | 里程碑描述 |
| `dueDate` | `LocalDate` | 截止日期 |
| `weightPercent` | `BigDecimal` | 权重 |
| `status` | `MilestoneStatus` | 状态 |
| `sortOrder` | `Integer` | 排序 |
| `inheritedFromId` | `Long` | 继承来源ID |
| `createdAt` | `LocalDateTime` | 创建时间 |
| `updatedAt` | `LocalDateTime` | 更新时间 |

---

*报告生成时间: 2026-01-19*
*数据来源: 前端数据字典 + 后端 Entity/VO 类分析*
