# SISM Backend 文档目录

本目录包含 SISM 后端项目的技术文档。

## 📁 目录结构

### 核心文档

- **database-tables-index.md** - 数据库表结构索引和说明
- **flyway-migration-guide.md** - Flyway 数据库迁移指南
- **IndicatorController-API文档.md** - 指标管理 API 文档
- **TaskController-API文档.md** - 任务管理 API 文档
- **production-fix-2026-02-16.md** - 最新生产环境修复记录

### 📂 audit/ - 代码审计和清单

持续维护的代码清单文档：

- **README.md** - 审计目录说明
- **controller-inventory.md** - Controller 层清单
- **dto-vo-inventory.md** - DTO/VO 清单
- **entity-inventory.md** - Entity 实体清单
- **repository-inventory.md** - Repository 数据访问层清单
- **service-inventory.md** - Service 业务逻辑层清单
- **dependency-graph.md** - 组件依赖关系图

### 📂 deployment/ - 部署相关

- **fix-sudo-permissions.md** - 部署工作流 sudo 权限配置指南
- **springdoc-requirement.md** - SpringDoc/Swagger 部署要求说明

### 📂 scripts/ - 运维脚本

自动化部署和运维脚本：

- **deploy-and-restart-nopasswd.sh** - 无密码部署脚本（推荐）
- **deploy-and-restart.sh** - 标准部署脚本
- **setup-sudoers.sh** - 配置 sudoers 权限
- **health-check.sh** - 健康检查脚本
- **backup-database.sh** - 数据库备份脚本
- **restore-database.sh** - 数据库恢复脚本
- **init-database.sh** - 数据库初始化脚本
- **quick-setup.sh** - 快速环境搭建脚本
- 其他运维脚本...

### 📂 nginx/ - Nginx 配置

- **sism.conf** - SISM 项目 Nginx 配置文件

### 📂 performance/ - 性能测试

- **performance-benchmarks.md** - 性能基准测试报告

### 📂 security/ - 安全相关

- **owasp-dependency-check-guide.md** - OWASP 依赖检查指南
- **SECURITY-SCAN-SETUP-SUMMARY.md** - 安全扫描配置总结

### 📂 tables/ - 数据库表文档

每个数据库表的详细文档（自动生成）：

- `indicator-table.md` - 指标表
- `task-table.md` - 任务表
- `sys_user-table.md` - 用户表
- `sys_org-table.md` - 组织表
- 其他 40+ 张表的文档...

### 📂 architecture/ - 架构设计

- **adr/** - Architecture Decision Records (架构决策记录)

### 📂 archive/ - 归档文档

已完成任务的临时文档归档：

- **2026-02-cleanup/** - 2026年2月清理的临时报告
  - 废弃表清理报告
  - 外键删除报告
  - 审计临时报告
  - 数据库修改说明

## 📝 文档维护原则

### 保留的文档类型

1. **持续维护的清单** - audit/ 目录下的清单文档
2. **核心技术文档** - API 文档、数据库文档
3. **部署运维文档** - 部署指南、脚本说明
4. **最新的修复记录** - 生产环境问题修复文档

### 归档的文档类型

1. **已完成的临时报告** - 审计报告、进度报告
2. **过时的修复记录** - 超过 1 个月的临时修复文档
3. **重复的文档** - 内容被其他文档替代的

### 文档命名规范

- **清单文档**: `{component}-inventory.md`
- **API 文档**: `{Controller}-API文档.md`
- **表文档**: `{table_name}-table.md`
- **修复记录**: `{issue}-fix-{date}.md`
- **指南文档**: `{topic}-guide.md`

## 🔄 文档更新流程

1. **新增文档** - 放在对应的目录下
2. **更新文档** - 直接修改现有文档
3. **归档文档** - 移动到 `archive/{year}-{month}-{topic}/`
4. **删除文档** - 仅删除完全过时且无参考价值的文档

## 📚 相关文档

- **项目根目录 README.md** - 项目总体说明
- **database/** - 数据库迁移脚本和种子数据
- **.github/workflows/** - CI/CD 工作流配置

## 🔗 快速链接

### 开发相关
- [数据库表索引](./database-tables-index.md)
- [Flyway 迁移指南](./flyway-migration-guide.md)
- [Controller 清单](./audit/controller-inventory.md)
- [Entity 清单](./audit/entity-inventory.md)

### 部署相关
- [部署权限配置](./deployment/fix-sudo-permissions.md)
- [SpringDoc 要求](./deployment/springdoc-requirement.md)
- [部署脚本](./scripts/deploy-and-restart-nopasswd.sh)

### API 文档
- [指标管理 API](./IndicatorController-API文档.md)
- [任务管理 API](./TaskController-API文档.md)

---

**最后更新**: 2026-02-16
**维护者**: 开发团队
