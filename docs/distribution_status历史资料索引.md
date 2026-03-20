# `distribution_status` 历史资料索引

## 1. 目的

这份索引用来区分三类资料：

- 现行资料：应与当前代码和数据库事实保持一致
- 审计留痕：保留排查、决策、删列、验证过程
- 备份归档：仅供回溯，不应作为当前实现依据

## 2. 当前结论

截至 2026-03-19：

- 远程库 `public.indicator.distribution_status` 已删除
- 现行前后端主链路已统一使用 `status`
- 现行 seed、generator、导出文档、OpenAPI 静态文件已完成清理
- 当前仓库中关于 `distribution_status` 的剩余命中，主要集中在审计留痕和备份归档层

## 3. 现行资料

以下文件应当被当作“当前口径”使用：

- [API接口文档.md](/Users/blackevil/Documents/前端架构测试/sism-backend/docs/API接口文档.md)
- [strategic-db-tables-and-columns.md](/Users/blackevil/Documents/前端架构测试/sism-backend/docs/db-export/strategic-db-tables-and-columns.md)
- [strategic-db-tables-and-columns-2026-03-19-remote.md](/Users/blackevil/Documents/前端架构测试/sism-backend/docs/db-export/strategic-db-tables-and-columns-2026-03-19-remote.md)
- [strategic-db-columns-with-comments.tsv](/Users/blackevil/Documents/前端架构测试/sism-backend/docs/db-export/strategic-db-columns-with-comments.tsv)
- [strategic-db-columns-with-comments-2026-03-19-remote.csv](/Users/blackevil/Documents/前端架构测试/sism-backend/docs/db-export/strategic-db-columns-with-comments-2026-03-19-remote.csv)
- [openapi.json](/Users/blackevil/Documents/前端架构测试/sism-backend/openapi.json)
- [openapi-latest.json](/Users/blackevil/Documents/前端架构测试/sism-backend/openapi-latest.json)
- [openapi-full.json](/Users/blackevil/Documents/前端架构测试/sism-backend/docs/openapi/openapi-full.json)
- [seed_data_v2.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/seeds/seed_data_v2.sql)
- [seed_data_v3_review.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/seeds/seed_data_v3_review.sql)
- [generate-2025-indicators.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/scripts/generate-2025-indicators.sql)
- [fill_2026_functional_department_linked_data.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/scripts/fill_2026_functional_department_linked_data.sql)

## 4. 审计留痕

以下文件保留历史过程，允许继续出现 `distribution_status`：

- [distribution_status兼容下线方案.md](/Users/blackevil/Documents/前端架构测试/sism-backend/docs/distribution_status兼容下线方案.md)
- [distribution_status删列执行前检查清单.md](/Users/blackevil/Documents/前端架构测试/sism-backend/docs/distribution_status删列执行前检查清单.md)
- [V34__consolidate_indicator_status.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/migrations/V34__consolidate_indicator_status.sql)
- [V44__drop_indicator_distribution_status.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/migrations/V44__drop_indicator_distribution_status.sql)

这些文件的作用是提供证据链，不应拿来判断“当前数据库是否还有该字段”。

## 5. 备份归档

以下文件和目录属于历史备份或归档：

- [seed_data_v2.sql.backup](/Users/blackevil/Documents/前端架构测试/sism-backend/database/seeds/seed_data_v2.sql.backup)
- [archive](/Users/blackevil/Documents/前端架构测试/sism-backend/database/scripts/archive)

使用规则：

- 可用于追溯和对比
- 不应用作当前执行脚本
- 如需进一步降噪，可在后续单独迁移到统一归档目录

## 6. 当前接口测试摘要

2026-03-19 本地接口冒烟测试结果：

- `POST /api/v1/auth/login` 正常
- 指标相关接口当前仍存在运行时异常：
  - `GET /api/v1/indicators`
  - `GET /api/v1/indicators/{id}`
  - `POST /api/v1/indicators`

因此现阶段可以确认：

- `distribution_status` 下线与文档整理已完成
- 当前指标接口仍有独立的运行问题，需要单独排查，不应再归因到旧字段未删除
