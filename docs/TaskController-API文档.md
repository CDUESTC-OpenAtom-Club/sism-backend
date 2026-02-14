# TaskController API 接口文档

> **版本**: 1.0.0  
> **更新时间**: 2026-01-31  
> **基础路径**: `/api/tasks`  
> **负责人**: 后端开发团队

---

## 📋 概述

TaskController 提供战略任务的完整 CRUD 操作，包括任务的创建、查询、更新和删除功能。

**主要功能:**
- ✅ 获取所有任务列表
- ✅ 按 ID 查询单个任务
- ✅ 按考核周期查询任务
- ✅ 按组织查询任务
- ✅ 关键词搜索任务
- ✅ 创建新任务
- ✅ 更新任务信息
- ✅ 删除任务

**权限要求:**
- 所有接口需要 JWT Token 认证
- 创建、更新、删除操作需要战略发展部权限

---

## 🔐 认证说明

所有请求需要在 Header 中携带 JWT Token：

```http
Authorization: Bearer <your_jwt_token>
```

---

## 📖 接口列表

### 1. 获取所有任务

**接口描述:** 获取系统中所有战略任务列表

**请求方式:** `GET`

**请求路径:** `/api/tasks`

**请求参数:** 无

**响应格式:**

```json
{
  "success": true,
  "message": "操作成功",
  "data": [
    {
      "taskId": 1,
      "cycleId": 1,
      "cycleName": "2026年度考核",
      "year": 2026,
      "taskName": "全力促进毕业生多元化高质量就业创业",
      "taskDesc": "提高就业率和就业质量",
      "taskType": "KEY",
      "orgId": 2,
      "orgName": "就业创业指导中心",
      "createdByOrgId": 1,
      "createdByOrgName": "战略发展部",
      "sortOrder": 1,
      "remark": "重点任务",
      "createdAt": "2026-01-15T10:30:00",
      "updatedAt": "2026-01-15T10:30:00"
    }
  ]
}
```

**TaskVO 字段说明:**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| taskId | Long | 任务ID（主键） |
| cycleId | Long | 考核周期ID |
| cycleName | String | 考核周期名称 |
| year | Integer | 年份 |
| taskName | String | 任务名称 |
| taskDesc | String | 任务描述 |
| taskType | String | 任务类型（BASIC/REGULAR/KEY/SPECIAL/QUANTITATIVE/DEVELOPMENT） |
| orgId | Long | 负责组织ID |
| orgName | String | 负责组织名称 |
| createdByOrgId | Long | 创建方组织ID |
| createdByOrgName | String | 创建方组织名称 |
| sortOrder | Integer | 排序号 |
| remark | String | 备注 |
| createdAt | DateTime | 创建时间 |
| updatedAt | DateTime | 更新时间 |

**任务类型枚举 (TaskType):**

| 枚举值 | 说明 |
|--------|------|
| BASIC | 基础性任务 |
| REGULAR | 常规任务 |
| KEY | 重点任务 |
| SPECIAL | 专项任务 |
| QUANTITATIVE | 定量任务 |
| DEVELOPMENT | 发展性任务 |

---

### 2. 按 ID 获取任务

**接口描述:** 根据任务ID获取单个任务详情

**请求方式:** `GET`

**请求路径:** `/api/tasks/{id}`

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 任务ID |

**请求示例:**

```http
GET /api/tasks/1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**成功响应 (200):**

```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "taskId": 1,
    "cycleId": 1,
    "cycleName": "2026年度考核",
    "year": 2026,
    "taskName": "全力促进毕业生多元化高质量就业创业",
    "taskDesc": "提高就业率和就业质量",
    "taskType": "KEY",
    "orgId": 2,
    "orgName": "就业创业指导中心",
    "createdByOrgId": 1,
    "createdByOrgName": "战略发展部",
    "sortOrder": 1,
    "remark": "重点任务",
    "createdAt": "2026-01-15T10:30:00",
    "updatedAt": "2026-01-15T10:30:00"
  }
}
```

**错误响应 (404):**

```json
{
  "success": false,
  "message": "任务不存在",
  "error": {
    "code": "NOT_FOUND",
    "details": "Task with ID 999 not found"
  }
}
```

---

### 3. 按考核周期获取任务

**接口描述:** 获取指定考核周期下的所有任务

**请求方式:** `GET`

**请求路径:** `/api/tasks/cycle/{cycleId}`

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| cycleId | Long | 是 | 考核周期ID |

**请求示例:**

```http
GET /api/tasks/cycle/1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**响应格式:** 同"获取所有任务"，返回 TaskVO 数组

