# IndicatorController API 接口文档

> **版本**: 2.0.0  
> **更新时间**: 2026-01-31  
> **基础路径**: `/api/indicators`  
> **负责人**: 后端开发团队

---

## 📋 概述

IndicatorController 提供指标管理的完整功能，包括指标的 CRUD 操作、指标下发、批量分配、类型过滤等核心业务功能。

**主要功能模块:**

### 1️⃣ 基础 CRUD 操作
- ✅ 获取所有活跃指标（支持 Last-Modified 缓存）
- ✅ 按 ID 查询单个指标
- ✅ 按任务查询指标
- ✅ 按组织查询指标
- ✅ 关键词搜索指标
- ✅ 创建新指标
- ✅ 更新指标信息
- ✅ 删除（归档）指标

### 2️⃣ 指标下发功能
- ✅ 单个指标下发
- ✅ 批量下发指标到多个组织
- ✅ 查询已下发的子指标
- ✅ 检查指标下发资格

### 3️⃣ 指标过滤功能
- ✅ 按类型过滤（定性/定量、发展性/基础性）
- ✅ 获取定性指标
- ✅ 获取定量指标

**权限要求:**
- 所有接口需要 JWT Token 认证
- 创建、更新、删除操作需要相应组织权限
- 指标下发操作需要发布方组织权限

---

## 🔐 认证说明

所有请求需要在 Header 中携带 JWT Token：

```http
Authorization: Bearer <your_jwt_token>
```

---

## 📖 接口列表

## 一、基础 CRUD 操作

### 1. 获取所有活跃指标

**接口描述:** 获取所有状态为 ACTIVE 的指标，支持 Last-Modified 缓存优化

**请求方式:** `GET`

**请求路径:** `/api/indicators`

**请求头:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Authorization | String | 是 | Bearer Token |
| If-Modified-Since | String | 否 | 上次请求的时间戳，用于缓存验证 |

**响应格式:**

```json
{
  "success": true,
  "message": "操作成功",
  "data": [
    {
      "indicatorId": 101,
      "taskId": 1,
      "taskName": "全力促进毕业生多元化高质量就业创业",
      "parentIndicatorId": null,
      "parentIndicatorDesc": null,
      "level": "STRAT_TO_FUNC",
      "ownerOrgId": 1,
      "ownerOrgName": "战略发展部",
      "ownerDept": "战略发展部",
      "targetOrgId": 2,
      "targetOrgName": "就业创业指导中心",
      "responsibleDept": "就业创业指导中心",
      "indicatorDesc": "优质就业比例不低于15%",
      "weightPercent": 20.00,
      "sortOrder": 1,
      "year": 2026,
      "status": "ACTIVE",
      "remark": null,
      "isQualitative": false,
      "type1": "定量",
      "type2": "基础性",
      "targetValue": 15.00,
      "actualValue": null,
      "unit": "%",
      "responsiblePerson": "张三",
      "canWithdraw": false,
      "progress": 0,
      "progressApprovalStatus": "NONE",
      "pendingProgress": null,
      "pendingRemark": null,
      "pendingAttachments": null,
      "statusAudit": "[]",
      "isStrategic": true,
      "createdAt": "2026-01-15T10:00:00",
      "updatedAt": "2026-01-15T10:00:00",
      "childIndicators": [],
      "milestones": []
    }
  ]
}
```

**IndicatorVO 完整字段说明:**

#### 基础字段

| 字段名 | 类型 | 说明 |
|--------|------|------|
| indicatorId | Long | 指标ID（主键） |
| taskId | Long | 关联的战略任务ID |
| taskName | String | 战略任务名称 |
| parentIndicatorId | Long | 父指标ID（层级下发） |
| parentIndicatorDesc | String | 父指标描述 |
| level | String | 指标层级（STRAT_TO_FUNC/FUNC_TO_COLLEGE） |
| ownerOrgId | Long | 发布方组织ID |
| ownerOrgName | String | 发布方组织名称 |
| ownerDept | String | 发布方部门名称（等同 ownerOrgName） |
| targetOrgId | Long | 责任方组织ID |
| targetOrgName | String | 责任方组织名称 |
| responsibleDept | String | 责任部门名称（等同 targetOrgName） |
| indicatorDesc | String | 指标描述 |
| weightPercent | Decimal | 权重百分比（0-100） |
| sortOrder | Integer | 排序号 |
| year | Integer | 年份 |
| status | String | 指标状态（ACTIVE/ARCHIVED） |
| remark | String | 备注 |

