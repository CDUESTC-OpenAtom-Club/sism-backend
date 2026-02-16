# database/scripts 清理报告 - 2026-02-16

**清理时间**: 2026-02-16 19:00
**清理范围**: sism-backend/database/scripts 目录
**状态**: ✅ 完成

---

## 📊 清理总结

### 清理统计

| 类别 | 文件数 | 说明 | 归档位置 |
|------|--------|------|----------|
| 数据库迁移脚本 | 7 | V1.6-V1.9 迁移脚本 | archive/2026-02-migrations-and-fixes/ |
| 数据修复脚本 | 4 | indicator、task、org 修复 | archive/2026-02-migrations-and-fixes/ |
| 表删除脚本 | 3 | 废弃表和索引删除 | archive/2026-02-migrations-and-fixes/ |
| 检查脚本 | 4 | 数据完整性检查 | archive/2026-02-migrations-and-fixes/ |
| 清理脚本 | 3 | 废弃表清理、迁移执行 | archive/2026-02-migrations-and-fixes/ |
| **总计** | **21** | - | - |

---

## 📁 详细清理内容

### 归档的脚本（21个文件）

归档到: `sism-backend/database/scripts/archive/2026-02-migrations-and-fixes/`

#### 1. 数据库迁移脚本（7个）
| 文件 | 创建日期 | 用途 |
|------|----------|------|
| run-v1.6-migration.js | 2026-02-15 | V1.6 版本迁移 |
| run-v1.7-migration.js | 2026-02-10 | V1.7 版本迁移 |
| run-v1.8-migration.js | 2026-02-10 | V1.8 版本迁移 |
| run-v1.9-remove-foreign-keys.js | 2026-02-15 | V1.9 删除外键 |
| run-v1.9-remove-remaining-fk.js | 2026-02-15 | V1.9 删除剩余外键 |
| execute-sys-org-migration.js | 2026-02-10 | 组织表迁移 |
| sync-cycle-to-assessment-cycle.js | 2026-02-10 | 周期表同步 |

#### 2. 数据修复脚本（4个）
| 文件 | 创建日期 | 用途 |
|------|----------|------|
| fix-indicator-task-id.js | 2026-02-15 | 修复 indicator.task_id |
| fix-indicator-task-ids.js | 2026-02-15 | 批量修复 task_id |
| fix-indicator-create-test-tasks.js | 2026-02-15 | 创建测试任务 |
| fix-null-target-org.js | 2026-02-15 | 修复空的 target_org |

#### 3. 表删除脚本（3个）
| 文件 | 创建日期 | 用途 |
|------|----------|------|
| drop-deprecated-tables.js | 2026-02-15 | 删除废弃表 |
| drop-deprecated-indexes.js | 2026-02-15 | 删除废弃索引 |
| drop-app-user-table.js | 2026-02-15 | 删除 app_user 表 |

#### 4. 检查脚本（4个）
| 文件 | 创建日期 | 用途 |
|------|----------|------|
| check-deprecated-table-references.js | 2026-02-15 | 检查废弃表引用 |
| check-foreign-keys.js | 2026-02-15 | 检查外键 |
| check-indicator-constraint.js | 2026-02-15 | 检查指标约束 |
| check-strategic-task-data.js | 2026-02-15 | 检查战略任务数据 |

#### 5. 清理和迁移执行脚本（3个）
| 文件 | 创建日期 | 用途 |
|------|----------|------|
| cleanup-deprecated-tables.js | 2026-02-15 | 清理废弃表 |
| run-migration.js | 2026-02-10 | 执行迁移 |
| run-migration-steps.js | 2026-02-10 | 分步执行迁移 |

**清理原因**:
- 这些都是一次性数据库迁移和修复脚本
- 创建于2月10日和2月15日，距今已经1-6天
- 脚本名称以"fix-"、"run-v"、"drop-"、"check-"开头，表明是临时操作
- 数据库已经迁移到最新版本，这些脚本不再需要

---

## 📂 保留的目录结构

### database/scripts/ 目录（清理后）

```
sism-backend/database/scripts/
├── db-setup.js                # 数据库初始化（保留）
├── check-indicator-schema.js  # Schema 检查（保留）
├── insert-organizations.sql   # 组织数据插入（保留）
├── validate-data.sql          # 数据验证（保留）
├── verify-schema.sql          # Schema 验证（保留）
└── archive/                   # 归档目录
    └── 2026-02-migrations-and-fixes/
        ├── [7个迁移脚本]
        ├── [4个修复脚本]
        ├── [3个删除脚本]
        ├── [4个检查脚本]
        └── [3个清理脚本]
```

### 保留的脚本（5个）

| 文件 | 用途 | 保留原因 |
|------|------|----------|
| db-setup.js | 数据库初始化 | 可能持续使用 |
| check-indicator-schema.js | Schema 检查 | 可能持续使用 |
| insert-organizations.sql | 组织数据插入 | 可能持续使用 |
| validate-data.sql | 数据验证 | 持续使用 |
| verify-schema.sql | Schema 验证 | 持续使用 |

---

## ✅ 清理验证

### 检查项

- [x] 21个一次性脚本已归档
- [x] 保留5个可能持续使用的脚本
- [x] 归档目录结构清晰
- [x] 所有迁移脚本已归档（V1.6-V1.9）
- [x] 所有修复脚本已归档（fix-*）
- [x] 所有删除脚本已归档（drop-*）

---

## 📝 清理原则

本次清理遵循以下原则：

1. **识别一次性脚本**: 迁移、修复、删除、检查脚本
2. **检查创建日期**: 2月10日和2月15日创建
3. **确认完成状态**: 数据库已迁移到最新版本
4. **保留持续使用**: 初始化、验证脚本保留
5. **归档而非删除**: 移动到 archive/ 目录保存

---

## 🎯 清理效果

### 目录整洁度
- ✅ scripts/ 从26个文件减少到5个
- ✅ 清晰区分持续使用和一次性脚本
- ✅ 归档脚本有序保存

### 维护性提升
- 更容易识别当前使用的脚本
- 减少混淆（不会误执行一次性迁移脚本）
- 历史脚本可在archive中查阅

---

## 📚 归档位置

`sism-backend/database/scripts/archive/2026-02-migrations-and-fixes/`

包含21个一次性迁移和修复脚本，都创建于2026-02-10至2026-02-15期间。

---

## 🔄 后续建议

### 脚本管理
1. 新的迁移脚本执行后及时归档
2. 脚本命名清晰标识用途
3. 持续使用的脚本放在主目录

### 定期清理
建议每月检查：
1. 是否有新的一次性脚本需要归档
2. 归档脚本是否可以完全删除（超过6个月）

---

**清理完成时间**: 2026-02-16 19:00
**清理执行者**: Kiro AI Assistant

