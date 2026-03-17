# SISM 多Agent API测试框架 - 快速启动指南

## 📋 框架概览

这是一个基于业务流程文档（`docs/流程.md`）设计的分布式Agent测试系统，实现了SISM系统的全流程自动化测试。

### 核心特性

✅ **完整流程覆盖** - 涵盖战略→职能→学院全链路业务闭环
✅ **多Agent协作** - 5个专业Agent各司其职，协同工作
✅ **智能数据管理** - Agent间自动共享测试数据
✅ **灵活执行模式** - 支持顺序执行、并行执行、单Agent测试
✅ **详细测试报告** - 生成HTML和JSON格式的可视化报告

### 测试流程图

```
┌─────────────────────────────────────────────────────────────┐
│                      IAM认证Agent                            │
│                  - 登录认证 (第一步)                         │
│                  - Token管理                                 │
│                  - 用户信息获取                               │
└─────────────────────────────────────────────────────────────┘
                              ↓
        ┌─────────────────────┼─────────────────────┐
        ↓                     ↓                     ↓
┌───────────────┐   ┌─────────────────┐   ┌───────────────┐
│ 战略部门Agent  │   │  职能部门Agent   │   │   学院Agent    │
│ - 创建指标     │   │ - 接收父指标     │   │ - 查看指标     │
│ - 下发指标     │   │ - 拆分指标       │   │ - 创建填报     │
│ - 审批通过     │   │ - 二次下发       │   │ - 提交审批     │
└───────────────┘   └─────────────────┘   └───────────────┘
        ↓                     ↓                     ↓
        └─────────────────────┼─────────────────────┘
                              ↓
                    ┌─────────────────┐
                    │  工作流Agent     │
                    │ - 多级审批       │
                    │ - 驳回回退       │
                    │ - 时间轴验证     │
                    └─────────────────┘
```

## 🚀 5分钟快速开始

### 步骤1: 安装依赖（1分钟）

```bash
cd sism-backend/scripts/agent-testing
npm install
```

### 步骤2: 配置测试环境（1分钟）

编辑 `config/test-users.json`，确保后端地址正确：

```json
{
  "baseUrl": "http://localhost:8080/api/v1",
  "users": [
    {
      "role": "strategic",
      "username": "admin",
      "password": "admin123"
    }
  ]
}
```

### 步骤3: 启动后端服务（2分钟）

```bash
cd sism-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

等待看到 "Started SismApplication" 表示启动成功。

### 步骤4: 运行测试（1分钟）

**新开一个终端**，执行：

```bash
cd sism-backend/scripts/agent-testing

# 方式1: 使用启动脚本（推荐）
./run-tests.sh --all

# 方式2: 使用npm命令
npm test
```

### 查看测试结果

测试完成后，会显示：
```
✅ 测试完成
JSON报告: reports/test-report-2024-03-16-100000.json
HTML报告: reports/test-report-2024-03-16-100000.html
```

打开HTML报告即可看到详细测试结果：

```bash
# Mac
open reports/test-report-*.html

# Linux
xdg-open reports/test-report-*.html
```

## 📊 测试覆盖场景

### ✅ 已实现的测试场景

| 场景ID | 场景名称 | 涉及Agent | 测试内容 |
|--------|---------|----------|---------|
| 1 | 指标创建与下发 | IAM + Strategy | 创建→审批→下发全流程 |
| 2 | 指标拆分 | IAM + Strategy + Functional | 父子指标关系验证 |
| 3 | 学院填报 | IAM + Strategy + College | 填报创建→提交→审批 |
| 4 | 多级审批 | All Agents | 学院审批→职能部门审批 |
| 5 | 审批驳回 | IAM + Strategy + Workflow | 驳回→状态回退→意见记录 |
| 6 | 审批时间轴 | IAM + Strategy + Workflow | 时间轴完整性验证 |
| 7 | 填报撤回 | IAM + Strategy + College | 提交后撤回功能 |
| 8 | 父子指标关系 | IAM + Strategy + Functional | 关系查询+无进度聚合 |
| 9 | 周期标识 | IAM + Strategy | 周期格式验证 |
| 10 | 数据变更追踪 | All Agents | 变更记录+对比 |

### 🎯 测试的核心业务逻辑

#### 状态流转验证
```
指标状态: DRAFT → PENDING → DISTRIBUTED
填报状态: DRAFT → PENDING → APPROVED/DRAFT
驳回流程: PENDING → DRAFT (带原因)
```

#### 多级审批验证
```
学院填报 → 学院审批 → 职能审批 → 完成
            ↓ 驳回      ↓ 驳回
          回到DRAFT  回到DRAFT
