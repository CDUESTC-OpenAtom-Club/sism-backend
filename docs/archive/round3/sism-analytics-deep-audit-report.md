# sism-analytics 模块第三轮深度审计报告

**审计日期:** 2026-04-06
**审计人员:** Claude Code (Automated Deep Audit)
**模块版本:** 当前主分支

---

## 模块概览

| 项目 | 数据 |
|------|------|
| 模块名称 | sism-analytics |
| 模块职责 | 仪表盘管理、数据导出、报表管理、分析汇总 |
| Java 文件总数 | 78 |
| 核心实体 | Dashboard, DataExport, Report |
| Repository 数量 | 3 |
| Service 数量 | 6 |
| Controller 数量 | 4 |

### 包结构

```
com.sism.analytics/
├── application/
│   ├── DashboardApplicationService.java
│   ├── DashboardSummaryService.java
│   ├── DataExportApplicationService.java
│   ├── ExportService.java
│   └── ReportApplicationService.java
├── domain/
│   ├── Dashboard.java
│   ├── DataExport.java
│   ├── ExportFormat.java
│   ├── Report.java
│   └── ReportType.java
├── infrastructure/
│   ├── AnalyticsModuleConfig.java
│   └── repository/
│       ├── DashboardRepository.java
│       ├── DataExportRepository.java
│       └── ReportRepository.java
└── interfaces/
    ├── dto/
    │   ├── CompleteDataExportRequest.java
    │   ├── CopyDashboardRequest.java
    │   ├── CreateDashboardRequest.java
    │   ├── CreateDataExportRequest.java
    │   ├── CreateReportRequest.java
    │   ├── DashboardDTO.java
    │   ├── DashboardSummaryDTO.java
    │   ├── DataExportDTO.java
    │   ├── DepartmentProgressDTO.java
    │   ├── GenerateReportRequest.java
    │   ├── ReportDTO.java
    │   ├── UpdateDashboardRequest.java
    │   ├── UpdateDataExportRequest.java
    │   └── UpdateReportRequest.java
    └── rest/
        ├── DashboardController.java
        ├── DashboardSummaryController.java
        ├── DataExportController.java
        └── ReportController.java
```

---

## 一、安全漏洞与权限控制

### 🔴 Critical: DataExportController 和 ReportController 无权限控制

**文件:** `DataExportController.java`, `ReportController.java`

**问题描述:**
1. DataExportController 和 ReportController 的所有 API 端点都没有添加 `@PreAuthorize` 权限注解
2. 任何用户都可以访问、创建、修改、删除任何用户的数据导出任务和分析报告
3. 存在越权操作风险，攻击者可以通过API端点操作其他用户的数据

**攻击示例:**
```bash
# 删除其他用户的数据导出任务
DELETE /api/v1/analytics/exports/123

# 获取所有用户的报告列表
GET /api/v1/analytics/reports/generated

# 创建其他用户的报告
POST /api/v1/analytics/reports
{
  "name": "恶意报告",
  "type": "STRATEGIC",
  "format": "PDF",
  "generatedBy": 999,
  "parameters": "{}"
}
```

**风险影响:**
- 敏感数据被未授权访问、修改或删除
- 用户数据完整性和隐私泄露
- 系统数据被篡改或破坏

**严重等级:** 🔴 **Critical**

---

### 🔴 High: 数据导出和报告模块缺乏归属验证

**文件:** `DataExportApplicationService.java`, `ReportApplicationService.java`

**问题描述:**
1. 服务层方法没有验证数据导出任务和报告的所有权
2. 任何用户都可以操作任何用户的数据导出和报告
3. 例如 `deleteDataExport`、`failDataExport`、`generateReport` 等方法都没有验证当前用户是否为创建者

**风险影响:**
- 恶意用户可以删除或篡改其他用户的数据导出任务
- 可以伪造报告生成状态
- 数据导出文件可能被未授权下载

**严重等级:** 🔴 **High**

---

### 🟠 Medium: 权限控制不一致

**对比:** `DashboardController` ✅ vs `DataExportController` ❌ vs `ReportController` ❌

**问题描述:**
1. DashboardController 已经正确添加了权限控制和用户验证
2. 但 DataExportController 和 ReportController 完全没有权限控制
3. 同一模块内安全策略不一致，增加维护难度

