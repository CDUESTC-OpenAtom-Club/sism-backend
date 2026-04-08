# sism-organization 模块审计报告

**审计日期:** 2026-04-06
**审计人员:** Claude Code (Automated Audit)
**模块版本:** 当前主分支

---

## 模块概览

| 项目 | 数据 |
|------|------|
| 模块名称 | sism-organization |
| 模块职责 | 组织管理、组织层级、组织状态管理 |
| Java 文件总数 | 15 |
| 核心实体 | SysOrg |
| Repository 数量 | 1 |
| Service 数量 | 1 |
| Controller 数量 | 1 |

### 包结构

```
com.sism.organization/
├── application/
│   └── OrganizationApplicationService.java
├── domain/
│   ├── SysOrg.java
│   ├── OrgType.java
│   ├── event/
│   │   ├── OrgCreatedEvent.java
│   │   ├── OrgActivatedEvent.java
│   │   └── OrgDeactivatedEvent.java
│   └── repository/
│       └── OrganizationRepository.java
├── infrastructure/
│   ├── OrganizationModuleConfig.java
│   └── persistence/
│       ├── JpaOrganizationRepository.java
│       └── JpaOrganizationRepositoryInternal.java
└── interfaces/
    ├── dto/
    │   ├── OrgRequest.java
    │   ├── OrgResponse.java
    │   └── OrgMapper.java
    └── rest/
        └── OrganizationController.java
```

---

## 一、安全漏洞

### 🔴 Critical: 所有 API 端点完全缺少权限控制

**文件:** `OrganizationController.java`
**行号:** 全文

```java
@RestController
@RequestMapping({"/api/v1/organizations", "/api/v1/orgs"})
public class OrganizationController {

    @PostMapping
    @Operation(summary = "创建新组织")
    public ResponseEntity<ApiResponse<OrgResponse>> createOrganization(
            @Valid @RequestBody OrgRequest request) {
        // ❌ 无 @PreAuthorize - 任何用户都可以创建组织
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "激活组织")
    public ResponseEntity<ApiResponse<OrgResponse>> activateOrganization(
            @Parameter(description = "组织ID") @PathVariable Long id) {
        // ❌ 无权限控制 - 任何用户都可以激活组织
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "停用组织")
    public ResponseEntity<ApiResponse<OrgResponse>> deactivateOrganization(
            @Parameter(description = "组织ID") @PathVariable Long id) {
        // ❌ 无权限控制 - 任何用户都可以停用组织
    }

    @PutMapping("/{id}/name")
    @Operation(summary = "重命名组织")
    public ResponseEntity<ApiResponse<OrgResponse>> renameOrganization(...) {
        // ❌ 无权限控制 - 任何用户都可以重命名组织
    }

    @PutMapping("/{id}/type")
    @Operation(summary = "修改组织类型")
    public ResponseEntity<ApiResponse<OrgResponse>> changeOrganizationType(...) {
        // ❌ 无权限控制 - 任何用户都可以修改组织类型
    }
}
```

**问题描述:**
1. 所有组织管理操作都没有权限控制
2. 任何认证用户都可以创建、修改、激活/停用组织
3. 组织是系统核心数据，被篡改会影响整个系统

**风险影响:**
- 组织结构被恶意修改
- 系统数据完整性受损
- 业务流程被破坏

**严重等级:** 🔴 **Critical**

**建议修复:**
```java
@RestController
@RequestMapping({"/api/v1/organizations", "/api/v1/orgs"})
public class OrganizationController {

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建新组织")
    public ResponseEntity<ApiResponse<OrgResponse>> createOrganization(...) { }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG_MANAGER')")
    @Operation(summary = "激活组织")
    public ResponseEntity<ApiResponse<OrgResponse>> activateOrganization(...) { }

    @PutMapping("/{id}/name")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "重命名组织")
    public ResponseEntity<ApiResponse<OrgResponse>> renameOrganization(...) { }
}
```

---

### 🔴 High: 用户列表接口无权限控制

**文件:** `OrganizationController.java`
**行号:** 78-84

```java
@GetMapping("/{id}/users")
@Operation(summary = "根据组织ID获取用户列表")
public ResponseEntity<ApiResponse<List<User>>> getUsersByOrganizationId(
        @Parameter(description = "组织ID") @PathVariable Long id) {
    // ❌ 无权限控制 - 任何用户都可以查看任何组织的用户列表
    List<User> users = organizationApplicationService.getUsersByOrganizationId(id);
    return ResponseEntity.ok(ApiResponse.success(users));
}
```

**问题描述:**
此接口返回组织下的所有用户信息，可能包含敏感数据，但完全没有权限控制。

