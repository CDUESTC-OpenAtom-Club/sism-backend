#  API 接口文档

## 文档说明

本文档描述了完整 REST API 接口规范，覆盖**战略→职能→学院**全链路业务闭环。

**总计接口数**：约 205 个端点（新增38个）
**基础路径**：`/api/v1`
**认证方式**：Bearer Token (JWT)
**数据格式**：JSON

---

## 全局错误码

为了帮助客户端开发者建立统一、健壮的错误处理逻辑，本章节集中列出所有通用的HTTP状态码以及系统自定义的业务错误码。

### HTTP 状态码

所有接口返回的标准 HTTP 状态码及其含义：

| HTTP 状态码 | 说明 | 业务场景 |
|------------|------|----------|
| 200 OK | 请求成功 | 请求被成功处理并返回数据 |
| 201 Created | 创建成功 | 资源创建成功并返回 |
| 204 No Content | 无内容 | 请求成功但无返回内容（如删除操作） |
| 400 Bad Request | 请求参数错误 | 请求参数格式错误、必填参数缺失、参数验证失败 |
| 401 Unauthorized | 未认证 | Token缺失、Token过期、Token无效 |
| 403 Forbidden | 无权限 | Token有效但无权限访问该资源 |
| 404 Not Found | 资源不存在 | 请求的资源不存在 |
| 409 Conflict | 资源冲突 | 资源已存在（如用户名重复）、状态冲突 |
| 422 Unprocessable Entity | 无法处理的实体 | 业务逻辑验证失败（如状态不允许该操作） |
| 429 Too Many Requests | 请求过多 | 触发限流 |
| 500 Internal Server Error | 服务器内部错误 | 服务器异常、未捕获的错误 |
| 503 Service Unavailable | 服务不可用 | 服务维护中、依赖服务不可用 |

### 业务错误码

系统自定义的业务错误码，在响应体中的 `code` 字段返回：

#### 通用错误码 (1000-1999)

| 错误码 | 说明 | HTTP状态码 | 处理建议 |
|--------|------|-----------|----------|
| 1000 | 系统错误 | 500 | 联系系统管理员 |
| 1001 | 参数验证失败 | 400 | 检查请求参数 |
| 1002 | 数据不存在 | 404 | 确认资源ID是否正确 |
| 1003 | 数据已存在 | 409 | 修改唯一标识字段 |
| 1004 | 操作不允许 | 422 | 检查当前业务状态 |
| 1005 | 数据格式错误 | 400 | 检查数据格式 |
| 1006 | 请求超时 | 408 | 重试或检查网络 |
| 1007 | 服务维护中 | 503 | 稍后重试 |
| 1008 | 限流 | 429 | 降低请求频率 |
| 1009 | 数据版本冲突 | 409 | 刷新数据重试 |

#### 认证授权错误码 (2000-2999)

| 错误码 | 说明 | HTTP状态码 | 处理建议 |
|--------|------|-----------|----------|
| 2000 | 未登录 | 401 | 跳转登录页 |
| 2001 | Token过期 | 401 | 使用刷新令牌刷新Token |
| 2002 | Token无效 | 401 | 重新登录 |
| 2003 | 用户名或密码错误 | 401 | 检查用户名密码 |
| 2004 | 用户已被禁用 | 403 | 联系管理员 |
| 2005 | 无权限访问 | 403 | 申请相应权限 |
| 2006 | 刷新令牌过期 | 401 | 重新登录 |
| 2007 | 验证码错误 | 400 | 重新输入验证码 |
| 2008 | 验证码已过期 | 400 | 刷新验证码 |
| 2009 | 密码强度不足 | 400 | 使用更强的密码 |
| 2010 | 原密码错误 | 400 | 确认原密码 |

#### 用户管理错误码 (3000-3999)

| 错误码 | 说明 | HTTP状态码 | 处理建议 |
|--------|------|-----------|----------|
| 3000 | 用户不存在 | 404 | 确认用户ID |
| 3001 | 用户名已存在 | 409 | 使用其他用户名 |
| 3002 | 邮箱已存在 | 409 | 使用其他邮箱 |
| 3003 | 手机号已存在 | 409 | 使用其他手机号 |
| 3004 | 用户状态异常 | 422 | 联系管理员 |
| 3005 | 不能删除自己 | 422 | 使用其他管理员删除 |
| 3006 | 不能禁用自己 | 422 | 使用其他管理员禁用 |
| 3007 | 用户有待办任务 | 422 | 完成任务后再操作 |
| 3008 | 角色不存在 | 404 | 确认角色ID |
| 3009 | 角色已被使用 | 409 | 删除角色关联后操作 |
| 3010 | 权限不存在 | 404 | 确认权限ID |
| 3011 | 不能删除系统角色 | 422 | 修改而非删除系统角色 |
| 3012 | 角色无权限变更 | 403 | 联系管理员 |
| 3013 | 邮箱格式错误 | 400 | 检查邮箱格式 |
| 3014 | 手机号格式错误 | 400 | 检查手机号格式 |

#### 指标管理错误码 (4000-4999)

| 错误码 | 说明 | HTTP状态码 | 处理建议 |
|--------|------|-----------|----------|
| 4000 | 指标不存在 | 404 | 确认指标ID |
| 4001 | 指标编码已存在 | 409 | 使用其他编码 |
| 4002 | 指标已下发 | 422 | 先撤回再操作 |
| 4003 | 指标已提交审批 | 422 | 等待审批完成 |
| 4004 | 不能删除已发布的指标 | 422 | 先撤回再删除 |
| 4005 | 指标权重总和必须为1 | 400 | 调整指标权重 |
| 4006 | 目标组织不存在 | 404 | 确认组织ID |
| 4007 | 责任组织不存在 | 404 | 确认组织ID |
| 4008 | 指标层级错误 | 400 | 检查指标层级 |
| 4009 | 任务不存在 | 404 | 确认任务ID |
| 4010 | 指标值超出范围 | 400 | 检查指标值范围 |
| 4020 | 周期年份已存在 | 409 | 使用其他年份 |
| 4021 | 日期范围无效 | 400 | 检查日期范围 |
| 4022 | 里程碑月份重复 | 409 | 检查月份设置 |
| 4023 | 目标进度值无效 | 400 | 检查进度值 |
| 4024 | 里程碑不存在 | 404 | 确认里程碑ID |
| 4025 | 里程碑已完成，不可修改 | 422 | 无法修改已完成里程碑 |
| 4026 | 里程碑已有填报数据，不可删除 | 422 | 先删除填报数据 |
| 4027 | 指标状态不允许下发 | 422 | 检查指标状态 |
| 4028 | 目标组织不存在 | 404 | 确认组织ID |
| 4029 | 指标未下发，无法撤回 | 422 | 指标需先下发 |
| 4030 | 指标已审批通过，不可撤回 | 422 | 联系管理员 |

#### 审批流程错误码 (5000-5999)

| 错误码 | 说明 | HTTP状态码 | 处理建议 |
|--------|------|-----------|----------|
| 5000 | 审批实例不存在 | 404 | 确认审批实例ID |
| 5001 | 审批流不存在 | 404 | 确认审批流编码 |
| 5002 | 审批流已禁用 | 422 | 启用审批流或使用其他流程 |
| 5003 | 当前用户不是审批人 | 403 | 联系实际审批人 |
| 5004 | 审批已通过 | 422 | 无需重复审批 |
| 5005 | 审批已驳回 | 422 | 重新提交后再审批 |
| 5006 | 审批已撤回 | 422 | 无需操作 |
| 5007 | 不是当前步骤 | 422 | 等待前置步骤完成 |
| 5008 | 审批超时 | 422 | 联系管理员处理 |
| 5009 | 无审批权限 | 403 | 联系管理员授权 |
| 5010 | 不能撤回已审批的实例 | 422 | 联系管理员 |
| 5011 | 审批流版本冲突 | 409 | 刷新配置 |

#### 组织管理错误码 (6000-6999)

| 错误码 | 说明 | HTTP状态码 | 处理建议 |
|--------|------|-----------|----------|
| 6000 | 组织不存在 | 404 | 确认组织ID |
| 6001 | 组织编码已存在 | 409 | 使用其他编码 |
| 6002 | 不能删除有子组织的组织 | 422 | 先删除子组织 |
| 6003 | 不能删除有用户的组织 | 422 | 先迁移用户 |
| 6004 | 组织层级超过限制 | 400 | 调整组织层级 |
| 6005 | 父组织不存在 | 404 | 确认父组织ID |
| 6006 | 不能将自己设为父组织 | 422 | 使用其他父组织 |

#### 附件管理错误码 (7000-7999)

| 错误码 | 说明 | HTTP状态码 | 处理建议 |
|--------|------|-----------|----------|
| 7000 | 附件不存在 | 404 | 确认附件ID |
| 7001 | 文件大小超限 | 400 | 压缩文件后重试 |
| 7002 | 文件类型不允许 | 400 | 使用允许的文件类型 |
| 7003 | 文件上传失败 | 500 | 重试或联系管理员 |
| 7004 | 文件下载失败 | 500 | 重试或联系管理员 |
| 7005 | 附件数量超限 | 400 | 删除部分附件 |
| 7006 | 文件内容损坏 | 400 | 重新上传文件 |

### 错误响应示例

#### 参数验证失败 (400)
```json
{
  "code": 1001,
  "message": "参数验证失败",
  "errors": [
    {
      "field": "name",
      "message": "不能为空"
    },
    {
      "field": "email",
      "message": "格式错误"
    }
  ],
  "timestamp": "2024-01-21T10:00:00"
}
```

#### 未认证 (401)
```json
{
  "code": 2002,
  "message": "Token无效",
  "timestamp": "2024-01-21T10:00:00"
}
```

#### 无权限 (403)
```json
{
  "code": 2005,
  "message": "无权限访问该资源",
  "requiredPermission": "indicator:manage",
  "timestamp": "2024-01-21T10:00:00"
}
```

#### 资源不存在 (404)
```json
{
  "code": 1002,
  "message": "数据不存在",
  "resourceType": "Indicator",
  "resourceId": "999",
  "timestamp": "2024-01-21T10:00:00"
}
```

#### 业务逻辑错误 (422)
```json
{
  "code": 2004,
  "message": "不能删除已发布的指标",
  "indicatorId": 1,
  "currentStatus": "PUBLISHED",
  "suggestion": "先撤回指标再删除",
  "timestamp": "2024-01-21T10:00:00"
}
```

#### 服务器错误 (500)
```json
{
  "code": 1000,
  "message": "系统错误",
  "errorId": "ERR-20240121-0001",
  "timestamp": "2024-01-21T10:00:00"
}
```

### 前端错误处理建议

```javascript
// 统一错误处理拦截器
axios.interceptors.response.use(
  response => response,
  error => {
    const { code, message } = error.response.data || {}

    switch (code) {
      // 认证错误 - 清除token并跳转登录
      case 2000:
      case 2001:
      case 2002:
      case 2006:
        localStorage.clear()
        window.location.href = '/login'
        break

      // 权限错误 - 提示无权限
      case 2005:
      case 5010:
        Message.error(message || '无权限执行此操作')
        break

      // 参数错误 - 提示检查输入
      case 1001:
        // 显示具体字段错误
        if (error.response.data.errors) {
          error.response.data.errors.forEach(err => {
            Message.error(`${err.field}: ${err.message}`)
          })
        }
        break

      // 业务错误 - 提示具体原因
      case 4002:
      case 4004:
      case 5004:
        Message.warning(message)
        break

      // 系统错误 - 提示联系管理员
      case 1000:
        Message.error('系统错误，请联系管理员')
        break

      default:
        Message.error(message || '请求失败')
    }

    return Promise.reject(error)
  }
)
```

---

## 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                    业务工作流层                           │
│  - 指标下发流程：总部→部门→学院                            │
│  - 计划填报流程：填报→提交→审批                            │
│  - 报告提交流程：起草→提交→多级审批                         │
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │         调用审批工作流引擎进行审批                │    │
│  └──────────────────┬─────────────────────────────┘    │
│                     │                                    │
│                     ▼                                    │
│  ┌────────────────────────────────────────────────┐    │
│  │              审批工作流引擎                       │    │
│  │  - 审批流定义（流程配置、步骤管理）                │    │
│  │  - 审批实例管理（发起、待办、通过、驳回）           │    │
│  │  - 审批历史记录                                    │    │
│  │  - 审批统计                                        │    │
│  │  - 特点：通用化、可复用、不绑定具体业务             │    │
│  └────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

---

## 目录

