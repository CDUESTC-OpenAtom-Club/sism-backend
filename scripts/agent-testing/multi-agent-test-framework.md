# SISM 多Agent API测试框架

## 概述

基于业务流程文档（流程.md）设计的分布式Agent测试系统，用于自动化测试SISM系统的各个模块API。

## 架构设计

### Agent角色定义

```
┌─────────────────────────────────────────────────────────────┐
│                      测试协调器 (Master Agent)               │
│                  - 分配测试任务                              │
│                  - 收集测试结果                              │
│                  - 生成测试报告                              │
└─────────────────────────────────────────────────────────────┘
                              ↓
        ┌─────────────────────┼─────────────────────┐
        ↓                     ↓                     ↓
┌───────────────┐   ┌─────────────────┐   ┌───────────────┐
│ 战略部门Agent  │   │  职能部门Agent   │   │   学院Agent    │
│ - 创建指标     │   │ - 拆分指标       │   │ - 填报进度     │
│ - 下发指标     │   │ - 二次下发       │   │ - 提交审批     │
│ - 审批指标     │   │ - 审批           │   │ - 查看状态     │
└───────────────┘   └─────────────────┘   └───────────────┘
        ↓                     ↓                     ↓
┌─────────────────────────────────────────────────────────────┐
│                    共享服务Agent                             │
│  - WorkflowAgent (工作流测试)                                │
│  - IAMAgent (认证授权测试)                                   │
│  - AnalyticsAgent (数据分析测试)                             │
│  - AlertAgent (告警测试)                                     │
└─────────────────────────────────────────────────────────────┘
```

### 测试流程映射

根据`流程.md`文档，测试流程分为以下阶段：

#### 阶段1: 指标创建与第一层下发 (战略发展部 -> 职能部门)
- **测试场景**:
  - 草稿指标创建
  - 指标下发（状态变更: DRAFT -> PENDING）
  - 审批驳回（状态回退: PENDING -> DRAFT）
  - 审批通过（状态变更: PENDING -> DISTRIBUTED）
- **涉及API**:
  - POST /api/v1/indicators (创建)
  - POST /api/v1/indicators/{id}/distribute (下发)
  - POST /api/v1/workflow/approve (审批)
  - POST /api/v1/workflow/reject (驳回)

#### 阶段2: 指标拆分与第二层下发 (职能部门 -> 学院)
- **测试场景**:
  - 接收父指标
  - 拆分为多个子指标
  - 子指标下发到学院
- **涉及API**:
  - GET /api/v1/indicators/{id} (查询父指标)
  - POST /api/v1/indicators (创建子指标, parent_indicator_id)
  - POST /api/v1/indicators/{id}/distribute

#### 阶段3: 学院进度填报与审批 (核心循环)
- **测试场景**:
  - 创建填报（DRAFT状态）
  - 提交审批（DRAFT -> PENDING）
  - 学院内部审批
  - 职能部门审批
  - 进度更新验证
- **涉及API**:
  - POST /api/v1/reports (创建填报)
  - POST /api/v1/reports/{id}/submit (提交)
  - GET /api/v1/workflow/instances/{id}/timeline (审批时间轴)

#### 阶段4: 多级审批与驳回机制
- **测试场景**:
  - 节点顺序推进验证
  - 驳回回退到上一节点
  - 审批时间轴完整性
- **涉及API**:
  - GET /api/v1/workflow/steps (步骤定义)
  - POST /api/v1/workflow/navigate (流转)
  - GET /api/v1/workflow/instances/{id}/history (历史记录)

#### 阶段5: 特殊场景测试
- 撤回功能测试
- 父子指标关系查询
- 周期标识验证
- 数据变更对比

## Agent配置

### 环境配置
```bash
# Base URL
BASE_URL=http://localhost:8080/api/v1

# 测试账户
STRATEGIC_USER=admin:admin123
FUNCTIONAL_USER=func_user:func123
COLLEGE_USER=college_user:college123

# 测试数据存储
TEST_DATA_DIR=./test-data
```

