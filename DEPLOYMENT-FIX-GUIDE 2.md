# 部署修复指南

## 问题说明

GitHub Actions self-hosted runner 遇到 Git 权限问题，导致 `actions/checkout@v4` 失败。

错误信息：
```
error: insufficient permission for adding an object to repository database .git/objects
fatal: failed to write object
fatal: unpack-objects failed
```

## 修复步骤

### 1. SSH 登录到服务器

```bash
ssh root@175.24.139.148
```

### 2. 执行权限修复脚本

```bash
# 进入后端工作目录
cd /opt/sism/actions-runner/_work/sism-backend/sism-backend

# 拉取最新代码（包含修复脚本）
git fetch origin main
git reset --hard origin/main

# 执行修复脚本
sudo bash scripts/fix-runner-git-permissions.sh
```

### 3. 重启 Runner 服务

```bash
# 方法 1: 使用 systemctl
sudo systemctl restart actions.runner.*.service

# 方法 2: 手动重启
cd /opt/sism/actions-runner
sudo -u sism ./svc.sh stop
sudo -u sism ./svc.sh start
```

### 4. 验证修复

```bash
# 检查目录权限
ls -ld /opt/sism/actions-runner/_work/sism-backend/sism-backend
ls -ld /opt/sism/actions-runner/_work/sism-backend/sism-backend/.git

# 应该显示所有者为 sism:sism
```

## 本地测试通过

✅ Maven 构建成功：
```
[INFO] BUILD SUCCESS
[INFO] Total time:  50.346 s
[INFO] Finished at: 2026-03-05T15:15:51+08:00
```

✅ JAR 文件生成：
```
target/sism-backend-1.0.0.jar
```

## 推送计划

修复完成后，将推送以下提交：

1. `fix(ci)`: 修复 GitHub Actions self-hosted runner Git 权限问题
   - 添加 workflow 自动修复步骤
   - 提供服务器端修复脚本
   - 生成 CHANGELOG.md

2. `chore`: 添加本地 CI/CD 测试脚本
   - scripts/test-ci-locally.sh

3. `feat(approval)`: 两级主管审批流程完整实现（后端）
   - 完整的审批流程功能
   - 测试指南文档

## 相关文档

- `scripts/fix-runner-git-permissions.sh` - 服务器端权限修复脚本
- `scripts/test-ci-locally.sh` - 本地 CI/CD 测试脚本
- `.github/workflows/deploy.yml` - 已添加自动权限修复步骤

## 注意事项

1. 修复脚本需要 root 权限执行
2. 修复后需要重启 runner 服务
3. 确认 runner 用户为 `sism`
4. 如果问题持续，可能需要完全重置工作目录

## 联系方式

如有问题，请查看详细文档或联系开发团队。
