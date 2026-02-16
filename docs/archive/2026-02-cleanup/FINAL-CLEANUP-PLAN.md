# docs 目录最终清理方案

**日期**: 2026-02-16
**目标**: 彻底清理过时和临时文档

---

## 🗑️ 删除清单

### 1. audit/ 目录（整个删除）

**原因**: 
- 2026-02-14 创建的重构项目基线文档
- 重构任务已完成
- 清单已过时，不再准确
- 直接查看源代码更准确

**文件列表**（7个）:
- README.md - 审计目录说明
- controller-inventory.md - Controller 清单
- dto-vo-inventory.md - DTO/VO 清单
- entity-inventory.md - Entity 清单
- repository-inventory.md - Repository 清单
- service-inventory.md - Service 清单
- dependency-graph.md - 依赖关系图

**操作**: `rm -rf audit/`

### 2. 临时清理文档（归档）

**文件列表**（2个）:
- CLEANUP-REPORT.md - 清理报告
- DOCS-CLEANUP-ANALYSIS.md - 清理分析

**操作**: 移动到 `archive/2026-02-cleanup/`

---

## ✅ 保留的文档

### 核心文档（6个）
1. README.md - 文档目录说明
2. production-fix-2026-02-16.md - 最新生产修复
3. database-tables-index.md - 数据库表索引
4. flyway-migration-guide.md - Flyway 迁移指南
5. IndicatorController-API文档.md - 指标 API 文档
6. TaskController-API文档.md - 任务 API 文档

### 子目录（全部保留）
- architecture/adr/ - 架构决策记录（16个）
- deployment/ - 部署文档（1个）
- nginx/ - Nginx 配置（1个）
- performance/ - 性能测试（1个）
- scripts/ - 运维脚本（12个）
- security/ - 安全文档（2个）
- tables/ - 表文档（45个）

---

## 📊 清理效果

### 清理前
- 根目录文件: 8 个
- audit/ 目录: 7 个文件
- 总文件数: ~110 个

### 清理后
- 根目录文件: 6 个（核心文档）
- audit/ 目录: 删除
- 总文件数: ~103 个

### 改善
- ✅ 删除 7 个过时的清单文档
- ✅ 归档 2 个临时清理文档
- ✅ 根目录减少 25% 的文件
- ✅ 只保留核心和持续维护的文档

---

## 🎯 执行步骤

1. 删除 audit/ 目录
2. 归档临时清理文档
3. 提交到 Git
4. 更新 README.md

---

**创建时间**: 2026-02-16
**执行状态**: 待执行
