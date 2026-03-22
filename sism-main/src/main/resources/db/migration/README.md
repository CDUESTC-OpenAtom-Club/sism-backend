# Flyway Migration Guide

当前活跃 Flyway 目录：

- `sism-main/src/main/resources/db/migration/`

当前策略：

- 现有数据库结构已经冻结为新的 `V1` 基线
- 基线文件：`V1__baseline_current_schema.sql`
- 后续如需数据库结构变更，从 `V2__*.sql` 开始继续新增

重要原则：

1. 尽量不要改数据库结构，除非客户需求明确要求
2. 不要修改已经生效的基线文件 `V1__baseline_current_schema.sql`
3. 不要把归档目录里的旧迁移重新放回活跃 Flyway 目录
4. 新迁移尽量保持幂等，优先使用 `IF EXISTS` / `IF NOT EXISTS`
5. 结构变更前，先评估是否可以通过应用层或种子数据解决

建议流程：

1. 先确认是不是客户真实需求
2. 如果不是必要需求，尽量不改表结构
3. 如果必须改，新增一个新的 `V2__...` / `V3__...` 文件
4. 先在本地或测试库验证，再执行到远程

备注：

- 种子数据继续维护在 `database/seeds/`
- Flyway 只负责结构变更，不负责业务种子全量导入
