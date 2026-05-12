# API 压力测试方案（500 并发用户）

## 1. 测试目标

### 1.1 总体目标

- 验证系统在 500 并发用户下的稳定性和性能
- 确定系统的性能瓶颈和最大承载能力
- 建立性能基线，为后续优化提供数据支撑
- 验证关键业务场景的响应时间是否满足 SLA 要求

### 1.2 性能指标基线

| 指标 | 目标值 | 告警阈值 | 说明 |
|------|--------|----------|------|
| 平均响应时间 | < 500ms | > 1000ms | P50 |
| 95 分位响应时间 | < 1000ms | > 2000ms | P95 |
| 99 分位响应时间 | < 2000ms | > 5000ms | P99 |
| 吞吐量 (TPS) | > 200 TPS | < 100 TPS | 每秒处理请求数 |
| 错误率 | < 0.1% | > 1% | HTTP 5xx 比例 |
| CPU 使用率 | < 70% | > 85% | 应用服务器 |
| 内存使用率 | < 75% | > 85% | JVM 堆 |
| 数据库连接池使用率 | < 70% | > 85% | HikariCP |

## 2. 测试环境

### 2.1 环境要求

| 项目 | 配置 |
|------|------|
| 应用服务器 | 175.24.139.148:8386 (生产环境) 或同等配置的测试环境 |
| 应用服务器配置 | 4C8G 或以上 |
| 数据库 | PostgreSQL 12+，独立服务器 |
| 压测机 | 8C16G 或以上，与应用服务器同机房 |
| 网络 | 压测机与应用服务器之间延迟 < 1ms |

### 2.2 测试数据准备

- 用户账号：准备 50+ 个不同角色的测试账号
- 组织数据：至少 20 个组织（涵盖 admin、functional、academic 类型）
- 考核周期：至少 3 个活跃周期
- 计划数据：每个周期至少 50 个计划
- 任务数据：每个计划至少 10 个任务
- 指标数据：每个任务至少 5 个指标

## 3. 工具选择

### 3.1 推荐工具：k6

选择理由：
- 原生支持 JavaScript 编写测试脚本，学习曲线低
- 内置分布式执行能力
- 丰富的指标收集和输出格式（JSON、InfluxDB、Grafana）
- 适合 HTTP API 压测
- 社区活跃，文档完善

### 3.2 备选工具对比

| 特性 | k6 | JMeter | Gatling |
|------|-----|--------|---------|
| 脚本语言 | JavaScript | Java/XML | Scala |
| 资源消耗 | 低 | 高 | 中 |
| 分布式支持 | 原生 | 需插件 | 原生 |
| 实时监控 | Grafana | 插件 | 内置 |
| 学习曲线 | 低 | 中 | 高 |
| 协议支持 | HTTP/gRPC/WebSocket | 广泛 | HTTP/JMS |

### 3.3 安装

```bash
# macOS
brew install k6

# Linux (Debian/Ubuntu)
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
    --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D68
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
    | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install k6

# Docker
docker pull grafana/k6
```

## 4. 测试场景设计

### 4.1 用户行为模型

根据系统角色和使用频率，设计以下用户分布：

| 用户角色 | 比例 | 并发数 | 主要操作 |
|----------|------|--------|----------|
| 战略部管理 (VICE_PRESIDENT) | 5% | 25 | 全局查看、审批 |
| 战略部负责人 (STRATEGY_DEPT_HEAD) | 5% | 25 | 查看、创建、审批 |
| 战略部填报人 (REPORTER) | 15% | 75 | 创建任务、填报报告 |
| 职能部门填报人 | 20% | 100 | 查看任务、填报报告 |
| 职能部门审批人 | 10% | 50 | 审批报告 |
| 学院填报人 | 25% | 125 | 查看指标、填报进度 |
| 学院审批人 | 10% | 50 | 审批报告 |
| 只读用户 | 10% | 50 | 查看数据、导出报表 |

### 4.2 接口并发分配

#### 4.2.1 认证模块 (10% 流量, ~50 并发)

| 接口 | 方法 | 并发数 | 说明 |
|------|------|--------|------|
| `/api/v1/auth/login` | POST | 30 | 登录，获取 Token |
| `/api/v1/auth/refresh` | POST | 10 | Token 刷新 |
| `/api/v1/auth/me` | GET | 10 | 获取当前用户信息 |

