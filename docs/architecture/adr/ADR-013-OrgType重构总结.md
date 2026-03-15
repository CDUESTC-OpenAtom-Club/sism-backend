# ADR-013: OrgType 统一化重构总结

**日期**: 2025-01-21
**状态**: 进行中
**作者**: Claude Assistant

---

## 概述

本文档记录了将组织类型 `OrgType` 从 7 种类型统一为 3 种类型的重构过程。

**最终目标**: 将组织类型严格统一为：
- **`ADMIN`** - 系统管理层（对应旧类型：`SCHOOL`, `STRATEGY_DEPT`）
- **`FUNCTIONAL`** - 职能部门（对应旧类型：`FUNCTIONAL_DEPT`, `FUNCTION_DEPT`, `OTHER`）
- **`ACADEMIC`** - 二级学院（对应旧类型：`COLLEGE`, `DIVISION`）

---

## 已完成的工作

### 1. 删除重复定义

**文件**: `sism-organization/src/main/java/com/sism/organization/domain/OrgType.java`
- **操作**: ✅ 已删除
- **说明**: 该文件与 `sism-shared-kernel` 中的定义重复

### 2. 修改核心枚举定义

**文件**: `sism-shared-kernel/src/main/java/com/sism/enums/OrgType.java`

**变更内容**:
```java
// 修改前: 7种类型
SCHOOL, FUNCTIONAL_DEPT, FUNCTION_DEPT, COLLEGE, STRATEGY_DEPT, DIVISION, OTHER

// 修改后: 3种类型，带 @JsonValue 注解
ADMIN("admin"),
FUNCTIONAL("functional"),
ACADEMIC("academic")
```

**新增特性**:
- 添加了 `@JsonValue` 注解，使 API 输出使用小写字符串（`"admin"`, `"functional"`, `"academic"`）
- 添加了中文注释说明每种类型的用途

### 3. 更新所有引用文件

已更新以下文件的导入语句，从 `com.sism.organization.domain.OrgType` 改为 `com.sism.enums.OrgType`:

| 文件路径 | 状态 |
|---------|------|
| `sism-organization/src/main/java/com/sism/organization/domain/SysOrg.java` | ✅ 已更新 |
| `sism-organization/src/main/java/com/sism/organization/application/OrganizationApplicationService.java` | ✅ 已更新 |
| `sism-organization/src/main/java/com/sism/organization/interfaces/rest/OrganizationController.java` | ✅ 已更新 |
| `sism-organization/src/main/java/com/sism/organization/interfaces/dto/OrgRequest.java` | ✅ 已更新 |
| `sism-organization/src/main/java/com/sism/organization/interfaces/dto/OrgResponse.java` | ✅ 已更新 |
| `sism-organization/src/main/java/com/sism/organization/domain/repository/OrganizationRepository.java` | ✅ 已更新 |
| `sism-organization/src/main/java/com/sism/organization/infrastructure/persistence/JpaOrganizationRepository.java` | ✅ 已更新 |
| `sism-organization/src/main/java/com/sism/organization/infrastructure/persistence/JpaOrganizationRepositoryInternal.java` | ✅ 已更新 |

### 4. 更新 Swagger 文档示例值

已更新以下文件中的 Swagger 注解示例值：
- `OrgRequest.java`: `example = "functional"`
- `OrgResponse.java`: `example = "functional"`

### 5. 创建数据库迁移脚本

**文件**: `database/migrations/V1.10__update_org_type_enum_values.sql`

**脚本内容**:
- 将 `FUNCTIONAL_DEPT`, `FUNCTION_DEPT` 映射为 `'functional'`
- 将 `COLLEGE`, `DIVISION` 映射为 `'academic'`
- 将 `SCHOOL`, `STRATEGY_DEPT` 映射为 `'admin'`
- 将 `OTHER` 映射为 `'functional'`
- 添加 `chk_sys_org_type` 约束，确保未来只能插入这三个值

### 6. 更新 API 文档

**文件**: `docs/API接口文档.md`

**更新内容**:
- 在数据字典部分添加了 `ORG_TYPE` 字典类型
- 添加了创建组织和更新组织类型的接口说明
- 提供了旧类型→新类型的映射说明

### 7. 更新测试文件

已更新以下测试文件：
- `sism-organization/src/test/java/com/sism/organization/domain/SysOrgTest.java`
- `sism-strategy/src/test/java/com/sism/strategy/domain/IndicatorTest.java`
- `sism-task/src/test/java/com/sism/task/domain/StrategicTaskTest.java`

---

## 待完成的工作

### 1. 验证测试
- 运行所有测试，确保没有问题
- 特别是我们更新过的三个测试文件

### 2. 执行数据库迁移
在部署前，需要执行 Flyway 迁移：
```bash
mvn flyway:migrate
```

### 3. 更新更多文档
需要更新的文档：
- `docs/architecture/appendix/OrgController.md`
- 其他相关文档

---

## 验证清单

在完成重构后，请检查以下项目：

- [ ] 所有测试文件已更新并编译通过
- [ ] 所有单元测试通过 (`mvn test`)
- [ ] 数据库迁移脚本已测试
- [ ] 相关文档已更新
- [ ] Swagger API 文档中的示例正确
- [ ] 前端已更新以使用新的枚举值（如需要）
- [ ] 已验证与现有数据的兼容性
- [ ] 已进行手动测试验证关键功能

---

## 风险评估

### 潜在风险

1. **数据库迁移风险**
   - 风险：迁移可能失败或数据映射不正确
   - 缓解：先在测试环境执行，备份生产数据

2. **API 兼容性风险**
   - 风险：前端可能仍在使用旧的枚举值
   - 缓解：确认前端是否需要同步更新，或提供向后兼容的转换层

3. **测试覆盖率风险**
   - 风险：测试更新可能不完整，遗漏某些场景
   - 缓解：进行全面的回归测试

---

## 回滚计划

如果发现问题，可以按以下步骤回滚：

1. **代码回滚**:
   ```bash
   git revert <commit-hash>
   ```

2. **数据库回滚**:
   ```bash
   mvn flyway:undo
   # 注意：需要先创建 undo 脚本
   ```

3. **验证**:
   - 确保所有功能恢复正常
   - 运行所有测试

---

## 相关文档

- [前端所需API接口文档.md](../../../../docs/前端所需API接口文档.md)
- [前后端接口对比报告.md](../../../../docs/前后端接口对比报告.md)
- ADR-002: Dual Repository Pattern
- ADR-003: Postponed Entity Renaming

---

## 变更记录

| 日期 | 版本 | 变更内容 | 作者 |
|------|------|---------|------|
| 2025-01-21 | v1.0 | 初始版本，记录重构进展 | Claude Assistant |
