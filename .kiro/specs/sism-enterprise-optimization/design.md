# SISM 企业级优化方案 - 设计文档

## 概述

本设计文档描述了战略指标管理系统 (SISM) 企业级优化的技术实现方案。优化分为四个阶段：安全加固、可靠性增强、可维护性提升和性能优化。

### 设计原则

1. **最小侵入性**: 尽量复用现有代码结构，避免大规模重构
2. **渐进式改进**: 按优先级分阶段实施，每阶段可独立部署
3. **向后兼容**: 新功能不影响现有 API 和用户体验
4. **可测试性**: 所有核心功能都有对应的属性测试

### 技术栈

- **前端**: Vue 3 + TypeScript + Vite + Pinia
- **后端**: Spring Boot 3 + Java 17 + Spring Security
- **数据库**: PostgreSQL 15+
- **测试**: Vitest + fast-check (前端), JUnit + jqwik (后端)

---

## 架构

### 整体架构变更

```
┌─────────────────────────────────────────────────────────────────┐
│                         前端 (Vue 3)                             │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ Logger      │  │ TokenManager│  │ RetryInterceptor        │  │
│  │ (日志工具)   │  │ (Token管理) │  │ (重试拦截器)             │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ ErrorHandler│  │ IdempotencyKey│ │ PerformanceMonitor     │  │
│  │ (错误处理)   │  │ (幂等性Key)  │  │ (性能监控)              │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        后端 (Spring Boot)                        │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ EnvConfig   │  │ RefreshToken│  │ RateLimiter             │  │
│  │ (环境配置)   │  │ (刷新Token) │  │ (频率限制)              │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ Idempotency │  │ ApiVersion  │  │ CacheManager            │  │
│  │ (幂等性检查) │  │ (API版本)   │  │ (缓存管理)              │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 文件结构变更

```
strategic-task-management/src/
├── utils/
│   ├── logger.ts           # [新增] 统一日志工具
│   ├── tokenManager.ts     # [新增] Token 管理器
│   ├── idempotency.ts      # [新增] 幂等性 Key 生成
│   ├── performance.ts      # [新增] 性能监控
│   └── security.ts         # [修改] 增强安全工具
├── api/
│   ├── index.ts            # [修改] 集成重试和错误处理
│   ├── retry.ts            # [新增] 重试拦截器
│   └── errorHandler.ts     # [新增] 统一错误处理
└── stores/
    └── auth.ts             # [修改] 集成 TokenManager

sism-backend/src/main/java/com/sism/
├── config/
│   ├── EnvConfigValidator.java    # [新增] 环境变量验证
│   ├── RateLimitConfig.java       # [新增] 频率限制配置
│   └── CacheConfig.java           # [新增] 缓存配置
├── filter/
│   ├── IdempotencyFilter.java     # [新增] 幂等性过滤器
│   └── RateLimitFilter.java       # [新增] 频率限制过滤器
├── service/
│   ├── RefreshTokenService.java   # [新增] 刷新 Token 服务
│   └── IdempotencyService.java    # [新增] 幂等性服务
└── controller/
    └── AuthController.java        # [修改] 增加刷新 Token 端点
```

---

## 组件和接口

### 第一轮：安全加固

#### 1.1 环境变量配置验证器 (EnvConfigValidator)

**职责**: 启动时验证必要环境变量，缺失时阻止启动并给出明确提示。

```java
@Component
public class EnvConfigValidator implements ApplicationRunner {
    
    private static final List<String> REQUIRED_VARS = List.of(
        "JWT_SECRET",
        "DB_URL", 
        "DB_USERNAME",
        "DB_PASSWORD"
    );
    
    @Override
    public void run(ApplicationArguments args) {
        List<String> missing = REQUIRED_VARS.stream()
            .filter(var -> System.getenv(var) == null)
            .toList();
        
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                "Missing required environment variables: " + missing
            );
        }
    }
}
```

#### 1.2 Token 管理器 (TokenManager)

**职责**: 管理 Access Token 和 Refresh Token 的存储、刷新和清除。

```typescript
interface TokenManager {
  // Access Token 存储在内存中
  getAccessToken(): string | null;
  setAccessToken(token: string): void;
  clearAccessToken(): void;
  
  // Refresh Token 通过 HttpOnly Cookie 管理
  refreshAccessToken(): Promise<string>;
  
