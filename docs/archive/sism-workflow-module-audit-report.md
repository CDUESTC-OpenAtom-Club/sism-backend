# sism-workflow 模块审计报告

**审计日期:** 2026-04-06
**审计人员:** Claude Code (Automated Audit)
**模块版本:** 当前主分支

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

### 包结构

```
com.sism.workflow/
├── application/
│   ├── WorkflowApplicationService.java
│   ├── BusinessWorkflowApplicationService.java
│   ├── PlanWorkflowSyncService.java
│   ├── PlanWorkflowEventListener.java
│   ├── ReportWorkflowEventListener.java
│   ├── definition/
│   │   ├── WorkflowDefinitionQueryService.java
│   │   └── WorkflowPreviewQueryService.java
│   ├── runtime/
│   │   ├── StartWorkflowUseCase.java
│   │   ├── ApproveWorkflowUseCase.java
│   │   ├── RejectWorkflowUseCase.java
│   │   ├── CancelWorkflowUseCase.java
│   │   ├── WorkflowInstanceQueryService.java
│   │   └── WorkflowTaskCommandService.java
│   ├── query/
│   │   ├── WorkflowReadModelService.java
│   │   └── WorkflowReadModelMapper.java
│   └── support/
│       ├── ApproverResolver.java
│       ├── FlowResolver.java
│       ├── StepInstanceFactory.java
│       ├── SubmissionStepAutoCompletePolicy.java
│       └── WorkflowEventDispatcher.java
├── domain/
│   ├── AuditStatus.java
│   ├── enums/
│   │   └── WorkflowStatus.java
│   ├── event/
│   │   ├── WorkflowDomainEvent.java
│   │   ├── WorkflowStartedEvent.java
│   │   ├── WorkflowApprovedEvent.java
│   │   └── WorkflowCompletedEvent.java
│   ├── definition/
│   │   ├── model/
│   │   │   ├── AuditFlowDef.java
│   │   │   └── AuditStepDef.java
│   │   └── repository/
│   │       └── FlowDefinitionRepository.java
│   └── runtime/
│       ├── model/
│       │   ├── AuditInstance.java
│       │   ├── AuditStepInstance.java
│       │   └── WorkflowTask.java
│       └── repository/
│           ├── AuditInstanceRepository.java
│           └── WorkflowTaskRepository.java
├── infrastructure/
│   └── persistence/
│       ├── JpaWorkflowRepository.java
│       ├── JpaWorkflowRepositoryInternal.java
│       ├── AuditFlowDefJpaRepository.java
│       ├── AuditInstanceRepositoryImpl.java
│       ├── FlowDefinitionRepositoryImpl.java
│       └── ...
└── interfaces/
    ├── dto/
    │   ├── ApprovalRequest.java
    │   ├── RejectionRequest.java
    │   ├── StartWorkflowRequest.java
    │   ├── WorkflowInstanceResponse.java
    │   └── ...
    └── rest/
        ├── WorkflowController.java
        ├── BusinessWorkflowController.java
        └── ApprovalFlowCompatibilityController.java
```

---

## 一、安全漏洞

### 🔴 Critical: 所有审批操作无权限控制

**文件:** `WorkflowController.java`
**行号:** 128-160

```java
@PostMapping("/instances/{instanceId}/approve")
@Operation(summary = "Approve approval instance")
public ResponseEntity<ApiResponse<AuditInstance>> approveInstance(
        @PathVariable Long instanceId,
        @RequestBody AuditInstance instance,
        @RequestParam Long userId,  // ❌ userId 从参数获取，无验证
        @RequestParam(required = false) String comment) {
    // ❌ 无 @PreAuthorize，任何用户都可以审批！
    AuditInstance approved = workflowApplicationService.approveAuditInstance(existingInstance, userId, comment);
    return ResponseEntity.ok(ApiResponse.success(approved));
}

@PostMapping("/instances/{instanceId}/reject")
@Operation(summary = "Reject approval instance")
public ResponseEntity<ApiResponse<AuditInstance>> rejectInstance(
        @PathVariable Long instanceId,
        @RequestBody AuditInstance instance,
        @RequestParam Long userId,  // ❌ 同样无验证
        @RequestParam(required = false) String comment) {
    // ❌ 无权限控制
}
```

**问题描述:**
1. 审批/驳回操作无任何权限控制
2. `userId` 直接从请求参数获取，未验证是否为当前登录用户
3. 攻击者可以冒充其他用户进行审批

**攻击示例:**
```bash
# 任何用户都可以冒充管理员审批
POST /api/v1/approval/instances/123/approve?userId=1&comment=approved
```

**风险影响:**
- 审批流程完全失效
- 任何用户可审批任何流程
- 业务安全风险