---

### 4. 按组织获取任务

**接口描述:** 获取指定组织负责的所有任务

**请求方式:** `GET`

**请求路径:** `/api/tasks/org/{orgId}`

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orgId | Long | 是 | 组织ID |

**请求示例:**

```http
GET /api/tasks/org/2
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**响应格式:** 同"获取所有任务"，返回 TaskVO 数组

---

### 5. 搜索任务

**接口描述:** 根据关键词搜索任务（支持任务名称、描述模糊搜索）

**请求方式:** `GET`

**请求路径:** `/api/tasks/search`

**查询参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | String | 是 | 搜索关键词 |

**请求示例:**

```http
GET /api/tasks/search?keyword=就业
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**响应格式:** 同"获取所有任务"，返回匹配的 TaskVO 数组

---

### 6. 创建任务 ⭐

**接口描述:** 创建新的战略任务（仅战略发展部可操作）

**请求方式:** `POST`

**请求路径:** `/api/tasks`

**请求头:**

```http
Content-Type: application/json
Authorization: Bearer <token>
```

**请求体 (TaskCreateRequest):**

| 字段名 | 类型 | 必填 | 校验规则 | 说明 |
|--------|------|------|----------|------|
| cycleId | Long | 是 | @NotNull | 考核周期ID |
| taskName | String | 是 | @NotBlank, @Size(max=200) | 任务名称 |
| taskDesc | String | 否 | - | 任务描述 |
| taskType | String | 否 | 枚举值 | 任务类型，默认 BASIC |
| orgId | Long | 是 | @NotNull | 负责组织ID |
| createdByOrgId | Long | 是 | @NotNull | 创建方组织ID |
| sortOrder | Integer | 否 | - | 排序号，默认 0 |
| remark | String | 否 | - | 备注 |

**请求示例:**

```json
{
  "cycleId": 1,
  "taskName": "全力促进毕业生多元化高质量就业创业",
  "taskDesc": "提高就业率和就业质量，确保毕业生高质量就业",
  "taskType": "KEY",
  "orgId": 2,
  "createdByOrgId": 1,
  "sortOrder": 1,
  "remark": "2026年重点任务"
}
```

**成功响应 (200):**

```json
{
  "success": true,
  "message": "Task created successfully",
  "data": {
    "taskId": 11,
    "cycleId": 1,
    "cycleName": "2026年度考核",
    "year": 2026,
    "taskName": "全力促进毕业生多元化高质量就业创业",
    "taskDesc": "提高就业率和就业质量，确保毕业生高质量就业",
    "taskType": "KEY",
    "orgId": 2,
    "orgName": "就业创业指导中心",
    "createdByOrgId": 1,
    "createdByOrgName": "战略发展部",
    "sortOrder": 1,
    "remark": "2026年重点任务",
    "createdAt": "2026-01-31T14:30:00",
    "updatedAt": "2026-01-31T14:30:00"
  }
}
```

**错误响应 (400 - 参数验证失败):**

```json
{
  "success": false,
  "message": "参数验证失败",
  "error": {
    "code": "VALIDATION_ERROR",
    "details": {
      "taskName": "Task name is required",
      "cycleId": "Cycle ID is required"
    }
  }
}
```

---

### 7. 更新任务 ⭐

**接口描述:** 更新已存在的战略任务

**请求方式:** `PUT`

**请求路径:** `/api/tasks/{id}`

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 任务ID |

**请求体 (TaskUpdateRequest):**

| 字段名 | 类型 | 必填 | 校验规则 | 说明 |
|--------|------|------|----------|------|
| taskName | String | 否 | @Size(max=200) | 任务名称 |
| taskDesc | String | 否 | - | 任务描述 |
| taskType | String | 否 | 枚举值 | 任务类型 |
| orgId | Long | 否 | - | 负责组织ID |
| sortOrder | Integer | 否 | - | 排序号 |
| remark | String | 否 | - | 备注 |