  // 检查 Token 是否即将过期
  isTokenExpiring(thresholdMs: number): boolean;
}
```

**实现要点**:
- Access Token 存储在闭包变量中，不暴露到全局
- Refresh Token 由后端设置为 HttpOnly Cookie
- 自动在 Token 过期前 5 分钟刷新

#### 1.3 统一日志工具 (Logger)

**职责**: 提供分级日志输出，生产环境自动禁用调试日志。

```typescript
type LogLevel = 'debug' | 'info' | 'warn' | 'error';

interface Logger {
  debug(message: string, ...args: unknown[]): void;
  info(message: string, ...args: unknown[]): void;
  warn(message: string, ...args: unknown[]): void;
  error(message: string, ...args: unknown[]): void;
  
  // 配置
  setLevel(level: LogLevel): void;
  getLevel(): LogLevel;
}

// 敏感数据过滤
const SENSITIVE_PATTERNS = [
  /token/i, /password/i, /secret/i, /key/i
];
```

### 第二轮：可靠性增强

#### 2.1 重试拦截器 (RetryInterceptor)

**职责**: 为 GET 请求提供自动重试，使用指数退避策略。

```typescript
interface RetryConfig {
  maxRetries: number;        // 最大重试次数，默认 3
  baseDelay: number;         // 基础延迟，默认 1000ms
  maxDelay: number;          // 最大延迟，默认 10000ms
  retryCondition: (error: AxiosError) => boolean;
}

// 默认重试条件
const defaultRetryCondition = (error: AxiosError): boolean => {
  // 网络错误或 5xx 错误
  return !error.response || (error.response.status >= 500);
};
```

#### 2.2 幂等性服务 (IdempotencyService)

**职责**: 防止重复请求产生重复数据。

```java
public interface IdempotencyService {
    /**
     * 检查请求是否重复
     * @param idempotencyKey 幂等性 Key
     * @return 如果是重复请求，返回之前的响应；否则返回 null
     */
    Optional<String> checkDuplicate(String idempotencyKey);
    
    /**
     * 保存请求结果
     * @param idempotencyKey 幂等性 Key
     * @param response 响应内容
     * @param ttlSeconds 过期时间（秒）
     */
    void saveResult(String idempotencyKey, String response, int ttlSeconds);
}
```

**前端幂等性 Key 生成**:

```typescript
function generateIdempotencyKey(
  method: string, 
  url: string, 
  data: unknown
): string {
  const payload = JSON.stringify({ method, url, data });
  return crypto.subtle.digest('SHA-256', new TextEncoder().encode(payload))
    .then(hash => Array.from(new Uint8Array(hash))
      .map(b => b.toString(16).padStart(2, '0'))
      .join(''));
}
```

#### 2.3 分布式频率限制 (RateLimiter)

**职责**: 保护 API 免受恶意请求，支持内存和 Redis 两种存储。

```java
public interface RateLimiter {
    /**
     * 检查是否允许请求
     * @param key 限制 Key（如 IP 或用户 ID）
     * @param limit 限制次数
     * @param windowSeconds 时间窗口（秒）
     * @return 是否允许
     */
    boolean isAllowed(String key, int limit, int windowSeconds);
    
    /**
     * 获取剩余配额
     */
    int getRemainingQuota(String key, int limit, int windowSeconds);
    
    /**
     * 获取重置时间（秒）
     */
    long getResetTime(String key, int windowSeconds);
}
```

### 第三轮：可维护性提升

#### 3.1 统一错误格式

**错误响应结构**:

```typescript
interface ApiErrorResponse {
  code: string;           // 错误码，如 "AUTH_001"
  message: string;        // 用户友好的错误消息
  details?: unknown;      // 详细错误信息（开发环境）
  requestId: string;      // 请求 ID，用于日志关联
  timestamp: string;      // ISO 8601 格式时间戳
}
```

**错误码规范**:

| 前缀 | 模块 | 示例 |
|------|------|------|
| AUTH | 认证授权 | AUTH_001: Token 过期 |
| VAL | 数据验证 | VAL_001: 必填字段缺失 |
| BIZ | 业务逻辑 | BIZ_001: 指标已存在 |
| SYS | 系统错误 | SYS_001: 数据库连接失败 |

#### 3.2 API 版本管理

**URL 路径版本**:

```
/api/v1/indicators
/api/v2/indicators  (未来版本)
```

**后端实现**:

```java
@RestController
@RequestMapping("/api/v1/indicators")
public class IndicatorControllerV1 { ... }

@RestController
@RequestMapping("/api/v2/indicators")
@Deprecated
public class IndicatorControllerV2 { ... }
```

### 第四轮：性能优化

#### 4.1 前端性能监控 (PerformanceMonitor)

**收集指标**:

```typescript
interface PerformanceMetrics {
  // Core Web Vitals
  lcp: number;    // Largest Contentful Paint
  fid: number;    // First Input Delay
  cls: number;    // Cumulative Layout Shift
  
