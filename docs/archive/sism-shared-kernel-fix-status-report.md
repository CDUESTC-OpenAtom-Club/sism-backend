# sism-shared-kernel 模块审计问题修复状态报告

**审查日期:** 2026-04-06
**原审计报告:** sism-shared-kernel-module-audit-report.md
**审查人员:** Claude Code (Automated Review)

---

## 修复状态总览

| 状态 | 数量 | 占比 |
|------|------|------|
| ✅ 已修复 | 2 | 12% |
| ⚠️ 部分修复 | 3 | 18% |
| ❌ 未修复 | 12 | 70% |
| 🔍 无法验证 | 0 | 0% |
| **合计** | **17** | 100% |

---

## 详细审查结果

### 已修复问题

| # | 问题 | 严重等级 | 状态 | 证据 |
|---|------|----------|------|------|
| 1 | CORS 默认配置过于宽松 | 🟠 Medium | ✅ 已修复 | `allowedOrigins` 默认值改为空字符串，未配置时记录警告并禁止跨域 |
| 2 | Redis 密码可能被记录到日志 | 🟠 Medium | ✅ 已修复 | 日志改为输出 `passwordConfigured=true/false` 布尔值，不再输出密码 |

### 部分修复问题

| # | 问题 | 严重等级 | 状态 | 证据 |
|---|------|----------|------|------|
| 3 | TokenBlacklistService 使用内存存储 | 🔴 High | ⚠️ 部分修复 | 共享内核版已支持 Redis + TTL 回退，但 IAM 模块仍存在旧版使用纯 ConcurrentHashMap |
| 4 | EnvConfig 静态初始化可能失败 | 🟠 Medium | ⚠️ 部分修复 | 添加了 System.getenv 回退和 get(key, defaultValue) 方法，但优先级顺序未按建议调整 |
| 5 | GlobalExceptionHandler 方法过长 | 🟡 Low | ⚠️ 部分修复 | 代码组织有所改善（清晰的段落注释），但未拆分为多个处理器类 |

### 未修复问题

| # | 问题 | 严重等级 | 说明 |
|---|------|----------|------|
| 6 | DomainEvent getEventId() 每次调用生成新 ID | 🟡 Low | 仍使用 `default` 方法返回 `UUID.randomUUID()` |
| 7 | GlobalExceptionHandler 重复异常处理 | 🟠 Medium | 仍存在两套异常层次结构的处理器 |
| 8 | Percentage 构造器边界检查消息不一致 | 🟡 Low | 消息仍为 "between 0 and 100"，缺少 "(inclusive)" |
| 9 | RequestLoggingFilter 请求 ID 过短（8字符） | 🟡 Low | 仍使用 `substring(0, 8)` |
| 10 | DomainEventPublisher 无异步事件发布 | 🟠 Medium | 仍同步执行 save + publishEvent |
| 11 | CacheUtils 创建独立 ObjectMapper 实例 | 🟡 Low | 未使用 Spring 管理的单例 |
| 12 | 重复的异常类层次结构 | 🟠 Medium | `com.sism.exception` 和 `com.sism.shared.domain.exception` 仍共存 |
| 13 | SecurityHeadersFilter 硬编码 API 路径前缀 | 🟠 Medium | 仍硬编码 `/api/` |
| 14 | 枚举类未实现公共接口 | 🟡 Low | 无 `BaseEnum` 接口 |
| 15 | AggregateRoot validate() 未自动调用 | 🟠 Medium | 无 `@PrePersist/@PreUpdate` 回调 |
| 16 | Repository 接口过于碎片化 | 🟡 Low | 仍分为 ReadRepository、WriteRepository、Repository 三个文件 |
| 17 | 值对象未实现 Comparable | 🟡 Low | Percentage、DateRange、EntityId 均未实现 |

---

## 修复质量评估

### 按严重等级统计

| 严重等级 | 总数 | 已修复 | 部分修复 | 修复率 |
|----------|------|--------|----------|--------|
| 🔴 High | 1 | 0 | 1 | 0% |
| 🟠 Medium | 9 | 2 | 1 | 22% |
| 🟡 Low | 7 | 0 | 2 | 0% |
| **合计** | **17** | **2** | **3** | **12%** |

### 整体结论

sism-shared-kernel 模块修复率较低（12%），仅修复了两个安全相关的 Medium 问题（CORS 默认值和 Redis 密码日志）。High 级别的 TokenBlacklistService 仅部分修复（共享内核版支持 Redis，但 IAM 模块仍有旧版副本）。大量架构规范类问题（重复异常层次、Repository 碎片化、值对象设计等）均未触及。作为共享内核模块，这些问题会影响所有依赖它的业务模块。

---

**审查完成日期:** 2026-04-06