#### 4.2.2 任务模块 (25% 流量, ~125 并发)

| 接口 | 方法 | 并发数 | 说明 |
|------|------|--------|------|
| `/api/v1/tasks` | POST | 15 | 创建任务 |
| `/api/v1/tasks` | GET | 20 | 获取所有任务 |
| `/api/v1/tasks/search` | GET | 40 | 搜索任务(高频) |
| `/api/v1/tasks/{id}` | GET | 25 | 获取任务详情 |
| `/api/v1/tasks/{id}/activate` | POST | 5 | 激活任务 |
| `/api/v1/tasks/{id}/complete` | POST | 5 | 完成任务 |
| `/api/v1/tasks/by-plan/{planId}` | GET | 15 | 按计划查询 |

#### 4.2.3 指标模块 (20% 流量, ~100 并发)

| 接口 | 方法 | 并发数 | 说明 |
|------|------|--------|------|
| `/api/v1/indicators` | GET | 25 | 获取指标列表 |
| `/api/v1/indicators/search` | GET | 35 | 搜索指标(高频) |
| `/api/v1/indicators/{id}` | GET | 20 | 获取指标详情 |
| `/api/v1/indicators/{id}/submit` | POST | 10 | 提交指标 |
| `/api/v1/indicators/{id}/distribute` | POST | 10 | 下发指标 |

#### 4.2.4 计划模块 (15% 流量, ~75 并发)

| 接口 | 方法 | 并发数 | 说明 |
|------|------|--------|------|
| `/api/v1/plans` | GET | 20 | 获取计划列表 |
| `/api/v1/plans/{id}` | GET | 20 | 获取计划详情 |
| `/api/v1/plans/cycle/{cycleId}` | GET | 15 | 按周期查询 |
| `/api/v1/plans/{id}/submit` | POST | 10 | 提交计划 |
| `/api/v1/plans/{id}/approve` | POST | 10 | 审批计划 |

#### 4.2.5 报告模块 (15% 流量, ~75 并发)

| 接口 | 方法 | 并发数 | 说明 |
|------|------|--------|------|
| `/api/v1/reports` | GET | 20 | 获取报告列表 |
| `/api/v1/reports/search` | GET | 25 | 搜索报告 |
| `/api/v1/reports/{id}` | GET | 15 | 获取报告详情 |
| `/api/v1/reports/{id}/submit` | POST | 8 | 提交报告 |
| `/api/v1/reports/{id}/approve` | POST | 7 | 审批报告 |

#### 4.2.6 其他模块 (15% 流量, ~75 并发)

| 接口 | 方法 | 并发数 | 说明 |
|------|------|--------|------|
| `/api/v1/organizations` | GET | 15 | 组织列表 |
| `/api/v1/cycles` | GET | 10 | 周期列表 |
| `/api/v1/dashboard` | GET | 20 | 仪表盘数据 |
| `/api/v1/notifications/my` | GET | 15 | 我的通知 |
| `/api/v1/workflows/my-tasks` | GET | 15 | 我的审批任务 |

## 5. 测试脚本示例

### 5.1 项目结构

```
stress-test/
├── config/
│   └── test-config.js          # 测试配置（URL、账号、阈值）
├── scenarios/
│   ├── auth-scenario.js        # 认证场景
│   ├── task-scenario.js        # 任务场景
│   ├── indicator-scenario.js   # 指标场景
│   ├── plan-scenario.js        # 计划场景
│   ├── report-scenario.js      # 报告场景
│   └── browse-scenario.js      # 浏览场景
├── lib/
│   ├── auth-helper.js          # 认证辅助函数
│   └── data-helper.js          # 测试数据辅助
├── main-test.js                # 主测试入口
└── run.sh                      # 执行脚本
```

### 5.2 主测试脚本 (main-test.js)