1. [认证授权](#1-认证授权)
2. [指标管理](#2-指标管理)
3. [任务管理](#3-任务管理)
4. [计划管理](#4-计划管理)
5. [审批工作流引擎](#5-审批工作流引擎)待完成列表
6. [业务工作流](#6-业务工作流)
7. [用户管理](#7-用户管理)
8. [报告管理](#8-报告管理)
9. [附件管理](#9-附件管理)
10. [通知中心](#10-通知中心)
11. [预警告警](#11-预警告警)
12. [系统监控](#12-系统监控)
13. [批量&扩展](#13-批量扩展)
14. [首页/工作台](#14-首页工作台)
15. [个人中心](#15-个人中心)
16. [组织架构管理](#16-组织架构管理)
17. [数据字典](#17-数据字典)
18. [系统配置](#18-系统配置)

---

## 1. 认证授权

**业务场景**：用户登录、令牌刷新、登出、注册、获取当前用户信息

**Controller**: `AuthController`

### 1.1 用户登录

**接口地址**: `POST /api/v1/auth/login`

**业务说明**: 用户使用用户名和密码登录系统，成功后返回访问令牌和刷新令牌

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码（加密传输） |
| captcha | String | 是 | 验证码 |
| captchaKey | String | 是 | 验证码标识 |

**请求示例**:
```json
{
  "username": "admin",
  "password": "******",
  "captcha": "1234",
  "captchaKey": "uuid-key"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 7200,
    "user": {
      "id": 1,
      "username": "admin",
      "realName": "管理员",
      "orgId": 1,
      "roles": ["ROLE_ADMIN"]
    }
  }
}
```

---

### 1.2 刷新令牌

**接口地址**: `POST /api/v1/auth/refresh`

**业务说明**: 使用刷新令牌获取新的访问令牌，用于延长用户会话

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| refreshToken | String | 是 | 刷新令牌 |

**请求示例**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 7200
  }
}
```

---

### 1.3 用户登出

**接口地址**: `POST /api/v1/auth/logout`

**业务说明**: 用户主动登出，服务端使令牌失效

**请求头**:
```
Authorization: Bearer {accessToken}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "登出成功"
}
```

---

### 1.4 用户注册

**接口地址**: `POST /api/v1/auth/register`

**业务说明**: 新用户注册账号（可能需要管理员审批）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名（唯一） |
| password | String | 是 | 密码 |
| realName | String | 是 | 真实姓名 |
| email | String | 是 | 邮箱 |
| phone | String | 否 | 手机号 |
| orgId | Long | 是 | 所属组织ID |

**请求示例**:
```json
{
  "username": "zhangsan",
  "password": "******",
  "realName": "张三",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "orgId": 10
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "注册成功，等待管理员审核",
  "data": {
    "userId": 100,
    "status": "PENDING"
  }
}
```

---

### 1.5 获取当前用户信息

**接口地址**: `GET /api/v1/auth/userinfo`

**业务说明**: 获取当前登录用户的详细信息

**请求头**:
```
Authorization: Bearer {accessToken}
```

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "管理员",
    "email": "admin@example.com",
    "phone": "13800138000",
    "orgId": 1,
    "orgName": "总部",
    "roles": [
      {
        "id": 1,
        "code": "ROLE_ADMIN",
        "name": "系统管理员"
      }
    ],
    "permissions": [
      "indicator:manage",
      "plan:approve"
    ]
  }
}
```

**⚠️ Token验证说明（重要）**：

**本接口也用于验证Token有效性**。当前端需要验证token是否过期时，可直接调用此接口：

- **Token有效**：返回200和用户信息
- **Token无效**：返回401，前端需清除token并跳转登录页

**前端使用示例**：
```javascript
// 在路由守卫中验证token
async validateToken() {
  try {
    const response = await axios.get('/api/v1/auth/userinfo')
    return response.data
  } catch (error) {
    if (error.response?.status === 401) {
      // Token失效，清除本地存储
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      // 跳转登录页
      window.location.href = '/login'
    }
    throw error
  }
}
```

**为什么不需要单独的 `/api/v1/auth/validate` 接口？**

1. **复用现有接口**：`/api/v1/auth/userinfo` 已经可以验证token有效性，返回200即token有效
2. **一次调用两用**：既验证了token，又获取了用户信息，减少网络请求
3. **符合RESTful设计**：获取用户信息本身就是验证token的最佳方式

**Token失效的判断条件**：

| 状态码 | 说明 | 前端处理 |
|--------|------|---------|
| 200 | Token有效 | 正常访问 |
| 401 | Token过期或无效 | 清除token，跳转登录页 |
| 403 | Token有效但无权限 | 显示权限不足提示 |

**完整的Token失效处理流程**：

1. **axios拦截器捕获401** → 2. **尝试刷新token** → 3. **刷新失败** → 4. **清除本地状态** → 5. **跳转登录页**

（详见附录B：前端开发指南）

---

## 2. 指标管理

**业务场景**: 指标的增删改查、按任务/组织/角色检索、下发、撤回、审批

**Controller**: `IndicatorController`

> **注意**: 指标表字段已优化（2026-03），删除了冗余字段如 `level`, `year`, `code`, `value`, `unit`, `targetValue` 等，状态值只保留 `DRAFT`, `PENDING`, `DISTRIBUTED`。

### 2.1 查询指标列表

**接口地址**: `GET /api/v1/indicators`

**业务说明**: 分页查询指标列表，支持多条件筛选

**请求参数** (Query Parameters):

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码，默认0 |
| size | Integer | 否 | 每页数量，默认20 |
| sort | String | 否 | 排序字段，如createdAt |
| order | String | 否 | 排序方向，asc/desc |
| indicatorDesc | String | 否 | 指标描述（模糊查询） |
| type | String | 否 | 指标类型：QUALITATIVE/QUANTITATIVE |
| status | String | 否 | 状态：DRAFT/PENDING/DISTRIBUTED |
| taskId | Long | 否 | 所属任务ID |
| ownerOrgId | Long | 否 | 责任组织ID |
| targetOrgId | Long | 否 | 目标组织ID |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 1,
        "indicatorDesc": "年度学生满意度",
        "type": "QUANTITATIVE",
        "progress": 85,
        "status": "DISTRIBUTED",
        "taskId": 10,
        "taskName": "教学质量提升",
        "ownerOrgId": 1,
        "ownerOrgName": "教务处",
        "targetOrgId": 5,
        "targetOrgName": "各学院",
        "responsibleUserId": 145,
        "weightPercent": 0.3,
        "sortOrder": 1,
        "createdAt": "2024-01-15T10:00:00",
        "updatedAt": "2024-01-15T10:00:00"
      }
    ],
    "totalElements": 156,
    "totalPages": 8,
    "number": 0,
    "size": 20
  }
}
```

---

### 2.2 查询指标详情

**接口地址**: `GET /api/v1/indicators/{id}`

**业务说明**: 查询单个指标的完整信息，包括子指标、历史记录等

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 指标ID |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "indicatorDesc": "年度学生满意度",
    "type": "QUANTITATIVE",
    "progress": 85,
    "status": "DISTRIBUTED",
    "weightPercent": 0.3,
    "sortOrder": 1,
    "remark": "全校学生满意度调查指标",
    "taskId": 10,
    "taskName": "教学质量提升",
    "ownerOrgId": 1,
    "ownerOrgName": "教务处",
    "targetOrgId": 5,
    "targetOrgName": "各学院",
    "responsibleUserId": 145,
    "parentIndicatorId": null,
    "childIndicators": [
      {
        "id": 11,
        "indicatorDesc": "本科生满意度",
        "progress": 90
      }
    ],
    "milestones": [
      {
        "id": 101,
        "month": 1,
        "targetProgress": 10,
        "actualProgress": 8
      }
    ],
    "createdAt": "2024-01-15T10:00:00",
    "updatedAt": "2024-01-20T14:30:00"
  }
}
```

---

### 2.3 创建指标

**接口地址**: `POST /api/v1/indicators`

**业务说明**: 创建新的绩效指标。如果需要创建“子指标”，请在请求中附带 `parentIndicatorId` 来指定其来源的“父指标”。

**请求参数**:

```json
{
  "indicatorDesc": "年度科研经费",
  "type": "QUANTITATIVE",
  "progress": 0,
  "weightPercent": 25,
  "sortOrder": 1,
  "remark": "年度科研经费到账总额",
  "taskId": 11,
  "ownerOrgId": 1,
  "targetOrgId": 5,
  "responsibleUserId": 145,
  "parentIndicatorId": null
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| indicatorDesc | String | 是 | 指标描述/名称 |
| type | String | 是 | 类型：QUALITATIVE（定性）/QUANTITATIVE（定量） |
| progress | Integer | 否 | 进度（0-100），默认0 |
| weightPercent | BigDecimal | 否 | 权重百分比（0-100） |
| sortOrder | Integer | 否 | 排序顺序，默认0 |
| remark | String | 否 | 备注 |
| taskId | Long | 是 | 所属任务ID |
| ownerOrgId | Long | 是 | 责任组织ID |
| targetOrgId | Long | 是 | 目标组织ID |
| responsibleUserId | Long | 否 | 负责人用户ID，关联sys_user表 |
| parentIndicatorId | Long | 否 | 父指标ID，用于创建子指标 |

**响应示例**:
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 2,
    "indicatorDesc": "年度科研经费",
    "status": "DRAFT",
    "createdAt": "2024-01-21T10:00:00"
  }
}
```

---

### 2.4 更新指标

**接口地址**: `PUT /api/v1/indicators/{id}`

**业务说明**: 更新指标信息，草稿状态可修改所有字段，已发布状态只能修改部分字段

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 指标ID |

**请求参数**: （同创建指标）

**响应示例**:
```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "id": 2,
    "updatedAt": "2024-01-21T11:00:00"
  }
}
```

---

### 2.5 删除指标

**接口地址**: `DELETE /api/v1/indicators/{id}`

**业务说明**: 删除指标，只能删除草稿状态的指标

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 指标ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "删除成功"
}
```

---

### 2.6 下发指标

**接口地址**: `POST /api/v1/indicators/{id}/distribute`

**业务说明**: 将指标下发到下级组织

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 指标ID |

**请求参数**:

```json
{
  "targetOrgIds": [5, 6, 7],
  "message": "请在月底前完成填报",
  "deadline": "2024-01-31"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "下发成功",
  "data": {
    "distributedCount": 3,
    "failedOrgs": []
  }
}
```

---

### 2.7 批量下发指标

**接口地址**: `POST /api/v1/indicators/{id}/distribute/batch`

**业务说明**: 批量下发多个指标到指定组织

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 指标ID |

**请求参数**:

```json
{
  "indicatorIds": [1, 2, 3],
  "targetOrgIds": [5, 6],
  "deadline": "2024-01-31"
}
```

---

### 2.8 撤回指标

**接口地址**: `POST /api/v1/indicators/{id}/withdraw`

**业务说明**: 撤回已下发的指标

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 指标ID |

**请求参数**:

```json
{
  "reason": "指标需要调整"
}
```

---

### 2.9 提交指标审批

**接口地址**: `POST /api/v1/indicators/{id}/submit-approval`

**业务说明**: 将草稿状态的指标提交到审批工作流引擎进行审批

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 指标ID |

**请求参数**:

```json
{
  "comment": "已完成数据核对，请审批",
  "flowCode": "INDICATOR_APPROVAL"
}
```

**说明**：
- 调用审批工作流引擎创建审批实例
- flowCode 指定使用的审批流程
- 返回审批实例ID

**响应示例**:
```json
{
  "code": 200,
  "message": "已提交审批",
  "data": {
    "approvalInstanceId": 5001,
    "currentStep": "部门审核",
    "status": "PENDING"
  }
}
```

---

### 2.10 按任务查询指标

**接口地址**: `GET /api/v1/indicators/task/{taskId}`

**业务说明**: 查询某个战略任务下的所有指标

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | Long | 是 | 任务ID |

---

### 2.11 搜索指标

**接口地址**: `GET /api/v1/indicators/search`

**业务说明**: 按关键字搜索指标，支持模糊匹配指标描述

**请求参数** (Query Parameters):

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | String | 是 | 搜索关键字，用于模糊匹配指标描述 |
| page | Integer | 否 | 页码，默认0 |
| size | Integer | 否 | 每页数量，默认20 |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 1,
        "indicatorDesc": "年度学生满意度",
        "type": "QUANTITATIVE",
        "progress": 85,
        "status": "DISTRIBUTED"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

---

### 2.12 查询指标下发状态

**接口地址**: `GET /api/v1/indicators/{id}/distribution-status`

**业务说明**: 查询指标的下发状态、接收情况、以及下发历史

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 指标ID |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "status": "DISTRIBUTED",
    "distributedAt": "2024-01-15T10:00:00",
    "distributedBy": 145,
    "receivedBy": [
      {
        "orgId": 5,
        "orgName": "计算机学院",
        "receivedAt": "2024-01-15T10:30:00"
      }
    ],
    "withdrawn": false
  }
}
```

---

### 2.13 查询指标分发记录

**接口地址**: `GET /api/v1/indicators/{id}/distribution-records`

**业务说明**: 查询指标的分发记录历史，包括下发给哪些组织、接收状态等

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 指标ID |

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "indicatorId": 10,
      "targetOrgId": 5,
      "targetOrgName": "计算机学院",
      "status": "RECEIVED",
      "distributedAt": "2024-01-15T10:00:00",
      "distributedBy": 145,
      "receivedAt": "2024-01-15T10:30:00",
      "deadline": "2024-12-31"
    },
    {
      "id": 2,
      "indicatorId": 10,
      "targetOrgId": 6,
      "targetOrgName": "软件学院",
      "status": "PENDING_RECEIVE",
      "distributedAt": "2024-01-15T10:00:00",
      "distributedBy": 145,
      "deadline": "2024-12-31"
    }
  ]
}
```

**分发状态说明**:

| 状态 | 说明 |
|------|------|
| PENDING_RECEIVE | 待接收 |
| RECEIVED | 已接收 |
| DECOMPOSED | 已分解 |
| COMPLETED | 已完成 |
| WITHDRAWN | 已撤回 |

---

### 2.14 变更指标下发状态（待实现）

**接口地址**: `PATCH /api/v1/indicators/{id}/distribution-status`

**业务说明**: 直接变更指标的下发状态（仅管理员权限）

**注意**: 此接口暂未实现，需评估业务需求后决定是否开发

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 指标ID |

**请求参数**:
```json
{
  "status": "DISTRIBUTED",
  "reason": "直接变更状态说明"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | String | 是 | 目标状态：DRAFT/PENDING/DISTRIBUTED |
| reason | String | 否 | 变更状态的原因 |

---

### 2.15-2.25 其他查询接口

包括：按责任组织查询、按目标组织查询、按层级查询、筛选、定性/定量查询、按角色上下文查询、待填报查询、下发资格查询、工作流上下文查询等。

---

## 2.A 考核周期管理

**业务场景**: 考核周期的创建和查询，用于任务创建时选择周期

**Controller**: `CycleController`

### 2.A.1 获取周期列表

**接口地址**: `GET /api/v1/cycles`

**业务说明**: 获取所有考核周期列表

**请求参数** (Query Parameters):

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码，默认0 |
| size | Integer | 否 | 每页数量，默认20 |
| status | String | 否 | 状态：ACTIVE/INACTIVE |
| year | Integer | 否 | 年份筛选 |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "2026年度考核周期",
        "year": 2026,
        "startDate": "2026-01-01",
        "endDate": "2026-12-31",
        "status": "ACTIVE"
      }
    ],
    "totalElements": 2
  }
}
```

---

### 2.A.2 创建周期

**接口地址**: `POST /api/v1/cycles`

**业务说明**: 创建新的考核周期（管理员权限）

**请求参数**:

```json
{
  "name": "2027年度考核周期",
  "year": 2027,
  "startDate": "2027-01-01",
  "endDate": "2027-12-31"
}
```

**响应示例**:
```json
{
  "code": 201,
  "message": "创建成功",
  "data": {
    "id": 3,
    "name": "2027年度考核周期",
    "year": 2027,
    "status": "ACTIVE"
  }
}
```

---

## 2.B 里程碑管理

**业务场景**: 为指标设置月度里程碑目标

**Controller**: `MilestoneController`

### 2.B.1 批量创建里程碑

**接口地址**: `POST /api/v1/indicators/{id}/milestones`

**业务说明**: 为指标一次性创建多个月份的里程碑

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 指标ID |

**请求参数**:

```json
{
  "milestones": [
    {
      "month": 1,
      "targetProgress": 10,
      "deadline": "2026-01-31"
    },
    {
      "month": 2,
      "targetProgress": 20,
      "deadline": "2026-02-28"
    }
  ]
}
```

**响应示例**:
```json
{
  "code": 201,
  "message": "批量创建成功",
  "data": {
    "createdCount": 2,
    "milestones": [
      {
        "id": 101,
        "month": 1,
        "targetProgress": 10
      }
    ]
  }
}
```

---

### 2.B.2 更新里程碑

**接口地址**: `PUT /api/v1/milestones/{milestoneId}`

**业务说明**: 更新已创建的里程碑

**请求参数**:

```json
{
  "targetProgress": 15,
  "deadline": "2026-01-31"
}
```

---

### 2.B.3 删除里程碑

**接口地址**: `DELETE /api/v1/milestones/{milestoneId}`

**业务说明**: 删除错误的里程碑

**响应示例**:
```json
{
  "code": 204,
  "message": "删除成功"
}
```

---

### 2.B.4 检查里程碑配对状态

**接口地址**: `GET /api/v1/milestones/{milestoneId}/is-paired`

**业务说明**: 检查指定里程碑是否已与填报数据配对

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| milestoneId | Long | 是 | 里程碑ID |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "milestoneId": 101,
    "isPaired": true,
    "message": "里程碑已配对"
  }
}
```

---

## 3. 任务管理

**业务场景**: 战略任务的增删改查、查询战略级任务

**Controller**: `TaskController`

### 3.1 查询任务列表

**接口地址**: `GET /api/v1/tasks`

**业务说明**: 分页查询战略任务列表

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |
| name | String | 否 | 任务名称（模糊） |
| status | String | 否 | 状态 |
| strategic | Boolean | 否 | 是否战略级任务 |
| year | Integer | 否 | 年度 |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 10,
        "name": "教学质量提升",
        "code": "TASK-2024-001",
        "description": "全面提升教学质量指标",
        "status": "ACTIVE",
        "strategic": true,
        "year": 2024,
        "weight": 0.4,
        "startDate": "2024-01-01",
        "endDate": "2024-12-31",
        "ownerOrgId": 1,
        "ownerOrgName": "教务处",
        "indicatorCount": 5,
        "createdAt": "2024-01-01T00:00:00"
      }
    ],
    "totalElements": 25
  }
}
```

---

### 3.2-3.6 其他任务管理接口

包括：查询任务详情、创建任务、更新任务、删除任务、查询战略级任务等。

---

## 4. 计划管理

**业务场景**: 计划的增删改查、按周期/目标组织检索、提交审批

**Controller**: `PlanController`

### 4.1 查询计划列表

**接口地址**: `GET /api/v1/plans`

**业务说明**: 分页查询计划列表

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |
| name | String | 否 | 计划名称 |
| cycleId | Long | 否 | 周期ID |
| targetOrgId | Long | 否 | 目标组织ID |
| status | String | 否 | 状态 |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 5,
        "name": "2024年度计算机学院绩效计划",
        "cycleId": 1,
        "cycleName": "2024年度",
        "targetOrgId": 5,
        "targetOrgName": "计算机学院",
        "status": "PENDING",
        "indicatorCount": 10,
        "totalWeight": 1.0,
        "submittedAt": null,
        "approvedAt": null
      }
    ],
    "totalElements": 50
  }
}
```

---

### 4.2 提交计划审批

**接口地址**: `POST /api/v1/plans/{id}/submit-approval`

**业务说明**: 提交计划进入审批工作流

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 计划ID |

**请求参数**:

```json
{
  "comment": "已完成核对，请审批",
  "flowCode": "PLAN_APPROVAL"
}
```

**说明**：
- 调用审批工作流引擎创建审批实例
- flowCode 指定使用计划审批流程

**响应示例**:
```json
{
  "code": 200,
  "message": "已提交审批",
  "data": {
    "approvalInstanceId": 5002,
    "currentStep": "部门审核",
    "status": "PENDING"
  }
}
```

---

### 4.3-4.10 其他计划管理接口

包括：查询计划详情、创建计划、更新计划、删除计划、按周期查询、按目标组织查询、查询待审批数量等。

---

## 5. 审批工作流引擎

**定位**：提供通用的审批能力，不绑定具体业务

**职责**：
- 审批流定义（流程配置、步骤管理）
- 审批实例管理（发起、待办、通过、驳回）
- 审批历史记录
- 审批统计

**特点**：通用化、可复用、被业务工作流调用

**Controller**: `ApprovalWorkflowController`

### 5.1 审批流定义

#### 5.1.1 查询审批流列表

**接口地址**: `GET /api/v1/approval/flows`

**业务说明**: 分页查询审批流定义

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |
| name | String | 否 | 流程名称 |
| enabled | Boolean | 否 | 是否启用 |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "指标审批流程",
        "code": "INDICATOR_APPROVAL",
        "description": "指标发布审批流程",
        "entityType": "INDICATOR",
        "enabled": true,
        "version": 1,
        "stepCount": 3,
        "createdAt": "2024-01-01T00:00:00"
      }
    ],
    "totalElements": 15
  }
}
```

---

#### 5.1.2 查询审批流详情

**接口地址**: `GET /api/v1/approval/flows/{id}`

**业务说明**: 查询审批流定义详情及步骤配置

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 审批流ID |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "name": "指标审批流程",
    "code": "INDICATOR_APPROVAL",
    "description": "指标发布审批流程",
    "entityType": "INDICATOR",
    "enabled": true,
    "version": 1,
    "steps": [
      {
        "stepOrder": 1,
        "stepCode": "DEPT_REVIEW",
        "stepName": "部门审核",
        "approverType": "ROLE",
        "approvers": ["ROLE_DEPT_ADMIN"],
        "autoApprove": false,
        "timeoutDays": 3
      },
      {
        "stepOrder": 2,
        "stepCode": "SCHOOL_REVIEW",
        "stepName": "校级审批",
        "approverType": "ROLE",
        "approvers": ["ROLE_SCHOOL_ADMIN"],
        "autoApprove": false,
        "timeoutDays": 5
      }
    ]
  }
}
```

---

#### 5.1.3 按编码查询审批流

**接口地址**: `GET /api/v1/approval/flows/code/{flowCode}`

**业务说明**: 根据流程编码查询审批流

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| flowCode | String | 是 | 流程编码，如 INDICATOR_APPROVAL |

---

#### 5.1.4 按实体类型查询审批流

**接口地址**: `GET /api/v1/approval/flows/entity-type/{entityType}`

**业务说明**: 查询适用于特定实体类型的审批流

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| entityType | String | 是 | 实体类型，如 INDICATOR/PLAN/REPORT |

---

#### 5.1.5 创建审批流

**接口地址**: `POST /api/v1/approval/flows`

**业务说明**: 创建新的审批流定义

**请求参数**:

```json
{
  "name": "报告审批流程",
  "code": "REPORT_APPROVAL",
  "entityType": "REPORT",
  "description": "报告发布审批流程",
  "enabled": true,
  "steps": [
    {
      "stepOrder": 1,
      "stepCode": "DEPT_REVIEW",
      "stepName": "部门审核",
      "approverType": "ROLE",
      "approvers": ["ROLE_DEPT_ADMIN"],
      "autoApprove": false,
      "timeoutDays": 3
    },
    {
      "stepOrder": 2,
      "stepCode": "SCHOOL_REVIEW",
      "stepName": "校级审批",
      "approverType": "ROLE",
      "approvers": ["ROLE_SCHOOL_ADMIN"],
      "autoApprove": false,
      "timeoutDays": 5
    }
  ]
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 流程名称 |
| code | String | 是 | 流程编码（唯一） |
| entityType | String | 是 | 实体类型 |
| description | String | 否 | 流程描述 |
| enabled | Boolean | 是 | 是否启用 |
| steps | List | 是 | 审批步骤列表 |

**审批步骤参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| stepOrder | Integer | 是 | 步骤顺序 |
| stepCode | String | 是 | 步骤编码 |
| stepName | String | 是 | 步骤名称 |
| approverType | String | 是 | 审批人类型：ROLE/USER/DEPT_LEADER/DYNAMIC |
| approvers | List<String> | 是 | 审批人列表（角色代码/用户ID/部门ID） |
| autoApprove | Boolean | 否 | 是否自动审批 |
| timeoutDays | Integer | 否 | 超时天数 |

**响应示例**:
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 2,
    "code": "REPORT_APPROVAL",
    "version": 1
  }
}
```

---

#### 5.1.6 更新审批流

**接口地址**: `PUT /api/v1/approval/flows/{id}`

**业务说明**: 更新审批流定义

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 审批流ID |

**请求参数**: （同创建审批流）

---

#### 5.1.7 删除审批流

**接口地址**: `DELETE /api/v1/approval/flows/{id}`

**业务说明**: 删除审批流定义（需确保没有运行中的审批实例）

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 审批流ID |

---

#### 5.1.8 启用/禁用审批流

**接口地址**: `PATCH /api/v1/approval/flows/{id}/status`

**业务说明**: 修改审批流启用状态

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 审批流ID |

**请求参数**:

```json
{
  "enabled": false
}
```

---

### 5.2 审批实例管理

#### 5.2.1 创建审批实例

**接口地址**: `POST /api/v1/approval/instances`

**业务说明**: 创建审批实例（由业务工作流调用）

**请求参数**:

```json
{
  "flowCode": "INDICATOR_APPROVAL",
  "entityType": "INDICATOR",
  "entityId": "1",
  "entityTitle": "年度学生满意度指标",
  "applicantId": 5,
  "comment": "已完成数据核对，请审批",
  "businessData": {
    "indicatorName": "年度学生满意度",
    "targetValue": 90,
    "currentValue": 85
  }
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| flowCode | String | 是 | 审批流编码 |
| entityType | String | 是 | 业务实体类型 |
| entityId | String | 是 | 业务实体ID |
| entityTitle | String | 是 | 业务实体标题 |
| applicantId | Long | 是 | 申请人ID |
| comment | String | 否 | 申请说明 |
| businessData | Object | 否 | 业务数据（用于审批时参考） |

**响应示例**:
```json
{
  "code": 200,
  "message": "审批实例创建成功",
  "data": {
    "instanceId": 5001,
    "instanceNo": "APR-20240121-0001",
    "currentStep": {
      "stepOrder": 1,
      "stepName": "部门审核",
      "stepCode": "DEPT_REVIEW"
    },
    "status": "PENDING",
    "createdAt": "2024-01-21T10:00:00"
  }
}
```

---

#### 5.2.2 查询审批实例列表

**接口地址**: `GET /api/v1/approval/instances`

**业务说明**: 分页查询审批实例

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |
| instanceNo | String | 否 | 实例编号 |
| entityType | String | 否 | 实体类型 |
| entityId | String | 否 | 实体ID |
| status | String | 否 | 状态：PENDING/APPROVED/REJECTED/CANCELLED |
| applicantId | Long | 否 | 申请人ID |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "instanceId": 5001,
        "instanceNo": "APR-20240121-0001",
        "flowName": "指标审批流程",
        "entityType": "INDICATOR",
        "entityId": "1",
        "entityTitle": "年度学生满意度指标",
        "currentStep": "部门审核",
        "status": "PENDING",
        "applicant": {
          "id": 5,
          "name": "张三"
        },
        "createdAt": "2024-01-21T10:00:00"
      }
    ],
    "totalElements": 89
  }
}
```

