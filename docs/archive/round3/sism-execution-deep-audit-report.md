# sism-execution 模块第三轮深度审计报告

**审计日期:** 2026-04-06
**审计人员:** Claude Code (Automated Deep Audit)
**模块版本:** 当前主分支
**审计轮次:** 第三轮

---

## 模块概览

| 项目 | 数据 |
|------|------|
| 模块名称 | sism-execution |
| 模块职责 | 计划报告管理、里程碑管理、执行进度跟踪 |
| Java 文件总数 | 47 |
| 核心实体 | PlanReport, Milestone |
| Repository 数量 | 5 |
| Service 数量 | 2 |
| Controller 数量 | 2 |

### 包结构

```
com.sism.execution/
├── application/
│   ├── MilestoneApplicationService.java
│   └── ReportApplicationService.java
├── domain/
│   ├── model/
│   │   ├── milestone/
│   │   │   └── Milestone.java
│   │   └── report/
│   │       ├── PlanReport.java
│   │       ├── ReportOrgType.java
│   │       └── event/
│   │           ├── PlanReportApprovedEvent.java
│   │           ├── PlanReportRejectedEvent.java
│   │           └── PlanReportSubmittedEvent.java
│   ├── repository/
│   │   ├── ExecutionMilestoneRepository.java
│   │   ├── PlanReportAttachmentSnapshot.java
│   │   ├── PlanReportIndicatorRepository.java
│   │   ├── PlanReportIndicatorSnapshot.java
│   │   └── PlanReportRepository.java
│   └── service/
│       └── PlanReportDomainService.java
├── infrastructure/
│   ├── ExecutionModuleConfig.java
│   └── persistence/
│       ├── JdbcPlanReportIndicatorRepository.java
│       ├── JpaExecutionMilestoneRepository.java
│       ├── JpaExecutionMilestoneRepositoryInternal.java
│       ├── JpaMilestoneRepositoryInternal.java
│       └── JpaPlanReportRepository.java
└── interfaces/
    ├── dto/
    │   ├── ApprovePlanReportRequest.java
    │   ├── CreateMilestoneRequest.java
    │   ├── CreatePlanReportRequest.java
    │   ├── MilestoneResponse.java
    │   ├── PlanReportIndicatorDetailResponse.java
    │   ├── PlanReportQueryRequest.java
    │   ├── PlanReportResponse.java
    │   ├── PlanReportSimpleResponse.java
    │   ├── RejectPlanReportRequest.java
    │   ├── ReportAttachmentResponse.java
    │   ├── SubmitPlanReportRequest.java
    │   ├── UpdateMilestoneRequest.java
    │   ├── UpdatePlanReportIndicatorDetailRequest.java
    │   └── UpdatePlanReportRequest.java
    └── rest/
        ├── MilestoneController.java
        └── ReportController.java
```

---

## 一、任务执行和进度跟踪审计

### ✅ 已修复的关键问题

1. **计划报告数据持久化问题已解决**
   - 所有关键字段（title、content、summary、progress、issues、nextPlan 等）已从 @Transient 改为 @Column 持久化映射
   - 仅 indicatorDetails 保留 @Transient 作为计算字段
   - 修复了数据无法保存到数据库的严重 Bug

2. **里程碑字段持久化优化**
   - Milestone.inheritedFrom 字段已添加 @Column 注解，不再是临时字段
   - 修复了里程碑数据无法持久化的问题

3. **状态常量一致性修复**
   - 统一了状态常量命名与实际值
   - 添加了 STATUS_SUBMITTED_LEGACY = "IN_REVIEW" 保持向后兼容

4. **报告审批流程完善**
   - 所有审批/提交/删除操作已添加权限验证
   - 实现了用户身份一致性检查
   - 完善了状态转换校验逻辑

### 🟠 仍需改进的任务管理问题

#### 1. 任务进度填报权限控制不完整
**严重等级:** 🟠 Medium
**文件:** ReportController.java, MilestoneController.java

**问题描述:**
- 部分更新操作（如 updateReport）和所有 GET 查询端点缺少权限控制
- GET 端点返回所有数据，未做组织级别的数据隔离
- 未验证用户是否有权限访问特定报告或里程碑

**风险影响:**
- 数据泄露风险：任何认证用户都可以查询其他组织的报告和里程碑
- 未授权操作：用户可以修改不属于自己组织的数据

#### 2. 里程碑状态管理缺失
**严重等级:** 🟠 Medium
**文件:** Milestone.java, MilestoneApplicationService.java

**问题描述:**
- Milestone 类使用字符串常量表示状态，缺乏类型安全
- 没有状态转换验证逻辑
- 缺少里程碑状态变更的历史记录
- 没有统一的状态管理方法

**风险影响:**
- 状态值可能不一致
- 难以维护和扩展状态逻辑
- 缺乏审计追溯能力