### Agent依赖关系
```
MasterAgent
  ├── IAMAgent (认证，必须首先运行)
  ├── WorkflowAgent (工作流初始化)
  ├── StrategyAgent (战略部门测试)
  │     ├── 依赖: IAMAgent
  │     └── 创建: 指标、审批流
  ├── OrganizationAgent (组织管理测试)
  │     └── 依赖: IAMAgent
  ├── ExecutionAgent (执行层测试)
  │     ├── 依赖: StrategyAgent
  │     └── 测试: 填报、审批
  ├── AnalyticsAgent (数据分析测试)
  │     └── 依赖: ExecutionAgent
  └── AlertAgent (告警测试)
        └── 依赖: 所有其他Agent
```

## 测试脚本结构

```bash
sism-backend/scripts/agent-testing/
├── config/
│   ├── endpoints.json          # API端点配置
│   ├── test-scenarios.json     # 测试场景定义
│   └── test-users.json         # 测试用户配置
├── agents/
│   ├── master-agent.js         # 主控Agent
│   ├── iam-agent.js            # 认证授权Agent
│   ├── strategy-agent.js       # 战略部门Agent
│   ├── functional-agent.js     # 职能部门Agent
│   ├── college-agent.js        # 学院Agent
│   ├── workflow-agent.js       # 工作流Agent
│   ├── analytics-agent.js      # 分析Agent
│   └── alert-agent.js          # 告警Agent
├── lib/
│   ├── api-client.js           # API请求客户端
│   ├── assertions.js           # 断言库
│   ├── data-factory.js         # 测试数据生成器
│   └── test-reporter.js        # 测试报告生成器
├── workflows/
│   ├── indicator-distribution.json  # 指标下发流程
│   ├── report-approval.json         # 填报审批流程
│   └── multi-level-approval.json    # 多级审批流程
└── reports/                         # 测试报告输出目录
```

## 运行方式

### 1. 单个Agent测试
```bash
node scripts/agent-testing/agents/strategy-agent.js
```

### 2. 顺序执行（按依赖关系）
```bash
node scripts/agent-testing/agents/master-agent.js --sequential
```

### 3. 并发执行（独立模块）
```bash
node scripts/agent-testing/agents/master-agent.js --parallel
```

### 4. 指定流程测试
```bash
node scripts/agent-testing/agents/master-agent.js --workflow indicator-distribution
```

## 测试报告格式

```json
{
  "testRunId": "2024-03-16-001",
  "startTime": "2024-03-16T10:00:00Z",
  "endTime": "2024-03-16T10:15:30Z",
  "agents": [
    {
      "name": "StrategyAgent",
      "status": "completed",
      "tests": {
        "total": 15,
        "passed": 14,
        "failed": 1,
        "skipped": 0
      },
      "scenarios": [
        {
          "name": "指标创建与下发",
          "status": "passed",
          "steps": [
            {"name": "创建草稿指标", "status": "passed", "duration": 150},
            {"name": "提交审批", "status": "passed", "duration": 200},
            {"name": "审批通过", "status": "passed", "duration": 180}
          ]
        }
      ]
    }
  ],
  "summary": {
    "totalTests": 85,
    "passed": 82,
    "failed": 3,
    "passRate": "96.5%"
  }
}
```

## 核心功能实现

### 1. 状态流转验证
每个Agent自动验证状态机转换的正确性：
- DRAFT -> PENDING -> DISTRIBUTED (正常流程)
- PENDING -> DRAFT (驳回流程)
- 多状态并行验证

### 2. 审批时间轴完整性
- 自动记录每个节点的审批人、时间、意见
- 验证节点顺序
- 检查驳回回退路径

### 3. 数据一致性检查
- 父子指标关系验证
- 进度更新确认
- 周期标识校验

### 4. 错误场景覆盖
- 权限不足测试
- 状态冲突测试
- 并发修改测试

## 下一步实施

1. ✅ 创建框架结构
2. ⬜ 实现API客户端
3. ⬜ 实现各模块Agent
4. ⬜ 实现主控Agent
5. ⬜ 集成CI/CD
6. ⬜ 生成HTML测试报告