---

#### 5.2.3 查询审批实例详情

**接口地址**: `GET /api/v1/approval/instances/{instanceId}`

**业务说明**: 查询审批实例详情及审批历史

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| instanceId | Long | 是 | 审批实例ID |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "instanceId": 5001,
    "instanceNo": "APR-20240121-0001",
    "flowName": "指标审批流程",
    "entityType": "INDICATOR",
    "entityId": "1",
    "entityTitle": "年度学生满意度指标",
    "status": "PENDING",
    "currentStep": {
      "stepOrder": 1,
      "stepCode": "DEPT_REVIEW",
      "stepName": "部门审核"
    },
    "applicant": {
      "id": 5,
      "name": "张三",
      "orgName": "计算机学院"
    },
    "businessData": {
      "indicatorName": "年度学生满意度",
      "targetValue": 90,
      "currentValue": 85
    },
    "steps": [
      {
        "stepOrder": 1,
        "stepCode": "DEPT_REVIEW",
        "stepName": "部门审核",
        "status": "PENDING",
        "approvers": [
          {
            "id": 10,
            "name": "李主任",
            "approved": false
          }
        ]
      },
      {
        "stepOrder": 2,
        "stepCode": "SCHOOL_REVIEW",
        "stepName": "校级审批",
        "status": "PENDING"
      }
    ],
    "history": [
      {
        "action": "SUBMIT",
        "operator": {
          "id": 5,
          "name": "张三"
        },
        "comment": "已完成数据核对，请审批",
        "operatedAt": "2024-01-21T10:00:00"
      }
    ],
    "createdAt": "2024-01-21T10:00:00"
  }
}
```

---

#### 5.2.4 查询我的待办审批

**接口地址**: `GET /api/v1/approval/instances/my-pending`

**业务说明**: 查询当前用户的待办审批列表

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "instanceId": 5001,
        "instanceNo": "APR-20240121-0001",
        "entityType": "INDICATOR",
        "entityTitle": "年度学生满意度指标",
        "currentStep": "部门审核",
        "applicant": "张三",
        "submittedAt": "2024-01-21T10:00:00",
        "pendingDays": 2
      }
    ],
    "totalElements": 15,
    "unreadCount": 3
  }
}
```

---

#### 5.2.5 查询我的已办审批

**接口地址**: `GET /api/v1/approval/instances/my-approved`

**业务说明**: 查询当前用户已审批的列表

---

#### 5.2.6 查询我发起的审批

**接口地址**: `GET /api/v1/approval/instances/my-applied`

**业务说明**: 查询当前用户发起的审批列表

---

#### 5.2.7 审批通过

**接口地址**: `POST /api/v1/approval/instances/{instanceId}/approve`

**业务说明**: 审批通过当前实例

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| instanceId | Long | 是 | 审批实例ID |

**请求参数**:

```json
{
  "comment": "数据准确，同意通过",
  "action": "APPROVE"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| comment | String | 否 | 审批意见 |
| action | String | 是 | 操作：APPROVE（通过）/REJECT（驳回）/CANCEL（撤回） |

**响应示例**:
```json
{
  "code": 200,
  "message": "审批通过",
  "data": {
    "instanceId": 5001,
    "status": "APPROVED",
    "nextStep": null,
    "completedAt": "2024-01-21T11:00:00"
  }
}
```

---

#### 5.2.8 审批驳回

**接口地址**: `POST /api/v1/approval/instances/{instanceId}/reject`

**业务说明**: 驳回审批实例

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| instanceId | Long | 是 | 审批实例ID |

**请求参数**:

```json
{
  "comment": "数据需要核实，请重新提交",
  "rejectTo": "APPLICATION",
  "requireResubmit": true
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| comment | String | 是 | 驳回原因 |
| rejectTo | String | 是 | 驳回到：APPLICATION（申请人）/PREVIOUS_STEP（上一步）/SPECIFIC_STEP（指定步骤） |
| requireResubmit | Boolean | 否 | 是否需要重新提交 |
| specificStepCode | String | 否 | 指定驳回到的步骤编码（rejectTo=SPECIFIC_STEP 时必填） |

**响应示例**:
```json
{
  "code": 200,
  "message": "已驳回",
  "data": {
    "instanceId": 5001,
    "status": "REJECTED",
    "currentStep": "APPLICATION"
  }
}
```

---

#### 5.2.9 撤回审批

**接口地址**: `POST /api/v1/approval/instances/{instanceId}/cancel`

**业务说明**: 申请人撤回审批实例（仅在当前步骤未审批时可用）

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| instanceId | Long | 是 | 审批实例ID |

**请求参数**:

```json
{
  "reason": "需要补充材料"
}
```

---

#### 5.2.10 转办审批

**接口地址**: `POST /api/v1/approval/instances/{instanceId}/transfer`

**业务说明**: 将审批任务转办给其他人

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| instanceId | Long | 是 | 审批实例ID |

**请求参数**:

```json
{
  "transfereeId": 15,
  "reason": "委托审批"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| transfereeId | Long | 是 | 被转办人ID |
| reason | String | 否 | 转办原因 |

---

#### 5.2.11 加签

**接口地址**: `POST /api/v1/approval/instances/{instanceId}/add-approver`

**业务说明**: 在当前步骤添加额外审批人

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| instanceId | Long | 是 | 审批实例ID |

**请求参数**:

```json
{
  "approverIds": [16, 17],
  "type": "BEFORE",
  "reason": "需要额外审核"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| approverIds | List<Long> | 是 | 审批人ID列表 |
| type | String | 是 | 类型：BEFORE（前加签）/AFTER（后加签） |
| reason | String | 否 | 加签原因 |

---

### 5.3 审批历史与统计

#### 5.3.1 查询审批历史

**接口地址**: `GET /api/v1/approval/instances/{instanceId}/history`

**业务说明**: 查询审批实例的完整操作历史

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| instanceId | Long | 是 | 审批实例ID |

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "historyId": 10001,
      "action": "SUBMIT",
      "operator": {
        "id": 5,
        "name": "张三"
      },
      "stepName": "提交申请",
      "comment": "已完成数据核对",
      "operatedAt": "2024-01-21T10:00:00"
    },
    {
      "historyId": 10002,
      "action": "APPROVE",
      "operator": {
        "id": 10,
        "name": "李主任"
      },
      "stepName": "部门审核",
      "comment": "同意",
      "operatedAt": "2024-01-21T14:00:00"
    },
    {
      "historyId": 10003,
      "action": "APPROVE",
      "operator": {
        "id": 20,
        "name": "王校长"
      },
      "stepName": "校级审批",
      "comment": "批准",
      "operatedAt": "2024-01-22T09:00:00"
    }
  ]
}
```

---

#### 5.3.2 审批统计

**接口地址**: `GET /api/v1/approval/statistics`

**业务说明**: 获取审批统计数据

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| dateFrom | LocalDate | 否 | 统计开始日期 |
| dateTo | LocalDate | 否 | 统计结束日期 |
| entityType | String | 否 | 实体类型过滤 |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "totalInstances": 500,
    "pendingInstances": 45,
    "approvedInstances": 420,
    "rejectedInstances": 25,
    "cancelledInstances": 10,
    "avgApprovalTime": "2.3天",
    "byEntityType": [
      {
        "entityType": "INDICATOR",
        "count": 200,
        "percentage": 40
      },
      {
        "entityType": "PLAN",
        "count": 150,
        "percentage": 30
      },
      {
        "entityType": "REPORT",
        "count": 150,
        "percentage": 30
      }
    ],
    "byStep": [
      {
        "stepName": "部门审核",
        "avgDuration": "1.2天",
        "count": 450
      },
      {
        "stepName": "校级审批",
        "avgDuration": "1.1天",
        "count": 420
      }
    ],
    "byApprover": [
      {
        "approverId": 10,
        "approverName": "李主任",
        "pendingCount": 8,
        "approvedCount": 120,
        "rejectedCount": 5
      }
    ]
  }
}
```

---

#### 5.3.3 审批耗时统计

**接口地址**: `GET /api/v1/approval/statistics/duration`

**业务说明**: 统计各审批环节的平均耗时

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "totalAvgDuration": "2.5天",
    "byFlow": [
      {
        "flowCode": "INDICATOR_APPROVAL",
        "flowName": "指标审批",
        "avgDuration": "2.3天",
        "count": 200
      },
      {
        "flowCode": "PLAN_APPROVAL",
        "flowName": "计划审批",
        "avgDuration": "2.8天",
        "count": 150
      }
    ],
    "byStep": [
      {
        "stepCode": "DEPT_REVIEW",
        "stepName": "部门审核",
        "avgDuration": "1.2天",
        "timeoutRate": "5%"
      },
      {
        "stepCode": "SCHOOL_REVIEW",
        "stepName": "校级审批",
        "avgDuration": "1.3天",
        "timeoutRate": "3%"
      }
    ]
  }
}
```

---

#### 5.3.4 待办数量统计

**接口地址**: `GET /api/v1/approval/statistics/pending-count`

**业务说明**: 查询当前用户待办审批数量

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "totalPending": 15,
    "urgentPending": 3,
    "timeoutPending": 1,
    "byEntityType": {
      "INDICATOR": 8,
      "PLAN": 5,
      "REPORT": 2
    }
  }
}
```

---

## 6. 业务工作流

**定位**：具体的业务流程管理

**职责**：
- 指标下发流程：总部→职能部门→学院
- 计划填报流程：填报→提交→审批
- 报告提交流程：起草→提交→多级审批

**特点**：业务相关、会调用审批工作流引擎

**Controller**: `BusinessWorkflowController`

### 6.1 指标下发流程

#### 6.1.1 一键下发指标

**接口地址**: `POST /workflow/indicator/distribute`

**业务说明**: 一键将一级指标下发给二级组织

**请求参数**:

```json
{
  "indicatorIds": [1, 2, 3],
  "targetOrgIds": [5, 6, 7],
  "deadline": "2024-12-31",
  "message": "请在月底前完成分解"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "下发成功",
  "data": {
    "successCount": 3,
    "distributionRecords": [
      {
        "indicatorId": 1,
        "orgId": 5,
        "status": "PENDING_RECEIVE"
      }
    ]
  }
}
```

---

#### 6.1.2 确认接收指标

**接口地址**: `POST /workflow/indicator/{id}/confirm-receive`

**业务说明**: 接收方确认已收到下发的指标

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 指标ID |

**请求参数**:

```json
{
  "comment": "已确认，将尽快分解落实"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "接收成功"
}
```

---

#### 6.1.3 分解指标

**接口地址**: `POST /workflow/indicator/{id}/decompose`

**业务说明**: 将一级指标分解为多个二级子指标

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 父指标ID |

**请求参数**:

```json
{
  "decompositions": [
    {
      "name": "计算机学院学生满意度",
      "targetOrgId": 5,
      "value": 85,
      "weight": 0.4
    },
    {
      "name": "软件学院学生满意度",
      "targetOrgId": 6,
      "value": 90,
      "weight": 0.6
    }
  ]
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "分解成功",
  "data": {
    "parentId": 1,
    "createdCount": 2,
    "childIndicators": [
      {
        "id": 101,
        "name": "计算机学院学生满意度",
        "targetOrgId": 5,
        "value": 85,
        "weight": 0.4
      },
      {
        "id": 102,
        "name": "软件学院学生满意度",
        "targetOrgId":   6,
        "value": 90,
        "weight": 0.6
      }
    ]
  }
}
```

---

#### 6.1.4 提 fanc交指标进度

**接口地址**: `POST /workflow/indicator/{indicatorId}/submit-progress`

**业务说明**: 提交指标完成进度

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| indicatorId | Long | 是 | 指标ID |

**请求参数**:

```json
{
  "value": 88,
  "evidence": "已完成满意度调查",
  "attachments": [201, 202]
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "提交成功",
  "data": {
    "reportId": 100,
    "status": "PENDING_APPROVAL"
  }
}
```

---

#### 6.1.5 查询指标下发状态

**接口地址**: `GET /workflow/indicator/{indicatorId}/distribution-status`

**业务说明**: 查询指标的下发状态和接收情况

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| indicatorId | Long | 是 | 指标ID |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "indicatorId": 1,
    "indicatorName": "年度学生满意度",
    "distributionStatus": "COMPLETED",
    "targetOrgs": [
      {
        "orgId": 5,
        "orgName": "计算机学院",
        "status": "COMPLETED",
        "receivedAt": "2024-01-16T09:00:00",
        "decomposedAt": "2024-01-17T10:00:00",
        "submittedAt": "2024-01-20T15:00:00"
      },
      {
        "orgId": 6,
        "orgName": "软件学院",
        "status": "PENDING_RECEIVE"
      }
    ]
  }
}
```

---

### 6.2 计划填报流程

#### 6.2.1 创建计划

**接口地址**: `POST /workflow/plan/create`

**业务说明**: 创建新的绩效计划

**请求参数**:

```json
{
  "name": "2024年度计算机学院绩效计划",
  "cycleId": 1,
  "targetOrgId": 5,
  "indicatorIds": [20, 21, 22, 23, 24]
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "planId": 10,
    "status": "DRAFT"
  }
}
```

---

#### 6.2.2 填报计划数据

**接口地址**: `POST /workflow/plan/{planId}/fill`

**业务说明**: 填报计划指标数据

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| planId | Long | 是 | 计划ID |

**请求参数**:

```json
{
  "indicatorData": [
    {
      "indicatorId": 20,
      "value": 85,
      "evidence": "教学评估结果"
    },
    {
      "indicatorId": 21,
      "value": 90,
      "evidence": "科研到账统计"
    }
  ]
}
```

---

#### 6.2.3 提交计划审批

**接口地址**: `POST /workflow/plan/{planId}/submit`

**业务说明**: 提交计划进入审批流程（调用审批工作流引擎）

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| planId | Long | 是 | 计划ID |

**请求参数**:

```json
{
  "comment": "已完成填报，请审批"
}
```

**说明**：
- 内部调用审批工作流引擎的 `/api/v1/approval/instances` 接口
- 使用 `PLAN_APPROVAL` 审批流
- 返回审批实例信息

**响应示例**:
```json
{
  "code": 200,
  "message": "已提交审批",
  "data": {
    "planId": 10,
    "approvalInstanceId": 5002,
    "currentStep": "部门审核",
    "status": "PENDING_APPROVAL"
  }
}
```

---

### 6.3 报告提交流程

#### 6.3.1 创建报告

**接口地址**: `POST /workflow/report/create`

**业务说明**: 创建指标完成报告

**请求参数**:

```json
{
  "indicatorId": 1,
  "value": 88,
  "evidence": "已完成满意度调查"
}
```

---

#### 6.3.2 提交报告审批

**接口地址**: `POST /workflow/report/{reportId}/submit`

**业务说明**: 提交报告进入多级审批流程（调用审批工作流引擎）

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| reportId | Long | 是 | 报告ID |

**请求参数**:

```json
{
  "comment": "请审批"
}
```

**说明**：
- 内部调用审批工作流引擎
- 使用 `REPORT_APPROVAL` 审批流

---

### 6.4 工作流看板

#### 6.4.1 查询工作流看板

**接口地址**: `GET /api/v1/analytics/dashboard`

**业务说明**: 获取工作流整体数据看板

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "indicatorDistribution": {
      "pendingDistribute": 10,
      "pendingReceive": 15,
      "pendingDecompose": 8,
      "pendingSubmit": 20,
      "completed": 120
    },
    "planFill": {
      "draft": 5,
      "pendingSubmit": 8,
      "pendingApproval": 12,
      "approved": 50
    },
    "reportSubmit": {
      "draft": 3,
      "pendingSubmit": 5,
      "pendingApproval": 10,
      "approved": 80
    },
    "byOrg": [
      {
        "orgId": 5,
        "orgName": "计算机学院",
        "pendingTasks": 8
      }
    ]
  }
}
```

---

#### 6.4.2 查询待办任务

**接口地址**: `GET /workflow/my-tasks`

**业务说明**: 查询当前用户在工作流中的待办任务

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "pendingReceive": [
      {
        "indicatorId": 2,
        "indicatorName": "年度科研经费",
        "fromOrg": "总部",
        "receivedAt": null
      }
    ],
    "pendingDecompose": [
      {
        "indicatorId": 1,
        "indicatorName": "年度学生满意度"
      }
    ],
    "pendingSubmit": [
      {
        "indicatorId": 11,
        "indicatorName": "计算机学院本科生满意度",
        "deadline": "2024-01-31"
      }
    ],
    "pendingApproval": {
      "approvals": [
        {
          "instanceId": 5001,
          "entityType": "INDICATOR",
          "entityTitle": "年度学生满意度指标",
          "currentStep": "部门审核"
        }
      ]
    }
  }
}
```

---

## 7. 用户管理

**业务场景**: 用户的增删改查、启用/禁用、重置密码、管理员接口

**Controller**: `UserManagementController`, `RoleManagementController`, `UserAnalyticsController`

### 7.1 查询用户列表

**接口地址**: `GET /api/v1/users`

**业务说明**: 分页查询用户列表

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |
| username | String | 否 | 用户名（模糊） |
| realName | String | 否 | 真实姓名（模糊） |
| orgId | Long | 否 | 组织ID |
| status | String | 否 | 状态：ACTIVE/DISABLED |
| roleId | Long | 否 | 角色ID |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 1,
        "username": "admin",
        "realName": "管理员",
        "email": "admin@example.com",
        "phone": "13800138000",
        "orgId": 1,
        "orgName": "总部",
        "status": "ACTIVE",
        "roles": [
          {
            "id": 1,
            "name": "系统管理员",
            "code": "ROLE_ADMIN"
          }
        ],
        "lastLoginAt": "2024-01-21T10:00:00",
        "createdAt": "2024-01-01T00:00:00"
      }
    ],
    "totalElements": 150
  }
}
```

---

### 7.2 查询用户详情

**接口地址**: `GET /api/v1/users/{id}`