**严重等级:** 🟠 **Medium**

---

### 🟠 Medium: 敏感数据暴露风险

**文件:** `ExportService.java`

**问题描述:**
1. 导出文件路径直接暴露在API响应中
2. 文件下载端点没有验证当前用户是否为导出任务的创建者
3. 攻击者可以通过猜测ID下载任意导出文件

**风险影响:**
- 敏感导出文件被未授权下载
- 泄露业务数据和用户隐私

**严重等级:** 🟠 **Medium**

---

## 二、数据验证与完整性问题

### 🔴 High: 输入验证不完整

**文件:** `DashboardController.java` 中的 `createDashboard` 方法

**问题描述:**
1. 虽然DTO层有基本验证，但控制器层重复传递了`request.getUserId()`
2. 控制器层使用 `ensureCurrentUserOwnsRequestedUser` 验证用户，但服务层又重新验证
3. 存在重复验证逻辑，但更重要的是：当创建仪表盘时，应该**强制使用当前用户ID**，而不是接受请求中的用户ID

**风险影响:**
- 用户可以伪造创建其他用户的仪表盘
- 虽然有控制器层验证，但如果API被直接调用可能绕过验证

**严重等级:** 🔴 **High**

---

### 🟠 Medium: 硬编码验证逻辑

**文件:** `Dashboard.java`, `DataExport.java`, `Report.java`

**问题描述:**
1. 验证逻辑分散在各个实体类中
2. 格式验证（如导出格式、报告类型）使用硬编码字符串比较
3. 缺乏集中式验证管理

**建议:**
```java
// 集中式验证示例
public enum ExportFormat {
    EXCEL("EXCEL", ".xlsx"),
    CSV("CSV", ".csv"),
    PDF("PDF", ".pdf");
    
    private final String code;
    private final String extension;
    
    // 构造方法和验证方法
    public static boolean isValidFormat(String format) {
        return Arrays.stream(values()).anyMatch(f -> f.code.equals(format));
    }
}
```

**严重等级:** 🟠 **Medium**

---

### 🟠 Medium: 配置字段大小未限制

**文件:** `Dashboard.java`

**问题描述:**
```java
@Column(name = "config", columnDefinition = "TEXT")
private String config;  // 未限制长度
```
1. config字段使用TEXT类型但未限制最大长度
2. 恶意用户可以提交超大配置导致数据库性能问题

**严重等级:** 🟠 **Medium**

---

### 🟡 Low: 缺少参数验证

**文件:** 多处存在

**问题描述:**
1. `ExportService.exportToExcel` 和 `exportToCSV` 方法未验证输入数据和headers是否为空
2. `DashboardSummaryService` 中的原生SQL查询未进行参数化，存在SQL注入风险
3. `getDepartmentProgress` 方法直接拼接SQL字符串

**风险影响:**
- 潜在的SQL注入漏洞
- 空指针异常和数据处理错误

**严重等级:** 🟡 **Low**

---

## 三、API端点与权限分析

### 🟠 Medium: API端点设计问题

**文件:** 各Controller

**问题描述:**
1. **DataExportController**: `/user/{requestedBy}` 端点直接暴露用户ID，无需身份验证
2. **ReportController**: `/user/{generatedBy}` 端点同样存在直接暴露问题
3. **日期范围查询**没有分页参数，大数据量时会导致性能问题
4. **搜索端点**缺少用户上下文，任何人都可以搜索所有报告和导出

**严重等级:** 🟠 **Medium**

---

### 🟡 Low: 端点命名不一致

**问题描述:**
1. DashboardController 使用 `/api/v1/analytics/dashboard`
2. DashboardSummaryController 使用 `/api/v1/dashboard`
3. DataExportController 使用 `/api/v1/analytics/exports`
4. ReportController 使用 `/api/v1/analytics/reports`

命名空间不一致，增加API使用复杂度。

**严重等级:** 🟡 **Low**

---

## 四、数据库查询与性能

### 🔴 High: 原生SQL查询风险

**文件:** `DashboardSummaryService.java`

**问题描述:**
1. 使用原生SQL查询直接访问外部表（indicator, sys_org, alert_event）
2. 缺乏参数化查询，存在SQL注入风险
3. 硬编码的表名和字段名，耦合度高
4. 没有分页限制，大数据量表查询会导致性能问题