**严重等级:** 🔴 **Critical**

**建议修复:**
```java
@PostMapping("/instances/{instanceId}/approve")
@PreAuthorize("isAuthenticated()")
@Operation(summary = "Approve approval instance")
public ResponseEntity<ApiResponse<AuditInstance>> approveInstance(
        @PathVariable Long instanceId,
        @AuthenticationPrincipal CurrentUser currentUser,  // 使用认证用户
        @RequestParam(required = false) String comment) {
    // 验证用户是否有审批权限
    AuditInstance instance = workflowApplicationService.getAuditInstanceById(instanceId);
    if (!workflowApplicationService.canApprove(instance, currentUser.getId())) {
        throw new UnauthorizedException("您没有权限审批此流程");
    }
    // ...
}
```

---

### 🔴 High: BusinessWorkflowController 同样缺少权限控制

**文件:** `BusinessWorkflowController.java`

```java
@PostMapping("/tasks/{taskId}/approve")
@Operation(summary = "审批当前工作流节点并推进到下一个节点")
public ResponseEntity<ApiResponse<WorkflowInstanceResponse>> approveTask(
        @PathVariable String taskId,
        @Valid @RequestBody ApprovalRequest request,
        @AuthenticationPrincipal CurrentUser currentUser) {
    // ❌ 使用了 @AuthenticationPrincipal 但无 @PreAuthorize
    // ❌ 未验证 currentUser 是否有权限审批此任务
    WorkflowInstanceResponse response = workflowService.approveTask(taskId, request, currentUser.getId());
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

**问题描述:**
虽然使用了 `@AuthenticationPrincipal` 获取当前用户，但未验证：
1. 当前用户是否是任务的审批人
2. 任务是否处于可审批状态

**严重等级:** 🔴 **High**

**建议修复:**
```java
@PostMapping("/tasks/{taskId}/approve")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<ApiResponse<WorkflowInstanceResponse>> approveTask(...) {
    // 在服务层验证审批权限
    workflowService.validateApprovalPermission(taskId, currentUser.getId());
    // ...
}
```

---

### 🟠 Medium: 流程定义创建无权限控制

**文件:** `BusinessWorkflowController.java`
**行号:** 222-240

```java
@PostMapping("/definitions")
@Operation(summary = "创建带有明确有序步骤的固定工作流模板")
public ResponseEntity<ApiResponse<WorkflowDefinitionResponse>> createDefinition(
        @Valid @RequestBody CreateWorkflowDefinitionRequest request
) {
    // ❌ 任何用户都可以创建工作流定义
    WorkflowDefinitionResponse response = workflowService.createDefinition(request);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

**问题描述:**
工作流定义是系统配置，应仅由管理员创建。

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
@PostMapping("/definitions")
@PreAuthorize("hasRole('ADMIN')")
@Operation(summary = "创建工作流模板（仅管理员）")
public ResponseEntity<ApiResponse<WorkflowDefinitionResponse>> createDefinition(...) { }
```

---

## 二、潜在 Bug 和逻辑错误

### 🔴 High: 状态常量值重复

**文件:** `AuditInstance.java`
**行号:** 32-33

```java
public static final String STATUS_PENDING = "IN_REVIEW";
public static final String STATUS_IN_PROGRESS = "IN_REVIEW";  // ❌ 与 STATUS_PENDING 值相同
public static final String STATUS_APPROVED = "APPROVED";
public static final String STATUS_REJECTED = "REJECTED";
```

**问题描述:**
`STATUS_PENDING` 和 `STATUS_IN_PROGRESS` 具有相同的值 `"IN_REVIEW"`，可能导致：
1. 代码混淆 - 开发者不知道该使用哪个常量
2. 状态语义不清晰
3. 潜在的逻辑错误

**严重等级:** 🔴 **High**

**建议修复:**
```java
public static final String STATUS_PENDING = "PENDING";  // 等待处理
public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";  // 处理中
public static final String STATUS_IN_REVIEW = "IN_REVIEW";  // 审批中
public static final String STATUS_APPROVED = "APPROVED";
public static final String STATUS_REJECTED = "REJECTED";
```

---

### 🟠 Medium: Controller 方法参数设计问题

**文件:** `WorkflowController.java`
**行号:** 128-143

```java
@PostMapping("/instances/{instanceId}/approve")
public ResponseEntity<ApiResponse<AuditInstance>> approveInstance(
        @PathVariable Long instanceId,
        @RequestBody AuditInstance instance,  // ❌ 接收整个实例但实际未使用
        @RequestParam Long userId,
        @RequestParam(required = false) String comment) {
    // 先从数据库获取完整的实例信息
    AuditInstance existingInstance = workflowApplicationService.getAuditInstanceById(instanceId);
    // ...
    // 使用数据库中的实例进行操作
    AuditInstance approved = workflowApplicationService.approveAuditInstance(existingInstance, userId, comment);
}
```

**问题描述:**
1. 接收 `AuditInstance` 作为请求体，但完全忽略它
2. 从数据库重新查询实例
3. 这违反了 RESTful 设计原则

**风险影响:**
- 请求体无意义
- API 设计混乱
- 可能的混淆

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
@PostMapping("/instances/{instanceId}/approve")
public ResponseEntity<ApiResponse<AuditInstance>> approveInstance(
        @PathVariable Long instanceId,
        @RequestParam(required = false) String comment,
        @AuthenticationPrincipal CurrentUser currentUser) {
    // ...
}
```

---

### 🟠 Medium: 不支持的操作仍暴露 API

**文件:** `AuditInstance.java`
**行号:** 300-305

```java
public void transfer(Long targetUserId) {
    throw new UnsupportedOperationException("Workflow task reassignment is not supported for fixed approval templates");
}

public void addApprover(Long approverId) {
    throw new UnsupportedOperationException("Dynamic approver insertion is not supported for fixed approval templates");
}
```

**问题:** 这些操作在 Controller 中仍然暴露为 API 端点，会导致运行时错误。

**严重等级:** 🟠 **Medium**

**建议修复:**
移除不支持的 API 端点，或在文档中明确标注为"不支持"。

---

### 🟡 Low: 审批人判断逻辑过于简单

**文件:** `AuditInstance.java`
**行号:** 251-266

```java
private boolean isRequesterSubmitStep(AuditStepInstance step) {
    if (Integer.valueOf(1).equals(step.getStepNo())) {
        return true;  // ❌ 仅根据步骤号判断
    }

    if (requesterId != null && requesterId.equals(step.getApproverId())) {
        return true;
    }

    String stepName = step.getStepName();
    return stepName != null && stepName.contains("提交");  // ❌ 根据名称判断
}
```

**问题描述:**
判断是否为"提交步骤"的逻辑过于简单，可能误判。

**严重等级:** 🟡 **Low**

---

## 三、性能瓶颈

### 🟠 Medium: 潜在的 N+1 查询 - StepInstances

**文件:** `AuditInstance.java`

```java
@OneToMany(mappedBy = "instance", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderBy("stepNo ASC")
private List<AuditStepInstance> stepInstances = new ArrayList<>();
```

**问题描述:**
1. `AuditInstance` 与 `AuditStepInstance` 是一对多关系
2. 在查询工作流实例时可能触发 N+1 查询
3. 多个方法遍历 `stepInstances` 列表

**风险影响:**
- 数据库查询性能下降
- 响应延迟

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
// 在 Repository 中使用 JOIN FETCH
@Query("SELECT ai FROM WorkflowAuditInstance ai LEFT JOIN FETCH ai.stepInstances WHERE ai.id = :id")
Optional<AuditInstance> findByIdWithSteps(Long id);
```

---

### 🟠 Medium: 无分页的列表查询

**文件:** `WorkflowController.java`
**行号:** 73-78

```java
@GetMapping("/instances")
@Operation(summary = "Get all approval instances")
public ResponseEntity<ApiResponse<List<AuditInstance>>> getAllInstances() {
    List<AuditInstance> instances = workflowApplicationService.getAllAuditInstances();  // ❌ 无分页
    return ResponseEntity.ok(ApiResponse.success(instances));
}
```

**问题描述:**
返回所有工作流实例，无分页限制。如果有大量实例，会导致性能问题。

**严重等级:** 🟠 **Medium**

---

### 🟡 Low: 重复查询工作流定义

**文件:** `ApproveWorkflowUseCase.java`
**行号:** 49-54

```java
private boolean createNextStep(AuditInstance instance) {
    AuditFlowDef flowDef = flowDefinitionRepository.findById(instance.getFlowDefId()).orElse(null);  // 每次审批都查询
    // ...
}
```

**问题描述:**
每次审批操作都会查询工作流定义，可以考虑缓存。

**严重等级:** 🟡 **Low**

---

## 四、代码质量和可维护性

### 🟠 Medium: API 路径命名不一致

**文件:** `WorkflowController.java` vs `BusinessWorkflowController.java`

```java
// WorkflowController
@RequestMapping("/api/v1/approval")
@GetMapping("/legacy-flows")  // ❌ "legacy-" 前缀

// BusinessWorkflowController
@RequestMapping("/api/v1/workflows")
@GetMapping("/definitions")  // ✅ 现代命名
```

**问题描述:**
1. 两个控制器处理类似功能但路径不同
2. `legacy-flows` 暗示是遗留 API，但没有迁移计划
3. API 版本管理混乱

**严重等级:** 🟠 **Medium**

---

### 🟠 Medium: 异常处理不一致

**文件:** 多处

```java
// WorkflowController
if (flowDef == null) {
    return ResponseEntity.ok(ApiResponse.error(404, "Flow definition not found"));  // 返回 200 但包含错误码
}

// AuditInstance
throw new IllegalArgumentException("Entity ID must be positive");  // 抛出异常
throw new IllegalStateException("Cannot approve: workflow is not in review");
```

**问题描述:**
有些地方返回错误响应，有些抛出异常，处理方式不一致。

**严重等级:** 🟠 **Medium**

---

### 🟡 Low: 魔法字符串 - 步骤状态

**文件:** `AuditInstance.java`

```java
if (STEP_STATUS_APPROVED.equals(step.getStatus())) { }
if ("APPROVED".equals(terminalStatus)) { }  // ❌ 魔法字符串
```

**问题描述:**
状态比较应使用定义的常量。

**严重等级:** 🟡 **Low**

---

### 🟡 Low: 未使用的导入和代码

**文件:** 多处存在未使用的导入和注释掉的代码。

---

## 五、架构最佳实践

### 🟠 Medium: 模块间耦合 - PlanWorkflowSyncService

**文件:** `PlanWorkflowSyncService.java`

```java
@Service
public class PlanWorkflowSyncService {
    // 直接操作 sism-strategy 模块的 Plan 状态
    public void syncAfterWorkflowChanged(AuditInstance instance) {
        // 更新 Plan 状态
    }
}
```

**问题描述:**
Workflow 模块直接依赖 Strategy 模块的 Plan 实体，违反了模块边界。

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
// 使用事件驱动解耦
@EventListener
public void onWorkflowApproved(WorkflowApprovedEvent event) {
    // 通过事件通知 Strategy 模块
}
```

---

### 🟠 Medium: 事件发布未持久化

**文件:** `ApproveWorkflowUseCase.java`
**行号:** 43-46

```java
AuditInstance saved = auditInstanceRepository.save(instance);
planWorkflowSyncService.syncAfterWorkflowChanged(saved);
workflowEventDispatcher.publish(saved);  // ❌ 未持久化事件
```

**问题描述:**
事件发布后未保存到事件存储，无法进行事件溯源或审计追踪。

**严重等级:** 🟠 **Medium**

---

### ✅ 亮点: 使用 Use Case 模式

模块采用了 Use Case 模式组织业务逻辑：
- `StartWorkflowUseCase`
- `ApproveWorkflowUseCase`
- `RejectWorkflowUseCase`
- `CancelWorkflowUseCase`

这符合单一职责原则，每个用例专注于一个业务操作。

---

## 六、审计总结

### 问题统计

| 严重等级 | 数量 | 类别分布 |
|----------|------|----------|
| 🔴 Critical | 1 | 安全（审批无权限控制） |
| 🔴 High | 2 | 安全、Bug |
| 🟠 Medium | 8 | 安全、Bug、性能、代码质量、架构 |
| 🟡 Low | 4 | 代码质量、性能 |

### 最紧急修复项

| 优先级 | 问题 | 影响 |
|--------|------|------|
| P0 | 审批操作无权限控制 | 任何用户可审批任何流程 |
| P1 | 状态常量值重复 | 代码混乱、潜在 Bug |
| P1 | userId 从参数获取无验证 | 用户冒充风险 |
| P2 | 流程定义创建无权限控制 | 配置被恶意修改 |

### 整体评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 安全性 | 🔴 需改进 | 审批操作完全无权限控制 |
| 可靠性 | 🟠 需改进 | 状态常量混乱 |
| 性能 | 🟠 需改进 | 存在 N+1 查询风险 |
| 可维护性 | ✅ 良好 | Use Case 模式设计良好 |
| 架构合规性 | 🟠 需改进 | 模块间存在直接耦合 |

### 亮点

1. **Use Case 模式**: 业务逻辑按用例组织，职责清晰
2. **事件驱动**: 使用领域事件进行解耦
3. **聚合根设计**: AuditInstance 作为聚合根管理步骤实例
4. **测试覆盖**: 存在多个单元测试文件

### 关键建议

1. **立即添加权限控制**: 为审批操作添加 `@PreAuthorize` 和审批权限验证
2. **使用 @AuthenticationPrincipal**: 移除 userId 参数，使用认证用户
3. **修复状态常量**: 消除重复的状态值
4. **添加分页**: 为列表查询添加分页支持
5. **模块解耦**: 使用事件替代直接依赖其他模块

---

**审计完成日期:** 2026-04-06
**下一步行动:** 修复审批权限控制后再部署生产环境