```

#### 数据一致性验证
```
✅ 父子指标关系正确
✅ 无进度聚合（仅关系查询）
✅ 周期标识完整
✅ 审批意见记录
```

## 🔧 常用命令

### 运行特定工作流

```bash
# 指标下发流程
./run-tests.sh --workflow indicator-distribution

# 多级审批流程
./run-tests.sh --workflow multi-level-approval

# 学院填报流程
./run-tests.sh --workflow report-submission
```

### 运行单个Agent

```bash
./run-tests.sh --iam          # IAM认证
./run-tests.sh --strategy     # 战略部门
./run-tests.sh --functional   # 职能部门
./run-tests.sh --college      # 学院
./run-tests.sh --workflow     # 工作流
```

### 并行执行

```bash
./run-tests.sh --parallel
```

### 查看测试报告

```bash
./run-tests.sh --report
```

## 📁 项目结构

```
agent-testing/
├── agents/              # Agent实现
│   ├── base-agent.js       # 基础Agent类
│   ├── master-agent.js     # 主控Agent
│   ├── iam-agent.js        # IAM认证Agent
│   ├── strategy-agent.js   # 战略部门Agent
│   ├── functional-agent.js # 职能部门Agent
│   ├── college-agent.js    # 学院Agent
│   └── workflow-agent.js   # 工作流Agent
├── config/              # 配置文件
│   ├── test-users.json     # 测试用户配置
│   ├── endpoints.json      # API端点配置
│   └── test-scenarios.json # 测试场景定义
├── lib/                 # 核心库
│   ├── api-client.js       # API客户端
│   ├── assertions.js       # 断言库
│   ├── data-factory.js     # 测试数据生成器
│   └── test-reporter.js    # 测试报告生成器
├── workflows/           # 工作流定义（预留）
├── reports/             # 测试报告输出目录
├── package.json         # NPM配置
├── README.md            # 完整文档
├── USAGE_EXAMPLES.md    # 使用示例
├── QUICKSTART.md        # 本文档
└── run-tests.sh         # 启动脚本
```

## 🎨 测试报告预览

测试报告包含：
- 📊 总体通过率
- 📈 各Agent测试结果
- ✅/❌ 每个场景的详细步骤
- ⏱️ 执行时间统计
- 💾 失败原因记录

HTML报告特性：
- 响应式设计
- 颜色编码状态
- 可折叠的场景详情
- 性能指标可视化

## 🔍 故障排除

### 问题1: 连接后端失败

```bash
# 检查后端是否运行
curl http://localhost:8080/api/v1/health

# 查看后端日志
cd sism-backend
tail -f logs/sism-backend.log
```

### 问题2: 认证失败

```bash
# 检查测试用户
psql -U postgres -d sism_dev -c "SELECT username, real_name FROM sys_user;"

# 重置测试用户密码
psql -U postgres -d sism_dev -c "UPDATE sys_user SET password = 'admin123' WHERE username = 'admin';"
```

### 问题3: 测试数据不足

```bash
# 加载测试数据
psql -U postgres -d sism_dev -f database/seeds/seed-data.sql
psql -U postgres -d sism_dev -f database/seeds/seed-indicators-2026.sql
```

## 📚 进阶使用

### 自定义测试场景

编辑 `agents/xxx-agent.js`，添加新的测试场景：

```javascript
await this.runScenario('我的测试场景', async (scenario) => {
  await this.runStep('步骤1', async () => {
    // 测试代码
  }, scenario);

  await this.runStep('步骤2', async () => {
    // 测试代码
  }, scenario);
});
```

### 修改测试数据

编辑 `lib/data-factory.js`，调整数据生成逻辑：

```javascript
generateIndicator(overrides = {}) {
  return {
    name: this.randomString('指标'),
    targetValue: this.randomNumber(80, 100), // 修改这里
    ...overrides
  };
}
```

### 集成到CI/CD

参考 `USAGE_EXAMPLES.md` 中的CI/CD集成示例。

## 🆘 获取帮助

```bash
# 查看帮助信息
./run-tests.sh --help

# 查看完整文档
cat README.md
cat USAGE_EXAMPLES.md
```

## 📞 联系方式

如有问题，请查看：
- 完整文档: `README.md`
- 使用示例: `USAGE_EXAMPLES.md`
- 架构设计: `multi-agent-test-framework.md`
- 业务流程: `../../docs/流程.md`

---

**祝测试愉快！** 🎉