**攻击示例:**
```java
// 潜在SQL注入
private long countScalar(String sql) {
    Object result = entityManager.createNativeQuery(sql).getSingleResult();
    return toLong(result);
}

// 调用时如果sql参数被篡改
countScalar("SELECT COUNT(*) FROM indicator WHERE is_deleted = false; DROP TABLE indicator;");
```

**严重等级:** 🔴 **High**

---

### 🟠 Medium: 无分页查询

**文件:** 多处列表查询接口

**问题描述:**
1. `findDashboardsByUserId`, `findAllPublicDashboards` 等方法返回全量数据
2. 没有分页参数，当数据量大时会导致内存问题和性能下降
3. 前端无法高效加载大量数据

**严重等级:** 🟠 **Medium**

---

### 🟠 Medium: 重复查询

**文件:** `DashboardSummaryService.java`

**问题描述:**
1. 多次执行相似的统计查询
2. 可以优化为单次查询获取所有统计数据
3. 缺乏缓存机制，频繁查询会增加数据库负载

**严重等级:** 🟠 **Medium**

---

### 🟡 Low: 硬编码表名

**文件:** `DashboardSummaryService.java`

**问题描述:**
1. 硬编码访问外部表：indicator, sys_org, alert_event
2. 当数据库表结构变更时需要修改代码
3. 缺乏配置化管理

**严重等级:** 🟡 **Low**

---

## 五、代码质量与可维护性

### 🔴 High: 代码重复

**问题描述:**
1. 各个Repository中的查询方法存在大量重复代码模式
2. 各个ApplicationService中的`publishAndSaveEvents`方法完全相同
3. Controller中的实体转换逻辑重复

**建议:**
```java
// 抽象通用方法
@Service
@RequiredArgsConstructor
public abstract class AbstractApplicationService {
    private final DomainEventPublisher eventPublisher;
    private final EventStore eventStore;
    
    protected void publishAndSaveEvents(AggregateRoot<?> aggregate) {
        List<DomainEvent> events = aggregate.getDomainEvents();
        if (events != null && !events.isEmpty()) {
            for (DomainEvent event : events) {
                eventStore.save(event);
            }
            eventPublisher.publishAll(events);
            aggregate.clearEvents();
        }
    }
}
```

**严重等级:** 🔴 **High**

---

### 🟠 Medium: 硬编码字符串

**文件:** 多处存在

**问题描述:**
1. 状态字符串硬编码：`STATUS_PENDING`, `STATUS_PROCESSING` 等
2. 导出格式硬编码：`EXCEL`, `CSV`, `PDF`
3. 报告类型硬编码：`TYPE_STRATEGIC`, `TYPE_EXECUTION` 等
4. 缺少枚举类集中管理

**严重等级:** 🟠 **Medium**

---

### 🟠 Medium: 异常处理不一致

**文件:** 多处存在

**问题描述:**
1. 使用 `IllegalArgumentException` 和 `AccessDeniedException` 等不同异常
2. 异常信息格式不统一
3. 缺乏全局异常处理

**严重等级:** 🟠 **Medium**

---

### 🟡 Low: 日志记录不完善

**文件:** 多处存在

**问题描述:**
1. 部分方法缺少日志记录
2. 异常日志级别不一致
3. 缺乏审计日志记录重要操作

**严重等级:** 🟡 **Low**

---

### 🟡 Low: 文档缺失

**文件:** 多处存在

**问题描述:**
1. 部分方法缺少JavaDoc注释
2. API端点缺乏详细的接口文档
3. 模块整体架构文档不足

**严重等级:** 🟡 **Low**

---

## 六、领域模型与DDD合规性

### ✅ 亮点: 正确的DDD分层

**优点:**
1. 清晰的分层架构：domain, application, infrastructure, interfaces
2. 聚合根设计正确：Dashboard, DataExport, Report 都继承自 AggregateRoot
3. 仓储模式正确使用
4. 领域事件支持完善

---

### ✅ 亮点: 软删除实现

**优点:**
1. 所有实体都正确实现了软删除模式
2. 查询方法都正确过滤了已删除记录
3. 统一的删除逻辑

---

### ✅ 亮点: 领域事件

**优点:**
1. 正确使用了领域事件模式
2. 事件存储和发布机制完善
3. Report实体正确定义了领域事件