```javascript
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// 自定义指标
const errorRate = new Rate('errors');
const taskCreateDuration = new Trend('task_create_duration');
const searchDuration = new Trend('search_duration');

// 测试配置
export const options = {
  scenarios: {
    // 认证场景 - 恒定速率
    auth_scenario: {
      executor: 'constant-arrival-rate',
      rate: 30,
      timeUnit: '1s',
      duration: '5m',
      preAllocatedVUs: 20,
      maxVUs: 50,
      exec: 'authScenario',
    },
    // 任务场景 - 分阶段递增
    task_scenario: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 40 },   // 升温
        { duration: '3m', target: 125 },  // 达到目标
        { duration: '5m', target: 125 },  // 持续负载
        { duration: '1m', target: 0 },    // 冷却
      ],
      exec: 'taskScenario',
    },
    // 指标场景 - 分阶段递增
    indicator_scenario: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 30 },
        { duration: '3m', target: 100 },
        { duration: '5m', target: 100 },
        { duration: '1m', target: 0 },
      ],
      exec: 'indicatorScenario',
    },
    // 计划场景
    plan_scenario: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 25 },
        { duration: '3m', target: 75 },
        { duration: '5m', target: 75 },
        { duration: '1m', target: 0 },
      ],
      exec: 'planScenario',
    },
    // 报告场景
    report_scenario: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 25 },
        { duration: '3m', target: 75 },
        { duration: '5m', target: 75 },
        { duration: '1m', target: 0 },
      ],
      exec: 'reportScenario',
    },
    // 浏览场景 - 恒定并发
    browse_scenario: {
      executor: 'constant-vus',
      vus: 50,
      duration: '10m',
      exec: 'browseScenario',
    },
  },

  thresholds: {
    http_req_duration: ['p(95)<1000', 'p(99)<2000'],
    http_req_failed: ['rate<0.01'],
    errors: ['rate<0.01'],
    task_create_duration: ['p(95)<800'],
    search_duration: ['p(95)<600'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 测试账号池
const TEST_ACCOUNTS = [
  { username: 'admin', password: 'admin123', role: 'VICE_PRESIDENT' },
  { username: 'zlb_admin', password: 'admin123', role: 'STRATEGY_DEPT_HEAD' },
  { username: 'zlb_final1', password: 'admin123', role: 'REPORTER' },
  { username: 'jiaowu_report', password: 'admin123', role: 'FUNC_REPORTER' },
  { username: 'jiaowu_audit1', password: 'admin123', role: 'FUNC_APPROVER' },
  { username: 'jisuanji_report', password: 'admin123', role: 'COLLEGE_REPORTER' },
  { username: 'jisuanji_audit1', password: 'admin123', role: 'COLLEGE_APPROVER' },
];

function getToken(account) {
  const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ username: account.username, password: account.password }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(loginRes, {
    'login successful': (r) => r.status === 200,
  });

  if (loginRes.status === 200) {
    const body = JSON.parse(loginRes.body);
    return body.data?.token || body.token;
  }
  return null;
}

function authHeaders(token) {
  return {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  };
}

// ==================== 场景实现 ====================

export function authScenario() {
  const account = TEST_ACCOUNTS[__VU % TEST_ACCOUNTS.length];

  group('Auth Flow', () => {
    // 登录
    const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`,
      JSON.stringify({ username: account.username, password: account.password }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    errorRate.add(loginRes.status !== 200);
    check(loginRes, { 'auth login 200': (r) => r.status === 200 });

    if (loginRes.status === 200) {
      const body = JSON.parse(loginRes.body);
      const token = body.data?.token || body.token;

      // 获取当前用户
      const meRes = http.get(`${BASE_URL}/api/v1/auth/me`, authHeaders(token));
      errorRate.add(meRes.status !== 200);
      check(meRes, { 'auth me 200': (r) => r.status === 200 });

      sleep(1);

      // 刷新 Token
      const refreshRes = http.post(`${BASE_URL}/api/v1/auth/refresh`,
        JSON.stringify({ token: token }),
        authHeaders(token)
      );
      errorRate.add(refreshRes.status !== 200);
    }
  });

  sleep(Math.random() * 2 + 1);
}

