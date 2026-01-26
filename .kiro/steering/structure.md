# 项目结构

```
战略系统/
├── docs/                          # 📚 部署文档与运维脚本
├── sism-backend/                  # ☕ Spring Boot 后端
└── strategic-task-management/     # 🎨 Vue 3 前端
```

## 前端结构 (strategic-task-management/)

```
src/
├── api/                # API 请求层（含降级机制）
│   ├── index.ts       # Axios 实例和拦截器
│   ├── fallback.ts    # 降级服务
│   ├── indicator.ts   # 指标 API
│   ├── milestone.ts   # 里程碑 API
│   ├── org.ts         # 组织机构 API
│   └── strategic.ts   # 战略任务 API
├── components/         # Vue 组件
│   ├── approval/      # 审批相关组件
│   ├── charts/        # ECharts 图表组件
│   ├── common/        # 通用组件
│   ├── dashboard/     # 仪表盘组件
│   ├── indicator/     # 指标组件
│   ├── milestone/     # 里程碑组件
│   ├── profile/       # 个人中心组件
│   └── task/          # 任务组件
├── views/              # 页面视图
├── stores/             # Pinia 状态管理
│   ├── auth.ts        # 认证状态
│   ├── dashboard.ts   # 仪表盘状态
│   ├── org.ts         # 组织机构状态
│   ├── strategic.ts   # 战略任务状态
│   ├── auditLog.ts    # 审计日志状态
│   └── timeContext.ts # 时间上下文
├── types/              # TypeScript 类型定义
├── utils/              # 工具函数
├── config/             # 配置文件
├── data/               # 静态数据和模拟数据
└── router/             # 路由配置

server/                 # Node.js 后端 API 服务
├── routes/            # Express 路由
├── middleware/        # 中间件（auth, security）
├── db.js              # 数据库连接
└── index.js           # 入口文件

scripts/                # 数据同步与验证脚本
├── sync-all.js        # 数据同步主入口
├── sync-context.js    # 同步上下文管理
├── config.js          # 数据库配置
├── verify.js          # 统一验证工具
├── update-remarks.js  # 指标备注更新
└── phases/            # 各阶段同步脚本
    ├── sync-org.js    # 组织机构同步
    ├── sync-cycle.js  # 考核周期同步
    ├── sync-indicator.js # 指标同步
    ├── sync-milestone.js # 里程碑同步
    └── sync-task.js   # 任务同步

database/               # 数据库脚本
├── init.sql           # 建表脚本（含初始数据）
├── drop-all.sql       # 清理脚本
├── seed-data.sql      # 种子数据
├── validate-data.sql  # 数据校验
└── db-setup.js        # 数据库初始化工具

tests/                  # 测试文件
├── property/          # 属性测试（fast-check）
└── unit/              # 单元测试（vitest）
```

## 后端结构 (sism-backend/)

```
src/main/java/com/sism/
├── config/             # 配置类
│   ├── SecurityConfig.java        # Spring Security 配置
│   ├── CorsConfig.java            # CORS 配置
│   ├── JwtAuthenticationFilter.java # JWT 过滤器
│   ├── OpenApiConfig.java         # API 文档配置
│   ├── AuditLogAspect.java        # 审计日志切面
│   └── ...
├── controller/         # REST API 控制器
│   ├── AuthController.java        # 认证控制器
│   ├── IndicatorController.java   # 指标控制器
│   ├── MilestoneController.java   # 里程碑控制器
│   ├── OrgController.java         # 组织机构控制器
│   ├── TaskController.java        # 任务控制器
│   └── ...
├── service/            # 业务逻辑层
├── repository/         # JPA 数据访问层
├── entity/             # JPA 实体类
│   ├── BaseEntity.java            # 基础实体
│   ├── Indicator.java             # 指标实体
│   ├── Milestone.java             # 里程碑实体
│   ├── Org.java                   # 组织机构实体
│   ├── StrategicTask.java         # 战略任务实体
│   └── ...
├── dto/                # 请求 DTO (Data Transfer Object)
├── vo/                 # 响应 VO (Value Object)
├── enums/              # 枚举类型
│   ├── OrgType.java               # 组织类型
│   ├── IndicatorStatus.java       # 指标状态
│   ├── MilestoneStatus.java       # 里程碑状态
│   └── ...
├── exception/          # 异常处理
│   ├── GlobalExceptionHandler.java # 全局异常处理器
│   ├── BusinessException.java      # 业务异常
│   └── ...
├── common/             # 通用类
│   ├── ApiResponse.java           # 统一响应格式
│   └── PageResult.java            # 分页结果
└── util/               # 工具类
    ├── JwtUtil.java               # JWT 工具
    └── LoggingUtils.java          # 日志工具

src/main/resources/
├── application.yml     # 主配置
├── application-dev.yml # 开发环境配置
├── application-prod.yml# 生产环境配置
├── schema.sql          # 数据库 Schema
├── data.sql            # 初始数据
└── logback-spring.xml  # 日志配置

database/               # 数据库脚本
├── seed-indicators-2026.sql # 2026年指标数据
├── fix-admin-password.sql   # 修复管理员密码
└── verify-schema.sql        # 验证数据库结构

src/test/java/com/sism/
├── controller/         # 集成测试
├── service/            # 服务层测试
└── property/           # 属性测试 (jqwik)
```

## 命名约定

### 前端
- **组件**: PascalCase (e.g., `IndicatorDetailDialog.vue`)
- **视图**: PascalCase + View 后缀 (e.g., `DashboardView.vue`)
- **Store**: camelCase (e.g., `useAuthStore`)
- **API**: camelCase (e.g., `getIndicatorList`)
- **类型**: PascalCase (e.g., `Department`, `Indicator`)

### 后端
- **Entity**: PascalCase (e.g., `StrategicTask.java`)
- **DTO**: PascalCase + Request 后缀 (e.g., `IndicatorCreateRequest`)
- **VO**: PascalCase + VO 后缀 (e.g., `IndicatorVO`)
- **Controller**: PascalCase + Controller 后缀
- **Service**: PascalCase + Service 后缀
- **Repository**: PascalCase + Repository 后缀

## 路径别名

```typescript
'@'           -> src/
'@components' -> src/components/
'@views'      -> src/views/
'@stores'     -> src/stores/
'@types'      -> src/types/
'@utils'      -> src/utils/
'@api'        -> src/api/
```

## 关键文件说明

### 前端核心文件
- `src/App.vue` - 应用主组件，包含部门切换器
- `src/main.ts` - 应用入口
- `src/router/index.ts` - 路由配置
- `vite.config.ts` - Vite 构建配置
- `tsconfig.json` - TypeScript 配置

### 后端核心文件
- `SismApplication.java` - Spring Boot 应用入口
- `pom.xml` - Maven 依赖配置
- `application.yml` - 应用配置

### 配置文件
- `.env` / `.env.development` / `.env.production` - 前端环境变量
- `application-dev.yml` / `application-prod.yml` - 后端环境配置
