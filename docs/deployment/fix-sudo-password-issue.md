# 修复部署工作流 Sudo 密码问题

**问题**: GitHub Actions 部署失败，提示 `sudo: a password is required`

**原因**: 部署脚本需要 sudo 权限但服务器未配置 NOPASSWD

---

## 快速修复（3 步）

### 步骤 1: SSH 到服务器

```bash
ssh -i ~/.ssh/your_key your_user@your_server
```

### 步骤 2: 下载并运行配置脚本

```bash
# 下载配置脚本（如果还没有）
# 或者从 GitHub 仓库获取

# 运行配置脚本
sudo ./configure-server-for-ci.sh <deploy-username>
```

**示例**:
```bash
sudo ./configure-server-for-ci.sh github-deploy
```

### 步骤 3: 重新运行 GitHub Actions 工作流

在 GitHub 仓库中：
1. 进入 Actions 标签
2. 选择失败的工作流
3. 点击 "Re-run jobs"

---

## 配置脚本做了什么

配置脚本 (`configure-server-for-ci.sh`) 会：

1. **创建 sudoers 配置** (`/etc/sudoers.d/sism-deploy`)
   - 允许部署用户无密码执行 systemctl 命令
   - 允许查看服务日志
   - 允许更新符号链接

2. **设置目录权限**
   - 创建 `sism` 用户组
   - 将部署用户添加到 sism 组
   - 设置 `/opt/sism/backend/` 权限为 775

3. **验证配置**
   - 测试 sudo 访问
   - 测试目录写入权限

---

## 手动配置（如果脚本不可用）

### 1. 创建 sudoers 文件

```bash
sudo visudo -f /etc/sudoers.d/sism-deploy
```

添加以下内容（替换 `<deploy-user>` 为实际用户名）:

```
# SISM Backend Deployment - Passwordless sudo
<deploy-user> ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart sism-backend
<deploy-user> ALL=(ALL) NOPASSWD: /usr/bin/systemctl status sism-backend
<deploy-user> ALL=(ALL) NOPASSWD: /usr/bin/systemctl is-active sism-backend
<deploy-user> ALL=(ALL) NOPASSWD: /usr/bin/systemctl stop sism-backend
<deploy-user> ALL=(ALL) NOPASSWD: /usr/bin/systemctl start sism-backend
<deploy-user> ALL=(ALL) NOPASSWD: /usr/bin/journalctl -u sism-backend*
<deploy-user> ALL=(ALL) NOPASSWD: /usr/bin/ln -sf /opt/sism/backend/* /opt/sism/backend/sism-backend.jar
```

保存并退出 (Ctrl+X, Y, Enter)

### 2. 设置目录权限

```bash
# 创建 sism 组
sudo groupadd sism

# 添加部署用户到 sism 组
sudo usermod -aG sism <deploy-user>

# 设置目录权限
sudo chown -R root:sism /opt/sism
sudo chmod -R 775 /opt/sism/backend
```

### 3. 验证配置

```bash
# 测试 sudo 访问（应该不要求密码）
sudo systemctl status sism-backend

# 测试目录写入
touch /opt/sism/backend/.test && rm /opt/sism/backend/.test
```

---

## 验证部署工作流

### 测试 1: 检查 sudoers 配置

```bash
# 作为部署用户运行
sudo -n systemctl status sism-backend
```

**预期**: 显示服务状态，不要求密码

### 测试 2: 检查目录权限

```bash
# 作为部署用户运行
ls -la /opt/sism/backend/
touch /opt/sism/backend/.test && rm /opt/sism/backend/.test
```

**预期**: 可以创建和删除文件

### 测试 3: 运行部署脚本

```bash
# 作为部署用户运行
~/deploy-and-restart.sh sism-backend-1.0.0.jar
```

**预期**: 脚本成功执行，服务重启

---

## 故障排查

### 问题: sudoers 配置无效

**症状**: 仍然要求密码

**解决方案**:
```bash
# 检查 sudoers 语法
sudo visudo -c -f /etc/sudoers.d/sism-deploy

# 检查文件权限
ls -la /etc/sudoers.d/sism-deploy
# 应该是: -r--r----- (0440)

# 修复权限
sudo chmod 0440 /etc/sudoers.d/sism-deploy
```

### 问题: 目录权限被拒绝

**症状**: `Permission denied` 写入 `/opt/sism/backend/`

**解决方案**:
```bash
# 检查当前权限
ls -la /opt/sism/

# 确认用户在 sism 组中
groups <deploy-user>

# 重新设置权限
sudo chown -R root:sism /opt/sism
sudo chmod -R 775 /opt/sism/backend

# 重新登录以应用组更改
exit
ssh -i ~/.ssh/your_key your_user@your_server
```

### 问题: 服务未重启

**症状**: 健康检查失败

**解决方案**:
```bash
# 检查服务状态
sudo systemctl status sism-backend

# 查看服务日志
sudo journalctl -u sism-backend -n 100

# 手动重启
sudo systemctl restart sism-backend

# 检查健康端点
curl http://localhost:8080/api/actuator/health
```

---

## 安全注意事项

### ✅ 安全的做法

1. **限制 sudo 命令**: 只允许特定的 systemctl 命令
2. **使用专用用户**: 创建专门的部署用户
3. **最小权限**: 只授予必要的权限
4. **审计日志**: 所有 sudo 命令都会记录在系统日志中

### ❌ 不安全的做法

1. **不要使用**: `<user> ALL=(ALL) NOPASSWD: ALL`
2. **不要共享**: 不要使用个人用户账户进行部署
3. **不要暴露**: 不要在 GitHub Secrets 中存储密码

---

## 相关文件

- **部署脚本**: `scripts/deployment/deploy-and-restart-nosudo.sh`
- **配置脚本**: `scripts/deployment/configure-server-for-ci.sh`
- **Sudoers 设置**: `scripts/deployment/setup-sudoers.sh`
- **工作流**: `.github/workflows/deploy.yml`

---

## 总结

1. ✅ 在服务器上运行 `configure-server-for-ci.sh`
2. ✅ 验证 sudoers 配置和目录权限
3. ✅ 重新运行 GitHub Actions 工作流
4. ✅ 监控部署日志确认成功

**配置完成后，部署工作流将自动运行，无需密码。**
