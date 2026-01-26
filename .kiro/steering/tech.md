# 技术栈

## 前端 (strategic-task-management/)

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.5.x | 核心框架，使用 Composition API + `<script setup>` |
| TypeScript | 5.9.x | 类型检查 |
| Vite | 7.x | 构建工具 |
| Pinia | 2.x | 状态管理 |
| Vue Router | 4.x | 路由管理 |
| Element Plus | 2.x | UI 组件库 |
| ECharts | 5.x | 图表可视化 |
| Axios | 1.x | HTTP 客户端 |
| Vitest | 4.x | 单元测试 |
| fast-check | 4.x | 属性测试 |

## 后端 (sism-backend/)

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.0 | 核心框架 |
| Java | 17 | 编程语言 |
| Spring Data JPA | - | ORM |
| Spring Security | - | 认证授权 |
| JWT (jjwt) | 0.12.x | Token 认证 |
| PostgreSQL | 15+ | 数据库 |
| Lombok | - | 代码简化 |
| MapStruct | 1.5.x | 对象映射 |
| SpringDoc OpenAPI | 2.3.x | API 文档 |
| jqwik | 1.8.x | 属性测试 |

## 常用命令

### 前端

```bash
cd strategic-task-management

# 开发
npm run dev              # 启动开发服务器 (端口 3000)
npm run server           # 启动后端 API 服务器 (端口 8080)

# 构建
npm run build            # 生产构建
npm run build:check      # 类型检查 + 构建

# 测试
npm run test             # 运行测试 (单次)
npm run test:watch       # 监听模式测试

# 数据
npm run sync-db          # 同步数据到数据库
```

### 后端

```bash
cd sism-backend

# 开发
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 构建
./mvnw clean install

# 测试
./mvnw test
./mvnw test -Dtest=IndicatorServiceTest  # 单个测试类
```

## API 代理配置

前端开发服务器将 `/api` 请求代理到 `http://localhost:8080`。

## 环境配置

- 前端: `.env`, `.env.development`, `.env.production`
- 后端: `application.yml`, `application-dev.yml`, `application-prod.yml`