**业务说明**: 查询指定用户的详细信息

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "id": 5,
    "username": "zhangsan",
    "realName": "张三",
    "email": "zhangsan@example.com",
    "phone": "13800138000",
    "avatar": "https://example.com/avatar/5.jpg",
    "orgId": 5,
    "orgName": "计算机学院",
    "status": "ACTIVE",
    "roles": [
      {
        "id": 2,
        "code": "ROLE_DEPT_ADMIN",
        "name": "部门管理员",
        "description": "负责部门内绩效管理"
      }
    ],
    "permissions": [
      "indicator:manage",
      "plan:approve",
      "report:view"
    ],
    "lastLoginAt": "2024-01-21T09:30:00",
    "loginCount": 156,
    "createdAt": "2024-01-01T00:00:00",
    "createdBy": {
      "id": 1,
      "name": "管理员"
    }
  }
}
```

---

### 7.3 创建用户

**接口地址**: `POST /api/v1/users`

**业务说明**: 创建新用户（需要管理员权限）

**请求参数**:

```json
{
  "username": "lisi",
  "password": "Password123!",
  "realName": "李四",
  "email": "lisi@example.com",
  "phone": "13800138001",
  "orgId": 5,
  "roleIds": [2, 3],
  "status": "ACTIVE"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名（唯一） |
| password | String | 是 | 初始密码（需符合密码策略） |
| realName | String | 是 | 真实姓名 |
| email | String | 是 | 邮箱（唯一） |
| phone | String | 否 | 手机号 |
| orgId | Long | 是 | 所属组织ID |
| roleIds | List<Long> | 否 | 角色ID列表 |
| status | String | 否 | 状态，默认ACTIVE |

**响应示例**:
```json
{
  "code": 200,
  "message": "用户创建成功",
  "data": {
    "id": 6,
    "username": "lisi",
    "realName": "李四",
    "status": "ACTIVE",
    "createdAt": "2024-01-21T10:00:00"
  }
}
```

---

### 7.4 更新用户

**接口地址**: `PUT /api/v1/users/{id}`

**业务说明**: 更新用户信息

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 用户ID |

**请求参数**:

```json
{
  "realName": "李四丰",
  "email": "lisifeng@example.com",
  "phone": "13900139000",
  "orgId": 6,
  "status": "ACTIVE"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "用户信息更新成功",
  "data": {
    "id": 6,
    "updatedAt": "2024-01-21T11:00:00"
  }
}
```

---

### 7.5 删除用户

**接口地址**: `DELETE /api/v1/users/{id}`

**业务说明**: 删除用户（软删除，保留数据）

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 用户ID |

**请求参数**:

```json
{
  "reason": "用户离职"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "用户删除成功"
}
```

---

### 7.6 启用/禁用用户

**接口地址**: `PUT /api/v1/users/{id}/status`

**业务说明**: 启用或禁用用户账户

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 用户ID |

**请求参数**:

```json
{
  "status": "DISABLED",
  "reason": "临时禁用，等待审核"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | String | 是 | 状态：ACTIVE（启用）/DISABLED（禁用） |
| reason | String | 否 | 操作原因 |

**响应示例**:
```json
{
  "code": 200,
  "message": "用户状态已更新",
  "data": {
    "id": 6,
    "status": "DISABLED",
    "disabledAt": "2024-01-21T10:00:00",
    "disabledBy": {
      "id": 1,
      "name": "管理员"
    }
  }
}
```

---

### 7.7 重置密码（管理员）

**接口地址**: `POST /api/v1/users/{id}/reset-password`

**业务说明**: 管理员为用户重置密码

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 用户ID |

**请求参数**:

```json
{
  "newPassword": "NewPassword123!",
  "forceChangeOnNextLogin": true,
  "notifyUser": true
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| newPassword | String | 是 | 新密码 |
| forceChangeOnNextLogin | Boolean | 否 | 首次登录是否强制修改密码，默认true |
| notifyUser | Boolean | 否 | 是否通知用户，默认true |

**响应示例**:
```json
{
  "code": 200,
  "message": "密码重置成功",
  "data": {
    "userId": 6,
    "resetAt": "2024-01-21T10:00:00",
    "notified": true
  }
}
```

---

### 7.8 修改密码（用户自己）

**接口地址**: `PUT /api/v1/users/me/password`

**业务说明**: 用户自己修改密码

**请求参数**:

```json
{
  "oldPassword": "OldPassword123!",
  "newPassword": "NewPassword456!"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| oldPassword | String | 是 | 原密码 |
| newPassword | String | 是 | 新密码（需符合密码策略） |

**响应示例**:
```json
{
  "code": 200,
  "message": "密码修改成功",
  "data": {
    "changedAt": "2024-01-21T10:00:00"
  }
}
```

---

### 7.9 为用户分配角色

**接口地址**: `POST /api/v1/users/{id}/roles`

**业务说明**: 为用户直接分配角色

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 用户ID |

**请求参数**:

```json
{
  "roleIds": [2, 3, 5],
  "replace": false
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| roleIds | List<Long> | 是 | 角色ID列表 |
| replace | Boolean | 否 | 是否替换原有角色，默认false（追加） |

**响应示例**:
```json
{
  "code": 200,
  "message": "角色分配成功",
  "data": {
    "userId": 6,
    "roles": [
      {"id": 2, "name": "部门管理员"},
      {"id": 3, "name": "指标管理员"},
      {"id": 5, "name": "报告查看员"}
    ]
  }
}
```

---

### 7.10 移除用户角色

**接口地址**: `DELETE /api/v1/users/{id}/roles/{roleId}`

**业务说明**: 移除用户的指定角色

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 用户ID |
| roleId | Long | 是 | 角色ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "角色移除成功"
}
```

---

## 7.1 角色权限管理

**业务场景**: 角色的增删改查、权限分配、角色权限配置

**Controller**: `RoleManagementController`

### 7.1.1 查询角色列表

**接口地址**: `GET /api/v1/roles`

**业务说明**: 分页查询所有角色列表

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |
| name | String | 否 | 角色名称（模糊） |
| code | String | 否 | 角色编码 |
| enabled | Boolean | 否 | 是否启用 |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "系统管理员",
        "code": "ROLE_ADMIN",
        "description": "拥有系统所有权限",
        "enabled": true,
        "isSystem": true,
        "permissionCount": 50,
        "userCount": 3,
        "createdAt": "2024-01-01T00:00:00"
      },
      {
        "id": 2,
        "name": "部门管理员",
        "code": "ROLE_DEPT_ADMIN",
        "description": "负责部门内绩效管理",
        "enabled": true,
        "isSystem": false,
        "permissionCount": 15,
        "userCount": 12,
        "createdAt": "2024-01-01T00:00:00"
      }
    ],
    "totalElements": 8
  }
}
```

---

### 7.1.2 查询角色详情

**接口地址**: `GET /api/v1/roles/{id}`

**业务说明**: 查询指定角色的详细信息及权限列表

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 角色ID |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "id": 2,
    "name": "部门管理员",
    "code": "ROLE_DEPT_ADMIN",
    "description": "负责部门内绩效管理",
    "enabled": true,
    "isSystem": false,
    "permissions": [
      {
        "id": 101,
        "code": "indicator:view",
        "name": "查看指标",
        "module": "指标管理"
      },
      {
        "id": 102,
        "code": "indicator:manage",
        "name": "管理指标",
        "module": "指标管理"
      },
      {
        "id": 201,
        "code": "plan:approve",
        "name": "审批计划",
        "module": "计划管理"
      }
    ],
    "userCount": 12,
    "users": [
      {
        "id": 5,
        "username": "zhangsan",
        "realName": "张三",
        "orgName": "计算机学院"
      }
    ],
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-15T10:00:00"
  }
}
```

---

### 7.1.3 创建角色

**接口地址**: `POST /api/v1/roles`

**业务说明**: 创建新角色

**请求参数**:

```json
{
  "name": "指标管理员",
  "code": "ROLE_INDICATOR_ADMIN",
  "description": "负责指标创建和下发",
  "enabled": true,
  "permissionIds": [101, 102, 103, 104]
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 角色名称 |
| code | String | 是 | 角色编码（唯一，建议使用ROLE_前缀） |
| description | String | 否 | 角色描述 |
| enabled | Boolean | 是 | 是否启用 |
| permissionIds | List<Long> | 否 | 权限ID列表 |

**响应示例**:
```json
{
  "code": 200,
  "message": "角色创建成功",
  "data": {
    "id": 10,
    "name": "指标管理员",
    "code": "ROLE_INDICATOR_ADMIN",
    "enabled": true,
    "createdAt": "2024-01-21T10:00:00"
  }
}
```

---

### 7.1.4 更新角色

**接口地址**: `PUT /api/v1/roles/{id}`

**业务说明**: 更新角色信息

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 角色ID |

**请求参数**:

```json
{
  "name": "高级指标管理员",
  "description": "负责指标创建、下发和审批",
  "enabled": true
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "角色更新成功",
  "data": {
    "id": 10,
    "updatedAt": "2024-01-21T11:00:00"
  }
}
```

---

### 7.1.5 删除角色

**接口地址**: `DELETE /api/v1/roles/{id}`

**业务说明**: 删除角色（不能删除系统角色和正在使用的角色）

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 角色ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "角色删除成功"
}
```

---

### 7.1.6 启用/禁用角色

**接口地址**: `PATCH /api/v1/roles/{id}/status`

**业务说明**: 修改角色启用状态

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 角色ID |

**请求参数**:

```json
{
  "enabled": false
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "角色状态已更新",
  "data": {
    "id": 10,
    "enabled": false,
    "updatedAt": "2024-01-21T10:00:00"
  }
}
```

---

### 7.1.7 查询所有权限点

**接口地址**: `GET /api/v1/permissions`

**业务说明**: 获取系统定义的所有权限点（按模块分组）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| module | String | 否 | 模块名称过滤 |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "modules": [
      {
        "module": "指标管理",
        "permissions": [
          {
            "id": 101,
            "code": "indicator:view",
            "name": "查看指标",
            "description": "查看指标列表和详情"
          },
          {
            "id": 102,
            "code": "indicator:create",
            "name": "创建指标",
            "description": "创建新指标"
          },
          {
            "id": 103,
            "code": "indicator:edit",
            "name": "编辑指标",
            "description": "修改指标信息"
          },
          {
            "id": 104,
            "code": "indicator:delete",
            "name": "删除指标",
            "description": "删除指标"
          },
          {
            "id": 105,
            "code": "indicator:distribute",
            "name": "下发指标",
            "description": "将指标下发给下级组织"
          },
          {
            "id": 106,
            "code": "indicator:approve",
            "name": "审批指标",
            "description": "审批指标发布"
          }
        ]
      },
      {
        "module": "计划管理",
        "permissions": [
          {
            "id": 201,
            "code": "plan:view",
            "name": "查看计划",
            "description": "查看计划列表和详情"
          },
          {
            "id": 202,
            "code": "plan:create",
            "name": "创建计划",
            "description": "创建新计划"
          },
          {
            "id": 203,
            "code": "plan:fill",
            "name": "填报计划",
            "description": "填报计划数据"
          },
          {
            "id": 204,
            "code": "plan:approve",
            "name": "审批计划",
            "description": "审批计划提交"
          }
        ]
      },
      {
        "module": "报告管理",
        "permissions": [
          {
            "id": 301,
            "code": "report:view",
            "name": "查看报告",
            "description": "查看报告列表和详情"
          },
          {
            "id": 302,
            "code": "report:create",
            "name": "创建报告",
            "description": "创建新报告"
          },
          {
            "id": 303,
            "code": "report:export",
            "name": "导出报告",
            "description": "导出报告PDF"
          }
        ]
      },
      {
        "module": "审批管理",
        "permissions": [
          {
            "id": 401,
            "code": "approval:view",
            "name": "查看审批",
            "description": "查看审批列表和详情"
          },
          {
            "id": 402,
            "code": "approval:process",
            "name": "处理审批",
            "description": "审批通过或驳回"
          },
          {
            "id": 403,
            "code": "approval:transfer",
            "name": "转办审批",
            "description": "将审批转办给他人"
          }
        ]
      },
      {
        "module": "系统管理",
        "permissions": [
          {
            "id": 501,
            "code": "system:user:manage",
            "name": "用户管理",
            "description": "管理系统用户"
          },
          {
            "id": 502,
            "code": "system:role:manage",
            "name": "角色管理",
            "description": "管理系统角色"
          },
          {
            "id": 503,
            "code": "system:org:manage",
            "name": "组织管理",
            "description": "管理组织架构"
          },
          {
            "id": 504,
            "code": "system:config:manage",
            "name": "配置管理",
            "description": "管理系统配置"
          }
        ]
      }
    ]
  }
}
```

---

### 7.1.8 为角色分配权限

**接口地址**: `POST /api/v1/roles/{id}/permissions`

**业务说明**: 为指定角色分配权限

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 角色ID |

**请求参数**:

```json
{
  "permissionIds": [101, 102, 103, 104, 201, 301],
  "replace": false
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| permissionIds | List<Long> | 是 | 权限ID列表 |
| replace | Boolean | 否 | 是否替换原有权限，默认false（追加） |

**响应示例**:
```json
{
  "code": 200,
  "message": "权限分配成功",
  "data": {
    "roleId": 2,
    "permissionCount": 6,
    "assignedAt": "2024-01-21T10:00:00"
  }
}
```

---

### 7.1.9 移除角色权限

**接口地址**: `DELETE /api/v1/roles/{id}/permissions/{permissionId}`

**业务说明**: 移除角色的指定权限

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 角色ID |
| permissionId | Long | 是 | 权限ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "权限移除成功"
}
```

---

### 7.1.10 批量为角色分配权限

**接口地址**: `POST /api/v1/roles/batch/permissions`

**业务说明**: 批量为多个角色分配权限

**请求参数**:

```json
{
  "roleIds": [2, 3, 5],
  "permissionIds": [101, 102, 103],
  "replace": false
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "批量权限分配成功",
  "data": {
    "affectedRoleCount": 3,
    "assignedAt": "2024-01-21T10:00:00"
  }
}
```

---

### 7.1.11 查询角色用户列表

**接口地址**: `GET /api/v1/roles/{id}/users`

**业务说明**: 查询拥有指定角色的所有用户

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 角色ID |

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 5,
        "username": "zhangsan",
        "realName": "张三",
        "email": "zhangsan@example.com",
        "orgName": "计算机学院",
        "assignedAt": "2024-01-01T00:00:00"
      },
      {
        "id": 6,
        "username": "lisi",
        "realName": "李四",
        "email": "lisi@example.com",
        "orgName": "软件学院",
        "assignedAt": "2024-01-15T00:00:00"
      }
    ],
    "totalElements": 12
  }
}
```

---

## 7.2 统计分析与导出

**业务场景**: 首页工作台数据、绩效统计分析、数据导出

**Controller**: `UserAnalyticsController`, `AnalyticsDashboardController`

### 7.2.1 获取工作台统计数据

**接口地址**: `GET /api/v1/analytics/dashboard`

**业务说明**: 提供工作台或首页所需的关键统计数据

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orgId | Long | 否 | 组织ID（不传则查询当前用户组织） |
| period | String | 否 | 统计周期：month/quarter/year |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "overview": {
      "totalIndicators": 156,
      "activeIndicators": 120,
      "completedIndicators": 100,
      "completionRate": 83.3,
      "totalPlans": 50,
      "pendingPlans": 8,
      "approvedPlans": 35,
      "totalReports": 89,
      "pendingReports": 12,
      "approvedReports": 77
    },
    "departments": [
      {
        "orgId": 5,
        "orgName": "计算机学院",
        "indicatorCount": 25,
        "completionRate": 88.5,
        "pendingTasks": 3,
        "overdueTasks": 0
      },
      {
        "orgId": 6,
        "orgName": "软件学院",
        "indicatorCount": 22,
        "completionRate": 76.3,
        "pendingTasks": 5,
        "overdueTasks": 1
      },
      {
        "orgId": 7,
        "orgName": "信息学院",
        "indicatorCount": 28,
        "completionRate": 92.1,
        "pendingTasks": 2,
        "overdueTasks": 0
      }
    ],
    "approvals": {
      "pendingCount": 15,
      "urgentCount": 3,
      "timeoutCount": 1,
      "todayProcessed": 8,
      "avgProcessTime": "2.3天"
    },
    "trends": {
      "indicatorCompletion": [
        {"month": "2024-01", "rate": 75},
        {"month": "2024-02", "rate": 82},
        {"month": "2024-03", "rate": 88}
      ],
      "planApproval": [
        {"month": "2024-01", "count": 12},
        {"month": "2024-02", "count": 15},
        {"month": "2024-03", "count": 18}
      ]
    },
    "alerts": [
      {
        "type": "WARNING",
        "message": "软件学院有3个指标即将到期",
        "count": 3
      },
      {
        "type": "ERROR",
        "message": "信息学院有1个审批超时",
        "count": 1
      }
    ]
  }
}
```

---

### 7.2.2 导出指标数据

**接口地址**: `POST /api/v1/analytics/export/indicators`

**业务说明**: 根据筛选条件导出指标数据为Excel或CSV文件

**请求参数**:

```json
{
  "filters": {
    "orgId": 5,
    "status": "PUBLISHED",
    "level": 2,
    "dateFrom": "2024-01-01",
    "dateTo": "2024-12-31"
  },
  "columns": [
    "id",
    "name",
    "code",
    "type",
    "value",
    "targetValue",
    "completionRate",
    "status",
    "ownerOrgName",
    "targetOrgName"
  ],
  "format": "EXCEL",
  "includeDetails": true
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| filters | Object | 否 | 筛选条件 |
| columns | List<String> | 是 | 导出字段列表 |
| format | String | 是 | 导出格式：EXCEL/CSV |
| includeDetails | Boolean | 否 | 是否包含明细数据，默认false |

**响应**: 文件流

**响应头**:
```
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="indicators_export_20240121.xlsx"
```

---

### 7.2.3 导出计划数据

**接口地址**: `POST /api/v1/analytics/export/plans`

**业务说明**: 导出计划数据

**请求参数**:

```json
{
  "filters": {
    "cycleId": 1,
    "targetOrgId": 5,
    "status": "APPROVED"
  },
  "columns": [
    "id",
    "name",
    "cycleName",
    "targetOrgName",
    "indicatorCount",
    "status",
    "approvedAt"
  ],
  "format": "EXCEL"
}
```

**响应**: Excel文件流

---

### 7.2.4 导出审批数据

**接口地址**: `POST /api/v1/analytics/export/approvals`

**业务说明**: 导出审批历史数据

**请求参数**:

```json
{
  "filters": {
    "entityType": "INDICATOR",
    "dateFrom": "2024-01-01",
    "dateTo": "2024-12-31",
    "status": "APPROVED"
  },
  "columns": [
    "instanceNo",
    "entityTitle",
    "applicantName",
    "flowName",
    "currentStep",
    "status",
    "submittedAt",
    "completedAt",
    "duration"
  ],
  "format": "EXCEL"
}
```

**响应**: Excel文件流

---

### 7.2.5 导出用户数据

**接口地址**: `POST /api/v1/analytics/export/users`

**业务说明**: 导出用户数据（管理员功能）

**请求参数**:

```json
{
  "filters": {
    "orgId": 5,
    "status": "ACTIVE",
    "roleId": 2
  },
  "columns": [
    "id",
    "username",
    "realName",
    "email",
    "phone",
    "orgName",
    "roles",
    "status",
    "lastLoginAt",
    "createdAt"
  ],
  "format": "EXCEL"
}
```

**响应**: Excel文件流

---

### 7.2.6 获取绩效分析报告

**接口地址**: `GET /api/v1/analytics/report`

**业务说明**: 获取详细的绩效分析报告数据

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orgId | Long | 否 | 组织ID |
| year | Integer | 否 | 年度 |
| quarter | Integer | 否 | 季度（1-4） |
| month | Integer | 否 | 月份（1-12） |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "period": {
      "year": 2024,
      "quarter": 1,
      "month": null
    },
    "summary": {
      "totalIndicators": 156,
      "completedIndicators": 130,
      "overallCompletionRate": 83.3,
      "onTimeCompletionRate": 78.2,
      "avgApprovalTime": "2.1天"
    },
    "byDepartment": [
      {
        "orgId": 5,
        "orgName": "计算机学院",
        "totalIndicators": 25,
        "completedIndicators": 23,
        "completionRate": 92.0,
        "rank": 1
      },
      {
        "orgId": 6,
        "orgName": "软件学院",
        "totalIndicators": 22,
        "completedIndicators": 18,
        "completionRate": 81.8,
        "rank": 3
      }
    ],
    "byCategory": [
      {
        "category": "教学质量",
        "totalIndicators": 60,
        "completionRate": 88.5
      },
      {
        "category": "科研成果",
        "totalIndicators": 45,
        "completionRate": 79.2
      },
      {
        "category": "社会服务",
        "totalIndicators": 30,
        "completionRate": 85.0
      }
    ],
    "trends": {
      "completionRate": [
        {"period": "Q1", "rate": 75.5},
        {"period": "Q2", "rate": 82.3},
        {"period": "Q3", "rate": 85.7},
        {"period": "Q4", "rate": 83.3}
      ]
    },
    "issues": [
      {
        "type": "DELAY",
        "description": "软件学院有5个指标超期未完成",
        "count": 5,
        "severity": "HIGH"
      },
      {
        "type": "APPROVAL_BACKLOG",
        "description": "科研处有8个待审批任务",
        "count": 8,
        "severity": "MEDIUM"
      }
    ]
  }
}
```

---

### 7.2.7 获取用户活动统计

**接口地址**: `GET /api/v1/analytics/user-activity`

