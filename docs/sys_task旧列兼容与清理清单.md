# sys_task 旧列收口清单

更新时间：2026-03-20

## 当前结论

- `sys_task` 的业务主字段已经固定为 `name` / `desc`。
- `task_name` / `task_desc` 已不再作为业务字段保留。
- 当前种子最终版也已经恢复为只写 `name` / `desc`。

## 已完成事项

1. 运行时代码确认只读写 `name` / `desc`
   - [StrategicTask.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-task/src/main/java/com/sism/task/domain/StrategicTask.java)
   - [JpaTaskRepositoryInternal.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-task/src/main/java/com/sism/task/infrastructure/persistence/JpaTaskRepositoryInternal.java)
   - [CreateTaskRequest.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-task/src/main/java/com/sism/task/application/dto/CreateTaskRequest.java)
   - [UpdateTaskRequest.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-task/src/main/java/com/sism/task/application/dto/UpdateTaskRequest.java)
   - [TaskResponse.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-task/src/main/java/com/sism/task/application/dto/TaskResponse.java)

2. 种子最终版恢复为只写 `name` / `desc`
   - [sys_task-data.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/seeds/sys_task-data.sql)

3. 历史同步脚本不再继续写旧模型脏数据
   - [sync-task.js](/Users/blackevil/Documents/前端架构测试/sism-backend/scripts/sync/phases/sync-task.js)
   - 现状：
     - 仅保留读取现有 `sys_task` 建立映射的兼容能力
     - 自动新增能力已停用，避免在缺少 `plan_id` 等现代上下文时误写数据

4. 旧校验脚本已标记为历史用途
   - [validate-data.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/scripts/validate-data.sql)
   - 说明：
     - 该脚本仍面向旧结构
     - 不应用于当前 `sys_task` 干净种子链路

## 历史脚本中仍会看到旧列引用

下面这些保留旧列引用是正常的，因为它们属于历史迁移或历史结构工具，不代表当前运行时仍依赖旧列：

- [V49__sync_sys_task_name_desc_columns.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/migrations/V49__sync_sys_task_name_desc_columns.sql)
- [V50__drop_sys_task_task_name_task_desc.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/migrations/V50__drop_sys_task_task_name_task_desc.sql)
- [V1.8__migrate_task_to_strategic_task.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/migrations/V1.8__migrate_task_to_strategic_task.sql)
- [run-v1.8-migration.js](/Users/blackevil/Documents/前端架构测试/sism-backend/database/scripts/run-v1.8-migration.js)
- [V40__fix_task_cycle_mapping.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/migrations/V40__fix_task_cycle_mapping.sql)
- [SysTaskMigration.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-shared-kernel/src/main/java/com/sism/util/SysTaskMigration.java)

建议：

- 这些文件可以保留，但应视为历史工具。
- 后续如继续整理仓库，可统一补“历史迁移专用”说明，或移入更明确的归档目录。

## 名字相近但不是 sys_task 旧列问题

下面这些字段不能和 `sys_task.task_name` 混为一谈：

1. `workflow_task.task_name`
   - [WorkflowTask.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-workflow/src/main/java/com/sism/workflow/domain/runtime/model/WorkflowTask.java)
   - 这是工作流运行时任务表自己的字段。

2. `workflow_task-data.sql`
   - [workflow_task-data.sql](/Users/blackevil/Documents/前端架构测试/sism-backend/database/seeds/workflow_task-data.sql)
   - 这里的 `task_name` 属于 `workflow_task` 表。

3. analytics 领域里的 `task_name`
   - [DataExport.java](/Users/blackevil/Documents/前端架构测试/sism-backend/sism-analytics/src/main/java/com/sism/analytics/domain/DataExport.java)
   - 这是 analytics 模型字段，不是 `sys_task` 旧列。

## 后续建议

1. 为当前 `sys_org / sys_user / cycle / sys_task / indicator_milestone` 主链路补一份新的现结构校验脚本。
2. 若后续开启 Flyway 自动管理，需确认 V49/V50 在数据库中的实际登记策略与历史一致。
3. 后续复查全仓 `task_name|task_desc` 时，应只继续清理真正属于 `sys_task` 历史遗留的引用，不误伤其他独立表。
