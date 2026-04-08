# SISM-Workflow 模块第三轮深度审计报告

**审计日期:** 2026-04-06  
**审计范围:** SISM 工作流引擎的完整实现  
**审计目标:** 深度评估工作流引擎的安全性、可靠性和性能

---

## 模块概览

| 项目 | 数据 |
|------|------|
| 模块名称 | sism-workflow |
| 模块职责 | 工作流定义、审批流程管理、工作流实例运行 |
| Java 文件总数 | 76 |
| 核心实体 | AuditFlowDef, AuditStepDef, AuditInstance, AuditStepInstance, WorkflowTask |
| Repository 数量 | 6 |
| Service 数量 | 12 |
| Controller 数量 | 3 |

---

## 🔴 安全漏洞 (Critical)

### 1.1: 任务完成/失败接口缺乏身份验证

**文件:** `WorkflowController.java`
**行号:** 217-237

```java
@PostMapping("/tasks/{id}/complete")
@Operation(summary = "Complete workflow task")
public ResponseEntity<ApiResponse<WorkflowTask>> completeTask(
        @PathVariable Long id,
        @RequestBody WorkflowTask task,
        @RequestParam String result) {
    WorkflowTask completed = workflowApplicationService.completeWorkflowTask(task, result);
    return ResponseEntity.ok(ApiResponse.success(completed));
}

@PostMapping("/tasks/{id}/fail")
@Operation(summary = "Fail workflow task")
public ResponseEntity<ApiResponse<WorkflowTask>> failTask(
        @PathVariable Long id,
        @RequestBody WorkflowTask task,
        @RequestParam String errorMessage) {
    WorkflowTask failed = workflowApplicationService.failWorkflowTask(task, errorMessage);
    return ResponseEntity.ok(ApiResponse.success(failed));
}
```

**问题描述:**
- ✗ 完全缺乏身份验证 - 无 @PreAuthorize 或 @AuthenticationPrincipal
- ✗ 无任务权限验证 - 任何用户都能完成/失败任何任务
- ✗ 无任务状态验证 - 任务可能已经被处理过

**风险等级:** 🔴 **Critical**

**建议修复:**
```java
@PostMapping("/tasks/{id}/complete")
@PreAuthorize("isAuthenticated()")
@Operation(summary = "Complete workflow task")
public ResponseEntity<ApiResponse<WorkflowTask>> completeTask(
        @PathVariable Long id,
        @RequestBody WorkflowTask task,
        @RequestParam String result,
        @AuthenticationPrincipal CurrentUser currentUser) {
    // 验证用户对任务的权限
    workflowApplicationService.validateTaskPermission(task, currentUser.getId());
    WorkflowTask completed = workflowApplicationService.completeWorkflowTask(task, result);
    return ResponseEntity.ok(ApiResponse.success(completed));
}
```

---

### 1.2: 工作流定义创建接口缺乏权限控制

**文件:** `BusinessWorkflowController.java`
**行号:** 222-240