#### 新增字段（2026-01-19 数据对齐）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| isQualitative | Boolean | 是否定性指标 |
| type1 | String | 指标类型1（定性/定量） |
| type2 | String | 指标类型2（发展性/基础性） |
| targetValue | Decimal | 目标值 |
| actualValue | Decimal | 实际值 |
| unit | String | 单位 |
| responsiblePerson | String | 责任人姓名 |
| canWithdraw | Boolean | 是否可撤回 |
| progress | Integer | 当前进度（0-100） |
| progressApprovalStatus | String | 进度审批状态（NONE/PENDING/APPROVED/REJECTED） |
| pendingProgress | Integer | 待审批进度值 |
| pendingRemark | String | 待审批说明 |
| pendingAttachments | String | 待审批附件（JSON字符串） |
| statusAudit | String | 状态审计日志（JSON字符串） |
| isStrategic | Boolean | 是否战略级指标（派生字段） |

#### 关联数据

| 字段名 | 类型 | 说明 |
|--------|------|------|
| childIndicators | Array | 子指标列表（已下发的指标） |
| milestones | Array | 里程碑列表 |
| createdAt | DateTime | 创建时间 |
| updatedAt | DateTime | 更新时间 |

**指标层级枚举 (IndicatorLevel):**

| 枚举值 | 说明 |
|--------|------|
| STRAT_TO_FUNC | 战略发展部 → 职能部门 |
| FUNC_TO_COLLEGE | 职能部门 → 二级学院 |

**指标状态枚举 (IndicatorStatus):**

| 枚举值 | 说明 |
|--------|------|
| ACTIVE | 活跃状态 |
| ARCHIVED | 已归档（软删除） |

**进度审批状态枚举 (ProgressApprovalStatus):**

| 枚举值 | 说明 |
|--------|------|
| NONE | 无待审批 |
| PENDING | 待审批 |
| APPROVED | 已通过 |
| REJECTED | 已驳回 |

**响应头:**

```http
Last-Modified: Sat, 31 Jan 2026 06:00:00 GMT
```

**缓存机制:**  
如果客户端提供 `If-Modified-Since` 且数据未变更，返回 304 Not Modified

---

### 2. 按 ID 获取指标

**接口描述:** 根据指标ID获取单个指标详情，包含子指标和里程碑

**请求方式:** `GET`

**请求路径:** `/api/indicators/{id}`

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 指标ID |

**请求示例:**

