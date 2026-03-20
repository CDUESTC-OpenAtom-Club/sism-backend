# `distribution_status` 删列执行前检查清单

> 当前状态更新（2026-03-19）:
> 远程库 `indicator.distribution_status` 已删除。
> 本清单中的“删列前检查”内容保留为审计记录与回溯依据，不代表当前仍未删列。

本清单最初用于执行 `indicator.distribution_status` 删列前的最终核查，现保留为审计记录与回溯依据。

目标：

- 确认后端主代码不再依赖该字段
- 确认历史数据与页面展示不再依赖该字段
- 确认删列后不会影响指标创建、查询、审批和下发链路

## 1. 执行范围

本次检查仅针对：

- 表：`public.indicator`
- 字段：`distribution_status`

## 2. 当前计划移除内容

### 2.1 数据库

- `indicator.distribution_status`

### 2.2 代码

- `Indicator.distributionStatus`
- `Indicator.getDistributionStatus()`
- `Indicator.setDistributionStatus(...)`
- create / update 接口中的 `distributionStatus`
- `/indicators/{id}/distribution-status` 接口

### 2.3 脚本与数据

- seed 数据中的 `distribution_status`
- generator 脚本中的 `distribution_status`
- 导出文档中的旧字段说明

## 3. 代码依赖检查

执行前先在当前仓库全文检索：

```bash
rg -n "distribution_status|distributionStatus" sism-backend
```

通过标准：

1. `sism-strategy/src/main/java` 主代码不再把 `distributionStatus` 作为实体主字段、服务主逻辑或控制器主入参
2. Repository / Query 不再使用 `distribution_status` 参与过滤和排序
3. API 文档、OpenAPI、导出文档已经同步到只认 `status`

## 4. 数据库对象依赖检查

### 4.1 视图依赖

```sql
SELECT table_schema,
       table_name,
       view_definition
FROM information_schema.views
WHERE view_definition ILIKE '%distribution_status%';
```

通过标准：

- 不再有视图引用 `distribution_status`

### 4.2 函数 / 存储过程依赖

```sql
SELECT n.nspname AS schema_name,
       p.proname AS function_name,
       pg_get_functiondef(p.oid) AS definition
FROM pg_proc p
JOIN pg_namespace n ON n.oid = p.pronamespace
WHERE pg_get_functiondef(p.oid) ILIKE '%distribution_status%';
```

通过标准：

- 不再有函数引用 `distribution_status`

### 4.3 触发器依赖

```sql
SELECT event_object_table,
       trigger_name,
       action_statement
FROM information_schema.triggers
WHERE event_object_table = 'indicator';
```

人工确认：

- trigger 不再读写 `distribution_status`

### 4.4 索引依赖

```sql
SELECT schemaname,
       tablename,
       indexname,
       indexdef
FROM pg_indexes
WHERE tablename = 'indicator';
```

人工确认：

- `distribution_status` 不在任何索引定义中

### 4.5 约束依赖

```sql
SELECT conrelid::regclass AS table_name,
       conname AS constraint_name,
       pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE conrelid::regclass::text = 'indicator';
```

人工确认：

- `distribution_status` 不在任何保留约束定义中

## 5. 数据检查

### 5.1 数据量与值分布

执行：

```sql
SELECT COUNT(*) AS total_rows FROM indicator;

SELECT COUNT(*) AS non_null_rows
FROM indicator
WHERE distribution_status IS NOT NULL;

SELECT COALESCE(distribution_status, '<NULL>') AS distribution_status,
       COUNT(*)
FROM indicator
GROUP BY COALESCE(distribution_status, '<NULL>')
ORDER BY 2 DESC, 1;
```

确认点：

- 是否仍有非空历史值
- 是否存在与 `status` 不一致的数据

### 5.2 一致性检查

执行：

```sql
SELECT id, status, distribution_status
FROM indicator
WHERE COALESCE(status::text, '<NULL>') <> COALESCE(distribution_status::text, '<NULL>');
```

确认点：

- 删列前必须明确这些差异是否可以接受
- 若存在依赖历史值的页面或导出，需要先改代码再删列

## 6. 接口行为检查

删列前至少验证以下场景：

1. 创建指标
2. 更新指标
3. 提交审核
4. 审批通过
5. 审批驳回
6. 指标详情查询
7. 指标列表查询

通过标准：

- 不再需要 `distributionStatus` 才能完成上述链路
- 返回结果以 `status` 为准

## 7. 执行前备份

执行删列前必须完成：

1. 整库备份
2. `indicator` 表结构备份
3. `id, status, distribution_status` 数据导出备份

## 8. 删列后验证

删列后立即验证：

1. 主应用编译通过
2. 指标相关接口返回正常
3. 指标列表、详情、审批链路无 SQL 异常
4. 文档与导出结构已同步更新

## 9. 当前检查结论

截至 2026-03-19 启动下线工作时：

- 当前字段仍存在历史数据
- 当前主代码仍有引用
- 当前不满足直接删列条件

因此当前结论是：

- 先做兼容下线
- 暂不直接删库中字段
- 每完成一轮代码清理后，回到本清单重新核查

现阶段补充结论：

- 远程库删列已经完成
- 本清单继续保留，用于说明删列前后的检查过程和证据链

## 10. 第一轮完成情况

- 已完成:
  - 后端 create / update 主链路不再读写 `distributionStatus`
  - 前端主页面与 API 层不再把 `distributionStatus` 当作状态来源
  - `/api/v1/indicators/{id}/distribution-status` 已改为明确停用响应
  - 修改历史已记录到方案文档
- 未完成:
  - API 文档中旧接口说明仍需完全改成下线后口径
  - seed / generator / 历史脚本中的 `distribution_status` 仍未完成分层清理
  - 数据一致性核查尚未执行
  - 数据库删列 migration 尚未执行

## 11. 第二轮完成情况

- 已完成:
  - `indicator.status` 与 `distribution_status` 一致性核查已执行
  - 删列前数据备份与表结构备份已完成
  - 远程库 `indicator.distribution_status` 已删除
  - `indicator_distribution_status_check` 约束已删除
  - 后端停用路由已移除
  - 本地 migration 文件已补充
- 仍待后续处理:
  - 历史脚本、备份文件、归档目录中的 `distribution_status` 分层归档

## 12. 第三轮完成情况

- 已完成:
  - 现行 seed / generator 脚本已去除 `distribution_status`
  - 数据库导出主文档与远程导出文档已移除 `distribution_status` 字段记录
  - API 文档与 OpenAPI/README 已改为 `status` 统一口径
  - OpenAPI 静态导出文件已删除历史下发状态路径
- 有意保留:
  - 下线方案文档中的历史记录
  - 删列检查清单中的核查 SQL 与审计说明
  - `seed_data_v2.sql.backup` 及 `database/scripts/archive/**` 中的历史内容

## 13. 第四轮完成情况

- 已完成:
  - 为本清单补充“当前状态更新”提示，避免将删列前结论误读为当前结论
  - 增加历史资料索引，方便后续区分现行资料与审计留痕
  - 完成一轮本地接口冒烟测试，验证登录正常但指标接口当前仍返回 `500`
- 后续建议:
  - 继续排查本地服务中的指标接口运行时异常
  - 如需彻底收口，再将备份文件与 archive 目录做单独迁移或压缩
