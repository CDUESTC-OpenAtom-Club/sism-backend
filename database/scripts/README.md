# Database Scripts Guide

`database/scripts/` 不是当前 Flyway 活跃迁移链的一部分。

这个目录目前只保留本地 clean seed 重置链所需的最小脚本集。

高风险提醒：

- `reset-clean-seeds.sh`

该脚本会重置并重新导入本地 seed 数据，只能用于本地或明确批准的测试库。

使用原则：

- 不要把这里的脚本当成可直接上线的正式迁移
- 任何会改数据的脚本都应先确认目标环境，默认只建议在本地或受控测试环境执行
- 如需把某项修复纳入正式发布流程，应把它转成 `sism-main/src/main/resources/db/migration/` 下的 Flyway 迁移

推荐执行顺序：

1. 优先看活跃 Flyway 迁移目录
2. 再看 `DATABASE-SAFETY-CONFIG.md`
3. 最后才评估是否需要执行本目录脚本
