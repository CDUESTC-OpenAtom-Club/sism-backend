# Design Document: API Response Type Safety

## Overview

本设计文档描述了 SISM 系统 API 层类型安全优化的技术实现方案。主要目标是：

1. **统一字段命名风格**：将 org API 从 snake_case 转换为 camelCase，与 indicator API 保持一致
2. **TypeScript 类型强化**：定义完整的 API 响应类型接口
3. **Zod 运行时验证**：引入 Zod 库进行 API 响应的运行时类型验证

### 设计原则

- **最小改动原则**：仅修改必要的文件，保持现有架构
- **渐进式迁移**：先更新后端 API，再更新前端类型和验证
- **类型推导优先**：使用 `z.infer` 从 Zod schema 推导 TypeScript 类型，确保一致性

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend (Vue 3)                          │
├─────────────────────────────────────────────────────────────────┤
│  src/api/org.ts                                                  │
│  ┌─────────────────┐    ┌─────────────────┐                     │
│  │  orgApi         │───▶│  Zod Validation │                     │
│  │  getAllOrgs()   │    │  orgVOSchema    │                     │
│  └─────────────────┘    └─────────────────┘                     │
│           │                      │                               │
│           ▼                      ▼                               │
│  ┌─────────────────┐    ┌─────────────────┐                     │
│  │  OrgVO (type)   │◀───│  z.infer<>      │                     │
│  │  camelCase      │    │  Type Inference │                     │
│  └─────────────────┘    └─────────────────┘                     │
├─────────────────────────────────────────────────────────────────┤
│                        HTTP Request                              │
├─────────────────────────────────────────────────────────────────┤
│                    Node.js API Server                            │
├─────────────────────────────────────────────────────────────────┤
│  server/routes/org.js                                            │
│  ┌─────────────────┐    ┌─────────────────┐                     │
│  │  Database Query │───▶│ convertToOrgVO  │                     │
│  │  snake_case     │    │ snake→camelCase │                     │
│  └─────────────────┘    └─────────────────┘                     │
│                                │                                 │
│                                ▼                                 │
│                      ┌─────────────────┐                        │
│                      │  ApiResponse    │                        │
│                      │  { success,     │                        │
│                      │    data,        │                        │
│                      │    message,     │                        │
│                      │    timestamp }  │                        │
│                      └─────────────────┘                        │
└─────────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. 后端组件 (Node.js API)

#### 1.1 convertToOrgVO 函数

位置：`server/routes/org.js`

```javascript
/**
 * 将数据库行转换为驼峰格式的 OrgVO
 * @param {Object} row - 数据库查询结果行 (snake_case)
 * @returns {OrgVO} - 驼峰格式的组织 VO
 */
function convertToOrgVO(row) {
  return {
    orgId: parseInt(row.org_id),
    orgName: row.org_name,
    orgType: row.org_type,
    parentOrgId: row.parent_org_id ? parseInt(row.parent_org_id) : null,
    isActive: row.is_active ?? true,
    sortOrder: row.sort_order ?? 0,
    remark: row.remark ?? null,
    createdAt: row.created_at,
    updatedAt: row.updated_at
  };
}
```

#### 1.2 API 响应格式

所有 org API 端点将返回标准 ApiResponse 格式：

```javascript
// GET /api/orgs
{
  success: true,
  data: OrgVO[],
  message: 'OK',
  timestamp: Date
}

// GET /api/orgs/:id
{
  success: true,
  data: OrgVO,
  message: 'OK',
  timestamp: Date
}
```

### 2. 前端组件

#### 2.1 Zod Schema 定义

位置：`src/api/schemas/org.schema.ts`（新文件）

