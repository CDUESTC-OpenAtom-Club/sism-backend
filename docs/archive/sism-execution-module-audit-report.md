# sism-execution 模块审计报告

**审计日期:** 2026-04-06
**审计人员:** Claude Code (Automated Audit)
**模块版本:** 当前主分支

---

## 模块概览

| 项目 | 数据 |
|------|------|
| 模块名称 | sism-execution |
| 模块职责 | 计划执行管理、月度报告、里程碑管理 |
| Java 文件总数 | 36 |
| 核心实体 | PlanReport, Milestone |
| Repository 数量 | 4 |
| Service 数量 | 2 |
| Controller 数量 | 2 |

### 包结构

```
com.sism.execution/
├── application/
│   ├── ReportApplicationService.java
│   └── MilestoneApplicationService.java
├── domain/
│   ├── model/
│   │   ├── report/
│   │   │   ├── PlanReport.java
│   │   │   ├── ReportOrgType.java
│   │   │   └── event/
│   │   │       ├── PlanReportSubmittedEvent.java
│   │   │       ├── PlanReportApprovedEvent.java
│   │   │       └── PlanReportRejectedEvent.java
│   │   └── milestone/
│   │       └── Milestone.java
│   ├── repository/
│   │   ├── PlanReportRepository.java
│   │   ├── PlanReportIndicatorRepository.java
│   │   └── ExecutionMilestoneRepository.java
│   └── service/
│       └── PlanReportDomainService.java
├── infrastructure/
│   └── persistence/
│       ├── JpaPlanReportRepository.java
│       ├── JpaExecutionMilestoneRepository.java
│       └── JdbcPlanReportIndicatorRepository.java
└── interfaces/
    ├── dto/
    │   ├── CreatePlanReportRequest.java
    │   ├── UpdatePlanReportRequest.java
    │   ├── PlanReportResponse.java
    │   └── ...
    └── rest/
        ├── ReportController.java
        └── MilestoneController.java
```

---

## 一、安全漏洞

### 🔴 Critical: 所有 API 端点完全缺少权限控制

**文件:** `ReportController.java` 和 `MilestoneController.java`

```java
// ReportController.java
@PostMapping
@Operation(summary = "创建月度报告（草稿）")
public ResponseEntity<ApiResponse<PlanReportResponse>> createReport(...) {
    // ❌ 无权限控制
}

@PostMapping("/{id}/submit")
@Operation(summary = "提交报告")
public ResponseEntity<ApiResponse<PlanReportResponse>> submitReport(...) {
    // ❌ 无权限控制，任何用户可提交报告
}

@PostMapping("/{id}/approve")
@Operation(summary = "审批通过报告")
public ResponseEntity<ApiResponse<PlanReportResponse>> approveReport(...) {
    // ❌ 无权限控制，任何用户可审批报告！
}

@PostMapping("/{id}/reject")
@Operation(summary = "驳回报告")
public ResponseEntity<ApiResponse<PlanReportResponse>> rejectReport(...) {
    // ❌ 无权限控制
}

// MilestoneController.java
@PostMapping
@Operation(summary = "创建里程碑")
public ResponseEntity<ApiResponse<MilestoneResponse>> createMilestone(...) {
    // ❌ 无权限控制
}

@DeleteMapping("/{id}")
@Operation(summary = "删除里程碑")
public ResponseEntity<ApiResponse<Void>> deleteMilestone(...) {
    // ❌ 无权限控制
}
```

**问题描述:**
1. 报告审批/驳回等敏感操作无权限控制
2. 里程碑 CRUD 操作无权限控制
3. 任何认证用户都可以审批/驳回报告

**风险影响:**
- 审批流程可被任意用户执行
- 报告数据可被恶意篡改
- 业务流程被绕过

**严重等级:** 🔴 **Critical**

**建议修复:**
```java
@PostMapping("/{id}/approve")
@PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
@Operation(summary = "审批通过报告")
public ResponseEntity<ApiResponse<PlanReportResponse>> approveReport(...) { }

@PostMapping("/{id}/reject")
@PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
@Operation(summary = "驳回报告")
public ResponseEntity<ApiResponse<PlanReportResponse>> rejectReport(...) { }

@PostMapping
@PreAuthorize("hasAnyRole('ADMIN', 'STRATEGY_DEPT', 'FUNC_DEPT')")
@Operation(summary = "创建月度报告（草稿）")
public ResponseEntity<ApiResponse<PlanReportResponse>> createReport(...) { }
```

---

### 🔴 High: 报告审批无业务规则校验

**文件:** `ReportApplicationService.java`
**行号:** 247-259

