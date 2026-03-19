# 修复任务 02: sism-strategy 规划和里程碑管理

**模块**: sism-strategy  
**实现率**: ~43%  
**优先级**: 🔴 P0 (最高)  
**工作量**: 35-45 小时  
**关键路径**: 是 (战略规划核心)

---

## 📋 问题概述

### 完整问题列表

| 问题类型 | 描述 | 影响 | 严重度 |
|---------|------|------|--------|
| 缺失控制器 | MilestoneController | 无法管理里程碑 | 🔴 致命 |
| 缺失控制器 | PlanController | 无法管理战略规划 | 🔴 致命 |
| API 签名不匹配 | IndicatorController 参数/返回值不符 | 前端集成失败 | 🔴 致命 |
| Repository 缺失 | IndicatorRepository, CycleRepository 无 JPA 实现 | 数据无法持久化 | 🔴 致命 |
| DTO 不完整 | 部分请求响应模型缺失 | API 调用困难 | 🟡 中等 |
| 业务逻辑漏洞 | 指标下发、分解等流程不完整 | 功能不可用 | 🟡 中等 |

### 支撑数据

```
缺失 API 端点数:  14 个
- Milestone:       4 个
- Plan:           10 个

影响模块：8+
需要补充的 DTO: 12+ 个
需要 Repository JPA: 2 个
```

---

## 🎯 缺失的控制器详解

### 1. MilestoneController (缺失)

**位置**: `sism-strategy/src/main/java/.../interface/controller/MilestoneController.java`

**背景**: 里程碑是战略规划中的重要节点，代表重要的阶段目标

**需要实现的 API**:

```java
@RestController
@RequestMapping("/api/v1/milestones")
public class MilestoneController {

    @Autowired
    private MilestoneApplicationService milestoneService;

    // 1. 里程碑查询
    @GetMapping("/{id}")
    public ResponseEntity<MilestoneResponse> getMilestoneById(
        @PathVariable Long id
    ) { }
    
    @GetMapping("/plan/{planId}")
    public ResponseEntity<List<MilestoneResponse>> getMilestonesByPlan(
        @PathVariable Long planId
    ) { }
    
    // 2. 里程碑创建
    @PostMapping
    public ResponseEntity<MilestoneResponse> createMilestone(
        @Valid @RequestBody CreateMilestoneRequest request
    ) { }
    
    // 3. 里程碑更新
    @PutMapping("/{id}")
    public ResponseEntity<MilestoneResponse> updateMilestone(
        @PathVariable Long id,
        @Valid @RequestBody UpdateMilestoneRequest request
    ) { }
    
    // 4. 里程碑删除
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMilestone(@PathVariable Long id) { }
}
```

**需要创建的 DTO 类**:

```java
@Data
class CreateMilestoneRequest {
    @NotBlank
    private String milestoneName;
    
    private String description;
    
    @NotNull
    @Future
    private LocalDateTime targetDate;
    
    @NotNull
    private Long planId;
    
    private Integer priority;
    
    private String status; // PLANNED, IN_PROGRESS, COMPLETED, DELAYED
}

@Data
class UpdateMilestoneRequest {
    private String milestoneName;
    private String description;
    private LocalDateTime targetDate;
    private Integer priority;
    private String status;
}

@Data
class MilestoneResponse {
    private Long id;
    private String milestoneName;
    private String description;
    private LocalDateTime targetDate;
    private LocalDateTime actualDate;
    private String status;
    private Integer priority;
    private Integer completionPercentage;
    private Long planId;
    private LocalDateTime createTime;
}
```

**关键实现要点**:

```java
// 1. 创建时验证计划存在
Plan plan = planService.findById(request.getPlanId());
if (plan == null) {
    throw new PlanNotFoundException();
}

// 2. 目标日期验证
if (request.getTargetDate().isBefore(LocalDateTime.now())) {
    throw new InvalidMilestoneDateException();
}

// 3. 不能删除已完成的里程碑
if (milestone.getStatus().equals("COMPLETED")) {
    throw new CannotDeleteCompletedMilestoneException();
}

// 4. 更新状态时检查依赖
if (newStatus.equals("COMPLETED")) {
    // 检查是否所有依赖任务都已完成
    List<Task> dependentTasks = taskService.findByMilestoneId(id);
    boolean allCompleted = dependentTasks.stream()
        .allMatch(t -> t.isCompleted());
    if (!allCompleted) {
        throw new DependentTasksNotCompletedException();
    }
}
```

**工作量**: 8-10 小时

---

### 2. PlanController (缺失)

**位置**: `sism-strategy/src/main/java/.../interface/controller/PlanController.java`

**背景**: Plan（战略规划）是整个战略管理的顶层，包含了整个规划周期内的目标和指标

**需要实现的 API**:

```java
@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

    @Autowired
    private PlanApplicationService planService;

    // 1. 规划查询
    @GetMapping
    public ResponseEntity<PageResult<PlanResponse>> listPlans(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) String status
    ) { }
    
    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> getPlanById(@PathVariable Long id) { }
    
    @GetMapping("/cycle/{cycleId}")
    public ResponseEntity<List<PlanResponse>> getPlansByCycle(
        @PathVariable Long cycleId
    ) { }
    
    // 2. 规划创建
    @PostMapping
    public ResponseEntity<PlanResponse> createPlan(
        @Valid @RequestBody CreatePlanRequest request
    ) { }
    
    // 3. 规划更新
    @PutMapping("/{id}")
    public ResponseEntity<PlanResponse> updatePlan(
        @PathVariable Long id,
        @Valid @RequestBody UpdatePlanRequest request
    ) { }
    
    // 4. 规划删除
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) { }
    
    // 5. 规划发布（状态流转）
    @PostMapping("/{id}/publish")
    public ResponseEntity<PlanResponse> publishPlan(@PathVariable Long id) { }
    
    // 6. 规划归档
    @PostMapping("/{id}/archive")
    public ResponseEntity<PlanResponse> archivePlan(@PathVariable Long id) { }
    
    // 7. 获取规划详情（包含指标和里程碑）
    @GetMapping("/{id}/details")
    public ResponseEntity<PlanDetailsResponse> getPlanDetails(
        @PathVariable Long id
    ) { }
}
```

**需要创建的 DTO 类**:

```java
@Data
class CreatePlanRequest {
    @NotBlank
    private String planName;
    
    private String description;
    
    @NotNull
    private Long cycleId;
    
    @NotBlank
    private String planType; // STRATEGY, OPERATION, etc.
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    private String ownerDepartment;
}

@Data
class UpdatePlanRequest {
    private String planName;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String ownerDepartment;
}

@Data
class PlanResponse {
    private Long id;
    private String planName;
    private String description;
    private String planType;
    private String status; // DRAFT, PUBLISHED, ARCHIVED
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String ownerDepartment;
    private Integer completionPercentage;
    private Integer indicatorCount;
    private Integer milestoneCount;
    private LocalDateTime createTime;
}

@Data
class PlanDetailsResponse extends PlanResponse {
    private List<IndicatorResponse> indicators;
    private List<MilestoneResponse> milestones;
    private List<TaskResponse> tasks;
}
```

**关键实现要点**:

```java
// 1. 创建时验证周期存在
Cycle cycle = cycleService.findById(request.getCycleId());
if (cycle == null) {
    throw new CycleNotFoundException();
}

// 2. 验证日期逻辑
if (request.getStartDate().isAfter(request.getEndDate())) {
    throw new InvalidDateRangeException();
}

// 3. 发布时检查必要条件
if (plan.getStatus().equals("PUBLISHED")) {
    throw new PlanAlreadyPublishedException();
}
// 检查是否有足够的指标
if (plan.getIndicators().isEmpty()) {
    throw new NoIndicatorsDefinedException();
}

// 4. 计算完成度
int completionPercentage = calculateCompletion(plan);

// 5. 处理状态流转
plan.publish(); // 发布
plan.archive(); // 归档
// 状态不能倒退（已发布的规划不能回到草稿）
```

**工作量**: 12-15 小时

---

## 🔧 IndicatorController 签名修复

### 问题描述

文档说的 API 签名与实现不一致，导致前端集成困难。

### 需要修复的端点 (5+)

| 端点 | 文档签名 | 实现签名 | 需要改动 |
|------|---------|---------|---------|
| GET /indicators | Query 返回 List | 返回 PageResult | ✅ 统一使用分页 |
| POST /indicators | 参数 X | 参数 Y | ✅ 统一参数格式 |
| PUT /indicators/{id} | 返回 X | 返回 Y | ✅ 统一响应格式 |

### 修复步骤

**步骤 1: 规范化列表查询**

```java
// ✅ 修复后的标准格式
@GetMapping
public ResponseEntity<PageResult<IndicatorResponse>> listIndicators(
    @RequestParam(defaultValue = "1") int pageNum,
    @RequestParam(defaultValue = "10") int pageSize,
    @RequestParam(required = false) String status,
    @RequestParam(required = false) Long cycleId
) {
    return ResponseEntity.ok(
        indicatorService.queryIndicators(pageNum, pageSize, status, cycleId)
    );
}
```

**步骤 2: 规范化创建请求**

```java
// ✅ 修复后的创建请求
@Data
class CreateIndicatorRequest {
    @NotBlank
    private String indicatorName;
    
    @NotBlank
    private String indicatorCode;
    
    private String description;
    
    @NotNull
    private Long cycleId;
    
    @NotNull
    private Long departmentId;
    
    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal targetValue;
    
    private String unit;
    
    private String dimension; // FINANCIAL, OPERATION, etc.
}
```

