# 🗄️ SISM 数据库管理完整指南

## 📋 概述

本项目提供了完整的数据库管理工具集，包括：
1. **统计分析工具** - 检查数据库现状
2. **数据修复脚本** - 清理问题数据
3. **演示数据生成器** - 创建符合业务规范的示例数据

## 🛠️ 工具清单

| 文件 | 类型 | 用途 |
|------|------|------|
| `database/scripts/analyze-database-data.sql` | SQL | 全面统计分析数据库 |
| `database/scripts/fix-database-data.sql` | SQL | 修复数据质量问题 |
| `database/scripts/seed-demo-data.sql` | SQL | 生成演示数据 |
| `scripts/check-database.sh` | Shell | 自动检查脚本 |
| `src/main/java/com/sism/util/DatabaseDataChecker.java` | Java | 数据检查工具类 |
| `src/main/java/com/sism/service/DatabaseAdminService.java` | Java | 数据库管理服务 |
| `src/main/java/com/sism/controller/DatabaseAdminController.java` | Java | REST API 端点 |

---

## 📊 方法一：使用 SQL 脚本（推荐用于生产环境）

### 步骤 1: 统计数据库

```bash
# 连接到远程数据库
psql -h 175.24.139.148 -p 8386 -U postgres -d strategic -f database/scripts/analyze-database-data.sql
```

**输出示例**：
```
>>> 1. 表记录数量统计
[sys_org] 总计: 11 条
[sys_user] 总计: 7 条
[assessment_cycle] 总计: 2 条
...
```

### 步骤 2: 修复数据（如需要）

```bash
psql -h 175.24.139.148 -p 8386 -U postgres -d strategic -f database/scripts/fix-database-data.sql
```

**修复内容**：
- ✅ 清理测试数据（用户、组织、任务、指标）
- ✅ 删除孤儿记录（无关联的指标、里程碑）
- ✅ 修复缺失的外键关联
- ✅ 设置默认状态和进度值

### 步骤 3: 生成演示数据（如数据库为空）

```bash
psql -h 175.24.139.148 -p 8386 -U postgres -d strategic -f database/scripts/seed-demo-data.sql
```

**生成数据**：
- 📊 评估周期：2个
- 🏢 组织结构：11个
- 👥 系统用户：7个
- 📋 战略任务：8个
- 🎯 战略指标：29个
- 📍 里程碑：14个

---

## 🌐 方法二：使用 HTTP API（推荐用于开发调试）

### 启动应用

```bash
cd sism-backend
./mvnw spring-boot:run
```

### 获取数据库报告

```bash
# 完整数据库报告
curl http://localhost:8080/admin/database/report | jq

# 表记录数统计
curl http://localhost:8080/admin/database/counts | jq

# 数据质量检查
curl http://localhost:8080/admin/database/quality-check | jq

# 数据样例
curl http://localhost:8080/admin/database/sample | jq
```

**报告 JSON 示例**：
```json
{
  "summary": {
    "totalTables": 19,
    "tablesWithData": 12,
    "emptyTables": 7,
    "totalRecords": 1234,
    "hasCriticalIssues": false,
    "criticalIssues": []
  },
  "tableCounts": {
    "sys_org": 11,
    "sys_user": 7,
    "assessment_cycle": 2,
    ...
  },
  "testDataStats": {
    "testUsers": 0,
    "testOrgs": 0,
    "testTasks": 0,
    "testIndicators": 0
  }
}
```

---

## 📊 方法三：使用自动化脚本（推荐用于 CI/CD）

```bash
cd sism-backend
./scripts/check-database.sh
```

**脚本功能**：
- ✅ 自动检测并使用 psql 或 HTTP API
- ✅ 生成完整的分析报告
- ✅ 自动清理进程

---

## 🎯 典型使用场景

### 场景 1: 首次部署，需要生成演示数据

```bash
# 1. 检查数据库状态（应该为空）
psql -h 175.24.139.148 -p 8386 -U postgres -d strategic -f database/scripts/analyze-database-data.sql

# 2. 生成演示数据
psql -h 175.24.139.148 -p 8386 -U postgres -d strategic -f database/scripts/seed-demo-data.sql
```

### 场景 2: 生产环境清理测试数据

```bash
# 1. 分析当前数据
psql -h 175.24.139.148 -p 8386 -U postgres -d strategic -f database/scripts/analyze-database-data.sql

# 2. 修复问题数据
psql -h 175.24.139.148 -p 8386 -U postgres -d strategic -f database/scripts/fix-database-data.sql
```

### 场景 3: 开发调试数据检查

```bash
# 启动应用
./mvnw spring-boot:run

# 在另一个终端获取报告
curl http://localhost:8080/admin/database/report | jq
```

---

## ⚠️ 注意事项

### 安全性
- 🔒 生产环境使用前请备份数据库
- 🔑 确保数据库密码安全存储
- 🚫 生产环境禁用 DatabaseAdminController

### 数据修复风险
- ⚠️ `fix-database-data.sql` 会删除数据，请先备份
- 📋 建议先在测试环境验证
- ✅ 每个脚本都有事务保护，失败会回滚

### 业务规范
生成的演示数据符合以下业务逻辑：
- ✅ 层级清晰的组织架构
- ✅ 完整的任务-指标-里程碑关联
- ✅ 合理的权重分配
- ✅ 真实可衡量的指标目标
- ✅ 符合高等教育业务场景

---

## 📞 支持

如遇问题，请检查：
1. 数据库连接配置是否正确
2. 数据库用户是否有足够权限
3. 应用日志：`/tmp/sism-app.log`
4. 数据库日志：PostgreSQL 日志

---

**最后更新**: 2026-02-24
**版本**: v1.0
**维护**: SISM 开发团队
