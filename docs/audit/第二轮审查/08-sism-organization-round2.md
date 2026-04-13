# 第二轮审计报告：sism-organization（组织管理）

**审计日期:** 2026-04-13
**范围:** 19 个 Java 源文件全面复检
**参照:** 第一轮报告 `08-sism-organization.md` (2026-04-12)

---

## 修复总览

| 指标 | 数值 |
|------|------|
| 第一轮问题总数 | 17 |
| 已确认修复 | **17** (100%) |
| 第二轮新发现 | **5** |

**评价:** 与 alert 模块并列修复率最高。

---

## A. 第一轮问题 — 全部修复 ✅

### Critical (2项)
- C-01 分页 `first` 标志 → 正确使用 7 参数 `PageResult` 构造
- C-02 OrgType 双枚举 → 统一为模块内枚举 + `OrgMapper.toSharedOrgType()` 边界转换

### High (4项)
- H-01 循环层级检测 N+1 → `findAll()` 一次加载 + Map 遍历
- H-02 内存过滤活跃状态 → `findByTypesAndIsActive()` 数据库层过滤
- H-03 停用不检查影响 → `validateCanDeactivate()` 检查子组织/用户/指标引用
- H-04 setOrgName 绕过 rename → 委托给 `rename()`

### Medium (8项) + Low (3项) — 全部修复

---

## B. 第二轮新发现问题

### NM-01. [MEDIUM] 循环层级检测每次全量加载
**文件:** `application/OrganizationApplicationService.java:254`

已从 N+1 优化为单次 `findAll()`，但每次修改父组织仍加载全部实体到内存。

**最优解 — PostgreSQL 递归 CTE：**
```java
// Repository 新增方法
@Query(value = """
    WITH RECURSIVE ancestors AS (
        SELECT id, parent_org_id FROM sys_org WHERE id = :candidateParentId
        UNION ALL
        SELECT o.id, o.parent_org_id FROM sys_org o
        JOIN ancestors a ON o.id = a.parent_org_id
    )
    SELECT COUNT(*) > 0 FROM ancestors WHERE id = :orgId
    """, nativeQuery = true)
boolean isAncestorOf(@Param("orgId") Long orgId,
                     @Param("candidateParentId") Long candidateParentId);

// Service 使用
public void validateNoCircularHierarchy(Long orgId, Long newParentId) {
    if (organizationRepository.isAncestorOf(orgId, newParentId)) {
        throw new ConflictException("Cannot set parent: would create circular hierarchy");
    }
}
```

### NM-02. [MEDIUM] OrganizationReferenceCheckService 硬编码 SQL 表名
**文件:** `application/OrganizationReferenceCheckService.java:17-42`

通过 `JdbcTemplate` 硬编码 `public.plan`、`public.indicator`、`public.sys_task` 表名列名。其他模块 schema 变更时无编译期检查。

**最优解 — 抽取为常量 + 接口隔离：**
```java
public final class ReferenceTableNames {
    public static final String PLAN = "plan";
    public static final String INDICATOR = "indicator";
    public static final String TASK = "sys_task";
    public static final String COL_ORG_ID = "org_id";
    public static final String COL_DELETED = "is_deleted";
    // 集中管理，schema 变更时单点修改
}

// 或更好的方案：定义跨模块查询接口
public interface OrganizationReferenceQueryPort {
    int countActivePlansByOrgId(Long orgId);
    int countActiveIndicatorsByOrgId(Long orgId);
    int countActiveTasksByOrgId(Long orgId);
}
```

### NM-03. [MEDIUM] OrgResponse 使用共享内核 OrgType 而 OrgRequest 使用模块内 OrgType
**文件:** `OrgResponse.java:3` vs `OrgRequest.java:3`

```java
// OrgResponse: import com.sism.enums.OrgType (共享内核)
// OrgRequest:  import com.sism.organization.domain.OrgType (模块内)
```

**最优解:** 响应 DTO 统一使用 String 类型 + `@JsonValue`，避免跨模块枚举序列化耦合：
```java
public record OrgResponse(
    Long id,
    String name,
    String type,  // 而非 OrgType 枚举
    // ...
) {}
```

### NL-01. [LOW] OrgMapper 使用 `Collectors.toList()` 风格不一致
建议统一为 Java 16+ `.toList()`。

### NL-02. [LOW] getOrganizationTree 无结果大小保护
`findAll()` 构建树结构，大量组织时响应体过大。建议添加结果大小日志或懒加载子树 API。

---

## C. 总结

| 严重度 | 第一轮 | 第二轮新发现 |
|--------|--------|-------------|
| Critical | 2 (✅ 全部修复) | 0 |
| High | 4 (✅ 全部修复) | 0 |
| Medium | 8 (✅ 全部修复) | 3 |
| Low | 3 (✅ 全部修复) | 2 |
| **总计** | **17** | **5** |

**模块评级:** ⭐⭐⭐⭐⭐ (5/5)
