# 审计整改汇总报告

**整改日期:** 2026-04-06  
**整改口径:** 以当前仓库代码与实际验证结果为准

---

## 一、整改结论

本轮已完成多模块 round3 收口，重点解决了仍然真实存在、且能在当前代码基础上安全落地的 P0/P1 问题。
当前已完成重点整改并通过验证的模块包括：

- `sism-alert`
- `sism-analytics`
- `sism-execution`
- `sism-iam`
- `sism-main`
- `sism-organization`
- `sism-strategy`
- `sism-task`
- `sism-workflow`

---

## 二、本轮新增完成的关键整改

### 1. sism-organization

- 组织层级管理能力补齐
- 创建组织时支持 `parentOrgId` 与 `sortOrder`
- 新增更新父组织接口：`PUT /api/v1/organizations/{id}/parent`
- 服务层补充循环层级校验，阻止 `A -> B -> C -> A`
- 补齐名称长度、排序与层级回退相关测试

### 2. sism-strategy

- 为剩余高风险写接口补齐权限控制
- 修复 `Indicator.activate()/terminate()` 事件状态值
- `BasicTaskWeightValidationService` 不再扫描全量指标，改为按任务批量加载
- 新增 controller 安全注解回归测试与批量查询测试

### 3. sism-analytics

- 创建接口强制使用认证用户，不再信任请求体里的用户ID
- 收紧 Dashboard / Export / Report 的归属校验
- 导出下载访问校验收紧
- 增加 ExportService 输入校验
- 增加 Dashboard 配置长度约束与日期范围校验
- 新增应用服务与控制器测试

### 4. sism-workflow

- 收紧 legacy `WorkflowController` 的安全边界
- 工作流定义读取接口补齐管理员限制
- 任务完成/失败/审批/拒绝接口改为使用认证主体
- 新增 path/body `taskId` 一致性校验与对应测试
- 统计接口改为管理员权限
- 分页参数和字符串 ID 解析增加保护与测试

### 5. sism-execution

- 报告创建/更新/提交/审批/驳回统一使用认证主体
- GET 查询端点增加组织级访问过滤
- 新增控制器安全测试，验证不会信任外部 `userId/operatorUserId`
- `System.err` 替换为日志输出
- `not found` / `conflict` 场景进一步收敛到共享异常

### 6. sism-task

- GET/读接口权限已补齐
- 增加了对应 controller 测试覆盖
- `searchTasks` 从内存分页改为数据库分页
- 搜索查询直接下推组织和状态约束，避免后置过滤导致总数失真

### 7. sism-alert

- 主应用补齐 `com.sism.alert` 扫描
- 统一告警状态/严重度规范值
- 补齐 DTO 必填校验与 `jsonb` 映射
- 收紧状态流转与权限过滤
- 统计接口改为数据库聚合与按权限聚合
- 新增针对性单测

### 8. sism-iam

- JWT 刷新流程改为回查真实用户，并校验用户仍存在且处于启用状态
- `CurrentUser.isEnabled()` 改为使用真实启用状态
- `NotificationController` 与 `RoleManagementController` 高风险接口已具备管理员限制
- 注册逻辑补齐用户名/密码/姓名/组织ID校验
- `UserProfileController` 未登录语义统一为 `401`
- `createUser` 请求校验补齐
- 新增/更新对应服务测试

### 9. sism-main

- 主应用组件扫描补齐 `com.sism.alert`
- 清理重复扫描配置与冗余 `@Import`
- 安全配置路径常量化
- 新增 `SismMainApplication` 扫描断言测试

### 10. sism-shared-kernel

- 修复 `EnvConfig` 默认值回退逻辑
- `SecurityHeadersFilter` 的 API 前缀改为可配置
- `TokenBlacklistService.clear()` 不再误清空整库
- 新增对应基础设施测试

### 11. 重复目录清理

已清理：

- `sism-analytics/src/main 2`
- `sism-analytics/src/test 2`
- `sism-alert/src/main/java 2`

---

## 三、实际验证命令

```bash
./mvnw -pl sism-alert,sism-analytics,sism-strategy,sism-workflow,sism-main -am test -DskipITs
./mvnw -pl sism-iam,sism-execution -am test -DskipITs
./mvnw -pl sism-organization,sism-task -am test -DskipITs
```

验证结论：上述命令均已执行通过，相关模块测试全部为 `BUILD SUCCESS`。

---

## 四、当前剩余项

当前剩余问题已主要收敛到 P2 及以下，典型包括：

- `sism-strategy` 的 `awaitWorkflowSnapshot` 仍为轮询等待实现
- `sism-execution` 仍存在跨模块直接更新
- `sism-organization` 的 `includeUsers` 参数已明确降级为兼容保留参数，不再作为真实能力推进；树构建性能仍可继续优化
- `sism-main` 与 `sism-shared-kernel` 仍有结构性整理空间（配置归并、基础设施一致性等）
- `sism-alert` 仍有若干列表接口保留内存过滤实现
- `sism-workflow` 仍保留 legacy 接口与更深层权限/性能问题

这些问题不再阻塞当前整仓测试通过，但仍建议在下一轮按 P2 做结构整理。
