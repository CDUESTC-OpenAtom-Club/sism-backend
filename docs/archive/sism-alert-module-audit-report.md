# sism-alert 模块审计报告

**审计日期:** 2026-04-06
**审计人员:** Claude Code (Automated Audit)
**模块版本:** 当前主分支

---

## 模块概览

| 项目 | 数据 |
|------|------|
| 模块名称 | sism-alert |
| 模块职责 | 预警管理、预警触发、预警解决 |
| Java 文件总数 | 11 (主目录) + 7 (重复目录) |
| 核心实体 | Alert |
| Repository 数量 | 1 |
| Service 数量 | 1 |
| Controller 数量 | 1 |

### 包结构

```
com.sism.alert/
├── application/
│   └── AlertApplicationService.java
├── domain/
│   ├── Alert.java
│   ├── enums/
│   │   └── AlertSeverity.java
│   └── repository/
│       └── AlertRepository.java
├── infrastructure/
│   └── persistence/
│       └── JpaAlertRepository.java
└── interfaces/
    ├── dto/
    │   ├── AlertRequest.java
    │   ├── AlertStatsDTO.java
    │   └── ResolveAlertRequest.java
    └── rest/
        └── AlertController.java

⚠️ 发现重复目录: src/main/java 2/ 包含重复文件
```

---

## 一、安全漏洞

### 🟠 Medium: 权限检查实现不完整

**文件:** `AlertController.java`
**行号:** 202-217

```java
private void checkAlertAccessPermission(Alert alert, Authentication authentication) {
    if (isAdmin(authentication)) {
        return;
    }
    // All authenticated users can access alerts for now
    // Future: check indicator ownership via indicatorId  // ❌ 权限检查被注释掉
}

private List<Alert> filterAlertsByPermission(List<Alert> alerts, Authentication authentication) {
    if (isAdmin(authentication)) {
        return alerts;
    }
    // All authenticated users can view alerts for now
    // Future: filter by indicator ownership  // ❌ 过滤未实现
    return alerts;
}
```

**问题描述:**
1. 权限检查方法存在但未实际实现
2. 所有认证用户都可以访问所有预警
3. 注释中提到"Future"但没有实现计划

**风险影响:**
- 预警数据可能泄露给不应看到的人
- 跨组织数据访问

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
private void checkAlertAccessPermission(Alert alert, Authentication authentication) {
    if (isAdmin(authentication)) {
        return;
    }

    // 获取当前用户的组织ID
    Long userOrgId = extractUserOrgId(authentication);

    // 获取指标所属组织
    Indicator indicator = indicatorRepository.findById(alert.getIndicatorId())
            .orElseThrow(() -> new IllegalArgumentException("Indicator not found"));

    // 验证用户有权限访问该指标
    if (!indicator.getOwnerOrg().getId().equals(userOrgId)) {
        throw new AuthorizationException("无权访问此预警");
    }
}
```

---

### 🟠 Medium: extractUserId 异常处理问题

**文件:** `AlertController.java`
**行号:** 224-234

```java
private Long extractUserId(Authentication authentication) {
    if (authentication.getPrincipal() instanceof CurrentUser currentUser) {
        return currentUser.getId();
    }
    String username = authentication.getName();
    try {
        return Long.parseLong(username);  // ❌ 假设 username 是数字
    } catch (NumberFormatException e) {
        throw new AuthorizationException("无法获取当前用户ID，请联系管理员");
    }
}
```

**问题描述:**
1. 假设用户名是数字，这在大多数系统中不成立
2. 如果用户名是字符串（如 "admin"），会抛出异常
3. 异常消息暴露了系统实现细节

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
private Long extractUserId(Authentication authentication) {
    if (authentication.getPrincipal() instanceof CurrentUser currentUser) {
        return currentUser.getId();
    }
    throw new AuthorizationException("无法识别当前用户身份");
}
```

---

## 二、潜在 Bug 和逻辑错误

### 🔴 High: 重复的源代码目录

**问题描述:**
存在两个源代码目录：
- `src/main/java/` - 主目录
- `src/main/java 2/` - 重复目录

**风险影响:**
- 构建时可能使用错误的文件版本
- 维护困难
- 潜在的版本不一致

**严重等级:** 🔴 **High**

