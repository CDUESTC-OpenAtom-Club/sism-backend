# 2026-05 主线自动部署治理与故障复盘

## 1. 背景

本次治理目标是把 SISM 的交付方式统一为：

- 前后端以 `main` 作为唯一自动部署入口
- `sism-backend` 作为整套系统的系统级部署入口
- `baseline/*` 继续保留为开发基线，但不再直接承担自动部署职责

目标服务器：

- `47.108.249.115`

## 2. 初始问题

### 2.1 分支与部署入口混乱

治理前存在以下问题：

- 前后端 workflow 同时响应 `baseline/*` 和 `main`
- 前端成功构建后，会派发后端 `baseline` 的部署，而不是后端 `main`
- `main` 已经是系统发布主线，但自动化仍残留旧阶段基线逻辑

### 2.2 后端部署健康校验误判

后端容器重建后，workflow 立即单次访问：

- `http://127.0.0.1:8080/api/v1/actuator/health`

由于后端健康检查自身配置了启动宽限期，导致真实服务已在正常启动，但 workflow 过早判定为失败。

### 2.3 测试服务器数据库为空

修复主线部署后，页面可访问，但登录失败。排查结果：

- `sys_user = 0`
- `sys_org = 0`

即，当前测试服务器数据库只有结构，没有 clean seed 数据，因此测试账号不存在。

### 2.4 后端旧部署 run 卡死

后端历史运行 `25596177124` 卡在服务器侧：

- `podman pull ghcr.io/cduestc-openatom-club/sism-backend:2013173...`

表现为：

- GitHub Actions 中该 run 长时间 `in_progress`
- 新的后端 `main` 部署 run 因并发锁而 `pending`
- 服务器端存在残留 `run-remote-deploy.sh` 和 `podman pull` 进程

## 3. 已执行修复

### 3.1 主线部署入口统一

前后端 workflow 调整为：

- 只有 `main` 自动触发正式部署链路
- 前端 `main` 构建成功后，派发后端仓库 `main` 的 `frontend-only` 部署
- 后端 `main` 构建成功后，执行系统级部署

对应提交：

- 前端：`69e8af7` `ci: restrict frontend deploy automation to main`
- 后端：`c3fd9cb` `ci: restrict backend deploy automation to main`

### 3.2 健康校验增加等待重试

后端部署 workflow 的健康检查改为重试等待，而不是单次立即失败。

对应提交：

- `eb8a43f` `ci: wait for service readiness during backend deploy verification`

### 3.3 空库自动补 clean seeds

在后端部署 workflow 中加入空库探测与 clean seed 引导：

- 自动上传 clean seed bundle
- 若检测到 `sys_user=0` 且 `sys_org=0`
  - 自动执行 `bootstrap-local-seed-support.sql`
  - 自动执行 `reset-and-load-clean-seeds.sql`
  - 自动执行 `validate-clean-seeds.sql`

对应提交：

- `2013173` `ci: bootstrap clean seeds on empty deployment databases`
- `1de680e` `ci: load clean seeds from postgres container bundle`

### 3.4 旧部署卡死解法

后端部署 workflow 加入以下保护：

- 部署前清理旧的 `run-remote-deploy.sh` 进程
- 部署前清理旧的 `podman pull` 进程
- `podman pull` 增加 `timeout` 和重试
- 使用更强的后台脱离方式启动远端部署脚本

对应提交：

- `2a60e5a` `ci: timeout and replace stale backend deploy runs`

## 4. 实机排查关键发现

### 4.1 LoginRequest 不是根因

线上代码中：

- `LoginRequest.account`
- `@JsonAlias(\"username\")`

说明前端发送：

```json
{"account":"admin","password":"admin123"}
```

是可以被后端正确绑定的。

### 4.2 日志真实报错不是“参数不合法”

页面提示为“请求参数不合法”，但服务器日志显示真实业务异常为：

- `用户名或密码错误`

进一步核对数据库发现：

- 不是密码比对算法有误
- 而是数据库中根本没有测试账号

### 4.3 手工补种子后登录恢复

手工导入 clean seeds 后，服务器恢复到：

- `sys_user = 104`
- `sys_org = 28`

随后实测成功：

- 接口登录：`admin / admin123`
- 页面登录：从 `/login` 成功跳转到 `/strategic-tasks`

## 5. 当前稳定状态

截至本文档生成时，以下能力已被证明成立：

### 5.1 已验证通过

- 前端 `main` 自动构建、自动派发后端 `frontend-only` 部署
- 后端 `main` 自动 CI、自动构建、自动系统部署
- 后端健康检查等待逻辑已修复
- 空库测试环境可通过 clean seeds 恢复测试账号体系
- 登录链路已恢复可用

### 5.2 服务器业务状态

服务器当前应满足：

- 前端可访问
- 后端 `/api/v1/actuator/health` 返回 `UP`
- `sys_user` 非空
- `admin / admin123` 可登录

## 6. 根因总结

本次并不是单点故障，而是多个部署层问题叠加：

1. 自动部署入口未完全从 `baseline/*` 收束到 `main`
2. 健康校验比应用真实 readiness 更早
3. 测试服务器默认空库，自动部署未补测试种子
4. 旧远端部署进程缺乏超时与替换机制，导致新 run 被并发锁卡住

## 7. 工程经验

### 7.1 不要把“服务启动成功”和“业务环境可用”等同

本次后端容器健康，但登录失败，说明：

- 结构迁移成功
- 不等于测试环境就具备业务账号与验证数据

### 7.2 自动部署必须定义环境基线

测试环境若依赖固定业务账号、固定审批链、固定样例数据，就不能只部署 schema 和镜像，必须明确：

- 是否自动补种子
- 在什么条件下补种子
- 如何防止误覆盖已有业务数据

### 7.3 远端后台部署必须防“僵尸进程”

若远端部署脚本没有超时、替换、清理机制，一次卡住会造成：

- 当前 run 不结束
- 后续 run 一直 pending
- 看上去像“GitHub 卡住”，其实是远端部署进程没被治理

## 8. 后续建议

### 8.1 非阻塞改进

- 升级 GitHub Actions 中仍在 Node 20 上运行的 action 版本
- 给后端部署增加“当前服务器数据库是否为空”的显式日志标识
- 给部署成功消息增加：
  - 后端镜像 tag
  - 前端镜像 tag
  - `sys_user` 数量
  - `flyway_schema_history` 最新版本

### 8.2 运维建议

- 若再次出现部署长期 `pending`
  - 先看是否有旧 `in_progress` run 占着并发组
  - 再看服务器是否有残留 `run-remote-deploy.sh` 或 `podman pull`
- 若再次出现“页面在线但无法登录”
  - 第一优先级先检查 `sys_user` 和 `sys_org` 是否为空

## 9. 关联文档

- `docs/architecture/main-branch-release-and-deploy-runbook.md`
  - 当前主线发布、自动部署、分支职责与故障排查手册
