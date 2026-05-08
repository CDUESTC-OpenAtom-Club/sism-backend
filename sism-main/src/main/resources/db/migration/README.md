# Flyway Migration Guide

当前活跃 Flyway 目录：

- `sism-main/src/main/resources/db/migration/`
- `sism-analytics/src/main/resources/db/migration/`

当前策略：

- 现有数据库结构已经冻结为新的 `V1` 基线
- 基线文件：`V1__baseline_current_schema.sql`
- 后续如需数据库结构变更，从 `V2__*.sql` 开始继续新增
- 历史 `V1.x` 迁移已经归档到 `db/migration-archive/legacy-pre-container/`

重要原则：

1. 尽量不要改数据库结构，除非客户需求明确要求
2. 不要修改已经生效的基线文件 `V1__baseline_current_schema.sql`
3. 不要把归档目录里的旧迁移重新放回活跃 Flyway 目录
4. 新迁移尽量保持幂等，优先使用 `IF EXISTS` / `IF NOT EXISTS`
5. 结构变更前，先评估是否可以通过应用层或种子数据解决
6. 对约束型迁移，先做数据审计，再执行迁移；不要假设线上数据天然满足约束
7. 如需让老库重建 Flyway 历史，先确认目标库结构已与当前基线一致，再执行 baseline / migrate
8. 本地手工执行 Flyway 时，必须同时包含 `sism-main` 与 `sism-analytics` 的活跃迁移目录；否则会漏掉 `progress_report` 的后续结构变更

建议流程：

1. 先确认是不是客户真实需求
2. 如果不是必要需求，尽量不改表结构
3. 如果必须改，新增一个新的 `V2__...` / `V3__...` 文件
4. 先在本地或测试库验证，再执行到远程
5. 如果涉及重建 Flyway 历史，确认 `flyway_schema_history` 只从新的基线起点开始记录
6. 空库初始化必须能直接从 `V1` + 活跃 `V2+` 迁移链完成启动

备注：

- 种子数据继续维护在 `database/seeds/`
- Flyway 只负责结构变更，不负责业务种子全量导入
- `db/migration-archive/` 只作为历史追溯材料保留，不参与当前 Flyway 执行
