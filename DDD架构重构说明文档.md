# DDD架构重构说明文档

## 1. 架构重构背景

### 1.1 项目现状
项目已完成从传统 3 层架构向 **领域驱动设计 (DDD)** 架构的完整重构。当前架构基于限界上下文(Bounded Context)划分模块，每个模块独立负责特定业务领域的功能。

### 1.2 技术栈
- **框架**: Spring Boot 3.2.0 + Java 17
- **项目管理**: Maven 多模块结构
- **数据库**: PostgreSQL 12+
- **ORM**: Spring Data JPA
- **迁移工具**: Flyway
- **架构模式**: DDD 四层架构(Interfaces → Application → Domain → Infrastructure)

---

## 2. 项目结构

### 2.1 模块架构
```
sism-backend/
├── pom.xml                      # 父项目 POM 文件
├── sism-shared-kernel/          # 共享内核 - 基础组件和工具
├── sism-iam/                    # 身份与访问管理(Bounded Context)
├── sism-organization/           # 组织管理(Bounded Context)
├── sism-strategy/               # 战略规划(Bounded Context) - 核心指标管理
├── sism-task/                   # 任务与执行(Bounded Context)
├── sism-execution/              # 执行管理(Bounded Context) - 进度报告
├── sism-workflow/               # 工作流与审批(Bounded Context)
├── sism-analytics/              # 数据分析与报表(Bounded Context)
├── sism-alert/                  # 预警管理(Bounded Context)
└── sism-main/                   # 主应用程序入口
```

### 2.2 限界上下文说明

| 模块 | 领域 | 主要职责 | API 前缀 |
|------|------|----------|----------|
| sism-shared-kernel | 共享内核 | 基础组件、工具类、异常处理、配置 | `/api/shared` |
| sism-iam | 身份管理 | 用户、角色、权限、认证授权 | `/api/auth`, `/api/users` |
| sism-organization | 组织管理 | 机构、部门、层级、组织架构 | `/api/organizations` |
| sism-strategy | 战略规划 | 指标、评估周期、战略计划 | `/api/indicators`, `/api/plans` |
| sism-task | 任务管理 | 任务分配、执行、状态跟踪 | `/api/tasks` |
| sism-execution | 执行管理 | 进度报告、里程碑、任务执行 | `/api/reports`, `/api/milestones` |
| sism-workflow | 工作流管理 | 审批流程、流程实例、步骤管理 | `/api/workflows`, `/api/approvals` |
| sism-analytics | 数据分析 | 报表、图表、数据分析 | `/api/analytics` |
| sism-alert | 预警管理 | 预警级别、规则、通知 | `/api/alerts` |

---

## 3. DDD 架构实现

### 3.1 分层架构
每个 Bounded Context 遵循 DDD 标准分层架构:

```
┌─────────────────────────┐
│    Interfaces 层        │  - REST API 控制器
│  (接口适配层)           │  - DTO 转换
│                         │  - 请求/响应处理
├─────────────────────────┤
│  Application 层          │  - 应用服务
│  (应用服务层)           │  - 用例编排
│                         │  - 事务管理
├─────────────────────────┤
│    Domain 层            │  - 领域实体
│  (领域层)              │  - 聚合根
│                         │  - 值对象
│                         │  - 领域服务
│                         │  - 领域事件
├─────────────────────────┤
│ Infrastructure 层       │  - 数据持久化
│  (基础设施层)           │  - 外部系统集成
│                         │  - 消息队列
│                         │  - 文件存储
└─────────────────────────┘
```

### 3.2 关键架构特性

#### 3.2.1 聚合根设计
```java
// com.sism.strategy.domain.Indicator - 指标聚合根
@Getter
@Setter
@Entity
@Table(name = "indicator")
public class Indicator extends AggregateRoot<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "indicator_desc")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private IndicatorStatus status;

    @ManyToOne
    @JoinColumn(name = "owner_org_id")
    private SysOrg ownerOrg;

    @ManyToOne
    @JoinColumn(name = "target_org_id")
    private SysOrg targetOrg;

    // 领域行为方法
    public void distributeFrom(Indicator parent, SysOrg targetOrg, Double weight) {
        if (this.status != IndicatorStatus.DRAFT) {
            throw new IllegalStateException("Cannot distribute: indicator must be in DRAFT state");
        }
        this.parent = parent;
        this.targetOrg = targetOrg;
        this.weight = BigDecimal.valueOf(weight);
        this.status = IndicatorStatus.DISTRIBUTED;
        this.addEvent(new IndicatorDistributedEvent(this.id, targetOrg.getId()));
    }

    public boolean canBreakdown() {
        return this.isFirstLevel() && this.status == IndicatorStatus.DISTRIBUTED;
    }
}
```

