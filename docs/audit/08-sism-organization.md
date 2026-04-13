# 审计报告：sism-organization 模块（组织管理）

**审计日期:** 2026-04-12
**审计范围:** 18个Java源文件，涵盖组织CRUD、树形结构、部门管理。

---

## 一、Critical 严重 (共2个)

### C-01. 分页响应 `first` 标志永远为 false
**状态：已修复（2026-04-12）**
**文件:** `interfaces/rest/OrganizationController.java:208-215`
**描述:** `toOrgPageResult` 用4参数 `PageResult` 构造函数，`first = page == 0`。但 service 层返回的 page 是 1-based（>=1），导致 `first` 永远为 false，即使请求第一页。
**修复建议:** 使用7参数构造函数保留原始 first/last：
```java
return new PageResult<>(items, total, pageResult.getPage(), pageResult.getPageSize(),
    pageResult.getTotalPages(), pageResult.isFirst(), pageResult.isLast());
```

### C-02. OrgType 双枚举映射脆弱 — Repository import类型与Entity不一致
**状态：已修复（2026-04-12）**
**文件:** `domain/OrgType.java` vs `domain/repository/OrganizationRepository.java:5`
**描述:** `OrganizationRepository` import 共享内核的 `com.sism.enums.OrgType`，而 `SysOrg` 实体用模块内 `com.sism.organization.domain.OrgType`。两边枚举值必须严格同名，否则运行时崩溃。
**修复建议:** 统一使用单一 OrgType 枚举。

---

## 二、High 高 (共4个)

### H-01. 循环层级检测 N+1 查询
**状态：已修复（2026-04-12）**
**文件:** `application/OrganizationApplicationService.java:243-257`
**描述:** `validateNoCircularHierarchy` 通过 while 循环逐级 `findById` 查询父组织，20层产生20次DB查询。
**修复建议:** 使用递归CTE一次性查询完整祖先链路。

### H-02. 内存过滤活跃状态
**状态：已修复（2026-04-12）**
**文件:** `application/OrganizationApplicationService.java:165-176`
**描述:** `getDepartmentOrganizations(false)` 先查全部再 Stream 过滤 `isActive`。
**修复建议:** 添加 Repository 组合查询方法 `findByTypeInAndIsActive`。

### H-03. 停用组织不检查影响范围
**状态：已修复（2026-04-13）**
**文件:** `application/OrganizationApplicationService.java:78-84`
**描述:** 直接停用组织，未检查是否有活跃子组织、关联用户、关联指标/任务。
**修复结果:** 停用前现在会同时检查活跃子组织、已分配用户，以及 `plan`、`indicator`、`sys_task` 表中的未删除关联记录。

### H-04. setOrgName() 绕过 rename() 的业务校验
**状态：已修复（2026-04-12）**
**文件:** `domain/SysOrg.java:213-215`
**描述:** `setOrgName()` 直接设置 `name` 无校验，绕过 `rename()` 的空值检查和长度限制。
**修复建议:** 让 `setOrgName()` 委托给 `rename()`，或标记 `@Deprecated`。

---

## 三、Medium 中等 (共8个)

| # | 状态 | 文件:行号 | 问题 | 类别 |
|---|---|---|---|---|
| M-01 | 已修复（2026-04-13） | `OrganizationApplicationService.java` | `getOrganizationById` 返回 `Optional`，不再返回 `null` | Bug |
| M-02 | 已修复（2026-04-13） | `SysOrg.java` | `updateName()` 已委托 `rename()`，去掉重复业务逻辑 | DRY |
| M-03 | 已修复（2026-04-13） | `SysOrg.java` | 已删除无实际字段支撑的 `updateDescription()` / `getDescription()` 死代码 | 死代码 |
| M-04 | 已修复（2026-04-13） | `SysOrg.java` | 已删除 `getParentOrg()` 的僵尸实体创建逻辑 | 数据安全 |
| M-05 | 已修复（2026-04-13） | `OrganizationController.java` | 重命名改为 `PUT Body` 请求 DTO | 安全 |
| M-06 | 已修复（2026-04-13） | `OrganizationApplicationService.java` | 组织排序收口到统一 Comparator 常量 | DRY |
| M-07 | 已修复（2026-04-13） | `pom.xml` | 已移除未使用的 MapStruct 依赖和处理器配置 | 依赖 |
| M-08 | 已修复（2026-04-13） | `OrgRequest.java`, `OrganizationController.java` | 请求 DTO 和控制器已统一使用模块内 `OrgType` | 代码质量 |

---

## 四、Low 低 (共3个)

| # | 状态 | 文件:行号 | 问题 |
|---|---|---|---|
| L-01 | 已修复（2026-04-13） | `OrganizationApplicationService.java`, `OrganizationController.java` | `includeUsers=true` 现在显式拒绝，不再静默忽略 |
| L-02 | 已修复（2026-04-13） | `OrganizationApplicationService.java` | 递归构建树增加最大深度保护 |
| L-03 | 已修复（2026-04-13） | `OrgRequest.java` | DTO 已切换到模块内 `OrgType`，不再依赖共享枚举转换 |

---

## 汇总统计

| 严重性 | 数量 |
|--------|------|
| **Critical** | 2 |
| **High** | 4 |
| **Medium** | 8 |
| **Low** | 3 |
| **总计** | **17** |