**步骤 3: 规范化响应格式**

```java
@Data
class IndicatorResponse {
    private Long id;
    private String indicatorName;
    private String indicatorCode;
    private String description;
    private Long cycleId;
    private Long departmentId;
    private String departmentName;
    private BigDecimal targetValue;
    private String unit;
    private String status;
    private String dimension;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

**涉及文件**:
- IndicatorController (所有 5 个端点)
- 相关 DTO 类
- 集成测试

**工作量**: 8-10 小时

---

## 📦 Repository JPA 实现

### IndicatorRepository 实现

**位置**: `sism-strategy/src/main/java/.../infrastructure/persistence/IndicatorRepositoryImpl.java`

```java
@Repository
public class IndicatorRepositoryImpl implements IndicatorRepository {

    @Autowired
    private IndicatorJpaRepository jpaRepository;

    @Override
    public Page<Indicator> findByCycleId(Long cycleId, Pageable pageable) {
        return jpaRepository.findByCycleId(cycleId, pageable);
    }

    @Override
    public Page<Indicator> findByStatus(String status, Pageable pageable) {
        return jpaRepository.findByStatus(status, pageable);
    }

    @Override
    public List<Indicator> findByDepartmentId(Long deptId) {
        return jpaRepository.findByDepartmentId(deptId);
    }

    // ... 其他方法
}
```

**对应 JPA 接口**:

```java
@Repository
public interface IndicatorJpaRepository extends JpaRepository<Indicator, Long> {
    Page<Indicator> findByCycleId(Long cycleId, Pageable pageable);
    Page<Indicator> findByStatus(String status, Pageable pageable);
    List<Indicator> findByDepartmentId(Long deptId);
}
```

### CycleRepository 实现

参考 Repository JPA 实现指南的 CycleRepositoryImpl 部分

**工作量**: 4-6 小时

---

## 📊 工作量评估

| 任务 | 工时 | 优先级 |
|------|------|--------|
| MilestoneController 实现 | 8-10h | 🔴 P0 |
| PlanController 实现 | 12-15h | 🔴 P0 |
| IndicatorController 签名修复 | 8-10h | 🔴 P0 |
| Repository JPA 实现 | 4-6h | 🔴 P0 |
| 单元测试补充 | 8-10h | 🟡 P2 |
| **总计** | **40-51h** | |

---

## ✅ 完成标准

- [ ] MilestoneController 全部 4 个 API 可用
- [ ] PlanController 全部 10 个 API 可用
- [ ] IndicatorController 所有签名匹配文档
- [ ] IndicatorRepository 和 CycleRepository 有 JPA 实现
- [ ] 所有 DTO 已定义并使用
- [ ] 单元测试覆盖率 > 80%
- [ ] API 文档同步更新
- [ ] 集成测试全部通过

---

## 🚀 实施步骤

### Phase 1: 基础 Repository (Day 1)

1. 实现 IndicatorRepositoryImpl 和 JPA 接口
2. 实现 CycleRepositoryImpl 和 JPA 接口
3. 验证编译通过

### Phase 2: 里程碑管理 (Day 2)

1. 创建 MilestoneController
2. 创建所有必要的 DTO
3. 实现业务逻辑和验证

### Phase 3: 规划管理 (Day 3-4)

1. 创建 PlanController
2. 创建所有必要的 DTO
3. 实现业务逻辑和验证

### Phase 4: 指标修复和测试 (Day 5)

1. 修复 IndicatorController 所有签名
2. 补充单元和集成测试
3. 更新 API 文档

---

## 🔗 依赖关系

```
IndicatorRepository JPA 实现
    ↓
IndicatorController 可用
    ↓
MilestoneController (依赖 Plan 存在)
    ↓
PlanController (依赖 Indicator 和 Milestone)
```

需要按顺序实施。

---

## 📖 相关文件

| 文件 | 说明 |
|--------|------|
| sism-strategy/.../service/IndicatorDomainService.java | 指标领域服务 |
| sism-strategy/.../service/PlanDomainService.java | 规划领域服务 |
| sism-strategy/.../aggregates/Plan.java | 规划聚合根 |
| sism-strategy/.../aggregates/Indicator.java | 指标聚合根 |

---

## 🔗 前置运行任务

实施本任务前需要完成:
- [ ] 修复任务-01: sism-iam 认证基础 (中等依赖)
- [ ] 快速修复指南: Repository JPA 实现框架 (高度依赖)

---

**创建时间**: 2026-03-14  
**状态**: 待实施  
**业主**: Strategy 团队  
**下一步**: 分解为 JIRA ticket

