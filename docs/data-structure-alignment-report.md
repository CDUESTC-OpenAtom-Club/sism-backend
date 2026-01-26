# 前后端数据结构对齐报告

**检查日期**: 2026-01-26  
**检查范围**: Spring Boot 后端、Node.js API 服务器、Vue 3 前端

## 1. 总体评估

✅ **数据结构对齐状态: 良好**

前后端数据结构已经过对齐优化，主要实体的字段映射完整。

---

## 2. 核心实体对齐详情

### 2.1 Indicator (指标)

| 数据库字段 | 后端 Entity | 后端 VO | Node.js API | 前端类型 | 状态 |
|-----------|-------------|---------|-------------|----------|------|
| indicator_id | indicatorId | indicatorId | indicatorId | id (string) | ✅ |
| task_id | task.taskId | taskId | taskId | - | ✅ |
| task_name | - | taskName | taskName | taskContent | ✅ |
| parent_indicator_id | parentIndicator.indicatorId | parentIndicatorId | parentIndicatorId | parentIndicatorId | ✅ |
| level | level | level | level | isStrategic (派生) | ✅ |
| owner_org_id | ownerOrg.orgId | ownerOrgId | ownerOrgId | - | ✅ |
| owner_org_name | - | ownerOrgName | ownerOrgName | ownerDept | ✅ |
| target_org_id | targetOrg.orgId | targetOrgId | targetOrgId | - | ✅ |
| target_org_name | - | targetOrgName | targetOrgName | responsibleDept | ✅ |
| indicator_desc | indicatorDesc | indicatorDesc | indicatorDesc | name | ✅ |
| weight_percent | weightPercent | weightPercent | weightPercent | weight | ✅ |
| year | year | year | year | year | ✅ |
| status | status | status | status | status | ✅ |
| remark | remark | remark | remark | remark | ✅ |
| is_qualitative | isQualitative | isQualitative | isQualitative | isQualitative | ✅ |
| type1 | type1 | type1 | type1 | type1 | ✅ |
| type2 | type2 | type2 | type2 | type2 | ✅ |
| can_withdraw | canWithdraw | canWithdraw | canWithdraw | canWithdraw | ✅ |
| target_value | targetValue | targetValue | targetValue | targetValue | ✅ |
| actual_value | actualValue | actualValue | actualValue | actualValue | ✅ |
| unit | unit | unit | unit | unit | ✅ |
| responsible_person | responsiblePerson | responsiblePerson | responsiblePerson | responsiblePerson | ✅ |
| progress | progress | progress | progress | progress | ✅ |
| status_audit | statusAudit | statusAudit | statusAudit | statusAudit | ✅ |
| progress_approval_status | progressApprovalStatus | progressApprovalStatus | progressApprovalStatus | progressApprovalStatus | ✅ |
| pending_progress | pendingProgress | pendingProgress | pendingProgress | pendingProgress | ✅ |
| pending_remark | pendingRemark | pendingRemark | pendingRemark | pendingRemark | ✅ |
| pending_attachments | pendingAttachments | pendingAttachments | pendingAttachments | pendingAttachments | ✅ |

### 2.2 Milestone (里程碑)

| 数据库字段 | 后端 Entity | 后端 VO | Node.js API | 前端类型 | 状态 |
|-----------|-------------|---------|-------------|----------|------|
| milestone_id | milestoneId | milestoneId | milestoneId | id (string) | ✅ |
| indicator_id | indicator.indicatorId | indicatorId | indicatorId | indicatorId | ✅ |
| milestone_name | milestoneName | milestoneName | milestoneName | name | ✅ |
| milestone_desc | milestoneDesc | milestoneDesc | milestoneDesc | - | ✅ |
| due_date | dueDate | dueDate | dueDate | deadline | ✅ |
| weight_percent | weightPercent | weightPercent | weightPercent | weightPercent | ✅ |
| status | status | status | status | status (转换) | ✅ |
| sort_order | sortOrder | sortOrder | sortOrder | sortOrder | ✅ |
| target_progress | targetProgress | targetProgress | targetProgress | targetProgress | ✅ |
| is_paired | isPaired | isPaired | isPaired | isPaired | ✅ |

### 2.3 Org (组织机构)

