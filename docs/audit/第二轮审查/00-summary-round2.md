# SISM 第二轮审计总结报告

**审计日期:** 2026-04-13
**审计范围:** 全部 10 个后端模块，393 个 Java 源文件
**参照基准:** 第一轮审计报告 (2026-04-12)

---

## 一、各模块修复总览

| # | 模块 | 第一轮问题 | 已修复 | 部分修复 | 未修复 | 修复率 | 新发现 | 评级 |
|---|------|-----------|--------|---------|--------|--------|--------|------|
| 01 | shared-kernel | 68 | 35 | 9 | 24 | **51.5%** | 6 | ⭐⭐⭐ |
| 02 | iam | 32 | 13 | 3 | 16 | **40.6%** | 11 | ⭐⭐⭐ |
| 03 | execution | 27 | 8 | 2 | 17 | **29.6%** | 10 | ⭐⭐ |
| 04 | strategy | 36 | 10 | 3 | 23 | **27.8%** | 8 | ⭐⭐ |
| 05 | workflow | 28 | 15 | 0 | 13 | **53.6%** | 7 | ⭐⭐⭐ |
| 06 | analytics | 26 | 10 | 2 | 14 | **38.5%** | 18 | ⭐⭐⭐ |
| 07 | alert | 14 | 14 | 0 | 0 | **100%** | 5 | ⭐⭐⭐⭐⭐ |
| 08 | organization | 17 | 17 | 0 | 0 | **100%** | 5 | ⭐⭐⭐⭐⭐ |
| 09 | task | 18 | 17 | 1 | 0 | **94.4%** | 7 | ⭐⭐⭐⭐ |
| 10 | main | 18 | 9 | 0 | 9* | **50%** | 5 | ⭐⭐⭐ |
| | **合计** | **284** | **148** | **20** | **116** | **52.1%** | **80** | |

*main 模块有 10 项因代码位于 sism-config 而无法验证

---

## 二、修复率排名

```
alert         ████████████████████ 100%
organization  ████████████████████ 100%
task          ███████████████████░ 94.4%
workflow      ██████████▊───────── 53.6%
shared-kernel ██████████---------- 51.5%
main          ██████████---------- 50.0%*
iam           ████████------------ 40.6%
analytics     ███████▋------------ 38.5%
execution     █████▉-------------- 29.6%
strategy      █████▌-------------- 27.8%
```

---

## 三、跨模块共性问题 TOP 10

以下问题在 3 个以上模块中反复出现，需跨模块统一治理：

### 1. @Transient 字段导致核心数据不持久化
**影响模块:** execution (C-01), analytics (M-03), strategy
**严重度:** Critical
**修复建议:** 全面排查所有 `@Transient` 注解，区分「有意不持久化」和「遗漏」，后者立即移除注解并添加 `@Column`。

### 2. 字符串常量代替枚举 (String-typed statuses)
**影响模块:** shared-kernel (M-23), execution (H-02), workflow
**严重度:** High
**修复建议:** 统一创建 JPA 枚举转换器：
```java
@Converter(autoApply = true)
public class AuditStatusConverter extends AttributeConverter<AuditStatus, String> { ... }
```

### 3. 软删除查询遗漏 isDeleted 过滤
**影响模块:** execution (NEW-05, L-06), shared-kernel
**严重度:** High
**修复建议:** 使用 Hibernate `@Where` 注解全局过滤：
```java
@Entity
@Where(clause = "is_deleted = false")
public class PlanReport extends AggregateRoot<Long> { ... }
```

### 4. 双异常体系 (Dual Exception Hierarchy)
**影响模块:** shared-kernel (C-06), 全局
**严重度:** Critical
**修复建议:** 统一为 `shared.domain.exception.BusinessException`，旧版标记 `@Deprecated` 过渡。

### 5. JPA 字段遮蔽 (Field Shadowing)
**影响模块:** shared-kernel (H-11), execution, strategy
**严重度:** High
**修复建议:** 基类改为 `@MappedSuperclass`，子类直接声明持久化字段。

### 6. N+1 查询 / 内存过滤分页
**影响模块:** execution (NEW-01), iam (H-03/H-04), analytics
**严重度:** High
**修复建议:** 所有分页查询在数据库层面完成权限过滤，使用 `@EntityGraph` 或 JOIN FETCH。

### 7. CacheManager / 缓存配置不一致
**影响模块:** analytics (NH-01/02), shared-kernel
**严重度:** High
**修复建议:** `@Cacheable` 和 `@CacheEvict` 统一指定 `cacheManager` 参数。

### 8. 跨模块直接 SQL / 硬编码表名
**影响模块:** organization (NM-02), task (NM-02), execution
**严重度:** Medium
**修复建议:** 定义跨上下文查询接口 (Port/Adapter 模式)。

### 9. 缺少 @Transactional(readOnly = true)
**影响模块:** task (NM-01), execution, workflow
**严重度:** Medium
**修复建议:** 所有查询方法标注 `@Transactional(readOnly = true)`。

### 10. 安全头配置不完整
**影响模块:** shared-kernel (M-08), 全局
**严重度:** Medium
**修复建议:** 添加 CSP + HSTS，移除废弃的 `X-XSS-Protection`。

---

## 四、P0 紧急修复项（影响生产稳定性）

| # | 模块 | 问题 | 影响 |
|---|------|------|------|
| 1 | execution | C-01 @Transient 数据丢失 | 报告内容/审批/拒绝数据不持久化 |
| 2 | shared-kernel | M-28 EventStore Bean 冲突 | 特定配置下应用启动失败 |
| 3 | execution | NEW-05 7个查询缺 isDeleted | 软删除数据泄露到 API |
| 4 | execution | NEW-01 内存权限过滤 | totalElements 不准确 + 数据泄露 |
| 5 | iam | NEW-01 刷新 token 重放 | 安全漏洞 |

---

## 五、各模块报告索引

| 报告 | 路径 |
|------|------|
| 共享内核 | `第二轮审查/01-sism-shared-kernel-round2.md` |
| 身份认证 | `第二轮审查/02-sism-iam-round2.md` |
| 执行管理 | `第二轮审查/03-sism-execution-round2.md` |
| 战略规划 | `第二轮审查/04-sism-strategy-round2.md` |
| 工作流 | `第二轮审查/05-sism-workflow-round2.md` |
| 数据分析 | `第二轮审查/06-sism-analytics-round2.md` |
| 预警管理 | `第二轮审查/07-sism-alert-round2.md` |
| 组织管理 | `第二轮审查/08-sism-organization-round2.md` |
| 任务管理 | `第二轮审查/09-sism-task-round2.md` |
| 应用入口 | `第二轮审查/10-sism-main-round2.md` |

---

## 六、下一步建议

### 短期 (1-2 周)
1. **修复 5 个 P0 紧急项** — 数据丢失 + 启动失败 + 安全漏洞
2. **统一枚举方案** — 创建共享枚举 + JPA Converter 基类
3. **全局软删除过滤** — 使用 `@Where` 注解或 Hibernate Filter

### 中期 (2-4 周)
4. **合并异常体系** — 统一到 DDD 异常，旧版 @Deprecated
5. **修复 N+1 查询** — execution + iam 模块的权限过滤
6. **补全安全头** — CSP + HSTS + 移除废弃头

### 长期 (1-2 月)
7. **跨模块接口治理** — Port/Adapter 替代硬编码 SQL
8. **EventStore 架构优化** — 统一存储后端、批量操作
9. **缓存策略统一** — CacheManager 限定符 + 配置化 TTL