**业务说明**: 获取用户活动统计（登录、操作等）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 否 | 用户ID（不传则统计所有用户） |
| dateFrom | LocalDate | 否 | 统计开始日期 |
| dateTo | LocalDate | 否 | 统计结束日期 |
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "summary": {
      "totalUsers": 150,
      "activeUsers": 120,
      "activeRate": 80.0,
      "totalLogins": 2500,
      "avgLoginsPerUser": 16.7
    },
    "activities": {
      "content": [
        {
          "userId": 5,
          "username": "zhangsan",
          "realName": "张三",
          "loginCount": 25,
          "lastLoginAt": "2024-01-21T09:30:00",
          "operations": {
            "indicatorCreate": 5,
            "indicatorDistribute": 10,
            "planApprove": 8,
            "reportView": 15
          }
        }
      ],
      "totalElements": 120
    }
  }
}
```

---

## 8. 报告管理

**业务场景**: 报告的增删改查、导出PDF、变更历史

**Controller**: `ReportController`

### 8.1 查询报告列表

**接口地址**: `GET /api/v1/reports`

**业务说明**: 分页查询报告列表

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |
| indicatorId | Long | 否 | 指标ID |
| orgId | Long | 否 | 组织ID |
| status | String | 否 | 状态 |
| dateFrom | LocalDate | 否 | 日期起 |
| dateTo | LocalDate | 否 | 日期止 |

---

### 8.2 查询报告详情

**接口地址**: `GET /api/v1/reports/{id}`

**业务说明**: 根据报告ID查询报告详细信息

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 报告ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reportId": 1,
    "indicatorId": 100,
    "indicatorDesc": "年度教学指标",
    "milestoneId": 10,
    "milestoneName": "第一季度完成",
    "adhocTaskId": null,
    "adhocTaskTitle": null,
    "percentComplete": 75.50,
    "achievedMilestone": false,
    "narrative": "第一季度进展顺利，已完成75%",
    "reporterId": 5,
    "reporterName": "张三",
    "reporterOrgId": 2,
    "reporterOrgName": "软件学院",
    "status": "SUBMITTED",
    "isFinal": false,
    "versionNo": 1,
    "reportedAt": "2024-01-15T10:30:00",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

----

### 8.3 查询指标报告列表

**接口地址**: `GET /api/v1/reports/indicator/{indicatorId}`

**业务说明**: 查询指定指标的所有报告（不分页）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| indicatorId | Long | 是 | 指标ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "reportId": 1,
      "indicatorId": 100,
      "indicatorDesc": "年度教学指标",
      "status": "SUBMITTED",
      "percentComplete": 75.50,
      "reportedAt": "2024-01-15T10:30:00"
    }
  ]
}
```

----

### 8.4 查询指标报告列表（分页）

**接口地址**: `GET /api/v1/reports/indicator/{indicatorId}/page`

**业务说明**: 分页查询指定指标的报告列表

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| indicatorId | Long | 是 | 指标ID |
| page | Integer | 否 | 页码（默认0） |
| size | Integer | 否 | 每页数量（默认10） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "reportId": 1,
        "indicatorId": 100,
        "status": "SUBMITTED",
        "percentComplete": 75.50
      }
    ],
    "totalElements": 25,
    "page": 0,
    "size": 10
  }
}
```

----

### 8.5 按状态查询报告

**接口地址**: `GET /api/v1/reports/status/{status}`

**业务说明**: 查询指定状态的所有报告

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | String | 是 | 报告状态（DRAFT/SUBMITTED/APPROVED/REJECTED/RETURNED） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "reportId": 1,
      "status": "SUBMITTED",
      "indicatorDesc": "年度教学指标"
    }
  ]
}
```

----

### 8.6 查询我的报告

**接口地址**: `GET /api/v1/reports/my-reports`

**业务说明**: 查询当前登录用户提交的所有报告

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "reportId": 1,
      "indicatorId": 100,
      "status": "DRAFT",
      "reporterId": 5,
      "reporterName": "张三",
      "createdAt": "2024-01-15T10:30:00"
    }
  ]
}
```

----

### 8.7 创建报告

**接口地址**: `POST /api/v1/reports`

**业务说明**: 创建新的进度报告（初始状态为DRAFT）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| indicatorId | Long | 是 | 指标ID |
| milestoneId | Long | 否 | 里程碑ID（不能与adhocTaskId同时设置） |
| adhocTaskId | Long | 否 | 临时任务ID（不能与milestoneId同时设置） |
| percentComplete | BigDecimal | 否 | 完成百分比（0-100，默认0） |
| achievedMilestone | Boolean | 否 | 是否达成里程碑（默认false） |
| narrative | String | 否 | 报告描述/说明 |
| reporterId | Long | 是 | 报告人ID |

**请求示例**:
```json
{
  "indicatorId": 100,
  "milestoneId": 10,
  "percentComplete": 75.50,
  "achievedMilestone": false,
  "narrative": "第一季度进展顺利，已完成75%",
  "reporterId": 5
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "Report created successfully",
  "data": {
    "reportId": 1,
    "indicatorId": 100,
    "status": "DRAFT",
    "versionNo": 1,
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

----

### 8.8 更新报告

**接口地址**: `PUT /api/v1/reports/{id}`

**业务说明**: 更新报告（仅DRAFT和RETURNED状态的报告可以更新）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 报告ID（路径参数） |
| milestoneId | Long | 否 | 里程碑ID（不能与adhocTaskId同时设置） |
| adhocTaskId | Long | 否 | 临时任务ID（不能与milestoneId同时设置） |
| percentComplete | BigDecimal | 否 | 完成百分比（0-100） |
| achievedMilestone | Boolean | 否 | 是否达成里程碑 |
| narrative | String | 否 | 报告描述/说明 |

**请求示例**:
```json
{
  "percentComplete": 85.00,
  "achievedMilestone": true,
  "narrative": "第一季度已顺利完成"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "Report updated successfully",
  "data": {
    "reportId": 1,
    "status": "DRAFT",
    "percentComplete": 85.00,
    "updatedAt": "2024-01-15T11:30:00"
  }
}
```

----

### 8.9 提交报告

**接口地址**: `POST /api/v1/reports/{id}/submit`

**业务说明**: 提交草稿报告进行审批（状态从DRAFT变更为SUBMITTED）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 报告ID（路径参数） |

**响应示例**:
```json
{
  "code": 200,
  "message": "Report submitted successfully",
  "data": {
    "reportId": 1,
    "status": "SUBMITTED",
    "updatedAt": "2024-01-15T12:00:00"
  }
}
```

----

### 8.10 撤回报告

**接口地址**: `POST /api/v1/reports/{id}/withdraw`

**业务说明**: 撤回已提交的报告（状态从SUBMITTED变更为DRAFT）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 报告ID（路径参数） |

**响应示例**:
```json
{
  "code": 200,
  "message": "Report withdrawn successfully",
  "data": {
    "reportId": 1,
    "status": "DRAFT",
    "updatedAt": "2024-01-15T12:30:00"
  }
}
```

----

### 8.11 查询待审批报告列表

**接口地址**: `GET /api/v1/reports/pending-approval`

**业务说明**: 查询所有待审批的报告（不分页）

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "reportId": 1,
      "indicatorId": 100,
      "status": "SUBMITTED",
      "reporterName": "张三",
      "reporterOrgName": "软件学院",
      "submittedAt": "2024-01-15T10:30:00"
    }
  ]
}
```

----

### 8.12 查询待审批报告列表（分页）

**接口地址**: `GET /api/v1/reports/pending-approval/page`

**业务说明**: 分页查询待审批的报告列表

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码（默认0） |
| size | Integer | 否 | 每页数量（默认10） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "reportId": 1,
        "indicatorId": 100,
        "status": "SUBMITTED",
        "reporterName": "张三"
      }
    ],
    "totalElements": 50,
    "page": 0,
    "size": 10
  }
}
```

----

### 8.13 查询报告审批记录

**接口地址**: `GET /api/v1/reports/{id}/approval-records`

**业务说明**: 查询指定报告的审批历史记录

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 报告ID（路径参数） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "approvalId": 1,
      "reportId": 1,
      "approverId": 10,
      "approverName": "李四",
      "approverOrgId": 3,
      "approverOrgName": "科研处",
      "action": "APPROVE",
      "comment": "报告内容详实，同意通过",
      "actedAt": "2024-01-16T09:00:00"
    }
  ]
}
```

----

### 8.14 处理报告审批

**接口地址**: `POST /api/v1/reports/approve`

**业务说明**: 对提交的报告进行审批（批准/驳回/退回）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| reportId | Long | 是 | 报告ID |
| action | String | 是 | 审批动作（APPROVE/REJECT/RETURN） |
| approved | Boolean | 是 | 是否批准（true批准，false驳回） |
| comment | String | 否 | 审批意见（最大2000字符） |
| approvalNotes | String | 否 | 审批备注（最大2000字符） |
| rejectionReason | String | 否 | 驳回原因（approved=false时必填，最大1000字符） |
| improvementSuggestions | String | 否 | 改进建议（最大1500字符） |

**请求示例（批准）**:
```json
{
  "reportId": 1,
  "action": "APPROVE",
  "approved": true,
  "comment": "报告内容详实，数据准确，同意通过",
  "approvalNotes": "建议继续保持"
}
```

**请求示例（驳回）**:
```json
{
  "reportId": 1,
  "action": "REJECT",
  "approved": false,
  "comment": "报告内容需要补充",
  "rejectionReason": "缺少关键数据支撑，需要补充具体的实施方案",
  "improvementSuggestions": "建议补充详细的数据来源和分析过程"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "Approval processed successfully",
  "data": {
    "reportId": 1,
    "status": "APPROVED",
    "updatedAt": "2024-01-16T09:00:00"
  }
}
```

----

## 9. 附件管理

**业务场景**: 附件的上传、下载、删除

**Controller**: `AttachmentController`

### 9.1-9.4 附件管理接口

包括：查询附件列表、上传附件、下载附件、删除附件。

---

## 10. 通知中心

**业务场景**: 站内通知的增删改查、已读/未读、批量操作、统计、导出、快捷操作

**Controller**: `NotificationController`

### 10.1 查询用户通知列表（分页）

**接口地址**: `GET /api/v1/notifications/user/{userId}`

**业务说明**: 分页查询指定用户的通知列表

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID（路径参数） |
| page | Integer | 否 | 页码（默认0） |
| size | Integer | 否 | 每页数量（默认10） |
| sort | String | 否 | 排序字段（如：sentAt,desc） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "recipientUser": {
          "userId": 5,
          "username": "zhangsan",
          "realName": "张三",
          "email": "zhangsan@example.com"
        },
        "notificationType": "INDICATOR_DISTRIBUTED",
        "notificationTypeDisplayName": "指标已下发",
        "title": "新的战略指标已分配给您",
        "message": "您有一个新的战略指标需要确认，请及时处理",
        "relatedIndicator": {
          "indicatorId": 100,
          "indicatorName": "年度教学质量指标",
          "indicatorLevel": 1,
          "workflowStatus": "DISTRIBUTED"
        },
        "isRead": false,
        "sentAt": "2024-01-15T10:30:00",
        "priority": "HIGH",
        "priorityDisplayName": "高",
        "isOverdue": false,
        "isHighPriority": true
      }
    ],
    "totalElements": 50,
    "page": 0,
    "size": 10
  }
}
```

----

### 10.2 查询用户未读通知

**接口地址**: `GET /api/v1/notifications/user/{userId}/unread`

**业务说明**: 查询指定用户的所有未读通知（不分页）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID（路径参数） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "notificationType": "PROGRESS_SUBMITTED",
      "title": "进度报告已提交",
      "message": "用户提交了新的进度报告，请审批",
      "isRead": false,
      "sentAt": "2024-01-15T10:30:00",
      "priority": "URGENT"
    }
  ]
}
```

----

### 10.3 查询用户通知统计

**接口地址**: `GET /api/v1/notifications/user/{userId}/statistics`

**业务说明**: 查询指定用户的通知统计信息

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID（路径参数） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalCount": 150,
    "unreadCount": 25,
    "readCount": 125,
    "highPriorityCount": 8,
    "overdueCount": 5,
    "indicatorDistributedCount": 30,
    "progressSubmittedCount": 45,
    "approvalDecisionCount": 35,
    "overdueReminderCount": 15,
    "workflowCreatedCount": 25
  }
}
```

----

### 10.4 按条件查询通知

**接口地址**: `GET /api/v1/notifications/search`

**业务说明**: 根据多个条件组合查询通知（分页）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| recipientUserId | Long | 否 | 接收用户ID |
| notificationType | String | 否 | 通知类型（如：INDICATOR_DISTRIBUTED、PROGRESS_SUBMITTED等） |
| isRead | Boolean | 否 | 是否已读 |
| priority | String | 否 | 优先级（LOW、MEDIUM、HIGH、URGENT） |
| startDate | LocalDateTime | 否 | 开始时间 |
| endDate | LocalDateTime | 否 | 结束时间 |
| page | Integer | 否 | 页码（默认0） |
| size | Integer | 否 | 每页数量（默认10） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "notificationType": "INDICATOR_DISTRIBUTED",
        "isRead": false,
        "priority": "HIGH",
        "sentAt": "2024-01-15T10:30:00"
      }
    ],
    "totalElements": 35,
    "page": 0,
    "size": 10
  }
}
```

----

### 10.5 查询指标相关通知

**接口地址**: `GET /api/v1/notifications/indicator/{indicatorId}`

**业务说明**: 查询与指定指标相关的所有通知

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| indicatorId | Long | 是 | 指标ID（路径参数） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "notificationType": "INDICATOR_DISTRIBUTED",
      "title": "指标已下发",
      "relatedIndicator": {
        "indicatorId": 100,
        "indicatorName": "年度教学质量指标"
      },
      "sentAt": "2024-01-15T10:30:00"
    }
  ]
}
```

----

### 10.6 查询进度报告相关通知

**接口地址**: `GET /api/v1/notifications/report/{reportId}`

**业务说明**: 查询与指定进度报告相关的所有通知

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| reportId | Long | 是 | 报告ID（路径参数） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "notificationType": "PROGRESS_SUBMITTED",
      "title": "进度报告已提交",
      "relatedReport": {
        "reportId": 50,
        "reportPeriod": "2024年第一季度",
        "progressValue": 75.5
      },
      "sentAt": "2024-01-15T10:30:00"
    }
  ]
}
```

----

### 10.7 标记单条通知为已读

**接口地址**: `PUT /api/v1/notifications/{notificationId}/read`

**业务说明**: 标记指定通知为已读状态

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| notificationId | Long | 是 | 通知ID（路径参数） |
| userId | Long | 是 | 用户ID（确保只能操作自己的通知） |

**响应示例**:
```json
{
  "code": 200,
  "message": "通知已标记为已读",
  "data": true
}
```

----

### 10.8 标记用户所有通知为已读

**接口地址**: `PUT /api/v1/notifications/user/{userId}/read-all`

**业务说明**: 将指定用户的所有未读通知标记为已读

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID（路径参数） |

**响应示例**:
```json
{
  "code": 200,
  "message": "已标记 25 条通知为已读",
  "data": 25
}
```

----

### 10.9 按类型标记通知为已读

**接口地址**: `PUT /api/v1/notifications/user/{userId}/read-by-type`

**业务说明**: 将指定用户特定类型的所有未读通知标记为已读

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID（路径参数） |
| notificationType | String | 是 | 通知类型（如：INDICATOR_DISTRIBUTED） |

**响应示例**:
```json
{
  "code": 200,
  "message": "已标记 10 条 INDICATOR_DISTRIBUTED 类型通知为已读",
  "data": 10
}
```

----

### 10.10 删除单条通知

**接口地址**: `DELETE /api/v1/notifications/{notificationId}`

**业务说明**: 删除指定的通知（软删除或硬删除）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| notificationId | Long | 是 | 通知ID（路径参数） |
| userId | Long | 是 | 用户ID（确保只能删除自己的通知） |

**响应示例**:
```json
{
  "code": 200,
  "message": "通知已删除",
  "data": true
}
```

----

### 10.11 批量删除通知

**接口地址**: `DELETE /api/v1/notifications/bulk`

**业务说明**: 批量删除多条通知

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| notificationIds | List[Long] | 是 | 通知ID列表（如：1,2,3） |
| userId | Long | 是 | 用户ID（确保只能删除自己的通知） |

**响应示例**:
```json
{
  "code": 200,
  "message": "已删除 5 条通知",
  "data": 5
}
```

----

### 10.12 查询所有通知模板

**接口地址**: `GET /api/v1/notifications/templates`

**业务说明**: 获取系统中所有可用的通知模板

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "templateId": "INDICATOR_DISTRIBUTED",
      "templateName": "指标下发通知",
      "title": "新的战略指标已分配",
      "content": "您有一个新的战略指标需要确认",
      "supportedVariables": ["indicatorName", "indicatorLevel", "deadline"]
    },
    {
      "templateId": "PROGRESS_SUBMITTED",
      "templateName": "进度提交通知",
      "title": "进度报告已提交",
      "content": "用户提交了新的进度报告",
      "supportedVariables": ["reporterName", "progressValue", "submitTime"]
    }
  ]
}
```

----

### 10.13 查询指定类型的通知模板

**接口地址**: `GET /api/v1/notifications/templates/{notificationType}`

**业务说明**: 根据通知类型查询对应的通知模板

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| notificationType | String | 是 | 通知类型（路径参数，如：INDICATOR_DISTRIBUTED） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "templateId": "INDICATOR_DISTRIBUTED",
    "templateName": "指标下发通知",
    "title": "新的战略指标已分配给您",
    "content": "您有一个新的战略指标【{indicatorName}】需要确认，截止日期为{deadline}",
    "supportedVariables": [
      {
        "variableName": "indicatorName",
        "description": "指标名称",
        "dataType": "String"
      },
      {
        "variableName": "deadline",
        "description": "截止日期",
        "dataType": "LocalDateTime"
      }
    ]
  }
}
```

----

### 10.14 预览通知模板

**接口地址**: `POST /api/v1/notifications/templates/{templateId}/preview`

**业务说明**: 使用示例数据预览通知模板的渲染效果

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| templateId | String | 是 | 模板ID（路径参数） |
| sampleData | Map | 否 | 示例数据（用于填充模板变量） |

**请求示例**:
```json
{
  "indicatorName": "年度教学质量指标",
  "deadline": "2024-12-31T23:59:59",
  "assigneeName": "张三"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "title": "新的战略指标已分配给您",
    "content": "您有一个新的战略指标【年度教学质量指标】需要确认，截止日期为2024-12-31 23:59:59",
    "htmlContent": "<p>您有一个新的战略指标【年度教学质量指标】需要确认</p>",
    "previewUrl": "/api/v1/notifications/templates/INDICATOR_DISTRIBUTED/preview"
  }
}
```

----

### 10.15 查询通知发送统计

**接口地址**: `GET /api/v1/notifications/statistics/delivery`

**业务说明**: 查询指定时间段内的通知发送统计信息

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| startDate | LocalDateTime | 否 | 开始时间（默认为30天前） |
| endDate | LocalDateTime | 否 | 结束时间（默认为当前时间） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalSent": 1250,
    "successfullyDelivered": 1180,
    "failedDelivery": 70,
    "pendingDelivery": 0,
    "deliverySuccessRate": 94.4,
    "averageDeliveryTime": "0.5秒",
    "byType": {
      "INDICATOR_DISTRIBUTED": 350,
      "PROGRESS_SUBMITTED": 420,
      "APPROVAL_DECISION": 280,
      "OVERDUE_REMINDER": 200
    },
    "byPriority": {
      "LOW": 250,
      "MEDIUM": 500,
      "HIGH": 350,
      "URGENT": 150
    },
    "dailyStats": [
      {
        "date": "2024-01-15",
        "count": 125
      },
      {
        "date": "2024-01-14",
        "count": 118
      }
    ]
  }
}
```

----

### 10.16 查询部门通知统计

**接口地址**: `GET /api/v1/notifications/statistics/department/{departmentId}`