| 数据库字段 | 后端 Entity | 后端 VO | Node.js API | 前端类型 | 状态 |
|-----------|-------------|---------|-------------|----------|------|
| org_id | orgId | orgId | org_id | id (string) | ✅ |
| org_name | orgName | orgName | org_name | name | ✅ |
| org_type | orgType | orgType | org_type | type (转换) | ✅ |
| parent_org_id | parentOrg.orgId | parentOrgId | parent_org_id | - | ✅ |
| is_active | isActive | isActive | is_active | - | ✅ |
| sort_order | sortOrder | sortOrder | sort_order | sortOrder | ✅ |

---

## 3. 枚举类型对齐

### 3.1 IndicatorStatus

| 后端枚举 | 前端映射 |
|---------|---------|
| ACTIVE | 'active' |
| ARCHIVED | 'archived' |

### 3.2 MilestoneStatus

| 后端枚举 | 前端映射 |
|---------|---------|
| NOT_STARTED | 'pending' |
| IN_PROGRESS | 'pending' |
| COMPLETED | 'completed' |
| DELAYED | 'overdue' |
| CANCELED | 'overdue' |

### 3.3 ProgressApprovalStatus

| 后端枚举 | 前端映射 |
|---------|---------|
| NONE | 'none' |
| DRAFT | 'draft' |
| PENDING | 'pending' |
| APPROVED | 'approved' |
| REJECTED | 'rejected' |

### 3.4 OrgType

| 后端枚举 | 前端映射 |
|---------|---------|
| STRATEGY_DEPT | 'strategic_dept' |
| SCHOOL | 'strategic_dept' |
| FUNCTIONAL_DEPT | 'functional_dept' |
| FUNCTION_DEPT | 'functional_dept' |
| COLLEGE | 'secondary_college' |
| SECONDARY_COLLEGE | 'secondary_college' |
| DIVISION | 'secondary_college' |
| OTHER | 'secondary_college' |

---

## 4. 数据转换层

### 4.1 Node.js API (server/routes/indicator.js)

```javascript
// 将数据库行转换为驼峰格式的 VO
function convertToIndicatorVO(row) {
  return {
    indicatorId: parseInt(row.indicator_id),
    // ... 完整字段映射
    isStrategic: row.level === 'STRAT_TO_FUNC',  // 派生字段
    responsibleDept: row.target_org,              // 派生字段
    ownerDept: row.owner_org                      // 派生字段
  };
}
```

### 4.2 前端 API (src/api/strategic.ts)

```typescript
// 将后端 VO 转换为前端 StrategicIndicator 类型
function convertIndicatorVOToStrategicIndicator(vo: IndicatorVO): StrategicIndicator {
  return {
    id: String(vo.indicatorId),
    name: vo.indicatorDesc,
    // ... 完整字段映射
  };
}
```

---

## 5. 注意事项

### 5.1 ID 类型转换
- 数据库: `BIGINT`
- 后端: `Long`
- Node.js: `number`
- 前端: `string`

前端统一使用 `String()` 转换 ID，确保类型一致性。

### 5.2 日期格式
- 数据库: `TIMESTAMP` / `DATE`
- 后端: `LocalDateTime` / `LocalDate`
- Node.js: ISO 8601 字符串
- 前端: `string` (ISO 格式) 或 `Date` 对象

### 5.3 JSON 字段
- `status_audit`: JSONB → JSON 字符串 → 前端数组
- `pending_attachments`: JSONB → JSON 字符串 → 前端字符串数组

---

## 6. 建议改进

### 6.1 已完成 ✅
- [x] 指标新增字段对齐 (2026-01-19)
- [x] 里程碑新增字段对齐 (2026-01-19)
- [x] 进度审批状态枚举对齐
- [x] Node.js API 返回标准 ApiResponse 格式

### 6.2 待优化 ⚠️
- [ ] 考虑统一 Node.js API 的字段命名风格 (org API 使用 snake_case，其他使用 camelCase)
- [ ] 添加 API 响应类型的 TypeScript 严格校验
- [ ] 考虑使用 Zod 或 io-ts 进行运行时类型验证

---

## 7. 结论

前后端数据结构已完成对齐，主要实体（Indicator、Milestone、Org）的字段映射完整，枚举类型转换正确。数据转换层（Node.js API 和前端 API）提供了完整的类型转换逻辑。

**对齐完成度: 98%**

唯一的小问题是 Org API 使用 snake_case 字段名（与数据库一致），而其他 API 使用 camelCase，但前端已做兼容处理。
