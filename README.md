# SISM 后端服务

战略指标管理系统 - Spring Boot 后端

## 技术栈

- **框架**: Spring Boot 3.2.0
- **语言**: Java 17
- **构建工具**: Maven
- **数据库**: PostgreSQL
- **ORM**: Spring Data JPA (Hibernate)
- **API 文档**: SpringDoc OpenAPI (Swagger)
- **安全**: Spring Security + JWT
- **测试**: JUnit 5, Mockito, jqwik (属性测试)

## 项目结构

```
src/main/java/com/sism/
├── config/         # 配置类
├── controller/     # REST API 控制器
├── service/        # 业务逻辑层
├── repository/     # 数据访问层 (JPA 仓库)
├── entity/         # JPA 实体类
├── dto/            # 数据传输对象 (请求)
├── vo/             # 值对象 (响应)
├── enums/          # 枚举类型
├── exception/      # 异常处理
├── common/         # 通用工具
└── util/           # 工具类
```

## 快速开始

### 环境要求

- Java 17 或更高版本
- Maven 3.8+
- PostgreSQL 12+

### 数据库配置

1. 创建 PostgreSQL 数据库:
```sql
CREATE DATABASE sism_dev;
```

2. 在 `src/main/resources/application-dev.yml` 中更新数据库凭据

### 构建和运行

```bash
# 构建项目
mvn clean install
```

**Windows 快捷启动 (推荐):**
```cmd
# 开发模式 - 自动加载 .env 配置
start-dev.bat

# 生产模式 - 自动加载 .env 配置
start-prod.bat
```

**手动运行 (需先配置环境变量):**
```bash
# 开发模式
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 生产模式 (需先设置 DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET)
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

> 注意: 生产模式需要配置环境变量，建议使用 `start-prod.bat` 脚本自动加载 `.env` 文件

### API 文档

应用启动后，访问 Swagger UI:
- http://localhost:8080/api/swagger-ui/index.html

API 文档 JSON 地址:
- http://localhost:8080/api/v3/api-docs

> 注意: 生产环境默认禁用 Swagger。如需启用，在 `.env` 文件中设置 `SWAGGER_ENABLED=true`

## 配置说明

### 环境配置文件

- **dev**: 开发环境 (本地数据库)
- **prod**: 生产环境 (远程数据库 175.24.139.148:8386)

### 环境变量

复制 `.env.example` 为 `.env` 并填写配置:
```bash
copy .env.example .env
```

必需的环境变量:

| 变量名 | 说明 |
|--------|------|
| `DB_URL` | 数据库连接 URL |
| `DB_USERNAME` | 数据库用户名 |
| `DB_PASSWORD` | 数据库密码 |
| `JWT_SECRET` | JWT 签名密钥 (至少 256 位) |
| `LOG_PATH` | 日志路径 (可选，默认 `logs`) |

## 测试

```bash
# 运行所有测试
mvn test

# 运行指定测试类
mvn test -Dtest=IndicatorServiceTest

# 运行测试并生成覆盖率报告
mvn test jacoco:report
```

## 开发规范

1. 遵循分层架构: Controller → Service → Repository
2. 请求参数使用 DTO，响应数据使用 VO
3. 业务逻辑在 Service 层实现
4. 使用 JPA Repository 进行数据访问
5. 使用全局异常处理器处理异常
6. 使用 OpenAPI 注解编写 API 文档
7. 编写单元测试和属性测试

## 许可证

专有软件 - SISM 开发团队
