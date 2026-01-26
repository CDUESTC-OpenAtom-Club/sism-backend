# Implementation Plan: API Response Type Safety

## Overview

本实现计划将 API 响应类型安全优化分解为可执行的编码任务。主要工作包括：
1. 更新后端 org API 返回 camelCase 格式
2. 添加 Zod 依赖并创建验证 schema
3. 更新前端 API 层使用 Zod 验证
4. 编写属性测试验证正确性

## Tasks

- [x] 1. 安装 Zod 依赖并创建 schema 文件
  - [x] 1.1 安装 Zod 依赖到前端项目
    - 运行 `npm install zod` 在 strategic-task-management 目录
    - _Requirements: 3.1_
  
  - [x] 1.2 创建 Zod schema 文件 `src/api/schemas/org.schema.ts`
    - 定义 `orgTypeSchema` 枚举 schema
    - 定义 `orgVOSchema` 对象 schema
    - 定义 `orgListResponseSchema` API 响应 schema
    - 使用 `z.infer` 导出 TypeScript 类型
    - _Requirements: 3.1, 3.2, 3.5_

- [x] 2. 更新后端 org API 返回 camelCase 格式
  - [x] 2.1 在 `server/routes/org.js` 中添加 `convertToOrgVO` 函数
    - 实现 snake_case 到 camelCase 的字段转换
    - 处理 null 值和类型转换
    - _Requirements: 1.4_
  
  - [x] 2.2 更新 GET /api/orgs 端点返回标准 ApiResponse 格式
    - 使用 `convertToOrgVO` 转换每条记录
    - 返回 `{ success, data, message, timestamp }` 格式
    - _Requirements: 1.1, 1.3_
  
  - [x] 2.3 更新 GET /api/orgs/:id 端点返回标准 ApiResponse 格式
    - 使用 `convertToOrgVO` 转换单条记录
    - 返回 `{ success, data, message, timestamp }` 格式
    - _Requirements: 1.2, 1.3_

- [x] 3. Checkpoint - 验证后端 API 变更
  - 确保后端 API 返回 camelCase 格式
  - 手动测试 `/api/orgs` 和 `/api/orgs/:id` 端点
  - 确保所有测试通过，如有问题请询问用户

- [x] 4. 更新前端 API 层使用 Zod 验证
  - [x] 4.1 更新 `src/api/org.ts` 中的 OrgVO 接口
    - 从 `./schemas/org.schema` 导入类型
    - 移除旧的 snake_case OrgVO 接口定义
    - _Requirements: 2.1, 2.3_
  
  - [x] 4.2 更新 `getAllOrgs` 方法使用 Zod 验证
    - 使用 `orgListResponseSchema.safeParse` 验证响应
    - 验证失败时记录错误并返回空数组
    - _Requirements: 4.1, 4.2, 4.3_
  
  - [x] 4.3 更新 `convertOrgVOToDepartment` 函数
    - 修改为接收 camelCase 格式的 OrgVO
    - 移除 snake_case 兼容代码
    - _Requirements: 4.4_

- [x] 5. Checkpoint - 验证前端集成
  - 确保前端能正确加载部门数据
  - 验证部门切换器正常工作
  - 确保所有测试通过，如有问题请询问用户

- [x] 6. 编写属性测试
  - [x]* 6.1 创建属性测试文件 `tests/property/org-api-type-safety.property.test.ts`
    - 设置 fast-check 测试配置（100 次迭代）
    - 创建测试数据生成器（arbitrary）
    - _Requirements: 6.1, 6.2_
  
  - [x]* 6.2 实现 Property 1: snake_case to camelCase 转换测试
    - **Property 1: snake_case to camelCase Conversion Preserves Data**
    - 生成随机 snake_case 对象，验证转换后值相等
    - **Validates: Requirements 1.1, 1.2, 1.4**
  
  - [x]* 6.3 实现 Property 3: Zod 验证拒绝无效数据测试
    - **Property 3: Zod Validation Rejects Invalid Data**
    - 生成随机无效对象，验证 Zod 返回错误
    - **Validates: Requirements 3.3**
  
  - [x]* 6.4 实现 Property 4: Zod 验证接受有效数据测试
    - **Property 4: Zod Validation Accepts Valid Data**
    - 生成随机有效 OrgVO，验证 Zod 接受
    - **Validates: Requirements 3.4, 3.5**
  
  - [x]* 6.5 实现 Property 5: OrgVO 到 Department 转换测试
    - **Property 5: OrgVO to Department Conversion Correctness**
    - 生成随机 OrgVO，验证 Department 转换正确
    - **Validates: Requirements 4.4**

- [x] 7. 编写单元测试
  - [x]* 7.1 创建单元测试文件 `tests/unit/org-api.unit.test.ts`
    - 测试 `convertToOrgVO` 边界情况（null 值、缺失字段）
    - 测试 `convertOrgVOToDepartment` 所有 OrgType 映射
    - _Requirements: 6.3, 6.4_

- [x] 8. Final Checkpoint - 确保所有测试通过
  - 运行 `npm run test` 验证所有测试通过
  - 确保类型检查通过 `npm run build:check`
  - 如有问题请询问用户

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 后端 API 变更需要先完成，前端才能适配
- Zod schema 是类型安全的核心，需要与 TypeScript 类型保持同步
- 属性测试使用 fast-check 库，每个测试运行 100 次迭代
