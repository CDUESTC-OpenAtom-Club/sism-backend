# SISM 企业级优化方案 - 任务列表

## 第一轮：安全加固 (M1)

### 1. 敏感配置外部化
- [x] 1.1 创建后端环境变量验证器 `EnvConfigValidator.java`
  - [x] 1.1.1 定义必需环境变量列表 (JWT_SECRET, DB_URL, DB_USERNAME, DB_PASSWORD)
  - [x] 1.1.2 实现 ApplicationRunner 接口，启动时验证
  - [x] 1.1.3 缺失变量时抛出明确异常并列出所有缺失项
  - [x] 1.1.4 编写属性测试 `EnvConfigPropertyTest.java` 验证 P1

- [x] 1.2 更新后端配置文件
  - [x] 1.2.1 修改 `application.yml` 移除所有硬编码默认值
  - [x] 1.2.2 修改 `application-dev.yml` 使用开发环境变量
  - [x] 1.2.3 修改 `application-prod.yml` 强制使用环境变量

- [x] 1.3 更新前端配置
  - [x] 1.3.1 修改 `security.ts` 移除硬编码的 API_SECRET
  - [x] 1.3.2 更新 `.env.example` 添加所有必需变量说明

### 2. 统一日志工具
- [x] 2.1 创建前端日志工具 `src/utils/logger.ts`
  - [x] 2.1.1 实现 Logger 接口 (debug, info, warn, error)
  - [x] 2.1.2 实现日志级别控制逻辑
  - [x] 2.1.3 实现敏感数据过滤 (token, password, secret, key)
  - [x] 2.1.4 根据 NODE_ENV 自动设置默认级别
  - [x] 2.1.5 编写属性测试 `logger.property.test.ts` 验证 P3, P4

- [x] 2.2 替换现有 console.log 调用
  - [x] 2.2.1 替换 `src/api/index.ts` 中的调试日志
  - [x] 2.2.2 替换 `src/stores/auth.ts` 中的调试日志
  - [x] 2.2.3 替换 `src/api/fallback.ts` 中的调试日志
  - [x] 2.2.4 替换 `src/utils/apiHealth.ts` 中的调试日志

### 3. Token 安全存储
- [x] 3.1 创建后端 Refresh Token 服务
  - [x] 3.1.1 创建 `refresh_tokens` 数据库表
  - [x] 3.1.2 创建 `RefreshToken` 实体类
  - [x] 3.1.3 创建 `RefreshTokenRepository`
  - [x] 3.1.4 创建 `RefreshTokenService` 实现生成、验证、撤销逻辑
  - [x] 3.1.5 修改 `AuthController` 添加 `/auth/refresh` 端点

- [x] 3.2 创建前端 Token 管理器 `src/utils/tokenManager.ts`
  - [x] 3.2.1 实现 Access Token 内存存储
  - [x] 3.2.2 实现 Token 刷新逻辑
  - [x] 3.2.3 实现 Token 过期检测
  - [x] 3.2.4 编写属性测试 `token-manager.property.test.ts` 验证 P2

- [x] 3.3 集成 Token 管理器到 Auth Store
  - [x] 3.3.1 修改 `src/stores/auth.ts` 使用 TokenManager
  - [x] 3.3.2 移除 localStorage 中的 auth_token 存储
  - [x] 3.3.3 实现页面刷新后的会话恢复

---

## 第二轮：可靠性增强 (M2)

### 4. API 请求重试机制
- [x] 4.1 创建重试拦截器 `src/api/retry.ts`
  - [x] 4.1.1 实现指数退避延迟计算
  - [x] 4.1.2 实现重试条件判断 (网络错误, 5xx)
  - [x] 4.1.3 实现 GET 请求自动重试
  - [x] 4.1.4 实现重试配置接口
  - [x] 4.1.5 编写属性测试 `retry.property.test.ts` 验证 P5, P6

- [x] 4.2 集成重试拦截器
  - [x] 4.2.1 修改 `src/api/index.ts` 添加重试拦截器
  - [x] 4.2.2 配置默认重试策略

### 5. 请求幂等性保护
- [x] 5.1 创建前端幂等性 Key 生成器 `src/utils/idempotency.ts`
  - [x] 5.1.1 实现基于请求内容的 SHA-256 哈希生成
  - [x] 5.1.2 编写属性测试 `idempotency.property.test.ts` 验证 P7

- [x] 5.2 创建后端幂等性服务
  - [x] 5.2.1 创建 `idempotency_records` 数据库表
  - [x] 5.2.2 创建 `IdempotencyService` 接口和实现
  - [x] 5.2.3 创建 `IdempotencyFilter` 过滤器
  - [x] 5.2.4 编写属性测试 `IdempotencyPropertyTest.java` 验证 P8

- [x] 5.3 集成幂等性检查
  - [x] 5.3.1 修改 `src/api/index.ts` 为写操作添加幂等性 Key
  - [x] 5.3.2 配置需要幂等性检查的接口列表

