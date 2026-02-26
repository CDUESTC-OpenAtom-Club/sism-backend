# 更新 GitHub Secrets 配置

## 需要的 Secrets

### 1. SERVER_SSH_KEY（必须更新）
- **用途**：SSH 私钥，用于连接服务器
- **操作**：更新为新生成的密钥

访问：https://github.com/CDUESTC-OpenAtom-Club/sism-backend/settings/secrets/actions

点击 `SERVER_SSH_KEY` 右侧的 "Update"，将下面的私钥内容完整复制粘贴：

```
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtz
c2gtZW
QyNTUxOQAAACDpK7fFcJkr/B4zyN48/o7vs3lQ6N+GMkuz2ftqOqugzAAAAKB53F
71edxe
9QAAAAtzc2gtZWQyNTUxOQAAACDpK7fFcJkr/B4zyN48/o7vs3lQ6N+GMkuz2ftq
OqugzA
AAAEDwZteidTz1Jdz4R7FUNkuHrmdSjxtRCmvSn6+d/s70vOkrt8VwmSv8HjPI3j
z+ju+z
eVDo34YyS7PZ+2o6q6DMAAAAGWdpdGh1Yi1hY3Rpb25zLWRlcGxveS1uZXcBAgME
-----END OPENSSH PRIVATE KEY-----
```

## 需要删除的 Secrets

### 2. SERVER_SUDO_PASSWORD（可以删除）
- **原因**：现在使用 root 用户直接操作，不需要 sudo 密码
- **操作**：在 Secrets 页面找到并删除

### 3. SERVER_USER（可以删除）
- **原因**：已在工作流中硬编码为 `root`
- **操作**：在 Secrets 页面找到并删除

### 4. SERVER_HOST（可以删除）
- **原因**：已在工作流中硬编码为 `175.24.139.148`
- **操作**：在 Secrets 页面找到并删除

## 最终配置

更新后，GitHub Secrets 中只需要保留：
- ✅ `SERVER_SSH_KEY` - SSH 私钥（已更新）

可以删除：
- ❌ `SERVER_SUDO_PASSWORD` - 不再需要
- ❌ `SERVER_USER` - 已硬编码在工作流
- ❌ `SERVER_HOST` - 已硬编码在工作流

## 验证

更新 `SERVER_SSH_KEY` 后，重新运行 GitHub Actions 工作流测试部署。

## 对应的公钥（已添加到服务器）
```
ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOkrt8VwmSv8HjPI3jz+ju+zeVDo34YyS7PZ+2o6q6DM github-actions-deploy-new
```

位置：`/root/.ssh/authorized_keys`
