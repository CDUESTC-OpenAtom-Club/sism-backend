# 数据库安全配置说明

**配置日期**: 2026-03-12  
**数据库版本**: V31 (最新)  
**状态**: 🔒 已锁定，禁止自动迁移

---

## ⚠️ 重要说明

**数据库已更新到最新版本 (V31)，所有迁移脚本已手动执行完成。**

为确保数据库安全和稳定性，已采取以下保护措施：

---

## 🔒 安全配置

### 1. Flyway 自动迁移已禁用

**配置文件**: `src/main/resources/application.yml`

```yaml
flyway:
  enabled: false  # ✅ 已禁用自动迁移
  baseline-version: 31  # 当前数据库版本
  validate-on-migrate: false
```

**作用**:
- ✅ 防止应用启动时自动执行迁移脚本
- ✅ 避免意外的数据库结构变更
- ✅ 保护生产数据安全

### 2. JPA DDL 自动更新已禁用

```yaml
jpa:
  hibernate:
    ddl-auto: none  # ✅ 不允许 Hibernate 自动修改表结构
```

**作用**:
- ✅ 防止 Hibernate 根据实体类自动修改表结构
- ✅ 确保数据库结构完全由迁移脚本控制

### 3. SQL 初始化已禁用

```yaml
sql:
  init:
    mode: never  # ✅ 不自动执行 schema.sql 和 data.sql
```

**作用**:
- ✅ 防止启动时执行初始化脚本
- ✅ 避免重复创建表或插入数据

---

## 📊 当前数据库状态

### 版本信息

| 项目 | 值 |
|------|-----|
| **当前版本** | V31 |
| **最新迁移** | V31__remove_audit_instance_redundant_columns.sql |
| **迁移状态** | ✅ 已完成 |
| **表结构** | ✅ 最新 |

### 最近的变更 (V31)

**变更内容**: 审批实例表 (`audit_instance`) 重构

**移除的字段** (9个):
- `approved_approvers`, `pending_approvers`, `rejected_approvers`
- `completed_at`
- `current_step_order`
- `submitter_dept_id`, `direct_supervisor_id`, `level2_supervisor_id`, `superior_dept_id`

**影响**:
- ✅ 提升数据库规范性
- ✅ 消除数据冗余
- ✅ 改善审批引擎通用性

---

## 🚫 禁止的操作

### 严格禁止

1. **❌ 不允许启用 Flyway 自动迁移**
   ```yaml
   # ❌ 禁止修改为 true
   flyway:
     enabled: false
   ```

2. **❌ 不允许修改 JPA DDL 配置**
   ```yaml
   # ❌ 禁止修改为 create, update, create-drop
   jpa:
     hibernate:
       ddl-auto: none
   ```

3. **❌ 不允许手动执行迁移脚本**
   ```bash
   # ❌ 禁止执行
   mvn flyway:migrate
   psql -f migration.sql
   ```

4. **❌ 不允许直接修改数据库表结构**
   ```sql
   -- ❌ 禁止执行
   ALTER TABLE ...
   DROP TABLE ...
   CREATE TABLE ...
   ```

---

## ✅ 允许的操作

### 安全操作

1. **✅ 查询数据**
   ```sql
   SELECT * FROM indicators;
   SELECT * FROM audit_instance;
   ```

2. **✅ 插入/更新/删除数据**
   ```sql
   INSERT INTO indicators (...) VALUES (...);
   UPDATE indicators SET ... WHERE ...;
   DELETE FROM indicators WHERE ...;
   ```

3. **✅ 创建索引（性能优化）**
   ```sql
   CREATE INDEX idx_indicator_year ON indicators(year);
   ```

4. **✅ 查看表结构**
   ```sql
   \d indicators
   SELECT * FROM information_schema.columns WHERE table_name = 'indicators';
   ```

---

## 🔧 如果需要数据库变更

### 紧急情况处理流程

如果确实需要修改数据库结构（极端情况）：

1. **评估必要性**
   - 是否可以通过应用层解决？
   - 是否可以使用现有表结构？
   - 变更的风险和影响？

2. **备份数据库**
   ```bash
   pg_dump -h 175.24.139.148 -p 8386 -U postgres -d strategic > backup_$(date +%Y%m%d_%H%M%S).sql
   ```

3. **创建迁移脚本**
   - 在 `src/main/resources/db/migration/` 创建新脚本
   - 命名格式: `V32__description.sql`
   - 使用 `IF EXISTS` 和 `IF NOT EXISTS` 确保幂等性

4. **测试环境验证**
   - 在测试数据库上执行
   - 验证应用正常运行
   - 回归测试所有功能

5. **生产环境执行**
   - 选择低峰时段
   - 手动执行迁移脚本
   - 监控应用状态

6. **更新配置**
   - 更新 `baseline-version` 到新版本
   - 更新本文档

---

## 📋 检查清单

### 启动前检查

在启动应用前，请确认：

- [ ] `flyway.enabled = false`
- [ ] `jpa.hibernate.ddl-auto = none`
- [ ] `sql.init.mode = never`
- [ ] 数据库连接配置正确
- [ ] 数据库版本为 V31

### 部署前检查

在部署新版本前，请确认：

- [ ] 没有新的迁移脚本
- [ ] 实体类变更不影响现有表结构
- [ ] API 兼容性测试通过
- [ ] 数据库配置未被修改

---

## 🔍 验证数据库配置

### 检查 Flyway 状态

```sql
-- 查看 Flyway 迁移历史
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 10;

-- 应该看到 V31 是最新的记录
```

### 检查表结构

```sql
-- 验证 audit_instance 表结构（V31 变更）
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'audit_instance'
ORDER BY ordinal_position;

-- 确认以下字段已不存在：
-- approved_approvers, pending_approvers, rejected_approvers
-- completed_at, current_step_order
-- submitter_dept_id, direct_supervisor_id, level2_supervisor_id, superior_dept_id
```

### 检查应用配置

```bash
# 查看 Flyway 配置
grep -A 10 "flyway:" src/main/resources/application.yml

# 应该看到 enabled: false
```

---

## 📞 联系方式

如有数据库相关问题，请联系：

- **数据库管理员**: [联系方式]
- **技术负责人**: [联系方式]
- **紧急联系**: [联系方式]

---

## 📝 变更日志

| 日期 | 版本 | 变更内容 | 操作人 |
|------|------|----------|--------|
| 2026-03-12 | V31 | 禁用 Flyway 自动迁移，锁定数据库版本 | System |
| 2026-03-12 | V31 | 审批实例表重构完成 | Claude AI |

---

## 🔗 相关文档

- [数据库重构报告](DATABASE_REFACTORING_REPORT.md)
- [API接口文档](docs/API接口文档.md)
- [后端开发规范](../backend-development.md)
- [Flyway迁移指南](docs/flyway-migration-guide.md)

---

**最后更新**: 2026-03-12  
**配置状态**: 🔒 已锁定  
**数据库版本**: V31 (最新)