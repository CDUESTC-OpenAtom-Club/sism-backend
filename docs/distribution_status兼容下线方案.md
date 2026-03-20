# `distribution_status` 兼容下线方案

> 当前状态更新（2026-03-19）:
> 远程库 `public.indicator.distribution_status` 已删除。
> 本文档保留的早期“字段仍存在”描述，属于阶段性历史记录，不再代表当前库现状。

## 1. 背景

在下线工作启动时，`public.indicator` 表曾保留 `distribution_status` 字段。从代码与迁移文件现状看，这个字段当时已经处于“历史兼容字段”状态，而不是指标主流程的唯一状态来源。

启动下线工作时已确认事实：

1. 远程库 `public.indicator` 中仍存在 `distribution_status` 字段。
2. 截至 2026-03-19，远程库 `indicator` 共 `67` 条数据，其中：
   - `distribution_status = 'DRAFT'` 有 `63` 条
   - `distribution_status IS NULL` 有 `4` 条
3. 当前指标领域模型和接口层仍读写该字段。
4. 当前指标主流程的真正状态流转主要由 `status` 驱动，而不是由 `distribution_status` 驱动。
5. [V34__consolidate_indicator_status.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/migrations/V34__consolidate_indicator_status.sql) 已明确表达“合并到 `status` 后删除 `distribution_status`”的目标方向，但远程库现状与该目标尚未完全一致。

## 2. 当前依赖现状

### 2.1 数据库层

- 表：`public.indicator`
- 字段：`distribution_status character varying(20)`
- 当前库中该字段仍有历史值，不能直接视为无效空壳字段。

### 2.2 代码层

当前仍存在以下依赖：

- 实体映射：
  - [Indicator.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/domain/Indicator.java)
- 创建/更新时写入：
  - [StrategyApplicationService.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/application/StrategyApplicationService.java)
- 接口入参/返回：
  - [IndicatorController.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/interfaces/rest/IndicatorController.java)
- 种子/脚本：
  - [seed_data_v2.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/seeds/seed_data_v2.sql)
  - [seed_data_v3_review.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/seeds/seed_data_v3_review.sql)
  - [generate-2025-indicators.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/scripts/generate-2025-indicators.sql)

### 2.3 主流程判断

当前领域方法中：

- `submitForReview()`
- `approve()`
- `reject()`
- `distribute()`
- `confirmReceive()`

这些方法都主要更新 `status`，而不是同步更新 `distribution_status`。

因此可以判断：

- `distribution_status` 当前不是指标主流程的唯一权威状态字段
- 它更像历史兼容字段
- 若继续保留，存在与 `status` 漂移不一致的风险

## 3. 下线目标

目标是把指标状态统一收敛到 `indicator.status`，并逐步移除：

1. 数据库字段 `indicator.distribution_status`
2. Java 实体映射 `distributionStatus`
3. 创建/更新接口中的 `distributionStatus` 入参
4. 查询接口中的 `distribution-status` 兼容输出
5. seed / generator / 文档中的历史引用

## 4. 分阶段执行策略

### 阶段 1：冻结新增依赖

目标：

- 不再新增任何新的 `distribution_status` 读取或写入
- 新逻辑统一只认 `status`

动作：

1. 代码审查中禁止新增 `distributionStatus` 新引用
2. 文档中明确 `status` 为指标主状态字段
3. 新接口设计不再暴露 `distributionStatus`

### 阶段 2：做兼容读取，停止主流程写入

目标：

- 主流程统一只维护 `status`
- 若旧接口仍读取 `distributionStatus`，则优先从 `status` 计算或回退

动作：

1. 审查 `Indicator.getDistributionStatus()` 的兼容策略
2. 确认所有状态变化操作只依赖 `status`
3. 将控制器和 DTO 标记为兼容输出，而非主字段

### 阶段 3：清理接口与代码引用

目标：

- 移除后端主代码中对 `distributionStatus` 的显式入参和写入

动作：

1. 删除实体字段映射
2. 删除控制器入参 `distributionStatus`
3. 删除服务层 create/update 中的 `distributionStatus` 处理逻辑
4. 删除单独的 `/distribution-status` 查询接口，或改为返回 `status`
5. 修正 OpenAPI / API 文档

### 阶段 4：数据库删列

前提：

- 主代码不再依赖该字段
- seed / 生成脚本 / 查询脚本都已切换
- 数据回放确认历史页面不再依赖

动作：

1. 执行专门的删列 migration
2. 删除旧字段说明与旧导出文档中的主流程描述
3. 保留删列前备份与变更记录

## 5. 推荐执行顺序

建议严格按以下顺序推进：

1. 先落文档和检查清单
2. 再清主代码依赖
3. 再清脚本与种子数据
4. 最后执行数据库删列

不建议直接先删库里的列，否则容易把还在跑的兼容代码打断。

## 6. 变更历史记录要求

后续每一次相关修改，都必须在本文档追加记录，最少包含：

- 日期
- 修改人
- 修改范围
- 是否改动数据库
- 是否改动 Java 实体/接口/脚本
- 是否完成回归验证

推荐格式：

```md
## YYYY-MM-DD 变更记录

- 修改人:
- 变更范围:
- 数据库:
- 代码:
- 验证:
- 备注:
```

## 7. 当前记录

## 7.1 当前归档口径

- 远程数据库现状：`distribution_status` 已删列
- 现行代码现状：主链路已统一使用 `status`
- 现行文档现状：主文档、OpenAPI、seed、generator 已完成清理
- 本文档用途：保留排查、分轮实施、删列与验证的完整历史