**注意:** 所有字段均为可选，只更新提供的字段

**请求示例:**

```json
{
  "taskName": "全力促进毕业生多元化高质量就业创业（修订版）",
  "taskDesc": "更新后的任务描述",
  "sortOrder": 2
}
```

**成功响应 (200):**

```json
{
  "success": true,
  "message": "Task updated successfully",
  "data": {
    "taskId": 11,
    "taskName": "全力促进毕业生多元化高质量就业创业（修订版）",
    "taskDesc": "更新后的任务描述",
    "sortOrder": 2,
    "updatedAt": "2026-01-31T15:00:00"
  }
}
```

**错误响应 (404):**

```json
{
  "success": false,
  "message": "任务不存在",
  "error": {
    "code": "NOT_FOUND",
    "details": "Task with ID 999 not found"
  }
}
```

---

### 8. 删除任务 ⚠️

**接口描述:** 删除指定的战略任务（物理删除，谨慎操作）

**请求方式:** `DELETE`

**请求路径:** `/api/tasks/{id}`

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 任务ID |

**请求示例:**

```http
DELETE /api/tasks/11
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**成功响应 (200):**

```json
{
  "success": true,
  "message": "Task deleted successfully",
  "data": null
}
```

**错误响应 (404):**

```json
{
  "success": false,
  "message": "任务不存在",
  "error": {
    "code": "NOT_FOUND",
    "details": "Task with ID 999 not found"
  }
}
```

---

## 🐛 常见问题 & 已知问题

### ⚠️ **重要问题：创建的任务刷新页面后丢失**

**问题描述:**  
战略发展部创建的任务和指标在前端显示正常，但刷新页面后数据消失，无法持久化到数据库。

**问题分析:**
1. **可能原因1**: Service 层没有调用 `flush()` 方法，导致事务未及时提交
2. **可能原因2**: 前端可能使用了本地缓存或 Mock 数据，创建后未正确同步到后端
3. **可能原因3**: 数据库事务配置问题

**排查建议:**
1. 检查 `TaskService.createTask()` 方法是否调用了 `entityManager.flush()`
2. 检查前端是否正确调用了 `/api/tasks` POST 接口
3. 查看后端日志，确认 SQL INSERT 语句是否执行
4. 检查数据库事务隔离级别和自动提交配置

**修复方案:**

在 `TaskService` 的 `createTask` 方法中添加 `flush()`:

```java
@Transactional
public TaskVO createTask(TaskCreateRequest request) {
    // ... 创建逻辑 ...
    StrategicTask savedTask = taskRepository.save(task);
    
    // 🔧 添加这行，强制刷新到数据库
    entityManager.flush();
    
    return convertToVO(savedTask);
}
```

---

## 📊 HTTP 状态码说明

| 状态码 | 说明 | 场景 |
|--------|------|------|
| 200 | 成功 | 请求处理成功 |
| 400 | 请求参数错误 | 参数验证失败 |
| 401 | 未授权 | Token 缺失或无效 |
| 403 | 无权限 | 当前用户无操作权限 |
| 404 | 资源不存在 | 任务ID不存在 |
| 500 | 服务器内部错误 | 服务器异常 |

---

## 🔧 测试建议

### 使用 Swagger UI 测试

访问: `http://localhost:8080/api/swagger-ui/index.html`

### 使用 cURL 测试

```bash
# 1. 登录获取 Token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zhanlue","password":"123456"}'

# 2. 创建任务
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your_token>" \
  -d '{
    "cycleId": 1,
    "taskName": "测试任务",
    "taskDesc": "测试描述",
    "taskType": "BASIC",
    "orgId": 2,
    "createdByOrgId": 1,
    "sortOrder": 0
  }'

# 3. 查询所有任务
curl -X GET http://localhost:8080/api/tasks \
  -H "Authorization: Bearer <your_token>"
```

---

## 📝 更新日志

| 版本 | 日期 | 变更说明 |
|------|------|----------|
| 1.0.0 | 2026-01-31 | 初始版本，整理所有接口文档 |

---

**文档维护:** 后端开发团队  
**最后更新:** 2026-01-31  
**联系方式:** 如有问题请联系学长或查看项目 README