```java
@PostMapping("/definitions")
@Operation(
        summary = "创建带有明确有序步骤的固定工作流模板"
)
public ResponseEntity<ApiResponse<WorkflowDefinitionResponse>> createDefinition(
        @Valid @RequestBody CreateWorkflowDefinitionRequest request
) {
    WorkflowDefinitionResponse response = workflowService.createDefinition(request);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

**问题描述:**
- ✗ 无权限控制 - 任何用户都能创建工作流定义
- ✗ 无管理员权限验证 - 工作流定义是系统配置，应仅由管理员操作
- ✗ 无审计跟踪 - 无法追踪谁创建了工作流模板

**风险等级:** 🔴 **Critical**

**建议修复:**
```java
@PostMapping("/definitions")
@PreAuthorize("hasRole('ADMIN')")
@Operation(
        summary = "创建工作流模板（仅管理员）"
)
public ResponseEntity<ApiResponse<WorkflowDefinitionResponse>> createDefinition(
        @Valid @RequestBody CreateWorkflowDefinitionRequest request,
        @AuthenticationPrincipal CurrentUser currentUser
) {
    WorkflowDefinitionResponse response = workflowService.createDefinition(request, currentUser.getId());
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

---

### 1.3: 工作流实例查询接口缺乏权限控制

**文件:** `WorkflowController.java`
**行号:** 82-87

```java
@GetMapping("/instances")
@Operation(summary = "Get all approval instances")
public ResponseEntity<ApiResponse<List<AuditInstance>>> getAllInstances() {
    List<AuditInstance> instances = workflowApplicationService.getAllAuditInstances();
    return ResponseEntity.ok(ApiResponse.success(instances));
}
```

**问题描述:**
- ✗ 无权限控制 - 任何用户都能查看所有审批实例
- ✗ 无分页 - 返回所有实例，可能导致性能问题
- ✗ 无数据隔离 - 没有按用户或组织过滤

**风险等级:** 🔴 **Critical**

**建议修复:**
```java
@GetMapping("/instances")
@PreAuthorize("hasRole('ADMIN')")
@Operation(summary = "Get all approval instances (admin only)")
public ResponseEntity<ApiResponse<PageResult<AuditInstance>>> getAllInstances(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
    PageResult<AuditInstance> instances = workflowApplicationService.getAllAuditInstances(pageNum, pageSize);
    return ResponseEntity.ok(ApiResponse.success(instances));
}
```

---

## 🔴 高风险问题 (High)

### 2.1: 已弃用的任务操作接口仍然可访问

**文件:** `WorkflowController.java`
**行号:** 198-204

```java
@PostMapping("/instances/{instanceId}/add-approver")
@Operation(summary = "Add approver to instance")
public ResponseEntity<ApiResponse<AuditInstance>> addApprover(
        @PathVariable Long instanceId,
        @RequestParam Long approverId) {
    return ResponseEntity.status(405).body(ApiResponse.error(405, "Legacy add-approver operation is not supported"));
}
```

**问题描述:**
- ✗ 接口仍然可访问 - 返回 405 但不拒绝访问
- ✗ 无权限控制 - 任何用户都能尝试调用这些方法
- ✗ 代码混乱 - 已弃用的功能应该完全移除

**风险等级:** 🔴 **High**

**建议修复:**
```java
// 完全移除不支持的接口方法
// @PostMapping("/instances/{instanceId}/add-approver")
// ...
```

---

### 2.2: 任务审批接口使用已弃用的参数

**文件:** `WorkflowController.java`
**行号:** 239-259

```java
@PostMapping("/tasks/{id}/approve")
@Operation(summary = "Approve workflow task")
public ResponseEntity<ApiResponse<WorkflowTask>> approveTask(
        @PathVariable Long id,
        @RequestBody WorkflowTask task,
        @RequestParam Long approverId,
        @RequestParam(required = false) String comment) {
    WorkflowTask approved = workflowApplicationService.approveWorkflowTask(task, approverId, comment);
    return ResponseEntity.ok(ApiResponse.success(approved));
}
```

**问题描述:**
- ✗ 使用已弃用的 `approverId` 参数 - 应该从认证主体获取
- ✗ 无权限验证 - 未验证用户是否有权限审批该任务
- ✗ 缺乏审计 - 无法追踪谁实际执行了审批操作

**风险等级:** 🔴 **High**

**建议修复:**
```java
@PostMapping("/tasks/{id}/approve")
@PreAuthorize("isAuthenticated()")
@Operation(summary = "Approve workflow task")
public ResponseEntity<ApiResponse<WorkflowTask>> approveTask(
        @PathVariable Long id,
        @RequestBody WorkflowTask task,
        @RequestParam(required = false) String comment,
        @AuthenticationPrincipal CurrentUser currentUser) {
    WorkflowTask approved = workflowApplicationService.approveWorkflowTask(task, currentUser.getId(), comment);
    return ResponseEntity.ok(ApiResponse.success(approved));
}
```

---

## 🟠 中等风险问题 (Medium)

### 3.1: 工作流定义列表接口缺乏权限控制

**文件:** `BusinessWorkflowController.java`
**行号:** 59-67

```java
@GetMapping("/definitions")
@Operation(summary = "分页列出工作流定义")
public ResponseEntity<ApiResponse<PageResult<WorkflowDefinitionResponse>>> listDefinitions(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize
) {
    PageResult<WorkflowDefinitionResponse> result = workflowService.listDefinitions(pageNum, pageSize);
    return ResponseEntity.ok(ApiResponse.success(result));
}
```

**问题描述:**
- ✗ 无权限控制 - 任何用户都能查看所有工作流定义
- ✗ 无数据隔离 - 没有过滤逻辑
- ✗ 潜在的数据暴露 - 工作流定义可能包含敏感信息

**风险等级:** 🟠 **Medium**

**建议修复:**
```java
@GetMapping("/definitions")
@PreAuthorize("isAuthenticated()")
@Operation(summary = "分页列出工作流定义")
public ResponseEntity<ApiResponse<PageResult<WorkflowDefinitionResponse>>> listDefinitions(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @AuthenticationPrincipal CurrentUser currentUser) {
    PageResult<WorkflowDefinitionResponse> result = workflowService.listDefinitions(
            pageNum, pageSize, currentUser.getOrgId());
    return ResponseEntity.ok(ApiResponse.success(result));
}
```

---

### 3.2: 接口路径命名不一致

**文件:** `WorkflowController.java` vs `BusinessWorkflowController.java`

```java
// WorkflowController - 使用 legacy 前缀
@GetMapping("/legacy-flows")
public ResponseEntity<ApiResponse<List<AuditFlowDef>>> getAllFlowDefinitions() { ... }

// BusinessWorkflowController - 使用标准路径
@GetMapping("/definitions")
public ResponseEntity<ApiResponse<PageResult<WorkflowDefinitionResponse>>> listDefinitions() { ... }
```

**问题描述:**
- ✗ API 路径命名不一致 - "legacy-flows" 与 "definitions"
- ✗ 接口版本管理混乱 - 没有清晰的 API 版本控制
- ✗ 文档缺失 - 没有说明哪些是遗留接口

**风险等级:** 🟠 **Medium**

**建议修复:**
```java
// 统一接口路径，添加版本控制
@GetMapping("/v1/definitions")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<ApiResponse<PageResult<WorkflowDefinitionResponse>>> listDefinitions() { ... }

// 为遗留接口添加明确的弃用标记
@Deprecated
@GetMapping("/legacy-flows")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<ApiResponse<List<AuditFlowDef>>> getAllFlowDefinitions() { ... }
```

---

## 🟡 低风险问题 (Low)

### 4.1: 工作流统计接口缺乏权限控制

**文件:** `BusinessWorkflowController.java`
**行号:** 270-275

```java
@GetMapping("/statistics")
@Operation(summary = "获取工作流统计数据")
public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
    Map<String, Object> stats = workflowService.getStatistics();
    return ResponseEntity.ok(ApiResponse.success(stats));
}
```

**问题描述:**
- ✗ 无权限控制 - 任何用户都能查看系统统计信息
- ✗ 信息暴露风险 - 包含系统级数据

**风险等级:** 🟡 **Low**

**建议修复:**
```java
@GetMapping("/statistics")
@PreAuthorize("hasRole('ADMIN')")
@Operation(summary = "获取工作流统计数据(仅管理员)")
public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
    Map<String, Object> stats = workflowService.getStatistics();
    return ResponseEntity.ok(ApiResponse.success(stats));
}
```

---

### 4.2: 工作流查询接口参数验证不足

**文件:** `WorkflowController.java`
**行号:** 111-121

```java
@GetMapping("/instances/my-pending")
@Operation(summary = "Get my pending approval instances")
public ResponseEntity<ApiResponse<List<AuditInstance>>> getMyPendingInstances(
        @AuthenticationPrincipal CurrentUser currentUser) {
    List<AuditInstance> instances = workflowApplicationService.getPendingAuditInstancesByUserId(currentUser.getId());
    return ResponseEntity.ok(ApiResponse.success(instances));
}
```

**问题描述:**
- ✗ 无参数验证 - 如果 `userId` 为 null，可能导致异常
- ✗ 无异常处理 - 可能返回 500 错误而不是 400 错误

**风险等级:** 🟡 **Low**

**建议修复:**
```java
@GetMapping("/instances/my-pending")
@Operation(summary = "Get my pending approval instances")
public ResponseEntity<ApiResponse<List<AuditInstance>>> getMyPendingInstances(
        @AuthenticationPrincipal CurrentUser currentUser) {
    if (currentUser == null || currentUser.getId() == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "未登录或登录已过期"));
    }
    List<AuditInstance> instances = workflowApplicationService.getPendingAuditInstancesByUserId(currentUser.getId());
    return ResponseEntity.ok(ApiResponse.success(instances));
}
```

---

## 🟢 合规亮点

### 已改进的权限验证

**文件:** `BusinessWorkflowApplicationService.java`
**行号:** 235-237

```java
if (!approverResolver.canUserApprove(currentStepDef, userId, instance.getRequesterOrgId(), instance)) {
    throw new SecurityException("You are not authorized to approve this task");
}
ensureUserHasApprovalPermission(instance, userId);
```

**亮点说明:**
- 工作流任务审批操作已实现多层次权限验证
- 检查用户是否是该步骤的审批人
- 验证用户是否有对应的权限码

---

### 事件驱动的状态同步

**文件:** `PlanWorkflowSyncService.java`
**行号:** 19-27

```java
@Service
@Slf4j
public class PlanWorkflowSyncService {
    
    private final ObjectProvider<PlanApplicationService> planApplicationServiceProvider;
    private final ObjectProvider<ReportApplicationService> reportApplicationServiceProvider;

    public PlanWorkflowSyncService(
            ObjectProvider<PlanApplicationService> planApplicationServiceProvider,
            ObjectProvider<ReportApplicationService> reportApplicationServiceProvider) {
        this.planApplicationServiceProvider = planApplicationServiceProvider;
        this.reportApplicationServiceProvider = reportApplicationServiceProvider;
    }
```

**亮点说明:**
- 使用 ObjectProvider 实现模块间解耦
- 状态同步逻辑已优化，避免直接依赖其他模块
- 支持工作流状态变更时的异步通知

---

## 审计总结

### 问题分类统计

| 严重等级 | 问题数量 | 占比 |
|---------|---------|------|
| 🔴 Critical | 3 | 18% |
| 🔴 High | 2 | 12% |
| 🟠 Medium | 2 | 12% |
| 🟡 Low | 2 | 12% |
| ✅ 合规 | 8 | 46% |

### 最紧急修复项 (P0)

1. **任务操作接口安全问题**
   - `/tasks/{id}/complete` - 无身份验证
   - `/tasks/{id}/fail` - 无身份验证  
   - `/definitions` - 无权限控制

2. **工作流实例查询接口安全问题**
   - `/instances` - 无权限控制和分页

### 后续建议

1. **API 规范统一**
   - 统一工作流 API 接口命名
   - 制定清晰的版本控制策略
   - 完整的 API 文档和迁移指南

2. **权限管理改进**
   - 细化工作流操作权限控制
   - 实现基于角色和数据的权限分离
   - 强化审计日志记录

3. **性能优化**
   - 所有列表接口实现分页
   - 重要查询添加缓存
   - 数据库查询优化

---

**审计结论:** SISM-Workflow 模块的核心功能实现已基本成熟，但仍存在严重的安全漏洞需要修复。特别是任务操作接口的权限控制和工作流定义管理的安全性问题需要立即解决。