export function taskScenario() {
  const account = TEST_ACCOUNTS[__VU % TEST_ACCOUNTS.length];
  const token = getToken(account);
  if (!token) return;

  const headers = authHeaders(token);

  group('Task Operations', () => {
    // 搜索任务 (高频操作)
    const searchStart = Date.now();
    const searchRes = http.get(`${BASE_URL}/api/v1/tasks/search?page=0&size=10`, headers);
    searchDuration.add(Date.now() - searchStart);
    errorRate.add(searchRes.status !== 200);
    check(searchRes, { 'task search 200': (r) => r.status === 200 });

    sleep(0.5);

    // 获取所有任务
    const listRes = http.get(`${BASE_URL}/api/v1/tasks`, headers);
    errorRate.add(listRes.status !== 200);
    check(listRes, { 'task list 200': (r) => r.status === 200 });

    sleep(0.5);

    // 如果是 REPORTER 角色，创建任务
    if (account.role === 'STRATEGY_DEPT_HEAD' || account.role === 'REPORTER') {
      const createStart = Date.now();
      const createRes = http.post(`${BASE_URL}/api/v1/tasks`,
        JSON.stringify({
          name: `压力测试任务-${Date.now()}-${__VU}`,
          taskType: 'BASIC',
          planId: 1,
          cycleId: 1,
          orgId: 36,
          createdByOrgId: 35,
          desc: '压力测试自动创建的任务',
        }),
        headers
      );
      taskCreateDuration.add(Date.now() - createStart);
      errorRate.add(createRes.status !== 200);
      check(createRes, { 'task create 200': (r) => r.status === 200 });

      // 如果创建成功，获取详情
      if (createRes.status === 200) {
        const body = JSON.parse(createRes.body);
        const taskId = body.data?.id;
        if (taskId) {
          sleep(0.3);
          const detailRes = http.get(`${BASE_URL}/api/v1/tasks/${taskId}`, headers);
          errorRate.add(detailRes.status !== 200);
          check(detailRes, { 'task detail 200': (r) => r.status === 200 });
        }
      }
    }

    sleep(Math.random() * 2 + 1);
  });
}

export function indicatorScenario() {
  const account = TEST_ACCOUNTS[__VU % TEST_ACCOUNTS.length];
  const token = getToken(account);
  if (!token) return;

  const headers = authHeaders(token);

  group('Indicator Operations', () => {
    // 搜索指标
    const searchRes = http.get(`${BASE_URL}/api/v1/indicators/search?page=0&size=10`, headers);
    errorRate.add(searchRes.status !== 200);
    check(searchRes, { 'indicator search 200': (r) => r.status === 200 });

    sleep(0.5);

    // 获取指标列表
    const listRes = http.get(`${BASE_URL}/api/v1/indicators`, headers);
    errorRate.add(listRes.status !== 200);
    check(listRes, { 'indicator list 200': (r) => r.status === 200 });

    sleep(Math.random() * 2 + 1);
  });
}

export function planScenario() {
  const account = TEST_ACCOUNTS[__VU % TEST_ACCOUNTS.length];
  const token = getToken(account);
  if (!token) return;

  const headers = authHeaders(token);

  group('Plan Operations', () => {
    // 获取计划列表
    const listRes = http.get(`${BASE_URL}/api/v1/plans`, headers);
    errorRate.add(listRes.status !== 200);
    check(listRes, { 'plan list 200': (r) => r.status === 200 });

    sleep(0.5);

    // 按周期查询计划
    const cycleRes = http.get(`${BASE_URL}/api/v1/plans/cycle/1`, headers);
    errorRate.add(cycleRes.status !== 200);
    check(cycleRes, { 'plan by cycle 200': (r) => r.status === 200 });

    sleep(Math.random() * 2 + 1);
  });
}

export function reportScenario() {
  const account = TEST_ACCOUNTS[__VU % TEST_ACCOUNTS.length];
  const token = getToken(account);
  if (!token) return;

  const headers = authHeaders(token);

  group('Report Operations', () => {
    // 搜索报告
    const searchRes = http.get(`${BASE_URL}/api/v1/reports/search?page=0&size=10`, headers);
    errorRate.add(searchRes.status !== 200);
    check(searchRes, { 'report search 200': (r) => r.status === 200 });

    sleep(0.5);

    // 获取待审批报告
    const pendingRes = http.get(`${BASE_URL}/api/v1/reports/pending`, headers);
    errorRate.add(pendingRes.status !== 200);
    check(pendingRes, { 'report pending 200': (r) => r.status === 200 });

    sleep(Math.random() * 2 + 1);
  });
}