**风险影响:**
- 用户信息泄露
- 可被用于社工攻击

**严重等级:** 🔴 **High**

**建议修复:**
```java
@GetMapping("/{id}/users")
@PreAuthorize("hasAnyRole('ADMIN', 'ORG_MANAGER') or @securityService.hasOrgAccess(#id)")
@Operation(summary = "根据组织ID获取用户列表")
public ResponseEntity<ApiResponse<List<User>>> getUsersByOrganizationId(@PathVariable Long id) { }
```

---

## 二、潜在 Bug 和逻辑错误

### 🔴 High: getParentOrg() 方法总是返回 null

**文件:** `SysOrg.java`
**行号:** 168-171

```java
public SysOrg getParentOrg() {
    // Parent org is stored as ID only - this method is for future use
    return null;  // ❌ 总是返回 null
}
```

**问题描述:**
1. `parentOrgId` 字段存储了父组织 ID，但 `getParentOrg()` 总是返回 `null`
2. 这导致 `buildTree` 方法无法正确处理父子关系
3. 注释说 "for future use" 但代码已经在使用

**风险影响:**
- 组织树构建可能失败
- 父子关系判断错误

**严重等级:** 🔴 **High**

**建议修复:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_org_id")
private SysOrg parentOrg;

public SysOrg getParentOrg() {
    return this.parentOrg;
}
```

---

### 🟠 Medium: 重复的 OrgType 枚举

**问题描述:**
存在两个 `OrgType` 枚举：
1. `com.sism.organization.domain.OrgType`
2. `com.sism.enums.OrgType` (在 shared-kernel)

两者需要手动转换：
```java
// OrganizationController.java
com.sism.organization.domain.OrgType domainType =
    com.sism.organization.domain.OrgType.fromSharedOrgType(request.getType());
```

**问题描述:**
1. 违反 DRY 原则
2. 需要维护两份相同的枚举定义
3. 容易出现不一致

**严重等级:** 🟠 **Medium**

**建议修复:**
统一使用一个 OrgType 枚举，推荐使用 `com.sism.enums.OrgType`。

---

### 🟠 Medium: 重复的方法定义

**文件:** `SysOrg.java`

```java
// 方法 1
public void rename(String newName) { ... }

// 方法 2 - 功能相同
public void updateName(String name) { ... }

// 方法 3
public String getOrgName() { return this.name; }

// 方法 4 - 功能相同
public String getName() { return this.name; }  // Lombok 生成
```

**问题描述:**
存在多个功能相同的方法，增加维护成本。

**严重等级:** 🟠 **Medium**

**建议修复:**
保留一套方法，删除冗余方法。

---

### 🟡 Low: 占位方法未实现

**文件:** `SysOrg.java`
**行号:** 144-153

```java
public void updateDescription(String description) {
    // Description is not currently stored in the entity - this method is for future use
    this.updatedAt = LocalDateTime.now();  // ❌ 仅更新时间，不存储值
}

public String getDescription() {
    // Description is not currently stored in the entity - this method is for future use
    return null;  // ❌ 总是返回 null
}
```

**问题描述:**
方法存在但不实现功能，可能导致误解。

**严重等级:** 🟡 **Low**

---

## 三、性能瓶颈

### 🟠 Medium: 组织树每次请求重新构建

**文件:** `OrganizationApplicationService.java`
**行号:** 85-114

```java
public List<SysOrg> getOrganizationTree() {
    List<SysOrg> allOrgs = organizationRepository.findAll();  // ❌ 每次查询所有组织
    return buildTree(allOrgs, null);  // ❌ 每次重新构建树
}

private List<SysOrg> buildTree(List<SysOrg> allOrgs, Long parentId) {
    return allOrgs.stream()
            .filter(org -> { ... })
            .peek(org -> {
                List<SysOrg> children = buildTree(allOrgs, org.getId());  // ❌ 递归遍历
                org.getChildren().clear();
                org.getChildren().addAll(children);
            })
            .collect(Collectors.toList());
}
```

**问题描述:**
1. 每次请求都从数据库查询所有组织
2. 每次都重新构建树结构
3. 组织结构通常变化不频繁，应该缓存

**风险影响:**
- 数据库压力
- 响应延迟

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
@Cacheable(value = "orgTree", key = "'all'")
public List<SysOrg> getOrganizationTree() {
    // ...
}

@CacheEvict(value = "orgTree", allEntries = true)
public SysOrg createOrganization(...) {
    // 创建时清除缓存
}
```

---

### 🟡 Low: 查询所有组织无分页