### 6. 分布式频率限制
- [x] 6.1 创建后端频率限制服务
  - [x] 6.1.1 创建 `RateLimiter` 接口
  - [x] 6.1.2 创建 `InMemoryRateLimiter` 实现
  - [x] 6.1.3 创建 `RedisRateLimiter` 实现 (可选)
  - [x] 6.1.4 创建 `RateLimitFilter` 过滤器
  - [x] 6.1.5 编写属性测试 `RateLimitPropertyTest.java` 验证 P9

- [x] 6.2 配置频率限制策略
  - [x] 6.2.1 配置登录接口限制 (5次/分钟)
  - [x] 6.2.2 配置通用 API 限制 (100次/分钟)
  - [x] 6.2.3 配置 429 响应格式

---

## 第三轮：可维护性提升 (M3)

### 7. 统一错误处理
- [x] 7.1 定义错误响应格式
  - [x] 7.1.1 创建 `src/types/error.ts` 定义 ApiErrorResponse 类型
  - [x] 7.1.2 创建错误码常量文件 `src/constants/errorCodes.ts`

- [x] 7.2 创建前端错误处理器 `src/api/errorHandler.ts`
  - [x] 7.2.1 实现错误格式转换逻辑
  - [x] 7.2.2 实现 requestId 生成和传递
  - [x] 7.2.3 编写属性测试 `error-handler.property.test.ts` 验证 P10

- [x] 7.3 更新后端错误处理
  - [x] 7.3.1 修改 `GlobalExceptionHandler` 统一响应格式
  - [x] 7.3.2 添加 requestId 到所有错误响应
  - [x] 7.3.3 编写属性测试 `ErrorResponsePropertyTest.java` 验证 P10

- [x] 7.4 集成错误处理
  - [x] 7.4.1 修改 `src/api/index.ts` 使用新的错误处理器
  - [x] 7.4.2 更新组件中的错误处理逻辑

### 8. 代码质量工具集成
- [x] 8.1 配置 ESLint
  - [x] 8.1.1 添加禁止 console.log 规则 (允许 warn/error)
  - [x] 8.1.2 添加 TypeScript 严格模式规则

- [x] 8.2 配置 Prettier
  - [x] 8.2.1 创建 `.prettierrc` 配置文件
  - [x] 8.2.2 配置与 ESLint 集成

- [x] 8.3 配置 Git Hooks
  - [x] 8.3.1 安装和配置 Husky
  - [x] 8.3.2 配置 pre-commit 钩子运行 lint
  - [x] 8.3.3 配置 commitlint 规范提交信息

---

## 第四轮：性能优化 (M4)

### 9. 前端性能监控
- [x] 9.1 创建性能监控工具 `src/utils/performance.ts`
  - [x] 9.1.1 实现 Core Web Vitals 收集 (LCP, FID, CLS)
  - [x] 9.1.2 实现 API 请求耗时统计
  - [x] 9.1.3 实现性能数据上报接口

- [x] 9.2 集成性能监控
  - [x] 9.2.1 在 `main.ts` 中初始化性能监控
  - [x] 9.2.2 在 API 拦截器中记录请求耗时

### 10. API 响应缓存
- [x] 10.1 创建前端缓存管理器 `src/utils/cache.ts`
  - [x] 10.1.1 实现基于 ETag 的缓存验证
  - [x] 10.1.2 实现缓存过期策略
  - [x] 10.1.3 实现手动刷新接口

- [x] 10.2 配置后端缓存响应头
  - [x] 10.2.1 为组织机构接口添加 ETag
  - [x] 10.2.2 为指标列表接口添加 Last-Modified

- [x] 10.3 集成缓存
  - [x] 10.3.1 修改 `src/api/index.ts` 添加缓存拦截器
  - [x] 10.3.2 配置各接口的缓存策略

---

## 验收测试

### 安全加固验收
- [ ] V1.1 验证缺少 JWT_SECRET 时应用无法启动
- [ ] V1.2 验证生产环境不输出 debug 日志
- [ ] V1.3 验证 localStorage 中不存在 access_token
- [ ] V1.4 验证 Refresh Token 能正确恢复会话

### 可靠性增强验收
- [ ] V2.1 验证 GET 请求在 5xx 错误时自动重试
- [ ] V2.2 验证 POST 请求不自动重试
- [ ] V2.3 验证重复提交返回相同结果
- [ ] V2.4 验证频率限制触发 429 响应

### 可维护性提升验收
- [ ] V3.1 验证所有错误响应包含 requestId
- [x] V3.2 验证 ESLint 禁止 console.log
- [x] V3.3 验证 pre-commit 钩子正常工作

### 性能优化验收
- [ ] V4.1 验证性能指标正确收集
- [ ] V4.2 验证缓存命中时不发送请求
