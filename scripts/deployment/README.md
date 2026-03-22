# SISM Backend Deployment Scripts

简化的部署脚本集合，用于自动化 SISM 后端的部署和维护。

---

## 🚀 快速开始

### 首次部署设置

**在服务器上运行（仅需一次）:**

```bash
# 1. SSH 到服务器
ssh your-user@your-server

# 2. 运行配置脚本
sudo ./setup-server.sh <deploy-username>

# 3. 测试部署
./deploy.sh sism-backend-1.0.0.jar
```

**在 GitHub 上:**

1. 配置 Secrets (Settings → Secrets and variables → Actions):
   - `SERVER_HOST`: 服务器地址
   - `SERVER_USER`: 部署用户名
   - `SERVER_SSH_KEY`: SSH 私钥

2. 推送代码到 main 分支触发自动部署

---

## 📁 脚本说明

### 核心脚本（2个）

#### `deploy.sh` ⭐ 通用部署脚本
**用途**: 智能部署脚本，自动检测权限并选择最佳方式

**使用**:
```bash
./deploy.sh [jar-name]
```

**功能**:
- 🔍 自动检测 sudo 权限（passwordless/password/none）
- 🔗 更新 JAR 符号链接
- 🔄 重启服务（自动选择最佳方式）
- 🏥 健康检查验证
- 🧪 API 端点测试

**支持场景**:
- ✅ CI/CD 自动部署（无密码 sudo）
- ✅ 手动部署（有密码 sudo）
- ✅ 受限环境（无 sudo 权限）

**智能检测**:
```
1. 尝试无密码 sudo → 使用 sudo systemctl
2. 尝试 systemctl --user → 使用用户级服务
3. 尝试有密码 sudo → 提示输入密码
4. 创建重启信号文件 → 等待服务自动检测
```

---

#### `setup-server.sh` ⭐ 服务器配置脚本
**用途**: 一次性服务器配置，启用无密码部署

**使用**:
```bash
sudo ./setup-server.sh <deploy-username>
```

**功能**:
- 📝 配置 sudoers (NOPASSWD)
- 👥 创建 sism 用户组
- 📂 设置目录权限
- ✅ 验证配置

**示例**:
```bash
sudo ./setup-server.sh github-deploy
```

---

### 数据库脚本（3个）

#### `init-database.sh`
**用途**: 初始化生产数据库

**使用**:
```bash
./init-database.sh
```

**功能**:
- 创建数据库
- 运行 Flyway 迁移
- 加载种子数据

---

#### `backup-database.sh`
**用途**: 备份生产数据库

**使用**:
```bash
./backup-database.sh
```

**输出**: `backup_YYYYMMDD_HHMMSS.sql`

---

#### `restore-database.sh`
**用途**: 从备份恢复数据库

**使用**:
```bash
./restore-database.sh <backup-file>
```

---

### 维护脚本（2个）

#### `health-check.sh`
**用途**: 检查服务健康状态

**使用**:
```bash
./health-check.sh
```

**检查项**:
- 服务状态
- 健康端点
- API 可访问性
- 数据库连接

---

#### `quick-setup.sh`
**用途**: 快速设置开发/测试环境

**使用**:
```bash
./quick-setup.sh
```

---

## 🔧 部署工作流

### GitHub Actions 自动部署

**触发条件**:
- 推送到 main 分支
- 手动触发 (workflow_dispatch)

**流程**:
1. 构建 JAR (Maven)
2. 上传 JAR 到服务器
3. 上传部署脚本
4. 执行 `deploy.sh`
5. 健康检查验证

**工作流文件**: `.github/workflows/deploy.yml`

---

### 手动部署

```bash
# 1. 构建 JAR
./mvnw clean package -DskipTests

# 2. 上传到服务器
scp target/sism-backend-1.0.0.jar user@server:/opt/sism/backend/

# 3. SSH 到服务器
ssh user@server

# 4. 运行部署脚本
./deploy.sh sism-backend-1.0.0.jar
```

