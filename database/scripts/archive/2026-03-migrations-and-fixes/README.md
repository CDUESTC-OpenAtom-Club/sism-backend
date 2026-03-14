# 数据库脚本归档 - 2026-03 迁移和修复

**归档时间**: 2026-03-12
**归档原因**: 清理一次性迁移、修复和测试脚本
**状态**: ✅ 已完成

---

## 📊 归档统计

| 类别 | 文件数 | 说明 |
|------|--------|------|
| 迁移脚本 | 7 | V1.6-V1.9版本迁移 |
| 修复脚本 | 5 | 数据修复和完整性修复 |
| 清理脚本 | 5 | 废弃表和索引清理 |
| 生成脚本 | 6 | 2025年测试数据生成 |
| 分析脚本 | 7 | 数据分析和验证 |
| **总计** | **30** | - |

---

## 📁 目录结构

```
archive/2026-03-migrations-and-fixes/
├── migrations/          # 数据库迁移脚本（7个）
├── fixes/              # 数据修复脚本（5个）
├── cleanup/            # 清理脚本（5个）
├── generators/         # 数据生成脚本（6个）
├── analysis/           # 分析验证脚本（7个）
└── README.md          # 本文档
```

---

## 📝 归档内容详情

### 1. migrations/ - 迁移脚本（7个）

| 文件 | 用途 |
|------|------|
| run-v1.6-migration.js | V1.6版本迁移 - 添加组织外键 |
| run-v1.7-migration.js | V1.7版本迁移 - 重命名用户表 |
| run-v1.8-migration.js | V1.8版本迁移 - 任务表重构 |
| run-v1.9-remove-foreign-keys.js | V1.9版本迁移 - 删除外键 |
| run-v1.9-remove-remaining-fk.js | V1.9版本迁移 - 删除剩余外键 |
| run-migration.js | 通用迁移执行器 |
| run-migration-steps.js | 分步迁移执行器 |

**归档原因**: 数据库已迁移到V1.9，这些脚本不应再执行

### 2. fixes/ - 修复脚本（5个）

| 文件 | 用途 |
|------|------|
| fix-indicator-task-id.js | 修复指标的task_id字段 |
| fix-indicator-task-ids.js | 批量修复task_id |
| fix-indicator-create-test-tasks.js | 创建测试任务数据 |
| fix-null-target-org.js | 修复空的target_org字段 |
| fix-database-data.sql | 通用数据修复SQL |

**归档原因**: 历史数据问题已修复，不应重复执行

### 3. cleanup/ - 清理脚本（5个）

| 文件 | 用途 |
|------|------|
| drop-deprecated-tables.js | 删除废弃的数据表 |
| drop-deprecated-indexes.js | 删除废弃的索引 |
| drop-app-user-table.js | 删除app_user表 |
| cleanup-deprecated-tables.js | 清理废弃表 |
| cleanup-2025-data.sql | 清理2025年测试数据 |

**归档原因**: 清理操作已完成，废弃表已删除

### 4. generators/ - 生成脚本（6个）

| 文件 | 用途 |
|------|------|
| generate-2025-data.sql | 生成2025年基础数据 |
| generate-2025-complete-data.sql | 生成2025年完整数据 |
| generate-2025-indicators.sql | 生成2025年指标数据 |
| generate-multi-year-test-data.sql | 生成多年测试数据 |
| execute-generate-2025-data.js | 执行2025数据生成 |
| regenerate-2025-data.js | 重新生成2025数据 |

**归档原因**: 2025年已过，这些测试数据生成脚本不再需要

### 5. analysis/ - 分析脚本（7个）

| 文件 | 用途 |
|------|------|
| execute-sys-org-migration.js | 组织表迁移执行 |
| sync-cycle-to-assessment-cycle.js | 周期表同步 |
| verify-2025-data.js | 验证2025年数据 |
| analyze-2026-tasks.js | 分析2026年任务 |
| analyze-task-year.js | 分析任务年份 |
| analyze-database-data.sql | 分析数据库数据 |
| check-2026-structure.js | 检查2026年结构 |

**归档原因**: 临时分析脚本，问题已解决

---

## ⚠️ 重要提示

1. **不要执行归档脚本**: 这些脚本已完成历史使命，重复执行可能导致数据问题
2. **仅供参考**: 归档脚本保留用于查阅历史操作和问题排查
3. **数据库状态**: 当前数据库已在V1.9版本，所有迁移和修复已完成

---

## 🔍 如何使用归档脚本

### 查阅历史操作
如果需要了解某个历史迁移或修复的具体内容，可以查看对应脚本

### 问题排查
如果遇到与历史迁移相关的问题，可以参考这些脚本了解当时的操作

### 学习参考
新的迁移脚本可以参考这些归档脚本的写法和模式

---

## 📚 相关文档

- [清理建议文档](../../CLEANUP-RECOMMENDATION-20260312.md)
- [Flyway迁移指南](../../../docs/flyway-migration-guide.md)
- [数据库Schema文档](../../../docs/database-schema.md)

---

**归档执行者**: Kiro AI Assistant
**归档完成时间**: 2026-03-12