## 2026-03-19 变更记录

- 修改人: Codex
- 变更范围: 完成 `distribution_status` 现状排查与下线方案建立
- 数据库: 未修改字段，仅核查远程库现状与值分布
- 代码: 未修改主代码，仅核查实体、服务、控制器、脚本中的引用
- 验证:
  - 远程库 `indicator` 共 `67` 条
  - `distribution_status` 非空 `63` 条，全部为 `DRAFT`
  - `distribution_status IS NULL` 为 `4` 条
- 备注:
  - 当前字段仍有历史数据
  - 当前主流程以 `status` 为准
  - 后续应按“先停依赖，再删列”的顺序推进

## 2026-03-19 第一轮实施记录

- 修改人: Codex
- 变更范围:
  - 后端 `sism-strategy` 移除 `distributionStatus` 作为 create/update 主入参语义
  - 前端 `strategic-task-management` 移除主链路中的 `distributionStatus` 类型、映射和页面依赖
  - `/api/v1/indicators/{id}/distribution-status` 保留路由但改为停用响应
- 数据库:
  - 未删列
  - 远程库中的 `indicator.distribution_status` 继续保留为历史兼容字段
- 代码:
  - `Indicator` 主模型不再保留 `distributionStatus` 运行时字段
  - `StrategyApplicationService` 不再显式写入 `distributionStatus`
  - `IndicatorController` 的创建/更新请求不再接收 `distributionStatus`
  - 前端页面和 API 不再调用 `/distribution-status` 接口
  - 前端页面状态展示统一改为以 `status` 推导
- 验证:
  - 主代码 `rg -n "distributionStatus|distribution_status|distribution-status"` 仅剩停用接口路由命中
  - 前后端主链路实现文件已不再把 `distributionStatus` 作为状态来源
- 备注:
  - 本轮刻意保留停用接口路径，用于向旧调用方返回明确的废弃错误
  - 第二轮再执行一致性核查、数据备份和数据库删列

## 2026-03-19 第二轮实施记录

- 修改人: Codex
- 变更范围:
  - 执行远程库 `indicator.distribution_status` 删列
  - 删除后端最后的停用兼容路由
  - 新增本地 migration 文件记录删列动作
- 数据库:
  - 已备份 `indicator(id, status, distribution_status)` 数据
  - 已备份删列前 `indicator` 表结构
  - 已从远程库删除 `distribution_status` 字段及其 check 约束
- 代码:
  - 删除 `IndicatorController` 中 `/api/v1/indicators/{id}/distribution-status` 路由
  - 新增 [V44__drop_indicator_distribution_status.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/migrations/V44__drop_indicator_distribution_status.sql)
- 验证:
  - 删列前漂移核查结果：
    - `status=DRAFT, distribution_status=DRAFT` 为 `54` 条
    - `status=PENDING, distribution_status=DRAFT` 为 `9` 条
    - `status=DISTRIBUTED, distribution_status=NULL` 为 `2` 条
    - `status=DRAFT, distribution_status=NULL` 为 `2` 条
  - 远程库核查 `information_schema.columns` 结果为 `0`，说明字段已删除
  - `mvn -pl sism-strategy -am -DskipTests compile` 通过

## 2026-03-19 第三轮实施记录

- 修改人: Codex
- 变更范围:
  - 清理现行 seed / generator 脚本中的 `distribution_status`
  - 清理数据库导出文档、API 文档、OpenAPI 静态导出中的历史接口与字段描述
- 数据库:
  - 未新增远程 DDL
  - 现行文档已与“远程库已删列”的事实对齐
- 代码与文档:
  - [seed_data_v2.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/seeds/seed_data_v2.sql) 已改为只写 `status`
  - [seed_data_v3_review.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/seeds/seed_data_v3_review.sql) 已改为只写 `status`
  - [generate-2025-indicators.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/scripts/generate-2025-indicators.sql) 已移除 `distribution_status`
  - [fill_2026_functional_department_linked_data.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/scripts/fill_2026_functional_department_linked_data.sql) 已移除 `distribution_status`
  - 数据库导出文档与 OpenAPI 静态导出已去除现行残留
- 验证:
  - 面向现行文档与脚本的仓库扫描已不再命中运行链路文件
  - 剩余命中仅保留在历史方案、检查清单、备份文件与 archive 目录
- 备注:
  - 旧字段已确认发生状态漂移，因此不适合作为继续保留的兼容来源
  - 历史脚本、seed、导出文档中的旧字段提及仍可后续继续清理

## 2026-03-19 第四轮实施记录

- 修改人: Codex
- 变更范围:
  - 整理历史文档口径，区分“当前状态”和“阶段性历史记录”
  - 新增历史资料索引，标记哪些文件属于现行资料、哪些属于审计留痕、哪些属于备份归档
  - 对当前本地服务执行指标相关接口冒烟测试
- 数据库:
  - 未新增远程 DDL
- 文档:
  - 本文档增加当前状态提示，避免历史描述被误读为当前事实
  - 新增 [distribution_status历史资料索引.md](/Users/blackevil/Documents/前端架构测试/sism-backend/docs/distribution_status历史资料索引.md)
- 验证:
  - `POST /api/v1/auth/login` 使用 `admin/admin123` 与 `zlb_admin/admin123` 均返回 `200`
  - `GET /api/v1/indicators`、`GET /api/v1/indicators/{id}`、`POST /api/v1/indicators` 在业务账号下当前返回 `500`
  - 当前本地服务在线，但指标接口仍存在运行时异常，需要继续排查
