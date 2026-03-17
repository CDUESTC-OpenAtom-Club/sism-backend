# SISM Backend OpenAPI 文档

**生成时间**: 2026-03-17
**API版本**: 1.0.0
**OpenAPI版本**: 3.0.1
**端点数量**: 181个

---

## 快速开始

### 1. 在线访问

**Swagger UI**: http://localhost:8080/api/swagger-ui.html

**OpenAPI JSON**: http://localhost:8080/api-docs

### 2. 本地文件

```
docs/openapi/
├── openapi-full.json  # 完整的OpenAPI 3.0 JSON规范（12,185行）
├── openapi-full.yaml  # 完整的OpenAPI YAML规范（需要yq工具）
└── README.md          # 本说明文档
```

---

## API端点分组（17个Tag）

| Tag | 端点数 | 说明 |
|-----|-------|------|
| Authentication | 5+ | 用户认证和授权 |
| Indicators | 20+ | 指标管理（创建、下发、审批） |
| Workflows | 15+ | 审批工作流引擎 |
| Tasks | 10+ | 战略任务管理 |
| Plans | 10+ | 计划管理 |
| Reports | 10+ | 进度填报和报告 |
| Organizations | 8+ | 组织架构管理 |
| Cycles | 5+ | 评估周期管理 |
| Milestones | 8+ | 里程碑管理 |
| Analytics Dashboards | 10+ | 数据看板 |
| Analytics Data Exports | 10+ | 数据导出 |
| Analytics Reports | 8+ | 分析报告 |
| User Profile | 5+ | 用户信息管理 |
| Role Management | 6+ | 角色权限管理 |
| Notifications | 5+ | 通知消息 |
| Alerts | 8+ | 预警管理 |
| Business Workflows | 10+ | 业务工作流 |

---

## 认证方式

所有API端点（除认证接口外）都需要JWT Bearer Token认证：

```http
Authorization: Bearer <your-jwt-token>
```

**获取Token**:
```bash
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123",
  "captcha": "1234",
  "captchaKey": "test-key"
}
```

---

## 导入到工具

### Postman

1. 打开Postman
2. 点击 `Import` 按钮
3. 选择 `File` 标签
4. 上传 `docs/openapi/openapi-full.json`
5. Postman会自动创建所有API请求

### Swagger UI

直接访问 http://localhost:8080/api/swagger-ui.html

### VS Code REST Client

1. 安装 `Humao REST Client` 扩展
2. 创建新文件 `test.http`
3. 引入OpenAPI文档：
```http
# @name login
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

### 在线工具

- **Swagger Editor**: https://editor.swagger.io/
- **Stoplight Studio**: https://stoplight.io/studio/

---

## 核心业务流程示例

### 1. 指标下发流程

```http
# 1. 创建指标（草稿）
POST /api/v1/indicators
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "indicatorName": "教学质量指标",
  "description": "教学质量评估指标",
  "type": "QUANTITATIVE",
  "weightPercent": 0.3
}

# 2. 提交审批
POST /api/v1/indicators/{id}/submit-approval
Authorization: Bearer {{token}}

# 3. 下发指标
POST /api/v1/indicators/{id}/distribute
Authorization: Bearer {{token}}

# 4. 查询下发状态
GET /api/v1/indicators/{id}/distribution-status
Authorization: Bearer {{token}}
```

### 2. 进度填报与审批

```http
# 1. 创建填报报告
POST /api/v1/reports
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "planId": 1,
  "reportMonth": "202603",
  "reportOrgType": "COLLEGE",
  "reportOrgId": 1,
  "indicators": [
    {
      "indicatorId": 1,
      "progressValue": 85,
      "evidence": "已完成80%的教学任务"
    }
  ]
}

# 2. 提交报告
POST /api/v1/reports/{id}/submit
Authorization: Bearer {{token}}

# 3. 审批通过
POST /api/v1/approval/instances/{instanceId}/approve
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "comment": "审批通过，进度符合要求"
}

# 4. 驳回
POST /api/v1/approval/instances/{instanceId}/reject
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "comment": "需要补充更多支撑材料"
}
```

### 3. 审批时间轴查询

```http
# 查询审批实例详情
GET /api/v1/approval/instances/{instanceId}
Authorization: Bearer {{token}}

# Response包含完整的审批时间轴
{
  "instance": {
    "id": 123,
    "status": "APPROVED",
    "currentStep": 2,
    "steps": [
      {
        "stepName": "学院审批",
        "approver": "张三",
        "status": "APPROVED",
        "comment": "同意",
        "createdAt": "2026-03-15T10:00:00"
      },
      {
        "stepName": "职能部门审批",
        "approver": "李四",
        "status": "APPROVED",
        "comment": "复核通过",
        "createdAt": "2026-03-16T14:30:00"
      }
    ]
  }
}
```

---

## 错误码说明

| HTTP状态码 | 业务错误码 | 说明 |
|-----------|----------|------|
| 200 | - | 成功 |
| 401 | 2000-2010 | 未认证、Token过期、密码错误 |
| 403 | 2005, 3005, 5009 | 无权限 |
| 404 | 1002, 3000, 4000, 5000 | 资源不存在 |
| 409 | 1003, 1009, 3001, 4001 | 资源冲突、版本冲突 |
| 422 | 1004, 4002, 4003, 5004 | 业务逻辑验证失败 |

完整错误码列表请参考 `docs/API接口文档.md`

---

## 数据模型

### Indicator（指标）

```json
{
  "id": 1,
  "indicatorDesc": "教学质量指标",
  "status": "DISTRIBUTED",
  "progress": 75,
  "weightPercent": 0.3,
  "parentIndicatorId": null,
  "ownerOrgId": 1,
  "targetOrgId": 2,
  "responsibleUserId": 10
}
```

### AuditInstance（审批实例）

```json
{
  "id": 123,
  "entityId": 1,
  "entityType": "INDICATOR_DISTRIBUTE",
  "status": "PENDING",
  "currentStepId": 456,
  "requesterId": 10,
  "requesterOrgId": 1
}
```

### PlanReport（填报报告）

```json
{
  "id": 1,
  "planId": 1,
  "reportMonth": "202603",
  "reportOrgType": "COLLEGE",
  "status": "DRAFT",
  "submittedAt": "2026-03-15T10:00:00"
}
```

---

## 开发建议

### 前端集成

1. **使用Swagger UI生成的TypeScript客户端**:
   ```bash
   npm install -g @openapitools/openapi-generator-cli
   openapi-generator-cli generate -i docs/openapi/openapi-full.json \
     -g typescript-axios -o src/api/generated
   ```

2. **使用AutoRest生成Python客户端**:
   ```bash
   autorest --input-file=docs/openapi/openapi-full.json \
     --language=python --output-folder=generated
   ```

### Mock Server

使用Postman创建Mock Server：

1. 导入OpenAPI文档到Postman
2. 创建Collection并配置Mock Server
3. 前端可以基于Mock进行开发

---

## 更新日志

### v1.0.0 (2026-03-17)

- ✅ 初始版本
- ✅ 181个API端点
- ✅ 17个业务模块
- ✅ JWT Bearer认证
- ✅ 完整的请求/响应Schema

---

## 联系方式

**开发团队**: SISM Development Team
**邮箱**: support@sism.example.com
**文档**: https://sism.example.com/docs

---

## 许可证

Proprietary - https://sism.example.com/license