export function browseScenario() {
  const account = TEST_ACCOUNTS[__VU % TEST_ACCOUNTS.length];
  const token = getToken(account);
  if (!token) return;

  const headers = authHeaders(token);

  group('Browse Operations', () => {
    // 仪表盘
    const dashRes = http.get(`${BASE_URL}/api/v1/dashboard`, headers);
    errorRate.add(dashRes.status !== 200);
    check(dashRes, { 'dashboard 200': (r) => r.status === 200 });

    sleep(1);

    // 组织列表
    const orgRes = http.get(`${BASE_URL}/api/v1/organizations`, headers);
    errorRate.add(orgRes.status !== 200);
    check(orgRes, { 'organizations 200': (r) => r.status === 200 });

    sleep(1);

    // 周期列表
    const cycleRes = http.get(`${BASE_URL}/api/v1/cycles`, headers);
    errorRate.add(cycleRes.status !== 200);
    check(cycleRes, { 'cycles 200': (r) => r.status === 200 });

    sleep(1);

    // 我的通知
    const notifRes = http.get(`${BASE_URL}/api/v1/notifications/my`, headers);
    errorRate.add(notifRes.status !== 200);
    check(notifRes, { 'notifications 200': (r) => r.status === 200 });

    sleep(Math.random() * 3 + 2);
  });
}
```

### 5.3 执行脚本 (run.sh)

```bash
#!/bin/bash
set -euo pipefail

# 配置
BASE_URL="${BASE_URL:-http://localhost:8080}"
RESULTS_DIR="./results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

mkdir -p "${RESULTS_DIR}"

echo "============================================"
echo "SISM API Stress Test"
echo "Target: ${BASE_URL}"
echo "Time: ${TIMESTAMP}"
echo "============================================"

# 运行测试
k6 run \
  --env BASE_URL="${BASE_URL}" \
  --out json="${RESULTS_DIR}/raw-${TIMESTAMP}.json" \
  --summary-export="${RESULTS_DIR}/summary-${TIMESTAMP}.json" \
  main-test.js 2>&1 | tee "${RESULTS_DIR}/output-${TIMESTAMP}.log"

echo ""
echo "Test completed. Results saved to ${RESULTS_DIR}/"
echo "  - Raw data: raw-${TIMESTAMP}.json"
echo "  - Summary:  summary-${TIMESTAMP}.json"
echo "  - Log:      output-${TIMESTAMP}.log"
```

### 5.4 InfluxDB + Grafana 实时监控（可选）

```bash
# 启动 InfluxDB + Grafana
docker-compose up -d influxdb grafana

# 运行测试并输出到 InfluxDB
k6 run \
  --out influxdb=http://localhost:8086/k6 \
  main-test.js
```

```yaml
# docker-compose.yml (监控栈)
version: '3.8'
services:
  influxdb:
    image: influxdb:1.8
    ports:
      - "8086:8086"
    environment:
      - INFLUXDB_DB=k6

  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"
    environment:
      - GF_AUTH_ANONYMOUS_ENABLED=true
      - GF_AUTH_ANONYMOUS_ORG_ROLE=Admin
```

## 6. 测试执行计划

### 6.1 测试阶段

| 阶段 | 时长 | 并发数 | 目的 |
|------|------|--------|------|
| 预热 | 2 分钟 | 10-50 | JIT 编译、连接池初始化 |
| 升温 | 1 分钟 | 50-200 | 逐步增加负载 |
| 目标负载 | 5 分钟 | 200-500 | 达到目标并发数 |
| 持续负载 | 5 分钟 | 500 | 稳定运行，观察指标 |
| 峰值测试 | 2 分钟 | 500-800 | 超载测试，验证弹性 |
| 冷却 | 1 分钟 | 500-0 | 观察系统恢复 |

### 6.2 执行顺序

```
1. 环境检查      -> 确认服务器、数据库、网络正常
2. 数据准备      -> 生成测试数据
3. 冒烟测试      -> 10 并发，验证脚本正确性
4. 基准测试      -> 50 并发，建立基线
5. 标准压力测试  -> 500 并发，核心测试
6. 峰值测试      -> 800 并发，极限测试
7. 稳定性测试    -> 300 并发，持续 30 分钟
8. 结果分析      -> 生成报告
```

## 7. 监控要点

### 7.1 应用层监控

| 监控项 | 数据来源 | 关注指标 |
|--------|----------|----------|
| JVM 堆内存 | Actuator `/actuator/metrics/jvm.memory.used` | 使用率、GC 频率 |
| 线程状态 | Actuator `/actuator/metrics/jvm.threads.live` | 线程数、死锁 |
| HTTP 连接 | Actuator `/actuator/metrics/http.server.requests` | 活跃连接、请求队列 |
| 数据库连接池 | HikariCP metrics | 活跃连接、等待线程、超时 |
| 日志 | 应用日志 | ERROR 日志频率 |

### 7.2 系统层监控

| 监控项 | 工具 | 关注指标 |
|--------|------|----------|
| CPU | top / htop / Prometheus node_exporter | 使用率、负载 |
| 内存 | free / htop | 使用率、swap |
| 磁盘 I/O | iostat | 读写速率、await |
| 网络 | iftop / nethogs | 带宽、连接数 |
| PostgreSQL | pg_stat_activity | 活跃查询、锁等待 |

### 7.3 数据库监控 SQL

```sql
-- 当前活跃连接
SELECT count(*) FROM pg_stat_activity WHERE state = 'active';