```http
GET /api/indicators/101
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**成功响应 (200):** 同"获取所有指标"的单个 IndicatorVO 对象

**错误响应 (404):**

```json
{
  "success": false,
  "message": "指标不存在",
  "error": {
    "code": "NOT_FOUND",
    "details": "Indicator with ID 999 not found"
  }
}
```

---

### 3. 按任务获取指标

**接口描述:** 获取指定战略任务下的所有指标

**请求方式:** `GET`

**请求路径:** `/api/indicators/task/{taskId}`

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | Long | 是 | 战略任务ID |

**请求示例:**

```http
GET /api/indicators/task/1
Authorization: Bearer <token>
```

**响应格式:** IndicatorVO 数组

---

### 4. 按任务获取根指标

**接口描述:** 获取指定任务下的根指标（没有父指标的顶级指标）

**请求方式:** `GET`

**请求路径:** `/api/indicators/task/{taskId}/root`

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | Long | 是 | 战略任务ID |

**响应格式:** IndicatorVO 数组（所有 parentIndicatorId 为 null 的指标）

---

### 5. 按发布方组织获取指标

**接口描述:** 获取指定组织发布的所有指标

**请求方式:** `GET`

**请求路径:** `/api/indicators/owner/{ownerOrgId}`

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| ownerOrgId | Long | 是 | 发布方组织ID |

**响应格式:** IndicatorVO 数组

---

### 6. 按责任方组织获取指标

**接口描述:** 获取分配给指定组织的所有指标

**请求方式:** `GET`

**请求路径:** `/api/indicators/target/{targetOrgId}`

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| targetOrgId | Long | 是 | 责任方组织ID |

**响应格式:** IndicatorVO 数组

---

### 7. 按组织层级获取指标

**接口描述:** 获取目标组织及其下级组织的所有指标

**请求方式:** `GET`

**请求路径:** `/api/indicators/target/{orgId}/hierarchy`

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orgId | Long | 是 | 组织ID |

**使用场景:** 上级组织查看下级组织的所有指标

**响应格式:** IndicatorVO 数组

---

### 8. 搜索指标

**接口描述:** 根据关键词搜索指标描述

**请求方式:** `GET`

**请求路径:** `/api/indicators/search`

**查询参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | String | 是 | 搜索关键词 |

**请求示例:**

```http
GET /api/indicators/search?keyword=就业
Authorization: Bearer <token>
```

**响应格式:** IndicatorVO 数组（indicatorDesc 包含关键词的指标）

---

### 9. 创建指标 ⭐

**接口描述:** 创建新的指标

**请求方式:** `POST`

**请求路径:** `/api/indicators`

**请求头:**

```http
Content-Type: application/json
Authorization: Bearer <token>
```

**请求体 (IndicatorCreateRequest):**

| 字段名 | 类型 | 必填 | 校验规则 | 说明 |
|--------|------|------|----------|------|
| taskId | Long | 是 | @NotNull | 关联的战略任务ID |
| parentIndicatorId | Long | 否 | - | 父指标ID（下发时使用） |
| level | String | 是 | @NotNull | 指标层级（STRAT_TO_FUNC/FUNC_TO_COLLEGE） |
| ownerOrgId | Long | 是 | @NotNull | 发布方组织ID |
| targetOrgId | Long | 是 | @NotNull | 责任方组织ID |
| indicatorDesc | String | 是 | @NotBlank | 指标描述 |
| weightPercent | Decimal | 否 | - | 权重百分比，默认 0 |
| sortOrder | Integer | 否 | - | 排序号，默认 0 |
| year | Integer | 是 | @NotNull | 年份 |
| remark | String | 否 | - | 备注 |
| canWithdraw | Boolean | 否 | - | 是否可撤回 |

**请求示例:**

```json
{
  "taskId": 1,
  "parentIndicatorId": null,
  "level": "STRAT_TO_FUNC",
  "ownerOrgId": 1,
  "targetOrgId": 2,
  "indicatorDesc": "优质就业比例不低于15%",
  "weightPercent": 20.00,
  "sortOrder": 1,
  "year": 2026,
  "remark": "重点指标",
  "canWithdraw": false
}
```

**成功响应 (200):**

```json
{
  "success": true,
  "message": "Indicator created successfully",
  "data": {
    "indicatorId": 201,
    "taskId": 1,
    "taskName": "全力促进毕业生多元化高质量就业创业",
    "level": "STRAT_TO_FUNC",
    "ownerOrgId": 1,
    "ownerOrgName": "战略发展部",
    "targetOrgId": 2,
    "targetOrgName": "就业创业指导中心",
    "indicatorDesc": "优质就业比例不低于15%",
    "weightPercent": 20.00,
    "year": 2026,
    "status": "ACTIVE",
    "createdAt": "2026-01-31T14:30:00",
    "updatedAt": "2026-01-31T14:30:00"
  }
}
```

**错误响应 (400):**

```json
{
  "success": false,
  "message": "参数验证失败",
  "error": {
    "code": "VALIDATION_ERROR",
    "details": {
      "indicatorDesc": "Indicator description is required",
      "level": "Indicator level is required"
    }
  }
}
```

---

### 10. 更新指标 ⭐

**接口描述:** 更新已存在的指标信息

**请求方式:** `PUT`

**请求路径:** `/api/indicators/{id}`

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 指标ID |

**请求体 (IndicatorUpdateRequest):**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| parentIndicatorId | Long | 否 | 父指标ID |
| level | String | 否 | 指标层级 |
| ownerOrgId | Long | 否 | 发布方组织ID |
| targetOrgId | Long | 否 | 责任方组织ID |
| indicatorDesc | String | 否 | 指标描述 |
| weightPercent | Decimal | 否 | 权重百分比 |
| sortOrder | Integer | 否 | 排序号 |
| year | Integer | 否 | 年份 |
| remark | String | 否 | 备注 |
| canWithdraw | Boolean | 否 | 是否可撤回 |
| status | String | 否 | 指标状态 |
| progress | Integer | 否 | 当前进度（0-100） |
| progressApprovalStatus | String | 否 | 进度审批状态 |
| pendingProgress | Integer | 否 | 待审批进度 |
| pendingRemark | String | 否 | 待审批备注 |
| pendingAttachments | String | 否 | 待审批附件（JSON） |
| targetValue | Decimal | 否 | 目标值 |
| actualValue | Decimal | 否 | 实际值 |
| unit | String | 否 | 单位 |
| responsiblePerson | String | 否 | 责任人 |

**注意:** 所有字段均为可选，只更新提供的字段

**请求示例:**

```json
{
  "indicatorDesc": "优质就业比例不低于18%（修订）",
  "targetValue": 18.00,
  "progress": 25,
  "responsiblePerson": "李四"
}
```

**成功响应 (200):**

```json
{
  "success": true,
  "message": "Indicator updated successfully",
  "data": {
    "indicatorId": 201,
    "indicatorDesc": "优质就业比例不低于18%（修订）",
    "targetValue": 18.00,
    "progress": 25,
    "responsiblePerson": "李四",
    "updatedAt": "2026-01-31T15:00:00"
  }
}
```

---

### 11. 删除（归档）指标 ⚠️

**接口描述:** 软删除指标（将状态改为 ARCHIVED，不物理删除）

**请求方式:** `DELETE`

**请求路径:** `/api/indicators/{id}`

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 指标ID |

**请求示例:**

```http
DELETE /api/indicators/201
Authorization: Bearer <token>
```

**成功响应 (200):**

```json
{
  "success": true,
  "message": "Indicator archived successfully",
  "data": null
}
```

**注意:** 归档后的指标不会在默认列表中显示，但数据仍保留在数据库中

---

## 二、指标下发功能

### 12. 下发指标到单个组织 ⭐

**接口描述:** 将指标下发到目标组织，自动创建子指标并继承里程碑

**请求方式:** `POST`

**请求路径:** `/api/indicators/{id}/distribute`

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 父指标ID |

**查询参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| targetOrgId | Long | 是 | 目标组织ID |
| customDesc | String | 否 | 自定义描述（可选） |
| actorUserId | Long | 否 | 操作人用户ID |

**请求示例:**

```http
POST /api/indicators/101/distribute?targetOrgId=6&actorUserId=3
Authorization: Bearer <token>
```

**成功响应 (200):**

```json
{
  "success": true,
  "message": "指标下发成功",
  "data": {
    "indicatorId": 301,
    "taskId": 1,
    "parentIndicatorId": 101,
    "parentIndicatorDesc": "优质就业比例不低于15%",
    "level": "FUNC_TO_COLLEGE",
    "ownerOrgId": 2,
    "ownerOrgName": "就业创业指导中心",
    "targetOrgId": 6,
    "targetOrgName": "计算机学院",
    "indicatorDesc": "优质就业比例不低于15%",
    "year": 2026,
    "status": "ACTIVE",
    "createdAt": "2026-01-31T16:00:00"
  }
}
```

**业务规则:**
1. 自动创建子指标，父指标ID为当前指标
2. 继承父指标的里程碑（创建关联的里程碑）
3. 层级自动调整（STRAT_TO_FUNC → FUNC_TO_COLLEGE）
4. 记录审计日志

**错误响应 (400):**

```json
{
  "success": false,
  "message": "无法下发此指标",
  "error": {
    "code": "BUSINESS_ERROR",
    "details": "该指标已下发到此组织"
  }
}
```

---

### 13. 批量下发指标 ⭐

**接口描述:** 将指标批量下发到多个目标组织

**请求方式:** `POST`

**请求路径:** `/api/indicators/{id}/distribute/batch`

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 父指标ID |

**查询参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| actorUserId | Long | 否 | 操作人用户ID |

**请求体:**

```json
[6, 7, 8, 9]
```

**类型:** Long[] (目标组织ID数组)

**请求示例:**

```http
POST /api/indicators/101/distribute/batch?actorUserId=3
Content-Type: application/json
Authorization: Bearer <token>