**建议修复:**
删除 `src/main/java 2/` 目录及其所有内容。

---

### 🟠 Medium: getUnresolvedAlerts 返回错误状态

**文件:** `AlertApplicationService.java`
**行号:** 76-78

```java
public List<Alert> getUnresolvedAlerts() {
    return alertRepository.findByStatus(Alert.STATUS_TRIGGERED);  // ❌ 仅返回 TRIGGERED，不包括 PENDING
}
```

**问题描述:**
方法名为 `getUnresolvedAlerts`，但仅返回 `TRIGGERED` 状态的预警，不包括 `PENDING` 状态。方法名与行为不一致。

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
public List<Alert> getUnresolvedAlerts() {
    List<Alert> triggered = alertRepository.findByStatus(Alert.STATUS_TRIGGERED);
    List<Alert> pending = alertRepository.findByStatus(Alert.STATUS_PENDING);
    List<Alert> result = new ArrayList<>(triggered);
    result.addAll(pending);
    return result;
}

// 或在 Repository 中添加查询方法
List<Alert> findByStatusIn(List<String> statuses);
```

---

### 🟠 Medium: 缺少删除方法

**文件:** `AlertController.java`

**问题描述:**
Controller 中没有删除预警的 API 端点，但 Repository 中定义了 `delete` 方法。这可能是有意设计（预警不应被删除），但需要明确文档说明。

**严重等级:** 🟠 **Medium**

---

### 🟡 Low: 缺少告警窗口定义

**文件:** `Alert.java`

```java
@Column(name = "window_id")
private Long windowId;  // 引用告警窗口，但没有关联实体
```

**问题描述:**
`windowId` 字段引用告警窗口，但模块中没有定义 `AlertWindow` 实体，无法理解窗口的业务含义。

**严重等级:** 🟡 **Low**

---

## 三、性能瓶颈

### 🟠 Medium: 列表查询无分页

**文件:** `AlertController.java`
**行号:** 74-81

```java
@GetMapping
@Operation(summary = "获取所有预警")
public ResponseEntity<ApiResponse<List<Alert>>> getAllAlerts(Authentication authentication) {
    List<Alert> alerts = alertApplicationService.getAllAlerts();  // ❌ 无分页
    List<Alert> filteredAlerts = filterAlertsByPermission(alerts, authentication);
    return ResponseEntity.ok(ApiResponse.success(filteredAlerts));
}
```

**问题描述:**
1. 所有列表查询都无分页
2. 预警数据量大时会有性能问题
3. 内存中过滤（`filterAlertsByPermission`）效率低

**风险影响:**
- 内存溢出风险
- 响应延迟
- 数据库压力

**严重等级:** 🟠 **Medium**

**建议修复:**
```java
@GetMapping
public ResponseEntity<ApiResponse<PageResult<Alert>>> getAllAlerts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Alert> alerts = alertApplicationService.getAllAlerts(pageable);
    return ResponseEntity.ok(ApiResponse.success(PageResult.from(alerts)));
}
```

---

### 🟡 Low: getAlertStats 执行多次数据库查询

**文件:** `AlertApplicationService.java`
**行号:** 107-123

```java
public Map<String, Object> getAlertStats() {
    long totalOpen = alertRepository.countByStatus(Alert.STATUS_TRIGGERED)  // 查询 1
            + alertRepository.countByStatus(Alert.STATUS_PENDING);          // 查询 2

    Map<String, Long> countBySeverity = new LinkedHashMap<>();
    countBySeverity.put("CRITICAL", alertRepository.countBySeverityAndStatus("CRITICAL", Alert.STATUS_TRIGGERED)  // 查询 3
            + alertRepository.countBySeverityAndStatus("CRITICAL", Alert.STATUS_PENDING));                       // 查询 4
    countBySeverity.put("MAJOR", alertRepository.countBySeverityAndStatus("MAJOR", Alert.STATUS_TRIGGERED)      // 查询 5
            + alertRepository.countBySeverityAndStatus("MAJOR", Alert.STATUS_PENDING));                         // 查询 6
    countBySeverity.put("MINOR", alertRepository.countBySeverityAndStatus("MINOR", Alert.STATUS_TRIGGERED)      // 查询 7
            + alertRepository.countBySeverityAndStatus("MINOR", Alert.STATUS_PENDING));                         // 查询 8
    // ...
}
```

**问题描述:**
统计方法执行 8 次数据库查询，可以通过一次分组查询优化。

**严重等级:** 🟡 **Low**

**建议修复:**
```java
// 使用原生 SQL 分组查询
@Query("SELECT a.severity, a.status, COUNT(a) FROM Alert a GROUP BY a.severity, a.status")
List<Object[]> countGroupBySeverityAndStatus();
```

---

## 四、代码质量和可维护性

### 🟠 Medium: 缺少规则定义

**问题描述:**
Alert 实体有 `ruleId` 字段，但模块中没有定义 `AlertRule` 实体或规则引擎逻辑。预警规则如何工作不清楚。

**严重等级:** 🟠 **Medium**

---

### 🟡 Low: 魔法字符串 - 严重程度

**文件:** `AlertApplicationService.java`
**行号:** 112-117

```java
countBySeverity.put("CRITICAL", ...);  // ❌ 魔法字符串
countBySeverity.put("MAJOR", ...);
countBySeverity.put("MINOR", ...);
```

**问题描述:**
严重程度使用字符串硬编码，应使用 `AlertSeverity` 枚举。

**严重等级:** 🟡 **Low**

**建议修复:**
```java
countBySeverity.put(AlertSeverity.CRITICAL.name(), ...);
countBySeverity.put(AlertSeverity.MAJOR.name(), ...);
countBySeverity.put(AlertSeverity.MINOR.name(), ...);
```

---

### 🟡 Low: 缺少预警更新方法

**问题描述:**
只能创建、触发和解决预警，无法更新预警详情。

---

## 五、架构最佳实践

### ✅ 亮点: 权限注解使用正确

**文件:** `AlertController.java`

```java
@PostMapping
@PreAuthorize("isAuthenticated()")  // ✅ 使用权限注解
public ResponseEntity<ApiResponse<Alert>> createAlert(...) { }
```

模块中的所有 API 端点都使用了 `@PreAuthorize("isAuthenticated()")` 注解，确保只有认证用户可以访问。

---

### ✅ 亮点: 聚合根设计

Alert 实体继承自 `AggregateRoot`，实现了 `validate()` 方法，符合 DDD 设计原则。

---

### 🟡 Low: 缺少领域事件

**问题描述:**
预警触发和解决是重要的业务事件，应该发布领域事件：
- `AlertTriggeredEvent`
- `AlertResolvedEvent`

---

## 六、审计总结

### 问题统计

| 严重等级 | 数量 | 类别分布 |
|----------|------|----------|
| 🔴 High | 1 | Bug（重复目录） |
| 🟠 Medium | 6 | 安全、Bug、性能、代码质量 |
| 🟡 Low | 4 | 代码质量、性能 |

### 最紧急修复项

| 优先级 | 问题 | 影响 |
|--------|------|------|
| P1 | 重复的源代码目录 | 构建混乱、版本不一致 |
| P2 | 权限检查未实现 | 数据访问控制缺失 |
| P2 | 列表查询无分页 | 性能问题 |
| P2 | getUnresolvedAlerts 逻辑错误 | 功能不完整 |

### 整体评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 安全性 | 🟠 需改进 | 权限检查存在但未实现 |
| 可靠性 | ✅ 良好 | 实体设计合理 |
| 性能 | 🟠 需改进 | 缺少分页 |
| 可维护性 | 🟠 需改进 | 存在重复目录 |
| 架构合规性 | ✅ 良好 | DDD 分层清晰 |

### 亮点

1. **权限注解**: 所有端点都有权限检查
2. **聚合根设计**: Alert 继承 AggregateRoot
3. **状态机设计**: 预警状态流转清晰
4. **模块独立**: 职责单一，不依赖其他业务模块

### 关键建议

1. **删除重复目录**: 移除 `src/main/java 2/`
2. **实现权限检查**: 完成指标所有权验证
3. **添加分页支持**: 为列表查询添加分页
4. **修复方法语义**: `getUnresolvedAlerts` 应包含 PENDING 状态
5. **添加领域事件**: 预警触发和解决应发布事件

---

**审计完成日期:** 2026-04-06
**下一步行动:** 删除重复目录后重新构建项目