**业务说明**: 查询指定部门在指定时间段内的通知统计信息

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| departmentId | Long | 是 | 部门ID（路径参数） |
| startDate | LocalDateTime | 否 | 开始时间（默认为30天前） |
| endDate | LocalDateTime | 否 | 结束时间（默认为当前时间） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "departmentId": 3,
    "departmentName": "软件学院",
    "totalReceived": 450,
    "totalRead": 395,
    "totalUnread": 55,
    "readRate": 87.8,
    "averageReadTime": "2.5小时",
    "byType": {
      "INDICATOR_DISTRIBUTED": 120,
      "PROGRESS_SUBMITTED": 180,
      "APPROVAL_DECISION": 150
    },
    "topUsers": [
      {
        "userId": 5,
        "realName": "张三",
        "receivedCount": 45,
        "readRate": 95.5
      }
    ]
  }
}
```

----

### 10.17 查询通知类型统计

**接口地址**: `GET /api/v1/notifications/statistics/types`

**业务说明**: 查询指定时间段内各类型通知的统计信息

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| startDate | LocalDateTime | 否 | 开始时间（默认为30天前） |
| endDate | LocalDateTime | 否 | 结束时间（默认为当前时间） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "INDICATOR_DISTRIBUTED": 350,
    "PROGRESS_SUBMITTED": 420,
    "APPROVAL_DECISION": 280,
    "OVERDUE_REMINDER": 200,
    "WORKFLOW_CREATED": 150,
    "CONFIRMATION_REQUIRED": 120
  }
}
```

----

### 10.18 查询失败通知统计

**接口地址**: `GET /api/v1/notifications/statistics/failed`

**业务说明**: 查询系统中发送失败的通知统计信息

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalFailed": 70,
    "byType": {
      "INDICATOR_DISTRIBUTED": 15,
      "PROGRESS_SUBMITTED": 25,
      "APPROVAL_DECISION": 20,
      "OVERDUE_REMINDER": 10
    },
    "byReason": {
      "USER_NOT_FOUND": 25,
      "INVALID_EMAIL": 15,
      "TIMEOUT": 20,
      "OTHER": 10
    },
    "avgRetryCount": 1.5,
    "needsRetry": 45,
    "maxRetriesExceeded": 25
  }
}
```

----

### 10.19 检查通知系统健康状态

**接口地址**: `GET /api/v1/notifications/health`

**业务说明**: 检查通知系统的运行健康状态

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "HEALTHY",
    "uptime": "15天 8小时 30分钟",
    "lastCheckTime": "2024-01-15T10:30:00",
    "queueSize": 125,
    "processingRate": "95.5%",
    "averageResponseTime": "0.3秒",
    "failedLastHour": 2,
    "services": {
      "database": "UP",
      "redis": "UP",
      "emailService": "UP",
      "smsService": "UP"
    },
    "memoryUsage": {
      "used": "512MB",
      "total": "1024MB",
      "percentage": 50
    }
  }
}
```

----

### 10.20 重试失败的通知发送

**接口地址**: `POST /api/v1/notifications/retry-failed`

**业务说明**: 重新尝试发送所有失败的通知

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "已重试 45 条失败通知",
  "data": 45
}
```

----

### 10.21 发送逾期提醒通知

**接口地址**: `POST /api/v1/notifications/send-overdue-reminders`

**业务说明**: 向超过指定时间未读的通知发送提醒

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| hoursOverdue | Integer | 否 | 逾期小时数（默认为24小时） |

**响应示例**:
```json
{
  "code": 200,
  "message": "已发送 15 条逾期提醒",
  "data": 15
}
```

----

### 10.22 清理过期通知

**接口地址**: `DELETE /api/v1/notifications/cleanup`

**业务说明**: 清理指定天数之前的旧通知

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| daysOld | Integer | 否 | 天数（默认为90天） |

**响应示例**:
```json
{
  "code": 200,
  "message": "已清理 250 条过期通知",
  "data": 250
}
```

----

### 10.23 获取通知中心数据

**接口地址**: `GET /api/v1/notifications/center/{userId}`

**业务说明**: 获取通知中心的完整数据（包括通知列表、统计信息等）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID（路径参数） |
| includeRead | Boolean | 否 | 是否包含已读通知（默认false） |
| limit | Integer | 否 | 返回数量限制（默认50） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "notifications": [
      {
        "id": 1,
        "notificationType": "INDICATOR_DISTRIBUTED",
        "title": "新的战略指标已分配",
        "isRead": false,
        "sentAt": "2024-01-15T10:30:00"
      }
    ],
    "summary": {
      "totalCount": 150,
      "unreadCount": 25,
      "highPriorityCount": 8
    },
    "quickActions": [
      {
        "actionType": "MARK_ALL_READ",
        "label": "全部标为已读"
      },
      {
        "actionType": "DELETE_OLD",
        "label": "删除30天前的通知"
      }
    ]
  }
}
```

----

### 10.24 获取通知中心摘要

**接口地址**: `GET /api/v1/notifications/center/{userId}/summary`

**业务说明**: 获取通知中心的摘要信息（统计数据）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID（路径参数） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalCount": 150,
    "unreadCount": 25,
    "readCount": 125,
    "highPriorityCount": 8,
    "overdueCount": 5,
    "todayCount": 12,
    "weekCount": 45,
    "byType": {
      "INDICATOR_DISTRIBUTED": 10,
      "PROGRESS_SUBMITTED": 8,
      "APPROVAL_DECISION": 5,
      "OVERDUE_REMINDER": 2
    },
    "recentNotifications": [
      {
        "id": 1,
        "title": "新的战略指标已分配",
        "isRead": false,
        "sentAt": "2024-01-15T10:30:00"
      }
    ]
  }
}
```

----

### 10.25 搜索通知

**接口地址**: `GET /api/v1/notifications/search/{userId}`

**业务说明**: 根据关键字搜索用户的通知（支持标题和内容搜索）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID（路径参数） |
| query | String | 是 | 搜索关键字 |
| page | Integer | 否 | 页码（默认0） |
| size | Integer | 否 | 每页数量（默认10） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "新的战略指标已分配",
        "message": "您有一个新的战略指标需要确认",
        "highlight": {
          "title": "新的<strong>战略</strong>指标已分配",
          "message": "您有一个新的<strong>战略</strong>指标需要确认"
        }
      }
    ],
    "totalElements": 18,
    "page": 0,
    "size": 10
  }
}
```

----

### 10.26 高级通知过滤

**接口地址**: `POST /api/v1/notifications/filter/{userId}`

**业务说明**: 使用复杂的过滤条件筛选通知

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID（路径参数） |
| filters | Map | 是 | 过滤条件（JSON格式） |
| page | Integer | 否 | 页码（默认0） |
| size | Integer | 否 | 每页数量（默认10） |

**请求示例**:
```json
{
  "notificationTypes": ["INDICATOR_DISTRIBUTED", "PROGRESS_SUBMITTED"],
  "priorities": ["HIGH", "URGENT"],
  "isRead": false,
  "dateRange": {
    "startDate": "2024-01-01T00:00:00",
    "endDate": "2024-01-31T23:59:59"
  },
  "relatedIndicatorIds": [100, 101, 102]
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "notificationType": "INDICATOR_DISTRIBUTED",
        "priority": "HIGH",
        "isRead": false
      }
    ],
    "totalElements": 35,
    "page": 0,
    "size": 10
  }
}
```

----

### 10.27 批量标记通知为已读

**接口地址**: `PUT /api/v1/notifications/bulk-read`

**业务说明**: 批量将多条通知标记为已读

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| notificationIds | List[Long] | 是 | 通知ID列表 |
| userId | Long | 是 | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "已标记 10 条通知为已读",
  "data": 10
}
```

----

### 10.28 批量标记通知为未读

**接口地址**: `PUT /api/v1/notifications/bulk-unread`

**业务说明**: 批量将多条通知标记为未读

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| notificationIds | List[Long] | 是 | 通知ID列表 |
| userId | Long | 是 | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "已标记 5 条通知为未读",
  "data": 5
}
```

----

### 10.29 归档通知

**接口地址**: `PUT /api/v1/notifications/archive`

**业务说明**: 将指定的通知归档（从收件箱移到归档）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| notificationIds | List[Long] | 是 | 通知ID列表 |
| userId | Long | 是 | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "已归档 8 条通知",
  "data": 8
}
```

----

### 10.30 获取归档的通知

**接口地址**: `GET /api/v1/notifications/archived/{userId}`

**业务说明**: 查询用户已归档的通知（分页）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID（路径参数） |
| page | Integer | 否 | 页码（默认0） |
| size | Integer | 否 | 每页数量（默认10） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "notificationType": "INDICATOR_DISTRIBUTED",
        "title": "新的战略指标已分配",
        "archivedAt": "2024-01-10T15:30:00"
      }
    ],
    "totalElements": 45,
    "page": 0,
    "size": 10
  }
}
```

----

### 10.31 恢复归档的通知

**接口地址**: `PUT /api/v1/notifications/restore`

**业务说明**: 将归档的通知恢复到收件箱

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| notificationIds | List[Long] | 是 | 通知ID列表 |
| userId | Long | 是 | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "已恢复 3 条归档通知",
  "data": 3
}
```

----

### 10.32 获取通知偏好设置

**接口地址**: `GET /api/v1/notifications/preferences/{userId}`

**业务说明**: 查询用户的通知偏好设置

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID（路径参数） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 5,
    "emailEnabled": true,
    "smsEnabled": false,
    "pushEnabled": true,
    "digestFrequency": "DAILY",
    "quietHours": {
      "enabled": true,
      "startTime": "22:00",
      "endTime": "08:00"
    },
    "typePreferences": {
      "INDICATOR_DISTRIBUTED": {
        "enabled": true,
        "email": true,
        "push": true,
        "sms": false
      },
      "PROGRESS_SUBMITTED": {
        "enabled": true,
        "email": true,
        "push": true,
        "sms": false
      }
    },
    "priorityPreferences": {
      "LOW": {
        "email": false,
        "push": true,
        "sms": false
      },
      "HIGH": {
        "email": true,
        "push": true,
        "sms": true
      },
      "URGENT": {
        "email": true,
        "push": true,
        "sms": true
      }
    }
  }
}
```

----

### 10.33 更新通知偏好设置

**接口地址**: `PUT /api/v1/notifications/preferences/{userId}`

**业务说明**: 更新用户的通知偏好设置

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID（路径参数） |
| preferences | Map | 是 | 偏好设置（JSON格式） |

**请求示例**:
```json
{
  "emailEnabled": true,
  "smsEnabled": false,
  "pushEnabled": true,
  "digestFrequency": "DAILY",
  "quietHours": {
    "enabled": true,
    "startTime": "22:00",
    "endTime": "08:00"
  },
  "typePreferences": {
    "INDICATOR_DISTRIBUTED": {
      "enabled": true,
      "email": true,
      "push": true,
      "sms": false
    }
  }
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "通知偏好设置已更新",
  "data": true
}
```

----

### 10.34 获取通知趋势分析

**接口地址**: `GET /api/v1/notifications/trends/{userId}`

**业务说明**: 查询用户在指定天数内的通知趋势分析

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID（路径参数） |
| days | Integer | 否 | 天数（默认30天） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "period": {
      "startDate": "2023-12-16",
      "endDate": "2024-01-15",
      "days": 30
    },
    "totalReceived": 150,
    "totalRead": 125,
    "averageReadTime": "2.5小时",
    "dailyTrend": [
      {
        "date": "2024-01-15",
        "received": 12,
        "read": 10
      },
      {
        "date": "2024-01-14",
        "received": 8,
        "read": 7
      }
    ],
    "typeDistribution": {
      "INDICATOR_DISTRIBUTED": 30,
      "PROGRESS_SUBMITTED": 45,
      "APPROVAL_DECISION": 35,
      "OVERDUE_REMINDER": 20,
      "OTHER": 20
    },
    "peakHours": [
      {
        "hour": 9,
        "count": 25
      },
      {
        "hour": 14,
        "count": 20
      }
    ]
  }
}
```

----

### 10.35 导出通知数据

**接口地址**: `POST /api/v1/notifications/export/{userId}`

**业务说明**: 导出用户的通知数据（支持多种格式）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID（路径参数） |
| filters | Map | 否 | 导出过滤条件（JSON格式） |
| format | String | 否 | 导出格式（EXCEL、CSV、PDF，默认EXCEL） |

**请求示例**:
```json
{
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-01-31T23:59:59",
  "notificationTypes": ["INDICATOR_DISTRIBUTED", "PROGRESS_SUBMITTED"],
  "includeRead": true
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "通知数据导出成功",
  "data": "/exports/notifications/user_5_20240115.xlsx"
}
```

----

### 10.36 获取通知快捷操作

**接口地址**: `GET /api/v1/notifications/quick-actions/{userId}`

**业务说明**: 获取用户可执行的快捷操作列表

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID（路径参数） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "actionType": "MARK_ALL_READ",
      "label": "全部标为已读",
      "icon": "el-icon-check",
      "description": "将所有未读通知标记为已读",
      "confirmRequired": false
    },
    {
      "actionType": "DELETE_OLD",
      "label": "删除30天前的通知",
      "icon": "el-icon-delete",
      "description": "删除30天前已读的通知",
      "confirmRequired": true,
      "confirmMessage": "确定要删除30天前的通知吗？"
    },
    {
      "actionType": "ARCHIVE_READ",
      "label": "归档已读通知",
      "icon": "el-icon-folder",
      "description": "将所有已读通知归档",
      "confirmRequired": false
    },
    {
      "actionType": "MARK_OVERDUE_READ",
      "label": "标记逾期通知为已读",
      "icon": "el-icon-time",
      "description": "将所有逾期未读通知标记为已读",
      "confirmRequired": false
    }
  ]
}
```

----

### 10.37 执行通知快捷操作

**接口地址**: `POST /api/v1/notifications/quick-actions/{userId}/{actionType}`

**业务说明**: 执行指定的快捷操作

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID（路径参数） |
| actionType | String | 是 | 操作类型（路径参数） |
| parameters | Map | 否 | 操作参数（JSON格式） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true,
    "message": "已成功将25条通知标记为已读",
    "affectedCount": 25,
    "actionType": "MARK_ALL_READ",
    "executedAt": "2024-01-15T10:30:00"
  }
}
```
---

### 10.38 查询通知模板列表

**接口地址**: `GET /api/v1/notifications/templates`

**业务说明**: 获取所有通知模板

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| notificationType | String | 否 | 通知类型筛选 |
| page | Integer | 否 | 页码（默认0） |
| size | Integer | 否 | 每页数量（默认10） |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "指标下发通知模板",
        "notificationType": "INDICATOR_DISTRIBUTED",
        "titleTemplate": "【指标下发】{indicatorName}",
        "messageTemplate": "您有一个新的战略指标需要处理，请在{deadline}前完成",
        "supportedChannels": ["EMAIL", "SMS", "WEBSOCKET"],
        "defaultChannel": "WEBSOCKET",
        "enabled": true,
        "createdAt": "2024-01-01T00:00:00"
      }
    ],
    "totalElements": 10
  }
}
```

---

### 10.39 创建通知模板

**接口地址**: `POST /api/v1/notifications/templates`

**业务说明**: 创建新的通知模板

**请求参数**:
```json
{
  "name": "任务完成通知模板",
  "notificationType": "TASK_COMPLETED",
  "titleTemplate": "【任务完成】{taskName}",
  "messageTemplate": "任务{taskName}已完成，请查看详情",
  "supportedChannels": ["EMAIL", "WEBSOCKET"],
  "defaultChannel": "WEBSOCKET",
  "enabled": true
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "通知模板创建成功",
  "data": {
    "id": 11,
    "name": "任务完成通知模板",
    "notificationType": "TASK_COMPLETED",
    "createdAt": "2024-03-13T10:00:00"
  }
}
```

---

### 10.40 更新通知模板

**接口地址**: `PUT /api/v1/notifications/templates/{id}`

**业务说明**: 更新通知模板

**请求参数**:
```json
{
  "titleTemplate": "【指标下发】{indicatorName}（已更新）",
  "enabled": false
}
```

---

### 10.41 删除通知模板

**接口地址**: `DELETE /api/v1/notifications/templates/{id}`

**业务说明**: 删除通知模板（系统内置模板不可不可删除）

---

### 10.42 预览通知模板

**接口地址**: `POST /api/v1/notifications/templates/preview`

**业务说明**: 使用模板数据预览通知效果

**请求参数**:
```json
{
  "templateId": 1,
  "variables": {
    "indicatorName": "年度教学质量指标",
    "deadline": "2024-12-31"
  }
}
```

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "title": "【指标下发】年度教学质量指标",
    "message": "您有一个新的战略指标需要处理，请在2024-12-31前完成"
  }
}
```

---

### 10.43 获取用户通知偏好

**接口地址**: `GET /api/v1/notifications/preferences/{userId}`

**业务说明**: 获取用户的通知偏好设置

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "userId": 5,
    "enableEmail": true,
    "enableSms": false,
    "enableWebSocket": true,
    "enablePush": true,
    "muteAll": false,
    "quietHours": {
      "enabled": true,
      "start": "22:00",
      "end": "08:00"
    },
    "typePreferences": {
      "INDICATOR_DISTRIBUTED": {"email": true, "sms": false, "websocket": true},
      "TASK_COMPLETED": {"email": false, "sms": false, "websocket": true},
      "APPROVAL_REQUEST": {"email": true, "sms": true, "websocket": true}
    }
  }
}
```

---

### 10.44 更新用户通知偏好

**接口地址**: `PUT /api/v1/notifications/preferences/{userId}`

**业务说明**: 更新用户的通知偏好设置

**请求参数**:
```json
{
  "enableEmail": true,
  "enableSms": false,
  "enableWebSocket": true,
  "muteAll": false,
  "quietHours": {
    "enabled": true,
    "start": "23:00",
    "end": "07:00"
  }
}
```

---

### 10.45 重置用户通知偏好为默认

**接口地址**: `POST /api/v1/notifications/preferences/{userId}/reset`

**业务说明**: 将用户通知偏好恢复为系统默认值
```

----

## 11. 预警告警

**业务场景**: 通过规则自动监控指标进度、任务状态，触发预警或告警并通知相关人员

**Controller**: `WarningController`, `AlertController`

### 11.1 预警规则管理

**接口地址**: `GET /api/v1/warnings/rules`

**业务说明**: 查询预警规则列表

**请求参数**: `page`, `size`, `name`, `status` (ENABLED/DISABLED)

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "content": [{
      "id": 1,
      "name": "指标进度滞后预警",
      "entityType": "INDICATOR",
      "condition": "progress < milestone.targetProgress * 0.9",
      "status": "ENABLED"
    }]
  }
}
```

---

### 11.2 创建预警规则

**接口地址**: `POST /api/v1/warnings/rules`

**请求参数**:
```json
{
  "name": "月度报告未提交预警",
  "entityType": "PLAN_REPORT",
  "condition": "status == 'DRAFT' && dueDate < NOW()",
  "messageTemplate": "【预警】${orgName}的${month}月度报告尚未提交"
}
```

---

### 11.3 查询预警事件

**接口地址**: `GET /api/v1/warnings/events`

**请求参数**: `page`, `size`, `indicatorId`, `orgId`, `status` (ACTIVE/ACKNOWLEDGED)

---

### 11.4 确认预警

**接口地址**: `POST /api/v1/warnings/events/{id}/acknowledge`

**业务说明**: 标记预警为已确认

---

### 11.5 查询告警规则

**接口地址**: `GET /api/v1/alerts/rules`

**业务说明**: 查询告警规则列表

---

### 11.6 创建告警规则

**接口地址**: `POST /api/v1/alerts/rules`

**请求参数**:
```json
{
  "name": "指标连续未填报告警",
  "severity": "CRITICAL",
  "entityType": "INDICATOR",
  "condition": "progress == 0 && consecutiveMonths >= 2"
}
```

---

### 11.7 查询告警事件

**接口地址**: `GET /api/v1/alerts/events`

**请求参数**: `page`, `size`, `severity` (CRITICAL/MAJOR/MINOR), `status` (OPEN/IN_PROGRESS/CLOSED)

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "content": [{
      "id": 201,
      "severity": "CRITICAL",
      "message": "【告警】'新生报到率'指标已连续2个月未填报",
      "status": "OPEN",
      "triggeredAt": "2026-03-01T09:00:00"
    }]
  }
}
```

---

### 11.8 查询未关闭告警

**接口地址**: `GET /api/v1/alerts/events/unclosed`

**业务说明**: 获取所有未关闭的告警

---

### 11.9 处理告警

**接口地址**: `POST /api/v1/alerts/events/{id}/process`

**请求参数**:
```json
{
  "assigneeId": 123,
  "status": "IN_PROGRESS",
  "actionLog": "已联系相关部门，正在收集数据"
}
```

---

### 11.10 关闭告警

**接口地址**: `POST /api/v1/alerts/events/{id}/close`

**请求参数**:
```json
{
  "resolution": "已完成数据补报，问题已解决"
}
```

---

### 11.11 告警统计

**接口地址**: `GET /api/v1/alerts/stats`

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "totalOpen": 15,
    "countBySeverity": {
      "CRITICAL": 3,
      "MAJOR": 5,
      "MINOR": 7
    }
  }
}
```

