# SISM 多Agent API测试框架 - 使用示例

## 目录
- [快速开始](#快速开始)
- [详细使用示例](#详细使用示例)
- [测试场景说明](#测试场景说明)
- [常见问题](#常见问题)
- [最佳实践](#最佳实践)

## 快速开始

### 1. 安装依赖

```bash
cd sism-backend/scripts/agent-testing
npm install
```

### 2. 配置测试环境

编辑 `config/test-users.json`，确保后端服务地址和测试账户正确：

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

### 3. 启动后端服务

```bash
cd sism-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. 运行测试

```bash
# 使用启动脚本
./run-tests.sh --all

# 或使用npm
npm test
```

## 详细使用示例

### 场景1: 测试完整的指标下发流程

```bash
# 测试从战略部门创建指标到职能部门的完整流程
./run-tests.sh --workflow indicator-distribution
```

这个测试会执行：
1. IAM Agent - 登录认证
2. Strategy Agent - 创建指标、提交审批、审批通过
3. 验证状态流转：DRAFT → PENDING → DISTRIBUTED

### 场景2: 测试多级审批流程

```bash
# 测试填报的多级审批（学院审批 → 职能部门审批）
./run-tests.sh --workflow multi-level-approval
```

测试内容：
- 学院创建并提交填报
- 学院内部审批
- 职能部门审批
- 验证审批时间轴完整性

### 场景3: 测试审批驳回流程

```bash
# 测试驳回后状态回退
./run-tests.sh --workflow approval-rejection
```

测试内容：
- 创建并提交审批
- 审批人驳回（必须填写意见）
- 验证状态回退到 DRAFT
- 验证驳回意见已记录

### 场景4: 并行执行独立模块

```bash
# 并行执行可以同时运行的Agent
./run-tests.sh --parallel
```

并行策略：
- IAM 必须首先执行
- Strategy 依赖 IAM
- Functional 和 College 可以并行执行
- Workflow 依赖所有前面的Agent

## 测试场景说明

### IAM认证Agent测试场景

```javascript
// 1. 用户登录测试
✓ 使用正确凭证登录
✓ 验证Token有效性

// 2. 错误凭证登录测试
✓ 使用错误密码登录（应返回401）

// 3. Token刷新测试
✓ 刷新访问令牌
✓ 验证新Token与旧Token不同

// 4. 获取用户列表
✓ 获取所有用户
✓ 按角色分类用户

// 5. 用户登出
✓ 登出当前用户
```

### 战略部门Agent测试场景

```javascript
// 1. 创建草稿指标
✓ 创建单个指标
✓ 批量创建指标

// 2. 更新指标
✓ 修改草稿状态的指标

// 3. 指标下发审批流程
✓ 提交指标下发审批（DRAFT → PENDING）
✓ 查询审批时间轴

// 4. 审批通过下发
✓ 审批人审批通过（PENDING → DISTRIBUTED）

// 5. 查询指标列表
✓ 获取所有指标
✓ 按状态筛选指标

// 6. 指标驳回流程
✓ 创建新指标并提交审批
✓ 审批人驳回指标（PENDING → DRAFT）

// 7. 删除草稿指标
✓ 删除草稿状态的指标

// 8. 创建考核周期
✓ 创建2026年度周期
✓ 激活考核周期
```

### 职能部门Agent测试场景

```javascript
// 1. 接收父指标
✓ 获取已下发的指标
✓ 查看父指标详情

// 2. 拆分指标为子指标
✓ 创建多个子指标
✓ 验证父子关系

// 3. 验证父子指标关系
✓ 查询子指标列表
✓ 验证没有进度聚合

// 4. 下发子指标到学院
✓ 提交子指标下发审批
✓ 审批通过子指标下发

// 5. 审批学院填报
✓ 获取待审批的填报
✓ 审批填报
```

### 学院Agent测试场景

```javascript
// 1. 查看已分配指标
✓ 获取已下发的指标
✓ 验证周期标识

// 2. 创建填报草稿
✓ 为指标创建填报
✓ 保存多次草稿

// 3. 提交填报审批
✓ 提交填报（DRAFT → PENDING）
✓ 查看审批时间轴

// 4. 填报撤回功能
✓ 创建并提交新填报
✓ 撤回填报（PENDING → DRAFT）

// 5. 驳回后重新提交
✓ 模拟填报被驳回
✓ 修改被驳回的填报

// 6. 批量填报
✓ 为多个指标创建填报
✓ 批量提交填报

// 7. 查看历史填报
✓ 按周期查询填报
```

### 工作流Agent测试场景

```javascript
// 1. 查询工作流定义
✓ 获取所有工作流定义
✓ 查看工作流定义详情

// 2. 审批时间轴完整性测试
✓ 获取工作流实例
✓ 验证时间轴完整性
✓ 验证必需字段（operator, timestamp, action, stepName）

// 3. 多级审批流程测试
✓ 创建测试指标
✓ 提交多级审批
✓ 执行多级审批

// 4. 驳回回退测试
✓ 创建新指标并提交审批
✓ 在第2级驳回
✓ 验证驳回意见已记录

// 5. 审批历史记录测试
✓ 获取完整审批历史
✓ 验证历史记录字段

// 6. 数据变更记录测试
✓ 记录数据变更
✓ 验证变更记录
```

## 常见问题

### Q1: 测试失败，提示连接后端失败

**原因**: 后端服务未启动或端口配置错误

**解决方案**:
```bash
# 检查后端服务是否运行
curl http://localhost:8080/api/v1/health

# 检查配置文件中的baseUrl
cat config/test-users.json | grep baseUrl

# 如果后端在其他端口，修改配置
vim config/test-users.json
```

### Q2: 认证失败，提示用户名或密码错误

**原因**: 测试账户配置不正确

**解决方案**:
```bash
# 检查数据库中的测试账户
psql -U postgres -d sism_dev -c "SELECT username, real_name FROM sys_user;"

# 更新配置文件中的用户名和密码
vim config/test-users.json
```

### Q3: 某些测试被跳过

**原因**: 前置条件不满足（如没有已下发的指标）

**解决方案**:
```bash
# 按顺序执行完整的测试流程
./run-tests.sh --all

# 或从特定的依赖开始
./run-tests.sh --workflow indicator-distribution
./run-tests.sh --workflow indicator-split
```

### Q4: 如何只测试某个模块？

```bash
# 方法1: 使用启动脚本
./run-tests.sh --iam          # 只测试IAM
./run-tests.sh --strategy     # 只测试战略部门
./run-tests.sh --functional   # 只测试职能部门
./run-tests.sh --college      # 只测试学院
./run-tests.sh --workflow     # 只测试工作流

# 方法2: 直接运行Agent
node agents/iam-agent.js
node agents/strategy-agent.js
```

## 最佳实践

### 1. 测试数据隔离

在测试数据库中运行测试，避免污染生产数据：

```bash
# 使用测试配置文件
cp config/test-users.json.example config/test-users.json
# 修改为测试环境的配置
```

### 2. 定期清理测试数据

```bash
# 定期清理测试生成的数据
psql -U postgres -d sism_test -f database/cleanup-test-data.sql
```

### 3. 使用CI/CD集成

在GitHub Actions中集成测试：

```yaml
name: API Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Setup Node.js
        uses: actions/setup-node@v2
        with:
          node-version: '18'
      - name: Install dependencies
        run: |
          cd sism-backend/scripts/agent-testing
          npm install
      - name: Run API tests
        run: |
          cd sism-backend/scripts/agent-testing
          ./run-tests.sh --all
```

### 4. 查看详细日志

```bash
# 运行时输出详细日志
node agents/master-agent.js 2>&1 | tee test-output.log
```

### 5. 调试单个测试场景

在Agent文件中找到对应的场景，修改或添加调试信息：

```javascript
await this.runScenario('创建草稿指标', async (scenario) => {
  await this.runStep('创建单个指标', async () => {
    console.log('DEBUG: 创建指标数据:', indicatorData);
    const response = await this.apiClient.indicators.create(indicatorData);
    console.log('DEBUG: 响应:', response);
    // ...
  }, scenario);
});
```

### 6. 生成测试覆盖率报告

```bash
# 安装覆盖率工具
npm install --save-dev nyc

# 运行测试并生成覆盖率
nyc node agents/master-agent.js
nyc report --reporter=html
```

## 更多资源

- [完整README](./README.md)
- [架构设计文档](./multi-agent-test-framework.md)
- [业务流程文档](../../docs/流程.md)
- [API接口文档](../../docs/API接口文档.md)
