# SISM 数据库初始化和验证

本目录包含 SISM 系统的数据库初始化和验证脚本。

## 目录结构

```
database/
├── README.md              # 本文件
├── verify-schema.sql      # 数据库表结构验证SQL脚本
└── (可选) schema.sql      # 数据库表结构定义
└── (可选) data.sql        # 初始数据插入脚本
```

## 数据库初始化

### 前提条件

1. PostgreSQL 数据库已安装并运行
2. 已创建数据库（开发环境: `sism_dev`, 生产环境: `sism_prod`）
3. 数据库用户具有足够的权限

### 初始化方式

SISM 系统使用以下方式初始化数据库：

#### 方式一：使用现有的初始化脚本（推荐）

项目根目录下的 `strategic-task-management/database/` 目录包含完整的数据库初始化脚本：

```bash
# 1. 进入数据库脚本目录
cd strategic-task-management/database/

# 2. 执行初始化脚本
psql -U postgres -d sism_dev -f init.sql

# 3. (可选) 插入示例数据
psql -U postgres -d sism_dev -f seed-data.sql
```

#### 方式二：使用 JPA 自动创建（不推荐生产环境）

在开发环境中，可以配置 JPA 自动创建表结构：

```yaml
# application-dev.yml
spring:
  jpa:
    hibernate:
      ddl-auto: create  # 或 update
```

**注意**: 生产环境必须使用 `ddl-auto: none` 或 `validate`

## 数据库表结构验证

### 方式一：使用 SQL 脚本验证

```bash
# 执行验证脚本
psql -U postgres -d sism_dev -f verify-schema.sql
```

该脚本会验证：
- ✓ 所有表是否存在
- ✓ 所有枚举类型是否存在
- ✓ 外键约束是否正确
- ✓ 索引是否创建
- ✓ 主键约束是否存在
- ✓ 检查约束是否正确
- ✓ 触发器是否创建
- ✓ 各表记录数统计

### 方式二：使用 Java 测试类验证

```bash
# 运行数据库验证测试
cd sism-backend
mvn test -Dtest=DatabaseSchemaVerifier
```

该测试类会验证：
- ✓ 所有必需的表都已创建（15个表）
- ✓ 所有枚举类型都已创建（13个枚举）
- ✓ 外键约束正确配置
- ✓ 索引已创建
- ✓ 主键约束存在
- ✓ 检查约束（如 progress_report 的互斥约束）
- ✓ 触发器（updated_at 自动更新）
- ✓ 业务视图（3个视图）

## JPA ddl-auto 策略配置

### 开发环境 (application-dev.yml)

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # 验证表结构，不自动创建
```

**validate**: 启动时验证实体类与数据库表结构是否一致，不一致则抛出异常

### 生产环境 (application-prod.yml)

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: none  # 不进行任何操作
```

**none**: 完全禁用 Hibernate 的 DDL 自动生成功能，生产环境必须使用此配置

## 数据库表清单

### 核心业务表（15个）

1. **org** - 组织表
2. **app_user** - 用户表
3. **assessment_cycle** - 考核周期表
4. **strategic_task** - 战略任务表
5. **indicator** - 指标表（支持自引用）
6. **milestone** - 里程碑表
7. **progress_report** - 进度汇报表
8. **approval_record** - 审批记录表
9. **alert_window** - 预警窗口表
10. **alert_rule** - 预警规则表
11. **alert_event** - 预警事件表
12. **adhoc_task** - 临时任务表
13. **adhoc_task_target** - 临时任务-目标组织表
14. **adhoc_task_indicator_map** - 临时任务-指标映射表
15. **audit_log** - 审计日志表

### 枚举类型（13个）

- org_type
- task_type
- indicator_level
- indicator_status
- milestone_status
- report_status
- approval_action
- alert_severity
- alert_status
- adhoc_scope_type
- adhoc_task_status
- audit_action
- audit_entity_type

### 业务视图（3个）

- v_milestone_weight_sum - 里程碑权重合计验证
- v_indicator_latest_report - 指标最新报告
- v_overdue_milestones - 逾期里程碑

## 关键约束

### 外键约束

所有表之间的关联关系都通过外键约束保证数据完整性。

### 检查约束

- **progress_report.chk_report_target**: milestone_id 和 adhoc_task_id 不能同时非空

### 触发器

所有表都配置了 `updated_at` 字段的自动更新触发器。

## 故障排查

### 问题1: 表不存在

**症状**: 启动应用时报错 "relation does not exist"

**解决方案**:
1. 确认数据库已创建
2. 执行 `init.sql` 初始化脚本
3. 检查 `application-dev.yml` 中的数据库连接配置

### 问题2: 枚举类型不匹配

**症状**: 启动时报错 "column is of type xxx but expression is of type character varying"

**解决方案**:
1. 确认数据库中的枚举类型已创建
2. 检查 Java 枚举类与数据库枚举值是否一致
3. 重新执行 `init.sql` 创建枚举类型

### 问题3: ddl-auto 配置错误

**症状**: 生产环境表结构被意外修改

**解决方案**:
1. 立即将 `ddl-auto` 改为 `none`
2. 从备份恢复数据库
3. 使用 SQL 脚本管理表结构变更

## 最佳实践

1. **开发环境**: 使用 `ddl-auto: validate` 验证表结构一致性
2. **生产环境**: 必须使用 `ddl-auto: none`，通过 SQL 脚本管理表结构
3. **版本控制**: 所有数据库变更脚本都应纳入版本控制
4. **定期验证**: 定期运行验证脚本确保数据库结构正确
5. **备份策略**: 生产环境执行任何数据库变更前必须备份

## 参考文档

- [Spring Data JPA 文档](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Hibernate DDL Auto 配置](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#configurations-hbmddl)
- [PostgreSQL 文档](https://www.postgresql.org/docs/)