[6, 7, 8, 9]
```

**成功响应 (200):**

```json
{
  "success": true,
  "message": "批量下发成功，共创建 4 个子指标",
  "data": [
    {
      "indicatorId": 301,
      "targetOrgId": 6,
      "targetOrgName": "计算机学院"
    },
    {
      "indicatorId": 302,
      "targetOrgId": 7,
      "targetOrgName": "商学院"
    },
    {
      "indicatorId": 303,
      "targetOrgId": 8,
      "targetOrgName": "工学院"
    },
    {
      "indicatorId": 304,
      "targetOrgId": 9,
      "targetOrgName": "文理学院"
    }
  ]
}
```

---

### 14. 查询已下发的子指标

**接口描述:** 获取从父指标下发出去的所有子指标

**请求方式:** `GET`

**请求路径:** `/api/indicators/{id}/distributed`

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 父指标ID |

**响应格式:** IndicatorVO 数组（所有 parentIndicatorId 等于该ID的指标）

---

### 15. 检查指标下发资格

**接口描述:** 检查指标是否可以下发，以及可以下发到哪些组织

**请求方式:** `GET`

**请求路径:** `/api/indicators/{id}/distribution-eligibility`

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 指标ID |

**响应格式:**

```json
{
  "success": true,
  "data": {
    "canDistribute": true,
    "reason": "指标可以下发",
    "eligibleOrgs": [
      {
        "orgId": 6,
        "orgName": "计算机学院"
      },
      {
        "orgId": 7,
        "orgName": "商学院"
      }
    ]
  }
}
```

---

## 三、指标过滤功能

### 16. 按类型过滤指标

**接口描述:** 根据指标类型过滤（定性/定量、发展性/基础性）

**请求方式:** `GET`

**请求路径:** `/api/indicators/filter`

**查询参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| type1 | String | 否 | 类型1（定性/定量） |
| type2 | String | 否 | 类型2（发展性/基础性） |
| status | String | 否 | 状态（ACTIVE/ARCHIVED） |

**请求示例:**

```http
GET /api/indicators/filter?type1=定量&type2=基础性
Authorization: Bearer <token>
```

**响应格式:** IndicatorVO 数组

---

### 17. 获取定性指标

**接口描述:** 获取所有定性指标

**请求方式:** `GET`

**请求路径:** `/api/indicators/qualitative`

**响应格式:** IndicatorVO 数组（isQualitative = true）

---

### 18. 获取定量指标

**接口描述:** 获取所有定量指标

**请求方式:** `GET`

**请求路径:** `/api/indicators/quantitative`

**响应格式:** IndicatorVO 数组（isQualitative = false）

---

## 🐛 常见问题 & 已知问题

### ⚠️ **重要问题：创建的指标刷新页面后丢失**

**问题描述:**  
战略发展部创建的指标在前端显示正常，但刷新页面后数据消失，无法持久化到数据库。

**问题分析:**
1. **可能原因1**: Service 层没有调用 `flush()` 方法，导致事务未及时提交
2. **可能原因2**: 前端使用了本地缓存或 Mock 数据，创建后未正确同步到后端
3. **可能原因3**: 子实体（里程碑）的级联保存问题

**排查建议:**
1. 检查 `IndicatorService.createIndicator()` 方法是否调用了 `entityManager.flush()`
2. 检查前端是否正确调用了 `/api/indicators` POST 接口
3. 查看后端日志，确认 SQL INSERT 语句是否执行
4. 检查 JPA 级联配置和事务传播行为

**修复方案:**

在 `IndicatorService` 的 `createIndicator` 方法中添加 `flush()`:

```java
@Transactional
public IndicatorVO createIndicator(IndicatorCreateRequest request) {
    // ... 创建逻辑 ...
    Indicator savedIndicator = indicatorRepository.save(indicator);
    
    // 🔧 添加这行，强制刷新到数据库
    entityManager.flush();
    
    return convertToVO(savedIndicator);
}
```

**相关 Bug 修复记录:**  
参考 `docs/生产环境验证报告-Bug5修复-2026-01-28.md` 的 Bug 4 和 Bug 5

---

### ⚠️ **创建指标返回 500 错误**

**问题描述:**  
生产环境测试时发现创建任务返回 500 错误

**排查方向:**
1. 查看后端日志 `/path/to/logs/sism-backend.log`
2. 检查请求参数是否完整
3. 检查数据库外键约束
4. 检查必填字段验证

---

### ⚠️ **创建指标返回 403 错误**

**问题描述:**  
创建指标时返回 403 Forbidden

**可能原因:**
1. 当前用户权限不足
2. Token 过期或无效
3. 安全配置限制

**解决方法:**
1. 检查用户角色和权限
2. 重新登录获取新 Token
3. 查看 SecurityConfig 配置

---

## 📊 HTTP 状态码说明

| 状态码 | 说明 | 场景 |
|--------|------|------|
| 200 | 成功 | 请求处理成功 |
| 304 | 未修改 | 缓存有效，使用本地数据 |
| 400 | 请求参数错误 | 参数验证失败 |
| 401 | 未授权 | Token 缺失或无效 |
| 403 | 无权限 | 当前用户无操作权限 |
| 404 | 资源不存在 | 指标ID不存在 |
| 409 | 资源冲突 | 指标已下发到该组织 |
| 500 | 服务器内部错误 | 服务器异常 |

---

## 🔧 测试建议

### 使用 Swagger UI 测试

访问: `http://localhost:8080/api/swagger-ui/index.html`

