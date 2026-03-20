# 修复任务 03: sism-workflow 业务工作流

**模块**: sism-workflow  
**实现率**: 72%  
**优先级**: 🔴 P0 (最高)  
**工作量**: 25-35 小时  
**关键路径**: 是 (审批流程核心)

---

## 📋 问题概述

### 完整问题列表

| 问题类型 | 描述 | 影响 | 严重度 |
|---------|------|------|--------|
| 缺失控制器 | BusinessWorkflowController | 无法启动业务工作流 | 🔴 致命 |
| 认证问题 | 多个端点不使用 Spring Security | 安全漏洞 | 🟡 中等 |
| API 签名问题 | 某些端点参数/返回值不规范 | 前端集成困难 | 🟡 中等 |
| 工作流引擎 | 缺少工作流定义解析执行 | 工作流无法流转 | 🔴 致命 |

### 支撑数据

```
缺失 API 端点数:  11 个
- 工作流启动:     3 个
- 工作流查询:     4 个
- 工作流操作:     4 个

需要改造的认证端点: 5+ 个
需要补充的 DTO: 8+ 个
```

---

## 🎯 缺失的控制器详解

### BusinessWorkflowController (缺失)

**位置**: `sism-workflow/src/main/java/.../interface/controller/BusinessWorkflowController.java`

**背景**: 业务工作流管理指标下发、报告审批等核心业务流程

**需要实现的 API**:

```java
@RestController
@RequestMapping("/api/v1/workflows")
public class BusinessWorkflowController {

    @Autowired
    private BusinessWorkflowApplicationService workflowService;

    // 1. 启动工作流
    @PostMapping("/start")
    public ResponseEntity<WorkflowInstanceResponse> startWorkflow(
        @Valid @RequestBody StartWorkflowRequest request,
        @AuthenticationPrincipal CurrentUser currentUser
    ) { }
    
    @PostMapping("/{definitionId}/instances")
    public ResponseEntity<WorkflowInstanceResponse> startWorkflowInstance(
        @PathVariable String definitionId,
        @Valid @RequestBody StartInstanceRequest request,
        @AuthenticationPrincipal CurrentUser currentUser
    ) { }
    
    // 2. 查询工作流
    @GetMapping("/definitions")
    public ResponseEntity<PageResult<WorkflowDefinitionResponse>> listDefinitions(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize
    ) { }
    
    @GetMapping("/{definitionId}/instances")
    public ResponseEntity<PageResult<WorkflowInstanceResponse>> listInstances(
        @PathVariable String definitionId,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize
    ) { }
    
    @GetMapping("/instances/{instanceId}")
    public ResponseEntity<WorkflowInstanceDetailResponse> getInstanceDetail(
        @PathVariable String instanceId
    ) { }
    
    @GetMapping("/my-tasks")
    public ResponseEntity<PageResult<WorkflowTaskResponse>> getMyPendingTasks(
        @AuthenticationPrincipal CurrentUser currentUser,
        @RequestParam(defaultValue = "1") int pageNum
    ) { }
    
    // 3. 工作流操作
    @PostMapping("/tasks/{taskId}/approve")
    public ResponseEntity<WorkflowInstanceResponse> approveTask(
        @PathVariable String taskId,
        @Valid @RequestBody ApprovalRequest request,
        @AuthenticationPrincipal CurrentUser currentUser
    ) { }
    
    @PostMapping("/tasks/{taskId}/reject")
    public ResponseEntity<WorkflowInstanceResponse> rejectTask(
        @PathVariable String taskId,
        @Valid @RequestBody RejectionRequest request,
        @AuthenticationPrincipal CurrentUser currentUser
    ) { }
    
    @PostMapping("/tasks/{taskId}/reassign")
    public ResponseEntity<WorkflowInstanceResponse> reassignTask(
        @PathVariable String taskId,
        @Valid @RequestBody ReassignRequest request,
        @AuthenticationPrincipal CurrentUser currentUser
    ) { }
    
    @PostMapping("/{instanceId}/cancel")
    public ResponseEntity<Void> cancelInstance(
        @PathVariable String instanceId,
        @AuthenticationPrincipal CurrentUser currentUser
    ) { }
}
```

**需要创建的 DTO 类**:

```java
// 请求
@Data
class StartWorkflowRequest {
    @NotBlank
    private String workflowCode; // "indicator_distribution", "report_approval" 等
    
    @NotNull
    private Long businessEntityId; // 业务实体 ID (Indicator, Report 等)
    
    private String businessEntityType; // 业务实体类型
    
    private Map<String, Object> variables; // 工作流变量
}

@Data
class StartInstanceRequest {
    @NotNull
    private Long businessEntityId;
    
    private Map<String, Object> variables;
}

@Data
class ApprovalRequest {
    @NotBlank
    private String comment; // 批注
    
    private Map<String, Object> variables;
}

@Data
class RejectionRequest {
    @NotBlank
    private String reason; // 拒绝原因
    
    private String returnToStep; // 返回至哪一步 (可选)
    
    private Map<String, Object> variables;
}

@Data
class ReassignRequest {
    @NotNull
    private Long targetUserId; // 目标用户 ID
    
    private String reason; // 转办原因
}

// 响应
@Data
class WorkflowDefinitionResponse {
    private String definitionId;
    private String definitionName;
    private String description;
    private String category;
    private String version;
    private boolean isActive;
    private LocalDateTime createTime;
}

@Data
class WorkflowInstanceResponse {
    private String instanceId;
    private String definitionId;
    private String status; // RUNNING, COMPLETED, REJECTED, CANCELLED
    private Long businessEntityId;
    private Long starterId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

@Data
class WorkflowInstanceDetailResponse extends WorkflowInstanceResponse {
    private List<WorkflowTaskResponse> tasks;
    private List<WorkflowHistoryResponse> history;
}

@Data
class WorkflowTaskResponse {
    private String taskId;
    private String taskName;
    private String taskKey;
    private String status; // PENDING, COMPLETED, REJECTED
    private Long assigneeId;
    private String assigneeName;
    private LocalDateTime createdTime;
    private LocalDateTime dueDate;
    private String claimUrl; // 前端可以直接打开的链接
    private Map<String, Object> variables;
}

@Data
class WorkflowHistoryResponse {
    private String historyId;
    private String taskId;
    private String taskName;
    private Long operatorId;
    private String operatorName;
    private String action; // APPROVE, REJECT, REASSIGN
    private String comment;
    private LocalDateTime operateTime;
}
```

**关键实现要点**:

```java
// 1. 启动工作流时验证业务实体存在
if (!businessEntityService.exists(request.getBusinessEntityId())) {
    throw new BusinessEntityNotFoundException();
}

// 2. 检查工作流定义是否存在且已激活
WorkflowDefinition definition = 
    workflowDefinitionService.findByCode(request.getWorkflowCode());
if (definition == null || !definition.isActive()) {
    throw new WorkflowDefinitionNotFoundException();
}

// 3. 检查是否已存在未完成的工作流实例
if (workflowInstanceService.hasActiveInstance(
    request.getBusinessEntityId(),
    request.getWorkflowCode())) {
    throw new ActiveWorkflowAlreadyExistsException();
}

// 4. 创建实例
WorkflowInstance instance = new WorkflowInstance(definition, 
    request.getBusinessEntityId(), currentUser.getId());
instance.start(request.getVariables());

// 5. 获取当前用户的待办任务（从 Spring Security）
List<WorkflowTask> tasks = 
    taskService.findByAssignee(currentUser.getId());

// 6. 审批/拒绝时检查权限
if (!task.getAssigneeId().equals(currentUser.getId())) {
    throw new UnauthorizedTaskOperationException();
}

// 7. 记录历史
workflowHistoryService.record(new WorkflowHistory(
    task.getId(),
    currentUser.getId(),
    action,
    request.getComment(),
    LocalDateTime.now()
));
```

**工作量**: 20-25 小时

---

## 🔧 认证模式改造

### 问题描述

当前多个端点问题:

```
❌ 问题 1: 显式传递 userId 参数
POST /api/v1/workflows/approve?userId=123

❌ 问题 2: 路径中包含 userId
GET /api/v1/workflows/user/123/tasks

✅ 正确做法:
使用 @AuthenticationPrincipal 获取当前用户
```

### 需要改造的端点

```
1. 我的待办任务
   ❌ GET /api/v1/workflows/tasks/user/{userId}
   ✅ GET /api/v1/workflows/my-tasks

2. 审批操作
   ❌ POST /tasks/{id}/approve?userId=123
   ✅ POST /tasks/{id}/approve (从 Security 获取)

3. 拒绝操作
   ❌ POST /tasks/{id}/reject?userId=123
   ✅ POST /tasks/{id}/reject (从 Security 获取)

4. 转办操作
   ❌ POST /tasks/{id}/reassign?fromUserId=123&toUserId=456
   ✅ POST /tasks/{id}/reassign (fromUserId 自动从 Security 获取)
```