```java
@Transactional
public PlanReport approveReport(Long reportId, Long userId) {
    PlanReport report = planReportRepository.findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

    report.approve(userId);  // ❌ 未验证 userId 是否有审批权限
    report = planReportRepository.save(report);
    // ...
}
```

**问题描述:**
1. 未验证审批人是否有权限审批该报告
2. 未验证报告所属组织的审批层级
3. userId 直接从请求参数获取，未验证是否为当前登录用户

**风险影响:**
- 越权审批
- 审批链被绕过

**严重等级:** 🔴 **High**

**建议修复:**
```java
@Transactional
public PlanReport approveReport(Long reportId, Long userId, CurrentUser currentUser) {
    // 验证 userId 与当前登录用户一致
    if (!currentUser.getId().equals(userId)) {
        throw new UnauthorizedException("User ID mismatch");
    }

    // 验证用户有审批权限
    if (!currentUser.canApprove(report.getReportOrgId())) {
        throw new UnauthorizedException("No approval permission for this organization");
    }
    // ...
}
```

---

## 二、潜在 Bug 和逻辑错误

### 🔴 Critical: PlanReport 大量关键字段为 Transient

**文件:** `PlanReport.java`
**行号:** 60-94

```java
@Transient
private String title;

@Transient
private String content;

@Transient
private String summary;

@Transient
private Integer progress;

@Transient
private String issues;

@Transient
private String nextPlan;

@Transient
private Long submittedBy;

@Transient
private Long approvedBy;

@Transient
private LocalDateTime approvedAt;

@Transient
private String rejectionReason;

@Transient
private List<PlanReportIndicatorSnapshot> indicatorDetails = List.of();
```

**问题描述:**
1. **报告内容字段不持久化**: `content`, `summary`, `progress`, `issues`, `nextPlan` 全部是 `@Transient`
2. **审批信息不持久化**: `submittedBy`, `approvedBy`, `approvedAt`, `rejectionReason` 不存储到数据库
3. **updateContent() 方法无效**: 只更新了时间戳，实际内容丢失

**风险影响:**
- 报告内容无法保存
- 审批记录丢失
- 数据完整性问题

**严重等级:** 🔴 **Critical**

**建议修复:**
```java
// 方案1: 添加数据库列
@Column(name = "title", length = 200)
private String title;

@Column(name = "content", columnDefinition = "TEXT")
private String content;

@Column(name = "summary", columnDefinition = "TEXT")
private String summary;

@Column(name = "progress")
private Integer progress;

// 方案2: 使用关联表存储详细信息
@OneToMany(mappedBy = "report")
private List<PlanReportDetail> details;
```

---

### 🔴 High: 状态常量命名与实际值不一致

**文件:** `PlanReport.java`
**行号:** 26-29

```java
public static final String STATUS_DRAFT = "DRAFT";
public static final String STATUS_SUBMITTED = "IN_REVIEW";  // ❌ 常量名是 SUBMITTED，值是 IN_REVIEW
public static final String STATUS_APPROVED = "APPROVED";
public static final String STATUS_REJECTED = "REJECTED";
```

**问题描述:**
常量名 `STATUS_SUBMITTED` 与实际值 `IN_REVIEW` 不一致，可能导致：
1. 代码中使用 `STATUS_SUBMITTED` 但期望值是 `SUBMITTED`
2. 代码中使用字符串 `"SUBMITTED"` 进行比较，永远无法匹配
3. 日志和错误消息混乱

**严重等级:** 🔴 **High**

**建议修复:**
```java
public static final String STATUS_DRAFT = "DRAFT";
public static final String STATUS_SUBMITTED = "SUBMITTED";  // 或改为 STATUS_IN_REVIEW = "IN_REVIEW"
public static final String STATUS_APPROVED = "APPROVED";
public static final String STATUS_REJECTED = "REJECTED";
```

---

### 🟠 Medium: Milestone.inheritedFrom 是 Transient 字段

**文件:** `Milestone.java`
**行号:** 45-46

```java
@Transient
private Long inheritedFrom;  // ❌ 不持久化
```

**问题描述:**
里程碑的继承来源信息不持久化，重启后丢失。

**严重等级:** 🟠 **Medium**

---

### 🟠 Medium: 使用 System.err 而非日志框架

**文件:** `ReportApplicationService.java`
**行号:** 296-299, 333-335

```java
} catch (Exception e) {
    // 记录日志但不影响主流程
    System.err.println("[ReportApplicationService] Failed to update audit_instance: " + e.getMessage());  // ❌ 使用 System.err
}
```