  // 自定义指标
  apiLatency: Map<string, number[]>;  // API 请求耗时
  pageLoadTime: number;               // 页面加载时间
  resourceLoadTime: number;           // 资源加载时间
}
```

#### 4.2 API 响应缓存

**缓存策略**:

| 接口 | 缓存时间 | 验证方式 |
|------|----------|----------|
| /api/v1/orgs/tree | 5 分钟 | ETag |
| /api/v1/indicators | 条件缓存 | Last-Modified |
| /api/v1/users/me | 不缓存 | - |

---

## 数据模型

### Refresh Token 存储

```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP,
    device_info VARCHAR(255),
    ip_address VARCHAR(45)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

### 幂等性记录存储

```sql
CREATE TABLE idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    response_body TEXT,
    status_code INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_idempotency_expires_at ON idempotency_records(expires_at);
```

### 频率限制记录 (Redis)

```
Key: rate_limit:{type}:{identifier}
Value: { count: number, resetAt: timestamp }
TTL: windowSeconds
```

---


## 正确性属性

基于需求分析，以下是需要通过属性测试验证的核心正确性属性：

### P1: 环境变量配置完整性

**属性**: 对于任意必需环境变量的子集缺失，系统应拒绝启动并报告所有缺失变量。

```
∀ missingVars ⊆ REQUIRED_VARS, |missingVars| > 0:
  startup(env - missingVars) → Error(contains(missingVars))
```

**验证**: Requirements 1.1.1, 1.1.2, 1.1.4

**测试框架**: jqwik (后端)

### P2: Token 内存存储不泄露

**属性**: 在任何操作序列后，localStorage 中不应存在 access_token 键。

```
∀ operations ∈ [login, logout, refresh, navigate]*:
  execute(operations) → localStorage.getItem('access_token') === null
```

**验证**: Requirements 1.2.1, 1.2.5

**测试框架**: fast-check (前端)

### P3: 日志敏感数据过滤

**属性**: 对于任意包含敏感字段的对象，日志输出不应包含原始敏感值。

```
∀ obj with sensitiveFields, ∀ logLevel:
  log(logLevel, obj) → output ∩ sensitiveValues(obj) = ∅
```

**验证**: Requirements 1.3.4

**测试框架**: fast-check (前端), jqwik (后端)

### P4: 日志级别控制

**属性**: 当日志级别设置为 L 时，只有级别 ≥ L 的日志才会输出。

```
∀ configLevel ∈ LogLevels, ∀ logLevel ∈ LogLevels:
  setLevel(configLevel) → log(logLevel, msg) outputs iff logLevel ≥ configLevel
```

**验证**: Requirements 1.3.1, 1.3.2, 1.3.3

**测试框架**: fast-check (前端)

### P5: 重试策略正确性

**属性**: GET 请求在可重试错误时最多重试 maxRetries 次，延迟遵循指数退避。

```
∀ error ∈ RetryableErrors, ∀ attempt ∈ [1..maxRetries]:
  delay(attempt) = min(baseDelay * 2^(attempt-1), maxDelay)
```

**验证**: Requirements 2.1.1, 2.1.2

**测试框架**: fast-check (前端)

### P6: 非幂等请求不重试

**属性**: POST/PUT/DELETE/PATCH 请求在任何错误时都不自动重试。

```
∀ method ∈ {POST, PUT, DELETE, PATCH}, ∀ error:
  request(method, url, error) → retryCount = 0
```

**验证**: Requirements 2.1.3

**测试框架**: fast-check (前端)

### P7: 幂等性 Key 唯一性

**属性**: 不同的请求参数组合生成不同的幂等性 Key。

```
∀ (method1, url1, data1) ≠ (method2, url2, data2):
  generateKey(method1, url1, data1) ≠ generateKey(method2, url2, data2)
```

**验证**: Requirements 2.2.1

**测试框架**: fast-check (前端)

### P8: 幂等性时间窗口

**属性**: 在 TTL 时间内，相同 Key 的请求返回缓存结果；超过 TTL 后，请求正常处理。

```
∀ key, ∀ t1, t2 where t2 - t1 < TTL:
  request(key, t1) = response → request(key, t2) = response

∀ key, ∀ t1, t2 where t2 - t1 ≥ TTL:
  request(key, t1) = response1 → request(key, t2) may ≠ response1
```

