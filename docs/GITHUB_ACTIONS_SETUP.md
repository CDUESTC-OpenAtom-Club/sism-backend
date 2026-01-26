# GitHub Actions 自动部署配置指南

## 概述

当代码推送到 `main` 分支时，GitHub Actions 会自动：
1. 构建后端 JAR 包
2. 构建前端静态资源
3. 通过 SSH 部署到服务器
4. 重启服务并验证健康状态

## 配置步骤

### 1. 服务器准备

#### 1.1 创建部署用户（在服务器上执行）

```bash
# 创建部署用户
sudo useradd -m -s /bin/bash deploy

# 允许 deploy 用户执行特定 sudo 命令（无需密码）
sudo visudo
# 添加以下行：
# deploy ALL=(ALL) NOPASSWD: /bin/systemctl restart sism-backend, /bin/systemctl status sism-backend, /bin/systemctl reload nginx, /usr/sbin/nginx -t, /bin/chown -R www-data\:www-data /var/www/sism, /bin/ln -sf /opt/sism/backend/sism-backend-1.0.0.jar /opt/sism/backend/sism-backend.jar
```

#### 1.2 生成 SSH 密钥对（在本地执行）

```bash
# 生成专用于部署的 SSH 密钥
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/sism_deploy_key -N ""

# 查看私钥（需要添加到 GitHub Secrets）
cat ~/.ssh/sism_deploy_key

# 查看公钥（需要添加到服务器）
cat ~/.ssh/sism_deploy_key.pub
```

#### 1.3 配置服务器 SSH（在服务器上执行）

```bash
# 切换到 deploy 用户
sudo su - deploy

# 创建 .ssh 目录
mkdir -p ~/.ssh
chmod 700 ~/.ssh

# 添加公钥
echo "你的公钥内容" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# 确保 deploy 用户有权限写入部署目录
sudo chown -R deploy:deploy /opt/sism/backend
sudo chown -R deploy:deploy /var/www/sism
```

### 2. GitHub Secrets 配置

在 GitHub 仓库中配置以下 Secrets：

**路径**: `Settings` → `Secrets and variables` → `Actions` → `New repository secret`

| Secret 名称 | 说明 | 示例值 |
|------------|------|--------|
| `SERVER_HOST` | 服务器 IP 或域名 | `175.24.139.148` |
| `SERVER_USER` | SSH 登录用户名 | `deploy` |
| `SERVER_SSH_KEY` | SSH 私钥（完整内容） | `-----BEGIN OPENSSH PRIVATE KEY-----...` |

### 3. 域名配置

#### 3.1 DNS 配置

在域名服务商控制台添加 A 记录：

| 主机记录 | 记录类型 | 记录值 |
|---------|---------|--------|
| `sism` 或 `@` | A | `你的服务器IP` |

#### 3.2 修改 Nginx 配置

```bash
# 编辑 Nginx 配置
sudo nano /etc/nginx/sites-available/sism

# 修改 server_name
server_name your-domain.com;  # 改为你的域名
```

#### 3.3 申请 SSL 证书（Let's Encrypt 免费）

```bash
# 安装 certbot
sudo apt install -y certbot python3-certbot-nginx

# 申请证书（自动配置 Nginx）
sudo certbot --nginx -d your-domain.com

# 测试自动续期
sudo certbot renew --dry-run
```

### 4. 更新项目配置

#### 4.1 后端 CORS 配置

编辑 `/opt/sism/backend/.env`：

```bash
ALLOWED_ORIGINS=https://your-domain.com
```

#### 4.2 前端 API 配置

确认 `strategic-task-management/.env.production`：

```bash
VITE_API_BASE_URL=/api
```

### 5. 触发部署

部署会在以下情况自动触发：
- 推送代码到 `main` 分支
- 手动触发：GitHub → Actions → Deploy to Production → Run workflow

### 6. 验证部署

```bash
# 检查后端状态
sudo systemctl status sism-backend

# 检查健康端点
curl https://your-domain.com/api/actuator/health

# 查看部署日志
sudo journalctl -u sism-backend -f
```

## 故障排查

### SSH 连接失败

```bash
# 测试 SSH 连接
ssh -i ~/.ssh/sism_deploy_key deploy@your-server-ip

# 检查 SSH 日志
sudo tail -f /var/log/auth.log
```

### 权限问题

```bash
# 确保部署目录权限正确
sudo chown -R deploy:deploy /opt/sism/backend
sudo chown -R deploy:deploy /var/www/sism
```

### 服务启动失败

```bash
# 查看详细日志
sudo journalctl -u sism-backend -n 100 --no-pager

# 检查环境变量
sudo cat /opt/sism/backend/.env
```

## 完整配置清单

- [ ] 服务器创建 deploy 用户
- [ ] 生成 SSH 密钥对
- [ ] 服务器添加公钥到 authorized_keys
- [ ] GitHub 添加 SERVER_HOST Secret
- [ ] GitHub 添加 SERVER_USER Secret  
- [ ] GitHub 添加 SERVER_SSH_KEY Secret
- [ ] DNS 添加 A 记录指向服务器
- [ ] 修改 Nginx server_name 为你的域名
- [ ] 申请 SSL 证书
- [ ] 更新后端 ALLOWED_ORIGINS
- [ ] 推送代码测试自动部署
