# 🎯 API 修复任务 - 快速参考卡

**生成时间**: 2026-03-14  
**用途**: 开发者快速查阅，了解全局和具体任务

---

## 📊 一页纸概览

### 项目状态

```
┌─────────────────────────────────────────────────────┐
│          API 实现进度                               │
├─────────────────────────────────────────────────────┤
│ 高优先级 (P0):  ████░░░░░░  20% (Week 1)           │
│ 中优先级 (P1):  ░░░░░░░░░░   0% (Week 2-3)         │
│ 低优先级 (P2):  ░░░░░░░░░░   0% (Week 4+)          │
│                                                     │
│ 总体进度:       ████░░░░░░░░  10-15%                │
│ 预期完成:       2026-04-30 (6-7 周)                 │
└─────────────────────────────────────────────────────┘
```

### 当前焦点 (Week 1)

```
🎯 sism-iam:       17.9% → 目标 80% (40-50h)
🎯 sism-strategy:  43% → 目标 90% (35-45h) 
🎯 sism-workflow:  72% → 目标 95% (25-35h)

▶ 每个任务有专门文档，见下表
```

---

## 📚 文档速查表

| 模块 | 优先级 | 文档 | 文件大小 | 关键内容 |
|------|--------|------|---------|---------|
| sism-iam | 🔴 P0 | [✅ 已生成](修复任务-01-sism-iam-角色和用户管理.md) | 12KB | Role + Profile 控制器 |
| sism-strategy | 🔴 P0 | [✅ 已生成](修复任务-02-sism-strategy-规划和里程碑.md) | 14KB | Plan + Milestone 控制器 |
| sism-workflow | 🔴 P0 | [✅ 已生成](修复任务-03-sism-workflow-业务工作流.md) | 13KB | BusinessWorkflow 控制器 |
| sism-task | 🟠 P1 | 📝 待生成 | - | Task API 签名修复 |
| sism-execution | 🟠 P1 | 📝 待生成 | - | Report 系统整架构 |
| sism-alert | 🟠 P1 | 📝 待生成 | - | Warning 控制器 |
| sism-organization | 🟠 P1 | 📝 待生成 | - | Org 管理系统 |
| sism-analytics | 🟡 P2 | 📝 待生成 | - | Analytics 功能 |
| sism-shared-kernel | 🟡 P2 | 📝 待生成 | - | 共享内核 |
| sism-main | 🟡 P2 | 📝 待生成 | - | 系统监控 |

---

## ⚡ 快速启动清单

### 第一天 Task List

```bash
# 1. 准备工作区
cd sism-backend
./mvnw clean install -DskipTests

# 2. 验证系统状态
./mvnw compile

# 3. 创建开发分支
git checkout -b fix/api-implementation

# 4. 阅读文档（优先级）
□ 修复任务-总索引和执行指南.md (20 min)
□ 修复任务-01-sism-iam-角色和用户管理.md (30 min)
□ 修复任务-02-sism-strategy-规划和里程碑.md (30 min)
□ 修复任务-03-sism-workflow-业务工作流.md (30 min)

# 5. 开始第一个任务（sism-iam）
□ 创建 RoleManagementController
□ 创建 UserProfileController
□ 创建相应的 DTO 类
□ 运行 ./mvnw test
```

---

## 🔧 键盘快速跳转

### 从本快速参考卡跳转到详细文档

```
按任务优先级:
  01-iam         → [详细文档](修复任务-01-sism-iam-角色和用户管理.md)
  02-strategy    → [详细文档](修复任务-02-sism-strategy-规划和里程碑.md)
  03-workflow    → [详细文档](修复任务-03-sism-workflow-业务工作流.md)
  
执行计划:
  总体计划       → [详细文档](修复任务-总索引和执行指南.md)
  Week 1 计划    → 见总索引
  依赖关系图     → 见总索引
  
问题排查:
  API问题原文    → 见 /sism-backend/docs/API问题.md
  常见问题       → 见总索引的"常见问题"
```

---

## 🎬 工作流快速参考

### 新增控制器的标准步骤

```java
// 1. 创建 Controller
@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {
    
    @Autowired
    private ResourceApplicationService service;
    
    // 2. 添加 API 端点
    @GetMapping
    public ResponseEntity<PageResult<ResourceResponse>> list() { }
    
    @PostMapping
    public ResponseEntity<ResourceResponse> create(
        @Valid @RequestBody CreateResourceRequest request
    ) { }
}

// 3. 创建 DTO
@Data
class CreateResourceRequest {
    @NotBlank
    private String name;
}

@Data
class ResourceResponse {
    private Long id;
    private String name;
}

// 4. 测试
@Test
void testCreateResource() {
    CreateResourceRequest req = new CreateResourceRequest();
    ResponseEntity<ResourceResponse> response = 
        controller.create(req);
    assertEquals(200, response.getStatusCodeValue());
}
```

### 改造现有 API 的标准步骤

```java
// ❌ 旧: 显式 userId 参数
@PostMapping("/approve")
public void approve(
    @RequestParam Long userId,
    @RequestParam String taskId
) { }

// ✅ 新: 使用 @AuthenticationPrincipal
@PostMapping("/tasks/{taskId}/approve")
public void approveTask(
    @PathVariable String taskId,
    @AuthenticationPrincipal CurrentUser currentUser
) {
    Long userId = currentUser.getId();
}
```