#### 3.2.2 值对象
```java
// com.sism.organization.domain.vo.OrgType - 组织类型值对象
public class OrgType {
    public static final String STRATEGY_DEPT = "STRATEGY_DEPT";
    public static final String FUNCTIONAL_DEPT = "FUNCTIONAL_DEPT";
    public static final String COLLEGE = "COLLEGE";

    private final String value;

    public OrgType(String value) {
        validate(value);
        this.value = value;
    }

    private void validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Organization type cannot be empty");
        }
        List<String> validTypes = Arrays.asList(
            STRATEGY_DEPT, FUNCTIONAL_DEPT, COLLEGE
        );
        if (!validTypes.contains(value)) {
            throw new IllegalArgumentException("Invalid organization type: " + value);
        }
    }
}
```

#### 3.2.3 领域服务
```java
// com.sism.strategy.domain.service.IndicatorDomainService
@Service
@RequiredArgsConstructor
public class IndicatorDomainService {
    private final IndicatorRepository indicatorRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional
    public Indicator distribute(Long sourceIndicatorId, Long targetOrgId, Double targetValue) {
        Indicator sourceIndicator = indicatorRepository.findById(sourceIndicatorId)
            .orElseThrow(() -> new IllegalArgumentException("Source indicator not found"));

        SysOrg targetOrg = organizationRepository.findById(targetOrgId)
            .orElseThrow(() -> new IllegalArgumentException("Target organization not found"));

        Indicator newIndicator = Indicator.distributeFrom(
            sourceIndicator,
            targetOrg,
            targetValue
        );

        return indicatorRepository.save(newIndicator);
    }

    @Transactional
    public List<Indicator> breakdown(Long parentIndicatorId, List<BreakdownItem> breakdownItems) {
        Indicator parent = indicatorRepository.findById(parentIndicatorId)
            .orElseThrow(() -> new IllegalArgumentException("Parent indicator not found"));

        if (!parent.canBreakdown()) {
            throw new IllegalStateException("Indicator cannot be broken down in current status");
        }

        List<Indicator> children = breakdownItems.stream()
            .map(item -> Indicator.breakdownFrom(parent, item.getTargetOrg(), item.getTargetValue()))
            .map(indicatorRepository::save)
            .toList();

        parent.markAsBrokenDown();
        indicatorRepository.save(parent);

        return children;
    }
}
```

---

## 4. 构建与部署

### 4.1 编译项目

```bash
cd /Users/blackevil/Documents/前端架构测试/sism-backend

# 编译所有模块
mvn clean compile -DskipTests

# 运行测试
mvn test

# 安装到本地 Maven 仓库
mvn install -DskipTests
```

### 4.2 启动应用

```bash
# 方式一: 使用 Maven 启动主应用
cd sism-main
mvn spring-boot:run

# 方式二: 打包后运行
mvn clean package -DskipTests
cd sism-main
java -jar target/sism-main-1.0.0.jar
```

### 4.3 数据库初始化

```bash
# 配置数据库连接
cp .env.example .env

# 编辑 .env 文件，配置数据库连接信息
DB_URL=jdbc:postgresql://localhost:5432/sism_dev
DB_USERNAME=your_username
DB_PASSWORD=your_password
JWT_SECRET=your_jwt_secret

# 运行数据库迁移
mvn flyway:migrate -pl sism-shared-kernel -Dflyway.configFiles=../.env

# 加载种子数据
psql -U your_username -d sism_dev -f src/main/resources/db/seeds/seed-data.sql
```

---

## 5. API 文档

### 5.1 访问地址

- **Swagger UI**: `http://localhost:8080/api/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api/v3/api-docs`
- **Redoc**: `http://localhost:8080/api/redoc.html`

### 5.2 测试用户

| 用户名 | 密码 | 角色 | 权限说明 |
|--------|------|------|----------|
| admin | admin123 | 系统管理员 | 所有权限 |
| manager | manager123 | 部门经理 | 指标管理、任务分配 |
| user | user123 | 普通用户 | 任务执行、进度报告 |

---

## 6. 架构优势

### 6.1 业务清晰度
- 每个限界上下文专注于单一业务领域
- 领域模型与业务语言一致
- 边界清晰，降低模块间耦合

### 6.2 可维护性
- 分层架构便于扩展和修改
- 领域逻辑集中在 Domain 层
- 基础设施层实现与领域逻辑分离

### 6.3 可测试性
- 领域模型独立于框架，易于单元测试
- 应用服务层支持集成测试
- 外部依赖可在基础设施层进行 Mock

### 6.4 可扩展性
- 新业务领域可通过新增限界上下文实现
- 模块间通信通过接口定义
- 支持分布式部署和水平扩展

---

## 7. 数据库架构

### 7.1 迁移管理

项目使用 Flyway 进行数据库版本控制:
- 迁移脚本位置: `src/main/resources/db/migration/`
- 种子数据: `src/main/resources/db/seeds/`
- 幂等性设计: 所有迁移脚本支持重复执行

### 7.2 核心数据表

| 表名 | 模块 | 说明 |
|------|------|------|
| indicator | strategy | 指标表 |
| sys_org | organization | 组织表 |
| sys_user | iam | 用户表 |
| plan_report | execution | 计划报告表 |
| task | task | 任务表 |
| milestone | execution | 里程碑表 |
| audit_flow | workflow | 审批流程表 |
| alert | alert | 预警表 |