```typescript
import { z } from 'zod';

// OrgType 枚举 schema
export const orgTypeSchema = z.enum([
  'STRATEGY_DEPT',
  'FUNCTIONAL_DEPT', 
  'COLLEGE',
  'DIVISION',
  'SCHOOL',
  'OTHER',
  'SECONDARY_COLLEGE'
]);

// OrgVO schema
export const orgVOSchema = z.object({
  orgId: z.number(),
  orgName: z.string(),
  orgType: orgTypeSchema,
  parentOrgId: z.number().nullable(),
  isActive: z.boolean(),
  sortOrder: z.number(),
  remark: z.string().nullable(),
  createdAt: z.string(),
  updatedAt: z.string()
});

// ApiResponse<OrgVO[]> schema
export const orgListResponseSchema = z.object({
  success: z.boolean(),
  data: z.array(orgVOSchema),
  message: z.string(),
  timestamp: z.string().or(z.date())
});

// 从 schema 推导类型
export type OrgType = z.infer<typeof orgTypeSchema>;
export type OrgVO = z.infer<typeof orgVOSchema>;
export type OrgListResponse = z.infer<typeof orgListResponseSchema>;
```

#### 2.2 更新后的 org API

位置：`src/api/org.ts`

```typescript
import { orgListResponseSchema, type OrgVO } from './schemas/org.schema';

export const orgApi = {
  async getAllOrgs(): Promise<OrgVO[]> {
    const response = await apiService.get('/orgs');
    
    // Zod 运行时验证
    const result = orgListResponseSchema.safeParse(response);
    
    if (!result.success) {
      console.error('[Org API] 响应验证失败:', result.error);
      return [];
    }
    
    return result.data.data;
  }
};
```

### 3. 类型定义更新

#### 3.1 OrgVO 接口（由 Zod 推导）

```typescript
// 从 Zod schema 推导，无需手动定义
export type OrgVO = z.infer<typeof orgVOSchema>;

// 等价于：
interface OrgVO {
  orgId: number;
  orgName: string;
  orgType: 'STRATEGY_DEPT' | 'FUNCTIONAL_DEPT' | 'COLLEGE' | 'DIVISION' | 'SCHOOL' | 'OTHER' | 'SECONDARY_COLLEGE';
  parentOrgId: number | null;
  isActive: boolean;
  sortOrder: number;
  remark: string | null;
  createdAt: string;
  updatedAt: string;
}
```

#### 3.2 Department 接口（保持不变）

```typescript
// 前端业务层使用的类型，保持不变
export interface Department {
  id: string;
  name: string;
  type: 'strategic_dept' | 'functional_dept' | 'secondary_college';
  sortOrder: number;
}
```

## Data Models

### 数据流转换

```
Database (snake_case)     →    Node.js API (camelCase)    →    Frontend (camelCase)
─────────────────────          ─────────────────────          ─────────────────────
org_id: BIGINT                 orgId: number                  orgId: number
org_name: VARCHAR              orgName: string                orgName: string
org_type: VARCHAR              orgType: OrgType               orgType: OrgType
parent_org_id: BIGINT          parentOrgId: number | null     parentOrgId: number | null
is_active: BOOLEAN             isActive: boolean              isActive: boolean
sort_order: INTEGER            sortOrder: number              sortOrder: number
remark: TEXT                   remark: string | null          remark: string | null
created_at: TIMESTAMP          createdAt: string              createdAt: string
updated_at: TIMESTAMP          updatedAt: string              updatedAt: string
```

### OrgType 枚举映射

| 后端 OrgType | 前端 Department.type |
|-------------|---------------------|
| STRATEGY_DEPT | strategic_dept |
| SCHOOL | strategic_dept |
| FUNCTIONAL_DEPT | functional_dept |
| COLLEGE | secondary_college |
| SECONDARY_COLLEGE | secondary_college |
| DIVISION | secondary_college |
| OTHER | secondary_college |



## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

Based on the prework analysis, the following properties have been identified for property-based testing:

### Property 1: snake_case to camelCase Conversion Preserves Data

*For any* valid database row with snake_case fields (org_id, org_name, org_type, parent_org_id, is_active, sort_order, remark, created_at, updated_at), when converted by `convertToOrgVO`, the resulting object SHALL have:
- All values preserved (orgId equals org_id, orgName equals org_name, etc.)
- All keys in camelCase format
- Correct type coercion (org_id string → orgId number)

**Validates: Requirements 1.1, 1.2, 1.4**

### Property 2: API Response Structure Validation

