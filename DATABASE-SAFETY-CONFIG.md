# 数据库基线说明

**更新日期**: 2026-03-22  
**当前策略**: 以当前数据库结构作为新的 Flyway `V1` 基线

## 当前状态

- 活跃 Flyway 目录: `sism-main/src/main/resources/db/migration/`
- 当前基线文件: `V1__baseline_current_schema.sql`
- 旧迁移已归档到: `database/migrations-archive/legacy-pre-baseline-20260322/`
- `database/migrations/` 仅保留说明，不再承载活跃迁移链

## 原则

1. 不再继续沿用历史 `V2 ~ V53` 迁移链
2. 当前已验证数据库结构视为新的 `V1`
3. 后续所有结构变更从 `V2__*.sql` 继续追加
4. 种子数据继续放在 `database/seeds/`

## 远程库重建 Flyway 历史

对于已经和当前基线一致的数据库：

1. 执行 `database/scripts/rebaseline-flyway-history-to-v1.sql`
2. 再执行 Flyway baseline
3. 确认 `flyway_schema_history` 只记录新的基线起点

## 注意

- 基线文件用于新库建库，不用于老库反复覆盖
- 老库要先重置 Flyway 历史，再让 Flyway 认领新的 `V1`
- 不要把归档目录重新加入 Flyway 扫描路径