-- 长时间运行的查询
SELECT pid, now() - pg_stat_activity.query_start AS duration, query
FROM pg_stat_activity
WHERE (now() - pg_stat_activity.query_start) > interval '5 seconds'
  AND state = 'active';

-- 锁等待
SELECT blocked_locks.pid AS blocked_pid,
       blocked_activity.usename AS blocked_user,
       blocking_locks.pid AS blocking_pid,
       blocking_activity.usename AS blocking_user,
       blocked_activity.query AS blocked_statement
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks
    ON blocking_locks.locktype = blocked_locks.locktype
    AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
    AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
    AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
    AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
    AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
    AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
    AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
    AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
    AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
    AND blocking_locks.pid != blocked_locks.pid
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;

-- 表统计（检查慢表）
SELECT relname, seq_scan, seq_tup_read, idx_scan, idx_tup_fetch,
       n_tup_ins, n_tup_upd, n_tup_del
FROM pg_stat_user_tables
ORDER BY seq_tup_read DESC LIMIT 10;
```

## 8. 结果分析与报告模板

### 8.1 测试报告结构

```
1. 测试概述
   - 测试时间、环境、配置
   - 测试场景和并发数

2. 性能指标汇总
   - 响应时间分布 (P50/P95/P99)
   - 吞吐量 (TPS)
   - 错误率
   - 并发用户数

3. 各接口性能明细
   - 每个接口的响应时间、吞吐量、错误率
   - 性能最差的 Top 5 接口

4. 资源使用情况
   - CPU / 内存 / 磁盘 IO 使用率
   - JVM 堆内存和 GC 情况
   - 数据库连接池使用率

5. 瓶颈分析
   - 识别性能瓶颈（CPU / 内存 / IO / 数据库 / 应用逻辑）
   - 根因分析

6. 优化建议
   - 短期优化（配置调整）
   - 中期优化（代码优化）
   - 长期优化（架构改进）

7. 附录
   - 详细测试数据
   - 监控截图
```

### 8.2 k6 结果解读

k6 输出的关键指标：

```
http_req_duration        - HTTP 请求响应时间
  ├── avg                - 平均值
  ├── min                - 最小值
  ├── max                - 最大值
  ├── med                - 中位数 (P50)
  ├── p(90)              - 90 分位
  ├── p(95)              - 95 分位
  └── p(99)              - 99 分位

http_reqs                - 总请求数
http_req_failed          - 失败请求率
http_req_connecting      - TCP 连接建立时间
http_req_tls_handshaking - TLS 握手时间
http_req_waiting        - 服务端处理时间 (TTFB)
iterations               - 场景迭代次数
vus                      - 当前虚拟用户数
vus_max                  - 最大虚拟用户数
data_received            - 接收数据量
data_sent                - 发送数据量
```

## 9. 风险与注意事项

### 9.1 测试风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 压测影响生产数据 | 高 | 使用独立测试环境，或在测试数据库上运行 |
| 压测机性能不足 | 中 | 压测机配置不低于 8C16G，监控压测机资源 |
| 网络瓶颈 | 中 | 压测机与服务器同机房，避免跨网络 |
| 测试数据不够 | 中 | 提前准备充足测试数据 |
| Token 过期 | 低 | 实现 Token 自动刷新逻辑 |

### 9.2 注意事项

1. **提前通知**：如果在生产环境压测，提前通知运维团队
2. **数据隔离**：测试数据使用特定前缀（如 `压力测试-`），便于清理
3. **逐步加压**：不要直接从 0 跳到 500 并发，分阶段递增
4. **监控同步**：压测期间必须同步监控服务器资源
5. **多次执行**：每轮测试至少执行 3 次，取平均值
6. **环境一致**：测试环境配置应与生产环境一致