---

### 🟠 Medium: 领域事件缺失

**文件:** `Dashboard.java`, `DataExport.java`

**问题描述:**
1. Dashboard和DataExport实体没有定义领域事件
2. 缺少关键业务事件：DashboardCreated, DashboardUpdated, DataExportCreated 等
3. 无法通过事件驱动机制实现业务扩展

**严重等级:** 🟠 **Medium**

---

### 🟡 Low: equals/hashCode 不完整

**文件:** `Dashboard.java`, `DataExport.java`, `Report.java`

**问题描述:**
1. equals/hashCode方法未包含所有业务字段
2. 可能导致集合操作错误

**严重等级:** 🟡 **Low**

---

## 七、架构最佳实践

### 🟠 Medium: 模块职责不符

**问题描述:**
1. 模块名为 `sism-analytics`，但主要功能是CRUD管理
2. 真正的数据分析功能仅在DashboardSummaryService中有限实现
3. 缺乏真正的数据分析、统计和可视化生成功能

**建议:**
1. 重命名模块为 `sism-dashboard-management`
2. 或扩展模块添加真正的数据分析功能

**严重等级:** 🟠 **Medium**

---

### 🟡 Low: 缺乏配置类

**问题描述:**
1. 导出文件路径 `EXPORT_BASE_PATH` 硬编码
2. 保留天数等参数硬编码
3. 缺乏统一的配置管理

**严重等级:** 🟡 **Low**

---

### 🟡 Low: 测试覆盖不足

**问题描述:**
1. 部分服务和控制器缺乏单元测试
2. 集成测试覆盖不完整
3. 异常场景测试不足

**严重等级:** 🟡 **Low**

---

## 八、审计总结

### 问题统计

| 严重等级 | 数量 | 类别分布 |
|----------|------|----------|
| 🔴 Critical | 4 | 安全漏洞、权限控制、SQL注入 |
| 🔴 High | 4 | 归属验证、代码重复、查询风险 |
| 🟠 Medium | 9 | 权限不一致、API设计、性能问题、代码质量 |
| 🟡 Low | 6 | 代码质量、文档、架构问题 |

### 最紧急修复项

| 优先级 | 问题 | 影响 |
|--------|------|------|
| P0 | 所有API端点添加权限控制 | 直接阻止越权访问 |
| P0 | 修复DataExport和Report模块的归属验证 | 防止数据被未授权操作 |
| P1 | 修复DashboardSummaryService的SQL注入风险 | 潜在的数据泄露和破坏 |
| P1 | 删除重复代码，抽象通用方法 | 提高代码可维护性 |
| P2 | 添加分页支持 | 提高系统性能 |

### 整体评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 安全性 | 🔴 严重不足 | 大部分API无权限控制，存在SQL注入风险 |
| 可靠性 | 🟠 需改进 | 存在数据验证问题和异常处理不一致 |
| 性能 | 🟠 需改进 | 无分页查询，存在性能瓶颈 |
| 可维护性 | 🟠 需改进 | 代码重复，文档不足 |
| 架构合规性 | ✅ 良好 | DDD分层正确，软删除和领域事件设计合理 |

### 亮点

1. **DDD分层架构**: 清晰的四层架构设计
2. **软删除实现**: 统一的软删除模式
3. **领域事件框架**: 完善的事件发布机制
4. **DTO验证**: 基本的请求参数验证
5. **统一异常**: 服务层统一使用标准异常类型

### 关键建议

1. **立即修复权限控制**: 为所有API端点添加 `@PreAuthorize` 注解
2. **添加归属验证**: 在服务层验证用户对数据的所有权
3. **修复SQL注入风险**: 使用参数化查询替代直接字符串拼接
4. **抽象通用代码**: 提取重复的领域事件发布逻辑
5. **添加分页支持**: 为所有列表查询接口添加分页参数
6. **统一API命名**: 标准化所有API端点的命名空间
7. **引入枚举类**: 集中管理状态、格式和类型常量
8. **添加审计日志**: 记录重要操作便于审计
9. **完善文档**: 添加JavaDoc和API文档
10. **扩展模块功能**: 添加真正的数据分析能力

---

**审计完成日期:** 2026-04-06
**下一步行动:** 优先修复权限控制和SQL注入漏洞，再逐步改进其他问题
