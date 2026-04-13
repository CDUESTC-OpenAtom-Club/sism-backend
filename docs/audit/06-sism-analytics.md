# 审计报告：sism-analytics 模块（数据分析/导出）

**审计日期:** 2026-04-12
**审计范围:** 37个Java源文件，涵盖interfaces/application/domain/infrastructure四层。

---

## 一、Critical 严重 (共4个)

### C-01. 路径遍历漏洞 - 导出文件下载接口可被利用读取任意文件
**状态:** 已修复（2026-04-12）
**文件:** `ExportService.java:259-277`
**描述:** `resolveExportPath` 方法检查 `resolved.startsWith(exportRoot)`，但 `exportRoot` 依赖配置值 `export.temp-dir`（默认 `./exports`），若被篡改为 `/`，所有文件可被访问。`DataExportController.java:315-324` 的下载端点使用 `Files.readAllBytes(filePath)` 将整个文件读入内存。
**修复建议:** 对 `exportBasePath` 增加白名单校验，禁止配置为根路径；解析路径后增加 `toRealPath()` 调用；限制下载文件大小上限或使用流式输出。

### C-02. 前端传入文件路径，可构造任意文件读取攻击链
**状态:** 已修复（2026-04-12）
**文件:** `DataExportController.java:74-88`, `ReportController.java:60-74`
**描述:** `completeDataExport` 和 `generateReport` 接口曾接受前端传入的 `filePath`。现已改为由后端统一生成并托管受控路径，Request DTO 不再暴露 `filePath` 字段，下载链路无法再借此读取任意文件。
**修复建议:** 已完成。保留 `fileSize` 作为请求元数据不会改变文件定位逻辑。

### C-03. 缺乏授权控制的"无用户版本"服务方法
**状态:** 已修复（2026-04-12）
**文件:** `DataExportApplicationService.java:45-50,63-69,82-88,101-107,121-126`; `ReportApplicationService.java:44-50,64-69,83-88,101-107`
**描述:** 多个服务方法存在不带 `currentUserId` 参数的重载版本（如 `startProcessing(Long exportId)`），不进行所有权校验。任何已认证用户都可操作其他用户的导出任务和报告。虽然 Controller 调用的是带用户参数版本，但公开方法是定时炸弹。
**修复建议:** 将不带 `currentUserId` 的方法改为 `private` 或 `protected`。

### C-04. SQL 查询结果列索引硬编码，数据错位风险
**状态:** 已修复（2026-04-12）
**文件:** `infrastructure/persistence/NativeDashboardSummaryQueryRepository.java:122-180`
**描述:** 通过 `Object[] row` 的索引（`row[0]` 到 `row[7]`）映射查询结果。若 SELECT 列顺序变化，映射完全错位且不抛异常，造成静默数据错误。
**修复建议:** 使用 JPA `SqlResultSetMapping` 或 `Tuple` 接口通过列名获取数据；或增加显式列数断言。

---

## 二、High 高 (共6个)

### H-01. 文件下载将整个文件读入内存 - 大文件 OOM
**状态:** 已修复（2026-04-12）
**文件:** `DataExportController.java:315-324`
**描述:** `Files.readAllBytes(filePath)` 一次性加载整个文件到堆内存，大文件导致 OOM。
**修复建议:** 使用 `StreamingResponseBody` 或 `Resource` 流式传输，设置文件大小上限。

### H-02. 缓存键固定共享，无手动刷新机制
**状态:** 已修复（2026-04-12）
**文件:** `DashboardSummaryService.java:39,75,87`; `AnalyticsModuleConfig.java:19-30`
**描述:** 三个缓存仍使用共享 key，但已补充显式缓存清除能力，`DashboardSummaryController` 现在提供受认证保护的刷新入口，`DashboardSummaryService` 也暴露统一失效方法。
**修复建议:** 已完成。后续如需细分用户维度缓存，再按业务口径扩展 key 设计。

### H-03. 导出方法在同一事务中执行文件 I/O
**状态:** 已修复（2026-04-12）
**文件:** `ExportService.java:47-106,111-153`
**描述:** `exportToExcel()` 和 `exportToCSV()` 已移除方法级 `@Transactional`，数据库状态切换继续通过应用服务各自的事务完成，文件 I/O 不再包在长事务里。
**修复建议:** 已完成。

### H-04. 大量重复代码违反 DRY 原则
**状态:** 已修复（2026-04-12）
**文件:** `DataExportApplicationService.java`, `ReportApplicationService.java`, `DataExportController.java`, `ReportController.java`
**描述:** `publishAndSaveEvents()` 已统一下沉到 `BaseApplicationService`，三个 ApplicationService 不再各自复制一份事件保存/发布实现。
**修复建议:** 已完成。本轮未继续抽 controller 基类，但核心重复热点已收敛。

