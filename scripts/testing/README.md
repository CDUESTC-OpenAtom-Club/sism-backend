# Testing / Check Scripts

本目录不再存放历史联调、审批演示或一次性 smoke 脚本，当前只保留一个长期有价值的校验工具。

## 当前保留

### `check-flyway-version-collisions.sh`
用于检查 `db/migration` 目录下是否存在重复的 Flyway 版本号，适合在提交迁移脚本前快速执行。

```bash
./scripts/testing/check-flyway-version-collisions.sh
```

## 已移除的脚本类型

以下类型的测试脚本已经清理：

- 依赖历史账号或固定计划 ID 的审批流程脚本
- 与当前接口不一致的联调脚本
- 与 CI 工作流重复、但无人持续维护的本地模拟脚本

## 替代方式

- 审批联调与 smoke：优先参考 `docs/workflow-test-guide.md` 与前端 `PLAN_*` 测试清单
- 常规后端验证：使用 `./mvnw test`
- CI 验证：以 `.github/workflows/ci.yml`、`.github/workflows/deploy.yml` 为准
