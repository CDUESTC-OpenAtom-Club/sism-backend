# Requirements Document

## Introduction

本功能旨在优化 SISM 系统的 API 层类型安全性，主要包括三个方面：
1. 统一 Node.js API 的字段命名风格（将 org API 从 snake_case 转换为 camelCase）
2. 添加 TypeScript 严格类型校验
3. 引入 Zod 进行运行时类型验证

这些改进将提高代码的可维护性、减少类型相关的 bug，并确保前后端数据结构的一致性。

## Glossary

- **Org_API**: 组织机构 API，位于 `server/routes/org.js`，负责返回部门/组织数据
- **OrgVO**: 组织机构值对象，API 返回的组织数据结构
- **Zod_Schema**: Zod 库定义的运行时类型验证模式
- **camelCase**: 驼峰命名法，如 `orgId`、`orgName`
- **snake_case**: 下划线命名法，如 `org_id`、`org_name`
- **Runtime_Validation**: 运行时类型验证，在程序执行时检查数据类型是否符合预期
- **API_Response**: API 响应对象，包含 success、data、message、timestamp 字段

## Requirements

### Requirement 1: Org API 字段命名统一

**User Story:** As a 前端开发者, I want org API 返回 camelCase 格式的字段, so that 与其他 API 保持一致，减少前端转换逻辑。

#### Acceptance Criteria

1. WHEN Org_API 返回组织列表 THEN THE Org_API SHALL 返回 camelCase 格式的字段（orgId, orgName, orgType, parentOrgId, isActive, sortOrder）
2. WHEN Org_API 返回单个组织 THEN THE Org_API SHALL 返回 camelCase 格式的字段
3. WHEN Org_API 返回数据 THEN THE Org_API SHALL 使用标准 ApiResponse 格式包装（success, data, message, timestamp）
4. WHEN 数据库返回 snake_case 字段 THEN THE Org_API SHALL 通过 convertToOrgVO 函数转换为 camelCase

### Requirement 2: TypeScript 类型定义优化

**User Story:** As a 前端开发者, I want 完整的 TypeScript 类型定义, so that 编译时能捕获类型错误。

#### Acceptance Criteria

1. THE OrgVO_Interface SHALL 定义所有 camelCase 字段及其类型（orgId: number, orgName: string, orgType: OrgType, parentOrgId: number | null, isActive: boolean, sortOrder: number, remark: string | null, createdAt: string, updatedAt: string）
2. THE OrgType_Enum SHALL 定义所有有效的组织类型（STRATEGY_DEPT, FUNCTIONAL_DEPT, COLLEGE, DIVISION, SCHOOL, OTHER, SECONDARY_COLLEGE）
3. WHEN 前端调用 org API THEN THE Frontend_Code SHALL 使用 OrgVO 类型进行类型检查
4. THE Department_Interface SHALL 保持现有定义，作为前端业务层使用的类型

### Requirement 3: Zod 运行时类型验证

**User Story:** As a 系统开发者, I want API 响应的运行时类型验证, so that 能在运行时捕获数据格式错误。

#### Acceptance Criteria

1. THE Zod_Schema SHALL 定义 OrgVO 的完整验证模式
2. THE Zod_Schema SHALL 定义 ApiResponse<OrgVO[]> 的验证模式
3. WHEN API 响应数据不符合 schema THEN THE Validation SHALL 返回详细的错误信息
4. WHEN API 响应数据符合 schema THEN THE Validation SHALL 返回类型安全的数据对象
5. THE Zod_Schema SHALL 与 TypeScript 类型定义保持同步（使用 z.infer 推导类型）

### Requirement 4: 前端 API 层适配

**User Story:** As a 前端开发者, I want 前端 API 层自动处理类型验证, so that 业务代码无需关心验证逻辑。

#### Acceptance Criteria

1. WHEN 调用 orgApi.getAllOrgs() THEN THE API_Layer SHALL 使用 Zod 验证响应数据
2. WHEN Zod 验证失败 THEN THE API_Layer SHALL 记录错误日志并返回空数组或抛出异常
3. WHEN Zod 验证成功 THEN THE API_Layer SHALL 返回类型安全的 OrgVO 数组
4. THE convertOrgVOToDepartment 函数 SHALL 接收 camelCase 格式的 OrgVO（移除 snake_case 兼容代码）

### Requirement 5: 向后兼容性

**User Story:** As a 系统管理员, I want API 变更不影响现有功能, so that 系统稳定运行。

#### Acceptance Criteria

1. WHEN 前端代码使用新的 OrgVO 格式 THEN THE System SHALL 正常显示部门列表
2. WHEN 部门切换器加载数据 THEN THE System SHALL 正确显示所有部门
3. IF 后端返回意外格式 THEN THE Frontend SHALL 优雅降级并记录错误日志
4. THE Migration SHALL 确保所有使用 org API 的组件正常工作

### Requirement 6: 测试覆盖

**User Story:** As a 开发者, I want 完整的测试覆盖, so that 确保类型安全改进的正确性。

#### Acceptance Criteria

1. THE Property_Tests SHALL 验证 Zod schema 与 TypeScript 类型的一致性
2. THE Property_Tests SHALL 验证 snake_case 到 camelCase 转换的正确性
3. THE Unit_Tests SHALL 验证 convertToOrgVO 函数的边界情况
4. THE Unit_Tests SHALL 验证 Zod 验证失败时的错误处理