---

## 8. 开发指南

### 8.1 代码规范

1. **分层架构**: 严格遵循 DDD 四层架构
2. **领域逻辑**: 所有业务逻辑应在 Domain 层实现
3. **数据访问**: 通过 Repository 接口访问数据
4. **服务编排**: 应用服务层负责业务流程编排
5. **接口设计**: RESTful API，使用 DTO 进行数据传输

### 8.2 新增业务功能

```java
// 1. 在 Domain 层创建实体或值对象
@Entity
@Table(name = "new_entity")
public class NewEntity extends AggregateRoot<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 属性和行为
}

// 2. 创建 Repository 接口
@Repository
public interface NewEntityRepository extends JpaRepository<NewEntity, Long> {
    List<NewEntity> findBySomeCondition(String condition);
}

// 3. 创建应用服务
@Service
@RequiredArgsConstructor
public class NewEntityApplicationService {
    private final NewEntityRepository repository;

    @Transactional
    public NewEntity create(NewEntityDTO dto) {
        NewEntity entity = NewEntity.create(dto);
        return repository.save(entity);
    }
}

// 4. 创建 REST 控制器
@RestController
@RequestMapping("/api/new-entities")
@RequiredArgsConstructor
@Tag(name = "New Entities")
public class NewEntityController {
    private final NewEntityApplicationService service;

    @PostMapping
    @Operation(summary = "Create new entity")
    public ResponseEntity<ApiResponse<NewEntity>> create(@RequestBody NewEntityDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(service.create(dto)));
    }
}
```

### 8.3 常见问题

#### 问题: 模块依赖冲突
**解决方案**:
```xml
<!-- 在子模块 pom.xml 中排除冲突依赖 -->
<dependency>
    <groupId>com.sism</groupId>
    <artifactId>sism-shared-kernel</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

#### 问题: 启动时找不到 Bean
**解决方案**:
检查主应用的 `@SpringBootApplication` 扫描包配置:
```java
@SpringBootApplication(scanBasePackages = {"com.sism.iam", "com.sism.organization", "com.sism.strategy"})
```

---

## 9. 生产部署

### 9.1 服务器配置
- **CPU**: 8核+
- **内存**: 16GB+
- **存储**: 100GB+
- **操作系统**: CentOS 7+ 或 Ubuntu 18.04+

### 9.2 部署脚本

```bash
#!/bin/bash

# 部署到生产服务器
SERVER=175.24.139.148
PORT=8386
DEPLOY_PATH=/opt/sism

# 打包应用
mvn clean package -DskipTests -Pprod

# 上传到服务器
scp sism-main/target/sism-main-1.0.0.jar user@${SERVER}:${DEPLOY_PATH}/

# 执行远程部署
ssh user@${SERVER} << 'EOF'
cd /opt/sism
./stop.sh
sleep 5
./start.sh
EOF

echo "部署完成！"
echo "访问地址: http://${SERVER}:${PORT}"
```

---

## 10. 监控与维护

### 10.1 健康检查
```bash
# 应用健康状态
curl -X GET http://localhost:8080/actuator/health

# 详细健康信息
curl -X GET http://localhost:8080/actuator/health/details

# 系统信息
curl -X GET http://localhost:8080/actuator/info
```

### 10.2 日志管理

```bash
# 查看应用日志
tail -f /opt/sism/logs/sism-main.log

# 查看错误日志
tail -f /opt/sism/logs/sism-main-error.log

# 每日日志归档
0 2 * * * /usr/bin/find /opt/sism/logs -name "*.log" -mtime +7 -delete
```

### 10.3 性能监控

```bash
# JVM 内存使用
curl -X GET http://localhost:8080/actuator/metrics/jvm.memory.used

# 线程信息
curl -X GET http://localhost:8080/actuator/metrics/thread.count

# 数据库连接池
curl -X GET http://localhost:8080/actuator/metrics/hikaricp.connections.usage
```

---

## 11. 架构决策记录(ADRs)

项目的重大架构决策已记录在 `docs/architecture/adr/` 目录下:

- ADR-001: 架构风格选择(DDD vs. 微服务)
- ADR-002: 数据库设计规范
- ADR-003: 领域事件实现方案
- ADR-004: 异常处理策略
- ADR-005: 身份认证与授权方案
- ADR-006: 文件存储策略
- ADR-007: 缓存策略
- ADR-008: 消息队列选型
- ADR-009: 测试策略
- ADR-010: 部署架构

---

## 12. 总结

本项目已完成从传统 3 层架构到 DDD 架构的完整重构，具有以下特点:

1. **架构清晰**: 每个限界上下文边界明确，业务逻辑集中
2. **可维护性强**: 分层架构便于扩展和修改
3. **可测试性高**: 领域模型独立于框架，便于单元测试
4. **生产就绪**: 完善的部署、监控和运维方案
5. **业务支持**: 完整的战略指标管理、任务执行、进度跟踪功能

项目已准备好投入生产使用，架构设计符合业务需求，并为未来扩展奠定了坚实基础。