**验证**: Requirements 2.2.2, 2.2.4

**测试框架**: jqwik (后端)

### P9: 频率限制正确性

**属性**: 在时间窗口内，请求次数超过限制时返回 429。

```
∀ key, ∀ requests[1..n] in window:
  n ≤ limit → all requests succeed
  n > limit → requests[limit+1..n] return 429
```

**验证**: Requirements 2.3.4, 2.3.5

**测试框架**: jqwik (后端)

### P10: 错误响应格式一致性

**属性**: 所有错误响应都包含必需字段且格式正确。

```
∀ error ∈ AllErrors:
  response(error) has { code: string, message: string, requestId: string, timestamp: ISO8601 }
```

**验证**: Requirements 3.1.1, 3.1.5

**测试框架**: fast-check (前端), jqwik (后端)

---

## 测试策略

### 属性测试覆盖

| 属性 | 前端测试 | 后端测试 | 优先级 |
|------|----------|----------|--------|
| P1 | - | ✓ | P0 |
| P2 | ✓ | - | P1 |
| P3 | ✓ | ✓ | P0 |
| P4 | ✓ | - | P1 |
| P5 | ✓ | - | P1 |
| P6 | ✓ | - | P1 |
| P7 | ✓ | - | P2 |
| P8 | - | ✓ | P2 |
| P9 | - | ✓ | P2 |
| P10 | ✓ | ✓ | P1 |

### 测试文件位置

**前端**:
- `strategic-task-management/tests/property/logger.property.test.ts`
- `strategic-task-management/tests/property/token-manager.property.test.ts`
- `strategic-task-management/tests/property/retry.property.test.ts`
- `strategic-task-management/tests/property/idempotency.property.test.ts`
- `strategic-task-management/tests/property/error-handler.property.test.ts`

**后端**:
- `sism-backend/src/test/java/com/sism/property/EnvConfigPropertyTest.java`
- `sism-backend/src/test/java/com/sism/property/IdempotencyPropertyTest.java`
- `sism-backend/src/test/java/com/sism/property/RateLimitPropertyTest.java`
- `sism-backend/src/test/java/com/sism/property/ErrorResponsePropertyTest.java`

---

## 安全考虑

### 威胁模型

1. **XSS 攻击**: 通过 Token 内存存储和 HttpOnly Cookie 缓解
2. **CSRF 攻击**: 通过 SameSite Cookie 和签名验证缓解
3. **暴力破解**: 通过频率限制和账户锁定缓解
4. **重放攻击**: 通过幂等性检查和时间戳验证缓解

### 安全配置清单

- [ ] JWT 密钥长度 ≥ 256 位
- [ ] Refresh Token 有效期 ≤ 7 天
- [ ] Access Token 有效期 ≤ 15 分钟
- [ ] 登录频率限制 ≤ 5 次/分钟
- [ ] 敏感操作需要签名验证
- [ ] 所有 Cookie 设置 Secure 和 SameSite 属性

---

## 部署考虑

### 环境变量清单

| 变量名 | 必需 | 描述 | 示例 |
|--------|------|------|------|
| JWT_SECRET | ✓ | JWT 签名密钥 | 64 字符随机字符串 |
| JWT_EXPIRATION | - | Access Token 有效期(ms) | 900000 (15分钟) |
| JWT_REFRESH_EXPIRATION | - | Refresh Token 有效期(ms) | 604800000 (7天) |
| DB_URL | ✓ | 数据库连接 URL | jdbc:postgresql://... |
| DB_USERNAME | ✓ | 数据库用户名 | sism_user |
| DB_PASSWORD | ✓ | 数据库密码 | *** |
| REDIS_URL | - | Redis 连接 URL | redis://localhost:6379 |
| LOG_LEVEL | - | 日志级别 | INFO |
| RATE_LIMIT_ENABLED | - | 是否启用频率限制 | true |

### 回滚策略

每个优化阶段都可以独立回滚：

1. **安全加固回滚**: 恢复原有 Token 存储方式，移除环境变量检查
2. **可靠性增强回滚**: 禁用重试和幂等性检查
3. **可维护性提升回滚**: 恢复原有错误格式
4. **性能优化回滚**: 禁用缓存和性能监控

---

## 里程碑

| 阶段 | 内容 | 预计时间 | 依赖 |
|------|------|----------|------|
| M1 | 安全加固 | 2 天 | - |
| M2 | 可靠性增强 | 3 天 | M1 |
| M3 | 可维护性提升 | 2 天 | M1 |
| M4 | 性能优化 | 2 天 | M1, M2 |