#### 3. 任务进度同步逻辑存在N+1查询问题
**严重等级:** 🟠 Medium
**文件:** ReportApplicationService.java, line 535-547

**问题描述:**
- syncApprovedIndicatorProgress 方法对每个指标单独查询和更新
- 当报告包含大量指标时，会触发多次数据库查询

**风险影响:**
- 数据库性能下降
- 响应延迟增加
- 高并发场景下可能出现性能瓶颈

---

## 二、API 安全和权限审计

### ✅ 已修复的关键问题

1. **核心操作权限控制已实现**
   - 审批、驳回、提交、创建等写操作已添加 @PreAuthorize 注解
   - 实现了服务层编程式权限验证（requireAnyRole）
   - 实现了用户身份一致性检查（resolveUserId）

2. **组织级权限验证基础**
   - 部分接口已实现数据过滤
   - 权限验证逻辑已集成到服务层

### ⚠️ 部分修复的问题

1. **大部分 API 端点权限控制不完善**
**严重等级:** 🟠 High
**文件:** ReportController.java (Lines 54-86, 135-303), MilestoneController.java

**问题描述:**
- updateReport 端点未添加权限控制
- 所有 GET 查询端点未做组织级别的数据隔离
- 未实现 `ensureCanAccessReport` 和 `filterReportsByPermission` 类的权限过滤方法
- 任何认证用户都可以访问所有报告和里程碑数据

**风险影响:**
- 敏感数据泄露风险
- 组织间数据隔离失效
- 违反数据安全合规要求

#### 2. 权限验证逻辑重复
**严重等级:** 🟠 Low
**文件:** ReportApplicationService.java

**问题描述:**
- 权限验证逻辑重复出现在多个方法中
- 没有统一的权限验证工具类
- 代码冗余，难以维护

---

## 三、数据库性能审计

### ✅ 已修复的关键问题

1. **报告查询性能优化**
   - 批量查询 submittedBy 和 approvalSnapshots 数据
   - 使用 IN (...) 替代逐条查询
   - 减少了 N+1 查询问题

### 🔴 高风险数据库问题

#### 1. 指标详情查询仍存在N+1问题
**严重等级:** 🟠 Medium
**文件:** ReportApplicationService.java, line 596-599

**问题描述:**
- enrichReportMetadata 方法中对每个报告单独查询 indicatorDetails
- 当返回大量报告列表时，会触发大量数据库查询

**风险影响:**
- 数据库性能下降
- API 响应延迟增加
- 高并发场景下性能问题加剧

#### 2. 跨模块直接更新数据库
**严重等级:** 🟠 Medium
**文件:** ReportApplicationService.java, line 318-326, 354-362, 404-413

**问题描述:**
- 直接使用 JdbcTemplate 更新 audit_instance 和 plan 表
- 违反了领域驱动设计的分层架构原则
- 直接操作其他模块的数据库表

**风险影响:**
- 模块间耦合度增加
- 数据一致性难以保证
- 难以进行事务管理和回滚

#### 3. 分页查询内存处理
**严重等级:** 🟠 Low
**文件:** MilestoneApplicationService.java, line 213-226

**问题描述:**
- convertListToPage 方法在内存中进行分页
- 当数据量较大时，会占用大量内存

**风险影响:**
- 内存资源浪费
- 响应延迟增加

---

## 四、业务逻辑验证审计

### ✅ 已修复的关键问题

1. **业务规则校验增强**
   - 报告提交审批前验证状态合法性
   - 指标进度填报验证逻辑完善
   - 报告唯一性检查已实现

2. **事件驱动架构完善**
   - 领域事件发布机制已实现
   - 报告审批状态变更事件已添加
   - 事件发布和保存逻辑已完善

### 🔴 高风险业务逻辑问题

#### 1. 异常处理不规范
**严重等级:** 🟠 Medium
**文件:** 多处文件

**问题描述:**
- 仍在使用 IllegalArgumentException 和 IllegalStateException 作为业务异常
- 异常消息混合中英文（英文如 "Report not found"，中文如 "当前月份已有报告正在审批中"）
- 没有统一的业务异常类
- 缺乏全局异常处理机制

**风险影响:**
- 错误信息不规范
- 难以进行统一的异常处理
- 用户体验差
- 运维排查困难

#### 2. 直接使用 System.err 打印日志
**严重等级:** 🟠 Low
**文件:** ReportApplicationService.java, line 329, 365

**问题描述:**
- 两处 catch 块仍使用 System.err.println 而非日志框架
- 不符合企业级应用的日志规范

**风险影响:**
- 日志管理混乱
- 无法通过日志系统收集和分析错误信息

#### 3. 领域服务未充分发挥作用
**严重等级:** 🟠 Low
**文件:** PlanReportDomainService.java

