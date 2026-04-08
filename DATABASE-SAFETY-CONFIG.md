# 数据库基线说明

**更新日期**: 2026-03-22  
**当前策略**: 以当前数据库结构作为新的 Flyway `V1` 基线

## 当前状态

- 活跃 Flyway 目录: `sism-main/src/main/resources/db/migration/`
- 当前基线文件: `V1__baseline_current_schema.sql`
- 当前真实活跃链中仍包含多个历史追加迁移，例如 `V53`、`V57`、`V60`、`V66`

## 原则

1. 当前已验证数据库结构视为新的 `V1`
2. 活跃迁移只认 `sism-main/src/main/resources/db/migration/`
3. 后续结构变更继续向当前活跃链追加，版本号必须唯一，不要求强行从 `V2` 连续编号
4. 种子数据继续放在 `database/seeds/`
5. `database/scripts/` 中保留的文件主要服务于本地 seed 重置，不得默认视为可直接上线的迁移

## 远程库重建 Flyway 历史

对于已经和当前基线一致的数据库：

1. 先确认目标库是否确实需要重建 Flyway 历史
2. 再以当前活跃 Flyway 目录为准执行 baseline / migrate
3. 确认 `flyway_schema_history` 只记录新的基线起点

## 注意

- 基线文件用于新库建库，不用于老库反复覆盖
- 老库要先重置 Flyway 历史，再让 Flyway 认领新的 `V1`
- 不要把归档目录重新加入 Flyway 扫描路径
- 对约束型迁移，先做数据审计，再执行迁移；不要假设线上数据天然满足约束