**问题描述:**
使用 `System.err` 而非 SLF4J 日志框架，日志无法被正确收集和管理。

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
} catch (Exception e) {
    log.warn("[ReportApplicationService] Failed to update audit_instance: {}", e.getMessage());
}
```

---

## 三、性能瓶颈

### 🔴 High: enrichReportMetadata 每次返回报告都执行额外查询

**文件:** `ReportApplicationService.java`
**行号:** 567-581

```java
private PlanReport enrichReportMetadata(PlanReport report) {
    if (report == null) {
        return null;
    }

    report.setIndicatorDetails(report.getId() == null
            ? List.of()
            : planReportIndicatorRepository.findByReportId(report.getId()));  // 查询 1

    report.setSubmittedBy(resolveSubmittedBy(report));  // 查询 2

    ApprovalSnapshot approvalSnapshot = resolveApprovalSnapshot(report.getAuditInstanceId());  // 查询 3
    report.setApprovedBy(approvalSnapshot.approvedBy());
    report.setApprovedAt(approvalSnapshot.approvedAt());
    return report;
}
```

**问题描述:**
1. 每个报告对象都触发 1-3 次额外数据库查询
2. 在列表查询时会产生严重的 N+1 问题
3. 如果返回 20 个报告，可能触发 60+ 次数据库查询

**风险影响:**
- 严重的性能问题
- 数据库压力
- 响应延迟

**严重等级:** 🔴 **High**

**建议修复:**
```java
// 使用批量查询
public List<PlanReport> findReportsWithMetadata(List<Long> reportIds) {
    Map<Long, List<PlanReportIndicatorSnapshot>> details =
        planReportIndicatorRepository.findByReportIds(reportIds);
    Map<Long, ApprovalSnapshot> approvals =
        resolveApprovalSnapshots(reportIds);
    // ...
}
```

---

### 🟠 Medium: syncApprovedIndicatorProgress 执行 N+1 更新

**文件:** `ReportApplicationService.java`
**行号:** 517-524

```java
private void syncApprovedIndicatorProgress(Long reportId) {
    for (var detail : planReportIndicatorRepository.findByReportId(reportId)) {
        Indicator indicator = indicatorRepository.findById(detail.indicatorId())  // 查询
                .orElseThrow(() -> new IllegalArgumentException("Indicator not found: " + detail.indicatorId()));
        indicator.setProgress(detail.progress());
        indicatorRepository.save(indicator);  // 逐条更新
    }
}
```

**问题描述:**
每个指标都单独查询和更新，如果有 10 个指标，会产生 20+ 次数据库操作。

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
// 使用批量更新
@Modifying
@Query("UPDATE Indicator i SET i.progress = :progress WHERE i.id = :id")
int updateProgress(@Param("id") Long id, @Param("progress") Integer progress);

// 或使用 JdbcTemplate 批量更新
jdbcTemplate.batchUpdate(sql, batchArgs);
```

---

### 🟠 Medium: 服务层直接使用 JdbcTemplate 更新关联表

**文件:** `ReportApplicationService.java`
**行号:** 285-300

```java
@Transactional
public PlanReport markWorkflowApproved(Long reportId, Long approverId) {
    // ...
    if (report.getAuditInstanceId() != null) {
        try {
            jdbcTemplate.update(
                    """
                    UPDATE public.audit_instance
                    SET status = 'APPROVED', completed_at = ?
                    WHERE id = ?
                    """,
                    LocalDateTime.now(),
                    report.getAuditInstanceId()
            );  // ❌ 直接更新其他模块的表
        } catch (Exception e) {
            System.err.println("...");
        }
    }
}
```

**问题描述:**
1. 服务层直接使用 JdbcTemplate 更新其他模块的表
2. 绕过了领域模型和业务规则
3. 违反了模块边界

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
// 通过领域服务或事件处理
@Autowired
private WorkflowService workflowService;

workflowService.markApproved(report.getAuditInstanceId(), approverId);
```

---

## 四、代码质量和可维护性

### 🟠 Medium: 异常消息混合中英文

**文件:** `ReportApplicationService.java` 和 `PlanReport.java`

```java
// ReportApplicationService.java
throw new IllegalArgumentException("Report not found: " + reportId);  // 英文
throw new IllegalStateException("当前月份已有报告正在审批中，请等待审批完成或先撤回");  // 中文
throw new IllegalArgumentException("填报进度必须大于真实进度，当前真实进度为 " + currentProgress + "%");  // 中文

