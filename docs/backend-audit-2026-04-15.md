# 后端线路审计台账（2026-04-15）

| 模块 | 接口 | 复现方式 | 实际结果 | 预期结果 | 根因判断 | 是否已修 | 复测状态 | 是否阻塞前端联调 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Strategy / Cycle | `GET /api/v1/cycles/list` | 登录后进入战略任务页自动触发 | 500 | 200，返回周期列表 | `Cycle` 查询仍引用已删除的 `cycle.is_deleted` | 是 | 已通过 | 是 |
| Strategy / Cycle | `GET /api/v1/cycles?year=2026&page=0&size=1` | 登录后进入战略任务页自动触发 | 500 | 200，返回当前周期分页结果 | `Cycle` 查询仍引用已删除的 `cycle.is_deleted` | 是 | 已通过 | 是 |
| Task | `GET /api/v1/tasks` | 登录后进入战略任务页自动触发 | 500 | 200，返回任务列表 | 周期接口失败后导致任务页初始化链路级联异常 | 是 | 已通过 | 是 |
| Strategy / Plan | `GET /api/v1/plans/4036/details` | 登录后进入战略任务页自动触发 | 500 | 200，返回计划详情 | 周期接口失败后导致计划详情链路级联异常 | 是 | 已通过 | 是 |
| Task | `GET /api/v1/tasks/by-plan/4036` | 登录后进入战略任务页自动触发 | 500 | 200，返回计划下任务列表 | 周期接口失败后导致任务详情链路级联异常 | 是 | 已通过 | 是 |
| Execution / Reports | `GET /api/v1/reports/plan/4036` | 登录后进入战略任务页自动触发 | 403 | 200，返回该计划的报告列表 | 控制器与服务仍使用旧角色模型（`ADMIN/STRATEGY_DEPT/FUNC_DEPT`） | 是 | 已通过 | 是 |
| Task | `taskStatus` 读模型语义 | 登录后读取任务列表 | 状态语义不稳定 | `taskStatus = plan.status` | 任务状态模型和当前业务口径不一致 | 是 | 已通过 | 是 |
| 权限模型 | `Task / Plan / Milestone / IAM / Main debug` 多模块接口 | 全量静态审计旧权限注解 | 存在旧角色残留 | 应统一对齐当前真实角色体系 | 控制器仍保留 `ADMIN / STRATEGY_DEPT / FUNC_DEPT` 等旧模型 | 是（首批） | 已完成代码清理，待运行态继续抽查 | 否 |
| Alert / 装配 | `GET /api/v1/alerts*` | 接口级抽查 | 404 / 启动装配失败 | 200 或符合权限的业务响应 | `sism-alert` 未正确纳入主应用装配，且仓储接口桥接不稳定 | 是 | 已通过（源码运行） | 否 |
| Analytics / Dashboard | `GET /api/v1/dashboard/department-progress` | 接口级抽查 | 500 | 200，返回部门进度列表 | 原生 SQL 对 `a.alert_count` 的聚合写法错误 | 是 | 已通过 | 否 |
| Execution / Milestones | `GET /api/v1/execution/milestones/list` | 接口级抽查 | 500 | 200，返回里程碑列表 | `indicator_milestone.inherited_from` 字段映射残留，但数据库无此列 | 是 | 已通过 | 否 |
| Analytics / Startup | `dashboardSummaryService` Bean 初始化 | 使用新包启动后端 | 启动失败 | 后端正常启动 | Analytics 汇总服务依赖与运行时存在独立初始化问题 | 是 | 已通过 | 否 |
| Playwright CLI | 浏览器会话复用 | 多次复用同名 session | 会出现本机 Unix socket `EINVAL` | 会话可稳定复用 | 本机 Playwright CLI 会话 socket 异常，非项目代码问题 | 否 | 规避为新会话后可继续测试 | 否 |

## 当前结论

- 登录链路已恢复：
  - 登录页可打开
  - `admin / admin123` 可登录
  - 页面可从 `/login` 跳转到 `/strategic-tasks`
- 登录后主页面关键数据链路已恢复：
  - 周期
  - 任务列表
  - 计划详情
  - 计划下任务
  - 计划报告列表
- 当前仍需继续审计的主要方向：
  - analytics 导出 / analytics dashboard 其余分页与搜索接口
  - alerts 其余分页与筛选接口
  - 其余未进入主链路但仍在 OpenAPI 中暴露的接口