**问题描述:**
- PlanReportDomainService 接口未被充分使用
- 部分业务逻辑仍直接实现在应用服务中
- 没有充分发挥领域驱动设计的优势

---

## 五、集成与外部依赖审计

### ✅ 已修复的关键问题

1. **跨模块依赖基础完善**
   - 集成了 IndicatorRepository 用于指标查询
   - 实现了与审计模块的基本集成
   - 与附件模块的集成已基本实现

### 🟠 仍需改进的集成问题

#### 1. 跨模块直接访问其他模块的 Repository
**严重等级:** 🟠 Medium
**文件:** ReportApplicationService.java, line 14, 214-215

**问题描述:**
- 直接注入 IndicatorRepository 访问其他模块的数据
- 违反了领域驱动设计的依赖倒置原则
- 应该通过领域服务或防腐层访问其他模块

**风险影响:**
- 模块间耦合度增加
- 难以进行模块替换和升级
- 违反了微服务架构的边界原则

#### 2. 硬编码默认值
**严重等级:** 🟠 Low
**文件:** PlanReport.java, line 127

**问题描述:**
- planId 默认值硬编码为 1L：`report.planId = planId != null ? planId : 1L;`
- 缺乏合理的默认值处理策略

**风险影响:**
- 数据不一致风险
- 难以追踪和调试

---

## 六、审计总结

### 问题统计

| 严重等级 | 数量 | 类别分布 | 状态 |
|----------|------|----------|------|
| 🔴 Critical | 0 | - | ✅ 已修复 |
| 🔴 High | 1 | 权限控制 | ⚠️ 部分修复 |
| 🟠 Medium | 7 | 任务管理、数据库、业务逻辑、集成 | 部分修复 |
| 🟡 Low | 3 | 代码质量、架构设计 | ❌ 未修复 |

### 最紧急修复项

| 优先级 | 问题 | 影响 |
|--------|------|------|
| P1 | API 端点权限控制不完善 | 数据泄露和未授权访问风险 |
| P2 | 指标详情查询N+1问题 | 数据库性能下降 |
| P2 | 跨模块直接更新数据库 | 架构违规和数据一致性问题 |
| P3 | 异常处理不规范 | 代码质量和维护性问题 |

### 整体评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 安全性 | 🟠 需改进 | 核心操作已加固，但大部分读操作无权限控制 |
| 可靠性 | ✅ 良好 | 数据持久化问题已解决，核心功能稳定 |
| 性能 | 🟠 需改进 | 存在N+1查询问题，需进一步优化 |
| 可维护性 | 🟠 需改进 | 异常处理和代码质量有待提升 |
| 集成架构 | 🟠 需改进 | 模块间耦合度较高，违反DDD原则 |

### 亮点

1. **DDD 分层结构清晰** - domain/application/infrastructure/interfaces 包结构符合领域驱动设计规范
2. **事件驱动机制完善** - 领域事件发布和处理逻辑已实现
3. **权限控制基础架构已建立** - 核心写操作已添加权限验证
4. **数据持久化问题已全面修复** - 所有关键字段现在正确保存到数据库
5. **批量查询优化** - 部分N+1问题已通过批量查询解决

### 关键建议

1. **立即修复API权限控制** - 为所有GET端点添加组织级别的数据隔离
2. **优化指标详情查询** - 改为批量查询 indicatorDetails 数据
3. **移除跨模块直接数据库更新** - 通过领域服务或事件驱动方式更新其他模块数据
4. **统一异常处理** - 引入统一的业务异常类和全局异常处理器
5. **替换System.err日志** - 使用SLF4J日志框架替代直接打印
6. **优化里程碑状态管理** - 使用枚举类替代字符串常量，添加状态转换验证

---

## 七、修复优先级路线图

### 第一阶段（P0-P1）- 立即修复（0-2周）
1. **修复API权限控制不完善问题** - 为所有端点添加适当的权限验证
2. **优化指标详情查询N+1问题** - 实现批量查询 indicatorDetails
3. **移除跨模块直接数据库更新** - 替换JdbcTemplate直接更新操作

### 第二阶段（P2）- 短期优化（2-4周）
4. **统一异常处理** - 引入业务异常类和全局异常处理器
5. **替换System.err日志** - 使用SLF4J日志框架
6. **优化里程碑状态管理** - 使用枚举类重构里程碑状态

### 第三阶段（P3）- 长期改进（4-8周）
7. **引入领域服务** - 充分发挥 PlanReportDomainService 的作用
8. **优化模块间依赖** - 通过防腐层访问其他模块的Repository
9. **添加操作审计日志** - 记录所有报告和里程碑的变更历史
10. **完善单元测试和集成测试** - 提高代码覆盖率和质量

---

**审计完成日期:** 2026-04-06
**下一步行动:** 按修复优先级路线图实施修复，重点关注 P0 和 P1 级问题
