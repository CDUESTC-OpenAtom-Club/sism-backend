# SISM Backend 文档目录

本目录只保留当前仍在使用的后端基线文档、审批工作流重构文档和生成产物。

## 当前主文档

- `BOUNDED_CONTEXT_MAP.md`
  - 后端限界上下文与模块边界总览
- `audit_flow_def_重构设计方案.md`
  - `audit_flow_def` 表重构设计
- `audit_instance_重构设计方案.md`
  - `audit_instance` 表重构设计
- `audit_step_def_重构设计方案.md`
  - `audit_step_def` 表重构设计
- `audit_step_instance_重构设计方案.md`
  - `audit_step_instance` 表重构设计
- `审批工作流模块化重构设计方案.md`
  - 审批工作流整体模块化方案
- `审批相关数据库与字段使用报告.md`
  - 当前审批表字段使用情况与收口结果
- `数据库删列执行前检查清单.md`
  - 数据库删列执行前检查与回归清单
- `flyway-migration-guide.md`
  - Flyway 迁移执行说明
- `API接口文档.md`
  - 后端接口说明补充文档
- `流程.md`
  - 后端关键流程说明
- `用户账号密码文档.md`
  - 环境账号信息说明

## 生成产物与配置

- `db-export/`
  - 数据库表与字段导出结果
- `openapi/`
  - OpenAPI 快照与导出文件
- `nginx/`
  - 部署用 Nginx 配置

## 归档目录

- `archive/2026-03-task-and-report-history/`
  - 已完成的 API 修复任务包
  - 阶段性后端架构报告

## 清理原则

1. 根目录只放当前仍会被继续引用的基线文档和执行文档。
2. 阶段报告、任务拆解、历史交付物统一移动到 `archive/`。
3. 自动生成文件保留在专门子目录，不和设计文档混放。
4. 新增文档前先判断它属于“主文档”“生成产物”还是“归档材料”。

## 维护建议

1. 新的审批设计或数据库执行文档，优先放在根目录。
2. 一次性任务文档、阶段报告、已完成修复清单，完成后立即归档。
3. `db-export/` 与 `openapi/` 更新时，只覆盖最新快照，不再在根目录额外复制一份。
