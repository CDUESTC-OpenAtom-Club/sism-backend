# 数据库脚本目录

本目录包含SISM系统的所有数据库相关脚本。

## 目录结构

```
database/
├── migrations/          # Flyway数据库迁移脚本
│   ├── V1.0__init.sql  # 初始化数据库结构
│   └── V1.1__add_refresh_tokens.sql  # 添加刷新令牌表
├── scripts/            # 维护和管理脚本
│   ├── db-setup.js     # 数据库初始化工具
│   ├── drop-all.sql    # 清空所有表
│   ├── fix-missing-users.sql  # 修复缺失用户
│   ├── validate-data.sql  # 数据验证
│   └── verify-schema.sql  # 结构验证
└── seeds/              # 种子数据
    └── seed-indicators-2026.sql  # 2026年指标数据
```

## 使用说明

### 1. 初始化数据库

使用Flyway自动执行迁移脚本：

```bash
# 开发环境
./mvnw flyway:migrate -Dflyway.configFiles=src/main/resources/application-dev.yml

# 生产环境
./mvnw flyway:migrate -Dflyway.configFiles=src/main/resources/application-prod.yml
```

### 2. 手动执行脚本

```bash
# 连接数据库
psql -h localhost -U postgres -d strategic

# 执行迁移脚本
\i database/migrations/V1.0__init.sql

# 执行种子数据
\i database/seeds/seed-indicators-2026.sql
```

### 3. 维护脚本

```bash
# 修复缺失的用户账号
psql -h localhost -U postgres -d strategic -f database/scripts/fix-missing-users.sql

# 验证数据完整性
psql -h localhost -U postgres -d strategic -f database/scripts/validate-data.sql

# 验证数据库结构
psql -h localhost -U postgres -d strategic -f database/scripts/verify-schema.sql

# 清空所有数据（谨慎使用）
psql -h localhost -U postgres -d strategic -f database/scripts/drop-all.sql
```

## 脚本说明

### migrations/ - 数据库迁移

- **V1.0__init.sql**: 创建所有表结构、索引、视图和触发器
- **V1.1__add_refresh_tokens.sql**: 添加JWT刷新令牌支持

### scripts/ - 维护脚本

- **db-setup.js**: Node.js数据库初始化工具
- **drop-all.sql**: 删除所有表和数据
- **fix-missing-users.sql**: 为所有28个组织创建测试用户（密码：123456）
- **validate-data.sql**: 验证数据完整性和一致性
- **verify-schema.sql**: 验证数据库结构

### seeds/ - 种子数据

- **seed-indicators-2026.sql**: 2026年度指标数据

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
16. **refresh_token** - 刷新令牌表

### 枚举类型（13个）

- org_type, task_type, indicator_level, indicator_status
- milestone_status, report_status, approval_action
- alert_severity, alert_status, adhoc_scope_type
- adhoc_task_status, audit_action, audit_entity_type

### 业务视图（3个）

- v_milestone_weight_sum - 里程碑权重合计验证
- v_indicator_latest_report - 指标最新报告
- v_overdue_milestones - 逾期里程碑

## 注意事项

1. **生产环境**: 执行任何脚本前务必备份数据库
2. **迁移脚本**: 不要修改已执行的迁移脚本，创建新的版本
3. **种子数据**: 仅在开发和测试环境使用
4. **权限**: 确保数据库用户有足够的权限执行DDL和DML操作

## 数据库备份

```bash
# 备份数据库
pg_dump -h localhost -U postgres strategic > backup_$(date +%Y%m%d_%H%M%S).sql

# 恢复数据库
psql -h localhost -U postgres strategic < backup_20250127_120000.sql
```

## JPA配置

### 开发环境
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # 验证表结构
```

### 生产环境
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: none  # 禁用自动DDL
```