// PlanReport.java
throw new IllegalArgumentException("Report month cannot be null or empty");  // 英文
throw new IllegalStateException("Cannot submit report: not in DRAFT status");  // 英文
```

**严重等级:** 🟠 **Medium**

---

### 🟠 Medium: 缺少业务异常类

**文件:** `ReportApplicationService.java`

```java
throw new IllegalArgumentException("Report not found: " + reportId);
throw new IllegalStateException("当前月份已有报告正在审批中");
```

**问题描述:**
使用 `IllegalArgumentException` 和 `IllegalStateException` 而非业务异常，无法区分业务错误和系统错误。

**建议修复:**
```java
throw new ResourceNotFoundException("报告", reportId);
throw new BusinessException("REPORT_ALREADY_IN_REVIEW", "当前月份已有报告正在审批中");
```

---

### 🟡 Low: Milestone 不是聚合根

**文件:** `Milestone.java`

```java
@Entity
@Table(name = "indicator_milestone")
public class Milestone {  // ❌ 未继承 AggregateRoot
    // 没有业务方法，只是数据容器
}
```

**问题描述:**
Milestone 实体没有继承 AggregateRoot，也没有领域行为方法，只是一个贫血模型。

**建议修复:**
```java
@Entity
public class Milestone extends AggregateRoot<Long> {

    public void complete() {
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException("里程碑未处于进行中状态");
        }
        this.status = "COMPLETED";
        addEvent(new MilestoneCompletedEvent(this.id));
    }
}
```

---

## 五、架构最佳实践

### 🟠 Medium: 跨模块直接访问其他模块的表

**文件:** `ReportApplicationService.java`

```java
// 直接操作 sism-strategy 模块的 indicator 表
indicatorRepository.findById(indicatorId);
indicatorRepository.save(indicator);

// 直接操作 sism-workflow 模块的 audit_instance 表
jdbcTemplate.update("UPDATE public.audit_instance ...");
```

**问题描述:**
执行模块直接访问策略模块和工作流模块的数据，违反了模块边界。

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
// 通过应用服务或领域事件
@Autowired
private IndicatorApplicationService indicatorService;

indicatorService.updateProgress(indicatorId, progress);

// 或使用事件驱动
@EventListener
public void onReportApproved(ReportApprovedEvent event) {
    indicatorService.syncProgress(event.getIndicatorDetails());
}
```

---

### 🟡 Low: 分页参数从 1 开始

**文件:** `ReportController.java`
**行号:** 124-126

```java
@GetMapping
public ResponseEntity<ApiResponse<PageResult<PlanReportSimpleResponse>>> getAllReports(
        @RequestParam(defaultValue = "1") int page,  // ❌ 从 1 开始
        @RequestParam(defaultValue = "10") int size) {
    Page<PlanReport> reportPage = reportApplicationService.findAllActiveReports(page, size);
}
```

**问题描述:**
分页参数从 1 开始，而 Spring Data 的 Pageable 从 0 开始，需要手动转换，容易出错。

---

## 六、审计总结

### 问题统计

| 严重等级 | 数量 | 类别分布 |
|----------|------|----------|
| 🔴 Critical | 3 | 安全、Bug（Transient 字段、状态常量不一致） |
| 🔴 High | 3 | 安全、性能 |
| 🟠 Medium | 6 | Bug、性能、代码质量、架构 |
| 🟡 Low | 2 | 代码质量、架构 |

### 最紧急修复项

| 优先级 | 问题 | 影响 |
|--------|------|------|
| P0 | 报告审批无权限控制 | 任何用户可审批报告 |
| P0 | PlanReport 关键字段不持久化 | 报告内容丢失 |
| P0 | 状态常量命名与值不一致 | 业务逻辑错误 |
| P1 | enrichReportMetadata N+1 查询 | 性能问题 |
| P1 | 跨模块直接访问数据库 | 架构违规 |

### 整体评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 安全性 | 🔴 需改进 | 审批操作无权限控制 |
| 可靠性 | 🔴 需改进 | 关键字段不持久化 |
| 性能 | 🟠 需改进 | 存在 N+1 查询问题 |
| 可维护性 | 🟠 需改进 | 跨模块耦合、异常处理不一致 |
| 架构合规性 | 🟠 需改进 | 违反模块边界 |

### 亮点

1. **领域事件设计**: PlanReport 使用领域事件进行解耦
2. **状态机设计**: 报告状态流转清晰
3. **DTO 分离**: 使用 SimpleResponse 和完整 Response 区分查询场景

### 关键建议

1. **立即添加权限控制**: 为审批操作添加 `@PreAuthorize`
2. **修复 Transient 字段**: 添加数据库列或使用关联表
3. **统一状态常量**: 使常量名与值一致
4. **使用批量查询**: 解决 enrichReportMetadata 的 N+1 问题
5. **遵守模块边界**: 通过服务接口或事件进行跨模块通信

---

**审计完成日期:** 2026-04-06
**下一步行动:** 修复 Critical 级别问题后再部署生产环境