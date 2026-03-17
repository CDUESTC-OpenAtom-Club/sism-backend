# SISM 多Agent API测试框架

基于业务流程文档（流程.md）设计的分布式Agent测试系统，用于自动化测试SISM系统的各个模块API。

## 快速开始

### 安装依赖

```bash
cd scripts/agent-testing
npm install
```

### 运行所有测试

```bash
# 顺序执行（推荐）
npm test

# 或并行执行
npm run test:parallel
```

### 运行特定工作流测试

```bash
# 指标创建与下发流程
npm run test:indicator-distribution

# 多级审批流程
npm run test:multi-level-approval

# 学院填报流程
npm run test:report-submission
```

### 运行单个Agent

```bash
# IAM认证Agent
npm run agent:iam

# 战略部门Agent
npm run agent:strategy

# 职能部门Agent
npm run agent:functional

# 学院Agent
npm run agent:college

# 工作流Agent
npm run agent:workflow
```

## 测试流程覆盖

### 阶段1: 指标创建与第一层下发 (战略发展部 -> 职能部门)
- ✅ 草稿指标创建
- ✅ 指标下发（状态变更: DRAFT -> PENDING）
- ✅ 审批驳回（状态回退: PENDING -> DRAFT）
- ✅ 审批通过（状态变更: PENDING -> DISTRIBUTED）

### 阶段2: 指标拆分与第二层下发 (职能部门 -> 学院)
- ✅ 接收父指标
- ✅ 拆分为多个子指标
- ✅ 子指标下发到学院

### 阶段3: 学院进度填报与审批 (核心循环)
- ✅ 创建填报（DRAFT状态）
- ✅ 提交审批（DRAFT -> PENDING）
- ✅ 学院内部审批
- ✅ 职能部门审批
- ✅ 进度更新验证

### 阶段4: 多级审批与驳回机制
- ✅ 节点顺序推进验证
- ✅ 驳回回退到上一节点
- ✅ 审批时间轴完整性

### 阶段5: 特殊场景测试
- ✅ 撤回功能测试
- ✅ 父子指标关系查询
- ✅ 周期标识验证
- ✅ 数据变更对比

## 测试报告

测试完成后，会在 `reports/` 目录下生成以下报告：

- `test-report-<timestamp>.html` - 可视化HTML报告
- `test-report-<timestamp>.json` - JSON格式报告

打开HTML报告即可查看详细的测试结果、通过率和错误信息。

## 配置说明

### 环境配置

编辑 `config/test-users.json`:

```json
{
  "users": [
    {
      "role": "strategic",
      "username": "admin",
      "password": "admin123"
    },
    {
      "role": "functional",
      "username": "func_user",
      "password": "func123"
    },
    {
      "role": "college",
      "username": "college_user",
      "password": "college123"
    }
  ],
  "baseUrl": "http://localhost:8080/api/v1"
}
```

### API端点配置

所有API端点定义在 `config/endpoints.json`，如需修改API路径，请编辑此文件。

## 架构说明

```
MasterAgent (主控)
    ├─ IAMAgent (认证授权)
    ├─ StrategyAgent (战略部门)
    ├─ FunctionalAgent (职能部门)
    ├─ CollegeAgent (学院)
    └─ WorkflowAgent (工作流)
```

每个Agent负责测试特定的业务流程，Agent之间通过共享数据传递信息。

## 故障排除

### 连接失败

确保后端服务已启动：
```bash
cd sism-backend
mvn spring-boot:run
```

### 认证失败

检查 `config/test-users.json` 中的用户名和密码是否正确。

### 测试数据库

建议使用测试数据库，避免污染生产数据。

## 扩展指南

### 添加新的Agent

1. 继承 `BaseAgent` 类
2. 实现 `execute()` 方法
3. 使用 `runScenario()` 和 `runStep()` 组织测试
4. 在 `MasterAgent` 中注册新的Agent

### 添加新的测试场景

在相应的Agent中添加 `runScenario()` 调用，并实现测试逻辑。

## License

MIT