### 改造步骤

**步骤 1: 修改端点签名**

```java
// ❌ 改造前
@PostMapping("/approve")
public ResponseEntity<WorkflowResponse> approve(
    @RequestParam Long userId,
    @RequestParam String taskId,
    @RequestBody ApprovalRequest request
) { }

// ✅ 改造后
@PostMapping("/tasks/{taskId}/approve")
public ResponseEntity<WorkflowResponse> approveTask(
    @PathVariable String taskId,
    @RequestBody ApprovalRequest request,
    @AuthenticationPrincipal CurrentUser currentUser  // ← 自动获取当前用户
) {
    Long userId = currentUser.getId();
    // ... 业务逻辑
}
```

**步骤 2: 权限检查**

```java
// 验证当前用户是否是该任务的受理人
if (!task.getAssigneeId().equals(currentUser.getId())) {
    throw new UnauthorizedTaskOperationException(
        "You are not the assignee of this task"
    );
}
```

**涉及文件**:
- BusinessWorkflowController (所有涉及当前用户的端点)
- 现有的 WorkflowController (如果已有)
- 集成测试

**工作量**: 8-12 小时

---

## 📊 工作量评估

| 任务 | 工时 | 优先级 |
|------|------|--------|
| BusinessWorkflowController 实现 | 20-25h | 🔴 P0 |
| 认证模式改造 | 5-8h | 🟠 P1 |
| 现有端点签名修复 | 3-5h | 🟠 P1 |
| 单元测试补充 | 6-8h | 🟡 P2 |
| **总计** | **34-46h** | |

---

## ✅ 完成标准

- [ ] BusinessWorkflowController 全部 11 个 API 可用
- [ ] 所有工作流操作使用 @AuthenticationPrincipal
- [ ] 权限检查正确实现
- [ ] 工作流历史正确记录
- [ ] 单元测试覆盖率 > 80%
- [ ] API 文档同步更新
- [ ] 集成测试全部通过

---

## 🚀 实施步骤

### Phase 1: 基础架构 (Day 1)

1. 确认 CurrentUser 类已定义
2. 修改现有端点使用 @AuthenticationPrincipal
3. 添加权限检查

### Phase 2: 核心控制器 (Day 2-3)

1. 创建 BusinessWorkflowController
2. 创建所有必要的 DTO
3. 实现工作流启动逻辑

### Phase 3: 工作流操作 (Day 4)

1. 实现审批/拒绝/转办
2. 实现历史记录
3. 实现查询操作

### Phase 4: 测试和文档 (Day 5)

1. 补充单元测试
2. 运行集成测试
3. 更新 API 文档

---

## 🔗 工作流定义示例

```xml
<!-- indicator_distribution.xml -->
<process id="indicatorDistribution">
    <startEvent id="start" />
    
    <userTask id="submitTask" name="战略部提交指标" 
        assignee="strategic_dept" />
    
    <userTask id="reviewTask" name="职能部审核"
        assignee="functional_dept" />
    
    <userTask id="approveTask" name="二级学院批准"
        assignee="college_admin" />
    
    <endEvent id="end" />
    
    <sequenceFlow sourceRef="start" targetRef="submitTask" />
    <sequenceFlow sourceRef="submitTask" targetRef="reviewTask" />
    <sequenceFlow sourceRef="reviewTask" targetRef="approveTask" />
    <sequenceFlow sourceRef="approveTask" targetRef="end" />
</process>
```

工作流引擎需要解析并执行这个定义。

---

## 📖 相关文件

| 文件 | 说明 |
|------|------|
| sism-workflow/.../domain/WorkflowDefinition.java | 工作流定义聚合根 |
| sism-workflow/.../domain/WorkflowInstance.java | 工作流实例聚合根 |
| sism-workflow/.../domain/WorkflowTask.java | 工作流任务 |
| sism-workflow/.../domain/service/WorkflowEngine.java | 工作流引擎 |

---

## 🔗 前置运行任务

实施本任务前需要完成:
- [ ] 修复任务-01: sism-iam 认证基础 (高度依赖)
- [ ] CurrentUser 类定义 (高度依赖)

---

**创建时间**: 2026-03-14  
**状态**: 待实施  
**业主**: Workflow 团队  
**下一步**: 分解为 JIRA ticket