---

## 12. 系统监控

**业务场景**: 健康检查、性能监控、数据库管理、审计日志、系统信息

**Controller**: `HealthController`, `MonitoringController`, `DatabaseController`, `AuditLogController`

---

### 12.1 健康检查

**接口地址**: `GET /api/v1/health`

**业务说明**: 基础健康检查，返回系统运行状态

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "系统运行正常",
  "data": {
    "status": "UP",
    "timestamp": "2024-01-21T10:00:00"
  }
}
```

---

### 12.2 详细健康检查（Actuator）

**接口地址**: `GET /actuator/health`

**业务说明**: Spring Boot Actuator健康检查，包含数据库、Redis等组件状态

**请求参数**: 无

**响应示例**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "6.2.6"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000,
        "threshold": 10485760
      }
    }
  }
}
```

---

### 12.3 系统信息

**接口地址**: `GET /api/v1/system/info`

**业务说明**: 获取系统基本信息（版本、环境、JVM等）

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "version": "1.0.0",
    "environment": "production",
    "jvm": {
      "version": "17.0.2",
      "vendor": "Oracle Corporation",
      "maxMemory": "2048MB",
      "totalMemory": "1024MB",
      "freeMemory": "512MB"
    },
    "os": {
      "name": "Linux",
      "version": "5.10.0",
      "arch": "amd64"
    },
    "startTime": "2024-01-20T08:00:00",
    "uptime": "26h 15m"
  }
}
```

---

### 12.4 性能指标

**接口地址**: `GET /api/v1/monitoring/metrics`

**业务说明**: 获取系统性能指标（CPU、内存、线程等）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| timeRange | String | 否 | 时间范围：1h/6h/24h/7d，默认1h |

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "cpu": {
      "usage": 45.5,
      "cores": 8
    },
    "memory": {
      "used": 1536,
      "total": 2048,
      "usagePercent": 75.0
    },
    "threads": {
      "active": 120,
      "peak": 150,
      "daemon": 80
    },
    "gc": {
      "youngGcCount": 1500,
      "youngGcTime": 3500,
      "fullGcCount": 5,
      "fullGcTime": 2000
    },
    "timestamp": "2024-01-21T10:00:00"
  }
}
```

---

### 12.5 数据库信息

**接口地址**: `GET /api/v1/database/info`

**业务说明**: 获取数据库连接信息和统计

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "type": "MySQL",
    "version": "8.0.32",
    "url": "jdbc:mysql://localhost:3306/sism",
    "driver": "com.mysql.cj.jdbc.Driver",
    "connectionPool": {
      "active": 5,
      "idle": 5,
      "max": 10,
      "min": 2
    },
    "statistics": {
      "totalConnections": 15000,
      "activeQueries": 3,
      "slowQueries": 12
    }
  }
}
```

---

### 12.6 数据库表信息

**接口地址**: `GET /api/v1/database/tables`

**业务说明**: 获取数据库表列表及统计信息

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "tables": [
      {
        "name": "strategic_indicators",
        "rows": 1500,
        "dataSize": "2.5MB",
        "indexSize": "1.2MB",
        "engine": "InnoDB",
        "collation": "utf8mb4_unicode_ci"
      },
      {
        "name": "plans",
        "rows": 5000,
        "dataSize": "8.3MB",
        "indexSize": "3.1MB",
        "engine": "InnoDB",
        "collation": "utf8mb4_unicode_ci"
      }
    ],
    "totalTables": 25,
    "totalSize": "150MB"
  }
}
```

---

### 12.7 数据库备份

**接口地址**: `POST /api/v1/database/backup`

**业务说明**: 触发数据库备份任务（异步）

**请求参数**:

```json
{
  "type": "FULL",
  "description": "月度备份"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| type | String | 是 | 备份类型：FULL（全量）/INCREMENTAL（增量） |
| description | String | 否 | 备份说明 |

**响应示例**:
```json
{
  "code": 200,
  "message": "备份任务已创建",
  "data": {
    "taskId": "backup_20240121_100000",
    "status": "PENDING",
    "createdAt": "2024-01-21T10:00:00"
  }
}
```

---

### 12.8 备份任务状态

**接口地址**: `GET /api/v1/database/backup/{taskId}`

**业务说明**: 查询备份任务状态

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | String | 是 | 任务ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "taskId": "backup_20240121_100000",
    "status": "COMPLETED",
    "type": "FULL",
    "startTime": "2024-01-21T10:00:00",
    "endTime": "2024-01-21T10:15:00",
    "duration": "15m",
    "fileSize": "250MB",
    "filePath": "/backups/backup_20240121_100000.sql.gz",
    "description": "月度备份"
  }
}
```

---

### 12.9 备份历史

**接口地址**: `GET /api/v1/database/backups`

**业务说明**: 获取备份历史记录

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页数量，默认20 |
| type | String | 否 | 备份类型筛选 |

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "items": [
      {
        "taskId": "backup_20240121_100000",
        "status": "COMPLETED",
        "type": "FULL",
        "startTime": "2024-01-21T10:00:00",
        "endTime": "2024-01-21T10:15:00",
        "fileSize": "250MB",
        "description": "月度备份"
      }
    ],
    "total": 50,
    "page": 1,
    "size": 20
  }
}
```

---

### 12.10 审计日志查询

**接口地址**: `GET /api/v1/audit/logs`

**业务说明**: 查询系统审计日志

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页数量，默认20 |
| userId | Long | 否 | 用户ID筛选 |
| action | String | 否 | 操作类型筛选 |
| startTime | String | 否 | 开始时间 |
| endTime | String | 否 | 结束时间 |
| keyword | String | 否 | 关键词搜索 |

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "items": [
      {
        "id": 1001,
        "userId": 5,
        "userName": "张三",
        "action": "UPDATE_INDICATOR",
        "resource": "strategic_indicators",
        "resourceId": 100,
        "description": "更新指标名称",
        "ip": "192.168.1.100",
        "userAgent": "Mozilla/5.0...",
        "status": "SUCCESS",
        "timestamp": "2024-01-21T10:00:00"
      }
    ],
    "total": 5000,
    "page": 1,
    "size": 20
  }
}
```

---

### 12.11 密码强度验证

**接口地址**: `POST /api/v1/system/validate-password`

**业务说明**: 验证密码强度是否符合要求

**请求参数**:

```json
{
  "password": "MyP@ssw0rd123"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "密码强度验证通过",
  "data": {
    "valid": true,
    "strength": "STRONG",
    "score": 85,
    "suggestions": []
  }
}
```

**密码强度不足示例**:
```json
{
  "code": 400,
  "message": "密码强度不足",
  "data": {
    "valid": false,
    "strength": "WEAK",
    "score": 35,
    "suggestions": [
      "密码长度至少8位",
      "需要包含大写字母",
      "需要包含特殊字符"
    ]
  }
}
```

---

---

### 12.11 性能指标（Actuator）

**接口地址**: `GET /actuator/metrics`

**业务说明**: 获取应用性能指标（JVM、内存、线程、GC等）

**响应示例**:
```json
{
  "names": ["jvm.memory.max", "jvm.memory.used", "system.cpu.usage", "process.uptime"]
}
```

---

### 12.12 获取特定指标

**接口地址**: `GET /actuator/metrics/{metric.name}`

**业务说明**: 获取特定指标的最新值

**响应示例**:
```json
{
  "name": "jvm.memory.used",
  "measurements": [
    {"statistic": "VALUE", "value": 5368704}
  ]
}
```

---

### 12.13 线程转储

**接口地址**: `GET /actuator/threaddump`

**业务说明**: 获取线程转储信息（用于诊断线程问题）

---

### 12.14 环境信息

**接口地址**: `GET /actuator/env`

**业务说明**: 获取应用环境配置（敏感信息已脱敏）

---

### 12.15 应用信息

**接口地址**: `GET /actuator/info`

**业务说明**: 获取应用基本信息

**响应示例**:
```json
{
  "app": {
    "name": "SISM",
    "version": "1.0.0"
  },
  "java": {
    "version": "17.0.2",
    "vendor": "Oracle Corporation"
  }
}
```
## 13. 批量&扩展

**业务场景**: 批量审批/驳回、填报锁定、模板下载/上传、审批耗时统计、指标进度汇总、催办、报告导出、变更历史等

**Controller**: `BatchController`, `IndicatorController`, `PlanController`, `AuditFlowController`, `WorkflowNotificationController`, `ReportController`, `IndicatorHistoryController`

### 13.1 批量审批报告

**接口地址**: `POST /api/v1/plans/reports/batch-approve`

**业务场景**: 职能部门一次勾选 N 条学院报告，点「批量通过」

**请求参数**:

```json
{
  "reportIds": [100, 101, 102, 103],
  "comment": "批量审批通过"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| reportIds | List<Long> | 是 | 报告ID列表 |
| comment | String | 否 | 审批意见 |

**响应示例**:
```json
{
  "code": 200,
  "message": "成功审批4个报告",
  "data": {
    "successCount": 4,
    "failedCount": 0,
    "failedReports": []
  }
}
```

**说明**：
- 遍历所有报告ID，依次调用审批通过接口
- 返回成功/失败统计
- 如果某个报告审批失败，不影响其他报告的审批

---

### 13.2 批量驳回报告

**接口地址**: `POST /api/v1/plans/reports/batch-reject`

**业务场景**: 批量驳回并填写统一的驳回原因

**请求参数**:

```json
{
  "reportIds": [104, 105],
  "comment": "数据需要补充，请重新提交",
  "requireResubmit": true
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| reportIds | List<Long> | 是 | 报告ID列表 |
| comment | String | 是 | 驳回原因 |
| requireResubmit | Boolean | 否 | 是否需要重新提交，默认true |

**响应示例**:
```json
{
  "code": 200,
  "message": "成功驳回2个报告",
  "data": {
    "successCount": 2,
    "failedCount": 0,
    "failedReports": []
  }
}
```

---

### 13.3 一键催办

**接口地址**: `POST /api/v1/notifications/overdue-reminders`

**业务场景**: 列表页对「待确认」指标发站内催办通知

**请求参数**:

```json
{
  "indicatorIds": [1, 2, 3],
  "message": "请尽快确认并分解指标",
  "urgency": "HIGH"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| indicatorIds | List<Long> | 是 | 指标ID列表 |
| message | String | 是 | 催办消息 |
| urgency | String | 否 | 紧急程度：NORMAL/HIGH/URGENT |

**响应示例**:
```json
{
  "code": 200,
  "message": "催办通知发送成功",
  "data": {
    "successCount": 3,
    "notificationsSent": 5
  }
}
```

**说明**：
- 查询每个指标的待接收组织
- 为每个组织生成催办通知
- 通过通知中心发送

---

### 13.4 查询指标进度汇总

**接口地址**: `GET /api/v1/indicators/{id}/aggregated-progress`

**业务场景**: 战略部看板需要「一级指标自动汇总所有二级指标平均进度」

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 一级指标ID |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "indicatorId": 1,
    "indicatorName": "年度学生满意度",
    "totalValue": 86.5,
    "targetValue": 90,
    "completionRate": 96.1,
    "childIndicators": [
      {
        "id": 11,
        "name": "计算机学院本科生满意度",
        "value": 85,
        "targetValue": 90,
        "weight": 0.3,
        "completionRate": 94.4
      },
      {
        "id": 12,
        "name": "软件学院本科生满意度",
        "value": 88,
        "targetValue": 90,
        "weight": 0.3,
        "completionRate": 97.8
      },
      {
        "id": 13,
        "name": "本科生满意度",
        "value": 87,
        "targetValue": 90,
        "weight": 0.4,
        "completionRate": 96.7
      }
    ],
    "summary": {
      "totalChildren": 3,
      "completedChildren": 3,
      "pendingChildren": 0,
      "avgCompletionRate": 96.3
    }
  }
}
```

**说明**：
- 查询所有二级指标
- 根据权重计算加权平均值
- 返回汇总进度和明细

---

### 13.5 查询报告锁定状态

**接口地址**: `GET /api/v1/plans/reports/{reportId}/lock-status`

**业务场景**: 每月25号后禁止再改当月报告（业务规则）

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| reportId | Long | 是 | 报告ID |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "reportId": 100,
    "locked": true,
    "lockedBy": {
      "id": 5,
      "name": "张三"
    },
    "lockedAt": "2024-01-26T10:00:00",
    "lockExpiresAt": "2024-02-01T00:00:00",
    "reason": "每月25号后禁止修改",
    "canEdit": false
  }
}
```

**说明**：
- 检查当前日期是否超过25号
- 如果超过，返回锁定状态
- 未超过则返回正常状态

---

### 13.6 锁定报告

**接口地址**: `PUT /api/v1/plans/reports/{reportId}/lock`

**业务场景**: 手动锁定报告（管理员操作）

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| reportId | Long | 是 | 报告ID |

**请求参数**:

```json
{
  "reason": "数据已确认，禁止修改"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "报告已锁定",
  "data": {
    "reportId": 100,
    "locked": true,
    "lockedAt": "2024-01-21T10:00:00"
  }
}
```

---

### 13.7 审批流配置热刷新

**接口地址**: `GET /api/v1/approval/flows/{id}/refresh`

**业务场景**: 运营后台可随时改审批人而不重启

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 审批流ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "配置已刷新",
  "data": {
    "flowId": 1,
    "version": 2,
    "refreshedAt": "2024-01-21T10:00:00",
    "activeInstances": 5
  }
}
```

---

### 13.8 重新加载审批流

**接口地址**: `POST /api/v1/approval/flows/{id}/reload`

**业务场景**: 强制重新加载审批流配置

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 审批流ID |

**请求参数**:

```json
{
  "force": true,
  "comment": "审批人调整，需要重新加载"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| force | Boolean | 否 | 是否强制重新加载，默认false |
| comment | String | 否 | 操作说明 |

**响应示例**:
```json
{
  "code": 200,
  "message": "审批流已重新加载",
  "data": {
    "flowId": 1,
    "oldVersion": 1,
    "newVersion": 2,
    "affectedInstances": 5
  }
}
```

---

### 13.9 下载指标分解模板

**接口地址**: `GET /api/v1/indicators/template/second-level`

**业务场景**: 职能部门分解30条二级指标前先下载Excel模板

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| parentId | Long | 否 | 一级指标ID（可选，用于预填部分数据） |

**响应**: Excel文件流

**响应头**:
```
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="indicator_decompose_template.xlsx"
```

**说明**：
- Excel模板包含：指标名称、目标组织、目标值、权重等列
- 如果提供parentId，预填一级指标信息
- 包含填写示例和数据验证规则

---

### 13.10 上传Excel批量分解指标

**接口地址**: `POST /api/v1/indicators/batch-decompose`

**业务场景**: 上传Excel批量生成二级指标

**请求类型**: `multipart/form-data`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | Excel文件 |
| parentIndicatorId | Long | 是 | 一级指标ID |
| validateOnly | Boolean | 否 | 是否仅验证不创建，默认false |

**响应示例**:
```json
{
  "code": 200,
  "message": "成功创建10个二级指标",
  "data": {
    "parentId": 1,
    "successCount": 10,
    "failedCount": 0,
    "validationErrors": [],
    "createdIndicators": [
      {
        "id": 11,
        "name": "计算机学院本科生满意度",
        "targetOrgId": 5
      }
    ]
  }
}
```

**验证失败示例**:
```json
{
  "code": 400,
  "message": "数据验证失败",
  "data": {
    "parentId": 1,
    "successCount": 0,
    "failedCount": 2,
    "validationErrors": [
      {
        "row": 3,
        "column": "目标组织",
        "error": "组织不存在",
        "value": "无效组织"
      },
      {
        "row": 5,
        "column": "权重",
        "error": "权重总和必须为1",
        "value": "0.6"
      }
    ]
  }
}
```

---

### 13.11 导出月度报告PDF

**接口地址**: `GET /api/v1/plans/reports/{reportId}/export/pdf`

**业务场景**: 学院/职能部门需要把月度报告导出PDF留档

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| reportId | Long | 是 | 报告ID |

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| includeAttachments | Boolean | 否 | 是否包含附件，默认true |
| watermark | Boolean | 否 | 是否添加水印，默认false |

**响应**: PDF文件流

**响应头**:
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="monthly_report_202401.pdf"
```

**说明**：
- 使用报告模板生成PDF
- 包含指标明细、完成情况、附件等
- 支持水印和电子签章

---

### 13.12 查询指标变更历史

**接口地址**: `GET /api/v1/indicators/{id}/history`

**业务场景**: 指标权重/目标值调整后留痕

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 指标ID |

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |
| action | String | 否 | 操作类型过滤 |
| dateFrom | LocalDate | 否 | 日期起 |
| dateTo | LocalDate | 否 | 日期止 |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "historyId": 500,
        "indicatorId": 1,
        "action": "UPDATE",
        "field": "targetValue",
        "oldValue": "85",
        "newValue": "90",
        "reason": "调整目标值",
        "operatedBy": {
          "id": 5,
          "name": "张三"
        },
        "operatedAt": "2024-01-21T10:00:00",
        "ipAddress": "192.168.1.100"
      },
      {
        "historyId": 499,
        "indicatorId": 1,
        "action": "DISTRIBUTE",
        "description": "下发到计算机学院",
        "operatedBy": {
          "id": 1,
          "name": "管理员"
        },
        "operatedAt": "2024-01-15T10:00:00"
      },
      {
        "historyId": 498,
        "indicatorId": 1,
        "action": "CREATE",
        "description": "创建指标",
        "operatedBy": {
          "id": 1,
          "name": "管理员"
        },
        "operatedAt": "2024-01-10T09:00:00"
      }
    ],
    "totalElements": 25
  }
}
```

**说明**：
- 记录指标的所有变更操作
- 包括创建、更新、下发、撤回等
- 保留变更前后值
- 支持审计追溯

---

### 13.13 审批耗时统计

**接口地址**: `GET /api/v1/approval/statistics/duration`

**业务场景**: 领导看板需要「平均审批耗时」

**说明**: 此接口已在「5.3.3 审批耗时统计」中定义，此处不再重复

---

### 13.14 标记所有通知为已读

**接口地址**: `PUT /api/v1/notifications/user/{userId}/read-all`

**业务场景**: 消息中心「一键已读」

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |

**请求参数**:

```json
{
  "type": "ALL"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| type | String | 否 | 类型：ALL（全部）/UNREAD（仅未读） |

**响应示例**:
```json
{
  "code": 200,
  "message": "已标记50条通知为已读",
  "data": {
    "markedCount": 50,
    "markedAt": "2024-01-21T10:00:00"
  }
}
```

---

### 13.15 快捷操作统计

**接口地址**: `GET /api/v1/notifications/quick-actions/{userId}`

**业务场景**: 获取可执行的快捷操作列表

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "unreadCount": 15,
    "quickActions": [
      {
        "actionType": "READ_ALL",
        "name": "全部已读",
        "available": true
      },
      {
        "actionType": "DELETE_READ",
        "name": "删除已读",
        "available": true
      },
      {
        "actionType": "ARCHIVE_ALL",
        "name": "全部归档",
        "available": false,
        "reason": "没有可归档的通知"
      }
    ]
  }
}
```

---

## 14. 首页/工作台

**业务场景**: 首页统计数据、待办任务、图表数据、快捷入口

**Controller**: `AnalyticsDashboardController`
**说明**: Dashboard已归入Analytics Context下

### 14.1 首页概览数据

**接口地址**: `GET /api/v1/analytics/dashboard/overview`

**业务说明**: 获取首页统计卡片数据（待办、已办、数据概览等）

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "pendingTasks": {
      "totalCount": 25,
      "urgentCount": 5,
      "items": [
        {
          "type": "INDICATOR_DISTRIBUTE",
          "count": 10,
          "url": "/indicators?status=PENDING_DISTRIBUTE"
        },
        {
          "type": "APPROVAL_PENDING",
          "count": 15,
          "url": "/approval/pending"
        }
      ]
    },
    "statistics": {
      "totalIndicators": 156,
      "activeIndicators": 120,
      "completedIndicators": 100,
      "totalPlans": 50,
      "approvedPlans": 35,
      "totalReports": 89,
      "approvedReports": 80
    },
    "todaySummary": {
      "submittedToday": 8,
      "approvedToday": 12,
      "createdToday": 5
    }
  }
}
```