**文件:** `OrganizationController.java`
**行号:** 42-48

```java
@GetMapping
public ResponseEntity<ApiResponse<List<OrgResponse>>> getAllOrganizations() {
    List<SysOrg> orgs = organizationApplicationService.getAllOrganizations();  // ❌ 无分页
    // ...
}
```

**问题描述:**
返回所有组织列表，如果组织数量多，会有性能问题。

**严重等级:** 🟡 **Low**

---

## 四、代码质量和可维护性

### 🟠 Medium: 模块依赖 sism-iam

**文件:** `OrganizationApplicationService.java`
**行号:** 26

```java
private final UserRepository userRepository;  // ❌ 依赖 sism-iam 模块
```

**问题描述:**
Organization 模块依赖 IAM 模块的 `UserRepository`，这创建了模块间的耦合。理想情况下，应该通过接口或服务层访问用户数据。

**严重等级:** 🟠 **Medium**

---

### 🟠 Medium: API 路径别名可能导致混淆

**文件:** `OrganizationController.java`
**行号:** 22

```java
@RequestMapping({"/api/v1/organizations", "/api/v1/orgs"})
```

**问题描述:**
控制器映射了两个路径，可能导致 API 混乱。

**严重等级:** 🟠 **Medium**

**建议修复:**
选择一个主要路径，另一个使用重定向或废弃。

---

### 🟡 Low: 异常消息混合中英文

**文件:** `SysOrg.java`

```java
throw new IllegalArgumentException("Organization name cannot be null or empty");  // 英文
throw new IllegalArgumentException("Organization is already active");  // 英文
```

**问题描述:**
虽然当前使用英文，但与其他模块的中文消息不一致。

**严重等级:** 🟡 **Low**

---

## 五、架构最佳实践

### ✅ 亮点: 领域事件设计

SysOrg 实体正确使用了领域事件：

```java
public static SysOrg create(String name, OrgType type) {
    // ...
    org.addEvent(new OrgCreatedEvent(org.id, name, type.name()));
    return org;
}

public void activate() {
    // ...
    this.addEvent(new OrgActivatedEvent(this.id, this.name));
}
```

---

### ✅ 亮点: 聚合根设计

SysOrg 继承自 `AggregateRoot`，实现了 `validate()` 方法，符合 DDD 设计原则。

---

### ✅ 亮点: 软删除实现

```java
public void delete() {
    this.isDeleted = true;
    this.updatedAt = LocalDateTime.now();
}
```

---

### 🟡 Low: 索引设计良好

SysOrg 实体定义了多个有用的索引：

```java
@Index(name = "idx_sys_org_type", columnList = "type"),
@Index(name = "idx_sys_org_active", columnList = "is_active"),
@Index(name = "idx_sys_org_parent", columnList = "parent_org_id"),
```

---

## 六、审计总结

### 问题统计

| 严重等级 | 数量 | 类别分布 |
|----------|------|----------|
| 🔴 Critical | 1 | 安全（无权限控制） |
| 🔴 High | 2 | 安全、Bug |
| 🟠 Medium | 5 | Bug、性能、代码质量 |
| 🟡 Low | 4 | 代码质量、性能 |

### 最紧急修复项

| 优先级 | 问题 | 影响 |
|--------|------|------|
| P0 | 所有 API 无权限控制 | 组织数据可被任意修改 |
| P1 | getParentOrg 返回 null | 组织树功能异常 |
| P1 | 用户列表无权限控制 | 用户信息泄露 |
| P2 | 重复 OrgType 枚举 | 维护困难 |

### 整体评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 安全性 | 🔴 需改进 | 完全缺失权限控制 |
| 可靠性 | 🟠 需改进 | getParentOrg 方法有问题 |
| 性能 | 🟠 需改进 | 缺少缓存 |
| 可维护性 | ✅ 良好 | DDD 设计清晰 |
| 架构合规性 | ✅ 良好 | 模块结构合理 |

### 亮点

1. **领域事件**: 正确使用领域事件进行解耦
2. **聚合根设计**: SysOrg 继承 AggregateRoot
3. **软删除**: 实现了软删除模式
4. **数据库索引**: 定义了合理的索引

### 关键建议

1. **立即添加权限控制**: 为所有写操作添加 `@PreAuthorize`
2. **修复 getParentOrg**: 正确实现父组织关系
3. **添加缓存**: 缓存组织树结构
4. **统一 OrgType 枚举**: 移除重复定义
5. **添加缓存失效**: 在组织变更时清除缓存

---

**审计完成日期:** 2026-04-06
**下一步行动:** 修复权限控制问题后再部署生产环境