*For any* org API response, the response object SHALL contain:
- `success` field of type boolean
- `data` field containing OrgVO array or single OrgVO
- `message` field of type string
- `timestamp` field of type string (ISO format) or Date

**Validates: Requirements 1.3**

### Property 3: Zod Validation Rejects Invalid Data

*For any* object that does not conform to the OrgVO schema (missing required fields, wrong types, invalid enum values), the Zod validation SHALL:
- Return `success: false` from `safeParse`
- Provide error details in the `error` field
- Not throw an exception

**Validates: Requirements 3.3**

### Property 4: Zod Validation Accepts Valid Data

*For any* object that conforms to the OrgVO schema (all required fields present with correct types), the Zod validation SHALL:
- Return `success: true` from `safeParse`
- Return the validated data in the `data` field
- The returned data SHALL be type-safe (TypeScript infers correct types)

**Validates: Requirements 3.4, 3.5**

### Property 5: OrgVO to Department Conversion Correctness

*For any* valid OrgVO object with camelCase fields, when converted by `convertOrgVOToDepartment`, the resulting Department object SHALL have:
- `id` equal to `String(orgVO.orgId)`
- `name` equal to `orgVO.orgName`
- `type` correctly mapped from `orgVO.orgType` (STRATEGY_DEPT/SCHOOL → strategic_dept, FUNCTIONAL_DEPT → functional_dept, others → secondary_college)
- `sortOrder` equal to `orgVO.sortOrder`

**Validates: Requirements 4.4**

## Error Handling

### 后端错误处理

| 错误场景 | HTTP 状态码 | 响应格式 |
|---------|------------|---------|
| 数据库查询失败 | 500 | `{ success: false, data: null, message: '服务器错误', timestamp }` |
| 组织不存在 | 404 | `{ success: false, data: null, message: '组织不存在', timestamp }` |
| 参数验证失败 | 400 | `{ success: false, data: null, message: '参数错误', timestamp }` |

### 前端错误处理

```typescript
// Zod 验证失败处理
const result = orgListResponseSchema.safeParse(response);
if (!result.success) {
  console.error('[Org API] 响应验证失败:', result.error.format());
  // 优雅降级：返回空数组
  return [];
}

// 网络错误处理
try {
  const response = await apiService.get('/orgs');
  // ...
} catch (error) {
  console.error('[Org API] 网络请求失败:', error);
  return [];
}
```

### 降级策略

1. **Zod 验证失败**：记录错误日志，返回空数组，不影响页面渲染
2. **网络请求失败**：使用 fallback 服务或缓存数据
3. **部分字段缺失**：Zod schema 使用 `.optional()` 或 `.default()` 处理

## Testing Strategy

### 测试框架

- **单元测试**: Vitest
- **属性测试**: fast-check
- **测试位置**: `tests/property/` 和 `tests/unit/`

### 属性测试配置

```typescript
import fc from 'fast-check';

// 每个属性测试运行 100 次迭代
const testConfig = { numRuns: 100 };
```

### 测试文件结构

```
tests/
├── property/
│   └── org-api-type-safety.property.test.ts  # 属性测试
└── unit/
    └── org-api.unit.test.ts                   # 单元测试
```

### 属性测试实现

每个 Correctness Property 对应一个属性测试：

1. **Property 1**: 生成随机 snake_case 对象，验证转换后的 camelCase 对象值相等
2. **Property 2**: 生成随机 API 响应，验证结构符合 ApiResponse 格式
3. **Property 3**: 生成随机无效对象，验证 Zod 拒绝并返回错误
4. **Property 4**: 生成随机有效 OrgVO，验证 Zod 接受并返回数据
5. **Property 5**: 生成随机 OrgVO，验证 Department 转换正确

### 单元测试覆盖

- `convertToOrgVO` 边界情况（null 值、缺失字段）
- `convertOrgVOToDepartment` 所有 OrgType 映射
- Zod schema 边界情况（空字符串、负数、特殊字符）
- API 错误响应处理

### 测试标签格式

```typescript
// Feature: api-response-type-safety, Property 1: snake_case to camelCase conversion
it.prop([orgRowArbitrary], testConfig)('Property 1: conversion preserves data', (row) => {
  // ...
});
```
