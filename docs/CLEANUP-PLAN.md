# sism-backend/docs 清理计划

## 📋 清理原则

1. **老旧文档** - 2026-02-13 之前的临时报告
2. **已完成任务** - 废弃表清理、外键删除等已完成的临时文档
3. **重复文档** - 内容被其他文档替代的
4. **临时脚本输出** - 审计报告、进度报告等临时性文档

## 🗑️ 建议删除的文件

### 1. 废弃表相关（已完成，可归档）
- `deprecated-table-audit-report.md` - 初始审查报告
- `deprecated-table-fix-FINAL-REPORT.md` - 完整修复报告
- `V1.9-foreign-key-removal-complete.md` - 外键删除完成报告

**原因**: 这些是 2026-02-13 的临时修复报告，问题已解决，可移至 archive

### 2. 数据库修改说明（已过时）
- `database-modification-explanation.md` - 2026-02-15 的错误修改反思

**原因**: 临时性的错误说明文档，问题已解决

### 3. audit 目录下的临时报告（大量重复）
- `audit/backend-refactoring-completion-report.md`
- `audit/code-coverage-analysis-2026-02-14.md`
- `audit/coverage-summary.md`
- `audit/phase1-implementation-guide.md`
- `audit/phase1-progress-2026-02-14.md`
- `audit/phase1-status-and-recommendations.md`
- `audit/service-unit-tests-completion-2026-02-14.md`
- `audit/task-1.1-code-review.md`
- `audit/test-coverage-report.md`
- `audit/test-improvement-report-2026-02-14.md`
- `audit/test-pass-rate-final-report-2026-02-14.md`
- `audit/test-pass-rate-phase1-implementation-plan.md`
- `audit/test-pass-rate-progress-2026-02-14.md`
- `audit/test-pass-rate-roadmap-to-100-percent.md`
- `audit/MULTI-REVIEWER-CODE-REVIEW-SUMMARY.md`
- `audit/openapi-swagger-annotations-report.md`
- `audit/enum-verification-report.md`

**原因**: 这些都是 2026-02-14 的临时审计报告，内容重复，已完成的任务

### 4. 保留的核心文档
✅ `audit/README.md` - 审计目录说明
✅ `audit/controller-inventory.md` - Controller 清单
✅ `audit/dto-vo-inventory.md` - DTO/VO 清单
✅ `audit/entity-inventory.md` - Entity 清单
✅ `audit/repository-inventory.md` - Repository 清单
✅ `audit/service-inventory.md` - Service 清单
✅ `audit/dependency-graph.md` - 依赖关系图

**原因**: 这些是持续维护的清单文档，有参考价值

### 5. 其他临时文档
- `flyway-migration-guide.md` - 可能已过时，需检查
- `production-fix-2026-02-16.md` - 最新的生产修复记录（保留）

## ✅ 保留的文档

### 核心文档
- `database-tables-index.md` - 数据库表索引（核心）
- `IndicatorController-API文档.md` - API 文档
- `TaskController-API文档.md` - API 文档
- `production-fix-2026-02-16.md` - 最新生产修复记录

### 部署相关
- `deployment/fix-sudo-permissions.md` - 部署权限配置
- `deployment/springdoc-requirement.md` - SpringDoc 要求

### 脚本
- `scripts/` 目录下的所有脚本（保留）

### 其他
- `nginx/sism.conf` - Nginx 配置
- `performance/performance-benchmarks.md` - 性能基准
- `security/` 目录下的文档
- `tables/` 目录下的表文档

## 📦 归档建议

创建 `archive/2026-02-cleanup/` 目录，移动以下文件：
1. 所有废弃表相关报告
2. 所有 audit 目录下的临时报告
3. database-modification-explanation.md

## 🎯 清理后的目录结构

```
sism-backend/docs/
├── README.md (新建)
├── database-tables-index.md
├── flyway-migration-guide.md
├── IndicatorController-API文档.md
├── TaskController-API文档.md
├── production-fix-2026-02-16.md
├── architecture/
│   └── adr/
├── audit/
│   ├── README.md
│   ├── controller-inventory.md
│   ├── dto-vo-inventory.md
│   ├── entity-inventory.md
│   ├── repository-inventory.md
│   ├── service-inventory.md
│   └── dependency-graph.md
├── deployment/
│   ├── fix-sudo-permissions.md
│   └── springdoc-requirement.md
├── nginx/
│   └── sism.conf
├── performance/
│   └── performance-benchmarks.md
├── scripts/
│   └── (所有脚本)
├── security/
│   └── (所有安全文档)
└── tables/
    └── (所有表文档)
```

## 执行步骤

1. 创建归档目录
2. 移动临时文档到归档
3. 删除重复的审计报告
4. 创建 docs/README.md 说明文档结构
5. 验证没有遗漏重要文档