### H-05. LIKE 注入风险 - 搜索参数未转义特殊字符
**状态:** 已修复（2026-04-12）
**文件:** `DashboardRepository.java:65`, `DataExportRepository.java:82,140`, `ReportRepository.java:131,143`
**描述:** 搜索参数现在会先在应用层做 `%` / `_` / `\\` 转义，Repository 查询也已统一使用 `ESCAPE '\\'`，通配符不再被用户输入直接放大。
**修复建议:** 已完成。

### H-06. deleteDashboard 领域事件时序不明确
**状态:** 已修复（2026-04-12）
**文件:** `DashboardApplicationService.java:86-93`
**描述:** `Dashboard.delete()` 现在会在状态变更时同步注册 `DashboardDeletedEvent`，应用服务仅负责持久化和发布，不再存在“先取旧事件列表再删”的时序歧义。
**修复建议:** 已完成。

---

## 三、Medium 中等 (共8个)

| # | 文件:行号 | 问题 | 类别 |
|---|---|---|---|
| M-01 | `Report.java:144-166` | 已修复（2026-04-13）：`generate()` 现在只允许 DRAFT 状态执行，`fail()` 也增加了状态前置条件 | Bug |
| M-02 | `Dashboard.java:58-75` | 已修复（2026-04-13）：`validate()` 和 `updateConfig()` 现在会拒绝空白 `description/config` | Bug |
| M-03 | `DataExport.java:77-78` | 已修复（2026-04-13）：`createdAt` 已改为委托 `requestedAt`，不再出现持久化后始终为 null 的响应 | Bug |
| M-04 | `AnalyticsModuleConfig.java:19-30` | 已修复（2026-04-13）：分析模块缓存管理器已改为具名 Bean `analyticsCacheManager`，不再占用通用 `cacheManager` 名称 | 架构 |
| M-05 | `Report.java:22`, `DataExport.java:21` | 已核实关闭（2026-04-13）：`progress_report` 与 `adhoc_task` 为基线 schema 中现存的兼容旧表映射，并非当前代码与数据库不一致 | 架构 |
| M-06 | `ExportService.java:226-250` | 已修复（2026-04-13）：过期导出清理改为批量软删除并统一 `saveAll`，不再逐条触发保存 | 性能 |
| M-07 | `DashboardSummaryService.java:27-32` | 已修复（2026-04-13）：未使用常量已删除，阈值收敛为实际使用的命名常量 | 代码质量 |
| M-08 | 三个 ApplicationService | 已修复（2026-04-13）：`publishAndSaveEvents` 增加事件保存补偿，保存链路失败时会回滚已写入事件，避免部分保存 | 架构 |

---

## 四、Low 低 (共8个)

| # | 文件:行号 | 问题 |
|---|---|---|
| L-01 | `DashboardSummaryService.java:116-122` | 已修复（2026-04-13）：阈值 80/50 已提取为命名常量 |  |
| L-02 | `DashboardSummaryService.java:125-127` | 已修复（2026-04-13）：`round2()` 改为 `BigDecimal` 舍入，避免 double 精度丢失 |  |
| L-03 | `ExportFormat.java` 与 `DataExport`/`Report` | 已修复（2026-04-13）：导出格式常量已统一回源到 `ExportFormat` |  |
| L-04 | `DashboardSummaryService.java:74,85` | 已修复（2026-04-13）：多余的 `@SuppressWarnings(\"unchecked\")` 已删除 |  |
| L-05 | `DataExportDTO.java:37-38`, `ReportDTO.java:48,78` | 已修复（2026-04-13）：控制器返回 DTO 时只暴露文件名，并对敏感错误信息做脱敏 |  |
| L-06 | `AnalyticsPaginationSupport.java:9` | 已修复（2026-04-13）：分页工具已改为 `public` 便于跨包复用 |  |
| L-07 | `CreateDataExportRequest.java:29-30` | 已修复（2026-04-13）：`requestedBy/generatedBy` 已从创建 DTO 移除，身份统一来自登录态 |  |
| L-08 | `UpdateDataExportRequest.java` 全文 | 已修复（2026-04-13）：未使用 DTO 已删除 |  |

---

## 汇总统计

| 严重性 | 数量 | 关键主题 |
|--------|------|----------|
| **Critical** | 4 | 路径遍历、任意文件读取、无授权方法、SQL列索引硬编码 |
| **High** | 6 | 大文件OOM、缓存无刷新、事务内I/O、DRY违反、LIKE注入 |
| **Medium** | 8 | 状态转换不严谨、@Transient字段、表名不匹配、批量删除性能 |
| **Low** | 8 | 魔法数字、精度丢失、内部信息泄露、死代码 |
| **总计** | **26** | |

**优先修复建议：**
1. **C-01/C-02** 路径遍历 + 任意文件读取攻击链 — 立即修复
2. **C-03** 无授权公开方法 — 安全隐患
3. **H-03** 事务内文件 I/O — 生产环境稳定性
4. **H-01** 大文件下载 OOM — 生产环境稳定性

**当前结论（2026-04-13）:** 本文档条目已全部收口。除 `M-05` 为核实后的兼容旧表映射关闭项外，其余问题均已在 `sism-analytics` 模块内完成修复并通过定向测试验证。