---

## 🚨 常见陷阱

| 陷阱 | 避免方法 | 相关任务 |
|------|---------|---------|
| ❌ 忘记权限检查 | 使用 @Secured 或 @PreAuthorize | 所有需有权限的 API |
| ❌ DTO 字段缺验证 | @NotNull, @NotBlank, @Email | 所有 Request DTO |
| ❌ 路径规范不一致 | 参考 /api/v1/resources 格式 | 所有新 API |
| ❌ 忘记 API 文档 | 添加 @Operation, @Parameter 注解 | 每个新 API |
| ❌ 没有集成测试 | 写 @SpringBootTest 测试 | 每个 Controller |

---

## 📈 工时投资对标

### 各任务的投资回报

```
sism-iam (43-60h)
  功能: 用户管理 + 权限管理 (系统基础)
  回报: 🟢 极高 (所有功能都需要)
  优先级: 🔴 必做
  
sism-strategy (40-51h)
  功能: 战略规划 (核心业务)
  回报: 🟢 极高 (指标和规划)
  优先级: 🔴 必做
  
sism-workflow (34-46h)
  功能: 工作流 (审批流程)
  回报: 🟡 高 (许多流程依赖)
  优先级: 🔴 必做
  
sism-task (15-20h)
  功能: 任务管理
  回报: 🟡 高 (日常工作)
  优先级: 🟠 重要
  
sism-execution (25-35h)
  功能: 执行报告
  回报: 🟡 高
  优先级: 🟠 重要
```

---

## 🎓 关键概念速查

### Spring Security 快速参考

```java
// 获取当前用户
@AuthenticationPrincipal CurrentUser user

// 检查权限
@Secured({"ROLE_ADMIN"})
@PreAuthorize("hasRole('ADMIN')")

// 手动检查
if (!user.getRoles().contains("APPROVER")) {
    throw new AccessDeniedException();
}
```

### 数据分页快速参考

```java
// API 参数
@RequestParam(defaultValue = "1") int pageNum,
@RequestParam(defaultValue = "10") int pageSize

// Service 层
Page<Entity> page = repository.findAll(
    PageRequest.of(pageNum - 1, pageSize)
);

// 响应
return ResponseEntity.ok(
    PageResult.success(page, pageNum, pageSize)
);
```

### 异常处理快速参考

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(
    ResourceNotFoundException e) {
    return ResponseEntity
        .status(404)
        .body(new ErrorResponse(
            "RESOURCE_NOT_FOUND",
            e.getMessage()
        ));
}
```

---

## 📞 快速联系

| 需求 | 联系方式 |
|------|---------|
| API 问题澄清 | 查看 /sism-backend/docs/API问题.md |
| 设计问题讨论 | 见总索引"代码审查要点" |
| 进度追踪 | 更新状态模板，见总索引 |
| 技术支持 | 见总索引"支持"部分 |

---

## 🔄 状态检查清单

### 每日检查

```
□ 编译通过 (./mvnw clean compile)
□ 没有 IDE 红色波浪线
□ 单元测试通过 (./mvnw test)
□ 代码覆盖率 > 70%
```

### 每个 PR 前检查

```
□ 所有单元测试通过
□ 集成测试通过
□ Javadoc 完整
□ API 文档已更新
□ 代码符合规范 (Spotbugs, Checkstyle)
```

### Week 检查点

```
✅ Week 1 末
  - sism-iam 75% 完成
  - sism-strategy 60% 完成
  - sism-workflow 30% 完成
  - 系统可编译并启动
  
✅ Week 2 末
  - P0 全部完成
  - P1 第一个完成
  - 集成测试 > 70% 通过
  
✅ Week 4 末
  - P0 + P1 全部完成
  - 集成测试 > 90% 通过
  - 前端可调用所有必需 API
```

---

## 🎯 成功度量

### 任何时可检查的 KPI

```
代码质量指标:
  ✓ 编译通过率: 100%
  ✓ 单元测试覆盖: > 70%
  ✓ 集成测试通过: > 90%
  
功能完整指标:
  ✓ API 端点实现: 100% (按计划)
  ✓ DTO 类完整: 100%
  ✓ API 文档: 100%
  
系统就绪指标:
  ✓ 系统启动: ✅ 成功
  ✓ 基础流程: ✅ 可跑通
  ✓ 前端集成: ✅ 可调用
```

---

## 🚀 今天就做这些

### 如果你只有 2 小时

```
□ 阅读本快速参考卡 (10 min)
□ 阅读总索引文档 (20 min)
□ 阅读 sism-iam 修复任务 (30 min)
□ 浏览 API问题.md 原始文档 (20 min)
□ 讨论问题和采纳反馈 (40 min)
```

### 如果你要开始编码

```
□ 检查列出的附件（全部代码）
□ 在 IDE 创建 RoleManagementController
□ 复制对应的 DTO 代码
□ 创建对应的测试
□ 运行并验证 ./mvnw test
□ 提交 PR 让人审查
```

---

**快速参考卡版本**: 1.0  
**最后更新**: 2026-03-14  
**打印友好**: 是 (选择"打印为PDF")