---

### 14.2 待办任务列表

**接口地址**: `GET /api/v1/analytics/dashboard/pending-tasks`

**业务说明**: 获取当前用户的待办任务列表（聚合视图）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| limit | Integer | 否 | 返回数量限制，默认10 |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "pendingReceive": [
      {
        "indicatorId": 2,
        "indicatorName": "年度科研经费",
        "fromOrg": "总部",
        "deadline": "2024-01-31",
        "pendingDays": 5
      }
    ],
    "pendingApproval": [
      {
        "instanceId": 5001,
        "entityType": "INDICATOR",
        "entityTitle": "年度学生满意度指标",
        "currentStep": "部门审核",
        "submittedBy": "张三",
        "submittedAt": "2024-01-20T10:00:00",
        "pendingDays": 1
      }
    ],
    "pendingFill": [
      {
        "indicatorId": 11,
        "indicatorName": "计算机学院本科生满意度",
        "deadline": "2024-01-31",
        "completionRate": 0
      }
    ]
  }
}
```

---

### 14.3 图表数据

**接口地址**: `GET /api/v1/analytics/dashboard/charts`

**业务说明**: 获取首页展示的图表数据（趋势图、占比图等）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| chartType | String | 否 | 图表类型：trend/comparison/distribution |
| period | String | 否 | 时间周期：week/month/quarter/year |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "trendData": {
      "indicatorCompletion": [
        {"month": "2024-01", "value": 75},
        {"month": "2024-02", "value": 82},
        {"month": "2024-03", "value": 88}
      ],
      "approvalSpeed": [
        {"month": "2024-01", "avgHours": 48},
        {"month": "2024-02", "avgHours": 42},
        {"month": "2024-03", "avgHours": 36}
      ]
    },
    "distributionData": {
      "byOrg": [
        {"orgName": "计算机学院", "count": 45},
        {"orgName": "软件学院", "count": 38},
        {"orgName": "信息学院", "count": 35}
      ],
      "byStatus": [
        {"status": "已完成", "count": 100},
        {"status": "进行中", "count": 40},
        {"status": "未开始", "count": 16}
      ]
    }
  }
}
```

---

### 14.4 快捷入口

**接口地址**: `GET /api/v1/analytics/dashboard/shortcuts`

**业务说明**: 获取当前用户的快捷入口配置

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "shortcuts": [
      {
        "id": 1,
        "name": "创建指标",
        "icon": "plus",
        "url": "/indicators/create",
        "color": "primary"
      },
      {
        "id": 2,
        "name": "待办审批",
        "icon": "check-circle",
        "url": "/approval/pending",
        "color": "warning",
        "badge": 15
      },
      {
        "id": 3,
        "name": "我的报告",
        "icon": "file-text",
        "url": "/reports/my",
        "color": "success"
      }
    ]
  }
}
```

---

## 15. 个人中心

**业务场景**: 用户个人信息管理、修改密码、头像上传

**Controller**: `UserProfileController`

### 15.1 查询个人资料

**接口地址**: `GET /api/v1/users/profile`

**业务说明**: 获取当前用户的个人资料

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "id": 5,
    "username": "zhangsan",
    "realName": "张三",
    "email": "zhangsan@example.com",
    "phone": "13800138000",
    "avatar": "https://example.com/avatar/5.jpg",
    "orgId": 5,
    "orgName": "计算机学院",
    "roles": ["ROLE_USER", "ROLE_DEPT_ADMIN"],
    "bio": "负责学院绩效管理工作",
    "createdAt": "2024-01-01T00:00:00",
    "lastLoginAt": "2024-01-21T09:30:00"
  }
}
```

---

### 15.2 更新个人资料

**接口地址**: `PUT /api/v1/users/profile`

**业务说明**: 修改当前用户的个人资料

**请求参数**:

```json
{
  "realName": "张三丰",
  "email": "zhangsanfeng@example.com",
  "phone": "13900139000",
  "bio": "负责学院绩效管理和数据分析工作"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| realName | String | 是 | 真实姓名 |
| email | String | 是 | 邮箱 |
| phone | String | 否 | 手机号 |
| bio | String | 否 | 个人简介 |

**响应示例**:
```json
{
  "code": 200,
  "message": "个人资料更新成功",
  "data": {
    "id": 5,
    "updatedAt": "2024-01-21T10:00:00"
  }
}
```

---

### 15.3 修改密码

**接口地址**: `PUT /api/v1/users/change-password`

**业务说明**: 修改当前用户密码

**请求参数**:

```json
{
  "oldPassword": "oldPassword123",
  "newPassword": "newPassword456"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| oldPassword | String | 是 | 原密码 |
| newPassword | String | 是 | 新密码（需符合密码策略） |

**响应示例**:
```json
{
  "code": 200,
  "message": "密码修改成功"
}
```

**错误示例**:
```json
{
  "code": 400,
  "message": "原密码错误"
}
```

---

### 15.4 请求重置密码（邮件）

**接口地址**: `POST /api/v1/users/reset-password/request`

**业务说明**: 忘记密码时，通过邮箱请求重置密码

**请求参数**:

```json
{
  "email": "zhangsan@example.com"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| email | String | 是 | 注册邮箱 |

**响应示例**:
```json
{
  "code": 200,
  "message": "重置链接已发送到您的邮箱，请查收",
  "data": {
    "expiresIn": 1800
  }
}
```

**说明**：
- 发送包含重置token的邮件
- token有效期30分钟
- 需要验证邮箱是否存在

---

### 15.5 重置密码（邮件链接）

**接口地址**: `POST /api/v1/users/reset-password/confirm`

**业务说明**: 通过邮件中的重置链接重置密码

**请求参数**:

```json
{
  "token": "reset_token_from_email",
  "newPassword": "newPassword789"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| token | String | 是 | 邮件中的重置token |
| newPassword | String | 是 | 新密码 |

**响应示例**:
```json
{
  "code": 200,
  "message": "密码重置成功"
}
```

---

### 15.6 上传头像

**接口地址**: `POST /api/v1/users/avatar`

**业务说明**: 上传用户头像图片

**请求类型**: `multipart/form-data`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | 图片文件（支持jpg、png） |
| crop | Boolean | 否 | 是否自动裁剪，默认true |

**响应示例**:
```json
{
  "code": 200,
  "message": "头像上传成功",
  "data": {
    "avatar": "https://example.com/avatar/5_new.jpg",
    "url": "https://example.com/avatar/5_new.jpg"
  }
}
```

**说明**：
- 自动裁剪为正方形
- 生成缩略图
- 返回新的头像URL

---

## 16. 组织架构管理

**业务场景**: 组织树查询、组织下用户列表、组织详情

**Controller**: `OrganizationController`

### 16.1 查询组织架构树

**接口地址**: `GET /api/v1/organizations/tree`

**业务说明**: 获取完整的组织架构树（用于下拉选人）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| includeUsers | Boolean | 否 | 是否包含用户，默认false |
| includeDisabled | Boolean | 否 | 是否包含禁用组织，默认false |

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "总部",
      "code": "HQ",
      "parentId": null,
      "level": 1,
      "children": [
        {
          "id": 2,
          "name": "教务处",
          "code": "AA",
          "parentId": 1,
          "level": 2,
          "children": [
            {
              "id": 5,
              "name": "计算机学院",
              "code": "CS",
              "parentId": 2,
              "level": 3,
              "children": []
            }
          ]
        },
        {
          "id": 3,
          "name": "科研处",
          "code": "RD",
          "parentId": 1,
          "level": 2,
          "children": []
        }
      ]
    }
  ]
}
```

---

### 16.2 查询组织下用户列表

**接口地址**: `GET /api/v1/organizations/{id}/users`

**业务说明**: 查询指定组织下的所有用户

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 组织ID |

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |
| includeSubOrgs | Boolean | 否 | 是否包含子组织用户，默认false |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 10,
        "username": "lisi",
        "realName": "李四",
        "email": "lisi@example.com",
        "phone": "13800138001",
        "avatar": "https://example.com/avatar/10.jpg",
        "roles": ["ROLE_USER"]
      },
      {
        "id": 11,
        "username": "wangwu",
        "realName": "王五",
        "email": "wangwu@example.com",
        "phone": "13800138002",
        "avatar": "https://example.com/avatar/11.jpg",
        "roles": ["ROLE_USER"]
      }
    ],
    "totalElements": 25
  }
}
```

---

### 16.3 查询组织详情

**接口地址**: `GET /api/v1/organizations/{id}`

**业务说明**: 查询组织详细信息

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 组织ID |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "id": 5,
    "name": "计算机学院",
    "code": "CS",
    "parentId": 2,
    "parentName": "教务处",
    "level": 3,
    "type": "academic",
    "description": "负责计算机相关教学和科研工作",
    "leader": {
      "id": 10,
      "name": "李四"
    },
    "userCount": 25,
    "indicatorCount": 15,
    "createdAt": "2024-01-01T00:00:00"
  }
}
```

---

### 16.4 创建组织

**接口地址**: `POST /api/v1/organizations`

**业务说明**: 创建新的组织

**请求参数**:
```json
{
  "name": "计算机学院",
  "code": "CS",
  "type": "academic",
  "parentId": 2,
  "description": "负责计算机相关教学和科研工作",
  "sortOrder": 1
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 组织名称 |
| code | String | 是 | 组织编码 |
| type | String | 是 | 组织类型：admin/functional/academic |
| parentId | Long | 否 | 上级组织ID |
| description | String | 否 | 组织描述 |
| sortOrder | Integer | 否 | 排序顺序，默认0 |

**响应示例**:
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 5,
    "name": "计算机学院",
    "code": "CS",
    "type": "academic",
    "parentId": 2,
    "parentName": "教务处",
    "level": 3,
    "description": "负责计算机相关教学和科研工作",
    "sortOrder": 1,
    "isActive": true,
    "createdAt": "2024-01-01T00:00:00"
  }
}
```

---

### 16.5 更新组织类型

**接口地址**: `PUT /api/v1/organizations/{id}/type`

**业务说明**: 更新组织的类型

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 组织ID |

**请求参数**:
```json
{
  "type": "functional"
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| type | String | 是 | 新的组织类型：admin/functional/academic |

**响应示例**:
```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "id": 5,
    "name": "计算机学院",
    "code": "CS",
    "type": "functional",
    "parentId": 2,
    "parentName": "教务处",
    "level": 3,
    "description": "负责计算机相关教学和科研工作",
    "sortOrder": 1,
    "isActive": true,
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

---

## 17. 数据字典

**业务场景**: 字典数据查询（用于下拉框、筛选器）

**Controller**: `DictionaryController`

### 17.1 查询字典类型列表

**接口地址**: `GET /api/v1/dictionaries`

**业务说明**: 获取所有字典类型

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "type": "INDICATOR_TYPE",
      "typeName": "指标类型",
      "description": "指标的分类",
      "itemCount": 2
    },
    {
      "type": "INDICATOR_STATUS",
      "typeName": "指标状态",
      "description": "指标的当前状态",
      "itemCount": 4
    },
    {
      "type": "ORG_TYPE",
      "typeName": "组织类型",
      "description": "组织的类型分类",
      "itemCount": 3
    },
    {
      "type": "ORG_LEVEL",
      "typeName": "组织层级",
      "description": "组织的层级划分",
      "itemCount": 3
    }
  ]
}
```

---

### 17.2 查询字典项

**接口地址**: `GET /api/v1/dictionaries/{type}`

**业务说明**: 获取指定类型的字典项

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| type | String | 是 | 字典类型 |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "type": "INDICATOR_TYPE",
    "typeName": "指标类型",
    "items": [
      {
        "code": "QUALITATIVE",
        "name": "定性指标",
        "value": "1",
        "sort": 1,
        "enabled": true
      },
      {
        "code": "QUANTITATIVE",
        "name": "定量指标",
        "value": "2",
        "sort": 2,
        "enabled": true
      }
    ]
  }
}
```

**常用字典类型**:

| 类型 | 说明 | 示例项 |
|------|------|--------|
| INDICATOR_TYPE | 指标类型 | 定性、定量 |
| INDICATOR_STATUS | 指标状态 | 草稿、已发布、已撤回 |
| INDICATOR_LEVEL | 指标层级 | 一级、二级、三级 |
| ORG_TYPE | 组织类型 | admin, functional, academic |
| ORG_LEVEL | 组织层级 | 总部、部门、学院 |
| APPROVAL_STATUS | 审批状态 | 待审批、已通过、已驳回 |
| NOTIFICATION_TYPE | 通知类型 | 下发通知、催办通知、系统通知 |

**查询组织类型字典项的响应示例**:
```json
{
  "code": 200,
  "data": {
    "type": "ORG_TYPE",
    "typeName": "组织类型",
    "items": [
      {
        "code": "admin",
        "name": "系统管理",
        "value": "admin",
        "sort": 1,
        "enabled": true
      },
      {
        "code": "functional",
        "name": "职能部门",
        "value": "functional",
        "sort": 2,
        "enabled": true
      },
      {
        "code": "academic",
        "name": "二级学院",
        "value": "academic",
        "sort": 3,
        "enabled": true
      }
    ]
  }
}
```

**组织类型映射说明**:
| 旧类型 | 新类型 | 说明 |
|-------|--------|------|
| SCHOOL | admin | 校级组织 |
| STRATEGY_DEPT | admin | 战略发展部 |
| FUNCTIONAL_DEPT | functional | 职能部门 |
| FUNCTION_DEPT | functional | 职能部门（别名） |
| OTHER | functional | 其他类型 |
| COLLEGE | academic | 二级学院 |
| DIVISION | academic | 学部（学院下属） |

---

## 18. 系统配置

**业务场景**: 系统参数配置、业务规则设置

**Controller**: `SystemConfigController`

### 18.1 查询系统配置

**接口地址**: `GET /api/v1/system/config`

**业务说明**: 获取系统配置参数

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keys | String | 否 | 配置键列表，逗号分隔（不传则返回所有） |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "system": {
      "name": "绩效管理系统",
      "version": "v1.0",
      "copyright": "2024"
    },
    "business": {
      "reportLockDay": 25,
      "reportLockTime": "23:59:59",
      "autoReminderDays": 3,
      "approvalTimeoutDays": 5
    },
    "features": {
      "enableNotification": true,
      "enableEmail": true,
      "enableSms": false,
      "enableWebSocket": true
    },
    "limits": {
      "maxUploadFileSize": 10485760,
      "maxIndicatorsPerPlan": 50,
      "maxAttachmentCount": 10
    }
  }
}
```

---

### 18.2 更新系统配置

**接口地址**: `PUT /api/v1/system/config`

**业务说明**: 更新系统配置（需要管理员权限）

**请求参数**:

```json
{
  "reportLockDay": 26,
  "autoReminderDays": 5,
  "enableNotification": false
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| reportLockDay | Integer | 否 | 报告锁定日期（每月几号） |
| reportLockTime | String | 否 | 报告锁定时间 |
| autoReminderDays | Integer | 否 | 自动催办提前天数 |
| approvalTimeoutDays | Integer | 否 | 审批超时天数 |
| enableNotification | Boolean | 否 | 是否启用通知 |
| enableEmail | Boolean | 否 | 是否启用邮件 |
| enableSms | Boolean | 否 | 是否启用短信 |

**响应示例**:
```json
{
  "code": 200,
  "message": "系统配置更新成功",
  "data": {
    "updatedKeys": ["reportLockDay", "autoReminderDays"],
    "updatedAt": "2024-01-21T10:00:00"
  }
}
```

---

### 18.3 刷新配置缓存

**接口地址**: `POST /api/v1/system/config/refresh`

**业务说明**: 刷新系统配置缓存（配置更新后调用）

**响应示例**:
```json
{
  "code": 200,
  "message": "配置缓存已刷新",
  "data": {
    "refreshedAt": "2024-01-21T10:00:00"
  }
}
```

---

## 附录A：通用说明

### 数据格式

所有接口请求和响应均使用 JSON 格式，Content-Type 为 `application/json`

**通用响应结构**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {},
  "timestamp": "2024-01-21T10:00:00"
}
```

**状态码说明**:

| Code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

---

### 认证方式

使用 Bearer Token (JWT) 进行认证：

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

### 分页参数

列表接口默认支持分页：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| page | 0 | 页码（从0开始） |
| size | 20 | 每页数量 |
| sort | createdAt | 排序字段 |
| order | desc | 排序方向（asc/desc） |

---

### 时间格式

所有时间字段使用 ISO 8601 格式：

```
2024-01-21T10:00:00
```

日期格式：

```
2024-01-21
```

---

### 错误响应

```json
{
  "code": 400,
  "message": "请求参数错误",
  "errors": [
    {
      "field": "name",
      "message": "不能为空"
    }
  ],
  "timestamp": "2024-01-21T10:00:00"
}
```

---

## 附录B：使用指南

### 开发流程

1. **接口文档确认**：根据本文档确认接口规范
2. **Controller 开发**：在对应 Controller 中实现接口
3. **Service 层实现**：
   - 审批工作流引擎：通用审批逻辑
   - 业务工作流：业务流程逻辑，调用审批工作流引擎
4. **Swagger 注解**：添加 `@Operation` 注解完善文档
5. **单元测试**：编写接口单元测试
6. **集成测试**：验证接口业务逻辑
7. **Swagger 验收**：确保 Swagger UI 中可以看到所有接口

### 模块协作示例

**示例1：指标提交审批流程**

```java
// 1. 业务工作流层 - IndicatorController
@PostMapping("/indicators/{id}/submit-approval")
public Result<?> submitIndicatorApproval(@PathVariable Long id, @RequestBody SubmitApprovalRequest request) {
    // 准备审批数据
    ApprovalCreateRequest approvalRequest = new ApprovalCreateRequest();
    approvalRequest.setFlowCode("INDICATOR_APPROVAL");
    approvalRequest.setEntityType("INDICATOR");
    approvalRequest.setEntityId(String.valueOf(id));
    approvalRequest.setComment(request.getComment());

    // 调用审批工作流引擎
    return approvalWorkflowService.createApprovalInstance(approvalRequest);
}

// 2. 审批工作流引擎 - ApprovalWorkflowService
public ApprovalInstance createApprovalInstance(ApprovalCreateRequest request) {
    // 根据flowCode获取审批流定义
    ApprovalFlow flow = flowService.getFlowByCode(request.getFlowCode());

    // 创建审批实例
    ApprovalInstance instance = new ApprovalInstance();
    instance.setFlowId(flow.getId());
    instance.setEntityType(request.getEntityType());
    instance.setEntityId(request.getEntityId());
    // ...

    // 初始化审批步骤
    initApprovalSteps(instance, flow.getSteps());

    return instanceRepository.save(instance);
}
```

### Swagger 使用

访问 Swagger UI：

```
http://localhost:8080/swagger-ui.html
```

### 接口版本管理

- 当前版本：v1.0
- 基础路径：`/api/v1`
- 版本控制：通过 URL 路径（如 `/api/v2/xxx`）

---

**文档版本**: v2.1
**最后更新**: 2026-03-13
**维护者**: 开发团队
**架构说明**: 本文档采用分层架构，审批工作流引擎提供通用审批能力，业务工作流负责具体业务流程并调用审批工作流引擎。

**更新内容**:
- 更新指标管理模块字段，将 `responsible_person` 替换为 `responsible_user_id`（关联 sys_user 表）