---

## 🔐 安全配置

### Sudoers 配置

**位置**: `/etc/sudoers.d/sism-deploy`

**允许的命令**:
- `systemctl restart sism-backend`
- `systemctl status sism-backend`
- `systemctl is-active sism-backend`
- `journalctl -u sism-backend*`
- `ln -sf /opt/sism/backend/* /opt/sism/backend/sism-backend.jar`

**特点**:
- ✅ 最小权限原则
- ✅ 仅允许特定命令
- ✅ 所有操作记录在审计日志
- ✅ 不允许任意命令执行

---

### 目录权限

**结构**:
```
/opt/sism/
├── backend/              (root:sism, 775)
│   ├── sism-backend.jar  (symlink)
│   └── *.jar             (部署的 JAR 文件)
└── logs/                 (可选)
```

**权限**:
- 所有者: root
- 组: sism
- 权限: 775 (rwxrwxr-x)
- 部署用户必须在 sism 组中

---

## 🐛 故障排查

### 问题: sudo 要求密码

**解决方案**:
```bash
# 运行配置脚本
sudo ./setup-server.sh <deploy-user>

# 验证 sudoers
sudo visudo -c -f /etc/sudoers.d/sism-deploy

# 测试
sudo -n systemctl status sism-backend
```

---

### 问题: 权限被拒绝

**解决方案**:
```bash
# 检查用户组
groups <deploy-user>

# 添加到 sism 组
sudo usermod -aG sism <deploy-user>

# 重新登录
exit && ssh user@server

# 设置目录权限
sudo chown -R root:sism /opt/sism
sudo chmod -R 775 /opt/sism/backend
```

---

### 问题: 服务未重启

**解决方案**:
```bash
# 检查服务状态
sudo systemctl status sism-backend

# 查看日志
sudo journalctl -u sism-backend -n 100

# 手动重启
sudo systemctl restart sism-backend
```

---

### 问题: 健康检查失败

**解决方案**:
```bash
# 检查服务是否运行
sudo systemctl is-active sism-backend

# 检查端口
netstat -tlnp | grep 8080

# 测试健康端点
curl http://localhost:8080/api/actuator/health

# 查看应用日志
sudo journalctl -u sism-backend -f
```

---

## 📚 相关文档

- **修复指南**: `docs/deployment/fix-sudo-password-issue.md`
- **部署工作流**: `.github/workflows/deploy.yml`
- **生产配置**: `docs/deployment/springdoc-requirement.md`
- **主 README**: `README.md`

---

## 🎯 最佳实践

### 部署前

1. ✅ 在本地运行测试: `./mvnw test`
2. ✅ 备份生产数据库: `./backup-database.sh`
3. ✅ 检查服务状态: `./health-check.sh`

### 部署中

1. ✅ 监控部署日志
2. ✅ 等待健康检查通过
3. ✅ 验证 API 端点

### 部署后

1. ✅ 测试关键功能
2. ✅ 检查应用日志
3. ✅ 监控性能指标

---

## 📊 脚本简化历史

### v2.0 (2026-02-15) - 简化版本
- ✅ 合并 3 个部署脚本为 1 个通用脚本
- ✅ 合并 2 个配置脚本为 1 个服务器配置脚本
- ✅ 从 11 个文件减少到 7 个文件
- ✅ 智能权限检测，自动选择最佳方式

### v1.0 (2026-02-14) - 初始版本
- 11 个独立脚本
- 针对不同场景的专用脚本

---

## 📞 支持

遇到问题？

1. 查看 [故障排查](#-故障排查) 部分
2. 查看 [修复指南](../../docs/deployment/fix-sudo-password-issue.md)
3. 查看服务日志: `sudo journalctl -u sism-backend -n 100`
4. 联系开发团队

---

**最后更新**: 2026-02-15  
**版本**: 2.0 (简化版)
