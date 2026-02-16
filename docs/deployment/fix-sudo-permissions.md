# 修复部署工作流 Sudo 权限问题

## 问题描述

GitHub Actions 部署工作流失败，错误信息：
```
sudo: no password was provided
sudo: 1 incorrect password attempt
```

**原因**: SSH 非交互式会话无法读取密码输入，即使通过 echo 传递也不可靠。

## 解决方案：配置 Sudoers 无密码执行

### 方案 1：允许部署用户无密码执行 systemctl（推荐）

在服务器上执行：

```bash
# 1. 创建 sudoers 配置文件
sudo visudo -f /etc/sudoers.d/sism-deploy

# 2. 添加以下内容（替换 <deploy-user> 为实际用户名）
<deploy-user> ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart sism-backend
<deploy-user> ALL=(ALL) NOPASSWD: /usr/bin/systemctl status sism-backend
<deploy-user> ALL=(ALL) NOPASSWD: /usr/bin/systemctl is-active sism-backend
<deploy-user> ALL=(ALL) NOPASSWD: /usr/bin/journalctl -u sism-backend*
<deploy-user> ALL=(ALL) NOPASSWD: /usr/bin/ln -sf /opt/sism/backend/* /opt/sism/backend/sism-backend.jar

# 3. 设置正确的权限
sudo chmod 0440 /etc/sudoers.d/sism-deploy

# 4. 验证配置
sudo visudo -c
```

### 方案 2：使用 systemd 用户服务（更安全）

将服务改为用户级服务，不需要 sudo：

```bash
# 1. 创建用户服务目录
mkdir -p ~/.config/systemd/user/

# 2. 移动服务文件
sudo cp /etc/systemd/system/sism-backend.service ~/.config/systemd/user/
sudo chown $USER:$USER ~/.config/systemd/user/sism-backend.service

# 3. 修改服务文件，移除 User= 行

# 4. 重新加载并启用
systemctl --user daemon-reload
systemctl --user enable sism-backend
systemctl --user start sism-backend

# 5. 启用用户服务在登录时自动启动
sudo loginctl enable-linger $USER
```

然后修改部署脚本使用 `systemctl --user` 而不是 `sudo systemctl`。

## 快速修复：更新部署脚本

如果暂时无法修改服务器配置，可以先修改部署脚本使用 sudo -S：

### 更新 deploy.yml

```yaml
- name: Deploy and restart service
  run: |
    ssh -i ~/.ssh/deploy_key ${{ secrets.SERVER_USER }}@${{ secrets.SERVER_HOST }} << 'EOF'
      echo "🚀 Deploying and restarting service..."
      echo "${{ secrets.SERVER_SUDO_PASSWORD }}" | sudo -S ln -sf /opt/sism/backend/${{ env.JAR_NAME }} /opt/sism/backend/sism-backend.jar
      echo "${{ secrets.SERVER_SUDO_PASSWORD }}" | sudo -S systemctl restart sism-backend
      sleep 30
      systemctl is-active sism-backend && echo "✅ Service is running" || echo "❌ Service failed"
    EOF
```

但这种方式不够可靠，建议使用方案 1。

## 推荐实施步骤

### 第一步：在服务器上配置 sudoers

```bash
# SSH 到服务器
ssh root@175.24.139.148

# 创建 sudoers 配置（假设部署用户是 sism）
cat > /etc/sudoers.d/sism-deploy << 'EOL'
# SISM Backend Deployment - Allow specific systemctl commands without password
sism ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart sism-backend
sism ALL=(ALL) NOPASSWD: /usr/bin/systemctl status sism-backend
sism ALL=(ALL) NOPASSWD: /usr/bin/systemctl is-active sism-backend
sism ALL=(ALL) NOPASSWD: /usr/bin/journalctl -u sism-backend*
sism ALL=(ALL) NOPASSWD: /usr/bin/ln -sf /opt/sism/backend/* /opt/sism/backend/sism-backend.jar
EOL

# 设置权限
chmod 0440 /etc/sudoers.d/sism-deploy

# 验证
visudo -c

# 测试（切换到部署用户）
su - sism
sudo systemctl status sism-backend  # 应该不需要密码
```

### 第二步：简化部署脚本

创建新的无密码部署脚本：

```bash
#!/bin/bash
# deploy-and-restart-nopasswd.sh
set -e

JAR_NAME="${1:-sism-backend-1.0.0.jar}"
SISM_HOME="/opt/sism"

echo "🔗 Updating symlink..."
sudo ln -sf "$SISM_HOME/backend/$JAR_NAME" "$SISM_HOME/backend/sism-backend.jar"

echo "🔄 Restarting service..."
sudo systemctl restart sism-backend

echo "⏳ Waiting 30 seconds..."
sleep 30

echo "🔍 Checking status..."
if sudo systemctl is-active sism-backend > /dev/null; then
    echo "✅ Service is running"
    curl -s http://localhost:8080/api/actuator/health || echo "Health check pending..."
    exit 0
else
    echo "❌ Service failed"
    sudo systemctl status sism-backend --no-pager
    sudo journalctl -u sism-backend -n 50 --no-pager
    exit 1
fi
```

### 第三步：更新 GitHub Actions 工作流

```yaml
- name: Deploy and restart service
  run: |
    ssh -i ~/.ssh/deploy_key ${{ secrets.SERVER_USER }}@${{ secrets.SERVER_HOST }} \
      "~/deploy-and-restart-nopasswd.sh ${{ env.JAR_NAME }}"
```

## 验证

配置完成后，测试部署：

```bash
# 在服务器上手动测试
~/deploy-and-restart-nopasswd.sh sism-backend-1.0.0.jar

# 或触发 GitHub Actions
git commit --allow-empty -m "test: 触发部署测试"
git push
```

## 安全考虑

1. **最小权限原则**: 只允许特定命令，不是完全的 NOPASSWD
2. **命令白名单**: 明确指定可执行的命令路径
3. **审计日志**: systemd 和 journalctl 会记录所有操作
4. **文件权限**: sudoers 文件权限必须是 0440

## 当前状态

- ✅ 服务实际上已经启动（从日志看 Active: active (running)）
- ❌ 部署脚本因为密码问题退出失败
- ✅ 生产环境正常运行

**结论**: 服务已经成功部署和重启，只是脚本报告失败。配置 sudoers 后，下次部署将完全自动化。