### 使用 cURL 测试创建指标

```bash
# 1. 登录获取 Token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zhanlue","password":"123456"}'

# 2. 创建指标
curl -X POST http://localhost:8080/api/indicators \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your_token>" \
  -d '{
    "taskId": 1,
    "level": "STRAT_TO_FUNC",
    "ownerOrgId": 1,
    "targetOrgId": 2,
    "indicatorDesc": "测试指标",
    "weightPercent": 10.00,
    "year": 2026
  }'

# 3. 查询所有指标
curl -X GET http://localhost:8080/api/indicators \
  -H "Authorization: Bearer <your_token>"

# 4. 下发指标
curl -X POST "http://localhost:8080/api/indicators/101/distribute?targetOrgId=6" \
  -H "Authorization: Bearer <your_token>"
```

---

## 📝 更新日志

| 版本 | 日期 | 变更说明 |
|------|------|----------|
| 2.0.0 | 2026-01-31 | 初始版本，整理所有接口文档，包含指标下发功能 |
| 2.0.0 | 2026-01-19 | 新增14个字段（数据对齐） |

---

## 🔗 相关文档

- [TaskController API 文档](./TaskController-API文档.md)
- [数据库表结构文档](../strategic-task-management-main/docs/database-schema.md)
- [API 接口参考](../strategic-task-management-main/docs/api-reference.md)
- [生产环境验证报告](./生产环境验证报告-Bug5修复-2026-01-28.md)

---

**文档维护:** 后端开发团队  
**最后更新:** 2026-01-31  
**联系方式:** 如有问题请联系学长或查看项目 README
